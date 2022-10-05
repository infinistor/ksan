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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HttpHeaders;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.ksanapi.*;

public class S3RequestFactory {
	private final Logger logger;

	private final String OP_DELETE_WEBSITE = "REST.DELETE.WEBSITE";
	private final String OP_DELETE_POLICY = "REST.DELETE.POLICY";
	private final String OP_DELETE_CORS = "REST.DELETE.CORS";
	private final String OP_DELETE_LIFECYCLE = "REST.DELETE.LIFECYCLE";
	private final String OP_DELETE_PUBLICACCESSBLOCK = "REST.DELETE.PUBLICACCESSBLOCK";
	private final String OP_DELETE_BUCKET_TAGGING = "REST.DELETE.BUCKET.TAGGING";
	private final String OP_DELETE_ENCRYPTION = "REST.DELETE.ENCRYPTION";
	private final String OP_DELETE_LOGGING = "REST.DELETE.LOGGING";
	private final String OP_DELETE_BUCKET = "REST.DELETE.BUCKET";
	private final String OP_DELETE_OBJECT_UPLOAD = "REST.DELETE.OBJECT.UPLOAD";
	private final String OP_DELETE_OBJECT_TAGGING = "REST.DELETE.OBJECT.TAGGING";
	private final String OP_DELETE_OBJECT = "REST.DELETE.OBJECT";
	private final String OP_DELETE_OBJECTLOCK = "REST.DELETE.OBJECTLOCK";
	private final String OP_DELETE_NOTIFICATION = "REST.DELETE.NOTIFICATION";
	private final String OP_DELETE_REPLICATION = "REST.DELETE.REPLICATION";

	private final String OP_GET_LISTBUCKET = "REST.GET.LISTBUCKET";
	private final String OP_GET_WEBSITE = "REST.GET.WEBSITE";
	private final String OP_GET_POLICY = "REST.GET.POLICY";
	private final String OP_GET_CORS = "REST.GET.CORS";
	private final String OP_GET_LIFECYCLE = "REST.GET.LIFECYCLE";
	private final String OP_GET_PUBLICACCESSBLOCK = "REST.GET.PUBLICACCESSBLOCK";
	private final String OP_GET_BUCKET_TAGGING = "REST.GET.BCUKET.TAGGING";
	private final String OP_GET_LOGGING = "REST.GET.LOGGING";
	private final String OP_GET_OBJECTLOCK = "REST.GET.OBJECTLOCK";
	private final String OP_GET_NOTIFICATION = "REST.GET.NOTIFICATION";
	private final String OP_GET_BUCKET_POLICY_STATUS = "REST.GET.BUCKET.POLICY.STATUS";
	private final String OP_GET_REPLICATION = "REST.GET.REPLICATION";

	private final String OP_GET_ENCRYPTION = "REST.GET.ENCRYPTION";
	private final String OP_GET_BUCKET_ACL = "REST.GET.BUCKET.ACL";
	private final String OP_GET_LOCATION = "REST.GET.LOCATION";
	private final String OP_GET_UPLOADS = "REST.GET.UPLOADS";
	private final String OP_GET_VERSIONING = "REST.GET.VERSIONING";
	private final String OP_GET_LISTOBJECTSV2 = "REST.GET.LISTOBJECTV2";
	private final String OP_GET_LISTOBJECTS = "REST.GET.LISTOBJECT";
	private final String OP_GET_LISTVERSIONS = "REST.GET.LISTVERSIONS";
	private final String OP_GET_OBJECT_ACL = "REST.GET.OBJECT.ACL";
	private final String OP_GET_OBJECT_RETENTION = "REST.GET.OBJECT.RETENTION";
	private final String OP_GET_OBJECT_LEGAL_HOLD = "REST.GET.OBJECT.LEGAL.HOLD";
	private final String OP_GET_OBJECT_LISTPARTS = "REST.GET.OBJECT.LISTPARTS";
	private final String OP_GET_OBJECT_TAGGING = "REST.GET.OBJECT.TAGGING";
	private final String OP_GET_OBJECT = "REST.GET.OBJECT";

	private final String OP_HEAD_BUCKET = "REST.HEAD.BUCKET";
	private final String OP_HEAD_OBJECT = "REST.HEAD.OBJECT";

	private final String OP_POST_DELETEOBJECTS = "REST.POST.DELETEOBJECTS";
	private final String OP_POST_UPLOAD = "REST.POST.UPLOAD";
	private final String OP_POST_COMPLETE = "REST.POST.COMPLETEUPLOAD";
	private final String OP_POST_OBJECT = "REST.POST.OBJECT";

	private final String OP_PUT_WEBSITE = "REST.PUT.WEBSITE";
	private final String OP_PUT_POLICY = "REST.PUT.POLICY";
	private final String OP_PUT_CORS = "REST.PUT.CORS";
	private final String OP_PUT_LIFECYCLE = "REST.PUT.LIFECYCLE";
	private final String OP_PUT_PUBLICACCESSBLOCK = "REST.PUT.PUBLICACCESSBLOCK";
	private final String OP_PUT_BUCKET_TAGGING = "REST.PUT.BUCKET.TAGGING";
	private final String OP_PUT_LOGGING = "REST.PUT.LOGGING";
	private final String OP_PUT_ENCRYPTION = "REST.PUT.ENCRYPTION";
	private final String OP_PUT_BUCKET_ACL = "REST.PUT.BUCKET.ACL";
	private final String OP_PUT_VERSIONING = "REST.PUT.VERSIONING";
	private final String OP_PUT_BUCKET = "REST.PUT.BUCKET";
	private final String OP_PUT_OBJECTLOCK = "REST.PUT.OBJECTLOCK";
	private final String OP_PUT_NOTIFICATION = "REST.PUT.NOTIFICATION";
	private final String OP_PUT_REPLICATION = "REST.PUT.REPLICATION";
	
	private final String OP_PUT_OBJECT_PART_COPY = "REST.PUT.OBJECT.PART.COPY";
	private final String OP_PUT_OBJECT_PART = "REST.PUT.OBJECT.PART";
	private final String OP_PUT_OBJECT_COPY = "REST.PUT.OBJECT.COPY";
	private final String OP_PUT_OBJECT_RETENTION = "REST.PUT.OBJECT.RETENTION";
	private final String OP_PUT_OBJECT_LEGAL_HOLD = "REST.PUT.OBJECT.LEGAL.HOLD";
	private final String OP_PUT_OBJECT_TAGGING = "REST.PUT.OBJECT.TAGGING";
	private final String OP_PUT_OBJECT_ACL = "REST.PUT.OBJECT.ACL";
	private final String OP_PUT_OBJECT = "REST.PUT.OBJECT";

	private final String OP_OPTIONS = "REST.OPTIONS";
	
	private final String OP_ADMIN_DELETE_OBJECT = "ADMIN.DELETE.OBJECT";
	private final String OP_ADMIN_DELETE_OBJECT_TAGGING = "ADMIN.DELETE.OBJECT.TAGGING";
	private final String OP_ADMIN_DELETE_OBJECT_UPLOAD = "ADMIN.DELETE.OBJECT.UPLOAD";
	private final String OP_ADMIN_GET_OBJECT_ACL = "ADMIN.GET.OBJECT.ACL";
	private final String OP_ADMIN_GET_OBJECT_TAGGING = "ADMIN.GET.OBJECT.TAGGING";
	private final String OP_ADMIN_GET_OBJECT = "ADMIN.GET.OBJECT";
	private final String OP_ADMIN_HEAD_OBJECT = "ADMIN.HEAD.OBJECT";
	private final String OP_ADMIN_POST_UPLOAD = "ADMIN.POST.UPLOAD";
	private final String OP_ADMIN_POST_COMPLETE = "ADMIN.POST.COMPLETEUPLOAD";
	private final String OP_ADMIN_PUT_OBJECT_ACL = "ADMIN.PUT.OBJECT.ACL";
	private final String OP_ADMIN_PUT_OBJECT_TAGGING = "ADMIN.PUT.OBJECT.TAGGING";
	private final String OP_ADMIN_PUT_OBJECT = "ADMIN.PUT.OBJECT";
	private final String OP_ADMIN_PUT_OBJECT_PART = "ADMIN.PUT.OBJECT.PART";
	private final String OP_ADMIN_PUT_OBJECT_PART_COPY = "ADMIN.PUT.OBJECT.PART.COPY";

	public S3RequestFactory() {
		logger = LoggerFactory.getLogger(S3RequestFactory.class);
	}

	public S3Request createS3Request(S3Parameter s3Parameter) throws GWException {
		switch (s3Parameter.getMethod()) {
			case GWConstants.METHOD_DELETE:
				if (GWConstants.CATEGORY_BUCKET.equals(s3Parameter.getPathCategory())) {
					if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_WEBSITE))) {
						s3Parameter.setOperation(OP_DELETE_WEBSITE);
						return new DeleteBucketWebsite(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_POLICY))) {
						s3Parameter.setOperation(OP_DELETE_POLICY);
						return new DeleteBucketPolicy(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_CORS))) {
						s3Parameter.setOperation(OP_DELETE_CORS);
						return new DeleteBucketCors(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_LIFECYCLE))) {
						s3Parameter.setOperation(OP_DELETE_LIFECYCLE);
						return new DeleteBucketLifeCycle(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_PUBLIC_ACCESS_BLOCK))) {
						s3Parameter.setOperation(OP_DELETE_PUBLICACCESSBLOCK);
						return new DeletePublicAccessBlock(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_TAGGING))) {
						s3Parameter.setOperation(OP_DELETE_BUCKET_TAGGING);
						return new DeleteBucketTagging(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_ENCRYPTION))) {
						s3Parameter.setOperation(OP_DELETE_ENCRYPTION);
						return new DeleteBucketEncryption(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_LOGGING))) {
						s3Parameter.setOperation(OP_DELETE_LOGGING);
						return new DeleteBucketLogging(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_OBJECT_LOCK))) {
						s3Parameter.setOperation(OP_DELETE_OBJECTLOCK);
						return new DeleteBucketObjectLock(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_REPLICATION))) {
						s3Parameter.setOperation(OP_DELETE_REPLICATION);
						return new DeleteBucketReplication(s3Parameter);
					}

					if (s3Parameter.isPublicAccess()) {
						throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
					}

					s3Parameter.setOperation(OP_DELETE_BUCKET);
					return new DeleteBucket(s3Parameter);
				}

				if (GWConstants.CATEGORY_OBJECT.equals(s3Parameter.getPathCategory())) {
					if (s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_UPLOAD_ID) != null) {
						if (s3Parameter.isAdmin()) {
							s3Parameter.setOperation(OP_ADMIN_DELETE_OBJECT_UPLOAD);
							return new KsanAbortMultipartUpload(s3Parameter);
						} else {
							s3Parameter.setOperation(OP_DELETE_OBJECT_UPLOAD);
							return new AbortMultipartUpload(s3Parameter);
						}
					}

					if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_TAGGING))) {
						if (s3Parameter.isAdmin()) {
							s3Parameter.setOperation(OP_ADMIN_DELETE_OBJECT_TAGGING);
							return new KsanDeleteObjectTagging(s3Parameter);
						} else {
							s3Parameter.setOperation(OP_DELETE_OBJECT_TAGGING);
							return new DeleteObjectTagging(s3Parameter);
						}
					} else {
						if (s3Parameter.isAdmin()) {
							s3Parameter.setOperation(OP_ADMIN_DELETE_OBJECT);
							return new KsanDeleteObject(s3Parameter);
						} else {
							s3Parameter.setOperation(OP_DELETE_OBJECT);
							return new DeleteObject(s3Parameter);
						}
					}
				}
				break;

			case GWConstants.METHOD_GET:
				if (GWConstants.CATEGORY_ROOT.equals(s3Parameter.getPathCategory())) {
					if (s3Parameter.isPublicAccess()) {
						throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
					}
					s3Parameter.setOperation(OP_GET_LISTBUCKET);
					return new ListBuckets(s3Parameter);
				}

				if (GWConstants.CATEGORY_BUCKET.equals(s3Parameter.getPathCategory())) {
					if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_WEBSITE))) {
						s3Parameter.setOperation(OP_GET_WEBSITE);
						return new GetBucketWebsite(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_POLICY))) {
						s3Parameter.setOperation(OP_GET_POLICY);
						return new GetBucketPolicy(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_CORS))) {
						s3Parameter.setOperation(OP_GET_CORS);
						return new GetBucketCors(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_LIFECYCLE))) {
						s3Parameter.setOperation(OP_GET_LIFECYCLE);
						return new GetBucketLifecycleConfiguration(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_PUBLIC_ACCESS_BLOCK))) {
						s3Parameter.setOperation(OP_GET_PUBLICACCESSBLOCK);
						return new GetPublicAccessBlock(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_TAGGING))) {
						s3Parameter.setOperation(OP_GET_BUCKET_TAGGING);
						return new GetBucketTagging(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_ENCRYPTION))) {
						s3Parameter.setOperation(OP_GET_ENCRYPTION);
						return new GetBucketEncryption(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_LOGGING))) {
						s3Parameter.setOperation(OP_GET_LOGGING);
						return new GetBucketLogging(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_ACL))) {
						s3Parameter.setOperation(OP_GET_BUCKET_ACL);
						return new GetBucketAcl(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_LOCATION))) {
						s3Parameter.setOperation(OP_GET_LOCATION);
						return new GetBucketLocation(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_UPLOADS))) {
						s3Parameter.setOperation(OP_GET_UPLOADS);
						return new ListMultipartUploads(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_VERSIONING))) {
						s3Parameter.setOperation(OP_GET_VERSIONING);
						return new GetBucketVersioning(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_OBJECT_LOCK))) {
						s3Parameter.setOperation(OP_GET_OBJECTLOCK);
						return new GetObjectLockConfiguration(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_REPLICATION))) {
						s3Parameter.setOperation(OP_GET_REPLICATION);
						return new GetBucketReplication(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_POLICY_STATUS))) {
						s3Parameter.setOperation(OP_GET_BUCKET_POLICY_STATUS);
						return new GetBucketPolicyStatus(s3Parameter);
					}

					if (s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_LIST_TYPE) != null) {
						s3Parameter.setOperation(OP_GET_LISTOBJECTSV2);
						return new ListObjectsV2(s3Parameter);
					} else if (s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_VERSIONS) != null) {
						s3Parameter.setOperation(OP_GET_LISTVERSIONS);
						return new ListObjectVersions(s3Parameter);
					} else {
						s3Parameter.setOperation(OP_GET_LISTOBJECTS);
						return new ListObjects(s3Parameter);
					}
				}

				if (GWConstants.CATEGORY_OBJECT.equals(s3Parameter.getPathCategory())) {
					if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_ACL))) {
						if (s3Parameter.isAdmin()) {
							s3Parameter.setOperation(OP_ADMIN_GET_OBJECT_ACL);
							return new KsanGetObjectAcl(s3Parameter);
						} else {
							s3Parameter.setOperation(OP_GET_OBJECT_ACL);
							return new GetObjectAcl(s3Parameter);
						}
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_RETENTION))) {
						s3Parameter.setOperation(OP_GET_OBJECT_RETENTION);
						return new GetObjectRetention(s3Parameter);
					} else if (s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_UPLOAD_ID) != null) {
						s3Parameter.setOperation(OP_GET_OBJECT_LISTPARTS);
						return new ListParts(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_TAGGING))) {
						if (s3Parameter.isAdmin()) {
							s3Parameter.setOperation(OP_ADMIN_GET_OBJECT_TAGGING);
							return new KsanGetObjectTagging(s3Parameter);
						} else {
							s3Parameter.setOperation(OP_GET_OBJECT_TAGGING);
							return new GetObjectTagging(s3Parameter);
						}
					}

					if (s3Parameter.isAdmin()) {
						s3Parameter.setOperation(OP_ADMIN_GET_OBJECT);
						return new KsanGetObject(s3Parameter);
					} else {
						s3Parameter.setOperation(OP_GET_OBJECT);
						return new GetObject(s3Parameter);
					}
				}
				break;

			case GWConstants.METHOD_HEAD:
				if (GWConstants.CATEGORY_BUCKET.equals(s3Parameter.getPathCategory())) {
					s3Parameter.setOperation(OP_HEAD_BUCKET);
					return new HeadBucket(s3Parameter);
				}

				if (GWConstants.CATEGORY_OBJECT.equals(s3Parameter.getPathCategory())) {
					s3Parameter.setOperation(OP_HEAD_OBJECT);
					return new HeadObject(s3Parameter);
				}
				break;

			case GWConstants.METHOD_POST:
				if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_DELETE))) {
					s3Parameter.setOperation(OP_POST_DELETEOBJECTS);
					return new DeleteObjects(s3Parameter);
				} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_UPLOADS))) {
					if (s3Parameter.isAdmin()) {
						s3Parameter.setOperation(OP_ADMIN_POST_UPLOAD);
						return new KsanCreateMultipartUpload(s3Parameter);
					} else {
						s3Parameter.setOperation(OP_POST_UPLOAD);
						return new CreateMultipartUpload(s3Parameter);
					}
				} else if (s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_UPLOAD_ID) != null
						&& s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_PART_NUMBER) == null) {
					if (s3Parameter.isAdmin()) {
						s3Parameter.setOperation(OP_ADMIN_POST_COMPLETE);
						return new KsanCompleteMultipartUpload(s3Parameter);
					} else {
						s3Parameter.setOperation(OP_POST_COMPLETE);
						return new CompleteMultipartUpload(s3Parameter);
					}
				}

				if (s3Parameter.getRequest().getHeader(HttpHeaders.CONTENT_TYPE) != null && 
					s3Parameter.getRequest().getHeader(HttpHeaders.CONTENT_TYPE).startsWith(GWConstants.CONTENT_TYPE_POST_OBJECT)) {
					s3Parameter.setOperation(OP_POST_OBJECT);
					return new PostObject(s3Parameter);
				}
				break;

			case GWConstants.METHOD_PUT:
				if (GWConstants.CATEGORY_BUCKET.equals(s3Parameter.getPathCategory())) {
					if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_WEBSITE))) {
						s3Parameter.setOperation(OP_PUT_WEBSITE);
						return new PutBucketWebsite(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_POLICY))) {
						s3Parameter.setOperation(OP_PUT_POLICY);
						return new PutBucketPolicy(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_CORS))) {
						s3Parameter.setOperation(OP_PUT_CORS);
						return new PutBucketCors(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_LIFECYCLE))) {
						s3Parameter.setOperation(OP_PUT_LIFECYCLE);
						return new PutBucketLifecycleConfiguration(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_PUBLIC_ACCESS_BLOCK))) {
						s3Parameter.setOperation(OP_PUT_PUBLICACCESSBLOCK);
						return new PutPublicAccessBlock(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_TAGGING))) {
						s3Parameter.setOperation(OP_PUT_BUCKET_TAGGING);
						return new PutBucketTagging(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_ENCRYPTION))) {
						s3Parameter.setOperation(OP_PUT_ENCRYPTION);
						return new PutBucketEncryption(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_LOGGING))) {
						s3Parameter.setOperation(OP_PUT_LOGGING);
						return new PutBucketLogging(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_ACL))) {
						s3Parameter.setOperation(OP_PUT_BUCKET_ACL);
						return new PutBucketAcl(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_VERSIONING))) {
						s3Parameter.setOperation(OP_PUT_VERSIONING);
						return new PutBucketVersioning(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_OBJECT_LOCK))) {
						s3Parameter.setOperation(OP_PUT_OBJECTLOCK);
						return new PutObjectLockConfiguration(s3Parameter);
					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_REPLICATION))) {
						s3Parameter.setOperation(OP_PUT_REPLICATION);
						return new PutBucketReplication(s3Parameter);
					}

					if (s3Parameter.isPublicAccess()) {
						throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
					}

					s3Parameter.setOperation(OP_PUT_BUCKET);
					return new CreateBucket(s3Parameter);
				}

				if (GWConstants.CATEGORY_OBJECT.equals(s3Parameter.getPathCategory())) {
					if (s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_UPLOAD_ID) != null) {
						if (s3Parameter.getRequest().getHeader(GWConstants.PARAMETER_COPY_SOURCE) != null) {
							s3Parameter.setOperation(OP_PUT_OBJECT_PART_COPY);
							return new UploadPartCopy(s3Parameter);
						} else {
							if (s3Parameter.isAdmin()) {
								s3Parameter.setOperation(OP_ADMIN_PUT_OBJECT_PART);
								return new KsanUploadPart(s3Parameter);
							} else {
								s3Parameter.setOperation(OP_PUT_OBJECT_PART);
								return new UploadPart(s3Parameter);
							}							
						}
					} else {
						if (s3Parameter.getRequest().getHeader(GWConstants.PARAMETER_COPY_SOURCE) != null) {
							s3Parameter.setOperation(OP_PUT_OBJECT_COPY);
							return new CopyObject(s3Parameter);
						} else {
							if (s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_RETENTION) != null) {
								s3Parameter.setOperation(OP_PUT_OBJECT_RETENTION);
								return new PutObjectRetention(s3Parameter);
							} else if (s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_TAGGING) != null) {
								if (s3Parameter.isAdmin()) {
									s3Parameter.setOperation(OP_ADMIN_PUT_OBJECT_TAGGING);
									return new KsanPutObjectTagging(s3Parameter);
								} else {
									s3Parameter.setOperation(OP_PUT_OBJECT_TAGGING);
									return new PutObjectTagging(s3Parameter);
								}
							} else if (s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_ACL) != null) {
								if (s3Parameter.isAdmin()) {
									s3Parameter.setOperation(OP_ADMIN_PUT_OBJECT_ACL);
									return new KsanPutObjectAcl(s3Parameter);
								} else {
									s3Parameter.setOperation(OP_PUT_OBJECT_ACL);
									return new PutObjectAcl(s3Parameter);
								}
							}

							if (s3Parameter.isAdmin()) {
								s3Parameter.setOperation(OP_ADMIN_PUT_OBJECT);
								return new KsanPutObject(s3Parameter);
							} else {
								s3Parameter.setOperation(OP_PUT_OBJECT);
								return new PutObject(s3Parameter);
							}
						}
					}
				}
				break;

			case GWConstants.METHOD_OPTIONS:
			s3Parameter.setOperation(OP_OPTIONS);
				return new OptionsObject(s3Parameter);

			default:
				break;
		}

		logger.error(GWConstants.UNDEFINED_METHOD, s3Parameter.getMethod());
		throw new GWException(GWErrorCode.NOT_IMPLEMENTED, s3Parameter);
	}
}
