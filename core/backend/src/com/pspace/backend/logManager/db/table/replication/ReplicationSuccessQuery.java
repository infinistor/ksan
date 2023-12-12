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
package com.pspace.backend.LogManager.DB.Table.Replication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.LogManager.DB.Table.QueryConstants;

public class ReplicationSuccessQuery implements BaseReplicationQuery {
	static final Logger log = LoggerFactory.getLogger(ReplicationSuccessQuery.class);
	public static final String DB_TABLE_NAME = "REPLICATION_SUCCESS";

	public static String create() {
		return "CREATE TABLE IF NOT EXISTS " + getTableName() + " ( " +
				DB_ID + " BIGINT AUTO_INCREMENT PRIMARY KEY, " +
				DB_IN_DATE + " TIMESTAMP(6) NOT NULL, " +
				DB_EVENT_START_TIME + " TIMESTAMP(6) NOT NULL, " +
				DB_EVENT_END_TIME + " TIMESTAMP(6) NOT NULL, " +
				DB_OPERATION + " VARCHAR(64) NOT NULL, " +
				DB_OBJECTNAME + " VARCHAR(2048) NOT NULL, " +
				DB_VERSION_ID + " VARCHAR(32) NOT NULL, " +
				DB_SOURCE_BUCKET_NAME + " VARCHAR(256) NOT NULL, " +
				DB_TARGET_BUCKET_NAME + " VARCHAR(256) NOT NULL, " +
				DB_TARGET_REGION + " VARCHAR(32) NULL) " +
				"ENGINE=INNODB DEFAULT CHARSET=utf8mb4;";
	}
	static public String insert() {
		return String.format("INSERT INTO %s(%s, %s, %s, %s, %s, %s, %s, %s, %s) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)",
				getTableName(), DB_IN_DATE, DB_EVENT_START_TIME, DB_EVENT_END_TIME, DB_OPERATION, DB_OBJECTNAME,
				DB_VERSION_ID, DB_SOURCE_BUCKET_NAME, DB_TARGET_BUCKET_NAME, DB_TARGET_REGION);
	}

	public static String getTableName() {
		return QueryConstants.getTodayTableName(DB_TABLE_NAME);
	}
}
