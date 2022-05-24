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
        JSONObject jsonEC = (JSONObject)jsonConfig.get("ec");
        JSONObject jsonCache = (JSONObject)jsonConfig.get("cache");

        setPoolSize((int)(long)jsonConfig.get("pool-size"));
        setPort((int)(long)jsonConfig.get("port"));
        setTrashScheduleMinutes((int)(long)jsonConfig.get("trash-schedule-minutes"));

        setECScheduleMinutes((int)(long)jsonEC.get("schedule-minutes"));
        setECApplyMinutes((int)(long)jsonEC.get("apply-minutes"));
        setECFileSize((long)jsonEC.get("file-size"));

        setCacheDisk((String)jsonCache.get("disk"));
        setCacheScheduleMinutes((int)(long)jsonCache.get("schedule-minutes"));
        setCacheFileSize((long)jsonCache.get("file-size"));
        setCacheLimitMinutes((int)(long)jsonCache.get("limit-minutes"));

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
            fileWriter.write("version=" + version + "\n");
            fileWriter.write("pool_size=" + poolSize + "\n");
            fileWriter.write("port=" + port + "\n");
            fileWriter.write("ec_schedule_minutes=" + ecScheduleMinutes + "\n");
            fileWriter.write("ec_apply_minutes=" + ecApplyMinutes + "\n");
            fileWriter.write("ec_file_size=" + ecFileSize + "\n");
            fileWriter.write("cache_disk=" + cacheDisk + "\n");
            fileWriter.write("cache_schedule_minutes=" + cacheScheduleMinutes + "\n");
            fileWriter.write("cache_file_size=" + cacheFileSize + "\n");
            fileWriter.write("cache_limit_minutes=" + cacheLimitMinutes + "\n");
            fileWriter.write("trash_schedule_minutes=" + trashScheduleMinutes + "\n");
            fileWriter.close();
        } catch (IOException e) {
            throw new IOException(e);
        }
    }
}
