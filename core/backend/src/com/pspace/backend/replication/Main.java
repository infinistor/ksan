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
package com.pspace.backend.replication;

import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.Utility;
import com.pspace.backend.libs.Data.Constants;
import com.pspace.backend.libs.Heartbeat.Heartbeat;
import com.pspace.backend.libs.Ksan.AgentConfig;
import com.pspace.backend.libs.Ksan.ObjManagerHelper;
import com.pspace.backend.libs.Ksan.PortalManager;
import com.pspace.backend.replication.Replicator.MainReplicator;

public class Main {
	static final Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("KST"));
		logger.info("Replication Start!");

		// KSAN 설정을 읽어온다.
		var ksanConfig = AgentConfig.getInstance();
		if (!ksanConfig.getConfig()) {
			logger.error("Config Read Failed!");
			return;
		}
		logger.info("ksanAgent Read end");

		// Get Service Id
		var ServiceId = Utility.getServiceId(Constants.REPLICATION_MANAGER_SERVICE_ID_PATH);
		if (ServiceId == null) {
			logger.error("Service Id is Empty. path : {}", Constants.REPLICATION_MANAGER_SERVICE_ID_PATH);
			return;
		}
		// Heartbeat
		Thread HBThread;
		Heartbeat HB;
		try {
			HB = new Heartbeat(ServiceId, ksanConfig.mqHost, ksanConfig.mqPort, ksanConfig.mqUser, ksanConfig.mqPassword);
			HBThread = new Thread(() -> HB.Start(ksanConfig.ServiceMonitorInterval));
			HBThread.start();
		} catch (Exception e) {
			logger.error("", e);
			return;
		}

		// 리전 설정
		var portal = PortalManager.getInstance();
		if (!portal.RegionUpdate()) {
			logger.error("Portal Manager Init Failed!");
			return;
		}
		logger.info("Portal initialized");

		// replication 설정을 읽어온다.
		var config = portal.getReplicationManagerConfig();
		logger.info(config.toString());

		// ObjManager 초기화
		var objManager = ObjManagerHelper.getInstance();
		try {
			objManager.init(portal.getObjManagerConfig());
		} catch (Exception e) {
			logger.error("", e);
			return;
		}

		// Replication Initialization
		var replicator = new MainReplicator();
		if (!replicator.start(config.threadCount)) {
			logger.error("MainReplicator is not started!");
			return;
		}
		logger.info("Replicator Start!");

		while (true) {
			Utility.delay(10000);
		}
	}
}