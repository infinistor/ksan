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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.pspace.ifs.ksan.gw.identity.S3Region;
import com.pspace.ifs.ksan.gw.identity.S3User;
import com.pspace.ifs.ksan.gw.object.objmanager.ObjManagers;
import com.pspace.ifs.ksan.libs.osd.OSDClientManager2;
import com.pspace.ifs.ksan.libs.mq.MQCallback;
import com.pspace.ifs.ksan.libs.mq.MQReceiver;
import com.pspace.ifs.ksan.libs.mq.MQResponse;
import com.pspace.ifs.ksan.libs.mq.MQResponseType;
import com.pspace.ifs.ksan.libs.mq.MQResponseCode;
import com.pspace.ifs.ksan.libs.DiskManager;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.config.AgentConfig;
import com.pspace.ifs.ksan.libs.disk.Disk;
import com.pspace.ifs.ksan.libs.disk.DiskPool;
import com.pspace.ifs.ksan.libs.disk.Server;
import com.pspace.ifs.ksan.libs.HeartbeatManager;
import com.pspace.ifs.ksan.libs.Constants;

import com.google.common.base.Strings;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.entity.ContentType;
import org.apache.http.ssl.SSLContextBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

class ConfigUpdateCallback implements MQCallback{
	private static final Logger logger = LoggerFactory.getLogger(ConfigUpdateCallback.class);
	@Override
	public MQResponse call(String routingKey, String body) {
		logger.info(GWConstants.GWPORTAL_RECEIVED_CONFIG_CHANGE);
		logger.info(GWConstants.LOG_GWPORTAL_RECEIVED_MESSAGE_QUEUE_DATA, routingKey, body);

		GWPortal.getInstance().getConfig();
		
		return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
	}    
}

class ObjManagerConfigUpdateCallback implements MQCallback{
	private static final Logger logger = LoggerFactory.getLogger(ObjManagerConfigUpdateCallback.class);
	@Override
	public MQResponse call(String routingKey, String body) {
		logger.info(GWConstants.GWPORTAL_RECEIVED_OBJMANAGER_CONFIG_CHANGE);
		logger.info(GWConstants.LOG_GWPORTAL_RECEIVED_MESSAGE_QUEUE_DATA, routingKey, body);

		GWPortal.getInstance().getObjManagerConfig();
		
		return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
	}    
}

class DiskUpdateCallback implements MQCallback{
	private static final Logger logger = LoggerFactory.getLogger(DiskUpdateCallback.class);
	@Override
	public MQResponse call(String routingKey, String body) {
		try {
			logger.info(GWConstants.GWPORTAL_RECEIVED_DISK_CHANGE);
			logger.info(GWConstants.LOG_GWPORTAL_RECEIVED_MESSAGE_QUEUE_DATA, routingKey, body);
	
			if (GWPortal.getInstance().isAppliedDiskpools()) {
				GWPortal.getInstance().getDiskPoolsDetails();
				ObjManagers.getInstance().notifyChangeDiskpools(routingKey, body);
			} 
		} catch (Exception e) {
			PrintStack.logging(logger, e);
		} finally {
			return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
		}
	}
}

class DiskpoolsUpdateCallback implements MQCallback{
	private static final Logger logger = LoggerFactory.getLogger(DiskpoolsUpdateCallback.class);
	@Override
	public MQResponse call(String routingKey, String body) {
		try {
			logger.info(GWConstants.GWPORTAL_RECEIVED_DISKPOOLS_CHANGE);
			logger.info(GWConstants.LOG_GWPORTAL_RECEIVED_MESSAGE_QUEUE_DATA, routingKey, body);
	
			if (GWPortal.getInstance().isAppliedDiskpools()) {
				GWPortal.getInstance().getDiskPoolsDetails();
				ObjManagers.getInstance().notifyChangeDiskpools(routingKey, body);
			} 
		} catch (Exception e) {
			PrintStack.logging(logger, e);
		} finally {
			return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
		}
	}    
}

class UserUpdateCallBack implements MQCallback{
	private static final Logger logger = LoggerFactory.getLogger(UserUpdateCallBack.class);
	@Override
	public MQResponse call(String routingKey, String body) {
		logger.info(GWConstants.GWPORTAL_RECEIVED_USER_CHANGE);
		logger.info(GWConstants.LOG_GWPORTAL_RECEIVED_MESSAGE_QUEUE_DATA, routingKey, body);
		JSONParser parser = new JSONParser();
		JSONObject data = null;
		JSONArray jsonUserDiskpools = null;
		try {
			data = (JSONObject)parser.parse(body);
		} catch (ParseException e) {
			PrintStack.logging(logger, e);
		}
		
		if (routingKey.contains(GWConstants.GWPORTAL_RECEIVED_USER_ADDED)) {
			jsonUserDiskpools = (JSONArray)data.get(S3User.USER_DISK_POOLS);
			S3User user = new S3User((String)data.get(S3User.USER_ID), 
									 (String)data.get(S3User.USER_NAME), 
									 (String)data.get(S3User.USER_EMAIL), 
									 (String)data.get(S3User.ACCESS_KEY), 
									 (String)data.get(S3User.ACCESS_SECRET),
									 jsonUserDiskpools);
			S3UserManager.getInstance().addUser(user);
			logger.info(GWConstants.LOG_GWPORTAL_RECEIVED_USER_DATA, user.getUserId(), user.getUserName(), user.getUserEmail(), user.getAccessKey(), user.getAccessSecret());
			S3UserManager.getInstance().printUsers();
		} else if (routingKey.contains(GWConstants.GWPORTAL_RECEIVED_USER_UPDATED)) {
			jsonUserDiskpools = (JSONArray)data.get(S3User.USER_DISK_POOLS);
			S3User user = S3UserManager.getInstance().getUserById((String)data.get(S3User.USER_ID));
			S3UserManager.getInstance().removeUser(user);
			user = new S3User((String)data.get(S3User.USER_ID), 
							  (String)data.get(S3User.USER_NAME), 
							  (String)data.get(S3User.USER_EMAIL), 
							  (String)data.get(S3User.ACCESS_KEY), 
							  (String)data.get(S3User.ACCESS_SECRET),
							  jsonUserDiskpools);
			S3UserManager.getInstance().addUser(user);
			logger.info(GWConstants.LOG_GWPORTAL_RECEIVED_USER_DATA, user.getUserId(), user.getUserName(), user.getUserEmail(), user.getAccessKey(), user.getAccessSecret());
			S3UserManager.getInstance().printUsers();
		} else if (routingKey.contains(GWConstants.GWPORTAL_RECEIVED_USER_REMOVED)) {
			S3User user = S3UserManager.getInstance().getUserById((String)data.get(S3User.USER_ID));
			S3UserManager.getInstance().removeUser(user);
			S3UserManager.getInstance().printUsers();
		} else {
			logger.info(GWConstants.LOG_GWPORTAL_RECEIVED_USER_WRONG_ROUTING_KEY, routingKey);
		}

		return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
	}
}

class ServiceUpdateCallback implements MQCallback{
	private static final Logger logger = LoggerFactory.getLogger(ServiceUpdateCallback.class);
	@Override
	public MQResponse call(String routingKey, String body) {
		try {
			logger.info(GWConstants.GWPORTAL_RECEIVED_SERVICE_CHANGE);
			logger.info(GWConstants.LOG_GWPORTAL_RECEIVED_MESSAGE_QUEUE_DATA, routingKey, body);
			if (routingKey.equals(GWConstants.MQUEUE_NAME_GW_SERVICE_ADDED_ROUTING_KEY)) {
				logger.info("service added ... {}", body);
			} else if (routingKey.equals(GWConstants.MQUEUE_NAME_GW_SERVICE_UPDATED_ROUTING_KEY)) {
				logger.info("service updated ... {}", body);
			} else if (routingKey.equals(GWConstants.MQUEUE_NAME_GW_SERVICE_REMOVED_ROUTING_KEY)) {
				logger.info("service removed ... {}", body);
			} else {
				logger.info("not defined routing key");
			}
		} catch (Exception e) {
			PrintStack.logging(logger, e);
		} finally {
			return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
		}
	}    
}

// class RegionUpdateCallBack implements MQCallback{
// 	private static final Logger logger = LoggerFactory.getLogger(RegionUpdateCallBack.class);
// 	@Override
// 	public MQResponse call(String routingKey, String body) {
// 		logger.info(GWConstants.GWPORTAL_RECEIVED_USER_CHANGE);
// 		logger.info(GWConstants.LOG_GWPORTAL_RECEIVED_MESSAGE_QUEUE_DATA, routingKey, body);
// 		JSONParser parser = new JSONParser();
// 		JSONObject data = null;
// 		JSONArray jsonUserDiskpools = null;
// 		try {
// 			data = (JSONObject)parser.parse(body);
// 		} catch (ParseException e) {
// 			PrintStack.logging(logger, e);
// 		}
		
		// if (routingKey.contains(GWConstants.GWPORTAL_RECEIVED_USER_ADDED)) {
		// 	jsonUserDiskpools = (JSONArray)data.get(S3User.USER_DISK_POOLS);
		// 	S3User user = new S3User((String)data.get(S3User.USER_ID), 
		// 							 (String)data.get(S3User.USER_NAME), 
		// 							 (String)data.get(S3User.USER_EMAIL), 
		// 							 (String)data.get(S3User.ACCESS_KEY), 
		// 							 (String)data.get(S3User.ACCESS_SECRET),
		// 							 jsonUserDiskpools);
		// 	S3UserManager.getInstance().addUser(user);
		// 	logger.info(GWConstants.LOG_GWPORTAL_RECEIVED_USER_DATA, user.getUserId(), user.getUserName(), user.getUserEmail(), user.getAccessKey(), user.getAccessSecret());
		// 	S3UserManager.getInstance().printUsers();
		// } else if (routingKey.contains(GWConstants.GWPORTAL_RECEIVED_USER_UPDATED)) {
		// 	jsonUserDiskpools = (JSONArray)data.get(S3User.USER_DISK_POOLS);
		// 	S3User user = S3UserManager.getInstance().getUserById((String)data.get(S3User.USER_ID));
		// 	S3UserManager.getInstance().removeUser(user);
		// 	user = new S3User((String)data.get(S3User.USER_ID), 
		// 					  (String)data.get(S3User.USER_NAME), 
		// 					  (String)data.get(S3User.USER_EMAIL), 
		// 					  (String)data.get(S3User.ACCESS_KEY), 
		// 					  (String)data.get(S3User.ACCESS_SECRET),
		// 					  jsonUserDiskpools);
		// 	S3UserManager.getInstance().addUser(user);
		// 	logger.info(GWConstants.LOG_GWPORTAL_RECEIVED_USER_DATA, user.getUserId(), user.getUserName(), user.getUserEmail(), user.getAccessKey(), user.getAccessSecret());
		// 	S3UserManager.getInstance().printUsers();
		// } else if (routingKey.contains(GWConstants.GWPORTAL_RECEIVED_USER_REMOVED)) {
		// 	S3User user = S3UserManager.getInstance().getUserById((String)data.get(S3User.USER_ID));
		// 	S3UserManager.getInstance().removeUser(user);
		// 	S3UserManager.getInstance().printUsers();
		// } else {
		// 	logger.info(GWConstants.LOG_GWPORTAL_RECEIVED_USER_WRONG_ROUTING_KEY, routingKey);
		// }

// 		return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
// 	}
// }

public class GWPortal {
	private boolean isAppliedDiskpools;
	private boolean isAppliedUsers;
	private AgentConfig agentConfig;
	private String serviceId;
	private MQReceiver receiverConfigureCB;
	private MQReceiver receiverConfigureObjManagerCB;
	private MQReceiver receiveDiskAddCB;
	private MQReceiver receiveDiskUpadateCB;
	private MQReceiver receiveDiskRemoveCB;
	private MQReceiver receiveDiskStateCB;
	private MQReceiver receiveDiskModeCB;
	private MQReceiver receiveDiskpoolsCB;
	private MQReceiver receiveUserCB;
	private MQReceiver receiveServiceAddCB;
	private MQReceiver receiveServiceUpdateCB;
	private MQReceiver receiverServiceRemoveCB;

    private static final Logger logger = LoggerFactory.getLogger(GWPortal.class);

    public static GWPortal getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static final GWPortal INSTANCE = new GWPortal();
    }

    private GWPortal() {
        agentConfig = AgentConfig.getInstance(); 
        agentConfig.configure();
		String mqHost = agentConfig.getMQHost();
		int mqPort = Integer.parseInt(agentConfig.getMQPort());
		String userName = agentConfig.getMQUser();
		String password = agentConfig.getMQPassword();

		logger.info("host : {}, port : {}, user :  {}, password : {}", mqHost, mqPort, userName, password);

		// serviceId
		try {
			BufferedReader reader = new BufferedReader(new FileReader(System.getProperty(Constants.GW_SERVICEID_KEY) + File.separator + Constants.GW_SERVICEID_FILE, StandardCharsets.UTF_8));

			serviceId = reader.readLine();
			logger.info("serviceId : {}", serviceId);
			reader.close();
		} catch (IOException e) {
			PrintStack.logging(logger, e);
			throw new RuntimeException(new RuntimeException());
		}

		postGWEvent(true);

		try {
			HeartbeatManager heartbeatManager = new HeartbeatManager(serviceId, mqHost, mqPort, userName, password, GWConstants.MQUEUE_EXCHANGE_NAME);
			heartbeatManager.startHeartbeat(agentConfig.getServiceMonitorInterval());
		} catch (IOException e) {
			PrintStack.logging(logger, e);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
		}

		if (Strings.isNullOrEmpty(agentConfig.getServerId())) {
			logger.error("mq server id is null or empty");
			throw new RuntimeException(new RuntimeException());
		}

        try
		{
			MQCallback configureCB = new ConfigUpdateCallback();
			receiverConfigureCB = new MQReceiver(mqHost,
											   mqPort,
											   userName,
											   password,
											   GWConstants.MQUEUE_NAME_GW_CONFIG + serviceId, //agentConfig.getServerId(), 
											   GWConstants.MQUEUE_EXCHANGE_NAME, 
											   false,
											   "", 
											   GWConstants.MQUEUE_NAME_GW_CONFIG_ROUTING_KEY, 
											   configureCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}

		try
		{
			MQCallback configureObjManagerCB = new ObjManagerConfigUpdateCallback();
			receiverConfigureObjManagerCB = new MQReceiver(mqHost,
											   mqPort,
											   userName,
											   password,
											   GWConstants.MQUEUE_NAME_OBJMANAGER_CONFIG + serviceId, //agentConfig.getServerId(), 
											   GWConstants.MQUEUE_EXCHANGE_NAME, 
											   false,
											   "", 
											   GWConstants.MQUEUE_NAME_OBJMANAGER_CONFIG_ROUTING_KEY, 
											   configureObjManagerCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}

		try {
			MQCallback diskAddCB = new DiskUpdateCallback();
			receiveDiskAddCB = new MQReceiver(mqHost,
											   mqPort,
											   userName,
											   password,
											   GWConstants.MQUEUE_NAME_GW_DISK + serviceId, //agentConfig.getServerId(), 
											   GWConstants.MQUEUE_EXCHANGE_NAME, 
											   false, 
											   "", 
											   GWConstants.MQUEUE_NAME_GW_DISK_ADDED_ROUTING_KEY, 
											   diskAddCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}

		try {
			MQCallback diskUpadateCB = new DiskUpdateCallback();
			receiveDiskUpadateCB = new MQReceiver(mqHost,
											   mqPort,
											   userName,
											   password,
											   GWConstants.MQUEUE_NAME_GW_DISK + serviceId, //agentConfig.getServerId(), 
											   GWConstants.MQUEUE_EXCHANGE_NAME, 
											   false, 
											   "", 
											   GWConstants.MQUEUE_NAME_GW_DISK_UPDATED_ROUTING_KEY, 
											   diskUpadateCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}

		try {
			MQCallback diskRemoveCB = new DiskUpdateCallback();
			receiveDiskRemoveCB = new MQReceiver(mqHost, 
											   mqPort,
											   userName,
											   password,
											   GWConstants.MQUEUE_NAME_GW_DISK + serviceId, //agentConfig.getServerId(), 
											   GWConstants.MQUEUE_EXCHANGE_NAME, 
											   false, 
											   "", 
											   GWConstants.MQUEUE_NAME_GW_DISK_REMOVED_ROUTING_KEY, 
											   diskRemoveCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}

		try {
			MQCallback diskStateCB = new DiskUpdateCallback();
			receiveDiskStateCB = new MQReceiver(mqHost, 
											   mqPort,
											   userName,
											   password,
											   GWConstants.MQUEUE_NAME_GW_DISK + serviceId, //agentConfig.getServerId(), 
											   GWConstants.MQUEUE_EXCHANGE_NAME, 
											   false, 
											   "", 
											   GWConstants.MQUEUE_NAME_GW_DISK_STATE_ROUTING_KEY, 
											   diskStateCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}

		try {
			MQCallback diskModeCB = new DiskUpdateCallback();
			receiveDiskModeCB = new MQReceiver(mqHost, 
											   mqPort,
											   userName,
											   password,
											   GWConstants.MQUEUE_NAME_GW_DISK + serviceId, //agentConfig.getServerId(), 
											   GWConstants.MQUEUE_EXCHANGE_NAME, 
											   false, 
											   "", 
											   GWConstants.MQUEUE_NAME_GW_DISK_RWMODE_ROUTING_KEY, 
											   diskModeCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}

		try {
			MQCallback diskpoolsCB = new DiskpoolsUpdateCallback();
			receiveDiskpoolsCB = new MQReceiver(mqHost, 
											   mqPort,
											   userName,
											   password,
											   GWConstants.MQUEUE_NAME_GW_DISKPOOL + serviceId, //agentConfig.getServerId(), 
											   GWConstants.MQUEUE_EXCHANGE_NAME, 
											   false, 
											   "", 
											   GWConstants.MQUEUE_NAME_GW_DISKPOOL_ROUTING_KEY, 
											   diskpoolsCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}

		try {
			MQCallback userCB = new UserUpdateCallBack();
			receiveUserCB = new MQReceiver(mqHost, 
											   mqPort,
											   userName,
											   password,
											   GWConstants.MQUEUE_NAME_GW_USER + serviceId, //agentConfig.getServerId(), 
											   GWConstants.MQUEUE_EXCHANGE_NAME, 
											   false, 
											   "", 
											   GWConstants.MQUEUE_NAME_GW_USER_ROUTING_KEY, 
											   userCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}

		try {
			MQCallback serviceAddCB = new ServiceUpdateCallback();
			receiveServiceAddCB = new MQReceiver(mqHost, 
											   mqPort,
											   userName,
											   password,
											   GWConstants.MQUEUE_NAME_GW_SERVICE + serviceId, //agentConfig.getServerId(), 
											   GWConstants.MQUEUE_EXCHANGE_NAME, 
											   false, 
											   "", 
											   GWConstants.MQUEUE_NAME_GW_SERVICE_ADDED_ROUTING_KEY, 
											   serviceAddCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}

		try {
			MQCallback serviceUpdateCB = new ServiceUpdateCallback();
			receiveServiceUpdateCB = new MQReceiver(mqHost, 
											   mqPort,
											   userName,
											   password,
											   GWConstants.MQUEUE_NAME_GW_SERVICE + serviceId, //agentConfig.getServerId(), 
											   GWConstants.MQUEUE_EXCHANGE_NAME, 
											   false, 
											   "", 
											   GWConstants.MQUEUE_NAME_GW_SERVICE_UPDATED_ROUTING_KEY, 
											   serviceUpdateCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}

		try {
			MQCallback serviceRemoveCB = new ServiceUpdateCallback();
			receiverServiceRemoveCB = new MQReceiver(mqHost, 
											   mqPort,
											   userName,
											   password,
											   GWConstants.MQUEUE_NAME_GW_SERVICE + serviceId, //agentConfig.getServerId(), 
											   GWConstants.MQUEUE_EXCHANGE_NAME, 
											   false, 
											   "", 
											   GWConstants.MQUEUE_NAME_GW_SERVICE_REMOVED_ROUTING_KEY, 
											   serviceRemoveCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}

		isAppliedDiskpools = false;
		isAppliedUsers = false;
    }

	public boolean isAppliedDiskpools() {
		return isAppliedDiskpools;
	}

	public boolean isAppliedUsers() {
		return isAppliedUsers;
	}

    public void getConfig() {
        try {
			HttpClient client = HttpClients
                .custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
								
			HttpGet getRequest = new HttpGet(GWConstants.HTTPS + agentConfig.getPortalIp() + GWConstants.COLON + agentConfig.getPortalPort() + GWConstants.PORTAL_REST_API_CONFIG_GW);
			getRequest.addHeader(GWConstants.AUTHORIZATION, agentConfig.getPortalKey());

			HttpResponse response = client.execute(getRequest);
			if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				String body = handler.handleResponse(response);
                logger.info(body);

                JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject)parser.parse(body);
                JSONObject jsonData = (JSONObject)jsonObject.get(AgentConfig.DATA);
				String version = String.valueOf(jsonData.get(AgentConfig.VERSION));
                String config = (String)jsonData.get(AgentConfig.CONFIG);
                logger.info(config);

                JSONObject jsonConfig = (JSONObject)parser.parse(config);
                
                GWConfig.getInstance().setConfig(jsonConfig);
				GWConfig.getInstance().setVersion(version);
                GWConfig.getInstance().saveConfigFile();
				return;
			}
			throw new RuntimeException(new RuntimeException());
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new RuntimeException(e);
		}
    }

	public void getObjManagerConfig() {
        try {
			HttpClient client = HttpClients
                .custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
								
			HttpGet getRequest = new HttpGet(GWConstants.HTTPS + agentConfig.getPortalIp() + GWConstants.COLON + agentConfig.getPortalPort() + GWConstants.PORTAL_REST_API_CONFIG_OBJMANAGER);
			getRequest.addHeader(GWConstants.AUTHORIZATION, agentConfig.getPortalKey());

			HttpResponse response = client.execute(getRequest);
			if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				String body = handler.handleResponse(response);
                logger.info(body);

                JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject)parser.parse(body);
                JSONObject jsonData = (JSONObject)jsonObject.get(AgentConfig.DATA);
				String version = String.valueOf(jsonData.get(AgentConfig.VERSION));
                String config = (String)jsonData.get(AgentConfig.CONFIG);
                logger.info(config);

                JSONObject jsonConfig = (JSONObject)parser.parse(config);

				ObjectManagerConfig.getInstance().setVersion(version);
                ObjectManagerConfig.getInstance().setConfig(jsonConfig);
				ObjectManagerConfig.getInstance().setMqQueueName(GWConstants.MQUEUE_NAME);
				ObjectManagerConfig.getInstance().setMqExchangeName(GWConstants.MQUEUE_EXCHANGE_NAME);
				ObjectManagerConfig.getInstance().setMqOsdExchangeName(GWConstants.MQUEUE_OSD_EXCHANGE_NAME);
				ObjectManagerConfig.getInstance().setMqHost(agentConfig.getMQHost());
				ObjectManagerConfig.getInstance().setMqPort(Integer.parseInt(agentConfig.getMQPort()));
				ObjectManagerConfig.getInstance().setMqUser(agentConfig.getMQUser()); 
				ObjectManagerConfig.getInstance().setMqPassword(agentConfig.getMQPassword());
                ObjectManagerConfig.getInstance().saveConfigFile();
				return;
			} else {
				logger.error("Failed to get config from portal. status code : {}", response.getStatusLine().getStatusCode());
				throw new RuntimeException(new RuntimeException());
			}
		} catch (RuntimeException e) {
			PrintStack.logging(logger, e);
			throw new RuntimeException(e);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new RuntimeException(e);
		}
    }

    public void getDiskPoolsDetails() {
        try {
			HttpClient client = HttpClients
                .custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
								
			HttpGet getRequest = new HttpGet(GWConstants.HTTPS + agentConfig.getPortalIp() + GWConstants.COLON + agentConfig.getPortalPort() + GWConstants.PORTAL_REST_API_DISKPOOLS_DETAILS);
			getRequest.addHeader(GWConstants.AUTHORIZATION, agentConfig.getPortalKey());

			HttpResponse response = client.execute(getRequest);
			if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				String body = handler.handleResponse(response);
				logger.info(body);
                JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject)parser.parse(body);
				JSONObject jsonData = (JSONObject)jsonObject.get(DiskManager.DATA);

				if ((long)jsonData.get("TotalCount") == 0) {
					logger.info("diskpools total count is 0");
					return;
				} 

				JSONArray jsonItems = (JSONArray)jsonData.get(DiskManager.ITEMS);
				DiskManager.getInstance().clearDiskPoolList();

				for (int i = 0; i < jsonItems.size(); i++) {
					JSONObject item = (JSONObject)jsonItems.get(i);
					JSONObject jsonEC = (JSONObject)item.get(DiskPool.EC);
					
					DiskPool diskPool = null;
					if (jsonEC != null) {
						logger.info("jsonEC : {}", jsonEC.toString());
						diskPool = new DiskPool((String)item.get(DiskPool.ID), 
						   	    				(String)item.get(DiskPool.NAME), 
												(String)item.get(DiskPool.DISK_POOL_TYPE), 
												(String)item.get(DiskPool.REPLICATION_TYPE),
												(int)(long)jsonEC.get(DiskPool.EC_M),
												(int)(long)jsonEC.get(DiskPool.EC_K));
					} else {
						diskPool = new DiskPool((String)item.get(DiskPool.ID), 
												(String)item.get(DiskPool.NAME), 
												(String)item.get(DiskPool.DISK_POOL_TYPE), 
												(String)item.get(DiskPool.REPLICATION_TYPE));
					}
					
					JSONArray jsonServers = (JSONArray)item.get(DiskPool.SERVERS);
					if (jsonServers == null || jsonServers.isEmpty()) {
						logger.info("diskpools -- servers is empty");
						return;
					}
					for (int j = 0; j < jsonServers.size(); j++) {
						JSONObject jsonServer = (JSONObject)jsonServers.get(j);
						JSONArray jsonNetwork = (JSONArray)jsonServer.get(Server.NETWORK_INTERFACES);
						String ipAddress = null;
						for (int k = 0; k < jsonNetwork.size(); k++) {
							JSONObject network = (JSONObject)jsonNetwork.get(k);
							ipAddress = (String)network.get(Server.IP_ADDRESS);
						}
						JSONArray jsonDisks = (JSONArray)jsonServer.get(Server.DISKS);
						Server server = new Server((String)jsonServer.get(Server.ID), ipAddress, (String)jsonServer.get(Server.STATE));
						for (int l = 0; l < jsonDisks.size(); l++) {
							JSONObject jsonDisk = (JSONObject)jsonDisks.get(l);
							Disk disk = new Disk((String)jsonDisk.get(Disk.ID), (String)jsonDisk.get(Disk.RW_MODE), (String)jsonDisk.get(Disk.PATH), (String)jsonDisk.get(Disk.STATE));
							server.addDisk(disk);
						}
						diskPool.addServer(server);
					}
					DiskManager.getInstance().addDiskPool(diskPool);
				}
				DiskManager.getInstance().configure();
				DiskManager.getInstance().saveFile();
				// OSDClientManager2.getInstance().update((int)GWConfig.getInstance().getOsdPort(), (int)GWConfig.getInstance().getOsdClientCount());
				
				for (DiskPool diskpool : DiskManager.getInstance().getDiskPoolList()) {
					for (Server server : diskpool.getServerList()) {
						if (GWUtils.getLocalIP().equals(server.getIp())) {
							for (Disk disk : server.getDiskList()) {
								File file = new File(disk.getPath() + GWConstants.SLASH + Constants.OBJ_DIR);
								if (!file.exists()) {
									if (!file.mkdirs()) {
										logger.error("Failed to create obj dir : {}", file.getAbsolutePath());
										throw new RuntimeException(new RuntimeException());
									}
								}

								// file = new File(disk.getPath() + GWConstants.SLASH + Constants.TEMP_DIR);
								// if (!file.exists()) {
								// 	if (!file.mkdirs()) {
								// 		logger.error("Failed to create temp dir : {}", file.getAbsolutePath());
								// 		throw new RuntimeException(new RuntimeException());
								// 	}
								// }

								file = new File(disk.getPath() + GWConstants.SLASH + Constants.TRASH_DIR);
								if (!file.exists()) {
									if (!file.mkdirs()) {
										logger.error("Failed to create trash dir : {}", file.getAbsolutePath());
										throw new RuntimeException(new RuntimeException());
									}
								}
							}
						}
					}
				}

				isAppliedDiskpools = true;
				return;
			} else {
				logger.error("Failed to get diskpool from portal. status code : {}", response.getStatusLine().getStatusCode());
				throw new RuntimeException(new RuntimeException());
			}
		} catch (RuntimeException e) {
			PrintStack.logging(logger, e);
			throw new RuntimeException(e);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new RuntimeException(e);
		}
    }

	public void getS3Users() {
		try {
			HttpClient client = HttpClients
                .custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
								
			HttpGet getRequest = new HttpGet(GWConstants.HTTPS + agentConfig.getPortalIp() + GWConstants.COLON + agentConfig.getPortalPort() + GWConstants.PORTAL_REST_API_KSAN_USERS);
			getRequest.addHeader(GWConstants.AUTHORIZATION, agentConfig.getPortalKey());

			HttpResponse response = client.execute(getRequest);
			if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				String body = handler.handleResponse(response);
				logger.info("ksan users info from portal : {}", body);
				
				JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject)parser.parse(body);
                JSONObject jsonData = (JSONObject)jsonObject.get(S3User.DATA);
                JSONArray jsonItems = (JSONArray)jsonData.get(S3User.ITEMS);
				for (int i = 0; i < jsonItems.size(); i++) {
					JSONObject item = (JSONObject)jsonItems.get(i);
					JSONArray jsonUserDiskpools = (JSONArray)item.get(S3User.USER_DISK_POOLS);
					logger.info("jsonUserDiskpools : {}", jsonUserDiskpools.toString());
					S3User user = new S3User((String)item.get(S3User.USER_ID), 
											 (String)item.get(S3User.USER_NAME), 
											 (String)item.get(S3User.USER_EMAIL), 
											 (String)item.get(S3User.ACCESS_KEY), 
											 (String)item.get(S3User.ACCESS_SECRET),
											 jsonUserDiskpools);
					S3UserManager.getInstance().addUser(user);
				}
				S3UserManager.getInstance().printUsers();
				isAppliedUsers = true;
				return;
			} else {
				logger.error("Failed to get user from portal. status code : {}", response.getStatusLine().getStatusCode());
				throw new RuntimeException(new RuntimeException());
			}
		} catch (RuntimeException e) {
			PrintStack.logging(logger, e);
			throw new RuntimeException(e);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new RuntimeException(e);
		}
	}

	public void getS3Regions() {
		try {
			HttpClient client = HttpClients
                .custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
								
			HttpGet getRequest = new HttpGet(GWConstants.HTTPS + agentConfig.getPortalIp() + GWConstants.COLON + agentConfig.getPortalPort() + GWConstants.PORTAL_REST_API_KSAN_REGIONS);
			getRequest.addHeader(GWConstants.AUTHORIZATION, agentConfig.getPortalKey());

			HttpResponse response = client.execute(getRequest);
			if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				String body = handler.handleResponse(response);
				logger.info(" ksan regions info from portal : {}", body);
				
				JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject)parser.parse(body);
                JSONObject jsonData = (JSONObject)jsonObject.get(S3User.DATA);
                JSONArray jsonItems = (JSONArray)jsonData.get(S3User.ITEMS);
				for (int i = 0; i < jsonItems.size(); i++) {
					JSONObject item = (JSONObject)jsonItems.get(i);
					S3Region region = new S3Region((String)item.get(S3Region.NAME), 
											 (String)item.get(S3Region.ADDRESS), 
											 (int)((long)item.get(S3Region.PORT)), 
											 (int)((long)item.get(S3Region.SSL_PORT)), 
											 (String)item.get(S3Region.ACCESS_KEY),
											 (String)item.get(S3Region.ACCESS_SECRET));
					S3RegionManager.getInstance().addRegion(region);
				}
				S3RegionManager.getInstance().printRegions();

				return;
			} else {
				logger.error("Failed to get region from portal. status code : {}", response.getStatusLine().getStatusCode());
				throw new RuntimeException(new RuntimeException());
			}
		} catch (RuntimeException e) {
			PrintStack.logging(logger, e);
			throw new RuntimeException(e);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new RuntimeException(e);
		}
	}

	public void postGWEvent(boolean isStart) {
		try {
			HttpClient client = HttpClients
                .custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
			
			JSONObject eventData;
			eventData = new JSONObject();
			eventData.put(GWConstants.PORTAL_REST_API_KSAN_EVENT_ID, serviceId);
			if (isStart) {
				eventData.put(GWConstants.PORTAL_REST_API_KSAN_EVENT_TYPE, GWConstants.PORTAL_REST_API_KSAN_EVENT_START);
				eventData.put(GWConstants.PORTAL_REST_API_KSAN_EVENT_MESSAGE, GWConstants.EMPTY_STRING);
			} else {
				eventData.put(GWConstants.PORTAL_REST_API_KSAN_EVENT_TYPE, GWConstants.PORTAL_REST_API_KSAN_EVENT_STOP);
				eventData.put(GWConstants.PORTAL_REST_API_KSAN_EVENT_MESSAGE, GWConstants.PORTAL_REST_API_KSAN_EVENT_SIGTERM);
			}
			
			StringEntity entity = new StringEntity(eventData.toString(), ContentType.APPLICATION_JSON);
								
			HttpPost getRequest = new HttpPost(GWConstants.HTTPS + agentConfig.getPortalIp() + GWConstants.COLON + agentConfig.getPortalPort() + GWConstants.PORTAL_REST_API_KSAN_EVENT);
			getRequest.addHeader(GWConstants.AUTHORIZATION, agentConfig.getPortalKey());
			getRequest.setEntity(entity);

			HttpResponse response = client.execute(getRequest);
			if (response.getStatusLine().getStatusCode() == 200) {
				return;
			} else {
				logger.error("post gw event. response code : {}", response.getStatusLine().getStatusCode());
				throw new RuntimeException(new RuntimeException());
			}
		} catch (RuntimeException e) {
			PrintStack.logging(logger, e);
			throw new RuntimeException(e);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new RuntimeException(e);
		}
	}
}
