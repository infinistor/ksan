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
package com.pspace.backend.Main;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.pspace.DB.DBManager;
import com.pspace.backend.Data.LifecycleFailedData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LifecycleSender {
    private final Logger logger;
    private final AmazonS3 Client;
    private final DBManager DB;

    public LifecycleSender(DBManager DB, String S3URL, String AccessKey, String SecretKey)
    {
        this.DB = DB;
        logger = LoggerFactory.getLogger(LifecycleSender.class);
        Client = CreateClient(S3URL, AccessKey, SecretKey);
    }

    public void Start()
    {
        while(true)
        {
            var EventList = DB.GetLifecycleEventList();

            for(var Event : EventList)
            {
                logger.debug(Event.toString());
                //결과값 초기화
                String Result = "";

                // UploadId가 존재하지 않을 경우 오브젝트 삭제
                if (Event.UploadId.isBlank())
                {
                    //VersionId가 존재하지 않을 경우 일반적인 삭제로 취급 (3회 시도)
                    if (Event.VersionId.isBlank()) Result = DeleteObject(Event.BucketName, Event.ObjectName);

                    //VersionId가 존재할 경우 버전아이디를 포함한 삭제로 취급 (3회 시도)
                    else Result = DeleteObjectVersion(Event.BucketName, Event.ObjectName, Event.VersionId);
                }
                // UploadId가 존재할 경우 Multipart 삭제
                else Result = AbortMultipartUpload(Event.BucketName, Event.ObjectName, Event.UploadId);
                
                //반환값이 비어있지 않을 경우 - 에러가 발생할 경우
                if (!Result.isBlank()) DB.InsertLifecycleFailed(new LifecycleFailedData(Event, Result));
            }
            // DB에서 가져온 목록이 1000개 이하일경우 
            if(EventList.size() < 1000) break;
        }
    }

    /*************************************** Utility *******************************************/
    private void Delay(int Time)
    {
        try{
            Thread.sleep(Time);
        }catch(Exception e){
        }
    }

    protected AmazonS3 CreateClient(String S3URL, String AccessKey, String SecretKey)
    {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(AccessKey, SecretKey);

        return AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(S3URL, ""))
                .withPathStyleAccessEnabled(true).build();
    }

    protected String DeleteObject(String BucketName, String ObjectName)
    {
        var Result = "";
        for(int i = 0; i < 3; i++)
        {
            try {
                Client.deleteObject(BucketName, ObjectName);
                return Result;
            } catch (Exception e) {
                logger.error("", e);
                Result = e.getMessage();
            }
        }
        return Result;
    }

    protected String DeleteObjectVersion(String BucketName, String ObjectName, String VersionId)
    {
        var Result = "";
        for(int i = 0; i < 3; i++)
        {
            try {
                Client.deleteVersion(BucketName, ObjectName, VersionId);
                return Result;
            } catch (Exception e) {
                logger.error("", e);
                Result = e.getMessage();
            }
        }
        return Result;
    }

    protected String AbortMultipartUpload(String BucketName, String ObjectName, String UploadId)
    {
        var Result = "";
        for(int i = 0; i < 3; i++)
        {
            try {
                Client.abortMultipartUpload(new AbortMultipartUploadRequest(BucketName, ObjectName, UploadId));
                return Result;
            } catch (Exception e) {
                logger.error("", e);
                Result = e.getMessage();
            }
        }
        return Result;
    }
}
