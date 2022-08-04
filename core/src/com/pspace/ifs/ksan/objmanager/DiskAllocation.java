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
import java.util.HashMap;
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
         
        logger.error("[allocPrimaryServer]Allocation algorithm is not defined or set!");
         throw new ResourceNotFoundException("Allocation algorithm is not defined or set!");  
    }
    
    private HashMap<String, String> getOSDDistanceMap(DISK primary, DISK replica){
        HashMap<String, String> osdDistanceMap = new HashMap();
        if (primary != null)
            osdDistanceMap.put(primary.getOsdIp(), primary.getId());
        
        if (replica != null){
            osdDistanceMap.put(replica.getOsdIp(), replica.getId());
            
        }
        //System.out.println("[OSDDistanceMap] " + osdDistanceMap.toString());
        return osdDistanceMap;
    }
    
    private HashMap<String, String> getOSDDistanceMap(Metadata mt){
        try {
            return getOSDDistanceMap(mt.getPrimaryDisk(), mt.getReplicaDisk());
        } catch (ResourceNotFoundException ex) {
            return getOSDDistanceMap(mt.getPrimaryDisk(), null);
        }
    }
    
    private DISK allocReplicaDisk(DISKPOOL dskPool, Metadata mt) throws ResourceNotFoundException, AllServiceOfflineException{
         SERVER replica;
         DISK dsk;
         int numRoatate = 0;
         HashMap<String, String> osdDistanceMap; 
         
         osdDistanceMap = getOSDDistanceMap(mt);
         do{
            replica = dskPool.getNextServer();
            if (replica == null && numRoatate > 2){
                logger.error("[allocReplicaDisk] There is no osd server for replica allocation!");
                throw new ResourceNotFoundException("there is no osd server for replica allocation!");
            }
            
            if (osdDistanceMap.containsKey(replica.getName())){
                numRoatate++;
                continue;
            }
           /*if (mt.isPrimaryExist()){
               
               if (mt.getPrimaryDisk().getOSDServerId().equals(replica.getId())){
                   numRoatate++;
                   continue;
               }
           }
           if (mt.isReplicaExist()){
               if (mt.getReplicaDisk().getOSDServerId().equals(replica.getId())){
                   numRoatate++;
                   continue;
               }
           }*/
 
            break;
         }while(true);
         dsk = replica.getNextDisk();
         dsk.setOSDIP(replica.getName());
         dsk.setOSDServerId(replica.getId());
         logger.debug("[ReplicaAllocation] bucket : {} key : {} versionId : {}  DiskId {} osdIp {} ", mt.getBucket(), mt.getPath(), mt.getVersionId(), dsk.getId(), replica.getName());
         return dsk;
    }
    
    private DISKPOOL getDiskPool(String dskPoolId) throws ResourceNotFoundException{
       DISKPOOL dskPool; 
       try  {
           dskPool = obmCache.getDiskPoolFromCache(dskPoolId);
       } catch(ResourceNotFoundException ex){
           obmCache.reloadDiskPoolList();
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
             //logger.debug("disk pool id : {}", dskPoolId);
             dskPool = getDiskPool(dskPoolId);
             if (dskPool == null) {
                logger.error("[allocDisk] There is no diskpool with Id {} for bucketName : {} key: {} objId in the system!", dskPoolId, md.getBucket(), md.getPath(), md.getObjId());
                throw new ResourceNotFoundException("[allocDisk] There is no diskpool in the system!");
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
                replicaDisk = this.allocReplicaDisk(dskPool, md);
                md.setReplicaDISK(replicaDisk);
             } catch(ResourceNotFoundException e){
                logger.error("[allocDisk]Replica disk not allocated for bucketName {} key {} objId {}!", md.getBucket(), md.getPath(), md.getObjId());
             }
         } catch(ResourceNotFoundException e){
            logger.error(e.getMessage());
             throw new IOException(e.getMessage());//"No disk in the system!"
         }
         return 0;
    }
    
    // allocate only replica disk
    public DISK allocDisk(String dskPoolId, Metadata mt) throws ResourceNotFoundException, AllServiceOfflineException{
        DISK replicaDisk;
        DISKPOOL dskPool;
        
        //logger.debug("disk pool id : {}", dskPoolId);
        dskPool = getDiskPool(dskPoolId);
        if (dskPool == null) {
            logger.error("[allocDisk]There is no diskpool with the Id "+ dskPoolId+"!");
            throw new ResourceNotFoundException("[allocDisk]There is no diskpool with the Id "+ dskPoolId+"!");
        }
        
        replicaDisk = this.allocReplicaDisk(dskPool, mt);
        return replicaDisk;
    }
    
    public boolean isReplicationAllowedInDisk(String dskPoolId, DISK primary, DISK replica, String replicaDiskId, boolean allowdToUseLocalDisk){
        DISKPOOL dskPool;
        SERVER rsvr;
        HashMap<String, String> osdDistanceMap; 
         
        try {
            if (replicaDiskId.isEmpty())
                return true;
            
            if (replicaDiskId.equals(primary.getId()))
                return false;
            
            if (replica != null){
                if (replicaDiskId.equals(replica.getId()))
                    return false;
            }
            
            dskPool = getDiskPool(dskPoolId);
            if (dskPool == null)
                return false;
             
            rsvr = dskPool.getServer("", replicaDiskId);
            if (rsvr == null)
                return false;
            
            
            osdDistanceMap = getOSDDistanceMap(primary, replica);
            if (allowdToUseLocalDisk){
                if (osdDistanceMap.containsKey(rsvr.getName()))
                    return true; // to let move from local one disk to other
            }
            
            if (osdDistanceMap.containsKey(rsvr.getName())){
                return false;
            }
            return true;
     
         } catch (ResourceNotFoundException ex) {
             return false;
         }
    }
}
