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

package com.pspace.ifs.watcher.utils;

import com.pspace.ifs.watcher.db.MariaDB;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class AsyncHandler {
    //private final static Logger logger = LoggerFactory.getLogger(AsyncHandler.class);

    public static void accountMin(MariaDB s3db) {
        // insert bucket io meter
        s3db.bucketUsageMeterMin();
        
        // insert bucket api meter
        s3db.bucketAPIMeterMin();

        // insert bucket io meter
        s3db.bucketIOMeterMin();
    }

    public static void accountHour(MariaDB s3db) {
        // insert bucket io meter
        s3db.bucketUsageMeterHour();
        
        // insert bucket api meter
        s3db.bucketAPIMeterHour();
        
        // insert bucket io meter
        s3db.bucketIOMeterHour();
    }
}