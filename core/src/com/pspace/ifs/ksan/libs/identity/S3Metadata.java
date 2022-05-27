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

import com.google.common.base.Strings;

public class S3Metadata {
    private String storageType;
	private String name;
	private String uri;
	private Map<String, String> userMetadata;
	private String eTag;
	private Date creationDate;
	private Date lastModified;
	private Date lastAccess;
	private Long size;
	private String tier;
	private String cacheControl;
	private String readEncryption;
	private String readEncryptionSize;
	private String customerAlgorithm;
	private String customerKey;
	private String customerKeyMD5;
	private String serversideEncryption;
	private String deleteMarker;
	private String isLatest;
	private Long contentLength;
	private String contentDisposition;
	private String contentEncoding;
	private String contentType;
	private String ownerId;
	private String ownerName;
	private String contentLanguage;
	private Date expires;
	private String taggingCount;
	private long decodedContentLength;
	private String contentMD5;
	private String encryption;
	private String versionId;
	private String uploadId;
	private int partNumber;

	private String lockMode;
	private String lockExpires;
	private String legalHold;

	public S3Metadata() {
		storageType = "";
		name = "";
		uri = "";
		userMetadata = new HashMap<String, String>();
		eTag = "";
		creationDate = null;
		lastModified = null;
		lastAccess = null;
		size = 0L;
		tier = "STANDARD";
		cacheControl = "";
		readEncryption = "";
		readEncryptionSize = "";
		customerAlgorithm = "";
		customerKey = "";
		customerKeyMD5 = "";
		serversideEncryption = "";
		deleteMarker = "";
		isLatest = "";
		contentLength = 0L;
		contentDisposition = "";
		contentEncoding = "";
		contentType = "";
		ownerId = "";
		ownerName = "";
		contentLanguage = "";
		expires = null;
		taggingCount = "";
		decodedContentLength = 0L;
		contentMD5 = "";
		encryption = "";
		versionId = "";
		uploadId = "";
		partNumber = 0;
	}

	public Map<String, String> getUserMetadataMap() {
		return userMetadata;
	}

	public void setUserMetadataMap(Map<String, String> userMetadata) {
		this.userMetadata = userMetadata;
	}
	
	public void addUserMetadata(String Key, String Value) {
		userMetadata.put(Key, Value);
	}

	public String getUserMetadata(String key) {
		return userMetadata.get(key);
	}

	public String getStorageType() {
		return Strings.nullToEmpty(storageType);
	}

	public void setStorageType(String storageType) {
		this.storageType = storageType;
	}

	public String getName() {
		return Strings.nullToEmpty(name);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUri() {
		return Strings.nullToEmpty(uri);
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getETag() {
		return Strings.nullToEmpty(eTag);
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

	public Date getLastAccess() {
		return lastAccess;
	}

	public void setLastAccess(Date lastAccess) {
		this.lastAccess = lastAccess;
	}

	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	public String getTier() {
		return Strings.nullToEmpty(tier);
	}

	public void setTier(String tier) {
		this.tier = tier;
	}

	public String getCacheControl() {
		return Strings.nullToEmpty(cacheControl);
	}

	public void setCacheControl(String cacheControl) {
		this.cacheControl = cacheControl;
	}

	public String getReadEncryption() {
		return Strings.nullToEmpty(readEncryption);
	}

	public void setReadEncryption(String readEncryption) {
		this.readEncryption = readEncryption;
	}

	public String getReadEncryptionSize() {
		return Strings.nullToEmpty(readEncryptionSize);
	}

	public void setReadEncryptionSize(String readEncryptionSize) {
		this.readEncryptionSize = readEncryptionSize;
	}

	public String getCustomerAlgorithm() {
		return Strings.nullToEmpty(customerAlgorithm);
	}

	public void setCustomerAlgorithm(String customerAlgorithm) {
		this.customerAlgorithm = customerAlgorithm;
	}

	public String getCustomerKey() {
		return Strings.nullToEmpty(customerKey);
	}

	public void setCustomerKey(String customerKey) {
		this.customerKey = customerKey;
	}

	public String getCustomerKeyMD5() {
		return Strings.nullToEmpty(customerKeyMD5);
	}

	public void setCustomerKeyMD5(String customerKeyMD5) {
		this.customerKeyMD5 = customerKeyMD5;
	}

	public String getServersideEncryption() {
		return Strings.nullToEmpty(serversideEncryption);
	}

	public void setServersideEncryption(String serversideEncryption) {
		this.serversideEncryption = serversideEncryption;
	}

	public String getDeleteMarker() {
		return Strings.nullToEmpty(deleteMarker);
	}

	public void setDeleteMarker(String deleteMarker) {
		this.deleteMarker = deleteMarker;
	}

	public String getIsLatest() {
		return Strings.nullToEmpty(isLatest);
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
		return Strings.nullToEmpty(contentDisposition);
	}

	public void setContentDisposition(String contentDisposition) {
		this.contentDisposition = contentDisposition;
	}

	public String getContentEncoding() {
		return Strings.nullToEmpty(contentEncoding);
	}

	public void setContentEncoding(String contentEncoding) {
		this.contentEncoding = contentEncoding;
	}

	public String getContentType() {
		return Strings.nullToEmpty(contentType);
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getOwnerId() {
		return Strings.nullToEmpty(ownerId);
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getOwnerName() {
		return Strings.nullToEmpty(ownerName);
	}

	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	public String getContentLanguage() {
		return Strings.nullToEmpty(contentLanguage);
	}

	public void setContentLanguage(String contentLanguage) {
		this.contentLanguage = contentLanguage;
	}

	public Date getExpires() {
		return expires;
	}

	public void setExpires(Date expires) {
		this.expires = expires;
	}

	public String getTaggingCount() {
		return Strings.nullToEmpty(taggingCount);
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
		return Strings.nullToEmpty(contentMD5);
	}

	public void setContentMD5(String contentMD5) {
		this.contentMD5 = contentMD5;
	}

	public String getEncryption() {
		return Strings.nullToEmpty(encryption);
	}

	public void setEncryption(String encryption) {
		this.encryption = encryption;
	}

	public String getVersionId() {
		return Strings.nullToEmpty(versionId);
	}

	public void setVersionId(String versionId) {
		this.versionId = versionId;
	}

	public String getUploadId() {
		return Strings.nullToEmpty(uploadId);
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

	public String getLockMode() {
		return lockMode;
	}

	public void setLockMode(String lockMode) {
		this.lockMode = lockMode;
	}

	public String getLockExpires() {
		return lockExpires;
	}

	public void setLockExpires(String lockExpires) {
		this.lockExpires = lockExpires;
	}

	public String getLegalHold() {
		return legalHold;
	}

	public void setLegalHold(String legalHold) {
		this.legalHold = legalHold;
	}
}
