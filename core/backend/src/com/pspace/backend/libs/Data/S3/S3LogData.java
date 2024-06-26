/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License. See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.backend.libs.data.s3;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspace.backend.libs.data.BaseData;

public class S3LogData implements BaseData {
	@JsonProperty("UserName")
	public String userName;
	@JsonProperty("BucketName")
	public String bucketName;
	@JsonProperty("Date")
	public long date;
	@JsonProperty("RemoteHost")
	public String remoteHost;
	@JsonProperty("RequestUser")
	public String requestUser;
	@JsonProperty("RequestId")
	public String requestId;
	@JsonProperty("Operation")
	public String operation;
	@JsonProperty("ObjectName")
	public String objectName;
	@JsonProperty("RequestURI")
	public String requestURI;
	@JsonProperty("StatusCode")
	public int statusCode;
	@JsonProperty("ErrorCode")
	public String errorCode;
	@JsonProperty("ResponseLength")
	public long responseLength;
	@JsonProperty("ObjectLength")
	public long objectLength;
	@JsonProperty("TotalTime")
	public long totalTime;
	@JsonProperty("RequestLength")
	public long requestLength;
	@JsonProperty("Referer")
	public String referer;
	@JsonProperty("UserAgent")
	public String userAgent;
	@JsonProperty("VersionId")
	public String versionId;
	@JsonProperty("HostId")
	public String hostId;
	@JsonProperty("Sign")
	public String sign;
	@JsonProperty("SSLGroup")
	public String sslGroup;
	@JsonProperty("SignType")
	public String signType;
	@JsonProperty("EndPoint")
	public String endPoint;
	@JsonProperty("TLSVersion")
	public String tlsVersion;

	public S3LogData() {
		Init();
	}

	public S3LogData(String userName, String bucketName, long date, String remoteHost,
			String requestUser, String requestId, String operation, String objectName, String requestURI,
			int statusCode, String errorCode, long responseLength, long objectLength, long totalTime,
			long requestLength, String referer, String userAgent, String versionId, String hostId, String sign,
			String sslGroup, String signType, String endPoint, String tlsVersion) {
		this.userName = userName;
		this.bucketName = bucketName;
		this.date = date;
		this.remoteHost = remoteHost;
		this.requestUser = requestUser;
		this.requestId = requestId;
		this.operation = operation;
		this.objectName = objectName;
		this.requestURI = requestURI;
		this.statusCode = statusCode;
		this.errorCode = errorCode;
		this.responseLength = responseLength;
		this.objectLength = objectLength;
		this.totalTime = totalTime;
		this.requestLength = requestLength;
		this.referer = referer;
		this.userAgent = userAgent;
		this.versionId = versionId;
		this.hostId = hostId;
		this.sign = sign;
		this.sslGroup = sslGroup;
		this.signType = signType;
		this.endPoint = endPoint;
		this.tlsVersion = tlsVersion;
	}

	public void Init() {
		userName = null;
		bucketName = null;
		date = 0L;
		remoteHost = null;
		requestUser = null;
		requestId = null;
		operation = null;
		objectName = null;
		requestURI = null;
		statusCode = 0;
		errorCode = null;
		responseLength = 0;
		objectLength = 0;
		totalTime = 0;
		requestLength = 0;
		referer = null;
		userAgent = null;
		versionId = null;
		hostId = null;
		sign = null;
		sslGroup = null;
		signType = null;
		endPoint = null;
		tlsVersion = null;
	}

	@JsonIgnore
	public boolean isOperationEmpty() {
		return operation.isEmpty() || operation.equals("-");
	}

	@JsonIgnore
	public boolean isBucketNameEmpty() {
		return bucketName.isEmpty() || bucketName.equals("-");
	}

	@JsonIgnore
	public boolean isError() {
		return !errorCode.equals("-") && !errorCode.isBlank();
	}

	@JsonIgnore
	public String Print() {
		return String.format("%s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s\n",
				userName, bucketName, date, remoteHost, requestUser, requestId, operation, objectName,
				requestURI, statusCode, errorCode, responseLength, objectLength, totalTime, requestLength, referer,
				userAgent, versionId, hostId, sign, sslGroup, signType, endPoint, tlsVersion);
	}

	@Override
	public List<Object> getInsertDBParameters() {
		var param = new ArrayList<Object>();
		param.add(userName);
		param.add(bucketName);
		param.add(date);
		param.add(remoteHost);
		param.add(requestUser);
		param.add(requestId);
		param.add(operation);
		param.add(objectName);
		param.add(requestURI);
		param.add(statusCode);
		param.add(errorCode);
		param.add(responseLength);
		param.add(objectLength);
		param.add(totalTime);
		param.add(requestLength);
		param.add(referer);
		param.add(userAgent);
		param.add(versionId);
		param.add(hostId);
		param.add(sign);
		param.add(sslGroup);
		param.add(signType);
		param.add(endPoint);
		param.add(tlsVersion);

		return param;
	}

	@Override
	public String toString() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return null;
		}
	}
}
