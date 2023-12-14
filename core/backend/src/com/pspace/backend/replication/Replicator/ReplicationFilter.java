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
package com.pspace.backend.Replication.Replicator;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspace.backend.Libs.Data.Constants;
import com.pspace.backend.Libs.Data.Replication.ReplicationEventData;
import com.pspace.backend.Libs.Data.S3.S3BucketData;
import com.pspace.backend.Libs.Data.S3.S3LogData;
import com.pspace.backend.Libs.Ksan.AgentConfig;
import com.pspace.backend.Libs.Ksan.ObjManagerHelper;
import com.pspace.backend.Libs.S3.S3Parameters;
import com.pspace.ifs.ksan.libs.mq.*;

public class ReplicationFilter implements MQCallback {
	private static final Logger logger = LoggerFactory.getLogger(ReplicationFilter.class);
	private final AgentConfig ksanConfig;
	private final MQSender mq;

	public ReplicationFilter() throws Exception {
		this.ksanConfig = AgentConfig.getInstance();
		mq = new MQSender(
				ksanConfig.mqHost,
				ksanConfig.mqPort,
				ksanConfig.mqUser,
				ksanConfig.mqPassword,
				Constants.MQ_KSAN_LOG_EXCHANGE,
				Constants.MQ_EXCHANGE_OPTION_TOPIC,
				Constants.MQ_BINDING_REPLICATION_EVENT);
	}

	@Override
	public MQResponse call(String routingKey, String body) {

		try {
			// GW 로그가 아닐 경우 무시
			if (!routingKey.equals(Constants.MQ_BINDING_GW_LOG))
				return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);

			logger.debug("{} : {}", routingKey, body);

			// 문자열을 Log 클래스로 변환
			var Mapper = new ObjectMapper();
			var s3Log = Mapper.readValue(body, new TypeReference<S3LogData>() {
			});

			// 변환 실패할 경우 에러를 전송한다.
			if (s3Log == null) {
				logger.error("Failed to read S3 Logging");
				return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_INVALID_REQUEST, "", 0);
			}
			logger.info("S3 Log : {}", s3Log.toString());

			// 버킷 이름이 비어있을 경우 무시
			if (s3Log.isBucketNameEmpty())
				return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);

			// 기본정보 정의
			var Operation = s3Log.operation;
			var bucketName = s3Log.bucketName;

			// 해당 작업이 에러이거나 복제 대상이 아닐경우 무시
			if (s3Log.isError() || !S3Parameters.ReplicateOperationCheck(Operation))
				return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);

			// 버킷 정보를 가져온다.
			var objManager = ObjManagerHelper.getInstance();
			var bucketInfo = objManager.getBucket(bucketName);

			// Replication 필터링
			if (bucketInfo.isReplication)
				ReplicationEventFiltering(bucketInfo, s3Log);
			logger.info("Replication Filter End");

		} catch (Exception e) {
			logger.error("", e);
			return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_UNKNOWN_ERROR, e.getMessage(), 0);
		}
		// 정상적으로 처리되었음을 알린다.
		return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
	}

	private void ReplicationEventFiltering(S3BucketData bucketInfo, S3LogData s3Log) {
		try {
			// 복제 설정이 없을 경우 스킵
			if (!bucketInfo.isReplication)
				return;

			var Operation = s3Log.operation; // 오퍼레이션 가져오기
			var ReplicationRules = bucketInfo.Replications;// 룰 정보 가져오기
			var sourceBucketName = bucketInfo.BucketName; // 소스버킷 이름 가져오기
			var ObjectName = s3Log.objectName;
			var versionId = s3Log.versionId;

			// 룰이 존재하지 않을 경우 스킵
			if (ReplicationRules == null)
				return;

			logger.info("Replication Event Check {}", s3Log.operation);

			// ObjManager 가져오기
			var objManager = ObjManagerHelper.getInstance();

			// 모든 룰에 대해 필터링
			for (var MyRule : ReplicationRules) {

				try {
					// 룰이 비어있을 경우 스킵
					if (MyRule == null) {
						logger.error("[{}] Replication Rule Invalid!", sourceBucketName);
						continue;
					}

					// 룰이 활성화 되어있지 않을 경우 스킵
					if (!MyRule.Status)
						continue;

					// 오퍼레이션이 Delete이고 설정이 Delete가 활성화 되어있지 않을 경우 스킵
					if (S3Parameters.DeleteOperationCheck(Operation) && !MyRule.DeleteMarker)
						continue;

					// 필터 설정이 존재할 경우
					if (MyRule.isFiltering) {
						// Prefix 설정이 있다면
						if (StringUtils.isBlank(MyRule.Prefix)) {
							// 해당 오브젝트의 Prefix가 일치하지 않을 경우 스킵
							if (!s3Log.objectName.startsWith(MyRule.Prefix))
								continue;

							// And 태그 설정이 있다면
							var TagList = MyRule.Tags;
							if (TagList.size() > 0) {
								// 해당 오브젝트의 태그 정보를 가져오기
								var Object = objManager.getObject(sourceBucketName, ObjectName, versionId);

								// 해당 오브젝트가 존재하지 않을 경우 스킵
								if (Object == null)
									continue;

								// 해당 오브젝트의 태그가 존재하지 않을 경우 스킵
								if (Object.tags == null)
									continue;
								if (Object.tags.size() == 0)
									continue;

								int TagCount = 0;

								// 필터에 설정된 모든 태그를 비교
								for (var FilterTag : MyRule.Tags) {
									// 필터에 설정된 태그가 오브젝트에 존재하는지 확인
									for (var MyTag : MyRule.Tags) {
										// 오브젝트의 태그가 필터에 설정된 태그가 일치할 경우 태그 갯수를 1 증가
										if (MyTag.key == FilterTag.key && MyTag.value == FilterTag.value)
											TagCount++;
									}
								}

								// 해당 오브젝트의 태그목록에 설정한 태그들이 존재하지 않을 경우 스킵
								if (TagCount != MyRule.Tags.size())
									continue;
							}
						}
					}
					// 이벤트 저장
					var Data = new ReplicationEventData(Operation, s3Log.objectName, s3Log.versionId, sourceBucketName, MyRule.TargetBucket, MyRule.TargetRegion);
					mq.send(Data.toString(), Constants.MQ_BINDING_REPLICATION_EVENT);
					logger.info("Save Event : {}", Data.toString());
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			logger.error("", e);
		}
	}

}
