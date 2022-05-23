package com.pspace.ifs.ksan.libs;

public class Constants {
    public static final String PORTAL_REST_API_CONFIG_S3 = "/api/v1/Config/S3";
	public static final String PORTAL_REST_API_DISKPOOLS_DETAILS = "/api/v1/DiskPools/Details";
	public static final String PORTAL_REST_API_S3USERS = "/api/v1/S3Users";
    public static final String KMON_CONFIG_PATH = "/usr/local/ksan/etc/ksanMon.conf";
	public static final String KMON_PROPERTY_PORTAL_IP = "MgsIp";
	public static final String KMON_PROPERTY_PORTAL_PORT = "IfsPortalPort";
	public static final String KMON_POOPERTY_POTAL_KEY = "IfsPortalKey";
	public static final String KMON_PROPERTY_MQ_PORT = "MqPort";
	public static final String KMON_PROPERTY_SERVER_ID = "ServerId";

    public static final String LOG_CONFIG_NOT_EXIST = "Properties file is not exist";
	public static final String LOG_CONFIG_FAILED_LOADING = "Properties file load is fail";
	public static final String LOG_CONFIG_MUST_CONTAIN = "Properties file must contain: ";
}
