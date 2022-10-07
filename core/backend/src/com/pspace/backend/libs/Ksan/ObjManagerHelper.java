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
package com.pspace.backend.libs.Ksan;

import java.sql.SQLException;
import java.util.List;

import com.pspace.backend.libs.Data.S3.S3BucketData;
import com.pspace.backend.libs.Data.S3.S3ObjectData;
import com.pspace.ifs.ksan.objmanager.Bucket;
import com.pspace.ifs.ksan.objmanager.ObjManagerConfig;
import com.pspace.ifs.ksan.objmanager.ObjManagerUtil;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

public class ObjManagerHelper {
	private static ObjManagerUtil ObjManager;
	
	public static ObjManagerHelper getInstance() {
		return LazyHolder.INSTANCE;
	}

	private static class LazyHolder {
		private static final ObjManagerHelper INSTANCE = new ObjManagerHelper();
	}

	public void init(ObjManagerConfig config) throws Exception
	{
		ObjManager = new ObjManagerUtil(config);
	}

	public List<Bucket> getBucketList()
	{
		return ObjManager.getBucketList();
	}

	public S3BucketData getBucket(String bucketName) throws ResourceNotFoundException, SQLException
	{
		return new S3BucketData(ObjManager.getBucket(bucketName));
	}

	public S3ObjectData getObject(String bucketName, String objectName) throws ResourceNotFoundException{
		return new S3ObjectData(ObjManager.getObject(bucketName, objectName));
	}
	public S3ObjectData getObject(String bucketName, String objectName, String versionId) throws ResourceNotFoundException{
		return new S3ObjectData(ObjManager.getObject(bucketName, objectName, versionId));
	}
}
