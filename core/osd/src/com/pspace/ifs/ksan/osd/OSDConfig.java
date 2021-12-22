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

package com.pspace.ifs.ksan.osd;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Properties;

public class OSDConfig {
    private Properties properties;
    private String poolSize;
    private String port;
    private String ip;
    private String replicaPort;
    private String ecScheduleMinutes;
    private String ecApplyMinutes;
    private String ecFileSize;
    
    public OSDConfig(String path) {
        properties = new Properties();
		try (InputStream myis = new FileInputStream(path)) {
			properties.load(myis);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    public void configure() throws URISyntaxException {
        poolSize = properties.getProperty(OSDConstants.POOL_SIZE);
        ip = properties.getProperty(OSDConstants.OSD_LOCAL_IP);
        port = properties.getProperty(OSDConstants.OSD_PORT);
        ecScheduleMinutes = properties.getProperty(OSDConstants.EC_SCHEDULE_MINUTES);
        ecApplyMinutes = properties.getProperty(OSDConstants.EC_APPLY_MINUTES);
        ecFileSize = properties.getProperty(OSDConstants.EC_FILE_SIZE);
    }

    public String getPoolSize() {
        return poolSize;
    }

    public String getPort() {
        return port;
    }

    public String getIP() {
        return ip;
    }

    public String getReplicaPort() {
        return replicaPort;
    }

    public String getECScheduleMinutes() {
        return ecScheduleMinutes;
    }

    public String getECApplyMinutes() {
        return ecApplyMinutes;
    }

    public String getECFileSize() {
        return ecFileSize;
    }
}
