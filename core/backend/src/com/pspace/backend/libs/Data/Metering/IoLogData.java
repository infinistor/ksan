package com.pspace.backend.libs.Data.Metering;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

public class IoLogData extends BaseLogData {
	static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IoLogData.class);

	public long upload;
	public long download;

	public IoLogData(String inDate, String user, String bucket, long upload, long download) {
		super(inDate, user, bucket);
		this.upload = upload;
		this.download = download;
	}

	@Override
	public List<Object> getInsertDBParameters() {
		var param = new ArrayList<Object>();
		param.add(inDate);
		param.add(user);
		param.add(bucket);
		param.add(upload);
		param.add(download);
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
