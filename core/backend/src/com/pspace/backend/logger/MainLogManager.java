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
package com.pspace.backend.logger;

import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.Utility;
import com.pspace.backend.libs.Ksan.AgentConfig;
import com.pspace.backend.libs.Ksan.ObjManagerHelper;
import com.pspace.backend.libs.Ksan.PortalManager;
import com.pspace.backend.libs.data.Constants;
import com.pspace.backend.libs.db.DBManager;
import com.pspace.backend.libs.heartbeat.Heartbeat;
import com.pspace.backend.logger.logging.MainLogger;
import com.pspace.backend.logger.metering.MainMetering;

public class MainLogManager {
	static final Logger logger = LoggerFactory.getLogger(MainLogManager.class);

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("KST"));
		logger.info("logManager Start!");

		// KSAN 설정을 읽어온다.
		var ksanConfig = AgentConfig.getInstance();
		if (!ksanConfig.getConfig()) {
			logger.error("Config Read Failed!");
			return;
		}
		logger.info("ksanAgent Read end");

		// Get Service Id
		var serviceId = Utility.getServiceId(Constants.LOG_MANAGER_SERVICE_ID_PATH);
		if (serviceId == null) {
			logger.error("Service Id is Empty. path : {}", Constants.LOG_MANAGER_SERVICE_ID_PATH);
			return;
		}

		// Heartbeat
		Thread heartbeatThread;
		Heartbeat heartbeat;
		try {
			heartbeat = new Heartbeat(serviceId, ksanConfig.mqHost, ksanConfig.mqPort, ksanConfig.mqUser, ksanConfig.mqPassword);
			heartbeatThread = new Thread(() -> heartbeat.start(ksanConfig.ServiceMonitorInterval));
			heartbeatThread.start();
		} catch (Exception e) {
			logger.error("", e);
			return;
		}

		// 포탈 초기화
		var portal = PortalManager.getInstance();
		logger.info("Portal initialized");

		// Read Configuration to Portal
		var config = portal.getLogManagerConfig();
		logger.info("Log Manager Config : {}", config);

		// DB 초기화
		var db = DBManager.getInstance();
		try {
			db.init(config.getDBConfig());
			db.connect();
		} catch (Exception e) {
			logger.error("db connect error : ", e);
			return;
		}
		logger.info("DB initialized");

		// ObjManager 초기화
		var objManager = ObjManagerHelper.getInstance();
		var objManagerConfig = portal.getObjManagerConfig();
		try {
			objManager.init(objManagerConfig);
		} catch (Exception e) {
			logger.error("objManager init error : ", e);
			return;
		}
		logger.info("ObjManager initialized");

		// Logger 초기화
		var logging = new MainLogger();
		if (!logging.start(config.threadCount)) {
			logger.error("MainLogger is not started!");
			return;
		}
		logger.info("Logger Start!");

		// Metering 초기화
		var metering = new MainMetering(config.getMeterConfig());
		if (!metering.start()) {
			logger.error("MainMetering is not started!");
			return;
		}
		logger.info("Metering Start!");

		while (true) {
			db.check();
			Utility.delay(10000);
		}
	}
}