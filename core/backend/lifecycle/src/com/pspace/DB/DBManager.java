/*
* Copyright 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE.md for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.DB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.DB.Table.*;
import com.pspace.backend.Data.*;

public class DBManager {
	static final Logger logger = LoggerFactory.getLogger(DBManager.class);

	private final BucketTable BucketQuery = new BucketTable();
	private final ObjectTable ObjectQuery = new ObjectTable();
	private final MultipartTable MultipartQuery = new MultipartTable();
	private final BaseTable LifecycleQuery = new LifecycleEventTable();
	private final BaseTable LifecycleFailedQuery = new LifecycleFailedTable();

	private static final long DB_SELECT_LIMIT = 1000;

	private final DBConfig Config;
	private final String URL;

	public DBManager(DBConfig Config) {
		this.Config = Config;
		URL = String.format("jdbc:mysql://%s:%d/%s?useSSL=false", Config.Host, Config.Port, Config.DatabaseName);
	}

	/**************************************
	 * Select
	 ***************************************/

	public List<BucketData> GetBucketList() {
		var DataList = new ArrayList<BucketData>();

		boolean loop = true;
		long Index = 0;

		while (loop) {
			var rmap = Select(BucketQuery.GetSelectQuery(Index));
			if (rmap == null)
				break;
			if (rmap.size() == 0)
				break;

			if (rmap.size() < DB_SELECT_LIMIT)
				loop = false;
			else
				Index += 1000;

			DataList.addAll(BucketTable.GetList(rmap));
		}

		return DataList;
	}

	public List<ObjectData> GetObjectList(String BucketName) {
		var DataList = new ArrayList<ObjectData>();

		boolean loop = true;
		long Index = 0;

		while (loop) {
			var rmap = Select(ObjectQuery.GetSelectQuery(BucketName, Index));
			if (rmap == null)
				break;
			if (rmap.size() == 0)
				break;

			if (rmap.size() < DB_SELECT_LIMIT)
				loop = false;
			else
				Index += 1000;

			DataList.addAll(ObjectTable.GetList(BucketName, rmap));
		}

		return DataList;
	}

	public List<MultipartData> GetMultipartList(String BucketName) {
		var DataList = new ArrayList<MultipartData>();

		boolean loop = true;
		long Index = 0;

		while (loop) {
			var rmap = Select(MultipartQuery.GetSelectQuery(BucketName, Index));
			if (rmap == null)
				break;
			if (rmap.size() == 0)
				break;

			if (rmap.size() < DB_SELECT_LIMIT)
				loop = false;
			else
				Index += 1000;

			DataList.addAll(MultipartTable.GetList(BucketName, rmap));
		}

		return DataList;
	}

	public List<LifecycleEventData> GetLifecycleEventList() {
		List<HashMap<String, Object>> rmap = Select(LifecycleQuery.GetSelectQuery(0));
		if (rmap == null)
			return null;
		return LifecycleEventTable.GetList(rmap);
	}

	/**************************************
	 * Insert
	 ***************************************/

	public boolean CreateTables() {
		// DB Create Table
		if (!Execute(URL, LifecycleQuery.GetCreateTableQuery()))
			return false;
		if (!Execute(URL, LifecycleFailedQuery.GetCreateTableQuery()))
			return false;
		return true;
	}

	public boolean InsertLifecycles(List<LifecycleEventData> Lifecycles) {
		boolean result = true;

		for (var Lifecycle : Lifecycles)
			if (!InsertLifecycle(Lifecycle))
				result = false;

		return result;
	}

	public boolean InsertLifecycle(LifecycleEventData Lifecycle) {
		return Insert(URL, LifecycleQuery.GetInsertQuery(), Lifecycle.GetInsertDBParameters());
	}

	public boolean InsertLifecycleFailed(LifecycleFailedData LifecycleFailed) {
		return Insert(URL, LifecycleFailedQuery.GetInsertQuery(), LifecycleFailed.GetInsertDBParameters());
	}

	/**************************************
	 * Clear
	 ****************************************/

	public boolean LifecycleEventsClear() {
		return Delete(URL, LifecycleQuery.GetClearQuery(0));
	}

	/**************************************
	 * Utility
	 **************************************/

	private boolean Execute(String URL, String Query) {
		try (
				Connection conn = DriverManager.getConnection(URL, Config.User, Config.Password);
				PreparedStatement stmt = conn.prepareStatement(Query);) {
			stmt.execute();
			return true;
		} catch (SQLException e) {
			logger.error("Query Error : {}", Query, e);
			return false;
		}
	}

	private boolean Insert(String URL, String Query, List<Object> Params) {
		try (
				Connection conn = DriverManager.getConnection(URL, Config.User, Config.Password);
				LogPreparedStatement stmt = new LogPreparedStatement(conn, Query);) {
			int index = 1;
			for (Object Param : Params)
				stmt.setObject(index++, Param);
			logger.debug(stmt.toString());
			return stmt.executeUpdate() > 0;
		} catch (SQLException e) {
			logger.error("Query Error : {}", Query, e);
			return false;
		}
	}

	private List<HashMap<String, Object>> Select(String Query) {
		try (
				Connection conn = DriverManager.getConnection(URL, Config.User, Config.Password);
				LogPreparedStatement stmt = new LogPreparedStatement(conn, Query);
				ResultSet rs = stmt.executeQuery();) {
			logger.debug(stmt.toString());

			List<HashMap<String, Object>> rmap = null;

			ResultSetMetaData md = rs.getMetaData();
			int columns = md.getColumnCount();

			int init = 0;
			while (rs.next()) {
				if (init == 0) {
					rmap = new ArrayList<HashMap<String, Object>>();
					init++;
				}

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
			return null;
		}
	}

	private boolean Delete(String URL, String Query) {
		try (
				Connection conn = DriverManager.getConnection(URL, Config.User, Config.Password);
				LogPreparedStatement stmt = new LogPreparedStatement(conn, Query);) {
			stmt.execute();
			logger.debug(stmt.toString());
			return true;
		} catch (SQLException e) {
			logger.error("Query Error : {}", Query, e);
			return false;
		}
	}
}