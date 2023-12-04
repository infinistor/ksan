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
package com.pspace.ifs.ksan.libs.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.pspace.ifs.ksan.libs.Constants;

public class AgentConfig {
    private Properties properties;

    private String portalIp;
    private String portalPort;
    private String serverId;
    private String portalKey;
    private String mqHost;
    private String mqPort;
    private String mqUser;
    private String mqPassword;
    private long serviceMonitorInterval;
    private String multipartUploadMethod;
    private int objIndexDirDepth;

    public static final String DATA = "Data";
    public static final String VERSION = "Version";
    public static final String CONFIG = "Config";

    public static AgentConfig getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static final AgentConfig INSTANCE = new AgentConfig();
    }

    private AgentConfig() {
        String path = System.getProperty("configure");
		if (path == null) {
			path = System.getProperty(Constants.AGENT_CONF_KEY) + File.separator + Constants.AGENT_CONFIG_FILE;
		}

        properties = new Properties();
        try (InputStream myis = new FileInputStream(path)) {
            properties.load(myis);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(Constants.LOG_CONFIG_NOT_EXIST);
        } catch (IOException e) {
            throw new IllegalArgumentException(Constants.LOG_CONFIG_FAILED_LOADING);
        }
    }

    public void configure() {
        portalIp = properties.getProperty(Constants.AGENT_PROPERTY_PORTAL_HOST);
        if (portalIp == null) {
            throw new IllegalArgumentException(
					Constants.LOG_CONFIG_MUST_CONTAIN +
					Constants.AGENT_PROPERTY_PORTAL_HOST);
        }
		portalPort = properties.getProperty(Constants.AGENT_PROPERTY_PORTAL_PORT);
        if (portalPort == null) {
            throw new IllegalArgumentException(
					Constants.LOG_CONFIG_MUST_CONTAIN +
					Constants.AGENT_PROPERTY_PORTAL_PORT);
        }
        portalKey = properties.getProperty(Constants.AGENT_POOPERTY_POTAL_KEY);
        if (portalKey == null) {
            throw new IllegalArgumentException(
					Constants.LOG_CONFIG_MUST_CONTAIN +
					Constants.AGENT_POOPERTY_POTAL_KEY);
        }
        mqHost = properties.getProperty(Constants.AGENT_PROPERTY_MQ_HOST);
        if (mqHost == null) {
            throw new IllegalArgumentException(
					Constants.LOG_CONFIG_MUST_CONTAIN +
					Constants.AGENT_PROPERTY_MQ_HOST);
        }
        mqPort = properties.getProperty(Constants.AGENT_PROPERTY_MQ_PORT);
        if (mqPort == null) {
            throw new IllegalArgumentException(
					Constants.LOG_CONFIG_MUST_CONTAIN +
					Constants.AGENT_PROPERTY_MQ_PORT);
        }
        serverId = properties.getProperty(Constants.AGENT_PROPERTY_SERVER_ID);
        if (serverId == null) {
            throw new IllegalArgumentException(
					Constants.LOG_CONFIG_MUST_CONTAIN +
					Constants.AGENT_PROPERTY_SERVER_ID);
        }
        mqUser = properties.getProperty(Constants.AGENT_PROPERTY_MQ_USER);
        if (mqUser == null) {
            throw new IllegalArgumentException(
					Constants.LOG_CONFIG_MUST_CONTAIN +
					Constants.AGENT_PROPERTY_MQ_USER);
        }
        mqPassword = properties.getProperty(Constants.AGENT_PROPERTY_MQ_PASSWORD);
        if (mqPassword == null) {
            throw new IllegalArgumentException(
					Constants.LOG_CONFIG_MUST_CONTAIN +
					Constants.AGENT_PROPERTY_MQ_PASSWORD);
        }
        String interval = properties.getProperty(Constants.AGENT_PROPERTY_SERVICE_MONITOR_INTERVAL);
        if (interval == null) {
            throw new IllegalArgumentException(
					Constants.LOG_CONFIG_MUST_CONTAIN +
					Constants.AGENT_PROPERTY_SERVICE_MONITOR_INTERVAL);
        } else {
            serviceMonitorInterval = Long.parseLong(interval);
        }
        multipartUploadMethod = properties.getProperty(Constants.AGENT_PROPERTY_MULTIPART_UPLOAD_METHOD);
        if (multipartUploadMethod == null) {
            multipartUploadMethod = Constants.MULTIPART_UPLOAD_MERGE;
        }
        String depth = properties.getProperty(Constants.AGENT_PROPERTY_OBJ_INDEX_DIR_DEPTH);
        if (depth == null) {
            objIndexDirDepth = Constants.OBJECT_INDEX_DIR_DEPTH_2;
        } else {
            objIndexDirDepth = Integer.parseInt(depth);
            if (objIndexDirDepth < Constants.OBJECT_INDEX_DIR_DEPTH_2 || objIndexDirDepth > Constants.OBJECT_INDEX_DIR_DEPTH_3) {
                objIndexDirDepth = Constants.OBJECT_INDEX_DIR_DEPTH_2;
            }
        }
    }

    public String getPortalIp() {
        return portalIp;
    }

    public String getPortalPort() {
        return portalPort;
    }

    public String getPortalKey() {
        return portalKey;
    }

    public String getMQPort() {
        return mqPort;
    }

    public String getServerId() {
        return serverId;
    }

    public String getMQUser() {
        return mqUser;
    }

    public String getMQPassword() {
        return mqPassword;
    }

    public String getMQHost() {
        return mqHost;
    }

    public long getServiceMonitorInterval() {
        return serviceMonitorInterval;
    }

    public String getMultipartUploadMethod() {
        return multipartUploadMethod;
    }

    public int getObjIndexDirDepth() {
        return objIndexDirDepth;
    }
}
