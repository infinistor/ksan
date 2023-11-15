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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Map;
import java.util.ArrayList;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import com.google.common.base.Strings;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.pspace.ifs.ksan.libs.config.AgentConfig;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.Owner;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.AccessControlList;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.AccessControlList.Grant;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.AccessControlList.Grant.Grantee;
import com.pspace.ifs.ksan.libs.identity.ObjectListParameter;
import com.pspace.ifs.ksan.libs.identity.S3BucketSimpleInfo;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.libs.identity.S3ObjectList;
import com.pspace.ifs.ksan.libs.mq.MQSender;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.object.objmanager.ObjManagers;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.gw.utils.S3UserManager;
import com.pspace.ifs.ksan.objmanager.Bucket;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjManager;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceAlreadyExistException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.objmanager.ObjMultipart;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.data.DataPostObject;
import com.pspace.ifs.ksan.gw.data.S3RequestData;
import com.pspace.ifs.ksan.gw.format.Policy;
import com.pspace.ifs.ksan.gw.format.Policy.Statement;
import com.pspace.ifs.ksan.gw.condition.PolicyCondition;
import com.pspace.ifs.ksan.gw.condition.PolicyConditionFactory;
import com.pspace.ifs.ksan.gw.format.PublicAccessBlockConfiguration;
import jakarta.servlet.http.HttpServletResponse;
import com.pspace.ifs.ksan.libs.mq.MQSender;
import org.json.simple.JSONObject;
import java.io.IOException;

import org.slf4j.Logger;

public abstract class S3Request {
	protected S3Parameter s3Parameter;
	protected S3RequestData s3RequestData;
	protected ObjManager objManager;
	protected Logger logger;
	protected Bucket srcBucket;
	protected Bucket dstBucket;
	protected AccessControlPolicy bucketAccessControlPolicy;
	protected AccessControlPolicy objectAccessControlPolicy;
	protected static final HashFunction MD5 = Hashing.md5();

	public S3Request(S3Parameter s3Parameter) {
		this.s3Parameter = new S3Parameter(s3Parameter);
		s3RequestData = new S3RequestData(s3Parameter);
		srcBucket = null;
		dstBucket = null;
		bucketAccessControlPolicy = null;
		objectAccessControlPolicy = null;
		objManager = ObjManagers.getInstance().getObjManager();
	}
	
	public abstract void process() throws GWException;

	protected void setSrcBucket(String bucket) throws GWException {
		checkBucket(bucket);
		srcBucket = getBucket(bucket);
		if (srcBucket == null) {
			logger.info(GWConstants.LOG_BUCKET_IS_NOT_EXIST, bucket);
			throw new GWException(GWErrorCode.NO_SUCH_BUCKET, s3Parameter);
		}
	}

	protected boolean isExistBucket(String bucket) throws GWException {
		boolean result = false;
		try {
			result = objManager.isBucketExist(bucket);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		return result;
	}

	protected Bucket getSrcBucket() {
		return srcBucket;
	}

	protected Bucket getBucketInfo() {
		return dstBucket;
	}
	
	private void checkBucket(String bucket) throws GWException {
		if (Strings.isNullOrEmpty(bucket)) {
			logger.error(GWConstants.LOG_BUCKET_IS_NULL);
			throw new GWException(GWErrorCode.METHOD_NOT_ALLOWED, s3Parameter);
		}
	}

	protected void initBucketInfo(String bucket) throws GWException {
		checkBucket(bucket);
		try {
			dstBucket = objManager.getBucket(bucket);
			if (dstBucket != null) {
				String bucketAcl = dstBucket.getAcl();
				if (!Strings.isNullOrEmpty(bucketAcl)) {
					bucketAccessControlPolicy = AccessControlPolicy.getAclClassFromJson(bucketAcl);
				}
			}
		} catch (ResourceNotFoundException e) {
			throw new GWException(GWErrorCode.NO_SUCH_BUCKET, s3Parameter);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		if (dstBucket == null) {
			logger.info(GWConstants.LOG_BUCKET_IS_NOT_EXIST, bucket);
			throw new GWException(GWErrorCode.NO_SUCH_BUCKET, s3Parameter);
		}

		S3Bucket s3Bucket = new S3Bucket();
		s3Bucket.setBucket(bucket);
		s3Bucket.setUserName(dstBucket.getUserName());
		s3Bucket.setCors(dstBucket.getCors());
		s3Bucket.setAccess(dstBucket.getAccess());
		s3Bucket.setPolicy(dstBucket.getPolicy());
		s3Bucket.setAcl(dstBucket.getAcl());
		s3Parameter.setBucket(s3Bucket);
	}

	protected Bucket getSomeBucket(String bucket) throws GWException {
		checkBucket(bucket);
		Bucket bucketInfo = null;
		try {
			bucketInfo = objManager.getBucket(bucket);
		} catch (ResourceNotFoundException e) {
			throw new GWException(GWErrorCode.NO_SUCH_BUCKET, s3Parameter);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		if (bucketInfo == null) {
			logger.info(GWConstants.LOG_BUCKET_IS_NOT_EXIST, bucket);
			throw new GWException(GWErrorCode.NO_SUCH_BUCKET, s3Parameter);
		}
		
		return bucketInfo;
	}
	
	protected boolean isBucketOwner(String id) throws GWException {		
		if (dstBucket == null) {
			return false;
		}
		
		if (bucketAccessControlPolicy.owner != null) {
			if (bucketAccessControlPolicy.owner.id != null) {
				if (bucketAccessControlPolicy.owner.id.compareTo(id) == 0) {
					return true;
				}
			}
		}
		
		return false;
	}

	// cannot call BlobStore.getContext().utils().date().iso8601DateFormatsince
	// it has unwanted millisecond precision
	protected String formatDate(Date date) {
		SimpleDateFormat formatter = new SimpleDateFormat(GWConstants.ISO_8601_TIME_SIMPLE_FORMAT);
		formatter.setTimeZone(TimeZone.getTimeZone(GWConstants.GMT));
		return formatter.format(date);
	}
	
	protected void writeSimpleElement(XMLStreamWriter xml,
			String elementName, String characters) throws XMLStreamException {
		xml.writeStartElement(elementName);
		xml.writeCharacters(characters);
		xml.writeEndElement();
	}
	
	protected void writeInitiatorStanza(XMLStreamWriter xml) throws XMLStreamException {
		xml.writeStartElement(GWConstants.XML_INITIATOR);

		writeSimpleElement(xml, GWConstants.XML_ID, GWConstants.FAKE_INITIATOR_ID);
		writeSimpleElement(xml, GWConstants.XML_DISPLAY_NAME, GWConstants.FAKE_INITIATOR_DISPLAY_NAME);

		xml.writeEndElement();
	}
	
	protected void writeOwnerInfini(XMLStreamWriter xml, String id, String user)
			throws XMLStreamException {
		xml.writeStartElement(GWConstants.XML_OWNER);

		writeSimpleElement(xml, GWConstants.XML_ID, id);
		writeSimpleElement(xml, GWConstants.XML_DISPLAY_NAME, user);

		xml.writeEndElement();
	}

	protected String getBucketVersioning (String bucket) throws GWException {
		String versioningStatus = null;
		try {
			versioningStatus = objManager.getBucketVersioning(bucket);
		} catch (ResourceNotFoundException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.NO_SUCH_BUCKET, s3Parameter);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		return versioningStatus;
	}

	protected Metadata getObjectWithVersionId(String bucket, String object, String versionId) throws GWException {
		Metadata objMeta = null;
		try {
			objMeta = objManager.getObjectWithVersionId(bucket, object, versionId);
		} catch (ResourceNotFoundException e) {
			logger.info(GWConstants.LOG_REQUEST_NOT_FOUND_IN_DB, bucket, object);
			throw new GWException(GWErrorCode.NO_SUCH_KEY, s3Parameter);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		return objMeta;
	}

	protected Metadata open(String bucket, String object) throws GWException {
		Metadata meta = null;
		try {
			meta = objManager.open(bucket, object);
			if (meta != null) {
				String objectAcl = meta.getAcl();
				if (!Strings.isNullOrEmpty(objectAcl)) {
					objectAccessControlPolicy = AccessControlPolicy.getAclClassFromJson(objectAcl);
				}
			}
		} catch (ResourceNotFoundException e) {
			throw new GWException(GWErrorCode.NO_SUCH_KEY, s3Parameter);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		return meta;
	}
	
	protected Metadata open(String bucket, String objcet, String versionId) throws GWException {
		Metadata meta = null;
		try {
			meta = objManager.open(bucket, objcet, versionId);
			if (meta != null) {
				String objectAcl = meta.getAcl();
				if (!Strings.isNullOrEmpty(objectAcl)) {
					objectAccessControlPolicy = AccessControlPolicy.getAclClassFromJson(objectAcl);
				}
			}
		} catch (ResourceNotFoundException e) {
			throw new GWException(GWErrorCode.NO_SUCH_KEY, s3Parameter);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		return meta;
	}

	protected Metadata create(String bucket, String object) throws GWException {
		Metadata meta = null;
		try {
			meta = objManager.create(bucket, object);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		return meta;
	}

	protected Metadata create(String bucket, String object, String versionId) throws GWException {
		Metadata meta = null;
		try {
			meta = objManager.create(bucket, object, versionId);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		return meta;
	}

	protected Metadata createLocal(String bucket, String object) throws GWException {
		Metadata meta = null;
		try {
			meta = objManager.createLocal(bucket, object, "null");
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		return meta;
	}

	protected Metadata createLocal(String bucket, String object, String versionId) throws GWException {
		Metadata meta = null;
		try {
			meta = objManager.createLocal(bucket, object, versionId);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		return meta;
	}

	protected Metadata createLocal(String diskpoolId, String bucket, String object, String versionId) throws GWException {
		Metadata meta = null;
		try {
			meta = objManager.createLocal(diskpoolId, bucket, object, versionId);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		return meta;
	}

	protected Metadata createCopy(String srcBucket, String srcObjectName, String srcVersionId, String bucket, String object) throws GWException {
		Metadata objMeta;
		try {
			objMeta = objManager.createCopy(srcBucket, srcObjectName, srcVersionId, bucket, object);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		return objMeta;
	}

	protected void remove(String bucket, String object) throws GWException {
		try {
			objManager.remove(bucket, object);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
	}

	protected void remove(String bucket, String object, String versionId) throws GWException {
		try {
			objManager.remove(bucket, object, versionId);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
	}

	protected int createBucket(String bucket, String userName, String userId, String acl, String encryption, String objectlock) throws GWException {
		int result = 0;
		try {
            result = objManager.createBucket(bucket, userName, userId, acl, encryption, objectlock);
        } catch (ResourceAlreadyExistException e) {
			PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.BUCKET_ALREADY_EXISTS, s3Parameter);
        } catch(ResourceNotFoundException e) {
			PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
        } catch (Exception e) {
			PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		if (result != 0) {
			throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR, s3Parameter);
		}

		return  result;
	}

	protected int createBucket(Bucket bucket) throws GWException {
		int result = 0;
		try {
            result = objManager.createBucket(bucket);
        } catch (ResourceAlreadyExistException e) {
			PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.BUCKET_ALREADY_EXISTS, s3Parameter);
        } catch(ResourceNotFoundException e) {
			PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
        } catch (Exception e) {
			PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		if (result != 0) {
			throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR, s3Parameter);
		}

		return  result;
	}

	protected void deleteBucket(String bucket) throws GWException {
		boolean result = false;
		try {
			result = objManager.isBucketDelete(bucket);
			if (result) {
				objManager.removeBucket(bucket);
			}
        } catch (Exception e) {
            PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }

		if (!result) {
			logger.info(GWConstants.LOG_REQUEST_BUCKET_IS_NOT_EMPTY);
            throw new GWException(GWErrorCode.BUCKET_NOT_EMPTY, s3Parameter);
		}
	}

	protected int insertObject(String bucket, String object, Metadata data) throws GWException {
		int result = 0;
		try {
			result = objManager.close(bucket, object, data);
		} catch (Exception e) {
			// duplicate key error
			try {
				result = objManager.close(bucket, object, data);
			} catch (Exception e1) {
				PrintStack.logging(logger, e1);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		return result;
	}

	protected void updateObjectMeta(Metadata meta) throws GWException {
		try {
			objManager.updateObjectMeta(meta);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
	}

	protected void updateBucketAcl(String bucket, String aclXml) throws GWException {
		try {
			objManager.updateBucketAcl(bucket, aclXml);
		} catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }
	}

	protected void updateBucketCors(String bucket, String cors) throws GWException {
		try {
            objManager.updateBucketCors(bucket, cors);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }
	}

	protected void updateBucketLifecycle(String bucket, String lifecycle) throws GWException {
		try {
            objManager.updateBucketLifecycle(bucket, lifecycle);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }
	}

	protected void updateBucketReplication(String bucket, String replica) throws GWException {
		try {
            objManager.updateBucketReplication(bucket, replica);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }
	}

	protected void updateBucketTagging(String bucket, String tagging) throws GWException {
		try {
            objManager.updateBucketTagging(bucket, tagging);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }
	}

	protected boolean isBucketTagIndex(String bucket) throws GWException {
		boolean enable = false;
		try {
			enable = objManager.getObjectTagsIndexing().isIndexingEnabled(bucket);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }

		return enable;
	}

	protected void updateBucketTagIndex(String bucket, boolean enable) throws GWException {
		try {
			if (enable) {
				objManager.getObjectTagsIndexing().enableIndexing(bucket);
			} else {
				objManager.getObjectTagsIndexing().disableIndexing(bucket);
			}
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }
	}

	protected void updateBucketWeb(String bucket, String web) throws GWException {
		try {
            objManager.updateBucketWeb(bucket, web);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }
	}

	protected void updateBucketAccess(String bucket, String access) throws GWException {
		try {
            objManager.updateBucketAccess(bucket, access);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }
	}

	protected void updateBucketEncryption(String bucket, String encryption) throws GWException {
		try {
            objManager.updateBucketEncryption(bucket, encryption);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }
	}

	protected void updateBucketObjectLock(String bucket, String lock) throws GWException {
		try {
            objManager.updateBucketObjectLock(bucket, lock);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }
	}

	protected void updateBucketPolicy(String bucket, String policy) throws GWException {
		try {
            objManager.updateBucketPolicy(bucket, policy);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }
	}

	protected void updateBucketLogging(String bucket, String logging) throws GWException {
		try {
            objManager.updateBucketLogging(bucket, logging);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }
	}

	protected void updateBucketAnalytics(String bucket, String analytics) throws GWException {
		try {
			objManager.updateBucketAnalyticsConfiguration(bucket, analytics);
		} catch (Exception e) {
			logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
	}

	protected void updateBucketAccelerate(String bucket, String accelerate) throws GWException {
		try {
			objManager.updateBucketAccelerateConfiguration(bucket, accelerate);
		} catch (Exception e) {
			logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
	}

	protected void updateBucketPayment(String bucket, String payment) throws GWException {
		try {
			objManager.updateBucketPayment(bucket, payment);
		} catch (Exception e) {
			logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
	}

	protected Bucket getBucket(String bucket) throws GWException {
		Bucket bucketInfo = null;
		try {
			bucketInfo = objManager.getBucket(bucket);
		} catch (ResourceNotFoundException e) {
			return null;
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		return bucketInfo;
	}

	protected void putBucketVersioning(String bucket, String status) throws GWException {
		try {
			objManager.putBucketVersioning(bucket, status);
		} catch (ResourceNotFoundException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.NO_SUCH_BUCKET, s3Parameter);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
	}

	protected void insertRestoreObject(String bucket, String object, String versionId, String restoreXml) throws GWException {
		try {
			MQSender mqSender = new MQSender(AgentConfig.getInstance().getMQHost(), 
				Integer.parseInt(AgentConfig.getInstance().getMQPort()),
				AgentConfig.getInstance().getMQUser(),
				AgentConfig.getInstance().getMQPassword(),
				GWConstants.MQUEUE_LOG_EXCHANGE_NAME,
				GWConstants.MESSAGE_QUEUE_OPTION_DIRECT,
				"");

			JSONObject obj;
			obj = new JSONObject();

			obj.put(GWConstants.RESTORE_BUCKET_NAME, bucket);
			obj.put(GWConstants.RESTORE_OBJECT_NAME, object);
			obj.put(GWConstants.RESTORE_VERSION_ID, versionId);
			obj.put(GWConstants.RESTORE_XML, restoreXml);
			mqSender.send(obj.toString(), GWConstants.MQUEUE_NAME_GW_RESTORE_ROUTING_KEY);
			logger.debug("mqsender : {}", obj.toString());
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
	}

	protected List<S3BucketSimpleInfo> listBucketSimpleInfo(String userName, String userId) throws GWException {
		List<S3BucketSimpleInfo> bucketList = null;
		try {
			bucketList = objManager.listBucketSimpleInfo(userName, userId);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
		
		return bucketList;
	}

	protected List<Metadata> listBucketTags(String bucket, String tags, int max) throws GWException {
		List<Metadata> tagList = null;
		try {
			tagList = objManager.getObjectTagsIndexing().getObjectWithTags(bucket, tags, max);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
		
		return tagList;
	}

	protected ObjectListParameter listObject(String bucket, S3ObjectList s3ObjectList) throws GWException {
		ObjectListParameter objectListParameter = null;
		try {
			objectListParameter = objManager.listObject(bucket, s3ObjectList);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		return objectListParameter;
	}

	protected ObjectListParameter listObjectV2(String bucket, S3ObjectList s3ObjectList) throws GWException {
		ObjectListParameter objectListParameter = null;
		try {
			objectListParameter = objManager.listObjectV2(bucket, s3ObjectList);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		return objectListParameter;
	}

	protected ObjectListParameter listObjectVersions(String bucket, S3ObjectList s3ObjectList) throws GWException {
		ObjectListParameter objectListParameter = null;
		try {
			objectListParameter = objManager.listObjectVersions(bucket, s3ObjectList);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		return objectListParameter;
	}

	protected List<Map<String, String>> listBucketAnalytics(String userName, String userId) throws GWException {
		List<Map<String, String>> bucketAnalystics = null;
		try {
			bucketAnalystics = objManager.listBucketAnalyticsConfiguration(userName, userId);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
		
		return bucketAnalystics;
	}

	protected void updateObjectTagging(Metadata meta) throws GWException {
		try {
			objManager.updateObjectTagging(meta);
		} catch(Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
	}

	protected void updateObjectAcl(Metadata meta) throws GWException {
		try {
			objManager.updateObjectAcl(meta);
		} catch(Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
	}

	protected void updateObjectRestore(Metadata meta) throws GWException {
		try {
			objManager.updateObjectTagging(meta);
		} catch(Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
	}

	protected ObjMultipart getInstanceObjMultipart(String bucket) throws GWException {
		ObjMultipart objMultipart = null;
		try {
			objMultipart = objManager.getMultipartInsatance(bucket);
		}catch(Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		return objMultipart;
	}

	protected void checkGrantBucket(boolean isOwner, String s3Grant) throws GWException {
		if (s3Parameter.isPublicAccess()) {
			if (!checkGrant(true, isOwner, GWConstants.LOG_REQUEST_ROOT_ID, s3Grant)) {
				logger.error(GWConstants.LOG_REQUEST_PUBLIC_ACCESS_DENIED);
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
		} else {
			if (!checkGrant(true, isOwner, s3Parameter.getUser().getUserId(), s3Grant)) {
				logger.error(GWConstants.LOG_REQUEST_PUBLIC_ACCESS_DENIED);
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
		}
	}

	protected void checkGrantObject(boolean isOwner, String s3Grant) throws GWException {
		if (s3Parameter.isPublicAccess()) {
			if (!checkGrant(false, isOwner, GWConstants.LOG_REQUEST_ROOT_ID, s3Grant)) {
				logger.error(GWConstants.LOG_REQUEST_PUBLIC_ACCESS_DENIED);
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
		} else {
			if (!checkGrant(false, isOwner, s3Parameter.getUser().getUserId(), s3Grant)) {
				logger.error(GWConstants.LOG_REQUEST_PUBLIC_ACCESS_DENIED);
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
		}
	}

	protected boolean checkGrant(boolean isBucket, boolean isOwner, String id, String s3grant) throws GWException {
		AccessControlPolicy acp = null;
		if (isBucket) {
			acp = bucketAccessControlPolicy;
			if (acp == null) {
				logger.error("bucket AccessControlPolicy is null.");
				return false;
			}
		} else {
			acp = objectAccessControlPolicy;
			if (acp == null) {
				logger.error("object AccessControlPolicy is null.");
				return false;
			}
		}

		// owner
		if (isOwner) {
			if (acp.owner != null) {
				if (acp.owner.id != null) {
					if (acp.owner.id.compareTo(id) == 0) {
						return true;
					}
				}
			}
		}

		for (Grant grant: acp.aclList.grants) {
			if (grant.permission.compareTo(GWConstants.GRANT_FULL_CONTROL) == 0) {
				if (grant.grantee.type.compareTo(GWConstants.CANONICAL_USER) == 0) {
					if (grant.grantee.id.compareTo(id) == 0) {
						return true;
					}
				} else if (grant.grantee.type.compareTo(GWConstants.GROUP) == 0) {
					return true;
				}
			} else {
				if (grant.permission.compareTo(s3grant) == 0) {
					if (grant.grantee.type.compareTo(GWConstants.CANONICAL_USER) == 0) {
						if (grant.grantee.id.compareTo(id) == 0) {
							return true;
						}
					} else if (grant.grantee.type.compareTo(GWConstants.GROUP) == 0) {
						return true;
					}
				}
			}
		}

		return false;
	}

	protected static void readAclHeader(String grantstr, String permission, AccessControlPolicy policy) {
		String[] ids = grantstr.split(GWConstants.COMMA);
		for (String readid : ids) {
			String[] idkeyvalue = readid.split(GWConstants.EQUAL);
			Grant rg = new Grant();
			rg.grantee = new Grantee();

			if (idkeyvalue[0].trim().compareTo(GWConstants.ID) == 0) {
				rg.grantee.type = GWConstants.CANONICAL_USER;
				rg.grantee.id = idkeyvalue[1].replaceAll(GWConstants.DOUBLE_QUOTE, "");
			}

			if (idkeyvalue[0].trim().compareTo(GWConstants.URI) == 0) {
				rg.grantee.type = GWConstants.GROUP;
				rg.grantee.uri = idkeyvalue[1].replaceAll(GWConstants.DOUBLE_QUOTE, "");
			}

			// if (idkeyvalue[0].trim().compareTo(GWConstants.EMAIL_ADDRESS) == 0) {
			// 	rg.grantee.type = GWConstants.CANONICAL_USER;
			// 	rg.grantee.emailAddress = idkeyvalue[1].replaceAll(GWConstants.DOUBLE_QUOTE, "");
			// }

			rg.permission = permission;
			policy.aclList.grants.add(rg);
		}
	}

	protected String makeAcl(AccessControlPolicy preAccessControlPolicy, boolean isUsedAclXml) throws GWException {
		PublicAccessBlockConfiguration pabc = null;
		if (dstBucket != null && !Strings.isNullOrEmpty(dstBucket.getAccess())) {
			try {
				pabc = new XmlMapper().readValue(dstBucket.getAccess(), PublicAccessBlockConfiguration.class);
			} catch (JsonProcessingException e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		AccessControlPolicy acp = new AccessControlPolicy();
		acp.aclList = new AccessControlList();
		acp.aclList.grants = new ArrayList<Grant>();
		acp.owner = new Owner();

		if (preAccessControlPolicy != null) {
			acp.owner.id = preAccessControlPolicy.owner.id;
			acp.owner.displayName = preAccessControlPolicy.owner.displayName;
		} else {			
			acp.owner.id = s3Parameter.getUser().getUserId();
			acp.owner.displayName = s3Parameter.getUser().getUserName();
		}

		String cannedAcl = s3RequestData.getXAmzAcl();
		String grantRead = s3RequestData.getXAmzGrantRead();
		String grantReadAcp = s3RequestData.getXAmzGrantReadAcp();
		String grantWrite = s3RequestData.getXAmzGrantWrite();
		String grantWriteAcp = s3RequestData.getXAmzGrantWriteAcp();
		String grantFullControl = s3RequestData.getXAmzGrantFullControl();

		boolean isKeyword = false;
		if (!Strings.isNullOrEmpty(cannedAcl)) {
			isKeyword = true;
		} else if (!Strings.isNullOrEmpty(grantRead)) {
			isKeyword = true;
		} else if (!Strings.isNullOrEmpty(grantReadAcp)) {
			isKeyword = true;
		} else if (!Strings.isNullOrEmpty(grantWrite)) {
			isKeyword = true;
		} else if (!Strings.isNullOrEmpty(grantWriteAcp)) {
			isKeyword = true;
		} else if (!Strings.isNullOrEmpty(grantFullControl)) {
			isKeyword = true;
		}

		if (!isKeyword && isUsedAclXml) {
			acp = AccessControlPolicy.getAclClassFromXml(s3RequestData.getAclXml(), s3Parameter);
		} else {
			logger.debug("x-amz-acl : {}", cannedAcl);
			if (Strings.isNullOrEmpty(cannedAcl)) {
				if (Strings.isNullOrEmpty(grantRead)
				&& Strings.isNullOrEmpty(grantReadAcp)
				&& Strings.isNullOrEmpty(grantWrite)
				&& Strings.isNullOrEmpty(grantWriteAcp)
				&& Strings.isNullOrEmpty(grantFullControl)) {
					Grant priUser = new Grant();
					priUser.grantee = new Grantee();
					priUser.grantee.type = GWConstants.CANONICAL_USER;
					priUser.grantee.id = acp.owner.id;
					priUser.grantee.displayName = acp.owner.displayName;
					priUser.permission = GWConstants.GRANT_FULL_CONTROL;
					acp.aclList.grants.add(priUser);
				}
			} else {
				if (GWConstants.CANNED_ACLS_PRIVATE.equalsIgnoreCase(cannedAcl)) {
					Grant priUser = new Grant();
					priUser.grantee = new Grantee();
					priUser.grantee.type = GWConstants.CANONICAL_USER;
					priUser.grantee.id = acp.owner.id;
					priUser.grantee.displayName = acp.owner.displayName;
					priUser.permission = GWConstants.GRANT_FULL_CONTROL;
					acp.aclList.grants.add(priUser);
				} else if (GWConstants.CANNED_ACLS_PUBLIC_READ.equalsIgnoreCase(cannedAcl)) {
					if (pabc != null && GWConstants.STRING_TRUE.equalsIgnoreCase(pabc.BlockPublicAcls)) {
						logger.info(GWConstants.LOG_ACCESS_DENIED_PUBLIC_ACLS);
						throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
					}
					Grant priUser = new Grant();
					priUser.grantee = new Grantee();
					priUser.grantee.type = GWConstants.CANONICAL_USER;
					priUser.grantee.id = acp.owner.id;
					priUser.grantee.displayName = acp.owner.displayName;
					priUser.permission = GWConstants.GRANT_FULL_CONTROL;
					acp.aclList.grants.add(priUser);
	
					Grant pubReadUser = new Grant();
					pubReadUser.grantee = new Grantee();
					pubReadUser.grantee.type = GWConstants.GROUP;
					pubReadUser.grantee.uri = GWConstants.AWS_GRANT_URI_ALL_USERS;
					pubReadUser.permission = GWConstants.GRANT_READ;
					acp.aclList.grants.add(pubReadUser);
				} else if (GWConstants.CANNED_ACLS_PUBLIC_READ_WRITE.equalsIgnoreCase(cannedAcl)) {
					if (pabc != null && GWConstants.STRING_TRUE.equalsIgnoreCase(pabc.BlockPublicAcls)) {
						logger.info(GWConstants.LOG_ACCESS_DENIED_PUBLIC_ACLS);
						throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
					}
					Grant priUser = new Grant();
					priUser.grantee = new Grantee();
					priUser.grantee.type = GWConstants.CANONICAL_USER;
					priUser.grantee.id = acp.owner.id;
					priUser.grantee.displayName = acp.owner.displayName;
					priUser.permission = GWConstants.GRANT_FULL_CONTROL;
					acp.aclList.grants.add(priUser);
	
					Grant pubReadUser = new Grant();
					pubReadUser.grantee = new Grantee();
					pubReadUser.grantee.type = GWConstants.GROUP;
					pubReadUser.grantee.uri = GWConstants.AWS_GRANT_URI_ALL_USERS;
					pubReadUser.permission = GWConstants.GRANT_READ;
					acp.aclList.grants.add(pubReadUser);
	
					Grant pubWriteUser = new Grant();
					pubWriteUser.grantee = new Grantee();
					pubWriteUser.grantee.type = GWConstants.GROUP;
					pubWriteUser.grantee.uri = GWConstants.AWS_GRANT_URI_ALL_USERS;
					pubWriteUser.permission = GWConstants.GRANT_WRITE;
					acp.aclList.grants.add(pubWriteUser);
				} else if (GWConstants.CANNED_ACLS_AUTHENTICATED_READ.equalsIgnoreCase(cannedAcl)) {
					if (pabc != null && GWConstants.STRING_TRUE.equalsIgnoreCase(pabc.BlockPublicAcls)) {
						logger.info(GWConstants.LOG_ACCESS_DENIED_PUBLIC_ACLS);
						throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
					}
					Grant priUser = new Grant();
					priUser.grantee = new Grantee();
					priUser.grantee.type = GWConstants.CANONICAL_USER;
					priUser.grantee.id = acp.owner.id;
					priUser.grantee.displayName = acp.owner.displayName;
					priUser.permission = GWConstants.GRANT_FULL_CONTROL;
					acp.aclList.grants.add(priUser);
	
					Grant authReadUser = new Grant();
					authReadUser.grantee = new Grantee();
					authReadUser.grantee.type = GWConstants.GROUP;
					authReadUser.grantee.uri = GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS;
					authReadUser.permission = GWConstants.GRANT_READ;
					acp.aclList.grants.add(authReadUser);
				} else if (GWConstants.CANNED_ACLS_BUCKET_OWNER_READ.equalsIgnoreCase(cannedAcl)) {
					Grant priUser = new Grant();
					priUser.grantee = new Grantee();
					priUser.grantee.type = GWConstants.CANONICAL_USER;
					priUser.grantee.id = acp.owner.id;
					priUser.grantee.displayName = acp.owner.displayName;
					priUser.permission = GWConstants.GRANT_FULL_CONTROL;
					acp.aclList.grants.add(priUser);
	
					Grant bucketOwnerReadUser = new Grant();
					bucketOwnerReadUser.grantee = new Grantee();
					bucketOwnerReadUser.grantee.type = GWConstants.CANONICAL_USER;
					bucketOwnerReadUser.grantee.id = dstBucket.getUserId();
					bucketOwnerReadUser.grantee.displayName = dstBucket.getUserName();
					bucketOwnerReadUser.permission = GWConstants.GRANT_READ;
					acp.aclList.grants.add(bucketOwnerReadUser);
				} else if (GWConstants.CANNED_ACLS_BUCKET_OWNER_FULL_CONTROL.equalsIgnoreCase(cannedAcl)) {
					Grant priUser = new Grant();
					priUser.grantee = new Grantee();
					priUser.grantee.type = GWConstants.CANONICAL_USER;
					priUser.grantee.id = acp.owner.id;
					priUser.grantee.displayName = acp.owner.displayName;
					priUser.permission = GWConstants.GRANT_FULL_CONTROL;
					acp.aclList.grants.add(priUser);
	
					Grant bucketOwnerFullUser = new Grant();
					bucketOwnerFullUser.grantee = new Grantee();
					bucketOwnerFullUser.grantee.type = GWConstants.CANONICAL_USER;
					bucketOwnerFullUser.grantee.id = dstBucket.getUserId();
					bucketOwnerFullUser.grantee.displayName = dstBucket.getUserName();
					bucketOwnerFullUser.permission = GWConstants.GRANT_FULL_CONTROL;
					acp.aclList.grants.add(bucketOwnerFullUser);
				} else if (GWConstants.CANNED_ACLS.contains(cannedAcl)) {
					logger.error(GWErrorCode.NOT_IMPLEMENTED.getMessage() + GWConstants.LOG_ACCESS_CANNED_ACL, cannedAcl);
					throw new GWException(GWErrorCode.NOT_IMPLEMENTED, s3Parameter);
				} else {
					logger.error(HttpServletResponse.SC_BAD_REQUEST + GWConstants.LOG_ACCESS_PROCESS_FAILED);
					throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
				}
			}

			if (!Strings.isNullOrEmpty(grantRead)) {
				readAclHeader(grantRead, GWConstants.GRANT_READ, acp);
			}
			if (!Strings.isNullOrEmpty(grantWrite)) {
				readAclHeader(grantWrite, GWConstants.GRANT_WRITE, acp);
			}
			if (!Strings.isNullOrEmpty(grantReadAcp)) {
				readAclHeader(grantReadAcp, GWConstants.GRANT_READ_ACP, acp);
			}
			if (!Strings.isNullOrEmpty(grantWriteAcp)) {
				readAclHeader(grantWriteAcp, GWConstants.GRANT_WRITE_ACP, acp);
			}
			if (!Strings.isNullOrEmpty(grantFullControl)) {
				readAclHeader(grantFullControl, GWConstants.GRANT_FULL_CONTROL, acp);
			}
		}

		// check user
		if (S3UserManager.getInstance().getUserById(acp.owner.id) == null) {
			logger.error("cant find user id : {}", acp.owner.id);
			throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
		}
		for (Grant grant: acp.aclList.grants) {
			if (!Strings.isNullOrEmpty(grant.grantee.id)) {
				if (S3UserManager.getInstance().getUserById(grant.grantee.id) == null) {
					logger.error("cant find user id : {}", grant.grantee.id);
					throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
				}
			}
		}

		return acp.toString();
	}

	protected String makeAcl(AccessControlPolicy preAccessControlPolicy, DataPostObject data) throws GWException {
		PublicAccessBlockConfiguration pabc = null;
		if (dstBucket != null && !Strings.isNullOrEmpty(dstBucket.getAccess())) {
			try {
				pabc = new XmlMapper().readValue(dstBucket.getAccess(), PublicAccessBlockConfiguration.class);
			} catch (JsonProcessingException e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		AccessControlPolicy acp = new AccessControlPolicy();
		acp.aclList = new AccessControlList();
		acp.aclList.grants = new ArrayList<Grant>();
		acp.owner = new Owner();

		if (preAccessControlPolicy != null) {
			acp.owner.id = preAccessControlPolicy.owner.id;
			acp.owner.displayName = preAccessControlPolicy.owner.displayName;
		} else {			
			acp.owner.id = s3Parameter.getUser().getUserId();
			acp.owner.displayName = s3Parameter.getUser().getUserName();
		}

		String cannedAcl = data.getXAmzAcl();
		String grantRead = data.getXAmzGrantRead();
		String grantReadAcp = data.getXAmzGrantReadAcp();
		String grantWrite = data.getXAmzGrantWrite();
		String grantWriteAcp = data.getXAmzGrantWriteAcp();
		String grantFullControl = data.getXAmzGrantFullControl();

		logger.debug("x-amz-acl : {}", cannedAcl);
		if (Strings.isNullOrEmpty(cannedAcl)) {
			if (Strings.isNullOrEmpty(grantRead)
			&& Strings.isNullOrEmpty(grantReadAcp)
			&& Strings.isNullOrEmpty(grantWrite)
			&& Strings.isNullOrEmpty(grantWriteAcp)
			&& Strings.isNullOrEmpty(grantFullControl)) {
				Grant priUser = new Grant();
				priUser.grantee = new Grantee();
				priUser.grantee.type = GWConstants.CANONICAL_USER;
				priUser.grantee.id = acp.owner.id;
				priUser.grantee.displayName = acp.owner.displayName;
				priUser.permission = GWConstants.GRANT_FULL_CONTROL;
				acp.aclList.grants.add(priUser);
			}
		} else {
			if (GWConstants.CANNED_ACLS_PRIVATE.equalsIgnoreCase(cannedAcl)) {
				Grant priUser = new Grant();
				priUser.grantee = new Grantee();
				priUser.grantee.type = GWConstants.CANONICAL_USER;
				priUser.grantee.id = acp.owner.id;
				priUser.grantee.displayName = acp.owner.displayName;
				priUser.permission = GWConstants.GRANT_FULL_CONTROL;
				acp.aclList.grants.add(priUser);
			} else if (GWConstants.CANNED_ACLS_PUBLIC_READ.equalsIgnoreCase(cannedAcl)) {
				if (pabc != null && GWConstants.STRING_TRUE.equalsIgnoreCase(pabc.BlockPublicAcls)) {
					logger.info(GWConstants.LOG_ACCESS_DENIED_PUBLIC_ACLS);
					throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
				}
				Grant priUser = new Grant();
				priUser.grantee = new Grantee();
				priUser.grantee.type = GWConstants.CANONICAL_USER;
				priUser.grantee.id = acp.owner.id;
				priUser.grantee.displayName = acp.owner.displayName;
				priUser.permission = GWConstants.GRANT_FULL_CONTROL;
				acp.aclList.grants.add(priUser);

				Grant pubReadUser = new Grant();
				pubReadUser.grantee = new Grantee();
				pubReadUser.grantee.type = GWConstants.GROUP;
				pubReadUser.grantee.uri = GWConstants.AWS_GRANT_URI_ALL_USERS;
				pubReadUser.permission = GWConstants.GRANT_READ;
				acp.aclList.grants.add(pubReadUser);
			} else if (GWConstants.CANNED_ACLS_PUBLIC_READ_WRITE.equalsIgnoreCase(cannedAcl)) {
				if (pabc != null && GWConstants.STRING_TRUE.equalsIgnoreCase(pabc.BlockPublicAcls)) {
					logger.info(GWConstants.LOG_ACCESS_DENIED_PUBLIC_ACLS);
					throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
				}
				Grant priUser = new Grant();
				priUser.grantee = new Grantee();
				priUser.grantee.type = GWConstants.CANONICAL_USER;
				priUser.grantee.id = acp.owner.id;
				priUser.grantee.displayName = acp.owner.displayName;
				priUser.permission = GWConstants.GRANT_FULL_CONTROL;
				acp.aclList.grants.add(priUser);

				Grant pubReadUser = new Grant();
				pubReadUser.grantee = new Grantee();
				pubReadUser.grantee.type = GWConstants.GROUP;
				pubReadUser.grantee.uri = GWConstants.AWS_GRANT_URI_ALL_USERS;
				pubReadUser.permission = GWConstants.GRANT_READ;
				acp.aclList.grants.add(pubReadUser);

				Grant pubWriteUser = new Grant();
				pubWriteUser.grantee = new Grantee();
				pubWriteUser.grantee.type = GWConstants.GROUP;
				pubWriteUser.grantee.uri = GWConstants.AWS_GRANT_URI_ALL_USERS;
				pubWriteUser.permission = GWConstants.GRANT_WRITE;
				acp.aclList.grants.add(pubWriteUser);
			} else if (GWConstants.CANNED_ACLS_AUTHENTICATED_READ.equalsIgnoreCase(cannedAcl)) {
				if (pabc != null && GWConstants.STRING_TRUE.equalsIgnoreCase(pabc.BlockPublicAcls)) {
					logger.info(GWConstants.LOG_ACCESS_DENIED_PUBLIC_ACLS);
					throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
				}
				Grant priUser = new Grant();
				priUser.grantee = new Grantee();
				priUser.grantee.type = GWConstants.CANONICAL_USER;
				priUser.grantee.id = acp.owner.id;
				priUser.grantee.displayName = acp.owner.displayName;
				priUser.permission = GWConstants.GRANT_FULL_CONTROL;
				acp.aclList.grants.add(priUser);

				Grant authReadUser = new Grant();
				authReadUser.grantee = new Grantee();
				authReadUser.grantee.type = GWConstants.GROUP;
				authReadUser.grantee.uri = GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS;
				authReadUser.permission = GWConstants.GRANT_READ;
				acp.aclList.grants.add(authReadUser);
			} else if (GWConstants.CANNED_ACLS_BUCKET_OWNER_READ.equalsIgnoreCase(cannedAcl)) {
				Grant priUser = new Grant();
				priUser.grantee = new Grantee();
				priUser.grantee.type = GWConstants.CANONICAL_USER;
				priUser.grantee.id = acp.owner.id;
				priUser.grantee.displayName = acp.owner.displayName;
				priUser.permission = GWConstants.GRANT_FULL_CONTROL;
				acp.aclList.grants.add(priUser);

				Grant bucketOwnerReadUser = new Grant();
				bucketOwnerReadUser.grantee = new Grantee();
				bucketOwnerReadUser.grantee.type = GWConstants.CANONICAL_USER;
				bucketOwnerReadUser.grantee.id = dstBucket.getUserId();
				bucketOwnerReadUser.grantee.displayName = dstBucket.getUserName();
				bucketOwnerReadUser.permission = GWConstants.GRANT_READ;
				acp.aclList.grants.add(bucketOwnerReadUser);
			} else if (GWConstants.CANNED_ACLS_BUCKET_OWNER_FULL_CONTROL.equalsIgnoreCase(cannedAcl)) {
				Grant priUser = new Grant();
				priUser.grantee = new Grantee();
				priUser.grantee.type = GWConstants.CANONICAL_USER;
				priUser.grantee.id = acp.owner.id;
				priUser.grantee.displayName = acp.owner.displayName;
				priUser.permission = GWConstants.GRANT_FULL_CONTROL;
				acp.aclList.grants.add(priUser);

				Grant bucketOwnerFullUser = new Grant();
				bucketOwnerFullUser.grantee = new Grantee();
				bucketOwnerFullUser.grantee.type = GWConstants.CANONICAL_USER;
				bucketOwnerFullUser.grantee.id = dstBucket.getUserId();
				bucketOwnerFullUser.grantee.displayName = dstBucket.getUserName();
				bucketOwnerFullUser.permission = GWConstants.GRANT_FULL_CONTROL;
				acp.aclList.grants.add(bucketOwnerFullUser);
			} else if (GWConstants.CANNED_ACLS.contains(cannedAcl)) {
				logger.error(GWErrorCode.NOT_IMPLEMENTED.getMessage() + GWConstants.LOG_ACCESS_CANNED_ACL, cannedAcl);
				throw new GWException(GWErrorCode.NOT_IMPLEMENTED, s3Parameter);
			} else {
				logger.error(HttpServletResponse.SC_BAD_REQUEST + GWConstants.LOG_ACCESS_PROCESS_FAILED);
				throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
			}
		}

		if (!Strings.isNullOrEmpty(grantRead)) {
			readAclHeader(grantRead, GWConstants.GRANT_READ, acp);
		}
		if (!Strings.isNullOrEmpty(grantWrite)) {
			readAclHeader(grantWrite, GWConstants.GRANT_WRITE, acp);
		}
		if (!Strings.isNullOrEmpty(grantReadAcp)) {
			readAclHeader(grantReadAcp, GWConstants.GRANT_READ_ACP, acp);
		}
		if (!Strings.isNullOrEmpty(grantWriteAcp)) {
			readAclHeader(grantWriteAcp, GWConstants.GRANT_WRITE_ACP, acp);
		}
		if (!Strings.isNullOrEmpty(grantFullControl)) {
			readAclHeader(grantFullControl, GWConstants.GRANT_FULL_CONTROL, acp);
		}

		// check user
		if (S3UserManager.getInstance().getUserById(acp.owner.id) == null) {
			logger.error("cant find user id : {}", acp.owner.id);
			throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
		}
		for (Grant grant: acp.aclList.grants) {
			if (!Strings.isNullOrEmpty(grant.grantee.id)) {
				if (S3UserManager.getInstance().getUserById(grant.grantee.id) == null) {
					logger.error("cant find user id : {}", grant.grantee.id);
					throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
				}
			}
		}

		return acp.toString();
	}

	protected boolean policyIdCheck(String aws) {
		boolean effectcheck = false;
		if (aws.equals(GWConstants.ASTERISK)) {
			effectcheck = true;
			return effectcheck;
		}

		String[] arn = aws.split(GWConstants.COLON, -1);
		if (!Strings.isNullOrEmpty(arn[4])) {
			if (s3Parameter.getUser().getUserId().equals(arn[4])) {
				effectcheck = true;
				return effectcheck;
			}
		}

		return effectcheck;
	}

	protected boolean policyResourceCheck(String resource) {
		boolean effectcheck = false;

		String[] res = resource.split(GWConstants.COLON, -1);
		if (Strings.isNullOrEmpty(res[5])) {
			return effectcheck;
		}

		// all resource check
		if (res[5].equals("*")) {
			effectcheck = true;
			return effectcheck;
		}

		// bucket resource check
		String[] path = res[5].split(GWConstants.SLASH, 2);
		if (path.length == 1) {
			if (path[0].equals(s3Parameter.getBucketName())) {
				effectcheck = true;
				return effectcheck;
			}
			return effectcheck;
		}

		// object resource check
		if (path.length == 2) {
			if (!path[0].equals(s3Parameter.getBucketName())) {
				return effectcheck;
			}

			if (path[1].equals(GWConstants.ASTERISK)) {
				effectcheck = true;
				return effectcheck;
			}

			if (GWUtils.likematch(path[1], s3Parameter.getObjectName())) {
				effectcheck = true;
				return effectcheck;
			}
		}

		return effectcheck;
	}

	public void retentionCheck(String meta, String bypassGovernanceRetention, S3Parameter s3Parameter) throws GWException {
		S3Metadata retentionMetadata = null;
		if (!Strings.isNullOrEmpty(meta)) {
			retentionMetadata = S3Metadata.getS3Metadata(meta);
			if (!Strings.isNullOrEmpty(retentionMetadata.getLockMode()) && retentionMetadata.getLockMode().equalsIgnoreCase(GWConstants.GOVERNANCE)) {
				if (Strings.isNullOrEmpty(bypassGovernanceRetention)
					|| (!Strings.isNullOrEmpty(bypassGovernanceRetention) && !bypassGovernanceRetention.equalsIgnoreCase(GWConstants.STRING_TRUE))) {
					// check retention date
					long untilDate = GWUtils.parseRetentionTimeExpire(retentionMetadata.getLockExpires(), s3Parameter);
					long now = System.currentTimeMillis() / 1000;
					if (untilDate > now) {
						throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
					}
				}
			}

			if (!Strings.isNullOrEmpty(retentionMetadata.getLockMode()) && retentionMetadata.getLockMode().equalsIgnoreCase(GWConstants.COMPLIANCE)) {
				// check retention date
				long untilDate = GWUtils.parseRetentionTimeExpire(retentionMetadata.getLockExpires(), s3Parameter);
				long now = System.currentTimeMillis() / 1000;
				if (untilDate > now) {
					throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
				}
			}

			if (!Strings.isNullOrEmpty(retentionMetadata.getLegalHold()) && retentionMetadata.getLegalHold().equalsIgnoreCase(GWConstants.ON)) {
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
		}
	}

	public boolean checkPolicyBucket(String givenAction, S3Parameter s3Parameter) throws GWException {
		boolean effect = false;
		String policyJson = s3Parameter.getBucket().getPolicy();
		if (Strings.isNullOrEmpty(policyJson)) {
			return effect;
		}

		Policy policy = null;
		ObjectMapper jsonMapper = new ObjectMapper();
		try {
			policy = jsonMapper.readValue(policyJson, Policy.class);
			if (policy == null) {
				return effect;
			}
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		// check policy 
		for (Statement statement : policy.statements) {
			// check action
			logger.debug("statement : {}", statement);
			for (String action : statement.actions) {
				boolean isEffect = false;
				if (givenAction.equals(action) || GWConstants.ACTION_ALL.equals(action)) {
					// check principal (id)
					for (String aws : statement.principal.aws) {
						if (checkPolicyId(aws, s3Parameter)) {
							break;
						}
					}
					// check Resource (object path, bucket path)
					for (String resource : statement.resources) {
						if (checkPolicyResource(resource, s3Parameter)) {
							break;
						}
					}

					if (checkCondition(s3Parameter, statement)) {
						isEffect = true;
					}

					if (statement.effect.equals(GWConstants.ALLOW)) {
						logger.info("allow... {}", isEffect);
						if (isEffect) {
							effect = true;
						} else {
							throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
						}
					} else if (statement.effect.equals(GWConstants.DENY)) {
						logger.info("deny... {}", isEffect);
						if (isEffect) {
							throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
						}
					}
				}
			}
		}

		return effect;
	}

	public boolean checkPolicyId(String aws, S3Parameter s3Parameter) {
		boolean effectcheck = false;
		if (aws.equals(GWConstants.ASTERISK)) {
			effectcheck = true;
			return effectcheck;
		}

		String[] arn = aws.split(GWConstants.COLON, -1);
		if (!Strings.isNullOrEmpty(arn[4])) {
			if (s3Parameter.getUser().getUserId().equals(arn[4])) {
				effectcheck = true;
				return effectcheck;
			}
		}

		return effectcheck;
	}

	public boolean checkPolicyResource(String resource, S3Parameter s3Parameter) {
		boolean effectcheck = false;

		String[] res = resource.split(GWConstants.COLON, -1);
		if (Strings.isNullOrEmpty(res[5])) {
			return effectcheck;
		}

		// all resource check
		if (res[5].equals(GWConstants.ASTERISK)) {
			effectcheck = true;
			return effectcheck;
		}

		// bucket resource check
		String[] path = res[5].split(GWConstants.SLASH, 2);
		if (path.length == 1) {
			if (path[0].equals(s3Parameter.getBucketName())) {
				effectcheck = true;
				return effectcheck;
			}
			return effectcheck;
		}

		// object resource check
		if (path.length == 2) {
			if (!path[0].equals(s3Parameter.getBucketName())) {
				return effectcheck;
			}

			if (path[1].equals(GWConstants.ASTERISK)) {
				effectcheck = true;
				return effectcheck;
			}

			if (GWUtils.likematch(path[1], s3Parameter.getObjectName())) {
				effectcheck = true;
				return effectcheck;
			}
		}

		return effectcheck;
	}

	public boolean checkCondition(S3Parameter s3Parameter, Statement s) throws GWException {
		// condition check
		boolean conditioncheck = true;
		if (s.condition == null)
			return conditioncheck;

		for (Map.Entry<String, JsonNode> entry : s.condition.getUserExtensions().entries()) {
			logger.debug("key:{}, value:{}", entry.getKey(), entry.getValue());
			PolicyCondition pc = PolicyConditionFactory.createPolicyCondition(entry.getKey(), entry.getValue());
			if (pc == null) {
				continue;
			}

			pc.process();

			String comp = null;
			logger.info("PolicyCondition key : {}", pc.getKey());
			switch (pc.getKey()) {
				case GWConstants.KEY_AUTH_TYPE:
					break;
				case GWConstants.KEY_DELIMITER:
					comp = s3RequestData.getPolicyDelimter();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_MAXKYES:
					comp = s3RequestData.getPolicyMaxkeys();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_PREFIX:
					comp = s3RequestData.getPolicyPrefix();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_CONTENT_SHA256:
					comp = s3RequestData.getContentSHA256();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_ACL:
					comp = s3RequestData.getXAmzAcl();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_COPY_SOURCE:
					comp = s3RequestData.getXAmzCopySource();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_GRANT_FULL_CONTROL:
					comp = s3RequestData.getXAmzGrantFullControl();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_XAMZ_GRANT_READ:
					comp = s3RequestData.getXAmzGrantRead();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_GRANT_READ_ACP:
					comp = s3RequestData.getXAmzGrantReadAcp();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_GRANT_WRITE:
					comp = s3RequestData.getXAmzGrantWrite();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_GRANT_WRITE_ACP:
					comp = s3RequestData.getXAmzGrantWriteAcp();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_METADATA_DIRECTIVE:
					comp = s3RequestData.getXAmzMetadataDirective();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_SERVER_SIDE_ENCRYPTION:
					comp = s3RequestData.getXAmzServerSideEncryption();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_SERVER_SIDE_ENCRYPTION_AWS_KMS_KEY_ID:
					comp = s3RequestData.getXAmzServerSideEncryptionAwsKmsKeyId();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_STORAGE_CLASS:
					comp = s3RequestData.getXAmzStorageClass();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_WEBSITE_REDIRECT_LOCATION:
					comp = s3RequestData.getXAmzWebsiteRedirectLocation();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_OBJECT_LOCK_MODE:
					comp = s3RequestData.getXAmzObjectLockMode();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_OBJECT_LOCK_RETAIN_UNTIL_DATE:
					comp = s3RequestData.getXAmzObjectLockRetainUntilDate();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_OBJECT_LOCK_REMAINING_RETENTION_DAYS:
					// comp = dataRequest.getXAmzObjectLockRemainingRetentionDays();
					// conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_OBJECT_LOCK_LEGAL_HOLD:
					comp = s3RequestData.getXAmzObjectLockLegalHold();
					conditioncheck = pc.compare(comp);
					break;
				// IP address
				case "aws:SourceIp":
					comp = s3Parameter.getRemoteAddr();
					logger.debug("client ip : {}", comp);
					conditioncheck = pc.compare(comp);
					logger.debug("condition : {}", conditioncheck);
					break;
				default:
					break;
			}

			if (pc.getKey().startsWith(GWConstants.KEY_EXISTING_OBJECT_TAG)) {
				conditioncheck = pc.compareTagging(s3Parameter);
			}

			if (conditioncheck == false) {
				break;
			}
		}

		return conditioncheck;
	}
}
