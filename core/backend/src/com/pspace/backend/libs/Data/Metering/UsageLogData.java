package com.pspace.backend.libs.Data.Metering;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

public class UsageLogData extends BaseLogData {
	public long usedSize;

	public UsageLogData(String inDate, String user, String bucket, long usedSize) {
		super(inDate, user, bucket);
		this.usedSize = usedSize;
	}

	@Override
	public List<Object> getInsertDBParameters() {
		var param = new ArrayList<Object>();
		param.add(inDate);
		param.add(user);
		param.add(bucket);
		param.add(usedSize);
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
