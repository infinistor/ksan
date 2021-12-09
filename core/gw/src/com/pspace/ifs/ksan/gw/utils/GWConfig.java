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
package com.pspace.ifs.ksan.gw.utils;

import static java.util.Objects.requireNonNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;

public class GWConfig {
	private Properties properties;
	private URI endpoint;
	private URI secureEndpoint;
	
	private String authorizationString;
	private String servicePath;
	private String keyStorePath;
	private String keyStorePassword;
	private long MaxFileSize = GWConstants.MAX_FILE_SIZE;
	private long MaxListSize = GWConstants.MAX_LIST_SIZE;
	private boolean ignoreUnknownHeaders;
	private int maxTimeSkew = GWConstants.MAX_TIME_SKEW;
	private int replicationCount = GWConstants.DEFAULT_REPLICATION_VALUE;
	private int osdPort = GWConstants.DEFAULT_OSD_PORT;
	private int osdClientCount = GWConstants.DEFAULT_OSD_CLIENT_SIZE;
	private int objManagerCount = GWConstants.DEFAULT_OBJMANAGER_SIZE;
	private String localIP;
	
	private static String dbRepository;
	private String dbHost;
	private String database;
	private String dbPort;
	private String dbUser;
	private String dbPass;

	public static GWConfig getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static GWConfig INSTANCE = new GWConfig();
    }
	
	private GWConfig() {}
	
	public void configure(String path) throws URISyntaxException, GWException {
		properties = new Properties();
		try (InputStream myis = new FileInputStream(path)) {
			properties.load(myis);
		} catch (FileNotFoundException e) {
			throw new GWException(GWErrorCode.SERVER_ERROR, e.getMessage());
		} catch (IOException e) {
			throw new GWException(GWErrorCode.SERVER_ERROR, e.getMessage());
		}

		String endpoint = properties.getProperty(GWConstants.PROPERTY_ENDPOINT);
		String secureEndpoint = properties.getProperty(GWConstants.PROPERTY_SECURE_ENDPOINT);
		if (endpoint == null && secureEndpoint == null) {
			throw new IllegalArgumentException(
					GWConstants.CONFIG_MUST_CONTAIN +
					GWConstants.PROPERTY_ENDPOINT + GWConstants.OR +
					GWConstants.PROPERTY_SECURE_ENDPOINT);
		}

		if (endpoint != null) {
			this.endpoint = requireNonNull(new URI(endpoint));
		}

		if (secureEndpoint != null) {
			this.secureEndpoint = requireNonNull(new URI(secureEndpoint));
		}

		this.authorizationString = properties.getProperty(GWConstants.PROPERTY_AUTHORIZATION);
		
		if (this.authorizationString == null) {
			throw new IllegalArgumentException(GWConstants.CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_AUTHORIZATION);
		}
		
		String maxTimeSkew = properties.getProperty(GWConstants.PROPERTY_MAXIMUM_TIME_SKEW);
	    if (maxTimeSkew != null) {
	        this.maxTimeSkew = Integer.parseInt(maxTimeSkew);
	    } else {
			throw new IllegalArgumentException(GWConstants.CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_MAXIMUM_TIME_SKEW);
		}

		String replication = properties.getProperty(GWConstants.PROPERTY_REPLICATION);
		if (replication != null) {
			replicationCount = Integer.parseInt(replication);
		} else {
			throw new IllegalArgumentException(GWConstants.CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_REPLICATION);
		}

		String osdPort = properties.getProperty(GWConstants.PROPERTY_OSD_PORT);
		if (osdPort != null) {
			this.osdPort = Integer.parseInt(osdPort);
		} else {
			throw new IllegalArgumentException(GWConstants.CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_OSD_PORT);
		}

		String osdClientCount = properties.getProperty(GWConstants.PROPERTY_OSD_CLIENT_COUNT);
		if (osdClientCount != null) {
			this.osdClientCount = Integer.parseInt(osdClientCount);
		} else {
			throw new IllegalArgumentException(GWConstants.CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_OSD_CLIENT_COUNT);
		}

		String objManagerCount = properties.getProperty(GWConstants.PROPERTY_OBJMANAGER_COUNT);
		if (objManagerCount != null) {
			this.objManagerCount = Integer.parseInt(objManagerCount);
		} else {
			throw new IllegalArgumentException(GWConstants.CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_OBJMANAGER_COUNT);
		}

		localIP = properties.getProperty(GWConstants.PROPERTY_LOCAL_IP);
		if (localIP == null) {
			throw new IllegalArgumentException(GWConstants.CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_LOCAL_IP);
		}
	    
		setDbRepository(properties.getProperty(GWConstants.PROPERTY_DB_REPOSITORY));
		if (getDbRepository() == null || getDbRepository().isEmpty()) {
			throw new IllegalArgumentException(GWConstants.CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_DB_REPOSITORY);
		}

	    this.dbHost = properties.getProperty(GWConstants.PROPERTY_DB_HOST);
	    if (this.dbHost == null) {
	    	throw new IllegalArgumentException(GWConstants.CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_DB_HOST);
	    }
	    
	    this.database = properties.getProperty(GWConstants.PROPERTY_DB_NAME);
	    if (this.database == null) {
	    	throw new IllegalArgumentException(GWConstants.CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_DB_NAME);
	    }
	    
	    this.dbPort = properties.getProperty(GWConstants.PROPERTY_DB_PORT);
	    if (this.dbPort == null) {
	    	throw new IllegalArgumentException(GWConstants.CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_DB_PORT);
	    }
	    
	    this.dbUser = properties.getProperty(GWConstants.PROPERTY_DB_USER);
	    if (this.dbUser == null) {
	    	throw new IllegalArgumentException(GWConstants.CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_DB_USER);
	    }
	    
	    this.dbPass = properties.getProperty(GWConstants.PROPERTY_DB_PASS);
	    if (this.dbPass == null) {
	    	throw new IllegalArgumentException(GWConstants.CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_DB_PASS);
	    }
	}
	
	public URI endpoint() {
		return this.endpoint;
	}
	
	public URI secureEndpoint() {
		return this.secureEndpoint;
	}
	
	public String keyStorePath() {
		return this.keyStorePath;
	}
	
	public String keyStorePassword() {
		return this.keyStorePassword;
	}
	
	public String servicePath() {
		return this.servicePath;
	}
	
	public long MaxFileSize() {
		return this.MaxFileSize;
	}
	
	public long MaxListSize() {
		return this.MaxListSize;
	}
	
	public boolean ignoreUnknownHeaders() {
		return this.ignoreUnknownHeaders;
	}
	
	public int maxTimeSkew() {
		return this.maxTimeSkew;
	}

	public int replicationCount() {
		return this.replicationCount;
	}

	public String localIP() {
		return this.localIP;
	}

	public int osdPort() {
		return osdPort;
	}

	public int osdClientCount() {
		return osdClientCount;
	}

	public int objManagerCount() {
		return objManagerCount;
	}
	
	public static String getDbRepository() {
		return dbRepository;
	}

	public void setDbRepository(String db) {
		GWConfig.dbRepository = db;
	}

	public String dbHost() {
		return this.dbHost;
	}
	
	public String database() {
		return this.database;
	}
	
	public String dbPort() {
		return this.dbPort;
	}
	
	public String dbUser() {
		return this.dbUser;
	}
	
	public String dbPass() {
		return this.dbPass;
	}
}
