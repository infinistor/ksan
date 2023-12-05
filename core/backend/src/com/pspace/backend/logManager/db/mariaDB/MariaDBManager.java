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
package com.pspace.backend.logManager.db.mariaDB;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.pspace.backend.libs.Data.Replication.ReplicationSuccessData;
import com.pspace.backend.libs.Data.S3.S3LogData;
import com.pspace.backend.logManager.db.IDBManager;
import com.pspace.backend.logManager.db.table.Lifecycle.LifecycleLogQuery;
import com.pspace.backend.logManager.db.table.Lifecycle.RestoreLogQuery;
import com.pspace.backend.logManager.db.table.Logging.BackendLogQuery;
import com.pspace.backend.logManager.db.table.Logging.S3LogQuery;
import com.pspace.backend.logManager.db.table.Metering.BackendApiMeteringQuery;
import com.pspace.backend.logManager.db.table.Metering.BucketApiMeteringQuery;
import com.pspace.backend.logManager.db.table.Metering.BackendErrorMeteringQuery;
import com.pspace.backend.logManager.db.table.Metering.BackendIoMeteringQuery;
import com.pspace.backend.logManager.db.table.Metering.BucketErrorMeteringQuery;
import com.pspace.backend.logManager.db.table.Metering.BucketIoMeteringQuery;
import com.pspace.backend.logManager.db.table.Metering.BucketUsageMeteringQuery;
import com.pspace.backend.logManager.db.table.replication.ReplicationFailedQuery;
import com.pspace.backend.logManager.db.table.replication.ReplicationSuccessQuery;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

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
				config.host, config.port, config.databaseName);
		var hikariConfig = new HikariConfig();
		hikariConfig.setJdbcUrl(URL);
		hikariConfig.setUsername(config.user);
		hikariConfig.setPassword(config.password);
		hikariConfig.setDriverClassName("org.mariadb.jdbc.Driver");
		hikariConfig.setConnectionTestQuery("select 1");
		hikariConfig.addDataSourceProperty("maxPoolSize", config.poolSize);
		hikariConfig.addDataSourceProperty("minPoolSize", config.poolSize);
		hikariConfig.setPoolName("ksan");
		hikariConfig.setMaximumPoolSize(config.poolSize);
		hikariConfig.setMinimumIdle(config.poolSize);
		ds = new HikariDataSource(hikariConfig);

		createTables();
	}

	public boolean check() {
		return createLogTables();
	}

	/***************************** Select **********************************/

	////////////////////// Logging //////////////////////
	public List<S3LogData> getLoggingEventList(String BucketName) {
		var map = select(S3LogQuery.select(BucketName));
		return S3LogQuery.getList(map);
	}

	@Override
	public List<ApiLogData> getBucketApiMeteringEvents(DateRange range) {
		return BucketApiMeteringQuery.getMeterList(range, select(BucketApiMeteringQuery.selectMeter(range)));
	}

	@Override
	public List<IoLogData> getBucketIoMeteringEvents(DateRange range) {
		return BucketIoMeteringQuery.getMeterList(range, select(BucketIoMeteringQuery.selectMeter(range)));
	}

	@Override
	public List<ErrorLogData> getBucketErrorMeteringEvents(DateRange range) {
		return BucketErrorMeteringQuery.getMeterList(range, select(BucketErrorMeteringQuery.selectMeter(range)));
	}

	@Override
	public List<ApiLogData> getBackendApiMeteringEvents(DateRange range) {
		return BackendApiMeteringQuery.getMeterList(range, select(BackendApiMeteringQuery.selectMeter(range)));
	}

	@Override
	public List<IoLogData> getBackendIoMeteringEvents(DateRange range) {
		return BackendIoMeteringQuery.getMeterList(range, select(BackendIoMeteringQuery.selectMeter(range)));
	}

	@Override
	public List<ErrorLogData> getBackendErrorMeteringEvents(DateRange range) {
		return BackendErrorMeteringQuery.getMeterList(range, select(BackendErrorMeteringQuery.selectMeter(range)));
	}

	/***************************** Insert *****************************/

	boolean createTables() {
		// DB Create Table
		return execute(ReplicationSuccessQuery.create()) &&
				execute(ReplicationFailedQuery.create()) &&
				execute(S3LogQuery.create()) &&
				execute(BackendLogQuery.create()) &&
				execute(BucketApiMeteringQuery.createMeter()) &&
				execute(BucketApiMeteringQuery.createAsset()) &&
				execute(BucketIoMeteringQuery.createMeter()) &&
				execute(BucketIoMeteringQuery.createAsset()) &&
				execute(BucketUsageMeteringQuery.createMeter()) &&
				execute(BucketUsageMeteringQuery.createAsset()) &&
				execute(BucketErrorMeteringQuery.createMeter()) &&
				execute(BucketErrorMeteringQuery.createAsset()) &&
				execute(BucketApiMeteringQuery.createMeter()) &&
				execute(BucketApiMeteringQuery.createAsset()) &&
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

	@Override
	public boolean insertLogging(S3LogData data) {
		return insert(S3LogQuery.insert(), data);
	}

	@Override
	public boolean insertReplicationLog(ReplicationLogData data) {
		if (data == null)
			return false;
		if (StringUtils.isBlank(data.message))
			return insertReplicationSuccessLog(new ReplicationSuccessData(data));
		else
			return insertReplicationFailedLog(data);
	}

	private boolean insertReplicationSuccessLog(ReplicationSuccessData data) {
		return insert(ReplicationSuccessQuery.insert(), data);
	}

	private boolean insertReplicationFailedLog(ReplicationLogData data) {
		return insert(ReplicationFailedQuery.insert(), data);
	}

	@Override
	public boolean insertLifecycleLog(LifecycleLogData data) {
		return insert(LifecycleLogQuery.getInsert(), data);
	}

	@Override
	public boolean insertRestoreLog(RestoreLogData data) {
		return insert(RestoreLogQuery.getInsert(), data);
	}

	@Override
	public boolean insertUsageMeter(List<UsageLogData> events) {
		return insertBulk(BucketUsageMeteringQuery.insertMeter(), events);
	}

	@Override
	public boolean insertUsageAsset(DateRange range) {
		return executeUpdate(BucketUsageMeteringQuery.insertAsset(range));
	}

	@Override
	public boolean insertBucketApiMeter(List<ApiLogData> events) {
		return insertBulk(BucketApiMeteringQuery.insertMeter(), events);
	}

	@Override
	public boolean insertBucketApiAsset(DateRange range) {
		return executeUpdate(BucketApiMeteringQuery.insertAsset(range));
	}

	@Override
	public boolean insertBucketIoMeter(List<IoLogData> events) {
		return insertBulk(BucketIoMeteringQuery.insertMeter(), events);
	}

	@Override
	public boolean insertBucketIoAsset(DateRange range) {
		return executeUpdate(BucketIoMeteringQuery.insertAsset(range));
	}

	@Override
	public boolean insertBucketErrorMeter(List<ErrorLogData> events) {
		return insertBulk(BucketErrorMeteringQuery.insertMeter(), events);
	}

	@Override
	public boolean insertBucketErrorAsset(DateRange range) {
		return executeUpdate(BucketErrorMeteringQuery.insertAsset(range));
	}

	@Override
	public boolean insertBackendApiMeter(List<ApiLogData> events) {
		return insertBulk(BackendApiMeteringQuery.insertMeter(), events);
	}

	@Override
	public boolean insertBackendApiAsset(DateRange range) {
		return executeUpdate(BackendApiMeteringQuery.insertAsset(range));
	}

	@Override
	public boolean insertBackendIoMeter(List<IoLogData> events) {
		return insertBulk(BackendIoMeteringQuery.insertMeter(), events);
	}

	@Override
	public boolean insertBackendIoAsset(DateRange range) {
		return executeUpdate(BackendIoMeteringQuery.insertAsset(range));
	}

	@Override
	public boolean insertBackendErrorMeter(List<ErrorLogData> events) {
		return insertBulk(BackendErrorMeteringQuery.insertMeter(), events);
	}

	@Override
	public boolean insertBackendErrorAsset(DateRange range) {
		return executeUpdate(BackendErrorMeteringQuery.insertAsset(range));
	}

	/***************************** Expiration *****************************/
	@Override
	public boolean expiredMeter() {
		return delete(BucketApiMeteringQuery.expiredMeter()) &&
				delete(BucketIoMeteringQuery.expiredMeter()) &&
				delete(BucketErrorMeteringQuery.expiredMeter()) &&
				delete(BucketUsageMeteringQuery.expiredMeter()) &&
				delete(BackendApiMeteringQuery.expiredMeter()) &&
				delete(BackendIoMeteringQuery.expiredMeter()) &&
				delete(BackendErrorMeteringQuery.expiredMeter());
	}

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
			logger.debug(stmt.toString());
			stmt.execute();
			return true;
		} catch (SQLException e) {
			logger.error("Query Error : {}", query, e);
		} catch (Exception e) {
			logger.error("Query Error : {}", query, e);
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
			logger.debug(stmt.toString());
			stmt.executeUpdate();
			return true;
		} catch (SQLException e) {
			logger.error("Query Error : {}", query, e);
		} catch (Exception e) {
			logger.error("Query Error : {}", query, e);
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
			logger.debug(stmt.toString());
			return stmt.executeUpdate() > 0;
		} catch (SQLException e) {
			logger.error("Query Error : {}", query, e);
		} catch (Exception e) {
			logger.error("Query Error : {}", query, e);
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
			logger.debug(stmt.toString());

			stmt.executeBatch();
			return true;
		} catch (SQLException e) {
			logger.error("Query Error : {}", query, e);
		} catch (Exception e) {
			logger.error("Query Error : {}", query, e);
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
	List<HashMap<String, Object>> select(String Query) {
		try (
				var conn = ds.getConnection();
				var stmt = new LogPreparedStatement(conn, Query);
				var rs = stmt.executeQuery();) {
			// logger.debug(stmt.toString());

			var result = new ArrayList<HashMap<String, Object>>();

			var md = rs.getMetaData();
			int columns = md.getColumnCount();

			while (rs.next()) {

				HashMap<String, Object> map = null;
				map = new HashMap<String, Object>(columns);
				for (int i = 1; i <= columns; ++i) {
					map.put(md.getColumnName(i), rs.getObject(i));
				}
				result.add(map);
			}
			return result;

		} catch (SQLException e) {
			logger.error("Query Error : {}", Query, e);
		} catch (Exception e) {
			logger.error("Query Error : {}", Query, e);
		}
		return null;
	}

	/**
	 * 삭제 쿼리를 실행한다.
	 * 
	 * @param query
	 *            쿼리
	 * @return 성공/실패 여부
	 */
	boolean delete(String Query) {
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