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
package com.pspace.ifs.ksan.libs.osd;

import java.util.HashMap;
import java.util.Map;

import com.pspace.ifs.ksan.libs.KsanUtils;
import com.pspace.ifs.ksan.libs.DiskManager;
import com.pspace.ifs.ksan.libs.disk.DiskPool;
import com.pspace.ifs.ksan.libs.disk.Server;
import com.pspace.ifs.ksan.libs.osd.OSDClient;
import com.pspace.ifs.ksan.libs.osd.OSDClientPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OSDClientManager {
    private static Logger logger = LoggerFactory.getLogger(OSDClientManager.class);
    private static Map<String, OSDClientPool> pools = new HashMap<String, OSDClientPool>();

    public static OSDClientManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static OSDClientManager INSTANCE = new OSDClientManager();
    }

    private OSDClientManager() {
    }

    public void init(int port, int osdPerCount) {
        logger.debug("osd client count : {}", osdPerCount);
        for (DiskPool diskpool : DiskManager.getInstance().getDiskPoolList()) {
            for (Server server : diskpool.getServerList()) {
                logger.debug("server ip : {}, server status : {}", server.getIp(), server.getStatus());
                if (server.getStatus().equalsIgnoreCase(Server.STATUS_ONLINE) 
                  && !KsanUtils.getLocalIP().equals(server.getIp())
                  && !pools.containsKey(server.getIp())) {
                    logger.debug("add osd client : {}, {}", server.getIp(), osdPerCount);
                    OSDClientPool pool = new OSDClientPool(server.getIp(), port, osdPerCount);
                    pools.put(server.getIp(), pool);
                }
            }
        }
    }

    public void update(int port, int osdPerCount) {
        logger.debug("osd client count : {}", osdPerCount);
        pools.clear();
        for (DiskPool diskpool : DiskManager.getInstance().getDiskPoolList()) {
            for (Server server : diskpool.getServerList()) {
                logger.debug("server ip : {}, server status : {}", server.getIp(), server.getStatus());
                if (server.getStatus().equalsIgnoreCase(Server.STATUS_ONLINE)) {
                    if (!KsanUtils.getLocalIP().equals(server.getIp())
                        && !pools.containsKey(server.getIp())) {
                        logger.debug("add osd client : {}, {}", server.getIp(), osdPerCount);
                        OSDClientPool pool = new OSDClientPool(server.getIp(), port, osdPerCount);
                        pools.put(server.getIp(), pool);
                    }
                } 
            }
        }
    }

    public OSDClient getOSDClient(String ip) throws Exception {
        if (pools.containsKey(ip)) {
            OSDClientPool pool = pools.get(ip);
            return pool.getOsdClient();
        }

        return null;
    }

    public void releaseOSDClient(OSDClient client) throws Exception {
        if (pools.containsKey(client.getHost())) {
            OSDClientPool pool = pools.get(client.getHost());
            pool.returnOsdClient(client);
        }
    }

    public void close() {
        for (OSDClientPool pool : pools.values()) {
            pool.close();
        }
    }
}
