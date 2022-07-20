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

package com.pspace.metering;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.Portal.PortalConfig;
import com.pspace.Portal.PortalManager;
import com.pspace.metering.db.MariaDB;
import com.pspace.metering.utils.AsyncHandler;

import jnr.constants.platform.RLIMIT;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import jnr.posix.util.Platform;

public final class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	private Main() {
		throw new AssertionError("intentionally not implemented");
	}

	public static void main(String[] args) throws Exception {

		POSIX posix = POSIXFactory.getPOSIX();

		if (Platform.IS_LINUX) {
			posix.setrlimit(RLIMIT.RLIMIT_NOFILE.intValue(), 50000L, 50000L);
		}

		// Read Configuration
		var PortalData = new PortalConfig("/usr/local/ksan/etc/ksanMonitor.conf");
		if (!PortalData.GetConfig()) {
			logger.error("Config Read Failed!");
			return;
		}
		logger.info("{} => {}:{}", PortalData.FileName, PortalData.Ip, PortalData.Port);

		// Read Configuration to Portal
		var Portal = new PortalManager(PortalData.Ip, PortalData.Port, PortalData.Key);
		var Config = Portal.MeteringConfig();
		if (Config == null) {
			logger.error("Metering Config Read Failed!");
			return;
		}
		logger.info(Config.toString());

		MariaDB s3db = new MariaDB();

		// init
		s3db.init(Config.dbHost(), Config.dbPort(), Config.dbS3(), Config.dbUser(), Config.dbPass());
		logger.info("DB Connection Complete");

		ScheduledExecutorService executor1M = Executors.newScheduledThreadPool(1);
		ScheduledExecutorService executor1H = Executors.newScheduledThreadPool(1);

		Runnable runnableMin = () -> {
			AsyncHandler.accountMin(s3db, Config.chkVIP());
		};

		Runnable runnableHour = () -> {
			AsyncHandler.accountHour(s3db, Config.chkVIP());
		};

		executor1M.scheduleAtFixedRate(runnableMin, 0, 1, TimeUnit.MINUTES);
		executor1H.scheduleAtFixedRate(runnableHour, 0, 1, TimeUnit.HOURS);
	}
}