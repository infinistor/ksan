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
	private long replicaCount;
	private String cacheDisk;
	private String performanceMode;
	private long cacheFileSize;

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

    public long getReplicaCount() {
        return replicaCount;
    }

    public void setReplicaCount(long replicaCount) {
        this.replicaCount = replicaCount;
    }

    public String getCacheDisk() {
        return cacheDisk;
    }

    public void setCacheDisk(String cacheDisk) {
        this.cacheDisk = cacheDisk;
    }

    public String getPerformanceMode() {
        return performanceMode;
    }

    public void setPerformanceMode(String performanceMode) {
        this.performanceMode = performanceMode;
    }

    public long getCacheFileSize() {
        return cacheFileSize;
    }

    public void setCacheFileSize(long cacheFileSize) {
        this.cacheFileSize = cacheFileSize;
    }

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

    public void setConfig(JSONObject jsonConfig) throws URISyntaxException {
        JSONObject jsonDB = (JSONObject)jsonConfig.get("db");
        JSONObject jsonCache = (JSONObject)jsonConfig.get("cache");

        setAuthorizationString((String)jsonConfig.get("authorization"));
        String endpoint = (String)jsonConfig.get("endpoint");
		String secureEndpoint = (String)jsonConfig.get("secure-endpoint");
		if (endpoint == null && secureEndpoint == null) {
			throw new IllegalArgumentException(
					GWConstants.LOG_CONFIG_MUST_CONTAIN +
					GWConstants.PROPERTY_ENDPOINT + GWConstants.OR +
					GWConstants.PROPERTY_SECURE_ENDPOINT);
		}

		if (endpoint != null) {
			setEndpoint(requireNonNull(new URI(endpoint)));
		}
		if (secureEndpoint != null) {
			setSecureEndpoint(requireNonNull(new URI(secureEndpoint)));
		}

        setKeyStorePath((String)jsonConfig.get("keystore-path"));
        setKeyStorePassword((String)jsonConfig.get("keystore-password"));
        setMaxFileSize((long)jsonConfig.get("max-file-size"));
        setMaxListSize((long)jsonConfig.get("max-list-size"));
        setMaxTimeSkew((long)jsonConfig.get("max-time-skew"));
        setReplicaCount((long)jsonConfig.get("replication"));
        setOsdPort((long)jsonConfig.get("osd-port"));
        setJettyMaxThreads((long)jsonConfig.get("jetty-max-threads"));
        setOsdClientCount((long)jsonConfig.get("osd-client-count"));
        setObjManagerCount((long)jsonConfig.get("objmanager-count"));

        setPerformanceMode((String)jsonConfig.get("performance-mode"));
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

        setDbRepository((String)jsonDB.get("repository"));
        setDbHost((String)jsonDB.get("host"));
        setDatabase((String)jsonDB.get("database"));
        setDbPort((long)jsonDB.get("port"));
        setDbUser((String)jsonDB.get("user"));
        setDbPass((String)jsonDB.get("pass"));
        setDbPoolSize((long)jsonDB.get("poolsize"));

        setCacheDisk((String)jsonCache.get("path"));
        setCacheFileSize((long)jsonCache.get("file-size"));

        logger.debug(getAuthorizationString());
        logger.debug(getEndpoint().toString());
        logger.debug(getSecureEndpoint().toString());
        logger.debug(getKeyStorePath());
        logger.debug(getKeyStorePassword());
        logger.debug("{}", getMaxFileSize());
        logger.debug("{}", getMaxListSize());
        logger.debug("{}", getMaxTimeSkew());
        logger.debug("{}", getReplicaCount());
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
        logger.debug(getCacheDisk());
        logger.debug("{}", getCacheFileSize());
    }

    public void saveConfigFile() throws IOException {
        try {
            FileWriter fileWriter = new FileWriter(GWConstants.CONFIG_PATH + "2", false);
            fileWriter.write("version=" + version + "\n");
            fileWriter.write("gw.authorization=" + authorizationString + "\n");
            fileWriter.write("gw.endpoint=" + endpoint.toString() + "\n");
            fileWriter.write("gw.secure-endpoint=" + secureEndpoint.toString() + "\n");
            fileWriter.write("gw.keystore-path=" + keyStorePath + "\n");
            fileWriter.write("gw.keystore-password=" + keyStorePassword + "\n");
            fileWriter.write("gw.max-file-size=" + maxFileSize + "\n");
            fileWriter.write("gw.max-list-size=" + maxListSize + "\n");
            fileWriter.write("gw.max-time-skew=" + maxTimeSkew + "\n");
            fileWriter.write("gw.replication=" + replicaCount + "\n");
            fileWriter.write("gw.osd-port=" + osdPort + "\n");
            fileWriter.write("gw.jetty-max-threads=" + jettyMaxThreads + "\n");
            fileWriter.write("gw.osd-client-count=" + osdClientCount + "\n");
            fileWriter.write("gw.objmanager-count=" + objManagerCount + "\n");
            fileWriter.write("gw.performance-mode=" + performanceMode + "\n");
            fileWriter.write("db.repository=" + dbRepository + "\n");
            fileWriter.write("db.host=" + dbHost + "\n");
            fileWriter.write("db.database=" + database + "\n");
            fileWriter.write("db.port=" + dbPort + "\n");
            fileWriter.write("db.user=" + dbUser + "\n");
            fileWriter.write("db.pass=" + dbPass + "\n");
            fileWriter.write("db.poolsize=" + dbPoolSize + "\n");
            fileWriter.write("cache.path=" + cacheDisk + "\n");
            fileWriter.write("cache.file-size=" + cacheFileSize + "\n");
            fileWriter.close();
        } catch (IOException e) {
            throw new IOException(e);
        }
    }
}
