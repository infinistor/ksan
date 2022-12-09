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

import java.util.SortedMap;

import jakarta.servlet.http.HttpServletResponse;

import com.pspace.ifs.ksan.gw.data.DataAbortMultipartUpload;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.object.S3ObjectOperation;
import com.pspace.ifs.ksan.libs.multipart.Part;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjMultipart;
import com.pspace.ifs.ksan.libs.PrintStack;

import org.slf4j.LoggerFactory;

public class AbortMultipartUpload extends S3Request {

	public AbortMultipartUpload(S3Parameter s3Parameter) {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(AbortMultipartUpload.class);
	}

	@Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_ABORT_MULTIPART_UPLOAD_START);
		
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);
		
		String object = s3Parameter.getObjectName();

		GWUtils.checkCors(s3Parameter);

		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}

		DataAbortMultipartUpload dataAbortMultipartUpload = new DataAbortMultipartUpload(s3Parameter);
		dataAbortMultipartUpload.extract();

		checkPolicyBucket(GWConstants.ACTION_ABORT_MULTIPART_UPLOAD, s3Parameter, dataAbortMultipartUpload);

		String uploadId = dataAbortMultipartUpload.getUploadId();
		
		s3Parameter.setUploadId(uploadId);

		ObjMultipart objMultipart = null;
		SortedMap<Integer, Part> listPart = null;
		boolean isUploadId = true;
		try {
			objMultipart = getInstanceObjMultipart(bucket);
			if (!objMultipart.isUploadId(uploadId)) {
				isUploadId = false;
				throw new GWException(GWErrorCode.NO_SUCH_UPLOAD, s3Parameter);
			}
			listPart = objMultipart.getParts(uploadId);
		} catch (Exception e) {
			if (!isUploadId) {
				throw new GWException(GWErrorCode.NO_SUCH_UPLOAD, s3Parameter);
			}
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		// get Paths
		Metadata objMeta = new Metadata(bucket, object);

		S3ObjectOperation objectOperation = new S3ObjectOperation(objMeta, null, s3Parameter, null, null);
		objectOperation.abortMultipart(listPart);
		
		objMultipart.abortMultipartUpload(uploadId);

		s3Parameter.getResponse().setStatus(HttpServletResponse.SC_NO_CONTENT);
	}
}
