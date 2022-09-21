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
package com.pspace.backend.Main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.Ksan.KsanConfig;
import com.pspace.Ksan.PortalManager;
import com.pspace.Utility.Util;
import com.pspace.backend.Data.LifecycleConstants;
import com.pspace.backend.Heartbeat.Heartbeat;
import com.pspace.backend.Lifecycle.LifecycleFilter;
import com.pspace.backend.Lifecycle.LifecycleSender;
import com.pspace.ifs.ksan.objmanager.ObjManagerConfig;
import com.pspace.ifs.ksan.objmanager.ObjManagerUtil;

public class Main {
	static final Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		logger.info("Lifecycle Manager Start!");

		// Read Configuration
		var KsanConfig = new KsanConfig(LifecycleConstants.AGENT_CONF_PATH);
		if (!KsanConfig.GetConfig()) {
			logger.error("Config Read Failed!");
			return;
		}

		// Get Service Id
		var ServiceId = Util.ReadServiceId(LifecycleConstants.LIFECYCLE_SERVICE_ID_PATH);
		if (ServiceId == null) {
			logger.error("Service Id is Empty");
			return;
		}

		// Heartbeat
		Thread HBThread;
		Heartbeat HB;
		try {
			HB = new Heartbeat(ServiceId, KsanConfig.MQHost, KsanConfig.MQPort, KsanConfig.MQUser,
					KsanConfig.MQPassword);
			HBThread = new Thread(() -> HB.Start(KsanConfig.ServiceMonitorInterval));
			HBThread.start();
		} catch (Exception e) {
			logger.error("", e);
			return;
		}

		// Create PortalManager
		var Portal = new PortalManager(KsanConfig.PortalHost, KsanConfig.PortalPort, KsanConfig.APIKey);
		var today = Util.GetNowDay();
		var AlreadyRun = false;

		while (true) {
			try {
				if (today != Util.GetNowDay())
				{
					today = Util.GetNowDay();
					AlreadyRun = false;
				}

				// Read Configuration to Portal
				var LifecycleConfig = Portal.GetConfig2Lifecycle();
				if (LifecycleConfig == null) {
					logger.error("Lifecycle Config Read Failed!");
					return;
				}
				logger.info(LifecycleConfig.toString());

				// Read Region to Portal
				var Region = Portal.GetRegion(LifecycleConfig.Region);
				if (Region == null) {
					logger.error("Region Read Failed!");
					return;
				}

				// Schedule
				var Schedule = Util.String2Time(LifecycleConfig.Schedule);
				if (Schedule < 0) {
					logger.error("Schedule is not a valid value. {}\n", LifecycleConfig.Schedule);
					return;
				}
				Thread.sleep(LifecycleConfig.CheckInterval);

				if (!Util.isRun(Schedule) || AlreadyRun) {
					continue;
				}
				AlreadyRun = true;

				// Create ObjManager
				ObjManagerUtil ObjManager = null;
				try {
					ObjManagerConfig ObjConfig = new ObjManagerConfig();
					ObjConfig.dbRepository = LifecycleConfig.DBType;
					ObjConfig.dbHost = LifecycleConfig.Host;
					ObjConfig.dbport = LifecycleConfig.Port;
					ObjConfig.dbName = LifecycleConfig.DatabaseName;
					ObjConfig.dbUsername = LifecycleConfig.User;
					ObjConfig.dbPassword = LifecycleConfig.Password;

					logger.info(ObjConfig.toString());

					ObjManager = new ObjManagerUtil(ObjConfig);
				} catch (Exception e) {
					logger.error("", e);
					return;
				}

				logger.info("Lifecycle Filter Start!");
				var Filter = new LifecycleFilter(ObjManager);

				if (Filter.Filtering()) {
					logger.info("Lifecycle Sender Start!");
					var Sender = new LifecycleSender(ObjManager, Region.GetURL(), Region.AccessKey, Region.SecretKey);
					Sender.Start();
				} else
					logger.info("Lifecycle filtering Empty!");

				logger.info("Lifecycle Manager End!");
			} catch (Exception e) {
				logger.error("", e);
			}
		}
	}
}
