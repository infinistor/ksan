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
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.AccessControlList.Grant;
import com.pspace.ifs.ksan.libs.identity.ObjectListParameter;
import com.pspace.ifs.ksan.libs.identity.S3BucketSimpleInfo;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.libs.identity.S3ObjectList;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.object.objmanager.ObjManagerHelper;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.objmanager.Bucket;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjManager;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceAlreadyExistException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.objmanager.ObjMultipart;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.data.S3DataRequest;
import com.pspace.ifs.ksan.gw.format.Policy;
import com.pspace.ifs.ksan.gw.format.Policy.Statement;
import com.pspace.ifs.ksan.gw.condition.PolicyCondition;
import com.pspace.ifs.ksan.gw.condition.PolicyConditionFactory;

import org.slf4j.Logger;

public abstract class S3Request {
	protected S3Parameter s3Parameter;
	protected ObjManager objManager;
	protected Logger logger;
	protected Bucket srcBucket;
	protected Bucket dstBucket;
	protected AccessControlPolicy accessControlPolicy;
	protected AccessControlPolicy accessControlPolicyObject;
	protected static final HashFunction MD5 = Hashing.md5();

	public S3Request(S3Parameter s3Parameter) {
		this.s3Parameter = s3Parameter;
		srcBucket = null;
		dstBucket = null;
		accessControlPolicy = null;
	}
	
	public abstract void process() throws GWException;

	public void setObjManager() throws Exception {
		objManager = ObjManagerHelper.getInstance().getObjManager();
	}

	public void releaseObjManager() throws Exception {
		ObjManagerHelper.getInstance().returnObjManager(objManager);
	}

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
			setObjManager();
			result = objManager.isBucketExist(bucket);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
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
			setObjManager();
			dstBucket = objManager.getBucket(bucket);
			if (dstBucket != null) {
				if (!dstBucket.getAcl().startsWith(GWConstants.XML_VERSION)) {
					dstBucket.setAcl(GWUtils.makeOriginalXml(dstBucket.getAcl(), s3Parameter));
				}
			}
		} catch (ResourceNotFoundException e) {
			throw new GWException(GWErrorCode.NO_SUCH_BUCKET, s3Parameter);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
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
		s3Parameter.setBucket(s3Bucket);
	}

	protected Bucket getSomeBucket(String bucket) throws GWException {
		checkBucket(bucket);
		Bucket bucketInfo = null;
		try {
			setObjManager();
			bucketInfo = objManager.getBucket(bucket);
			if (bucketInfo != null) {
				if (!bucketInfo.getAcl().startsWith(GWConstants.XML_VERSION)) {
					bucketInfo.setAcl(GWUtils.makeOriginalXml(bucketInfo.getAcl(), s3Parameter));
				}
			}
		} catch (ResourceNotFoundException e) {
			throw new GWException(GWErrorCode.NO_SUCH_BUCKET, s3Parameter);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
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
		
		if (accessControlPolicy == null) {
			XmlMapper xmlMapper = new XmlMapper();
			try {
				accessControlPolicy = xmlMapper.readValue(dstBucket.getAcl(), AccessControlPolicy.class);
			} catch (JsonMappingException e) {
				logger.error(e.getMessage());
				new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
			} catch (JsonProcessingException e) {
				logger.error(e.getMessage());
				new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
			}
		}
		
		if (accessControlPolicy.owner != null) {
			if (accessControlPolicy.owner.id != null) {
				if (accessControlPolicy.owner.id.compareTo(id) == 0) {
					return true;
				}
			}
		}
		
		return false;
	}

	protected boolean isGrant(String id, String s3grant) throws GWException {		
		if (dstBucket == null) {
			return false;
		}
		
		XmlMapper xmlMapper = new XmlMapper();
		try {
			accessControlPolicy = xmlMapper.readValue(dstBucket.getAcl(), AccessControlPolicy.class);
		} catch (JsonMappingException e) {
			logger.error(e.getMessage());
			new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage());
			new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		logger.info(GWConstants.LOG_REQUEST_CHECK_ACL_ID_GRANT, id, s3grant);
		logger.info(GWConstants.LOG_REQUEST_BUCKET_ACL, dstBucket.getAcl());
		logger.info(GWConstants.LOG_REQUEST_BUCKET_OWNER_ID, accessControlPolicy.owner.id);
		if (accessControlPolicy.owner.id.compareTo(id) == 0) {
			return true;	// owner has full-grant
		}
		
		switch (s3grant) {
		case GWConstants.GRANT_READ:
			for (Grant grant : accessControlPolicy.aclList.grants) {
				if (grant.permission.compareTo(GWConstants.GRANT_FULL_CONTROL) == 0) {
					if (grant.grantee.type.compareTo(GWConstants.GROUP) == 0) {
						if (grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_ALL_USERS) == 0 
						|| grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS) == 0) {
						   return true;
					   }
					} else if (grant.grantee.type.compareTo(GWConstants.CANONICAL_USER) == 0) {
						if (grant.grantee.id.compareTo(id) == 0) {
							return true;
						}
					}
				} else if (grant.permission.compareTo(GWConstants.GRANT_READ) == 0) {
					if (grant.grantee.type.compareTo(GWConstants.GROUP) == 0) {
						if (grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_ALL_USERS) == 0 
						 || grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS) == 0) {
							return true;
						}
					} else if (grant.grantee.type.compareTo(GWConstants.CANONICAL_USER) == 0) {
						if (grant.grantee.id.compareTo(id) == 0) {
							return true;
						}
					}
				}
			}
			break;
			
		case GWConstants.GRANT_WRITE:
			for (Grant grant : accessControlPolicy.aclList.grants) {
				if (grant.permission.compareTo(GWConstants.GRANT_FULL_CONTROL) == 0) {
					if (grant.grantee.type.compareTo(GWConstants.GROUP) == 0) {
						if (grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_ALL_USERS) == 0 
						 || grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS) == 0) {
							return true;
						}
					} else if (grant.grantee.type.compareTo(GWConstants.CANONICAL_USER) == 0) {
						if (grant.grantee.id.compareTo(id) == 0) {
							return true;
						}
					}
				} else if (grant.permission.compareTo(GWConstants.GRANT_WRITE) == 0) {
					if (grant.grantee.type.compareTo(GWConstants.GROUP) == 0) {
						if (grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_ALL_USERS) == 0 
						 || grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS) == 0) {
							return true;
						}
					} else if (grant.grantee.type.compareTo(GWConstants.CANONICAL_USER) == 0) {
						if (grant.grantee.id.compareTo(id) == 0) {
							return true;
						}
					}
				}
			}
			break;
			
		case GWConstants.GRANT_READ_ACP:
			for (Grant grant : accessControlPolicy.aclList.grants) {
				if (grant.permission.compareTo(GWConstants.GRANT_FULL_CONTROL) == 0) {
					if (grant.grantee.type.compareTo(GWConstants.GROUP) == 0) {
						if (grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_ALL_USERS) == 0 
						 || grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS) == 0) {
							return true;
						}
					} else if (grant.grantee.type.compareTo(GWConstants.CANONICAL_USER) == 0) {
						if (grant.grantee.id.compareTo(id) == 0) {
							return true;
						}
					}
				} else if (grant.permission.compareTo(GWConstants.GRANT_READ_ACP) == 0) {
					if (grant.grantee.type.compareTo(GWConstants.GROUP) == 0) {
						if (grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_ALL_USERS) == 0 
						 || grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS) == 0) {
							return true;
						}
					} else if (grant.grantee.type.compareTo(GWConstants.CANONICAL_USER) == 0) {
						if (grant.grantee.id.compareTo(id) == 0) {
							return true;
						}
					}
				}
			}
			break;
			
		case GWConstants.GRANT_WRITE_ACP:
			for (Grant grant : accessControlPolicy.aclList.grants) {
				if (grant.permission.compareTo(GWConstants.GRANT_FULL_CONTROL) == 0) {
					if (grant.grantee.type.compareTo(GWConstants.GROUP) == 0) {
						if (grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_ALL_USERS) == 0 
						 || grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS) == 0) {
							return true;
						}
					} else if (grant.grantee.type.compareTo(GWConstants.CANONICAL_USER) == 0) {
						if (grant.grantee.id.compareTo(id) == 0) {
							return true;
						}
					}
				} else if (grant.permission.compareTo(GWConstants.GRANT_WRITE_ACP) == 0) {
					if (grant.grantee.type.compareTo(GWConstants.GROUP) == 0) {
						if (grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_ALL_USERS) == 0 
						 || grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS) == 0) {
							return true;
						}
					} else if (grant.grantee.type.compareTo(GWConstants.CANONICAL_USER) == 0) {
						if (grant.grantee.id.compareTo(id) == 0) {
							return true;
						}
					}
				}
			}
			break;
			
		case GWConstants.GRANT_FULL_CONTROL:
			for (Grant grant : accessControlPolicy.aclList.grants) {
				if (grant.grantee.type.compareTo(GWConstants.GROUP) == 0) {
					if (grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_ALL_USERS) == 0 
					 || grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS) == 0) {
						return true;
					}
				} else if (grant.grantee.type.compareTo(GWConstants.CANONICAL_USER) == 0) {
					if (grant.grantee.id.compareTo(id) == 0) {
						return true;
					}
				}
			}
			break;
			
		default:
			logger.error(GWConstants.LOG_REQUEST_GRANT_NOT_DEFINED, s3grant);
			new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}
		
		return false;
	}

	protected boolean checkGrant(String id, String s3grant, AccessControlPolicy acp) throws GWException {
		switch (s3grant) {
			case GWConstants.GRANT_READ:
				for (Grant grant : acp.aclList.grants) {
					if (grant.permission.compareTo(GWConstants.GRANT_FULL_CONTROL) == 0) {
						if (grant.grantee.type == null) {
							if (grant.grantee.id.compareTo(id) == 0) {
								return true;
							}
						} else {
							if (grant.grantee.type.compareTo(GWConstants.GROUP) == 0) {
								if (grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_ALL_USERS) == 0
										|| grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS) == 0) {
									return true;
								}
							} else if (grant.grantee.type.compareTo(GWConstants.CANONICAL_USER) == 0) {
								if (grant.grantee.id.compareTo(id) == 0) {
									return true;
								}
							}
						}
					} else if (grant.permission.compareTo(GWConstants.GRANT_READ) == 0) {
						if (grant.grantee.type == null) {
							if (grant.grantee.id.compareTo(id) == 0) {
								return true;
							}
						} else {
							if (grant.grantee.type.compareTo(GWConstants.GROUP) == 0) {
								if (grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_ALL_USERS) == 0
										|| grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS) == 0) {
									return true;
								}
							} else if (grant.grantee.type.compareTo(GWConstants.CANONICAL_USER) == 0) {
								if (grant.grantee.id.compareTo(id) == 0) {
									return true;
								}
							}
						}
					}
				}
				break;

			case GWConstants.GRANT_WRITE:
				for (Grant grant : acp.aclList.grants) {
					if (grant.permission.compareTo(GWConstants.GRANT_FULL_CONTROL) == 0) {
						if (grant.grantee.type == null) {
							if (grant.grantee.id.compareTo(id) == 0) {
								return true;
							}
						} else {
							if (grant.grantee.type.compareTo(GWConstants.GROUP) == 0) {
								if (grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_ALL_USERS) == 0
										|| grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS) == 0) {
									return true;
								}
							} else if (grant.grantee.type.compareTo(GWConstants.CANONICAL_USER) == 0) {
								if (grant.grantee.id.compareTo(id) == 0) {
									return true;
								}
							}
						}
					} else if (grant.permission.compareTo(GWConstants.GRANT_WRITE) == 0) {
						if (grant.grantee.type == null) {
							if (grant.grantee.id.compareTo(id) == 0) {
								return true;
							}
						} else {
							if (grant.grantee.type.compareTo(GWConstants.GROUP) == 0) {
								if (grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_ALL_USERS) == 0
										|| grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS) == 0) {
									return true;
								}
							} else if (grant.grantee.type.compareTo(GWConstants.CANONICAL_USER) == 0) {
								if (grant.grantee.id.compareTo(id) == 0) {
									return true;
								}
							}
						}
					}
				}
				break;

			case GWConstants.GRANT_READ_ACP:
				for (Grant grant : acp.aclList.grants) {
					if (grant.permission.compareTo(GWConstants.GRANT_FULL_CONTROL) == 0) {
						if (grant.grantee.type == null) {
							if (grant.grantee.id.compareTo(id) == 0) {
								return true;
							}
						} else {
							if (grant.grantee.type.compareTo(GWConstants.GROUP) == 0) {
								if (grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_ALL_USERS) == 0
										|| grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS) == 0) {
									return true;
								}
							} else if (grant.grantee.type.compareTo(GWConstants.CANONICAL_USER) == 0) {
								if (grant.grantee.id.compareTo(id) == 0) {
									return true;
								}
							}
						}
					} else if (grant.permission.compareTo(GWConstants.GRANT_READ_ACP) == 0) {
						if (grant.grantee.type == null) {
							if (grant.grantee.id.compareTo(id) == 0) {
								return true;
							}
						} else {
							if (grant.grantee.type.compareTo(GWConstants.GROUP) == 0) {
								if (grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_ALL_USERS) == 0
										|| grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS) == 0) {
									return true;
								}
							} else if (grant.grantee.type.compareTo(GWConstants.CANONICAL_USER) == 0) {
								if (grant.grantee.id.compareTo(id) == 0) {
									return true;
								}
							}
						}
					}
				}
				break;

			case GWConstants.GRANT_WRITE_ACP:
				for (Grant grant : acp.aclList.grants) {
					if (grant.permission.compareTo(GWConstants.GRANT_FULL_CONTROL) == 0) {
						if (grant.grantee.type == null) {
							if (grant.grantee.id.compareTo(id) == 0) {
								return true;
							}
						} else {
							if (grant.grantee.type.compareTo(GWConstants.GROUP) == 0) {
								if (grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_ALL_USERS) == 0
										|| grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS) == 0) {
									return true;
								}
							} else if (grant.grantee.type.compareTo(GWConstants.CANONICAL_USER) == 0) {
								if (grant.grantee.id.compareTo(id) == 0) {
									return true;
								}
							}
						}
					} else if (grant.permission.compareTo(GWConstants.GRANT_WRITE_ACP) == 0) {
						if (grant.grantee.type == null) {
							if (grant.grantee.id.compareTo(id) == 0) {
								return true;
							}
						} else {
							if (grant.grantee.type.compareTo(GWConstants.GROUP) == 0) {
								if (grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_ALL_USERS) == 0
										|| grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS) == 0) {
									return true;
								}
							} else if (grant.grantee.type.compareTo(GWConstants.CANONICAL_USER) == 0) {
								if (grant.grantee.id.compareTo(id) == 0) {
									return true;
								}
							}
						}
					}
				}
				break;

			case GWConstants.GRANT_FULL_CONTROL:
				for (Grant grant : acp.aclList.grants) {
					if (grant.grantee.type == null) {
						if (grant.grantee.id.compareTo(id) == 0) {
							return true;
						}
					} else {
						if (grant.grantee.type.compareTo(GWConstants.GROUP) == 0) {
							if (grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_ALL_USERS) == 0
									|| grant.grantee.uri.compareTo(GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS) == 0) {
								return true;
							}
						} else if (grant.grantee.type.compareTo(GWConstants.CANONICAL_USER) == 0) {
							if (grant.grantee.id.compareTo(id) == 0) {
								return true;
							}
						}
					}
				}
				break;

			default:
				logger.error(GWConstants.LOG_REQUEST_GRANT_NOT_DEFINED, s3grant);
				new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		return false;
	}

	protected boolean isGrantBucket(String id, String s3grant) throws GWException {
		if (getBucketInfo() == null) {
			return false;
		}

		if (accessControlPolicy == null) {
			XmlMapper xmlMapper = new XmlMapper();
			try {
				accessControlPolicy = xmlMapper.readValue(getBucketInfo().getAcl(), AccessControlPolicy.class);
			} catch (JsonMappingException e) {
				logger.error(e.getMessage());
				throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
			} catch (JsonProcessingException e) {
				logger.error(e.getMessage());
				throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
			}
		}

		logger.info(GWConstants.LOG_REQUEST_CHECK_ACL_ID_GRANT, id, s3grant);
		logger.info(GWConstants.LOG_REQUEST_BUCKET_ACL, getBucketInfo().getAcl());
		// logger.info(GWConstants.LOG_REQUEST_BUCKET_OWNER_ID, accessControlPolicy.owner.id);

		if (accessControlPolicy.aclList == null) {
			return false;
		}

		if (accessControlPolicy.aclList.grants == null) {
			return false;
		}

		return checkGrant(id, s3grant, accessControlPolicy);
	}

	protected boolean isGrantObjectOwner(Metadata meta, String id, String s3grant) throws GWException {
		if (meta == null) {
			return false;
		}

		if (accessControlPolicyObject == null) {
			XmlMapper xmlMapper = new XmlMapper();
			try {
				accessControlPolicyObject = xmlMapper.readValue(meta.getAcl(), AccessControlPolicy.class);
			} catch (JsonMappingException e) {
				logger.error(e.getMessage());
				throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
			} catch (JsonProcessingException e) {
				logger.error(e.getMessage());
				throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
			}
		}

		logger.info(GWConstants.LOG_REQUEST_CHECK_ACL_ID_GRANT, id, s3grant);
		logger.info(GWConstants.LOG_REQUEST_BUCKET_ACL, meta.getAcl());

		if (accessControlPolicyObject.owner != null) {
			if (accessControlPolicyObject.owner.id != null) {
				logger.info(GWConstants.LOG_REQUEST_BUCKET_OWNER_ID, accessControlPolicyObject.owner.id);
				if (accessControlPolicyObject.owner.id.compareTo(id) == 0) {
					return true; // owner has full-grant
				}
			}
		}

		if (accessControlPolicyObject.aclList == null) {
			return false;
		}

		if (accessControlPolicyObject.aclList.grants == null) {
			return false;
		}

		return checkGrant(id, s3grant, accessControlPolicyObject);
	}

	protected boolean isGrantObject(Metadata meta, String id, String s3grant) throws GWException {
		if (meta == null) {
			return false;
		}

		if (accessControlPolicyObject == null) {
			XmlMapper xmlMapper = new XmlMapper();
			try {
				accessControlPolicyObject = xmlMapper.readValue(meta.getAcl(), AccessControlPolicy.class);
			} catch (JsonMappingException e) {
				logger.error(e.getMessage());
				throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
			} catch (JsonProcessingException e) {
				logger.error(e.getMessage());
				throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
			}
		}

		logger.info(GWConstants.LOG_REQUEST_CHECK_ACL_ID_GRANT, id, s3grant);
		logger.info(GWConstants.LOG_REQUEST_BUCKET_ACL, meta.getAcl());
		// logger.info(GWConstants.LOG_REQUEST_BUCKET_OWNER_ID, accessControlPolicyObject.owner.id);

		if (accessControlPolicyObject.aclList == null) {
			return false;
		}

		if (accessControlPolicyObject.aclList.grants == null) {
			return false;
		}

		return checkGrant(id, s3grant, accessControlPolicyObject);
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
			setObjManager();
			versioningStatus = objManager.getBucketVersioning(bucket);
			
		} catch (ResourceNotFoundException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.NO_SUCH_BUCKET, s3Parameter);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		return versioningStatus;
	}

	protected Metadata getObjectWithVersionId(String bucket, String object, String versionId) throws GWException {
		Metadata objMeta = null;
		try {
			setObjManager();
			objMeta = objManager.getObjectWithVersionId(bucket, object, versionId);
		} catch (ResourceNotFoundException e) {
			logger.info(GWConstants.LOG_REQUEST_NOT_FOUND_IN_DB, bucket, object);
			throw new GWException(GWErrorCode.NO_SUCH_KEY, s3Parameter);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		return objMeta;
	}

	protected Metadata open(String bucket, String object) throws GWException {
		Metadata meta = null;
		try {
			setObjManager();
			meta = objManager.open(bucket, object);
			meta.setAcl(GWUtils.makeOriginalXml(meta.getAcl(), s3Parameter));
		} catch (ResourceNotFoundException e) {
			throw new GWException(GWErrorCode.NO_SUCH_KEY, s3Parameter);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		return meta;
	}
	
	protected Metadata open(String bucket, String objcet, String versionId) throws GWException {
		Metadata meta = null;
		try {
			setObjManager();
			meta = objManager.open(bucket, objcet, versionId);
			meta.setAcl(GWUtils.makeOriginalXml(meta.getAcl(), s3Parameter));
		} catch (ResourceNotFoundException e) {
			throw new GWException(GWErrorCode.NO_SUCH_KEY, s3Parameter);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		return meta;
	}

	protected Metadata create(String bucket, String object) throws GWException {
		Metadata meta = null;
		try {
			setObjManager();
			meta = objManager.create(bucket, object);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		return meta;
	}

	protected Metadata create(String bucket, String object, String versionId) throws GWException {
		Metadata meta = null;
		try {
			setObjManager();
			meta = objManager.create(bucket, object, versionId);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		return meta;
	}

	protected Metadata createLocal(String bucket, String object) throws GWException {
		Metadata meta = null;
		try {
			setObjManager();
			meta = objManager.createLocal(bucket, object, "null");
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		return meta;
	}

	protected Metadata createLocal(String bucket, String object, String versionId) throws GWException {
		Metadata meta = null;
		try {
			setObjManager();
			meta = objManager.createLocal(bucket, object, versionId);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		return meta;
	}

	protected Metadata createLocal(String diskpoolId, String bucket, String object, String versionId) throws GWException {
		Metadata meta = null;
		try {
			setObjManager();
			meta = objManager.createLocal(diskpoolId, bucket, object, versionId);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		return meta;
	}

	protected Metadata createCopy(String srcBucket, String srcObjectName, String srcVersionId, String bucket, String object) throws GWException {
		Metadata objMeta;
		try {
			setObjManager();
			objMeta = objManager.createCopy(srcBucket, srcObjectName, srcVersionId, bucket, object);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		return objMeta;
	}

	protected void remove(String bucket, String object) throws GWException {
		try {
			setObjManager();
			objManager.remove(bucket, object);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}
	}

	protected void remove(String bucket, String object, String versionId) throws GWException {
		try {
			setObjManager();
			objManager.remove(bucket, object, versionId);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}
	}

	protected int createBucket(String bucket, String userName, String userId, String acl, String encryption, String objectlock) throws GWException {
		int result = 0;
		try {
			setObjManager();
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
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		if (result != 0) {
			throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR, s3Parameter);
		}

		return  result;
	}

	protected int createBucket(Bucket bucket) throws GWException {
		int result = 0;
		try {
			setObjManager();
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
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		if (result != 0) {
			throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR, s3Parameter);
		}

		return  result;
	}

	protected void deleteBucket(String bucket) throws GWException {
		boolean result = false;
		try {
            setObjManager();
			result = objManager.isBucketDelete(bucket);
			if (result) {
				objManager.removeBucket(bucket);
			}
        } catch (Exception e) {
            PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        } finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		if (!result) {
			logger.info(GWConstants.LOG_REQUEST_BUCKET_IS_NOT_EMPTY);
            throw new GWException(GWErrorCode.BUCKET_NOT_EMPTY, s3Parameter);
		}
	}

	protected int insertObject(String bucket, String object, Metadata data) throws GWException {
		int result = 0;
		try {
			setObjManager();
			result = objManager.close(bucket, object, data);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		return result;
	}

	protected void updateObjectMeta(Metadata meta) throws GWException {
		try {
			setObjManager();
			objManager.updateObjectMeta(meta);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}
	}

	protected void updateBucketAcl(String bucket, String aclXml) throws GWException {
		try {
			setObjManager();
			objManager.updateBucketAcl(bucket, aclXml);
		} catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        } finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}
	}

	protected void updateBucketCors(String bucket, String cors) throws GWException {
		try {
			setObjManager();
            objManager.updateBucketCors(bucket, cors);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        } finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}
	}

	protected void updateBucketLifecycle(String bucket, String lifecycle) throws GWException {
		try {
			setObjManager();
            objManager.updateBucketLifecycle(bucket, lifecycle);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        } finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}
	}

	protected void updateBucketReplication(String bucket, String replica) throws GWException {
		try {
            setObjManager();
            objManager.updateBucketReplication(bucket, replica);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        } finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}
	}

	protected void updateBucketTagging(String bucket, String tagging) throws GWException {
		try {
			setObjManager();
            objManager.updateBucketTagging(bucket, tagging);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        } finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}
	}

	protected void updateBucketWeb(String bucket, String web) throws GWException {
		try {
			setObjManager();
            objManager.updateBucketWeb(bucket, web);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        } finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}
	}

	protected void updateBucketAccess(String bucket, String access) throws GWException {
		try {
			setObjManager();
            objManager.updateBucketAccess(bucket, access);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        } finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}
	}

	protected void updateBucketEncryption(String bucket, String encryption) throws GWException {
		try {
			setObjManager();
            objManager.updateBucketEncryption(bucket, encryption);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        } finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}
	}

	protected void updateBucketObjectLock(String bucket, String lock) throws GWException {
		try {
			setObjManager();
            objManager.updateBucketObjectLock(bucket, lock);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        } finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}
	}

	protected void updateBucketPolicy(String bucket, String policy) throws GWException {
		try {
			setObjManager();
            objManager.updateBucketPolicy(bucket, policy);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        } finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}
	}

	protected void updateBucketLogging(String bucket, String logging) throws GWException {
		try {
			setObjManager();
            objManager.updateBucketLogging(bucket, logging);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        } finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}
	}

	protected Bucket getBucket(String bucket) throws GWException {
		Bucket bucketInfo = null;
		try {
			setObjManager();
			bucketInfo = objManager.getBucket(bucket);
		} catch (ResourceNotFoundException e) {
			return null;
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		return bucketInfo;
	}

	protected void putBucketVersioning(String bucket, String status) throws GWException {
		try {
			setObjManager();
			objManager.putBucketVersioning(bucket, status);
		} catch (ResourceNotFoundException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.NO_SUCH_BUCKET, s3Parameter);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}
	}

	protected List<S3BucketSimpleInfo> listBucketSimpleInfo(String userName, String userId) throws GWException {
		List<S3BucketSimpleInfo> bucketList = null;
		try {
			setObjManager();
			bucketList = objManager.listBucketSimpleInfo(userName, userId);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}
		
		return bucketList;
	}

	protected ObjectListParameter listObject(String bucket, S3ObjectList s3ObjectList) throws GWException {
		ObjectListParameter objectListParameter = null;
		try {
			setObjManager();
			objectListParameter = objManager.listObject(bucket, s3ObjectList);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		return objectListParameter;
	}

	protected ObjectListParameter listObjectV2(String bucket, S3ObjectList s3ObjectList) throws GWException {
		ObjectListParameter objectListParameter = null;
		try {
			setObjManager();
			objectListParameter = objManager.listObjectV2(bucket, s3ObjectList);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		return objectListParameter;
	}

	protected ObjectListParameter listObjectVersions(String bucket, S3ObjectList s3ObjectList) throws GWException {
		ObjectListParameter objectListParameter = null;
		try {
			setObjManager();
			objectListParameter = objManager.listObjectVersions(bucket, s3ObjectList);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		return objectListParameter;
	}

	protected void updateObjectTagging(Metadata meta) throws GWException {
		try {
			setObjManager();
			objManager.updateObjectTagging(meta);
		} catch(Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}
	}

	protected void updateObjectAcl(Metadata meta) throws GWException {
		try {
			setObjManager();
			objManager.updateObjectAcl(meta);
		} catch(Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}
	}

	protected ObjMultipart getInstanceObjMultipart(String bucket) throws GWException {
		ObjMultipart objMultipart = null;
		try {
			setObjManager();
			objMultipart = objManager.getMultipartInsatance(bucket);
		}catch(Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		return objMultipart;
	}

	protected boolean isGrantBucketOwner(String id, String s3grant) throws GWException {
		if (dstBucket == null) {
			return false;
		}

		if (accessControlPolicy == null) {
			XmlMapper xmlMapper = new XmlMapper();
			try {
				accessControlPolicy = xmlMapper.readValue(dstBucket.getAcl(), AccessControlPolicy.class);
			} catch (JsonMappingException e) {
				logger.error(e.getMessage());
				new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
			} catch (JsonProcessingException e) {
				logger.error(e.getMessage());
				new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
			}
		}

		logger.info(GWConstants.LOG_REQUEST_CHECK_ACL_ID_GRANT, id, s3grant);
		logger.info(GWConstants.LOG_REQUEST_BUCKET_ACL, dstBucket.getAcl());

		if (accessControlPolicy.owner != null) {
			if (accessControlPolicy.owner.id != null) {
				logger.info(GWConstants.LOG_REQUEST_BUCKET_OWNER_ID, accessControlPolicy.owner.id);
				if (accessControlPolicy.owner.id.compareTo(id) == 0) {
					return true; // owner has full-grant
				}
			}
		}

		if (accessControlPolicy.aclList == null) {
			return false;
		}

		if (accessControlPolicy.aclList.grants == null) {
			return false;
		}

		return checkGrant(id, s3grant, accessControlPolicy);
	}

	protected void checkGrantBucketOwner(boolean pub, String uid, String grant) throws GWException {
		if (pub) {
			if (!isGrantBucketOwner(GWConstants.LOG_REQUEST_ROOT_ID, grant)) {
				logger.error(GWConstants.LOG_REQUEST_PUBLIC_ACCESS_DENIED);
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
		} else {
			if (!isGrantBucketOwner(uid, grant)) {
				logger.error(GWConstants.LOG_REQUEST_USER_ACCESS_DENIED, uid);
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
		}
	}

	protected void checkGrantBucket(boolean pub, String uid, String grant) throws GWException {
		if (pub) {
			if (!isGrantBucket(GWConstants.LOG_REQUEST_ROOT_ID, grant)) {
				logger.error(GWConstants.LOG_REQUEST_PUBLIC_ACCESS_DENIED);
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
		} else {
			if (!isGrantBucket(uid, grant)) {
				logger.error(GWConstants.LOG_REQUEST_USER_ACCESS_DENIED, uid);
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
		}
	}

	protected void checkGrantObjectOwner(boolean pub, Metadata meta, String uid, String grant) throws GWException {
		if (pub) {
			if (!isGrantObjectOwner(meta, GWConstants.LOG_REQUEST_ROOT_ID, grant)) {
				logger.error(GWConstants.LOG_REQUEST_PUBLIC_ACCESS_DENIED);
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
		} else {
			if (!isGrantObjectOwner(meta, uid, grant)) {
				logger.error(GWConstants.LOG_REQUEST_USER_ACCESS_DENIED, uid);
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
		}
	}

	protected void checkGrantObject(boolean pub, Metadata meta, String uid, String grant) throws GWException {
		if (pub) {
			if (!isGrantObject(meta, GWConstants.LOG_REQUEST_ROOT_ID, grant)) {
				logger.error(GWConstants.LOG_REQUEST_PUBLIC_ACCESS_DENIED);
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
		} else {
			if (!isGrantObject(meta, uid, grant)) {
				logger.error(GWConstants.LOG_REQUEST_USER_ACCESS_DENIED, uid);
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
		}
	}

	protected boolean checkPolicyBucket(String action, S3DataRequest dataRequest) throws GWException {
		boolean effect = false;
		String policyJson = s3Parameter.getBucket().getPolicy();
		if (Strings.isNullOrEmpty(policyJson)) {
			return effect;
		}

		Policy policy = null;
		// read policy
		ObjectMapper jsonMapper = new ObjectMapper();
		try {
			policy = jsonMapper.readValue(policyJson, Policy.class);

			if (policy == null) {
				return effect;
			}
		} catch (JsonMappingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		// check policy - loop statement
		for (Statement s : policy.statements) {

			// action check
			for (String a : s.actions) {
				boolean effectcheck = false;
				if (action.equals(a) || GWConstants.ACTION_ALL.equals(a)) {
					// check principal (id)
					for (String aws : s.principal.aws) {
						if (policyIdCheck(aws))
							break;
					}

					// check Resource (object path, bucket path)
					for (String resource : s.resources) {
						if (policyResourceCheck(resource))
							break;
					}

					if (conditioncheck(s3Parameter, s, dataRequest)) {
						effectcheck = true;
					}

					if (s.effect.equals(GWConstants.ALLOW)) {
						if (!effectcheck) {
							throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
						} else {
							effect = true;
						}
					} else if (s.effect.equals(GWConstants.DENY) && effectcheck) {
						throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
					}
				}
			}
		}

		return effect;
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

			if (likematch(path[1], s3Parameter.getObjectName())) {
				effectcheck = true;
				return effectcheck;
			}
		}

		return effectcheck;
	}

	protected boolean conditioncheck(S3Parameter s3Parameter, Statement s, S3DataRequest dataRequest) throws GWException {
		// condition check
		boolean conditioncheck = true;
		if (s.condition == null)
			return conditioncheck;

		for (Map.Entry<String, JsonNode> entry : s.condition.getUserExtensions().entries()) {
			PolicyCondition pc = PolicyConditionFactory.createPolicyCondition(entry.getKey(), entry.getValue());
			if (pc == null) {
				continue;
			}

			pc.process();

			String comp = null;
			switch (pc.getKey()) {
				case GWConstants.KEY_AUTH_TYPE:
					break;
				case GWConstants.KEY_DELIMITER:
					comp = dataRequest.getPolicyDelimter();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_MAXKYES:
					comp = dataRequest.getPolicyMaxkeys();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_PREFIX:
					comp = dataRequest.getPolicyPrefix();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_CONTENT_SHA256:
					comp = s3Parameter.getRequest().getHeader(GWConstants.X_AMZ_CONTENT_SHA256);
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_ACL:
					comp = dataRequest.getXAmzAcl();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_COPY_SOURCE:
					comp = dataRequest.getXAmzCopySource();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_GRANT_FULL_CONTROL:
					comp = dataRequest.getXAmzGrantFullControl();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_XAMZ_GRANT_READ:
					comp = dataRequest.getXAmzGrantRead();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_GRANT_READ_ACP:
					comp = dataRequest.getXAmzGrantReadAcp();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_GRANT_WRITE:
					comp = dataRequest.getXAmzGrantWrite();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_GRANT_WRITE_ACP:
					comp = dataRequest.getXAmzGrantWriteAcp();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_METADATA_DIRECTIVE:
					comp = dataRequest.getXAmzMetadataDirective();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_SERVER_SIDE_ENCRYPTION:
					comp = dataRequest.getXAmzServerSideEncryption();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_SERVER_SIDE_ENCRYPTION_AWS_KMS_KEY_ID:
					comp = dataRequest.getXAmzServerSideEncryptionAwsKmsKeyId();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_STORAGE_CLASS:
					comp = dataRequest.getXAmzStorageClass();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_WEBSITE_REDIRECT_LOCATION:
					comp = dataRequest.getXAmzWebsiteRedirectLocation();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_OBJECT_LOCK_MODE:
					comp = dataRequest.getXAmzObjectLockMode();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_OBJECT_LOCK_RETAIN_UNTIL_DATE:
					comp = dataRequest.getXAmzObjectLockRetainUntilDate();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_OBJECT_LOCK_REMAINING_RETENTION_DAYS:
					comp = dataRequest.getXAmzObjectLockRemainingRetentionDays();
					conditioncheck = pc.compare(comp);
					break;
				case GWConstants.KEY_X_AMZ_OBJECT_LOCK_LEGAL_HOLD:
					comp = dataRequest.getXAmzObjectLockLegalHold();
					conditioncheck = pc.compare(comp);
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

	public boolean likematch(String first, String second) {
		// If we reach at the end of both strings,
		// we are done
		if (first.length() == 0 && second.length() == 0)
			return true;

		// Make sure that the characters after '*'
		// are present in second string.
		// This function assumes that the first
		// string will not contain two consecutive '*'
		if (first.length() > 1 && first.charAt(0) == GWConstants.CHAR_ASTERISK && second.length() == 0)
			return false;

		// If the first string contains '?',
		// or current characters of both strings match
		if ((first.length() > 1 && first.charAt(0) == GWConstants.CHAR_QUESTION)
				|| (first.length() != 0 && second.length() != 0 && first.charAt(0) == second.charAt(0)))
			return likematch(first.substring(1), second.substring(1));

		// If there is *, then there are two possibilities
		// a) We consider current character of second string
		// b) We ignore current character of second string.
		if (first.length() > 0 && first.charAt(0) == GWConstants.CHAR_ASTERISK)
			return likematch(first.substring(1), second) || likematch(first, second.substring(1));
		return false;
	}

	public void retentionCheck(String meta, String bypassGovernanceRetention, S3Parameter s3Parameter) throws GWException {
		S3Metadata retentionMetadata = null;
		if (!Strings.isNullOrEmpty(meta)) {
			try {
				retentionMetadata = new ObjectMapper().readValue(meta, S3Metadata.class);
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
			} catch (JsonProcessingException e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			} 
		}
	}
}
