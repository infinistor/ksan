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
package com.pspace.backend.libs.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.auth.AWS4SignerBase;
import com.pspace.backend.libs.auth.AWS4SignerForChunkedUpload;
import com.pspace.backend.libs.auth.MyResult;

public class KsanClient {
	private static final Logger logger = LoggerFactory.getLogger(KsanClient.class);

	static final String METHOD_DELETE = "DELETE";
	static final String METHOD_POST = "POST";
	static final String METHOD_GET = "GET";
	static final String METHOD_LIST = "GET";
	static final String METHOD_PUT = "PUT";
	static final String SERVICE_NAME = "s3";

	private final String host;
	private final int port;
	private final String accessKey;
	private final String secretKey;
	public static final String HEADER_DATA = "NONE";
	public static final String HEADER_BACKEND = "x-ifs-admin";

	public KsanClient(String host, int port, String accessKey, String secretKey) {
		this.host = host;
		this.port = port;
		this.accessKey = accessKey;
		this.secretKey = secretKey;
	}

	public MyResult storageMove(String bucketName, String key, String storageClass, String versionId) throws MalformedURLException {
		if (StringUtils.isBlank(bucketName)) {
			logger.error("bucketName is blank");
			return null;
		}

		var srtQuery = "storagemove&encoding-type=url";
		var query = new HashMap<String, String>();
		query.put("storagemove", "");
		query.put("encoding-type", "url");

		if (StringUtils.isNotBlank(storageClass)) {
			srtQuery += "&StorageClass=" + storageClass;
			query.put("StorageClass", storageClass);
		}
		if (StringUtils.isNotBlank(versionId)) {
			srtQuery += "&VersionId=" + versionId;
			query.put("VersionId", versionId);
		}

		var url = new URL(String.format("http://%s:%d/%s/%s?%s", host, port, bucketName, key, srtQuery));

		var headers = new HashMap<String, String>();
		headers.put("X-Amz-Content-SHA256", AWS4SignerBase.EMPTY_BODY_SHA256);
		headers.put("Content-Length", "0");
		headers.put("Content-Type", "text/plain");
		headers.put(HEADER_BACKEND, HEADER_DATA);

		var signer = new AWS4SignerForChunkedUpload(url, METHOD_POST, SERVICE_NAME, "us-west-2");

		String authorization = signer.computeSignature(headers, query, AWS4SignerBase.EMPTY_BODY_SHA256, accessKey,
				secretKey);
		headers.put("Authorization", authorization);

		return postUpload(url, headers);
	}

	public static MyResult postUpload(URL endPoint, Map<String, String> headers) {
		try {
			var connection = createHttpConnection(endPoint, METHOD_POST, headers);
			return send(connection);
		} catch (Exception e) {
			logger.error("", e);
		}

		return null;
	}

	public static HttpURLConnection createHttpConnection(URL endpointUrl, String httpMethod, Map<String, String> headers) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) endpointUrl.openConnection();
		connection.setRequestMethod(httpMethod);

		if (headers != null) {
			for (var header : headers.entrySet()) {
				connection.setRequestProperty(header.getKey(), header.getValue());
			}
		}
		connection.setFixedLengthStreamingMode(0);
		connection.setUseCaches(false);
		connection.setDoInput(true);
		connection.setDoOutput(true);
		return connection;
	}

	public static MyResult send(HttpURLConnection connection) {
		var result = new MyResult();
		if (connection == null) {
			result.Message = "connection is null";
			return result;
		}

		try (InputStream is = connection.getResponseCode() >= 400 ? connection.getErrorStream()
				: connection.getInputStream()) {
			var rd = new BufferedReader(new InputStreamReader(is));
			String line;
			var response = new StringBuilder();
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			result.Message = response.toString();
			result.StatusCode = connection.getResponseCode();
		} catch (Exception e) {
			logger.error("", e);
			result.Message = e.getMessage();
		} finally {
			connection.disconnect();
		}
		return result;
	}
}
