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
