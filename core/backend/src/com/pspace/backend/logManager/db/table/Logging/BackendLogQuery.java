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
package com.pspace.backend.logManager.db.table.Logging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.pspace.backend.libs.Data.S3.S3LogData;
import com.pspace.backend.logManager.db.table.QueryConstants;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackendLogQuery {
	static final Logger logger = LoggerFactory.getLogger(BackendLogQuery.class);

	public static final String DB_TABLE_NAME = "S3LOGGINGS";
	public static final String DB_ID = "ID";
	public static final String DB_USER_NAME = "USER_NAME";
	public static final String DB_BUCKET_NAME = "BUCKET_NAME";
	public static final String DB_IN_DATE = "IN_DATE";
	public static final String DB_REMOTE_HOST = "REMOTE_HOST";
	public static final String DB_REQUEST_USER = "REQUEST_USER";
	public static final String DB_REQUEST_ID = "REQUEST_ID";
	public static final String DB_OPERATION = "OPERATION";
	public static final String DB_OBJECT_NAME = "OBJECT_NAME";
	public static final String DB_REQUEST_URI = "REQUEST_URI";
	public static final String DB_STATUS_CODE = "STATUS_CODE";
	public static final String DB_ERROR_CODE = "ERROR_CODE";
	public static final String DB_RESPONSE_LENGTH = "RESPONSE_LENGTH";
	public static final String DB_OBJECT_LENGTH = "OBJECT_LENGTH";
	public static final String DB_TOTAL_TIME = "TOTAL_TIME";
	public static final String DB_REQUEST_LENGTH = "REQUEST_LENGTH";
	public static final String DB_REFERER = "REFERER";
	public static final String DB_USER_AGENT = "USER_AGENT";
	public static final String DB_VERSION_ID = "VERSION_ID";
	public static final String DB_HOST_ID = "HOST_ID";
	public static final String DB_SIGN = "SIGN";
	public static final String DB_SSL_GROUP = "SSL_GROUP";
	public static final String DB_SIGN_TYPE = "SIGN_TYPE";
	public static final String DB_ENDPOINT = "ENDPOINT";
	public static final String DB_TLS_VERSION = "TLS_VERSION";

	public static String create() {
		return "CREATE TABLE IF NOT EXISTS " + getTableName() + " ( " +
				DB_ID + " bigint auto_increment primary key, " +
				DB_USER_NAME + " varchar(64) DEFAULT NULL, " +
				DB_BUCKET_NAME + " varchar(64) DEFAULT NULL, " +
				DB_IN_DATE + " varchar(64) NOT NULL, " +
				DB_REMOTE_HOST + " varchar(256) DEFAULT NULL, " +
				DB_REQUEST_USER + " varchar(64) DEFAULT NULL, " +
				DB_REQUEST_ID + " varchar(64) DEFAULT NULL, " +
				DB_OPERATION + " varchar(64) DEFAULT NULL, " +
				DB_OBJECT_NAME + " varchar(2048) DEFAULT NULL, " +
				DB_REQUEST_URI + " varchar(2048) DEFAULT NULL, " +
				DB_STATUS_CODE + " int DEFAULT NULL, " +
				DB_ERROR_CODE + " varchar(256) DEFAULT NULL, " +
				DB_RESPONSE_LENGTH + " bigint DEFAULT NULL, " +
				DB_OBJECT_LENGTH + " bigint DEFAULT NULL, " +
				DB_TOTAL_TIME + " bigint DEFAULT NULL, " +
				DB_REQUEST_LENGTH + " bigint DEFAULT NULL, " +
				DB_REFERER + " varchar(64) DEFAULT NULL, " +
				DB_USER_AGENT + " varchar(256) DEFAULT NULL, " +
				DB_VERSION_ID + " varchar(64) DEFAULT NULL, " +
				DB_HOST_ID + " varchar(256) DEFAULT NULL, " +
				DB_SIGN + " varchar(32) DEFAULT NULL, " +
				DB_SSL_GROUP + " varchar(64) DEFAULT NULL, " +
				DB_SIGN_TYPE + " varchar(32) DEFAULT NULL, " +
				DB_ENDPOINT + " varchar(64) DEFAULT NULL, " +
				DB_TLS_VERSION + " varchar(32) DEFAULT NULL) " +
				"ENGINE=INNODB DEFAULT CHARSET=utf8mb4;";
	}

	public static String insert() {
		return String.format(
				"INSERT INTO %s(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)"
						+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
				DB_TABLE_NAME,
				DB_USER_NAME, DB_BUCKET_NAME, DB_IN_DATE, DB_REMOTE_HOST, DB_REQUEST_USER, DB_REQUEST_ID,
				DB_OPERATION, DB_OBJECT_NAME, DB_REQUEST_URI, DB_STATUS_CODE, DB_ERROR_CODE, DB_RESPONSE_LENGTH,
				DB_OBJECT_LENGTH, DB_TOTAL_TIME, DB_REQUEST_LENGTH, DB_REFERER, DB_USER_AGENT, DB_VERSION_ID,
				DB_HOST_ID, DB_SIGN, DB_SSL_GROUP, DB_SIGN_TYPE, DB_ENDPOINT, DB_TLS_VERSION);
	}

	public static String select(String BucketName) {
		return String.format("SELECT * FROM %s WHERE %s = '%s';", DB_TABLE_NAME, DB_BUCKET_NAME, BucketName);
	}

	public static String delete(String BucketName, long Index) {
		return String.format("DELETE FROM %s WHERE %s = '%s' and %s <= %d;", DB_TABLE_NAME, DB_BUCKET_NAME, BucketName,
				DB_ID, Index);
	}

	public static List<S3LogData> getList(List<HashMap<String, Object>> resultList) {
		if (resultList == null)
			return null;
		var MyList = new ArrayList<S3LogData>();
		try {
			for (HashMap<String, Object> result : resultList) {
				MyList.add(new S3LogData(
						(String) result.get(DB_USER_NAME),
						(String) result.get(DB_BUCKET_NAME),
						(String) result.get(DB_IN_DATE),
						(String) result.get(DB_REMOTE_HOST),
						(String) result.get(DB_REQUEST_USER),
						(String) result.get(DB_REQUEST_ID),
						(String) result.get(DB_OPERATION),
						(String) result.get(DB_OBJECT_NAME),
						(String) result.get(DB_REQUEST_URI),
						(int) result.get(DB_STATUS_CODE),
						(String) result.get(DB_ERROR_CODE),
						(long) result.get(DB_RESPONSE_LENGTH),
						(long) result.get(DB_OBJECT_LENGTH),
						(long) result.get(DB_TOTAL_TIME),
						(long) result.get(DB_REQUEST_LENGTH),
						(String) result.get(DB_REFERER),
						(String) result.get(DB_USER_AGENT),
						(String) result.get(DB_VERSION_ID),
						(String) result.get(DB_HOST_ID),
						(String) result.get(DB_SIGN),
						(String) result.get(DB_SSL_GROUP),
						(String) result.get(DB_SIGN_TYPE),
						(String) result.get(DB_ENDPOINT),
						(String) result.get(DB_TLS_VERSION)));
			}
		} catch (Exception e) {
			logger.error("", e);
			return null;
		}
		return MyList;
	}

	public static String getTableName() {
		return QueryConstants.getTodayTableName(DB_TABLE_NAME);
	}

	public static List<Object> getInsertParameters(S3LogData data) {
		var param = new ArrayList<Object>();
		param.add(data.userName);
		param.add(data.bucketName);
		param.add(data.date);
		param.add(data.remoteHost);
		param.add(data.requestUser);
		param.add(data.requestId);
		param.add(data.operation);
		param.add(data.objectName);
		param.add(data.requestURI);
		param.add(data.statusCode);
		param.add(data.errorCode);
		param.add(data.responseLength);
		param.add(data.objectLength);
		param.add(data.totalTime);
		param.add(data.requestLength);
		param.add(data.referer);
		param.add(data.userAgent);
		param.add(data.versionId);
		param.add(data.hostId);
		param.add(data.sign);
		param.add(data.sslGroup);
		param.add(data.signType);
		param.add(data.endPoint);
		param.add(data.tlsVersion);

		return param;
	}

	public static Document getInsertDocument(S3LogData data) {
		var param = new Document();
		param.put(DB_USER_NAME, data.userName);
		param.put(DB_BUCKET_NAME, data.bucketName);
		param.put(DB_IN_DATE, data.date);
		param.put(DB_REMOTE_HOST, data.remoteHost);
		param.put(DB_REQUEST_USER, data.requestUser);
		param.put(DB_REQUEST_ID, data.requestId);
		param.put(DB_OPERATION, data.operation);
		param.put(DB_OBJECT_NAME, data.objectName);
		param.put(DB_REQUEST_URI, data.requestURI);
		param.put(DB_STATUS_CODE, data.statusCode);
		param.put(DB_ERROR_CODE, data.errorCode);
		param.put(DB_RESPONSE_LENGTH, data.responseLength);
		param.put(DB_OBJECT_LENGTH, data.objectLength);
		param.put(DB_TOTAL_TIME, data.totalTime);
		param.put(DB_REQUEST_LENGTH, data.requestLength);
		param.put(DB_REFERER, data.referer);
		param.put(DB_USER_AGENT, data.userAgent);
		param.put(DB_VERSION_ID, data.versionId);
		param.put(DB_HOST_ID, data.hostId);
		param.put(DB_SIGN, data.sign);
		param.put(DB_SSL_GROUP, data.sslGroup);
		param.put(DB_SIGN_TYPE, data.signType);
		param.put(DB_ENDPOINT, data.endPoint);
		param.put(DB_TLS_VERSION, data.tlsVersion);

		return param;
	}
}
