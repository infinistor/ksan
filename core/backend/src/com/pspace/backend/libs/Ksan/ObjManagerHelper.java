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
package com.pspace.backend.libs.ksan;

import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.data.Metering.DateRange;
import com.pspace.backend.libs.data.Metering.UsageLogData;
import com.pspace.backend.libs.data.s3.S3BucketData;
import com.pspace.backend.libs.data.s3.S3ObjectData;
import com.pspace.ifs.ksan.objmanager.Bucket;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjManagerConfig;
import com.pspace.ifs.ksan.objmanager.ObjManagerUtil;
import com.pspace.ifs.ksan.objmanager.ObjMultipart;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

public class ObjManagerHelper {
	public static final int MAX_KEY_SIZE = 1000;

	private final Logger logger = LoggerFactory.getLogger(ObjManagerHelper.class);

	private ObjManagerUtil objManager;

	public static ObjManagerHelper getInstance() {
		return LazyHolder.INSTANCE;
	}

	private static class LazyHolder {
		private static final ObjManagerHelper INSTANCE = new ObjManagerHelper();
	}

	public void init(ObjManagerConfig config) throws Exception {
		objManager = new ObjManagerUtil(config);
	}

	public List<Bucket> getBucketList() {
		return objManager.getBucketList();
	}

	public List<UsageLogData> getBucketUsages(DateRange range) {
		var buckets = objManager.getBucketList();
		var usages = new java.util.ArrayList<UsageLogData>();

		for (var bucket : buckets) {
			var usage = new UsageLogData(range.start, bucket.getUserName(), bucket.getName(), bucket.getUsedSpace());
			usages.add(usage);
		}

		return usages;
	}

	public S3BucketData getBucket(String bucketName) throws ResourceNotFoundException, SQLException {
		return new S3BucketData(objManager.getBucket(bucketName));
	}

	public List<Metadata> listObjects(String bucketName, String lastObjId, long numObjects) {
		return objManager.listObjects(bucketName, lastObjId, numObjects);
	}

	public List<Metadata> listObjects(String bucketName, String keyMarker, String nextVersionId, long numObjects) {
		return objManager.listObjectsVersion(bucketName, "", keyMarker, nextVersionId, numObjects);
	}

	public List<Metadata> listExpiredObjects(String bucketName, String prefix, String nextMarker, long expiredTime) {
		try {
			return objManager.listExpiredObjects(bucketName, prefix, nextMarker, MAX_KEY_SIZE, expiredTime);
		} catch (Exception e) {
			logger.error("", e);
			return null;
		}
	}

	public List<Metadata> listExpiredObjectVersions(String bucketName, String prefix, String nextMarker, String nextVersionId, long expiredTime) {
		try {
			return objManager.listExpiredObjectVersions(bucketName, prefix, nextMarker, nextVersionId, MAX_KEY_SIZE, expiredTime);
		} catch (Exception e) {
			logger.error("", e);
			return null;
		}
	}

	/**
	 * 삭제 마커만 남은 객체 목록을 조회한다.
	 * 
	 * @param bucketName
	 * @param prefix
	 * @param nextMarker
	 * @return
	 */
	public List<Metadata> listDeleteMarkers(String bucketName, String prefix, String nextMarker) {
		try {
			return objManager.listDeleteMarkedObjects(bucketName, prefix, nextMarker, MAX_KEY_SIZE);
		} catch (Exception e) {
			logger.error("", e);
			return null;
		}
	}

	public S3ObjectData getObject(String bucketName, String objectName) throws ResourceNotFoundException {
		return new S3ObjectData(objManager.getObject(bucketName, objectName));
	}

	public S3ObjectData getObject(String bucketName, String objectName, String versionId)
			throws ResourceNotFoundException {
		return new S3ObjectData(objManager.getObject(bucketName, objectName, versionId));
	}

	public ObjMultipart getMultipartInstance(String bucketName) {
		return objManager.getMultipartInsatance(bucketName);
	}
}
