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
package com.pspace.backend.lifecycle.lifecycle;

import org.junit.platform.commons.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.DeleteVersionRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspace.backend.libs.client.KsanClient;
import com.pspace.backend.libs.data.BackendHeaders;
import com.pspace.backend.libs.data.Constants;
import com.pspace.backend.libs.data.lifecycle.LifecycleEventData;
import com.pspace.backend.libs.data.lifecycle.LifecycleLogData;
import com.pspace.backend.libs.ksan.AgentConfig;
import com.pspace.backend.libs.ksan.data.S3RegionData;
import com.pspace.ifs.ksan.libs.mq.MQCallback;
import com.pspace.ifs.ksan.libs.mq.MQResponse;
import com.pspace.ifs.ksan.libs.mq.MQResponseCode;
import com.pspace.ifs.ksan.libs.mq.MQResponseType;
import com.pspace.ifs.ksan.libs.mq.MQSender;

public class LifecycleSender implements MQCallback {
	private final Logger logger = LoggerFactory.getLogger(LifecycleSender.class);
	private final AmazonS3 client;
	private final KsanClient ksanClient;
	private final AgentConfig ksanConfig;
	private final ObjectMapper mapper = new ObjectMapper();
	private final MQSender mq;

	public LifecycleSender(S3RegionData region) throws Exception {
		client = region.client;
		ksanClient = new KsanClient(region.address, region.port, region.accessKey, region.secretKey);

		this.ksanConfig = AgentConfig.getInstance();
		mq = new MQSender(
				ksanConfig.mqHost,
				ksanConfig.mqPort,
				ksanConfig.mqUser,
				ksanConfig.mqPassword,
				Constants.MQ_KSAN_LOG_EXCHANGE,
				Constants.MQ_EXCHANGE_OPTION_TOPIC,
				Constants.MQ_BINDING_LIFECYCLE_LOG);
	}

	@Override
	public MQResponse call(String routingKey, String body) {
		try {

			if (!routingKey.equals(Constants.MQ_BINDING_LIFECYCLE_EVENT))
				return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);

			logger.debug("{} : {}", routingKey, body);

			// 문자열을 ReplicationEventData 클래스로 변환
			var event = mapper.readValue(body, new TypeReference<LifecycleEventData>() {
			});

			// 결과값 초기화
			String result = "";

			// 3회 시도
			for (int i = 0; i < 3; i++) {
				// UploadId가 존재하지 않을 경우 오브젝트 삭제 또는 이동
				if (StringUtils.isBlank(event.uploadId)) {
					// storageClass가 존재할 경우 스토리지 클래스 이동
					if (StringUtils.isNotBlank(event.storageClass)) {
						result = restoreObject(event.bucketName, event.objectName, event.storageClass, event.versionId);
					}
					// VersionId가 존재하지 않을 경우 일반적인 삭제로 취급
					else if (StringUtils.isBlank(event.versionId))
						result = deleteObject(event.bucketName, event.objectName);

					// VersionId가 존재할 경우 버전아이디를 포함한 삭제로 취급
					else
						result = deleteObjectVersion(event.bucketName, event.objectName, event.versionId);
				}
				// UploadId가 존재할 경우 Multipart 삭제
				else
					result = abortMultipartUpload(event.bucketName, event.objectName, event.uploadId);

				// 성공했을 경우 종료
				if (result.equals(""))
					break;
			}
			// 이벤트 저장
			if (!result.isBlank())
				// 에러가 발생할 경우
				mq.send(new LifecycleLogData(event, result).toString(), Constants.MQ_BINDING_LIFECYCLE_LOG);
			else
				// 성공할 경우
				mq.send(new LifecycleLogData(event, "").toString(), Constants.MQ_BINDING_LIFECYCLE_LOG);

			return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
		} catch (Exception e) {
			logger.error("", e);
			return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_UNKNOWN_ERROR, e.getMessage(), 0);
		}
	}

	/***************************************
	 * Utility
	 *******************************************/

	protected String restoreObject(String bucketName, String objectName, String storageClass, String versionId) {

		try {
			ksanClient.storageMove(bucketName, objectName, storageClass, versionId);
			return "";
		} catch (Exception e) {
			logger.error("", e);
			return e.getMessage();
		}
	}

	protected String deleteObject(String bucketName, String objectName) {
		try {
			var request = new DeleteObjectRequest(bucketName, objectName);
			request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
			request.putCustomRequestHeader(BackendHeaders.HEADER_LIFECYCLE, BackendHeaders.HEADER_DATA);
			client.deleteObject(request);
			return "";
		} catch (Exception e) {
			logger.error("", e);
			return e.getMessage();
		}
	}

	protected String deleteObjectVersion(String bucketName, String objectName, String versionId) {
		try {
			var request = new DeleteVersionRequest(bucketName, objectName, versionId);
			request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
			request.putCustomRequestHeader(BackendHeaders.HEADER_LIFECYCLE, BackendHeaders.HEADER_DATA);
			client.deleteVersion(request);
			return "";
		} catch (Exception e) {
			logger.error("", e);
			return e.getMessage();
		}
	}

	protected String abortMultipartUpload(String bucketName, String objectName, String uploadId) {
		try {
			var request = new AbortMultipartUploadRequest(bucketName, objectName, uploadId);
			request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
			request.putCustomRequestHeader(BackendHeaders.HEADER_LIFECYCLE, BackendHeaders.HEADER_DATA);
			client.abortMultipartUpload(request);
			return "";
		} catch (Exception e) {
			logger.error("", e);
			return e.getMessage();
		}
	}
}
