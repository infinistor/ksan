package com.pspace.backend.libs.data.Replication;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ReplicationSuccessData extends ReplicationEventData {

	public ReplicationSuccessData(ReplicationEventData event) {
		super(event);
		setEndTime();
	}

	@Override
	public List<Object> getInsertDBParameters() {
		var param = new ArrayList<Object>();
		param.add(inDate);
		param.add(startTime);
		param.add(endTime);
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
}
