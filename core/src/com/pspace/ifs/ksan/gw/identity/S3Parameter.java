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
package com.pspace.ifs.ksan.gw.identity;

import java.io.InputStream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.common.base.Strings;

public class S3Parameter {
	private HttpServletRequest request;
	private HttpServletResponse response;
	private InputStream inputStream;
    private String bucketName;
    private String objectName;
    private String pathCategory;
    private int maxTimeSkew;
	private String method;
	private S3User user;
	private S3Bucket bucket;
	private S3Bucket srcBucket;
    private String srcBucketName;
	private String srcPath;
	private String versionId;
	private String srcVersionId;
	private Long startTime;
	private long maxFileSize;
    private long fileSize;
    private String uploadId;
	private String partNumber;
	private boolean isWebsite;
    private boolean isPublicAccess;
    private int statusCode;
    private String errorCode;
    private String virtualHost;
	private String remoteHost;
    private String remoteAddr;
	private String requestURI;
	private String referer;
	private String userAgent;
	private String authorization;
	private String xAmzAlgorithm;
	private String hostName;
	private String hostID;
    private String operation;
    private String requestID;
    private long requestSize;
    private long responseSize;
    private String signVersion;
    private String uri;
    private boolean isAdmin;
    private String taggingInfo;

    private String identity;
    private String proxyHost;

    // KMS Parameter
	private String kmsType;
	private String kmsEndpoint;

    public S3Parameter() {
        request = null;
        response = null;
        inputStream = null;
        bucketName = "";
        objectName = "";
        pathCategory = "";
        maxTimeSkew = 0;
        method = "";
        user = null;
        bucket = null;
        srcBucket = null;
        srcPath = "";
        versionId = "";
        srcVersionId = "";
        startTime = 0L;
        maxFileSize = 0L;
        fileSize = 0L;
        uploadId = "";
        isWebsite = false;
        isPublicAccess = false;
        statusCode = 0;
        requestSize = 0L;
        responseSize = 0L;
        uri = "";
        isAdmin = false;
        taggingInfo = "";
        identity = "";
        proxyHost = "";
	    kmsType = "";
	    kmsEndpoint = "";
    }

    public S3Parameter(S3Parameter parameter) {
        request = parameter.getRequest();
        response = parameter.getResponse();
        inputStream = parameter.getInputStream();
        bucketName = parameter.getBucketName();
        objectName = parameter.getObjectName();
        pathCategory = parameter.getPathCategory();
        maxTimeSkew = parameter.getMaxTimeSkew();
        method = parameter.getMethod();
        user = parameter.getUser();
        bucket = parameter.getBucket();
        srcBucket = parameter.getSrcBucket();
        srcPath = parameter.getSrcPath();
        versionId = parameter.getVersionId();
        srcVersionId = parameter.getSrcVersionId();
        startTime = parameter.getStartTime();
        maxFileSize = parameter.getMaxFileSize();
        fileSize = parameter.getFileSize();
        uploadId = parameter.getUploadId();
        isWebsite = parameter.isWebsite();
        isPublicAccess = parameter.isPublicAccess();
        statusCode = parameter.getStatusCode();
        requestSize = parameter.getRequestSize();
        responseSize = parameter.getResponseSize();
        uri = parameter.getUri();
        isAdmin = parameter.isAdmin();
        taggingInfo = parameter.getTaggingInfo();
        identity = parameter.getIdentity();
        proxyHost = parameter.getProxyHost();
        kmsType = parameter.getKmsType();
        kmsEndpoint = parameter.getKmsEndpoint();
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public String getKmsType() {
        return kmsType;
    }

    public void setKmsType(String kmsType) {
        this.kmsType = kmsType;
    }

    public String getKmsEndpoint() {
        return kmsEndpoint;
    }

    public void setKmsEndpoint(String kmsEndpoint) {
        this.kmsEndpoint = kmsEndpoint;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream is) {
        this.inputStream = is;
    }

    public String getBucketName() {
        return Strings.nullToEmpty(bucketName);
    }

    public void setBucketName(String bucket) {
        this.bucketName = bucket;
    }

    public String getObjectName() {
        return Strings.nullToEmpty(objectName);
    }

    public void setObjectName(String object) {
        this.objectName = object;
    }

    public String getPathCategory() {
        return Strings.nullToEmpty(pathCategory);
    }

    public void setPathCategory(String pathCategory) {
        this.pathCategory = pathCategory;
    }

    public int getMaxTimeSkew() {
        return maxTimeSkew;
    }

    public void setMaxTimeSkew(int maxTimeSkew) {
        this.maxTimeSkew = maxTimeSkew;
    }

    public String getMethod() {
        return Strings.nullToEmpty(method);
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public S3User getUser() {
        return new S3User(user);
    }

    public void setUser(S3User user) {
        this.user = new S3User(user);
    }

    public S3Bucket getBucket() {
        return bucket;
    }

    public void setBucket(S3Bucket bucket) {
        this.bucket = new S3Bucket(bucket);
    }

    public S3Bucket getSrcBucket() {
        return srcBucket;
    }

    public void setSrcBucket(S3Bucket srcBucket) {
        this.srcBucket = new S3Bucket(srcBucket);
    }

    public String getSrcBucketName() {
        return srcBucketName;
    }

    public void setSrcBucketName(String srcBucketName) {
        this.srcBucketName = srcBucketName;
    }

    public String getSrcPath() {
        return Strings.nullToEmpty(srcPath);
    }

    public void setSrcPath(String srcPath) {
        this.srcPath = srcPath;
    }

    public String getVersionId() {
        return Strings.nullToEmpty(versionId);
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getSrcVersionId() {
        return Strings.nullToEmpty(srcVersionId);
    }

    public void setSrcVersionId(String srcVersionId) {
        this.srcVersionId = srcVersionId;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getUploadId() {
        return Strings.nullToEmpty(uploadId);
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getPartNumber() {
        return Strings.nullToEmpty(partNumber);
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public boolean isWebsite() {
        return isWebsite;
    }

    public void setWebsite(boolean website) {
        this.isWebsite = website;
    }

    public boolean isPublicAccess() {
        return isPublicAccess;
    }

    public void setPublicAccess(boolean isPublicAccess) {
        this.isPublicAccess = isPublicAccess;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public String getRequestURI() {
        return requestURI;
    }

    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getAuthorization() {
        return authorization;
    }

    public void setAuthorization(String authorization) {
        this.authorization = authorization;
    }

    public String getxAmzAlgorithm() {
        return xAmzAlgorithm;
    }

    public void setxAmzAlgorithm(String xAmzAlgorithm) {
        this.xAmzAlgorithm = xAmzAlgorithm;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostname) {
        this.hostName = hostname;
    }

    public String getHostID() {
        return hostID;
    }

    public void setHostID(String hostid) {
        this.hostID = hostid;
    }
    
    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getRequestID() {
        return requestID;
    }

    public void setRequestID(String requestID) {
        this.requestID = requestID;
    }

    public void setRequestSize(long requestSize) {
        this.requestSize = requestSize;
    }

    public void addRequestSize(long size) {
        this.requestSize += size;
    }

    public long getRequestSize() {
        return requestSize;
    }

    public void addResponseSize(long size) {
        this.responseSize += size;
    }

    public void setResponseSize(long size) {
        this.responseSize = size;
    }

    public long getResponseSize() {
        return responseSize;
    }

    public String getSignVersion() {
        return signVersion;
    }

    public void setSignVersion(String signVersion) {
        this.signVersion = signVersion;
    }

    public String getURI() {
        return uri;
    }

    public void setURI(String uri) {
        this.uri = uri;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public String getTaggingInfo() {
        return taggingInfo;
    }

    public void setTaggingInfo(String taggingInfo) {
        this.taggingInfo = taggingInfo;
    }
}
