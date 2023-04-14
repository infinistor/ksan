/*
* Copyright 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE.md for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.backend.lifecycle.Lifecycle;

import org.junit.platform.commons.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.DeleteVersionRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspace.backend.libs.AdminClient.KsanClient;
import com.pspace.backend.libs.Data.BackendHeaders;
import com.pspace.backend.libs.Data.Constants;
import com.pspace.backend.libs.Data.Lifecycle.LifecycleEventData;
import com.pspace.backend.libs.Data.Lifecycle.LifecycleLogData;
import com.pspace.backend.libs.Ksan.AgentConfig;
import com.pspace.backend.libs.Ksan.Data.S3RegionData;
import com.pspace.ifs.ksan.libs.mq.MQCallback;
import com.pspace.ifs.ksan.libs.mq.MQResponse;
import com.pspace.ifs.ksan.libs.mq.MQResponseCode;
import com.pspace.ifs.ksan.libs.mq.MQResponseType;
import com.pspace.ifs.ksan.libs.mq.MQSender;

public class LifecycleSender implements MQCallback {
	private final Logger logger = LoggerFactory.getLogger(LifecycleSender.class);
	private final AmazonS3 Client;
	private final KsanClient ksanClient;
	private final AgentConfig ksanConfig;
	private final ObjectMapper Mapper = new ObjectMapper();
	private final MQSender mq;

	public LifecycleSender(S3RegionData region) throws Exception {
		Client = CreateClient(region);
		ksanClient = new KsanClient(region.Address, region.Port, region.AccessKey, region.SecretKey);

		this.ksanConfig = AgentConfig.getInstance();
		mq = new MQSender(
				ksanConfig.MQHost,
				ksanConfig.MQPort,
				ksanConfig.MQUser,
				ksanConfig.MQPassword,
				Constants.MQ_KSAN_LOG_EXCHANGE,
				Constants.MQ_EXCHANGE_OPTION_TOPIC,
				Constants.MQ_BINDING_LIFECYCLE_LOG);
	}

	@Override
	public MQResponse call(String routingKey, String body) {
		try {
			// logger.debug("{} -> {}", routingKey, body);

			if (!routingKey.equals(Constants.MQ_BINDING_LIFECYCLE_EVENT))
				return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);


			// 문자열을 ReplicationEventData 클래스로 변환
			var event = Mapper.readValue(body, new TypeReference<LifecycleEventData>() {});
			logger.info(event.toString());

			// 결과값 초기화
			String Result = "";

			// 3회 시도
			for (int i = 0; i < 3; i++) {
				// UploadId가 존재하지 않을 경우 오브젝트 삭제 또는 이동
				if (StringUtils.isBlank(event.uploadId)) {
					// storageClass가 존재할 경우 스토리지 클래스 이동
					if (StringUtils.isNotBlank(event.storageClass)) {
						Result = RestoreObject(event.bucketName, event.objectName, event.storageClass, event.versionId);
					}
					// VersionId가 존재하지 않을 경우 일반적인 삭제로 취급
					else if (StringUtils.isBlank(event.versionId))
						Result = DeleteObject(event.bucketName, event.objectName);

					// VersionId가 존재할 경우 버전아이디를 포함한 삭제로 취급
					else
						Result = DeleteObjectVersion(event.bucketName, event.objectName,
								event.versionId);
				}
				// UploadId가 존재할 경우 Multipart 삭제
				else
					Result = AbortMultipartUpload(event.bucketName, event.objectName, event.uploadId);

				// 성공했을 경우 종료
				if (Result.equals(""))
					break;
			}
			// 에러가 발생할 경우
			if (!Result.isBlank()) {
				// 이벤트 저장
				try {
					var item = new LifecycleLogData(event, Result);
					mq.send(item.toString(), Constants.MQ_BINDING_LIFECYCLE_LOG);

				} catch (Exception e) {
					logger.error("", e);
				}
			}
			// 성공할 경우
			else {
				mq.send(event.toString(), Constants.MQ_BINDING_LIFECYCLE_LOG);
			}

			return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
		} catch (Exception e) {
			logger.error("", e);
			return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_UNKNOWN_ERROR, e.getMessage(), 0);
		}
	}

	/***************************************
	 * Utility
	 *******************************************/

	protected AmazonS3 CreateClient(S3RegionData region) {
		BasicAWSCredentials credentials = new BasicAWSCredentials(region.AccessKey, region.SecretKey);

		return AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials))
				.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(region.getHttpURL(), ""))
				.withPathStyleAccessEnabled(true).build();
	}

	protected String RestoreObject(String bucketName, String objectName, String storageClass, String versionId) {

		var Result = "";
		try {
			ksanClient.StorageMove(bucketName, objectName, storageClass, versionId);
		} catch (Exception e) {
			logger.error("", e);
			Result = e.getMessage();
		}
		return Result;
	}

	protected String DeleteObject(String bucketName, String objectName) {
		var Result = "";
		try {
			var Request = new DeleteObjectRequest(bucketName, objectName);
			Request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
			Request.putCustomRequestHeader(BackendHeaders.HEADER_LIFECYCLE, BackendHeaders.HEADER_DATA);
			Client.deleteObject(Request);
		} catch (Exception e) {
			logger.error("", e);
			Result = e.getMessage();
		}
		return Result;
	}

	protected String DeleteObjectVersion(String bucketName, String objectName, String VersionId) {
		var Result = "";
		try {
			var Request = new DeleteVersionRequest(bucketName, objectName, VersionId);
			Request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
			Request.putCustomRequestHeader(BackendHeaders.HEADER_LIFECYCLE, BackendHeaders.HEADER_DATA);
			Client.deleteVersion(Request);
		} catch (Exception e) {
			logger.error("", e);
			Result = e.getMessage();
		}
		return Result;
	}

	protected String AbortMultipartUpload(String bucketName, String objectName, String UploadId) {
		var Result = "";
		try {
			var Request = new AbortMultipartUploadRequest(bucketName, objectName, UploadId);
			Request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
			Request.putCustomRequestHeader(BackendHeaders.HEADER_LIFECYCLE, BackendHeaders.HEADER_DATA);
			Client.abortMultipartUpload(Request);
		} catch (Exception e) {
			logger.error("", e);
			Result = e.getMessage();
		}
		return Result;
	}
}
