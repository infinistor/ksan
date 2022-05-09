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

import com.pspace.ifs.ksan.gw.GWMain;
import com.pspace.ifs.ksan.gw.identity.S3User;
import com.pspace.ifs.ksan.gw.object.objmanager.ObjManagerHelper;
import com.pspace.ifs.ksan.gw.utils.disk.Disk;
import com.pspace.ifs.ksan.gw.utils.disk.DiskPool;
import com.pspace.ifs.ksan.gw.utils.disk.Server;
import com.pspace.ifs.ksan.mq.MQCallback;
import com.pspace.ifs.ksan.mq.MQReceiver;
import com.pspace.ifs.ksan.mq.MQResponse;
import com.pspace.ifs.ksan.mq.MQResponseType;

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
		logger.info("receive config change ...");
		logger.info("BiningKey : {}, body : {}}", routingKey, body);

		Portal.getInstance().getConfig();
		ObjManagerHelper.updateAllConfig();
		
		return new MQResponse(MQResponseType.SUCCESS, "", "", 0);
	}    
}

class DiskpoolsUpdateCallback implements MQCallback{
	private static final Logger logger = LoggerFactory.getLogger(DiskpoolsUpdateCallback.class);
	@Override
	public MQResponse call(String routingKey, String body) {
		logger.info("receive diskpools change ...");
		logger.info("BiningKey : {}, body : {}}", routingKey, body);

		Portal.getInstance().getDiskPoolsDetails();
		ObjManagerHelper.updateAllDiskpools();

		return new MQResponse(MQResponseType.SUCCESS, "", "", 0);
	}    
}

class UserUpdateCallBack implements MQCallback{
	private static final Logger logger = LoggerFactory.getLogger(DiskpoolsUpdateCallback.class);
	@Override
	public MQResponse call(String routingKey, String body) {
		logger.info("receive s3user change ...");
		logger.info("BiningKey : {}, body : {}}", routingKey, body);
		JSONParser parser = new JSONParser();
		JSONObject data = null;
		try {
			data = (JSONObject)parser.parse(body);
		} catch (ParseException e) {
			PrintStack.logging(logger, e);
		}
		
		if (routingKey.contains("added")) {
			S3User user = new S3User((String)data.get("Id"), (String)data.get("Name"), (String)data.get("Email"), (String)data.get("AccessKey"), (String)data.get("SecretKey"));
			S3UserManager.getInstance().addUser(user);
			logger.info("Id:{}, Name:{}, Email:{}, AccessKey:{}, SecretKey:{}", user.getUserId(), user.getUserName(), user.getUserEmail(), user.getAccessKey(), user.getAccessSecret());
			S3UserManager.getInstance().printUsers();
		} else if (routingKey.contains("updated")) {
			S3User user = S3UserManager.getInstance().getUserById((String)data.get("Id"));
			S3UserManager.getInstance().removeUser(user);
			user = new S3User((String)data.get("Id"), (String)data.get("Name"), (String)data.get("Email"), (String)data.get("AccessKey"), (String)data.get("SecretKey"));
			S3UserManager.getInstance().addUser(user);
			logger.info("Id:{}, Name:{}, Email:{}, AccessKey:{}, SecretKey:{}", user.getUserId(), user.getUserName(), user.getUserEmail(), user.getAccessKey(), user.getAccessSecret());
			S3UserManager.getInstance().printUsers();
		} else if (routingKey.contains("removed")) {
			S3User user = S3UserManager.getInstance().getUserById((String)data.get("Id"));
			S3UserManager.getInstance().removeUser(user);
			S3UserManager.getInstance().printUsers();
		} else {
			logger.info("wrong routingKey : {}", routingKey);
		}

		return new MQResponse(MQResponseType.SUCCESS, "", "", 0);
	}
}

public class Portal {
	private MonConfig config;

    private static final Logger logger = LoggerFactory.getLogger(Portal.class);

    public static Portal getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static final Portal INSTANCE = new Portal();
    }

    private Portal() {
        config = new MonConfig(); 
        config.configure();

        try
		{
			MQCallback configureCB = new ConfigUpdateCallback();
			MQReceiver mq1ton = new MQReceiver(config.getPortalIp(), GWConstants.MQUEUE_NAME_GW_CONFIG, GWConstants.MQUEUE_EXCHANGE_NAME, false, "fanout", GWConstants.MQUEUE_NAME_GW_CONFIG_ROUTING_KEY, configureCB);
			mq1ton.addCallback(configureCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}

		try {
			MQCallback diskpoolsCB = new DiskpoolsUpdateCallback();
			MQReceiver mq1ton = new MQReceiver(config.getPortalIp(), GWConstants.MQUEUE_NAME_GW_DISKPOOL, GWConstants.MQUEUE_EXCHANGE_NAME, false, "fanout", GWConstants.MQUEUE_NAME_GW_DISKPOOL_ROUTING_KEY, diskpoolsCB);
			mq1ton.addCallback(diskpoolsCB);
		} catch (Exception ex){
			PrintStack.logging(logger, ex);
		}

		try {
			MQCallback userCB = new UserUpdateCallBack();
			MQReceiver mq1ton = new MQReceiver(config.getPortalIp(), GWConstants.MQUEUE_NAME_GW_USER, GWConstants.MQUEUE_EXCHANGE_NAME, false, "fanout", GWConstants.MQUEUE_NAME_GW_USER_ROUTING_KEY, userCB);
			mq1ton.addCallback(userCB);
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
                JSONObject jsonData = (JSONObject)jsonObject.get("Data");
				String version = String.valueOf(jsonData.get("Version"));
                String config = (String)jsonData.get("Config");
                logger.info(config);

                JSONObject jsonConfig = (JSONObject)parser.parse(config);
                
                JSONObject jsonGW = (JSONObject)jsonConfig.get("gw");
                GWConfig.getInstance().setConfig(jsonGW);
				GWConfig.getInstance().setVersion(version);
                GWConfig.getInstance().saveConfigFile();

                JSONObject jsonObjM = (JSONObject)jsonConfig.get("objmanager");
                ObjectManagerConfig.getInstance().setConfig(jsonObjM);
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
				JSONObject jsonData = (JSONObject)jsonObject.get("Data");
				JSONArray jsonItems = (JSONArray)jsonData.get("Items");
				DiskManager.getInstance().clearDiskPoolList();

				for (int i = 0; i < jsonItems.size(); i++) {
					JSONObject item = (JSONObject)jsonItems.get(i);
					DiskPool diskPool = new DiskPool((String)item.get("Id"), (String)item.get("Name"), (String)item.get("ClassTypeId"), (String)item.get("ReplicationType"));
					JSONArray jsonServers = (JSONArray)item.get("Servers");
					for (int j = 0; j < jsonServers.size(); j++) {
						JSONObject jsonServer = (JSONObject)jsonServers.get(j);
						JSONArray jsonNetwork = (JSONArray)jsonServer.get("NetworkInterfaces");
						String ipAddress = null;
						for (int k = 0; k < jsonNetwork.size(); k++) {
							JSONObject network = (JSONObject)jsonNetwork.get(k);
							ipAddress = (String)network.get("IpAddress");
						}
						JSONArray jsonDisks = (JSONArray)jsonServer.get("Disks");
						Server server = new Server((String)jsonServer.get("Id"), ipAddress, (String)jsonServer.get("State"));
						for (int l = 0; l < jsonDisks.size(); l++) {
							JSONObject jsonDisk = (JSONObject)jsonDisks.get(l);
							Disk disk = new Disk((String)jsonDisk.get("Id"), (String)jsonDisk.get("RwMode"), (String)jsonDisk.get("Path"), (String)jsonDisk.get("State"));
							server.addDisk(disk);
						}
						diskPool.addServer(server);
					}
					DiskManager.getInstance().addDiskPool(diskPool);
				}
				DiskManager.getInstance().configure();
				DiskManager.getInstance().saveFile();
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
								
			HttpGet getRequest = new HttpGet(GWConstants.HTTPS + config.getPortalIp() + GWConstants.COLON + config.getPortalPort() + GWConstants.PORTAL_REST_API_S3USERS);
			getRequest.addHeader(GWConstants.AUTHORIZATION, config.getPortalKey());

			HttpResponse response = client.execute(getRequest);
			if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				String body = handler.handleResponse(response);
				
				JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject)parser.parse(body);
                JSONObject jsonData = (JSONObject)jsonObject.get("Data");
                JSONArray jsonItems = (JSONArray)jsonData.get("Items");
				for (int i = 0; i < jsonItems.size(); i++) {
					JSONObject item = (JSONObject)jsonItems.get(i);
					S3User user = new S3User((String)item.get("Id"), (String)item.get("Name"), (String)item.get("Email"), (String)item.get("AccessKey"), (String)item.get("SecretKey"));
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
								
			HttpGet getRequest = new HttpGet(GWConstants.HTTPS + config.getPortalIp() + GWConstants.COLON + config.getPortalPort() + GWConstants.PORTAL_REST_API_S3USERS + GWConstants.SLASH + id);
			getRequest.addHeader(GWConstants.AUTHORIZATION, config.getPortalKey());

			HttpResponse response = client.execute(getRequest);
			if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				String body = handler.handleResponse(response);
				JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject)parser.parse(body);
                JSONObject jsonData = (JSONObject)jsonObject.get("Data");

				S3User user = new S3User((String)jsonData.get("Id"), (String)jsonData.get("Name"), (String)jsonData.get("Email"), (String)jsonData.get("AccessKey"), (String)jsonData.get("SecretKey"));
				logger.info("get Portal user info -> Id:{}, Name:{}, Email:{}, AccessKey:{}, SecretKey:{}", user.getUserId(), user.getUserName(), user.getUserEmail(), user.getAccessKey(), user.getAccessSecret());
				return user;
			}
			throw new RuntimeException(new RuntimeException());
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new RuntimeException(e);
		}
	}
}
