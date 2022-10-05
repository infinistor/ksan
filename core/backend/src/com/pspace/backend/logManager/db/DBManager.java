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
package db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.Data.Constants;
import com.pspace.backend.libs.Data.Replication.ReplicationLogData;
import com.pspace.backend.libs.Data.S3.S3LogData;

import db.mariaDB.MariaDBManager;
import db.mongoDB.MongoDBManager;

public class DBManager {
	static final Logger logger = LoggerFactory.getLogger(DBManager.class);
	IDBManager dbManager;
	DBConfig config;

	public static DBManager getInstance() {
		return LazyHolder.INSTANCE;
	}

	private static class LazyHolder {
		private static final DBManager INSTANCE = new DBManager();
	}

	public void init(DBConfig config) {
		this.config = config;
		if (config.Type.equalsIgnoreCase(Constants.DB_TYPE_MARIADB))
			dbManager = new MariaDBManager(config);
		else
			dbManager = new MongoDBManager(config);
	}

	public void connect() throws Exception {
		dbManager.connect();
	}

	public boolean InsertLogging(S3LogData data) {
		return dbManager.InsertLogging(data);
	}

	public boolean InsertReplicationLog(ReplicationLogData data){
		return dbManager.InsertReplicationLog(data);
	}
}