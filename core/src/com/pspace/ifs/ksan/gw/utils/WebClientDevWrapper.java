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

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import com.pspace.ifs.ksan.libs.PrintStack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebClientDevWrapper {
    private static final Logger logger = LoggerFactory.getLogger(WebClientDevWrapper.class);
	
	public static HttpClient wrapClient() {
		try {
			HttpClientBuilder builder = HttpClientBuilder.create();
			SSLContextBuilder customContext = SSLContexts.custom();
			customContext = WebClientDevWrapper.ignoreUnverifiedSSL(customContext);
			
			if (customContext == null) {
				return null;
			}
						
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
					customContext.build(), new NoopHostnameVerifier());
			builder.setSSLSocketFactory(sslsf);
			
			Registry<ConnectionSocketFactory> sfr = RegistryBuilder.
					<ConnectionSocketFactory> create()
					.register("https", sslsf)
					.register("http", PlainConnectionSocketFactory.getSocketFactory())
					.build();

			PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(sfr);
			
			cm.setMaxTotal(500);
			cm.setDefaultMaxPerRoute(500);
			
			builder.setConnectionManager(cm);
			builder.setUserAgent("IFS-S3GW");
			
			return builder.build();
		} catch (Exception e) {
			logger.error("[WebClientDevWrapper - wrapCllient] Fail to create HttpClient");
            PrintStack.logging(logger,e);
			return null;
		}
	}
	
	public static HttpGet wrapHttpGet(HttpGet request, RequestConfig reqConfig) {
		request.setProtocolVersion(HttpVersion.HTTP_1_1);
		request.setConfig(reqConfig);

		return request;
	}
	
	public static HttpHead wrapHttpHead(HttpHead request, RequestConfig reqConfig) {
		request.setProtocolVersion(HttpVersion.HTTP_1_1);
		request.setConfig(reqConfig);

		return request;
	}

	public static HttpPost wrapHttpPost(HttpPost request, RequestConfig reqConfig) {
		request.setProtocolVersion(HttpVersion.HTTP_1_1);
		request.setConfig(reqConfig);

		return request;
	}

	public static HttpPut wrapHttpPut(HttpPut request, RequestConfig reqConfig) {
		request.setProtocolVersion(HttpVersion.HTTP_1_1);
		request.setConfig(reqConfig);

		return request;
	}

	public static HttpDelete wrapHttpDelete(HttpDelete request, RequestConfig reqConfig) {
		request.setProtocolVersion(HttpVersion.HTTP_1_1);
		request.setConfig(reqConfig);

		return request;
	}
	
	private static SSLContextBuilder ignoreUnverifiedSSL(SSLContextBuilder customContext) {
		TrustStrategy easyStrategy = new TrustStrategy() {
			
			@Override
			public boolean isTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
				return true;
			}
		};
		
		try {
			customContext = customContext.loadTrustMaterial(null, easyStrategy);
		} catch (NoSuchAlgorithmException e) {
			logger.error("[WebClientDevWrapper - ignoreUnverifiedSSL] Fail to loadTrustMaterial");
            PrintStack.logging(logger,e);

			return null;
		} catch (KeyStoreException e) {
			logger.error("[WebClientDevWrapper - ignoreUnverifiedSSL] Fail to loadTrustMaterial");
            PrintStack.logging(logger,e);
			
			return null;
		}
		
		return customContext;
	}
}
