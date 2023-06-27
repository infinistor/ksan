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
package com.pspace.ifs.ksan.gw.api.gcs;

import jakarta.servlet.http.HttpServletResponse;

import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.libs.identity.ObjectListParameter;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.libs.identity.S3ObjectList;

import org.slf4j.LoggerFactory;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.common.base.Strings;

public class GCSListObjects extends GCSRequest{

    public GCSListObjects(S3Parameter s3Parameter) {
        super(s3Parameter);
        logger = LoggerFactory.getLogger(GCSListObjects.class);
    }

    @Override
    public void process() throws GWException {
        logger.info(GWConstants.LOG_GCS_LIST_OBJECTS_START);

        String bucketName = s3Parameter.getBucketName();
        initBucketInfo(bucketName);
        logger.debug("bucketName : " + bucketName);

        S3ObjectList s3ObjectList = new S3ObjectList();
		s3ObjectList.setMaxKeys(GWConstants.DEFAULT_MAX_KEYS);
		s3ObjectList.setDelimiter("/");
		s3ObjectList.setMarker(null);
		s3ObjectList.setPrefix("");
        
        ObjectListParameter objectListParameter = listObject(bucketName, s3ObjectList);
        logger.debug("listObject : {}", objectListParameter.getObjects().size());

        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        jsonObject.put("kind", "storage#buckets");
        if (!Strings.isNullOrEmpty(objectListParameter.getNextMarker())) {
            jsonObject.put("nextPageToken", objectListParameter.getNextMarker());
        }

        for (S3Metadata s3Metadata : objectListParameter.getObjects()) {
            JSONObject json = new JSONObject();
            JSONArray array = new JSONArray();

            json.put("acl", array);
            
            json.put("bucket", bucketName);
            json.put("name", s3Metadata.getName());
            json.put("size", String.valueOf(s3Metadata.getContentLength()));
            json.put("contentType", "text/plain");
            json.put("md5Hash", s3Metadata.getContentMD5());
            json.put("crc32c", "crc32c");
            json.put("etag", s3Metadata.getETag());
            json.put("generation", s3Metadata.getVersionId());

            jsonArray.add(json);
        }
        jsonObject.put("items", jsonArray);
        JSONArray emptyArray = new JSONArray();
        jsonObject.put("prefixes", emptyArray);

        s3Parameter.getResponse().setContentType(GWConstants.JSON_CONTENT_TYPE);
        try {
            s3Parameter.getResponse().getOutputStream().write(jsonObject.toString().getBytes());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
        }
        s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
    }
    
}
