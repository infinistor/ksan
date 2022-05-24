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

import java.util.ArrayList;
import java.util.List;

import com.pspace.ifs.ksan.objmanager.ObjManager;
import com.pspace.ifs.ksan.objmanager.ObjManagerConfig;
import java.io.IOException;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class ObjManagerFactory implements PooledObjectFactory <ObjManager> {
    private ObjManagerConfig config;
    private List<ObjManager> objManagerList = new ArrayList<ObjManager>();
    public ObjManagerFactory() throws IOException {
        // this is temporatry and it is read from the config file
        config = new ObjManagerConfig();
        // replace with this after you got information from portal
        /*config = new ObjManagerConfig(String dbRepository, String dbHost, 
                        long dbport, String dbName, String dbUsername, 
                        String dbPassword, String mqHost, String mqQueeuname, 
                        String mqExchangename, String mqOsdExchangename);*/
    }

    private ObjManager create() throws Exception {
        ObjManager objManager = new ObjManager(config);
        objManagerList.add(objManager);
        return objManager;
    }

    private PooledObject<ObjManager> wrap(ObjManager objManager) throws Exception {
        return new DefaultPooledObject<>(objManager);
    }

    @Override
    public void activateObject(PooledObject<ObjManager> p) throws Exception {
        p.getObject().activate();
    }

    @Override
    public void destroyObject(PooledObject<ObjManager> p) throws Exception {
        p.getObject().close();
    }

    @Override
    public PooledObject<ObjManager> makeObject() throws Exception {
        return wrap(create());
    }

    @Override
    public void passivateObject(PooledObject<ObjManager> p) throws Exception {
        p.getObject().deactivate();
    }

    @Override
    public boolean validateObject(PooledObject<ObjManager> p) {
        return p.getObject().isValid();
    }
    
    public void notifyChangeConfig() {
        objManagerList.forEach(objManager -> objManager.updateConfig());
    }

    public void notifyChangeDiskpools(String routingKey) {
        objManagerList.forEach(objManager -> objManager.updateDiskpools());
    }
}
