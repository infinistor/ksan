package com.pspace.backend.LogManager.DB.Table.Replication;

public interface BaseReplicationQuery {
	public static final String DB_TABLE_NAME = "REPLICATION_LOG";
	public static final String DB_ID = "ID";
	public static final String DB_EVENT_START_TIME = "START_TIME";
	public static final String DB_EVENT_END_TIME = "END_TIME";
	public static final String DB_IN_DATE = "INDATE";
	public static final String DB_OPERATION = "OPERATION";
	public static final String DB_OBJECTNAME = "OBJECT_NAME";
	public static final String DB_VERSION_ID = "VERSION_ID";
	public static final String DB_SOURCE_BUCKET_NAME = "SOURCE_BUCKET_NAME";
	public static final String DB_TARGET_BUCKET_NAME = "TARGET_BUCKET_NAME";
	public static final String DB_TARGET_REGION = "TARGET_REGION";
	public static final String DB_MESSAGE = "MESSAGE";
}
