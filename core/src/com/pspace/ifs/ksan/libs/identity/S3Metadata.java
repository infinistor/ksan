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
package com.pspace.ifs.ksan.libs.identity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.ifs.ksan.libs.KsanUtils;

public class S3Metadata {
	private static Logger logger = LoggerFactory.getLogger(S3Metadata.class);

	private String key;
	// private String uri;
	private Map<String, String> userMetadata;
	private String eTag;
	private Date creationDate;
	private Date lastModified;
	// private Date lastAccess;
	private String tier;
	private String cacheControl;
	private String customerAlgorithm;
	private String customerKey;
	private String customerKeyMD5;
	private String serverSideEncryption;
	private String deleteMarker;
	private String isLatest;
	private long contentLength;
	private String contentDisposition;
	private String contentEncoding;
	private String contentType;
	private String ownerId;
	private String ownerName;
	private String contentLanguage;
	private String taggingCount;
	private long decodedContentLength;
	private String contentMD5;
	private String encryption;
	private String versionId;
	private String uploadId;
	private int partNumber;

	// lifecycle
	private Date expire;

	// retention
	private String lockMode;
	private String lockExpires;
	private String legalHold;

	// aws kms
	private String kmsKeyId;
	private String kmsKeyIndex;
	private String kmsKeyPath;
	private String bucketKeyEnabled;

	public S3Metadata() {
		userMetadata = new HashMap<String, String>();
	}

	public void addMetadata(String Key, String Value) {
		userMetadata.put(Key, Value);
	}

	public String getMetadata(String key) {
		return userMetadata.get(key);
	}
	
	public String getName() {
		return key;
	}

	public void setName(String key) {
		this.key = key;
	}

	public Map<String, String> getUserMetadata() {
		return userMetadata;
	}

	public void setUserMetadata(Map<String, String> userMetadata) {
		this.userMetadata = userMetadata;
	}

	public String getETag() {
		return eTag;
	}

	public void setETag(String eTag) {
		this.eTag = eTag;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	public String getTier() {
		return tier;
	}

	public void setTier(String tier) {
		this.tier = tier;
	}

	public String getCacheControl() {
		return cacheControl;
	}

	public void setCacheControl(String cacheControl) {
		this.cacheControl = cacheControl;
	}

	public String getCustomerAlgorithm() {
		return customerAlgorithm;
	}

	public void setCustomerAlgorithm(String customerAlgorithm) {
		this.customerAlgorithm = customerAlgorithm;
	}

	public String getCustomerKey() {
		return customerKey;
	}

	public void setCustomerKey(String customerKey) {
		this.customerKey = customerKey;
	}

	public String getCustomerKeyMD5() {
		return customerKeyMD5;
	}

	public void setCustomerKeyMD5(String customerKeyMD5) {
		this.customerKeyMD5 = customerKeyMD5;
	}

	public String getServerSideEncryption() {
		return serverSideEncryption;
	}

	public void setServerSideEncryption(String serversideEncryption) {
		this.serverSideEncryption = serversideEncryption;
	}

	public String getDeleteMarker() {
		return deleteMarker;
	}

	public void setDeleteMarker(String deleteMarker) {
		this.deleteMarker = deleteMarker;
	}

	public String getIsLatest() {
		return isLatest;
	}

	public void setIsLatest(String isLatest) {
		this.isLatest = isLatest;
	}

	public Long getContentLength() {
		return contentLength;
	}

	public void setContentLength(Long contentLength) {
		this.contentLength = contentLength;
	}

	public String getContentDisposition() {
		return contentDisposition;
	}

	public void setContentDisposition(String contentDisposition) {
		this.contentDisposition = contentDisposition;
	}

	public String getContentEncoding() {
		return contentEncoding;
	}

	public void setContentEncoding(String contentEncoding) {
		this.contentEncoding = contentEncoding;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getOwnerName() {
		return ownerName;
	}

	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	public String getContentLanguage() {
		return contentLanguage;
	}

	public void setContentLanguage(String contentLanguage) {
		this.contentLanguage = contentLanguage;
	}

	public Date getExpires() {
		return expire;
	}

	public void setExpires(Date expires) {
		this.expire = expires;
	}

	public String getTaggingCount() {
		return taggingCount;
	}

	public void setTaggingCount(String taggingCount) {
		this.taggingCount = taggingCount;
	}

	public long getDecodedContentLength() {
		return decodedContentLength;
	}

	public void setDecodedContentLength(long decodedContentLength) {
		this.decodedContentLength = decodedContentLength;
	}

	public String getContentMD5() {
		return contentMD5;
	}

	public void setContentMD5(String contentMD5) {
		this.contentMD5 = contentMD5;
	}

	public String getEncryption() {
		return encryption;
	}

	public void setEncryption(String encryption) {
		this.encryption = encryption;
	}

	public String getVersionId() {
		return versionId;
	}

	public void setVersionId(String versionId) {
		this.versionId = versionId;
	}

	public String getUploadId() {
		return uploadId;
	}

	public void setUploadId(String uploadId) {
		this.uploadId = uploadId;
	}

	public int getPartNumber() {
		return partNumber;
	}

	public void setPartNumber(int partNumber) {
		this.partNumber = partNumber;
	}

	public void setLockMode(String lockMode) {
		this.lockMode = lockMode;
	}

	public void setLegalHold(String legalHold) {
		this.legalHold = legalHold;
	}

	public void setLockExpires(String lockExpires) {
		this.lockExpires = lockExpires;
	}

	public String getLockMode() {
		return this.lockMode;
	}

	public String getLegalHold() {
		return this.legalHold;
	}

	public String getLockExpires() {
		return this.lockExpires;
	}

	public String getKmsKeyId() {
		return kmsKeyId;
	}

	public void setKmsKeyId(String kmsKeyId) {
		this.kmsKeyId = kmsKeyId;
	}

	public String getKmsKeyIndex() {
		return kmsKeyIndex;
	}

	public void setKmsKeyIndex(String kmsKeyIndex) {
		this.kmsKeyIndex = kmsKeyIndex;
	}

	public String getKmsKeyPath() {
		return kmsKeyPath;
	}

	public void setKmsKeyPath(String ksmKeyPath) {
		this.kmsKeyPath = ksmKeyPath;
	}

	public String getBucketKeyEnabled() {
		return bucketKeyEnabled;
	}

	public void setBucketKeyEnabled(String bucketKeyEnabled) {
		this.bucketKeyEnabled = bucketKeyEnabled;
	}

	private static final String USER_METADATA_PREFIX = "x-amz-meta-";
	private static final String LEFT_BRACE = "{";
	private static final String RIGHT_BRACE = "}";
	private static final String LEFT_BRACKET = "[";
	private static final String RIGHT_BRACKET = "]";
	private static final String QUOTAION = "\"";
	private static final String COLON = ":";
	private static final String COMMA = ",";

	private static final String KEY = "key";
	private static final String JSON_KEY = "\"key\":";
	private static final String USER_METADATA = "uM";
	private static final String JSON_USER_METADATA = "\"uM\":";
	private static final String ETAG = "eT";
	private static final String JSON_ETAG = "\"eT\":";
	private static final String CREATE_DATE = "cR";
	private static final String JSON_CREATE_DATE = "\"cR\":";
	private static final String LAST_MODIFIED = "lM";
	private static final String JSON_LAST_MODIFIED = "\"lM\":";
	private static final String TIER = "tier";
	private static final String JSON_TIER = "\"tier\":";
	private static final String CACHE_CONTROL = "cC";
	private static final String JSON_CACHE_CONTROL = "\"cC\":";
	private static final String CUSTOMER_ALGORITHM = "cA";
	private static final String JSON_CUSTOMER_ALGORITHM = "\"cA\":";
	private static final String CUSTOMER_KEY = "cK";
	private static final String JSON_CUSTOMER_KEY = "\"cK\":";
	private static final String CUSTOMER_KEY_MD5 = "cKMD5";
	private static final String JSON_CUSTOMER_KEY_MD5 = "\"cKMD5\":";
	private static final String SERVER_SIDE_ENCRYPTION = "ssE";
	private static final String JSON_SERVER_SIDE_ENCRYPTION = "\"ssE\":";
	private static final String DELETE_MARKER = "dM";
	private static final String JSON_DELETE_MARKER = "\"dM\":";
	private static final String IS_LATEST = "iL";
	private static final String JSON_IS_LATEST = "\"iL\":";
	private static final String CONTENT_LENGTH = "cL";
	private static final String JSON_CONTENT_LENGTH = "\"cL\":";
	private static final String CONTENT_DISPOSITION = "cD";
	private static final String JSON_CONTENT_DISPOSITION = "\"cD\":";
	private static final String CONTENT_ENCODING = "cE";
	private static final String JSON_CONTENT_ENCODING = "\"cE\":";
	private static final String CONTENT_TYPE = "cT";
	private static final String JSON_CONTENT_TYPE = "\"cT\":";
	private static final String OWNER_ID = "oI";
	private static final String JSON_OWNER_ID = "\"oI\":";
	private static final String OWNER_NAME = "oN";
	private static final String JSON_OWNER_NAME = "\"oN\":";
	private static final String CONTENT_LANGUAGE = "cLang";
	private static final String JSON_CONTENT_LANGUAGE = "\"cLang\":";
	private static final String TAGGING_COUNT = "tC";
	private static final String JSON_TAGGING_COUNT = "\"tC\":";
	private static final String DECODED_CONTENT_LENGTH = "dCL";
	private static final String JSON_DECODED_CONTENT_LENGTH = "\"dCL\":";
	private static final String CONTENT_MD5 = "cMD5";
	private static final String JSON_CONTENT_MD5 = "\"cMD5\":";
	private static final String ENCRYPTION = "en";
	private static final String JSON_ENCRYPTION = "\"en\":";
	private static final String VERSIONID = "vId";
	private static final String JSON_VERSIONID = "\"vId\":";
	private static final String UPLOADID = "uId";
	private static final String JSON_UPLOADID = "\"uId\":";
	private static final String PART_NUMBER = "pN";
	private static final String JSON_PART_NUMBER = "\"pN\":";
	private static final String EXPIRE = "ex";
	private static final String JSON_EXPIRE = "\"ex\":";
	private static final String LOCK_MODE = "lckM";
	private static final String JSON_LOCK_MODE = "\"lckM\":";
	private static final String LOCK_EXPIRES = "lckE";
	private static final String JSON_LOCK_EXPIRES = "\"lckE\":";
	private static final String LEGAL_HOLD = "lH";
	private static final String JSON_LEGAL_HOLD = "\"lH\":";
	private static final String KMS_MASTER_KEY_ID = "kmi";
	private static final String JSON_KMS_MASTER_KEY_ID = "\"kmi\":";
	private static final String KMS_KEY_INDEX = "kki";
	private static final String JSON_KMS_KEY_INDEX = "\"kki\":";
	private static final String KMS_KEY_PATH = "kkp";
	private static final String JSON_KMS_KEY_PATH = "\"kkp\":";
	private static final String BUCKET_KEY_ENABLED = "bke";
	private static final String JSON_BUCKET_KEY_ENABLED = "\"bke\":";
	private static final String EMPTY_STRING = "";

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(LEFT_BRACE);
		if (key != null) {
			sb.append(JSON_KEY);
			sb.append(QUOTAION);
			sb.append(key);
			sb.append(QUOTAION);
		}
		if (userMetadata != null && userMetadata.size() > 0) {
			sb.append(COMMA);
			sb.append(JSON_USER_METADATA);
			sb.append(LEFT_BRACKET);
			int index = 0;
			for (Map.Entry<String, String> element : userMetadata.entrySet()) {
				if (index > 0) {
					sb.append(COMMA);
				}
				sb.append(LEFT_BRACE);
				sb.append(QUOTAION);
				sb.append(element.getKey());
				sb.append(QUOTAION);
				sb.append(COLON);
				sb.append(QUOTAION);
				sb.append(element.getValue());
				sb.append(QUOTAION);
				sb.append(RIGHT_BRACE);
				index++;
			}
			sb.append(RIGHT_BRACKET);
		}
		if (eTag != null) {
			sb.append(COMMA);
			sb.append(JSON_ETAG);
			sb.append(QUOTAION);
			sb.append(getETag());
			sb.append(QUOTAION);
		}
		if (creationDate != null) {
			sb.append(COMMA);
			sb.append(JSON_CREATE_DATE);
			sb.append(QUOTAION);
			sb.append(creationDate.toString());
			sb.append(QUOTAION);
		}
		if (lastModified != null) {
			sb.append(COMMA);
			sb.append(JSON_LAST_MODIFIED);
			sb.append(QUOTAION);
			sb.append(lastModified.toString());
			sb.append(QUOTAION);
		}
		if (tier != null) {
			sb.append(COMMA);
			sb.append(JSON_TIER);
			sb.append(QUOTAION);
			sb.append(tier);
			sb.append(QUOTAION);
		}
		if (cacheControl != null) {
			sb.append(COMMA);
			sb.append(JSON_CACHE_CONTROL);
			sb.append(QUOTAION);
			sb.append(cacheControl);
			sb.append(QUOTAION);
		}
		if (customerAlgorithm != null) {
			sb.append(COMMA);
			sb.append(JSON_CUSTOMER_ALGORITHM);
			sb.append(QUOTAION);
			sb.append(customerAlgorithm);
			sb.append(QUOTAION);
		}
		if (customerKey != null) {
			sb.append(COMMA);
			sb.append(JSON_CUSTOMER_KEY);
			sb.append(QUOTAION);
			sb.append(customerKey);
			sb.append(QUOTAION);
		}
		if (customerKeyMD5 != null) {
			sb.append(COMMA);
			sb.append(JSON_CUSTOMER_KEY_MD5);
			sb.append(QUOTAION);
			sb.append(customerKeyMD5);
			sb.append(QUOTAION);
		}
		if (serverSideEncryption != null) {
			sb.append(COMMA);
			sb.append(JSON_SERVER_SIDE_ENCRYPTION);
			sb.append(QUOTAION);
			sb.append(serverSideEncryption);
			sb.append(QUOTAION);
		}
		if (deleteMarker != null) {
			sb.append(COMMA);
			sb.append(JSON_DELETE_MARKER);
			sb.append(QUOTAION);
			sb.append(deleteMarker);
			sb.append(QUOTAION);
		}
		if (isLatest != null) {
			sb.append(COMMA);
			sb.append(JSON_IS_LATEST);
			sb.append(QUOTAION);
			sb.append(isLatest);
			sb.append(QUOTAION);
		}
		if (contentLength >= 0) {
			sb.append(COMMA);
			sb.append(JSON_CONTENT_LENGTH);
			sb.append(QUOTAION);
			sb.append(contentLength);
			sb.append(QUOTAION);
		}
		if (contentDisposition != null) {
			sb.append(COMMA);
			sb.append(JSON_CONTENT_DISPOSITION);
			sb.append(QUOTAION);
			sb.append(contentDisposition);
			sb.append(QUOTAION);
		}
		if (contentEncoding != null) {
			sb.append(COMMA);
			sb.append(JSON_CONTENT_ENCODING);
			sb.append(QUOTAION);
			sb.append(contentEncoding);
			sb.append(QUOTAION);
		}
		if (contentType != null) {
			sb.append(COMMA);
			sb.append(JSON_CONTENT_TYPE);
			sb.append(QUOTAION);
			sb.append(contentType);
			sb.append(QUOTAION);
		}
		if (ownerId != null) {
			sb.append(COMMA);
			sb.append(JSON_OWNER_ID);
			sb.append(QUOTAION);
			sb.append(ownerId);
			sb.append(QUOTAION);
		}
		if (ownerName != null) {
			sb.append(COMMA);
			sb.append(JSON_OWNER_NAME);
			sb.append(QUOTAION);
			sb.append(ownerName);
			sb.append(QUOTAION);
		}
		if (contentLanguage != null) {
			sb.append(COMMA);
			sb.append(JSON_CONTENT_LANGUAGE);
			sb.append(QUOTAION);
			sb.append(contentLanguage);
			sb.append(QUOTAION);
		}
		if (taggingCount != null) {
			sb.append(COMMA);
			sb.append(JSON_TAGGING_COUNT);
			sb.append(QUOTAION);
			sb.append(taggingCount);
			sb.append(QUOTAION);
		}
		if (decodedContentLength > 0) {
			sb.append(COMMA);
			sb.append(JSON_DECODED_CONTENT_LENGTH);
			sb.append(QUOTAION);
			sb.append(decodedContentLength);
			sb.append(QUOTAION);
		}
		if (contentMD5 != null) {
			sb.append(COMMA);
			sb.append(JSON_CONTENT_MD5);
			sb.append(QUOTAION);
			sb.append(contentMD5);
			sb.append(QUOTAION);
		}
		if (encryption != null) {
			sb.append(COMMA);
			sb.append(JSON_ENCRYPTION);
			sb.append(QUOTAION);
			sb.append(encryption);
			sb.append(QUOTAION);
		}
		if (versionId != null) {
			sb.append(COMMA);
			sb.append(JSON_VERSIONID);
			sb.append(QUOTAION);
			sb.append(versionId);
			sb.append(QUOTAION);
		}
		if (uploadId != null) {
			sb.append(COMMA);
			sb.append(JSON_UPLOADID);
			sb.append(QUOTAION);
			sb.append(uploadId);
			sb.append(QUOTAION);
		}
		if (partNumber > 0) {
			sb.append(COMMA);
			sb.append(JSON_PART_NUMBER);
			sb.append(QUOTAION);
			sb.append(partNumber);
			sb.append(QUOTAION);
		}
		if (expire != null) {
			sb.append(COMMA);
			sb.append(JSON_EXPIRE);
			sb.append(QUOTAION);
			sb.append(expire.toString());
			sb.append(QUOTAION);
		}
		if (lockMode != null) {
			sb.append(COMMA);
			sb.append(JSON_LOCK_MODE);
			sb.append(QUOTAION);
			sb.append(lockMode);
			sb.append(QUOTAION);
		}
		if (lockExpires != null) {
			sb.append(COMMA);
			sb.append(JSON_LOCK_EXPIRES);
			sb.append(QUOTAION);
			sb.append(lockExpires);
			sb.append(QUOTAION);
		}
		if (legalHold != null) {
			sb.append(COMMA);
			sb.append(JSON_LEGAL_HOLD);
			sb.append(QUOTAION);
			sb.append(legalHold);
			sb.append(QUOTAION);
		}
		if (kmsKeyId != null) {
			sb.append(COMMA);
			sb.append(JSON_KMS_MASTER_KEY_ID);
			sb.append(QUOTAION);
			sb.append(kmsKeyId);
			sb.append(QUOTAION);
		}
		if (kmsKeyIndex != null) {
			sb.append(COMMA);
			sb.append(JSON_KMS_KEY_INDEX);
			sb.append(QUOTAION);
			sb.append(kmsKeyIndex);
			sb.append(QUOTAION);
		}
		if (kmsKeyPath != null) {
			sb.append(COMMA);
			sb.append(JSON_KMS_KEY_PATH);
			sb.append(QUOTAION);
			sb.append(kmsKeyPath);
			sb.append(QUOTAION);
		}
		if (bucketKeyEnabled != null) {
			sb.append(COMMA);
			sb.append(JSON_BUCKET_KEY_ENABLED);
			sb.append(QUOTAION);
			sb.append(bucketKeyEnabled);
			sb.append(QUOTAION);
		}

		return sb.append(RIGHT_BRACE).toString();
	}

	public static S3Metadata getS3Metadata(String json) {
		if (json == null || json.length() == 0) {
			return null;
		}
		S3Metadata s3Metadata = new S3Metadata();
		logger.debug("meta : {}", json);
		
		// key
		int startIndex = json.indexOf(JSON_KEY);
		if (startIndex < 0) {
			logger.error("can't find key ...");
			return null;
		}
		startIndex += JSON_KEY.length();
		int endIndex = json.indexOf(COMMA, startIndex);
		String data = json.substring(startIndex + 1, endIndex - 1);
		s3Metadata.setName(data);

		// user metadata
		startIndex = json.indexOf(JSON_USER_METADATA, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_USER_METADATA.length();
			endIndex = json.indexOf(RIGHT_BRACKET);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("usermeta : {}", data);
			int sIndex = 0;
			int eIndex = -1;
			String key = "";
			String value = "";
			while (true) {
				// key
				sIndex = data.indexOf(QUOTAION, eIndex + 1);
				if (sIndex < 0) {
					break;
				}
				eIndex = data.indexOf(QUOTAION, sIndex + 1);
				key = data.substring(sIndex + 1, eIndex);
				logger.debug("key : {}", key);
				// value
				sIndex = data.indexOf(COLON, eIndex);
				eIndex = data.indexOf(QUOTAION, sIndex + 2);
				value = data.substring(sIndex + 2, eIndex);
				logger.debug("value : {}", value); 
				s3Metadata.addMetadata(key, value);
			}
		}

		// etag
		startIndex = json.indexOf(JSON_ETAG, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_ETAG.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("etag : {}", data);
			s3Metadata.setETag(data);
		}

		// creation data
		startIndex = json.indexOf(JSON_CREATE_DATE, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_CREATE_DATE.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("creation date : {}", data);
			s3Metadata.setCreationDate(KsanUtils.getDate(data));
		}

		// last modified
		startIndex = json.indexOf(JSON_LAST_MODIFIED, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_LAST_MODIFIED.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("last modified : {}", data);
			s3Metadata.setLastModified(KsanUtils.getDate(data));
		}

		// tier
		startIndex = json.indexOf(JSON_TIER, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_TIER.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("tier : {}", data);
			s3Metadata.setTier(data);
		}

		// cache control
		startIndex = json.indexOf(JSON_CACHE_CONTROL, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_CACHE_CONTROL.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("cache control : {}", data);
			s3Metadata.setCacheControl(data);
		}

		// customer algorithm
		startIndex = json.indexOf(JSON_CUSTOMER_ALGORITHM, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_CUSTOMER_ALGORITHM.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("custom algorithm : {}", data);
			s3Metadata.setCustomerAlgorithm(data);
		}

		// customer Key
		startIndex = json.indexOf(JSON_CUSTOMER_KEY, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_CUSTOMER_KEY.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("customer Key : {}", data);
			s3Metadata.setCustomerKey(data);
		}

		// customer Key MD5
		startIndex = json.indexOf(JSON_CUSTOMER_KEY_MD5, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_CUSTOMER_KEY_MD5.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("customer Key MD5 : {}", data);
			s3Metadata.setCustomerKeyMD5(data);
		}

		// server side encryption
		startIndex = json.indexOf(JSON_SERVER_SIDE_ENCRYPTION, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_SERVER_SIDE_ENCRYPTION.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("Server side encryption : {}", data);
			s3Metadata.setServerSideEncryption(data);
		}

		// delete marker
		startIndex = json.indexOf(JSON_DELETE_MARKER, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_DELETE_MARKER.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("Delete marker: {}", data);
			s3Metadata.setDeleteMarker(data);
		}

		// isLatest
		startIndex = json.indexOf(JSON_IS_LATEST, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_IS_LATEST.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("Is latest: {}", data);
			s3Metadata.setIsLatest(data);
		}

		// content length
		startIndex = json.indexOf(JSON_CONTENT_LENGTH, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_CONTENT_LENGTH.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("Content length: {}", data);
			s3Metadata.setContentLength(Long.parseLong(data));
		}

		// content disposition
		startIndex = json.indexOf(JSON_CONTENT_DISPOSITION, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_CONTENT_DISPOSITION.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("Content disposition: {}", data);
			s3Metadata.setContentDisposition(data);
		}

		// content encoding
		startIndex = json.indexOf(JSON_CONTENT_ENCODING, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_CONTENT_ENCODING.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("Content encoding : {}", data);
			s3Metadata.setContentEncoding(data);
		}

		// content type
		startIndex = json.indexOf(JSON_CONTENT_TYPE, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_CONTENT_TYPE.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("content type : {}", data);
			s3Metadata.setContentType(data);
		}

		// owner ID
		startIndex = json.indexOf(JSON_OWNER_ID, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_OWNER_ID.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("owner id : {}", data);
			s3Metadata.setOwnerId(data);
		}

		// owner name
		startIndex = json.indexOf(JSON_OWNER_NAME, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_OWNER_NAME.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("owner name : {}", data);
			s3Metadata.setOwnerName(data);
		}

		// content language
		startIndex = json.indexOf(JSON_CONTENT_LANGUAGE, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_CONTENT_LANGUAGE.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("content language : {}", data);
			s3Metadata.setContentLanguage(data);
		}

		// tagging count
		startIndex = json.indexOf(JSON_TAGGING_COUNT, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_TAGGING_COUNT.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("tagging count : {}", data);
			s3Metadata.setTaggingCount(data);
		}

		// decoded content length
		startIndex = json.indexOf(JSON_DECODED_CONTENT_LENGTH, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_DECODED_CONTENT_LENGTH.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("decoded content length : {}", data);
			s3Metadata.setDecodedContentLength(Long.parseLong(data));
		}

		// content MD5
		startIndex = json.indexOf(JSON_CONTENT_MD5, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_CONTENT_MD5.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("content MD5 : {}", data);
			s3Metadata.setContentMD5(data);
		}

		// encryption
		startIndex = json.indexOf(JSON_ENCRYPTION, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_ENCRYPTION.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("encryption : {}", data);
			s3Metadata.setEncryption(data);
		}

		// versionId
		startIndex = json.indexOf(JSON_VERSIONID, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_VERSIONID.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("versionId : {}", data);
			s3Metadata.setVersionId(data);
		}

		// uploadId
		startIndex = json.indexOf(JSON_UPLOADID, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_UPLOADID.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("uploadId : {}", data);
			s3Metadata.setUploadId(data);
		}

		// part number
		startIndex = json.indexOf(JSON_PART_NUMBER, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_PART_NUMBER.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("part number : {}", data);
			s3Metadata.setPartNumber(Integer.parseInt(data));
		}

		// expire
		startIndex = json.indexOf(JSON_EXPIRE, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_EXPIRE.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("expire : {}", data);
			s3Metadata.setExpires(KsanUtils.getDate(data));
		}

		// lock mode
		startIndex = json.indexOf(JSON_LOCK_MODE, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_LOCK_MODE.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("lock mode : {}", data);
			s3Metadata.setLockMode(data);
		}

		// lock expires
		startIndex = json.indexOf(JSON_LOCK_EXPIRES, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_LOCK_EXPIRES.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("lock expires : {}", data);
			s3Metadata.setLockExpires(data);
		}

		// legal hold
		startIndex = json.indexOf(JSON_LEGAL_HOLD, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_LEGAL_HOLD.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("legal hold : {}", data);
			s3Metadata.setLegalHold(data);
		}

		// kms master key id
		startIndex = json.indexOf(JSON_KMS_MASTER_KEY_ID, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_KMS_MASTER_KEY_ID.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("kms master key id : {}", data);
			s3Metadata.setKmsKeyId(data);
		}

		// kms key index
		startIndex = json.indexOf(JSON_KMS_KEY_INDEX, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_KMS_KEY_INDEX.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("kms master key index : {}", data);
			s3Metadata.setKmsKeyIndex(data);
		}

		// kms key path
		startIndex = json.indexOf(JSON_KMS_KEY_PATH, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_KMS_KEY_PATH.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("kms master key path : {}", data);
			s3Metadata.setKmsKeyPath(data);
		}

		// bucket key enabled
		startIndex = json.indexOf(JSON_BUCKET_KEY_ENABLED, endIndex);
		if (startIndex > 0) {
			startIndex += JSON_BUCKET_KEY_ENABLED.length();
			endIndex = json.indexOf(QUOTAION, startIndex + 1);
			data = json.substring(startIndex + 1, endIndex);
			logger.debug("bucket key enabled : {}", data);
			s3Metadata.setBucketKeyEnabled(data);
		}

		return s3Metadata;
	}
}
