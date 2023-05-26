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
package com.pspace.backend.lifecycle;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.Utility;
import com.pspace.backend.libs.Data.Constants;
import com.pspace.backend.libs.Heartbeat.Heartbeat;
import com.pspace.backend.libs.Ksan.AgentConfig;
import com.pspace.backend.libs.Ksan.ObjManagerHelper;
import com.pspace.backend.libs.Ksan.PortalManager;
import com.pspace.backend.lifecycle.Lifecycle.LifecycleFilter;
import com.pspace.backend.lifecycle.Lifecycle.LifecycleSender;
import com.pspace.backend.lifecycle.Lifecycle.RestoreSender;
import com.pspace.ifs.ksan.libs.mq.MQReceiver;

public class Main {
	static final Logger logger = LoggerFactory.getLogger(Main.class);

	public static List<MQReceiver> sendReceivers = new ArrayList<MQReceiver>();
	public static List<MQReceiver> restoreReceivers = new ArrayList<MQReceiver>();

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
		var serviceId = Utility.ReadServiceId(Constants.LIFECYCLE_MANAGER_SERVICE_ID_PATH);
		if (serviceId == null) {
			logger.error("Service Id is Empty");
			return;
		}

		// Heartbeat
		Thread hbThread;
		Heartbeat hb;
		try {
			hb = new Heartbeat(serviceId, ksanConfig.MQHost, ksanConfig.MQPort, ksanConfig.MQUser,
					ksanConfig.MQPassword);
			hbThread = new Thread(() -> hb.Start(ksanConfig.ServiceMonitorInterval));
			hbThread.start();
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
		var config = portal.getLifecycleManagerConfig();
		logger.info(config.toString());

		// Read Region to Portal
		var region = portal.getLocalRegion();
		if (region == null) {
			logger.error("Region Read Failed!");
			return;
		}

		// objManager 초기화
		var objManager = ObjManagerHelper.getInstance();
		var objManagerConf = portal.getObjManagerConfig();
		try {
			objManager.init(objManagerConf);
		} catch (Exception e) {
			logger.error(objManagerConf.toString(), e);
			return;
		}

		// Event Receiver 생성
		for (int index = 0; index < config.threadCount; index++) {
			sendReceivers.add(new MQReceiver(
					ksanConfig.MQHost,
					ksanConfig.MQPort,
					ksanConfig.MQUser,
					ksanConfig.MQPassword,
					Constants.MQ_QUEUE_LIFECYCLE_EVENT_ADD,
					Constants.MQ_KSAN_LOG_EXCHANGE,
					false,
					"",
					Constants.MQ_BINDING_LIFECYCLE_EVENT,
					new LifecycleSender(region)));

			restoreReceivers.add(new MQReceiver(
					ksanConfig.MQHost,
					ksanConfig.MQPort,
					ksanConfig.MQUser,
					ksanConfig.MQPassword,
					Constants.MQ_QUEUE_RESTORE_EVENT_ADD,
					Constants.MQ_KSAN_LOG_EXCHANGE,
					false,
					"",
					Constants.MQ_BINDING_RESTORE_EVENT,
					new RestoreSender(region)));
		}

		var today = Utility.GetNowDay();
		var AlreadyRun = true;

		var Filter = new LifecycleFilter();
		try {
			logger.info("Lifecycle Filter Start!");
			Filter.Filtering();
			logger.info("Lifecycle Filter End!");
		} catch (Exception e) {
			logger.error("", e);
			return;
		}

		while (true) {
			try {
				if (today != Utility.GetNowDay()) {
					today = Utility.GetNowDay();
					AlreadyRun = false;
				}

				// Schedule
				var Schedule = Utility.String2Time(config.schedule);
				if (Schedule < 0) {
					logger.error("Schedule is not a valid value. {}\n", config.schedule);
					return;
				}
				Thread.sleep(config.checkInterval);

				if (!Utility.isRun(Schedule) || AlreadyRun) {
					continue;
				}
				AlreadyRun = true;

				logger.info("Lifecycle Filter Start!");
				Filter.Filtering();
				logger.info("Lifecycle Manager End!");
			} catch (Exception e) {
				logger.error("", e);
			}
		}
	}
}
