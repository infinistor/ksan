package com.pspace.backend.Data;

public class LifecycleConstants {
	public static final String AGENT_CONF_PATH = "/usr/local/ksan/etc/ksanAgent.conf";
	public static final String LIFECYCLE_SERVICE_ID_PATH = "/usr/local/ksan/sbin/.ksanLifecycleManager.ServiceId";
	public static final String MQ_EXCHANGE_NAME = "ksan.system";
	public static final String MQ_BINDING_KEY = "*.services.state";
}
