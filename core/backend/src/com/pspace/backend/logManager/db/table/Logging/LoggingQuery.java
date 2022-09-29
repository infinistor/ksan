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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.pspace.backend.libs.Data.S3.S3LogData;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingQuery {
	static final Logger logger = LoggerFactory.getLogger(LoggingQuery.class);

	public static final String DB_TABLE_NAME = "s3logging";
	public static final String DB_LOG_ID = "log_id";
	public static final String DB_USER_NAME = "user_name";
	public static final String DB_BUCKET_NAME = "bucket_name";
	public static final String DB_DATE_TIME = "date_time";
	public static final String DB_REMOTE_HOST = "remote_host";
	public static final String DB_REQUEST_USER = "request_user";
	public static final String DB_REQUEST_ID = "request_id";
	public static final String DB_OPERATION = "operation";
	public static final String DB_OBJECT_NAME = "object_name";
	public static final String DB_REQUEST_URI = "request_uri";
	public static final String DB_STATUS_CODE = "status_code";
	public static final String DB_ERROR_CODE = "error_code";
	public static final String DB_RESPONSE_LENGTH = "response_length";
	public static final String DB_OBJECT_LENGTH = "object_length";
	public static final String DB_TOTAL_TIME = "total_time";
	public static final String DB_REQUEST_LENGTH = "request_length";
	public static final String DB_REFERER = "referer";
	public static final String DB_USER_AGENT = "user_agent";
	public static final String DB_VERSION_ID = "version_id";
	public static final String DB_HOST_ID = "host_id";
	public static final String DB_SIGN = "sign";
	public static final String DB_SSL_GROUP = "ssl_group";
	public static final String DB_SIGN_TYPE = "sign_type";
	public static final String DB_ENDPOINT = "endpoint";
	public static final String DB_TLS_VERSION = "tls_version";

	public static String getCreateTable() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME + " ( " +
				DB_LOG_ID + " bigint auto_increment primary key, " +
				DB_USER_NAME + " varchar(64) DEFAULT NULL, " +
				DB_BUCKET_NAME + " varchar(64) DEFAULT NULL, " +
				DB_DATE_TIME + " varchar(64) NOT NULL, " +
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

	public static String getInsert() {
		return String.format(
				"INSERT INTO %s(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)"
						+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
				DB_TABLE_NAME,
				DB_USER_NAME, DB_BUCKET_NAME, DB_DATE_TIME, DB_REMOTE_HOST, DB_REQUEST_USER, DB_REQUEST_ID,
				DB_OPERATION, DB_OBJECT_NAME, DB_REQUEST_URI, DB_STATUS_CODE, DB_ERROR_CODE, DB_RESPONSE_LENGTH,
				DB_OBJECT_LENGTH, DB_TOTAL_TIME, DB_REQUEST_LENGTH, DB_REFERER, DB_USER_AGENT, DB_VERSION_ID,
				DB_HOST_ID, DB_SIGN, DB_SSL_GROUP, DB_SIGN_TYPE, DB_ENDPOINT, DB_TLS_VERSION);
	}

	public static String getSelect(String BucketName) {
		return String.format("SELECT * FROM %s WHERE %s = '%s';", DB_TABLE_NAME, DB_BUCKET_NAME, BucketName);
	}

	public static String getDelete(String BucketName, long Index) {
		return String.format("DELETE FROM %s WHERE %s = '%s' and %s <= %d;", DB_TABLE_NAME, DB_BUCKET_NAME, BucketName,
				DB_LOG_ID, Index);
	}

	public static String getClear(String BucketName) {
		return String.format("DELETE FROM %s WHERE %s = '%s';", DB_TABLE_NAME, DB_BUCKET_NAME, BucketName);
	}

	public static List<S3LogData> getListMariaDB(List<HashMap<String, Object>> resultList) {
		if (resultList == null)
			return null;
		var MyList = new ArrayList<S3LogData>();
		try {
			for (HashMap<String, Object> result : resultList) {
				MyList.add(new S3LogData(
						(String) result.get(DB_USER_NAME),
						(String) result.get(DB_BUCKET_NAME),
						(String) result.get(DB_DATE_TIME),
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

	
	public static List<Object> getInsertParameters(S3LogData data) {
		var param = new ArrayList<Object>();
		param.add(data.UserName);
		param.add(data.BucketName);
		param.add(data.Date);
		param.add(data.RemoteHost);
		param.add(data.RequestUser);
		param.add(data.RequestId);
		param.add(data.Operation);
		param.add(data.ObjectName);
		param.add(data.RequestURI);
		param.add(data.StatusCode);
		param.add(data.ErrorCode);
		param.add(data.ResponseLength);
		param.add(data.ObjectLength);
		param.add(data.TotalTime);
		param.add(data.RequestLength);
		param.add(data.Referer);
		param.add(data.UserAgent);
		param.add(data.VersionId);
		param.add(data.HostId);
		param.add(data.Sign);
		param.add(data.SSLGroup);
		param.add(data.SignType);
		param.add(data.EndPoint);
		param.add(data.TLSVersion);

		return param;
	}
	
	public static Document getInsertDocument(S3LogData data) {
		var param = new Document();
		param.put(DB_USER_NAME, data.UserName);
		param.put(DB_BUCKET_NAME, data.BucketName);
		param.put(DB_DATE_TIME, data.Date);
		param.put(DB_REMOTE_HOST, data.RemoteHost);
		param.put(DB_REQUEST_USER, data.RequestUser);
		param.put(DB_REQUEST_ID, data.RequestId);
		param.put(DB_OPERATION, data.Operation);
		param.put(DB_OBJECT_NAME, data.ObjectName);
		param.put(DB_REQUEST_URI, data.RequestURI);
		param.put(DB_STATUS_CODE, data.StatusCode);
		param.put(DB_ERROR_CODE, data.ErrorCode);
		param.put(DB_RESPONSE_LENGTH, data.ResponseLength);
		param.put(DB_OBJECT_LENGTH, data.ObjectLength);
		param.put(DB_TOTAL_TIME, data.TotalTime);
		param.put(DB_REQUEST_LENGTH, data.RequestLength);
		param.put(DB_REFERER, data.Referer);
		param.put(DB_USER_AGENT, data.UserAgent);
		param.put(DB_VERSION_ID, data.VersionId);
		param.put(DB_HOST_ID, data.HostId);
		param.put(DB_SIGN, data.Sign);
		param.put(DB_SSL_GROUP, data.SSLGroup);
		param.put(DB_SIGN_TYPE, data.SignType);
		param.put(DB_ENDPOINT, data.EndPoint);
		param.put(DB_TLS_VERSION, data.TLSVersion);

		return param;
	}
}
