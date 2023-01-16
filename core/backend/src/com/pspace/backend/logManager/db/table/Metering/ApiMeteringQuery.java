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
package com.pspace.backend.logManager.db.table.Metering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.logManager.db.table.Logging.LoggingQuery;

public class ApiMeteringQuery {
	static final Logger log = LoggerFactory.getLogger(ApiMeteringQuery.class);

	static final String DB_TABLE_NAME_METER = "BUCKET_API_METERS";
	static final String DB_TABLE_NAME_ASSET = "BUCKET_API_ASSETS";
	static final String DB_INDATE = "INDATE";
	static final String DB_USER = "USER_NAME";
	static final String DB_BUCKET = "BUCKET_NAME";
	static final String DB_EVENT = "OPERATION";
	static final String DB_COUNT = "COUNT";

	public static String createMeter() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_METER + " ( " +
				DB_INDATE + " datetime NOT NULL, " +
				DB_USER + " varchar(64) NOT NULL, " +
				DB_BUCKET + " varchar(64) NOT NULL, " +
				DB_EVENT + " varchar(200) NOT NULL, " +
				DB_COUNT + " bigint DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_INDATE + ", " + DB_USER + ", " + DB_BUCKET + ", " + DB_EVENT + "));";
	}

	public static String createAsset() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_ASSET + " ( " +
				DB_INDATE + " datetime NOT NULL, " +
				DB_USER + " varchar(64) NOT NULL, " +
				DB_BUCKET + " varchar(64) NOT NULL, " +
				DB_EVENT + " varchar(200) NOT NULL, " +
				DB_COUNT + " bigint DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_INDATE + ", " + DB_USER + ", " + DB_BUCKET + ", " + DB_EVENT + "));";
	}

	public static String insertMeter(int times) {
		String.format("insert into %s (%s, %s, %s, %s, %s)", DB_TABLE_NAME_METER, DB_INDATE, DB_USER, DB_BUCKET, DB_EVENT, DB_COUNT);
		
		return "insert into " + DB_TABLE_NAME_METER + "("+ DB_INDATE + ", " + DB_USER + ", " + DB_BUCKET + ", " + DB_EVENT + ", " + DB_COUNT + ")"
				+ "select now(), user_name, bucket_name, operation, count(*) as count "
				+ "from (select * from " + LoggingQuery.DB_TABLE_NAME
				+ " where DATE_SUB(NOW(), INTERVAL "+ times +" MINUTE) < date_time) as s3logging, "
				// + S3BucketQuery.DB_TABLE_NAME
				+ " where s3logging.bucket_name = bucketlist.bucket group by user_name, bucket_name, operation;";
	}

	public static String insertAsset() {
		return "insert into " + DB_TABLE_NAME_ASSET + "(INDATE, USER, BUCKET, EVENT, COUNT)"
				+ " select now(), user, bucket, event, sum(count) from " + DB_TABLE_NAME_METER
				+ " group by user, bucket, event;";
	}

	public static String meterClear() {
		return String.format("TRUNCATE %s;", DB_TABLE_NAME_METER);
	}
}