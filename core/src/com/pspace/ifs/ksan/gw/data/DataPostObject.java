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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.libs.PrintStack;

import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.fileupload.FileUploadBase.FileUploadIOException;
import org.apache.commons.fileupload.MultipartStream.MalformedStreamException;
import org.slf4j.LoggerFactory;

public class DataPostObject extends S3RequestData{
    private String accesskey;
	private byte[] payload;
	private String key;
	private String policy;
	private String signature;
	private String algorithm;
	private String successActionStatus;
	private String successActionRedirect;
	private String minContentLengthRange;
	private String maxContentLengthRange;

	private String cacheControl;
	private String contentDisposition;
	private String contentEncoding;
	private String contentLanguage;
	private String contentMD5;
	private String contentType;
	private String expires;
	private String acl;
	private boolean aclkeyword = false;
	private String grantFullControl;
	private String grantRead;
	private String grantWrite;
	private String grantReadAcp;
	private String grantWriteAcp;
	private String decodedContentLength;
	private String serverSideEncryption;
	private String storageClass;
	private String websiteRedirectLocation;
	private String serverSideEncryptionCustomerAlgorithm;
	private String serverSideEncryptionCustomerKey;
	private String serverSideEncryptionCustomerKeyMD5;
	private String serverSideEncryptionAwsKmsKeyId;
	private String serverSideEncryptionContext;
	private String serverSideEncryptionBucketKeyEnabled;
	private String requestPayer;
	private String tagging;
	private String objectLockMode;
	private String objectLockRetainUntilDate;
	private String objectLockLegalHold;
	private String expectedBucketOwner;
	private Map<String, String> userMetadata;
	private String expiration;
    
    public DataPostObject(S3Parameter s3Parameter) throws GWException {
        super(s3Parameter);
        userMetadata = new TreeMap<String, String>();
		logger = LoggerFactory.getLogger(DataPostObject.class);
    }

    public void extract() throws GWException {
        String boundaryHeader = s3Parameter.getRequest().getHeader(HttpHeaders.CONTENT_TYPE);
		String boundary = boundaryHeader.substring(boundaryHeader.indexOf('=') + 1);
		try {
			s3Parameter.setInputStream(s3Parameter.getRequest().getInputStream());
		} catch (IOException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		MultipartStream multipartStream = new MultipartStream(s3Parameter.getInputStream(), boundary.getBytes(StandardCharsets.UTF_8), 4096, null);
		boolean nextPart = false;
		try {
			nextPart = multipartStream.skipPreamble();
		} catch (IOException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		while (nextPart) {
			String header;
			try {
				header = multipartStream.readHeaders();
				try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
					multipartStream.readBodyData(baos);
					if (GWUtils.isField(header, GWConstants.PARAMETER_ACL)) {
						aclkeyword = true;
						acl = new String(baos.toByteArray());
					} else if (GWUtils.isField(header, GWConstants.AWS_ACCESS_KEY_ID)
							|| GWUtils.isField(header, GWConstants.X_AMZ_CREDENTIAL)) {
						accesskey = new String(baos.toByteArray());
					} else if (GWUtils.isField(header, GWConstants.CONTENT_TYPE)) {
						contentType = new String(baos.toByteArray());
					} else if (GWUtils.isField(header, GWConstants.OBJECT_TYPE_FILE)) {
						payload = baos.toByteArray();
					} else if (GWUtils.isField(header, GWConstants.KEY)) {
						key = new String(baos.toByteArray());
					} else if (GWUtils.isField(header, GWConstants.PARAMETER_POLICY)) {
						policy = new String(baos.toByteArray());
					} else if (GWUtils.isField(header, GWConstants.SIGNATURE) || GWUtils.isField(header, GWConstants.X_AMZ_SIGNATURE)) {
						signature = new String(baos.toByteArray());
					} else if (GWUtils.isField(header, GWConstants.X_AMZ_ALGORITHM)) {
						algorithm = new String(baos.toByteArray());
					} else if (GWUtils.isField(header, GWConstants.CACHE_CONTROL)) {
						cacheControl = new String(baos.toByteArray());
					} else if (GWUtils.isField(header, GWConstants.CONTENT_DISPOSITION)) {
						contentDisposition = new String(baos.toByteArray());
					} else if (GWUtils.isField(header, GWConstants.CONTENT_ENCODING)) {
						contentEncoding = new String(baos.toByteArray());
					} else if (GWUtils.isField(header, GWConstants.EXPIRES)) {
						expires = new String(baos.toByteArray());
					} else if (GWUtils.isField(header, GWConstants.PARAMETER_TAGGING)) {
						tagging = new String(baos.toByteArray());
					} else if (GWUtils.isField(header, GWConstants.SUCCESS_ACTION_STATUS)) {
						successActionStatus = new String(baos.toByteArray());
					} else if (GWUtils.isField(header, GWConstants.SUCCESS_ACTION_REDIRECT)) {
						successActionRedirect = new String(baos.toByteArray());
					} else if (GWUtils.isField(header, GWConstants.X_AMZ_STORAGE_CLASS)) {
						storageClass = new String(baos.toByteArray());
					} else if (GWUtils.isField(header, GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION)) {
						serverSideEncryption = new String(baos.toByteArray());
					} else if (GWUtils.isField(header, GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_AWS_KMS_KEY_ID)) {
						serverSideEncryptionAwsKmsKeyId = new String(baos.toByteArray());
					} else if (GWUtils.isField(header, GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CONTEXT)) {
						serverSideEncryptionContext = new String(baos.toByteArray());
					} else if (GWUtils.isField(header, GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_BUCKET_KEY_ENABLED)) {
						serverSideEncryptionBucketKeyEnabled = new String(baos.toByteArray());
					} else if (GWUtils.isField(header, GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM)) {
						serverSideEncryptionCustomerAlgorithm = new String(baos.toByteArray());
					} else if (GWUtils.isField(header, GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY)) {
						serverSideEncryptionCustomerKey = new String(baos.toByteArray());
					} else if (GWUtils.isField(header, GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5)) {
						serverSideEncryptionCustomerKeyMD5 = new String(baos.toByteArray());
					} else if (GWUtils.startsField(header, GWConstants.USER_METADATA_PREFIX)) {
						String parseHeader = header.substring(GWConstants.CONTENT_DISPOSITION_FORM_DATA.length());
						parseHeader = parseHeader.substring(0, parseHeader.lastIndexOf(GWConstants.DOUBLE_QUOTE));
						userMetadata.put(parseHeader, new String(baos.toByteArray()));
					}
				} catch (IOException e) {
					PrintStack.logging(logger, e);
					throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
				}
				nextPart = multipartStream.readBoundary();
			} catch (FileUploadIOException e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
			} catch (MalformedStreamException e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
			}
		}
    }
    
    public boolean getAclKeyword() {
		return aclkeyword;
	}
	
	public String getAccessKey() {
		return accesskey;
	}

	public String getKey() {
		return key;
	}

	public String getPolicy() {
		return policy;
	}

	public String getSignature() {
		return signature;
	}

	public String getAlgorithm() {
		return algorithm;
	}

	public byte[] getPayload() {
		return payload;
	}

	public String getSuccessActionStatus() {
		return successActionStatus;
	}

	public void setSuccessActionStatus(String status) {
		successActionStatus = status;
	}

	public String getSuccessActionRedirect() {
		return successActionRedirect;
	}

	public String getCacheControl() {
		return cacheControl;
	}

	public String getContentDisposition() {
		return contentDisposition;
	}

	public String getContentEncoding() {
		return contentEncoding;
	}

	public String getContentLanguage() {
		return contentLanguage;
	}

	public String getContentMD5() {
		return contentMD5;
	}

	public String getContentType() {
		return contentType;
	}

	public String getExpires() {
		return expires;
	}

	public String getServerSideEncryption() {
		return serverSideEncryption;
	}

	public String getStorageClass() {
		return storageClass;
	}

	public String getWebsiteRedirectLocation() {
		return websiteRedirectLocation;
	}

	public String getServerSideEncryptionCustomerAlgorithm() {
		return serverSideEncryptionCustomerAlgorithm;
	}

	public String getServerSideEncryptionCustomerKey() {
		return serverSideEncryptionCustomerKey;
	}

	public String getServerSideEncryptionCustomerKeyMD5() {
		return serverSideEncryptionCustomerKeyMD5;
	}

	public String getServerSideEncryptionAwsKmsKeyId() {
		return serverSideEncryptionAwsKmsKeyId;
	}

	public String getServerSideEncryptionContext() {
		return serverSideEncryptionContext;
	}

	public String getServerSideEncryptionBucketKeyEnabled() {
		return serverSideEncryptionBucketKeyEnabled;
	}

	public String getRequestPayer() {
		return requestPayer;
	}

	public String getTagging() {
		return tagging;
	}

	public String getObjectLockMode() {
		return objectLockMode;
	}

	public String getObjectLockRetainUntilDate() {
		return objectLockRetainUntilDate;
	}

	public String getObjectLockLegalHold() {
		return objectLockLegalHold;
	}

	public String getExpectedBucketOwner() {
		return expectedBucketOwner;
	}

	public String getAcl() {
		return acl;
	}

	public String getGrantFullControl() {
		return grantFullControl;
	}

	public String getGrantRead() {
		return grantRead;
	}

	public String getGrantWrite() {
		return grantWrite;
	}

	public String getGrantReadAcp() {
		return grantReadAcp;
	}

	public String getGrantWriteAcp() {
		return grantWriteAcp;
	}

	public String getDecodedContentLength() {
		return decodedContentLength;
	}

	public Map<String, String> getUserMetadata() {
		return userMetadata;
	}

	public void setMaxContentLengthRange(String value) {
		maxContentLengthRange = value;
	}

	public void setMinContentLengthRange(String value) {
		minContentLengthRange = value;
	}

	public String getMaxContentLengthRange() {
		return maxContentLengthRange;
	}

	public String getMinContentLengthRange() {
		return minContentLengthRange;
	}

	public String getExpiration() {
		return expiration;
	}

	public void setExpiration(String expire) {
		expiration = expire;
	}

	public void checkPolicy(String header, String value) throws GWException {
		if (header.startsWith(GWConstants.DOLLAR_SIGN)) {
			header = header.substring(1);
		}

		// case들을 enum으로 만들어서 loop로 해결할 예정입니다.
		if (header.equalsIgnoreCase(GWConstants.PARAMETER_ACL) && !acl.equals(value)) {
			logger.info(GWErrorCode.BAD_REQUEST.getMessage());
			throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		} else if (header.equalsIgnoreCase(GWConstants.AWS_ACCESS_KEY_ID) && !accesskey.equals(value)) {
			logger.info(GWErrorCode.BAD_REQUEST.getMessage());
			throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		} else if (header.equalsIgnoreCase(GWConstants.X_AMZ_CREDENTIAL) && !accesskey.equals(value)) {
			logger.info(GWErrorCode.BAD_REQUEST.getMessage());
			throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		} else if (header.equalsIgnoreCase(GWConstants.CONTENT_TYPE) && !contentType.equals(value)) {
			logger.info(GWErrorCode.BAD_REQUEST.getMessage());
			throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		} else if (header.equalsIgnoreCase(GWConstants.KEY) && !key.equals(value)) {
			logger.info(GWErrorCode.BAD_REQUEST.getMessage());
			throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		} else if (header.equalsIgnoreCase(GWConstants.SIGNATURE) && !signature.equals(value)) {
			logger.info(GWErrorCode.BAD_REQUEST.getMessage());
			throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		} else if (header.equalsIgnoreCase(GWConstants.X_AMZ_SIGNATURE) && !signature.equals(value)) {
			logger.info(GWErrorCode.BAD_REQUEST.getMessage());
			throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		} else if (header.equalsIgnoreCase(GWConstants.X_AMZ_ALGORITHM) && !algorithm.equals(value)) {
			logger.info(GWErrorCode.BAD_REQUEST.getMessage());
			throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		} else if (header.equalsIgnoreCase(GWConstants.CACHE_CONTROL) && !cacheControl.equals(value)) {
			logger.info(GWErrorCode.BAD_REQUEST.getMessage());
			throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		} else if (header.equalsIgnoreCase(GWConstants.CONTENT_DISPOSITION) && !contentDisposition.equals(value)) {
			logger.info(GWErrorCode.BAD_REQUEST.getMessage());
			throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		} else if (header.equalsIgnoreCase(GWConstants.CONTENT_ENCODING) && !contentEncoding.equals(value)) {
			logger.info(GWErrorCode.BAD_REQUEST.getMessage());
			throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		} else if (header.equalsIgnoreCase(GWConstants.PARAMETER_TAGGING) && !tagging.equals(value)) {
			logger.info(GWErrorCode.BAD_REQUEST.getMessage());
			throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		} else if (header.equalsIgnoreCase(GWConstants.SUCCESS_ACTION_STATUS) && !successActionStatus.equals(value)) {
			logger.info(GWErrorCode.BAD_REQUEST.getMessage());
			throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		} else if (header.equalsIgnoreCase(GWConstants.SUCCESS_ACTION_REDIRECT) && !successActionRedirect.equals(value)) {
			logger.info(GWErrorCode.BAD_REQUEST.getMessage());
			throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		} else if (header.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION) && !serverSideEncryption.equals(value)) {
			logger.info(GWErrorCode.BAD_REQUEST.getMessage());
			throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		} else if (header.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_AWS_KMS_KEY_ID)
				&& !serverSideEncryptionAwsKmsKeyId.equals(value)) {
					logger.info(GWErrorCode.BAD_REQUEST.getMessage());
					throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		} else if (header.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CONTEXT) && !serverSideEncryptionContext.equals(value)) {
			logger.info(GWErrorCode.BAD_REQUEST.getMessage());
			throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		} else if (header.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_BUCKET_KEY_ENABLED)
				&& !serverSideEncryptionBucketKeyEnabled.equals(value)) {
					logger.info(GWErrorCode.BAD_REQUEST.getMessage());
					throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		} else if (header.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM)
				&& !serverSideEncryptionCustomerAlgorithm.equals(value)) {
					logger.info(GWErrorCode.BAD_REQUEST.getMessage());
					throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		} else if (header.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY)
				&& !serverSideEncryptionCustomerKey.equals(value)) {
					logger.info(GWErrorCode.BAD_REQUEST.getMessage());
					throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		} else if (header.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5)
				&& !serverSideEncryptionCustomerKeyMD5.equals(value)) {
					logger.info(GWErrorCode.BAD_REQUEST.getMessage());
					throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		} else if (header.toLowerCase().startsWith(GWConstants.USER_METADATA_PREFIX)) {
			if (Strings.isNullOrEmpty(userMetadata.get(header))) {
				logger.info(GWErrorCode.ACCESS_DENIED.getMessage());
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}

			if (!userMetadata.get(header).equals(value)) {
				logger.info(GWErrorCode.ACCESS_DENIED.getMessage());
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
		}
	}

	public void checkPolityStarts(String header, String value) throws GWException {
		if (header.startsWith(GWConstants.DOLLAR_SIGN)) {
			header = header.substring(1);
		}

		String[] comma = value.split(GWConstants.COMMA);

		for (String val : comma) {

			// case들을 enum으로 만들어서 loop로 해결할 예정입니다.
			if (header.equalsIgnoreCase(GWConstants.PARAMETER_ACL) && ( acl.startsWith(val) || Strings.isNullOrEmpty(value))) {
				return;
			} else if (header.equalsIgnoreCase(GWConstants.AWS_ACCESS_KEY_ID) && (accesskey.startsWith(val) || Strings.isNullOrEmpty(value))) {
				return;
			} else if (header.equalsIgnoreCase(GWConstants.X_AMZ_CREDENTIAL) && (accesskey.startsWith(val) || Strings.isNullOrEmpty(value))) {
				return;
			} else if (header.equalsIgnoreCase(GWConstants.CONTENT_TYPE) && (contentType.startsWith(val) || Strings.isNullOrEmpty(value))) {
				return;
			} else if (header.equalsIgnoreCase(GWConstants.KEY) && (key.startsWith(val) || Strings.isNullOrEmpty(value))) {
				return;
			} else if (header.equalsIgnoreCase(GWConstants.SIGNATURE) && (signature.startsWith(val) || Strings.isNullOrEmpty(value))) {
				return;
			} else if (header.equalsIgnoreCase(GWConstants.X_AMZ_SIGNATURE) && (signature.startsWith(val) || Strings.isNullOrEmpty(value))) {
				return;
			} else if (header.equalsIgnoreCase(GWConstants.X_AMZ_ALGORITHM) && (algorithm.startsWith(val) || Strings.isNullOrEmpty(value))) {
				return;
			} else if (header.equalsIgnoreCase(GWConstants.CACHE_CONTROL) && (cacheControl.startsWith(val) || Strings.isNullOrEmpty(value))) {
				return;
			} else if (header.equalsIgnoreCase(GWConstants.CONTENT_DISPOSITION) && (contentDisposition.startsWith(val) || Strings.isNullOrEmpty(value))) {
				return;
			} else if (header.equalsIgnoreCase(GWConstants.CONTENT_ENCODING) && (contentEncoding.startsWith(val) || Strings.isNullOrEmpty(value))) {
				return;
			} else if (header.equalsIgnoreCase(GWConstants.TAGGING) && (tagging.startsWith(val) || Strings.isNullOrEmpty(value))) {
				return;
			} else if (header.equalsIgnoreCase(GWConstants.SUCCESS_ACTION_STATUS) && (successActionStatus.startsWith(val) || Strings.isNullOrEmpty(value))) {
				return;
			} else if (header.equalsIgnoreCase(GWConstants.SUCCESS_ACTION_REDIRECT) && (successActionRedirect.startsWith(val) || Strings.isNullOrEmpty(value))) {
				return;
			} else if (header.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION) && (serverSideEncryption.startsWith(val) || Strings.isNullOrEmpty(value))) {
				return;
			} else if (header.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_AWS_KMS_KEY_ID)
					&& (serverSideEncryptionAwsKmsKeyId.startsWith(val) || Strings.isNullOrEmpty(value))) {
						return;
			} else if (header.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CONTEXT)
					&& (serverSideEncryptionContext.startsWith(val) || Strings.isNullOrEmpty(value))) {
						return;
			} else if (header.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_BUCKET_KEY_ENABLED)
					&& (serverSideEncryptionBucketKeyEnabled.startsWith(val) || Strings.isNullOrEmpty(value))) {
						return;
			} else if (header.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM)
					&& (serverSideEncryptionCustomerAlgorithm.startsWith(val) || Strings.isNullOrEmpty(value))) {
						return;
			} else if (header.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY)
					&& (serverSideEncryptionCustomerKey.startsWith(val) || Strings.isNullOrEmpty(value))) {
						return;
			} else if (header.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5)
					&& (serverSideEncryptionCustomerKeyMD5.startsWith(val) || Strings.isNullOrEmpty(value))) {
						return;
			} else if (header.toLowerCase().startsWith(GWConstants.USER_METADATA_PREFIX)) {
				if (Strings.isNullOrEmpty(userMetadata.get(header))) {
					logger.info(GWErrorCode.ACCESS_DENIED.getMessage());
					throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
				}

				if ( (userMetadata.get(header).startsWith(val) || Strings.isNullOrEmpty(value)))
					return;
			}
		}

		logger.info(GWErrorCode.ACCESS_DENIED.getMessage());
		throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
	}
}
