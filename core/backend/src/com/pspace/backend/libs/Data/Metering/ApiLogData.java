package com.pspace.backend.Libs.Data.Metering;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspace.backend.Libs.S3.S3Parameters;

public class ApiLogData extends BaseLogData {
	static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ApiLogData.class);

	public String event;
	public long count;

	public ApiLogData(String inDate, String user, String bucket) {
		super(inDate, user, bucket);
		event = S3Parameters.OP_PUT_OBJECT;
		count = 0;
	}

	public ApiLogData(String inDate, String user, String bucket, String event, long count) {
		super(inDate, user, bucket);
		if (StringUtils.isBlank(event))
			this.event = "-";
		else
			this.event = event;
		this.count = count;
	}

	@Override
	public List<Object> getInsertDBParameters() {
		var param = new ArrayList<Object>();
		param.add(inDate);
		param.add(user);
		param.add(bucket);
		param.add(event);
		param.add(count);
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
