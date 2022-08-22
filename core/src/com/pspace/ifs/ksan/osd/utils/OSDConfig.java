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

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OSDConfig {
    public static final String OSD_JSON_CONFIG_DATA = "Data";
    public static final String OSD_JSON_CONFIG_VERSION = "Version";
    public static final String OSD_JSON_CONFIG = "Config";

    private String version;

    private int poolSize;
    private int port;
    private int ecScheduleMilliseconds;
    private int ecApplyMilliseconds;
    private long ecFileSize;
    private String cacheDisk;
    private int cacheScheduleMilliseconds;
    private long cacheFileSize;
    private int cacheLimitMilliseconds;
    private int trashScheduleMilliseconds;

    private static final String VERSION = "version";
    private static final String POOL_SIZE = "osd.pool_size";
    private static final String PORT = "osd.port";
    private static final String EC_SCHEDULE_MINUTES = "osd.ec_check_interval"; //"osd.ec_schedule_milliseconds";
    private static final String EC_APPLY_MINUTES = "osd.ec_excute_timeout"; //"osd.ec_apply_milliseconds";
    private static final String EC_FILE_SIZE = "osd.ec_min_size"; //"osd.ec_file_size";
    private static final String CACHE_DISK = "osd.cache_diskpath";
    private static final String CACHE_SCHEDULE_MINUTES = "osd.cache_check_interval"; //"osd.cache_schedule_milliseconds";
    // private static final String CACHE_FILE_SIZE = "osd.cache_file_size";
    private static final String CACHE_LIMIT_MINUTES = "osd.cache_expire"; //"osd.cache_limit_milliseconds";
    private static final String TRASH_SCHEDULE_MINUTES = "osd.trash_check_interval"; //"osd.trash_schedule_milliseconds";
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

    public int getECScheduleMilliseconds() {
        return ecScheduleMilliseconds;
    }

    public void setECScheduleMilliseconds(int ecScheduleMilliseconds) {
        this.ecScheduleMilliseconds = ecScheduleMilliseconds;
    }

    public int getECApplyMilliseconds() {
        return ecApplyMilliseconds;
    }

    public void setECApplyMilliseconds(int ecApplyMilliseconds) {
        this.ecApplyMilliseconds = ecApplyMilliseconds;
    }

    public long getECFileSize() {
        return ecFileSize;
    }

    public void setECFileSize(long ecFileSize) {
        this.ecFileSize = ecFileSize;
    }

    public String getCacheDisk() {
        return cacheDisk;
    }

    public void setCacheDisk(String cacheDisk) {
        this.cacheDisk = cacheDisk;
    }

    public int getCacheScheduleMilliseconds() {
        return cacheScheduleMilliseconds;
    }

    public void setCacheScheduleMilliseconds(int cacheScheduleMilliseconds) {
        this.cacheScheduleMilliseconds = cacheScheduleMilliseconds;
    }

    public long getCacheFileSize() {
        return cacheFileSize;
    }

    public void setCacheFileSize(long cacheFileSize) {
        this.cacheFileSize = cacheFileSize;
    }
    
    public int getCacheLimitMilliseconds() {
        return cacheLimitMilliseconds;
    }

    public void setCacheLimitMilliseconds(int cacheLimitMilliseconds) {
        this.cacheLimitMilliseconds = cacheLimitMilliseconds;
    }

    public int getTrashScheduleMilliseconds() {
        return trashScheduleMilliseconds;
    }

    public void setTrashScheduleMilliseconds(int trashScheduleMilliseconds) {
        this.trashScheduleMilliseconds = trashScheduleMilliseconds;
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
            setTrashScheduleMilliseconds((int)(long)jsonConfig.get(TRASH_SCHEDULE_MINUTES));
        } catch (Exception e) {
            setTrashScheduleMilliseconds(Integer.parseInt((String)jsonConfig.get(TRASH_SCHEDULE_MINUTES)));
        }

        try {
            setECScheduleMilliseconds((int)(long)jsonConfig.get(EC_SCHEDULE_MINUTES));
        } catch (Exception e) {
            setECScheduleMilliseconds(Integer.parseInt((String)jsonConfig.get(EC_SCHEDULE_MINUTES)));
        }

        try {
            setECApplyMilliseconds((int)(long)jsonConfig.get(EC_APPLY_MINUTES));
        } catch (Exception e) {
            setECApplyMilliseconds(Integer.parseInt((String)jsonConfig.get(EC_APPLY_MINUTES)));
        }

        try {
            setECFileSize((long)jsonConfig.get(EC_FILE_SIZE));
        } catch (Exception e) {
            setECFileSize(Long.parseLong((String)jsonConfig.get(EC_FILE_SIZE)));
        }

        setCacheDisk((String)jsonConfig.get(CACHE_DISK));
        
        try {
            setCacheScheduleMilliseconds((int)(long)jsonConfig.get(CACHE_SCHEDULE_MINUTES));
        } catch (Exception e) {
            setCacheScheduleMilliseconds(Integer.parseInt((String)jsonConfig.get(CACHE_SCHEDULE_MINUTES)));
        }
        
        try {
            setCacheFileSize((long)jsonConfig.get(CACHE_FILE_SIZE));
        } catch (Exception e) {
            setCacheFileSize(Long.parseLong((String)jsonConfig.get(CACHE_FILE_SIZE)));
        }
        
        try {
            setCacheLimitMilliseconds((int)(long)jsonConfig.get(CACHE_LIMIT_MINUTES));
        } catch (Exception e) {
            setCacheLimitMilliseconds(Integer.parseInt((String)jsonConfig.get(CACHE_LIMIT_MINUTES)));
        }

        logger.debug("pool size : {}", getPoolSize());
        logger.debug("port : {}", getPort());
        logger.debug("trash schedule minutes : {}", getTrashScheduleMilliseconds());
        logger.debug("ec schedule minutes : {}", getECScheduleMilliseconds());
        logger.debug("ec apply minutes : {}", getECApplyMilliseconds());
        logger.debug("ec file size : {}", getECFileSize());
        logger.debug("cache disk : {}", getCacheDisk());
        logger.debug("cache schedule minutes : {}", getCacheScheduleMilliseconds());
        logger.debug("cache file size : {}", getCacheFileSize());
        logger.debug("cache limit minutes : {}", getCacheLimitMilliseconds());
    }

    public void saveConfigFile() throws IOException {
        try {
            FileWriter fileWriter = new FileWriter(OSDConstants.CONFIG_PATH, false);
            fileWriter.write(VERSION + EQUAL + version + "\n");
            fileWriter.write(POOL_SIZE + EQUAL + poolSize + "\n");
            fileWriter.write(PORT + EQUAL + port + "\n");
            fileWriter.write(EC_SCHEDULE_MINUTES + EQUAL + ecScheduleMilliseconds + "\n");
            fileWriter.write(EC_APPLY_MINUTES + EQUAL + ecApplyMilliseconds + "\n");
            fileWriter.write(EC_FILE_SIZE + EQUAL + ecFileSize + "\n");
            fileWriter.write(CACHE_DISK + EQUAL + cacheDisk + "\n");
            fileWriter.write(CACHE_SCHEDULE_MINUTES + EQUAL + cacheScheduleMilliseconds + "\n");
            fileWriter.write(CACHE_FILE_SIZE + EQUAL + cacheFileSize + "\n");
            fileWriter.write(CACHE_LIMIT_MINUTES + EQUAL + cacheLimitMilliseconds + "\n");
            fileWriter.write(TRASH_SCHEDULE_MINUTES + EQUAL + trashScheduleMilliseconds + "\n");
            fileWriter.close();
        } catch (IOException e) {
            throw new IOException(e);
        }
    }
}
