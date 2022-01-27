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

import com.pspace.backend.Data.LifecycleEventData;

public class LifecycleEventTable implements BaseTable{

    protected final static String DB_TABLE_NAME = "lifecycle_event";
    protected final static String DB_ID         = "id";
    protected final static String DB_BUCKET     = "bucketname";
    protected final static String DB_OBJECT     = "objectname";
    protected final static String DB_VERSION_ID = "versionid";
    protected final static String DB_UPLOAD_ID  = "uploadid";

    @Override
    public String GetCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME + " ( " +
            DB_ID         + " bigint auto_increment primary key, " +
            DB_BUCKET     + " varchar(64) null, " +
            DB_OBJECT     + " varchar(2048) null, " +
            DB_VERSION_ID + " varchar(32) not null, " +
            DB_UPLOAD_ID  + " varchar(32) null);" ;
    }

    @Override
    public String GetInsertQuery() {
        return String.format("INSERT INTO %s(%s, %s, %s, %s) VALUES(?, ?, ?, ?)",
                            DB_TABLE_NAME, DB_BUCKET, DB_OBJECT, DB_VERSION_ID, DB_UPLOAD_ID);
    }

    @Override
    public String GetSelectQuery(long Index) {
        return String.format("SELECT * FROM %s LIMIT %d, %d;", DB_TABLE_NAME, Index, Index + 1000);
    }

    @Override
    public String GetDeleteQuery(long Index) {
        return String.format("Delete from %s where ID = %d;", DB_TABLE_NAME, Index);
    }

    @Override
    public String GetClearQuery(long LastIndex) {
        if(LastIndex == 0) return String.format("Delete from %s;", DB_TABLE_NAME);
        else               return String.format("Delete from %s where ID <= %d;", DB_TABLE_NAME, LastIndex);
    }

    public static List<LifecycleEventData> GetList(List<HashMap<String, Object>> resultList)
	{
        if (resultList == null) return null;
		var MyList = new ArrayList<LifecycleEventData>();

        for(var result : resultList)
        {
            var Index      = (long)result.get(DB_ID);
            var BucketName = (String)result.get(DB_BUCKET);
            var ObjectName = (String)result.get(DB_OBJECT);
            var VersionId  = (String)result.get(DB_VERSION_ID);
            var UploadId   = (String)result.get(DB_UPLOAD_ID);

            MyList.add(new LifecycleEventData(Index, BucketName, ObjectName, VersionId, UploadId));
        }
        return MyList;
	}
}
