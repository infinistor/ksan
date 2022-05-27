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
    private int ecScheduleMinutes;
    private int ecApplyMinutes;
    private long ecFileSize;
    private String cacheDisk;
    private int cacheScheduleMinutes;
    private long cacheFileSize;
    private int cacheLimitMinutes;
    private int trashScheduleMinutes;

    private static final String VERSION = "version";
    private static final String POOL_SIZE = "osd.pool_size";
    private static final String PORT = "osd.port";
    private static final String EC_SCHEDULE_MINUTES = "osd.ec_schedule_minutes";
    private static final String EC_APPLY_MINUTES = "osd.ec_apply_minutes";
    private static final String EC_FILE_SIZE = "osd.ec_file_size";
    private static final String CACHE_DISK = "osd.cache_disk";
    private static final String CACHE_SCHEDULE_MINUTES = "osd.cache_schedule_minutes";
    private static final String CACHE_FILE_SIZE = "osd.cache_file_size";
    private static final String CACHE_LIMIT_MINUTES = "osd.cache_limit_minutes";
    private static final String TRASH_SCHEDULE_MINUTES = "osd.trash_schedule_minutes";
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

    public int getECScheduleMinutes() {
        return ecScheduleMinutes;
    }

    public void setECScheduleMinutes(int ecScheduleMinutes) {
        this.ecScheduleMinutes = ecScheduleMinutes;
    }

    public int getECApplyMinutes() {
        return ecApplyMinutes;
    }

    public void setECApplyMinutes(int ecApplyMinutes) {
        this.ecApplyMinutes = ecApplyMinutes;
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

    public int getCacheScheduleMinutes() {
        return cacheScheduleMinutes;
    }

    public void setCacheScheduleMinutes(int cacheScheduleMinutes) {
        this.cacheScheduleMinutes = cacheScheduleMinutes;
    }

    public long getCacheFileSize() {
        return cacheFileSize;
    }

    public void setCacheFileSize(long cacheFileSize) {
        this.cacheFileSize = cacheFileSize;
    }
    
    public int getCacheLimitMinutes() {
        return cacheLimitMinutes;
    }

    public void setCacheLimitMinutes(int cacheLimitMinutes) {
        this.cacheLimitMinutes = cacheLimitMinutes;
    }

    public int getTrashScheduleMinutes() {
        return trashScheduleMinutes;
    }

    public void setTrashScheduleMinutes(int trashScheduleMinutes) {
        this.trashScheduleMinutes = trashScheduleMinutes;
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
            setTrashScheduleMinutes((int)(long)jsonConfig.get(TRASH_SCHEDULE_MINUTES));
        } catch (Exception e) {
            setTrashScheduleMinutes(Integer.parseInt((String)jsonConfig.get(TRASH_SCHEDULE_MINUTES)));
        }

        try {
            setECScheduleMinutes((int)(long)jsonConfig.get(EC_SCHEDULE_MINUTES));
        } catch (Exception e) {
            setECScheduleMinutes(Integer.parseInt((String)jsonConfig.get(EC_SCHEDULE_MINUTES)));
        }

        try {
            setECApplyMinutes((int)(long)jsonConfig.get(EC_APPLY_MINUTES));
        } catch (Exception e) {
            setECApplyMinutes(Integer.parseInt((String)jsonConfig.get(EC_APPLY_MINUTES)));
        }

        try {
            setECFileSize((long)jsonConfig.get(EC_FILE_SIZE));
        } catch (Exception e) {
            setECFileSize(Long.parseLong((String)jsonConfig.get(EC_FILE_SIZE)));
        }

        setCacheDisk((String)jsonConfig.get(CACHE_DISK));
        
        try {
            setCacheScheduleMinutes((int)(long)jsonConfig.get(CACHE_SCHEDULE_MINUTES));
        } catch (Exception e) {
            setCacheScheduleMinutes(Integer.parseInt((String)jsonConfig.get(CACHE_SCHEDULE_MINUTES)));
        }
        
        try {
            setCacheFileSize((long)jsonConfig.get(CACHE_FILE_SIZE));
        } catch (Exception e) {
            setCacheFileSize(Long.parseLong((String)jsonConfig.get(CACHE_FILE_SIZE)));
        }
        
        try {
            setCacheLimitMinutes((int)(long)jsonConfig.get(CACHE_LIMIT_MINUTES));
        } catch (Exception e) {
            setCacheLimitMinutes(Integer.parseInt((String)jsonConfig.get(CACHE_LIMIT_MINUTES)));
        }

        logger.debug("pool size : {}", getPoolSize());
        logger.debug("port : {}", getPort());
        logger.debug("trash schedule minutes : {}", getTrashScheduleMinutes());
        logger.debug("ec schedule minutes : {}", getECScheduleMinutes());
        logger.debug("ec apply minutes : {}", getECApplyMinutes());
        logger.debug("ec file size : {}", getECFileSize());
        logger.debug("cache disk : {}", getCacheDisk());
        logger.debug("cache schedule minutes : {}", getCacheScheduleMinutes());
        logger.debug("cache file size : {}", getCacheFileSize());
        logger.debug("cache limit minutes : {}", getCacheLimitMinutes());
    }

    public void saveConfigFile() throws IOException {
        try {
            FileWriter fileWriter = new FileWriter(OSDConstants.CONFIG_PATH, false);
            fileWriter.write(VERSION + EQUAL + version + "\n");
            fileWriter.write(POOL_SIZE + EQUAL + poolSize + "\n");
            fileWriter.write(PORT + EQUAL + port + "\n");
            fileWriter.write(EC_SCHEDULE_MINUTES + EQUAL + ecScheduleMinutes + "\n");
            fileWriter.write(EC_APPLY_MINUTES + EQUAL + ecApplyMinutes + "\n");
            fileWriter.write(EC_FILE_SIZE + EQUAL + ecFileSize + "\n");
            fileWriter.write(CACHE_DISK + EQUAL + cacheDisk + "\n");
            fileWriter.write(CACHE_SCHEDULE_MINUTES + EQUAL + cacheScheduleMinutes + "\n");
            fileWriter.write(CACHE_FILE_SIZE + EQUAL + cacheFileSize + "\n");
            fileWriter.write(CACHE_LIMIT_MINUTES + EQUAL + cacheLimitMinutes + "\n");
            fileWriter.write(TRASH_SCHEDULE_MINUTES + EQUAL + trashScheduleMinutes + "\n");
            fileWriter.close();
        } catch (IOException e) {
            throw new IOException(e);
        }
    }
}
