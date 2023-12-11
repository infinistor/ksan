// /*
// * Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
// * KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
// * the GNU General Public License as published by the Free Software Foundation, either version 
// * 3 of the License.  See LICENSE for details
// *
// * 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
// * KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
// * KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
// */
// package com.pspace.backend.logManager.logging;

// import java.time.LocalDateTime;
// import java.time.format.DateTimeFormatter;
// import java.util.Random;

// import org.apache.commons.lang3.StringUtils;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import com.amazonaws.auth.AWSStaticCredentialsProvider;
// import com.amazonaws.auth.BasicAWSCredentials;
// import com.amazonaws.client.builder.AwsClientBuilder;
// import com.amazonaws.services.s3.AmazonS3;
// import com.amazonaws.services.s3.AmazonS3ClientBuilder;
// import com.amazonaws.services.s3.model.AccessControlList;
// import com.amazonaws.services.s3.model.CanonicalGrantee;
// import com.amazonaws.services.s3.model.ObjectMetadata;
// import com.amazonaws.services.s3.model.Permission;
// import com.amazonaws.services.s3.model.PutObjectRequest;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.pspace.backend.libs.Utility;
// import com.pspace.backend.libs.Config.LogManagerConfig;
// import com.pspace.backend.libs.Data.BackendHeaders;
// import com.pspace.backend.libs.Data.S3.S3BucketData;
// import com.pspace.backend.libs.Ksan.ObjManagerHelper;
// import com.pspace.backend.libs.Ksan.PortalManager;
// import com.pspace.backend.libs.Ksan.Data.S3RegionData;
// import com.pspace.backend.libs.s3format.LoggingConfiguration;
// import com.pspace.backend.logManager.db.DBManager;

// public class SendLogger {
// 	private final char[] TEXT = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
// 			'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
// 	private Random rand = new Random();
// 	private final Logger logger = LoggerFactory.getLogger(SendLogger.class);
// 	private final int DEFAULT_UNIQUE_STRING_SIZE = 16;

// 	// private final LoggingConfig Config;
// 	private final DBManager DB = DBManager.getInstance();
// 	private final PortalManager portal = PortalManager.getInstance();
// 	private final ObjManagerHelper objManager = ObjManagerHelper.getInstance();
// 	private final LogManagerConfig config;
// 	private final AmazonS3 client;
// 	private boolean _pause = false;
// 	private boolean _quit = false;

// 	public SendLogger() {
// 		var region = portal.getLocalRegion();
// 		region.setClient();
// 		client = region.client;
// 		config = portal.getLogManagerConfig();
// 	}

// 	public void Run() {
// 		while (!_quit) {
// 			// 일시정지 확인
// 			if (_pause) {
// 				Utility.delay(1000);
// 				continue;
// 			}

// 			try {
// 				// 버킷 목록 가져오기
// 				var bucketList = objManager.getBucketList();

// 				// 비어있을 경우 재시도
// 				if (bucketList == null)
// 					continue;
				
// 				// 로깅 설정이 되어있는 버킷 목록 가져오기
// 				for (var bucket : bucketList) {
// 					// 로깅 설정이 되어있는 버킷인지 확인
// 					var strLogging = bucket.getLogging();

// 					// 로깅 설정이 되어있지 않은 버킷이면 다음 버킷으로
// 					if (StringUtils.isBlank(strLogging))
// 						continue;
					
// 					// 로깅 설정 파싱
// 					var bucketInfo = new S3BucketData(bucket.getName());
// 					bucketInfo.setLoggingConfiguration(strLogging);
// 					if (!bucketInfo.isLogging)
// 						continue;
					
// 					var SourceBucketName = bucketInfo.BucketName;
// 					var TargetBucketName = bucketInfo.Loggings.loggingEnabled.targetBucket;
// 					var Prefix = bucketInfo.Loggings.loggingEnabled.targetPrefix;
// 					var Grants = getAccessControlList(bucketInfo.Loggings.loggingEnabled.targetGrants);

// 					// 로그 출력
// 					var Loggings = DB.getLoggingEventList(SourceBucketName);
// 					var LastIndex = 0L;
// 					var Data = "";
// 					for (var Logging : Loggings) {
// 						LastIndex = Logging.Index;
// 						Data += Logging.Print();
// 					}
// 					if (LastIndex > 0) {
// 						var Key = NewKey(Prefix);
// 						PutLogging(TargetBucketName, Key, Data, Grants);
// 						DB.DeleteLoggingEvent(SourceBucketName, LastIndex);
// 					}
// 				}
// 			} catch (Exception e) {
// 				logger.error("", e);
// 			}

// 			// 대기
// 			Utility.Delay(Config.getDelay());
// 		}
// 	}

// 	public void pause() {
// 		_pause = true;
// 	}

// 	public void resume() {
// 		_pause = false;
// 	}

// 	public void quit() {
// 		_quit = true;
// 	}

// 	/********************** Utility ****************************/

// 	protected String RandomText(int Length) {
// 		StringBuffer sb = new StringBuffer();

// 		for (int i = 0; i < Length; i++)
// 			sb.append(TEXT[rand.nextInt(TEXT.length)]);
// 		return sb.toString();
// 	}

// 	protected String NewKey(String Prefix) {
// 		var now = LocalDateTime.now();
// 		var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
// 		if (Prefix == null)
// 			Prefix = "";
// 		return Prefix + now.format(formatter) + RandomText(DEFAULT_UNIQUE_STRING_SIZE);
// 	}

// 	protected AccessControlList getAccessControlList(LoggingConfiguration.LoggingEnabled.TargetGrants targetGrants) {
// 		if (targetGrants == null)
// 			return null;
// 		if (targetGrants.grants == null)
// 			return null;
// 		var Result = new AccessControlList();
// 		for (var Item : targetGrants.grants) {
// 			var User = new CanonicalGrantee(Item.grantee.id);
// 			User.setDisplayName(Item.grantee.displayName);

// 			Result.grantPermission(User, Permission.parsePermission(Item.permission));
// 		}
// 		return Result;
// 	}

// 	protected boolean PutLogging(String BucketName, String Key, String Body, AccessControlList Grants) {
// 		try {
// 			var Request = new PutObjectRequest(BucketName, Key, Utility.createBody(Body), new ObjectMetadata());
// 			// 권한 설정
// 			if (Grants != null)
// 				Request.setAccessControlList(Grants);

// 			// 헤더 추가
// 			Request.putCustomRequestHeader(BackendHeaders.HEADER_BACKEND, BackendHeaders.HEADER_DATA);
// 			Request.putCustomRequestHeader(BackendHeaders.HEADER_LOGGING, BackendHeaders.HEADER_DATA);

// 			client.putObject(Request);
// 			return true;
// 		} catch (Exception e) {
// 			logger.error("", e);
// 			return false;
// 		}
// 	}
// }
