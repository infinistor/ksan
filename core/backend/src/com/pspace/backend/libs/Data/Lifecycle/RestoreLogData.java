package com.pspace.backend.libs.data.lifecycle;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspace.backend.libs.data.BaseData;

public class RestoreLogData extends RestoreEventData implements BaseData {
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
	public List<Object> getInsertDBParameters() {
		var param = new ArrayList<Object>();
		param.add(bucketName);
		param.add(objectName);
		param.add(versionId);
		param.add(message);

		return param;
	}

	// @Override
	// public Document getInsertDBDocument() {
	// 	var param = new Document();
	// 	param.put(RestoreLogQuery.DB_BUCKETNAME, BucketName);
	// 	param.put(RestoreLogQuery.DB_OBJECTNAME, ObjectName);
	// 	param.put(RestoreLogQuery.DB_VERSIONID, VersionId);
	// 	param.put(RestoreLogQuery.DB_MESSAGE, message);

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
