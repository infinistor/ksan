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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.utils.DISKPOOLLIST.DISKPOOL.SERVER;
import com.pspace.ifs.ksan.gw.utils.DISKPOOLLIST.DISKPOOL.SERVER.DISK;
import com.pspace.ifs.ksan.osd.OSDConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GWConfig {
	private Properties properties;
	private URI endpoint;
	private URI secureEndpoint;
	
	private String authorizationString;
	private String servicePath;
	private String keyStorePath;
	private String keyStorePassword;
	private long maxFileSize = GWConstants.MAX_FILE_SIZE;
	private long maxListSize = GWConstants.MAX_LIST_SIZE;
	private boolean ignoreUnknownHeaders;
	private int maxTimeSkew = GWConstants.MAX_TIME_SKEW;
	private int replicationCount = GWConstants.DEFAULT_REPLICATION_VALUE;
	private int osdPort = GWConstants.DEFAULT_OSD_PORT;
	private int osdClientCount = GWConstants.DEFAULT_OSD_CLIENT_SIZE;
	private int objManagerCount = GWConstants.DEFAULT_OBJMANAGER_SIZE;
	
	private static String dbRepository;
	private String dbHost;
	private String database;
	private String dbPort;
	private String dbUser;
	private String dbPass;
	private int dbPoolSize;

	private String cacheDisk;
	private long cacheFileSize;

	private String performanceMode;

	private static final Logger logger = LoggerFactory.getLogger(GWConfig.class);

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
			throw new IllegalArgumentException(GWConstants.LOG_CONFIG_NOT_EXIST);
		} catch (IOException e) {
			throw new IllegalArgumentException(GWConstants.LOG_CONFIG_FAILED_LOADING);
		}

		String endpoint = properties.getProperty(GWConstants.PROPERTY_ENDPOINT);
		String secureEndpoint = properties.getProperty(GWConstants.PROPERTY_SECURE_ENDPOINT);
		if (endpoint == null && secureEndpoint == null) {
			throw new IllegalArgumentException(
					GWConstants.LOG_CONFIG_MUST_CONTAIN +
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
			throw new IllegalArgumentException(GWConstants.LOG_CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_AUTHORIZATION);
		}
		
		String maxTimeSkew = properties.getProperty(GWConstants.PROPERTY_MAXIMUM_TIME_SKEW);
	    if (maxTimeSkew != null) {
	        this.maxTimeSkew = Integer.parseInt(maxTimeSkew);
	    } else {
			throw new IllegalArgumentException(GWConstants.LOG_CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_MAXIMUM_TIME_SKEW);
		}

		String replication = properties.getProperty(GWConstants.PROPERTY_REPLICATION);
		if (replication != null) {
			replicationCount = Integer.parseInt(replication);
		} else {
			throw new IllegalArgumentException(GWConstants.LOG_CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_REPLICATION);
		}

		String osdPort = properties.getProperty(GWConstants.PROPERTY_OSD_PORT);
		if (osdPort != null) {
			this.osdPort = Integer.parseInt(osdPort);
		} else {
			throw new IllegalArgumentException(GWConstants.LOG_CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_OSD_PORT);
		}

		String osdClientCount = properties.getProperty(GWConstants.PROPERTY_OSD_CLIENT_COUNT);
		if (osdClientCount != null) {
			this.osdClientCount = Integer.parseInt(osdClientCount);
		} else {
			throw new IllegalArgumentException(GWConstants.LOG_CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_OSD_CLIENT_COUNT);
		}

		String objManagerCount = properties.getProperty(GWConstants.PROPERTY_OBJMANAGER_COUNT);
		if (objManagerCount != null) {
			this.objManagerCount = Integer.parseInt(objManagerCount);
		} else {
			throw new IllegalArgumentException(GWConstants.LOG_CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_OBJMANAGER_COUNT);
		}
	    
		setDbRepository(properties.getProperty(GWConstants.PROPERTY_DB_REPOSITORY));
		if (getDbRepository() == null || getDbRepository().isEmpty()) {
			throw new IllegalArgumentException(GWConstants.LOG_CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_DB_REPOSITORY);
		}

	    this.dbHost = properties.getProperty(GWConstants.PROPERTY_DB_HOST);
	    if (this.dbHost == null) {
	    	throw new IllegalArgumentException(GWConstants.LOG_CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_DB_HOST);
	    }
	    
	    this.database = properties.getProperty(GWConstants.PROPERTY_DB_NAME);
	    if (this.database == null) {
	    	throw new IllegalArgumentException(GWConstants.LOG_CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_DB_NAME);
	    }
	    
	    this.dbPort = properties.getProperty(GWConstants.PROPERTY_DB_PORT);
	    if (this.dbPort == null) {
	    	throw new IllegalArgumentException(GWConstants.LOG_CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_DB_PORT);
	    }
	    
	    this.dbUser = properties.getProperty(GWConstants.PROPERTY_DB_USER);
	    if (this.dbUser == null) {
	    	throw new IllegalArgumentException(GWConstants.LOG_CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_DB_USER);
	    }
	    
	    this.dbPass = properties.getProperty(GWConstants.PROPERTY_DB_PASS);
	    if (this.dbPass == null) {
	    	throw new IllegalArgumentException(GWConstants.LOG_CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_DB_PASS);
	    }

		String dbPoolSize = properties.getProperty(GWConstants.PROPERTY_DB_POOL_SIZE);
		if (dbPoolSize != null) {
			this.dbPoolSize = Integer.parseInt(dbPoolSize);
		} else {
			throw new IllegalArgumentException(GWConstants.LOG_CONFIG_MUST_CONTAIN + GWConstants.PROPERTY_DB_POOL_SIZE);
		}

		this.cacheDisk = properties.getProperty(GWConstants.PROPERTY_CACHE_DISK);
		if (this.cacheDisk != null) {
			try {
				logger.debug(GWConstants.LOG_OSDCLIENT_MANAGER_DISKPOOLS_CONFIGURE);
				XmlMapper xmlMapper = new XmlMapper();
				InputStream is = new FileInputStream(OSDConstants.DISKPOOL_CONF_PATH);
				byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
				try {
					is.read(buffer, 0, OSDConstants.MAXBUFSIZE);
					is.close();
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
				String xml = new String(buffer);
				
				DISKPOOLLIST diskpoolList = xmlMapper.readValue(xml, DISKPOOLLIST.class);
				for (SERVER server : diskpoolList.getDiskpool().getServers()) {
					if (GWUtils.getLocalIP().equals(server.getIp())) {
						for (DISK disk : server.getDisks()) {
							File file = new File(cacheDisk + disk.getPath() + GWConstants.SLASH + GWConstants.OBJ_DIR);
							file.mkdirs();
							file = new File(cacheDisk + disk.getPath() + GWConstants.SLASH + GWConstants.TEMP_DIR);
							file.mkdirs();
							file = new File(cacheDisk + disk.getPath() + GWConstants.SLASH + GWConstants.TRASH_DIR);
							file.mkdirs();
							file = new File(disk.getPath() + GWConstants.SLASH + GWConstants.OBJ_DIR);
							file.mkdirs();
							file = new File(disk.getPath() + GWConstants.SLASH + GWConstants.TEMP_DIR);
							file.mkdirs();
							file = new File(disk.getPath() + GWConstants.SLASH + GWConstants.TRASH_DIR);
							file.mkdirs();
						}
					}
				}
			} catch (JsonProcessingException | FileNotFoundException e) {
				logger.error(e.getMessage());
			}
		} else {
			try {
				logger.debug(GWConstants.LOG_OSDCLIENT_MANAGER_DISKPOOLS_CONFIGURE);
				XmlMapper xmlMapper = new XmlMapper();
				InputStream is = new FileInputStream(OSDConstants.DISKPOOL_CONF_PATH);
				byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
				try {
					is.read(buffer, 0, OSDConstants.MAXBUFSIZE);
					is.close();
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
				String xml = new String(buffer);
				
				DISKPOOLLIST diskpoolList = xmlMapper.readValue(xml, DISKPOOLLIST.class);
				for (SERVER server : diskpoolList.getDiskpool().getServers()) {
					if (GWUtils.getLocalIP().equals(server.getIp())) {
						for (DISK disk : server.getDisks()) {
							File file = new File(disk.getPath() + GWConstants.SLASH + GWConstants.OBJ_DIR);
							file.mkdirs();
							file = new File(disk.getPath() + GWConstants.SLASH + GWConstants.TEMP_DIR);
							file.mkdirs();
							file = new File(disk.getPath() + GWConstants.SLASH + GWConstants.TRASH_DIR);
							file.mkdirs();
						}
					}
				}
			} catch (JsonProcessingException | FileNotFoundException e) {
				logger.error(e.getMessage());
			}
		}

		String cacheFileSize = properties.getProperty(GWConstants.PROPERTY_CACHE_FILE_SIZE);
		this.cacheFileSize = Long.parseLong(cacheFileSize);

		this.performanceMode = properties.getProperty(GWConstants.PROPERTY_PERFORMANCE_MODE);
		if (this.performanceMode == null) {
			this.performanceMode = GWConstants.PERFORMANCE_MODE_NO_OPTION;
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
	
	public long maxFileSize() {
		return this.maxFileSize;
	}
	
	public long maxListSize() {
		return this.maxListSize;
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

	public int dbPoolSize() {
		return this.dbPoolSize;
	}

	public String getCacheDisk() {
		return this.cacheDisk;
	}

	public long getCacheFileSize() {
		return this.cacheFileSize;
	}

	public String getPerformanceMode() {
		return this.performanceMode;
	}
}
