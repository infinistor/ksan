/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.backend.replication.Replicator;

import java.io.InputStream;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CanonicalGrantee;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetBucketAclRequest;
import com.amazonaws.services.s3.model.GetObjectAclRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRetentionRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.SetObjectAclRequest;
import com.amazonaws.services.s3.model.SetObjectRetentionRequest;
import com.amazonaws.services.s3.model.SetObjectTaggingRequest;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.pspace.backend.libs.Utility;
import com.pspace.backend.libs.Data.BackendHeaders;
import com.pspace.backend.libs.Data.Constants;
import com.pspace.backend.libs.Data.Replication.ReplicationEventData;
import com.pspace.backend.libs.Data.Replication.ReplicationLogData;
import com.pspace.backend.libs.s3format.S3Parameters;
import com.pspace.ifs.ksan.libs.mq.MQSender;

public class SendReplicator {
	private final Logger logger = LoggerFactory.getLogger(SendReplicator.class);
	private final AmazonS3 sourceClient;
	private final AmazonS3 targetClient;
	private final ReplicationEventData event;
	private final MQSender mq;
	private final long partSize;

	public SendReplicator(AmazonS3 sourceClient, AmazonS3 targetClient, MQSender mq, ReplicationEventData event,
			long partSize) {
		this.sourceClient = sourceClient;
		this.targetClient = targetClient;
		this.event = event;
		this.partSize = partSize;
		this.mq = mq;
	}

	public void run() {
		try {
			// 복제 수행
			String result = send();

			// 복제 결과 로그를 저장한다.
			var data = new ReplicationLogData(event, result);
			mq.send(data.toString(), Constants.MQ_BINDING_REPLICATION_LOG);
			logger.info("Save Log : {}", data.toString());

		} catch (Exception e) {
			logger.error("", e);
		}
	}

	private String send() {
		logger.debug(event.toString());
		if (sourceClient == null)
			throw new IllegalStateException("Source Client is NULL");
		if (targetClient == null)
			throw new IllegalStateException("Target Client is NULL");

		// 시작 시간 설정
		event.setStartTime();

		var Result = "";
		var retryCount = 3;
		while (retryCount > 0) {
			try {
				switch (event.operation) {
					case S3Parameters.OP_PUT_OBJECT:
						putObject();
						break;
					case S3Parameters.OP_PUT_OBJECT_COPY:
						copyObject();
						break;
					case S3Parameters.OP_POST_COMPLETE:
						multiPartUpload();
						break;
					case S3Parameters.OP_PUT_OBJECT_ACL:
						putObjectACL();
						break;
					case S3Parameters.OP_PUT_OBJECT_RETENTION:
						putObjectRetention();
						break;
					case S3Parameters.OP_PUT_OBJECT_TAGGING:
						putObjectTagging();
						break;
					case S3Parameters.OP_DELETE_OBJECT_TAGGING:
						deleteObjectTagging();
						break;
					case S3Parameters.OP_DELETE_OBJECT:
						deleteObject();
						break;
					default:
						return "Operation Unknown";
				}
				return "";
			} catch (AmazonServiceException e) {
				var statusCode = e.getStatusCode();
				var result = String.format("%s(%d)", e.getErrorCode(), statusCode);
				if (statusCode < 500) {
					logger.error("[{}/{}({})] {}", event.sourceBucketName, event.objectName, event.versionId, result);
					// 복제도중 Client에러로 실패할 경우 재시도 하지 않는다.
					retryCount = 0;
					break;
				} else {
					logger.warn("", e);
					retryCount--;
				}
			} catch (Exception e) {
				Result = e.getMessage();
				logger.warn("", e);
				retryCount--;
			}
		}
		return Result;
	}

	/**
	 * 복제할 대상의 메타 데이터를 설정한다.
	 * C# API를 사용하여 업로드 할경우 Jetty와의 호환성 이슈로
	 * UTF-8이 강제로 대문자 치환되는 버그가 존재하므로
	 * 해당 내용에 대응하기 위한 예외처리가 포함되어 있습니다.
	 * 
	 * @return Object Metadata
	 * @throws Exception
	 */
	protected ObjectMetadata getObjectMetadata() throws Exception {
		var Request = new GetObjectMetadataRequest(event.sourceBucketName, event.objectName, event.versionId);
		Request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		var metadata = sourceClient.getObjectMetadata(Request);

		// 메타 정보가 비어있을 경우
		if (metadata == null) {
			logger.error("Metadata is Null!");
			throw new Exception("Metadata is Null");
		}

		// UTF-8 sign 에러를 배제하기 위해 대문자로 변경
		if (metadata.getContentType() != null)
			metadata.setContentType(metadata.getContentType().replaceAll("UTF-8", "utf-8"));
		return metadata;
	}

	/**
	 * 복제할 대상의 태그 정보를 설정한다.
	 * 
	 * @return Object Tagging
	 * @throws Exception
	 */
	protected ObjectTagging getObjectTagging() throws Exception {
		var request = new GetObjectTaggingRequest(event.sourceBucketName, event.objectName, event.versionId);
		request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		var tagSet = sourceClient.getObjectTagging(request);
		// 태그가 비어있을 경우
		if (tagSet == null)
			return null;
		return new ObjectTagging(tagSet.getTagSet());
	}

	/**
	 * 원본 버킷의 권한정보와 타깃버킷의 유저 정보를 가져와서 복제할 대상의 권한 정보를 설정한다.
	 * 
	 * @return Object ACL
	 * @throws Exception
	 */
	protected AccessControlList getObjectACL() throws Exception {
		// 원본 권한 정보 가져오기
		var sourceRequest = new GetObjectAclRequest(event.sourceBucketName, event.objectName, event.versionId);
		sourceRequest.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		var sourceACL = sourceClient.getObjectAcl(sourceRequest);

		// 타겟 버킷 권한 정보 가져오기
		var targetRequest = new GetBucketAclRequest(event.targetBucketName);
		targetRequest.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		var targetACL = targetClient.getBucketAcl(targetRequest);
		var targetOwner = targetACL.getOwner();

		var user = new CanonicalGrantee(targetOwner.getId());
		user.setDisplayName(targetOwner.getDisplayName());

		sourceACL.setOwner(targetACL.getOwner());
		sourceACL.grantPermission(user, Permission.FullControl);

		return sourceACL;
	}

	/**
	 * 오브젝트를 PutObject로 복제한다.
	 * 
	 * @throws Exception
	 */
	protected void putObject() throws Exception {
		// 메타 정보 가져오기
		var metadata = getObjectMetadata();
		// 태그 정보 받아오기
		var tagging = getObjectTagging();
		// 권한 정보 받아오기
		var acl = getObjectACL();

		// 폴더일 경우 오브젝트의 메타데이터만 전송
		InputStream body = null;
		S3Object s3Object = null;
		if (folderCheck(event.objectName))
			body = Utility.createBody("");
		// 일반적인 오브젝트일 경우 버전 정보를 포함하여 오브젝트를 다운로드
		else {
			var request = new GetObjectRequest(event.sourceBucketName, event.objectName, event.versionId);
			request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
			s3Object = sourceClient.getObject(request);
			body = s3Object.getObjectContent();
		}

		// 리퀘스트 생성
		var putRequest = new PutObjectRequest(event.targetBucketName, event.objectName, body, metadata);
		putRequest.getRequestClientOptions().setReadLimit(100000);

		// 오브젝트의 태그 정보 등록
		putRequest.setTagging(tagging);
		// 오브젝트의 ACL 정보 등록
		putRequest.setAccessControlList(acl);

		// 오브젝트 Replication
		putRequest.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		putRequest.putCustomRequestHeader(BackendHeaders.HEADER_REPLICATION, BackendHeaders.HEADER_DATA);
		putRequest.putCustomRequestHeader(BackendHeaders.HEADER_VERSION_ID, event.versionId);
		targetClient.putObject(putRequest);
		// 전송 완료 후 Stream을 닫는다.
		if (body != null)
			body.close();
		if (s3Object != null)
			s3Object.close();
	}

	/**
	 * 오브젝트를 CopyObject로 복제한다.
	 * 
	 * @throws Exception
	 */
	protected void copyObject() throws Exception {

		// 같은 시스템일 경우 복사
		if (sourceClient == targetClient) {
			var request = new CopyObjectRequest(event.sourceBucketName, event.objectName, event.targetBucketName, event.objectName)
					.withSourceVersionId(event.versionId);
			request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
			request.putCustomRequestHeader(BackendHeaders.HEADER_REPLICATION, BackendHeaders.HEADER_DATA);

			// 복제
			sourceClient.copyObject(request);
			return;
		}
		throw new IllegalStateException("Anther System CopyObject is not supported.");

	}

	/**
	 * 오브젝트를 Multipart로 복제한다.
	 * 
	 * @throws Exception
	 */
	protected void multiPartUpload() throws Exception {
		// 메타 정보 가져오기
		ObjectMetadata Metadata = getObjectMetadata();
		// 태그 정보 받아오기
		ObjectTagging Tagging = getObjectTagging();
		// 권한 정보 받아오기
		AccessControlList ACL = getObjectACL();

		var InitRequest = new InitiateMultipartUploadRequest(event.targetBucketName, event.objectName, Metadata);
		// 오브젝트의 태그 정보 등록
		InitRequest.setTagging(Tagging);
		// 오브젝트의 ACL 정보 등록
		InitRequest.setAccessControlList(ACL);
		// 헤더추가
		InitRequest.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		// Multipart 등록
		var InitResponse = targetClient.initiateMultipartUpload(InitRequest);
		var UploadId = InitResponse.getUploadId();

		// 오브젝트의 사이즈 확인
		var Size = Metadata.getContentLength();

		// 업로드 시작
		var partList = new ArrayList<PartETag>();
		int PartNumber = 1;
		long startPosition = 0;

		while (startPosition < Size) {
			long EndPosition = startPosition + partSize;
			if (EndPosition > Size)
				EndPosition = Size;

			// 업로드할 내용 가져오기
			var request = new GetObjectRequest(event.sourceBucketName, event.objectName)
					.withRange(startPosition, EndPosition - 1);
			request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, "");
			var s3Object = sourceClient.getObject(request);

			// 업로드 파츠 생성
			var partRequest = new UploadPartRequest()
					.withBucketName(event.targetBucketName)
					.withKey(event.objectName)
					.withUploadId(UploadId)
					.withPartNumber(PartNumber++)
					.withInputStream(s3Object.getObjectContent())
					.withPartSize(s3Object.getObjectMetadata().getContentLength());

			// 헤더 추가
			partRequest.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
			partRequest.putCustomRequestHeader(BackendHeaders.HEADER_REPLICATION, BackendHeaders.HEADER_DATA);
			partRequest.putCustomRequestHeader(BackendHeaders.S3PROXY_HEADER_NO_DR, BackendHeaders.HEADER_DATA);

			var PartResPonse = targetClient.uploadPart(partRequest);
			partList.add(PartResPonse.getPartETag());

			startPosition += partSize;
			s3Object.close();
		}

		// 멀티파트 업로드 종료
		var compRequest = new CompleteMultipartUploadRequest(event.targetBucketName, event.objectName, UploadId, partList);

		// 헤더추가
		compRequest.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		compRequest.putCustomRequestHeader(BackendHeaders.HEADER_REPLICATION, BackendHeaders.HEADER_DATA);
		compRequest.putCustomRequestHeader(BackendHeaders.HEADER_VERSION_ID, event.versionId);

		targetClient.completeMultipartUpload(compRequest);
	}

	/**
	 * 오브젝트의 ACL 정보를 복제한다.
	 * 
	 * @throws Exception
	 */
	protected void putObjectACL() throws Exception {
		// 권한 정보 받아오기
		var ACL = getObjectACL();

		// ACL 정보 설정
		var request = new SetObjectAclRequest(event.targetBucketName, event.objectName, ACL);
		// 헤더추가
		request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		request.putCustomRequestHeader(BackendHeaders.HEADER_REPLICATION, BackendHeaders.HEADER_DATA);
		targetClient.setObjectAcl(request);
	}

	/**
	 * 오브젝트의 Retention 정보를 복제한다.
	 * 
	 * @throws Exception
	 */
	protected void putObjectRetention() throws Exception {
		// Retention 가져오기
		var getRequest = new GetObjectRetentionRequest().withBucketName(event.sourceBucketName)
				.withKey(event.objectName).withVersionId(event.versionId);
		getRequest.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		var ObjectRetention = sourceClient.getObjectRetention(getRequest);

		// Retention 설정
		var setRequest = new SetObjectRetentionRequest().withBucketName(event.targetBucketName)
				.withKey(event.objectName).withRetention(ObjectRetention.getRetention());
		// 헤더추가
		setRequest.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		setRequest.putCustomRequestHeader(BackendHeaders.HEADER_REPLICATION, BackendHeaders.HEADER_DATA);
		targetClient.setObjectRetention(setRequest);
	}

	/**
	 * 오브젝트의 Tagging 정보를 복제한다.
	 * 
	 * @throws Exception
	 */
	protected void putObjectTagging() throws Exception {
		// Tagging 가져오기
		var getRequest = new GetObjectTaggingRequest(event.sourceBucketName, event.objectName, event.versionId);
		getRequest.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		var tagging = sourceClient.getObjectTagging(getRequest);

		// Tagging 설정
		ObjectTagging objectTagging = new ObjectTagging(tagging.getTagSet());
		var request = new SetObjectTaggingRequest(event.targetBucketName, event.objectName, objectTagging);
		// 헤더추가
		request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		request.putCustomRequestHeader(BackendHeaders.HEADER_REPLICATION, BackendHeaders.HEADER_DATA);
		targetClient.setObjectTagging(request);
	}

	/**
	 * 오브젝트를 삭제한다.
	 * 
	 * @throws Exception
	 */
	protected void deleteObject() throws Exception {
		var request = new DeleteObjectRequest(event.targetBucketName, event.objectName);
		// 헤더추가
		request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		request.putCustomRequestHeader(BackendHeaders.HEADER_REPLICATION, BackendHeaders.HEADER_DATA);
		targetClient.deleteObject(request);
	}

	/**
	 * 오브젝트의 Tagging 정보를 삭제한다.
	 * 
	 * @throws Exception
	 */
	protected void deleteObjectTagging() throws Exception {
		var request = new DeleteObjectTaggingRequest(event.targetBucketName, event.objectName);
		// 헤더추가
		request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		request.putCustomRequestHeader(BackendHeaders.HEADER_REPLICATION, BackendHeaders.HEADER_DATA);
		targetClient.deleteObjectTagging(request);
	}

	/******************************************
	 * Utility
	 *************************************************/

	protected boolean folderCheck(String objectName) {
		return objectName.endsWith("/");
	}
}
