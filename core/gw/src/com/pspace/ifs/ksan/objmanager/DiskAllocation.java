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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
//import java.util.logging.Level;
//import java.util.logging.Logger;
/**
 *
 * @author legesse
 */

public class DiskAllocation {
     private final ObjManagerCache obmCache;
     private static Logger logger;
     
     
    public DiskAllocation(ObjManagerCache omCache){
         obmCache =omCache;
         logger = LoggerFactory.getLogger(DiskAllocation.class);
    }
     
    private SERVER allocPrimaryServer(int algorithm, DISKPOOL dskPool) 
            throws ResourceNotFoundException, AllServiceOfflineException{
         
         if (algorithm == AllocAlgorithm.ROUNDROBIN)
            return dskPool.getNextServer();//.getNextDisk();
         
         if (algorithm == AllocAlgorithm.LOCALPRIMARY)
             return dskPool.getLocalServer();//.getNextDisk();
         
        logger.error("Allocation algorithm is not defined or set!");
         throw new ResourceNotFoundException("Allocation algorithm is not defined or set!");  
     }
     
    private DISK allocReplicaDisk(DISKPOOL dskPool, SERVER primary) throws ResourceNotFoundException, AllServiceOfflineException{
         SERVER replica;
         DISK dsk;
         
         replica = dskPool.getNextServer();
         while(replica.equals(primary)){
             replica = dskPool.getNextServer();
             if (replica.equals(primary)){
                logger.error("there is no osd server for replica allocation!");
                throw new ResourceNotFoundException("there is no osd server for replica allocation!");
             }
         } 
         dsk = replica.getNextDisk();
         dsk.setOSDIP(replica.getName());
         return dsk;
    }
     
    public int allocDisk(Metadata md, String dskPoolId, int algorithm) 
             throws IOException, AllServiceOfflineException{
         DISK primaryDisk;
         DISK replicaDisk;
         DISKPOOL dskPool;
         try{
             logger.debug("disk pool id : {}", dskPoolId);
             dskPool = obmCache.getDiskPoolFromCache(dskPoolId);
             if (dskPool == null) {
                logger.error("there is no bucket in the system!");
                throw new ResourceNotFoundException("there is no bucket in the system!");
             }
             
             SERVER primary = this.allocPrimaryServer(algorithm, dskPool);
             primaryDisk = primary.getNextDisk();
             primaryDisk.setOSDIP(primary.getName());
             md.setPrimaryDisk(primaryDisk);
             try{
                replicaDisk = this.allocReplicaDisk(dskPool, primary);
                md.setReplicaDISK(replicaDisk);
             } catch(ResourceNotFoundException e){
                //replicaDisk = new DISK();
                logger.error("Replica disk not allocated!");
                // System.out.println(">>Replica disk not allocated!");
             }
         } catch(ResourceNotFoundException e){
            logger.error(e.getMessage());
             throw new IOException(e.getMessage());//"No disk in the system!"
         }
         return 0;
    }
    
    // allocate only replica disk
    public DISK allocDisk(String dskPoolId, DISK primary) throws ResourceNotFoundException, AllServiceOfflineException{
        DISK replicaDisk;
        DISKPOOL dskPool;
        SERVER svr;
        
        logger.debug("disk pool id : {}", dskPoolId);
        dskPool = obmCache.getDiskPoolFromCache(dskPoolId);
        if (dskPool == null) {
            logger.error("there is no bucket in the system!");
            throw new ResourceNotFoundException("there is no bucket in the system!");
        }
        svr = dskPool.getServer(primary.getPath(), primary.getId());
        replicaDisk = this.allocReplicaDisk(dskPool, svr);
        replicaDisk.setOSDIP(svr.getName());
        return replicaDisk;
    }
    
    public boolean isReplicationAllowedInDisk(String dskPoolId, DISK primary, DISK replica, String replicaDiskId){
        DISKPOOL dskPool;
        SERVER psvr;
        SERVER rsvr;
        SERVER rsvr2;
        
        try {
            dskPool = obmCache.getDiskPoolFromCache(dskPoolId);
            if (dskPool == null)
                return false;
             
            rsvr2 = dskPool.getServer("", replicaDiskId);
            if (rsvr2 == null)
                return false;
            
            psvr = dskPool.getServer(primary.getPath(), primary.getId());
            if (replica != null){
                rsvr = dskPool.getServer(replica.getPath(), replica.getId());
                return !(rsvr2.getId().equals(psvr.getId())) && !(rsvr2.getId().equals(psvr.getId()));
            }
            return !(rsvr2.getId().equals(psvr.getId()));
                
         } catch (ResourceNotFoundException ex) {
             return false;
         }
    }
}
