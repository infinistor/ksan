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

import org.slf4j.LoggerFactory;

import com.pspace.ifs.ksan.gw.encryption.S3Encryption;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.object.IObjectManager;
import com.pspace.ifs.ksan.gw.object.S3Object;
import com.pspace.ifs.ksan.gw.object.VFSObjectManager;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.libs.multipart.Upload;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjMultipart;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.base.Strings;

public class GCSPostObject extends GCSRequest {

    public GCSPostObject(S3Parameter s3Parameter) {
        super(s3Parameter);
        logger = LoggerFactory.getLogger(GCSPostObject.class);
    }

    @Override
    public void process() throws GWException {
        logger.info(GWConstants.LOG_GCS_POST_OBJECT_START);

        Metadata objMeta = null;
        String bucketName = s3Parameter.getBucketName();
        initBucketInfo(bucketName);
        String objectName = ""; 
        String diskpoolId = s3Parameter.getUser().getUserDefaultDiskpoolId();
        String uploadType = gcsRequestData.getUploadType();

        if (s3Parameter.getUri().startsWith("/resumable") || uploadType.equals("resumable")) {
            // resumable upload
            String contentLengthString = gcsRequestData.getUpladContentsLength();
            String data = gcsRequestData.readBody();
            
            objectName = gcsRequestData.getObjectName(data);
            logger.debug("bucketName : {}, objectName : {}", bucketName, objectName);

            try {
                // check exist object
                objMeta = open(bucketName, objectName);
            } catch (GWException e) {
                logger.info(e.getMessage());
                // reset error code
                s3Parameter.setErrorCode(GWConstants.EMPTY_STRING);
                objMeta = createLocal(diskpoolId, bucketName, objectName, GWConstants.VERSIONING_DISABLE_TAIL);
            }

            long contentLength = 0;
            if (!Strings.isNullOrEmpty(contentLengthString)) {
                contentLength = Long.parseLong(contentLengthString);
            }
             
			objMeta.setSize(contentLength);

            S3Metadata s3Metadata = new S3Metadata();
            s3Metadata.setName(objectName);
            s3Metadata.setContentLength(contentLength);
            s3Metadata.setTier(GWConstants.AWS_TIER_STANTARD);

            Instant instant = Instant.now();
            long nano = instant.getEpochSecond() * 1000000000L + instant.getNano();
            s3Metadata.setVersionId(String.valueOf(nano));

            String uploadId = null;
            try {
                ObjMultipart objMultipart = getInstanceObjMultipart(bucketName);
                objMeta.setMeta(s3Metadata.toString());
                uploadId = objMultipart.createMultipartUpload(objMeta);
            } catch (Exception e) {
                PrintStack.logging(logger, e);
                throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
            }
            logger.debug("uploadId : {}", uploadId);
            s3Parameter.getResponse().addHeader("X-GUploader-UploadID", uploadId);
            if (uploadType.equals("resumable")) {
                s3Parameter.getResponse().addHeader("Location", "http://" + GWUtils.getLocalIP() + ":7070/resumable" + s3Parameter.getURI() + "/" + objectName + "?upload_id=" + uploadId);
            } else {
                s3Parameter.getResponse().addHeader("Location", s3Parameter.getURI() + "/" + objectName + "?upload_id=" + uploadId);
            }

            s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
            return;
        } else if (s3Parameter.getUri().startsWith("/upload")) {
            // simple upload
            String boundary = gcsRequestData.getBoundary();
            logger.debug("boundary : {}", boundary);
            
            String body = gcsRequestData.readBody();
            logger.debug("body : {}", body);

            String[] parts = body.split("--" + boundary);
            for (String part : parts) {
                logger.debug("part : {}", part);
            }

            String jsonObject = parts[1].substring(parts[1].indexOf("\n\n") + 2);
            String data = parts[2].substring(parts[2].indexOf("\n\n") + 2, parts[2].length() - 1);

            logger.debug("jsonObject : {}", jsonObject);
            logger.debug("data : *{}*, {}", data, data.length());
            logger.debug("objectName : {}", gcsRequestData.getObjectName(jsonObject));

            objectName = gcsRequestData.getObjectName(jsonObject);
            
            try {
                // check exist object
                objMeta = open(bucketName, objectName);
            } catch (GWException e) {
                logger.info(e.getMessage());
                // reset error code
                s3Parameter.setErrorCode(GWConstants.EMPTY_STRING);
                objMeta = createLocal(diskpoolId, bucketName, objectName, GWConstants.VERSIONING_DISABLE_TAIL);
            }

            byte[] content;
            try {
                content = data.getBytes("UTF-8");
            } catch (Exception e) {
                PrintStack.logging(logger, e);
                throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
            }

            objMeta.setSize(content.length);
            s3Parameter.setInputStream(new ByteArrayInputStream(content));
        }

        IObjectManager objectManager = new VFSObjectManager();
        
        S3Object s3Object = objectManager.putObject(s3Parameter, objMeta, new S3Encryption(null, null, null, s3Parameter));

        S3Metadata s3Metadata = new S3Metadata();
        s3Metadata.setName(objectName);
        s3Metadata.setETag(s3Object.getEtag());
        s3Metadata.setContentLength(s3Object.getFileSize());
        s3Metadata.setTier(GWConstants.AWS_TIER_STANTARD);
        s3Metadata.setLastModified(s3Object.getLastModified());
        s3Metadata.setDeleteMarker(s3Object.getDeleteMarker());

        Instant instant = Instant.now();
        long nano = instant.getEpochSecond() * 1000000000L + instant.getNano();
        s3Metadata.setVersionId(String.valueOf(nano));
        
        // calculate crc32c, md5
        // String crc32c = calculateCRC32C(data);
        String md5 = s3Object.getMd5hash();
        s3Metadata.setContentMD5(md5);
        logger.debug("md5 : {}", md5);

        try {
            objMeta.set(s3Object.getEtag(), null, s3Metadata.toString(), null, s3Object.getFileSize());
            int result = insertObject(bucketName, objectName, objMeta);
            logger.debug(GWConstants.LOG_PUT_OBJECT_INFO, bucketName, objectName, s3Object.getFileSize(), s3Object.getEtag(), null, null);
        } catch (GWException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }

        JSONObject json = new JSONObject();
        JSONArray array = new JSONArray();

        json.put("acl", array);
        json.put("crc32c", "");
        json.put("etag", "");
        json.put("generation", s3Metadata.getVersionId());
        json.put("md5Hash", md5);
        json.put("size", s3Object.getFileSize());

        s3Parameter.getResponse().setContentType(GWConstants.JSON_CONTENT_TYPE);
        try {
            s3Parameter.getResponse().getOutputStream().write(json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
        }

        s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
    }
}
