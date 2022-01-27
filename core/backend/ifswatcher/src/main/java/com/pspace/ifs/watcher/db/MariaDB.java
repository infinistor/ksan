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
package com.pspace.ifs.watcher.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.pspace.ifs.watcher.utils.PrintStack;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDriver;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MariaDB {
    protected final String jdbcDriver = "jdbc:apache:commons:dbcp:cpMetering";
	protected Logger logger;
		
	public MariaDB() {
        logger = LoggerFactory.getLogger(MariaDB.class);
	}

    public static MariaDB getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static final MariaDB INSTANCE = new MariaDB();
    }
	
	public List<HashMap<String, Object>> select(String query, List<Object> params) {
        List<HashMap<String, Object>> rmap = null; 
        
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rset = null;
        try {
			conn = DriverManager.getConnection(jdbcDriver);
			pstmt = conn.prepareStatement(query);

            int index = 1;
			if(params != null) {
            	for(Object p : params) {
                	pstmt.setObject(index, p);
                	index++;
            	}
			}

            logger.debug(pstmt.toString());
			rset = pstmt.executeQuery();

            ResultSetMetaData md = rset.getMetaData();
            int columns = md.getColumnCount();
            int init = 0;
            while (rset.next()) {
                if(init == 0) {
                    rmap = new ArrayList<HashMap<String, Object>>();
                    init++;
                }

                HashMap<String, Object> map = null; 
                map = new HashMap<String, Object>(columns);
                for(int i=1; i<=columns; ++i) {
					map.put(md.getColumnName(i), rset.getObject(i));
				}
                rmap.add(map);
            }
		} catch (SQLException e) {
			PrintStack.logging(logger, e);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
		} finally {
			if ( rset != null ) try { rset.close(); } catch (Exception e) {PrintStack.logging(logger, e);}
			if ( pstmt != null ) try { pstmt.close(); } catch (Exception e) {PrintStack.logging(logger, e);}
			if ( conn != null ) try { conn.close(); } catch (Exception e) {PrintStack.logging(logger, e);}
		}

        return rmap;
    }

	public void execute(String query, List<Object> params) {

        try (Connection conn = DriverManager.getConnection(jdbcDriver);
			 PreparedStatement pstmt = conn.prepareStatement(query);
			) {

            int index = 1;
			if(params != null) {
            	for(Object p : params) {
                	pstmt.setObject(index, p);
                	index++;
            	}
			}

			logger.debug(pstmt.toString());
			pstmt.execute();
		} catch (SQLException e) {
			PrintStack.logging(logger, e);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
		}
    }

	public int executeUpdate(String query, List<Object> params){

        try (Connection conn = DriverManager.getConnection(jdbcDriver);
			 PreparedStatement pstmt = conn.prepareStatement(query);
			) {

            int index = 1;
			if(params != null) {
            	for(Object p : params) {
                	pstmt.setObject(index, p);
                	index++;
            	}
			}

			logger.debug(pstmt.toString());
			return pstmt.executeUpdate();
		} catch (SQLException e) {
			PrintStack.logging(logger, e);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
		}

		return 0;
    }
	
	private void initConnectionPool(String dbUrl, String dbPort, String dbName, String userName, String passwd) throws Exception{
		try {
			Class.forName("org.mariadb.jdbc.Driver");
			String jdbcUrl = "jdbc:mariadb://" + dbUrl + ":" + dbPort + "/"+ dbName + "?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8";
			logger.debug(jdbcUrl);

			ConnectionFactory connFactory = new DriverManagerConnectionFactory(jdbcUrl, userName, passwd);
			PoolableConnectionFactory poolableConnFactory = new PoolableConnectionFactory(connFactory, null);
			poolableConnFactory.setValidationQuery("select 1");
			
			GenericObjectPoolConfig<PoolableConnection> poolConfig = new GenericObjectPoolConfig<PoolableConnection>();
			Duration timeBetweenEvictionRuns = Duration.ofSeconds(60);
			poolConfig.setTimeBetweenEvictionRuns(timeBetweenEvictionRuns);
			poolConfig.setTestWhileIdle(true);
			poolConfig.setMinIdle(4);
			poolConfig.setMaxTotal(16);
			poolConfig.setTestOnBorrow(true);
			poolConfig.setTestWhileIdle(true);
			
			GenericObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(poolableConnFactory, poolConfig);
			poolableConnFactory.setPool(connectionPool);
			Class.forName("org.apache.commons.dbcp2.PoolingDriver");
			PoolingDriver driver = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
			driver.registerPool("cpMetering", connectionPool);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("fail to load JDBC Driver", e);
		} catch (SQLException e) {
			throw new RuntimeException("fail to load JDBC Driver", e);
		}

		initDBTable();
	}

	private void initDBTable() throws Exception {
		String query = "CREATE TABLE IF NOT EXISTS `ioMeter` ("
				+ "`indate` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
				+ "`user` varchar(200) NOT NULL DEFAULT '',"
				+ "`bucket` varchar(200) NOT NULL DEFAULT '',"
				+ "`upload` bigint(20) DEFAULT NULL,"
				+ "`download` bigint(20) DEFAULT NULL,"
				+ "PRIMARY KEY (`indate`,`user`,`bucket`)"
				+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
				
		execute(query, null);

		query = "CREATE TABLE IF NOT EXISTS `usageMeter` ("
				+ "`indate` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
				+ "`user` varchar(200) NOT NULL DEFAULT '',"
				+ "`bucket` varchar(200) NOT NULL DEFAULT '',"
				+ "`used` bigint(20) DEFAULT NULL,"
				+ "`filecount` bigint(20) DEFAULT NULL,"
				+ "PRIMARY KEY (`indate`,`user`,`bucket`)"
				+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
				
		execute(query, null);

		query = "CREATE TABLE IF NOT EXISTS `apiMeter` ("
				+ "`indate` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
				+ "`user` varchar(200) NOT NULL DEFAULT '',"
				+ "`bucket` varchar(200) NOT NULL DEFAULT '',"
				+ "`event` varchar(200) DEFAULT NULL,"
				+ "`count` bigint(20) DEFAULT NULL,"
				+ "PRIMARY KEY (`indate`,`user`,`bucket`, `event`)"
				+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
		
		execute(query, null);

		query = "CREATE TABLE IF NOT EXISTS `apiStat` ("
				+ "`indate` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
				+ "`user` varchar(200) NOT NULL DEFAULT '',"
				+ "`bucket` varchar(200) NOT NULL DEFAULT '',"
				+ "`event` varchar(200) DEFAULT NULL,"
				+ "`count` bigint(20) DEFAULT NULL,"
				+ "PRIMARY KEY (`indate`,`user`,`bucket`, `event`)"
				+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
		
		execute(query, null);

		query = "CREATE TABLE IF NOT EXISTS `ioStat` ("
				+ "`indate` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
				+ "`user` varchar(200) NOT NULL DEFAULT '',"
				+ "`bucket` varchar(200) NOT NULL DEFAULT '',"
				+ "`upload` bigint(20) DEFAULT NULL,"
				+ "`download` bigint(20) DEFAULT NULL,"
				+ "PRIMARY KEY (`indate`,`user`,`bucket`)"
				+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
		
		execute(query, null);

		query = "CREATE TABLE IF NOT EXISTS `usageStat` ("
				+ "`indate` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
				+ "`user` varchar(200) NOT NULL DEFAULT '',"
				+ "`bucket` varchar(200) NOT NULL DEFAULT '',"
				+ "`used` bigint(20) DEFAULT NULL,"
				+ "`filecount` bigint(20) DEFAULT NULL,"
				+ "PRIMARY KEY (`indate`,`user`,`bucket`)"
				+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

		execute(query, null);
	}
    
    public void init(String dbUrl, String dbPort, String dbName, String userName, String passwd) throws Exception {
		initConnectionPool(dbUrl, dbPort, dbName, userName, passwd);
    }
	
	public void bucketUsageMeterMin() {
		String query = "insert into usageMeter(indate, user, bucket, used, filecount) select now(), user, bucket, used, filecount from bucketlist;";
		executeUpdate(query, null);
	}

	public void bucketIOMeterMin() {
		String query = "insert into ioMeter(indate, user, bucket, upload, download) select now(), user_name, bucket_name, sum(request_length), sum(response_length) from s3logging where DATE_SUB(NOW(), INTERVAL 1 MINUTE) < date_time group by user_name, bucket;";
		executeUpdate(query, null);
	}

	public void bucketAPIMeterMin() {
		String query = "insert into apiMeter(indate, user, bucket, event, count) select now(), user_name, bucket_name, operation, count(*) as count from s3logging where DATE_SUB(NOW(), INTERVAL 1 MINUTE) < date_time group by user_name, bucket, operation;";
		executeUpdate(query, null);
	}

	public void bucketAPIMeterHour() {
		String query = "insert into apiStat(indate, user, bucket, event, count) select now(), user, bucket, event, sum(count) from apiMeter group by user, bucket, event";
		executeUpdate(query, null);

		query = "truncate table `apiMeter`";
		executeUpdate(query, null);

		query = "delete from `apiMeter` where indate < DATE_SUB(now(), interval 180 day)";
		executeUpdate(query, null);
	}

	public void bucketIOMeterHour() {
		String query = "insert into ioStat(indate, user, bucket, upload, download) select now(), user, bucket, upload, download from ioMeter group by user, bucket";
		executeUpdate(query, null);

		query = "truncate table `ioStat`";
		executeUpdate(query, null);

		query = "delete from `ioStat` where indate < DATE_SUB(now(), interval 180 day)";
		executeUpdate(query, null);
	}

	public void bucketUsageMeterHour() {
		String query = "insert into usageStat(indate, user, bucket, used, filecount) select now(), user, bucket, used, filecount from bucketlist;";
		executeUpdate(query, null);

		query = "truncate table `usageStat`";
		executeUpdate(query, null);

		query = "delete from `usageStat` where indate < DATE_SUB(now(), interval 180 day)";
		executeUpdate(query, null);
	}
}