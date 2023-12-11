package com.pspace.backend.libs.Data.Metering;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ApiLogData extends BaseLogData {
	static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ApiLogData.class);

	public String event;
	public long count;

	public ApiLogData(String inDate, String user, String bucket, String event, long count) {
		super(inDate, user, bucket);
		if (StringUtils.isBlank(event))
			event = "-";
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
