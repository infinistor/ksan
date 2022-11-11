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

package com.pspace.ifs.ksan.gw.api.azure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.ifs.ksan.gw.identity.AzuParameter;
import com.pspace.ifs.ksan.gw.utils.AzuConstants;
import com.pspace.ifs.ksan.gw.exception.*;

public class AzuRequestFactory {
    private final Logger logger;

    public AzuRequestFactory() {
        logger = LoggerFactory.getLogger(AzuRequestFactory.class);
    }

    public AzuRequest createRequest(AzuParameter parameter) throws AzuException {
        logger.info("method: {}, category : {}, comp : {}, restype : {}", parameter.getMethod(), parameter.getPathCategory(), parameter.getComp(), parameter.getRestype());
        switch (parameter.getMethod()) {
            case AzuConstants.HTTP_METHOD_DELETE:
            if (parameter.getRestype().equals(AzuConstants.RESTYPE_CONTAINER)) {
                    return new DeleteContainer(parameter);
                } else if (parameter.getPathCategory().equals(AzuConstants.PATH_CATEGORY_BLOB)) {
                    return new DeleteBlob(parameter);
                }
                break;

            case AzuConstants.HTTP_METHOD_GET:
                if (parameter.getRestype().equals(AzuConstants.RESTYPE_SERVICE) && parameter.getComp().equals(AzuConstants.COMP_PROPERTIES)) {
                    return new GetProperties(parameter);
                } else if (parameter.getRestype().equals(AzuConstants.RESTYPE_ACCOUNT) && parameter.getComp().equals(AzuConstants.COMP_PROPERTIES)) {
                    return new SharedKeyAuth(parameter);
                } else if (parameter.getPathCategory().equals(AzuConstants.PATH_CATEGORY_ROOT)) {
                    return new ListContainers(parameter);
                } else if (parameter.getPathCategory().equals(AzuConstants.PATH_CATEGORY_CONTAINER)) {
                    return new ListBlobs(parameter);
                } else if (parameter.getPathCategory().equals(AzuConstants.PATH_CATEGORY_BLOB)) {
                    return new GetBlob(parameter);
                }
                break;

            case AzuConstants.HTTP_METHOD_HEAD:
                if (parameter.getPathCategory().equals(AzuConstants.PATH_CATEGORY_CONTAINER)) {
                    return new HeadContainer(parameter);
                } else if (parameter.getPathCategory().equals(AzuConstants.PATH_CATEGORY_BLOB)) {
                    return new HeadBlob(parameter);
                }
                break;

            case AzuConstants.HTTP_METHOD_PUT:
                if (parameter.getPathCategory().equals(AzuConstants.PATH_CATEGORY_CONTAINER)) {
                    return new CreateContainer(parameter);
                } else if (parameter.getPathCategory().equals(AzuConstants.PATH_CATEGORY_BLOB)) {
                    if (parameter.getComp().equals(AzuConstants.COMP_BLOCK)) {
                        return new PutBlock(parameter);
                    } else if (parameter.getComp().equals(AzuConstants.COMP_BLOCKLIST)) {
                        return new PutBlockList(parameter);
                    } else {
                        return new PutBlob(parameter);
                    }
                }
                break;

            default:
                break;
        }

        logger.error("Unknown method: {}", parameter.getMethod());
        throw new AzuException(AzuErrorCode.NOT_IMPLEMENTED, parameter);
    }
}

