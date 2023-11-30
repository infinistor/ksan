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
package com.pspace.backend.libs.Data;

public class BackendHeaders {
	public static final String S3_NOT_ACTIVATED = "This S3 is not active.";

	public static final String HEADER_DATA = "NONE";
	public static final String HEADER_BACKEND = "x-ifs-admin";
	
	public static final String HEADER_REPLICATION = "x-ifs-replication";
	public static final String HEADER_VERSION_ID = "x-ifs-version-id";

	public static final String HEADER_LOGGING = "x-ifs-logging";
	public static final String HEADER_LIFECYCLE = "x-ifs-lifecycle";

	public static final String S3PROXY_HEADER_NO_DR = "x-amz-ifs-nodr";
	public static final String S3PROXY_HEADER_VERSIONID = "x-amz-ifs-VersionId";
}
