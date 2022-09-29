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
package com.pspace.backend.libs.Data.S3;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class S3LogData {
	public String UserName;
	public String BucketName;
	public String Date;
	public String RemoteHost;
	public String RequestUser;
	public String RequestId;
	public String Operation;
	public String ObjectName;
	public String RequestURI;
	public int StatusCode;
	public String ErrorCode;
	public long ResponseLength;
	public long ObjectLength;
	public long TotalTime;
	public long RequestLength;
	public String Referer;
	public String UserAgent;
	public String VersionId;
	public String HostId;
	public String Sign;
	public String SSLGroup;
	public String SignType;
	public String EndPoint;
	public String TLSVersion;

	public S3LogData(){
		Init();
	}
	public S3LogData(String UserName, String BucketName, String Date, String Remote_host,
			String RequestUser, String RequestId, String Operation, String ObjectName, String RequestURI,
			int StatusCode, String ErrorCode, long ResponseLength, long ObjectLength, long TotalTime,
			long RequestLength, String Referer, String UserAgent, String VersionId, String HostId, String Sign,
			String SSLGroup, String SignType, String EndPoint, String TLSVersion) {
		this.UserName = UserName;
		this.BucketName = BucketName;
		this.Date = Date;
		this.RemoteHost = Remote_host;
		this.RequestUser = RequestUser;
		this.RequestId = RequestId;
		this.Operation = Operation;
		this.ObjectName = ObjectName;
		this.RequestURI = RequestURI;
		this.StatusCode = StatusCode;
		this.ErrorCode = ErrorCode;
		this.ResponseLength = ResponseLength;
		this.ObjectLength = ObjectLength;
		this.TotalTime = TotalTime;
		this.RequestLength = RequestLength;
		this.Referer = Referer;
		this.UserAgent = UserAgent;
		this.VersionId = VersionId;
		this.HostId = HostId;
		this.Sign = Sign;
		this.SSLGroup = SSLGroup;
		this.SignType = SignType;
		this.EndPoint = EndPoint;
		this.TLSVersion = TLSVersion;
	}

	public void Init() {
		UserName = "";
		BucketName = "";
		Date = "";
		RemoteHost = "";
		RequestUser = "";
		RequestId = "";
		Operation = "";
		ObjectName = "";
		RequestURI = "";
		StatusCode = 0;
		ErrorCode = "";
		ResponseLength = 0;
		ObjectLength = 0;
		TotalTime = 0;
		RequestLength = 0;
		Referer = "";
		UserAgent = "";
		VersionId = "";
		HostId = "";
		Sign = "";
		SSLGroup = "";
		SignType = "";
		EndPoint = "";
		TLSVersion = "";
	}

	public boolean isOperationEmpty() {
		return Operation.isEmpty() || Operation.equals("-");
	}
	public boolean isBucketNameEmpty() {
		return BucketName.isEmpty() || BucketName.equals("-");
	}
	public boolean isError() {
		return !ErrorCode.equals("-");
	}

	public String Print() {
		return String.format("%s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s\n",
				UserName, BucketName, Date, RemoteHost, RequestUser, RequestId, Operation, ObjectName,
				RequestURI, StatusCode, ErrorCode, ResponseLength, ObjectLength, TotalTime, RequestLength, Referer,
				UserAgent, VersionId, HostId, Sign, SSLGroup, SignType, EndPoint, TLSVersion);
	}

	@Override
	public String toString() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return "";
		}
	}
}
