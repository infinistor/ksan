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
import com.pspace.Portal.Data.*;
import com.pspace.metering.utils.MeteringConfig;

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

	public MeteringConfig MeteringConfig() {
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

				return Mapper.readValue(Result.Data.Config, MeteringConfig.class);
			}

		} catch (Exception e) {
			logger.error("", e);
		}
		return null;
	}

	private String GetConfigURL() {
		return String.format("https://%s:%s/api/v1/Config/Metering", Address, Port);
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
