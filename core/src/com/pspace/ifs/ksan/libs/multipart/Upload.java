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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.libs.PrintStack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Upload {
    private String object;
    private String uploadId;
    private Date changeTime;
    private S3Metadata s3Metadata;
    private Logger logger = LoggerFactory.getLogger(Upload.class);

    public Upload(String object, Date changeTime, String uploadId, String meta)  {
        this.object = object;
        this.uploadId = uploadId;
        this.changeTime = new Date(changeTime.getTime());
        s3Metadata = S3Metadata.getS3Metadata(meta);
    }

    public String getObject() {
        return object;
    }

    public String getUploadId() {
        return uploadId;
    }

    public Date getChangeTime() {
        return new Date(changeTime.getTime());
    }

    public String getOwnerID() {
        return s3Metadata.getOwnerId();
    }

    public String getOwnerName() {
        return s3Metadata.getOwnerName();
    }
}
