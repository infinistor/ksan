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
package com.pspace.ifs.ksan.gw.identity;

import java.util.Date;

import com.google.common.base.Strings;

public class S3Bucket {
	private String userName;
	private String userId;
	private String bucket;
	private String acl;
	private String web;
	private String cors;
	private String lifecycle;
	private String versioning;
	private String access;
	private String tagging;
	private String encryption;
	private String replication;
	private String logging;
	private String notification;
	private String policy;
	private String objectLock;
	private Date timestamp;
	
	public S3Bucket() {
		userName = "";
		userId = "";
		bucket = "";
		acl = "";
		web = "";
		cors = "";
		lifecycle = "";
		versioning = "";
		access = "";
		tagging = "";
		encryption = "";
		replication = "";
		logging = "";
		notification = "";
		policy = "";
		objectLock = "";
		timestamp = null;
	}

	public String getUserName() {
		return userName;
	}
	public void setUserName(String user) {
		this.userName = user;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getBucket() {
		return bucket;
	}
	public void setBucket(String bucket) {
		this.bucket = bucket;
	}
	public String getAcl() {
		return acl;
	}
	public void setAcl(String acl) {
		this.acl = acl;
	}
	public String getWeb() {
		return web;
	}
	public void setWeb(String web) {
		this.web = web;
	}
	public String getCors() {
		return cors;
	}
	public void setCors(String cors) {
		this.cors = cors;
	}
	public String getLifecycle() {
		return lifecycle;
	}
	public void setLifecycle(String lifecycle) {
		this.lifecycle = lifecycle;
	}
	public String getVersioning() {
		return versioning;
	}
	public void setVersioning(String versioning) {
		this.versioning = versioning;
	}
	public String getAccess() {
		return access;
	}
	public void setAccess(String access) {
		this.access = access;
	}
	public String getTagging() {
		return tagging;
	}
	public void setTagging(String tagging) {
		this.tagging = tagging;
	}
	public String getEncryption() {
		return encryption;
	}
	public void setEncryption(String encryption) {
		this.encryption = encryption;
	}
	public String getReplication() {
		return replication;
	}
	public void setReplication(String replication) {
		this.replication = replication;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getLogging() {
		return logging;
	}

	public void setLogging(String logging) {
		this.logging = logging;
	}

	public String getNotification() {
		return notification;
	}

	public void setNotification(String notification) {
		this.notification = notification;
	}

	public String getPolicy() {
		return policy;
	}

	public void setPolicy(String policy) {
		this.policy = policy;
	}

	public String getObjectLock() {
		return objectLock;
	}

	public void setObjectLock(String objectLock) {
		this.objectLock = objectLock;
	}
	
}
