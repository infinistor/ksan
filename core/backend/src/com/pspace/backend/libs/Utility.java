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
package com.pspace.backend.libs;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.pspace.backend.libs.Ksan.Data.S3RegionData;

public class Utility {
	static final Logger logger = LoggerFactory.getLogger(Utility.class);
	private static final int TimeOut = 3 * 1000;
	private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

	public static String ReadServiceId(String FilePath) {
		try {
			BufferedReader Reader = new BufferedReader(new FileReader(FilePath));
			var ServiceId = Reader.readLine();
			Reader.close();
			return ServiceId;
		} catch (Exception e) {
			return null;
		}
	}

	public static String GetNowTime() {
		var now = new Date();
		return simpleDateFormat.format(now);
	}

	public static int String2Time(String Value) {
		if (Value == null || Value.isBlank())
			return -1;

		String Token[] = Value.split(":");
		if (Token.length > 1) {
			int Hour = Integer.parseInt(Token[0]);
			int Min = Integer.parseInt(Token[1]);
			return Hour * 60 + Min;
		} else
			return -1;
	}

	public static boolean isRun(int Time) {
		return Time == GetNowTimetoInt();
	}

	public static int GetNowDay() {
		Calendar time = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"), Locale.KOREA);
		return time.get(Calendar.DAY_OF_YEAR);
	}

	public static int GetNowTimetoInt() {
		Calendar time = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"), Locale.KOREA);
		int Hour = time.get(Calendar.HOUR_OF_DAY);
		int Minute = time.get(Calendar.MINUTE);

		return Hour * 60 + Minute;
	}

	public static void Delay(int milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
		}
	}

	public static boolean checkAlive(String URL) {
		try {
			HttpClient Client = HttpClientBuilder.create().build();
			HttpGet getRequest = new HttpGet(URL);
			RequestConfig requestConfig = RequestConfig.custom()
					.setSocketTimeout(TimeOut)
					.setConnectTimeout(TimeOut)
					.setConnectionRequestTimeout(TimeOut)
					.build();
			getRequest.setConfig(requestConfig);
			Client.execute(getRequest);
			getRequest.releaseConnection();
			return true;
		} catch (Exception e) {
			logger.error("URL : {}", URL, e);
		}
		return false;
	}

	public static InputStream CreateBody(String Body) {
		return new ByteArrayInputStream(Body.getBytes());
	}

	public static AmazonS3 CreateClient(S3RegionData region) {
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(region.AccessKey, region.SecretKey);
		logger.debug("Client : {}, {}", region.AccessKey, region.SecretKey);

		return AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds))
				.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(region.getHttpURL(), ""))
				.withPathStyleAccessEnabled(true).build();
	}

}