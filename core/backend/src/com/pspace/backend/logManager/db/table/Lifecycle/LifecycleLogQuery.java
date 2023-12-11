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
package com.pspace.backend.logManager.db.table.Lifecycle;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.Data.Lifecycle.LifecycleLogData;
import com.pspace.backend.logManager.db.table.QueryConstants;

public class LifecycleLogQuery {
	static final Logger logger = LoggerFactory.getLogger(LifecycleLogQuery.class);

	public static final String DB_TABLE_NAME = "LIFECYCLE_LOG";
	public static final String DB_ID = "ID";
	public static final String DB_IN_DATE = "IN_DATE";
	public static final String DB_BUCKET_NAME = "BUCKET_NAME";
	public static final String DB_OBJECTNAME = "OBJECT_NAME";
	public static final String DB_VERSIONID = "VERSION_ID";
	public static final String DB_UPLOAD_ID = "UPLOAD_ID";
	public static final String DB_MESSAGE = "MESSAGE";

	public static String getCreateTable() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME + " ( " +
				DB_ID + " BIGINT AUTO_INCREMENT PRIMARY KEY, " +
				DB_IN_DATE + " varchar(64) NOT NULL, " +
				DB_BUCKET_NAME + " VARCHAR(64) NOT NULL, " +
				DB_OBJECTNAME + " VARCHAR(2048) NOT NULL, " +
				DB_VERSIONID + " VARCHAR(256) NOT NULL, " +
				DB_UPLOAD_ID + " VARCHAR(256) NULL, " +
				DB_MESSAGE + " TEXT NULL) " +
				"ENGINE=INNODB DEFAULT CHARSET=utf8mb4;";
	}

	public static String getInsert() {
		return String.format("INSERT INTO %s(%s, %s, %s, %s, %s) VALUES(?, ?, ?, ?, ?)",
				DB_TABLE_NAME, DB_BUCKET_NAME, DB_OBJECTNAME, DB_VERSIONID, DB_UPLOAD_ID, DB_MESSAGE);
	}

	public static String getExpiration(int Days) {
		return String.format("delete FROM %s where %s < date_add(date_format(now() , '%s'), interval -%d day);",
				DB_TABLE_NAME, DB_IN_DATE, QueryConstants.DB_DATE_FORMAT, Days);
	}

	public static List<Object> getInsertDBParameters(LifecycleLogData data) {
		var param = new ArrayList<Object>();
		param.add(data.bucketName);
		param.add(data.objectName);
		param.add(data.versionId);
		param.add(data.uploadId);
		param.add(data.message);

		return param;
	}

	public static Document getInsertDocument(LifecycleLogData data) {
		var param = new Document();
		param.put(DB_IN_DATE, LocalDateTime.now());
		param.put(DB_BUCKET_NAME, data.bucketName);
		param.put(DB_OBJECTNAME, data.objectName);
		param.put(DB_VERSIONID, data.versionId);
		param.put(DB_UPLOAD_ID, data.uploadId);
		param.put(DB_MESSAGE, data.message);

		return param;
	}
}
