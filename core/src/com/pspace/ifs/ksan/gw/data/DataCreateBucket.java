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
package com.pspace.ifs.ksan.gw.data;

import java.util.Collections;

import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

import org.slf4j.LoggerFactory;

public class DataCreateBucket extends S3DataRequest {
	private boolean hasAclKeyword;
	private String acl;
	private String grantFullControl;
	private String grantRead;
	private String grantReadAcp;
	private String grantWrite;
	private String grantWriteAcp;
	private String bucketObjectLockEnabled;
	
	private String bucketConfiguration;
	
	public DataCreateBucket(S3Parameter s3Parameter) throws GWException {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(DataCreateBucket.class);
		hasAclKeyword = false;
	}

	@Override
	public void extract() throws GWException {
		for (String headerName : Collections.list(s3Parameter.getRequest().getHeaderNames())) {
			if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_BUCKET_OBJECT_LOCK_ENABLED)) {
				bucketObjectLockEnabled = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_ACL)) {
				acl = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
				if (!Strings.isNullOrEmpty(acl)) {
					hasAclKeyword = true;
				}
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_GRANT_FULL_CONTROL)) {
				grantFullControl = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
				if (!Strings.isNullOrEmpty(grantFullControl)) {
					hasAclKeyword = true;
				}
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_GRANT_READ)) {
				grantRead = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
				if (!Strings.isNullOrEmpty(grantRead)) {
					hasAclKeyword = true;
				}
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_GRANT_READ_ACP)) {
				grantReadAcp = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
				if (!Strings.isNullOrEmpty(grantReadAcp)) {
					hasAclKeyword = true;
				}
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_GRANT_WRITE)) {
				grantWrite = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
				if (!Strings.isNullOrEmpty(grantWrite)) {
					hasAclKeyword = true;
				}
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_GRANT_WRITE_ACP)) {
				grantWriteAcp = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
				if (!Strings.isNullOrEmpty(grantWriteAcp)) {
					hasAclKeyword = true;
				}
			} 
		}
	}
	
	public boolean hasAclKeyword() {
		return hasAclKeyword;
	}

	public String getBucketObjectLockEnabled() {
		return bucketObjectLockEnabled;
	}

	public String getAcl() {
		return acl;
	}

	public String getGrantFullControl() {
		return grantFullControl;
	}

	public String getGrantRead() {
		return grantRead;
	}

	public String getGrantReadAcp() {
		return grantReadAcp;
	}

	public String getGrantWrite() {
		return grantWrite;
	}

	public String getGrantWriteAcp() {
		return grantWriteAcp;
	}
	
	public String getBucketConfiguration() throws GWException {
		bucketConfiguration = readXml();
		return bucketConfiguration;
	}
}
