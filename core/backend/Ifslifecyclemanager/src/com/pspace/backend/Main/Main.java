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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.lang.management.ManagementFactory;

import com.pspace.DB.DBManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	static final Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		logger.info("Lifecycle Manager Start!");

		// Save Process Pid
		if (!SavePid("/var/run/ifs-lifecycleManager.pid")) {
			logger.error("Pid Save Failed!");
			return;
		}

		// Read Configuration
		var Config = new MainConfig("/usr/local/ksan/etc/ifs-lifecycleManager.conf");
		if (!Config.GetConfig()) {
			logger.error("Config Read Failed!");
			return;
		}

		// DB Initialization
		var DB = new DBManager(Config.DB);
		if (!DB.CreateTables()) {
			logger.error("DB Tables Create Failed!");
			return;
		}

		// Event Clear
		DB.LifecycleEventsClear();

		// Get Bucket
		var BucketList = DB.GetBucketList();
		if (BucketList.size() > 0) {
			logger.info("Lifecycle Filter Start!");
			var Filter = new LifecycleFilter(DB);

			if (Filter.Filtering()) {
				logger.info("Lifecycle Sender Start!");
				var Sender = new LifecycleSender(DB, Config.S3SourceURL, Config.AccessKey, Config.SecretKey);
				Sender.Start();
			} else
				logger.info("Lifecycle filtering Empty!");
		}

		logger.info("Lifecycle Manager End!");
	}

	public static boolean SavePid(String FilePath) {
		try {
			String temp = ManagementFactory.getRuntimeMXBean().getName();
			int index = temp.indexOf("@");
			String PID = temp.substring(0, index);

			File file = new File(FilePath);
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(PID);
			writer.close();
			return true;
		} catch (Exception e) {
			logger.error("", e);
			return false;
		}
	}
}
