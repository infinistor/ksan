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

public class DataCopyObject extends S3DataRequest {
	private String cacheControl;
	private String contentDisposition;
	private String contentEncoding;
	private String contentLanguage;
	private String contentType;
	private String copySource;
	private String copySourceIfMatch;
	private String copySourceIfModifiedSince;
	private String copySourceIfNoneMatch;
	private String copySourceIfUnmodifiedSince;
	private String acl;
	private String grantFullControl;
	private String grantRead;
	private String grantReadAcp;
	private String grantWrite;
	private String grantWriteAcp;
	private String expires;
	private String metadataDirective;
	private String taggingDirective;
	private String serverSideEncryption;
	private String serverSideEncryptionCustomerAlgorithm;
	private String serverSideEncryptionCustomerKey;
	private String serverSideEncryptionCustomerKeyMD5;
	private String serverSideEncryptionAwsKmsKeyId;
	private String storageClass;
	private String websiteRedirectLocation;
	private String serverSideEncryptionContext;
	private String serverSideEncryptionBucketKeyEnabled;
	private String copySourceServerSideEncryptionCustomerAlgorithm;
	private String copySourceServerSideEncryptionCustomerKey;
	private String copySourceServerSideEncryptionCustomerKeyMD5;
	private String tagging;
	private String objectLockMode;
	private String objectLockRetainUntilDate;
	private String objectLockLegalHold;
	private String expectedBucketOwner;
	private String sourceExpectedBucketOwner;
	private Map<String, String> userMetadata;
	private boolean hasAclKeyword;
	
	
	public DataCopyObject(S3Parameter s3Parameter) throws GWException {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(DataCopyObject.class);
		userMetadata = new TreeMap<String, String>();
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
			} else if (headerName.equalsIgnoreCase(GWConstants.CONTENT_TYPE)) {
				contentType = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_COPY_SOURCE)) {
				copySource = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_COPY_SOURCE_IF_MATCH)) {
				copySourceIfMatch = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_COPY_SOURCE_IF_MODIFIED_SINCE)) {
				copySourceIfModifiedSince = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_COPY_SOURCE_IF_NONE_MATCH)) {
				copySourceIfNoneMatch = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_COPY_SOURCE_IF_UNMODIFIED_SINCE)) {
				copySourceIfUnmodifiedSince = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_ACL)) {
				acl = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
				if (!Strings.isNullOrEmpty(acl)) {
					hasAclKeyword = true;
				}
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_GRANT_FULL_CONTROL)) {
				grantFullControl = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
				if (!Strings.isNullOrEmpty(grantFullControl)) {
					hasAclKeyword = true;
				}
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_GRANT_READ)) {
				grantRead = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
				if (!Strings.isNullOrEmpty(grantRead)) {
					hasAclKeyword = true;
				}
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_GRANT_READ_ACP)) {
				grantReadAcp = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
				if (!Strings.isNullOrEmpty(grantReadAcp)) {
					hasAclKeyword = true;
				}
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_GRANT_WRITE)) {
				grantWrite = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
				if (!Strings.isNullOrEmpty(grantWrite)) {
					hasAclKeyword = true;
				}
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_GRANT_WRITE_ACP)) {
				grantWriteAcp = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
				if (!Strings.isNullOrEmpty(grantWriteAcp)) {
					hasAclKeyword = true;
				}
			} else if (headerName.equalsIgnoreCase(GWConstants.EXPIRES)) {
				expires = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_METADATA_DIRECTIVE)) {
				metadataDirective = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_TAGGING_DIRECTIVE)) {
				taggingDirective = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION)) {
				serverSideEncryption = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM)) {
				serverSideEncryptionCustomerAlgorithm = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY)) {
				serverSideEncryptionCustomerKey = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5)) {
				serverSideEncryptionCustomerKeyMD5 = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_AWS_KMS_KEY_ID)) {
				serverSideEncryptionAwsKmsKeyId = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_STORAGE_CLASS)) {
				storageClass = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_WEBSITE_REDIRECT_LOCATION)) {
				websiteRedirectLocation = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CONTEXT)) {
				serverSideEncryptionContext = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_BUCKET_KEY_ENABLED)) {
				serverSideEncryptionBucketKeyEnabled = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_COPY_SOURCE_SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM)) {
				copySourceServerSideEncryptionCustomerAlgorithm = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_COPY_SOURCE_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY)) {
				copySourceServerSideEncryptionCustomerKey = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_COPY_SOURCE_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5)) {
				copySourceServerSideEncryptionCustomerKeyMD5 = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
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
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SOURCE_EXPECTED_BUCKET_OWNER)) {
				sourceExpectedBucketOwner = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.startsWith(GWConstants.USER_METADATA_PREFIX) ) {
				logger.info(GWConstants.LOG_DATA_HEADER, headerName, s3Parameter.getRequest().getHeader(headerName));
				userMetadata.put(headerName, s3Parameter.getRequest().getHeader(headerName));
			}
		}

		keyXAmzCopySource = getCopySource();
		keyXAmzAcl = getAcl();
		keyXAmzGrantFullControl = getGrantFullControl();
		keyXAmzGrantRead = getGrantRead();
		keyXAmzGrantReadAcp = getGrantReadAcp();
		keyXAmzGrantWrite = getGrantWrite();
		keyXAmzGrantWriteAcp = getGrantWriteAcp();
		keyXAmzMetadataDirective = getMetadataDirective();
		keyXAmzServerSideEncryption = getServerSideEncryption();
		keyXAmzServerSideEncryptionAwsKmsKeyId = getServerSideEncryptionAwsKmsKeyId();
		keyXAmzStorageClass = getStorageClass();
		keyXAmzWebsiteRedirectLocation = getWebsiteRedirectLocation();
		keyXAmzObjectLockMode = getObjectLockMode();
		keyXAmzObjectLockRetainUntilDate = getObjectLockRetainUntilDate();
		keyXAmzObjectLockLegalHold = getObjectLockLegalHold();
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

	public String getContentType() {
		return contentType;
	}

	public String getCopySource() {
		return copySource;
	}

	public String getExpires() {
		return expires;
	}

	public String getMetadataDirective() {
		return metadataDirective;
	}

	public String getTaggingDirective() {
		return taggingDirective;
	}

	public String getServerSideEncryption() {
		return serverSideEncryption;
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

	public String getStorageClass() {
		return storageClass;
	}

	public String getWebsiteRedirectLocation() {
		return websiteRedirectLocation;
	}

	public String getServerSideEncryptionContext() {
		return serverSideEncryptionContext;
	}

	public String getServerSideEncryptionBucketKeyEnabled() {
		return serverSideEncryptionBucketKeyEnabled;
	}

	public String getCopySourceServerSideEncryptionCustomerAlgorithm() {
		return copySourceServerSideEncryptionCustomerAlgorithm;
	}

	public String getCopySourceServerSideEncryptionCustomerKey() {
		return copySourceServerSideEncryptionCustomerKey;
	}

	public String getCopySourceServerSideEncryptionCustomerKeyMD5() {
		return copySourceServerSideEncryptionCustomerKeyMD5;
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

	public String getSourceExpectedBucketOwner() {
		return sourceExpectedBucketOwner;
	}

	public String getCopySourceIfMatch() {
		return copySourceIfMatch;
	}

	public String getCopySourceIfModifiedSince() {
		return copySourceIfModifiedSince;
	}

	public String getCopySourceIfNoneMatch() {
		return copySourceIfNoneMatch;
	}

	public String getCopySourceIfUnmodifiedSince() {
		return copySourceIfUnmodifiedSince;
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

	public String getGrantReadAcp() {
		return grantReadAcp;
	}

	public String getGrantWrite() {
		return grantWrite;
	}

	public String getGrantWriteAcp() {
		return grantWriteAcp;
	}	

	public Map<String, String> getUserMetadata() {
		return userMetadata;
	}
}
