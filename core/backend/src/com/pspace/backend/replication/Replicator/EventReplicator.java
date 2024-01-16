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

import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspace.backend.libs.data.Constants;
import com.pspace.backend.libs.data.Replication.ReplicationEventData;
import com.pspace.ifs.ksan.libs.mq.MQCallback;
import com.pspace.ifs.ksan.libs.mq.MQResponse;
import com.pspace.ifs.ksan.libs.mq.MQResponseCode;
import com.pspace.ifs.ksan.libs.mq.MQResponseType;
import com.pspace.ifs.ksan.libs.mq.MQSender;

public class EventReplicator extends BaseReplicator implements MQCallback {

	private final MQSender mq;

	public EventReplicator() throws Exception {
		super(LoggerFactory.getLogger(EventReplicator.class));
		mq = new MQSender(
				ksanConfig.mqHost,
				ksanConfig.mqPort,
				ksanConfig.mqUser,
				ksanConfig.mqPassword,
				Constants.MQ_KSAN_LOG_EXCHANGE,
				Constants.MQ_EXCHANGE_OPTION_TOPIC,
				Constants.MQ_BINDING_REPLICATION_LOG);
	}

	@Override
	public MQResponse call(String routingKey, String body) {
		try {

			if (!routingKey.equals(Constants.MQ_BINDING_REPLICATION_EVENT))
				return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);

			logger.debug("{} : {}", routingKey, body);

			// 문자열을 ReplicationEventData 클래스로 변환
			var mapper = new ObjectMapper();
			var event = mapper.readValue(body, new TypeReference<ReplicationEventData>() {
			});
			// 변환 실패시
			if (event == null) {
				logger.error("Invalid event data");
				return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_UNKNOWN_ERROR, "Invalid event data", 0);
			}

			// 타겟 클라이언트 생성
			AmazonS3 targetClient = getClient(event);

			// 전송 객체 생성
			var sender = new SendReplicator(sourceClient, targetClient, mq, event, config.partSize);

			// 복제 시작
			sender.run();
		} catch (Exception e) {
			logger.error("", e);
			return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_UNKNOWN_ERROR, e.getMessage(), 0);
		}

		return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
	}
}
