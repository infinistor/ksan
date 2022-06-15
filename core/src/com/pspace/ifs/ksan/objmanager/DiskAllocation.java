/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version
* 3 of the License. See LICENSE for details
*
* All materials such as this program, related source codes, and documents are provided as they are.
* Developers and developers of the KSAN project are not responsible for the results of using this program.
* The KSAN development team has the right to change the LICENSE method for all outcomes related to KSAN development without prior notice, permission, or consent.
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
     
    private DISK allocReplicaDisk(DISKPOOL dskPool, SERVER primary, SERVER replica1) throws ResourceNotFoundException, AllServiceOfflineException{
         SERVER replica;
         DISK dsk;
         int numRoatate = 0;
         
         do{
            replica = dskPool.getNextServer();
            if (replica == null && numRoatate > 2){
                logger.error("there is no osd server for replica allocation!");
                throw new ResourceNotFoundException("there is no osd server for replica allocation!");
            }
            
            if (replica.equals(primary)){
                numRoatate++;
                continue;
            }
            
            if (replica1 != null){
                if (replica.equals(replica1)){
                    numRoatate++;
                    continue;
                }
            }
            break;
         }while(true);
         /*replica = dskPool.getNextServer();
         while(replica.equals(primary)){
             replica = dskPool.getNextServer();
             if (replica.equals(primary)){
                logger.error("there is no osd server for replica allocation!");
                throw new ResourceNotFoundException("there is no osd server for replica allocation!");
             }
         } */
         dsk = replica.getNextDisk();
         dsk.setOSDIP(replica.getName());
         dsk.setOSDServerId(replica.getId());
         return dsk;
    }
    
    private DISKPOOL getDiskPool(String dskPoolId) throws ResourceNotFoundException{
       DISKPOOL dskPool; 
       try  {
           dskPool = obmCache.getDiskPoolFromCache(dskPoolId);
       } catch(ResourceNotFoundException ex){
           obmCache.loadDiskPools();
           //System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
           //obmCache.displayDiskPoolList();
           //System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
           dskPool = obmCache.getDiskPoolFromCache(dskPoolId);
           if(dskPool != null)
               System.out.println(">>>Get disk pool is fixed!!!!!");
       }
       return dskPool;
    }
    
    public int allocDisk(Metadata md, String dskPoolId, int replicaCount, int algorithm) 
             throws IOException, AllServiceOfflineException{
         DISK primaryDisk;
         DISK replicaDisk;
         DISKPOOL dskPool;
         try{
             logger.debug("disk pool id : {}", dskPoolId);
             dskPool = getDiskPool(dskPoolId);
             if (dskPool == null) {
                logger.error("there is no diskpool in the system!");
                throw new ResourceNotFoundException("there is no diskpool in the system!");
             }
             
             SERVER primary = this.allocPrimaryServer(algorithm, dskPool);
             primaryDisk = primary.getNextDisk();
             primaryDisk.setOSDIP(primary.getName());
             primaryDisk.setOSDServerId(primary.getId());
             md.setPrimaryDisk(primaryDisk);
             md.setReplicaCount(replicaCount);
             if (replicaCount == 1){
                 return 0;
             }
             
             try{
                replicaDisk = this.allocReplicaDisk(dskPool, primary, null);
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
    public DISK allocDisk(String dskPoolId, DISK primary, DISK replica1) throws ResourceNotFoundException, AllServiceOfflineException{
        DISK replicaDisk;
        DISKPOOL dskPool;
        SERVER svr;
        SERVER rsvr = null;
        
        logger.debug("disk pool id : {}", dskPoolId);
        dskPool = getDiskPool(dskPoolId);
        if (dskPool == null) {
            logger.error("there is no bucket in the system!");
            throw new ResourceNotFoundException("there is no bucket in the system!");
        }
        svr = dskPool.getServer(primary.getPath(), primary.getId());
        if (replica1 != null)
            rsvr = dskPool.getServer(replica1.getPath(), replica1.getId());
        
        replicaDisk = this.allocReplicaDisk(dskPool, svr, rsvr);
        replicaDisk.setOSDIP(svr.getName());
        replicaDisk.setOSDServerId(svr.getId());
        return replicaDisk;
    }
    
    public boolean isReplicationAllowedInDisk(String dskPoolId, DISK primary, DISK replica, String replicaDiskId){
        DISKPOOL dskPool;
        SERVER psvr;
        SERVER rsvr;
        SERVER rsvr2;
        
        try {
            dskPool = getDiskPool(dskPoolId);
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
