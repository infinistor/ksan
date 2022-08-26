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
package com.pspace.ifs.ksan.gw.utils;

import java.util.Set;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableSet;

public final class GWConstants {
	public static final String PID_PATH = "/var/run/ksangw.pid";
	public static final String JVM = "jvm";
    public static final String GET_PROCESS_ID = "getProcessId";
	public static final String LOG_GW_PID = "pid : {}";

	public static final String GW = "GW";
	public static final String HTTPS = "https://";
	public static final String INTENTIONALLY_NOT_IMPLEMENTED = "intentionally not implemented";
	public static final String HOOK_THREAD_INFO = "Hook Thread....";
	public static final String STOP_KSAN_GW = "Gracefully Stop KSAN-GW !!";
	public static final String CONFIG_PATH = "/var/log/ksan/gw/gw_dump.conf";
	public static final String OBJMANAGER_CONFIG_PATH = "/var/log/ksan/objmanager/objmanager_dump.conf";
	public static final String DISKPOOL_CONF_PATH = "/var/log/ksan/gw/diskpools_dump.xml";
	public static final String SERVICEID_PATH = ".ksanGW.ServiceId";

	public static final String MQUEUE_NAME = "disk";
    public static final String MQUEUE_EXCHANGE_NAME = "ksan.system";
    public static final String MQUEUE_OSD_EXCHANGE_NAME = "OSDExchange";

	public static final String MQUEUE_NAME_GW_CONFIG = "ksan-gw-configure-";
	public static final String MQUEUE_NAME_GW_CONFIG_ROUTING_KEY = "*.services.gw.config.*";
	public static final String MQUEUE_NAME_GW_DISK = "ksan-gw-disk-";
	public static final String MQUEUE_NAME_GW_DISK_ADDED_ROUTING_KEY = "*.servers.disks.added";
	public static final String MQUEUE_NAME_GW_DISK_UPDATED_ROUTING_KEY = "*.servers.disks.updated";
	public static final String MQUEUE_NAME_GW_DISK_REMOVED_ROUTING_KEY = "*.servers.disks.removed";
	public static final String MQUEUE_NAME_GW_DISK_STATE_ROUTING_KEY = "*.servers.disks.state";
	public static final String MQUEUE_NAME_GW_DISK_RWMODE_ROUTING_KEY = "*.servers.disks.rwmode";
	public static final String MQUEUE_NAME_GW_DISKPOOL = "ksan-gw-diskpool-";
	public static final String MQUEUE_NAME_GW_DISKPOOL_ROUTING_KEY = "*.servers.diskpools.*";
	public static final String MQUEUE_NAME_GW_USER = "ksan-gw-user-";
	public static final String MQUEUE_NAME_GW_USER_ROUTING_KEY = "*.services.gw.user.*";
	public static final String MQUEUE_NAME_GW_SERVICE = "ksan-gw-service-";
	public static final String MQUEUE_NAME_GW_SERVICE_ADDED_ROUTING_KEY = "*.services.added";
	public static final String MQUEUE_NAME_GW_SERVICE_UPDATED_ROUTING_KEY = "*.services.updated";
	public static final String MQUEUE_NAME_GW_SERVICE_REMOVED_ROUTING_KEY = "*.services.removed";

	public static final String PORTAL_REST_API_CONFIG_GW = "/api/v1/Config/KsanGw";
	public static final String PORTAL_REST_API_DISKPOOLS_DETAILS = "/api/v1/DiskPools/Details";
	public static final String PORTAL_REST_API_KSAN_USERS = "/api/v1/KsanUsers";
	public static final String PORTAL_REST_API_KSAN_REGIONS = "/api/v1/Regions";
	public static final String PORTAL_REST_API_KSAN_EVENT = "/api/v1/Services/Event";

	public static final String PORTAL_REST_API_KSAN_EVENT_ID = "Id";
	public static final String PORTAL_REST_API_KSAN_EVENT_TYPE = "EventType";
	public static final String PORTAL_REST_API_KSAN_EVENT_MESSAGE = "Message";
	public static final String PORTAL_REST_API_KSAN_EVENT_START = "start";
	public static final String PORTAL_REST_API_KSAN_EVENT_STOP = "stop";
	public static final String PORTAL_REST_API_KSAN_EVENT_SIGTERM = "SIGTERM";

	public static final String KMON_PROPERTY_PORTAL_IP = "MgsIp";
	public static final String KMON_PROPERTY_PORTAL_PORT = "IfsPortal";
	public static final String KMON_POOPERTY_POTAL_KEY = "IfsPortalKey";
	public static final String KMON_PROPERTY_MQ_PORT = "MqPort";
	public static final String KMON_PROPERTY_SERVER_ID = "ServerId";

	public static final String PERFORMANCE_MODE_NO_OPTION = "NO_OPTION";
	public static final String PERFORMANCE_MODE_NO_IO = "NO_IO";
	public static final String PERFORMANCE_MODE_NO_DISK = "NO_DISK";
	public static final String PERFORMANCE_MODE_NO_REPLICA = "NO_REPLICA";
	public static final int JETTY_MAX_THREADS = 1000;
	public static final int JETTY_MAX_IDLE_TIMEOUT = 30000;
	public static final long MAX_FILE_SIZE = 100 * 1024 * 1024 * 1024;
	public static final long MAX_LIST_SIZE = 200000;
	public static final long MEGABYTES = 1048576;
	public static final int MAX_TIME_SKEW = 15 * 60;
	public static final int DEFAULT_OSD_PORT = 8000;
	public static final int DEFAULT_OSD_CLIENT_SIZE = 10;
	public static final int DEFAULT_OBJMANAGER_SIZE = 10;
	public static final String OR = " or ";

	public static final String AWS_XMLNS = "http://s3.amazonaws.com/doc/2006-03-01/";
	
	public static final String FAKE_INITIATOR_ID =	"arn:aws:iam::111122223333:" + "user/samsung-user-11116a31-17b5-4fb7-9df5-b288870f11xx";
	public static final String FAKE_INITIATOR_DISPLAY_NAME = "umat-samsung-11116a31-17b5-4fb7-9df5-b288870f11xx";
	public static final String FAKE_REQUEST_ID = "4442587FB7D0A2F9";
	
	public static final CharMatcher VALID_BUCKET_CHAR =
			CharMatcher.inRange('a', 'z')
			.or(CharMatcher.inRange('0', '9'))
			.or(CharMatcher.is('-'))
			.or(CharMatcher.is('.'));

	public static final Set<String> CANNED_ACLS = ImmutableSet.of(
			"private",
			"public-read",
			"public-read-write",
			"authenticated-read",
			"bucket-owner-read",
			"bucket-owner-full-control",
			"log-delivery-write"
			);

	public static final String INVALID_HEADER = "Invalid header";
	public static final String INVALID_CREDENTIAL = "Invalid Credential: ";
	public static final String INVALID_SIGNATURE = "Invalid signature";

	public static final String XML_VERSION = "<?xml version";
	public static final String XML_VERSION_FULL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	public static final String XML_VERSION_FULL_STANDALONE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>";
	public static final String XML_VERSION_CONFIGURATION_END = "<VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"/>";
	public static final String XML_VERSION_CONFIGURATION = "<VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">";
	public static final String XML_VERSION_CONFIGURATION_STATUS = "<Status>";
	public static final String XML_VERSION_CONFIGURATION_STATUS_TAIL ="</Status>";
	public static final String XML_VERSION_CONFIGURATION_TAIL = "</VersioningConfiguration>";
	public static final String XML_CONTENT_TYPE = "application/xml";
	
	public static final String DIRECTORY_SUFFIX = GWConstants.SLASH;
	
	public static final int MAXBUFSIZE = 524288; // 512 * 1024
	public static final int BUFSIZE = 262144; // 256 * 1024
	public static final long PARTS_MIN_SIZE = 5242880; // 5MB

	public static final String DEFAULT_MAX_KEYS = "1000";
	   
	public static final String DIRECTORY_MD5 = "d41d8cd98f00b204e9800998ecf8427e";
	
	public static final String MARIADB = "MariaDB";
	public static final String MARIADB_URL = "jdbc:mariadb://";
	public static final String MARIADB_OPTIONS = "?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8";
	public static final String JDBC_DRIVER = "org.mariadb.jdbc.Driver";
	public static final String MARIADB_VALIDATION_QUERY = "select 1";

	public static final String OBJ_DIR = "obj";
	public static final String TEMP_DIR = "temp";
	public static final String TEMP_COMPLETE_DIR = "temp/complete";
	public static final String TEMP_COPY_DIR = "temp/copy";
	public static final String TRASH_DIR = "trash";
	public static final String EC_DIR = "ec";
	public static final int RETRY_COUNT = 3;

	public static final String METHOD_PUT = "PUT";
	public static final String METHOD_GET = "GET";
	public static final String METHOD_POST = "POST";
	public static final String METHOD_DELETE = "DELETE";
	public static final String METHOD_HEAD = "HEAD";
	public static final String METHOD_OPTIONS = "OPTIONS";
	
	public static final String STRING_TRUE = "TRUE";
	public static final String STRING_FALSE = "FALSE";
	public static final String ALLOW = "Allow";
	public static final String STRING_ERROR = "Error";
	public static final String CODE = "Code";
	public static final String MESSAGE = "Message";
	public static final String REQUEST_ID = "RequestId";

	public static final char CHAR_NEWLINE = '\n';
	public static final char CHAR_CARRIAGE_RETURN = '\r';
	public static final char CHAR_QUESTION = '?';
	public static final char CHAR_AMPERSAND = '&';
	public static final char CHAR_EQUAL = '=';
	public static final char CHAR_COLON = ':';
	public static final char CHAR_SEMICOLON = ';';
	public static final char CHAR_COMMA = ',';
	public static final char CHAR_SLASH = '/';
	public static final char CHAR_SPACE = ' ';
	public static final char CHAR_DASH = '-';
	public static final char CHAR_ASTERISK = '*';
	public static final char CHAR_LEFT_BRACE = '{';
	public static final char CHAR_RIGHT_BRACE = '}';
	public static final String EMPTY_STRING = "";
	public static final String AWS = "AWS";
	public static final String AWS_SPACE = "AWS ";
	public static final String AWS4_HMAC = "AWS4-HMAC";
	public static final String AWS4 = "AWS4";
	public static final String HMAC = "Hmac";
	public static final String HMACSHA256 = "HmacSHA256";
	public static final String HMACSHA1 = "HmacSHA1";
	public static final String AWS4_REQUEST = "aws4_request";
	public static final String S3_AWS4_REQUEST = "/s3/aws4_request";
	public static final String ASTERISK = "*";
	public static final String COMMA = ",";
	public static final String SLASH = "/";
	public static final String POINT = ".";
	public static final String COLON = ":";
	public static final String SEMICOLON = ";";
	public static final String SPACE = " ";
	public static final String EQUAL = "=";
	public static final String UNDERSCORE = "_";
	public static final String DASH = "-";
	public static final String PLUS = "+";
	public static final String URL_ESCAPER_FORMAT = "%20";
	public static final String AMPERSAND = "&";
	public static final String DOLLAR_SIGN = "$";
	public static final String NEWLINE = "\n";
	public static final String DOUBLE_QUOTE = "\"";
	public static final String SPACE_COLON_SPACE = " : ";
	public static final String DOUBLE_POINT = "..";
	public static final String POINT_DASH = ".-";
	public static final String DASH_POINT = "-.";
	public static final String XNN_DASH_DASH = "xn--";
	public static final String BACKSLASH_S_PLUS = "\\s+";
	public static final String URL = "url";
	public static final String WEBSITE = "website";
	public static final String SLASH_WEBSITE = "/website";
	public static final String POINT_WEBSIZE_POINT = ".website.";
	public static final String SLASH_WEBSITE_SLASH = "/website/";
	public static final String PERCENT_ESCAPER = "*-./_";
	public static final String PERCENT_ESCAPER_SIGNATURE = "-_.~";
	public static final String PARAMETER_BUCKET = "?bucket=";
	public static final String PARAMETER_KEY = "&key=";
	public static final String PARAMETER_ETAG = "&etag=%22";
	public static final String ENCODING_DOUBLE_QUOTE = "%22";
	public static final String STATUS_SC_OK = "200";

	public static final int AUTH_FIELD_SIZE = 2;
	public static final int IDENTITY_INDEX = 0;
	public static final int SIGNATURE_INDEX = 1;
	public static final int AWS4_AUTH_FIELD_SIZE = 5;
	public static final int AWS4_IDENTITY_FIELD_INDEX = 0;
	public static final int AWS4_DATE_INDEX = 1;
	public static final int AWS4_REGION_INDEX = 2;
	public static final int AWS4_SERVICE_INDEX = 3;
	
	public static final String OBJECT_TYPE_FILE = "file";
	public static final String OBJECT_TYPE_MARK = "mark";
	public static final String STATUS_ENABLED = "Enabled";
	public static final String STATUS_DISABLED = "Disabled";
	public static final String VERSIONING_ENABLED = "Enabled";
	public static final String VERSIONING_SUSPENDED = "Suspended";
	public static final String VERSIONING_DISABLE_TAIL = "null";
	public static final String GOVERNANCE = "GOVERNANCE";
	public static final String COMPLIANCE = "COMPLIANCE";

	public static final String S3_ARN = "arn:aws:s3";
	public static final String HTTP = "http://";
	public static final String S3_AMAZON_AWS_COM = ".s3.amazonaws.com/";
	public static final String BUCKET = "Bucket";
	public static final String KEY = "Key";
	public static final String KEY_ASSIGN = "Key=";
	public static final String VALUE = "Value";
	public static final String VALUE_ASSIGN = ",Vaule=";
	public static final String ETAG = "ETag";
	public static final String PART = "Part";
	public static final String PARTNUMBER = "PartNumber";
	public static final String AWS_TIER_STANTARD = "STANDARD";

	public static final String CATEGORY_ROOT = "root";
	public static final String CATEGORY_BUCKET = "bucket";
	public static final String CATEGORY_OBJECT = "object";
	public static final String PARAMETER_WEBSITE = "website";
	public static final String PARAMETER_POLICY = "policy";
	public static final String PARAMETER_CORS = "cors";
	public static final String PARAMETER_LIFECYCLE = "lifecycle";
	public static final String PARAMETER_PUBLIC_ACCESS_BLOCK = "publicAccessBlock";
	public static final String PARAMETER_TAGGING = "tagging";
	public static final String PARAMETER_ENCRYPTION = "encryption";
	public static final String PARAMETER_OBJECT_LOCK = "object-lock";
	public static final String PARAMETER_REPLICATION = "replication";
	public static final String PARAMETER_POLICY_STATUS = "policyStatus";
	public static final String PARAMETER_ACL = "acl";
	public static final String PARAMETER_UPLOAD_ID = "uploadId";
	public static final String PARAMETER_LOCATION = "location";
	public static final String PARAMETER_LOGGING = "logging";
	public static final String PARAMETER_NOTIFICATION = "notification";
	public static final String PARAMETER_UPLOADS = "uploads";
	public static final String PARAMETER_VERSIONING = "versioning";
	public static final String PARAMETER_LIST_TYPE = "list-type";
	public static final String PARAMETER_VERSIONS = "versions";
	public static final String PARAMETER_RETENTION = "retention";
	public static final String PARAMETER_LEGAL_HOLD = "legal-hold";
	public static final String PARAMETER_DELETE = "delete";
	public static final String PARAMETER_REQUEST_PAYMENT = "requestPayment";
	public static final String PARAMETER_PART_NUMBER = "partNumber";
	public static final String PARAMETER_COPY_SOURCE = "x-amz-copy-source";
	public static final String PARAMETER_TORRENT = "torrent";
	public static final String PARAMETER_VERSION_ID = "versionId";
	public static final String SUB_PARAMETER_VERSIONID = "?versionId=";
	public static final String PARAMETER_BACKSLASH_VERSIONID = "\\?versionId=";
	public static final String CONTENT_TYPE_POST_OBJECT = "multipart/form-data; boundary=";
	public static final String SUCCESS_ACTION_STATUS = "success_action_status";
	public static final String SUCCESS_ACTION_REDIRECT = "success_action_redirect";
	public static final String UNDEFINED_METHOD = "undefined method : {}";

	public static final String START_WITH_X_AMZ = "x-amz-";

	// policy actions constants
	public static final String ACTION_ALL = "s3:*";

	public static final String ACTION_ABORT_MULTIPART_UPLOAD = "s3:AbortMultipartUpload";
	public static final String ACTION_CREATE_BUCKET = "s3:CreateBucket";
	public static final String ACTION_DELETE_BUCKET = "s3:DeleteBucket";

	public static final String ACTION_DELETE_BUCKET_POLICY = "s3:DeleteBucketPolicy";
	public static final String ACTION_DELETE_BUCKET_WEBSITE = "s3:DeleteBucketWebsite";
	public static final String ACTION_DELETE_OBJECT = "s3:DeleteObject";
	public static final String ACTION_DELETE_OBJECT_VERSION = "s3:DeleteObjectVersion";
	public static final String ACTION_DELETE_OBJECT_TAGGING = "s3:DeleteObjectTagging";
	public static final String ACTION_DELETE_OBJECT_VERSION_TAGGING = "s3:DeleteObjectVersionTagging";

	public static final String ACTION_GET_BUCKET_ACL = "s3:GetBucketAcl";
	public static final String ACTION_GET_BUCKET_CORS = "s3:GetBucketCORS";
	public static final String ACTION_GET_BUCKET_LOGGING = "s3:GetBucketLogging";
	public static final String ACTION_GET_BUCKET_NOTIFICATION = "s3:GetBucketNotification";
	public static final String ACTION_GET_BUCKET_OBJECT_LOCK_CONFIGURATION = "s3:GetBucketObjectLockConfiguration";
	public static final String ACTION_GET_BUCKET_POLICY = "s3:GetBucketPolicy";
	public static final String ACTION_GET_BUCKET_PUBLIC_ACCESS_BLOCK = "s3:GetBucketPublicAccessBlock";
	public static final String ACTION_GET_BUCKET_TAGGING = "s3:GetBucketTagging";
	public static final String ACTION_GET_BUCKET_VERSIONING = "s3:GetBucketVersioning";
	public static final String ACTION_GET_BUCKET_WEBSITE = "s3:GetBucketWebsite";
	public static final String ACTION_GET_ENCRYPTION_CONFIGURATION = "s3:GetEncryptionConfiguration";
	public static final String ACTION_GET_LIFECYCLE_CONFIGURATION = "s3:GetLifecycleConfiguration";
	public static final String ACTION_GET_REPLICATION_CONFIGURATION = "s3:GetReplicationConfiguration";

	public static final String ACTION_GET_OBJECT = "s3:GetObject";
	public static final String ACTION_GET_OBJECT_ACL = "s3:GetObjectAcl";
	public static final String ACTION_GET_OBJECT_LEGAL_HOLD = "s3:GetObjectLegalHold";
	public static final String ACTION_GET_OBJECT_RETENTION = "s3:GetObjectRetention";
	public static final String ACTION_GET_OBJECT_TAGGING = "s3:GetObjectTagging";
	public static final String ACTION_GET_OBJECT_VERSION = "s3:GetObjectVersion";
	public static final String ACTION_GET_OBJECT_VERSION_ACL = "s3:GetObjectVersionAcl";
	public static final String ACTION_GET_OBJECT_VERSION_TAGGING = "s3:GetObjectVersionTagging";

	public static final String ACTION_LIST_BUCKET = "s3:ListBucket";
	public static final String ACTION_LIST_BUCKET_MULTIPART_UPLOADS = "s3:ListBucketMultipartUploads";
	public static final String ACTION_LIST_BUCKET_VERSIONS = "s3:ListBucketVersions";
	public static final String ACTION_LIST_MULTIPART_UPLOAD_PARTS = "s3:ListMultipartUploadParts";
	
	public static final String ACTION_PUT_BUCKET_ACL = "s3:PutBucketAcl";
	public static final String ACTION_PUT_BUCKET_CORS = "s3:PutBucketCORS";
	public static final String ACTION_PUT_BUCKET_LOGGING = "s3:PutBucketLogging";
	public static final String ACTION_PUT_BUCKET_NOTIFICATION = "s3:PutBucketNotification";
	public static final String ACTION_PUT_BUCKET_OBJECT_LOCK_CONFIGURATION = "s3:PutBucketObjectLockConfiguration";
	public static final String ACTION_PUT_BUCKET_POLICY = "s3:PutBucketPolicy";
	public static final String ACTION_PUT_BUCKET_TAGGING = "s3:PutBucketTagging";
	public static final String ACTION_PUT_BUCKET_VERSIONING = "s3:PutBucketVersioning";
	public static final String ACTION_PUT_BUCKET_WEBSITE = "s3:PutBucketWebsite";
	public static final String ACTION_PUT_ENCRYPTION_CONFIGURATION = "s3:PutEncryptionConfiguration";
	public static final String ACTION_PUT_LIFECYCLE_CONFIGURATION = "s3:PutLifecycleConfiguration";
	public static final String ACTION_PUT_REPLICATION_CONFIGURATION = "s3:PutReplicationConfiguration";

	public static final String ACTION_PUT_OBJECT = "s3:PutObject";
	public static final String ACTION_PUT_OBJECT_ACL = "s3:PutObjectAcl";
	public static final String ACTION_PUT_OBJECT_LEGAL_HOLD = "s3:PutObjectLegalHold";
	public static final String ACTION_PUT_OBJECT_RETENTION = "s3:PutObjectRetention";
	public static final String ACTION_PUT_OBJECT_TAGGING = "s3:PutObjectTagging";
	public static final String ACTION_PUT_OBJECT_VERSION_ACL = "s3:PutObjectVersionAcl";
	public static final String ACTION_PUT_OBJECT_VERSION_TAGGING = "s3:PutObjectVersionTagging";

	public static final String ACTION_BYPASS_GOVERNANCE_RETENTION = "s3:BypassGovernanceRetention";
	

	public static final String CHARSET_UTF_8 = "UTF-8";
	public static final String UTC = "UTC";
	public static final String GMT = "GMT";
	public static final String ISO_8601_TIME_FORMAT = "yyyyMMdd'T'HHmmss'Z'";
	public static final String ISO_8601_TIME_SIMPLE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	public static final String ISO_8601_TIME_FORMAT_MILI = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	public static final String LIFECYCLE_CONTAIN_TIME = "T00:00:00";

	public static final String HMAC_SHA1 = "HmacSHA1";
	public static final String AWS4_HMAC_SHA256 = "AWS4-HMAC-SHA256";
	public static final String SIGN_CREDENTIAL = " Credential=";
	public static final String SIGN_REQEUEST_SIGNED_HEADERS = ", requestSignedHeaders=";
	public static final String SIGN_SIGNATURE = ", Signature=";
	public static final String AUTH_HEADER = "AuthHeader";
	public static final String QUERY_STRING = "QueryString";

	public static final String MD5 = "MD5";
	public static final String SHA256 = "SHA256";
	public static final String SHA_256 = "SHA-256";
	public static final String SHA1 = "SHA1";
	public static final String SHA_1 = "SHA-1";
	public static final String AES_CBC_PKCS5PADDING = "AES/CBC/PKCS5Padding";
	public static final String AES = "AES";
	public static final String AES256 = "AES256";
	public static final String INFINISTOR = "INFINISTOR";
	public static final String SIGNATURE_FIELD = "Signature=";
    public static final String CREDENTIAL_FIELD = "Credential=";

	public static final String SIGNEDHEADERS_EQUAL = "SignedHeaders=";
	public static final String AWS_ACCESS_KEY_ID = "AWSAccessKeyId";
	public static final String SIGNATURE = "Signature";

	public static final String X_AMZ_ALGORITHM = "X-Amz-Algorithm";
	public static final String X_AMZ_ID_2 = "x-amz-id-2";
	public static final String X_FORWARDED_FOR = "X-Forwarded-For";
	public static final String X_AMZ_DATE = "X-Amz-Date";
	public static final String X_AMZ_DATE_LOWER = "x-amz-date";
	public static final String X_AMZ_SIGNATURE = "X-Amz-Signature";
	public static final String X_AMZ_CREDENTIAL = "X-Amz-Credential";
	public static final String X_AMZ_SIGNEDHEADERS = "X-Amz-SignedHeaders";
	public static final String X_AMZ_EXPIRES = "X-Amz-Expires";
	public static final String X_AMZ_CONTENT_SHA256 = "x-amz-content-sha256";
	public static final String X_AMZ_REQUEST_PAYER = "x-amz-request-payer";
	public static final String X_AMZ_EXPECTED_BUCKET_OWNER = "x-amz-expected-bucket-owner";
	public static final String X_AMZ_CONFIRM_REMOVE_SELF_BUCKET_ACCESS = "x-amz-confirm-remove-self-bucket-access";
	public static final String X_AMZ_COPY_SOURCE = "x-amz-copy-source";
	public static final String X_AMZ_COPY_SOURCE_IF_MATCH = "x-amz-copy-source-if-match";
	public static final String X_AMZ_COPY_SOURCE_IF_MODIFIED_SINCE = "x-amz-copy-source-if-modified-since";
	public static final String X_AMZ_COPY_SOURCE_IF_NONE_MATCH = "x-amz-copy-source-if-none-match";
	public static final String X_AMZ_COPY_SOURCE_IF_UNMODIFIED_SINCE = "x-amz-copy-source-if-unmodified-since";
	public static final String X_AMZ_ACL = "x-amz-acl";
	public static final String X_AMZ_GRANT_FULL_CONTROL = "x-amz-grant-full-control";
	public static final String X_AMZ_GRANT_READ = "x-amz-grant-read";
	public static final String X_AMZ_GRANT_READ_ACP = "x-amz-grant-read-acp";
	public static final String X_AMZ_GRANT_WRITE = "x-amz-grant-write";
	public static final String X_AMZ_GRANT_WRITE_ACP = "x-amz-grant-write-acp";
	public static final String X_AMZ_METADATA_DIRECTIVE = "x-amz-metadata-directive";
	public static final String X_AMZ_TAGGING_DIRECTIVE = "x-amz-tagging-directive";
	public static final String X_AMZ_SERVER_SIDE_ENCRYPTION = "x-amz-server-side-encryption";
	public static final String X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM = "x-amz-server-side-encryption-customer-algorithm";
	public static final String X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY = "x-amz-server-side-encryption-customer-key";
	public static final String X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5 = "x-amz-server-side-encryption-customer-key-MD5";
	public static final String X_AMZ_SERVER_SIDE_ENCRYPTION_AWS_KMS_KEY_ID = "x-amz-server-side-encryption-aws-kms-key-id";
	public static final String X_AMZ_STORAGE_CLASS = "x-amz-storage-class";
	public static final String X_AMZ_WEBSITE_REDIRECT_LOCATION = "x-amz-website-redirect-location";
	public static final String X_AMZ_SERVER_SIDE_ENCRYPTION_CONTEXT = "x-amz-server-side-encryption-context";
	public static final String X_AMZ_SERVER_SIDE_ENCRYPTION_BUCKET_KEY_ENABLED = "x-amz-server-side-encryption-bucket-key-enabled";
	public static final String X_AMZ_COPY_SOURCE_SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM = "x-amz-copy-source-server-side-encryption-customer-algorithm";
	public static final String X_AMZ_COPY_SOURCE_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY = "x-amz-copy-source-server-side-encryption-customer-key";
	public static final String X_AMZ_COPY_SOURCE_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5 = "x-amz-copy-source-server-side-encryption-customer-key-MD5";
	public static final String X_AMZ_TAGGING = "x-amz-tagging";
	public static final String X_AMZ_TAGGING_COUNT = "x-amz-tagging-count";
	public static final String X_AMZ_OBJECT_LOCK_MODE = "x-amz-object-lock-mode";
	public static final String X_AMZ_OBJECT_LOCK_RETAIN_UNTIL_DATE = "x-amz-object-lock-retain-until-date";
	public static final String X_AMZ_OBJECT_LOCK_LEGAL_HOLD = "x-amz-object-lock-legal-hold";
	public static final String X_AMZ_SOURCE_EXPECTED_BUCKET_OWNER = "x-amz-source-expected-bucket-owner";
	public static final String X_AMZ_BUCKET_OBJECT_LOCK_ENABLED = "x-amz-bucket-object-lock-enabled";
	public static final String X_AMZ_MFA = "x-amz-mfa";
	public static final String X_AMZ_REQUEST_S3_PAYER = "x-amz-s3Parameter.request-payer";
	public static final String X_AMZ_BYPASS_GOVERNANCE_RETENTION = "x-amz-bypass-governance-retention";
	public static final String X_AMZ_REQUEST_S3_GET_PAYER = "x-amz-s3Parameter.getRequest()-payer";
	public static final String X_AMZ_BUCKET_OBJECT_LOCK_TOKEN = "x-amz-bucket-object-lock-token";
	public static final String X_AMZ_DECODED_CONTENT_LENGTH = "x-amz-decoded-content-length";
	public static final String X_AMZ_COPY_SOURCE_RANGE = "x-amz-copy-source-range";
	public static final String X_AMZ_VERSION_ID = "x-amz-version-id";
	public static final String X_AMZ_DELETE_MARKER = "x-amz-delete-marker";

	public static final String USER_METADATA_PREFIX = "x-amz-meta-";

	public static final String X_IFS_ADMIN = "x-ifs-admin";
	public static final String X_IFS_VERSION_ID = "x-ifs-version-id";
	public static final String X_IFS_REPLICATION = "x-ifs-replication";
	public static final String X_IFS_DR = "x-ifs-dr";
	public static final String X_IFS_LOGGING = "x-ifs-logging";
	
	public static final String IFS_HEADER_REPLICATION = "replication";
	public static final String IFS_HEADER_DR = "dr";
	public static final String IFS_HEADER_LOGGING = "logging";

	public static final String STREAMING_AWS4_HMAC_SHA256_PAYLOAD = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD";
	public static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";

	public static final String BYTES = "bytes";
	public static final String AUTHORIZATION = "Authorization";
	public static final String UPLOAD_ID = "uploadId";
	
	public static final String CACHE_CONTROL = "Cache-Control";
	public static final String CONTENT_DISPOSITION = "Content-Disposition";
	public static final String CONTENT_DISPOSITION_FORM_DATA = "Content-Disposition: form-data; name=\"";
	public static final String CONTENT_ENCODING = "Content-Encoding";
	public static final String CONTENT_LANGUAGE = "Content-Language";
	public static final String CONTENT_TYPE = "Content-Type";
	
	public static final String EXPIRES = "Expires";	
	public static final String VERSION_ID = "versionId";
	public static final String PART_NUMBER = "partNumber";
	public static final String RESPONSE_CACHE_CONTROL = "response-cache-control";
	public static final String RESPONSE_CONTENT_DISPOSITION = "response-content-disposition";
	public static final String RESPONSE_CONTENT_ENCODING = "response-content-encoding";
	public static final String RESPONSE_CONTENT_LANGUAGE = "response-content-language";
	public static final String RESPONSE_CONTENT_TYPE = "response-content-type";
	public static final String RESPONSE_EXPIRES = "response-expires";
	public static final String RANGE = "Range";
	public static final String IF_MATCH = "If-Match";
	public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
	public static final String IF_NONE_MATCH = "If-None-Match";
	public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
	public static final String DELIMITER = "delimiter";
	public static final String ENCODING_TYPE = "encoding-type";
	public static final String MARKER = "marker";
	public static final String MAX_KEYS = "max-keys";
	public static final String PREFIX = "prefix";
	public static final String KEY_MARKER = "key-marker";
	public static final String MAX_UPLOADS = "max-uploads";
	public static final String UPLOAD_ID_MARKER = "upload-id-marker";
	
	public static final String CONTINUATION_TOKEN = "continuation-token";
	public static final String FETCH_OWNER = "fetch-owner";
	public static final String START_AFTER = "start-after";
	public static final String VERSION_ID_MARKER = "version-id-marker";
	public static final String MAX_PARTS = "max-parts";
	public static final String PART_NUMBER_MARKER = "part-number-marker";
	public static final String CONTENT_MD5 = "Content-MD5";
	public static final String CONTENT_LENGTH = "Content-Length";
	
	public static final String DELETE_RESULT = "DeleteResult";
	public static final String DELETE_RESULT_DELETED = "Deleted";
	public static final String DELETE_RESULT_KEY = "Key";
	public static final String DELETE_RESULT_DELETE_MARKER_VERSION_ID = "DeleteMarkerVersionId";
	public static final String REPLACE = "REPLACE";

	public static final String JDBC_MYSQL = "jdbc:mysql://";
	public static final String USE_SSL_FALSE = "?useSSL=false";
	public static final String MYSQL_JDBC_DRIVER = "com.mysql.jdbc.Driver";

	public static final String CREATE_DATABASE = "CREATE DATABASE IF NOT EXISTS ";
	public static final String CREATE_TABLE_USERS = 
				"CREATE TABLE IF NOT EXISTS `users` ("
                + "`user_id` int(10) unsigned NOT NULL AUTO_INCREMENT,"
				+ "`user_name` varchar(128) NOT NULL,"
				+ "`access_key` varchar(40) NOT NULL,"
				+ "`access_secret` varchar(40) NOT NULL,"
				+ "PRIMARY KEY (`user_id`)"
				+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
	public static final String CREATE_TABLE_S3LOGGING = 
				"CREATE TABLE IF NOT EXISTS `s3logging` ("
				+ "  `log_id` bigint NOT NULL AUTO_INCREMENT,"
				+ "  `user_name` varchar(64),"
				+ "  `bucket_name` varchar(64),"
				+ "  `date_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
				+ "  `remote_host` varchar(256),"
				+ "  `request_user` varchar(64),"
				+ "  `request_id` varchar(64),"
				+ "  `operation` varchar(64),"
				+ "  `object_name` varbinary(2048),"
				+ "  `request_uri` varbinary(2048),"
				+ "  `status_code` int,"
				+ "  `error_code` varchar(256),"
				+ "  `response_length` bigint,"
				+ "  `object_length` int,"
				+ "  `total_time` bigint,"
				+ "  `request_length` bigint,"
				+ "  `referer` varchar(64),"
				+ "  `user_agent` varchar(256),"
				+ "  `version_id` varchar(64),"
				+ "  `host_id` varchar(256),"
				+ "  `sign` varchar(32),"
				+ "  `ssl_group` varchar(64),"
				+ "  `sign_type` varchar(32),"
				+ "  `endpoint` varchar(64),"
				+ "  `tls_version` varchar(32),"
				+ "  PRIMARY KEY (`log_id`, `user_name`, `bucket_name`, `date_time`, `request_id`)"
				+ "  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;";
	public static final String  INSERT_S3LOGGING = 
		"insert into s3logging " +
		"(user_name, bucket_name,     date_time,     remote_host,  request_user, " +
		" request_id, operation,       object_name,   request_uri,  status_code, " + 
	    " error_code, response_length, object_length, total_time,   request_length, " + 
		" referer,    user_agent,      version_id,    host_id,      sign, " + 
		" ssl_group,  sign_type,       endpoint,      tls_version) " +
		" VALUES (?, ?, now(), ?, ?,    ?, ?, ?, ?, ?,       ?, ?, ?, ?, ?,       ?, ?, ?, ?, ?,      ?, ?, ?, ?);";

	public static final String SELECT_USERS = "select user_id, user_name, access_key, access_secret from dsan.users;";
	public static final String SELECT_USERS_ACCESS_KEY = "select user_id, user_name, access_secret from dsan.users where access_key = ?;";
	public static final String SELECT_USERS_USER_ID = "select user_id, user_name, access_key, access_secret from dsan.users where user_id = ?;";
	public static final String SELECT_USERS_USER_NAME = "select user_id, user_name, access_key, access_secret from dsan.users where user_name = ?;";

	public static final String USERS_TABLE_USER_ID = "user_id";
	public static final String USERS_TABLE_USER_NAME = "user_name";
	public static final String USERS_TABLE_ACCESS_KEY = "access_key";
	public static final String USERS_TABLE_ACCESS_SECRET = "access_secret";

	public static final String RESULT = "result : {}";

	public static final String ACCESS_CONTROL_POLICY = "<AccessControlPolicy>";
	public static final String ACCESS_CONTROL_POLICY_XMLNS = "<AccessControlPolicy xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">";
	public static final String ACCESS_CONTROL_POLICY_ID = "<ID/>";
	public static final String ACCESS_CONTROL_POLICY_DISPLAY_NAME = "<DisplayName/>";
	public static final String ACCESS_CONTROL_POLICY_EMAIL_ADDRESS = "<EmailAddress/>";
	public static final String ACCESS_CONTROL_POLICY_URI = "<URI/>";

	public static final String LIFECYCLE_XML_ID = "<ID/>";
	public static final String LIFECYCLE_XML_DATE = "<Date/>";
	public static final String LIFECYCLE_XML_EXPIRED_OBJECT_DELETE_MARKER = "<ExpiredObjectDeleteMarker/>";
	public static final String LIFECYCLE_XML_NON_CURRENT_VERSION_EXPIRATION = "<NoncurrentVersionExpiration/>";
	public static final String LIFECYCLE_XML_NON_CURRENT_VERSION_TRANSITION = "<NoncurrentVersionTransition/>";
	public static final String LIFECYCLE_XML_ABORT_INCOMPLETE_MULTIPART_UPLOAD = "<AbortIncompleteMultipartUpload/>";
	public static final String LIFECYCLE_XML_PREFIX = "<Prefix/>";
	public static final String LIFECYCLE_XML_TRANSITION = "<Transition/>";
	public static final String LIFECYCLE_XML_FILITER = "<Filter/>";
	public static final String LIFECYCLE_XML_DAYS = "<Days/>";
	public static final String LIFECYCLE_XML_STORAGE_CLASS = "<StorageClass/>";
	public static final String LIFECYCLE_XML_NON_CURRENT_DAYS = "<NoncurrentDays/>";
	public static final String LIFECYCLE_XML_DAYS_AFTER_INITIATION = "<DaysAfterInitiation/>";

	public static final String POLICY_STATUS = "PolicyStatus";
	public static final String POLICY_IS_PUBLIC = "IsPublic";
	public static final String LOCATION_CONSTRAINT = "LocationConstraint";
	public static final String AWS_SOURCE_ARN = "aws:SourceArn";
	public static final String AWS_SOURCE_VPC = "aws:SourceVpc";
	public static final String AWS_SOURCE_VPCE = "aws:SourceVpce";
	public static final String AWS_SOURCE_OWNER = "aws:SourceOwner";
	public static final String AWS_SOURCE_ACCOUNT = "aws:SourceAccount";
	public static final String AWS_SOURCE_IP = "aws:SourceIp";
	public static final String S3_SERVER_SIDE_ENCRYPTION_AWS_KMS_KEY_ID = "s3:x-amz-server-side-encryption-aws-kms-key-id";
	public static final String S3_DATA_ACCESS_POINT_ARN = "s3:DataAccessPointArn";

	public static final String VERSION_CONFIGURATION_XMLNS_DISABLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"/>";
	public static final String XML_SCHEMA = "http://www.w3.org/2001/XMLSchema-instance";
	public static final String VERSION_CONFIGURATION_XMLNS_ENABLED = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"><Status>Enabled</Status></VersioningConfiguration>";
	public static final String VERSION_CONFIGURATION_XMLNS_SUSPENDED = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"><Status>Suspended</Status></VersioningConfiguration>";

	public static final String TAGGING = "Tagging";
	public static final String TAG_SET = "TagSet";
	public static final String TAG_SET_ASSIGN = "tagset=";
	public static final String TAG_ASSIGN = "tags=";
	public static final String XML_TYPE = "type";

	public static final String LIST_BUCKET_RESULT = "ListBucketResult";
	public static final String LIST_VERSIONS_RESULT = "ListVersionsResult";
	public static final String LIST_ALL_MY_BUCKETS_RESULT = "ListAllMyBucketsResult";
	public static final String RETENTION = "Retention";
	public static final String RETAIN_UNTIL_DATE = "RetainUntilDate";
	public static final String COPY_OBJECT_RESULT = "CopyObjectResult";
	public static final String COPY_PART_RESULT = "CopyPartResult";
	public static final String LAST_MODIFIED = "LastModified";
	public static final String LIST_MULTIPART_UPLOADS_RESULT = "ListMultipartUploadsResult";
	public static final String COMPLETE_MULTIPART_UPLOAD_RESULT = "CompleteMultipartUploadResult";
	public static final String APPLY_SERVER_SIDE_ENCRYPTION_BY_DEFAULT = "ApplyServerSideEncryptionByDefault";
	public static final String INITATE_MULTIPART_UPLOAD_RESULT = "InitiateMultipartUploadResult";
	public static final String SSE_ALGORITHM = "SSEAlgorithm";
	public static final String KMS_MASTERKEY_ID = "KMSMasterKeyID";
	public static final String BUCKET_KEY_ENABLED = "BucketKeyEnabled";
	public static final String OBJECT_LOCK_ENABLED = "ObjectLockEnabled";
	public static final String DEFAULT_RETENTION = "DefaultRetention";
	public static final String ACCESS_CONTROL_LIST = "AccessControlList";
	public static final String DELETE_MARKER_REPLICATION = "DeleteMarkerReplication";
	public static final String LIST_PARTS_RESULT = "ListPartsResult";
	public static final String ACCESS_CONTROL_TRANSLATION = "AccessControlTranslation";
	public static final String ENCRYPTION_CONFIGURATION = "EncryptionConfiguration";
	public static final String CORS_RULE = "CORSRule";
	public static final String STORAGE_CLASS = "StorageClass";
	public static final String XML_BUCKETS = "Buckets";
	public static final String XML_BUCKET = "Bucket";
	public static final String XML_CREATION_DATE = "CreationDate";
	public static final String XML_DAYS = "Days";
	public static final String XML_MODE = "Mode";
	public static final String XML_YEARS = "Years";
	public static final String XML_DATE = "Date";
	public static final String XML_EXPIRATION = "Expiration";
	public static final String XML_EXPIRED_OBJECT_DELETE_MARKER = "ExpiredObjectDeleteMarker";
	public static final String XML_NON_CURRENT_VERSION_EXPIRATION = "NoncurrentVersionExpiration";
	public static final String XML_NON_CURRENT_VERSION_TRANSITION = "NoncurrentVersionTransition";
	public static final String XML_ABORT_INCOMPLETE_MULTIPART_UPLOAD = "AbortIncompleteMultipartUpload";
	public static final String XML_DAYS_AFTER_INITIATION = "DaysAfterInitiation";
	public static final String XML_NON_CURRENT_DAYS = "NoncurrentDays";
	public static final String XML_START_AFTER = "StartAfter";
	public static final String XML_CONTINUEATION_TOKEN = "ContinuationToken";
	public static final String XML_NEXT_CONTINUATION_TOKEN = "NextContinuationToken";
	public static final String XML_CONTENTS = "Contents";
	public static final String XML_NEXT_KEY_MARKER = "NextKeyMarker";
	public static final String XML_NEXT_VERSIONID_MARKER = "NextVersionIdMarker";
	public static final String XML_DELETE_MARKER = "DeleteMarker";
	public static final String XML_COMMON_PREFIXES = "CommonPrefixes";
	public static final String LOCATION = "Location";
	public static final String XML_NAME = "Name";
	public static final String XML_PREFIX = "Prefix";
	public static final String XML_ROLE = "Role";
	public static final String XML_RULE = "Rule";
	public static final String XML_OWNER = "Owner";
	public static final String XML_ID = "ID";
	public static final String XML_STATUS = "Status";
	public static final String XML_MFA_DELETE = "MfaDelete";
	public static final String XML_DISPLAY_NAME = "DisplayName";
	public static final String XML_EMAIL_ADDRESS = "EmailAddress";
	public static final String XML_URI = "URI";
	public static final String XML_GRANT = "Grant";
	public static final String XML_GRANTEE = "Grantee";
	public static final String XML_PERMISSION = "Permission";
	public static final String XML_UPLOADID = "UploadId";
	public static final String XML_DESTINATION = "Destination";
	public static final String XML_ACCOUNT = "Account";
	public static final String XML_REPLICA_KMS_KEYID = "ReplicaKmsKeyID";
	public static final String XML_METRICS = "Metrics";
	public static final String XML_EVENT_THRESHOLD = "EventThreshold";
	public static final String XML_MINUTES = "Minutes";
	public static final String XML_REPLICATION_TIME = "ReplicationTime";
	public static final String XML_TIME = "Time";
	public static final String XML_EXISTING_OBJECT_REPLICATION = "ExistingObjectReplication";
	public static final String XML_FILTER = "Filter";
	public static final String XML_AND = "And";
	public static final String XML_TAG = "Tag";
	public static final String XML_KEY = "Key";
	public static final String XML_VALUE = "Value";
	public static final String XML_OBJECT_SIZE_GREATER_THAN = "ObjectSizeGreaterThan";
	public static final String XML_OBJECT_SIZE_LESS_THAN = "ObjectSizeLessThan";
	public static final String XML_ALLOWED_HEADER = "AllowedHeader";
	public static final String XML_ALLOWED_METHOD = "AllowedMethod";
	public static final String XML_ALLOWED_ORIGIN = "AllowedOrigin";
	public static final String XML_EXPOSED_HEADER = "ExposeHeader";
	public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
	public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
	public static final String MAX_AGE_SECONDS = "MaxAgeSeconds";
	public static final String ORIGIN = "Origin";
	public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	public static final String QUIET = "Quiet";
	public static final String OBJECT = "Object";
	public static final String VERSIONID = "VersionId";
	public static final String XML_IS_LATEST = "IsLatest";
	public static final String XML_MAX_KEYS = "MaxKeys";
	public static final String XML_MARKER = "Marker";
	public static final String XML_NEXT_MARKER = "NextMarker";
	public static final String XML_KEY_MARKER = "KeyMarker";
	public static final String XML_VERSIONID_MARKER = "VersionIdMarker";
	public static final String XML_KEY_COUNT = "KeyCount";
	public static final String XML_DELIMITER = "Delimiter";
	public static final String XML_ENCODING_TYPE = "EncodingType";
	public static final String XML_IS_TRUNCATED = "IsTruncated";
	public static final String XML_NEXT_PART_NUMBER = "NextPartNumber";
	public static final String XML_SIZE = "Size";
	public static final String XML_TRUE = "true";
	public static final String XML_FALSE = "false";
	public static final String XML_TRANSITION = "Transition";
	public static final String XML_INITIATOR = "Initiator";
	public static final String VERSION = "Version";
	public static final String POLICY_ID = "Id";
	public static final String STATEMENT = "Statement";
	public static final String SID = "Sid";
	public static final String EFFECT = "Effect";
	public static final String PRINCIPAL = "Principal";
	public static final String ACTION = "Action";
	public static final String RESOURCE = "Resource";
	public static final String CONDITION = "Condition";
	public static final String BLOCK_PUBLIC_ACLS = "BlockPublicAcls";
	public static final String IGNORE_PUBLIC_ACLS = "IgnorePublicAcls";
	public static final String RESTRICT_PUBLIC_BUCKETS = "RestrictPublicBuckets";
	public static final String BLOCK_PUBLIC_POLICY = "BlockPublicPolicy";
	public static final String XML_MAX_UPLOADS = "MaxUploads";
	public static final String XML_UPLOADID_MARKER = "UploadIdMarker";
	public static final String XML_NEXT_UPLOADID_MARKER = "NextUploadIdMarker";
	public static final String XML_UPLOAD = "Upload";
	public static final String XML_INITIATED = "Initiated";
	public static final String XML_BYTES = "bytes=";

	public static final String LEFT_BRACE = "{";
	public static final String RIGHT_BRACE = "}";
	public static final String ACCESS_OW = "\"ow\":";
	public static final String ACCESS_ACS = ",\"acs\":";
	public static final String ACCESS_ID = "\"id\":";
	public static final String ACCESS_COMMA_ID = ",\"id\":";
	public static final String ACCESS_DN = ",\"dN\":";
	public static final String ACCESS_DDN = ",\"ddN\":";
	public static final String ACCESS_GT = "\"gt\":";
	public static final String ACCESS_GTE = "\"gte\":";
	public static final String ACCESS_PERM = ",\"perm\":";
	public static final String ACCESS_TYPE = "\"type\":";
	public static final String ACCESS_EA = "\"eA\":";
	public static final String ACCESS_URI = ",\"uri\":";

	public static final String ENCRYPTION_CUSTOMER_KEY_IS_INVALID = "encryption-customer-key is invalid";
	public static final String ENCRYPTION_CUSTOMER_KEY_IS_NULL = "encryption-customer-key is null";

	// object-lock
	public static final String OBJECT_LOCK_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ObjectLockConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"><ObjectLockEnabled>Enabled</ObjectLockEnabled></ObjectLockConfiguration>";
	// acl
	public static final String ID = "id";
	public static final String URI = "uri";
	public static final String EMAIL_ADDRESS = "emailAddress";
	public static final String AWS_GRANT_URI_ALL_USERS = "http://acs.amazonaws.com/groups/global/AllUsers";
	public static final String AWS_GRANT_URI_AUTHENTICATED_USERS = "http://acs.amazonaws.com/groups/global/AuthenticatedUsers";
	public static final String GRANT_AB_FC = "FC";
	public static final String GRANT_AB_W = "W";
	public static final String GRANT_AB_R = "R";
	public static final String GRANT_AB_RA = "RA";
	public static final String GRANT_AB_WA = "WA";
	public static final String GRANT_AB_CU = "CU";
	public static final String GRANT_AB_G = "G";
	public static final String GRANT_AB_PU = "PU";
	public static final String GRANT_AB_AU = "AU";
	public static final String GRANT_READ = "READ";
	public static final String GRANT_READ_ACP = "READ_ACP";
	public static final String GRANT_WRITE = "WRITE";
	public static final String GRANT_WRITE_ACP = "WRITE_ACP";
	public static final String GRANT_FULL_CONTROL = "FULL_CONTROL";
	public static final String CANONICAL_USER = "CanonicalUser";
	public static final String GROUP = "Group";
	public static final String JSON_AB_OW = "ow";
	public static final String JSON_AB_ACE = "acs";
	public static final String JSON_AB_DN = "dN";
	public static final String JSON_AB_GT = "gt";
	public static final String JSON_AB_GTE = "gte";
	public static final String JSON_AB_PERM = "perm";
	public static final String JSON_AB_TYPE = "type";
	public static final String JSON_AB_DDN = "ddN";
	public static final String JSON_AB_EA = "eA";

	public static final String CANNED_ACLS_PRIVATE = "private";
	public static final String CANNED_ACLS_PUBLIC_READ = "public-read";
	public static final String CANNED_ACLS_PUBLIC_READ_WRITE = "public-read-write";
	public static final String CANNED_ACLS_AUTHENTICATED_READ = "authenticated-read";
	public static final String CANNED_ACLS_BUCKET_OWNER_READ = "bucket-owner-read";
	public static final String CANNED_ACLS_BUCKET_OWNER_FULL_CONTROL = "bucket-owner-full-control";
	public static final String CANNED_ACLS_LOG_DELIVERY_WRITE = "log-delivery-write";

	public static final String WSTXNS = "wstxns[1-9]*";
	public static final String XSI = "xsi";

	public static final String BACKSLASH_D_PLUS = "\\d+";

	public static final String RANGE_CHECK_FORMET = "bytes %d-%d/%d";

	public static final String LOG_ACCESS_DENIED_PUBLIC_ACLS = "access denied : block public acls";
	public static final String LOG_ACCESS_CANNED_ACL = ", cannedAcl:{}";
	public static final String LOG_ACCESS_PROCESS_FAILED = ": x-amz-acl process fail";

	public static final String LOG_COPY_SOURCE_IS_NULL = "x-amz-copy-source is null";
	public static final String LOG_COPY_SOURCE_IS_NOT_IMPLEMENTED ="copy source : {}, arn:aws:s3 is not implemented";
	public static final String LOG_BUCKET_IS_NOT_EXIST = "bucket({}) is not exist.";
	public static final String LOG_SOURCE_INFO = "source key : {}/{}, versionId : {}";
	public static final String LOG_SOURCE_ETAG_MATCH = "source etag : {}, copySourceIfMatch : {}";
	public static final String LOG_ETAG_IS_MISMATCH = "etag is mismatch";
	public static final String LOG_MATCH_BEFORE = "%1$s is before %2$s";
	public static final String LOG_MATCH_AFTER = "%1$s is after %2$s";
	public static final String LOG_ACL = "acl : {}";
	public static final String LOG_BUCKET_IS_NULL = "bucket is null or empty.";
	public static final String LOG_PUT_DELETE_MARKER = "put deleteMarker";
	public static final String LOG_BUCKET_OBJECT = "bucket : {}, object : {}";
	public static final String LOG_OBJECT_META = "objMeta {}";
	public static final String LOG_META = "mata : {}";
	public static final String LOG_TAGGING = "tag : {}";
	public static final String LOG_ACCESS = "access : {}";
	public static final String LOG_UPLOAD_NOT_FOUND = "uploadId({}) is not found";
	public static final String SERVER_SIDE_OPTION = " : server side option";

	public static final String TAGGING_INIT = "0";
	public static final int TAG_KEY_SIZE = 2;
	public static final int TAG_KEY_INDEX = 0;
	public static final int TAG_VALUE_INDEX = 1;
	public static final int TAG_KEY_MAX = 128;
	public static final int TAG_VALUE_MAX = 256;
	public static final int TAG_MAX_SIZE = 10;
	public static final int RANGE_OFFSET_INDEX = 0;
	public static final int RANGE_LENGTH_INDEX = 1;

	public static final String UTILITY_EXCHANGE_KEY = "UtilityExchange";
	public static final String MESSAGE_QUEUE_OPTION = "fanout";

	// GWConfig
	public static final String LOG_CONFIG_NOT_EXIST = "Properties file is not exist";
	public static final String LOG_CONFIG_FAILED_LOADING = "Properties file load is fail";
	public static final String LOG_CONFIG_MUST_CONTAIN = "Properties file must contain: ";

	// GW
	public static final String LOG_GW_MUST_ENDPOINT = "Must provide endpoint or secure-endpoint";
	public static final String LOG_GW_MUST_ENDPOINT_PATH = "endpoint path must be empty, was: %s";
	public static final String LOG_GW_MUST_SECURE_ENDPOINT_PATH = "secure-endpoint path must be empty, was: %s";
	public static final String LOG_GW_MUST_KEYSTORE_PATH = "Must provide keyStorePath with HTTPS endpoint";
	public static final String LOG_GW_MUST_KEYSTORE_PASSWORD = "Must provide keyStorePassword with HTTPS endpoint";
	public static final String LOG_GW_RFC7230 = "RFC7230,MULTIPLE_CONTENT_LENGTHS";
	public static final String LOG_GW_RFC3986 = "RFC3986,-AMBIGUOUS_PATH_SEPARATOR";
	public static final String LOG_GW_ENDPOINT_IS_NULL = "endpoint is null";
	public static final String LOG_GW_SECURE_ENDPOINT_IS_NULL = "secureEndpoint is null";

	// GWHandler
	public static final String LOG_GWHANDLER_MOTHOD_CATEGORY = "method : {}, category : {}";
	public static final String LOG_GWHANDLER_ADMIN_MOTHOD_CATEGORY = "admin method : {}, category : {}";
	public static final String LOG_GWHANDLER_PREURI = "PREURI - {}";
	public static final String LOG_GWHANDLER_URI = "URI - {}";
	public static final String LOG_GWHANDLER_CLIENT_ADDRESS = "client address - {}";
	public static final String LOG_GWHANDLER_CLIENT_HOST = "client host - {}";
	public static final String LOG_GWHANDLER_METHOD = "M - {}";
	public static final String LOG_GWHANDLER_HEADER = "H - {} : {}";
	public static final String LOG_GWHANDLER_PARAMETER = "P - {} : {}";
	public static final String LOG_GWHANDLER_PATH = "path[{}] : {}";

	// S3Request
	public static final String LOG_REQUEST_CHECK_ACL_ID_GRANT = "check acl - id : {}, grant : {}";
	public static final String LOG_REQUEST_BUCKET_ACL = "bucket acl : {}";
	public static final String LOG_REQUEST_BUCKET_OWNER_ID = "bucket owner id : {}";
	public static final String LOG_REQUEST_ROOT_ID = "0";
	public static final String LOG_REQUEST_NOT_FOUND_IN_DB = "{}/{} not found in the DB";
	public static final String LOG_REQUEST_BUCKET_IS_NOT_EMPTY = "The bucket you tried to delete is not empty";
	public static final String LOG_REQUEST_PUBLIC_ACCESS_DENIED = "public is ACCESS_DENIED";
	public static final String LOG_REQUEST_USER_ACCESS_DENIED = "user({}) is ACCESS_DENIED";
	public static final String LOG_REQUEST_GRANT_NOT_DEFINED = "grant{} is not defined.";

	// AbortMultipartUpload
	public static final String LOG_ABORT_MULTIPART_UPLOAD_START = "AbortMultipartUpload ...";
	public static final String LOG_ADMIN_ABORT_MULTIPART_UPLOAD_START = "AdmAbortMultipartUpload ...";

	// CompleteMultipartUpload
	public static final String LOG_COMPLETE_MULTIPART_UPLOAD_START = "CompleteMultipartUpload ...";
	public static final String LOG_COMPLETE_MULTIPART_UPLOAD_PART_NO_EXIST = ": upload part doesn't exist";
	public static final String LOG_COMPLETE_MULTIPART_UPLOAD_XML_PARTS_SIZE = "xml parts size : {}";
	public static final String LOG_COMPLETE_MULTIPART_UPLOAD_PARTS_SIZE = "parts size : {}";
	public static final String LOG_COMPLETE_MULTIPART_UPLOAD_LESS_THAN = "The number of stored listparts is less than the number of CompleteMultipartUploadRequests.";
	public static final String LOG_COMPLETE_MULTIPART_UPLOAD_GREATER_THAN = "The number of stored listparts is greater than the number of CompleteMultipartUploadRequests.";
	public static final String LOG_COMPLETE_MULTIPART_UPLOAD_MD5 = "MD5 : {}";
	public static final String LOG_COMPLETE_MULTIPART_UPLOAD_FAILED = "object insert failed(CompleteMultipartUpload). bucket={}, object={}";
	public static final String LOG_COMPLETE_MULTIPART_UPLOAD_INFO = "pub object : {}/{}, size {}, etag {}, acl {}, versionId {}";
	public static final String LOG_COMPLETE_MULTIPART_VERSION_ID = "versionid : {}";

	// CopyObject
	public static final String LOG_COPY_OBJECT_START = "CopyObject ...";
	public static final String LOG_COPY_OBJECT_SOURCE_CUSTOMER_KEY_NO_MATCH = "source encryption-customer-key does not match";
	public static final String LOG_COPY_OBJECT_SOURCE_CUSTOMER_KEY_IS_NULL = "encryption-customer-key is null";
	public static final String LOG_COPY_OBJECT_METADATA_DIRECTIVE = "metadataDirective : {}";
	public static final String LOG_COPY_OBJECT_USER_METADATA = "user meta data : {}, {}";
	public static final String LOG_COPY_OBJECT_REPLACE_USER_METADATA = "replace user metadata : {}";
	public static final String LOG_COPY_OBJECT_META = "meta : {}";
	public static final String LOG_COPY_OBJECT_FAILED = "object insert failed(CopyObject). bucket={}, object={}";
	public static final String LOG_COPY_OBJECT_INFO = "pub object : {}/{}, size={}, etag={}, tag={}, acl={}, versionId={}";
	public static final String LOG_COPY_OBJECT_ENCRYPTION = "bucket encryption : {}";

	// CreateBucket
	public static final String LOG_CREATE_BUCKET_START = "CreateBucket ...";
	public static final String LOG_CREATE_BUCKET_NAME = "bucket : {}";
	public static final String LOG_CREATE_BUCKET_EXIST = "bucket({}) is already exists";
	public static final String LOG_CREATE_BUCKET_VERSIONING_ENABLED_OBJECT_LOCK_TRUE = "bucket versioning enabled, object-lock is true";
	public static final String LOG_CREATE_BUCKET_INVALID = "bucket name({}) is invalid";

	// CreateMultipartUpload
	public static final String LOG_CREATE_MULTIPART_UPLOAD_START = "CreateMultipartUpload ...";
	public static final String LOG_CREATE_MULTIPART_UPLOAD_FAILED = "object insert failed(CreateMultipartUpload). bucket={}, object={}";

	// DeleteBucket
	public static final String LOG_DELETE_BUCKET_START = "DeleteBucket ...";

	// DeleteBucketCors
	public static final String LOG_DELETE_BUCKET_CORS_START = "DeleteBucketCors ...";

	// DeleteBucketEncryption
	public static final String LOG_DELETE_BUCKET_ENCRYPTION_START = "DeleteBucketEncryption ...";

	// DeleteBucketLifeCycle
	public static final String LOG_DELETE_BUCKET_LIFECYCLE_START = "DeleteBucketLifeCycle ...";

	// DeleteBucketObjectLock
	public static final String LOG_DELETE_BUCKET_OBJECT_LOCK_START = "DeleteBucketObjectLock ...";

	// DeleteBucketPolicy
	public static final String LOG_DELETE_BUCKET_POLICY_START = "DeleteBucketPolicy ...";

	// DeleteBucketReplication
	public static final String LOG_DELETE_BUCKET_REPLICATION_START = "DeleteBucketReplication ...";

	// DeleteBucketTagging
	public static final String LOG_DELETE_BUCKET_TAGGING_START = "DeleteBucketTagging ...";

	// DeleteBucketWebsite
	public static final String LOG_DELETE_BUCKET_WEBSITE_START = "DeleteBucketWebsite ...";

	// DeleteObject
	public static final String LOG_DELETE_OBJECT_START = "DeleteObject ...";
	public static final String LOG_DELETE_OBJECT = "delete : {}/{}";
	public static final String LOG_DELETE_OBJECT_INFO = "versionId : {}, isLastVersion : {}, deleteMarker : {}";
	public static final String LOG_DELETE_OBJECT_BUCKET_VERSIONING = "bucket versioning : {}";
	public static final String LOG_DELETE_OBJECT_BUCKET_VERSIONING_DISABLED = "bucket versioning disabled";
	public static final String LOG_DELETE_OBJECT_BUCKET_VERSIONING_ENABLED = "bucket versioning enabled";
	public static final String LOG_DELETE_OBJECT_BUCKET_VERSIONING_SUSPENDED = "bucket versioning suspended";
	public static final String LOG_DELETE_OBJECT_BUCKET_VERSIONING_WRONG = "bucket versioning is wrong value : {}";
	public static final String LOG_DELETE_OBJECT_DELETE_MARKER_WRONG = "deleteMarker is wrong : {}";
	public static final String LOG_DELETE_OBJECT_FAILED_MARKER = "delete marker insert failed. bucket={}, object={}";

	// DeleteObjects
	public static final String LOG_DELETE_OBJECTS_START ="DeleteObjects ...";
	public static final String LOG_DELETE_OBJECTS_SIZE = "delete objects size : {}";
	public static final String LOG_DELETE_OBJECTS_QUIET_VALUE = "quiet is {}";
	public static final String LOG_DELETE_OBJECTS_ERROR = "Error : Key={}, versionId={}, Code={}, Message={}";

	// DeleteObjectTagging
	public static final String LOG_DELETE_OBJECT_TAGGING_START = "DeleteObjectTagging ...";

	// DeleteBucketPublicAccessBlock
	public static final String LOG_DELETE_BUCKET_PUBLIC_ACCESS_BLOCK_START = "DeleteBucketPublicAccessBlock ...";

	// GetBucketAcl
	public static final String LOG_GET_BUCKET_ACL_START = "GetBucketAcl ...";

	// GetBucketCors
	public static final String LOG_GET_BUCKET_CORS_START = "GetBucketCors ...";
	public static final String LOG_GET_BUCKET_CORS = "cors : {}";

	// GetBucketEncryption
	public static final String LOG_GET_BUCKET_ENCRYPTION_START = "GetBucketEncryption ...";
	public static final String LOG_GET_BUCKET_ENCRYPTION = "encryption : {}";

	// GetBucketLifecycleConfiguration
	public static final String LOG_GET_BUCKET_LIFECYCLE_START = "GetBucketLifecycleConfiguration ...";
	public static final String LOG_GET_BUCKET_LIFECYCLE = "lifecycle : {}";

	// GetBucketLocation
	public static final String LOG_GET_BUCKET_LOCATION_START = "GetBucketLocation ...";

	// GetBucketObjectLock
	public static final String LOG_GET_BUCKET_OBJECT_LOCK_START = "GetBucketObjectLock ...";
	public static final String LOG_OBJECT_LOCK = "object-lock : {}";

	// GetBucketPolicy
	public static final String LOG_GET_BUCKET_POLICY_START = "GetBucketPolicy ...";
	public static final String LOG_GET_BUCKET_POLICY = "policy : {}";

	// GetBucketPolicyStatus
	public static final String LOG_GET_BUCKET_POLICY_STATUS_START = "GetBucketPolicyStatus ...";

	// GetBucketReplication
	public static final String LOG_GET_BUCKET_REPLICATION_START = "GetBucketReplication ...";
	public static final String LOG_GET_BUCKET_REPLICATION = "replication : {}";

	// GetBucketTagging
	public static final String LOG_GET_BUCKET_TAGGING_START = "GetBucketTagging ...";

	// GetBucketVersioning
	public static final String LOG_GET_BUCKET_VERSIONING_START = "GetBucketVersioning ...";
	public static final String LOG_GET_BUCKET_VERSIONING = "bucket({}) versioning : {}";
	public static final String LOG_GET_BUCKET_VERSIONING_WRONG = "not defined versioning status: {}";
	public static final String LOG_GET_BUCKET_VERSIONING_XML = "xml : {}";

	// GetBucketWebsite
	public static final String LOG_GET_BUCKET_WEBSITE_START = "GetBucketWebsite ...";
	public static final String LOG_GET_BUCKET_WEBSITE = "web : {}";

	// GetObjcet
	public static final String LOG_GET_OBJECT_START = "GetObjcet ...";
	public static final String LOG_GET_OBJECT_CUSTOMER_KEY_NO_MATCH = "encryption-customer-key does not match";
	public static final String LOG_GET_OBJECT_IF_MATCH_ETAG = "source etag : {}, IfMatch : {}";
	public static final String LOG_GET_OBJECT_IF_NONE_MATCH_ETAG = "source etag : {}, IfNoneMatch : {}";
	public static final String LOG_GET_OBJECT_ETAG_DIFFERENT = "ETag is different";
	public static final String LOG_GET_OBJECT_ETAG_SAME = "ETag is same";
	public static final String LOG_GET_OBJECT_CONTENT_LENGTH = "CONTENT_LENGTH : {}";
	public static final String LOG_GET_OBJECT_USER_META_DATA = "usermetadata ==> {} : {}";
	public static final String LOG_GET_OBJECT_MODIFIED = "modified : {}";

	// GetObjectAcl
	public static final String LOG_GET_OBJECT_ACL_START = "GetObjectAcl ...";
	
	// GetObjectRetention
	public static final String LOG_GET_OBJECT_RETENTION_START = "GetObjectRetention ...";

	// GetObjectTagging
	public static final String LOG_GET_OBJECT_TAGGING_START = "GetObjectTagging ...";

	// GetPublicAccessBlock
	public static final String LOG_GET_PUBLIC_ACCESS_BLOCK_START = "GetPublicAccessBlock ...";

	// HeadBucket
	public static final String LOG_HEAD_BUCKET_START = "HeadBucket ...";

	// HeadObject
	public static final String LOG_HEAD_OBJECT_START = "HeadObject ...";

	// ListBuckets
	public static final String LOG_LIST_BUCKETS_START = "ListBuckets ...";
	public static final String LOG_LIST_BUCKETS_SIZE = "bucket list size : {}";
	public static final String LOG_LIST_BUCKETS_INFO = "{}, {}";

	// ListMultipartUploads
	public static final String LOG_LIST_MULTIPART_UPLOADS_START = "ListMultipartUploads ...";
	public static final String LOG_LIST_MULTIPART_UPLOADS_KEY = "Key : {}";
	public static final String LOG_LIST_MULTIPART_UPLOADS_UPLOADID = "UploadId : {}";
	public static final String LOG_LIST_MULTIPART_UPLOADS_CHANGE_TIME = "Initiated : {}";

	// ListObject
	public static final String LOG_LIST_OBJECT_START = "ListObject ...";
	public static final String LOG_LIST_OBJECT_PREFIX_ENCODING = "prefix, encoding type : {}, {}";

	// ListObjectV2
	public static final String LOG_LIST_OBJECT_V2_START = "ListObjectV2 ...";

	// ListObjectVersions
	public static final String LOG_LIST_OBJECT_VERSIONS_START = "ListObjectVersions ...";
	public static final String LOG_LIST_OBJECT_VERSIONS_MAXKEYS = "maxKeys = {}";
	public static final String LOG_LIST_OBJECT_VERSIONS_KEY_COUNT = "key count : {}";
	public static final String LOG_LIST_OBJECT_VERSIONS_INFO = "object : {}, lastModified : {}, versionId : {}";

	// ListParts
	public static final String LOG_LIST_PARTS_START = "ListParts ...";
	public static final int MAX_PART_VALUE = 1000;
	public static final String LOG_LIST_PARTS_PART_NUMBER = "PartNumber : {}";
	public static final String LOG_LIST_PARTS_LAST_MODIFIED = "LastModified : {}";
	public static final String LOG_LIST_PARTS_ETAG = "ETag : {}";
	public static final String LOG_LIST_PARTS_SIZE = "Size : {}";

	// OptionsObject
	public static final String LOG_OPTIONS_OBJECT_START = "OptionsObject ...";

	// PutBucketAcl
	public static final String LOG_PUT_BUCKET_ACL_START = "PutBucketAcl ...";

	// PutBucketCors
	public static final String LOG_PUT_BUCKET_CORS_START = "PutBucketCors ...";

	// PutBucketEncryption
	public static final String LOG_PUT_BUCKET_ENCRYPTION_START = "PutBucketEncryption ...";

	// PutBucketLifecycleConfiguration
	public static final String LOG_PUT_BUCKET_LIFECYCLE_START = "PutBucketLifecycleConfiguration ...";
	public static final String LOG_PUT_BUCKET_LIFECYCLE_XML = "lifecycle : {}";

	// PutBucketObjectLock
	public static final String LOG_PUT_BUCKET_OBJECT_LOCK_START = "PutObjectLockConfiguration ...";
	public static final String LOG_PUT_BUCKET_OBJECT_LOCK = "ObjectLock : {}";

	// PutBucketPolicy
	public static final String LOG_PUT_BUCKET_POLICY_START = "PutBucketPolicy ...";

	// PutBucketReplication
	public static final String LOG_PUT_BUCKET_REPLICATION_START = "PutBucketReplication ...";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULES = "{}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_ID = "rule.id : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_PREFIX = "rule.prefix : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_PRIORITY = "rule.priority : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_EXISTING_OBJECT_REPLICATION = "rule.existingObjectReplication : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_EXISTING_OBJECT_REPLICATION_STATUS = "rule.existingObjectReplication.status : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_FILTER = "rule.filter : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_FILTER_AND = "rule.filter.and : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_FILTER_AND_PREFIX = "rule.filter.and.prefix : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_FILTER_AND_TAG = "rule.filter.and.tag : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_FILTER_AND_TAG_KEY = "rule.filter.and.tag.key : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_FILTER_AND_TAG_VALUE = "rule.filter.and.tag.value : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_SOURCE_SELECTION_CRITERIA = "rule.sourceSelectionCriteria : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_SOURCE_SELECTION_CRITERIA_REPLICA_MODIFICATIONS = "rule.sourceSelectionCriteria.replicaModifications : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_SOURCE_SELECTION_CRITERIA_REPLICA_MODIFICATIONS_STATUS = "rule.sourceSelectionCriteria.replicaModifications.status : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_SOURCE_SELECTION_CRITERIA_SSE_KMS_ENCRYPTED_OBJECTS = "rule.sourceSelectionCriteria.sseKmsEncryptedObjects : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_SOURCE_SELECTION_CRITERIA_SSE_KMS_ENCRYPTED_OBJECTS_STATUS = "rule.sourceSelectionCriteria.sseKmsEncryptedObjects.status : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_DELETE_MARKER_REPLICATION = "rule.deleteMarkerReplication {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_DELETE_MARKER_REPLICATION_STATUS = "rule.deleteMarkerReplication.Status {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_BUCKET = "rule.destination.bucket : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_ACCESS_CONTROL_TRANSLATION = "rule.destination.accessControlTranslation : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_ACCESS_CONTROL_TRANSLATION_OWNER = "rule.destination.accessControlTranslation.owner : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_ENCRYPTION_CONFIGURATION = "rule.destination.encryptionConfiguration : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_ENCRYPTION_CONFIGURATION_REPLICAT_KMS_KEY_ID = "rule.destination.encryptionConfiguration.replicaKmsKeyID : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_METRICS = "rule.destination.metrics : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_METRICS_EVENT_THRESHOLD = "rule.destination.metrics.eventThreshold : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_METRICS_EVENT_THRESHOLD_MINUTES = "rule.destination.metrics.eventThreshold.minutes : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_METRICS_STATUS = "rule.destination.metrics.status : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_REPLICATION_TIME = "rule.destination.replicationTime : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_REPLICATION_TIME_STATUS = "rule.destination.replicationTime.status : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_REPLICATION_TIME_TIME = "rule.destination.replicationTime.time : {}";
	public static final String LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_STORAGE_CLASS = "rule.destination.storageClass : {}";

	// PutBucketTagging
	public static final String LOG_PUT_BUCKET_TAGGING_START = "PutBucketTagging ...";
	public static final String LOG_PUT_BUCKET_TAGGING = "tagging xml : {}";

	// PutBucketVersioning
	public static final String LOG_PUT_BUCKET_VERSIONING_START = "PutBucketVersioning ...";

	// PutBucketWebsite
	public static final String LOG_PUT_BUCKET_WEBSITE_START = "PutBucketWebsite ...";

	// PutObject
	public static final String LOG_PUT_OBJECT_START = "PutObject ...";
	public static final String LOG_PUT_OBJECT_HASHCODE_ILLEGAL = "HashCode Illegal";
	public static final String LOG_PUT_OBJECT_TAGGING_KEY_LENGTH = "key length : {}";
	public static final String LOG_PUT_OBJECT_TAGGING_VALUE_LENGTH = "key value length : {}";
	public static final String LOG_PUT_OBJECT_TAGGING_SIZE = "tag size : {}";
	public static final String LOG_PUT_OBJECT_LOCK_STATUS = "object lock status : {}";
	public static final String LOG_PUT_OBJECT_LOCK_MODE = "Lock mode : {}";
	public static final String LOG_PUT_OBJECT_PRIMARY_DISK_ID = "obj prmary disk id : {}";
	public static final String LOG_PUT_OBJECT_FAILED = "object insert failed(pubObject). bucket={}, object={}";
	public static final String LOG_PUT_OBJECT_INFO = "pub object : {}/{}, size {}, etag {}, acl {}, versionId {}";
	public static final String LOG_PUT_OBJECT_VERSIONID = "versionId : {}";
	
	// PutObjectAcl
	public static final String LOG_PUT_OBJECT_ACL_START = "PutObjectAcl ...";

	// PutObjectRetention
	public static final String LOG_PUT_OBJECT_RETENTION_START = "PutObjectRetention ...";

	// PutObjectTagging
	public static final String LOG_PUT_OBJECT_TAGGING_START = "PutObjectTagging ...";

	// PutBucketPublicAccessBlock
	public static final String LOG_PUT_BUCKET_PUBLIC_ACCESS_BLOCK_START = "PutBucketPublicAccessBlock ...";

	// UploadPart
	public static final String LOG_UPLOAD_PART_START = "UploadPart ...";
	public static final String LOG_UPLOAD_PART_WRONG_PART_NUMBER = " : Part number must be an integer between 1 and 10000, inclusive";
	public static final int MAX_PARTS_SIZE = 10000;
	public static final String ARGMENT_NAME = "ArgumentName";
	public static final String ARGMENT_VALUE = "ArgumentValue";
	public static final String LENGTH_REQUIRED = "Length Required";
	public static final String LOG_CANNOT_FIND_LOCAL_PATH = "Can not find local path";

	// UploadPartCopy
	public static final String LOG_UPLOAD_PART_COPY_START = "UploadPartCopy ...";
	public static final String LOG_UPLOAD_PART_COPY_SOURCE = "copySource : {}";
	public static final String LOG_UPLOAD_PART_COPY_SOURCE_RANGE = "copy source range : {}, file size : {}";

	// PostObject
	public static final String LOG_POST_OBJECT_START = "PostObject ...";

	// GWUtils
	public static final String LOG_UTILS_INIT_CACHE = "init disk for cache and dir ...";
	public static final String LOG_UTILS_INIT_DIR = "init disk for dir ...";
	public static final String PROPERTY_COMMONS_CRYPTO_STREAM_BUFFER_SIZE = "commons.crypto.stream.buffer.size";
	public static final long COMMONS_CRYPTO_STREAM_BUFFER_SIZE = MAXBUFSIZE;
	public static final String LOG_UTILS_USER_META_DATA = "user metadata ==> {} : {}";
	public static final String LOG_UTILS_8061_DATE = "8601date : {}";
	public static final String LOG_UTILS_MAX_TIME_SKEW = "maxtimeskew : {}";
	public static final String LOG_UTILS_TIME_SKEWED = "time skewed : {}, now : {}";
	public static final String LOG_UTILS_UNDEFINED_DB = "undefined db repository.";
	public static final String LOG_UTILS_KEY = "key : {}";
	public static final String LOG_UTILS_SOURCE_ACL = "source acl : {}";
	public static final String LOG_UTILS_CANNED_ACL = "cannedAcl : {}";
	public static final String LOG_UTILS_ACL_XML = "aclXml : {}";

	// Data
	public static final String LOG_DATA_UPLOAD_ID_NULL = "uploadid is null or empty";
	public static final String LOG_DATA_PART_NUMBER_NULL = "partNumber is null or empty";
	public static final String LOG_DATA_HEADER = "headerName : {}, value : {}";
	public static final String LOG_DATA_VERSION_ID_NULL = "versionId is null or empty";
	public static final String LOG_DATA_RESPONSE_CACHE_CONTROL_NULL = "responseCacheControl is null or empty";
	public static final String LOG_DATA_RESPONSE_CONTENT_DISPOSITION_NULL = "responseContentDisposition is null or empty";
	public static final String LOG_DATA_RESPONSE_CONTENT_ENCODING_NULL = "responseContentEncoding is null or empty";
	public static final String LOG_DATA_RESPONSE_CONTENT_LANGUAGE_NULL = "responseContentLanguage is null or empty";
	public static final String LOG_DATA_RESPONSE_CONTENT_TYPE_NULL = "responseContentType is null or empty";
	public static final String LOG_DATA_RESPONSE_EXPIRES_NULL = "responseExpires is null or empty";
	public static final String LOG_DATA_DELIMITER_NULL = "delimiter is null or empty";
	public static final String LOG_DATA_ENCODING_TYPE_NULL = "encodingType is null or empty";
	public static final String LOG_DATA_KEY_MARKER_NULL = "keyMarker is null or empty";
	public static final String LOG_DATA_MAX_KEYS_NULL = "maxKeys is null or empty";
	public static final String LOG_DATA_PREFIX_NULL = "prefix is null or empty";
	public static final String LOG_DATA_FETCH_OWNER_NULL = "fetchOwner is null or empty";
	public static final String LOG_DATA_START_AFTER_NULL = "startAfter is null or empty";
	public static final String LOG_DATA_MARKER_NULL = "marker is null or empty";
	public static final String LOG_DATA_MAX_UPLOADS_NULL = "maxUploads is null or empty";
	public static final String LOG_DATA_UPLOAD_ID_MARKER_NULL = "uploadIdMarker is null or empty";
	public static final String LOG_DATA_CONTINUATION_TOKEN_NULL = "continuationToken is null or empty";
	public static final String LOG_DATA_VERSION_ID_MARKER_NULL = "versionIdMarker is null or empty";
	public static final String LOG_DATA_MAX_PARTS_NULL = "maxParts is null or empty";
	public static final String LOG_DATA_PART_NUMBER_MARKER_NULL = "partNumberMarker is null or empty";
	public static final String LOG_DATA_LIFECYCLE_R1_STATUS = "rl.status : {}";
	public static final String LOG_DATA_LIFECYCLE_LCC_RULE_SIZE = "lcc.rules.size : {}, id.size : {}";

	// ObjManagerHelper
	public static final String LOG_OBJMANAGER_COUNT = "objManager count : {}";

	// OSDClient
	public static final String LOG_OSDCLIENT_SOCKET_INFO = "socket : {}";
	public static final String LOG_OSDCLIENT_CLOSE_SOCKET_INFO = "close socket : {}";
	public static final String LOG_OSDCLIENT_SOCKET_ERROR = "socket error, {}";
	public static final String LOG_OSDCLIENT_HEADER = "get header : {}";
	public static final String LOG_OSDCLIENT_READ = "read {} bytes";
	public static final String LOG_OSDCLIENT_WRITE = "write {} bytes";
	public static final String LOG_OSDCLIENT_PUT_HEADER = "put header : {}";
	public static final String LOG_OSDCLIENT_DELETE_HEADER = "delete header : {}";
	public static final String LOG_OSDCLIENT_COPY_HEADER = "copy header : {}";
	public static final String LOG_OSDCLIENT_PART_HEADER = "part header : {}";
	public static final String LOG_OSDCLIENT_DELETE_PART_HEADER = "delete part header : {}";
	public static final String LOG_OSDCLIENT_PART_COPY_HEADER = "partCopy header : {}";
	public static final String LOG_OSDCLIENT_COMPLETE_MULTIPART_HEADER = "completeMultipart header : {}";
	public static final String LOG_OSDCLIENT_ABORT_MULTIPART_HEADER = "abortMultipart header : {}";
	public static final String LOG_OSDCLIENT_UNKNOWN_RESULT = "Unknown result : {}";
	public static final String LOG_OSDCLIENT_ACTIVATE_SOCKET = "activate socket...";
	public static final String LOG_OSDCLIENT_DESACTIVATE_SOCKET = "desactivate socket...";

	// OSDClientManager
	public static final String LOG_OSDCLIENT_MANAGER_DISKPOOL_INFO = "disk pool id : {}, name = {}";
	public static final String LOG_OSDCLIENT_MANAGER_SERVER_SIZE = "server size : {}";
	public static final String LOG_OSDCLIENT_MANAGER_CLIENT_COUNT = "client count : {}";
	public static final String LOG_OSDCLIENT_MANAGER_OSD_SERVER_IP = "add osd server ip : {}";

	// S3ObjectOperation
	public static final String FILE_ATTRIBUTE_REPLICATION = "replication";
	public static final String FILE_ATTRIBUTE_REPLICA_DISK_ID = "replica-diskid";
	public static final String FILE_ATTRUBUTE_REPLICATION_PRIMARY = "primary";
	public static final String FILE_ATTRIBUTE_REPLICATION_REPLICA = "replica";
	public static final String FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL = "null";
	public static final String LOG_S3OBJECT_OPERATION_FILE_SIZE = "get obeject file size : {}";
	public static final String LOG_S3OBJECT_OPERATION_RANGE = "offset : {}, length : {}";
	public static final String LOG_S3OBJECT_OPERATION_OBJECT_PRIMARY_INFO = "obj primary : {}";
	public static final String LOG_S3OBJECT_OPERATION_ETAG_AND_VERSION_ID = "etag : {}, versionId : {}";
	public static final String LOG_S3OBJECT_OPERATION_PRIMARY_IP = "localIP : {}, primary disk server ip : {}";
	public static final String LOG_S3OBJECT_OPERATION_REPLICA_IP = "localIP : {}, replica disk server ip : {}";
	public static final String LOG_S3OBJECT_OPERATION_DELETE = "delete - success : bucket={}, objKey={}, versionId={}";
	public static final String LOG_S3OBJECT_OPERATION_OSD_ERROR = "Can't get osd data...";
	public static final String LOG_S3OBJECT_OPERATION_FAILED_FILE_DELETE = "failed file delete {}";
	public static final String LOG_S3OBJECT_OPERATION_FAILED_FILE_RENAME = "failed file rename s : {}, d : {}";
	public static final String LOG_S3OBJECT_OPERATION_OBJ_PATH = "obj path : {}";
	public static final String LOG_S3OBJECT_OPERATION_TEMP_PATH = "temp path : {}";
	public static final String LOG_S3OBJECT_OPERATION_TRASH_PATH = "trash path : {}";
	public static final String LOG_S3OBJECT_OPERATION_EC_PATH = "ec path : {}";
	public static final String LOG_S3OBJECT_OPERATION_LOCAL_IP = "local ip : {}";
	public static final String LOG_S3OBJECT_OPERATION_OBJ_PRIMARY_IP = "objMeta primary ip : {}";
	public static final String LOG_S3OBJECT_OPERATION_OBJ_REPLICA_IP = "objMeta replica ip : {}";
	public static final String LOG_S3OBJECT_OPERATION_COPY_SOURCE_RANGE = "copySourceRange : {}";
	public static final String LOG_S3OBJECT_OPERATION_DISK_IP_NULL = "diskid : {} -> ip is null. check disk pool";
	public static final String LOG_S3OBJECT_OPERATION_DISK_PATH_NULL = "diskid : {} -> path is null. check disk pool";
	public static final String ZUNFEC = "zunfec -o ";
	public static final String ZFEC_0 = ".0_4.fec";
	public static final String ZFEC_1 = ".1_4.fec";
	public static final String ZFEC_2 = ".2_4.fec";
	public static final String ZFEC_3 = ".3_4.fec";
	public static final String LOG_S3OBJECT_OPERATION_ZUNFEC_COMMAND = "command : {}";
	public static final String LOG_S3OBJECT_OPERATION_ZUNFEC_DECODE = "DECODE EC : {}";
	public static final String LOG_S3OBJECT_OPERATION_ZUNFEC_DECODE_EXIT_VALUE = "DECODE exit : {}";
	public static final String LOG_S3OBJECT_OPERATION_OPTION_NO_CASE = "option({}) is not supported.";

	// S3Range
	public static final String LOG_S3RANGE_VALUE = "range : {}";
	public static final String LOG_S3RANGE_EMPTY = "Range is empty";
	public static final String LOG_S3RANGE_INVALID = "Range({})_is not valid";
	public static final String LOG_S3RANGE_NOT_SATISFIABLE = "The requested range is not satisfiable : {}";
	public static final String LOG_S3RANGE_ENDPOSITION_GREATER_THAN = "endPosition is greater than file length";
	public static final String LOG_S3RANGE_INFO = "range : {}-{}, length : {}, file size : {}";

	// S3ServerSideEncryption
	public static final String LOG_S3SERVER_SIDE_ENCRYPTION_CALC_KEY = " encryption calc key : ";
	public static final String LOG_S3SERVER_SIDE_ENCRYPTION_SOURCE_KEY = ", encryption source key : ";

	// ChunkedInputStream
	public static final String LOG_CHUNKED_INPUT_STREAM_URF = "URF_UNREAD_FIELD";
	public static final String LOG_CHUNKED_JUSTIFICATION = "https://github.com/gaul/s3proxy/issues/205";
	public static final String LOG_CHUNKED_UNEXPECTED_CHAR_AFTER = "unexpected char after \\r: ";
	public static final String LOG_CHUNKED_UNEXPECTED_END = "unexpected end of stream";

	// S3Signature
	public static final String LOG_S3SIGNATURE_EXPIRES = "expires : {}";
	public static final String LOG_S3SIGNATURE_DATE = "No Date Header : {}";
	public static final String LOG_S3SIGNATURE_SIGN = "stringToSign: {}";

	// S3Signing
	public static final String LOG_S3SIGNING_HAVE_DATE = "have empty x-amz-date";
	public static final String LOG_S3SIGNING_UNSUPPORT_ENCODING_LANGUAGE = "unsupport encoding language";
	public static final String LOG_S3SIGNING_ENHANCE_PATH = "enhance path[{}]={}";
	public static final String LOG_S3SIGNING_SIGNATURE_OR_AUTH_HEADER_NULL = "Signature or auth header null : {}";
	public static final String LOG_S3SIGNING_AWS_REQUIRES_VALID_DATE = "AWS authentication requires a valid Date or x-amz-date header";
	public static final String LOG_S3SIGNING_V2_SIGNATURE_NULL = "V2 identity or signature null : {}";
	public static final String LOG_S3SIGNING_V4_SIGNATURE_NULL = "V4 identity or signature null : {}";
	public static final String LOG_S3SIGNING_V4_CREDENTIAL_NULL = "V4 credential or signature or signedHeaders null : {}";
	public static final String LOG_S3SIGNING_UNKNOWN_ALGORITHM_VALUE = "unknown algorithm : {}";
	public static final String LOG_S3SIGNING_UNKNOWN_ALGORITHM = "unknown algorithm : ";
	public static final String LOG_S3SIGNING_AUTH_HEADER = "S3AuthorizationHeader {}";
	public static final String LOG_S3SIGNING_ACCESS_NULL = "access key is null";
	public static final String LOG_S3SIGNING_USER_NULL = "User is null (Invalid access key)";
	public static final String LOG_S3SIGNING_USER = "user : {}";
	public static final String LOG_S3SIGNING_AUTHENTICATION_NULL = "Authentication Type is None : {}";
	public static final String LOG_S3SIGNING_INTO_V2 = "into process v2 {}";
	public static final String LOG_S3SIGNING_INTO_V4 = "into process v4 {}";
	public static final String LOG_S3SIGNING_DATE = "X-Amz-Date : {}";
	public static final String LOG_S3SIGNING_DATE_HEADER = "dateheader : {}";
	public static final String LOG_S3SIGNING_ILLEGAL_DATE_SKEW = "Illegal Argument dateSkew : {}";
	public static final String LOG_S3SIGNING_EXPIRES = "expiresString {}";
	public static final String LOG_S3SIGNING_HAS_EXPIRED = "Request has expired";
	public static final String LOG_S3SIGNING_URI = "URI - {}";
	public static final String LOG_S3SIGNING_PATH_LENGTH = "path.length({})";
	public static final String LOG_S3SIGNING_FAILED_VALIDATE_EXPECT_AND_AUTH_HEADER = "fail to validate signature expect({}), authheader({})";
	public static final String LOG_S3SIGNING_MATCH_TIME = "match time now({}), expire({})";
	public static final String LOG_S3SIGNING_TIME_EXPIRED = "time expired {} {}";
	public static final String LOG_S3SIGNING_HMACSHA256 = "HmacSHA256";

	// S3AuthorizationHeader
	public static final String S3AUTH_HEADER_IDENTITY = "Identity: ";
	public static final String S3AUTH_HEADER_SIGNATURE = "; Signature: ";
	public static final String S3AUTH_HEADER_HMAC_ALGORITHM = "; HMAC algorithm: ";
	public static final String S3AUTH_HEADER_HASH_ALGORITHM = "; Hash algorithm: ";
	public static final String S3AUTH_HEADER_REGION = "; region: ";
	public static final String S3AUTH_HEADER_DATE = "; date: ";
	public static final String S3AUTH_HEADER_SERVICE = "; service ";

	// MariaDB
	public static final String LOG_MARIA_DB_FAIL_TO_LOAD_DRIVER = "fail to load JDBC Driver";

	// S3ObjectEncryption
	public static final String S3OBJECT_ENCRYPTION_ENABLE_SSE_SERVER = "enableSSEServer : {}";
	public static final String S3OBJECT_ENCRYPTION_ENABLE_SSE_CUSTOMER = "enableSSECustomer : {}";

	// GWPortal
	public static final String GWPORTAL_RECEIVED_CONFIG_CHANGE = "receive config change ...";
	public static final String GWPORTAL_RECEIVED_DISK_CHANGE = "receive disk change ...";
	public static final String GWPORTAL_RECEIVED_DISKPOOLS_CHANGE = "receive diskpools change ...";
	public static final String GWPORTAL_RECEIVED_USER_CHANGE = "receive s3user change ...";
	public static final String GWPORTAL_RECEIVED_SERVICE_CHANGE = "receive service change ...";
	public static final String LOG_GWPORTAL_RECEIVED_MESSAGE_QUEUE_DATA = "BiningKey : {}, body : {}";

	public static final String GWPORTAL_RECEIVED_USER_ADDED = "added";
	public static final String GWPORTAL_RECEIVED_USER_UPDATED = "updated";
	public static final String GWPORTAL_RECEIVED_USER_REMOVED = "removed";
	public static final String LOG_GWPORTAL_RECEIVED_USER_WRONG_ROUTING_KEY = "wrong routingKey : {}";
	public static final String LOG_GWPORTAL_RECEIVED_USER_DATA = "Id:{}, Name:{}, Email:{}, AccessKey:{}, SecretKey:{}";
}
