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

	public SendReplicator(AmazonS3 sourceClient, AmazonS3 targetClient, MQSender mq, ReplicationEventData event, long partSize) {
		this.sourceClient = sourceClient;
		this.targetClient = targetClient;
		this.event = event;
		this.partSize = partSize;
		this.mq = mq;
	}

	public void run() {
		try {
			// 복제 수행
			String result = Send();

			// 복제 결과 로그를 저장한다.
			var data = new ReplicationLogData(event, result);
			mq.send(data.toString(), Constants.MQ_BINDING_REPLICATION_LOG);
			logger.info("Save Log : {}", data.toString());

		} catch (Exception e) {
			logger.error("", e);
		}
	}

	private String Send() {
		logger.debug(event.toString());
		if (sourceClient == null) throw new IllegalStateException("Source Client is NULL");
		if (targetClient == null) throw new IllegalStateException("Target Client is NULL");

		var Result = "";
		var RetryCount = 3;
		while (RetryCount > 0) {
			try {
				switch (event.Operation) {
					case S3Parameters.OP_PUT_OBJECT:
						PutObject();
						break;
					case S3Parameters.OP_PUT_OBJECT_COPY:
						CopyObject();
						break;
					case S3Parameters.OP_POST_COMPLETE:
						MultiPartUpload();
						break;
					case S3Parameters.OP_PUT_OBJECT_ACL:
						PutObjectACL();
						break;
					case S3Parameters.OP_PUT_OBJECT_RETENTION:
						PutObjectRetention();
						break;
					case S3Parameters.OP_PUT_OBJECT_TAGGING:
						PutObjectTagging();
						break;
					case S3Parameters.OP_DELETE_OBJECT_TAGGING:
						DeleteObjectTagging();
						break;
					case S3Parameters.OP_DELETE_OBJECT:
						DeleteObject();
						break;
					default:
						return "Operation Unknown";
				}
				return "";
			} catch (AmazonServiceException e) {
				Result = String.format("%s(%d) - %s", e.getErrorCode(), e.getStatusCode(), e.getMessage());
				logger.warn("", e);
				RetryCount--;
			} catch (Exception e) {
				Result = e.getMessage();
				logger.warn("", e);
				RetryCount--;
			}
		}
		return Result;
	}

	/**
	 * 복제할 대상의 메타 데이터를 설정한다.
	 * C# API를 사용하여 업로드 할경우 Jetty와의 호환성 이슈로
	 * UTF-8이 강제로 대문자 치환되는 버그가 존재하므로
	 * 해당 내용에 대응하기 위한 예외처리가 포함되어 있습니다.
	 * @return Object Metadata
	 * @throws Exception
	 */
	protected ObjectMetadata GetObjectMetadata() throws Exception {
		var Request = new GetObjectMetadataRequest(event.SourceBucketName, event.ObjectName, event.VersionId);
		Request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		var Metadata = sourceClient.getObjectMetadata(Request);

		// 메타 정보가 비어있을 경우
		if (Metadata == null) {
			logger.error("Metadata is Null!");
			throw new Exception("Metadata is Null");
		}

		// UTF-8 sign 에러를 배제하기 위해 대문자로 변경
		if (Metadata.getContentType() != null)
			Metadata.setContentType(Metadata.getContentType().replaceAll("UTF-8", "utf-8"));
		return Metadata;
	}

	/**
	 * 복제할 대상의 태그 정보를 설정한다.
	 * @return Object Tagging
	 * @throws Exception
	 */
	protected ObjectTagging GetObjectTagging() throws Exception {
		var Request = new GetObjectTaggingRequest(event.SourceBucketName, event.ObjectName, event.VersionId);
		Request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		var TagSet = sourceClient.getObjectTagging(Request);
		// 태그가 비어있을 경우
		if (TagSet == null)
			return null;
		return new ObjectTagging(TagSet.getTagSet());
	}

	/**
	 * 원본 버킷의 권한정보와 타깃버킷의 유저 정보를 가져와서 복제할 대상의 권한 정보를 설정한다.
	 * @return Object ACL
	 * @throws Exception
	 */
	protected AccessControlList GetObjectACL() throws Exception {
		// 원본 권한 정보 가져오기
		var SourceRequest = new GetObjectAclRequest(event.SourceBucketName, event.ObjectName, event.VersionId);
		SourceRequest.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		var SourceACL = sourceClient.getObjectAcl(SourceRequest);

		// 타겟 버킷 권한 정보 가져오기
		var TargetRequest = new GetBucketAclRequest(event.TargetBucketName);
		TargetRequest.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		var TargetACL = targetClient.getBucketAcl(TargetRequest);
		var TargetOwner = TargetACL.getOwner();

		var User = new CanonicalGrantee(TargetOwner.getId());
		User.setDisplayName(TargetOwner.getDisplayName());

		SourceACL.setOwner(TargetACL.getOwner());
		SourceACL.grantPermission(User, Permission.FullControl);

		return SourceACL;
	}

	protected void PutObject() throws Exception {
		// 메타 정보 가져오기
		var Metadata = GetObjectMetadata();
		// 태그 정보 받아오기
		var Tagging = GetObjectTagging();
		// 권한 정보 받아오기
		var ACL = GetObjectACL();

		// 폴더일 경우 오브젝트의 메타데이터만 전송
		InputStream Body = null;
		S3Object MyObject = null;
		if (FolderCheck(event.ObjectName))
			Body = Utility.CreateBody("");
		// 일반적인 오브젝트일 경우 버전 정보를 포함하여 오브젝트를 다운로드
		else {
			var Request = new GetObjectRequest(event.SourceBucketName, event.ObjectName, event.VersionId);
			Request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
			MyObject = sourceClient.getObject(Request);
			Body = MyObject.getObjectContent();
		}

		// 리퀘스트 생성
		var PutRequest = new PutObjectRequest(event.TargetBucketName, event.ObjectName, Body, Metadata);
		// 오브젝트의 태그 정보 등록
		PutRequest.setTagging(Tagging);
		// 오브젝트의 ACL 정보 등록
		PutRequest.setAccessControlList(ACL);

		// 오브젝트 Replication
		PutRequest.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		PutRequest.putCustomRequestHeader(BackendHeaders.HEADER_REPLICATION, BackendHeaders.HEADER_DATA);
		PutRequest.putCustomRequestHeader(BackendHeaders.HEADER_VERSIONID, event.VersionId);
		targetClient.putObject(PutRequest);
		if (MyObject != null)
			MyObject.close();
	}

	protected void CopyObject() throws Exception {

		// 같은 시스템일 경우 복사
		if (sourceClient == targetClient) {
			var Request = new CopyObjectRequest(event.SourceBucketName, event.ObjectName, event.TargetBucketName,
					event.ObjectName)
					.withSourceVersionId(event.VersionId);
			Request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
			Request.putCustomRequestHeader(BackendHeaders.HEADER_REPLICATION, BackendHeaders.HEADER_DATA);

			// 복제
			sourceClient.copyObject(Request);
			return;
		}
		throw new IllegalStateException("Anther System CopyObject is not supported.");

	}

	protected void MultiPartUpload() throws Exception {
		// 메타 정보 가져오기
		ObjectMetadata Metadata = GetObjectMetadata();
		// 태그 정보 받아오기
		ObjectTagging Tagging = GetObjectTagging();
		// 권한 정보 받아오기
		AccessControlList ACL = GetObjectACL();

		var InitRequest = new InitiateMultipartUploadRequest(event.TargetBucketName, event.ObjectName, Metadata);
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
		long StartPosition = 0;

		while (StartPosition < Size) {
			long EndPosition = StartPosition + partSize;
			if (EndPosition > Size)
				EndPosition = Size;

			// 업로드할 내용 가져오기
			var Request = new GetObjectRequest(event.SourceBucketName, event.ObjectName)
					.withRange(StartPosition, EndPosition - 1);
			Request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, "");
			var s3Object = sourceClient.getObject(Request);

			// 업로드 파츠 생성
			var PartRequest = new UploadPartRequest()
					.withBucketName(event.TargetBucketName)
					.withKey(event.ObjectName)
					.withUploadId(UploadId)
					.withPartNumber(PartNumber++)
					.withInputStream(s3Object.getObjectContent())
					.withPartSize(s3Object.getObjectMetadata().getContentLength());

			// 헤더 추가
			PartRequest.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
			PartRequest.putCustomRequestHeader(BackendHeaders.HEADER_REPLICATION, BackendHeaders.HEADER_DATA);
			PartRequest.putCustomRequestHeader(BackendHeaders.S3PROXY_HEADER_NO_DR, BackendHeaders.HEADER_DATA);

			var PartResPonse = targetClient.uploadPart(PartRequest);
			partList.add(PartResPonse.getPartETag());

			StartPosition += partSize;
			s3Object.close();
		}

		// 멀티파트 업로드 종료
		var CompRequest = new CompleteMultipartUploadRequest(event.TargetBucketName, event.ObjectName, UploadId,
				partList);

		// 헤더추가
		CompRequest.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		CompRequest.putCustomRequestHeader(BackendHeaders.HEADER_REPLICATION, BackendHeaders.HEADER_DATA);
		CompRequest.putCustomRequestHeader(BackendHeaders.HEADER_VERSIONID, event.VersionId);

		targetClient.completeMultipartUpload(CompRequest);
	}

	protected void PutObjectACL() throws Exception {
		// 권한 정보 받아오기
		var ACL = GetObjectACL();

		// ACL 정보 설정
		var SetRequest = new SetObjectAclRequest(event.TargetBucketName, event.ObjectName, ACL);
		// 헤더추가
		SetRequest.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		SetRequest.putCustomRequestHeader(BackendHeaders.HEADER_REPLICATION, BackendHeaders.HEADER_DATA);
		targetClient.setObjectAcl(SetRequest);
	}

	protected void PutObjectRetention() throws Exception {
		// Retention 가져오기
		var GetRequest = new GetObjectRetentionRequest().withBucketName(event.SourceBucketName)
				.withKey(event.ObjectName).withVersionId(event.VersionId);
		GetRequest.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		var ObjectRetention = sourceClient.getObjectRetention(GetRequest);

		// Retention 설정
		var SetRequest = new SetObjectRetentionRequest().withBucketName(event.TargetBucketName)
				.withKey(event.ObjectName).withRetention(ObjectRetention.getRetention());
		// 헤더추가
		SetRequest.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		SetRequest.putCustomRequestHeader(BackendHeaders.HEADER_REPLICATION, BackendHeaders.HEADER_DATA);
		targetClient.setObjectRetention(SetRequest);
	}

	protected void PutObjectTagging() throws Exception {
		// Tagging 가져오기
		var GetRequest = new GetObjectTaggingRequest(event.SourceBucketName, event.ObjectName,
				event.VersionId);
		GetRequest.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		var Tagging = sourceClient.getObjectTagging(GetRequest);

		// Tagging 설정
		ObjectTagging ObjectTagging = new ObjectTagging(Tagging.getTagSet());
		var SetRequest = new SetObjectTaggingRequest(event.TargetBucketName, event.ObjectName,
				ObjectTagging);
		// 헤더추가
		SetRequest.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		SetRequest.putCustomRequestHeader(BackendHeaders.HEADER_REPLICATION, BackendHeaders.HEADER_DATA);
		targetClient.setObjectTagging(SetRequest);
	}

	protected void DeleteObject() throws Exception {
		var DelRequest = new DeleteObjectRequest(event.TargetBucketName, event.ObjectName);
		// 헤더추가
		DelRequest.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		DelRequest.putCustomRequestHeader(BackendHeaders.HEADER_REPLICATION, BackendHeaders.HEADER_DATA);
		targetClient.deleteObject(DelRequest);
	}

	protected void DeleteObjectTagging() throws Exception {
		var DelRequest = new DeleteObjectTaggingRequest(event.TargetBucketName, event.ObjectName);
		// 헤더추가
		DelRequest.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
		DelRequest.putCustomRequestHeader(BackendHeaders.HEADER_REPLICATION, BackendHeaders.HEADER_DATA);
		targetClient.deleteObjectTagging(DelRequest);
	}

	/******************************************
	 * Utility
	 *************************************************/

	protected boolean FolderCheck(String ObjectName) {
		return ObjectName.endsWith("/");
	}
}
