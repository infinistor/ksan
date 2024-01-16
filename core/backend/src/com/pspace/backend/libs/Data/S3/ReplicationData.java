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
package com.pspace.backend.libs.data.s3;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspace.backend.libs.S3.ReplicationConfiguration;
import com.pspace.backend.libs.S3.S3Parameters;
import com.pspace.backend.libs.S3.ReplicationConfiguration.Rule.Filter.Tag;

public class ReplicationData {

	public boolean status;
	public boolean deleteMarker;
	public String targetAccessControlTranslationOwner;
	public String targetAccount;
	public String targetBucket;
	public String targetRegion;
	public int targetMetricsEventThreshold;
	public boolean targetMetricsStatus;
	public boolean targetReplicationTimeStatus;
	public int targetReplicationTime;
	public boolean existingObjectReplicationStatus;
	public String prefix;
	public Collection<Tag> tags;
	public String id;
	public int priority;
	public boolean replicaModificationsStatus;

	public boolean isFiltering;

	public ReplicationData() {
		Init();
	}

	public void Init() {
		status = true;
		deleteMarker = false;
		targetAccessControlTranslationOwner = "";
		targetAccount = "";
		targetBucket = "";
		targetRegion = "";
		targetMetricsEventThreshold = 0;
		targetMetricsStatus = false;
		targetReplicationTimeStatus = false;
		targetReplicationTime = 0;
		existingObjectReplicationStatus = false;
		prefix = "";
		tags = new ArrayList<Tag>();
		id = "";
		priority = 0;
		replicaModificationsStatus = false;
		isFiltering = false;
	}

	public boolean Set(ReplicationConfiguration.Rule Conf) {
		Init();
		if (Conf == null) return false;
		if (Conf.destination == null) return false;
		else {
			// destination 설정이 비어있지 않을 경우
			ReplicationConfiguration.Rule.Destination destination = Conf.destination;

			// 복제 설정이 켜져 있다면
			if (S3Parameters.isEnabled(Conf.status)) status = true;
			else status = false;

			targetAccount = destination.account;

			// 버킷이름 파싱
			if (destination.bucket != null)
				BucketNameParser(destination.bucket);

			// accessControlTranslation 설정이 있다면
			if (destination.accessControlTranslation != null)
				targetAccessControlTranslationOwner = destination.accessControlTranslation.owner;

			// metrics 설정이 있다면
			if (destination.metrics != null) {
				ReplicationConfiguration.Rule.Destination.Metrics metrics = destination.metrics;
				if (S3Parameters.isEnabled(metrics.status)) targetMetricsStatus = true;
				if (metrics.eventThreshold != null) targetMetricsEventThreshold = metrics.eventThreshold.minutes;
			}

			// replicationTime 설정이 있다면
			if (destination.replicationTime != null) {
				ReplicationConfiguration.Rule.Destination.ReplicationTime replicationTime = Conf.destination.replicationTime;
				if (S3Parameters.isEnabled(replicationTime.status)) targetReplicationTimeStatus = true;
				if (replicationTime.time != null) targetReplicationTime = replicationTime.time.minutes;
			}
		}

		if (Conf.deleteMarkerReplication != null && S3Parameters.isEnabled(Conf.deleteMarkerReplication.Status)) deleteMarker = true;
		if (Conf.existingObjectReplication != null && S3Parameters.isEnabled(Conf.existingObjectReplication.status)) existingObjectReplicationStatus = true;

		/// Filter Priority
		/// Filter.And : 1
		/// Filter : 2
		if (Conf.filter != null) {
			isFiltering = true;

			// And 옵션이 있을 경우 가져오기
			if (Conf.filter.and != null) {
				if (!StringUtils.isBlank(Conf.filter.and.prefix)) prefix = Conf.filter.and.prefix;
				if (Conf.filter.and.tag != null && Conf.filter.and.tag.size() > 0) tags = Conf.filter.and.tag;
			}
			// And 설정이 없을 경우 적용
			else {
				if (!StringUtils.isBlank(Conf.filter.prefix)) prefix = Conf.filter.prefix;
				if (Conf.filter.tag != null) tags.add(Conf.filter.tag);
			}
		}

		id = Conf.id;
		priority = Conf.priority;

		if (Conf.sourceSelectionCriteria != null) {
			ReplicationConfiguration.Rule.SourceSelectionCriteria sourceSelectionCriteria = Conf.sourceSelectionCriteria;
			if (sourceSelectionCriteria.replicaModifications != null)
				if (S3Parameters.isEnabled(sourceSelectionCriteria.replicaModifications.status)) replicaModificationsStatus = true;
		}
		return true;
	}

	private void BucketNameParser(String Bucket) {

		String Token[] = Bucket.split(":");

		// 버킷이름을 파싱했으나 :이 없다면
		if (Token.length < 5) {
			targetBucket = Bucket;
			return;
		}

		// String ARN = Token[0];
		// String Partition = Token[1];
		// String Service = Token[2];
		String Region = Token[3];
		String BucketName = Token[5];

		if (!StringUtils.isBlank(BucketName))
			targetBucket = BucketName.trim();

		//리전 정보가 존재할경우 리전 정보 저장
		if (!StringUtils.isBlank(Region))
			targetRegion = Region.trim();
	}

	@Override
	public String toString() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return "";
		}
	}
}
