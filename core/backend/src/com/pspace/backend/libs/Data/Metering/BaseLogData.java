package com.pspace.backend.Libs.Data.Metering;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspace.backend.Libs.Data.BaseData;

public class BaseLogData implements BaseData {
	public final String inDate;
	public final String user;
	public final String bucket;

	public BaseLogData(String inDate, String user, String bucket) {
		this.inDate = inDate;
		if (StringUtils.isBlank(user))
			this.user = "-";
		else
			this.user = user;

		if (StringUtils.isBlank(bucket))
			this.bucket = "-";
		else
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
