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
import com.pspace.backend.libs.Ksan.Data.ResponseConfig;
import com.pspace.backend.libs.Ksan.Data.ResponseData;
import com.pspace.backend.libs.Ksan.Data.ResponseList;
import com.pspace.backend.libs.Ksan.Data.ResponseRegion;
import com.pspace.backend.libs.Ksan.Data.S3RegionData;
import com.pspace.backend.libs.config.DBConfig;
import com.pspace.backend.libs.config.LifecycleManagerConfig;
import com.pspace.backend.libs.config.LogManagerConfig;
import com.pspace.backend.libs.config.ObjManagerConfigBuilder;
import com.pspace.backend.libs.config.ReplicationManagerConfig;
import com.pspace.backend.libs.data.Constants;
import com.pspace.ifs.ksan.objmanager.ObjManagerConfig;

public class PortalManager {
	static final Logger logger = LoggerFactory.getLogger(PortalManager.class);
	private AgentConfig config;

	private List<S3RegionData> regions = null;

	public static PortalManager getInstance() {
		return LazyHolder.INSTANCE;
	}

	private static class LazyHolder {
		private static final PortalManager INSTANCE = new PortalManager();
	}

	private PortalManager() {
		config = AgentConfig.getInstance();
	}

	/**
	 * 현재 시스템의 Region 정보를 가져온다.
	 * 
	 * @return Region 정보
	 */
	public S3RegionData getLocalRegion() {
		try (var client = getClient();) {

			var get = getRequest(getLocalRegionURL());
			var response = client.execute(get);
			if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				var body = handler.handleResponse(response);
				logger.debug("Body : {}", body);

				var mapper = new ObjectMapper();
				var result = mapper.readValue(body, new TypeReference<ResponseData<ResponseRegion>>() {
				});

				if (!result.code.isEmpty()) {
					logger.error("{}, {}", result.code, result.message);
					return null;
				}

				return new S3RegionData(result.data);
			}

		} catch (Exception e) {
			logger.error("", e);
		}
		return null;
	}

	/**
	 * MariaDB 설정 정보를 가져온다.
	 * 
	 * @return
	 */
	public DBConfig getDBConfig() {
		try (var client = getClient();) {

			var get = getRequest(getMariaDBConfigURL());
			var response = client.execute(get);
			if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				var body = handler.handleResponse(response);
				logger.debug("Body : {}", body);

				var mapper = new ObjectMapper();
				var result = mapper.readValue(body, new TypeReference<ResponseData<ResponseConfig>>() {
				});

				if (!result.code.isEmpty()) {
					logger.error("{}, {}", result.code, result.message);
					return null;
				}

				return mapper.readValue(result.data.config, DBConfig.class);
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
		try (var client = getClient();) {

			var get = getRequest(getObjManagerConfigURL());
			var response = client.execute(get);
			if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				var body = handler.handleResponse(response);
				logger.debug("Body : {}", body);

				var mapper = new ObjectMapper();
				var result = mapper.readValue(body, new TypeReference<ResponseData<ResponseConfig>>() {
				});

				if (!result.code.isEmpty()) {
					logger.error("{}, {}", result.code, result.message);
					return null;
				}

				var objManager = mapper.readValue(result.data.config, ObjManagerConfigBuilder.class);
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
		try (var client = getClient();) {

			var get = getRequest(getReplicationConfigURL());
			var response = client.execute(get);
			if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				var body = handler.handleResponse(response);
				logger.debug("Body : {}", body);

				var mapper = new ObjectMapper();
				var result = mapper.readValue(body, new TypeReference<ResponseData<ResponseConfig>>() {
				});

				if (!result.code.isEmpty()) {
					logger.error("{}, {}", result.code, result.message);
					return null;
				}

				return mapper.readValue(result.data.config, ReplicationManagerConfig.class);
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
		try (var client = getClient();) {

			var get = getRequest(getLogManagerConfigURL());
			var response = client.execute(get);
			if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				var body = handler.handleResponse(response);
				logger.debug("Body : {}", body);

				var mapper = new ObjectMapper();
				var result = mapper.readValue(body, new TypeReference<ResponseData<ResponseConfig>>() {
				});

				if (!result.code.isEmpty()) {
					logger.error("{}, {}", result.code, result.message);
					return null;
				}

				return mapper.readValue(result.data.config, LogManagerConfig.class);
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
		try (var client = getClient();) {

			var get = getRequest(getLifecycleManagerConfigURL());
			var response = client.execute(get);
			if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				var body = handler.handleResponse(response);
				logger.debug("Body : {}", body);

				var mapper = new ObjectMapper();
				var result = mapper.readValue(body, new TypeReference<ResponseData<ResponseConfig>>() {
				});

				if (!result.code.isEmpty()) {
					logger.error("{}, {}", result.code, result.message);
					return null;
				}

				return mapper.readValue(result.data.config, LifecycleManagerConfig.class);
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
	public boolean regionUpdate() {
		try (var client = getClient();) {

			var get = getRequest(getRegionsURL());
			var response = client.execute(get);

			if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				var body = handler.handleResponse(response);

				var mapper = new ObjectMapper();
				var result = mapper.readValue(body, new TypeReference<ResponseList<ResponseRegion>>() {
				});

				if (!result.code.isEmpty()) {
					logger.error("region is empty. [{}, {}]", result.code, result.message);
					return false;
				}

				if (regions == null) {
					regions = new ArrayList<>();
					for (var Region : result.data.Items)
						regions.add(new S3RegionData(Region));
				} else {
					for (var Region : result.data.Items) {
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
	 * @param address
	 *            찾을 리전 주소
	 * @return 리전 정보
	 */
	public S3RegionData get2Address(String address) {
		if (regions == null || regions.isEmpty() || StringUtils.isBlank(address))
			return null;
		return regions.stream().filter(f -> address.equals(f.address)).findAny().orElse(null);
	}

	/**
	 * 리전명을 바탕으로 리전정보를 조회한다.
	 * 
	 * @param regionName
	 *            찾을 리전 명
	 * @return 리전 정보
	 */
	public S3RegionData getRegion(String regionName) {
		if (regions == null || regions.isEmpty() || StringUtils.isBlank(regionName))
			return null;
		return regions.stream().filter(f -> regionName.equals(f.name)).findAny().orElse(null);
	}

	private String getURL() {
		return String.format("https://%s:%s", config.portalHost, config.portalPort);
	}

	private String getMariaDBConfigURL() {
		return getURL() + Constants.URL_MARIADB_CONFIG;
	}

	private String getObjManagerConfigURL() {
		return getURL() + Constants.URL_OBJ_MANAGER_CONFIG;
	}

	private String getLifecycleManagerConfigURL() {
		return getURL() + Constants.URL_LIFECYCLE_MANAGER_CONFIG;
	}

	private String getReplicationConfigURL() {
		return getURL() + Constants.URL_REPLICATION_MANAGER_CONFIG;
	}

	private String getLogManagerConfigURL() {
		return getURL() + Constants.URL_LOG_MANAGER_CONFIG;
	}

	private String getLocalRegionURL() {
		return getURL() + Constants.URL_LOCAL_REGION;
	}

	private String getRegionsURL() {
		return getURL() + Constants.URL_REGION;
	}

	private HttpGet getRequest(String url) {
		var request = new HttpGet(url);
		request.addHeader("Authorization", config.apiKey);
		return request;
	}

	private CloseableHttpClient getClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		return HttpClients.custom()
				.setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
				.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.build();
	}
}
