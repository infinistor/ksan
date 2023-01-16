package com.pspace.backend.libs.Data.Lifecycle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RestoreLogData extends RestoreEventData {
	public String message;

	public RestoreLogData() {
		init();
	}

	public RestoreLogData(RestoreEventData data, String message) {
		super(data);
		this.message = message;
	}

	public void init() {
		this.bucketName = "";
		this.objectName = "";
		this.versionId = "";
		this.restoreXml = "";
		this.message = "";
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
