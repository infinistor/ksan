package com.pspace.ifs.ksan.objmanager;

import java.util.Date;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author legesse
 */
public class LifeCycle {
    private long idx;
    private Date inDate;
    private String bucketName;
    private String key;
    private String versionId;
    private String uploadId;
    private String failerLog;
    private String objId;
    private boolean isFailed;
    
    public LifeCycle(long index, String bucketName, String key, String versionId, String uploadId, String failerLog){
        this.bucketName = bucketName;
        this.key = key;
        this.versionId = versionId;
        this.idx = index;
        this.uploadId = uploadId;
        this.failerLog = failerLog;
        this.inDate = new Date();
        objId = new Metadata(bucketName, key).getObjId();
        isFailed = true;
        if (failerLog.isEmpty())
            isFailed = false;
    }
    
    public long getIndex(){
        return idx;
    }
    
    public String getBucketName(){
        return bucketName;
    }
    
    public String getKey(){
        return key;
    }
    
    public String getVersionId(){
        return versionId;
    }
    
    public String getUploadId(){
        return uploadId;
    }
    
    public  String getLog(){
        return failerLog;
    }
    
    public Date getInDate(){
        return inDate;
    }
    
    public String getObjId(){
        return objId;
    }
    
    public boolean isFailed(){
        return isFailed;
    }
    
    public void setFailedEvent(boolean isFailed){
        this.isFailed = isFailed;
    }
    
    public void setIndex(long idx){
        this.idx = idx;
    }
    
    public void setBucketName(String bucketName){
        this.bucketName = bucketName;
    }
    
    public void setKey(String key){
        this.key = key;
    }
    
    public void setVersionId(String versionId){
        this.versionId = versionId;
    }
    
    public void setUploadId(String uploadId){
        this.uploadId = uploadId;
    }
    
    public void setLog(String failerLog){
        this.failerLog = failerLog;
    }
    
    public void setInDate(Date inDate){
        this.inDate = inDate;
    }
}
    
