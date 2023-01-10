package com.pspace.backend.libs.AdminClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.auth.AWS4SignerBase;
import com.pspace.backend.libs.auth.AWS4SignerForChunkedUpload;
import com.pspace.backend.libs.auth.MyResult;
import com.pspace.backend.libs.Data.BackendHeaders;

public class KsanClient {
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

	public MyResult StorageMove(String bucketName, String key, String storageClass, String versionId) throws Exception {
		if (StringUtils.isBlank(bucketName))
			throw new Exception("버킷 이름이 비어있습니다.");

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

		var URI = new URL(String.format("http://%s:%d/%s/%s?%s", host, port, bucketName, key, srtQuery));

		var headers = new HashMap<String, String>();
		headers.put("X-Amz-Content-SHA256", AWS4SignerBase.EMPTY_BODY_SHA256);
		headers.put("Content-Length", "0");
		headers.put("Content-Type", "text/plain");
		headers.put(HEADER_BACKEND, HEADER_DATA);

		var signer = new AWS4SignerForChunkedUpload(URI, METHOD_POST, SERVICE_NAME, "us-west-2");

		String authorization = signer.computeSignature(headers, query, AWS4SignerBase.EMPTY_BODY_SHA256, accessKey,
				secretKey);
		headers.put("Authorization", authorization);

		return PostUpload(URI, headers);
	}

	public static MyResult PostUpload(URL EndPoint, Map<String, String> headers) {
		try {
			var connection = createHttpConnection(EndPoint, METHOD_POST, headers);
			return Send(connection);
		} catch (Exception e) {
			// logger.error("", e);
			e.printStackTrace();
		}

		return null;
	}

	public static HttpURLConnection createHttpConnection(URL endpointUrl, String httpMethod,
			Map<String, String> headers) {
		try {
			HttpURLConnection connection = (HttpURLConnection) endpointUrl.openConnection();
			connection.setRequestMethod(httpMethod);

			if (headers != null) {
				for (String headerKey : headers.keySet()) {
					connection.setRequestProperty(headerKey, headers.get(headerKey));
					System.out.printf("%s, %s\n", headerKey, headers.get(headerKey));
				}
			}
			connection.setFixedLengthStreamingMode(0);
			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);
			return connection;
		} catch (Exception e) {
			throw new RuntimeException("Cannot create connection. " + e.getMessage(), e);
		}
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
}
