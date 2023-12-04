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
import com.pspace.ifs.ksan.gw.format.Inventory;
import com.pspace.ifs.ksan.gw.format.ObjectLockConfiguration;
import com.pspace.ifs.ksan.gw.format.ReplicationConfiguration;
import com.google.common.base.Strings;

import org.slf4j.LoggerFactory;

public class PutBucketInventory extends S3Request {

    public PutBucketInventory(S3Parameter s3Parameter) {
        super(s3Parameter);
        logger = LoggerFactory.getLogger(PutBucketInventory.class);
    }

    @Override
    public void process() throws GWException {
        logger.info(GWConstants.LOG_PUT_BUCKET_INVENTORY_START);

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

		String inventoryXml = s3RequestData.getInventoryXml();
        logger.info("inventory id : {}, xml : {}", id, inventoryXml);

        String modifyInventoryXml = inventoryXml.replace(GWConstants.XML_START, "")
            .replace(GWConstants.XML_INVENTORY_STRING, "<InventoryConfiguration>");
        logger.debug("Analytics configuration : {}", modifyInventoryXml);

        XmlMapper xmlMapper = new XmlMapper();
		Inventory inventory = null;
		try {
			inventory = xmlMapper.readValue(inventoryXml, Inventory.class);
            // InventoryConfiguration ic check
            if (Strings.isNullOrEmpty(inventory.id)) {
                logger.error("xml inventory id is empty");
                throw new GWException(GWErrorCode.INVALID_CONFIGURATION_ID, s3Parameter);
            }
            // check valid optional fieleds
            if (inventory.optionalFields != null) {
                for (String field : inventory.optionalFields.fields) {
                    if (!GWConstants.INVENTORY_OPTIONS.contains(field)) {
                        logger.error("xml inventory optional field is invalid : {}", field);
                        throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
                    }
                }
            }
            // check options version
            if (inventory.includedObjectVersions != null) {
                if (!GWConstants.INVENTORY_INCLUDE_OBJECT_VERSIONS.contains(inventory.includedObjectVersions)) {
                    logger.error("xml inventory includedObjectVersions is invalid : {}", inventory.includedObjectVersions);
                    throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
                }
            }
            // check valid schedule
            if (inventory.schedule != null) {
                if (!GWConstants.INVENTORY_SCHEDULE.contains(inventory.schedule.frequency)) {
                    logger.error("xml inventory schedule frequency is invalid : {}", inventory.schedule.frequency);
                    throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
                }
            }
            // check valid format
            if (inventory.destination != null &&  inventory.destination.s3BucketDestination != null && inventory.destination.s3BucketDestination.format != null) {
                if (!GWConstants.INVENTORY_FORMAT.contains(inventory.destination.s3BucketDestination.format)) {
                    logger.error("xml inventory destination s3BucketDestination format is invalid : {}", inventory.destination.s3BucketDestination.format);
                    throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
                }
            }
            // check target bucket
            if (inventory.destination != null &&  inventory.destination.s3BucketDestination != null && inventory.destination.s3BucketDestination.bucket != null) {
                logger.info("inventory destination s3BucketDestination bucket : {}", inventory.destination.s3BucketDestination.bucket);
                String[] arnPath = inventory.destination.s3BucketDestination.bucket.split(":", -1);
                if (arnPath.length != 6) {
					logger.error("arn path length is invalid : {}", arnPath.length);
					throw new GWException(GWErrorCode.INVALID_REQUEST, s3Parameter);
				}

				if (Strings.isNullOrEmpty(arnPath[5])) {
					logger.error("bucket is empty");
					throw new GWException(GWErrorCode.INVALID_REQUEST, s3Parameter);
				}

                if (getBucket(arnPath[5]) == null) {
                    logger.error("bucket is not exist : {}", arnPath[5]);
                    throw new GWException(GWErrorCode.NO_SUCH_BUCKET, s3Parameter);
                }

                String preInventory = getBucketInfo().getInventory();
                if (Strings.isNullOrEmpty(preInventory)) {
                    updateBucketInventory(bucket, modifyInventoryXml);
                } else {
                    preInventory += "\n" + modifyInventoryXml;
                    updateBucketInventory(bucket, preInventory);
                }
            }
		} catch (JsonMappingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}
    }
    
}
