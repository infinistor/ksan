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
package com.pspace.ifs.ksan.osd;

import com.pspace.ifs.ksan.osd.utils.OSDConfig;
import com.pspace.ifs.ksan.osd.utils.OSDConstants;
import com.pspace.ifs.ksan.osd.utils.OSDUtils;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.libs.DiskManager;
import com.pspace.ifs.ksan.libs.HeartbeatManager;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.config.AgentConfig;
import com.pspace.ifs.ksan.libs.disk.Disk;
import com.pspace.ifs.ksan.libs.disk.DiskPool;
import com.pspace.ifs.ksan.libs.disk.Server;
import com.pspace.ifs.ksan.libs.mq.MQCallback;
import com.pspace.ifs.ksan.libs.mq.MQReceiver;
import com.pspace.ifs.ksan.libs.mq.MQResponse;
import com.pspace.ifs.ksan.libs.mq.MQResponseType;
import com.pspace.ifs.ksan.libs.mq.MQResponseCode;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;

import org.apache.http.ssl.SSLContextBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class ConfigUpdateCallback implements MQCallback{
	private static final Logger logger = LoggerFactory.getLogger(ConfigUpdateCallback.class);
	@Override
	public MQResponse call(String routingKey, String body) {
		logger.info("receive agentConfig change ...");
		logger.info("BiningKey : {}, body : {}}", routingKey, body);

		OSDPortal.getInstance().getConfig();
		
		return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
	}
}

class DiskpoolsUpdateCallback implements MQCallback{
	private static final Logger logger = LoggerFactory.getLogger(DiskpoolsUpdateCallback.class);
	@Override
	public MQResponse call(String routingKey, String body) {
		try {
			logger.info("receive diskpools change ...");
			logger.info("BiningKey : {}, body : {}}", routingKey, body);
	
			if (OSDPortal.getInstance().isAppliedDiskpools()) {
				OSDPortal.getInstance().getDiskPoolsDetails();
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
		}
	}    
}

public class OSDPortal {
	private boolean isAppliedDiskpools;
    private AgentConfig agentConfig;
	private String serviceId;

    private static final Logger logger = LoggerFactory.getLogger(OSDPortal.class);

    public static OSDPortal getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static final OSDPortal INSTANCE = new OSDPortal();
    }
    
    private OSDPortal() {
        agentConfig = AgentConfig.getInstance(); 
        agentConfig.configure();
		logger.debug("ksan monitor agentConfig ...");
		int mqPort = Integer.parseInt(agentConfig.getMQPort());

		// serviceId
		try {
			BufferedReader reader = new BufferedReader(new FileReader(System.getProperty(Constants.OSD_SERVICEID_KEY) + File.separator + Constants.OSD_SERVICEID_FILE, StandardCharsets.UTF_8));
			serviceId = reader.readLine();
			logger.info("serviceId : {}", serviceId);
			reader.close();
		} catch (IOException e) {
			PrintStack.logging(logger, e);
			System.exit(1);
		}

		postGWEvent(true);

		try {
			HeartbeatManager heartbeatManager = new HeartbeatManager(serviceId, agentConfig.getMQHost(), mqPort, agentConfig.getMQUser(), agentConfig.getMQPassword(), OSDConstants.MQUEUE_EXCHANGE_NAME);
			heartbeatManager.startHeartbeat(agentConfig.getServiceMonitorInterval());
		} catch (IOException e) {
			PrintStack.logging(logger, e);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
		}

        try {
			MQCallback configureCB = new ConfigUpdateCallback();
			MQReceiver mq1ton = new MQReceiver(agentConfig.getMQHost(), 
				mqPort,
				agentConfig.getMQUser(),
				agentConfig.getMQPassword(),
				OSDConstants.MQUEUE_NAME_OSD_CONFIG + serviceId, //agentConfig.getServerId(), 
				OSDConstants.MQUEUE_EXCHANGE_NAME, 
				false, 
				"fanout", 
				OSDConstants.MQUEUE_NAME_OSD_CONFIG_ROUTING_KEY, 
				configureCB);
			mq1ton.addCallback(configureCB);
		} catch (Exception ex){
			throw new RuntimeException(ex);
		}

		try {
			MQCallback diskpoolsCB = new DiskpoolsUpdateCallback();
			MQReceiver mq1ton = new MQReceiver(agentConfig.getMQHost(), 
				mqPort,
				agentConfig.getMQUser(),
				agentConfig.getMQPassword(),
				OSDConstants.MQUEUE_NAME_OSD_DISKPOOL + serviceId, //agentConfig.getServerId(), 
				OSDConstants.MQUEUE_EXCHANGE_NAME, 
				false, 
				"fanout", 
				OSDConstants.MQUEUE_NAME_OSD_DISKPOOL_ROUTING_KEY, 
				diskpoolsCB);
			mq1ton.addCallback(diskpoolsCB);
		} catch (Exception ex){
			throw new RuntimeException(ex);
		}
		logger.debug("mq registered ...");
		isAppliedDiskpools = false;
    }

	public boolean isAppliedDiskpools() {
		return isAppliedDiskpools;
	}

    public void getConfig() {
        try {
			logger.info("get agentConfig ...");
			HttpClient client = HttpClients
                .custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
			
			HttpGet getRequest = new HttpGet(OSDConstants.HTTPS + agentConfig.getPortalIp() + OSDConstants.COLON + agentConfig.getPortalPort() + OSDConstants.PORTAL_REST_API_CONFIG_OSD);
			getRequest.addHeader(OSDConstants.AUTHORIZATION, agentConfig.getPortalKey());
            logger.info("{}", agentConfig.getPortalIp());
            logger.info("{}", agentConfig.getPortalPort());
            logger.info("portal key : {}", agentConfig.getPortalKey());
			HttpResponse response = client.execute(getRequest);
            logger.info("Response : {}", response.getStatusLine().toString());
			if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				String body = handler.handleResponse(response);
                logger.info(body);

                JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject)parser.parse(body);
                JSONObject jsonData = (JSONObject)jsonObject.get(AgentConfig.DATA);
				String version = String.valueOf(jsonData.get(AgentConfig.VERSION));
                String agentConfig = (String)jsonData.get(AgentConfig.CONFIG);
                
                logger.info(agentConfig);

                JSONObject jsonConfig = (JSONObject)parser.parse(agentConfig);
                OSDConfig.getInstance().setConfig(jsonConfig);
                OSDConfig.getInstance().setVersion(version);
                OSDConfig.getInstance().saveConfigFile();

				OSDServer.startEmptyTrash();
				OSDServer.startMoveCacheToDisk();
				return;
			}
			throw new RuntimeException(new RuntimeException());
		} catch (RuntimeException e) {
			OSDUtils.logging(logger, e);
			throw new RuntimeException(e);
		} catch (Exception e) {
			OSDUtils.logging(logger, e);
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
								
			HttpGet getRequest = new HttpGet(OSDConstants.HTTPS + agentConfig.getPortalIp() + OSDConstants.COLON + agentConfig.getPortalPort() + OSDConstants.PORTAL_REST_API_DISKPOOLS_DETAILS);
			getRequest.addHeader(OSDConstants.AUTHORIZATION, agentConfig.getPortalKey());

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
					// DiskPool diskPool = new DiskPool((String)item.get(DiskPool.ID), (String)item.get(DiskPool.NAME), (String)item.get(DiskPool.DISK_POOL_TYPE), (String)item.get(DiskPool.REPLICATION_TYPE));
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
				isAppliedDiskpools = true;
				return;
			}
			throw new RuntimeException(new RuntimeException());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }

	public String getServiceId() {
		return serviceId;
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
			eventData.put(OSDConstants.PORTAL_REST_API_KSAN_EVENT_ID, serviceId);
			if (isStart) {
				eventData.put(OSDConstants.PORTAL_REST_API_KSAN_EVENT_TYPE, OSDConstants.PORTAL_REST_API_KSAN_EVENT_START);
				eventData.put(OSDConstants.PORTAL_REST_API_KSAN_EVENT_MESSAGE, OSDConstants.EMPTY_STRING);
			} else {
				eventData.put(OSDConstants.PORTAL_REST_API_KSAN_EVENT_TYPE, OSDConstants.PORTAL_REST_API_KSAN_EVENT_STOP);
				eventData.put(OSDConstants.PORTAL_REST_API_KSAN_EVENT_MESSAGE, OSDConstants.PORTAL_REST_API_KSAN_EVENT_SIGTERM);
			}
			
			StringEntity entity = new StringEntity(eventData.toString(), ContentType.APPLICATION_JSON);
								
			HttpPost getRequest = new HttpPost(OSDConstants.HTTPS + agentConfig.getPortalIp() + OSDConstants.COLON + agentConfig.getPortalPort() + OSDConstants.PORTAL_REST_API_KSAN_EVENT);
			getRequest.addHeader(OSDConstants.AUTHORIZATION, agentConfig.getPortalKey());
			getRequest.setEntity(entity);

			HttpResponse response = client.execute(getRequest);
			if (response.getStatusLine().getStatusCode() == 200) {
				return;
			} else {
				logger.error("response code : {}", response.getStatusLine().getStatusCode());
			}
			throw new RuntimeException(new RuntimeException());
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new RuntimeException(e);
		}
	}
}
