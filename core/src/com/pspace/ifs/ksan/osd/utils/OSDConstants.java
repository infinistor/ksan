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

package com.pspace.ifs.ksan.osd.utils;

public final class OSDConstants {
    public static final String HOOK_THREAD_INFO = "Hook Thread....";
	public static final String STOP_KSAN_OSD = "Gracefully Stop KSAN-OSD !!";
    public static final String HTTPS = "https://";
    public static final String CHARSET_UTF_8 = "UTF-8";
    public static final String KMON_CONFIG_PATH = "/usr/local/ksan/etc/ksanAgent.conf";
    public static final String CONFIG_PATH = "/var/log/ksan/osd/osd_dump.conf";
    public static final String PID_PATH = "/var/run/ksanosd.pid";
    public static final String DISKPOOL_CONF_PATH = "/var/log/ksan/gw/diskpools_dump.xml";
    public static final String SERVICEID_PATH = "/usr/local/ksan/sbin/.ksanOSD.ServiceId";
    public static final String POOL_SIZE = "pool_size";
    public static final String OSD_LOCAL_IP = "local_ip";
    public static final String OSD_PORT = "port";

    public static final String OBJ_DIR = "obj";
	public static final String TEMP_DIR = "temp";
	public static final String TRASH_DIR = "trash";
	public static final String EC_DIR = "ec";

    public static final String EC_FUNCTION = "ec";
    public static final String EC_SCHEDULE_MINUTES = "ec_schedule_minutes";
    public static final String EC_APPLY_MINUTES = "ec_apply_minutes";
    public static final String EC_FILE_SIZE = "ec_file_size";

    public static final String CACHE_DISK = "cache_disk";
    public static final String CACHE_SCHEDULE_MINUTES = "cache_schedule_minutes";
    public static final String CACHE_FILE_SIZE = "cache_file_size";
    public static final String CACHE_LIMIT_MINUTES = "cache_limit_minutes";

    public static final String TRASH_SCHEDULE_MINUTES = "trash_schedule_minutes";

    public static final String PERFORMANCE_MODE = "performance_mode";
    public static final String PERFORMANCE_MODE_NO_DISK = "NO_DISK";

    public static final String FILE_ATTRIBUTE_REPLICATION = "replication";
    public static final String FILE_ATTRIBUTE_REPLICA_DISK_ID = "replica-diskid";
	public static final String FILE_ATTRUBUTE_REPLICATION_PRIMARY = "primary";
	public static final String FILE_ATTRIBUTE_REPLICATION_REPLICA = "replica";
    public static final String FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL = "null";

    public static final int HEADERSIZE = 1024;
    public static final int MAXBUFSIZE = 524288; // 512 * 1024
	public static final int BUFSIZE = 262144; // 256 * 1024
	public static final long PARTS_MIN_SIZE = 5242880; // 5MB
    public static final long MEGABYTES = 1024 * 1024;
    public static final long ONE_SEC_MILLISECONDS = 1000;
    public static final long ONE_MINUTE_MILLISECONDS = 60 * 1000;
    public static final long ONE_HOUR_MILLISECONDS = 60 * 60 * 1000;

    public static final int RETRY_COUNT = 3;

    public static final int RANGE_OFFSET_INDEX = 0;
    public static final int RANGE_LENGTH_INDEX = 1;

    public static final char CHAR_SLASH = '/';
    public static final String SLASH = "/";
    public static final String UNDERSCORE = "_";
    public static final String COMMA = ",";
    public static final String POINT = ".";
    public static final String SPACE = " ";
    public static final String COLON = ":";
    public static final String MD5 = "MD5";
    public static final String EMPTY_STRING = "";

    public static final String PROPERTY_COMMONS_CRYPTO_STREAM_BUFFER_SIZE = "commons.crypto.stream.buffer.size";
	public static final long COMMONS_CRYPTO_STREAM_BUFFER_SIZE = MAXBUFSIZE;

    public static final String AUTHORIZATION = "Authorization";
    public static final String MQUEUE_EXCHANGE_NAME = "ksan.system";
	public static final String MQUEUE_NAME_OSD_CONFIG = "ksan-osd-configure-";
    public static final String MQUEUE_NAME_OSD_DISKPOOL = "ksan-osd-diskpool-";
    public static final String MQUEUE_EXCHANGE_NAME_FOR_OSD = "ksan.osdExchange";
    public static final String MQUEUE_NAME_OSD_MOVE_OBJECT = "ksan-osd-move-object-";
    public static final String MQUEUE_NAME_OSD_DELETE_OBJECT = "ksan-osd-delete-object-";
    public static final String MQUEUE_NAME_OSD_GETATTR_OBJECT = "ksan-osd-getattr-object-";
    public static final String MQUEUE_NAME_OSD_COPY_OBJECT = "ksan-osd-copy-object-";
	public static final String MQUEUE_NAME_OSD_CONFIG_ROUTING_KEY = "*.services.osd.config.*";
	public static final String MQUEUE_NAME_OSD_DISKPOOL_ROUTING_KEY = "*.servers.diskpools.*";
    public static final String PORTAL_REST_API_CONFIG_OSD = "/api/v1/Config/KsanOsd";
	public static final String PORTAL_REST_API_DISKPOOLS_DETAILS = "/api/v1/DiskPools/Details";
    public static final String MQUEUE_NAME_OSD_OBJECT_ROUTING_KEY_PREFIX = "*.services.osd.";
    public static final String MQUEUE_NAME_OSD_MOVE_OBJECT_ROUTING_KEY_SUFFIX = ".object.move";
    public static final String MQUEUE_NAME_OSD_DELETE_OBJECT_ROUTING_KEY_SUFFIX = ".object.unlink";
    public static final String MQUEUE_NAME_OSD_GETATTR_OBJECT_ROUTING_KEY_SUFFIX = ".object.getattr";
    public static final String MQUEUE_NAME_OSD_COPY_OBJECT_ROUTING_KEY_SUFFIX = ".object.copy";
    public static final String PORTAL_REST_API_KSAN_EVENT = "/api/v1/Services/Event";

	public static final String PORTAL_REST_API_KSAN_EVENT_ID = "Id";
	public static final String PORTAL_REST_API_KSAN_EVENT_TYPE = "EventType";
	public static final String PORTAL_REST_API_KSAN_EVENT_MESSAGE = "Message";
	public static final String PORTAL_REST_API_KSAN_EVENT_START = "start";
	public static final String PORTAL_REST_API_KSAN_EVENT_STOP = "stop";
	public static final String PORTAL_REST_API_KSAN_EVENT_SIGTERM = "SIGTERM";

    // MonConfig
	public static final String LOG_CONFIG_NOT_EXIST = "Properties file is not exist";
	public static final String LOG_CONFIG_FAILED_LOADING = "Properties file load is fail";
	public static final String LOG_CONFIG_MUST_CONTAIN = "Properties file must contain: ";
    public static final String KMON_PROPERTY_PORTAL_IP = "MgsIp";
	public static final String KMON_PROPERTY_PORTAL_PORT = "IfsPortal";
	public static final String KMON_POOPERTY_POTAL_KEY = "IfsPortalKey";
	public static final String KMON_PROPERTY_MQ_PORT = "MqPort";
	public static final String KMON_PROPERTY_SERVER_ID = "ServerId";

    // DISKPOOLLIST
    public static final String DISKPOOLLIST = "DISKPOOLLIST";
    public static final String DISKPOOL = "DISKPOOL";
    public static final String SERVER = "SERVER";
    public static final String DISK = "DISK";

    // OSDServer constants
    public static final String LOG_OSD_SERVER_START = "OSD Server start ...";
    public static final String LOG_OSD_SERVER_PID = "pid : {}";
    public static final String LOG_OSD_SERVER_CONFIGURE_DISPOOLS = "configure diskpools ...";
    public static final String LOG_OSD_SERVER_DISK_POOL_INFO = "disk pool id : {}, name = {}";
    public static final String LOG_OSD_SERVER_SERVER_SIZE = "server size : {}";
    public static final String LOG_OSD_SERVER_CONNECTED_INFO = "connected : {}";
    public static final String LOG_OSD_SERVER_UNKNOWN_INDICATOR = "Unknown indicator : {}";
    public static final String LOG_OSD_SERVER_CAN_NOT_FIND_OSD_IP = "Can't find dest OSD IP... dest Path : {}";
    public static final String LOG_OSD_SERVER_DIFFERENCE_ETAG = "diff ETag, src : {}, dest : {}";
    public static final String LOG_OSD_SERVER_RANGE_INFO = "offset : {}, length : {}";
    public static final String LOG_OSD_SERVER_FAILED_FILE_DELETE = "failed file delete {}";
    public static final String LOG_OSD_SERVER_UNKNOWN_DATA = "Unknown data : {}";
    public static final String LOG_OSD_SERVER_FAILED_FILE_RENAME = "failed file rename {} -> {}";
    
    public static final String LOG_OSD_SERVER_GET_START = "get start ...";
    public static final String LOG_OSD_SERVER_GET_INFO = "path : {}, objId : {}, versionId : {}, source range : {}";
    public static final String LOG_OSD_SERVER_GET_END = "get end ... read total : {}";
    public static final String LOG_OSD_SERVER_GET_SUCCESS_INFO = "get - success : path={}, objId={}, versionId={}, source range={}";
    
    public static final String LOG_OSD_SERVER_PUT_START = "put start ...";
    public static final String LOG_OSD_SERVER_PUT_INFO = "path : {}, objId : {}, versionId : {}, length : {}, replicaiton : {}, mode : {}";
    public static final String LOG_OSD_SERVER_PUT_END = "put end ...";
    public static final String LOG_OSD_SERVER_PUT_SUCCESS_INFO = "put - success : path={}, objId={}, versionId={}, length={}";

    public static final String LOG_OSD_SERVER_DELETE_START = "delete start ...";
    public static final String LOG_OSD_SERVER_DELETE_INFO = "path : {}, objId : {}, versionId : {}";
    public static final String LOG_OSD_SERVER_DELETE_END = "delete end ...";
    public static final String LOG_OSD_SERVER_DELETE_SUCCESS_INFO = "delete - success : path={}, objId={}, versionId={}";

    public static final String LOG_OSD_SERVER_DELETE_PART_START = "delete part start ...";
    public static final String LOG_OSD_SERVER_DELETE_PART_INFO = "path : {}, objId : {}, partNo : {}";
    public static final String LOG_OSD_SERVER_DELETE_PART_END = "delete part end ...";
    public static final String LOG_OSD_SERVER_DELETE_PART_SUCCESS_INFO = "delete - success : path={}, objId={}, partNo={}";

    public static final String LOG_OSD_SERVER_DELETE_REPLICA_START = "delete replica start ...";
    public static final String LOG_OSD_SERVER_DELETE_REPLICA_PATH = "path : {}";
    public static final String LOG_OSD_SERVER_DELETE_REPLICA_END = "delete replica end ...";

    public static final String LOG_OSD_SERVER_COPY_START = "copy start ...";
    public static final String LOG_OSD_SERVER_COPY_INFO = "srcPath : {}, srcObjId : {}, srcVersionId : {}, destPath : {}, destObjId : {}, destVersionId : {}";
    public static final String LOG_OSD_SERVER_COPY_RELAY_OSD = "relay osd {}, put header : {}";
    public static final String LOG_OSD_SERVER_COPY_SUCCESS_INFO = "copy - success : srcPath={}, srcObjId={}, srcVersionId={}, destPath={}, destObjId={}, destVersionId={}";

    public static final String LOG_OSD_SERVER_GET_PART_START = "getPart start ...";
    public static final String LOG_OSD_SERVER_GET_PART_INFO = "path : {}, objId : {}, partNo : {}";
    public static final String LOG_OSD_SERVER_GET_PART_END = "getPart end ... read total : {}";
    public static final String LOG_OSD_SERVER_GET_PART_SUCCESS_INFO = "get - success : path={}, objId={}, partNo={}";

    public static final String LOG_OSD_SERVER_PART_START = "part start ...";
    public static final String LOG_OSD_SERVER_PART_INFO = "path : {}, objId : {}, partNo : {}, length : {}";
    public static final String LOG_OSD_SERVER_PART_END = "part end ...";
    public static final String LOG_OSD_SERVER_PART_SUCCESS_INFO = "part - success : path={}, objId={}, partNo={}, length={}";

    public static final String LOG_OSD_SERVER_PART_COPY_START = "part copy start ...";
    public static final String LOG_OSD_SERVER_PART_COPY_INFO = "srcPath : {}, srcObjId : {}, srcVersionId : {}, destPath : {}, destObjId : {}, destPartNo : {}, copySourceRange : {}";
    public static final String LOG_OSD_SERVER_PART_COPY_RELAY_OSD = "relay osd {}, part header : {}";
    public static final String LOG_OSD_SERVER_PART_COPY_END = "part copy end ...";
    public static final String LOG_OSD_SERVER_PART_COPY_SUCCESS_INFO = "part copy - success : srcPath={}, srcObjId={}, srcVersionId={}, destPath={}, destObjId={}, destPartNo={}, copySourceRange={}";

    public static final String LOG_OSD_SERVER_COMPLETE_MULTIPART_START ="completeMultipart start ...";
    public static final String LOG_OSD_SERVER_COMPLETE_MULTIPART_INFO = "path : {}, objId : {}, partNos : {}";
    public static final String LOG_OSD_SERVER_COMPLETE_MULTIPART_END = "completeMultipart end ...";
    public static final String LOG_OSD_SERVER_COMPLETE_MULTIPART_SUCCESS_INFO = "completeMultipart - success : path={}, objId={}, partNos={}";

    public static final String LOG_OSD_SERVER_ABORE_MULTIPART_START = "abortMultipart start ...";
    public static final String LOG_OSD_SERVER_ABORE_MULTIPART_INFO = "path : {}, objId : {}, partNos : {}";
    public static final String LOG_OSD_SERVER_ABORE_MULTIPART_END = "abortMultipart end ...";
    public static final String LOG_OSD_SERVER_ABORE_MULTIPART_SUCCESS_INFO = "abortMultipart - success : path : {}, objId : {}, partNos : {}";

    // DoECPriObject
    public static final String LOG_DO_EC_PRI_OBJECT_START = "DoECPriObject start ...";
    public static final String LOG_DO_EC_PRI_OBJECT_LOCAL_IP = "ip = {}";
    public static final String LOG_DO_EC_PRI_OBJECT_DISKLIST_SIZE = "diskList size : {}";
    public static final String LOG_DO_EC_PRI_OBJECT_PATH = "objPath : {}, ecPath : {}";
    public static final String LOG_DO_EC_PRI_OBJECT_FILE = "file : {}";
    public static final String LOG_DO_EC_PRI_OBJECT_APPLY_MINUTES = "ec apply minutes : {}";
    public static final String LOG_DO_EC_PRI_OBJECT_ENCODE_EC = "ENCODE EC : {}";
    public static final String DO_EC_PRI_OBJECT_ZFEC = "zfec -d ";
    public static final String DO_EC_PRI_OBJECT_ZFEC_PREFIX_OPTION = " -p ";
    public static final String DO_EC_PRI_OBJECT_ZFEC_TOTAL_NUMBER_OPTION = " -m 4 ";
    public static final String LOG_DO_EC_PRI_OBJECT_ZFEC_EXIT_CODE = "ENCODE exit code : {}";
    public static final String LOG_DO_EC_PRI_OBJECT_ZFEC_COMMAND = "command : {}";
    public static final String LOG_DO_EC_PRI_OBJECT_REPLICA_DISK_ID = "replica disk id : {}";
    public static final String LOG_DO_EC_PRI_OBJECT_HEADER = "send header : {}";

    // DoEmptyTrash
    public static final String LOG_DO_EMPTY_TRASH_START = "DoEmptyTrash start ...";

    // DoMoveCacheToDisk
    public static final String LOG_DO_MOVE_CACHE_TO_DISK = "DoMoveCacheToDisk start ...";
    public static final String DO_MOVE_CACHE_TO_DISK_COMMAND = "cp -a ";
    public static final String LOG_DO_MOVE_CACHE_TO_DISK_COMMAND = "{}";
    public static final String LOG_DO_MOVE_CACHE_TO_DISK_TARGET_PATH = "targetPath : {}";

    // MoveObject
    public static final String LOG_MOVE_OBJECT_START = "MoveObject start ...";
}
