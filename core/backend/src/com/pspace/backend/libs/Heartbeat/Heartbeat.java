/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License. See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.backend.libs.Heartbeat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.Utility;
import com.pspace.backend.libs.Data.Constants;
import com.pspace.ifs.ksan.libs.mq.MQSender;

public class Heartbeat {
	static final Logger logger = LoggerFactory.getLogger(Heartbeat.class);

	private static final String ONLINE = "Online";

	private final String ServiceId;
	private final MQSender Sender;

	public boolean Stop = false;

	public Heartbeat(String ServiceId, String Host, int Port, String User, String Password)
			throws Exception {
		this.ServiceId = ServiceId;
		Sender = new MQSender(Host, Port, User, Password, Constants.MQ_KSAN_SYSTEM_EXCHANGE, "direct", "");
	}

	public void Start(int Delay) {
		var Request = new RequestServiceState(ServiceId, ONLINE);

		while (!Stop) {
			try {
				Utility.delay(Delay);
				Sender.send(Request.toString(), Constants.MQ_HEARTBEAT_BINDING_KEY);
			} catch (Exception e) {
				logger.error("", e);
				
			}
		}
	}
}
