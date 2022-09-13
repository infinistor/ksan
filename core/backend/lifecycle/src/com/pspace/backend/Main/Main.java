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
import com.pspace.backend.Lifecycle.LifecycleFilter;
import com.pspace.backend.Lifecycle.LifecycleSender;
import com.pspace.ifs.ksan.objmanager.ObjManagerConfig;
import com.pspace.ifs.ksan.objmanager.ObjManagerUtil;

public class Main {
	static final Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		logger.info("Lifecycle Manager Start!");

		// Read Configuration
		var KsanConfig = new KsanConfig("/usr/local/ksan/etc/ksanAgent.conf");
		if (!KsanConfig.GetConfig()) {
			logger.error("Config Read Failed!");
			return;
		}
		logger.info(KsanConfig.toString());

		// Read Configuration to Portal
		var Portal = new PortalManager(KsanConfig.PortalHost, KsanConfig.PortalPort, KsanConfig.APIKey);
		var LifecycleConfig = Portal.GetConfig2Lifecycle();
		if (LifecycleConfig == null) {
			logger.error("Lifecycle Config Read Failed!");
			return;
		}
		logger.info(LifecycleConfig.toString());

		var Region = Portal.GetRegion(LifecycleConfig.Region);
		if (Region == null) {
			logger.error("Region Read Failed!");
			return;
		}

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
			ObjConfig.mqHost = KsanConfig.MQHost;
			ObjConfig.mqPort = KsanConfig.MQPort;
			ObjConfig.mqUsername = KsanConfig.MQUser;
			ObjConfig.mqPassword = KsanConfig.MQPassword;
			ObjConfig.mqOsdExchangename = "ksan.osdExchange";
			ObjConfig.mqExchangename = "ksan.system";
			ObjConfig.mqQueeuname = "disk";

			logger.info(ObjConfig.toString());

			ObjManager = new ObjManagerUtil(ObjConfig);
		} catch (Exception e) {
			logger.error("", e);
			return;
		}

		// Get Bucket
		logger.info("Lifecycle Filter Start!");
		var Filter = new LifecycleFilter(ObjManager);

		try {
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
		return;
	}
}
