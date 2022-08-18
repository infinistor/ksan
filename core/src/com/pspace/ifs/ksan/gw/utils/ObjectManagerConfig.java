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

import java.io.FileWriter;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectManagerConfig {
    private String version;

    private String dbRepository;
    private String dbHost;
    private String dbName;
    private long dbPort;
    private String dbUserName;
    private String dbPassword;
    private String mqHost;
    private int mqPort;
    private String mqUser;
    private String mqPassword;
    private String mqQueueName;
    private String mqExchangeName;
    private String mqOsdExchangeName;

    private static final String VERSION = "version";
    private static final String DB_REPOSITORY = "objM.db_repository";
    private static final String DB_HOST = "objM.db_host";
    private static final String DB_NAME = "objM.db_name";
    private static final String DB_PORT = "objM.db_port";
    private static final String DB_USER = "objM.db_user";
    private static final String DB_PASSWORD = "objM.db_password";
    private static final String MQ_HOST = "objM.mq_host";
    private static final String MQ_QUEUE_NAME = "objM.mq_queue_name";
    private static final String MQ_EXCHANGE_NAME = "objM.mq_exchange_name";
    private static final String MQ_OSD_EXCHANGE_NAME = "objM.mq_osd_exchange_name";
    private static final String EQUAL = "=";

    private static final Logger logger = LoggerFactory.getLogger(ObjectManagerConfig.class);

    public static ObjectManagerConfig getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static ObjectManagerConfig INSTANCE = new ObjectManagerConfig();
    }

    private ObjectManagerConfig() {}

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

    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public long getDbPort() {
        return dbPort;
    }

    public void setDbPort(long dbPort) {
        this.dbPort = dbPort;
    }

    public String getDbUserName() {
        return dbUserName;
    }

    public void setDbUserName(String dbUserName) {
        this.dbUserName = dbUserName;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getMqHost() {
        return mqHost;
    }

    public void setMqHost(String mqHost) {
        this.mqHost = mqHost;
    }

    public String getMqQueueName() {
        return mqQueueName;
    }

    public void setMqQueueName(String mqQueueName) {
        this.mqQueueName = mqQueueName;
    }

    public String getMqExchangeName() {
        return mqExchangeName;
    }

    public void setMqExchangeName(String mqExchangeName) {
        this.mqExchangeName = mqExchangeName;
    }

    public String getMqOsdExchangeName() {
        return mqOsdExchangeName;
    }

    public void setMqOsdExchangeName(String mqOsdExchangeName) {
        this.mqOsdExchangeName = mqOsdExchangeName;
    }

    public void setMqPort(int port) {
        this.mqPort = port;
    }

    public int getMqPort() {
        return this.mqPort;
    }

    public void setMqUser(String userName) {
        this.mqUser = userName;
    }

    public String getMqUser() {
        return this.mqUser;
    }

    public void setMqPassword(String password) {
        this.mqPassword = password;
    }

    public String getMqPassword() {
        return this.mqPassword;
    }

    public void setConfig(JSONObject jsonConfig) {

        setDbRepository((String)jsonConfig.get(DB_REPOSITORY));
        setDbHost((String)jsonConfig.get(DB_HOST));
        setDbName((String)jsonConfig.get(DB_NAME));
        setDbPort((long)jsonConfig.get(DB_PORT));
        setDbUserName((String)jsonConfig.get(DB_USER));
        setDbPassword((String)jsonConfig.get(DB_PASSWORD));
        // setMqHost((String)jsonConfig.get(MQ_HOST));
        // setMqQueueName((String)jsonConfig.get(MQ_QUEUE_NAME));
        // setMqExchangeName((String)jsonConfig.get(MQ_EXCHANGE_NAME));
        // setMqOsdExchangeName((String)jsonConfig.get(MQ_OSD_EXCHANGE_NAME));

        logger.debug(getDbRepository());
        logger.debug(getDbHost());
        logger.debug(getDbName());
        logger.debug("{}", getDbPort());
        logger.debug(getDbUserName());
        logger.debug(getDbPassword());
        // logger.debug(getMqHost());
        // logger.debug(getMqQueueName());
        // logger.debug(getMqExchangeName());
        // logger.debug(getMqOsdExchangeName());
    }

    public void saveConfigFile() throws IOException {
        try {
            FileWriter fileWriter = new FileWriter(GWConstants.OBJMANAGER_CONFIG_PATH, false);
            fileWriter.write(VERSION + EQUAL + version + "\n");
            fileWriter.write(DB_REPOSITORY + EQUAL + dbRepository + "\n");
            fileWriter.write(DB_HOST + EQUAL + dbHost + "\n");
            fileWriter.write(DB_NAME + EQUAL + dbName + "\n");
            fileWriter.write(DB_PORT + EQUAL + dbPort + "\n");
            fileWriter.write(DB_USER + EQUAL + dbUserName + "\n");
            fileWriter.write(DB_PASSWORD + EQUAL + dbPassword + "\n");
            fileWriter.write(MQ_HOST + EQUAL + mqHost + "\n");
            fileWriter.write(MQ_QUEUE_NAME + EQUAL + mqQueueName + "\n");
            fileWriter.write(MQ_EXCHANGE_NAME + EQUAL + mqExchangeName + "\n");
            fileWriter.write(MQ_OSD_EXCHANGE_NAME + EQUAL + mqOsdExchangeName + "\n");
            fileWriter.close();
        } catch (IOException e) {
            throw new IOException(e);
        }
    }
}
