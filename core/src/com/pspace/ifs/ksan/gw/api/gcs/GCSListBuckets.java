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
package com.pspace.ifs.ksan.gw.api.gcs;

import jakarta.servlet.http.HttpServletResponse;

import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.libs.identity.S3BucketSimpleInfo;

import java.nio.charset.Charset;
import java.util.List;

import org.slf4j.LoggerFactory;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class GCSListBuckets extends GCSRequest {

    public GCSListBuckets(S3Parameter s3Parameter) {
        super(s3Parameter);
        logger = LoggerFactory.getLogger(GCSListBuckets.class);
    }

    @Override
    public void process() throws GWException {
        logger.info(GWConstants.LOG_GCS_LIST_BUCKET_START);

        List<S3BucketSimpleInfo> bucketList = listBucketSimpleInfo(s3Parameter.getUser().getUserName(), s3Parameter.getUser().getUserId());

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("kind", "storage#buckets");
        // jsonObject.put("nextPageToken", null);
        JSONArray jsonArray = new JSONArray();
        for (S3BucketSimpleInfo bucket : bucketList) {
            JSONObject json = new JSONObject();
            JSONArray array = new JSONArray();
    
            json.put("name", bucket.getBucketName());
            json.put("acl", array);
            json.put("cors", array);
            json.put("defaultObjectAcl", array);
            json.put("id", bucket.getBucketName());
            jsonArray.add(json);
            logger.debug("bucket : {}", bucket.getBucketName());
        }
        jsonObject.put("items", jsonArray);

        s3Parameter.getResponse().setContentType(GWConstants.JSON_CONTENT_TYPE);
        try {
            s3Parameter.getResponse().getOutputStream().write(jsonObject.toString().getBytes(Charset.forName(Constants.UTF_8)));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
        }

        s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
    }
    
}
