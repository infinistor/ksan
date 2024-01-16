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

import java.io.File;

public final class AzuConstants {
    public static final int AZU_PORT = 10000;
    public static final int AZU_TIME_OUT = 60000;
    public static final String LOG_PRE_URI = "PREURI - {}";
    public static final String LOG_URI = "URI - {}";
    public static final String LOG_CLIENT_ADDRESS = "client address - {}";
    public static final String LOG_CLIENT_HOST = "client host - {}";
    public static final String LOG_METHOD = "method - {}";
    public static final String LOG_PARAMETER = "parameter - {}:{}";
    public static final String LOG_HEADER = "header - {}:{}";
    public static final String LOG_PATH = "path[{}] : {}";
    public static final String REQUEST_LOGS = "$logs";
    public static final String REQUEST_BLOCK_CHANGE_FEED = "$blobchangefeed";
    public static final String LOG_LOGS_BLOB_CHANGE_FEED = "request logs or blob change feed. container : {}, send SC_NOT_FOUND";

    public static final String EMPTY_STRING = "";
    public static final String CONFIG_FILE = "/usr/local/ksan/etc/azu.conf";
    public static final String SHARED_KEY_AUTH_PATH = "/usr/local/ksan/etc/sharedKeys.info";
    
    public static final String NEWLINE = "\n";
    public static final String TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
    public static final String REQUEST_ID = "RequestId";
    public static final String TIME = "Time";

    public static final String CANONICAL_HEADER_START_WITH = "x-ms-";
    public static final String X_MS_CLIENT_REQUEST_ID = "x-ms-client-request-id";
    public static final String X_MS_RANGE = "x-ms-range";
    public static final String X_MS_DATE = "x-ms-date";
	public static final String X_MS_VERSION = "x-ms-version";
    public static final String X_MS_BLOB_CONTENT_TYPE = "X-Ms-Blob-Content-Type";
    public static final String X_MS_BLOB_CONTENT_MD5 = "X-Ms-Blob-Content-Md5";
    public static final String X_MS_BLOB_TYPE = "X-Ms-Blob-Type";
    public static final String X_MS_BLOB_CONTENT_ENCODING = "X-Ms-Blob-Content-Encoding";
    public static final String X_MS_BLOB_CONTENT_LANGUAGE = "X-Ms-Blob-Content-Language";
    public static final String X_MS_BLOB_CONTENT_DISPOSITION = "X-Ms-Blob-Content-Disposition";

    public static final String PARAMETER_COMP = "comp";
    public static final String PARAMETER_RESTYPE = "restype";
    public static final String PARAMETER_DELIMITER = "delimiter";
    public static final String PARAMETER_MARKER = "marker";
    public static final String PARAMETER_PREFIX = "prefix";
    public static final String PARAMETER_INCLUDE = "include";
    public static final String PARAMETER_MAX_RESULTS = "maxresults";
    public static final String PARAMETER_TIMEOUT = "timeout";

    public static final String HEADER_LASTMODIFIED = "last-modified";
    public static final String HEADER_X_MS_CREATION_TIME = "x-ms-creation-time";
    public static final String HEADER_X_MS_BLOB_TYPE = "x-ms-blob-type";
    public static final String HEADER_X_MS_LEASE_STATE = "x-ms-lease-state";
    public static final String HEADER_X_MS_LEASE_STATUS = "x-ms-lease-status";
    public static final String HEADER_ETAG = "etag";
    public static final String HEADER_CONTENT_MD5 = "content-md5";
    public static final String HEADER_CONTENT_LENGTH = "content-length";
    public static final String HEADER_CONTENT_RANGE = "content-range";

    public static final String BLOB_TYPE_BLOCKBLOB = "BlockBlob";
    public static final String LEASE_STATE_AVAILABLE = "available";
    public static final String LEASE_STATUS_UNLOCKED = "unlocked";
    public static final String ETAG_DEFAULT = "0x1DB589B70D48620";

    public static final String HTTP_METHOD_GET = "GET";
    public static final String HTTP_METHOD_DELETE = "DELETE";
    public static final String HTTP_METHOD_HEAD = "HEAD";
    public static final String HTTP_METHOD_PUT = "PUT";

    public static final String RESTYPE_SERVICE = "service";
    public static final String RESTYPE_CONTAINER = "container";
    public static final String RESTYPE_ACCOUNT = "account";

    public static final String PATH_CATEGORY_ROOT = "root";
    public static final String PATH_CATEGORY_CONTAINER = "container";
    public static final String PATH_CATEGORY_BLOB = "blob";

    public static final String COMP_BLOCK = "block";
    public static final String COMP_BLOCKLIST = "blocklist";
    public static final String COMP_PROPERTIES = "properties";

    public static final String CONTAINER_DIRECTORY = "__container__";
    public static final String BLOB_INFO_DIRECTORY = "__info__";
    public static final String BLOB_BLOCK_DIRECTORY = "__block__";

    public static final String MD5 = "MD5";
    public static final int MAXBUFSIZE = 524288;
    public static final int BUFSIZE = 262144;

    public static final String TIMEZONE_GMT = "GMT";
    public static final String TIME_GMT_FORMAT = "E, dd MMM yyyy HH:mm:ss z";
    public static final String LOCALE_EN = "en";
    public static final String CHARSET_UTF_8 = "UTF-8";

    public static final String COLON = ":";
    public static final String SEPARATOR = "/";
    public static final char SEPARATOR_CHAR = '/';

    public static final String NULL = "null";

    public static final String CONTENT_TYPE_XML = "application/xml";
    public static final String CONTENT_TYPE_TEXT = "text/plain";
    public static final String CONTENT_TYPE_APPLICATION = "application/octet-stream";

    // BlockList
    public static final String BLOCK_LIST = "Blocklist";
    public static final String BLOCK_LATEST = "Latest";
    
    // XML Element Name
    public static final String XML_ELEMENT_LOGGING = "Logging";
    public static final String XML_ELEMENT_VERSION = "Version";
    public static final String XML_ELEMENT_DELETE = "Delete";
    public static final String XML_ELEMENT_READ = "Read";
    public static final String XML_ELEMENT_WRITE = "Write";
    public static final String XML_ELEMENT_RETENTION_POLICY = "RetentionPolicy";
    public static final String XML_ELEMENT_ENABLED = "Enabled";
    public static final String XML_ELEMENT_HOUR_METRICS = "HourMetrics";
    public static final String XML_ELEMENT_MINUTE_METRICS = "MinuteMetrics";
    public static final String XML_ELEMENT_CORS = "Cors";
    public static final String XML_ELEMENT_DEFAULT_SERVICE_VERSION = "DefaultServiceVersion";
    public static final String XML_ELEMENT_STATIC_WEBSITE = "StaticWebsite";

    public static final String XML_ELEMENT_ENUMERATION_RESULT = "EnumerationResults";
    public static final String XML_ATTRIBUTE_SERVICE_ENDPOINT = "ServiceEndpoint";
    public static final String XML_ATTRIBUTE_SERVICE_ENDPOINT_VALUE = "http://127.0.0.1:10000/";
    public static final String XML_ATTRIBUTE_CONTAINER_NAME = "ContainerName";
    public static final String XML_ELEMENT_PREFIX = "Prefix";
    public static final String XML_ELEMENT_MARKER = "Marker";
    public static final String XML_ELEMENT_DELIMITER = "Delimiter";
    public static final String XML_ELEMENT_MAX_RESULT = "MaxResults";
    public static final String XML_ELEMENT_LISTBLOB_MAX_RESULT_VALUE = "1000";
    public static final String XML_ELEMENT_LISTCONTAINER_MAX_RESULT_VALUE = "5000";
    public static final String XML_ELEMENT_CONTAINERS = "Containers";
    public static final String XML_ELEMENT_CONTAINER = "Container";
    public static final String XML_ELEMENT_BLOBS = "Blobs";
    public static final String XML_ELEMENT_BLOB = "Blob";
    public static final String XML_ELEMENT_NAME = "Name";
    public static final String XML_ELEMENT_PROPERTIES = "Properties";
    public static final String XML_ELEMENT_LAST_MODIFIED = "Last-Modified";
    public static final String XML_ELEMENT_ETAG = "Etag";
    public static final String XML_ELEMENT_LEASE_STATUS = "LeaseStatus";
    public static final String XML_ELEMENT_LEASE_STATE = "LeaseState";
    public static final String XML_ELEMENT_HAS_IMMUTABILITY_POLICY = "HasImmutabilityPolicy";
    public static final String XML_ELEMENT_HAS_LEGALHOLD = "HasLegalHold";
    public static final String XML_ELEMENT_NEXT_MARKER = "NextMarker";
    public static final String XML_ELEMENT_CREATION_TIME = "Creation-Time";
    public static final String XML_ELEMENT_CONTENT_LENGTH = "Content-Length";
    public static final String XML_ELEMENT_CONTENT_TYPE = "Content-Type";
    public static final String XML_ELEMENT_CONTENT_ENCODING = "Content-Encoding";
    public static final String XML_ELEMENT_CONTENT_LANGUAGE = "Content-Language";
    public static final String XML_ELEMENT_CONTENT_MD5 = "Content-MD5";
    public static final String XML_ELEMENT_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String XML_ELEMENT_CACHE_CONTROL = "Cache-Control";
    public static final String XML_ELEMENT_BLOB_TYPE = "BlobType";
    public static final String XML_ELEMENT_SERVER_ENCRYPTED = "ServerEncrypted";
    public static final String XML_ELEMENT_ACCESS_TIER = "AccessTier";
    public static final String XML_ELEMENT_ACCESS_TIER_INFERRED = "AccessTierInferred";
    public static final String XML_ELEMENT_ACCESS_TIER_CHANGE_TIME = "AccessTierChangeTime";
    public static final String XML_ELEMENT_BLOB_PREFIX = "BlobPrefix";
    public static final String XML_ELEMENT_VALUE_FALSE = "false";
    public static final String XML_ELEMENT_VALUE_TRUE = "true";
    public static final String XML_ELEMENT_VALUE_HOT = "Hot";
    public static final String XML_ELEMENT_VALUE_ETAG = "0x22178A0E1864500";

    public static final String XML_ELEMENT_ERROR = "Error";
    public static final String XML_ELEMENT_CODE = "Code";
    public static final String XML_ELEMENT_MESSAGE = "Message";

    public static final String XML_VERSION = "<?xml version";
    public static final String XML_VERSION_FULL = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";

    public static final String BLOCKID = "blockid";
    public static final String X_MS_BLOCK_CONTENT_MD5 = "X-Ms-Blob-Content-Md5";

    public static final String LATEST = "Latest";

    // StorageServiceProperties
    public static final String STORAGE_SERVICE_PROPERTIES = "StorageServiceProperties";

    // AzuRequest
    public static final String LOG_CONTAINER_IS_NOT_EXIST = "container is not exist : {}";
    
    // CreateContainer
    public static final String LOG_CREATE_CONTAINER_START = "CreateContainer ...";
    public static final String LOG_CREATE_CONTAINER_NAME = "container : {}";

    // HeadContainer
    public static final String LOG_HAED_CONTAINER_START = "HeadContainer ...";

    // DeleteContainer
    public static final String LOG_DELETE_CONTAINER_START = "DeleteContainer ...";

    // ListContainer
    public static final String LOG_LIST_CONTAINER_START = "ListContainer ...";
    public static final String LOG_LIST_CONTAINER_NAME_DATE = "container : {}, date : {}";

    // CreateBlob
    public static final String LOG_CREATE_BLOB_START = "CreateBlob ...";
    public static final String LOG_CREATE_BLOB_PRIMARY_DISK_ID = "obj prmary disk id : {}";

    // HeadBlob
    public static final String LOG_HEAD_BLOB_START = "HeadBlob ...";

    // GetBlob
    public static final String LOG_GET_BLOB_START = "GetBlob ...";

    // DeleteBlob
    public static final String LOG_DELETE_BLOB_START = "DeleteBlob ...";

    // ListBlob
    public static final String LOG_LIST_BLOB_START = "ListBlob ...";


    // GetProperties
    public static final String LOG_GET_PROPERTIES_START = "GetProperties ...";
    public static final String VERSION_VALUE = "1.0";
    public static final String SERVICE_VERSION_VALUE = "2021-10-04";

    // UploadBlock
    public static final String LOG_UPLOAD_BLOCK_START = "UploadBlock ...";

    // CompleteBlockList
    public static final String LOG_COMPLETE_BLOCK_LIST_START = "CompleteBlockList ...";

    // SharedKeyAuth
    public static final String LOG_SHARED_KEY_AUTH_START = "SharedKeyAuth ...";
    public static final String INVALID_STORAGE_ACCOUNT = "Invalid storage account.";
    public static final String INVALID_OPERATION = "InvalidOperation";

    // AzuObjectOperation
    public static final String LOG_S3OBJECT_OPERATION_DELETE = "delete {}, {}";
}

