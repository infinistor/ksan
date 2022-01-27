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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author legesse
 */
public class Bucket {
    private String name;
    private String id;
    private String diskPoolId;
    private String versioning;
    private String mfaDelete;
    private String acl;
    private String web;
    private String cors;
    private String lifecycle;
    private String access;
    private String tagging;
    private String encryption; // new
    private String replication;
    private String objectlock; // new
    private String policy; // new
    private String userName; // new
    private String userId;
    private Date createTime;
    private int replicaCount;
    private long usedSpace; // new
    private long fileCount; // new
    
    public Bucket(){
        name = "";
        id   = "";
        diskPoolId = "";
        versioning = "";
        mfaDelete = "";
        acl = "";
        userId = "";
        createTime = new Date(0);
        replicaCount= 0;
        encryption = "";
        objectlock = "";
        policy = "";
        userName = "";
        usedSpace = 0;
        fileCount = 0;
    }
    
    public Bucket(String name, String id, String diskPoolId){
        this.name = name;
        this.id   = id;
        this.diskPoolId = diskPoolId;
        versioning = "";
        mfaDelete = "";
        acl = "";
        userId = "";
        createTime = new Date(0);
        replicaCount = 0;
        encryption = "";
        objectlock = "";
        policy = "";
        userName = "";
        usedSpace = 0;
        fileCount = 0;
    }
    
    public Bucket(String name, String id, String diskPoolId, String versioning, String mfaDelete, String userId, String acl, Date createTime){
        this.name = name;
        this.id   = id;
        this.diskPoolId = diskPoolId;
        this.versioning = versioning;
        this.mfaDelete = "";
        this.acl = acl;
        this.userId = userId; 
        this.createTime = new Date(0);
        if (createTime != null)
            this.createTime = createTime;
          
        replicaCount = 0;
        encryption = "";
        objectlock = "";
        policy = "";
        userName = "";
        usedSpace = 0;
        fileCount = 0;
    }
    
    private void setHashOfName() {
        byte[] bytesOfMessage;
        byte[] thedigest;
        MessageDigest md;
        String res;
        
        try{
            bytesOfMessage = name.getBytes("UTF-8");
            md = MessageDigest.getInstance("MD5"); //"SHA-1"
            thedigest = md.digest(bytesOfMessage);
            id = DatatypeConverter.printHexBinary(thedigest);
        } catch(UnsupportedEncodingException | NoSuchAlgorithmException ex){
            id = name;
        } 
    }
    
    public void setName(String name){
        this.name = name;
        this.setHashOfName();
    }
    
    public void setId(String id){
        this.id   = id;
    }
    
    public void setDiskPoolId(String diskPoolId){
        this.diskPoolId = diskPoolId;
    }
    
    public void setVersioning(String versioning, String mfaDelete){
        this.versioning = versioning;
        this.mfaDelete = mfaDelete;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setAcl(String acl) {
        this.acl = acl;
    }

    public void setCreateTime(Date date) {
        this.createTime = date;
    }
    
    public void setEncryption(String encryption){
        this.encryption = encryption;
    }
    
    public void setObjectLock(String objectlock){
        this.objectlock = objectlock;
    }
    
    public void setPolicy(String policy){
        this.policy = policy;
    }
    
    public void setUserName(String userName){
        this.userName = userName;
    }
    
    public void setUsedSpace(long usedSpace){
        this.usedSpace = usedSpace;
    }
    
    public void setFileCount(long fileCount){
        this.fileCount = fileCount;
    }
    
    public void setWeb(String web) {
        this.web = web;
    }
 
    public void setCors(String cors) {
        this.cors = cors;
    }
    
    public void setLifecycle(String lifecycle) {
        this.lifecycle = lifecycle;
    }
     
    public void setAccess(String access) {
        this.access = access;
    }
    
    public void setTagging(String tagging) {
        this.tagging = tagging;
    }
     
    public void setReplication(String replication) {
        this.replication = replication;
    }
    
    public void setReplicaCount(int replicaCount){
        this.replicaCount = replicaCount;
    }
    
    public String getName(){
        return name;
    }
    
    public String getId(){
        return id;
    }
    
    public String getDiskPoolId(){
        return diskPoolId;
    }
    
    public String getVersioning(){
        if (versioning == null)
            return "";
        return versioning;
    }
    
    public String getMfaDelete(){
        return mfaDelete;
    }

    public String getUserId() {
        return userId;
    }

    public String getAcl() {
        return acl;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public String getWeb() {
        return web;
    }

    
    public String getCors() {
        return cors;
    }

    public int getReplicaCount(){
        return replicaCount;
    }
    
    

    public String getLifecycle() {
        return lifecycle;
    }

    public String getAccess() {
        return access;
    }
    
    public String getTagging() {
        return tagging;
    }
    
    public String getReplication() {
        return replication;
    }

    public String getEncryption(){
        return this.encryption;
    }
    
    public String getObjectLock(){
        return this.objectlock;
    }
    
    public String  getPolicy(){
        return this.policy;
    }
    
    public String getUserName(){
        return this.userName;
    }
    
    public long getUsedSpace(){
        return this.usedSpace;
    }
    
    public long getFileCount(){
        return this.fileCount;
    }
    
    @Override
    public String toString(){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String fomatTime = createTime == null ? "" :formatter.format(createTime);
        return String.format(
                "{name : %s id : %s diskPoolId : %s versioning : %s MfaDelete : %s userId : %s acl : %s createTime : %s}", 
                this.getName(), this.getId(), 
                this.getDiskPoolId(), 
                this.getVersioning(),
                this.mfaDelete,
                this.userId,
                this.acl,
                fomatTime);
    }
}
