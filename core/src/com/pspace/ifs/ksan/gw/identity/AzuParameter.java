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

public class AzuParameter {
    private HttpServletRequest request;
	private HttpServletResponse response;
	private InputStream inputStream;
    private S3User user;
    private String comp;
    private String restype;
    private String prefix;
    private String delimiter;
    private String marker;
    private String include;
    private String maxresults;
    private String timeout;

    private String userName;
    private String containerName;
    private String blobName;
    private String pathCategory;
	private String method;
    private int statusCode;
    private String errorCode;
    private long requestSize;
    private long responseSize;

    public AzuParameter() {
        request = null;
        response = null;
        inputStream = null;
        user = null;
        comp = "";
        restype = "";
        prefix = "";
        delimiter = "";
        marker = "";
        include = "";
        maxresults = "";
        timeout = "";
        userName = "";
        containerName = "";
        blobName = "";
        method = "";
        statusCode = 0;
        errorCode = "";
        requestSize = 0L;
        responseSize = 0L;
    }

    public AzuParameter(AzuParameter parameter) {
        request = parameter.getRequest();
        response = parameter.getResponse();
        inputStream = parameter.getInputStream();
        user = parameter.getUser();
        comp = parameter.getComp();
        restype = parameter.getRestype();
        prefix = parameter.getPrefix();
        delimiter = parameter.getDelimiter();
        marker = parameter.getMarker();
        include = parameter.getInclude();
        maxresults = parameter.getMaxResults();
        timeout = parameter.getTimeout();
        userName = parameter.getUserName();
        containerName = parameter.getContainerName();
        blobName = parameter.getBlobName();
        method = parameter.getMethod();
        statusCode = parameter.getStatusCode();
        errorCode = parameter.getErrorCode();
        requestSize = parameter.getRequestSize();
        responseSize = parameter.getResponseSize();
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
    
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }
    
    public S3User getUser() {
        return user;
    }

    public void setUser(S3User user) {
        this.user = user;
    }
    
    public String getComp() {
        return comp;
    }
    
    public void setComp(String comp) {
        this.comp = comp;
    }
    
    public String getRestype() {
        return restype;
    }
    
    public void setRestype(String restype) {
        this.restype = restype;
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }
    
    public String getDelimiter() {
        return delimiter;
    }
    
    public void setMarker(String marker) {
        this.marker = marker;
    }
    
    public String getMarker() {
        return marker;
    }
    
    public void setInclude(String include) {
        this.include = include;
    }
    
    public String getInclude() {
        return include;
    }
    
    public void setMaxresults(String maxresults) {
        this.maxresults = maxresults;
    }
    
    public String getMaxResults() {
        return maxresults;
    }
    
    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }
    
    public String getTimeout() {
        return timeout;
    }
    
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getUserName() {
        return this.userName;
    }
    
    public String getContainerName() {
        return containerName;
    }
    
    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }
    
    public String getBlobName() {
        return blobName;
    }
    
    public void setBlobName(String blobName) {
        this.blobName = blobName;
    }
    
    public String getPathCategory() {
        return pathCategory;
    }
    
    public void setPathCategory(String pathCategory) {
        this.pathCategory = pathCategory;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
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
    
}

