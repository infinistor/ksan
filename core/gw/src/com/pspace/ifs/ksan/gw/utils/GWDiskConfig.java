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
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.pspace.ifs.ksan.gw.utils.DISKPOOLLIST.DISKPOOL.SERVER;
import com.pspace.ifs.ksan.gw.utils.DISKPOOLLIST.DISKPOOL.SERVER.DISK;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GWDiskConfig {
    private static final Logger logger = LoggerFactory.getLogger(GWDiskConfig.class);
    private DISKPOOLLIST diskPoolList;
    private String localHost;
    private List<String> diskList = new ArrayList<String>();
    private List<String> pathList = new ArrayList<String>();

    public static GWDiskConfig getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static GWDiskConfig INSTANCE = new GWDiskConfig();
    }
	
	private GWDiskConfig() {
        try {
			XmlMapper xmlMapper = new XmlMapper();
			InputStream is = new FileInputStream(GWConstants.DISKPOOL_CONF_PATH);
			byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
			try {
				is.read(buffer, 0, GWConstants.MAXBUFSIZE);
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
			String xml = new String(buffer);
			
			logger.debug(xml);
			diskPoolList = xmlMapper.readValue(xml, DISKPOOLLIST.class);
		} catch (JsonProcessingException | FileNotFoundException e) {
			PrintStack.logging(logger, e);
            System.exit(-1);
		}

        localHost = GWUtils.getLocalIP();
        for (SERVER server : diskPoolList.getDiskpool().getServers()) {
            if (localHost.equals(server.getIp())) {
                for (DISK disk : server.getDisks()) {
                    diskList.add(disk.getId());
                    pathList.add(disk.getPath());
                }
            }
        }
    }

    public String getLocalDiskID() {
        if (diskList.size() > 0) {
            return diskList.get(0);
        }
        return null;
    }

    public String getLocalPath() {
        if (pathList.size() > 0) {
            return pathList.get(0);
        }
        return null;
    }

    public String getPath(String diskID) {
        for (SERVER server : diskPoolList.getDiskpool().getServers()) {
            for (DISK disk : server.getDisks()) {
                if (diskID.equals(disk.getId())) {
                    return disk.getPath();
                }
            }
        }

        return null;
    }

    public String getOSDIP(String diskID) {
        for (SERVER server : diskPoolList.getDiskpool().getServers()) {
            for (DISK disk : server.getDisks()) {
                if (diskID.equals(disk.getId())) {
                    return server.getIp();
                }
            }
        }

        return null;
    }
}
