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
package com.pspace.ifs.ksan.objmanager;

/**
 *
 * @author legesse
 */
public class ObjManagerSharedResource {
    
    private ObjManagerCache obmCache = null;
    private ObjManagerConfig config = null;
    private DiskMonitor diskM = null;
    private DataRepository dbm = null;
    
    private ObjManagerSharedResource(){}
    
    private void init(ObjManagerConfig config, boolean dumpCache) throws Exception{
        if (this.config == null){
            this.config = config;
            obmCache = new ObjManagerCache();
            
            dbm = new DataRepositoryLoader(config, obmCache).getDataRepository();

            obmCache.setDBManager(dbm);
            
            dbm.loadBucketList();
            
            if (dumpCache) 
                this.config.loadDiskPools(obmCache);
            
            diskM = new DiskMonitor(obmCache, config.mqHost, config.mqQueeuname, config.mqExchangename);
        }
    }
    
    public static ObjManagerSharedResource getInstance(ObjManagerConfig config, boolean dumpCache) throws Exception {
       ObjManagerSharedResource instance = ObjManagerSharedResourceHolder.INSTANCE;
       instance.init(config, dumpCache);
       return instance;
    }
    
    private static class ObjManagerSharedResourceHolder {
        private static final ObjManagerSharedResource INSTANCE = new ObjManagerSharedResource();
    }
    
    public DiskMonitor getDiskMonitor(){
        return diskM;
    }
    
    public ObjManagerCache getCache(){
        return obmCache;
    }

}
