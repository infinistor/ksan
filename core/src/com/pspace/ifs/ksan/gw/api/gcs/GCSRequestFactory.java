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

import com.google.common.base.Strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

public class GCSRequestFactory {
    private final Logger logger;

    public GCSRequestFactory() {
        logger = LoggerFactory.getLogger(GCSRequestFactory.class);
    }

    public GCSRequest createRequest(S3Parameter parameter) throws GWException {
        logger.info("uri : {}, path category : {}", parameter.getUri(), parameter.getPathCategory());
        switch (parameter.getMethod()) {
            case GWConstants.METHOD_DELETE:
                logger.info("DELETE request");
                if (GWConstants.CATEGORY_BUCKET.equals(parameter.getPathCategory())) {
                    GCSDeleteBucket deleteBucket = new GCSDeleteBucket(parameter);
                    return deleteBucket;
                } else if (GWConstants.CATEGORY_OBJECT.equals(parameter.getPathCategory())) {
                    GCSDeleteObject deleteObject = new GCSDeleteObject(parameter);
                    return deleteObject;
                } else {
                    logger.error("Unknown path category: {}", parameter.getPathCategory());
                    throw new GWException(GWErrorCode.NOT_IMPLEMENTED, parameter);
                }

            case GWConstants.METHOD_GET:
                logger.info("GET request");
                if (GWConstants.CATEGORY_BUCKET.equals(parameter.getPathCategory())) {
                    GCSListBuckets listBucket = new GCSListBuckets(parameter);
                    return listBucket;
                } else if (GWConstants.CATEGORY_OBJECT.equals(parameter.getPathCategory())) {
                    if (Strings.isNullOrEmpty(parameter.getObjectName())) {
                        GCSListObjects listObjects = new GCSListObjects(parameter);
                        return listObjects;
                    } else {
                        GCSGetObject headObject = new GCSGetObject(parameter);
                        return headObject;
                    }
                } else {
                    logger.error("Unknown path category: {}", parameter.getPathCategory());
                    throw new GWException(GWErrorCode.NOT_IMPLEMENTED, parameter);
                }

            case GWConstants.METHOD_HEAD:
                logger.info("HEAD request");
                if (GWConstants.CATEGORY_BUCKET.equals(parameter.getPathCategory())) {

                } else if (GWConstants.CATEGORY_OBJECT.equals(parameter.getPathCategory())) {

                } else {
                    logger.error("Unknown path category: {}", parameter.getPathCategory());
                    throw new GWException(GWErrorCode.NOT_IMPLEMENTED, parameter);
                }
                break;

            case GWConstants.METHOD_PUT:
                logger.info("PUT request");
                if (GWConstants.CATEGORY_BUCKET.equals(parameter.getPathCategory())) {

                } else if (GWConstants.CATEGORY_OBJECT.equals(parameter.getPathCategory())) {
                    GCSPutObject putObject = new GCSPutObject(parameter);
                    return putObject;
                } else {
                    logger.error("Unknown path category: {}", parameter.getPathCategory());
                    throw new GWException(GWErrorCode.NOT_IMPLEMENTED, parameter);
                }
                
                break;

            case GWConstants.METHOD_POST:
                logger.info("POST request");
                if (GWConstants.CATEGORY_BUCKET.equals(parameter.getPathCategory())) {
                    GCSCreateBucket createBucket = new GCSCreateBucket(parameter);
                    return createBucket;
                } else if (GWConstants.CATEGORY_OBJECT.equals(parameter.getPathCategory())) {
                    GCSPostObject putObject = new GCSPostObject(parameter);
                    return putObject;
                } else {
                    logger.error("Unknown path category: {}", parameter.getPathCategory());
                    throw new GWException(GWErrorCode.NOT_IMPLEMENTED, parameter);
                }

            default:
                break;
        }

        logger.error("Unknown method: {}", parameter.getMethod());
        throw new GWException(GWErrorCode.NOT_IMPLEMENTED, parameter);
    }
}
