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
package com.pspace.backend.libs.auth;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Various Http helper routines
 */
public class NetUtils {

	static final int USER_DATE_BLOCK_SIZE = 64 * 1024;
	static final String HTTP = "http://";
	static final String HTTPS = "https://";

	public static String CreateURLToHTTP(String Address, int Port) {
		var URL = HTTP + Address;

		if (URL.endsWith("/"))
			URL = URL.substring(0, URL.length() - 1);

		return String.format("%s:%d", URL, Port);
	}

	public static String CreateURLToHTTPS(String Address, int Port) {
		var URL = HTTPS + Address;

		if (URL.endsWith("/"))
			URL = URL.substring(0, URL.length() - 1);

		return String.format("%s:%d", URL, Port);
	}

	public static URL GetEndPoint(String Protocol, String Address, int Port, String BucketName)
			throws MalformedURLException {
		return new URL(String.format("%s%s:%d/%s", Protocol, Address, Port, BucketName));
	}

	public static URL GetEndPoint(String Protocol, String RegionName, String BucketName) throws MalformedURLException {
		return new URL(String.format("%s%s.s3-%s.amazonaws.com", Protocol, BucketName, RegionName));
	}

	public static URL GetEndPoint(String Protocol, String Address, int Port, String BucketName, String Key)
			throws MalformedURLException {
		return new URL(String.format("%s%s:%d/%s/%s", Protocol, Address, Port, BucketName, Key));
	}

	public static URL GetEndPoint(String Protocol, String RegionName, String BucketName, String Key)
			throws MalformedURLException {
		return new URL(String.format("%s%s.s3-%s.amazonaws.com/%s", Protocol, BucketName, RegionName, Key));
	}

	public static MyResult PostUpload(URL EndPoint, Map<String, String> headers, String requestBody) {
		try {
			var connection = createHttpConnection(EndPoint, "POST", headers);
			if (requestBody != null) {
				var wr = new DataOutputStream(connection.getOutputStream());
				wr.writeBytes(requestBody);
				wr.flush();
				wr.close();
			}

			return Send(connection);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static MyResult PutUpload(URL EndPoint, Map<String, String> headers, String requestBody) {
		try {
			var connection = createHttpConnection(EndPoint, "PUT", headers);
			if (requestBody != null) {
				var wr = new DataOutputStream(connection.getOutputStream());
				wr.writeBytes(requestBody);
				wr.flush();
				wr.close();
			}

			return Send(connection);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static MyResult Send(HttpURLConnection connection) {
		var Result = new MyResult();
		try {
			// Get Response
			InputStream is;
			try {
				is = connection.getInputStream();
			} catch (IOException e) {
				is = connection.getErrorStream();
			}

			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer();
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			Result.Message = response.toString();
			Result.StatusCode = connection.getResponseCode();
		} catch (Exception e) {
			e.printStackTrace();
			Result.Message = e.getMessage();
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		return Result;
	}

	public static HttpURLConnection createHttpConnection(URL endpointUrl, String httpMethod,
			Map<String, String> headers) {
		try {
			HttpURLConnection connection = (HttpURLConnection) endpointUrl.openConnection();
			connection.setRequestMethod(httpMethod);

			if (headers != null) {
				for (String headerKey : headers.keySet()) {
					connection.setRequestProperty(headerKey, headers.get(headerKey));
				}
			}

			return connection;
		} catch (Exception e) {
			throw new RuntimeException("Cannot create connection. " + e.getMessage(), e);
		}
	}

	public static String urlEncode(String url, boolean keepPathSlash) {
		String encoded;
		try {
			encoded = URLEncoder.encode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("UTF-8 encoding is not supported.", e);
		}
		if (keepPathSlash) {
			encoded = encoded.replace("%2F", "/");
		}
		return encoded;
	}
}
