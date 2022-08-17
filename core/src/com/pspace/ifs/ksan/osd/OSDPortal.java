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
import com.pspace.ifs.ksan.libs.DiskManager;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.config.MonConfig;
import com.pspace.ifs.ksan.libs.disk.Disk;
import com.pspace.ifs.ksan.libs.disk.DiskPool;
import com.pspace.ifs.ksan.libs.disk.Server;
import com.pspace.ifs.ksan.libs.mq.MQCallback;
import com.pspace.ifs.ksan.libs.mq.MQReceiver;
import com.pspace.ifs.ksan.libs.mq.MQResponse;
import com.pspace.ifs.ksan.libs.mq.MQResponseType;

import com.google.common.base.Strings;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConfigUpdateCallback implements MQCallback{
	private static final Logger logger = LoggerFactory.getLogger(ConfigUpdateCallback.class);
	@Override
	public MQResponse call(String routingKey, String body) {
		logger.info("receive config change ...");
		logger.info("BiningKey : {}, body : {}}", routingKey, body);

		OSDPortal.getInstance().getConfig();
		
		return new MQResponse(MQResponseType.SUCCESS, "", "", 0);
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
			return new MQResponse(MQResponseType.SUCCESS, "", "", 0);
		}
	}    
}

public class OSDPortal {
	private boolean isAppliedDiskpools;
    private MonConfig config;

    private static final Logger logger = LoggerFactory.getLogger(OSDPortal.class);

    public static OSDPortal getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static final OSDPortal INSTANCE = new OSDPortal();
    }
    
    private OSDPortal() {
        config = MonConfig.getInstance(); 
        config.configure();
		logger.debug("ksan monitor config ...");
		int mqPort = Integer.parseInt(config.getMqPort());
		if (Strings.isNullOrEmpty(config.getServerId())) {
			logger.error("mq server id is null or empty ...");
			throw new RuntimeException(new RuntimeException());
		}

        try
		{
			MQCallback configureCB = new ConfigUpdateCallback();
			MQReceiver mq1ton = new MQReceiver(config.getPortalIp(), 
				mqPort,
				config.getMqUser(),
				config.getMqPassword(),
				OSDConstants.MQUEUE_NAME_OSD_CONFIG + config.getServerId(), 
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
			MQReceiver mq1ton = new MQReceiver(config.getPortalIp(), 
				mqPort,
				config.getMqUser(),
				config.getMqPassword(),
				OSDConstants.MQUEUE_NAME_OSD_DISKPOOL + config.getServerId(), 
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
			logger.info("get config ...");
			HttpClient client = HttpClients
                .custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
			
			HttpGet getRequest = new HttpGet(OSDConstants.HTTPS + config.getPortalIp() + OSDConstants.COLON + config.getPortalPort() + OSDConstants.PORTAL_REST_API_CONFIG_OSD);
			getRequest.addHeader(OSDConstants.AUTHORIZATION, config.getPortalKey());
            logger.info("{}", config.getPortalIp());
            logger.info("{}", config.getPortalPort());
            logger.info("portal key : {}", config.getPortalKey());
			HttpResponse response = client.execute(getRequest);
            logger.info("Response : {}", response.getStatusLine().toString());
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
                OSDConfig.getInstance().setConfig(jsonConfig);
                OSDConfig.getInstance().setVersion(version);
                OSDConfig.getInstance().saveConfigFile();

				OSDServer.startEmptyTrash();
				OSDServer.startMoveCacheToDisk();
				return;
			}
			throw new RuntimeException(new RuntimeException());
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
								
			HttpGet getRequest = new HttpGet(OSDConstants.HTTPS + config.getPortalIp() + OSDConstants.COLON + config.getPortalPort() + OSDConstants.PORTAL_REST_API_DISKPOOLS_DETAILS);
			getRequest.addHeader(OSDConstants.AUTHORIZATION, config.getPortalKey());

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
					DiskPool diskPool = new DiskPool((String)item.get(DiskPool.ID), (String)item.get(DiskPool.NAME), (String)item.get(DiskPool.DISK_POOL_TYPE), (String)item.get(DiskPool.REPLICATION_TYPE));
					JSONArray jsonServers = (JSONArray)item.get(DiskPool.SERVERS);
					if (jsonServers != null && jsonServers.size() == 0) {
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
}
