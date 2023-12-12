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
package com.pspace.backend.LogManager.Logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspace.backend.Libs.Data.Constants;
import com.pspace.backend.Libs.Data.S3.S3LogData;
import com.pspace.backend.LogManager.DB.DBManager;
import com.pspace.ifs.ksan.libs.mq.MQCallback;
import com.pspace.ifs.ksan.libs.mq.MQResponse;
import com.pspace.ifs.ksan.libs.mq.MQResponseCode;
import com.pspace.ifs.ksan.libs.mq.MQResponseType;

public class BackendLogReceiver implements MQCallback {
	private final Logger logger = LoggerFactory.getLogger(BackendLogReceiver.class);
	private final ObjectMapper Mapper = new ObjectMapper();
	private final DBManager db = DBManager.getInstance();

	@Override
	public MQResponse call(String routingKey, String body) {

		try {
			logger.debug("{} -> {}", routingKey, body);

			if (!routingKey.equals(Constants.MQ_BINDING_BACKEND_LOG))
				return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);

			// 문자열을 S3LogData 클래스로 변환
			var event = Mapper.readValue(body, new TypeReference<S3LogData>() {
			});
			// 변환 실패시
			if (event == null)
				throw new Exception("Invalid BackendLogData : " + body);

			// DB에 저장
			logger.info("BackendLogData : {}", event.toString());
			db.insertBackendLog(event);

		} catch (Exception e) {
			logger.error("", e);
			return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_UNKNOWN_ERROR, e.getMessage(), 0);
		}

		return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
	}

}
