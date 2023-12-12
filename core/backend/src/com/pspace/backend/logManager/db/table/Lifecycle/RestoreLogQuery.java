package com.pspace.backend.LogManager.DB.Table.Lifecycle;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.Libs.Data.Lifecycle.RestoreLogData;
import com.pspace.backend.LogManager.DB.Table.QueryConstants;

public class RestoreLogQuery {

	static final Logger logger = LoggerFactory.getLogger(RestoreLogQuery.class);

	public static final String DB_TABLE_NAME = "RESTORE_LOG";
	public static final String DB_ID = "ID";
	public static final String DB_IN_DATE = "IN_DATE";
	public static final String DB_BUCKET_NAME = "BUCKET_NAME";
	public static final String DB_OBJECT_NAME = "OBJECT_NAME";
	public static final String DB_VERSION_ID = "VERSION_ID";
	public static final String DB_MESSAGE = "MESSAGE";

	public static String getCreateTable() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME + " ( " +
				DB_ID + " BIGINT AUTO_INCREMENT PRIMARY KEY, " +
				DB_IN_DATE + " varchar(64) NOT NULL, " +
				DB_BUCKET_NAME + " VARCHAR(64) NOT NULL, " +
				DB_OBJECT_NAME + " VARCHAR(2048) NOT NULL, " +
				DB_VERSION_ID + " VARCHAR(256) NOT NULL, " +
				DB_MESSAGE + " TEXT NULL) " +
				"ENGINE=INNODB DEFAULT CHARSET=utf8mb4;";
	}

	public static String getInsert() {
		return String.format("INSERT INTO %s(%s, %s, %s, %s) VALUES(?, ?, ?, ?)",
				DB_TABLE_NAME, DB_BUCKET_NAME, DB_OBJECT_NAME, DB_VERSION_ID, DB_MESSAGE);
	}

	public static String getExpiration(int Days) {
		return String.format("delete FROM %s where %s < date_add(date_format(now() , '%s'), interval -%d day);",
				DB_TABLE_NAME, DB_IN_DATE, QueryConstants.DB_DATE_FORMAT, Days);
	}

	public static List<Object> getInsertDBParameters(RestoreLogData data) {
		var param = new ArrayList<Object>();
		param.add(data.bucketName);
		param.add(data.objectName);
		param.add(data.versionId);
		param.add(data.message);

		return param;
	}

	public static Document getInsertDocument(RestoreLogData data) {
		var param = new Document();
		param.put(DB_BUCKET_NAME, data.bucketName);
		param.put(DB_OBJECT_NAME, data.objectName);
		param.put(DB_VERSION_ID, data.versionId);
		param.put(DB_MESSAGE, data.message);

		return param;
	}
}
