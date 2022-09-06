package com.pspace.backend.Data;

public class BackendHeaders {
	public static final String S3_NOT_ACTIVATED = "This S3 is not active.";

	public static final String HEADER_DATA = "NONE";
	public static final String HEADER_BACKEND = "x-ifs-admin";
	
	public static final String HEADER_REPLICATION = "x-ifs-replication";
	public static final String HEADER_VERSIONID = "x-ifs-version-id";

	public static final String HEADER_LOGGING = "x-ifs-logging";
	public static final String HEADER_LIFECYCLE = "x-ifs-lifecycle";

	public static final String S3PROXY_HEADER_NO_DR = "x-amz-ifs-nodr";
	public static final String S3PROXY_HEADER_VERSIONID = "x-amz-ifs-VersionId";
}
