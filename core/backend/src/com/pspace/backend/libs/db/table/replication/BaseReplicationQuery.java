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
package com.pspace.backend.libs.db.table.replication;

public class BaseReplicationQuery {

	public static final String DB_TABLE_NAME = "REPLICATION_LOG";
	public static final String DB_ID = "ID";
	public static final String DB_EVENT_START_TIME = "START_TIME";
	public static final String DB_EVENT_END_TIME = "END_TIME";
	public static final String DB_IN_DATE = "IN_DATE";
	public static final String DB_OPERATION = "OPERATION";
	public static final String DB_OBJECTNAME = "OBJECT_NAME";
	public static final String DB_VERSION_ID = "VERSION_ID";
	public static final String DB_SOURCE_BUCKET_NAME = "SOURCE_BUCKET_NAME";
	public static final String DB_TARGET_BUCKET_NAME = "TARGET_BUCKET_NAME";
	public static final String DB_TARGET_REGION = "TARGET_REGION";
	public static final String DB_MESSAGE = "MESSAGE";

	public static final int MAX_QUERY_SIZE = 1000;

	protected BaseReplicationQuery() {
	}
}
