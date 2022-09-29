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
package db.mariaDB;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.Data.Replication.ReplicationLogData;
import com.pspace.backend.libs.Data.S3.S3LogData;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import db.DBConfig;
import db.IDBManager;
import db.table.Logging.LoggingQuery;
import db.table.replication.ReplicationLogQuery;

public class MariaDBManager implements IDBManager {
	static final Logger logger = LoggerFactory.getLogger(MariaDBManager.class);

	HikariDataSource ds = null;

	DBConfig config;

	public MariaDBManager(DBConfig config) {
		this.config = config;
	}

	public void connect() throws Exception {
		var URL = String.format(
				"jdbc:mariadb://%s:%d/%s?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8mb4",
				config.Host, config.Port, config.DatabaseName);
		var hikariConfig = new HikariConfig();
		hikariConfig.setJdbcUrl(URL);
		hikariConfig.setUsername(config.User);
		hikariConfig.setPassword(config.Password);
		hikariConfig.setDriverClassName("org.mariadb.jdbc.Driver");
		hikariConfig.setConnectionTestQuery("select 1");
		hikariConfig.addDataSourceProperty("maxPoolSize", config.PoolSize);
		hikariConfig.addDataSourceProperty("minPoolSize", config.PoolSize);
		hikariConfig.setPoolName("ksan");
		hikariConfig.setMaximumPoolSize(config.PoolSize);
		hikariConfig.setMinimumIdle(config.PoolSize);
		ds = new HikariDataSource(hikariConfig);

		CreateTables();
	}

	/***************************** Select **********************************/

	////////////////////// Logging //////////////////////
	public List<S3LogData> getLoggingEventList(String BucketName) {
		var rmap = Select(LoggingQuery.getSelect(BucketName));
		return LoggingQuery.getListMariaDB(rmap);
	}

	/***************************** Insert *****************************/

	private boolean CreateTables() {
		// DB Create Table
		if (!Execute(ReplicationLogQuery.getCreateTable()))
			return false;
		if (!Execute(LoggingQuery.getCreateTable()))
			return false;
		return true;
	}

	public boolean InsertLogging(S3LogData data) {
		return Insert(LoggingQuery.getInsert(), LoggingQuery.getInsertParameters(data));
	}

	////////////////////// Replication //////////////////////
	public boolean InsertReplicationLog(ReplicationLogData data) {
		return Insert(ReplicationLogQuery.getInsertQuery(), ReplicationLogQuery.getInsertDBParameters(data));
	}

	public boolean Expiration() {
		if (!Delete(ReplicationLogQuery.getExpiration(config.Expires)))
			return false;
		return true;
	}

	/*********************** Utility ***********************/
	private boolean Execute(String Query) {
		try (
				var conn = ds.getConnection();
				var stmt = new LogPreparedStatement(conn, Query);) {
			logger.debug(stmt.toString());
			stmt.execute();
			return true;
		} catch (SQLException e) {
			logger.error("Query Error : {}", Query, e);
		} catch (Exception e) {
			logger.error("Query Error : {}", Query, e);
		}
		return false;
	}

	private boolean Insert(String Query, List<Object> Params) {
		try (
				var conn = ds.getConnection();
				var stmt = new LogPreparedStatement(conn, Query);) {
			int index = 1;
			for (Object Param : Params)
				stmt.setObject(index++, Param);
			logger.debug(stmt.toString());
			return stmt.executeUpdate() > 0;
		} catch (SQLException e) {
			logger.error("Query Error : {}", Query, e);
		} catch (Exception e) {
			logger.error("Query Error : {}", Query, e);
		}
		return false;
	}

	private List<HashMap<String, Object>> Select(String Query) {
		try (
				var conn = ds.getConnection();
				var stmt = new LogPreparedStatement(conn, Query);
				var rs = stmt.executeQuery();) {
			// logger.debug(stmt.toString());

			var rmap = new ArrayList<HashMap<String, Object>>();

			var md = rs.getMetaData();
			int columns = md.getColumnCount();

			while (rs.next()) {

				HashMap<String, Object> map = null;
				map = new HashMap<String, Object>(columns);
				for (int i = 1; i <= columns; ++i) {
					map.put(md.getColumnName(i), rs.getObject(i));
				}
				rmap.add(map);
			}
			return rmap;

		} catch (SQLException e) {
			logger.error("Query Error : {}", Query, e);
		} catch (Exception e) {
			logger.error("Query Error : {}", Query, e);
		}
		return null;
	}

	private boolean Delete(String Query) {
		try (
				var conn = ds.getConnection();
				var stmt = new LogPreparedStatement(conn, Query);) {
			stmt.execute();
			logger.debug(stmt.toString());
			stmt.close();
			conn.close();
			return true;
		} catch (SQLException e) {
			logger.error("Query Error : {}", Query, e);
		} catch (Exception e) {
			logger.error("Query Error : {}", Query, e);
		}
		return false;
	}
}