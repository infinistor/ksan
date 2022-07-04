/*
* Copyright 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE.md for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.DB.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.pspace.backend.Data.BucketData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BucketTable implements BaseTable {
	static final Logger logger = LoggerFactory.getLogger(BucketTable.class);

	protected final static String DB_TABLE_NAME = "BUCKETS";
	protected final static String DB_BUCKET = "name";
	protected final static String DB_BUCKET_LIFECYCLE = "lifecycle";

	@Override
	public String GetCreateTableQuery() {
		return "";
	}

	@Override
	public String GetInsertQuery() {
		return "";
	}

	@Override
	public String GetSelectQuery(long Index) {
		return String.format("SELECT %s, convert(%s using utf8) as %s FROM %s WHERE %s IS NOT NULL LIMIT %d, %d;",
				DB_BUCKET, DB_BUCKET_LIFECYCLE, DB_BUCKET_LIFECYCLE, DB_TABLE_NAME, DB_BUCKET_LIFECYCLE, Index,
				Index + 1000);
	}

	@Override
	public String GetDeleteQuery(long Index) {
		return "";
	}

	@Override
	public String GetClearQuery(long LastIndex) {
		return "";
	}

	public static String GetSelectOneQuery(String bucketName) {
		return String.format("SELECT %s, convert(%s using utf8) as %s FROM %s WHERE %s = '%s';",
				DB_BUCKET, DB_BUCKET_LIFECYCLE, DB_BUCKET_LIFECYCLE, DB_TABLE_NAME, DB_BUCKET, bucketName);
	}

	public static List<BucketData> GetList(List<HashMap<String, Object>> resultList) {
		if (resultList == null)
			return null;
		var MyList = new ArrayList<BucketData>();

		for (var result : resultList) {
			var BucketName = (String) result.get(DB_BUCKET);
			var StrLifecycle = (String) result.get(DB_BUCKET_LIFECYCLE);
			var BucketInfo = new BucketData(BucketName);

			BucketInfo.setLifecycleConfiguration(StrLifecycle);

			MyList.add(BucketInfo);
		}
		return MyList;
	}

	public static BucketData GetData(List<HashMap<String, Object>> resultList) {
		if (resultList == null)
			return null;
		if (resultList.size() > 2)
			return null;

		var result = resultList.get(0);

		var bucketName = (String) result.get(DB_BUCKET);
		var strLifecycle = (String) result.get(DB_BUCKET_LIFECYCLE);
		var bucketInfo = new BucketData(bucketName);

		bucketInfo.setLifecycleConfiguration(strLifecycle);
		return bucketInfo;
	}
}
