/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.KSAN.ObjManger;

/**
 *
 * @author legesse
 */
public class ListResult {
    private String bucket;
    private String key;
    private String uploadId;
    private int partNo;
    private boolean truncated;
    
    public ListResult(){
        
    }
    
    public void set(String bucket, String key, String uploadId, int partNo){
        this.bucket = bucket;
        this.key = key;
        this.uploadId = uploadId;
        this.partNo = partNo;
        this.truncated = false;
    }
    
    public void setTruncated(){
        truncated = true;
    }
    
    public String getBucket(){
        return bucket;
    }
    
    public String getKey(){
        return key;
    }
    
    public String getUploadId(){
        return uploadId;
    }
    
    public int getPartNo(){
        return partNo;
    }
    
    public boolean isTruncated(){
        return truncated;
    }
    
    @Override
    public String toString(){
       return String.format(
               "\n{bucket : %s,  key : %s,  uploadId : %s, partNo : %d, isTruncated : %s}", 
               bucket, key, uploadId, partNo, truncated);
    }
}
