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
package com.pspace.backend.Data;

import java.util.ArrayList;
import java.util.List;

public class LifecycleEventData implements BaseData {

    public long Index;
    public String BucketName;
    public String ObjectName;
    public String VersionId;
    public String UploadId;
        
    public LifecycleEventData(long Index, String BucketName, String ObjectName, String VersionId, String UploadId)
    {
        this.Index = Index;
        this.BucketName = BucketName;
        this.ObjectName = ObjectName;
        this.VersionId = VersionId;
        this.UploadId = UploadId;
    }

    public LifecycleEventData(String BucketName, String ObjectName)
    {
        this.Index = 0;
        this.BucketName = BucketName;
        this.ObjectName = ObjectName;
        this.VersionId = "";
        this.UploadId = "";
    }

    public LifecycleEventData(String BucketName, String ObjectName, String VersionId)
    {
        this.Index = 0;
        this.BucketName = BucketName;
        this.ObjectName = ObjectName;
        this.VersionId = VersionId;
        this.UploadId = "";
    }

    public LifecycleEventData(String BucketName, String ObjectName, String VersionId, String UploadId)
    {
        this.Index = 0;
        this.BucketName = BucketName;
        this.ObjectName = ObjectName;
        this.VersionId = VersionId;
        this.UploadId = UploadId;
    }

    @Override
    public void Init() {
        Index = 0;
        BucketName = "";
        ObjectName = "";
        VersionId = "";
        UploadId = "";
    }

    @Override
    public List<Object> GetInsertDBParameters() {
        var params = new ArrayList<Object>();
        params.add(BucketName);
        params.add(ObjectName);
        params.add(VersionId);
        params.add(UploadId);

        return params;
    }

    @Override
    public String toString() {
        return String.format(
        "%s{\n" + 
            "\t%s : %d,\n" + 
            "\t%s : %s,\n" + 
            "\t%s : %s,\n" + 
            "\t%s : %s,\n" + 
            "\t%s : %s\n" + 
        "}",
        "MultipartData",
        "Index"     , Index,
        "BucketName", BucketName,
        "ObjectName", ObjectName,
        "VersionId" , VersionId,
        "UploadId"  , UploadId
        );
    }
}
