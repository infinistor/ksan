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
package com.pspace.ifs.ksan.gw.api;

import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.AnalyticsConfiguration;
import com.pspace.ifs.ksan.libs.identity.S3BucketSimpleInfo;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.google.common.base.Strings;

import org.slf4j.LoggerFactory;

public class ListBucketInventory extends S3Request {

    public ListBucketInventory(S3Parameter s3Parameter) {
        super(s3Parameter);
        logger = LoggerFactory.getLogger(ListBucketInventory.class);
    }

    @Override
    public void process() throws GWException {
        logger.info(GWConstants.LOG_LIST_BUCEKT_INVENTORY_START);
        String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);

        String inventoryList = getBucketInfo().getInventory();
        
        String inventoryInfo = GWConstants.XML_VERSION_FULL;
        inventoryInfo += "<ListInventoryConfigurationsResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">";
        inventoryInfo += "<IsTruncated>false</IsTruncated>";
        
        if (!Strings.isNullOrEmpty(inventoryList)) {
            String[] inventoryIds = inventoryList.split("\\n");
            for (String inventoryId : inventoryIds) {
                inventoryInfo += inventoryId;
            }
        }

        inventoryInfo += "</ListInventoryConfigurationsResult>";
        logger.debug("inventoryInfo: " + inventoryInfo);
        try {
            s3Parameter.getResponse().setContentType(GWConstants.XML_CONTENT_TYPE);
            s3Parameter.getResponse().getOutputStream().write(inventoryInfo.getBytes(StandardCharsets.UTF_8));
            s3Parameter.getResponse().getOutputStream().flush();
            s3Parameter.getResponse().getOutputStream().close();
        } catch (IOException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }
        
        s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
    }
    
}
