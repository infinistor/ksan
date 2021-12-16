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
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.pspace.backend.Data.MultipartData;

public class MultipartTable implements BaseTable {

    protected final static String DB_TABLE_NAME = "MULTIPARTS";
    protected final static String DB_BUCKET     = "bucket";
    protected final static String DB_KEYNAME    = "objKey";
    protected final static String DB_CHANGETIME = "changeTime";
    protected final static String DB_COMPLETED  = "completed";
    protected final static String DB_UPLOADID   = "uploadid";
    @Override
    public String GetCreateTableQuery() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String GetInsertQuery() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String GetSelectQuery(long Index) {
        return String.format("SELECT %s, %s, %s, %s FROM %s WHERE %s = FALSE LIMIT %d, %d;",
        DB_BUCKET, DB_KEYNAME, DB_CHANGETIME, DB_UPLOADID, DB_TABLE_NAME, DB_COMPLETED, Index, Index + 1000);
    }
    
    public String GetSelectQuery(String BucketName, long Index) {
        return String.format("SELECT %s, %s, %s FROM %s WHERE %s = FALSE and %s = '%s' LIMIT %d, %d;",
                            DB_KEYNAME, DB_CHANGETIME, DB_UPLOADID, DB_TABLE_NAME, DB_COMPLETED, DB_BUCKET, BucketName, Index, Index + 1000);
    }


    @Override
    public String GetDeleteQuery(long Index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String GetClearQuery(long LastIndex) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public static List<MultipartData> GetList(String BucketName, List<HashMap<String, Object>> resultList)
	{
        if (resultList == null) return null;
		var MyList = new ArrayList<MultipartData>();

        for(var result : resultList)
        {
            var KeyName      = (String)result.get(DB_KEYNAME);
            var LastModified = (Date)result.get(DB_CHANGETIME);
            var UploadId     = (String)result.get(DB_UPLOADID);
            MyList.add(new MultipartData(BucketName, KeyName, LastModified, UploadId));
        }
        return MyList;
	}
}
