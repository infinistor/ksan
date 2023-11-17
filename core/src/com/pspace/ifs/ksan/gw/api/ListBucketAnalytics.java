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

import org.slf4j.LoggerFactory;

public class ListBucketAnalytics extends S3Request {

    public ListBucketAnalytics(S3Parameter s3Parameter) {
        super(s3Parameter);
        logger = LoggerFactory.getLogger(ListBucketAnalytics.class);
    }

    @Override
    public void process() throws GWException {
        logger.info(GWConstants.LOG_LIST_BUCKET_ANALYTICS_START);
        String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);
        
        String analyticsList = getBucketInfo().getAnalytics();
        logger.debug("analyticsList: " + analyticsList);
        
        String analyticsInfo = GWConstants.XML_VERSION_FULL;
        analyticsInfo += "<ListBucketAnalyticsConfigurationsResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">";
        analyticsInfo += "<IsTruncated>false</IsTruncated>";

        String[] analyticsIds = analyticsList.split("\\n");
        for (String analyticsId : analyticsIds) {
            analyticsInfo += analyticsId;
        }

        analyticsInfo += "</ListBucketAnalyticsConfigurationsResult>";
        logger.debug("analyticsInfo: " + analyticsInfo);

        try {
            s3Parameter.getResponse().setContentType(GWConstants.XML_CONTENT_TYPE);
            s3Parameter.getResponse().getOutputStream().write(analyticsInfo.getBytes(StandardCharsets.UTF_8));
            s3Parameter.getResponse().getOutputStream().flush();
            s3Parameter.getResponse().getOutputStream().close();
        } catch (IOException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }
        
        s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
        // XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        // try (Writer writer = s3Parameter.getResponse().getWriter()) {
		// 	s3Parameter.getResponse().setContentType(GWConstants.XML_CONTENT_TYPE);
			
		// 	XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(writer);
		// 	xmlStreamWriter.writeStartDocument();
		// 	xmlStreamWriter.writeStartElement(GWConstants.LIST_BUCKET_ANALYTICS_RESULT);
		// 	xmlStreamWriter.writeDefaultNamespace(GWConstants.AWS_XMLNS);
        //     writeSimpleElement(xmlStreamWriter, GWConstants.XML_IS_TRUNCATED, GWConstants.XML_FALSE);

		// 	if (bucketList != null) {
		// 		logger.debug(GWConstants.LOG_LIST_BUCKET_ANALYTICS_SIZE, bucketList.size());
		// 		for (Map<String, String> mapAanlytics : bucketList) {
        //             for (Map.Entry<String, String> entry : mapAanlytics.entrySet()) { 
        //                 XmlMapper xmlMapper = new XmlMapper();
        //                 AnalyticsConfiguration analyticsConfiguration = null;
        //                 try {
        //                     analyticsConfiguration = xmlMapper.readValue(entry.getValue(), AnalyticsConfiguration.class);
        //                     logger.debug("AnalyticsConfiguration: " + entry.getValue());
        //                 } catch (JsonMappingException e) {
        //                     PrintStack.logging(logger, e);
        //                     throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
        //                 } catch (JsonProcessingException e) {
        //                     PrintStack.logging(logger, e);
        //                     throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
        //                 }

        //                 xmlStreamWriter.writeStartElement(GWConstants.LIST_BUCKET_ANANLYTICS_CONFIGURATION);
        //                 writeSimpleElement(xmlStreamWriter, "Id", analyticsConfiguration.Id);
        //                 if (analyticsConfiguration.filter != null) {
        //                     xmlStreamWriter.writeStartElement("Filter");
        //                     if (analyticsConfiguration.filter.prefix != null) {
        //                         writeSimpleElement(xmlStreamWriter, "Prefix", analyticsConfiguration.filter.prefix);
        //                     }
        //                     if (analyticsConfiguration.filter.tag != null) {
        //                         xmlStreamWriter.writeStartElement("Tag");
        //                         writeSimpleElement(xmlStreamWriter, "Key", analyticsConfiguration.filter.tag.key);
        //                         writeSimpleElement(xmlStreamWriter, "Value", analyticsConfiguration.filter.tag.value);
        //                         xmlStreamWriter.writeEndElement();
        //                     }
        //                     xmlStreamWriter.writeEndElement();
        //                 }
        //                 xmlStreamWriter.writeStartElement("StorageClassAnalysis");
        //                 xmlStreamWriter.writeStartElement("DataExport");
        //                 xmlStreamWriter.writeStartElement("Destination");
        //                 xmlStreamWriter.writeStartElement("S3BucketDestination");
        //                 writeSimpleElement(xmlStreamWriter, "Bucket", analyticsConfiguration.storageClassAnalysis.dataExport.destination.s3bucketDestination.bucket);
        //                 writeSimpleElement(xmlStreamWriter, "BucketAccountId", analyticsConfiguration.storageClassAnalysis.dataExport.destination.s3bucketDestination.bucketAccountId);
        //                 writeSimpleElement(xmlStreamWriter, "Format", analyticsConfiguration.storageClassAnalysis.dataExport.destination.s3bucketDestination.format);
        //                 writeSimpleElement(xmlStreamWriter, "Prefix", analyticsConfiguration.storageClassAnalysis.dataExport.destination.s3bucketDestination.prefix);
        //                 xmlStreamWriter.writeEndElement(); // S3BucketDestination
        //                 xmlStreamWriter.writeEndElement(); // Destination
        //                 writeSimpleElement(xmlStreamWriter, "OutputSchemaVersion", analyticsConfiguration.storageClassAnalysis.dataExport.outputSchemaVersion);
        //                 xmlStreamWriter.writeEndElement(); // DataExport
        //                 xmlStreamWriter.writeEndElement(); // StorageClassAnalysis
        //                 xmlStreamWriter.writeEndElement(); // AnalyticsConfiguration
        //             }
		// 		}
		// 	}
		// 	xmlStreamWriter.writeEndElement();
		// 	xmlStreamWriter.flush();
		// } catch (XMLStreamException | IOException e) {
        //     PrintStack.logging(logger, e);
		// 	throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		// }
    }
}
