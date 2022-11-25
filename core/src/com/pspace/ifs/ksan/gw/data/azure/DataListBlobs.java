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

package com.pspace.ifs.ksan.gw.data.azure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.ifs.ksan.gw.exception.AzuException;
import com.pspace.ifs.ksan.gw.identity.AzuParameter;
import com.pspace.ifs.ksan.gw.utils.AzuConstants;

public class DataListBlobs extends AzuDataRequest {
    String prefix;
    String delimiter;
    String maxResults;
    String include;

    public DataListBlobs(AzuParameter parameter) throws AzuException {
        super(parameter);
        logger = LoggerFactory.getLogger(DataHeadContainer.class);
    }

    @Override
    public void extract() throws AzuException {
        prefix = parameter.getRequest().getParameter(AzuConstants.PARAMETER_PREFIX);
        delimiter = parameter.getRequest().getParameter(AzuConstants.PARAMETER_DELIMITER);
        maxResults = parameter.getRequest().getParameter(AzuConstants.PARAMETER_MAX_RESULTS);
        include = parameter.getRequest().getParameter(AzuConstants.PARAMETER_INCLUDE);
    }

    public String getPrefix() {
        return prefix;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public String getMaxResults() {
        return maxResults;
    }

    public String getInclude() {
        return include;
    }
}
