package com.pspace.Portal;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

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
import com.pspace.backend.Data.LifecycleConfig;
import com.pspace.backend.Data.Portal.*;

public class PortalManager {
	static final Logger logger = LoggerFactory.getLogger(PortalManager.class);

	private final String Address;
	private final int Port;
	private final String Key;

	public PortalManager(String Address, int Port, String Key) {
		this.Address = Address;
		this.Port = Port;
		this.Key = Key;
	}

	public LifecycleConfig GetConfig2Lifecycle() {
		try (var Client = GetClient();) {

			var Get = GetRequest(GetConfigURL());
			var Response = Client.execute(Get);
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

				return Mapper.readValue(Result.Data.Config, LifecycleConfig.class);
			}

		} catch (Exception e) {
			logger.error("", e);
		}
		return null;
	}

	public ResponseRegion GetRegion(String RegionName) {
		try (var Client = GetClient();) {

			var Get = GetRequest(GetRegionURL(RegionName));
			var Response = Client.execute(Get);

			if (Response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> handler = new BasicResponseHandler();
				var Body = handler.handleResponse(Response);

				var Mapper = new ObjectMapper();
				var Result = Mapper.readValue(Body, new TypeReference<ResponseData<ResponseRegion>>() {
				});

				if (!Result.Code.isEmpty()) {
					logger.error("%s, %s", Result.Code, Result.Message);
					return null;
				}

				return Result.Data;
			}

		} catch (Exception e) {
			logger.error("", e);
		}
		return null;
	}

	private String GetConfigURL() {
		return String.format("https://%s:%s/api/v1/Config/Lifecycle", Address, Port);
	}

	private String GetRegionURL(String RegionName) {
		return String.format("https://%s:%s/api/v1/Regions/%s", Address, Port, RegionName);
	}

	private HttpGet GetRequest(String URL) {
		var Request = new HttpGet(URL);
		Request.addHeader("Authorization", Key);
		return Request;
	}

	private CloseableHttpClient GetClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		return HttpClients.custom()
				.setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
				.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.build();
	}
}
