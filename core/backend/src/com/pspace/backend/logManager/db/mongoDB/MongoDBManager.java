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
package db.mongoDB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.pspace.backend.libs.Data.Lifecycle.LifecycleLogData;
import com.pspace.backend.libs.Data.Replication.ReplicationLogData;
import com.pspace.backend.libs.Data.S3.S3LogData;

import db.DBConfig;
import db.IDBManager;
import db.table.Lifecycle.LifecycleLogQuery;
import db.table.Logging.LoggingQuery;
import db.table.replication.ReplicationLogQuery;

public class MongoDBManager implements IDBManager {
	static final Logger logger = LoggerFactory.getLogger(MongoDBManager.class);

	MongoClient mongo;
	MongoDatabase db;
	DBConfig config;

	public MongoDBManager(DBConfig Config) {
		this.config = Config;
	}

	public void connect() throws Exception {
		var credential = MongoCredential.createCredential(config.User, config.DatabaseName,
				config.Password.toCharArray());
		var serverAddress = new ServerAddress(config.Host, config.Port);
		mongo = MongoClients.create(MongoClientSettings.builder()
						.applyToClusterSettings(builder -> builder.hosts(Arrays.asList(serverAddress)))
						.credential(credential)
						.build());
		db = mongo.getDatabase(config.DatabaseName);
	}

	/***************************** Select **********************************/

	////////////////////// Logging //////////////////////
	public List<S3LogData> getLoggingEventList(String BucketName) {
		var rmap = Select(LoggingQuery.getSelect(BucketName));
		return LoggingQuery.getListMariaDB(rmap);
	}

	/***************************** Insert *****************************/

	public boolean insertLogging(S3LogData data) {
		return Insert(LoggingQuery.DB_TABLE_NAME, LoggingQuery.getInsertDocument(data));
	}

	public boolean insertReplicationLog(ReplicationLogData data) {
		return Insert(ReplicationLogQuery.DB_TABLE_NAME, ReplicationLogQuery.getInsertDocument(data));
	}

	public boolean insertLifecycleLog(LifecycleLogData data) {
		return Insert(LifecycleLogQuery.DB_TABLE_NAME, LifecycleLogQuery.getInsertDocument(data));
	}

	/***************************** Expiration *****************************/
	public boolean Expiration() {
		return true;
	}

	/*********************** Utility ***********************/
	private boolean Insert(String tableName, Document document) {
		try {
			var collection = db.getCollection(tableName);
			collection.insertOne(document);
		} catch (Exception e) {
			logger.error("Error : {}", tableName, e);
		}
		return false;
	}

	private List<HashMap<String, Object>> Select(String tableName) {
		try {
			var rmap = new ArrayList<HashMap<String, Object>>();

			var collection = db.getCollection(tableName);
			var cursor = collection.find().iterator();

			while (cursor.hasNext()) {
				Document item = cursor.next();
				var keys = item.keySet();
				var map = new HashMap<String, Object>(keys.size());
				for (var key : keys)
					map.put(key, item.get(key));
				rmap.add(map);
			}
			cursor.close();
			return rmap;
		} catch (Exception e) {
			logger.error("Query Error : {}", tableName, e);
		}
		return null;
	}

	// private boolean Delete(String Query) {
	// try {
	// stmt.execute();
	// logger.debug(stmt.toString());
	// stmt.close();
	// conn.close();
	// return true;
	// } catch (Exception e) {
	// logger.error("Query Error : {}", Query, e);
	// }
	// return false;
	// }
}