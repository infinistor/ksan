
/*
* Copyright 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version
* 3 of the License. See LICENSE.md for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.Utility;
import com.pspace.backend.libs.Data.Constants;
import com.pspace.backend.libs.Heartbeat.Heartbeat;
import com.pspace.backend.libs.Ksan.PortalManager;
import com.pspace.backend.libs.Ksan.Data.AgentConfig;
import com.pspace.ifs.ksan.libs.mq.MQReceiver;

import Lifecycle.LifecycleFilter;
import Lifecycle.LifecycleSender;
import config.ConfigManager;

public class Main {
	static final Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws Exception {
		logger.info("Lifecycle Manager Start!");

		// KSAN 설정을 읽어온다.
		var ksanConfig = AgentConfig.getInstance();
		if (!ksanConfig.getConfig()) {
			logger.error("Config Read Failed!");
			return;
		}
		logger.info("ksanAgent Read end");

		// Get Service Id
		var ServiceId = Utility.ReadServiceId(Constants.LIFECYCLE_SERVICE_ID_PATH);
		if (ServiceId == null) {
			logger.error("Service Id is Empty");
			return;
		}

		// Heartbeat
		Thread HBThread;
		Heartbeat HB;
		try {
			HB = new Heartbeat(ServiceId, ksanConfig.MQHost, ksanConfig.MQPort, ksanConfig.MQUser,
					ksanConfig.MQPassword);
			HBThread = new Thread(() -> HB.Start(ksanConfig.ServiceMonitorInterval));
			HBThread.start();
		} catch (Exception e) {
			logger.error("", e);
			return;
		}

		// 포탈 초기화
		var portal = PortalManager.getInstance();
		if (!portal.RegionInit()) {
			logger.error("Portal Manager Init Failed!");
			return;
		}
		logger.info("Portal initialized");

		// Read Configuration to Portal
		var lifecycleConfig = ConfigManager.getInstance();
		if (lifecycleConfig == null) {
			logger.error("Lifecycle Config Read Failed!");
			return;
		}
		logger.info(lifecycleConfig.toString());

		// Read Region to Portal
		var region = portal.getRegion(lifecycleConfig.getRegion());
		if (region == null) {
			logger.error("Region Read Failed!");
			return;
		}

		// Event Receiver 생성
		var eventCallback = new LifecycleSender(region);
		var eventReceiver = new MQReceiver(
				ksanConfig.MQHost,
				ksanConfig.MQPort,
				ksanConfig.MQUser,
				ksanConfig.MQPassword,
				Constants.MQ_QUEUE_REPLICATION_EVENT_ADD,
				Constants.MQ_KSAN_LOG_EXCHANGE,
				false,
				"",
				Constants.MQ_BINDING_REPLICATION_EVENT,
				eventCallback);

		var today = Utility.GetNowDay();
		var AlreadyRun = false;

		while (true) {
			try {
				if (today != Utility.GetNowDay()) {
					today = Utility.GetNowDay();
					AlreadyRun = false;
				}

				// Schedule
				var Schedule = Utility.String2Time(lifecycleConfig.getSchedule());
				if (Schedule < 0) {
					logger.error("Schedule is not a valid value. {}\n", lifecycleConfig.getSchedule());
					return;
				}
				Thread.sleep(lifecycleConfig.getCheckInterval());

				if (!Utility.isRun(Schedule) || AlreadyRun) {
					continue;
				}
				AlreadyRun = true;

				logger.info("Lifecycle Filter Start!");
				var Filter = new LifecycleFilter();
				Filter.Filtering();
				logger.info("Lifecycle Manager End!");
			} catch (Exception e) {
				logger.error("", e);
			}
		}
	}
}
