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
package com.pspace.backend.logManager.logging;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.Config.ConfigManager;
import com.pspace.backend.libs.Data.Constants;
import com.pspace.backend.libs.Ksan.AgentConfig;
import com.pspace.ifs.ksan.libs.mq.MQReceiver;

public class MainLogger {
	final Logger logger = LoggerFactory.getLogger(MainLogger.class);
	final ConfigManager Config;
	protected final AgentConfig agent;

	List<MQReceiver> s3LogReceivers = new ArrayList<MQReceiver>();
	List<MQReceiver> backendLogReceivers = new ArrayList<MQReceiver>();
	List<MQReceiver> replicationLogReceivers = new ArrayList<MQReceiver>();
	List<MQReceiver> lifecycleLogReceivers = new ArrayList<MQReceiver>();
	List<MQReceiver> restoreLogReceivers = new ArrayList<MQReceiver>();

	// Thread SendThread;
	// SendLogger Sender;

	public MainLogger() {
		this.agent = AgentConfig.getInstance();
		Config = ConfigManager.getInstance();
	}

	public boolean start(int threadCount) {
		try {
			// Sender = new SendLogger(DB, Config, Region);
			// SendThread = new Thread(() -> Sender.Run());
			// SendThread.start();
			for (int index = 0; index < threadCount; index++) {
				s3LogReceivers.add(new MQReceiver(
						agent.mqHost,
						agent.mqPort,
						agent.mqUser,
						agent.mqPassword,
						Constants.MQ_QUEUE_LOG_MANAGER_S3_LOG,
						Constants.MQ_KSAN_LOG_EXCHANGE,
						false,
						Constants.MQ_EXCHANGE_OPTION_TOPIC,
						Constants.MQ_BINDING_GW_LOG,
						new S3LogReceiver()));

				backendLogReceivers.add(new MQReceiver(
						agent.mqHost,
						agent.mqPort,
						agent.mqUser,
						agent.mqPassword,
						Constants.MQ_QUEUE_LOG_MANAGER_BACKEND_LOG,
						Constants.MQ_KSAN_LOG_EXCHANGE,
						false,
						Constants.MQ_EXCHANGE_OPTION_TOPIC,
						Constants.MQ_BINDING_BACKEND_LOG,
						new BackendLogReceiver()));

				replicationLogReceivers.add(new MQReceiver(
						agent.mqHost,
						agent.mqPort,
						agent.mqUser,
						agent.mqPassword,
						Constants.MQ_QUEUE_LOG_MANAGER_REPLICATION_EVENT_LOG,
						Constants.MQ_KSAN_LOG_EXCHANGE,
						false,
						Constants.MQ_EXCHANGE_OPTION_TOPIC,
						Constants.MQ_BINDING_REPLICATION_LOG,
						new ReplicationLogReceiver()));

				lifecycleLogReceivers.add(new MQReceiver(
						agent.mqHost,
						agent.mqPort,
						agent.mqUser,
						agent.mqPassword,
						Constants.MQ_QUEUE_LOG_MANAGER_LIFECYCLE_EVENT_LOG,
						Constants.MQ_KSAN_LOG_EXCHANGE,
						false,
						Constants.MQ_EXCHANGE_OPTION_TOPIC,
						Constants.MQ_BINDING_LIFECYCLE_LOG,
						new LifecycleLogReceiver()));

				restoreLogReceivers.add(new MQReceiver(
						agent.mqHost,
						agent.mqPort,
						agent.mqUser,
						agent.mqPassword,
						Constants.MQ_QUEUE_LOG_MANAGER_RESTORE_EVENT_LOG,
						Constants.MQ_KSAN_LOG_EXCHANGE,
						false,
						"",
						Constants.MQ_BINDING_RESTORE_LOG,
						new RestoreLogReceiver()));
			}

			return true;
		} catch (Exception e) {
			logger.error("", e);
			return false;
		}
	}

	public void quit() {
		try {
			// Sender.Quit();
			// SendThread.join();
		} catch (Exception e) {
			logger.error("", e);
		}
	}

}
