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

	public String getServersideEncryption() {
		return serverSideEncryption;
	}

	public void setServersideEncryption(String serversideEncryption) {
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

	public static final String USER_METADATA_PREFIX = "x-amz-meta-";
	public static final String LEFT_BRACE = "{";
	public static final String RIGHT_BRACE = "}";
	public static final String LEFT_BRACKET = "[";
	public static final String RIGHT_BRACKET = "]";
	public static final String QUOTAION = "\"";
	public static final String COLON = ":";
	public static final String COMMA = ",";

	public static final String KEY = "key";
	public static final String JSON_KEY = "\"key\":";
	public static final String USER_METADATA = "uM";
	public static final String JSON_USER_METADATA = "\"uM\":";
	public static final String ETAG = "eT";
	public static final String JSON_ETAG = "\"eT\":";
	public static final String CREATE_DATE = "cR";
	public static final String JSON_CREATE_DATE = "\"cR\":";
	public static final String LAST_MODIFIED = "lM";
	public static final String JSON_LAST_MODIFIED = "\"lM\":";
	public static final String TIER = "tier";
	public static final String JSON_TIER = "\"tier\":";
	public static final String CACHE_CONTROL = "cC";
	public static final String JSON_CACHE_CONTROL = "\"cC\":";
	public static final String CUSTOMER_ALGORITHM = "cA";
	public static final String JSON_CUSTOMER_ALGORITHM = "\"cA\":";
	public static final String CUSTOMER_KEY = "cK";
	public static final String JSON_CUSTOMER_KEY = "\"cK\":";
	public static final String CUSTOMER_KEY_MD5 = "cKMD5";
	public static final String JSON_CUSTOMER_KEY_MD5 = "\"cKMD5\":";
	public static final String SERVER_SIDE_ENCRYPTION = "ssE";
	public static final String JSON_SERVER_SIDE_ENCRYPTION = "\"ssE\":";
	public static final String DELETE_MARKER = "dM";
	public static final String JSON_DELETE_MARKER = "\"dM\":";
	public static final String IS_LATEST = "iL";
	public static final String JSON_IS_LATEST = "\"iL\":";
	public static final String CONTENT_LENGTH = "cL";
	public static final String JSON_CONTENT_LENGTH = "\"cL\":";
	public static final String CONTENT_DISPOSITION = "cD";
	public static final String JSON_CONTENT_DISPOSITION = "\"cD\":";
	public static final String CONTENT_ENCODING = "cE";
	public static final String JSON_CONTENT_ENCODING = "\"cE\":";
	public static final String CONTENT_TYPE = "cT";
	public static final String JSON_CONTENT_TYPE = "\"cT\":";
	public static final String OWNER_ID = "oI";
	public static final String JSON_OWNER_ID = "\"oI\":";
	public static final String OWNER_NAME = "oN";
	public static final String JSON_OWNER_NAME = "\"oN\":";
	public static final String CONTENT_LANGUAGE = "cLang";
	public static final String JSON_CONTENT_LANGUAGE = "\"cLang\":";
	public static final String TAGGING_COUNT = "tC";
	public static final String JSON_TAGGING_COUNT = "\"tC\":";
	public static final String DECODED_CONTENT_LENGTH = "dCL";
	public static final String JSON_DECODED_CONTENT_LENGTH = "\"dCL\":";
	public static final String CONTENT_MD5 = "cMD5";
	public static final String JSON_CONTENT_MD5 = "\"cMD5\":";
	public static final String ENCRYPTION = "en";
	public static final String JSON_ENCRYPTION = "\"en\":";
	public static final String VERSIONID = "vId";
	public static final String JSON_VERSIONID = "\"vId\":";
	public static final String UPLOADID = "uId";
	public static final String JSON_UPLOADID = "\"uId\":";
	public static final String PART_NUMBER = "pN";
	public static final String JSON_PART_NUMBER = "\"pN\":";
	public static final String EXPIRE = "ex";
	public static final String JSON_EXPIRE = "\"ex\":";
	public static final String LOCK_MODE = "lckM";
	public static final String JSON_LOCK_MODE = "\"lckM\":";
	public static final String LOCK_EXPIRES = "lckE";
	public static final String JSON_LOCK_EXPIRES = "\"lckE\":";
	public static final String LEGAL_HOLD = "lH";
	public static final String JSON_LEGAL_HOLD = "\"lH\":";

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
			sb.append(JSON_CONTENT_ENCODING);
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

		return sb.append(RIGHT_BRACE).toString();
	}

	public static S3Metadata getS3Metadata(String json) {
		if (json == null) {
			return null;
		}
		S3Metadata s3Metadata = new S3Metadata();

		StringTokenizer stk = new StringTokenizer(json, ",{}[]\"");
		boolean isProcessed = false;
		String token = null;
		while (stk.hasMoreTokens()) {
			if (!isProcessed) {
				token = stk.nextToken();
			} else {
				isProcessed = false;
			}
			logger.debug("token : {}", token);
			switch (token) {
			case KEY:
				stk.nextToken();	// ":"
				s3Metadata.setName(stk.nextToken());
				break;
			case USER_METADATA:
				stk.nextToken();	// ":"
				while (true) {
					token = stk.nextToken();
					if (token.startsWith(USER_METADATA_PREFIX)) {
						stk.nextToken();	// ":"
						s3Metadata.addMetadata(token, stk.nextToken());
					} else {
						break;
					}
				}
				isProcessed = true;
				break;
			case ETAG:
				stk.nextToken();	// ":"
				s3Metadata.setETag(stk.nextToken());
				break;
			case CREATE_DATE:
				stk.nextToken();	// ":"
				s3Metadata.setCreationDate(KsanUtils.getDate(stk.nextToken()));
				break;
			case LAST_MODIFIED:
				stk.nextToken();	// ":"
				s3Metadata.setLastModified(KsanUtils.getDate(stk.nextToken()));
				break;
			case TIER:
				stk.nextToken();	// ":"
				s3Metadata.setTier(stk.nextToken());
				break;
			case CACHE_CONTROL:
				stk.nextToken();	// ":"
				s3Metadata.setCacheControl(stk.nextToken());
				break;
			case CUSTOMER_ALGORITHM:
				stk.nextToken();	// ":"
				s3Metadata.setCustomerAlgorithm(stk.nextToken());
				break;
			case CUSTOMER_KEY:
				stk.nextToken();	// ":"
				s3Metadata.setCustomerKey(stk.nextToken());
				break;
			case CUSTOMER_KEY_MD5:
				stk.nextToken();	// ":"
				s3Metadata.setCustomerKeyMD5(stk.nextToken());
				break;
			case SERVER_SIDE_ENCRYPTION:
				stk.nextToken();	// ":"
				s3Metadata.setServersideEncryption(stk.nextToken());
				break;
			case DELETE_MARKER:
				stk.nextToken();	// ":"
				s3Metadata.setDeleteMarker(stk.nextToken());
				break;
			case IS_LATEST:
				stk.nextToken();	// ":"
				s3Metadata.setIsLatest(stk.nextToken());
				break;
			case CONTENT_LENGTH:
				stk.nextToken();	// ":"
				s3Metadata.setContentLength(Long.parseLong(stk.nextToken()));
				break;
			case CONTENT_DISPOSITION:
				stk.nextToken();	// ":"
				s3Metadata.setContentDisposition(stk.nextToken());
				break;
			case CONTENT_ENCODING:
				stk.nextToken();	// ":"
				s3Metadata.setContentEncoding(stk.nextToken());
				break;
			case CONTENT_TYPE:
				stk.nextToken();	// ":"
				s3Metadata.setContentType(stk.nextToken());
				break;
			case OWNER_ID:
				stk.nextToken();	// ":"
				s3Metadata.setOwnerId(stk.nextToken());
				break;
			case OWNER_NAME:
				stk.nextToken();	// ":"
				s3Metadata.setOwnerName(stk.nextToken());
				break;
			case CONTENT_LANGUAGE:
				stk.nextToken();	// ":"
				s3Metadata.setContentLanguage(stk.nextToken());
				break;
			case TAGGING_COUNT:
				stk.nextToken();	// ":"
				s3Metadata.setTaggingCount(stk.nextToken());
				break;
			case DECODED_CONTENT_LENGTH:
				stk.nextToken();	// ":"
				s3Metadata.setDecodedContentLength(Long.parseLong(stk.nextToken()));
				break;
			case CONTENT_MD5:
				stk.nextToken();	// ":"
				s3Metadata.setContentMD5(stk.nextToken());
				break;
			case ENCRYPTION:
				stk.nextToken();	// ":"
				s3Metadata.setEncryption(stk.nextToken());
				break;
			case VERSIONID:
				stk.nextToken();	// ":"
				s3Metadata.setVersionId(stk.nextToken());
				break;
			case UPLOADID:
				stk.nextToken();	// ":"
				s3Metadata.setUploadId(stk.nextToken());
				break;
			case PART_NUMBER:
				stk.nextToken();	// ":"
				s3Metadata.setPartNumber(Integer.parseInt(stk.nextToken()));
				break;
			case EXPIRE:
				stk.nextToken();	// ":"
				s3Metadata.setExpires(KsanUtils.getDate(stk.nextToken()));
				break;
			case LOCK_MODE:
				stk.nextToken();	// ":"
				s3Metadata.setLockMode(stk.nextToken());
				break;
			case LOCK_EXPIRES:
				stk.nextToken();	// ":"
				s3Metadata.setLockExpires(stk.nextToken());
				break;
			case LEGAL_HOLD:
				stk.nextToken();	// ":"
				s3Metadata.setLegalHold(stk.nextToken());
				break;
			default:
				logger.error("undefile token : {}", token);
				break;
			}
		}

		return s3Metadata;
	}
}
