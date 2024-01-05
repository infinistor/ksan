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
package com.pspace.backend.logger.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspace.backend.libs.data.Constants;
import com.pspace.backend.libs.data.Replication.ReplicationFailedData;
import com.pspace.backend.libs.data.Replication.ReplicationSuccessData;
import com.pspace.backend.libs.db.DBManager;
import com.pspace.ifs.ksan.libs.mq.MQCallback;
import com.pspace.ifs.ksan.libs.mq.MQResponse;
import com.pspace.ifs.ksan.libs.mq.MQResponseCode;
import com.pspace.ifs.ksan.libs.mq.MQResponseType;

public class ReplicationLogReceiver implements MQCallback {
	private final Logger logger = LoggerFactory.getLogger(ReplicationLogReceiver.class);
	private final ObjectMapper mapper = new ObjectMapper();
	private final DBManager db = DBManager.getInstance();

	@Override
	public MQResponse call(String routingKey, String body) {

		try {
			logger.debug("{} -> {}", routingKey, body);

			// routingKey나 body가 null이거나 empty일 경우
			if (routingKey == null || body == null)
				return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_UNKNOWN_ERROR, "routingKey or body is null", 0);
			else if (routingKey.isEmpty() || body.isEmpty())
				return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_UNKNOWN_ERROR, "routingKey or body is empty", 0);

			// routingKey가 Replication Success Log 일경우
			else if (routingKey.equals(Constants.MQ_BINDING_REPLICATION_LOG_SUCCESS)) {
				// 문자열을 ReplicationSuccessData 클래스로 변환
				var event = mapper.readValue(body, new TypeReference<ReplicationSuccessData>() {
				});

				// 변환 실패시
				if (event == null) {
					logger.error("Failed to convert string to ReplicationSuccessData");
					return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_UNKNOWN_ERROR, "Failed to convert string to ReplicationSuccessData", 0);
				}

				// DB에 저장
				logger.info("ReplicationSuccessData : {}", event);
				db.insertReplicationSuccess(event);
			}
			// routingKey가 Replication Failed Log 일경우
			else if (routingKey.equals(Constants.MQ_BINDING_REPLICATION_LOG_FAILED)) {
				// 문자열을 ReplicationFailedData 클래스로 변환
				var event = mapper.readValue(body, new TypeReference<ReplicationFailedData>() {
				});

				// 변환 실패시
				if (event == null) {
					logger.error("Failed to convert string to ReplicationFailedData");
					return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_UNKNOWN_ERROR, "Failed to convert string to ReplicationFailedData", 0);
				}

				// DB에 저장
				logger.info("ReplicationFailedData : {}", event);
				db.insertReplicationFailed(event);
			} else {
				logger.error("Unknown routingKey : {}", routingKey);
				return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_UNKNOWN_ERROR, "Unknown routingKey", 0);
			}
			return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);

		} catch (Exception e) {
			logger.error("", e);
			return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_UNKNOWN_ERROR, e.getMessage(), 0);
		}

	}

}
