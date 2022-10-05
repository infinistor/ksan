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
package db.table.Logging;

public class LoggingLogQuery {

	public final String TABLE_NAME = "logging_log";
	public final String ID = "id";
	public final String SOURCE_BUCKET = "source_bucket";
	public final String TARGET_BUCKET = "target_bucket";
	public final String TARGET_KEY = "target_key";
	public final String DATE_TIME = "date_time";
	public final String LAST_LOG_ID = "last_log_id";
	public final String MESSAGE = "message";

	public String getCreateTableQuery() {
		return "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ( " +
				ID + " bigint auto_increment primary key, " +
				SOURCE_BUCKET + " varchar(64) NOT NULL, " +
				TARGET_BUCKET + " varchar(64) NOT NULL, " +
				TARGET_KEY + " varchar(2048) NOT NULL, " +
				DATE_TIME + " timestamp(3) NOT NULL DEFAULT current_timestamp(3), " +
				LAST_LOG_ID + " bigint NOT NULL, " +
				MESSAGE + " TEXT NULL);";
	}

	public String getInsertQuery() {
		return String.format("INSERT INTO %s(%s, %s, %s, %s, %s) VALUES(?, ?, ?, ?, ?);",
				TABLE_NAME, SOURCE_BUCKET, TARGET_BUCKET, TARGET_KEY, LAST_LOG_ID,
				MESSAGE);
	}
}
