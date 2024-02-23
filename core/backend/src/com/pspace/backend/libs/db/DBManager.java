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
package com.pspace.backend.libs.db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.config.DBConfig;
import com.pspace.backend.libs.data.BaseData;
import com.pspace.backend.libs.data.Metering.ApiLogData;
import com.pspace.backend.libs.data.Metering.DateRange;
import com.pspace.backend.libs.data.Metering.ErrorLogData;
import com.pspace.backend.libs.data.Metering.IoLogData;
import com.pspace.backend.libs.data.Metering.UsageLogData;
import com.pspace.backend.libs.data.lifecycle.LifecycleLogData;
import com.pspace.backend.libs.data.lifecycle.RestoreLogData;
import com.pspace.backend.libs.data.replication.ReplicationEventData;
import com.pspace.backend.libs.data.replication.ReplicationFailedData;
import com.pspace.backend.libs.data.replication.ReplicationSuccessData;
import com.pspace.backend.libs.data.s3.S3LogData;
import com.pspace.backend.libs.db.table.lifecycle.LifecycleLogQuery;
import com.pspace.backend.libs.db.table.lifecycle.RestoreLogQuery;
import com.pspace.backend.libs.db.table.logging.BackendLogQuery;
import com.pspace.backend.libs.db.table.logging.S3LogQuery;
import com.pspace.backend.libs.db.table.metering.BackendApiMeteringQuery;
import com.pspace.backend.libs.db.table.metering.BackendErrorMeteringQuery;
import com.pspace.backend.libs.db.table.metering.BackendIoMeteringQuery;
import com.pspace.backend.libs.db.table.metering.BucketApiMeteringQuery;
import com.pspace.backend.libs.db.table.metering.BucketErrorMeteringQuery;
import com.pspace.backend.libs.db.table.metering.BucketIoMeteringQuery;
import com.pspace.backend.libs.db.table.metering.BucketUsageMeteringQuery;
import com.pspace.backend.libs.db.table.replication.ReplicationEventQuery;
import com.pspace.backend.libs.db.table.replication.ReplicationFailedQuery;
import com.pspace.backend.libs.db.table.replication.ReplicationSuccessQuery;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DBManager {
	static final Logger logger = LoggerFactory.getLogger(DBManager.class);

	public static DBManager getInstance() {
		return LazyHolder.INSTANCE;
	}

	private static class LazyHolder {
		private static final DBManager INSTANCE = new DBManager();
	}

	public void init(DBConfig config) {
		this.config = config;
	}

	HikariDataSource ds = null;

	DBConfig config;

	public void connect() {
		var url = String.format(
				"jdbc:mariadb://%s:%d/%s?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8mb4",
				config.host, config.port, config.databaseName);
		var hikariConfig = new HikariConfig();
		hikariConfig.setJdbcUrl(url);
		hikariConfig.setUsername(config.user);
		hikariConfig.setPassword(config.password);
		hikariConfig.setDriverClassName("org.mariadb.jdbc.Driver");
		hikariConfig.setConnectionTestQuery("select 1");
		hikariConfig.addDataSourceProperty("maxPoolSize", config.poolSize);
		hikariConfig.addDataSourceProperty("minPoolSize", config.poolSize);
		hikariConfig.setPoolName("ksan");
		hikariConfig.setMaximumPoolSize(config.poolSize);
		hikariConfig.setMinimumIdle(config.poolSize);
		hikariConfig.setAutoCommit(true);
		hikariConfig.setConnectionTimeout(30000);
		hikariConfig.setMaxLifetime(30000);

		ds = new HikariDataSource(hikariConfig);

		if (createTables())
			logger.info("MariaDB createTables");
		else
			logger.error("MariaDB createTables Fail");
	}

	public boolean check() {
		return createLogTables();
	}

	// region Select
	/////////////////////////// Logging ///////////////////////////
	public List<S3LogData> getLoggingEventList(String bucketName) {
		var map = select(S3LogQuery.select(bucketName));
		return S3LogQuery.getList(map);
	}

	/////////////////////////// Replication ///////////////////////////
	public List<ReplicationEventData> getReplicationEvents(long index) {
		var map = select(ReplicationEventQuery.select(index));
		return ReplicationEventQuery.getList(map);
	}

	/////////////////////////// Metering ///////////////////////////
	public List<ApiLogData> getBucketApiMeteringEvents(DateRange range) {
		return BucketApiMeteringQuery.getMeterList(range, select(BucketApiMeteringQuery.selectMeter(range)));
	}

	public List<IoLogData> getBucketIoMeteringEvents(DateRange range) {
		return BucketIoMeteringQuery.getMeterList(range, select(BucketIoMeteringQuery.selectMeter(range)));
	}

	public List<ErrorLogData> getBucketErrorMeteringEvents(DateRange range) {
		return BucketErrorMeteringQuery.getMeterList(range, select(BucketErrorMeteringQuery.selectMeter(range)));
	}

	public List<ApiLogData> getBackendApiMeteringEvents(DateRange range) {
		return BackendApiMeteringQuery.getMeterList(range, select(BackendApiMeteringQuery.selectMeter(range)));
	}

	public List<IoLogData> getBackendIoMeteringEvents(DateRange range) {
		return BackendIoMeteringQuery.getMeterList(range, select(BackendIoMeteringQuery.selectMeter(range)));
	}

	public List<ErrorLogData> getBackendErrorMeteringEvents(DateRange range) {
		return BackendErrorMeteringQuery.getMeterList(range, select(BackendErrorMeteringQuery.selectMeter(range)));
	}
	// endregion

	// region Insert

	/////////////////////////// Create Table ///////////////////////////
	boolean createTables() {
		// DB Create Table
		return execute(ReplicationSuccessQuery.create()) &&
				execute(ReplicationFailedQuery.create()) &&
				execute(S3LogQuery.create()) &&
				execute(BackendLogQuery.create()) &&
				execute(LifecycleLogQuery.create()) &&
				execute(BucketApiMeteringQuery.createMeter()) &&
				execute(BucketApiMeteringQuery.createAsset()) &&
				execute(BucketIoMeteringQuery.createMeter()) &&
				execute(BucketIoMeteringQuery.createAsset()) &&
				execute(BucketUsageMeteringQuery.createMeter()) &&
				execute(BucketUsageMeteringQuery.createAsset()) &&
				execute(BucketErrorMeteringQuery.createMeter()) &&
				execute(BucketErrorMeteringQuery.createAsset()) &&
				execute(BackendApiMeteringQuery.createMeter()) &&
				execute(BackendApiMeteringQuery.createAsset()) &&
				execute(BackendIoMeteringQuery.createMeter()) &&
				execute(BackendIoMeteringQuery.createAsset()) &&
				execute(BackendErrorMeteringQuery.createMeter()) &&
				execute(BackendErrorMeteringQuery.createAsset());
	}

	boolean createLogTables() {
		// DB Create Table
		return execute(ReplicationSuccessQuery.create()) &&
				execute(ReplicationFailedQuery.create()) &&
				execute(S3LogQuery.create()) &&
				execute(BackendLogQuery.create());
	}

	/////////////////////////// Logging ///////////////////////////
	public boolean insertS3Log(S3LogData data) {
		return insert(S3LogQuery.insert(), data);
	}

	public boolean insertBackendLog(S3LogData data) {
		return insert(BackendLogQuery.insert(), data);
	}

	/////////////////////////// Replication ///////////////////////////
	public boolean insertReplicationEvent(ReplicationEventData data) {
		return insert(ReplicationEventQuery.insert(), data);
	}

	public boolean insertReplicationSuccess(ReplicationSuccessData data) {
		return insert(ReplicationSuccessQuery.insert(), data);
	}

	public boolean insertReplicationSuccess(List<ReplicationSuccessData> events) {
		return insertBulk(ReplicationEventQuery.insert(), events);
	}

	public boolean insertReplicationFailed(ReplicationFailedData data) {
		return insert(ReplicationFailedQuery.insert(), data);
	}

	public boolean insertReplicationFailed(List<ReplicationFailedData> events) {
		return insertBulk(ReplicationSuccessQuery.insert(), events);
	}

	/////////////////////////// Lifecycle ///////////////////////////
	public boolean insertLifecycleLog(LifecycleLogData data) {
		return insert(LifecycleLogQuery.insert(), data);
	}

	public boolean insertRestoreLog(RestoreLogData data) {
		return insert(RestoreLogQuery.getInsert(), data);
	}

	/////////////////////////// Metering ///////////////////////////
	public boolean insertUsageMeter(List<UsageLogData> events) {
		return insertBulk(BucketUsageMeteringQuery.insertMeter(), events);
	}

	public boolean insertUsageAsset(DateRange range) {
		return executeUpdate(BucketUsageMeteringQuery.insertAsset(range));
	}

	public boolean insertBucketApiMeter(List<ApiLogData> events) {
		return insertBulk(BucketApiMeteringQuery.insertMeter(), events);
	}

	public boolean insertBucketApiAsset(DateRange range) {
		return executeUpdate(BucketApiMeteringQuery.insertAsset(range));
	}

	public boolean insertBucketIoMeter(List<IoLogData> events) {
		return insertBulk(BucketIoMeteringQuery.insertMeter(), events);
	}

	public boolean insertBucketIoAsset(DateRange range) {
		return executeUpdate(BucketIoMeteringQuery.insertAsset(range));
	}

	public boolean insertBucketErrorMeter(List<ErrorLogData> events) {
		return insertBulk(BucketErrorMeteringQuery.insertMeter(), events);
	}

	public boolean insertBucketErrorAsset(DateRange range) {
		return executeUpdate(BucketErrorMeteringQuery.insertAsset(range));
	}

	public boolean insertBackendApiMeter(List<ApiLogData> events) {
		return insertBulk(BackendApiMeteringQuery.insertMeter(), events);
	}

	public boolean insertBackendApiAsset(DateRange range) {
		return executeUpdate(BackendApiMeteringQuery.insertAsset(range));
	}

	public boolean insertBackendIoMeter(List<IoLogData> events) {
		return insertBulk(BackendIoMeteringQuery.insertMeter(), events);
	}

	public boolean insertBackendIoAsset(DateRange range) {
		return executeUpdate(BackendIoMeteringQuery.insertAsset(range));
	}

	public boolean insertBackendErrorMeter(List<ErrorLogData> events) {
		return insertBulk(BackendErrorMeteringQuery.insertMeter(), events);
	}

	public boolean insertBackendErrorAsset(DateRange range) {
		return executeUpdate(BackendErrorMeteringQuery.insertAsset(range));
	}
	// endregion

	// region Delete
	/////////////////////////// Replication ///////////////////////////
	public boolean deleteReplicationEvent(long index) {
		return delete(ReplicationEventQuery.delete(index));
	}

	/////////////////////////// Expiration ////////////////////////////
	public boolean expiredMeter() {
		return delete(BucketApiMeteringQuery.expiredMeter()) &&
				delete(BucketIoMeteringQuery.expiredMeter()) &&
				delete(BucketErrorMeteringQuery.expiredMeter()) &&
				delete(BucketUsageMeteringQuery.expiredMeter()) &&
				delete(BackendApiMeteringQuery.expiredMeter()) &&
				delete(BackendIoMeteringQuery.expiredMeter()) &&
				delete(BackendErrorMeteringQuery.expiredMeter());
	}

	// endregion
	/*********************** Utility ***********************/

	/**
	 * 특정 쿼리를 실행한다.
	 * 
	 * @param query
	 *            쿼리
	 * @return 성공/실패 여부
	 */
	boolean execute(String query) {
		try (
				var conn = ds.getConnection();
				var stmt = new LogPreparedStatement(conn, query);) {
			logger.debug("execute : {}", stmt);
			stmt.execute();
			return true;
		} catch (SQLException e) {
			logger.error("execute : {}", query, e);
		}
		return false;
	}

	/**
	 * 특정 쿼리를 실행한다.
	 * 
	 * @param query
	 *            쿼리
	 * @return 성공/실패 여부
	 */
	boolean executeUpdate(String query) {
		try (
				var conn = ds.getConnection();
				var stmt = new LogPreparedStatement(conn, query);) {
			logger.debug("executeUpdate : {}", stmt);
			stmt.executeUpdate();
			return true;
		} catch (SQLException e) {
			logger.error("executeUpdate : {}", query, e);
		}
		return false;
	}

	/**
	 * 삽입 쿼리를 실행한다.
	 * 
	 * @param query
	 *            쿼리
	 * @param params
	 *            파라미터
	 * @return 성공/실패 여부
	 */
	<T extends BaseData> boolean insert(String query, T item) {
		try (
				var conn = ds.getConnection();
				var stmt = new LogPreparedStatement(conn, query);) {
			int index = 1;
			for (Object param : item.getInsertDBParameters())
				stmt.setObject(index++, param);
			logger.debug("insert : {}", stmt);
			return stmt.executeUpdate() > 0;
		} catch (SQLException e) {
			logger.error("insert : {}", query, e);
		}
		return false;
	}

	/**
	 * 대용량 추가 쿼리를 실행한다.
	 * 
	 * @param query
	 *            쿼리
	 * @param items
	 *            벌크 파라미터
	 * @return 성공/실패 여부
	 */
	<T extends BaseData> boolean insertBulk(String query, List<T> items) {
		try (
				var conn = ds.getConnection();
				var stmt = new LogPreparedStatement(conn, query);) {
			for (var item : items) {
				int index = 1;
				for (var object : item.getInsertDBParameters()) {
					stmt.setObject(index++, object);
				}
				stmt.addBatch();
			}
			logger.debug("insertBulk : {}", stmt);

			stmt.executeBatch();
			return true;
		} catch (SQLException e) {
			logger.error("insertBulk : {}", query, e);
		}
		return false;
	}

	/**
	 * 특정 쿼리를 실행한다.
	 * 
	 * @param query
	 *            쿼리
	 * @return 성공/실패 여부
	 */
	List<HashMap<String, Object>> select(String query) {
		try (
				var conn = ds.getConnection();
				var stmt = new LogPreparedStatement(conn, query);
				var rs = stmt.executeQuery();) {
			logger.debug("select : {}", stmt);

			var result = new ArrayList<HashMap<String, Object>>();

			var md = rs.getMetaData();
			int columns = md.getColumnCount();

			while (rs.next()) {

				HashMap<String, Object> map = null;
				map = new HashMap<>(columns);
				for (int i = 1; i <= columns; ++i) {
					map.put(md.getColumnName(i), rs.getObject(i));
				}
				result.add(map);
			}
			return result;

		} catch (SQLException e) {
			logger.error("Query Error : {}", query, e);
		}
		return Collections.emptyList();
	}

	/**
	 * 삭제 쿼리를 실행한다.
	 * 
	 * @param query
	 *            쿼리
	 * @return 성공/실패 여부
	 */
	boolean delete(String query) {
		try (
				var conn = ds.getConnection();
				var stmt = new LogPreparedStatement(conn, query);) {
			stmt.execute();
			logger.debug("delete : {}", stmt);
			return true;
		} catch (SQLException e) {
			logger.error("Query Error : {}", query, e);
		}
		return false;
	}
}