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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64.Decoder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.pspace.ifs.ksan.gw.data.DataPostObject;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.PostPolicy;
import com.pspace.ifs.ksan.gw.format.Tagging;
import com.pspace.ifs.ksan.gw.format.Tagging.TagSet;
import com.pspace.ifs.ksan.gw.format.Tagging.TagSet.Tag;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.object.S3Object;
import com.pspace.ifs.ksan.gw.object.S3ObjectOperation;
import com.pspace.ifs.ksan.gw.object.S3ServerSideEncryption;
import com.pspace.ifs.ksan.gw.sign.S3Signing;
import com.pspace.ifs.ksan.gw.utils.GWConfig;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.gw.utils.PrintStack;
import com.pspace.ifs.ksan.objmanager.Metadata;

import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;

public class PostObject extends S3Request {
    public PostObject(S3Parameter s3Parameter) {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(PostObject.class);
	}

    @Override
    public void process() throws GWException {
        logger.info(GWConstants.LOG_POST_OBJECT_START);
		
        // throw new GWException(GWErrorCode.NOT_IMPLEMENTED);
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);
		String object = s3Parameter.getObjectName();
		logger.debug(GWConstants.LOG_BUCKET_OBJECT, bucket, object);

		S3Bucket s3Bucket = new S3Bucket();
		s3Bucket.setCors(getBucketInfo().getCors());
		s3Bucket.setAccess(getBucketInfo().getAccess());
		s3Parameter.setBucket(s3Bucket);

		DataPostObject dataPostObject = new DataPostObject(s3Parameter);
		dataPostObject.extract();
        
		if (Strings.isNullOrEmpty(dataPostObject.getKey())) {
			logger.info(GWErrorCode.BAD_REQUEST.getMessage());
			throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		}

        if (!Strings.isNullOrEmpty(dataPostObject.getPolicy())) {
			Decoder decoder = Base64.getDecoder();
			byte[] bytePostPolicy = decoder.decode(dataPostObject.getPolicy());
			String postPolicy = new String(bytePostPolicy);
			ObjectMapper jsonMapper = new ObjectMapper();

			PostPolicy postPolicyJson = null;
			try {
				postPolicyJson = jsonMapper.readValue(postPolicy, PostPolicy.class);
			} catch (JsonProcessingException e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
			}

			Map<String, String> conditionMap = new HashMap<String, String>();
			if (postPolicyJson.conditions == null) {
				logger.info(GWErrorCode.BAD_REQUEST.getMessage());
				throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
			}

			if (postPolicyJson.conditions.size() == 0) {
				logger.info(GWErrorCode.BAD_REQUEST.getMessage());
				throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
			}

			for (Object o : postPolicyJson.conditions) {
				// check
				logger.info("conditions ==> className(" + o.getClass().getName() + ")");
				if (o.getClass().getName().equals("java.util.LinkedHashMap")) {
					@SuppressWarnings("unchecked")
					Map<String, String> policyMap = (HashMap<String, String>) o;

					for (Map.Entry<String, String> s : policyMap.entrySet()) {
						logger.info("conditions ==> key(" + s.getKey() + "), value(" + s.getValue() + ")");
						dataPostObject.checkPolicy(s.getKey(), s.getValue());
						conditionMap.put(s.getKey().toLowerCase(), s.getValue());
					}
				} else if (o.getClass().getName().equals("java.util.ArrayList")) {
					@SuppressWarnings("unchecked")
					List<Object> policyList = (List<Object>) o;

					if (!((String) policyList.get(0)).equalsIgnoreCase("starts-with")
							&& !((String) policyList.get(0)).equalsIgnoreCase("eq")
							&& !((String) policyList.get(0)).equalsIgnoreCase("content-length-range")) {
						throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
					}

					if (((String) policyList.get(0)).equalsIgnoreCase("eq")) {
						logger.info("conditions ==> cond(" + policyList.get(0) + "), value1 (" + policyList.get(1)
								+ "), value2 (" + policyList.get(2) + ")");
						dataPostObject.checkPolicy((String) policyList.get(1), (String) policyList.get(2));
					} else if (((String) policyList.get(0)).equalsIgnoreCase("starts-with")) {
						logger.info("conditions ==> cond(" + policyList.get(0) + "), value1 (" + policyList.get(1)
								+ "), value2 (" + policyList.get(2) + ")");
						dataPostObject.checkPolityStarts((String) policyList.get(1), (String) policyList.get(2));
					} else if (((String) policyList.get(0)).equalsIgnoreCase("content-length-range")) {
						if (policyList.size() != 3) {
							logger.info(GWErrorCode.BAD_REQUEST.getMessage());
							throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
						}

						logger.info("conditions ==> cond(" + policyList.get(0) + "), value1 (" + policyList.get(1)
								+ "), value2 (" + policyList.get(2) + ")");
						if ((int) policyList.get(1) < 0) {
							logger.info(GWErrorCode.BAD_REQUEST.getMessage());
							throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
						}

						if (dataPostObject.getPayload().length < (int) policyList.get(1)) {
							logger.info(GWErrorCode.BAD_REQUEST.getMessage());
							throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
						}

						if ((int) policyList.get(2) < 0) {
							logger.info(GWErrorCode.BAD_REQUEST.getMessage());
							throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
						}

						if (dataPostObject.getPayload().length > (int) policyList.get(2)) {
							logger.info(GWErrorCode.BAD_REQUEST.getMessage());
							throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
						}
					}
				} else {
					logger.info(o.getClass().getName());
				}
			}

			if (Strings.isNullOrEmpty(postPolicyJson.expiration)) {
				logger.info(GWErrorCode.BAD_REQUEST.getMessage());
				throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
			} else {
				dataPostObject.setExpiration(postPolicyJson.getExpiration());
			}

			// bucket check
			if (Strings.isNullOrEmpty(conditionMap.get(GWConstants.CATEGORY_BUCKET))) {
				logger.info(GWErrorCode.ACCESS_DENIED.getMessage());
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
        }

		if (!Strings.isNullOrEmpty(dataPostObject.getAccessKey())) {
			if (Strings.isNullOrEmpty(dataPostObject.getSignature())) {
				logger.info(GWErrorCode.BAD_REQUEST.getMessage());
				throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
			}

			// signing check
			S3Signing s3signing = new S3Signing(s3Parameter);
			s3Parameter = s3signing.validatePost(dataPostObject);

			if (!isGrantBucket(String.valueOf(s3Parameter.getUser().getUserId()), GWConstants.GRANT_WRITE)) {
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
		} else {
			if (!isGrantBucket(GWConstants.LOG_REQUEST_ROOT_ID, GWConstants.GRANT_WRITE)) {
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
		}

		s3Parameter.setInputStream(new ByteArrayInputStream(dataPostObject.getPayload()));
		
		String cacheControl = dataPostObject.getCacheControl();
		String contentDisposition = dataPostObject.getContentDisposition();
		String contentEncoding = dataPostObject.getContentEncoding();
		String contentLanguage = dataPostObject.getContentLanguage();
		String contentType = dataPostObject.getContentType();
		String customerAlgorithm = dataPostObject.getServerSideEncryptionCustomerAlgorithm();
		String customerKey = dataPostObject.getServerSideEncryptionCustomerKey();
		String customerKeyMD5 = dataPostObject.getServerSideEncryptionCustomerKeyMD5();
		String serversideEncryption = dataPostObject.getServerSideEncryption();

		S3Metadata s3Metadata = new S3Metadata();
		s3Metadata.setOwnerId(Long.toString(s3Parameter.getUser().getUserId()));
		s3Metadata.setOwnerName(s3Parameter.getUser().getUserName());
		s3Metadata.setUserMetadataMap(dataPostObject.getUserMetadata());

		if (!Strings.isNullOrEmpty(serversideEncryption)) {
			if (!serversideEncryption.equalsIgnoreCase(GWConstants.AES256)) {
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

		String aclXml = GWUtils.makeAclXml(accessControlPolicy, 
										null, 
										dataPostObject.getAclKeyword(), 
										null, 
										dataPostObject.getAcl(),
										getBucketInfo(),
										String.valueOf(s3Parameter.getUser().getUserId()),
										s3Parameter.getUser().getUserName(),
										dataPostObject.getGrantRead(),
										dataPostObject.getGrantWrite(), 
										dataPostObject.getGrantFullControl(), 
										dataPostObject.getGrantReadAcp(), 
										dataPostObject.getGrantWriteAcp(),
										s3Parameter);

		String bucketEncryption = getBucketInfo().getEncryption();
		S3ServerSideEncryption encryption = new S3ServerSideEncryption(bucketEncryption, serversideEncryption, customerAlgorithm, customerKey, customerKeyMD5, s3Parameter);
		encryption.build();

		// Tagging information
		String taggingCount = GWConstants.TAGGING_INIT;
		String taggingxml = "";
		Tagging tagging = new Tagging();
		tagging.tagset = new TagSet();

		try {
			if ( dataPostObject.getTagging() != null )
				tagging = new XmlMapper().readValue(dataPostObject.getTagging(), Tagging.class);
		} catch (JsonProcessingException e) {
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		try {
			if ( tagging != null)
				taggingxml = new XmlMapper().writeValueAsString(tagging);
		} catch (JsonProcessingException e) {
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		if (tagging != null) {
			if (tagging.tagset != null && tagging.tagset.tags != null) {
				for (Tag t : tagging.tagset.tags) {

					// key, value 길이 체크
					if (t.key.length() > 128) {
						throw new GWException(GWErrorCode.INVALID_TAG, s3Parameter);
					}

					if (t.value.length() > 256) {
						throw new GWException(GWErrorCode.INVALID_TAG, s3Parameter);
					}
				}
			}

			if (tagging.tagset != null && tagging.tagset.tags != null) {
				if (tagging.tagset.tags.size() > 10) {
					throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
				}

				taggingCount = String.valueOf(tagging.tagset.tags.size());
			}
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
			objMeta.set(s3Object.getEtag(), taggingxml, jsonmeta, aclXml, s3Object.getFileSize());
        	objMeta.setVersionId(versionId, GWConstants.OBJECT_TYPE_FILE, true);
			insertObject(bucket, object, objMeta);
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
		if (!Strings.isNullOrEmpty(dataPostObject.getSuccessActionRedirect())) {
			try {
				s3Parameter.getResponse().sendRedirect(dataPostObject.getSuccessActionRedirect() + GWConstants.PARAMETER_BUCKET + bucket
						+ GWConstants.PARAMETER_KEY + s3Parameter.getObjectName() + GWConstants.PARAMETER_ETAG + s3Metadata.getETag() + GWConstants.ENCODING_DOUBLE_QUOTE);
			} catch (IOException e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
			}

			dataPostObject.setSuccessActionStatus(GWConstants.STATUS_SC_OK);
		}

		if (!Strings.isNullOrEmpty(dataPostObject.getSuccessActionStatus())) {
			switch (Integer.parseInt(dataPostObject.getSuccessActionStatus())) {
				case HttpServletResponse.SC_OK:
					s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
					break;
				case HttpServletResponse.SC_CREATED:
					s3Parameter.getResponse().setStatus(HttpServletResponse.SC_CREATED);
					break;
				case HttpServletResponse.SC_NO_CONTENT:
					s3Parameter.getResponse().setStatus(HttpServletResponse.SC_NO_CONTENT);
					break;
				default:
					s3Parameter.getResponse().setStatus(HttpServletResponse.SC_NO_CONTENT);
					break;
			}
		} else {
			s3Parameter.getResponse().setStatus(HttpServletResponse.SC_NO_CONTENT);
		}
    }
}
