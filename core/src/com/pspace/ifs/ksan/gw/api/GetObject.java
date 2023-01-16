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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.object.ResultRange;
import com.pspace.ifs.ksan.gw.object.S3ObjectEncryption;
import com.pspace.ifs.ksan.gw.object.S3ObjectOperation;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.gw.utils.GWConfig;

import org.slf4j.LoggerFactory;

public class GetObject extends S3Request {

	public GetObject(S3Parameter s3Parameter) {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(GetObject.class);
	}

	@Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_GET_OBJECT_START);
		
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);
		String object = s3Parameter.getObjectName();
		logger.debug(GWConstants.LOG_BUCKET_OBJECT, bucket, object);

		GWUtils.checkCors(s3Parameter);
		
		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}

		String versionId = s3RequestData.getVersionId();
		String range = s3RequestData.getRange();
		String ifMatch = s3RequestData.getIfMatch();
		String ifNoneMatch = s3RequestData.getIfNoneMatch();
		String ifModifiedSince = s3RequestData.getIfModifiedSince();
		String ifUnmodifiedSince = s3RequestData.getIfUnmodifiedSince();

		Metadata objMeta = null;
		if (Strings.isNullOrEmpty(versionId)) {
			objMeta = open(bucket, object);
		} else {
			objMeta = open(bucket, object, versionId);
		}

		if (GWConfig.getInstance().isDBOP()) {
			s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
			return;
		}
		
		logger.debug(GWConstants.LOG_OBJECT_META, objMeta.toString());
		s3Parameter.setTaggingInfo(objMeta.getTag());

		if (Strings.isNullOrEmpty(versionId)) {
			if (!checkPolicyBucket(GWConstants.ACTION_GET_OBJECT, s3Parameter)) {
				checkGrantObject(false, GWConstants.GRANT_READ);
			}
		} else {
			if (!checkPolicyBucket(GWConstants.ACTION_GET_OBJECT_VERSION, s3Parameter)) {
				checkGrantObject(false, GWConstants.GRANT_READ);
			}
		}

		versionId = objMeta.getVersionId();
		s3Parameter.setVersionId(versionId);

		S3Metadata s3Metadata = S3Metadata.getS3Metadata(objMeta.getMeta());

		// check customer-key
		if (!Strings.isNullOrEmpty(s3Metadata.getCustomerKey())) {
			if (!Strings.isNullOrEmpty(s3RequestData.getServerSideEncryptionCustomerKey())) {
				if (!s3Metadata.getCustomerKey().equals(s3RequestData.getServerSideEncryptionCustomerKey())) {
					logger.warn(GWConstants.LOG_GET_OBJECT_CUSTOMER_KEY_NO_MATCH);
					throw new GWException(GWErrorCode.KEY_DOES_NOT_MATCH, s3Parameter);
				}
			} else {
				logger.warn(GWConstants.ENCRYPTION_CUSTOMER_KEY_IS_NULL);
				throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
			}
		}

		// Check match
		if (!Strings.isNullOrEmpty(ifMatch)) {
			logger.debug(GWConstants.LOG_GET_OBJECT_IF_MATCH_ETAG, s3Metadata.getETag(), ifMatch.replace(GWConstants.DOUBLE_QUOTE, ""));
			if (!s3Metadata.getETag().equals(ifMatch.replace(GWConstants.DOUBLE_QUOTE, ""))) {
				logger.error(GWConstants.LOG_GET_OBJECT_ETAG_DIFFERENT);
				throw new GWException(GWErrorCode.PRECONDITION_FAILED, s3Parameter);
			}
		}

		if (!Strings.isNullOrEmpty(ifNoneMatch)) {
			logger.debug(GWConstants.LOG_GET_OBJECT_IF_NONE_MATCH_ETAG, s3Metadata.getETag(), ifNoneMatch.replace(GWConstants.DOUBLE_QUOTE, ""));
			if (s3Metadata.getETag().equals(ifNoneMatch.replace(GWConstants.DOUBLE_QUOTE, ""))) {
				logger.error(GWConstants.LOG_GET_OBJECT_ETAG_SAME);
				throw new GWException(GWErrorCode.DOES_NOT_MATCH, String.format(GWConstants.LOG_ETAG_IS_MISMATCH), s3Parameter);
			}
		}

		if (!Strings.isNullOrEmpty(ifModifiedSince)) {
			Date modifiedSince = new Date(ifModifiedSince);
			if (s3Metadata.getLastModified().before(modifiedSince)) {
				throw new GWException(GWErrorCode.DOES_NOT_MATCH, String.format(GWConstants.LOG_MATCH_BEFORE, s3Metadata.getLastModified(), modifiedSince), s3Parameter);
			}
		}
		
		if (!Strings.isNullOrEmpty(ifUnmodifiedSince)) {
			Date unmodifiedSince = new Date(ifUnmodifiedSince);
			if (s3Metadata.getLastModified().after(unmodifiedSince)) {
				throw new GWException(GWErrorCode.PRECONDITION_FAILED, String.format(GWConstants.LOG_MATCH_AFTER, s3Metadata.getLastModified(), unmodifiedSince), s3Parameter);
			}
		}

		// check encryption
		S3ObjectEncryption s3ObjectEncryption = new S3ObjectEncryption(s3Parameter, s3Metadata);
		s3ObjectEncryption.build();

		ResultRange resultRange = new ResultRange(range, s3Metadata, s3Parameter);

		addMetadataToResponse(s3Parameter.getResponse(), s3Metadata, resultRange.getContentLengthHeaders(), resultRange.getStreamSize()); 
		
		S3ObjectOperation objectOperation = new S3ObjectOperation(objMeta, s3Metadata, s3Parameter, versionId, s3ObjectEncryption);
		try {
			objectOperation.getObject(resultRange.getS3Range());
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		if (GWConfig.getInstance().isS3OP()) {
			s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
			return;
		}

		s3Parameter.getResponse().setStatus(resultRange.getStatus());
	}

	public void addResponseHeaderWithOverride(HttpServletRequest request, HttpServletResponse response,
			String headerName, String overrideHeaderName, String value) {
		
		String override = request.getParameter(overrideHeaderName);

		// NPE in if value is null
		override = (!Strings.isNullOrEmpty(override)) ? override : value;

		if (!Strings.isNullOrEmpty(override)) {
			response.addHeader(headerName, override);
		}
	}

	public void addMetadataToResponse(HttpServletResponse response, S3Metadata metadata, List<String> contentsHeaders,
			Long streamSize) throws GWException {
		try {
			addResponseHeaderWithOverride(s3Parameter.getRequest(), response,
					HttpHeaders.CACHE_CONTROL, GWConstants.RESPONSE_CACHE_CONTROL,
					metadata.getCacheControl());
			addResponseHeaderWithOverride(s3Parameter.getRequest(), response,
					HttpHeaders.CONTENT_ENCODING, GWConstants.RESPONSE_CONTENT_ENCODING,
					metadata.getContentEncoding()); 
			addResponseHeaderWithOverride(s3Parameter.getRequest(), response,
					HttpHeaders.CONTENT_LANGUAGE, GWConstants.RESPONSE_CONTENT_LANGUAGE,
					metadata.getContentLanguage());
			addResponseHeaderWithOverride(s3Parameter.getRequest(), response,
					HttpHeaders.CONTENT_DISPOSITION, GWConstants.RESPONSE_CONTENT_DISPOSITION,
					metadata.getContentDisposition());
			addResponseHeaderWithOverride(s3Parameter.getRequest(), response,
					HttpHeaders.CONTENT_TYPE, GWConstants.RESPONSE_CONTENT_TYPE,
					metadata.getContentType());
			
			Collection<String> contentRanges = contentsHeaders;
			if (contentsHeaders != null && !contentRanges.isEmpty()) {
				for (String contents : contentsHeaders) {
					response.addHeader(HttpHeaders.CONTENT_RANGE, contents);
				}
				
				response.addHeader(HttpHeaders.ACCEPT_RANGES, GWConstants.BYTES);
				response.addHeader(HttpHeaders.CONTENT_LENGTH, streamSize.toString());
			} else {
				response.addHeader(HttpHeaders.CONTENT_LENGTH, metadata.getContentLength().toString());	
				logger.debug(GWConstants.LOG_GET_OBJECT_CONTENT_LENGTH, metadata.getContentLength());
			}
					
			String overrideContentType = s3Parameter.getRequest().getParameter(GWConstants.RESPONSE_CONTENT_TYPE);
			response.setContentType(overrideContentType != null ? overrideContentType : metadata.getContentType());
			
			if (!Strings.isNullOrEmpty(metadata.getCustomerAlgorithm())) {
				response.addHeader(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM, metadata.getCustomerAlgorithm());
			}
			
			if (!Strings.isNullOrEmpty(metadata.getCustomerKey())) {
				response.addHeader(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY, metadata.getCustomerKey());
			}
			
			if (!Strings.isNullOrEmpty(metadata.getCustomerKeyMD5())) {
				response.addHeader(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5, metadata.getCustomerKeyMD5());
			}
			
			if (!Strings.isNullOrEmpty(metadata.getServersideEncryption())) {
				response.addHeader(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION, metadata.getServersideEncryption());
			}

			if (!Strings.isNullOrEmpty(metadata.getLockMode())) {
				response.addHeader(GWConstants.X_AMZ_OBJECT_LOCK_MODE, metadata.getLockMode());
			}

			if (!Strings.isNullOrEmpty(metadata.getLockExpires())) {
				response.addHeader(GWConstants.X_AMZ_OBJECT_LOCK_RETAIN_UNTIL_DATE, metadata.getLockExpires());
			}

			if (!Strings.isNullOrEmpty(metadata.getLegalHold())) {
				response.addHeader(GWConstants.X_AMZ_OBJECT_LOCK_LEGAL_HOLD, metadata.getLegalHold());
			}

			if (metadata.getUserMetadata() != null) {
				for (Map.Entry<String, String> entry : metadata.getUserMetadata().entrySet()) {
					response.addHeader(entry.getKey(), entry.getValue());
					logger.debug(GWConstants.LOG_GET_OBJECT_USER_META_DATA, entry.getKey(), entry.getValue());
				}
			}
			
			response.addHeader(HttpHeaders.ETAG, GWUtils.maybeQuoteETag(metadata.getETag()));
			
			String overrideExpires = s3Parameter.getRequest().getParameter(GWConstants.RESPONSE_EXPIRES);
			if (overrideExpires != null) {
				response.addHeader(HttpHeaders.EXPIRES, overrideExpires);
			} else {
				Date expires = metadata.getExpires();
				if (expires != null) {
					response.addDateHeader(HttpHeaders.EXPIRES, expires.getTime());
				}
			}
			
			logger.debug(GWConstants.LOG_GET_OBJECT_MODIFIED, metadata.getLastModified().getTime());
			response.addDateHeader(HttpHeaders.LAST_MODIFIED, metadata.getLastModified().getTime());
			
			if (!Strings.isNullOrEmpty(metadata.getTaggingCount())) {
				response.addHeader(GWConstants.X_AMZ_TAGGING_COUNT, metadata.getTaggingCount());
			}

			if (!Strings.isNullOrEmpty(metadata.getVersionId())) {
				if (metadata.getVersionId().equalsIgnoreCase(GWConstants.VERSIONING_DISABLE_TAIL)) {
					response.addHeader(GWConstants.X_AMZ_VERSION_ID, GWConstants.VERSIONING_DISABLE_TAIL);
				} else {
					response.addHeader(GWConstants.X_AMZ_VERSION_ID, metadata.getVersionId());
				}
			} else {
				response.addHeader(GWConstants.X_AMZ_VERSION_ID, GWConstants.VERSIONING_DISABLE_TAIL);
			}
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
	}
}
