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
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.google.common.base.Strings;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GWConfig {
    private String version;

    private String dbRepository;
	// private long replicaCount;
	private String cacheDiskpath;
    private boolean isCacheDiskpath;
    private String performanceMode;
	// private long cacheFileSize;

	private URI endpoint;
	private URI secureEndpoint;
	private String authorizationString;
	private String keyStorePath;
	private String keyStorePassword;
	private long jettyMaxThreads = GWConstants.JETTY_MAX_THREADS;
	private long jettyMaxIdleTimeout = GWConstants.JETTY_MAX_IDLE_TIMEOUT;
	private long maxFileSize = GWConstants.MAX_FILE_SIZE;
	private long maxListSize = GWConstants.MAX_LIST_SIZE;
	private long maxTimeSkew = GWConstants.MAX_TIME_SKEW;
	private long osdPort = GWConstants.DEFAULT_OSD_PORT;
	private long osdClientCount = GWConstants.DEFAULT_OSD_CLIENT_SIZE;
	private long objManagerCount = GWConstants.DEFAULT_OBJMANAGER_SIZE;
	private String dbHost;
	private String database;
	private long dbPort;
	private String dbUser;
	private String dbPass;
	private long dbPoolSize;
	
	private boolean isNoOption;
	private boolean isNoIO;
	private boolean isNoDisk;
	private boolean isNoReplica;

    private String eventLog;
    private boolean isEventLog;

    private static final String VERSION = "version";
    private static final String AUTHORIZATION = "gw.authorization";
    private static final String ENDPOINT = "gw.endpoint";
    private static final String SECURE_ENDPOINT = "gw.secure_endpoint";
    private static final String KEYSTORE_PATH = "gw.keystore_path";
    private static final String KEYSTORE_PASSWORD = "gw.keystore_password";
    private static final String MAX_FILE_SIZE = "gw.max_file_size";
    private static final String MAX_LIST_SIZE = "gw.max_list_size";
    private static final String MAX_TIMESKEW = "gw.max_timeskew";
    // private static final String REPLICATION = "gw.replication";
    private static final String OSD_PORT = "gw.osd_port";
    private static final String JETTY_MAX_THREADS = "gw.jetty_max_threads";
    private static final String JETTY_MAX_IDLE_TIMEOUT = "gw.jetty_max_idle_timeout";
    private static final String OSD_CLIENT_COUNT = "gw.osd_client_count";
    private static final String OBJMANAGER_COUNT = "gw.objmanager_count";
    private static final String PERFORMANCE_MODE = "gw.performance_mode";
    private static final String DB_REPOSITORY = "gw.db_repository";
    private static final String DB_HOST = "gw.db_host";
    private static final String DB_NAME = "gw.db_name";
    private static final String DB_PORT = "gw.db_port";
    private static final String DB_USER = "gw.db_user";
    private static final String DB_PASSWORD = "gw.db_password";
    private static final String DB_POOL_SIZE = "gw.db_pool_size";
    private static final String CACHE_PATH = "gw.cache_diskpath";
    // private static final String CACHE_FILE_SIZE = "gw.cache_file_size";
    private static final String EVENT_LOG = "gw.eventlog";

    private static final String EQUAL = "=";
    private static final String ON = "on";
    private static final String OFF = "off";

    private static final Logger logger = LoggerFactory.getLogger(GWConfig.class);

    public static GWConfig getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static GWConfig INSTANCE = new GWConfig();
    }

    private GWConfig() {
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDbRepository() {
        return dbRepository;
    }

    public void setDbRepository(String dbRepository) {
        this.dbRepository = dbRepository;
    }

    // public long getReplicaCount() {
    //     return replicaCount;
    // }

    // public void setReplicaCount(long replicaCount) {
    //     this.replicaCount = replicaCount;
    // }

    public String getCacheDiskpath() {
        return cacheDiskpath;
    }

    public void setCacheDiskpath(String cacheDiskpath) {
        this.cacheDiskpath = cacheDiskpath;
    }

    public boolean isCacheDiskpath() {
        return isCacheDiskpath;
    }

    public void setCacheDiskpath(boolean isCacheDiskpath) {
        this.isCacheDiskpath = isCacheDiskpath;
    }

    public String getPerformanceMode() {
        return performanceMode;
    }

    public void setPerformanceMode(String performanceMode) {
        this.performanceMode = performanceMode;
    }

    // public long getCacheFileSize() {
    //     return cacheFileSize;
    // }

    // public void setCacheFileSize(long cacheFileSize) {
    //     this.cacheFileSize = cacheFileSize;
    // }

    public URI getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(URI endpoint) {
        this.endpoint = endpoint;
    }

    public URI getSecureEndpoint() {
        return secureEndpoint;
    }

    public void setSecureEndpoint(URI secureEndpoint) {
        this.secureEndpoint = secureEndpoint;
    }

    public String getAuthorizationString() {
        return authorizationString;
    }

    public void setAuthorizationString(String authorizationString) {
        this.authorizationString = authorizationString;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public long getJettyMaxThreads() {
        return jettyMaxThreads;
    }

    public void setJettyMaxThreads(long jettyMaxThreads) {
        this.jettyMaxThreads = jettyMaxThreads;
    }

    public long getJettyMaxIdleTimeout() {
        return jettyMaxIdleTimeout;
    }

    public void setJettyMaxIdleTimeout(long jettyMaxIdleTimeout) {
        this.jettyMaxIdleTimeout = jettyMaxIdleTimeout;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public long getMaxListSize() {
        return maxListSize;
    }

    public void setMaxListSize(long maxListSize) {
        this.maxListSize = maxListSize;
    }

    public long getMaxTimeSkew() {
        return maxTimeSkew;
    }

    public void setMaxTimeSkew(long maxTimeSkew) {
        this.maxTimeSkew = maxTimeSkew;
    }

    public long getOsdPort() {
        return osdPort;
    }

    public void setOsdPort(long osdPort) {
        this.osdPort = osdPort;
    }

    public long getOsdClientCount() {
        return osdClientCount;
    }

    public void setOsdClientCount(long osdClientCount) {
        this.osdClientCount = osdClientCount;
    }

    public long getObjManagerCount() {
        return objManagerCount;
    }

    public void setObjManagerCount(long objManagerCount) {
        this.objManagerCount = objManagerCount;
    }

    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public long getDbPort() {
        return dbPort;
    }

    public void setDbPort(long dbPort) {
        this.dbPort = dbPort;
    }

    public String getDbUser() {
        return dbUser;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    public String getDbPass() {
        return dbPass;
    }

    public void setDbPass(String dbPass) {
        this.dbPass = dbPass;
    }

    public long getDbPoolSize() {
        return dbPoolSize;
    }

    public void setDbPoolSize(long dbPoolSize) {
        this.dbPoolSize = dbPoolSize;
    }

    public boolean isNoOption() {
        return isNoOption;
    }

    public void setNoOption(boolean isNoOption) {
        this.isNoOption = isNoOption;
    }

    public boolean isNoIO() {
        return isNoIO;
    }

    public void setNoIO(boolean isNoIO) {
        this.isNoIO = isNoIO;
    }

    public boolean isNoDisk() {
        return isNoDisk;
    }

    public void setNoDisk(boolean isNoDisk) {
        this.isNoDisk = isNoDisk;
    }

    public boolean isNoReplica() {
        return isNoReplica;
    }

    public void setNoReplica(boolean isNoReplica) {
        this.isNoReplica = isNoReplica;
    }

    public boolean isEventLog() {
        return isEventLog;
    }

    public void setConfig(JSONObject jsonConfig) throws URISyntaxException {
        setAuthorizationString((String)jsonConfig.get(AUTHORIZATION));
        String endpoint = (String)jsonConfig.get(ENDPOINT);
		String secureEndpoint = (String)jsonConfig.get(SECURE_ENDPOINT);
		if (endpoint == null && secureEndpoint == null) {
			throw new IllegalArgumentException(
					GWConstants.LOG_CONFIG_MUST_CONTAIN +
					ENDPOINT + GWConstants.OR +
					SECURE_ENDPOINT);
		}

		if (endpoint != null) {
			setEndpoint(requireNonNull(new URI(endpoint)));
		}
		if (secureEndpoint != null) {
			setSecureEndpoint(requireNonNull(new URI(secureEndpoint)));
		}

        setKeyStorePath((String)jsonConfig.get(KEYSTORE_PATH));
        setKeyStorePassword((String)jsonConfig.get(KEYSTORE_PASSWORD));
        setMaxFileSize((long)jsonConfig.get(MAX_FILE_SIZE));
        setMaxListSize((long)jsonConfig.get(MAX_LIST_SIZE));
        setMaxTimeSkew((long)jsonConfig.get(MAX_TIMESKEW));
        // setReplicaCount((long)jsonConfig.get(REPLICATION));
        setOsdPort((long)jsonConfig.get(OSD_PORT));
        setJettyMaxThreads((long)jsonConfig.get(JETTY_MAX_THREADS));
        setJettyMaxIdleTimeout((long)jsonConfig.get(JETTY_MAX_IDLE_TIMEOUT));
        setOsdClientCount((long)jsonConfig.get(OSD_CLIENT_COUNT));
        setObjManagerCount((long)jsonConfig.get(OBJMANAGER_COUNT));

        setPerformanceMode((String)jsonConfig.get(PERFORMANCE_MODE));
        if (Strings.isNullOrEmpty(getPerformanceMode())) {
			setPerformanceMode(GWConstants.PERFORMANCE_MODE_NO_OPTION);
            setNoOption(true);
            setNoIO(false);
            setNoDisk(false);
            setNoReplica(false);
		} else {
			if (getPerformanceMode().equals(GWConstants.PERFORMANCE_MODE_NO_OPTION)) {
				setPerformanceMode(GWConstants.PERFORMANCE_MODE_NO_OPTION);
                setNoOption(true);
                setNoIO(false);
                setNoDisk(false);
                setNoReplica(false);
			} else if (getPerformanceMode().equals(GWConstants.PERFORMANCE_MODE_NO_IO)) {
				setNoOption(false);
                setNoIO(true);
                setNoDisk(false);
                setNoReplica(false);
			} else if (getPerformanceMode().equals(GWConstants.PERFORMANCE_MODE_NO_DISK)) {
				setNoOption(false);
                setNoIO(false);
                setNoDisk(true);
                setNoReplica(false);
			} else if (getPerformanceMode().equals(GWConstants.PERFORMANCE_MODE_NO_REPLICA)) {
				setNoOption(false);
                setNoIO(false);
                setNoDisk(false);
                setNoReplica(true);
			}
		}

        eventLog = (String)jsonConfig.get(EVENT_LOG);
        if (!Strings.isNullOrEmpty(eventLog) && eventLog.equalsIgnoreCase(OFF)) {
            isEventLog = false;
        } else {
            eventLog = ON;
            isEventLog = true;
        }

        setDbRepository((String)jsonConfig.get(DB_REPOSITORY));
        setDbHost((String)jsonConfig.get(DB_HOST));
        setDatabase((String)jsonConfig.get(DB_NAME));
        setDbPort((long)jsonConfig.get(DB_PORT));
        setDbUser((String)jsonConfig.get(DB_USER));
        setDbPass((String)jsonConfig.get(DB_PASSWORD));
        setDbPoolSize((long)jsonConfig.get(DB_POOL_SIZE));

        setCacheDiskpath((String)jsonConfig.get(CACHE_PATH));
        if (!Strings.isNullOrEmpty(getCacheDiskpath())) {
            File file = new File(getCacheDiskpath());
            setCacheDiskpath(file.exists());
        } else {
            setCacheDiskpath(false);
        }

        // setCacheFileSize((long)jsonConfig.get(CACHE_FILE_SIZE));

        logger.debug(getAuthorizationString());
        logger.debug(getEndpoint().toString());
        logger.debug(getSecureEndpoint().toString());
        logger.debug(getKeyStorePath());
        logger.debug(getKeyStorePassword());
        logger.debug("{}", getMaxFileSize());
        logger.debug("{}", getMaxListSize());
        logger.debug("{}", getMaxTimeSkew());
        // logger.debug("{}", getReplicaCount());
        logger.debug("{}", getOsdPort());
        logger.debug("{}", getJettyMaxThreads());
        logger.debug("{}", getOsdClientCount());
        logger.debug("{}", getObjManagerCount());
        logger.debug(getPerformanceMode());
        logger.debug(getDbRepository());
        logger.debug(getDbHost());
        logger.debug(getDatabase());
        logger.debug("{}", getDbPort());
        logger.debug(getDbUser());
        logger.debug(getDbPass());
        logger.debug("{}", getDbPoolSize());
        logger.debug(getCacheDiskpath());
        // logger.debug("{}", getCacheFileSize());
    }

    public void saveConfigFile() throws IOException {
        try {
            FileWriter fileWriter = new FileWriter(GWConstants.CONFIG_PATH, false);
            fileWriter.write(VERSION + EQUAL + version + "\n");
            fileWriter.write(AUTHORIZATION + EQUAL + authorizationString + "\n");
            fileWriter.write(ENDPOINT + EQUAL + endpoint.toString() + "\n");
            fileWriter.write(SECURE_ENDPOINT + EQUAL + secureEndpoint.toString() + "\n");
            fileWriter.write(KEYSTORE_PATH + EQUAL + keyStorePath + "\n");
            fileWriter.write(KEYSTORE_PASSWORD + EQUAL + keyStorePassword + "\n");
            fileWriter.write(MAX_FILE_SIZE + EQUAL + maxFileSize + "\n");
            fileWriter.write(MAX_LIST_SIZE + EQUAL + maxListSize + "\n");
            fileWriter.write(MAX_TIMESKEW + EQUAL + maxTimeSkew + "\n");
            fileWriter.write(EVENT_LOG + EQUAL + eventLog + "\n");
            // fileWriter.write(REPLICATION + EQUAL + replicaCount + "\n");
            fileWriter.write(OSD_PORT + EQUAL + osdPort + "\n");
            fileWriter.write(JETTY_MAX_THREADS + EQUAL + jettyMaxThreads + "\n");
            fileWriter.write(OSD_CLIENT_COUNT + EQUAL + osdClientCount + "\n");
            fileWriter.write(OBJMANAGER_COUNT + EQUAL + objManagerCount + "\n");
            fileWriter.write(PERFORMANCE_MODE + EQUAL + performanceMode + "\n");
            fileWriter.write(DB_REPOSITORY + EQUAL + dbRepository + "\n");
            fileWriter.write(DB_HOST + EQUAL + dbHost + "\n");
            fileWriter.write(DB_NAME + EQUAL + database + "\n");
            fileWriter.write(DB_PORT + EQUAL + dbPort + "\n");
            fileWriter.write(DB_USER + EQUAL + dbUser + "\n");
            fileWriter.write(DB_PASSWORD + EQUAL + dbPass + "\n");
            fileWriter.write(DB_POOL_SIZE + EQUAL + dbPoolSize + "\n");
            fileWriter.write(CACHE_PATH + EQUAL + cacheDiskpath + "\n");
            // fileWriter.write(CACHE_FILE_SIZE + EQUAL + cacheFileSize + "\n");
            fileWriter.close();
        } catch (IOException e) {
            throw new IOException(e);
        }
    }
}
