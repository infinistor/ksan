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
