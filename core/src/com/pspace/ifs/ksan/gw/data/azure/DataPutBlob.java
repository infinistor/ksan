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

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.exception.AzuException;
import com.pspace.ifs.ksan.gw.identity.AzuParameter;
import com.pspace.ifs.ksan.gw.utils.AzuConstants;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

public class DataPutBlob extends AzuDataRequest {
    private String contentType;
    private String contentMD5;

    public DataPutBlob(AzuParameter parameter) throws AzuException {
        super(parameter);
        logger = LoggerFactory.getLogger(DataPutBlob.class);
        contentType = "";
        contentMD5 = "";
    }

    @Override
	public String getContentLength() {
		return super.getContentLength();
	}

    @Override
    public void extract() throws AzuException {
        for (String headerName : Collections.list(parameter.getRequest().getHeaderNames())) {
            if (headerName.equalsIgnoreCase(AzuConstants.X_MS_BLOB_CONTENT_TYPE)) {
                contentType = Strings.nullToEmpty(parameter.getRequest().getHeader(headerName));
            } else if (headerName.equalsIgnoreCase(AzuConstants.X_MS_BLOB_CONTENT_MD5)) {
                contentMD5 = Strings.nullToEmpty(parameter.getRequest().getHeader(headerName));
            }
        }
    }

    public String getContentType() {
        return contentType;
    }

    public String getContentMD5() {
        return contentMD5;
    }
}

