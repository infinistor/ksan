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
package com.pspace.backend.libs.data.lifecycle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LifecycleEventData {

	public String bucketName;
	public String objectName;
	public String versionId;
	public String storageClass;
	public String uploadId;

	public LifecycleEventData() {
		bucketName = "";
		objectName = "";
		versionId = "";
		storageClass = "";
		uploadId = "";
	}

	public LifecycleEventData(LifecycleEventData data) {
		this.bucketName = data.bucketName;
		this.objectName = data.objectName;
		this.versionId = data.versionId;
		this.storageClass = data.storageClass;
		this.uploadId = data.uploadId;
	}

	public LifecycleEventData(String bucketName, String objectName, String versionId, String storageClass, String uploadId) {
		this.bucketName = bucketName;
		this.objectName = objectName;
		this.versionId = versionId;
		this.storageClass = storageClass;
		this.uploadId = uploadId;
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

	public static class Builder {
		private String bucketName;
		private String objectName;
		private String versionId;
		private String storageClass;
		private String uploadId;

		public Builder(String bucketName, String objectName) {
			this.bucketName = bucketName;
			this.objectName = objectName;
			this.versionId = "";
			this.storageClass = "";
			this.uploadId = "";
		}

		public Builder setVersionId(String versionId) {
			this.versionId = versionId;
			return this;
		}

		public Builder setStorageClass(String storageClass) {
			this.storageClass = storageClass;
			return this;
		}

		public Builder setUploadId(String uploadId) {
			this.uploadId = uploadId;
			return this;
		}

		public LifecycleEventData build() {
			return new LifecycleEventData(bucketName, objectName, versionId, storageClass, uploadId);
		}
	}
}
