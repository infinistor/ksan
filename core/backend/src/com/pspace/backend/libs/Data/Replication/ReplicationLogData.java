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
package com.pspace.backend.libs.Data.Replication;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspace.backend.libs.Data.BaseData;

public class ReplicationLogData extends ReplicationEventData implements BaseData {

	public String message;

	public ReplicationLogData(ReplicationEventData event, String message) {
		super(event);
		this.message = message;
		setEndTime();
	}

	@Override
	public List<Object> getInsertDBParameters() {
		var param = new ArrayList<Object>();
		param.add(operation);
		param.add(objectName);
		param.add(versionId);
		param.add(sourceBucketName);
		param.add(targetBucketName);
		param.add(targetRegion);
		param.add(message);

		return param;
	}

	// @Override
	// public Document getInsertDBDocument() {
	// 	var param = new Document();
	// 	param.put(BaseReplicationQuery.DB_IN_DATE, LocalDateTime.now());
	// 	param.put(BaseReplicationQuery.DB_OPERATION, operation);
	// 	param.put(BaseReplicationQuery.DB_OBJECTNAME, objectName);
	// 	param.put(BaseReplicationQuery.DB_VERSION_ID, versionId);
	// 	param.put(BaseReplicationQuery.DB_SOURCE_BUCKET_NAME, sourceBucketName);
	// 	param.put(BaseReplicationQuery.DB_TARGET_BUCKET_NAME, targetBucketName);
	// 	param.put(BaseReplicationQuery.DB_TARGET_REGION, targetRegion);
	// 	param.put(BaseReplicationQuery.DB_MESSAGE, message);

	// 	return param;
	// }

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
