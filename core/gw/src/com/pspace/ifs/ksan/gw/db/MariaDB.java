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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3User;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MariaDB implements GWDB {
	protected Logger logger;
	private String url;
	private String userName;
	private String userPasswd;
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
    public void init(String dbUrl, String dbPort, String dbName, String userName, String passwd) throws Exception {		
		url = GWConstants.JDBC_MYSQL + dbUrl + GWConstants.COLON + dbPort + GWConstants.SLASH + GWConstants.USE_SSL_FALSE;
		this.userName = userName;
		this.userPasswd = passwd;
		
		createDB(dbName, userName, passwd);

		url = GWConstants.JDBC_MYSQL + dbUrl + GWConstants.COLON + dbPort + GWConstants.SLASH + dbName + GWConstants.USE_SSL_FALSE;

		createTable();
		loadUser();
    }
    
	private int createDB(String dbname, String userName, String userPasswd) throws Exception {
        Connection connC = null;
        Statement stmt = null;
        try {    
            Class.forName(GWConstants.MYSQL_JDBC_DRIVER);
            connC= DriverManager.getConnection(url, userName, userPasswd);
            stmt = connC.createStatement();
            stmt.executeUpdate(GWConstants.CREATE_DATABASE + dbname + GWConstants.SEMICOLON);
            return 0;
        } catch (ClassNotFoundException | SQLException ex) {
            logger.error(ex.getMessage());
        }finally{
            try {
                if (stmt != null)
                    stmt.close();
                if (connC != null)
                    connC.close();
            } catch (SQLException ex) {
                logger.error(ex.getMessage());
            }
        }
        return -1;
    }

    private void createTable() throws Exception {
		String query = GWConstants.CREATE_TABLE_USERS;
		execute(query, null);
	}
    
	public List<HashMap<String, Object>> select(String query, List<Object> params) throws GWException {
        List<HashMap<String, Object>> rmap = null; 
        
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rset = null;
        try {
			conn = DriverManager.getConnection(url, userName, userPasswd);
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
			throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR);
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR);
		} finally {
			if ( rset != null ) try { rset.close(); } catch (Exception e) {logger.error(e.getMessage()); throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR);}
			if ( pstmt != null ) try { pstmt.close(); } catch (Exception e) {logger.error(e.getMessage()); throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR);}
			if ( conn != null ) try { conn.close(); } catch (Exception e) {logger.error(e.getMessage()); throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR);}
		}

        return rmap;
    }

	private void execute(String query, List<Object> params) throws GWException {
        try (Connection conn = DriverManager.getConnection(url, userName, userPasswd);
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
			throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR);
		} catch (Exception e) {
			throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR);
		}
    }

    @Override
    public S3User getIdentity(String requestIdentity) throws GWException {
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

		resultList = select(query, params);
		
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

		resultList = select(query, params);
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
	public S3User getIdentityByID(String userId) throws GWException {
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

		resultList = select(query, params);
		
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
	public S3User getIdentityByName(String userName) throws GWException {
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

		resultList = select(query, params);
		
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
}
