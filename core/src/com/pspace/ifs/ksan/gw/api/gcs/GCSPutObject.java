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

import static com.google.common.io.BaseEncoding.base16;

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
import com.pspace.ifs.ksan.libs.multipart.Multipart;
import com.pspace.ifs.ksan.libs.multipart.Part;
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
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicReference;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.base.Strings;

public class GCSPutObject extends GCSRequest {

    public GCSPutObject(S3Parameter s3Parameter) {
        super(s3Parameter);
        logger = LoggerFactory.getLogger(GCSPutObject.class);
    }

    @Override
    public void process() throws GWException {
        logger.info(GWConstants.LOG_GCS_PUT_OBJECT_START);

        String bucketName = s3Parameter.getBucketName();
        initBucketInfo(bucketName);

        String objectName = s3Parameter.getObjectName();

        String uploadId = gcsRequestData.getParamUploadId();
        String contentsLengthString = gcsRequestData.getContentLength();
        String contentRange = gcsRequestData.getContentRange();

        logger.debug("Content-Length : {}", contentsLengthString);
        long contentLength = Long.parseLong(contentsLengthString);

        logger.debug("bucket : {}, object : {}, uploadId : {}", bucketName, objectName, uploadId);

        ObjMultipart objMultipart = null;
		Multipart multipart = null;
		try {
			objMultipart = getInstanceObjMultipart(bucketName);
            logger.debug("objMultipart : {}", objMultipart);
			multipart = objMultipart.getMultipart(uploadId);
			if (multipart == null) {
				logger.error(GWConstants.LOG_UPLOAD_NOT_FOUND, uploadId);
				throw new GWException(GWErrorCode.NO_SUCH_UPLOAD, s3Parameter);
			}
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
        // get metadata
		S3Metadata s3Metadata = null;
		try {
			s3Metadata = S3Metadata.getS3Metadata(multipart.getMeta());
		} catch(Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

        Metadata objMeta = null;
        try {
            // check exist object
            objMeta = open(bucketName, objectName);
        } catch (GWException e) {
            logger.info(e.getMessage());
            // reset error code
            s3Parameter.setErrorCode(GWConstants.EMPTY_STRING);
            objMeta = createLocal(multipart.getDiskPoolId(), bucketName, objectName, GWConstants.VERSIONING_DISABLE_TAIL);
        }

        // check range
        boolean isLast = false;
        if (!Strings.isNullOrEmpty(contentRange)) {
            String[] ranges = contentRange.split(" ");
            if (ranges.length == 2) {
                String[] range = ranges[1].split("-");
                if (range.length == 2) {
                    long start = Long.parseLong(range[0]);
                    long end = 0;
                    long size = 0;
                    long totalSize = 0;
                    if (range[1].contains("/")) {
                        String[] rangeSize = range[1].split("/");
                        end = Long.parseLong(rangeSize[0]);
                        if (rangeSize.length == 2) {
                            if (!rangeSize[1].equals("*")) {
                                isLast = true;
                                totalSize = Long.parseLong(rangeSize[1]);
                            }
                        }
                    } else {
                        end = Long.parseLong(range[1]);
                    }
                    size = end - start + 1;
                    logger.debug("start : {}, end : {}, size : {}", start, end, size);

                    // SortedMap<Integer, Part> listPart = null;
                    // try {
                    //     listPart = objMultipart.getParts(uploadId);
                    // } catch (Exception e) {
                    //     PrintStack.logging(logger, e);
                    //     throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
                    // }

                    // int partNumber = listPart.size() + 1;
                    // s3Parameter.setPartNumber(String.valueOf(partNumber));
                    // s3Parameter.setUploadId(uploadId);
                    // objMeta.setSize(size);

                    IObjectManager objectManager = new VFSObjectManager();
                    S3Object s3Object = objectManager.putObjectRange(s3Parameter, objMeta, start, size);
                    s3Metadata.setETag(s3Object.getEtag());
                    s3Metadata.setLastModified(s3Object.getLastModified());
                    s3Metadata.setContentLength(s3Object.getFileSize());
                    objMeta.setSize(s3Object.getFileSize());
                    objMeta.setEtag(s3Object.getEtag());
                    if (isLast) {
                        Instant instant = Instant.now();
                        long nano = instant.getEpochSecond() * 1000000000L + instant.getNano();
                        s3Metadata.setVersionId(String.valueOf(nano));
                    }
                    logger.debug("file size : {}", s3Object.getFileSize());
                    String jsonmeta = s3Metadata.toString();
                    String acl = multipart.getAcl();
                    try {
                        objMeta.set(s3Object.getEtag(), "", jsonmeta, acl, s3Object.getFileSize());
                        insertObject(bucketName, objectName, objMeta);
                    } catch (GWException e) {
                        PrintStack.logging(logger, e);
                        throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
                    }

                    // Metadata part = null;
                    // S3Object s3Object = null;
                    // try {
                    //     part = objMultipart.getObjectWithUploadIdPartNo(uploadId, partNumber);
                    //     if (part != null) {
                    //         objectManager.deletePart(s3Parameter, objMeta);
                    //     }
                    //     s3Object = objectManager.uploadPart(s3Parameter, objMeta);
                    //     s3Metadata.setETag(s3Object.getEtag());
                    //     s3Metadata.setLastModified(s3Object.getLastModified());
                    //     s3Metadata.setContentLength(s3Object.getFileSize());
                    //     objMeta.setSize(s3Object.getFileSize());
                    //     objMeta.setEtag(s3Object.getEtag());
                    // } catch (Exception e) {
                    //     PrintStack.logging(logger, e);
                    //     throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
                    // }
                    
                    // objMultipart.startSingleUpload(objMeta, uploadId, partNumber);
                    // objMultipart.finishSingleUpload(uploadId, partNumber);

                    if (isLast) {
                        // complete multipart upload
                        // logger.debug("complete multipart upload ...");
                        // try {
                        //     listPart = objMultipart.getParts(uploadId);
                        // } catch (Exception e) {
                        //     PrintStack.logging(logger, e);
                        //     throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
                        // }

                        // final AtomicReference<S3Object> s3ObjectComplete = new AtomicReference<>();
                        // S3Encryption encryption = new S3Encryption("upload", s3Metadata, s3Parameter);
		                // encryption.build();
                        // SortedMap<Integer, Part> constListPart = listPart;
			            // Metadata constObjMeta = objMeta;

                        // Thread thread = new Thread() {
                        //     @Override
                        //     public void run() {
                        //         try {
                        //             s3ObjectComplete.set(objectManager.completeMultipart(s3Parameter, constObjMeta, encryption, constListPart));
                        //         } catch (Exception e) {
                        //             logger.debug("completeMultipart error : {}", e.getMessage());
                        //         }
                        //     }
                        // };
            
                        // thread.start();
                        
                        // while (thread.isAlive()) {
                        //     try {
                        //         thread.join(500);
                        //     } catch (InterruptedException ie) {
                        //         PrintStack.logging(logger, ie);
                        //         throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
                        //     }
                        // }

		                // s3Metadata.setUploadId(uploadId);
                        // s3Metadata.setName(objectName);
                        // s3Metadata.setContentLength(s3ObjectComplete.get().getFileSize());
                        // s3Metadata.setETag(s3ObjectComplete.get().getEtag());
                        // s3Metadata.setLastModified(s3ObjectComplete.get().getLastModified());
                        // s3Metadata.setTier(GWConstants.AWS_TIER_STANTARD);
                        // s3Metadata.setDeleteMarker(s3ObjectComplete.get().getDeleteMarker());
                        // Instant instant = Instant.now();
                        // long nano = instant.getEpochSecond() * 1000000000L + instant.getNano();
                        // s3Metadata.setVersionId(String.valueOf(nano));

                        // String jsonmeta = s3Metadata.toString();
                        // String acl = multipart.getAcl();
                        // int result = 0;
                        // try {
                        //     objMeta.set(s3ObjectComplete.get().getEtag(), "", jsonmeta, acl, s3ObjectComplete.get().getFileSize());
                        //     // objMeta.setVersionId(String.valueOf(nano), GWConstants.OBJECT_TYPE_FILE, true);
                        //     result = insertObject(bucketName, objectName, objMeta);
                        // } catch (GWException e) {
                        //     PrintStack.logging(logger, e);
                        //     throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
                        // }

                        JSONObject json = new JSONObject();
                        // JSONArray array = new JSONArray();

                        // json.put("acl", array);
                        // json.put("crc32c", "");
                        // json.put("etag", s3ObjectComplete.get().getEtag());
                        // json.put("generation", String.valueOf(nano));
                        // json.put("md5Hash", "asdfasdfasdfasdfasdfasdfasdfasdfasf");
                        // json.put("size", String.valueOf(s3ObjectComplete.get().getFileSize()));

                        json.put("kind", "storage#object");
                        json.put("bucket", bucketName);
                        json.put("name", objectName);
                        json.put("size", String.valueOf(s3Object.getFileSize()));
                        json.put("contentType", "text/plain");
                        json.put("md5Hash", s3Metadata.getContentMD5());
                        json.put("crc32c", "crc32c");
                        json.put("etag", s3Metadata.getETag());
                        json.put("generation", s3Metadata.getVersionId());

                        s3Parameter.getResponse().setContentType(GWConstants.JSON_CONTENT_TYPE);
                        try {
                            s3Parameter.getResponse().getOutputStream().write(json.toString().getBytes(StandardCharsets.UTF_8));
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                            throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
                        }

                        s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
                    } else {
                        s3Parameter.getResponse().setStatus(308);
                        s3Parameter.getResponse().setHeader("Range", "bytes " + start + "-" + end);
                        s3Parameter.getResponse().setHeader("X-GUploader-UploadID", uploadId);
                        s3Parameter.getResponse().setHeader("X-Range-MD5", s3Object.getEtag());
                        logger.debug("Range : bytes {}-{}", start, end);
                    }
                    return;
                } else {
                    if (contentLength == 0) {
                        JSONObject json = new JSONObject();

                        json.put("kind", "storage#object");
                        json.put("bucket", bucketName);
                        json.put("name", objectName);
                        json.put("size", String.valueOf(objMeta.getSize()));
                        json.put("contentType", "text/plain");
                        json.put("md5Hash", s3Metadata.getContentMD5());
                        json.put("crc32c", "crc32c");
                        json.put("etag", s3Metadata.getETag());
                        json.put("generation", s3Metadata.getVersionId());
    
                        s3Parameter.getResponse().setContentType(GWConstants.JSON_CONTENT_TYPE);
                        try {
                            s3Parameter.getResponse().getOutputStream().write(json.toString().getBytes(StandardCharsets.UTF_8));
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                            throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
                        }
    
                        s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
                        return;
                    }
                }
            }
        }

        if (contentLength > 0) {
            objMeta.setSize(contentLength);
        } else {
            objMeta.setSize(s3Metadata.getContentLength());
        }

        IObjectManager objectManager = new VFSObjectManager();
        S3Object s3Object = objectManager.putObject(s3Parameter, objMeta, new S3Encryption(null, null, null, s3Parameter));

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

        // json.put("acl", array);
        // json.put("crc32c", "");
        // json.put("etag", "");
        // json.put("generation", s3Metadata.getVersionId());
        // json.put("md5Hash", md5);
        // json.put("size", String.valueOf(s3Object.getFileSize()));

        json.put("kind", "storage#object");
        json.put("bucket", bucketName);
        json.put("name", objectName);
        json.put("size", String.valueOf(s3Metadata.getContentLength()));
        json.put("contentType", "text/plain");
        json.put("md5Hash", s3Metadata.getContentMD5());
        json.put("crc32c", "crc32c");
        json.put("etag", s3Metadata.getETag());
        json.put("generation", s3Metadata.getVersionId());

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
