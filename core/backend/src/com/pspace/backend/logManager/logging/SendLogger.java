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
package com.pspace.backend.logManager.logging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CanonicalGrantee;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.pspace.backend.libs.Utility;
import com.pspace.backend.libs.Data.BackendHeaders;
import com.pspace.backend.libs.Ksan.Data.S3RegionData;
import com.pspace.backend.libs.s3format.LoggingConfiguration;

public class SendLogger {
	private final char[] TEXT = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
			'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
	private Random rand = new Random();
	private final Logger logger = LoggerFactory.getLogger(SendLogger.class);
	private final int DEFAULT_UNIQUE_STRING_SIZE = 16;

	// private final LoggingConfig Config;
	// private final DBManager DB;
	// private final AmazonS3 Client;
	private boolean stop = false;
	private boolean quit = false;

	public SendLogger(S3RegionData Region) {
		// this.DB = DB;
		// this.Config = Config;
		// this.Client = CreateClient(Region);
	}

	public void Run() {
		// while (!Quit) {
		// 	// 일시정지 확인
		// 	if (Stop) {
		// 		Utility.Delay(Config.getDelay());
		// 		continue;
		// 	}

		// 	try {
		// 		var BucketList = DB.getBucketInfoLoggingList();
		// 		for (var BucketInfo : BucketList) {
		// 			var SourceBucketName = BucketInfo.BucketName;
		// 			var TargetBucketName = BucketInfo.Loggings.loggingEnabled.targetBucket;
		// 			var Prefix = BucketInfo.Loggings.loggingEnabled.targetPrefix;
		// 			var Grants = getAccessControlList(BucketInfo.Loggings.loggingEnabled.targetGrants);
	
		// 			// 로그 출력
		// 			var Loggings = DB.getLoggingEventList(SourceBucketName);
		// 			var LastIndex = 0L;
		// 			var Data = "";
		// 			for (var Logging : Loggings) {
		// 				LastIndex = Logging.Index;
		// 				Data += Logging.Print();
		// 			}
		// 			if (LastIndex > 0) {
		// 				var Key = NewKey(Prefix);
		// 				PutLogging(TargetBucketName, Key, Data, Grants);
		// 				DB.DeleteLoggingEvent(SourceBucketName, LastIndex);
		// 			}
		// 		}
		// 	} catch (Exception e) {
		// 		logger.error("", e);
		// 	}

		// 	// 대기
		// 	Utility.Delay(Config.getDelay());
		// }
	}

	public void Stop() {
		stop = true;
	}

	public void Start() {
		stop = false;
	}

	public void Quit() {
		quit = true;
	}

	/********************** Utility ****************************/

	protected AmazonS3 CreateClient(S3RegionData Region) {
		BasicAWSCredentials credentials = new BasicAWSCredentials(Region.AccessKey, Region.SecretKey);

		return AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials))
				.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(Region.getHttpURL(), ""))
				.withPathStyleAccessEnabled(true).build();
	}

	protected String RandomText(int Length) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < Length; i++)
			sb.append(TEXT[rand.nextInt(TEXT.length)]);
		return sb.toString();
	}

	protected String NewKey(String Prefix) {
		var now = LocalDateTime.now();
		var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
		if (Prefix == null) Prefix = "";
		return Prefix + now.format(formatter) + RandomText(DEFAULT_UNIQUE_STRING_SIZE);
	}

	protected AccessControlList getAccessControlList(LoggingConfiguration.LoggingEnabled.TargetGrants targetGrants)
	{
		if (targetGrants == null) return null;
		if (targetGrants.grants == null) return null;
		var Result = new AccessControlList();
		for(var Item : targetGrants.grants)
		{
			var User = new CanonicalGrantee(Item.grantee.id);
			User.setDisplayName(Item.grantee.displayName);

			Result.grantPermission(User, Permission.parsePermission(Item.permission));
		}
		return Result;
	}

	protected boolean PutLogging(String BucketName, String Key, String Body, AccessControlList Grants) {
		try {
			var Request = new PutObjectRequest(BucketName, Key, Utility.CreateBody(Body), new ObjectMetadata());
			// 권한 설정
			if (Grants != null) Request.setAccessControlList(Grants);

			//헤더 추가
			Request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
			Request.putCustomRequestHeader(BackendHeaders.HEADER_LOGGING, BackendHeaders.HEADER_DATA);

			// Client.putObject(Request);
			return true;
		} catch (Exception e) {
			logger.error("", e);
			return false;
		}
	}
}
