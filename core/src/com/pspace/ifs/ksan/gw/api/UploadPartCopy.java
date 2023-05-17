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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.Date;

import jakarta.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.object.IObjectManager;
import com.pspace.ifs.ksan.gw.object.S3Object;
import com.pspace.ifs.ksan.gw.encryption.S3Encryption;
// import com.pspace.ifs.ksan.gw.object.S3ObjectOperation;
import com.pspace.ifs.ksan.gw.object.IObjectManager;
import com.pspace.ifs.ksan.gw.object.VFSObjectManager;
import com.pspace.ifs.ksan.gw.object.S3Range;
import com.pspace.ifs.ksan.libs.multipart.Multipart;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.DiskManager;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjMultipart;

import org.slf4j.LoggerFactory;

public class UploadPartCopy extends S3Request {

	public UploadPartCopy(S3Parameter s3Parameter) {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(UploadPartCopy.class);
	}

	@Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_UPLOAD_PART_COPY_START);
		
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);
		String object = s3Parameter.getObjectName();

		GWUtils.checkCors(s3Parameter);
		
		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}
		
		checkGrantBucket(false, GWConstants.GRANT_WRITE);

		String partNumber = s3RequestData.getPartNumber();
		String uploadId = s3RequestData.getUploadId();
		String copySource = s3RequestData.getCopySource();
		String copySourceRange = s3RequestData.getCopySourceRange();
		String copySourceIfMatch = s3RequestData.getCopySourceIfMatch();
		String copySourceIfNoneMatch = s3RequestData.getCopySourceIfNoneMatch();;
		String copySourceIfModifiedSince = s3RequestData.getCopySourceIfModifiedSince();
		String copySourceIfUnmodifiedSince = s3RequestData.getCopySourceIfUnmodifiedSince();
		// String customerAlgorithm = s3RequestData.getServerSideEncryptionCustomerAlgorithm();
		// String customerKey = s3RequestData.getServerSideEncryptionCustomerKey();
		// String customerKeyMD5 = s3RequestData.getServerSideEncryptionCustomerKeyMD5();
		String copySourceCustomerAlgorithm = s3RequestData.getCopySourceServerSideEncryptionCustomerAlgorithm();
		String copySourceCustomerKey = s3RequestData.getCopySourceServerSideEncryptionCustomerKey();
		String copySourceCustomerKeyMD5 = s3RequestData.getCopySourceServerSideEncryptionCustomerKeyMD5(); 

		// Check copy source
		if (Strings.isNullOrEmpty(copySource)) {
			logger.error(GWConstants.LOG_COPY_SOURCE_IS_NULL);
			throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		}
		
		try {
			copySource = URLDecoder.decode(copySource, Constants.CHARSET_UTF_8);
			logger.info(GWConstants.LOG_UPLOAD_PART_COPY_SOURCE, copySource);
		} catch (UnsupportedEncodingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		if (copySource.startsWith(GWConstants.SLASH)) {
			copySource = copySource.substring(1);
		} else if (copySource.contains(GWConstants.S3_ARN)) {
			logger.error(GWConstants.LOG_COPY_SOURCE_IS_NOT_IMPLEMENTED, copySource);
			throw new GWException(GWErrorCode.NOT_IMPLEMENTED, s3Parameter);
		}

		String[] sourcePath = copySource.split(GWConstants.SLASH, 2);
		if (sourcePath.length != 2) {
			throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
		}

		String srcBucket = sourcePath[0];
		String srcObjectName = sourcePath[1];
		String srcVersionId = null;

		if (!isExistBucket(srcBucket)) {
			logger.error(GWConstants.LOG_BUCKET_IS_NOT_EXIST, srcBucket);
            throw new GWException(GWErrorCode.NO_SUCH_BUCKET, s3Parameter);
        }

		if (srcObjectName.contains(GWConstants.SUB_PARAMETER_VERSIONID) == true) {
			String[] source = sourcePath[1].split(GWConstants.PARAMETER_BACKSLASH_VERSIONID, 2);
			srcObjectName = source[0];
			srcVersionId = source[1].replaceAll(GWConstants.DOUBLE_QUOTE, "");
		}

		logger.info("source bucket: {}, key: {}, versionId: {},", srcBucket, srcObjectName, srcVersionId);
		String versioningStatus = getBucketVersioning(srcBucket);

		Metadata srcMeta = null;
		if (GWConstants.VERSIONING_ENABLED.equalsIgnoreCase(versioningStatus)) {
			if (!Strings.isNullOrEmpty(srcVersionId)) {
				srcMeta = open(srcBucket, srcObjectName, srcVersionId);
			} else {
				srcMeta = open(srcBucket, srcObjectName);
				srcVersionId = srcMeta.getVersionId();
			}
		} else {
			srcMeta = open(srcBucket, srcObjectName);
			srcVersionId = srcMeta.getVersionId();
		}

		s3Parameter.setUploadId(uploadId);
		s3Parameter.setSrcVersionId(srcVersionId);
		s3Parameter.setSrcPath(srcObjectName);
		s3Parameter.setPartNumber(partNumber);
		
		logger.debug(GWConstants.LOG_SOURCE_INFO, srcBucket, srcObjectName, srcVersionId);

		checkGrantObject(false, GWConstants.GRANT_READ);

		// get metadata
		S3Metadata s3SrcMetadata = S3Metadata.getS3Metadata(srcMeta.getMeta());

		S3Encryption s3SrcEncryption;
		if (!Strings.isNullOrEmpty(copySourceCustomerAlgorithm)) {
			s3SrcEncryption = new S3Encryption(copySourceCustomerAlgorithm, copySourceCustomerKey, copySourceCustomerKeyMD5, s3Parameter);
		} else {
			s3SrcEncryption = new S3Encryption("get", s3SrcMetadata, s3Parameter);
		}
		s3SrcEncryption.build();

		// Check match
		if (!Strings.isNullOrEmpty(copySourceIfMatch)) {
			logger.debug(GWConstants.LOG_SOURCE_ETAG_MATCH, s3SrcMetadata.getETag(), copySourceIfMatch.replace(GWConstants.DOUBLE_QUOTE, ""));
			if (!GWUtils.maybeQuoteETag(s3SrcMetadata.getETag()).equals(copySourceIfMatch.replace(GWConstants.DOUBLE_QUOTE, ""))) {
				throw new GWException(GWErrorCode.PRECONDITION_FAILED, s3Parameter);
			}
		}

		if (!Strings.isNullOrEmpty(copySourceIfNoneMatch)) {
			logger.debug(GWConstants.LOG_SOURCE_ETAG_MATCH, s3SrcMetadata.getETag(), copySourceIfNoneMatch.replace(GWConstants.DOUBLE_QUOTE, ""));
			if (GWUtils.maybeQuoteETag(s3SrcMetadata.getETag()).equals(copySourceIfNoneMatch.replace(GWConstants.DOUBLE_QUOTE, ""))) {
				throw new GWException(GWErrorCode.DOES_NOT_MATCH, String.format(GWConstants.LOG_ETAG_IS_MISMATCH), s3Parameter);
			}
		}

		if (!Strings.isNullOrEmpty(copySourceIfModifiedSince)) {
			long copySourceIfModifiedSinceLong = Long.parseLong(copySourceIfModifiedSince);
			if (copySourceIfModifiedSinceLong != -1) {
				Date modifiedSince = new Date(copySourceIfModifiedSinceLong);
				if (s3SrcMetadata.getLastModified().before(modifiedSince)) {
					throw new GWException(GWErrorCode.DOES_NOT_MATCH, String.format(GWConstants.LOG_MATCH_BEFORE, s3SrcMetadata.getLastModified(), modifiedSince), s3Parameter);
				}
			}
		}
		
		if (!Strings.isNullOrEmpty(copySourceIfUnmodifiedSince)) {
			long copySourceIfUnmodifiedSinceLong = Long.parseLong(copySourceIfUnmodifiedSince);
			if (copySourceIfUnmodifiedSinceLong != -1) {
				Date unmodifiedSince = new Date(copySourceIfUnmodifiedSinceLong);
				if (s3SrcMetadata.getLastModified().after(unmodifiedSince)) {
					throw new GWException(GWErrorCode.PRECONDITION_FAILED, String.format(GWConstants.LOG_MATCH_AFTER, s3SrcMetadata.getLastModified(), unmodifiedSince), s3Parameter);
				}
			}
		}
		
		// Check copy source Range
		S3Range s3Range = new S3Range(s3Parameter);
		if (!Strings.isNullOrEmpty(copySourceRange)) {
			logger.info(GWConstants.LOG_UPLOAD_PART_COPY_SOURCE_RANGE, copySourceRange, s3SrcMetadata.getContentLength());
			s3Range.parseRange(copySourceRange, s3SrcMetadata.getContentLength(), true);
		}

		// 
		ObjMultipart objMultipart = null;
		Multipart multipart = null;
		try {
			objMultipart = getInstanceObjMultipart(bucket);
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
		S3Metadata s3Metadata = S3Metadata.getS3Metadata(multipart.getMeta());

		// if (!Strings.isNullOrEmpty(customerKey)) {
		// 	if (customerKey.compareTo(s3Metadata.getCustomerKey()) != 0) {
		// 		logger.error("different customer key : {}/{}", customerKey, s3Metadata.getCustomerKey());
		// 		new GWException(GWErrorCode.INVALID_REQUEST, s3Parameter);
		// 	}
		// }

		// check encryption
		// S3ObjectEncryption s3ObjectEncryption = new S3ObjectEncryption(s3Parameter, s3Metadata);
		// s3ObjectEncryption.build();

		Metadata objMeta = createLocal(multipart.getDiskPoolId(), bucket, object, "null");
		String path = DiskManager.getInstance().getLocalPath(objMeta.getPrimaryDisk().getId());
		if (path == null) {
			path = DiskManager.getInstance().getPath(objMeta.getPrimaryDisk().getId());
		}
		
		S3Object s3Object = null;
		// S3ObjectOperation objectOperation = new S3ObjectOperation(objMeta, s3Metadata, s3Parameter, null, s3ObjectEncryption);
		IObjectManager objectManager = new VFSObjectManager();
		try {
			// s3Object = objectOperation.uploadPartCopy(path, srcMeta, s3Range, s3SrcObjectEncryption);
			s3Object = objectManager.uploadPartCopy(s3Parameter, srcMeta, s3SrcEncryption, s3Range, objMeta);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
		objMeta.setSize(s3Object.getFileSize());
		objMeta.setEtag(s3Object.getEtag());
		// objMultipart.startSingleUpload(object, uploadId, Integer.parseInt(partNumber), "", "", s3Object.getEtag(), s3Object.getFileSize(), objMeta.getPrimaryDisk().getId());
		objMultipart.startSingleUpload(objMeta, uploadId, Integer.parseInt(partNumber));
		objMultipart.finishSingleUpload(uploadId, Integer.parseInt(partNumber));

		s3Parameter.setFileSize(s3Object.getFileSize());
		s3Parameter.getResponse().setCharacterEncoding(Constants.CHARSET_UTF_8);
		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
		try (Writer writer = s3Parameter.getResponse().getWriter()) {
			s3Parameter.getResponse().setContentType(GWConstants.XML_CONTENT_TYPE);
			XMLStreamWriter xmlout = xmlOutputFactory.createXMLStreamWriter(writer);
			xmlout.writeStartDocument();
			xmlout.writeStartElement(GWConstants.COPY_PART_RESULT);
			xmlout.writeDefaultNamespace(GWConstants.AWS_XMLNS);

			writeSimpleElement(xmlout, GWConstants.LAST_MODIFIED, formatDate(s3Object.getLastModified()));
			writeSimpleElement(xmlout, GWConstants.ETAG, GWUtils.maybeQuoteETag(s3Object.getEtag()));

			xmlout.writeEndElement();
			xmlout.flush();
		} catch (XMLStreamException | IOException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
		logger.info("End UploadPartCopy ... uploadId:{}, partNumber:{}, size:{}", uploadId, partNumber, s3Object.getFileSize());
		s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
	}
}
