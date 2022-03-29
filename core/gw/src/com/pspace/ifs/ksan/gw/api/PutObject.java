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
import java.security.InvalidParameterException;
import java.util.ArrayList;

import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Strings;
import com.google.common.hash.HashCode;
import com.google.common.io.BaseEncoding;
import com.google.common.net.HttpHeaders;
import com.pspace.ifs.ksan.gw.data.DataPutObject;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy;
import com.pspace.ifs.ksan.gw.format.ObjectLockConfiguration;
import com.pspace.ifs.ksan.gw.format.Tagging;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.AccessControlList;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.Owner;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.AccessControlList.Grant;
import com.pspace.ifs.ksan.gw.format.Tagging.TagSet;
import com.pspace.ifs.ksan.gw.format.Tagging.TagSet.Tag;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.object.S3Object;
import com.pspace.ifs.ksan.gw.object.S3ObjectOperation;
import com.pspace.ifs.ksan.gw.object.S3ServerSideEncryption;
import com.pspace.ifs.ksan.gw.utils.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConfig;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

import org.slf4j.LoggerFactory;



public class PutObject extends S3Request {

	public PutObject(S3Parameter s3Parameter) {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(PutObject.class);
	}

	@Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_PUT_OBJECT_START);
		
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);
		
		String object = s3Parameter.getObjectName();
		logger.debug(GWConstants.LOG_BUCKET_OBJECT, bucket, object);

		S3Bucket s3Bucket = new S3Bucket();
		s3Bucket.setCors(getBucketInfo().getCors());
		s3Bucket.setAccess(getBucketInfo().getAccess());
		s3Parameter.setBucket(s3Bucket);
		GWUtils.checkCors(s3Parameter);
		
		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}

		checkGrantBucket(s3Parameter.isPublicAccess(), String.valueOf(s3Parameter.getUser().getUserId()), GWConstants.GRANT_WRITE);
		
		DataPutObject dataPutObject = new DataPutObject(s3Parameter);
		dataPutObject.extract();

		S3Metadata s3Metadata = new S3Metadata();

		String cacheControl = dataPutObject.getCacheControl();
		String contentDisposition = dataPutObject.getContentDisposition();
		String contentEncoding = dataPutObject.getContentEncoding();
		String contentLanguage = dataPutObject.getContentLanguage();
		String contentType = dataPutObject.getContentType();
		String contentLengthString = dataPutObject.getContentLength();
		String decodedContentLengthString = dataPutObject.getDecodedContentLength();
		String contentMD5String = dataPutObject.getContentMD5();
		String customerAlgorithm = dataPutObject.getServerSideEncryptionCustomerAlgorithm();
		String customerKey = dataPutObject.getServerSideEncryptionCustomerKey();
		String customerKeyMD5 = dataPutObject.getServerSideEncryptionCustomerKeyMD5();
		String serversideEncryption = dataPutObject.getServerSideEncryption();

		s3Metadata.setOwnerId(String.valueOf(s3Parameter.getUser().getUserId()));
		s3Metadata.setOwnerName(s3Parameter.getUser().getUserName());
		s3Metadata.setUserMetadataMap(dataPutObject.getUserMetadata());
		
		if (!Strings.isNullOrEmpty(serversideEncryption)) {
			if (!GWConstants.AES256.equalsIgnoreCase(serversideEncryption)) {
				logger.error(GWErrorCode.NOT_IMPLEMENTED.getMessage() + GWConstants.SERVER_SIDE_OPTION);
				throw new GWException(GWErrorCode.NOT_IMPLEMENTED, s3Parameter);
			} else {
				s3Metadata.setServersideEncryption(serversideEncryption);
			}
		}
		
		if (!Strings.isNullOrEmpty(cacheControl)) {
			s3Metadata.setCacheControl(cacheControl);
		}
		if (!Strings.isNullOrEmpty(contentDisposition)) {
			s3Metadata.setContentDisposition(contentDisposition);
		}
		if (!Strings.isNullOrEmpty(contentEncoding)) {
			s3Metadata.setContentEncoding(contentEncoding);
		}
		if (!Strings.isNullOrEmpty(contentLanguage)) {
			s3Metadata.setContentLanguage(contentLanguage);
		}
		if (!Strings.isNullOrEmpty(contentType)) {
			s3Metadata.setContentType(contentType);
		}
		if (!Strings.isNullOrEmpty(customerAlgorithm)) {
			s3Metadata.setCustomerAlgorithm(customerAlgorithm);
		}
		if (!Strings.isNullOrEmpty(customerKey)) {
			s3Metadata.setCustomerKey(customerKey);
		}
		if (!Strings.isNullOrEmpty(customerKeyMD5)) {
			 s3Metadata.setCustomerKeyMD5(customerKeyMD5);
		}

		if (!Strings.isNullOrEmpty(decodedContentLengthString)) {
			contentLengthString = decodedContentLengthString;
		}

		HashCode contentMD5 = null;
		if (!Strings.isNullOrEmpty(contentMD5String)) {
			s3Metadata.setContentMD5(contentMD5String);
			try {
				contentMD5 = HashCode.fromBytes(BaseEncoding.base64().decode(contentMD5String));
			} catch (IllegalArgumentException iae) {
				PrintStack.logging(logger, iae);
				throw new GWException(GWErrorCode.INVALID_DIGEST, iae, s3Parameter);
			}
			if (contentMD5.bits() != MD5.bits()) {
				logger.error(GWErrorCode.INVALID_DIGEST.getMessage() + GWConstants.LOG_PUT_OBJECT_HASHCODE_ILLEGAL);
				throw new GWException(GWErrorCode.INVALID_DIGEST, s3Parameter);
			}
		}

		long contentLength;
		if (Strings.isNullOrEmpty(contentLengthString)) {
			logger.error(GWErrorCode.MISSING_CONTENT_LENGTH.getMessage());
			throw new GWException(GWErrorCode.MISSING_CONTENT_LENGTH, s3Parameter);
		} else {
			try {
				contentLength = Long.parseLong(contentLengthString);
				s3Metadata.setContentLength(contentLength);
			} catch (NumberFormatException nfe) {
				PrintStack.logging(logger, nfe);
				throw new GWException(GWErrorCode.INVALID_ARGUMENT, nfe, s3Parameter);
			}
		}

		accessControlPolicy = new AccessControlPolicy();
		accessControlPolicy.aclList = new AccessControlList();
		accessControlPolicy.aclList.grants = new ArrayList<Grant>();
		accessControlPolicy.owner = new Owner();
		accessControlPolicy.owner.id = String.valueOf(s3Parameter.getUser().getUserId());
		accessControlPolicy.owner.displayName = s3Parameter.getUser().getUserName();

		String aclXml = GWUtils.makeAclXml(accessControlPolicy, 
										null, 
										dataPutObject.hasAclKeyword(), 
										null, 
										dataPutObject.getAcl(),
										getBucketInfo(),
										String.valueOf(s3Parameter.getUser().getUserId()),
										s3Parameter.getUser().getUserName(),
										dataPutObject.getGrantRead(),
										dataPutObject.getGrantWrite(), 
										dataPutObject.getGrantFullControl(), 
										dataPutObject.getGrantReadAcp(), 
										dataPutObject.getGrantWriteAcp(),
										s3Parameter);
		logger.debug(GWConstants.LOG_ACL, aclXml);
		String bucketEncryption = getBucketInfo().getEncryption();
		// check encryption
		S3ServerSideEncryption encryption = new S3ServerSideEncryption(bucketEncryption, serversideEncryption, customerAlgorithm, customerKey, customerKeyMD5, s3Parameter);
		encryption.build();

		// Tagging information
		String taggingCount = GWConstants.TAGGING_INIT;
		String taggingxml = "";
		Tagging tagging = new Tagging();
		tagging.tagset = new TagSet();
		
		if (!Strings.isNullOrEmpty(dataPutObject.getTagging())) {
			String strtaggingInfo = dataPutObject.getTagging();
			String[] strtagset = strtaggingInfo.split(GWConstants.AMPERSAND);
			int starttag = 0;
			for (String strtag : strtagset) {

				if(starttag == 0)
					tagging.tagset.tags = new ArrayList<Tag>();

				starttag+=1;

				Tag tag = new Tag();
				String[] keyvalue = strtag.split(GWConstants.EQUAL);
				if(keyvalue.length == GWConstants.TAG_KEY_SIZE) {
					tag.key = keyvalue[GWConstants.TAG_KEY_INDEX];
					tag.value = keyvalue[GWConstants.TAG_VALUE_INDEX];
				} else {
					tag.key = keyvalue[GWConstants.TAG_KEY_INDEX];
					tag.value = "";
				}

				tagging.tagset.tags.add(tag);
			}

			try {
				taggingxml = new XmlMapper().writeValueAsString(tagging);
			} catch (JsonProcessingException e) {
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}

			if (tagging != null) {

				if (tagging.tagset != null && tagging.tagset.tags != null) {
					for (Tag t : tagging.tagset.tags) {

						// key, value 길이 체크
						if (t.key.length() > GWConstants.TAG_KEY_MAX) {
							logger.error(GWConstants.LOG_PUT_OBJECT_TAGGING_KEY_LENGTH, t.key.length());
							throw new GWException(GWErrorCode.INVALID_TAG, s3Parameter);
						}

						if (t.value.length() > GWConstants.TAG_VALUE_MAX) {
							logger.error(GWConstants.LOG_PUT_OBJECT_TAGGING_VALUE_LENGTH, t.value.length());
							throw new GWException(GWErrorCode.INVALID_TAG, s3Parameter);
						}
					}
				}

				if ( tagging.tagset != null && tagging.tagset.tags != null ) {
					if(tagging.tagset.tags.size() > GWConstants.TAG_MAX_SIZE) {
						logger.error(GWConstants.LOG_PUT_OBJECT_TAGGING_SIZE, tagging.tagset.tags.size());
						throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);	
					}

					taggingCount = String.valueOf(tagging.tagset.tags.size());
				}
			}
		}

		if (!Strings.isNullOrEmpty(dataPutObject.getObjectLockMode())) {
			try {
				logger.debug(GWConstants.LOG_OBJECT_LOCK, getBucketInfo().getObjectLock());
				ObjectLockConfiguration oc = new XmlMapper().readValue(getBucketInfo().getObjectLock(), ObjectLockConfiguration.class);
				if (!oc.objectLockEnabled.equals(GWConstants.STATUS_ENABLED) ) {
					logger.error(GWConstants.LOG_PUT_OBJECT_LOCK_STATUS, oc.objectLockEnabled);
					throw new GWException(GWErrorCode.INVALID_REQUEST, s3Parameter);
				}
			} catch (IOException e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}

			if (!dataPutObject.getObjectLockMode().equals(GWConstants.GOVERNANCE) && !dataPutObject.getObjectLockMode().equals(GWConstants.COMPLIANCE) ) {
				logger.error(GWConstants.LOG_PUT_OBJECT_LOCK_MODE, dataPutObject.getObjectLockMode());
				throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
			}

			s3Metadata.setLockMode(dataPutObject.getObjectLockMode());
		}

		if (!Strings.isNullOrEmpty(dataPutObject.getObjectLockRetainUntilDate())) {
			if (!dataPutObject.getObjectLockMode().equals(GWConstants.GOVERNANCE) && !dataPutObject.getObjectLockMode().equals(GWConstants.COMPLIANCE)) {
				logger.error(GWConstants.LOG_PUT_OBJECT_LOCK_MODE, dataPutObject.getObjectLockMode());
				throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
			}

			try {
				ObjectLockConfiguration oc = new XmlMapper().readValue(getBucketInfo().getObjectLock(), ObjectLockConfiguration.class);
				if (!oc.objectLockEnabled.equals(GWConstants.STATUS_ENABLED) ) {
					logger.error(GWConstants.LOG_PUT_OBJECT_LOCK_STATUS, oc.objectLockEnabled);
					throw new GWException(GWErrorCode.INVALID_REQUEST, s3Parameter);
				}
			} catch (IOException e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}

			s3Metadata.setLockExpires(dataPutObject.getObjectLockRetainUntilDate());
		}

		if (!Strings.isNullOrEmpty(dataPutObject.getObjectLockLegalHold())) {
			try {
				ObjectLockConfiguration oc = new XmlMapper().readValue(getBucketInfo().getObjectLock(), ObjectLockConfiguration.class);
				if (!oc.objectLockEnabled.equals(GWConstants.STATUS_ENABLED) ) {
					logger.error(GWConstants.LOG_PUT_OBJECT_LOCK_STATUS, oc.objectLockEnabled);
					throw new GWException(GWErrorCode.INVALID_REQUEST, s3Parameter);
				}
			} catch (IOException e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}

			s3Metadata.setLegalHold(dataPutObject.getObjectLockLegalHold());
		}

		String versioningStatus = getBucketVersioning(bucket);
		String versionId = null;
		Metadata objMeta = null;
		try {
			// check exist object
			objMeta = open(bucket, object);
			if (GWConstants.VERSIONING_ENABLED.equalsIgnoreCase(versioningStatus)) {
				versionId = String.valueOf(System.nanoTime());
			} else {
				versionId = GWConstants.VERSIONING_DISABLE_TAIL;
			}
		} catch (GWException e) {
			logger.info(e.getMessage());
			if (GWConfig.getReplicaCount() > 1) {
				objMeta = create(bucket, object);
			} else {
				objMeta = createLocal(bucket, object);
			}
			if (GWConstants.VERSIONING_ENABLED.equalsIgnoreCase(versioningStatus)) {
				versionId = String.valueOf(System.nanoTime());
			} else {
				versionId = GWConstants.VERSIONING_DISABLE_TAIL;
			}
		}

		S3ObjectOperation objectOperation = new S3ObjectOperation(objMeta, s3Metadata, s3Parameter, versionId, encryption);
		S3Object s3Object = objectOperation.putObject();

		s3Metadata.setETag(s3Object.getEtag());
		s3Metadata.setSize(s3Object.getFileSize());
		s3Metadata.setContentLength(s3Object.getFileSize());
		s3Metadata.setTier(GWConstants.AWS_TIER_STANTARD);
		s3Metadata.setLastModified(s3Object.getLastModified());
		s3Metadata.setDeleteMarker(s3Object.getDeleteMarker());
		s3Metadata.setVersionId(s3Object.getVersionId());
		s3Metadata.setTaggingCount(taggingCount);
		if(encryption.isEnableSSEServer()) {
			s3Metadata.setServersideEncryption(GWConstants.AES256);
		}

		s3Parameter.setFileSize(s3Object.getFileSize());

		ObjectMapper jsonMapper = new ObjectMapper();
		String jsonmeta = "";
		try {
			jsonmeta = jsonMapper.writeValueAsString(s3Metadata);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		logger.debug(GWConstants.LOG_PUT_OBJECT_PRIMARY_DISK_ID, objMeta.getPrimaryDisk().getId());
		try {
			int result;
			objMeta.set(s3Object.getEtag(), taggingxml, jsonmeta, aclXml, s3Object.getFileSize());
        	objMeta.setVersionId(versionId, GWConstants.OBJECT_TYPE_FILE, true);
			result = insertObject(bucket, object, objMeta);
			logger.debug(GWConstants.LOG_PUT_OBJECT_INFO, bucket, object, s3Object.getFileSize(), s3Object.getEtag(), aclXml, versionId);
		} catch (GWException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		s3Parameter.getResponse().addHeader(HttpHeaders.ETAG, GWUtils.maybeQuoteETag(s3Object.getEtag()));
		if (GWConstants.VERSIONING_ENABLED.equalsIgnoreCase(versioningStatus)) {
			s3Parameter.getResponse().addHeader(GWConstants.X_AMZ_VERSION_ID, s3Object.getVersionId());
			logger.debug(GWConstants.LOG_PUT_OBJECT_VERSIONID, s3Object.getVersionId());
		}
		s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
	}
}
