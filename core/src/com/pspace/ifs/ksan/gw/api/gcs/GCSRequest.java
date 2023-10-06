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
package com.pspace.ifs.ksan.gw.api.gcs;

import com.google.common.base.Strings;

import com.pspace.ifs.ksan.gw.data.gcs.GCSRequestData;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.object.objmanager.ObjManagers;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.identity.ObjectListParameter;
import com.pspace.ifs.ksan.libs.identity.S3BucketSimpleInfo;
import com.pspace.ifs.ksan.libs.identity.S3ObjectList;
import com.pspace.ifs.ksan.objmanager.Bucket;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjManager;
import com.pspace.ifs.ksan.objmanager.ObjMultipart;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceAlreadyExistException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

import java.util.List;

import org.slf4j.Logger;

public abstract class GCSRequest {
    protected S3Parameter s3Parameter;
	protected GCSRequestData gcsRequestData;
	protected ObjManager objManager;
	protected Logger logger;
	protected Bucket srcBucket;
	protected Bucket dstBucket;
	// protected AccessControlPolicy bucketAccessControlPolicy;
	// protected AccessControlPolicy objectAccessControlPolicy;

	public GCSRequest(S3Parameter s3Parameter) {
		this.s3Parameter = new S3Parameter(s3Parameter);
		gcsRequestData = new GCSRequestData(s3Parameter);
		srcBucket = null;
		dstBucket = null;
		// bucketAccessControlPolicy = null;
		// objectAccessControlPolicy = null;
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
			// if (dstBucket != null) {
			// 	String bucketAcl = dstBucket.getAcl();
			// 	if (!Strings.isNullOrEmpty(bucketAcl)) {
			// 		bucketAccessControlPolicy = AccessControlPolicy.getAclClassFromJson(bucketAcl);
			// 	}
			// }
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

    protected Metadata open(String bucket, String object) throws GWException {
		Metadata meta = null;
		try {
			meta = objManager.open(bucket, object);
			// if (meta != null) {
			// 	String objectAcl = meta.getAcl();
			// 	if (!Strings.isNullOrEmpty(objectAcl)) {
			// 		objectAccessControlPolicy = AccessControlPolicy.getAclClassFromJson(objectAcl);
			// 	}
			// }
		} catch (ResourceNotFoundException e) {
			throw new GWException(GWErrorCode.NO_SUCH_KEY, s3Parameter);
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

    protected void remove(String bucket, String object) throws GWException {
		try {
			objManager.remove(bucket, object);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
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
}
