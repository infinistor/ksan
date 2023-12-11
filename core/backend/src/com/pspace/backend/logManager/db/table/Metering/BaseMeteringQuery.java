package com.pspace.backend.logManager.db.table.Metering;

public interface BaseMeteringQuery {
	public static final int MAX_QUERY_SIZE = 1000;
	public static final String DB_DATE_FORMAT = "%Y-%m-%d %k:%i:%s";
	public static final int DEFAULT_EXPIRES_HOUR = 3;
	public static final int DEFAULT_EXPIRES_DAY = 30;
	public static final String DB_IN_DATE = "IN_DATE";
	public static final String DB_USER = "USER";
	public static final String DB_BUCKET = "BUCKET";

}