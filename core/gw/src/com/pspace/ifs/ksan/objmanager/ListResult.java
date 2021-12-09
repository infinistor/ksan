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
