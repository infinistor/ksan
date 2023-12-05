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
package com.pspace.backend.logManager.db.mongoDB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.pspace.backend.libs.Config.DBConfig;
import com.pspace.backend.libs.Data.BaseData;
import com.pspace.backend.libs.Data.Lifecycle.LifecycleLogData;
import com.pspace.backend.libs.Data.Lifecycle.RestoreLogData;
import com.pspace.backend.libs.Data.Metering.ApiLogData;
import com.pspace.backend.libs.Data.Metering.DateRange;
import com.pspace.backend.libs.Data.Metering.ErrorLogData;
import com.pspace.backend.libs.Data.Metering.IoLogData;
import com.pspace.backend.libs.Data.Metering.UsageLogData;
import com.pspace.backend.libs.Data.Replication.ReplicationLogData;
import com.pspace.backend.libs.Data.S3.S3LogData;
import com.pspace.backend.logManager.db.IDBManager;
import com.pspace.backend.logManager.db.table.Lifecycle.LifecycleLogQuery;
import com.pspace.backend.logManager.db.table.Lifecycle.RestoreLogQuery;
import com.pspace.backend.logManager.db.table.Logging.S3LogQuery;
import com.pspace.backend.logManager.db.table.replication.ReplicationFailedQuery;
import com.pspace.backend.logManager.db.table.replication.ReplicationSuccessQuery;

public class MongoDBManager implements IDBManager {
	static final Logger logger = LoggerFactory.getLogger(MongoDBManager.class);

	MongoClient mongo;
	MongoDatabase db;
	DBConfig config;

	public MongoDBManager(DBConfig config) {
		this.config = config;
	}

	public void connect() throws Exception {
		if (!config.host.startsWith("mongodb://")) {
			var credential = MongoCredential.createCredential(config.user, config.databaseName,
					config.password.toCharArray());
			var serverAddress = new ServerAddress(config.host, config.port);
			mongo = MongoClients.create(MongoClientSettings.builder()
					.applyToClusterSettings(builder -> builder.hosts(Arrays.asList(serverAddress)))
					.credential(credential)
					.build());
		} else {
			mongo = MongoClients.create(config.host);

		}
		db = mongo.getDatabase(config.databaseName);
	}

	public boolean check() {
		return true;
	}

	/***************************** Select **********************************/

	////////////////////// Logging //////////////////////
	public List<S3LogData> getLoggingEventList(String BucketName) {
		var map = select(S3LogQuery.select(BucketName));
		return S3LogQuery.getList(map);
	}

	/***************************** Insert *****************************/

	public boolean insertLogging(S3LogData data) {
		return insert(S3LogQuery.getTableName(), data);
	}

	public boolean insertReplicationLog(ReplicationLogData data) {
		if (StringUtils.isBlank(data.message))
			return insert(ReplicationSuccessQuery.DB_TABLE_NAME, data);
		else
			return insert(ReplicationFailedQuery.DB_TABLE_NAME, data);
	}

	public boolean insertLifecycleLog(LifecycleLogData data) {
		return insert(LifecycleLogQuery.DB_TABLE_NAME, data);
	}

	public boolean insertRestoreLog(RestoreLogData data) {
		return insert(RestoreLogQuery.DB_TABLE_NAME, data);
	}

	/***************************** Expiration *****************************/
	public boolean Expiration() {
		return true;
	}

	/*********************** Utility ***********************/
	<T extends BaseData> boolean insert(String tableName, T item) {
		// try {
		// 	var collection = db.getCollection(tableName);
		// 	collection.insertOne(item.getInsertDBDocument());
		// } catch (Exception e) {
		// 	logger.error("Error : {}", tableName, e);
		// }
		return false;
	}

	private List<HashMap<String, Object>> select(String tableName) {
		try {
			var result = new ArrayList<HashMap<String, Object>>();

			var collection = db.getCollection(tableName);
			var cursor = collection.find().iterator();

			while (cursor.hasNext()) {
				Document item = cursor.next();
				var keys = item.keySet();
				var map = new HashMap<String, Object>(keys.size());
				for (var key : keys)
					map.put(key, item.get(key));
				result.add(map);
			}
			cursor.close();
			return result;
		} catch (Exception e) {
			logger.error("Query Error : {}", tableName, e);
		}
		return null;
	}

	@Override
	public List<ApiLogData> getBucketApiMeteringEvents(DateRange range) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getBucketApiMeteringEvents'");
	}

	@Override
	public List<IoLogData> getBucketIoMeteringEvents(DateRange range) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getBucketIoMeteringEvents'");
	}

	@Override
	public List<ErrorLogData> getBucketErrorMeteringEvents(DateRange range) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getBucketErrorMeteringEvents'");
	}

	@Override
	public List<ApiLogData> getBackendApiMeteringEvents(DateRange range) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getBackendApiMeteringEvents'");
	}

	@Override
	public List<IoLogData> getBackendIoMeteringEvents(DateRange range) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getBackendIoMeteringEvents'");
	}

	@Override
	public List<ErrorLogData> getBackendErrorMeteringEvents(DateRange range) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getBackendErrorMeteringEvents'");
	}

	@Override
	public boolean insertBucketApiMeter(List<ApiLogData> events) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'insertBucketApiMeter'");
	}

	@Override
	public boolean insertBucketApiAsset(DateRange range) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'insertBucketApiAsset'");
	}

	@Override
	public boolean insertBucketIoMeter(List<IoLogData> events) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'insertBucketIoMeter'");
	}

	@Override
	public boolean insertBucketIoAsset(DateRange range) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'insertBucketIoAsset'");
	}

	@Override
	public boolean insertBucketErrorMeter(List<ErrorLogData> events) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'insertBucketErrorMeter'");
	}

	@Override
	public boolean insertBucketErrorAsset(DateRange range) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'insertBucketErrorAsset'");
	}

	@Override
	public boolean insertUsageMeter(List<UsageLogData> events) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'insertUsageMeter'");
	}

	@Override
	public boolean insertUsageAsset(DateRange range) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'insertUsageAsset'");
	}

	@Override
	public boolean insertBackendApiMeter(List<ApiLogData> events) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'insertBackendApiMeter'");
	}

	@Override
	public boolean insertBackendApiAsset(DateRange range) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'insertBackendApiAsset'");
	}

	@Override
	public boolean insertBackendIoMeter(List<IoLogData> events) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'insertBackendIoMeter'");
	}

	@Override
	public boolean insertBackendIoAsset(DateRange range) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'insertBackendIoAsset'");
	}

	@Override
	public boolean insertBackendErrorMeter(List<ErrorLogData> events) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'insertBackendErrorMeter'");
	}

	@Override
	public boolean insertBackendErrorAsset(DateRange range) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'insertBackendErrorAsset'");
	}

	@Override
	public boolean expiredMeter() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'expiredMeter'");
	}

}