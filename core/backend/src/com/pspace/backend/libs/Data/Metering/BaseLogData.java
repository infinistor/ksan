package com.pspace.backend.libs.Data.Metering;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspace.backend.libs.Data.BaseData;

public class BaseLogData implements BaseData {
	public final String inDate;
	public String user;
	public String bucket;

	public BaseLogData(String inDate, String user, String bucket) {
		this.inDate = inDate;
		this.user = user;
		this.bucket = bucket;
	}

	@Override
	public List<Object> getInsertDBParameters() {
		var param = new ArrayList<Object>();
		param.add(inDate);
		param.add(user);
		param.add(bucket);
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
