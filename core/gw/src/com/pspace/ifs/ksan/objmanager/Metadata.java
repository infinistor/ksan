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

import java.security.*;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import javax.xml.bind.DatatypeConverter;

import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import java.time.Instant;

/**
 *
 * @author legesse
 */

public class Metadata {
    private String bucket;
    private String path;
    private String etag;
    private String tag;
    private String meta;
    private String acl;
    private String objid;
    private String bucketId;
    private long lastModified;
    private long size;
    private int replicaCount;
    private String versionId;
    private String deleteMarker;
    private boolean lastVersion;
    //private LinkedList<DISK> disk;
    private HashMap<String, DISK> allocDisk;
    protected  static final String PRIMARYDISK = "primary";
    protected  static final String REPLICADISK = "replica";

    Metadata() {
        allocDisk = new HashMap<>();
    }
    
    private String hashOfPath(String key) {
        byte[] bytesOfMessage;
        byte[] thedigest;
        MessageDigest md;
        String res;
        
        try{
            bytesOfMessage = key.getBytes("UTF-8");
            md = MessageDigest.getInstance("MD5"); //"SHA-1"
            thedigest = md.digest(bytesOfMessage);
            res = DatatypeConverter.printHexBinary(thedigest);
            //res = base16().lowerCase().encode(thedigest);
            //res= Base64.getEncoder().encodeToString(thedigest);
        } catch(UnsupportedEncodingException | NoSuchAlgorithmException ex){
            res = key;
        } 
        
        return res;
    }
    
    public Metadata(String bucket, String path, String etag, String tag, String meta, 
            String pdiskid, String pdskPath, String rdiskid, String rdskPath){
        this.bucket = bucket;
        this.path = path;
        this.bucketId = hashOfPath(bucket);
        this.objid = hashOfPath(bucket + path);
        // this.objid = hashOfPath(path);
        this.etag = etag;
        this.tag = tag;
        this.meta = meta;
        this.size = 0;
        this.lastModified = getNowInNanoSec();
        //this.disk = new LinkedList<>();
        allocDisk = new HashMap<>();
        this.setPrimaryDisk(pdiskid, pdskPath, 0);
        if (!(rdskPath.isEmpty() && rdiskid.isEmpty())){
            this.setReplicaDISK(rdiskid, rdskPath);
        }
        this.versionId = "";
        this.deleteMarker = "";
        this.lastVersion = false;
    }

    public Metadata(String bucket, String path, String etag, String meta, String tag, long size, String acl,
            String pdiskid, String pdskPath, String rdiskid, String rdskPath, String versionId, String deleteMarker){
        this.bucket = bucket;
        this.path = path;
        this.bucketId = hashOfPath(bucket);
        this.objid = hashOfPath(bucket + path);
        // this.objid = hashOfPath(path);
        this.etag = etag;
        this.tag = tag;
        this.meta = meta;
        this.size = size;
        this.acl = acl;
        this.lastModified = getNowInNanoSec();
        //this.disk = new LinkedList<>();
        allocDisk = new HashMap<>();
        this.setPrimaryDisk(pdiskid, pdskPath, 0);
        if (!(rdskPath.isEmpty() && rdiskid.isEmpty())){
            this.setReplicaDISK(rdiskid, rdskPath);
        }
        this.versionId = versionId;
        this.deleteMarker = deleteMarker;
        this.lastVersion = false;
    }
   
    public Metadata(String bucket, String path){
        this.bucket = bucket;
        this.path = path;
        this.etag = "";
        this.meta = "";
        this.size = 0;
        this.lastModified = getNowInNanoSec();
        this.bucketId = hashOfPath(bucket);
        this.objid = hashOfPath(bucket + path);
        // this.objid = hashOfPath(path);
        this.tag = "";
        allocDisk = new HashMap<>();
        this.versionId = "";
        this.lastVersion = false;
    }
    
    private long getNowInNanoSec(){
        Instant instant = Instant.now();
        return  instant.getEpochSecond() * 1000000000L + instant.getNano();
    }
    
    public void set(String etag, String tag, String meta){  
        this.etag = etag;
        this.tag = tag;
        this.meta = meta;
    }

    public void set(String etag, String tag, String meta, String acl, long size){  
        this.etag = etag;
        this.tag = tag;
        this.meta = meta;
        this.acl = acl;
        this.size = size;
    }
    
    public long getLastModified(){
        return this.lastModified;
    }
    
    public long getSize(){
        return this.size;
    }
    
    public String getPath(){
        return this.path;
    }
    
    public String getObjId(){
        return this.objid;
    }
    
    public String getBucket(){
        return this.bucket;
    }
    
    public String getBucketId(){
        return this.bucketId;
    }
    
    public DISK getPrimaryDisk(){
        return allocDisk.get(PRIMARYDISK);
    }
    
    public DISK getReplicaDisk() throws ResourceNotFoundException{
        try{
            return allocDisk.get(REPLICADISK);
        } catch (IndexOutOfBoundsException e){
            throw new ResourceNotFoundException("bucket : " +getBucket()+" path : " + getPath() + " there is no replica disk");
        }
    }
    
    public String getEtag(){
        return this.etag;
    }
    
    public String getTag(){
        return this.tag;
    }
    
    public String getMeta(){
        return this.meta;
    }

    public String getAcl() {
        return this.acl;
    }
    
    public String getVersionId(){
        return this.versionId;
    }

    public String getDeleteMarker() {
        return this.deleteMarker;
    }
    
    public boolean getLastVersion(){
        return this.lastVersion;
    }
    
    public boolean isReplicaExist(){
        return allocDisk.containsKey(REPLICADISK);
    }
    
    public boolean isPrimaryExist(){
        return allocDisk.containsKey(PRIMARYDISK);
    }
    
    public int getReplicaCount(){
        return replicaCount;
    }
    
    public void setLastModified(long lastmodified){
        this.lastModified = lastmodified;
    }
    
    public void updateLastmodified(){
        this.lastModified = getNowInNanoSec();
    }
    
    public void setSize(long size){
        this.size = size;
    }
    
    private void setDisk(String key, DISK dsk){
        if (!dsk.getId().isEmpty() && !dsk.getPath().isEmpty()){
            DISK dsk1 = allocDisk.putIfAbsent(key, dsk);
        }
    }
    
    public void setPrimaryDisk(String diskid, String dskPath, int st){
        if (!diskid.isEmpty() && !dskPath.isEmpty()){
            DISK dsk = new DISK();
            dsk.setId(diskid);
            dsk.setPath(dskPath);
            this.setDisk(PRIMARYDISK, dsk);
        }
    }
        
    public void setPrimaryDisk(DISK dsk){
       this.setDisk(PRIMARYDISK, dsk);
    }
    
    public void setReplicaDISK(String diskid, String dskPath){
        
        if (!diskid.isEmpty() && !dskPath.isEmpty()){ 
            DISK dsk = new DISK();
            dsk.setId(diskid);
            dsk.setPath(dskPath);
            this.setDisk(REPLICADISK, dsk);
        }
    }
    
    public void setReplicaDISK( DISK dsk){
        this.setDisk(REPLICADISK, dsk);
    }
   
    // temp method
    public void setObjid(String objid){
        this.objid = objid;
    }
    
    public void setEtag(String etag){
        this.etag = etag;
    }
    
    public void setTag(String tag){
        this.tag = tag;
    }
    
    public void setMeta(String meta){
        this.meta = meta;
    }
    
    public void setAcl(String acl){
        this.acl = acl;
    }

    public void setVersionId(String versionId, String deleteMarker, Boolean lastVersion){
        this.versionId = versionId;
        this.deleteMarker = deleteMarker;
        this.lastVersion = lastVersion;
    }
    
    public void setReplicaCount(int replicaCount){
        this.replicaCount = replicaCount;
    }
    
    @Override
    public String toString(){
        String replicaDiskStr;
        
        try{
            if (this.isReplicaExist())
                replicaDiskStr = this.getReplicaDisk().toString();
            else
                replicaDiskStr = "null"; 
        } catch(ResourceNotFoundException ex){
            replicaDiskStr = "null";
        }
        
        return String.format(
                "{path : %s etag : %s tag : %s meta : %s acl : %s objid : %s PrimaryDisk: %s ReplicaDisk : %s}", 
                this.getPath(), this.getEtag(), 
                this.getTag(), this.getMeta(), this.getAcl(), this.getObjId(), 
                this.getPrimaryDisk().toString(), replicaDiskStr);
    }
}
