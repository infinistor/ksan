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
package com.pspace.ifs.ksan.gw.api;

import java.net.UnknownHostException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import com.pspace.ifs.ksan.gw.data.DataUploadPart;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.object.S3Object;
import com.pspace.ifs.ksan.gw.object.S3ObjectEncryption;
import com.pspace.ifs.ksan.gw.object.S3ObjectOperation;
import com.pspace.ifs.ksan.gw.object.multipart.Multipart;
import com.pspace.ifs.ksan.gw.utils.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWDiskConfig;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjMultipart;

import org.slf4j.LoggerFactory;

public class UploadPart extends S3Request {

	public UploadPart(S3Parameter s3Parameter) {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(UploadPart.class);
	}

	@Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_UPLOAD_PART_START);
		
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);
		String object = s3Parameter.getObjectName();

		S3Bucket s3Bucket = new S3Bucket();
		s3Bucket.setCors(getBucketInfo().getCors());
		s3Bucket.setAccess(getBucketInfo().getAccess());
		s3Parameter.setBucket(s3Bucket);
		GWUtils.checkCors(s3Parameter);
		
		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}
		
		checkGrantBucket(s3Parameter.isPublicAccess(), String.valueOf(s3Parameter.getUser().getUserId()), GWConstants.GRANT_WRITE);
		
		DataUploadPart dataUploadPart = new DataUploadPart(s3Parameter);
		dataUploadPart.extract();

		String partNumberStr = dataUploadPart.getPartNumber();
		int partNumber = Integer.parseInt(partNumberStr);
		String uploadId = dataUploadPart.getUploadId();

		s3Parameter.setUploadId(uploadId);
		s3Parameter.setPartNumber(partNumberStr);

		if (partNumber < 1 || partNumber > GWConstants.MAX_PARTS_SIZE) {
			logger.error(GWErrorCode.INVALID_ARGUMENT.getMessage() + GWConstants.LOG_UPLOAD_PART_WRONG_PART_NUMBER);
			
			throw new GWException(GWErrorCode.INVALID_ARGUMENT,	GWConstants.LOG_UPLOAD_PART_WRONG_PART_NUMBER, 
					(Throwable) null, ImmutableMap.of(GWConstants.ARGMENT_NAME, GWConstants.PART_NUMBER, GWConstants.ARGMENT_VALUE, partNumberStr), s3Parameter);
		}

		String contentLength = dataUploadPart.getContentLength();
		String contentMD5String = dataUploadPart.getContentMD5();
		String customerAlgorithm = dataUploadPart.getServerSideEncryptionCustomerAlgorithm();
		String customerKey = dataUploadPart.getServerSideEncryptionCustomerKey();
		String customerKeyMD5 = dataUploadPart.getServerSideEncryptionCustomerKeyMD5();
		
		if (Strings.isNullOrEmpty(contentLength)) {
			logger.error(GWConstants.LENGTH_REQUIRED);
			throw new GWException(GWErrorCode.MISSING_CONTENT_LENGTH, s3Parameter);
		}
		
		ObjMultipart objMultipart = null;
		Multipart multipart = null;
		try {
			objMultipart = new ObjMultipart(bucket);
			multipart = objMultipart.getMultipart(uploadId);
			if (multipart == null) {
				logger.error(GWConstants.LOG_UPLOAD_NOT_FOUND, uploadId);
				throw new GWException(GWErrorCode.NO_SUCH_UPLOAD, s3Parameter);
			}
		} catch (UnknownHostException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		// get metadata
		S3Metadata s3Metadata = new S3Metadata();
		ObjectMapper jsonMapper = new ObjectMapper();
		try {
			s3Metadata = jsonMapper.readValue(multipart.getMeta(), S3Metadata.class);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}
		
		// check SSE
		if (!Strings.isNullOrEmpty(customerAlgorithm)) {
			if (!GWConstants.AES256.equalsIgnoreCase(customerAlgorithm)) {
				logger.error(GWErrorCode.NOT_IMPLEMENTED.getMessage() + GWConstants.SERVER_SIDE_OPTION);
				throw new GWException(GWErrorCode.NOT_IMPLEMENTED, s3Parameter);
			} else {
				s3Metadata.setServersideEncryption(customerAlgorithm);
			}
		}
		if (!Strings.isNullOrEmpty(customerKey)) {
			s3Metadata.setCustomerKey(customerKey);
		}
		if (!Strings.isNullOrEmpty(customerKeyMD5)) {
			s3Metadata.setCustomerKeyMD5(customerKeyMD5);
		}
		if (!Strings.isNullOrEmpty(contentMD5String)) {
			s3Metadata.setContentMD5(contentMD5String);
		}
		long length = Long.parseLong(contentLength);
		s3Metadata.setContentLength(length);

		Metadata objMeta = createLocal(bucket, object);

		// check encryption
		S3ObjectEncryption s3ObjectEncryption = new S3ObjectEncryption(s3Parameter, s3Metadata);
		s3ObjectEncryption.build();

		String path = GWDiskConfig.getInstance().getLocalPath(objMeta.getPrimaryDisk().getId());
		if (path == null) {
			logger.error(GWConstants.LOG_CANNOT_FIND_LOCAL_PATH, objMeta.getPrimaryDisk().getId());
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		S3ObjectOperation objectOperation = new S3ObjectOperation(objMeta, s3Metadata, s3Parameter, null, s3ObjectEncryption);
		Metadata part = null;
		S3Object s3Object = null;
		try {
			part = objMultipart.getObjectWithUploadIdPartNo(uploadId, partNumber);
			if (part != null) {
				objectOperation.deletePart(part.getPrimaryDisk().getId());
			}
			s3Object = objectOperation.uploadPart(path, length);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}
		
		objMultipart.startSingleUpload(object, uploadId, partNumber, "", "", s3Object.getEtag(), s3Object.getFileSize(), objMeta.getPrimaryDisk().getId());
		objMultipart.finishSingleUpload(uploadId, partNumber);

		s3Parameter.addRequestSize(s3Object.getFileSize());
		s3Parameter.setFileSize(s3Object.getFileSize());
		
		s3Parameter.getResponse().addHeader(HttpHeaders.ETAG, GWUtils.maybeQuoteETag(s3Object.getEtag()));
	}
}
