package com.pspace.backend.libs.Data.Lifecycle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.pspace.backend.libs.s3format.RestoreObjectConfiguration;

public class RestoreEventData {
	public String BucketName;
	public String ObjectName;
	public String VersionId;
	public String RestoreXml;

	public RestoreEventData() {
		init();
	}

	public RestoreEventData(RestoreEventData data) {
		this.BucketName = data.BucketName;
		this.ObjectName = data.ObjectName;
		this.VersionId = data.VersionId;
		this.RestoreXml = data.RestoreXml;
	}

	public void init() {
		this.BucketName = "";
		this.ObjectName = "";
		this.VersionId = "";
		this.RestoreXml = "";
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
