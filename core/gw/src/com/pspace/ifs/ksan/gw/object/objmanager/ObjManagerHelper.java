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
package com.pspace.ifs.ksan.gw.object.objmanager;

import java.util.Set;

import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.objmanager.ObjManager;

import org.apache.commons.pool2.impl.DefaultPooledObjectInfo;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjManagerHelper {
    private static Logger logger;
    private static ObjManagerFactory factory;
    private static ObjManagerPool pool;
    
    public static ObjManagerHelper getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static ObjManagerHelper INSTANCE = new ObjManagerHelper();
    }

    private ObjManagerHelper() {
        logger = LoggerFactory.getLogger(ObjManagerHelper.class);
    }

    public void init(int count) throws Exception {
        logger.debug(GWConstants.LOG_OBJMANAGER_COUNT, count);
        GenericObjectPoolConfig  config = new GenericObjectPoolConfig();
        config.setMinIdle(count);
        config.setMaxIdle(count);
        config.setMaxTotal(count);
        factory = new ObjManagerFactory();
        pool = new ObjManagerPool(factory, config);
        pool.preparePool();
    }

    public void shutDown() {
        pool.close();
    }

    public ObjManager getObjManager() throws Exception {
        try {
            return pool.borrowObject();
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    public void returnObjManager(ObjManager objManager) throws Exception {
        pool.returnObject(objManager);
    }

    public static void updateAllConfig() {
        factory.notifyChangeConfig();
    }

    public static void updateAllDiskpools() {
        factory.notifyChangeDiskpools();
    }
}
