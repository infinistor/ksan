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

import com.pspace.ifs.ksan.gw.identity.S3User;
import com.pspace.ifs.ksan.gw.object.objmanager.ObjManagerHelper;
import com.pspace.ifs.ksan.libs.mq.MQCallback;
import com.pspace.ifs.ksan.libs.mq.MQReceiver;
import com.pspace.ifs.ksan.libs.mq.MQResponse;
import com.pspace.ifs.ksan.libs.mq.MQResponseType;
import com.pspace.ifs.ksan.libs.DiskManager;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.config.MonConfig;
import com.pspace.ifs.ksan.libs.disk.Disk;
import com.pspace.ifs.ksan.libs.disk.DiskPool;
import com.pspace.ifs.ksan.libs.disk.Server;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConfigUpdateCallback implements MQCallback{
	private static final Logger logger = LoggerFactory.getLogger(ConfigUpdateCallback.class);
	@Override
	public MQResponse call(String routingKey, String body) {
		logger.info(GWConstants.GWPORTAL_RECEIVED_CONFIG_CHANGE);
		logger.info(GWConstants.LOG_GWPORTAL_RECEIVED_MESSAGE_QUEUE_DATA, routingKey, body);

		GWPortal.getInstance().getConfig();
		// ObjManagerHelper.updateAllConfig();
		
		return new MQResponse(MQResponseType.SUCCESS, "", "", 0);
	}    
}

class DiskUpdateCallback implements MQCallback{
	private static final Logger logger = LoggerFactory.getLogger(DiskpoolsUpdateCallback.class);
	@Override
	public MQResponse call(String routingKey, String body) {
		logger.info(GWConstants.GWPORTAL_RECEIVED_DISK_CHANGE);
		logger.info(GWConstants.LOG_GWPORTAL_RECEIVED_MESSAGE_QUEUE_DATA, routingKey, body);

		GWPortal.getInstance().getDiskPoolsDetails();
		ObjManagerHelper.updateAllDiskpools(routingKey, body);

		return new MQResponse(MQResponseType.SUCCESS, "", "", 0);
	}
}

class DiskpoolsUpdateCallback implements MQCallback{
	private static final Logger logger = LoggerFactory.getLogger(DiskpoolsUpdateCallback.class);
	@Override
	public MQResponse call(String routingKey, String body) {
		logger.info(GWConstants.GWPORTAL_RECEIVED_DISKPOOLS_CHANGE);
		logger.info(GWConstants.LOG_GWPORTAL_RECEIVED_MESSAGE_QUEUE_DATA, routingKey, body);

		GWPortal.getInstance().getDiskPoolsDetails();
		ObjManagerHelper.updateAllDiskpools(routingKey, body);

		return new MQResponse(MQResponseType.SUCCESS, "", "", 0);
	}    
}

class UserUpdateCallBack implements MQCallback{
	private static final Logger logger = LoggerFactory.getLogger(DiskpoolsUpdateCallback.class);
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

		return new MQResponse(MQResponseType.SUCCESS, "", "", 0);
	}
}

public class GWPortal {
	private MonConfig config;

    private static final Logger logger = LoggerFactory.getLogger(GWPortal.class);

    public static GWPortal getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static final GWPortal INSTANCE = new GWPortal();
    }

    private GWPortal() {
        config = MonConfig.getInstance(); 
        config.configure();

        try
		{
			MQCallback configureCB = new ConfigUpdateCallback();
			MQReceiver mq1ton = new MQReceiver(config.getPortalIp(), 
											   GWConstants.MQUEUE_NAME_GW_CONFIG + config.getServerId(), 
											   GWConstants.MQUEUE_EXCHANGE_NAME, 
											   false,
											   "", 
											   GWConstants.MQUEUE_NAME_GW_CONFIG_ROUTING_KEY, 
											   configureCB);
			// mq1ton.addCallback(configureCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}

		try {
			MQCallback diskCB = new DiskUpdateCallback();
			MQReceiver mq1ton = new MQReceiver(config.getPortalIp(), 
											   GWConstants.MQUEUE_NAME_GW_DISK + config.getServerId(), 
											   GWConstants.MQUEUE_EXCHANGE_NAME, 
											   false, 
											   "", 
											   GWConstants.MQUEUE_NAME_GW_DISK_ADDED_ROUTING_KEY, 
											   diskCB);
			// mq1ton.addCallback(diskpoolsCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}

		try {
			MQCallback diskCB = new DiskUpdateCallback();
			MQReceiver mq1ton = new MQReceiver(config.getPortalIp(), 
											   GWConstants.MQUEUE_NAME_GW_DISK + config.getServerId(), 
											   GWConstants.MQUEUE_EXCHANGE_NAME, 
											   false, 
											   "", 
											   GWConstants.MQUEUE_NAME_GW_DISK_UPDATED_ROUTING_KEY, 
											   diskCB);
			// mq1ton.addCallback(diskpoolsCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}

		try {
			MQCallback diskCB = new DiskUpdateCallback();
			MQReceiver mq1ton = new MQReceiver(config.getPortalIp(), 
											   GWConstants.MQUEUE_NAME_GW_DISK + config.getServerId(), 
											   GWConstants.MQUEUE_EXCHANGE_NAME, 
											   false, 
											   "", 
											   GWConstants.MQUEUE_NAME_GW_DISK_REMOVED_ROUTING_KEY, 
											   diskCB);
			// mq1ton.addCallback(diskpoolsCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}

		try {
			MQCallback diskCB = new DiskUpdateCallback();
			MQReceiver mq1ton = new MQReceiver(config.getPortalIp(), 
											   GWConstants.MQUEUE_NAME_GW_DISK + config.getServerId(), 
											   GWConstants.MQUEUE_EXCHANGE_NAME, 
											   false, 
											   "", 
											   GWConstants.MQUEUE_NAME_GW_DISK_STATE_ROUTING_KEY, 
											   diskCB);
			// mq1ton.addCallback(diskpoolsCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}

		try {
			MQCallback diskCB = new DiskUpdateCallback();
			MQReceiver mq1ton = new MQReceiver(config.getPortalIp(), 
											   GWConstants.MQUEUE_NAME_GW_DISK + config.getServerId(), 
											   GWConstants.MQUEUE_EXCHANGE_NAME, 
											   false, 
											   "", 
											   GWConstants.MQUEUE_NAME_GW_DISK_RWMODE_ROUTING_KEY, 
											   diskCB);
			// mq1ton.addCallback(diskpoolsCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}

		try {
			MQCallback diskpoolsCB = new DiskpoolsUpdateCallback();
			MQReceiver mq1ton = new MQReceiver(config.getPortalIp(), 
											   GWConstants.MQUEUE_NAME_GW_DISKPOOL + config.getServerId(), 
											   GWConstants.MQUEUE_EXCHANGE_NAME, 
											   false, 
											   "", 
											   GWConstants.MQUEUE_NAME_GW_DISKPOOL_ROUTING_KEY, 
											   diskpoolsCB);
			// mq1ton.addCallback(diskpoolsCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}

		try {
			MQCallback userCB = new UserUpdateCallBack();
			MQReceiver mq1ton = new MQReceiver(config.getPortalIp(), 
											   GWConstants.MQUEUE_NAME_GW_USER + config.getServerId(), 
											   GWConstants.MQUEUE_EXCHANGE_NAME, 
											   false, 
											   "", 
											   GWConstants.MQUEUE_NAME_GW_USER_ROUTING_KEY, 
											   userCB);
			// mq1ton.addCallback(userCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}
    }

    public void getConfig() {
        try {
			HttpClient client = HttpClients
                .custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
								
			HttpGet getRequest = new HttpGet(GWConstants.HTTPS + config.getPortalIp() + GWConstants.COLON + config.getPortalPort() + GWConstants.PORTAL_REST_API_CONFIG_S3);
			getRequest.addHeader(GWConstants.AUTHORIZATION, config.getPortalKey());

			HttpResponse response = client.execute(getRequest);
			if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				String body = handler.handleResponse(response);
                logger.info(body);

                JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject)parser.parse(body);
                JSONObject jsonData = (JSONObject)jsonObject.get(MonConfig.DATA);
				String version = String.valueOf(jsonData.get(MonConfig.VERSION));
                String config = (String)jsonData.get(MonConfig.CONFIG);
                logger.info(config);

                JSONObject jsonConfig = (JSONObject)parser.parse(config);
                
                GWConfig.getInstance().setConfig(jsonConfig);
				GWConfig.getInstance().setVersion(version);
                GWConfig.getInstance().saveConfigFile();

                ObjectManagerConfig.getInstance().setConfig(jsonConfig);
				ObjectManagerConfig.getInstance().setVersion(version);
                ObjectManagerConfig.getInstance().saveConfigFile();
				return;
			}
			throw new RuntimeException(new RuntimeException());
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
								
			HttpGet getRequest = new HttpGet(GWConstants.HTTPS + config.getPortalIp() + GWConstants.COLON + config.getPortalPort() + GWConstants.PORTAL_REST_API_DISKPOOLS_DETAILS);
			getRequest.addHeader(GWConstants.AUTHORIZATION, config.getPortalKey());

			HttpResponse response = client.execute(getRequest);
			if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				String body = handler.handleResponse(response);
				logger.info(body);
                JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject)parser.parse(body);
				JSONObject jsonData = (JSONObject)jsonObject.get(DiskManager.DATA);
				JSONArray jsonItems = (JSONArray)jsonData.get(DiskManager.ITEMS);
				DiskManager.getInstance().clearDiskPoolList();

				for (int i = 0; i < jsonItems.size(); i++) {
					JSONObject item = (JSONObject)jsonItems.get(i);
					DiskPool diskPool = new DiskPool((String)item.get(DiskPool.ID), 
													 (String)item.get(DiskPool.NAME), 
													 (String)item.get(DiskPool.DISK_POOL_TYPE), 
													 (String)item.get(DiskPool.REPLICATION_TYPE));
					JSONArray jsonServers = (JSONArray)item.get(DiskPool.SERVERS);
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
				
				for (DiskPool diskpool : DiskManager.getInstance().getDiskPoolList()) {
					for (Server server : diskpool.getServerList()) {
						if (GWUtils.getLocalIP().equals(server.getIp())) {
							for (Disk disk : server.getDiskList()) {
								File file = new File(disk.getPath() + GWConstants.SLASH + GWConstants.OBJ_DIR);
								file.mkdirs();
								file = new File(disk.getPath() + GWConstants.SLASH + GWConstants.TEMP_DIR);
								file.mkdirs();
								file = new File(disk.getPath() + GWConstants.SLASH + GWConstants.TRASH_DIR);
								file.mkdirs();
							}
						}
					}
				}

				return;
			}
			throw new RuntimeException(new RuntimeException());
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
								
			HttpGet getRequest = new HttpGet(GWConstants.HTTPS + config.getPortalIp() + GWConstants.COLON + config.getPortalPort() + GWConstants.PORTAL_REST_API_KSAN_USERS);
			getRequest.addHeader(GWConstants.AUTHORIZATION, config.getPortalKey());

			HttpResponse response = client.execute(getRequest);
			if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				String body = handler.handleResponse(response);
				logger.info("Ksan Users : {}", body);
				
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
				return;
			}
			throw new RuntimeException(new RuntimeException());
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new RuntimeException(e);
		}
	}

	public S3User getS3User(String id) {
		try {
			HttpClient client = HttpClients
                .custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
								
			HttpGet getRequest = new HttpGet(GWConstants.HTTPS + config.getPortalIp() + GWConstants.COLON + config.getPortalPort() + GWConstants.PORTAL_REST_API_KSAN_USERS + GWConstants.SLASH + id);
			getRequest.addHeader(GWConstants.AUTHORIZATION, config.getPortalKey());

			HttpResponse response = client.execute(getRequest);
			if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				String body = handler.handleResponse(response);
				JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject)parser.parse(body);
                JSONObject jsonData = (JSONObject)jsonObject.get(S3User.DATA);
				JSONArray jsonUserDiskpools = (JSONArray)jsonData.get(S3User.USER_DISK_POOLS);
				S3User user = new S3User((String)jsonData.get(S3User.USER_ID), 
										 (String)jsonData.get(S3User.USER_NAME), 
										 (String)jsonData.get(S3User.USER_EMAIL), 
										 (String)jsonData.get(S3User.ACCESS_KEY), 
										 (String)jsonData.get(S3User.ACCESS_SECRET),
										 jsonUserDiskpools);
				logger.info(GWConstants.LOG_GWPORTAL_RECEIVED_USER_DATA, user.getUserId(), user.getUserName(), user.getUserEmail(), user.getAccessKey(), user.getAccessSecret());
				return user;
			}
			throw new RuntimeException(new RuntimeException());
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new RuntimeException(e);
		}
	}
}
