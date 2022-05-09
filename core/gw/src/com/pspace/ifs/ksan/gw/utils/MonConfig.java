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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonConfig {
    private Properties properties;

    private String portalIp;
    private String portalPort;
    private String serverId;
    private String portalKey = "5de46d7ccd5d0954fad7d11ffc22a417e2784cbedd9f1dae3992a46e97b367e8";
    private String mqPort;

    private static final Logger logger = LoggerFactory.getLogger(MonConfig.class);

    public MonConfig() {
        String path = System.getProperty("configure");
		if (path == null) {
			path = GWConstants.KMON_CONFIG_PATH;
		}
        logger.info(path);
        properties = new Properties();
        try (InputStream myis = new FileInputStream(path)) {
            properties.load(myis);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(GWConstants.LOG_CONFIG_NOT_EXIST);
        } catch (IOException e) {
            throw new IllegalArgumentException(GWConstants.LOG_CONFIG_FAILED_LOADING);
        }
    }

    public void configure() {
        portalIp = properties.getProperty(GWConstants.KMON_PROPERTY_PORTAL_IP);
        if (portalIp == null) {
            throw new IllegalArgumentException(
					GWConstants.LOG_CONFIG_MUST_CONTAIN +
					GWConstants.KMON_PROPERTY_PORTAL_IP);
        }
		portalPort = properties.getProperty(GWConstants.KMON_PROPERTY_PORTAL_PORT);
        if (portalPort == null) {
            throw new IllegalArgumentException(
					GWConstants.LOG_CONFIG_MUST_CONTAIN +
					GWConstants.KMON_PROPERTY_PORTAL_PORT);
        }
        // String portalKey = properties.getProperty(GWConstants.KMON_POOPERTY_POTAL_KEY);
        // if (portalKey == null) {
        //     throw new IllegalArgumentException(
		// 			GWConstants.LOG_CONFIG_MUST_CONTAIN +
		// 			GWConstants.KMON_POOPERTY_POTAL_KEY);
        // }
        mqPort = properties.getProperty(GWConstants.KMON_PROPERTY_MQ_PORT);
        if (mqPort == null) {
            throw new IllegalArgumentException(
					GWConstants.LOG_CONFIG_MUST_CONTAIN +
					GWConstants.KMON_PROPERTY_MQ_PORT);
        }
        serverId = properties.getProperty(GWConstants.KMON_PROPERTY_SERVER_ID);
        if (serverId == null) {
            throw new IllegalArgumentException(
					GWConstants.LOG_CONFIG_MUST_CONTAIN +
					GWConstants.KMON_PROPERTY_SERVER_ID);
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

    public String getMqPort() {
        return mqPort;
    }

    public String getServerId() {
        return serverId;
    }
}
