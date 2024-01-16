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
package com.pspace.backend.libs.data;

public class Constants {
	private Constants() {
	}

	public static final String ROOT_PATH = "/usr/local/ksan/etc/";
	public static final String AGENT_CONF_PATH = ROOT_PATH + "ksanAgent.conf";
	public static final String LIFECYCLE_MANAGER_SERVICE_ID_PATH = ROOT_PATH + "ksanLifecycleManager.ServiceId";
	public static final String LOG_MANAGER_SERVICE_ID_PATH = ROOT_PATH + "ksanLogManager.ServiceId";
	public static final String REPLICATION_MANAGER_SERVICE_ID_PATH = ROOT_PATH + "ksanReplicationManager.ServiceId";

	public static final String MQ_KSAN_SYSTEM_EXCHANGE = "ksan.system";
	public static final String MQ_KSAN_LOG_EXCHANGE = "ksan.log";

	public static final String MQ_HEARTBEAT_BINDING_KEY = "*.services.state";

	/************************* Lifecycle Manager *************************/
	public static final String MQ_QUEUE_RESTORE_EVENT_ADD = "ksan-restore-event-add";
	public static final String MQ_QUEUE_LIFECYCLE_EVENT_ADD = "ksan-lifecycle-event-add";

	public static final String MQ_BINDING_RESTORE_EVENT = "*.services.restore.event.add";
	public static final String MQ_BINDING_LIFECYCLE_EVENT = "*.services.lifecycle.event.add";

	/************************* Replication Manager *************************/
	public static final String MQ_QUEUE_REPLICATION_S3_LOG = "ksan-replication-s3-log";
	public static final String MQ_QUEUE_REPLICATION_EVENT_ADD = "ksan-replication-event-add";

	public static final String MQ_BINDING_REPLICATION_EVENT = "*.services.replication.event.add";
	/************************* Log Manager *************************/
	public static final String MQ_QUEUE_LOG_MANAGER_S3_LOG = "ksan-log-manager-s3-log";
	public static final String MQ_QUEUE_LOG_MANAGER_BACKEND_LOG = "ksan-log-manager-backend-log";
	public static final String MQ_QUEUE_LOG_MANAGER_RESTORE_EVENT_LOG = "ksan-log-manager-restore-event-log";
	public static final String MQ_QUEUE_LOG_MANAGER_LIFECYCLE_EVENT_LOG = "ksan-log-manager-lifecycle-event-log";
	public static final String MQ_QUEUE_LOG_MANAGER_REPLICATION_EVENT_LOG = "ksan-log-manager-replication-event-log";

	public static final String MQ_BINDING_GW_LOG = "*.services.gw.log.add";
	public static final String MQ_BINDING_BACKEND_LOG = "*.services.backend.log.add";
	public static final String MQ_BINDING_RESTORE_LOG = "*.services.restore.log.add";
	public static final String MQ_BINDING_LIFECYCLE_LOG = "*.services.lifecycle.log.*";
	public static final String MQ_BINDING_LIFECYCLE_LOG_SUCCESS = "*.services.lifecycle.log.success";
	public static final String MQ_BINDING_LIFECYCLE_LOG_FAILED = "*.services.lifecycle.log.failed";
	public static final String MQ_BINDING_REPLICATION_LOG = "*.services.replication.log.*";
	public static final String MQ_BINDING_REPLICATION_LOG_SUCCESS = "*.services.replication.log.success";
	public static final String MQ_BINDING_REPLICATION_LOG_FAILED = "*.services.replication.log.failed";

	public static final String MQ_EXCHANGE_OPTION_DIRECT = "direct";
	public static final String MQ_EXCHANGE_OPTION_TOPIC = "topic";

	public static final String EM_S3_NOT_WORKING = "This S3 does not work!";

	public static final String DB_TYPE_MARIADB = "MariaDB";
	public static final String DB_TYPE_MONGODB = "MongoDB";

	public static final String getQueueName(String queueName, String serviceId) {
		return queueName + "-" + serviceId;
	}

	/************************* Portal Manager *************************/
	public static final String URL_CONFIG = "/api/v1/Config";
	public static final String URL_OBJ_MANAGER_CONFIG = URL_CONFIG + "/KsanObjManager";
	public static final String URL_REPLICATION_MANAGER_CONFIG = URL_CONFIG + "/KsanReplicationManager";
	public static final String URL_LIFECYCLE_MANAGER_CONFIG = URL_CONFIG + "/KsanLifecycleManager";
	public static final String URL_LOG_MANAGER_CONFIG = URL_CONFIG + "/KsanLogManager";

	public static final String URL_REGION = "/api/v1/Regions";
	public static final String URL_LOCAL_REGION = URL_REGION + "/Default";
}
