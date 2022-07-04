/*
* Copyright 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE.md for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.backend.Data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.pspace.s3format.Tagging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectData {
	static final Logger logger = LoggerFactory.getLogger(ObjectData.class);

	public String BucketName;
	public String ObjectName;
	public long FileSize;
	public Tagging Tags;
	public long LastModified;
	public String VersionId;
	public String DeleteMarker;
	public boolean LastVersion;

	public ObjectData(String BucketName, String ObjectName, long FileSize, Tagging Tags, long LastModified,
			String VersionId, String DeleteMarker, boolean LastVersion) {
		this.BucketName = BucketName;
		this.ObjectName = ObjectName;
		this.FileSize = FileSize;
		this.Tags = Tags;
		this.LastModified = LastModified;
		this.VersionId = VersionId;
		this.DeleteMarker = DeleteMarker;
		this.LastVersion = LastVersion;
	}

	public ObjectData(String BucketName, String ObjectName, long FileSize, String Tags, long LastModified,
			String VersionId, String DeleteMarker, boolean LastVersion) {
		this.BucketName = BucketName;
		this.ObjectName = ObjectName;
		this.FileSize = FileSize;
		setTags(Tags);
		this.LastModified = LastModified;
		this.VersionId = VersionId;
		this.DeleteMarker = DeleteMarker;
		this.LastVersion = LastVersion;
	}

	public void setTags(String StrTags) {
		if (StrTags.isBlank()) {
			Tags = null;
			return;
		}

		try {
			// 수명주기 설정 언마샬링
			Tags = new XmlMapper().readValue(StrTags, Tagging.class);
		} catch (Exception e) {
			logger.error("Set Tag Failed : {}", ObjectName, e);
		}
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
