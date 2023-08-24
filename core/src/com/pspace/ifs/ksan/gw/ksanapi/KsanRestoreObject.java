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
package com.pspace.ifs.ksan.gw.ksanapi;

import jakarta.servlet.http.HttpServletResponse;

import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.google.common.base.Strings;
import org.slf4j.LoggerFactory;
// import com.pspace.ifs.ksan.gw.object.S3ObjectOperation;
import com.pspace.ifs.ksan.gw.object.IObjectManager;
import com.pspace.ifs.ksan.gw.object.VFSObjectManager;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy;

import com.pspace.ifs.ksan.gw.identity.S3User;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.gw.utils.S3UserManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.pspace.ifs.ksan.gw.api.S3Request;

public class KsanRestoreObject extends S3Request {

    public KsanRestoreObject(S3Parameter s3Parameter) {
        super(s3Parameter);
		logger = LoggerFactory.getLogger(KsanRestoreObject.class);
    }
    
    @Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_ADMIN_RESTORE_OBJECT_START);
		
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);
		String object = s3Parameter.getObjectName();
		logger.debug(GWConstants.LOG_BUCKET_OBJECT, bucket, object);
		
		GWUtils.checkCors(s3Parameter);

        String versionId = s3RequestData.getVersionId();

        if (Strings.isNullOrEmpty(versionId)) {
            versionId = GWConstants.VERSIONING_DISABLE_TAIL;
        }

        Metadata objMeta = null;
		if (Strings.isNullOrEmpty(versionId)) {
			objMeta = open(bucket, object);
		} else {
			objMeta = open(bucket, object, versionId);
		}

		logger.debug(GWConstants.LOG_OBJECT_META, objMeta.toString());

        S3User user = S3UserManager.getInstance().getUserByName(getBucketInfo().getUserName());
        if (user == null) {
            throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
        }
        String diskpoolId = user.getUserDiskpoolId(GWConstants.AWS_TIER_STANTARD);
        s3Parameter.setUser(user);

		logger.debug("storage STANDARD diskpoolId : {}", diskpoolId);

        Metadata restoreObjMeta = null;
		restoreObjMeta = createLocal(diskpoolId, bucket, object, versionId);
		s3Parameter.setVersionId(versionId);

        logger.info("replica count : {}, primary disk id : {}", restoreObjMeta.getReplicaCount(), restoreObjMeta.getPrimaryDisk().getId());

		// S3ObjectOperation objectOperation = new S3ObjectOperation(objMeta, null, s3Parameter, versionId, null);
        // objectOperation.restoreObject(restoreObjMeta);
        IObjectManager objectManager = new VFSObjectManager();
        objectManager.restoreObject(s3Parameter, objMeta, restoreObjMeta);

        // meta info
        S3Metadata s3Metadata = S3Metadata.getS3Metadata(objMeta.getMeta());

        // update tier
        s3Metadata.setTier(GWConstants.AWS_TIER_STANTARD);

        try {
            restoreObjMeta.set(objMeta.getEtag(), objMeta.getTag(), s3Metadata.toString(), objMeta.getAcl(), objMeta.getSize());
        	restoreObjMeta.setVersionId(versionId, GWConstants.OBJECT_TYPE_FILE, true);
			insertObject(bucket, object, restoreObjMeta);
			logger.debug(GWConstants.LOG_ADMIN_RESTORE_OBJECT_INFO, bucket, object, objMeta.getSize(), objMeta.getEtag(), versionId);
		} catch (GWException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
    }
}
