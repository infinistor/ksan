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

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

import org.slf4j.LoggerFactory;

public class DataPutObject extends S3DataRequest {
	private String cacheControl;
	private String contentDisposition;
	private String contentEncoding;
	private String contentLanguage;
	private String contentMD5;
	private String contentType;
	private String expires;
	private String acl;
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
	private boolean hasAclKeyword;
	private String versionId;
	private String replication;
	private String dr;
	private String logging;
	
	public DataPutObject(S3Parameter s3Parameter) throws GWException {
		super(s3Parameter);
		userMetadata = new TreeMap<String, String>();
		logger = LoggerFactory.getLogger(DataPutObject.class);
		hasAclKeyword = false;
	}

	@Override
	public void extract() throws GWException {		
		for (String headerName : Collections.list(s3Parameter.getRequest().getHeaderNames())) {
			if (headerName.equalsIgnoreCase(GWConstants.CACHE_CONTROL)) {
				cacheControl = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.CONTENT_DISPOSITION)) {
				contentDisposition = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.CONTENT_ENCODING)) {
				contentEncoding = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.CONTENT_LANGUAGE)) {
				contentLanguage = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.CONTENT_MD5)) {
				contentMD5 = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.CONTENT_TYPE)) {
				contentType = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.EXPIRES)) {
				expires = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_ACL)) {
				acl = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
				hasAclKeyword = true;
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_GRANT_FULL_CONTROL)) {
				grantFullControl = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
				hasAclKeyword = true;
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_GRANT_READ)) {
				grantRead = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
				hasAclKeyword = true;
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_GRANT_READ_ACP)) {
				grantReadAcp = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
				hasAclKeyword = true;
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_GRANT_WRITE)) {
				grantWrite = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
				hasAclKeyword = true;
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_GRANT_WRITE_ACP)) {
				grantWriteAcp = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
				hasAclKeyword = true;
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_DECODED_CONTENT_LENGTH)) {
				decodedContentLength = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION)) {
				serverSideEncryption = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_STORAGE_CLASS)) {
				storageClass = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_WEBSITE_REDIRECT_LOCATION)) {
				websiteRedirectLocation = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM)) {
				serverSideEncryptionCustomerAlgorithm = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY)) {
				serverSideEncryptionCustomerKey = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5)) {
				serverSideEncryptionCustomerKeyMD5 = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_AWS_KMS_KEY_ID)) {
				serverSideEncryptionAwsKmsKeyId = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CONTEXT)) {
				serverSideEncryptionContext = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_BUCKET_KEY_ENABLED)) {
				serverSideEncryptionBucketKeyEnabled = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_REQUEST_PAYER)) {
				requestPayer = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_TAGGING)) {
				tagging = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_OBJECT_LOCK_MODE)) {
				objectLockMode = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_OBJECT_LOCK_RETAIN_UNTIL_DATE)) {
				objectLockRetainUntilDate = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_OBJECT_LOCK_LEGAL_HOLD)) {
				objectLockLegalHold = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_EXPECTED_BUCKET_OWNER)) {
				expectedBucketOwner = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.startsWith(GWConstants.USER_METADATA_PREFIX) ) {
				userMetadata.put(headerName, s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_IFS_VERSION_ID)) {
				versionId = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_IFS_REPLICATION)) {
				replication = GWConstants.IFS_HEADER_REPLICATION;
			} else if (headerName.equalsIgnoreCase(GWConstants.X_IFS_DR)) {
				dr = GWConstants.IFS_HEADER_DR;
			} else if (headerName.equalsIgnoreCase(GWConstants.X_IFS_LOGGING)) {
				logging = GWConstants.IFS_HEADER_LOGGING;
			}
		}
	}

	public boolean hasAclKeyword() {
		return hasAclKeyword;
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

	@Override
	public String getContentLength() {
		return super.getContentLength();
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

	public String getVersionId() {
		return versionId;
	}

	public String getReplication() {
		return replication;
	}

	public String getDr() {
		return dr;
	}

	public String getLogging() {
		return logging;
	}
}