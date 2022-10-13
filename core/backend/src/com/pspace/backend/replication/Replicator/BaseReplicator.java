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
package Replicator;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.pspace.backend.libs.Utility;
import com.pspace.backend.libs.Data.Replication.ReplicationEventData;
import com.pspace.backend.libs.Ksan.PortalManager;
import com.pspace.backend.libs.Ksan.Data.S3RegionData;
import com.pspace.ifs.ksan.libs.mq.MQCallback;
import com.pspace.backend.libs.Ksan.Data.AgentConfig;

import config.ConfigManager;

public abstract class BaseReplicator implements MQCallback {
	final Logger logger;

	public enum OperationList {
		OP_PUT_OBJECT, OP_PUT_OBJECT_ACL, OP_PUT_OBJECT_RETENTION, OP_PUT_OBJECT_TAGGING, OP_DELETE_OBJECT,
		OP_DELETE_OBJECT_TAGGING
	}

	protected AmazonS3 SourceClient;
	protected AgentConfig ksanConfig;
	protected PortalManager portal;
	protected ConfigManager config;

	public BaseReplicator(Logger logger) {
		this.logger = logger;
		this.ksanConfig = AgentConfig.getInstance();
		this.portal = PortalManager.getInstance();
		this.config = ConfigManager.getInstance();
	}
	public BaseReplicator(Logger logger, String RegionName) {
		this.logger = logger;
		this.ksanConfig = AgentConfig.getInstance();
		this.portal = PortalManager.getInstance();
		this.config = ConfigManager.getInstance();
		SetRegion(RegionName);
	}

	public boolean SetRegion(String RegionName) {
		try {
			var SourceRegion = portal.getRegion(RegionName);
			if (SourceRegion == null) {
				logger.error("Region is not exists");
				return false;
			}

			SourceClient = CreateClient(SourceRegion);
			if (SourceClient == null) {
				logger.error("Source Client is NULL!");
				return false;
			}

			return true;
		} catch (Exception e) {
			logger.error("", e);
			return false;
		}
	}

	/******************************************
	 * Utility
	 *************************************************/

	protected boolean RegionCheck(String RegionName) {
		if (StringUtils.isBlank(RegionName))
			RegionName = config.getRegion();

		var Region = portal.getRegion(RegionName);
		if (Region == null) {
			logger.error("Region Name({}) is not exists!", RegionName);
			return false;
		}
		return Utility.S3AliveCheck(Region.getHttpURL());
	}

	protected AmazonS3 CreateClient(S3RegionData Region) {
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(Region.AccessKey, Region.SecretKey);
		logger.debug("Client : {}, {}", Region.AccessKey, Region.SecretKey);

		return AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds))
				.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(Region.getHttpURL(), ""))
				.withPathStyleAccessEnabled(true).build();
	}

	protected AmazonS3 CreateClient(ReplicationEventData Data) throws Exception {
		return CreateClient(Data.TargetRegion);
	}

	protected AmazonS3 CreateClient(String RegionName) throws Exception {
		if (StringUtils.isBlank(RegionName))
			return SourceClient;

		var Region = portal.getRegion(RegionName);
		if (Region == null)
			throw new Exception(RegionName + " Region is not exists.");

		BasicAWSCredentials awsCreds = new BasicAWSCredentials(Region.AccessKey, Region.SecretKey);

		return AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds))
				.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(Region.getHttpURL(), ""))
				.withPathStyleAccessEnabled(true).build();
	}
}
