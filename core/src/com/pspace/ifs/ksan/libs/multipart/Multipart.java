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
package com.pspace.ifs.ksan.libs.multipart;

import java.util.Date;

public class Multipart {
    private String bucket;
    private String object;
    private Date lastModified;
    private String uploadId;
    private int partNumber;
    private String acl;
    private String meta;
    private String etag;
    private long size;
    private String diskPoolId;

    public Multipart(String bucket, String object, String uploadId) {
        this.bucket = bucket;
        this.object = object;
        this.uploadId = uploadId;
    }

    public Date getLastModified() {
        return new Date(lastModified.getTime());
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = new Date(lastModified.getTime());
    }

    public int getPartNumber() {
        return partNumber;
    }

    public String getDiskPoolId(){
        return diskPoolId;
    }
    
    public void setDiskPoolId(String diskPoolId){
        this.diskPoolId = diskPoolId;
    }
    
    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    public String getAcl() {
        return acl;
    }

    public void setAcl(String acl) {
        this.acl = acl;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }

    public String getETag() {
        return etag;
    }

    public void setETag(String etag) {
        this.etag = etag;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
