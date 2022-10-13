/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License. See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.backend.libs.Data;

public class Constants {
	public static final String AGENT_CONF_PATH = "/usr/local/ksan/etc/ksanAgent.conf";
	public static final String LIFECYCLE_SERVICE_ID_PATH = "/usr/local/ksan/sbin/.ksanLifecycle.ServiceId";
	public static final String LOGMANAGER_SERVICE_ID_PATH = "/usr/local/ksan/sbin/.ksanLogManager.ServiceId";
	public static final String REPLICATION_SERVICE_ID_PATH = "/usr/local/ksan/sbin/.ksanReplication.ServiceId";

	public static final String MQ_KSAN_SYSTEM_EXCHANGE = "ksan.system";
	public static final String MQ_KSAN_LOG_EXCHANGE = "ksan.log";
	public static final String MQ_HEARTBEAT_BINDING_KEY = "*.services.state";

	public static final String MQ_QUEUE_REPLICATION_S3_LOG = "ksan-replication-s3-log";
	public static final String MQ_QUEUE_REPLICATION_EVENT_ADD = "ksan-replication-event-add";

	public static final String MQ_QUEUE_LOG_MANAGER_S3_LOG = "ksan-logManager-s3-log";
	public static final String MQ_QUEUE_LOG_MANAGER_REPLICATION_EVENT_LOG = "ksan-logManager-replication-event-log";


	public static final String MQ_BINDING_REPLICATION_EVENT = "*.services.replication.event.add";
	public static final String MQ_BINDING_REPLICATION_LOG = "*.services.replication.log.add";
	public static final String MQ_BINDING_GW_LOG = "*.services.gw.log.add";
	
	public static final String MQ_EXCHANGE_OPTION_DIRECT = "direct";
	public static final String MQ_EXCHANGE_OPTION_FANOUT = "fanout";
	public static final String MQ_EXCHANGE_OPTION_TOPIC = "topic";

	public static final String EM_S3_NOT_WORKING = "This S3 does not work!";

	public static final String DB_TYPE_MARIADB = "MariaDB";
	public static final String DB_TYPE_MONGODB = "MongoDB";

	public static final String MQ_QUEUE_NAME(String QueueName, String ServiceId)
	{
		return QueueName + "-" + ServiceId;
	}
}
