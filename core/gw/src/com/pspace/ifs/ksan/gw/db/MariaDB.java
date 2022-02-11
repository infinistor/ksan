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
package com.pspace.ifs.ksan.gw.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.identity.S3User;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDriver;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MariaDB implements GWDB {
	protected Logger logger;
	private Set<S3User> userSet = new HashSet<S3User>();

	private MariaDB() {
        logger = LoggerFactory.getLogger(MariaDB.class);
	}

    public static MariaDB getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static final MariaDB INSTANCE = new MariaDB();
    }

    @Override
    public void init(String dbUrl, String dbPort, String dbName, String userName, String passwd,  int poolSize) throws GWException {				
		try {
			Class.forName(GWConstants.JDBC_MARIADB_DRIVER);
			String jdbcUrl = GWConstants.MARIADB_URL + dbUrl + GWConstants.COLON + dbPort + GWConstants.SLASH + dbName + GWConstants.MARIADB_OPTIONS;
			ConnectionFactory connFactory = new DriverManagerConnectionFactory(jdbcUrl, userName, passwd);
			PoolableConnectionFactory poolableConnFactory = new PoolableConnectionFactory(connFactory, null);
			poolableConnFactory.setValidationQuery(GWConstants.MARIADB_VALIDATION_QUERY);
			
			GenericObjectPoolConfig<PoolableConnection> poolConfig = new GenericObjectPoolConfig<PoolableConnection>();
			Duration timeBetweenEvictionRuns = Duration.ofMinutes(60);
			poolConfig.setTimeBetweenEvictionRuns(timeBetweenEvictionRuns);
			poolConfig.setTestWhileIdle(true);
			poolConfig.setMinIdle(poolSize / 2);
			poolConfig.setMaxTotal(poolSize);
			poolConfig.setTestOnBorrow(true);
			poolConfig.setTestWhileIdle(true);
			GenericObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(poolableConnFactory, poolConfig);
			poolableConnFactory.setPool(connectionPool);
			Class.forName(GWConstants.DBCP2_DRIVER);
			PoolingDriver driver = (PoolingDriver) DriverManager.getDriver(GWConstants.JDBC_DRIVER_DBCP);
			driver.registerPool(GWConstants.CONNECTION_POOL, connectionPool);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(GWConstants.LOG_MARIA_DB_FAIL_TO_LOAD_DRIVER, e);
		} catch (SQLException e) {
			throw new RuntimeException(GWConstants.LOG_MARIA_DB_FAIL_TO_LOAD_DRIVER, e);
		}
		
		createDB(dbName, userName, passwd);

		createTable();
		loadUser();
    }
    
	private void createDB(String dbname, String userName, String userPasswd) throws GWException {
		String query = GWConstants.CREATE_DATABASE + dbname + GWConstants.SEMICOLON;
		execute(query, null, null);
    }

    private void createTable() throws GWException {
		String query = GWConstants.CREATE_TABLE_USERS;
		execute(query, null, null);
		query = GWConstants.CREATE_TABLE_S3LOGGING;
		execute(query, null, null);
	}
    
	public List<HashMap<String, Object>> select(String query, List<Object> params, S3Parameter s3Parameter) throws GWException {
        List<HashMap<String, Object>> rmap = null; 
        
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rset = null;
        try {
			conn = DriverManager.getConnection(GWConstants.JDBC_DRIVER);
			pstmt = conn.prepareStatement(query);

            int index = 1;
			if(params != null) {
            	for(Object p : params) {
                	pstmt.setObject(index, p);
                	index++;
            	}
			}

            logger.info(pstmt.toString());
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
			logger.error(e.getMessage());
			throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR, s3Parameter);
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR, s3Parameter);
		} finally {
			if ( rset != null ) try { rset.close(); } catch (Exception e) {logger.error(e.getMessage()); throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR, s3Parameter);}
			if ( pstmt != null ) try { pstmt.close(); } catch (Exception e) {logger.error(e.getMessage()); throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR, s3Parameter);}
			if ( conn != null ) try { conn.close(); } catch (Exception e) {logger.error(e.getMessage()); throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR, s3Parameter);}
		}

        return rmap;
    }

	private void execute(String query, List<Object> params, S3Parameter s3Parameter) throws GWException {
        try (Connection conn = DriverManager.getConnection(GWConstants.JDBC_DRIVER);
			 PreparedStatement pstmt = conn.prepareStatement(query);
			) {

            int index = 1;
			if(params != null) {
            	for(Object p : params) {
                	pstmt.setObject(index, p);
                	index++;
            	}
			}

			logger.info(pstmt.toString());
			pstmt.execute();
		} catch (SQLException e) {
			throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR, s3Parameter);
		} catch (Exception e) {
			throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR, s3Parameter);
		}
    }

    @Override
    public S3User getIdentity(String requestIdentity, S3Parameter s3Parameter) throws GWException {
		for (S3User user : userSet) {
			if (user.getAccessKey().equals(requestIdentity)) {
				return user;
			}
		}

		S3User user = null;

		String query = GWConstants.SELECT_USERS_ACCESS_KEY;
		List<HashMap<String, Object>> resultList = null;
		List<Object> params = new ArrayList<Object>();
		params.add(requestIdentity);

		resultList = select(query, params, s3Parameter);
		
		if (resultList != null) {
			logger.info(GWConstants.RESULT, resultList.get(0).get(GWConstants.USERS_TABLE_USER_ID));
			user = new S3User((long)resultList.get(0).get(GWConstants.USERS_TABLE_USER_ID), 
							(String)resultList.get(0).get(GWConstants.USERS_TABLE_USER_NAME), 
							requestIdentity, 
							(String)resultList.get(0).get(GWConstants.USERS_TABLE_ACCESS_SECRET));
			userSet.add(user);
		}

        return user;
    }

	@Override
	public void loadUser() throws GWException {
		String query = GWConstants.SELECT_USERS;
		List<HashMap<String, Object>> resultList = null;
		List<Object> params = new ArrayList<Object>();

		resultList = select(query, params, null);
		if (resultList != null) {
			for (HashMap<String, Object> result : resultList) {
				S3User user = new S3User((long)result.get(GWConstants.USERS_TABLE_USER_ID), 
									(String)result.get(GWConstants.USERS_TABLE_USER_NAME), 
									(String)result.get(GWConstants.USERS_TABLE_ACCESS_KEY), 
									(String)result.get(GWConstants.USERS_TABLE_ACCESS_SECRET));
				userSet.add(user);
			}
		}
	}

	@Override
	public S3User getIdentityByID(String userId, S3Parameter s3Parameter) throws GWException {
		long id = Long.parseLong(userId);
		for (S3User user : userSet) {
			if (user.getUserId() == id) {
				return user;
			}
		}
		S3User user = null;

		String query = GWConstants.SELECT_USERS_USER_ID;
		List<HashMap<String, Object>> resultList = null;
		List<Object> params = new ArrayList<Object>();
		params.add(userId);

		resultList = select(query, params, s3Parameter);
		
		if (resultList != null) {
			logger.info(GWConstants.RESULT, resultList.get(0).get(GWConstants.USERS_TABLE_USER_ID));
			user = new S3User((long)resultList.get(0).get(GWConstants.USERS_TABLE_USER_ID), 
							(String)resultList.get(0).get(GWConstants.USERS_TABLE_USER_NAME), 
							(String)resultList.get(0).get(GWConstants.USERS_TABLE_ACCESS_KEY), 
							(String)resultList.get(0).get(GWConstants.USERS_TABLE_ACCESS_SECRET));
			userSet.add(user);
		}

        return user;
	}

	@Override
	public S3User getIdentityByName(String userName, S3Parameter s3Parameter) throws GWException {
		for (S3User user : userSet) {
			if (user.getUserName().equals(userName)) {
				return user;
			}
		}
		S3User user = null;

		String query = GWConstants.SELECT_USERS_USER_NAME;
		List<HashMap<String, Object>> resultList = null;
		List<Object> params = new ArrayList<Object>();
		params.add(userName);

		resultList = select(query, params, s3Parameter);
		
		if (resultList != null) {
			logger.info(GWConstants.RESULT, resultList.get(0).get(GWConstants.USERS_TABLE_USER_ID));
			user = new S3User((long)resultList.get(0).get(GWConstants.USERS_TABLE_USER_ID), 
							(String)resultList.get(0).get(GWConstants.USERS_TABLE_USER_NAME), 
							(String)resultList.get(0).get(GWConstants.USERS_TABLE_ACCESS_KEY), 
							(String)resultList.get(0).get(GWConstants.USERS_TABLE_ACCESS_SECRET));
			userSet.add(user);
		}

        return user;
	}

	@Override
	public void putS3logging(S3Parameter s3Parameter) throws GWException {
        String query = GWConstants.INSERT_S3LOGGING;

        List<Object> params = new ArrayList<Object>();
        
        // bucket owner name
        if (s3Parameter.getBucket() != null && !Strings.isNullOrEmpty(s3Parameter.getBucket().getUserName())) {
            params.add(s3Parameter.getBucket().getUserName());
        } else {
            params.add(GWConstants.DASH);
        }

        // bucket name
        if (s3Parameter.getBucket() != null && !Strings.isNullOrEmpty(s3Parameter.getBucket().getBucket())) {
            params.add(s3Parameter.getBucket().getBucket());
        } else {
            params.add(GWConstants.DASH);
        }

        // remote host
        if (!Strings.isNullOrEmpty(s3Parameter.getRemoteHost())) {
            params.add(s3Parameter.getRemoteHost());
        } else {
            params.add(GWConstants.DASH);
        }

        // request user
        if (s3Parameter.getUser() != null && !Strings.isNullOrEmpty(s3Parameter.getUser().getUserName())) {
            params.add(s3Parameter.getUser().getUserName());
        } else {
            params.add(GWConstants.DASH);
        }

        // request id
        if (!Strings.isNullOrEmpty(s3Parameter.getRequestID())) {
            params.add(String.valueOf(s3Parameter.getRequestID()));
        } else {
            params.add(GWConstants.DASH);
        }

        // operation
        if (!Strings.isNullOrEmpty(s3Parameter.getOperation())) {
            params.add(s3Parameter.getOperation());
        } else {
            params.add(GWConstants.DASH);
        }

        // object name
        if (!Strings.isNullOrEmpty(s3Parameter.getObjectName())) {
            params.add(s3Parameter.getObjectName());
        } else {
            params.add(GWConstants.DASH);
        }

        // request uri
        if (!Strings.isNullOrEmpty(s3Parameter.getRequestURI())) {
			params.add(s3Parameter.getRequestURI());
        } else {
            params.add(GWConstants.DASH);
        }

        // reponse status code
        params.add(s3Parameter.getStatusCode());

        // response error code
        if (!Strings.isNullOrEmpty(s3Parameter.getErrorCode())) {
			params.add(s3Parameter.getErrorCode());
        } else {
            params.add(GWConstants.DASH);
        }

        // response length
        params.add(s3Parameter.getResponseSize());

		// object length
		if (s3Parameter.getFileSize() > 0) {
			params.add(s3Parameter.getFileSize());
		} else {
			params.add(0L);
		}

        // total time
        params.add(System.currentTimeMillis() - s3Parameter.getStartTime());

        // request length
        params.add(s3Parameter.getRequestSize());

        // referer
        if (!Strings.isNullOrEmpty(s3Parameter.getReferer())) {
			params.add(s3Parameter.getReferer());
        } else {
            params.add(GWConstants.DASH);
        }

        // User Agent
        if (!Strings.isNullOrEmpty(s3Parameter.getUserAgent())) {
			params.add(s3Parameter.getUserAgent());
        } else {
            params.add(GWConstants.DASH);
        }

        // Version id
        if (!Strings.isNullOrEmpty(s3Parameter.getVersionId())) {
			params.add(s3Parameter.getVersionId());
        } else {
            params.add(GWConstants.DASH);
        }

        // Host ID
        if (!Strings.isNullOrEmpty(s3Parameter.getHostID())) {
			params.add(s3Parameter.getHostID());
        } else {
            params.add(GWConstants.DASH);
        }

        // Sign Version
        if (!Strings.isNullOrEmpty(s3Parameter.getSignVersion())) {
			params.add(s3Parameter.getSignVersion());
        } else {
            params.add(GWConstants.DASH);
        }

        // ssl_group
        params.add(GWConstants.DASH);

        // sign type
        if (!Strings.isNullOrEmpty(s3Parameter.getAuthorization())) {
			params.add(GWConstants.AUTH_HEADER);
        } else if (!Strings.isNullOrEmpty(s3Parameter.getxAmzAlgorithm())) {
			params.add(GWConstants.QUERY_STRING);
		} else {
            params.add(GWConstants.DASH);
        }

        // endpoint
        if (!Strings.isNullOrEmpty(s3Parameter.getHostName())) {
			params.add(s3Parameter.getHostName());
        } else {
            params.add(GWConstants.DASH);
        }

        // tls version
        params.add(GWConstants.DASH);

		execute(query, params, s3Parameter);
    }
}
