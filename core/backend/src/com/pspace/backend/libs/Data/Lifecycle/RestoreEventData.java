package com.pspace.backend.libs.data.lifecycle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.pspace.backend.libs.S3.RestoreObjectConfiguration;

public class RestoreEventData {
	public String bucketName;
	public String objectName;
	public String versionId;
	public String restoreXml;

	public RestoreEventData() {
		init();
	}

	public RestoreEventData(RestoreEventData data) {
		this.bucketName = data.bucketName;
		this.objectName = data.objectName;
		this.versionId = data.versionId;
		this.restoreXml = data.restoreXml;
	}

	public void init() {
		this.bucketName = "";
		this.objectName = "";
		this.versionId = "";
		this.restoreXml = "";
	}

	public RestoreObjectConfiguration getRestoreObjectConfig(String restoreXml) throws JsonMappingException, JsonProcessingException {
		return new XmlMapper().readValue(restoreXml, RestoreObjectConfiguration.class);
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
