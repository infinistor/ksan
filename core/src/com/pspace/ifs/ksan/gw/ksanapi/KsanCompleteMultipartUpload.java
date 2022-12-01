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
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.pspace.ifs.ksan.gw.data.DataCompleteMultipartUpload;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.CompleteMultipartUploadRequest;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3User;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.object.S3Object;
import com.pspace.ifs.ksan.gw.object.S3ObjectEncryption;
import com.pspace.ifs.ksan.gw.object.S3ObjectOperation;
import com.pspace.ifs.ksan.libs.multipart.Multipart;
import com.pspace.ifs.ksan.libs.multipart.Part;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.gw.utils.S3UserManager;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjMultipart;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

import org.slf4j.LoggerFactory;

public class KsanCompleteMultipartUpload extends S3Request {
    public KsanCompleteMultipartUpload(S3Parameter s3Parameter) {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(KsanCompleteMultipartUpload.class);
	}

	@Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_ADMIN_COMPLETE_MULTIPART_UPLOAD_START);
		
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);

		String object = s3Parameter.getObjectName();

		GWUtils.checkCors(s3Parameter);

		DataCompleteMultipartUpload dataCompleteMultipartUpload = new DataCompleteMultipartUpload(s3Parameter);
		dataCompleteMultipartUpload.extract();

		String uploadId = dataCompleteMultipartUpload.getUploadId();
		s3Parameter.setUploadId(uploadId);
		String repVersionId = dataCompleteMultipartUpload.getVersionId();

		String multipartXml = dataCompleteMultipartUpload.getMultipartXml();
		XmlMapper xmlMapper = new XmlMapper();
		CompleteMultipartUploadRequest completeMultipartUpload = new CompleteMultipartUploadRequest();
		try {
			completeMultipartUpload = xmlMapper.readValue(multipartXml, CompleteMultipartUploadRequest.class);
		} catch (JsonMappingException e) {
			PrintStack.logging(logger, e);
			new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}
		
		if(completeMultipartUpload.parts == null || completeMultipartUpload.parts.size() == 0) {
			logger.error(GWErrorCode.MALFORMED_X_M_L.getMessage() + GWConstants.LOG_COMPLETE_MULTIPART_UPLOAD_PART_NO_EXIST);
			throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
		}

		SortedMap<Integer, Part> xmlListPart = new TreeMap<Integer, Part>();
		for (CompleteMultipartUploadRequest.Part sPart : completeMultipartUpload.parts ) {
			Part op = new Part();
			op.setPartETag(sPart.eTag);
			op.setPartNumber(sPart.partNumber);
			xmlListPart.put(sPart.partNumber, op);
		}

		SortedMap<Integer, Part> listPart = null;
		ObjMultipart objMultipart = null;
		try {
			objMultipart = getInstanceObjMultipart(bucket);
			listPart = objMultipart.getParts(uploadId);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}
		
		logger.info(GWConstants.LOG_COMPLETE_MULTIPART_UPLOAD_XML_PARTS_SIZE, completeMultipartUpload.parts.size());
		logger.info(GWConstants.LOG_COMPLETE_MULTIPART_UPLOAD_PARTS_SIZE, listPart.size());

		if (completeMultipartUpload.parts.size() > listPart.size()) {
			logger.warn(GWConstants.LOG_COMPLETE_MULTIPART_UPLOAD_LESS_THAN);
			throw new GWException(GWErrorCode.INVALID_PART, s3Parameter);
		} else if (completeMultipartUpload.parts.size() < listPart.size()) {
			logger.warn(GWConstants.LOG_COMPLETE_MULTIPART_UPLOAD_GREATER_THAN);
			throw new GWException(GWErrorCode.INVALID_PART, s3Parameter);
		}

		for (Iterator<Map.Entry<Integer, Part>> it = listPart.entrySet().iterator(); it.hasNext();) {
			Map.Entry<Integer, Part> entry = it.next();
			logger.debug("part number : {}, etag : {}, size : {}", entry.getValue().getPartNumber(), entry.getValue().getPartETag(), entry.getValue().getPartSize());
		}

		for (Iterator<Map.Entry<Integer, Part>> it = xmlListPart.entrySet().iterator(); it.hasNext();) {
			Map.Entry<Integer, Part> entry = it.next();
			String eTag = entry.getValue().getPartETag().replace(GWConstants.DOUBLE_QUOTE, "");
			if (listPart.containsKey(entry.getKey())) {
				Part part = listPart.get(entry.getKey());
				if (eTag.compareTo(part.getPartETag()) == 0 ) {
					logger.debug("part : {}, size : {}", entry.getKey(), part.getPartSize());
					if (part.getPartSize() < GWConstants.PARTS_MIN_SIZE && entry.getKey() < listPart.size()) {
						logger.error(GWErrorCode.ENTITY_TOO_SMALL.getMessage());
						throw new GWException(GWErrorCode.ENTITY_TOO_SMALL, s3Parameter);
					}
				} else {
					throw new GWException(GWErrorCode.INVALID_PART, s3Parameter);	
				}
			} else {
				throw new GWException(GWErrorCode.INVALID_PART, s3Parameter);
			}
		}

		// get Acl, Meta data
		Multipart multipart = null;
		try {
			multipart = objMultipart.getMultipart(uploadId);
			if (multipart == null) {
				logger.error(GWConstants.LOG_UPLOAD_NOT_FOUND, uploadId);
				throw new GWException(GWErrorCode.NO_SUCH_UPLOAD, s3Parameter);
			}
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		String acl = multipart.getAcl();
		String meta = multipart.getMeta();
		S3Metadata s3Metadata = null;
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			logger.debug(GWConstants.LOG_META, meta);
			s3Metadata = objectMapper.readValue(meta, S3Metadata.class);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
		
		// check encryption
		S3ObjectEncryption s3ObjectEncryption = new S3ObjectEncryption(s3Parameter, s3Metadata);
		s3ObjectEncryption.build();
		
		// check bucket versioning, and set versionId
		String versioningStatus = getBucketVersioning(bucket);
		Metadata objMeta = null;
		try {
			objMeta = createLocal(multipart.getDiskPoolId(), bucket, object, repVersionId);
		} catch (GWException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		if (GWConstants.VERSIONING_ENABLED.equalsIgnoreCase(versioningStatus)) {
			logger.info(GWConstants.LOG_COMPLETE_MULTIPART_VERSION_ID, repVersionId);
			s3Parameter.getResponse().addHeader(GWConstants.X_AMZ_VERSION_ID, repVersionId);
		}

		s3Parameter.getResponse().setCharacterEncoding(Constants.CHARSET_UTF_8);
		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
		try (Writer writer = s3Parameter.getResponse().getWriter()) {
			s3Parameter.getResponse().setContentType(GWConstants.XML_CONTENT_TYPE);

			final AtomicReference<S3Object> s3Object = new AtomicReference<>();
			final AtomicReference<Exception> S3Excp = new AtomicReference<>();

			S3ObjectOperation objectOperation = new S3ObjectOperation(objMeta, s3Metadata, s3Parameter, repVersionId, s3ObjectEncryption);
			
			// ObjectMapper jsonMapper = new ObjectMapper();
			
			SortedMap<Integer, Part> constListPart = listPart;

			Thread thread = new Thread() {
				@Override
				public void run() {
					try {
						s3Object.set(objectOperation.completeMultipart(constListPart));
					} catch (Exception e) {
						S3Excp.set(e);
					}
				}
			};

			thread.start();

			XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(writer);
			xmlStreamWriter.writeStartDocument();
			xmlStreamWriter.writeStartElement(GWConstants.COMPLETE_MULTIPART_UPLOAD_RESULT);
			xmlStreamWriter.writeDefaultNamespace(GWConstants.AWS_XMLNS);
			
			while (thread.isAlive()) {
				try {
					thread.join(500);
				} catch (InterruptedException ie) {
					PrintStack.logging(logger, ie);
					throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
				}
				xmlStreamWriter.writeCharacters(GWConstants.NEWLINE);
			}

			if( S3Excp.get() != null) {
				PrintStack.logging(logger, S3Excp.get());
				logger.error(S3Excp.get().getMessage());
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}

			writeSimpleElement(xmlStreamWriter, GWConstants.LOCATION, GWConstants.HTTP + bucket + GWConstants.S3_AMAZON_AWS_COM + object);
			writeSimpleElement(xmlStreamWriter, GWConstants.BUCKET, bucket);
			writeSimpleElement(xmlStreamWriter, GWConstants.KEY, object);

			// make ETag
			StringBuilder sb = new StringBuilder();
			for (Iterator<Map.Entry<Integer, Part>> it = listPart.entrySet().iterator(); it.hasNext();) {
				Map.Entry<Integer, Part> entry = it.next();
				sb.append(entry.getValue().getPartETag());
			}
			
			String hex = sb.toString();
			byte[] raw = BaseEncoding.base16().decode(hex.toUpperCase());
			Hasher hasher = Hashing.md5().newHasher();
			hasher.putBytes(raw);
			String digest = hasher.hash().toString() + GWConstants.DASH + listPart.size();
			s3Object.get().setEtag(digest);
			logger.debug(GWConstants.LOG_COMPLETE_MULTIPART_UPLOAD_MD5, s3Object.get().getEtag());
				
			writeSimpleElement(xmlStreamWriter, GWConstants.ETAG, GWConstants.DOUBLE_QUOTE + s3Object.get().getEtag() + GWConstants.DOUBLE_QUOTE);

			xmlStreamWriter.writeEndElement();
			xmlStreamWriter.flush();

			s3Metadata.setContentLength(s3Object.get().getFileSize());
			s3Metadata.setETag(s3Object.get().getEtag());
			s3Metadata.setLastModified(s3Object.get().getLastModified());
			s3Metadata.setTier(GWConstants.AWS_TIER_STANTARD);
			s3Metadata.setDeleteMarker(s3Object.get().getDeleteMarker());
			s3Metadata.setVersionId(s3Object.get().getVersionId());
			
			String jsonmeta = "";
			try {
				objectMapper.setSerializationInclusion(Include.NON_NULL);
				jsonmeta = objectMapper.writeValueAsString(s3Metadata);
			} catch (JsonProcessingException e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		
			int result = 0;
			try {
				if (!GWConstants.VERSIONING_ENABLED.equalsIgnoreCase(versioningStatus)) {
					remove(bucket, object);
				}
				objMeta.set(s3Object.get().getEtag(), "", jsonmeta, acl, s3Object.get().getFileSize());
				objMeta.setVersionId(repVersionId, GWConstants.OBJECT_TYPE_FILE, true);
				result = insertObject(bucket, object, objMeta);
			} catch (GWException e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
			if (result != 0) {
				logger.error(GWConstants.LOG_COMPLETE_MULTIPART_UPLOAD_FAILED, bucket, object);
			}
			logger.debug(GWConstants.LOG_COMPLETE_MULTIPART_UPLOAD_INFO, bucket, object, s3Object.get().getFileSize(), s3Object.get().getEtag(), acl, repVersionId);
			objMultipart.abortMultipartUpload(uploadId);
		} catch (IOException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} catch (XMLStreamException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
	}
}
