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
package com.pspace.ifs.ksan.gw.object.osdclient;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.pspace.ifs.ksan.gw.utils.GWConfig;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.osd.DISKPOOLLIST;
import com.pspace.ifs.ksan.osd.OSDConstants;
import com.pspace.ifs.ksan.osd.DISKPOOLLIST.DISKPOOL.SERVER;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OSDClientManager {
    private static Logger logger;
    private static DISKPOOLLIST diskpoolList;
    private static Map<String, OSDClientPool> pools = new HashMap<>();
    
    public static OSDClientManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static OSDClientManager INSTANCE = new OSDClientManager();
    }

    private OSDClientManager() {
        logger = LoggerFactory.getLogger(OSDClientManager.class);
        try {
            logger.debug(GWConstants.LOG_OSDCLIENT_MANAGER_DISKPOOLS_CONFIGURE);
			XmlMapper xmlMapper = new XmlMapper();
			InputStream is = new FileInputStream(OSDConstants.DISKPOOL_CONF_PATH);
			byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
			try {
				is.read(buffer, 0, OSDConstants.MAXBUFSIZE);
                is.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
			String xml = new String(buffer);
			
			diskpoolList = xmlMapper.readValue(xml, DISKPOOLLIST.class);
			logger.debug(GWConstants.LOG_OSDCLIENT_MANAGER_DISKPOOL_INFO, diskpoolList.getDiskpool().getId(), diskpoolList.getDiskpool().getName());
			logger.debug(GWConstants.LOG_OSDCLIENT_MANAGER_SERVER_SIZE, diskpoolList.getDiskpool().getServers().size());
		} catch (JsonProcessingException | FileNotFoundException e) {
			logger.error(e.getMessage());
		}
    }

    public void init(int port, int osdClientCount) throws Exception {
        logger.debug(GWConstants.LOG_OSDCLIENT_MANAGER_CLIENT_COUNT, osdClientCount);
        GenericObjectPoolConfig  config = new GenericObjectPoolConfig();
        config.setTestOnReturn(true);
        config.setMaxTotal(osdClientCount);

        for (SERVER server : diskpoolList.getDiskpool().getServers()) {
            if (!GWUtils.getLocalIP().equals(server.getIp())) {
                logger.debug(GWConstants.LOG_OSDCLIENT_MANAGER_OSD_SERVER_IP, server.getIp());
                OSDClientFactory factory = new OSDClientFactory(server.getIp(), port);
                OSDClientPool pool = new OSDClientPool(factory, config);
                pool.preparePool();
                pools.put(server.getIp(), pool);
            }
        }
    }

    public void shutDown() {
        for (SERVER server : diskpoolList.getDiskpool().getServers()) {
            pools.remove(server.getIp());
        }
    }

    public OSDClient getOSDClient(String host) throws Exception {
        return pools.get(host).borrowObject();
    }

    public void returnOSDClient(OSDClient client) throws Exception {
        pools.get(client.getHost()).returnObject(client);
    }
}
