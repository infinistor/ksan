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
package com.pspace.backend.libs.data.Replication;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspace.backend.libs.Utility;
import com.pspace.backend.libs.data.BaseData;

public class ReplicationEventData implements BaseData {

	public long index;
	public String inDate;
	public String startTime;
	public String endTime;
	public String operation;
	public String objectName;
	public String versionId;
	public String sourceBucketName;
	public String targetBucketName;
	public String targetRegion;

	public ReplicationEventData() {
		index = 0;
		inDate = "";
		startTime = "";
		endTime = "";
		operation = "";
		objectName = "";
		versionId = "";
		sourceBucketName = "";
		targetBucketName = "";
		targetRegion = "";
	}

	public ReplicationEventData(ReplicationEventData data) {
		this.index = data.index;
		this.inDate = data.inDate;
		this.startTime = data.startTime;
		this.endTime = data.endTime;
		this.operation = data.operation;
		this.objectName = data.objectName;
		this.versionId = data.versionId;
		this.sourceBucketName = data.sourceBucketName;
		this.targetBucketName = data.targetBucketName;
		this.targetRegion = data.targetRegion;
	}

	public ReplicationEventData(String operation, String objectName, String versionId, String sourceBucketName, String targetBucketName, String targetRegion) {
		startTime = "";
		endTime = "";
		this.operation = operation;
		this.objectName = objectName;
		this.versionId = versionId;
		this.sourceBucketName = sourceBucketName;
		this.targetBucketName = targetBucketName;
		this.targetRegion = targetRegion;
	}

	public ReplicationEventData(long index, String inDate, String startTime, String endTime, String operation, String objectName, String versionId, String sourceBucketName, String targetBucketName, String targetRegion) {
		this.index = index;
		this.inDate = inDate;
		this.startTime = startTime;
		this.endTime = endTime;
		this.operation = operation;
		this.objectName = objectName;
		this.versionId = versionId;
		this.sourceBucketName = sourceBucketName;
		this.targetBucketName = targetBucketName;
		this.targetRegion = targetRegion;
	}

	public void setStartTime() {
		startTime = Utility.getDateTime();
	}

	public void setEndTime() {
		endTime = Utility.getDateTime();
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

		return param;
	}

	@Override
	public String toString() {
		var mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return super.toString();
		}
	}

	public static ReplicationEventDataBuilder newBuilder() {
		return new ReplicationEventDataBuilder();
	}

	public static class ReplicationEventDataBuilder {
		private long index;
		private String inDate;
		private String startTime;
		private String endTime;
		private String operation;
		private String objectName;
		private String versionId;
		private String sourceBucketName;
		private String targetBucketName;
		private String targetRegion;

		public ReplicationEventDataBuilder() {
			index = 0;
			inDate = "";
			startTime = "";
			endTime = "";
			operation = "";
			objectName = "";
			versionId = "";
			sourceBucketName = "";
			targetBucketName = "";
			targetRegion = "";
		}

		public ReplicationEventDataBuilder setIndex(long index) {
			this.index = index;
			return this;
		}

		public ReplicationEventDataBuilder setInDate(String inDate) {
			this.inDate = inDate;
			return this;
		}

		public ReplicationEventDataBuilder setStartTime(String startTime) {
			this.startTime = startTime;
			return this;
		}

		public ReplicationEventDataBuilder setEndTime(String endTime) {
			this.endTime = endTime;
			return this;
		}

		public ReplicationEventDataBuilder setOperation(String operation) {
			this.operation = operation;
			return this;
		}

		public ReplicationEventDataBuilder setObjectName(String objectName) {
			this.objectName = objectName;
			return this;
		}

		public ReplicationEventDataBuilder setVersionId(String versionId) {
			this.versionId = versionId;
			return this;
		}

		public ReplicationEventDataBuilder setSourceBucketName(String sourceBucketName) {
			this.sourceBucketName = sourceBucketName;
			return this;
		}

		public ReplicationEventDataBuilder setTargetBucketName(String targetBucketName) {
			this.targetBucketName = targetBucketName;
			return this;
		}

		public ReplicationEventDataBuilder setTargetRegion(String targetRegion) {
			this.targetRegion = targetRegion;
			return this;
		}

		public ReplicationEventData build() {
			return new ReplicationEventData(index, inDate, startTime, endTime, operation, objectName, versionId, sourceBucketName, targetBucketName, targetRegion);
		}
	}
}
