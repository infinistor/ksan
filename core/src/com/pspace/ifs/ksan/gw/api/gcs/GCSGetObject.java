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

import com.pspace.ifs.ksan.gw.encryption.S3Encryption;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.object.IObjectManager;
import com.pspace.ifs.ksan.gw.object.ResultRange;
import com.pspace.ifs.ksan.gw.object.VFSObjectManager;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.objmanager.Metadata;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;

public class GCSGetObject extends GCSRequest {

    public GCSGetObject(S3Parameter s3Parameter) {
        super(s3Parameter);
        logger = LoggerFactory.getLogger(GCSGetObject.class);
    }

    @Override
    public void process() throws GWException {
        String bucketName = s3Parameter.getBucketName();
        initBucketInfo(bucketName);
        String objectName = s3Parameter.getObjectName();

        logger.debug("bucket : {}, object : {}", bucketName, objectName);
        String fields = gcsRequestData.getFields();
        String alt = gcsRequestData.getAlt();
        if (!Strings.isNullOrEmpty(alt) && alt.equalsIgnoreCase("media")) {
            logger.info(GWConstants.LOG_GCS_GET_OBJECT_START);
        } else {
            logger.info(GWConstants.LOG_GCS_HEAD_OBJECT_START);
        }
        String projection = gcsRequestData.getProjection();
        logger.debug("fields : {}", fields);

        Metadata objMeta = null;
		objMeta = open(bucketName, objectName);
        S3Metadata s3Metadata = S3Metadata.getS3Metadata(objMeta.getMeta());

        JSONObject json = new JSONObject();
        JSONArray array = new JSONArray();

        json.put("acl", array);
        if (!Strings.isNullOrEmpty(fields)) {
            String[] fieldArray = fields.split(",");
            for (String field : fieldArray) {
                if (field.equalsIgnoreCase("name")) {
                    json.put("name", objectName);
                    continue;
                }
                if (field.equalsIgnoreCase("size")) {
                    json.put("size", s3Metadata.getContentLength());
                    continue;
                }
                if (field.equalsIgnoreCase("contentType")) {
                    json.put("contentType", "text/plain");
                    continue;
                }
                if (field.equalsIgnoreCase("md5Hash")) {
                    json.put("md5Hash", s3Metadata.getContentMD5());
                    continue;
                }
                if (field.equalsIgnoreCase("crc32c")) {
                    json.put("crc32c", "");
                    continue;
                }
                if (field.equalsIgnoreCase("etag")) {
                    json.put("etag", s3Metadata.getETag());
                    continue;
                }
                if (field.equalsIgnoreCase("generation")) {
                    json.put("generation", s3Metadata.getVersionId());
                    continue;
                }
                if (field.equalsIgnoreCase("mediaLink")) {
                    String mediaLink = "https://storage.googleapis.com/download/storage/v1/b/" + bucketName + "/o/" + objectName + "?generation=" + s3Metadata.getVersionId() + "&alt=media";
                    json.put("mediaLink", mediaLink);
                    continue;
                }
            }
        } else if (!Strings.isNullOrEmpty(projection)) {
            json.put("kind", "storage#object");
            json.put("bucket", bucketName);
            json.put("name", objectName);
            json.put("size", String.valueOf(s3Metadata.getContentLength()));
            json.put("contentType", "text/plain");
            json.put("md5Hash", s3Metadata.getContentMD5());
            json.put("crc32c", "crc32c");
            json.put("etag", s3Metadata.getETag());
            json.put("generation", s3Metadata.getVersionId());
        }

        if ((!Strings.isNullOrEmpty(alt) && alt.equalsIgnoreCase("media"))) {
            // Get object
            S3Encryption s3Encryption = new S3Encryption("get", s3Metadata, s3Parameter);
            s3Encryption.build();
            String range = gcsRequestData.getRange();
            ResultRange resultRange = new ResultRange(range, s3Metadata, s3Parameter);

            s3Parameter.getResponse().addHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(s3Metadata.getContentLength()));
            // TODO : Range 처리 필요
            
            IObjectManager objectManager = new VFSObjectManager();
            try {
                objectManager.getObject(s3Parameter, objMeta, s3Encryption, resultRange.getS3Range());
            } catch (Exception e) {
                PrintStack.logging(logger, e);
                throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
            }
    
            s3Parameter.getResponse().setStatus(resultRange.getStatus());
            return;
        }

        logger.debug("json : {}", json.toString());
        s3Parameter.getResponse().setContentType(GWConstants.JSON_CONTENT_TYPE);
        try {
            s3Parameter.getResponse().getOutputStream().write(json.toString().getBytes(Charset.forName(Constants.UTF_8)));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
        }

        s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
    }
    
}
