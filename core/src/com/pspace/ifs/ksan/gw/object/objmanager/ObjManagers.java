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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.ifs.ksan.gw.utils.ObjectManagerConfig;
import com.pspace.ifs.ksan.objmanager.ObjManager;
import com.pspace.ifs.ksan.objmanager.ObjManagerConfig;
import com.pspace.ifs.ksan.gw.utils.GWConfig;

public class ObjManagers {
    private static final Logger logger = LoggerFactory.getLogger(ObjManagers.class);
    private ObjManagerConfig config;
    private ObjManager[] arrayObjManager = new ObjManager[(int)GWConfig.getInstance().getObjManagerCount()];
    private int index;

    public static ObjManagers getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static ObjManagers INSTANCE = new ObjManagers();
    }

    private ObjManagers() {
        try {
            config = new ObjManagerConfig(ObjectManagerConfig.getInstance().getDbRepository(),
                                        ObjectManagerConfig.getInstance().getDbHost(),
                                        ObjectManagerConfig.getInstance().getDbPort(),
                                        ObjectManagerConfig.getInstance().getDbName(),
                                        ObjectManagerConfig.getInstance().getDbUserName(),
                                        ObjectManagerConfig.getInstance().getDbPassword(),
                                        ObjectManagerConfig.getInstance().getMqHost(),
                                        ObjectManagerConfig.getInstance().getMqUser(),
                                        ObjectManagerConfig.getInstance().getMqPassword(),
                                        (long)ObjectManagerConfig.getInstance().getMqPort(),
                                        ObjectManagerConfig.getInstance().getMqQueueName(),
                                        ObjectManagerConfig.getInstance().getMqExchangeName(),
                                        ObjectManagerConfig.getInstance().getMqOsdExchangeName());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        
        index = -1;
    }

    public void init() throws Exception {
        for (int i = 0; i < 100; i++) {
            arrayObjManager[i] = new ObjManager(config);
        }
    }

    public synchronized ObjManager getObjManager() {
        if (index >= 99) {
            index = -1;
        }
        
        index++;
        
        if (arrayObjManager[index] == null) {
            arrayObjManager[index] = new ObjManager(config);
        }

        return arrayObjManager[index];
    }
}
