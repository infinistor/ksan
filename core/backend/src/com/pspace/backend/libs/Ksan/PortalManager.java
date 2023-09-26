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
package com.pspace.backend.libs.Ksan;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspace.backend.libs.Config.LifecycleManagerConfig;
import com.pspace.backend.libs.Config.LogManagerConfig;
import com.pspace.backend.libs.Config.ReplicationManagerConfig;
import com.pspace.backend.libs.Config.objManagerConfig;
import com.pspace.backend.libs.Data.Constants;
import com.pspace.backend.libs.Ksan.Data.ResponseConfig;
import com.pspace.backend.libs.Ksan.Data.ResponseData;
import com.pspace.backend.libs.Ksan.Data.ResponseList;
import com.pspace.backend.libs.Ksan.Data.ResponseRegion;
import com.pspace.backend.libs.Ksan.Data.S3RegionData;
import com.pspace.ifs.ksan.objmanager.ObjManagerConfig;

public class PortalManager {
	static final Logger logger = LoggerFactory.getLogger(PortalManager.class);
	private AgentConfig Config;

	private List<S3RegionData> regions = null;

	public static PortalManager getInstance() {
		return LazyHolder.INSTANCE;
	}

	private static class LazyHolder {
		private static final PortalManager INSTANCE = new PortalManager();
	}

	private PortalManager() {
		Config = AgentConfig.getInstance();
	}
	
	/**
	 * 현재 시스템의 Region 정보를 가져온다.
	 * 
	 * @return Region 정보
	 */
	public S3RegionData getLocalRegion() {
		try (var Client = getClient();) {

			var get = getRequest(getLocalRegionURL());
			var Response = Client.execute(get);
			if (Response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				var Body = handler.handleResponse(Response);
				logger.debug("Body : {}", Body);

				var Mapper = new ObjectMapper();
				var Result = Mapper.readValue(Body, new TypeReference<ResponseData<ResponseRegion>>() {
				});

				if (!Result.Code.isEmpty()) {
					logger.error("{}, {}", Result.Code, Result.Message);
					return null;
				}

				return new S3RegionData(Result.Data);
			}

		} catch (Exception e) {
			logger.error("", e);
		}
		return null;
	}

	/**
	 * ObjectManager 설정 정보를 가져온다.
	 * 
	 * @return ObjectManager 설정 정보
	 */
	public ObjManagerConfig getObjManagerConfig() {
		try (var Client = getClient();) {

			var get = getRequest(getObjManagerConfigURL());
			var Response = Client.execute(get);
			if (Response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				var Body = handler.handleResponse(Response);
				logger.debug("Body : {}", Body);

				var Mapper = new ObjectMapper();
				var Result = Mapper.readValue(Body, new TypeReference<ResponseData<ResponseConfig>>() {
				});

				if (!Result.Code.isEmpty()) {
					logger.error("{}, {}", Result.Code, Result.Message);
					return null;
				}

				var objManager = Mapper.readValue(Result.Data.Config, objManagerConfig.class);
				return objManager.getObjManagerConfig();
			}

		} catch (Exception e) {
			logger.error("", e);
		}
		return null;
	}

	/**
	 * Replication 설정 정보를 가져온다.
	 * 
	 * @return Replication 설정 정보
	 */
	public ReplicationManagerConfig getReplicationManagerConfig() {
		try (var Client = getClient();) {

			var get = getRequest(getReplicationConfigURL());
			var Response = Client.execute(get);
			if (Response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				var Body = handler.handleResponse(Response);
				logger.debug("Body : {}", Body);

				var Mapper = new ObjectMapper();
				var Result = Mapper.readValue(Body, new TypeReference<ResponseData<ResponseConfig>>() {
				});

				if (!Result.Code.isEmpty()) {
					logger.error("{}, {}", Result.Code, Result.Message);
					return null;
				}

				return Mapper.readValue(Result.Data.Config, ReplicationManagerConfig.class);
			}

		} catch (Exception e) {
			logger.error("", e);
		}
		return null;
	}

	/**
	 * LogManager 설정 정보를 가져온다.
	 * 
	 * @return LogManager 설정 정보
	 */
	public LogManagerConfig getLogManagerConfig() {
		try (var Client = getClient();) {

			var get = getRequest(getLogManagerConfigURL());
			var Response = Client.execute(get);
			if (Response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				var Body = handler.handleResponse(Response);
				logger.debug("Body : {}", Body);

				var Mapper = new ObjectMapper();
				var Result = Mapper.readValue(Body, new TypeReference<ResponseData<ResponseConfig>>() {
				});

				if (!Result.Code.isEmpty()) {
					logger.error("{}, {}", Result.Code, Result.Message);
					return null;
				}

				return Mapper.readValue(Result.Data.Config, LogManagerConfig.class);
			}

		} catch (Exception e) {
			logger.error("", e);
		}
		return null;
	}

	/**
	 * LifecycleManager 설정 정보를 가져온다.
	 * 
	 * @return LifecycleManager 설정 정보
	 */
	public LifecycleManagerConfig getLifecycleManagerConfig() {
		try (var Client = getClient();) {

			var get = getRequest(getLifecycleManagerConfigURL());
			var Response = Client.execute(get);
			if (Response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				var Body = handler.handleResponse(Response);
				logger.debug("Body : {}", Body);

				var Mapper = new ObjectMapper();
				var Result = Mapper.readValue(Body, new TypeReference<ResponseData<ResponseConfig>>() {
				});

				if (!Result.Code.isEmpty()) {
					logger.error("{}, {}", Result.Code, Result.Message);
					return null;
				}

				return Mapper.readValue(Result.Data.Config, LifecycleManagerConfig.class);
			}

		} catch (Exception e) {
			logger.error("", e);
		}
		return null;
	}

	/**
	 * Portal에 등록된 모든 Region 정보를 가져온다.
	 * 
	 * @return Region 정보
	 */
	public boolean RegionUpdate() {
		try (var Client = getClient();) {

			var get = getRequest(getRegionsURL());
			var Response = Client.execute(get);

			if (Response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				var Body = handler.handleResponse(Response);

				var Mapper = new ObjectMapper();
				var Result = Mapper.readValue(Body, new TypeReference<ResponseList<ResponseRegion>>() {
				});

				if (!Result.Code.isEmpty()) {
					logger.error("region is empty. [{}, {}]", Result.Code, Result.Message);
					return false;
				}

				if (regions == null) {
					regions = new ArrayList<S3RegionData>();
					for (var Region : Result.Data.Items)
						regions.add(new S3RegionData(Region));
				}
				else{
					for (var Region : Result.Data.Items) {
						var data = new S3RegionData(Region);
						var find = regions.stream().filter(f -> f.name.equals(data.name)).findAny().orElse(null);
						if (find == null) {
							regions.add(data);
						} else {
							find.update(data);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("", e);
			return false;
		}
		return true;
	}

	/**
	 * URL을 바탕으로 리전정보를 조회한다.
	 * 
	 * @param Address 찾을 리전 주소
	 * @return 리전 정보
	 */
	public S3RegionData get2Address(String Address) {
		if (regions == null || regions.size() == 0 || StringUtils.isBlank(Address))
			return null;
		return regions.stream().filter(f -> Address.equals(f.address)).findAny().orElse(null);
	}

	/**
	 * 리전명을 바탕으로 리전정보를 조회한다.
	 * 
	 * @param RegionName 찾을 리전 명
	 * @return 리전 정보
	 */
	public S3RegionData getRegion(String RegionName) {
		if (regions == null || regions.size() == 0 || StringUtils.isBlank(RegionName))
			return null;
		return regions.stream().filter(f -> RegionName.equals(f.name)).findAny().orElse(null);
	}

	private String getURL() {
		return String.format("https://%s:%s", Config.PortalHost, Config.PortalPort);
	}

	// private String getBackendConfigURL() {
	// return String.format("%s/api/v1/Config/KsanS3Backend", getURL());
	// }

	private String getObjManagerConfigURL() {
		return getURL()+ Constants.URL_OBJ_MANAGER_CONFIG;
	}

	private String getLifecycleManagerConfigURL() {
		return getURL()+ Constants.URL_LIFECYCLE_MANAGER_CONFIG;
	}

	private String getReplicationConfigURL() {
		return getURL()+ Constants.URL_REPLICATION_MANAGER_CONFIG;
	}

	private String getLogManagerConfigURL() {
		return getURL()+ Constants.URL_LOG_MANAGER_CONFIG;
	}

	private String getLocalRegionURL() {
		return getURL()+ Constants.URL_LOCAL_REGION;
	}

	private String getRegionsURL() {
		return getURL()+ Constants.URL_REGION;
	}

	// private String getRegionURL(String RegionName) {
	// return String.format("%s/api/v1/Regions/%s", getURL(), RegionName);
	// }

	private HttpGet getRequest(String URL) {
		var Request = new HttpGet(URL);
		Request.addHeader("Authorization", Config.APIKey);
		return Request;
	}

	private CloseableHttpClient getClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		return HttpClients.custom()
				.setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
				.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.build();
	}
}
