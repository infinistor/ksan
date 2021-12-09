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

import java.sql.SQLException;
import java.util.List;

import com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

/**
 *
 * @author legesse
 */
public class ObjManagerUtil {
    private DataRepository dbm;
    private DiskAllocation dAlloc;
    private ObjManagerCache  obmCache;
    private ObjManagerConfig config;
    //private DiskMonitor diskM;
    //private MQSender mqSender;
    private OMLogger logger;
    
    public ObjManagerUtil() throws Exception{
            
            config = new ObjManagerConfig();
            
            obmCache = new ObjManagerCache();
          
            //System.out.println(">>dbmCache : " + obmCache);
            if (config.dbRepository.equalsIgnoreCase("MYSQL"))
                 dbm = new MysqlDataRepository(obmCache, config.dbHost, config.dbUsername, config.dbPassword, config.dbName);
            else if(config.dbRepository.equalsIgnoreCase("MONGO"))
                 dbm = new MongoDataRepository(obmCache, config.dbHost, config.dbUsername, config.dbPassword, config.dbName, 27017);
            else 
                System.out.println("ObjManger initalization error :  there is no db storage configured!");
            
            //System.out.println(">>dbm : " + dbm);
            config.loadDiskPools(obmCache);
           
            dbm.loadBucketList();
            
            //obmCache.displayBucketList();
            
            //obmCache.displayDiskPoolList();
            
            dAlloc = new DiskAllocation(obmCache);
            
            logger = new OMLogger(ObjManager.class.getName());
    }
    
    public Metadata getObject(String bucketName, String objid) throws ResourceNotFoundException{
        return dbm.selectSingleObjectWithObjId(bucketName, objid);
    }
    
    public Metadata getObjectWithPath(String bucketName, String key) throws ResourceNotFoundException{
        return dbm.selectSingleObject(bucketName, key);
    }
    
    public int updateObject(String bucketName, String objid, Metadata mt, int updateCtrl){
        int ret = 0;
        
        if (updateCtrl == 1)
            ret = dbm.updateDisks(mt);
        else if (updateCtrl == 2)
            ret= dbm.updateSizeTime(mt);
        
        return ret;
    }
    
    public List<Metadata> listObjects(String bucketName, String diskid, int listCtrl, long offset, long numObjects){
        ListObject lo = new ListObject(dbm, obmCache, bucketName, diskid, offset, (int)numObjects);
        return lo.excute1();
    }
    /**
     * It will allocate a replica disk for recovery of failed replica object
     * @param bucketName   bucket name
     * @param dpath        primary disk path 
     * @param diskid       primary disk disk Id number
     * @return new DISK object
     * @throws ResourceNotFoundException if there is no server or disk available
     * @throws AllServiceOfflineException if all server are offline 
     *                                   or if all DISK are not Good state
     */
    public DISK allocReplicaDisk(String bucketName, String dpath, String diskid) throws ResourceNotFoundException, AllServiceOfflineException{
        DISK primary = new DISK();
        
        if (dpath == null && diskid == null)
            return null;

        if (dpath != null && diskid != null){
            if (dpath.isEmpty() && diskid.isEmpty())
                return null;
        }
        
        primary.setPath(dpath);
        primary.setId(diskid);
        String dskPoolId = obmCache.getBucketFromCache(bucketName).getDiskPoolId();
        return dAlloc.allocDisk(dskPoolId, primary);
    }
    
    public boolean allowedToReplicate(String bucketName, DISK primary,  DISK replica, String DstDiskId){
        String dskPoolId = obmCache.getBucketFromCache(bucketName).getDiskPoolId();
        if (dskPoolId == null)
            return false;
        
        return dAlloc.isReplicationAllowedInDisk(dskPoolId, primary, replica, DstDiskId);
    }
    /**
     * Replace replica disk with new one after recovery
     * @param bucketName  bucket name
     * @param objid       object id
     * @param pdiskid     primary disk id
     * @param rdiskid     new replica disk id
     * @return -1 if it is failed, 0 if nothing is updated or 1 if it is successful
     * @throws ResourceNotFoundException if the disk provided is not valid or exist in the system
     */
    public int replaceDisk(String bucketName, String objid, String pdiskid, String rdiskid) throws ResourceNotFoundException{
        String diskpoolid;
        DISK primary;
        DISK replica;
        diskpoolid = obmCache.getBucketFromCache(bucketName).getDiskPoolId();
        primary = obmCache.getDiskWithId(diskpoolid, pdiskid);
        replica = obmCache.getDiskWithId(diskpoolid, rdiskid);
        System.out.println(" PRIMARY > " + pdiskid + ", " + primary + " REPLICA >" + rdiskid + " ," + replica);
        Metadata md = new Metadata();
        md.setObjid(objid);
        md.setPrimaryDisk(primary);
        md.setReplicaDISK(replica);
        return dbm.updateDisks(md);
    }
    
    public List<Bucket> getExistedBucketList(){
        return dbm.getBucketList();
    }
    
    public List<String> getExistedDiskList(){
        try {
            return dbm.getAllUsedDiskId();
        } catch (SQLException ex) {
            return null;
        }
    }
}
