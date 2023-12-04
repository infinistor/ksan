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

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.gw.format.AnalyticsConfiguration;
import com.pspace.ifs.ksan.gw.format.ObjectLockConfiguration;
import com.pspace.ifs.ksan.gw.format.ReplicationConfiguration;
import com.google.common.base.Strings;

import org.slf4j.LoggerFactory;

public class PutBucketAnalytics extends S3Request {

    public PutBucketAnalytics(S3Parameter s3Parameter) {
        super(s3Parameter);
        logger = LoggerFactory.getLogger(PutBucketAnalytics.class);
    }

    @Override
    public void process() throws GWException {
        logger.info(GWConstants.LOG_PUT_BUCKET_ANALYTICS_START);
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);

        String id = s3RequestData.getId();
        if (Strings.isNullOrEmpty(id)) {
            throw new GWException(GWErrorCode.INVALID_CONFIGURATION_ID, s3Parameter);
        }

		GWUtils.checkCors(s3Parameter);

		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}

		if (!checkPolicyBucket(GWConstants.ACTION_PUT_BUCKET_POLICY, s3Parameter)) {
			checkGrantBucket(true, GWConstants.GRANT_WRITE_ACP);
		}

		String analytics = s3RequestData.getAnalyticsXml();
        logger.info("Analytics id : {}, xml : {}", id, analytics);

        String modifyAnalytics = analytics.replace(GWConstants.XML_START, "")
            .replace(GWConstants.XML_ANALYTICS_STRING, "<AnalyticsConfiguration>");
        logger.debug("Analytics configuration : {}", modifyAnalytics);

        XmlMapper xmlMapper = new XmlMapper();
		AnalyticsConfiguration analyticsConfiguration = null;
		try {
			analyticsConfiguration = xmlMapper.readValue(analytics, AnalyticsConfiguration.class);
		} catch (JsonMappingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

        if (analyticsConfiguration != null) {
            if (Strings.isNullOrEmpty(analyticsConfiguration.Id)) {
                logger.error("analyticsConfiguration.Id is null");
                throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
            }

            if (id.equals(analyticsConfiguration.Id)) {
                if (!analyticsConfiguration.storageClassAnalysis.dataExport.outputSchemaVersion.equals("V_1")) {
                    logger.error("outputSchemaVersion is not V_1. {}", analyticsConfiguration.storageClassAnalysis.dataExport.outputSchemaVersion);
                    throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
                }
                if (!analyticsConfiguration.storageClassAnalysis.dataExport.destination.s3bucketDestination.format.equals("CSV")) {
                    logger.error("format is not CSV, {}", analyticsConfiguration.storageClassAnalysis.dataExport.destination.s3bucketDestination.format);
                    throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
                }
                String preAnaliytics = getBucketInfo().getAnalytics();
                if (Strings.isNullOrEmpty(preAnaliytics)) {
                    updateBucketAnalytics(bucket, modifyAnalytics);
                } else {
                    preAnaliytics += "\n" + modifyAnalytics;
                    updateBucketAnalytics(bucket, preAnaliytics);
                }
            } else {
                logger.error("id is different from analyticsConfiguration.Id - id : {}, analyticsConfiguration.Id : {}", id, analyticsConfiguration.Id);
                throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
            }
        } else {
            logger.error("analyticsConfiguration is null");
            throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
        }

		s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
    }
    
}
