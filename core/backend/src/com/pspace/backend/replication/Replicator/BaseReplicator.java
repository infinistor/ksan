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
package com.pspace.backend.Replication.Replicator;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.amazonaws.services.s3.AmazonS3;
import com.pspace.backend.libs.Utility;
import com.pspace.backend.libs.Ksan.AgentConfig;
import com.pspace.backend.libs.Ksan.PortalManager;
import com.pspace.backend.libs.Ksan.Data.S3RegionData;
import com.pspace.backend.libs.config.ReplicationManagerConfig;
import com.pspace.backend.libs.data.Replication.ReplicationEventData;

public class BaseReplicator {
	final Logger logger;

	public enum OperationList {
		OP_PUT_OBJECT, OP_PUT_OBJECT_ACL, OP_PUT_OBJECT_RETENTION, OP_PUT_OBJECT_TAGGING, OP_DELETE_OBJECT, OP_DELETE_OBJECT_TAGGING
	}

	protected AmazonS3 sourceClient;
	protected AgentConfig ksanConfig;
	protected PortalManager portal;
	protected ReplicationManagerConfig config;
	protected S3RegionData sourceRegion;

	public BaseReplicator(Logger logger) {
		this.logger = logger;
		this.ksanConfig = AgentConfig.getInstance();
		this.portal = PortalManager.getInstance();
		setConfig();
	}

	public void setConfig() {
		this.config = portal.getReplicationManagerConfig();
		this.sourceRegion = portal.getLocalRegion();
		this.sourceClient = sourceRegion.client;
	}

	/**
	 * Region이 존재하는지 확인한다.
	 * 
	 * @param regionName
	 *            확인할 Region 이름
	 * @return 존재하면 true, 아니면 false
	 */
	protected boolean checkRegion(String regionName) {
		if (StringUtils.isBlank(regionName) || sourceRegion.name.equals(regionName))
			return true;

		var region = portal.getRegion(regionName);
		if (region == null) {
			logger.error("Region Name({}) is not exists!", regionName);
			return false;
		}
		return Utility.checkAlive(region.getHttpURL());
	}

	/**
	 * Region Manager에서 사용할 Client를 가져온다.
	 * 
	 * @param regionName
	 *            확인할 Region 이름
	 * @return 존재하면 true, 아니면 false
	 */
	protected AmazonS3 getClient(ReplicationEventData data) throws Exception {
		return getRegionClient(data.targetRegion);
	}

	/**
	 * Region Manager에서 사용할 Client를 가져온다.
	 * 
	 * @param regionName
	 *            확인할 Region 이름
	 * @return 존재하면 true, 아니면 false
	 */
	protected AmazonS3 getRegionClient(String regionName) throws Exception {
		if (StringUtils.isBlank(regionName))
			return sourceClient;

		var region = portal.getRegion(regionName);
		if (region == null)
			throw new Exception(regionName + " Region is not exists.");
		return region.client;
	}
}
