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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ReplicationEventData {

	public String Operation;
	public String ObjectName;
	public String VersionId;
	public String SourceBucketName;
	public String TargetBucketName;
	public String TargetRegion;

	public ReplicationEventData() {
		Init();
	}

	public ReplicationEventData(ReplicationEventData data) {
		this.Operation = data.Operation;
		this.ObjectName = data.ObjectName;
		this.VersionId = data.VersionId;
		this.SourceBucketName = data.SourceBucketName;
		this.TargetBucketName = data.TargetBucketName;
		this.TargetRegion = data.TargetRegion;
	}

	public ReplicationEventData(String Operation, String ObjectName, String VersionId, String SourceBucketName,
			String TargetBucketName, String TargetRegion) {
		this.Operation = Operation;
		this.ObjectName = ObjectName;
		this.VersionId = VersionId;
		this.SourceBucketName = SourceBucketName;
		this.TargetBucketName = TargetBucketName;
		this.TargetRegion = TargetRegion;
	}

	public void Init() {
		this.Operation = "";
		this.ObjectName = "";
		this.VersionId = "";
		this.SourceBucketName = "";
		this.TargetBucketName = "";
		this.TargetRegion = "";
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
