
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
    public static final String PORTAL_REST_API_CONFIG_S3 = "/api/v1/Config/S3";
	public static final String PORTAL_REST_API_DISKPOOLS_DETAILS = "/api/v1/DiskPools/Details";
	public static final String PORTAL_REST_API_S3USERS = "/api/v1/S3Users";
    public static final String KMON_CONFIG_PATH = "/usr/local/ksan/etc/ksanAgent.conf";
	public static final String DISKPOOL_CONF_PATH = "/var/log/ksan/gw/diskpools_dump.xml";
	public static final String KMON_PROPERTY_PORTAL_HOST = "PortalHost";
	public static final String KMON_PROPERTY_PORTAL_PORT = "PortalPort";
	public static final String KMON_POOPERTY_POTAL_KEY = "PortalApiKey";
	public static final String KMON_PROPERTY_MQ_HOST = "MqHost";
	public static final String KMON_PROPERTY_MQ_PORT = "MqPort";
	public static final String KMON_PROPERTY_MQ_USER = "MqUser";
	public static final String KMON_PROPERTY_MQ_PASSWORD = "MqPassword";
	public static final String KMON_PROPERTY_SERVER_ID = "ServerId";

    public static final String LOG_CONFIG_NOT_EXIST = "Properties file is not exist";
	public static final String LOG_CONFIG_FAILED_LOADING = "Properties file load is fail";
	public static final String LOG_CONFIG_MUST_CONTAIN = "Properties file must contain: ";

	private Constants() {
		throw new IllegalStateException("Utility class");
	}
}
