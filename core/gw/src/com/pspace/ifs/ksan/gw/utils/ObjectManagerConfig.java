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
    private String mqDiskPoolQueueName;
    private String mqDiskPoolExchangeName;
    private String mqOSDExchangeName;

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

    public String getMqDiskPoolQueueName() {
        return mqDiskPoolQueueName;
    }

    public void setMqDiskPoolQueueName(String mqDiskPoolQueueName) {
        this.mqDiskPoolQueueName = mqDiskPoolQueueName;
    }

    public String getMqDiskPoolExchangeName() {
        return mqDiskPoolExchangeName;
    }

    public void setMqDiskPoolExchangeName(String mqDiskPoolExchangeName) {
        this.mqDiskPoolExchangeName = mqDiskPoolExchangeName;
    }

    public String getMqOSDExchangeName() {
        return mqOSDExchangeName;
    }

    public void setMqOSDExchangeName(String mqOSDExchangeName) {
        this.mqOSDExchangeName = mqOSDExchangeName;
    }

    public void setConfig(JSONObject jsonConfig) {
        JSONObject jsonDB = (JSONObject)jsonConfig.get("db");
        JSONObject jsonMQ = (JSONObject)jsonConfig.get("mq");

        setDbRepository((String)jsonDB.get("repository"));
        setDbHost((String)jsonDB.get("host"));
        setDbName((String)jsonDB.get("name"));
        setDbPort((long)jsonDB.get("port"));
        setDbUserName((String)jsonDB.get("username"));
        setDbPassword((String)jsonDB.get("password"));
        setMqHost((String)jsonMQ.get("host"));
        setMqDiskPoolQueueName((String)jsonMQ.get("diskpool-queue-name"));
        setMqDiskPoolExchangeName((String)jsonMQ.get("diskpool-exchange-name"));
        setMqOSDExchangeName((String)jsonMQ.get("osd-exchange-name"));

        logger.debug(getDbRepository());
        logger.debug(getDbHost());
        logger.debug(getDbName());
        logger.debug("{}", getDbPort());
        logger.debug(getDbUserName());
        logger.debug(getDbPassword());
        logger.debug(getMqHost());
        logger.debug(getMqDiskPoolQueueName());
        logger.debug(getMqDiskPoolExchangeName());
        logger.debug(getMqOSDExchangeName());
    }

    public void saveConfigFile() throws IOException {
        try {
            FileWriter fileWriter = new FileWriter("/usr/local/ksan/etc/objmanger.conf2", false);
            fileWriter.write("version=" + version + "\n");
            fileWriter.write("db.repository=" + dbRepository + "\n");
            fileWriter.write("db.host=" + dbHost + "\n");
            fileWriter.write("db.name=" + dbName + "\n");
            fileWriter.write("db.port=" + dbPort + "\n");
            fileWriter.write("db.username=" + dbUserName + "\n");
            fileWriter.write("db.password=" + dbPassword + "\n");
            fileWriter.write("mq.host=" + mqHost + "\n");
            fileWriter.write("mq.diskpool-queue-name=" + mqDiskPoolQueueName + "\n");
            fileWriter.write("mq.diskpool-exchange-name=" + mqDiskPoolExchangeName + "\n");
            fileWriter.write("mq.osd-exchange-name=" + mqOSDExchangeName + "\n");
            fileWriter.close();
        } catch (IOException e) {
            throw new IOException(e);
        }
    }
}
