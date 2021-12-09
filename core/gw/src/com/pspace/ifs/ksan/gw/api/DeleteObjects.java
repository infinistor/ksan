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
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.data.DataDeleteObjects;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.DeleteMultipleObjectsRequest;
import com.pspace.ifs.ksan.gw.format.Objects;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.object.S3ObjectOperation;
import com.pspace.ifs.ksan.gw.utils.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

import org.slf4j.LoggerFactory;

public class DeleteObjects extends S3Request {

	public DeleteObjects(S3Parameter ip) {
		super(ip);
		logger = LoggerFactory.getLogger(DeleteObjects.class);
	}

	@Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_DELETE_OBJECTS_START);
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);

		S3Bucket s3Bucket = new S3Bucket();
		s3Bucket.setCors(getBucketInfo().getCors());
		s3Bucket.setAccess(getBucketInfo().getAccess());
		s3Parameter.setBucket(s3Bucket);
		GWUtils.checkCors(s3Parameter);

		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}

		checkGrantBucketOwner(s3Parameter.isPublicAccess(), String.valueOf(s3Parameter.getUser().getUserId()), GWConstants.GRANT_WRITE);
		
		DataDeleteObjects dataDeleteObjects = new DataDeleteObjects(s3Parameter);
		dataDeleteObjects.extract();
		String deleteXml = dataDeleteObjects.getDeleteXml();

		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
		try {
			DeleteMultipleObjectsRequest deleteMultipleObjectsRequest = new XmlMapper().readValue(deleteXml, DeleteMultipleObjectsRequest.class);
			Collection<Objects> objectNames = new ArrayList<>();

			if (deleteMultipleObjectsRequest.objects != null) {
				for (DeleteMultipleObjectsRequest.S3Object s3Object : deleteMultipleObjectsRequest.objects) {
					Objects object = new Objects();
					object.objectName = s3Object.key;
					if (Strings.isNullOrEmpty(s3Object.versionId)) {
						object.versionId = GWConstants.VERSIONING_DISABLE_TAIL;
					} else if (GWConstants.VERSIONING_DISABLE_TAIL.equalsIgnoreCase(s3Object.versionId)) {
						object.versionId = GWConstants.VERSIONING_DISABLE_TAIL;
					} else {
						object.versionId = s3Object.versionId;
					}
					objectNames.add(object);
				}
			}

			Writer writer = s3Parameter.getResponse().getWriter();
			s3Parameter.getResponse().setContentType(GWConstants.XML_CONTENT_TYPE);
			XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(writer);
			xmlStreamWriter.writeStartDocument();
			xmlStreamWriter.writeStartElement(GWConstants.DELETE_RESULT);
			xmlStreamWriter.writeDefaultNamespace(GWConstants.AWS_XMLNS);
			logger.debug(GWConstants.LOG_DELETE_OBJECTS_SIZE, objectNames.size());

			for (Objects object : objectNames) {
				deleteObject(s3Parameter, object.objectName, object.versionId, xmlStreamWriter, deleteMultipleObjectsRequest.quiet);
				// xmlStreamWriter.flush(); // In Tomcat, if you use flush(), you lose connection. jakarta, need to check
			}

			xmlStreamWriter.writeEndElement();
			xmlStreamWriter.flush();
		} catch (JsonParseException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR);
		} catch (JacksonException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR);
		} catch (IOException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR);
		} catch (XMLStreamException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR);
		}

		s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
	}

	public void deleteObject(S3Parameter s3Parameter, String object, String versionId, XMLStreamWriter xml, boolean quiet) throws GWException {
		String bucket = s3Parameter.getBucketName();
		
        String versioningStatus = getBucketInfo().getVersioning();

		Metadata objMeta = null;
		try {
			if (Strings.isNullOrEmpty(versionId)) {
				objMeta = open(bucket, object);
				versionId = objMeta.getVersionId();
			} else {
				objMeta = open(bucket, object, versionId);
			}
		} catch (GWException e) {
			logger.debug(GWConstants.LOG_DELETE_OBJECTS_QUIET_VALUE, quiet);
			if(!quiet) {
				try {
					xml.writeStartElement(GWConstants.DELETE_RESULT_DELETED);
					writeSimpleElement(xml, GWConstants.DELETE_RESULT_KEY, object);
					writeSimpleElement(xml, GWConstants.VERSIONID, versionId);
					xml.writeEndElement();
					logger.debug(GWConstants.LOG_DELETE_OBJECTS_ERROR, object, versionId, e.getError().getErrorCode(), e.getError().getMessage());
					return;
				} catch (XMLStreamException e1) {
					PrintStack.logging(logger, e1);
					throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR);
				}
			}
		}

		boolean isLastVersion = objMeta.getLastVersion();
		String deleteMarker = objMeta.getDeleteMarker();
		logger.debug(GWConstants.LOG_DELETE_OBJECT_INFO, versionId, isLastVersion, deleteMarker);

		S3ObjectOperation objectOperation = new S3ObjectOperation(objMeta, null, s3Parameter, versionId, null);
		
		try {
			if (versioningStatus.equalsIgnoreCase(GWConstants.VERSIONING_ENABLED)) { // Bucket Versioning Enabled
				logger.debug(GWConstants.LOG_DELETE_OBJECT_BUCKET_VERSIONING_ENABLED);
				if (GWConstants.VERSIONING_DISABLE_TAIL.equals(versionId)) {	// request versionId is null
					if (deleteMarker != null && deleteMarker.equalsIgnoreCase(GWConstants.OBJECT_TYPE_MARK)) {
						remove(bucket, object, versionId);
						if(!quiet) {
							xml.writeStartElement(GWConstants.DELETE_RESULT_DELETED);
							writeSimpleElement(xml, GWConstants.DELETE_RESULT_KEY, object);
							xml.writeEndElement();
						}
					} else {		
						try {
							S3Metadata s3Metadata = new S3Metadata();
							s3Metadata.setName(object);
							s3Metadata.setOwnerId(String.valueOf(s3Parameter.getUser().getUserId()));
							s3Metadata.setOwnerName(s3Parameter.getUser().getUserName());
							s3Metadata.setDeleteMarker(GWConstants.OBJECT_TYPE_MARK);
							s3Metadata.setVersionId(versionId);

							ObjectMapper jsonMapper = new ObjectMapper();
							String jsonmeta = "";
							jsonmeta = jsonMapper.writeValueAsString(s3Metadata);
							int result;
							if (objMeta.getReplicaDisk() != null) {
								result = insertObject(bucket, object, "", jsonmeta, "", 0L, "", objMeta.getPrimaryDisk().getPath(), objMeta.getReplicaDisk().getPath(), versionId, GWConstants.OBJECT_TYPE_MARK);
							} else {
								result = insertObject(bucket, object, "", jsonmeta, "", 0L, "", objMeta.getPrimaryDisk().getPath(), "", versionId, GWConstants.OBJECT_TYPE_MARK);
							}
							if (result != 0) {
								logger.error(GWConstants.LOG_DELETE_OBJECT_FAILED_MARKER, bucket, object);
							}
							logger.debug(GWConstants.LOG_PUT_DELETE_MARKER);
							xml.writeStartElement(GWConstants.DELETE_RESULT_DELETED);
							writeSimpleElement(xml, GWConstants.DELETE_RESULT_KEY, object);
							writeSimpleElement(xml, GWConstants.XML_DELETE_MARKER, GWConstants.XML_TRUE);
							writeSimpleElement(xml, GWConstants.DELETE_RESULT_DELETE_MARKER_VERSION_ID, versionId);
							xml.writeEndElement();
						} catch (InvalidParameterException | ResourceNotFoundException | JsonProcessingException e) {
							PrintStack.logging(logger, e);
							throw new GWException(GWErrorCode.SERVER_ERROR);
						}
					}
				} else {	// request with versionId
					if (isLastVersion) {	// request with versionId and same currentVid
						if (deleteMarker.equalsIgnoreCase(GWConstants.OBJECT_TYPE_MARK)) {
							remove(bucket, object);
						} else if (deleteMarker.equalsIgnoreCase(GWConstants.OBJECT_TYPE_FILE)) {
							remove(bucket, object, versionId);
							objectOperation.deleteObject();
						}
					} else {	// request with versionId not currentVid
						remove(bucket, object, versionId);
						objectOperation.deleteObject();
					}
					if (!quiet) {
						xml.writeStartElement(GWConstants.DELETE_RESULT_DELETED);
						writeSimpleElement(xml, GWConstants.DELETE_RESULT_KEY, object);
						writeSimpleElement(xml, GWConstants.VERSIONID, versionId);
						xml.writeEndElement();
					}
				}
			} else if (versioningStatus.equalsIgnoreCase(GWConstants.VERSIONING_SUSPENDED)) { // Bucket Versioning Suspended
				logger.debug(GWConstants.LOG_DELETE_OBJECT_BUCKET_VERSIONING_SUSPENDED);
				if (GWConstants.VERSIONING_DISABLE_TAIL.equals(versionId)) {
					if (isLastVersion) {
						if (deleteMarker.equalsIgnoreCase(GWConstants.OBJECT_TYPE_MARK)) {
							remove(bucket, object, versionId);
						} else {			
							try {
								S3Metadata s3Metadata = new S3Metadata();
								s3Metadata.setName(object);
								s3Metadata.setOwnerId(String.valueOf(s3Parameter.getUser().getUserId()));
								s3Metadata.setOwnerName(s3Parameter.getUser().getUserName());
								s3Metadata.setDeleteMarker(GWConstants.OBJECT_TYPE_MARK);
								s3Metadata.setVersionId(versionId);

								ObjectMapper jsonMapper = new ObjectMapper();
								String jsonmeta = "";
								jsonmeta = jsonMapper.writeValueAsString(s3Metadata);

								int result;
								if (objMeta.getReplicaDisk() != null) {
									result = insertObject(bucket, object, "", jsonmeta, "", 0L, "", objMeta.getPrimaryDisk().getPath(), objMeta.getReplicaDisk().getPath(), versionId, GWConstants.OBJECT_TYPE_MARK);
								} else {
									result = insertObject(bucket, object, "", jsonmeta, "", 0L, "", objMeta.getPrimaryDisk().getPath(), "", versionId, GWConstants.OBJECT_TYPE_MARK);
								}
								if (result != 0) {
									logger.error(GWConstants.LOG_DELETE_OBJECT_FAILED_MARKER, bucket, object);
								}
								logger.debug(GWConstants.LOG_PUT_DELETE_MARKER);
							} catch (InvalidParameterException | ResourceNotFoundException | JsonProcessingException e) {
								PrintStack.logging(logger, e);
								throw new GWException(GWErrorCode.SERVER_ERROR);
							}
						}
					} 
				} else {	// request with versionId
					remove(bucket, object, versionId);
					objectOperation.deleteObject();
				}

				if(!quiet) {
					xml.writeStartElement(GWConstants.DELETE_RESULT_DELETED);
					writeSimpleElement(xml, GWConstants.DELETE_RESULT_KEY, object);
					writeSimpleElement(xml, GWConstants.XML_DELETE_MARKER, GWConstants.STRING_TRUE);
					writeSimpleElement(xml, GWConstants.DELETE_RESULT_DELETE_MARKER_VERSION_ID, GWConstants.VERSIONING_DISABLE_TAIL);
					xml.writeEndElement();
				}
			} else { // Bucket Versioning Disabled
				logger.debug(GWConstants.LOG_DELETE_OBJECT_BUCKET_VERSIONING_DISABLED);
				remove(bucket, object);
				objectOperation.deleteObject();
				if(!quiet) {
					xml.writeStartElement(GWConstants.DELETE_RESULT_DELETED);
					writeSimpleElement(xml, GWConstants.DELETE_RESULT_KEY, object);
					xml.writeEndElement();
				}
			}
		} catch (GWException e) {
			try {
				if(!quiet) {
					xml.writeStartElement(GWConstants.STRING_ERROR);
					writeSimpleElement(xml, GWConstants.DELETE_RESULT_KEY, object);
					writeSimpleElement(xml, GWConstants.VERSIONID, versionId);
					writeSimpleElement(xml, GWConstants.CODE, e.getError().getErrorCode());
					writeSimpleElement(xml, GWConstants.MESSAGE, e.getError().getMessage());
					xml.writeEndElement();
				}
			} catch (XMLStreamException e1) {
				throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR);
			}
		} catch (XMLStreamException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR);
		}
	}
}
