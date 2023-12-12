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
package com.pspace.backend.Libs.Data.S3;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspace.backend.Libs.S3.ReplicationConfiguration;
import com.pspace.backend.Libs.S3.S3Parameters;
import com.pspace.backend.Libs.S3.ReplicationConfiguration.Rule.Filter.Tag;

public class ReplicationData {

	public boolean Status;
	public boolean DeleteMarker;
	public String TargetAccessControlTranslationOwner;
	public String TargetAccount;
	public String TargetBucket;
	public String TargetRegion;
	public int TargetMetricsEventThreshold;
	public boolean TargetMetricsStatus;
	public boolean TargetReplicationTimeStatus;
	public int TargetReplicationTime;
	public boolean ExistingObjectReplicationStatus;
	public String Prefix;
	public Collection<Tag> Tags;
	public String ID;
	public int Priority;
	public boolean ReplicaModificationsStatus;

	public boolean isFiltering;

	public ReplicationData() {
		Init();
	}

	public void Init() {
		Status = true;
		DeleteMarker = false;
		TargetAccessControlTranslationOwner = "";
		TargetAccount = "";
		TargetBucket = "";
		TargetRegion = "";
		TargetMetricsEventThreshold = 0;
		TargetMetricsStatus = false;
		TargetReplicationTimeStatus = false;
		TargetReplicationTime = 0;
		ExistingObjectReplicationStatus = false;
		Prefix = "";
		Tags = new ArrayList<Tag>();
		ID = "";
		Priority = 0;
		ReplicaModificationsStatus = false;
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
			if (S3Parameters.isEnabled(Conf.status)) Status = true;
			else Status = false;

			TargetAccount = destination.account;

			// 버킷이름 파싱
			if (destination.bucket != null)
				BucketNameParser(destination.bucket);

			// accessControlTranslation 설정이 있다면
			if (destination.accessControlTranslation != null)
				TargetAccessControlTranslationOwner = destination.accessControlTranslation.owner;

			// metrics 설정이 있다면
			if (destination.metrics != null) {
				ReplicationConfiguration.Rule.Destination.Metrics metrics = destination.metrics;
				if (S3Parameters.isEnabled(metrics.status)) TargetMetricsStatus = true;
				if (metrics.eventThreshold != null) TargetMetricsEventThreshold = metrics.eventThreshold.minutes;
			}

			// replicationTime 설정이 있다면
			if (destination.replicationTime != null) {
				ReplicationConfiguration.Rule.Destination.ReplicationTime replicationTime = Conf.destination.replicationTime;
				if (S3Parameters.isEnabled(replicationTime.status)) TargetReplicationTimeStatus = true;
				if (replicationTime.time != null) TargetReplicationTime = replicationTime.time.minutes;
			}
		}

		if (Conf.deleteMarkerReplication != null && S3Parameters.isEnabled(Conf.deleteMarkerReplication.Status)) DeleteMarker = true;
		if (Conf.existingObjectReplication != null && S3Parameters.isEnabled(Conf.existingObjectReplication.status)) ExistingObjectReplicationStatus = true;

		/// Filter Priority
		/// Filter.And : 1
		/// Filter : 2
		if (Conf.filter != null) {
			isFiltering = true;

			// And 옵션이 있을 경우 가져오기
			if (Conf.filter.and != null) {
				if (!StringUtils.isBlank(Conf.filter.and.prefix)) Prefix = Conf.filter.and.prefix;
				if (Conf.filter.and.tag != null && Conf.filter.and.tag.size() > 0) Tags = Conf.filter.and.tag;
			}
			// And 설정이 없을 경우 적용
			else {
				if (!StringUtils.isBlank(Conf.filter.prefix)) Prefix = Conf.filter.prefix;
				if (Conf.filter.tag != null) Tags.add(Conf.filter.tag);
			}
		}

		ID = Conf.id;
		Priority = Conf.priority;

		if (Conf.sourceSelectionCriteria != null) {
			ReplicationConfiguration.Rule.SourceSelectionCriteria sourceSelectionCriteria = Conf.sourceSelectionCriteria;
			if (sourceSelectionCriteria.replicaModifications != null)
				if (S3Parameters.isEnabled(sourceSelectionCriteria.replicaModifications.status)) ReplicaModificationsStatus = true;
		}
		return true;
	}

	private void BucketNameParser(String Bucket) {

		String Token[] = Bucket.split(":");

		// 버킷이름을 파싱했으나 :이 없다면
		if (Token.length < 5) {
			TargetBucket = Bucket;
			return;
		}

		// String ARN = Token[0];
		// String Partition = Token[1];
		// String Service = Token[2];
		String Region = Token[3];
		String BucketName = Token[5];

		if (!StringUtils.isBlank(BucketName))
			TargetBucket = BucketName.trim();

		//리전 정보가 존재할경우 리전 정보 저장
		if (!StringUtils.isBlank(Region))
			TargetRegion = Region.trim();
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
