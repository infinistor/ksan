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
