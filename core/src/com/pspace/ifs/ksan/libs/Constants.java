
/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.ifs.ksan.libs;

public class Constants {
	public static final int MAXBUFSIZE = 524288; // 512 * 1024

	public static final char CHAR_SLASH = '/';
	public static final char CHAR_POINT = '.';
	public static final String SLASH = "/";
	public static final String UNDERSCORE = "_";
	public static final String DASH = "-";
	public static final String POINT = ".";
	public static final String SPACE = " ";

	public static final String OBJ_DIR = "obj";
	public static final String TEMP_DIR = "temp";
	public static final String TEMP_COMPLETE_DIR = "temp/complete";
	public static final String TEMP_COPY_DIR = "temp/copy";
	public static final String TRASH_DIR = "trash";
	public static final String EC_DIR = "ec";

	public static final String VERSIONING_DISABLE_TAIL = "null";

	public static final String COLON = ":";
	public static final String CHARSET_UTF_8 = "UTF-8";

    public static final String PORTAL_REST_API_CONFIG_S3 = "/api/v1/Config/S3";
	public static final String PORTAL_REST_API_DISKPOOLS_DETAILS = "/api/v1/DiskPools/Details";
	public static final String PORTAL_REST_API_S3USERS = "/api/v1/S3Users";
    public static final String AGENT_CONFIG_PATH = "/usr/local/ksan/etc/ksanAgent.conf";
	public static final String DISKPOOL_CONF_PATH = "/var/log/ksan/gw/diskpools_dump.xml";
	public static final String AGENT_PROPERTY_PORTAL_HOST = "PortalHost";
	public static final String AGENT_PROPERTY_PORTAL_PORT = "PortalPort";
	public static final String AGENT_POOPERTY_POTAL_KEY = "PortalApiKey";
	public static final String AGENT_PROPERTY_MQ_HOST = "MQHost";
	public static final String AGENT_PROPERTY_MQ_PORT = "MQPort";
	public static final String AGENT_PROPERTY_MQ_USER = "MQUser";
	public static final String AGENT_PROPERTY_MQ_PASSWORD = "MQPassword";
	public static final String AGENT_PROPERTY_SERVER_ID = "ServerId";
	public static final String AGENT_PROPERTY_SERVICE_MONITOR_INTERVAL = "ServiceMonitorInterval";

    public static final String LOG_CONFIG_NOT_EXIST = "Properties file is not exist";
	public static final String LOG_CONFIG_FAILED_LOADING = "Properties file load is fail";
	public static final String LOG_CONFIG_MUST_CONTAIN = "Properties file must contain: ";

	public static final String HEARTBEAT_ID = "Id";
    public static final String HEARTBEAT_STATE = "State";
	public static final String HEARTBEAT_STATE_ONLINE = "Online";
	public static final String HEARTBEAT_STATE_OFFLINE = "Offline";
	public static final String HEARTBEAT_BINDING_KEY = "*.services.state";

	public static final String LOG_OSDCLIENT_SOCKET_INFO = "socket : {}";
	public static final String LOG_OSDCLIENT_CLOSE_SOCKET_INFO = "close socket : {}";
	public static final String LOG_OSDCLIENT_SOCKET_ERROR = "socket error, {}";
	public static final String LOG_OSDCLIENT_GET_HEADER = "get header : {}";
	public static final String LOG_OSDCLIENT_GET_EC_PART_HEADER = "get ec part header : {}";
	public static final String LOG_OSDCLIENT_READ = "read {} bytes";
	public static final String LOG_OSDCLIENT_WRITE = "write {} bytes";
	public static final String LOG_OSDCLIENT_PUT_HEADER = "put header : {}";
	public static final String LOG_OSDCLIENT_PUT_EC_PART_HEADER = "put ec part header : {}";
	public static final String LOG_OSDCLIENT_DELETE_HEADER = "delete header : {}";
	public static final String LOG_OSDCLIENT_DELETE_REPLICA_HEADER = "delete replica header : {}";
	public static final String LOG_OSDCLIENT_DELETE_EC_PART_HEADER = "delete ec part header : {}";
	public static final String LOG_OSDCLIENT_COPY_HEADER = "copy header : {}";
	public static final String LOG_OSDCLIENT_PART_HEADER = "part header : {}";
	public static final String LOG_OSDCLIENT_DELETE_PART_HEADER = "delete part header : {}";
	public static final String LOG_OSDCLIENT_PART_COPY_HEADER = "partCopy header : {}";
	public static final String LOG_OSDCLIENT_COMPLETE_MULTIPART_HEADER = "completeMultipart header : {}";
	public static final String LOG_OSDCLIENT_ABORT_MULTIPART_HEADER = "abortMultipart header : {}";
	public static final String LOG_OSDCLIENT_UNKNOWN_RESULT = "Unknown result : {}";
	public static final String LOG_OSDCLIENT_ACTIVATE_SOCKET = "activate socket...";
	public static final String LOG_OSDCLIENT_DESACTIVATE_SOCKET = "desactivate socket...";

	public static final String FILE_ATTRIBUTE_REPLICATION = "replica";
	public static final String FILE_ATTRIBUTE_REPLICA_DISK_ID = "diskid";
	public static final String FILE_ATTRUBUTE_REPLICATION_PRIMARY = "pri";
	public static final String FILE_ATTRIBUTE_REPLICATION_REPLICA = "rep";
	public static final String FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL = "nul";
	public static final int FILE_ATTRIBUTE_REPLICATION_SIZE = 3;
	public static final int FILE_ATTRIBUTE_REPLICATION_DISK_ID_SIZE = 36;

	public static final String ZFEC = "zfec -f -d ";
    public static final String ZFEC_PREFIX_OPTION = " -p ";
    public static final String ZFEC_TOTAL_SHARES_OPTION = " -m ";
	public static final String ZFEC_REQUIRED_SHARES_OPTION = " -k ";
	public static final String ZFEC_SUFFIX = ".fec";
	public static final String ZUNFEC = "zunfec -f -o ";

	private Constants() {
		throw new IllegalStateException("Utility class");
	}
}
