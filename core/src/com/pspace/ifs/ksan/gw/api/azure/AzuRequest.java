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

package com.pspace.ifs.ksan.gw.api.azure;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.List;

import org.slf4j.Logger;

import com.pspace.ifs.ksan.gw.exception.AzuException;
import com.pspace.ifs.ksan.gw.identity.AzuParameter;
import com.pspace.ifs.ksan.gw.object.objmanager.ObjManagers;
import com.pspace.ifs.ksan.gw.utils.AzuConstants;
import com.pspace.ifs.ksan.objmanager.ObjManager;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.gw.exception.AzuErrorCode;
import com.pspace.ifs.ksan.objmanager.Bucket;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceAlreadyExistException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.libs.identity.S3BucketSimpleInfo;
import com.pspace.ifs.ksan.libs.identity.ObjectListParameter;
import com.pspace.ifs.ksan.libs.identity.S3ObjectList;

public abstract class AzuRequest {
    protected AzuParameter azuParameter;
    protected ObjManager objManager;
    protected Bucket container;
    protected Logger logger;

    public AzuRequest(AzuParameter parameter) {
        this.azuParameter = parameter;
		objManager = ObjManagers.getInstance().getObjManager();
    }

    public abstract void process() throws AzuException;

    protected boolean isExistContainer(String containerName) throws AzuException {
		boolean result = false;
		try {
			result = objManager.isBucketExist(containerName);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
		}

		return result;
	}

    protected void initContainerInfo(String containerName) throws AzuException {
		try {
			container = objManager.getBucket(containerName);
		} catch (ResourceNotFoundException e) {
			throw new AzuException(AzuErrorCode.NO_SUCH_CONTAINER, azuParameter);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
		}

		if (container == null) {
			logger.info(AzuConstants.LOG_CONTAINER_IS_NOT_EXIST, containerName);
			throw new AzuException(AzuErrorCode.NO_SUCH_CONTAINER, azuParameter);
		}
	}

    protected int createContainer(Bucket container) throws AzuException {
		int result = 0;
		try {
            result = objManager.createBucket(container);
        } catch (ResourceAlreadyExistException e) {
			PrintStack.logging(logger, e);
            throw new AzuException(AzuErrorCode.CONTAINER_ALREADY_EXISTS, azuParameter);
        } catch(ResourceNotFoundException e) {
			PrintStack.logging(logger, e);
            throw new AzuException(AzuErrorCode.INTERNAL_SERVER_ERROR, azuParameter);
        } catch (Exception e) {
			PrintStack.logging(logger, e);
            throw new AzuException(AzuErrorCode.INTERNAL_SERVER_ERROR, azuParameter);
		}

		if (result != 0) {
			throw new AzuException(AzuErrorCode.INTERNAL_SERVER_DB_ERROR, azuParameter);
		}

		return  result;
	}

    protected void deleteContainer(String container) throws AzuException {
		boolean result = false;
		try {
            objManager.removeBucket(container);
        } catch (Exception e) {
            PrintStack.logging(logger, e);
			throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
        }
	}

    protected List<S3BucketSimpleInfo> listContainerSimpleInfo(String userName, String userId) throws AzuException {
		List<S3BucketSimpleInfo> bucketList = null;
		try {
			bucketList = objManager.listBucketSimpleInfo(userName, userId);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
		}
		
		return bucketList;
	}

    protected Metadata open(String containerName, String blobName) throws AzuException {
		Metadata meta = null;
		try {
			meta = objManager.open(containerName, blobName, "null");
		} catch (ResourceNotFoundException e) {
			throw new AzuException(AzuErrorCode.NO_SUCH_KEY, azuParameter);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
		}

		return meta;
	}

    protected Metadata createLocal(String diskpoolId, String containerName, String blobName, String versionId) throws AzuException {
		Metadata meta = null;
		try {
			meta = objManager.createLocal(diskpoolId, containerName, blobName, versionId);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
		}

		return meta;
	}

    protected int insertObject(String containerName, String blobName, Metadata data) throws AzuException {
		int result = 0;
		try {
			result = objManager.close(containerName, blobName, data);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
		}

		return result;
	}

    protected ObjectListParameter listObject(String bucket, S3ObjectList s3ObjectList) throws AzuException {
		ObjectListParameter objectListParameter = null;
		try {
			objectListParameter = objManager.listObject(bucket, s3ObjectList);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
		}

		return objectListParameter;
	}

    protected void remove(String bucket, String object) throws AzuException {
		try {
			objManager.remove(bucket, object);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
		}
	}

    protected String formatDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat(AzuConstants.TIME_GMT_FORMAT, new Locale(AzuConstants.LOCALE_EN));
        formatter.setTimeZone(TimeZone.getTimeZone(AzuConstants.TIMEZONE_GMT));

        return formatter.format(date);
    }
}
