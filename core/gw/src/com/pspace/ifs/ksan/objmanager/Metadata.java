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

import java.security.*;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import javax.xml.bind.DatatypeConverter;

import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

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
    private LocalDateTime lastModified;
    private long size;
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
        this.lastModified = LocalDateTime.now();
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
        this.lastModified = LocalDateTime.now();
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
        this.lastModified = LocalDateTime.now();
        this.bucketId = hashOfPath(bucket);
        this.objid = hashOfPath(bucket + path);
        // this.objid = hashOfPath(path);
        this.tag = "";
        allocDisk = new HashMap<>();
        this.versionId = "";
        this.lastVersion = false;
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
    
    public LocalDateTime getLastModified(){
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
   
    public void setLastModified(LocalDateTime lastmodified){
        this.lastModified = lastmodified;
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

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public void setVersionId(String versionId, String deleteMarker, Boolean lastVersion){
        this.versionId = versionId;
        this.deleteMarker = deleteMarker;
        this.lastVersion = lastVersion;
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
