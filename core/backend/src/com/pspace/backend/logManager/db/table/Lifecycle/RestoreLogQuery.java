package com.pspace.backend.logManager.db.table.Lifecycle;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.Data.Lifecycle.RestoreLogData;
import com.pspace.backend.logManager.db.table.QueryConstants;

public class RestoreLogQuery {

	static final Logger logger = LoggerFactory.getLogger(RestoreLogQuery.class);

	public static final String DB_TABLE_NAME = "RESTORE_LOG";
	public static final String DB_ID = "ID";
	public static final String DB_IN_DATE = "INDATE";
	public static final String DB_BUCKETNAME = "BUCKET_NAME";
	public static final String DB_OBJECTNAME = "OBJECT_NAME";
	public static final String DB_VERSIONID = "VERSION_ID";
	public static final String DB_MESSAGE = "MESSAGE";

	public static String getCreateTable() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME + " ( " +
				DB_ID + " BIGINT AUTO_INCREMENT PRIMARY KEY, " +
				DB_IN_DATE + " varchar(64) NOT NULL, " +
				DB_BUCKETNAME + " VARCHAR(64) NOT NULL, " +
				DB_OBJECTNAME + " VARCHAR(2048) NOT NULL, " +
				DB_VERSIONID + " VARCHAR(256) NOT NULL, " +
				DB_MESSAGE + " TEXT NULL) " +
				"ENGINE=INNODB DEFAULT CHARSET=utf8mb4;";
	}

	public static String getInsert() {
		return String.format("INSERT INTO %s(%s, %s, %s, %s) VALUES(?, ?, ?, ?)",
				DB_TABLE_NAME, DB_BUCKETNAME, DB_OBJECTNAME, DB_VERSIONID,  DB_MESSAGE);
	}

	public static String getExpiration(int Days) {
		return String.format("delete FROM %s where %s < date_add(date_format(now() , '%s'), interval -%d day);",
				DB_TABLE_NAME, DB_IN_DATE, QueryConstants.DB_DATE_FORMAT, Days);
	}

	public static List<Object> getInsertDBParameters(RestoreLogData data) {
		var param = new ArrayList<Object>();
		param.add(data.BucketName);
		param.add(data.ObjectName);
		param.add(data.VersionId);
		param.add(data.message);

		return param;
	}

	public static Document getInsertDocument(RestoreLogData data) {
		var param = new Document();
		param.put(DB_BUCKETNAME, data.BucketName);
		param.put(DB_OBJECTNAME, data.ObjectName);
		param.put(DB_VERSIONID, data.VersionId);
		param.put(DB_MESSAGE, data.message);

		return param;
	}
}
