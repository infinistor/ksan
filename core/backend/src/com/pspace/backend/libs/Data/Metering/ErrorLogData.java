package com.pspace.backend.libs.data.Metering;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ErrorLogData extends BaseLogData {
	static final Logger log = LoggerFactory.getLogger(ErrorLogData.class);
	public long clientError;
	public long serverError;

	public ErrorLogData(String inDate, String user, String bucket) {
		super(inDate, user, bucket);
		clientError = 0;
		serverError = 0;
	}

	public ErrorLogData(String inDate, String user, String bucket, long clientError, long serverError) {
		super(inDate, user, bucket);
		this.clientError = clientError;
		this.serverError = serverError;
	}

	@Override
	public List<Object> getInsertDBParameters() {
		var param = new ArrayList<Object>();
		param.add(inDate);
		param.add(user);
		param.add(bucket);
		param.add(clientError);
		param.add(serverError);
		return param;
	}

	@Override
	public String toString() {
		try {
			var mapper = new ObjectMapper();
			return mapper.writeValueAsString(this);
		} catch (Exception e) {
			return super.toString();
		}
	}
}
