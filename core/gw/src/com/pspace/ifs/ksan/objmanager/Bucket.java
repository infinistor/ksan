/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.ksan.objmanager;

import java.text.SimpleDateFormat;
import java.util.Date;

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
    private String replication;
    private String userId;
    private Date createTime;
    private int replicaCount;
    
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
    }
    
    /*public Bucket(String name, String id, String diskPoolId, String versioning, String mfaDelete){
        this.name = name;
        this.id   = id;
        this.diskPoolId = diskPoolId;
        this.versioning = versioning;
        this.mfaDelete = mfaDelete;
        acl = "";
        userId = "";
        createTime = null;
    }*/

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
    }
    
    public void setName(String name){
        this.name = name;
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

    public void setWeb(String web) {
        this.web = web;
    }

    public String getCors() {
        return cors;
    }

    public int getReplicaCount(){
        return replicaCount;
    }
    
    public void setCors(String cors) {
        this.cors = cors;
    }

    public String getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(String lifecycle) {
        this.lifecycle = lifecycle;
    }

    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }

    public String getTagging() {
        return tagging;
    }

    public void setTagging(String tagging) {
        this.tagging = tagging;
    }

    public String getReplication() {
        return replication;
    }

    public void setReplication(String replication) {
        this.replication = replication;
    }
    
    public void setReplicaCount(int replicaCount){
        this.replicaCount = replicaCount;
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
