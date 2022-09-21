package com.pspace.backend.Heartbeat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.Data.LifecycleConstants;
import com.pspace.ifs.ksan.libs.mq.MQSender;

public class Heartbeat {
	static final Logger logger = LoggerFactory.getLogger(Heartbeat.class);

	private static final String ONLINE = "Online";

	private final String ServiceId;
	private final MQSender Sender;

	public boolean Stop = false;

	public Heartbeat(String ServiceId, String Host, int Port, String User, String Password)
			throws Exception {
		this.ServiceId = ServiceId;
		Sender = new MQSender(Host, Port, User, Password, LifecycleConstants.MQ_EXCHANGE_NAME, "direct", "");
	}

	public void Start(int Delay) {
		var Request = new RequestServiceState(ServiceId, ONLINE);

		while (!Stop) {
			try {
				Thread.sleep(Delay);
				Sender.send(Request.toString(), LifecycleConstants.MQ_BINDING_KEY);
			} catch (Exception e) {
				logger.error("", e);
				
			}
		}
	}
}
