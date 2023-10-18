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
package com.pspace.ifs.ksan.gw.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.format.LifecycleConfiguration;
import com.pspace.ifs.ksan.gw.format.LifecycleConfiguration.Rule;
import com.pspace.ifs.ksan.gw.format.LoggingConfiguration;
import com.pspace.ifs.ksan.gw.format.LoggingConfiguration.LoggingEnabled;
import com.pspace.ifs.ksan.gw.format.LoggingConfiguration.LoggingEnabled.TargetGrants;
import com.pspace.ifs.ksan.gw.format.ObjectLockConfiguration;
import com.pspace.ifs.ksan.objmanager.ObjManager;
import com.pspace.ifs.ksan.gw.object.objmanager.ObjManagers;
import com.pspace.ifs.ksan.gw.utils.S3UserManager;

public class S3RequestData {
	protected String contentLength;
	protected S3Parameter s3Parameter;
	protected Logger logger;

	public S3RequestData(S3Parameter s3Parameter) {
		this.s3Parameter = new S3Parameter(s3Parameter);
		logger = LoggerFactory.getLogger(S3RequestData.class);
	}

	public String getContentLength() throws GWException {
		String contentLength = s3Parameter.getRequest().getHeader(HttpHeaders.CONTENT_LENGTH);
		if (!Strings.isNullOrEmpty(contentLength)) {
			long length = Long.parseLong(contentLength);
			if (length < 0) {
				throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
			}
		}
		return contentLength;
	}

	public String getUploadId() {
		return s3Parameter.getRequest().getParameter(GWConstants.UPLOAD_ID);
	}

	public String getRequestPayer() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_REQUEST_PAYER);
	}

	public String getExpectedBucketOwner() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_EXPECTED_BUCKET_OWNER);
	}

	public String getVersionId() {
		String versionId = s3Parameter.getRequest().getParameter(GWConstants.VERSION_ID);
		if (versionId == null) {
			versionId = s3Parameter.getRequest().getHeader(GWConstants.X_IFS_VERSION_ID);
		}

		return versionId;
		
	}

	public String getReplication() {
		if (s3Parameter.getRequest().getHeader(GWConstants.X_IFS_VERSION_ID) != null) {
			return GWConstants.IFS_HEADER_REPLICATION;
		} else {
			return null;
		}
	}

	public String getDr() {
		if (s3Parameter.getRequest().getHeader(GWConstants.X_IFS_DR) != null) {
			return GWConstants.IFS_HEADER_DR;
		} else {
			return null;
		}
	}

	public String getLogging() {
		if (s3Parameter.getRequest().getHeader(GWConstants.X_IFS_LOGGING) != null) {
			return GWConstants.IFS_HEADER_LOGGING;
		} else {
			return null;
		}
	}

	public String getCacheControl() {
		return s3Parameter.getRequest().getHeader(GWConstants.CACHE_CONTROL);
	}

	public String getContentDisposition() {
		return s3Parameter.getRequest().getHeader(GWConstants.CONTENT_DISPOSITION);
	}

	public String getContentEncoding() {
		return s3Parameter.getRequest().getHeader(GWConstants.CONTENT_ENCODING);
	}

	public String getContentLanguage() {
		return s3Parameter.getRequest().getHeader(GWConstants.CONTENT_LANGUAGE);
	}

	public String getContentType() {
		return s3Parameter.getRequest().getHeader(GWConstants.CONTENT_TYPE);
	}

	public String getCopySource() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_COPY_SOURCE);
	}

	public String getExpires() {
		return s3Parameter.getRequest().getHeader(GWConstants.EXPIRES);
	}

	public String getMetadataDirective() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_METADATA_DIRECTIVE);
	}

	public String getTaggingDirective() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_TAGGING_DIRECTIVE);
	}

	public String getServerSideEncryption() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION);
	}

	public String getServerSideEncryptionCustomerAlgorithm() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM);
	}

	public String getServerSideEncryptionCustomerKey() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY);
	}

	public String getServerSideEncryptionCustomerKeyMD5() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5);
	}

	public String getServerSideEncryptionAwsKmsKeyId() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_AWS_KMS_KEY_ID);
	}

	public String getStorageClass() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_STORAGE_CLASS);
	}

	public String getWebsiteRedirectLocation() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_WEBSITE_REDIRECT_LOCATION);
	}

	public String getServerSideEncryptionContext() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CONTEXT);
	}

	public String getServerSideEncryptionBucketKeyEnabled() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_BUCKET_KEY_ENABLED);
	}

	public String getCopySourceServerSideEncryptionCustomerAlgorithm() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_COPY_SOURCE_SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM);
	}

	public String getCopySourceServerSideEncryptionCustomerKey() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_COPY_SOURCE_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY);
	}

	public String getCopySourceServerSideEncryptionCustomerKeyMD5() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_COPY_SOURCE_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5);
	}

	public String getTagging() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_TAGGING);
	}

	public String getObjectLockMode() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_OBJECT_LOCK_MODE);
	}

	public String getObjectLockRetainUntilDate() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_OBJECT_LOCK_RETAIN_UNTIL_DATE);
	}

	public String getObjectLockLegalHold() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_OBJECT_LOCK_LEGAL_HOLD);
	}

	public String getSourceExpectedBucketOwner() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_SOURCE_EXPECTED_BUCKET_OWNER);
	}

	public String getIfMatch() {
		return s3Parameter.getRequest().getHeader(GWConstants.IF_MATCH);
	}

	public String getIfNoneMatch() {
		return s3Parameter.getRequest().getHeader(GWConstants.IF_NONE_MATCH);
	}

	public String getIfModifiedSince() {
		return s3Parameter.getRequest().getHeader(GWConstants.IF_MODIFIED_SINCE);
	}

	public String getIfUnmodifiedSince() {
		return s3Parameter.getRequest().getHeader(GWConstants.IF_UNMODIFIED_SINCE);
	}

	public String getCopySourceIfMatch() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_COPY_SOURCE_IF_MATCH);
	}

	public String getCopySourceIfModifiedSince() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_COPY_SOURCE_IF_MODIFIED_SINCE);
	}

	public String getCopySourceIfNoneMatch() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_COPY_SOURCE_IF_NONE_MATCH);
	}

	public String getCopySourceIfUnmodifiedSince() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_COPY_SOURCE_IF_UNMODIFIED_SINCE);
	}

	public String getAcl() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_ACL);
	}

	public String getGrantFullControl() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_GRANT_FULL_CONTROL);
	}

	public String getGrantRead() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_GRANT_READ);
	}

	public String getGrantReadAcp() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_GRANT_READ_ACP);
	}

	public String getGrantWrite() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_GRANT_WRITE);
	}

	public String getGrantWriteAcp() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_GRANT_WRITE_ACP);
	}

	public String getDecodedContentLength() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_DECODED_CONTENT_LENGTH);
	}

	public Map<String, String> getUserMetadata() {
		Map<String, String> userMetadata = new TreeMap<String, String>();
		for (String headerName : Collections.list(s3Parameter.getRequest().getHeaderNames())) {
			if (headerName.startsWith(GWConstants.USER_METADATA_PREFIX)) {
				userMetadata.put(headerName, s3Parameter.getRequest().getHeader(headerName));
			}
		}
		return userMetadata;
	}

	public String getBucketObjectLockEnabled() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_BUCKET_OBJECT_LOCK_ENABLED);
	}

	public String getMfa() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_MFA);
	}

	public String getBypassGovernanceRetention() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_BYPASS_GOVERNANCE_RETENTION);
	}

	public String getDeleteXml() throws GWException {
		return readXml();
	}

	public String getPartNumber() {
		return s3Parameter.getRequest().getParameter(GWConstants.PART_NUMBER);
	}

	public String getResponseCacheControl() {
		return s3Parameter.getRequest().getParameter(GWConstants.RESPONSE_CACHE_CONTROL);
	}

	public String getResponseContentDisposition() {
		return s3Parameter.getRequest().getParameter(GWConstants.RESPONSE_CONTENT_DISPOSITION);
	}

	public String getResponseContentEncoding() {
		return s3Parameter.getRequest().getParameter(GWConstants.RESPONSE_CONTENT_ENCODING);
	}

	public String getResponseContentLanguage() {
		return s3Parameter.getRequest().getParameter(GWConstants.RESPONSE_CONTENT_LANGUAGE);
	}

	public String getResponseContentType() {
		return s3Parameter.getRequest().getParameter(GWConstants.RESPONSE_CONTENT_TYPE);
	}

	public String getResponseExpires() {
		return s3Parameter.getRequest().getParameter(GWConstants.RESPONSE_EXPIRES);
	}

	public String getDelimiter() {
		return s3Parameter.getRequest().getParameter(GWConstants.DELIMITER);
	}

	public String getMaxKeys() {
		return s3Parameter.getRequest().getParameter(GWConstants.MAX_KEYS);
	}

	public String getMarker() {
		return s3Parameter.getRequest().getParameter(GWConstants.MARKER);
	}

	public String getEncodingType() {
		return s3Parameter.getRequest().getParameter(GWConstants.ENCODING_TYPE);
	}

	public String getMaxkeys() {
		return s3Parameter.getRequest().getParameter(GWConstants.MAX_KEYS);
	}

	public String getPrefix() {
		return s3Parameter.getRequest().getParameter(GWConstants.PREFIX);
	}

	public String getTag() {
        return s3Parameter.getRequest().getParameter(GWConstants.TAG);
    }

	public String getRange() {
		return s3Parameter.getRequest().getHeader(GWConstants.RANGE);
	}

	public String getKeyMarker() {
		return s3Parameter.getRequest().getParameter(GWConstants.KEY_MARKER);
	}

	public String getMaxUploads() {
		return s3Parameter.getRequest().getParameter(GWConstants.MAX_UPLOADS);
	}

	public String getUploadIdMarker() {
		return s3Parameter.getRequest().getParameter(GWConstants.UPLOAD_ID_MARKER);
	}

	public String getContinuationToken() {
		return s3Parameter.getRequest().getParameter(GWConstants.CONTINUATION_TOKEN);
	}

	public String getFetchOwner() {
		return s3Parameter.getRequest().getParameter(GWConstants.FETCH_OWNER);
	}

	public String getStartAfter() {
		return s3Parameter.getRequest().getParameter(GWConstants.START_AFTER);
	}

	public String getVersionIdMarker() {
		return s3Parameter.getRequest().getParameter(GWConstants.VERSION_ID_MARKER);
	}

	public String getMaxParts() {
		return s3Parameter.getRequest().getParameter(GWConstants.MAX_PARTS);
	}

	public String getPartNumberMarker() {
		return s3Parameter.getRequest().getParameter(GWConstants.PART_NUMBER_MARKER);
	}

	public String getAclXml() throws GWException {
		return readXml();
	}

	public String getContentMD5() {
		return s3Parameter.getRequest().getHeader(GWConstants.CONTENT_MD5);
	}

	public String getCorsXml() throws GWException {
		return readXml();
	}

	public String getEncryptionXml() throws GWException {
		return readXml();
	}

	public String getLifecycleXml() throws GWException {
		String lifecycleXml = readXml();

		XmlMapper xmlMapper = new XmlMapper();
		LifecycleConfiguration lcc;
		try {
			lcc = xmlMapper.readValue(lifecycleXml, LifecycleConfiguration.class);
		} catch (JsonMappingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		Map<String, String> id = new HashMap<String, String>(); 
		
		if (lcc.rules != null) {
			for (Rule rl : lcc.rules) {
				logger.debug("r1.id : {}", rl.id);
				if (rl.id != null) { 
					if (rl.id.length() > 255) {
						logger.error(GWConstants.LOG_DATA_LIFECYCLE_RULE_ID_LENGTH, rl.id.length());
						throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
					}
				
					id.put(rl.id, rl.id);
				} else {
					String generatedString = UUID.randomUUID().toString().substring(24).toUpperCase();
					id.put(generatedString, generatedString);
					rl.id = generatedString;
				}
				
				if (rl.status != null && rl.status.compareTo(GWConstants.STATUS_ENABLED) != 0 && rl.status.compareTo(GWConstants.STATUS_DISABLED) != 0) {
					logger.error(GWConstants.LOG_DATA_LIFECYCLE_R1_STATUS, rl.status);
					throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
				}

				// date check
				if( rl.expiration != null) {
					if(!Strings.isNullOrEmpty(rl.expiration.date)) {
						if(!rl.expiration.date.contains(GWConstants.LIFECYCLE_CONTAIN_TIME))
							throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
					}
				}
			}

			if( lcc.rules.size() > id.size() ) {
				logger.error(GWConstants.LOG_DATA_LIFECYCLE_LCC_RULE_SIZE, lcc.rules.size(), id.size());
				throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
			}
		}

		try {
			xmlMapper.setSerializationInclusion(Include.NON_EMPTY);
			lifecycleXml = xmlMapper.writeValueAsString(lcc);
		} catch (JsonMappingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		if(!lifecycleXml.contains(GWConstants.XML_VERSION)) {
			lifecycleXml = GWConstants.XML_VERSION_FULL_STANDALONE + lifecycleXml;
		}

		return lifecycleXml;
	}

	public String getLoggingXml() throws GWException {
		String LoggingXml = readXml();

		XmlMapper xmlMapper = new XmlMapper();
		LoggingConfiguration lc;
		try {
			lc = xmlMapper.readValue(LoggingXml, LoggingConfiguration.class);
			if (lc.loggingEnabled != null) {
				if (lc.loggingEnabled.targetBucket != null) {
					ObjManager objManager = null;
					boolean isTargetBucket = true;
					
					try {
						objManager = ObjManagers.getInstance().getObjManager();
						isTargetBucket = objManager.isBucketExist(lc.loggingEnabled.targetBucket);
					} catch (Exception e) {
						PrintStack.logging(logger, e);
						throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
					} 

					if (!isTargetBucket) {
						throw new GWException(GWErrorCode.INVALID_TARGET_BUCKET_FOR_LOGGING, s3Parameter);
					}
				}
				
				if (lc.loggingEnabled.targetGrants != null) {
					for (LoggingConfiguration.LoggingEnabled.TargetGrants.Grant grant : lc.loggingEnabled.targetGrants.grants) {
						if (grant.grantee != null && grant.grantee.id != null) {
							if (S3UserManager.getInstance().getUserById(grant.grantee.id) == null) {
								throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
							}
						}
					}
				}
			}
		} catch (JsonMappingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		return LoggingXml;
	}

	public String getObjectLockXml() throws GWException {
		// String ObjectLockXml = readXml();

		// XmlMapper xmlMapper = new XmlMapper();
		// @SuppressWarnings("unused")
		// ObjectLockConfiguration oc;
		// try {
		// 	oc = xmlMapper.readValue(ObjectLockXml, ObjectLockConfiguration.class);
		// } catch (JsonMappingException e) {
		// 	PrintStack.logging(logger, e);
		// 	throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		// } catch (JsonProcessingException e) {
		// 	PrintStack.logging(logger, e);
		// 	throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		// }

		// return ObjectLockXml;
		return readXml();
	}

	public String getConfirmRemoveSelfBucketAccess() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_CONFIRM_REMOVE_SELF_BUCKET_ACCESS);
	}

	public String getPolicyJson() throws GWException {
        return readJson();
    }

	public String getBucketObjectLockToken() {
        return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_BUCKET_OBJECT_LOCK_TOKEN);
    }

	public String getReplicationXml() throws GWException {
        return readXml();
    }

	public String getTaggingXml() throws GWException {
		return readXml();
	}

	public String getTagIndexXml() throws GWException {
		return readXml();
	}

	public String getVersioningXml() throws GWException {
		return readXml();
	}

	public String getWebsiteXml() throws GWException {
		return readXml();
	}

	public String getLegalHoldXml() throws GWException {
		return readXml();
	}

	public String getRetentionXml() throws GWException {
		return readXml();
    }

	public String getPublicAccessBlockXml() throws GWException {
		return readXml();
	}

	public String getRetoreXml() throws GWException {
		return readXml();
    }

	public String getCopySourceRange() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_COPY_SOURCE_RANGE);
	}

	public String getBucketConfiguration() throws GWException {
		return readXml();
	}

	public String getMultipartXml() throws GWException {
		String multipartXml = "";
		try {
			multipartXml = readXml();
		} catch (GWException e) {
			logger.error(GWErrorCode.MALFORMED_X_M_L.getMessage());
			throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
		}
	
		return multipartXml;
	}

	protected String readXml() throws GWException {
		String ret = null;
		byte[] srcByteBuf = new byte[Constants.MAXBUFSIZE];
		byte[] destByteBuf = new byte[Constants.MAXBUFSIZE];
		int readLength = 0;
		int bufferOff = 0;
		int bufferSize = Constants.BUFSIZE;
		OutputStream out = new ByteArrayOutputStream();
		try {
			while ((readLength = s3Parameter.getInputStream().read(srcByteBuf, 0, bufferSize)) >= 0) {
				s3Parameter.addRequestSize(readLength);
				System.arraycopy(srcByteBuf, 0, destByteBuf, bufferOff, readLength);
				bufferOff += readLength;

				if (bufferOff >= Constants.BUFSIZE) {
					out.write(destByteBuf, 0, bufferOff);
					bufferOff = 0;
				}
			}

			if (bufferOff != 0) {
				out.write(destByteBuf, 0, bufferOff);
			}
			ret = out.toString();
			out.close();
		} catch (IOException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		logger.info(ret);
		
		if(Strings.isNullOrEmpty(ret)) {
			logger.warn(GWErrorCode.INVALID_ARGUMENT.getMessage());
			throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
		}

		if(!ret.contains(GWConstants.XML_VERSION)) {
			ret = GWConstants.XML_VERSION_FULL_STANDALONE + ret;
		}
		
		return ret;
	}

	protected String readJson() throws GWException {
		String ret = null;
		byte[] srcByteBuf = new byte[Constants.MAXBUFSIZE];
		byte[] destByteBuf = new byte[Constants.MAXBUFSIZE];
		int readLength = 0;
		int bufferOff = 0;
		int bufferSize = Constants.BUFSIZE;
		OutputStream out = new ByteArrayOutputStream();
		try {
			while ((readLength = s3Parameter.getInputStream().read(srcByteBuf, 0, bufferSize)) >= 0) {
				s3Parameter.addRequestSize(readLength);
				System.arraycopy(srcByteBuf, 0, destByteBuf, bufferOff, readLength);
				bufferOff += readLength;

				if (bufferOff >= Constants.BUFSIZE) {
					out.write(destByteBuf, 0, bufferOff);
					bufferOff = 0;
				}
			}

			if (bufferOff != 0) {
				out.write(destByteBuf, 0, bufferOff);
			}

			ret = out.toString();
			out.close();
		} catch (IOException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		logger.info(ret);
		
		if(Strings.isNullOrEmpty(ret)) {
			logger.warn(GWErrorCode.INVALID_ARGUMENT.getMessage());
			throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
		}
		
		return ret;
	}

	public String getPolicyDelimter() {
		return getDelimiter();
	}

	public String getPolicyMaxkeys() {
		return getMaxKeys();
	}

	public String getPolicyPrefix() {
		return getPrefix();
	}

	public String getContentSHA256() {
		return s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_CONTENT_SHA256);
	}

	public String getXAmzAcl() {
		return getAcl();
	}

	public String getXAmzCopySource() {
		return getCopySource();
	}

	public String getXAmzGrantFullControl() {
		return getGrantFullControl();
	}

	public String getXAmzGrantRead() {
		return getGrantRead();
	}

	public String getXAmzGrantReadAcp() {
		return getGrantReadAcp();
	}

	public String getXAmzGrantWrite() {
		return getGrantWrite();
	}

	public String getXAmzGrantWriteAcp() {
		return getGrantWriteAcp();
	}

	public String getXAmzMetadataDirective() {
		return getMetadataDirective();
	}

	public String getXAmzServerSideEncryption() {
		return getServerSideEncryption();
	}
	
	public String getXAmzServerSideEncryptionAwsKmsKeyId() {
		return getServerSideEncryptionAwsKmsKeyId();
	}

	public String getXAmzStorageClass() {
		return getStorageClass();
	}

	public String getXAmzWebsiteRedirectLocation() {
		return getWebsiteRedirectLocation();
	}

	public String getXAmzObjectLockMode() {
		return getObjectLockMode();
	}

	public String getXAmzObjectLockRetainUntilDate() {
		return getObjectLockRetainUntilDate();
	}

	public String getXAmzObjectLockLegalHold() {
		return getObjectLockLegalHold();
	}
}
