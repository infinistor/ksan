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

import java.net.Socket;
import java.io.IOException;
import java.net.UnknownHostException;

import com.pspace.ifs.ksan.libs.osd.OSDClient;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OSDClientPool2 {
    private final ObjectPool<OSDClient> pool;
    
    public OSDClientPool2(String host, int port, int size) {
        OSDClientFactory factory = new OSDClientFactory(host, port);
        GenericObjectPoolConfig<OSDClient> config = new GenericObjectPoolConfig<OSDClient>();
        config.setMaxTotal(size);
        config.setMaxIdle(size);
        config.setMinIdle(0);
        config.setBlockWhenExhausted(true);
        // config.setTestOnBorrow(true);
        // config.setTestOnReturn(true);
        // config.setTestWhileIdle(true);
        // config.setMinEvictableIdleTimeMillis(60000);
        // config.setTimeBetweenEvictionRunsMillis(30000);
        // config.setNumTestsPerEvictionRun(-1);
        pool = new GenericObjectPool<OSDClient>(factory, config);
    }

    public OSDClient getOsdClient() throws Exception {
        return pool.borrowObject();
    }

    public void returnOsdClient(OSDClient osdClient) throws Exception {
        pool.returnObject(osdClient);
    }

    public void close() {
        pool.close();
    }
}
