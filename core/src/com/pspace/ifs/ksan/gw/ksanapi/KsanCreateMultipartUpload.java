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

import com.pspace.ifs.ksan.gw.api.S3Request;

import java.io.IOException;
import java.io.Writer;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy;
import com.pspace.ifs.ksan.gw.format.Tagging;
import com.pspace.ifs.ksan.gw.format.Tagging.TagSet;
import com.pspace.ifs.ksan.gw.format.Tagging.TagSet.Tag;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.AccessControlList;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.Owner;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.AccessControlList.Grant;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3User;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.gw.utils.S3UserManager;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjMultipart;

import org.slf4j.LoggerFactory;

public class KsanCreateMultipartUpload extends S3Request {
    public KsanCreateMultipartUpload(S3Parameter s3Parameter) {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(KsanCreateMultipartUpload.class);
	}

	@Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_ADMIN_CREATE_MULTIPART_UPLOAD_START);
		
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);

		String object = s3Parameter.getObjectName();

		GWUtils.checkCors(s3Parameter);
		
		String aclXml = makeAcl(null, false);
		
		String customerAlgorithm = s3RequestData.getServerSideEncryptionCustomerAlgorithm();
		String customerKey = s3RequestData.getServerSideEncryptionCustomerKey();
		String customerKeyMD5 = s3RequestData.getServerSideEncryptionCustomerKeyMD5();
		String serverSideEncryption = s3RequestData.getServerSideEncryption();

		if (!Strings.isNullOrEmpty(serverSideEncryption)) {
			if (!serverSideEncryption.equalsIgnoreCase(GWConstants.AES256)) {
				logger.error(GWErrorCode.NOT_IMPLEMENTED.getMessage() + GWConstants.SERVER_SIDE_OPTION);
				throw new GWException(GWErrorCode.NOT_IMPLEMENTED, s3Parameter);
			}
		}

		String storageClass = s3RequestData.getStorageClass();
		if (Strings.isNullOrEmpty(storageClass)) {
			storageClass = GWConstants.AWS_TIER_STANTARD;
		}
		String diskpoolId = s3Parameter.getUser().getUserDiskpoolId(storageClass);
		logger.debug("storage class : {}, diskpoolId : {}", storageClass, diskpoolId);
		
		S3Metadata s3Metadata = new S3Metadata();
		s3Metadata.setOwnerId(getBucketInfo().getUserId());
		s3Metadata.setOwnerName(getBucketInfo().getUserName());
		s3Metadata.setServerSideEncryption(serverSideEncryption);
		s3Metadata.setCustomerAlgorithm(customerAlgorithm);
		s3Metadata.setCustomerKey(customerKey);
		s3Metadata.setCustomerKeyMD5(customerKeyMD5);
		s3Metadata.setName(object);

		String cacheControl = s3RequestData.getCacheControl();
		String contentDisposition = s3RequestData.getContentDisposition();
		String contentEncoding = s3RequestData.getContentEncoding();
		String contentLanguage = s3RequestData.getContentLanguage();
		String contentType = s3RequestData.getContentType();
		String serversideEncryption = s3RequestData.getServerSideEncryption();
		String serverSideEncryptionAwsKmsKeyId = s3RequestData.getServerSideEncryptionAwsKmsKeyId();
		String serverSideEncryptionBucketKeyEnabled = s3RequestData.getServerSideEncryptionBucketKeyEnabled();

		s3Metadata.setOwnerId(s3Parameter.getUser().getUserId());
		s3Metadata.setOwnerName(s3Parameter.getUser().getUserName());
		s3Metadata.setUserMetadata(s3RequestData.getUserMetadata());
		
		if (!Strings.isNullOrEmpty(serversideEncryption)) {
			if (!serversideEncryption.equalsIgnoreCase(GWConstants.AES256)) {
				logger.error(GWErrorCode.NOT_IMPLEMENTED.getMessage() + GWConstants.SERVER_SIDE_OPTION);
				throw new GWException(GWErrorCode.NOT_IMPLEMENTED, s3Parameter);
			} else {
				s3Metadata.setServerSideEncryption(serversideEncryption);
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
		if (!Strings.isNullOrEmpty(serverSideEncryptionAwsKmsKeyId)) {
			s3Metadata.setKmsKeyId(serverSideEncryptionAwsKmsKeyId);
		}
		if (!Strings.isNullOrEmpty(serverSideEncryptionBucketKeyEnabled)) {
			s3Metadata.setBucketKeyEnabled(serverSideEncryptionBucketKeyEnabled);
		}

		// Tagging information
		String taggingCount = GWConstants.TAGGING_INIT;
		String taggingxml = "";
		Tagging tagging = new Tagging();
		tagging.tagset = new TagSet();
		
		if (!Strings.isNullOrEmpty(s3RequestData.getTagging())) {
			String strtaggingInfo = s3RequestData.getTagging();
			String[] strtagset = strtaggingInfo.split(GWConstants.AMPERSAND);
			int starttag = 0;
			for (String strtag : strtagset) {

				if(starttag == 0)
					tagging.tagset.tags = new ArrayList<Tag>();

				starttag+=1;

				Tag tag = new Tag();
				String[] keyvalue = strtag.split(GWConstants.EQUAL);
				if(keyvalue.length == GWConstants.TAG_KEY_SIZE) {
					tag.key = keyvalue[GWConstants.TAG_KEY_INDEX];
					tag.value = keyvalue[GWConstants.TAG_VALUE_INDEX];
				} else {
					tag.key = keyvalue[GWConstants.TAG_KEY_INDEX];
					tag.value = "";
				}

				tagging.tagset.tags.add(tag);
			}

			try {
				taggingxml = new XmlMapper().writeValueAsString(tagging);
			} catch (JsonProcessingException e) {
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}

			if (tagging != null) {

				if (tagging.tagset != null && tagging.tagset.tags != null) {
					for (Tag t : tagging.tagset.tags) {

						// key, value 길이 체크
						if (t.key.length() > GWConstants.TAG_KEY_MAX) {
							logger.error(GWConstants.LOG_PUT_OBJECT_TAGGING_KEY_LENGTH, t.key.length());
							throw new GWException(GWErrorCode.INVALID_TAG, s3Parameter);
						}

						if (t.value.length() > GWConstants.TAG_VALUE_MAX) {
							logger.error(GWConstants.LOG_PUT_OBJECT_TAGGING_VALUE_LENGTH, t.value.length());
							throw new GWException(GWErrorCode.INVALID_TAG, s3Parameter);
						}
					}
				}

				if ( tagging.tagset != null && tagging.tagset.tags != null ) {
					if(tagging.tagset.tags.size() > GWConstants.TAG_MAX_SIZE) {
						logger.error(GWConstants.LOG_PUT_OBJECT_TAGGING_SIZE, tagging.tagset.tags.size());
						throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);	
					}

					taggingCount = String.valueOf(tagging.tagset.tags.size());
				}
			}
		}
		s3Metadata.setTaggingCount(taggingCount);
		String metaJson = s3Metadata.toString();

		Metadata objMeta = null;
		try {
			// check exist object
			objMeta = createLocal(diskpoolId, bucket, object, "null");
		} catch (GWException e) {
			logger.info(e.getMessage());
			logger.error(GWConstants.LOG_CREATE_MULTIPART_UPLOAD_FAILED, bucket, object);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR, s3Parameter);
		}

		String uploadId = null;
		try {
			ObjMultipart objMultipart = getInstanceObjMultipart(bucket);
			// uploadId = objMultipart.createMultipartUpload(bucket, object, xml, s3Metadata.toString(), objMeta.getPrimaryDisk().getId());
			objMeta.setMeta(metaJson);
			objMeta.setAcl(aclXml);
			objMeta.setTag(taggingxml);
			uploadId = objMultipart.createMultipartUpload(objMeta);
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
