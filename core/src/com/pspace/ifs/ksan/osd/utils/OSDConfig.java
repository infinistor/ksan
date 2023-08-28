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

package com.pspace.ifs.ksan.osd.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import java.io.File;

public class OSDConfig {
    public static final String OSD_JSON_CONFIG_DATA = "Data";
    public static final String OSD_JSON_CONFIG_VERSION = "Version";
    public static final String OSD_JSON_CONFIG = "Config";

    private String version;

    private int poolSize;
    private int port;
    private int ecCheckInterval;
    private int ecWaitTime;
    private long ecMinSize;
    private String cacheDiskpath;
    private boolean isCacheDiskpath;
    
    private int cacheCheckInterval;
    // private long cacheFileSize;
    private int cacheExpire;
    private int trashCheckInterval;

    private static final String VERSION = "version";
    private static final String POOL_SIZE = "pool_size";
    private static final String PORT = "port";
    private static final String EC_CHECK_INTERVAL = "ec_check_interval"; //"osd.ec_schedule_milliseconds";
    private static final String EC_WAIT_TIME = "ec_wait_time"; //"osd.ec_apply_milliseconds";
    private static final String EC_MIN_FILE_SIZE = "ec_min_size"; //"osd.ec_file_size";
    private static final String CACHE_DISKPATH = "cache_diskpath";
    private static final String CACHE_CHECK_INTERVAL = "cache_check_interval"; //"osd.cache_schedule_milliseconds";
    // private static final String CACHE_FILE_SIZE = "osd.cache_file_size";
    private static final String CACHE_EXPIRE = "cache_expire"; //"osd.cache_limit_milliseconds";
    private static final String TRASH_CHECK_INTERVAL = "trash_check_interval"; //"osd.trash_schedule_milliseconds";
    private static final String EQUAL = "=";

    private static final Logger logger = LoggerFactory.getLogger(OSDConfig.class);

    public static OSDConfig getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static OSDConfig INSTANCE = new OSDConfig();
    }

    private OSDConfig() {
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getECCheckInterval() {
        return ecCheckInterval;
    }

    public void setECCheckInterval(int ecCheckInterval) {
        this.ecCheckInterval = ecCheckInterval;
    }

    public int getECWaitTime() {
        return ecWaitTime;
    }

    public void setECWaitTime(int ecWaitTime) {
        this.ecWaitTime = ecWaitTime;
    }

    public long getECMinSize() {
        return ecMinSize;
    }

    public void setECMinSize(long ecMinSize) {
        this.ecMinSize = ecMinSize;
    }

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

    public int getCacheCheckInterval() {
        return cacheCheckInterval;
    }

    public void setCacheCheckInterval(int cacheCheckInterval) {
        this.cacheCheckInterval = cacheCheckInterval;
    }

    // public long getCacheFileSize() {
    //     return cacheFileSize;
    // }

    // public void setCacheFileSize(long cacheFileSize) {
    //     this.cacheFileSize = cacheFileSize;
    // }
    
    public int getCacheExpire() {
        return cacheExpire;
    }

    public void setCacheExpire(int cacheExpire) {
        this.cacheExpire = cacheExpire;
    }

    public int getTrashCheckInterval() {
        return trashCheckInterval;
    }

    public void setTrashCheckInterval(int trashCheckInterval) {
        this.trashCheckInterval = trashCheckInterval;
    }

    public void setConfig(JSONObject jsonConfig) throws URISyntaxException {
        try {
            setPoolSize((int)(long)jsonConfig.get(POOL_SIZE));
        } catch (Exception e) {
            setPoolSize(Integer.parseInt((String)jsonConfig.get(POOL_SIZE)));
        }
        
        try {
            setPort((int)(long)jsonConfig.get(PORT));
        } catch (Exception e) {
            setPort(Integer.parseInt((String)jsonConfig.get(PORT)));
        }

        try {
            setTrashCheckInterval((int)(long)jsonConfig.get(TRASH_CHECK_INTERVAL));
        } catch (Exception e) {
            setTrashCheckInterval(Integer.parseInt((String)jsonConfig.get(TRASH_CHECK_INTERVAL)));
        }

        try {
            setECCheckInterval((int)(long)jsonConfig.get(EC_CHECK_INTERVAL));
        } catch (Exception e) {
            setECCheckInterval(Integer.parseInt((String)jsonConfig.get(EC_CHECK_INTERVAL)));
        }

        try {
            setECWaitTime((int)(long)jsonConfig.get(EC_WAIT_TIME));
        } catch (Exception e) {
            setECWaitTime(Integer.parseInt((String)jsonConfig.get(EC_WAIT_TIME)));
        }

        try {
            setECMinSize((long)jsonConfig.get(EC_MIN_FILE_SIZE));
        } catch (Exception e) {
            setECMinSize(Long.parseLong((String)jsonConfig.get(EC_MIN_FILE_SIZE)));
        }

        setCacheDiskpath((String)jsonConfig.get(CACHE_DISKPATH));
        if (!Strings.isNullOrEmpty(getCacheDiskpath())) {
            File file = new File(getCacheDiskpath());
            setCacheDiskpath(file.exists());
        } else {
            setCacheDiskpath(false);
        }
        
        try {
            setCacheCheckInterval((int)(long)jsonConfig.get(CACHE_CHECK_INTERVAL));
        } catch (Exception e) {
            setCacheCheckInterval(Integer.parseInt((String)jsonConfig.get(CACHE_CHECK_INTERVAL)));
        }
        
        // try {
        //     setCacheFileSize((long)jsonConfig.get(CACHE_FILE_SIZE));
        // } catch (Exception e) {
        //     setCacheFileSize(Long.parseLong((String)jsonConfig.get(CACHE_FILE_SIZE)));
        // }
        
        try {
            setCacheExpire((int)(long)jsonConfig.get(CACHE_EXPIRE));
        } catch (Exception e) {
            setCacheExpire(Integer.parseInt((String)jsonConfig.get(CACHE_EXPIRE)));
        }

        logger.debug("pool size : {}", getPoolSize());
        logger.debug("port : {}", getPort());
        logger.debug("trash schedule ms : {}", getTrashCheckInterval());
        logger.debug("ec schedule ms : {}", getECCheckInterval());
        logger.debug("ec apply ms : {}", getECWaitTime());
        logger.debug("ec file size : {}", getECMinSize());
        logger.debug("cache disk : {}", getCacheDiskpath());
        logger.debug("cache schedule ms : {}", getCacheCheckInterval());
        // logger.debug("cache file size : {}", getCacheFileSize());
        logger.debug("cache limit ms : {}", getCacheExpire());
    }

    public void saveConfigFile() throws IOException {
        try {
            com.google.common.io.Files.createParentDirs(new File(OSDConstants.CONFIG_PATH));
            try(FileWriter fileWriter = new FileWriter(OSDConstants.CONFIG_PATH, StandardCharsets.UTF_8)) {
                fileWriter.write(VERSION + EQUAL + version + "\n");
                fileWriter.write(POOL_SIZE + EQUAL + poolSize + "\n");
                fileWriter.write(PORT + EQUAL + port + "\n");
                fileWriter.write(EC_CHECK_INTERVAL + EQUAL + ecCheckInterval + "\n");
                fileWriter.write(EC_WAIT_TIME + EQUAL + ecWaitTime + "\n");
                fileWriter.write(EC_MIN_FILE_SIZE + EQUAL + ecMinSize + "\n");
                fileWriter.write(CACHE_DISKPATH + EQUAL + cacheDiskpath + "\n");
                fileWriter.write(CACHE_CHECK_INTERVAL + EQUAL + cacheCheckInterval + "\n");
                // fileWriter.write(CACHE_FILE_SIZE + EQUAL + cacheFileSize + "\n");
                fileWriter.write(CACHE_EXPIRE + EQUAL + cacheExpire + "\n");
                fileWriter.write(TRASH_CHECK_INTERVAL + EQUAL + trashCheckInterval + "\n");
            } catch (IOException e) {
                throw new IOException(e);
            }
        } catch (IOException e) {
            throw new IOException(e);
        }
    }
}
