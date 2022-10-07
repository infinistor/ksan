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
package com.pspace.ifs.ksan.gw.utils;

import java.util.concurrent.CompletableFuture;

import com.pspace.ifs.ksan.gw.db.GWDB;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.ifs.ksan.libs.PrintStack;

public class AsyncHandler {
    private final static Logger logger = LoggerFactory.getLogger(AsyncHandler.class);

    public static CompletableFuture<Integer> s3logging(S3Parameter s3Parameter) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        new Thread( () -> {
            if (GWConfig.getInstance().isEventLog()) {
                // GWDB gwDB = GWUtils.getDBInstance();
                // try {
                //     gwDB.putS3logging(s3Parameter);
                // } catch (GWException e) {
                //     PrintStack.logging(logger, e);
                // }
                GWLogging.getInstance().sendLog(s3Parameter);
            }
        }).start();

        return future;
    }
}
