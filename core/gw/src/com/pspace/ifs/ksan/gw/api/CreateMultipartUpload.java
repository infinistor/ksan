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
import java.io.Writer;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.data.DataCreateMultipartUpload;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.AccessControlList;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.Owner;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.AccessControlList.Grant;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjMultipart;

import org.slf4j.LoggerFactory;

public class CreateMultipartUpload extends S3Request {

	public CreateMultipartUpload(S3Parameter s3Parameter) {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(CreateMultipartUpload.class);
	}

	@Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_CREATE_MULTIPART_UPLOAD_START);
		
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);

		String object = s3Parameter.getObjectName();

		S3Bucket s3Bucket = new S3Bucket();
		s3Bucket.setCors(getBucketInfo().getCors());
		s3Bucket.setAccess(getBucketInfo().getAccess());
		s3Parameter.setBucket(s3Bucket);
		GWUtils.checkCors(s3Parameter);

		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}

		checkGrantBucket(s3Parameter.isPublicAccess(), String.valueOf(s3Parameter.getUser().getUserId()), GWConstants.GRANT_WRITE);

		DataCreateMultipartUpload dataCreateMultipartUpload = new DataCreateMultipartUpload(s3Parameter);
		dataCreateMultipartUpload.extract();

		accessControlPolicy = new AccessControlPolicy();
		accessControlPolicy.aclList = new AccessControlList();
		accessControlPolicy.aclList.grants = new ArrayList<Grant>();
		accessControlPolicy.owner = new Owner();
		accessControlPolicy.owner.id = String.valueOf(s3Parameter.getUser().getUserId());
		accessControlPolicy.owner.displayName = s3Parameter.getUser().getUserName();

		String xml = GWUtils.makeAclXml(accessControlPolicy, 
										null, 
										dataCreateMultipartUpload.hasAclKeyword(), 
										null, 
										dataCreateMultipartUpload.getAcl(),
										getBucketInfo(),
										String.valueOf(s3Parameter.getUser().getUserId()),
										s3Parameter.getUser().getUserName(),
										dataCreateMultipartUpload.getGrantRead(),
										dataCreateMultipartUpload.getGrantWrite(), 
										dataCreateMultipartUpload.getGrantFullControl(), 
										dataCreateMultipartUpload.getGrantReadAcp(), 
										dataCreateMultipartUpload.getGrantWriteAcp(),
										s3Parameter);
		
		String customerAlgorithm = dataCreateMultipartUpload.getServerSideEncryptionCustomerAlgorithm();
		String customerKey = dataCreateMultipartUpload.getServerSideEncryptionCustomerKey();
		String customerKeyMD5 = dataCreateMultipartUpload.getServerSideEncryptionCustomerKeyMD5();
		String serverSideEncryption = dataCreateMultipartUpload.getServerSideEncryption();

		if (!Strings.isNullOrEmpty(serverSideEncryption)) {
			if (!serverSideEncryption.equalsIgnoreCase(GWConstants.AES256)) {
				logger.error(GWErrorCode.NOT_IMPLEMENTED.getMessage() + GWConstants.SERVER_SIDE_OPTION);
				throw new GWException(GWErrorCode.NOT_IMPLEMENTED, s3Parameter);
			}
		}
		
		S3Metadata s3Metadata = new S3Metadata();
		s3Metadata.setOwnerId(String.valueOf(s3Parameter.getUser().getUserId()));
		s3Metadata.setOwnerName(s3Parameter.getUser().getUserName());
		s3Metadata.setServersideEncryption(serverSideEncryption);
		s3Metadata.setCustomerAlgorithm(customerAlgorithm);
		s3Metadata.setCustomerKey(customerKey);
		s3Metadata.setCustomerKeyMD5(customerKeyMD5);
		s3Metadata.setName(object);

		String cacheControl = dataCreateMultipartUpload.getCacheControl();
		String contentDisposition = dataCreateMultipartUpload.getContentDisposition();
		String contentEncoding = dataCreateMultipartUpload.getContentEncoding();
		String contentLanguage = dataCreateMultipartUpload.getContentLanguage();
		String contentType = dataCreateMultipartUpload.getContentType();
		String serversideEncryption = dataCreateMultipartUpload.getServerSideEncryption();

		s3Metadata.setOwnerId(String.valueOf(s3Parameter.getUser().getUserId()));
		s3Metadata.setOwnerName(s3Parameter.getUser().getUserName());
		s3Metadata.setUserMetadataMap(dataCreateMultipartUpload.getUserMetadata());
		
		if (!Strings.isNullOrEmpty(serversideEncryption)) {
			if (!serversideEncryption.equalsIgnoreCase(GWConstants.AES256)) {
				logger.error(GWErrorCode.NOT_IMPLEMENTED.getMessage() + GWConstants.SERVER_SIDE_OPTION);
				throw new GWException(GWErrorCode.NOT_IMPLEMENTED, s3Parameter);
			} else {
				s3Metadata.setServersideEncryption(serversideEncryption);
			}
		}
		
		if (!Strings.isNullOrEmpty(cacheControl)) {
			s3Metadata.setCacheControl(cacheControl);
		}
		if (!Strings.isNullOrEmpty(contentDisposition)) {
			s3Metadata.setContentDisposition(contentDisposition);
		}
		if (!Strings.isNullOrEmpty(contentEncoding)) {
			s3Metadata.setContentEncoding(contentEncoding);
		}
		if (!Strings.isNullOrEmpty(contentLanguage)) {
			s3Metadata.setContentLanguage(contentLanguage);
		}
		if (!Strings.isNullOrEmpty(contentType)) {
			s3Metadata.setContentType(contentType);
		}
		if (!Strings.isNullOrEmpty(customerAlgorithm)) {
			s3Metadata.setCustomerAlgorithm(customerAlgorithm);
		}
		if (!Strings.isNullOrEmpty(customerKey)) {
			s3Metadata.setCustomerKey(customerKey);
		}
		if (!Strings.isNullOrEmpty(customerKeyMD5)) {
			 s3Metadata.setCustomerKeyMD5(customerKeyMD5);
		}

		ObjectMapper jsonMapper = new ObjectMapper();
		String metaJson = "";
		try {
			metaJson = jsonMapper.writeValueAsString(s3Metadata);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR, s3Parameter);
		}

		Metadata objMeta = null;
		try {
			// check exist object
			objMeta = createLocal(bucket, object);
		} catch (GWException e) {
			logger.info(e.getMessage());
			logger.error(GWConstants.LOG_CREATE_MULTIPART_UPLOAD_FAILED, bucket, object);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR, s3Parameter);
		}

		String uploadId = null;
		try {
			ObjMultipart objMultipart = new ObjMultipart(bucket);
			uploadId = objMultipart.createMultipartUpload(bucket, object, xml, metaJson, objMeta.getPrimaryDisk().getId()); 
		} catch (UnknownHostException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
		try (Writer writer = s3Parameter.getResponse().getWriter()) {
			s3Parameter.getResponse().setContentType(GWConstants.XML_CONTENT_TYPE);
			XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(writer);
			xmlStreamWriter.writeStartDocument();
			xmlStreamWriter.writeStartElement(GWConstants.INITATE_MULTIPART_UPLOAD_RESULT);
			xmlStreamWriter.writeDefaultNamespace(GWConstants.AWS_XMLNS);
			writeSimpleElement(xmlStreamWriter, GWConstants.BUCKET, bucket);
			writeSimpleElement(xmlStreamWriter, GWConstants.KEY, object);
			writeSimpleElement(xmlStreamWriter, GWConstants.XML_UPLOADID, uploadId); 
			xmlStreamWriter.writeEndElement();
			xmlStreamWriter.flush();
		} catch (XMLStreamException xse) {
			PrintStack.logging(logger, xse);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		} catch (IOException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}
	}
}
