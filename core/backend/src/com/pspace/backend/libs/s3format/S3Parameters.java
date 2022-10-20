/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License. See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.backend.libs.s3format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class S3Parameters {
	public static final String ENABLED = "Enabled";
	public static final String DISABLED = "Disabled";
	public static final String PUT = "PUT";
	public static final String DELETE = "DELETE";

	public static final String OP_DELETE_WEBSITE = "REST.DELETE.WEBSITE";
	public static final String OP_DELETE_POLICY = "REST.DELETE.POLICY";
	public static final String OP_DELETE_CORS = "REST.DELETE.CORS";
	public static final String OP_DELETE_LIFECYCLE = "REST.DELETE.LIFECYCLE";
	public static final String OP_DELETE_PUBLICACCESSBLOCK = "REST.DELETE.PUBLICACCESSBLOCK";
	public static final String OP_DELETE_BUCKET_TAGGING = "REST.DELETE.BUCKET.TAGGING";
	public static final String OP_DELETE_ENCRYPTION = "REST.DELETE.ENCRYPTION";
	public static final String OP_DELETE_LOGGING = "REST.DELETE.LOGGING";
	public static final String OP_DELETE_BUCKET = "REST.DELETE.BUCKET";
	public static final String OP_DELETE_OBJECT_UPLOAD = "REST.DELETE.OBJECT.UPLOAD";
	public static final String OP_DELETE_OBJECT_TAGGING = "REST.DELETE.OBJECT.TAGGING";
	public static final String OP_DELETE_OBJECT = "REST.DELETE.OBJECT";
	public static final String OP_DELETE_OBJECTLOCK = "REST.DELETE.OBJECTLOCK";
	public static final String OP_DELETE_NOTIFICATION = "REST.DELETE.NOTIFICATION";
	public static final String OP_DELETE_REPLICATION = "REST.DELETE.REPLICATION";

	public static final String OP_GET_LISTBUCKET = "REST.GET.LISTBUCKET";
	public static final String OP_GET_WEBSITE = "REST.GET.WEBSITE";
	public static final String OP_GET_POLICY = "REST.GET.POLICY";
	public static final String OP_GET_CORS = "REST.GET.CORS";
	public static final String OP_GET_LIFECYCLE = "REST.GET.LIFECYCLE";
	public static final String OP_GET_PUBLICACCESSBLOCK = "REST.GET.PUBLICACCESSBLOCK";
	public static final String OP_GET_BUCKET_TAGGING = "REST.GET.BCUKET.TAGGING";
	public static final String OP_GET_LOGGING = "REST.GET.LOGGING";
	public static final String OP_GET_OBJECTLOCK = "REST.GET.OBJECTLOCK";
	public static final String OP_GET_NOTIFICATION = "REST.GET.NOTIFICATION";
	public static final String OP_GET_BUCKET_POLICY_STATUS = "REST.GET.BUCKET.POLICY.STATUS";
	public static final String OP_GET_REPLICATION = "REST.GET.REPLICATION";

	public static final String OP_GET_ENCRYPTION = "REST.GET.ENCRYPTION";
	public static final String OP_GET_BUCKET_ACL = "REST.GET.BUCKET.ACL";
	public static final String OP_GET_LOCATION = "REST.GET.LOCATION";
	public static final String OP_GET_UPLOADS = "REST.GET.UPLOADS";
	public static final String OP_GET_VERSIONING = "REST.GET.VERSIONING";
	public static final String OP_GET_LISTOBJECTSV2 = "REST.GET.LISTOBJECTV2";
	public static final String OP_GET_LISTOBJECTS = "REST.GET.LISTOBJECT";
	public static final String OP_GET_LISTVERSIONS = "REST.GET.LISTVERSIONS";
	public static final String OP_GET_OBJECT_ACL = "REST.GET.OBJECT.ACL";
	public static final String OP_GET_OBJECT_RETENTION = "REST.GET.OBJECT.RETENTION";
	public static final String OP_GET_OBJECT_LEGAL_HOLD = "REST.GET.OBJECT.LEGAL.HOLD";
	public static final String OP_GET_OBJECT_LISTPARTS = "REST.GET.OBJECT.LISTPARTS";
	public static final String OP_GET_OBJECT_TAGGING = "REST.GET.OBJECT.TAGGING";
	public static final String OP_GET_OBJECT = "REST.GET.OBJECT";

	public static final String OP_HEAD_BUCKET = "REST.HEAD.BUCKET";
	public static final String OP_HEAD_OBJECT = "REST.HEAD.OBJECT";

	public static final String OP_POST_DELETEOBJECTS = "REST.POST.DELETEOBJECTS";
	public static final String OP_POST_UPLOAD = "REST.POST.UPLOAD";
	public static final String OP_POST_COMPLETE = "REST.POST.COMPLETEUPLOAD";
	public static final String OP_POST_OBJECT = "REST.POST.OBJECT";

	public static final String OP_PUT_WEBSITE = "REST.PUT.WEBSITE";
	public static final String OP_PUT_POLICY = "REST.PUT.POLICY";
	public static final String OP_PUT_CORS = "REST.PUT.CORS";
	public static final String OP_PUT_LIFECYCLE = "REST.PUT.LIFECYCLE";
	public static final String OP_PUT_PUBLICACCESSBLOCK = "REST.PUT.PUBLICACCESSBLOCK";
	public static final String OP_PUT_BUCKET_TAGGING = "REST.PUT.BUCKET.TAGGING";
	public static final String OP_PUT_LOGGING = "REST.PUT.LOGGING";
	public static final String OP_PUT_ENCRYPTION = "REST.PUT.ENCRYPTION";
	public static final String OP_PUT_BUCKET_ACL = "REST.PUT.BUCKET.ACL";
	public static final String OP_PUT_VERSIONING = "REST.PUT.VERSIONING";
	public static final String OP_PUT_BCUKET = "REST.PUT.BUCKET";
	public static final String OP_PUT_OBJECTLOCK = "REST.PUT.OBJECTLOCK";
	public static final String OP_PUT_NOTIFICATION = "REST.PUT.NOTIFICATION";
	public static final String OP_PUT_REPLICATION = "REST.PUT.REPLICATION";

	public static final String OP_PUT_OBJECT_PART_COPY = "REST.PUT.OBJECT.PART.COPY";
	public static final String OP_PUT_OBJECT_PART = "REST.PUT.OBJECT.PART";
	public static final String OP_PUT_OBJECT_COPY = "REST.PUT.OBJECT.COPY";
	public static final String OP_PUT_OBJECT_RETENTION = "REST.PUT.OBJECT.RETENTION";
	public static final String OP_PUT_OBJECT_LEGAL_HOLD = "REST.PUT.OBJECT.LEGAL.HOLD";
	public static final String OP_PUT_OBJECT_TAGGING = "REST.PUT.OBJECT.TAGGING";
	public static final String OP_PUT_OBJECT_ACL = "REST.PUT.OBJECT.ACL";
	public static final String OP_PUT_OBJECT = "REST.PUT.OBJECT";

	public static boolean isEnabled(String Status) {
		return ENABLED.equalsIgnoreCase(Status);
	}

	public static boolean isDisabled(String Status) {
		return DISABLED.equalsIgnoreCase(Status);
	}

	// 해당 작업이 추가인지 확인
	public static boolean PutOperationCheck(String Operation) {
		// 이름에 PUT이 포함되어 있지 않을 경우 false 반환
		if (Operation.indexOf(PUT) < 0)
			return false;
		return true;
	}

	// 해당 작업이 삭제인지 확인
	public static boolean DeleteOperationCheck(String Operation) {
		// 이름에 DELETE가 포함되어 있지 않을 경우 false 반환
		if (Operation.indexOf(DELETE) < 0)
			return false;
		return true;
	}

	/*************************************
	 * Bucket Info Change Event
	 **************************************/
	// 버킷의 설정이 변경되는 작업 목록
	public static final List<String> BUCKET_INFO_CHANGED = new ArrayList<String>(Arrays.asList(new String[] {
			OP_PUT_LOGGING, OP_PUT_NOTIFICATION, OP_PUT_REPLICATION,
			OP_DELETE_BUCKET, OP_DELETE_LOGGING, OP_DELETE_NOTIFICATION, OP_DELETE_REPLICATION }));

	// 버킷의 설정이 변경되었는지 확인
	public static boolean BucketInfoChanged(String Operation) {
		if (BUCKET_INFO_CHANGED.contains(Operation))
			return true;
		return false;
	}

	// 버킷의 설정이 추가되는 작업 목록
	public static final List<String> BUCKET_INFO_UPDATE = new ArrayList<String>(Arrays.asList(new String[] {
			OP_PUT_LOGGING, OP_PUT_NOTIFICATION, OP_PUT_REPLICATION }));

	// 버킷의 설정이 추가되는지 확인
	public static boolean BucketInfoUpdateCheck(String Operation) {
		if (BUCKET_INFO_UPDATE.contains(Operation))
			return true;
		return false;
	}

	/********************************************
	 * Replication
	 ********************************************/
	// 복제해야할 작업 목록
	public static final List<String> REPLICATE_OPERATION_LIST = new ArrayList<>(Arrays.asList(new String[] {
			OP_PUT_OBJECT, OP_PUT_OBJECT_COPY, OP_POST_COMPLETE, OP_PUT_OBJECT_ACL, OP_PUT_OBJECT_RETENTION, OP_PUT_OBJECT_TAGGING, OP_DELETE_OBJECT,
			OP_DELETE_OBJECT_TAGGING }));

	// 복제해야할 작업 확인
	public static boolean ReplicateOperationCheck(String Operation) {
		if (REPLICATE_OPERATION_LIST.contains(Operation))
			return true;
		return false;
	}
}