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

import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

public class S3RequestFactory {
	private final Logger logger;

	public S3RequestFactory() {
		logger = LoggerFactory.getLogger(S3RequestFactory.class);
	}

	public S3Request createS3Request(S3Parameter s3Parameter) throws GWException {
		switch (s3Parameter.getMethod()) {
			case GWConstants.METHOD_DELETE:
				if (GWConstants.CATEGORY_BUCKET.equals(s3Parameter.getPathCategory())) {
					if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_WEBSITE))) {
						return new DeleteBucketWebsite(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_POLICY))) {
						return new DeleteBucketPolicy(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_CORS))) {
						return new DeleteBucketCors(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_LIFECYCLE))) {
						return new DeleteBucketLifeCycle(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_PUBLIC_ACCESS_BLOCK))) {
						return new DeletePublicAccessBlock(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_TAGGING))) {
						return new DeleteBucketTagging(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_ENCRYPTION))) {
						return new DeleteBucketEncryption(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_OBJECT_LOCK))) {
						return new DeleteBucketObjectLock(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_REPLICATION))) {
						return new DeleteBucketReplication(s3Parameter);
					}

					if (s3Parameter.isPublicAccess()) {
						throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
					}
					return new DeleteBucket(s3Parameter);
				}

				if (GWConstants.CATEGORY_OBJECT.equals(s3Parameter.getPathCategory())) {
					if (s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_UPLOAD_ID) != null) {
						return new AbortMultipartUpload(s3Parameter);
					}

					if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_TAGGING))) {
						return new DeleteObjectTagging(s3Parameter);
					} else {
						return new DeleteObject(s3Parameter);
					}
				}
				break;

			case GWConstants.METHOD_GET:
				if (GWConstants.CATEGORY_ROOT.equals(s3Parameter.getPathCategory())) {
					if (s3Parameter.isPublicAccess()) {
						throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
					}
					return new ListBuckets(s3Parameter);
				}

				if (GWConstants.CATEGORY_BUCKET.equals(s3Parameter.getPathCategory())) {
					if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_WEBSITE))) {
						return new GetBucketWebsite(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_POLICY))) {
						return new GetBucketPolicy(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_CORS))) {
						return new GetBucketCors(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_LIFECYCLE))) {
						return new GetBucketLifeCycle(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_PUBLIC_ACCESS_BLOCK))) {
						return new GetPublicAccessBlock(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_TAGGING))) {
						return new GetBucketTagging(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_ENCRYPTION))) {
						return new GetBucketEncryption(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_ACL))) {
						return new GetBucketAcl(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_LOCATION))) {
						return new GetBucketLocation(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_UPLOADS))) {
						return new ListMultipartUploads(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_VERSIONING))) {
						return new GetBucketVersioning(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_OBJECT_LOCK))) {
						return new GetBucketObjectLock(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_REPLICATION))) {
						return new GetBucketReplication(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_POLICY_STATUS))) {
						return new GetBucketPolicyStatus(s3Parameter);

					}

					if (s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_LIST_TYPE) != null) {
						return new ListObjectV2(s3Parameter);

					} else if (s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_VERSIONS) != null) {
						return new ListObjectVersions(s3Parameter);

					} else {
						return new ListObject(s3Parameter);
					}
				}

				if (GWConstants.CATEGORY_OBJECT.equals(s3Parameter.getPathCategory())) {
					if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_ACL))) {
						return new GetObjectAcl(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_RETENTION))) {
						return new GetObjectRetention(s3Parameter);

					} else if (s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_UPLOAD_ID) != null) {
						return new ListParts(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_TAGGING))) {
						return new GetObjectTagging(s3Parameter);

					}

					return new GetObject(s3Parameter);
				}
				break;

			case GWConstants.METHOD_HEAD:
				if (GWConstants.CATEGORY_BUCKET.equals(s3Parameter.getPathCategory())) {
					return new HeadBucket(s3Parameter);

				}

				if (GWConstants.CATEGORY_OBJECT.equals(s3Parameter.getPathCategory())) {
					return new HeadObject(s3Parameter);

				}
				break;

			case GWConstants.METHOD_POST:
				if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_DELETE))) {
					return new DeleteObjects(s3Parameter);

				} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_UPLOADS))) {
					return new CreateMultipartUpload(s3Parameter);

				} else if (s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_UPLOAD_ID) != null
						&& s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_PART_NUMBER) == null) {
					return new CompleteMultipartUpload(s3Parameter);
				}
				break;

			case GWConstants.METHOD_PUT:
				if (GWConstants.CATEGORY_BUCKET.equals(s3Parameter.getPathCategory())) {
					if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_WEBSITE))) {
						return new PutBucketWebsite(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_POLICY))) {
						return new PutBucketPolicy(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_CORS))) {
						return new PutBucketCors(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_LIFECYCLE))) {
						return new PutBucketLifeCycle(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_PUBLIC_ACCESS_BLOCK))) {
						return new PutPublicAccessBlock(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_TAGGING))) {
						return new PutBucketTagging(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_ENCRYPTION))) {
						return new PutBucketEncryption(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_ACL))) {
						return new PutBucketAcl(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_VERSIONING))) {
						return new PutBucketVersioning(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_OBJECT_LOCK))) {
						return new PutBucketObjectLock(s3Parameter);

					} else if (GWConstants.EMPTY_STRING.equals(s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_REPLICATION))) {
						return new PutBucketReplication(s3Parameter);

					}

					if (s3Parameter.isPublicAccess()) {
						throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
					}

					return new CreateBucket(s3Parameter);
				}

				if (GWConstants.CATEGORY_OBJECT.equals(s3Parameter.getPathCategory())) {
					if (s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_UPLOAD_ID) != null) {
						if (s3Parameter.getRequest().getHeader(GWConstants.PARAMETER_COPY_SOURCE) != null) {
							return new UploadPartCopy(s3Parameter);
						} else {
							return new UploadPart(s3Parameter);

						}
					} else {
						if (s3Parameter.getRequest().getHeader(GWConstants.PARAMETER_COPY_SOURCE) != null) {
							return new CopyObject(s3Parameter);

						} else {
							if (s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_RETENTION) != null) {
								return new PutObjectRetention(s3Parameter);

							} else if (s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_TAGGING) != null) {
								return new PutObjectTagging(s3Parameter);

							} else if (s3Parameter.getRequest().getParameter(GWConstants.PARAMETER_ACL) != null) {
								return new PutObjectAcl(s3Parameter);

							}

							return new PutObject(s3Parameter);
						}
					}
				}
				break;

			case GWConstants.METHOD_OPTIONS:
				return new OptionsObject(s3Parameter);

			default:
				break;
		}

		logger.error(GWConstants.UNDEFINED_METHOD, s3Parameter.getMethod());
		throw new GWException(GWErrorCode.NOT_IMPLEMENTED);
	}
}
