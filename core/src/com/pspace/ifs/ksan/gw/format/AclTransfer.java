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
package com.pspace.ifs.ksan.gw.format;

import java.util.Collection;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.sql.rowset.spi.XmlReader;

import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AclTransfer {
    private static final Logger logger = LoggerFactory.getLogger(AclTransfer.class);

    public static AclTransfer getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static AclTransfer INSTANCE = new AclTransfer();
    }

    private AclTransfer() {}

    public AccessControlPolicy getAclClassFromJson(String json) {
		if (Strings.isNullOrEmpty(json)) {
			return null;
		}

        AccessControlPolicy accessControlPolicy = new AccessControlPolicy();
        accessControlPolicy.owner = new AccessControlPolicy.Owner();
        AccessControlPolicy.AccessControlList.Grant grant = null;
        AccessControlPolicy.AccessControlList.Grant.Grantee grantee = null;

        StringTokenizer stk = new StringTokenizer(json, ":,{}[]\"");
        boolean isProcessed = true;
        String token = null;
        while(stk.hasMoreTokens()) {
            if (isProcessed) {
                token = stk.nextToken();
            } else {
                isProcessed = true;
            }
            logger.debug("token : {}", token);
            switch (token) {
            case GWConstants.JSON_AB_OWNER:
                if (stk.nextToken().equals(GWConstants.ID)) {
                    accessControlPolicy.owner.id = stk.nextToken();
                }
                if (stk.nextToken().equals(GWConstants.JSON_AB_DISPLAY_NAME)) {
                    accessControlPolicy.owner.displayName = stk.nextToken();
                }
                break;
            case GWConstants.JSON_AB_ACL:
                accessControlPolicy.aclList = new AccessControlPolicy.AccessControlList();
                accessControlPolicy.aclList.grants = new ArrayList<AccessControlPolicy.AccessControlList.Grant>();
                break;
            case GWConstants.JSON_AB_GRANT:
                break;
            case GWConstants.JSON_AB_GRANTEE:
                grant = new AccessControlPolicy.AccessControlList.Grant();
                grantee = new AccessControlPolicy.AccessControlList.Grant.Grantee();
                break;
            case GWConstants.JSON_AB_GRANTEE_TYPE:
                String type = stk.nextToken();
                if (type.equals(GWConstants.GRANT_AB_CANONICAL_USER)) {
                    grantee.type = GWConstants.CANONICAL_USER;
                    if (stk.nextToken().equals(GWConstants.ID)) {
                        grantee.id = stk.nextToken();
                    }
                    // display name may not exist.
                    String ddn = stk.nextToken();
                    if (ddn.equals(GWConstants.JSON_AB_GRANTEE_DISPLAY_NAME)) {
                        grantee.displayName = stk.nextToken();
                    } else if (ddn.equals(GWConstants.JSON_AB_GRANTEE_PERM)) {
                        token = ddn;
                        isProcessed = false;
                    }
                } else if (type.equals(GWConstants.GRANT_AB_GROUP)) {
                    grantee.type = GWConstants.GROUP;
                    if (stk.nextToken().equals(GWConstants.URI)) {
                        String uriType = stk.nextToken();
                        if (uriType.equals(GWConstants.GRANT_AB_ALL_USER)) {
                            grantee.uri = GWConstants.AWS_GRANT_URI_ALL_USERS;
                        } else if (uriType.equals(GWConstants.GRANT_AB_AUTH_USER)) {
                            grantee.uri = GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS;
                        }
                    } else {
                        logger.error("uri parsing error.");
                    }
                }
                grant.grantee = grantee;
                break;
            case GWConstants.JSON_AB_GRANTEE_PERM:
                String perm = stk.nextToken();
                if (perm.equals(GWConstants.GRANT_AB_FULLCONTROL)) {
                    grant.permission = GWConstants.GRANT_FULL_CONTROL;
                } else if (perm.equals(GWConstants.GRANT_AB_WRITE)) {
                    grant.permission = GWConstants.GRANT_WRITE;
                } else if (perm.equals(GWConstants.GRANT_AB_READ)) {
                    grant.permission = GWConstants.GRANT_READ;
                } else if (perm.equals(GWConstants.GRANT_AB_READ_ACP)) {
                    grant.permission = GWConstants.GRANT_READ_ACP;
                } else if (perm.equals(GWConstants.GRANT_AB_WRITE_ACP)) {
                    grant.permission = GWConstants.GRANT_WRITE_ACP;
                }
                accessControlPolicy.aclList.grants.add(grant);
                break;
            default:
                break;
            }
        }

        return accessControlPolicy;
    }

    public AccessControlPolicy getAclClassFromXml(String xml, S3Parameter s3Parameter) throws GWException {
        AccessControlPolicy accessControlPolicy = new AccessControlPolicy();
        accessControlPolicy.owner = new AccessControlPolicy.Owner();
        AccessControlPolicy.AccessControlList.Grant grant = null;
        AccessControlPolicy.AccessControlList.Grant.Grantee grantee = null;

        int indexStart = xml.indexOf(GWConstants.ACL_XML_OWNER_START);
        if (indexStart < 0) {
            logger.info("can not find {}", GWConstants.ACL_XML_OWNER_START);
            throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
        }
        indexStart += GWConstants.ACL_XML_OWNER_START.length();
        int indexEnd = xml.indexOf(GWConstants.ACL_XML_OWNER_END, indexStart);
        if (indexEnd < 0) {
            logger.info("can not find {}", GWConstants.ACL_XML_OWNER_END);
            throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
        }

        String owner = xml.substring(indexStart, indexEnd);
        logger.debug("owner : {}", owner);
        // Owner id
        indexStart = owner.indexOf(GWConstants.ACL_XML_OWNER_ID_START);
        if (indexStart < 0) {
            logger.info("can not find {}", GWConstants.ACL_XML_OWNER_ID_START);
            throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
        }
        indexStart += GWConstants.ACL_XML_OWNER_ID_START.length();
        indexEnd = owner.indexOf(GWConstants.ACL_XML_OWNER_ID_END, indexStart);
        if (indexEnd < 0) {
            logger.info("can not find {}", GWConstants.ACL_XML_OWNER_ID_END);
            throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
        }
        String ownerId = owner.substring(indexStart, indexEnd);
        logger.debug("owner id : {}", ownerId);
        accessControlPolicy.owner.id = ownerId;
        // Owner display name
        indexStart = indexEnd + GWConstants.ACL_XML_OWNER_ID_END.length();
        indexStart = owner.indexOf(GWConstants.ACL_XML_OWNER_DISPLAYNAME_START, indexStart);
        if (indexStart < 0) {
            logger.info("can not find {}", GWConstants.ACL_XML_OWNER_DISPLAYNAME_START);
            throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
        }
        indexStart += GWConstants.ACL_XML_OWNER_DISPLAYNAME_START.length();
        indexEnd = owner.indexOf(GWConstants.ACL_XML_OWNER_DISPLAYNAME_END, indexStart);
        if (indexEnd < 0) {
            logger.info("can not find {}", GWConstants.ACL_XML_OWNER_DISPLAYNAME_END);
            throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
        }
        String ownerDisplayName = owner.substring(indexStart, indexEnd);
        logger.debug("owner display name : {}", ownerDisplayName);
        accessControlPolicy.owner.displayName = ownerDisplayName;

        // Access Control List
        indexStart = xml.indexOf(GWConstants.ACL_XML_ACCESS_CONTROL_LIST_START, indexStart);
        if (indexStart < 0) {
            logger.info("can not find {}", GWConstants.ACL_XML_ACCESS_CONTROL_LIST_START);
            throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
        }
        indexStart += GWConstants.ACL_XML_ACCESS_CONTROL_LIST_START.length();
        indexEnd = xml.indexOf(GWConstants.ACL_XML_ACCESS_CONTROL_LIST_END, indexStart);
        if (indexEnd < 0) {
            logger.info("can not find {}", GWConstants.ACL_XML_ACCESS_CONTROL_LIST_END);
            throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
        }
        accessControlPolicy.aclList = new AccessControlPolicy.AccessControlList();
        accessControlPolicy.aclList.grants = new ArrayList<AccessControlPolicy.AccessControlList.Grant>();
        String acl = xml.substring(indexStart, indexEnd);
        logger.debug("acl : {}", acl);

        // Grant
        indexEnd = 0;
        int grantIndexStart = 0;
        int grantIndexEnd = 0;
        while ((indexStart = acl.indexOf(GWConstants.ACL_XML_GRANT_START, indexEnd)) != -1) {
            indexStart += GWConstants.ACL_XML_GRANT_START.length();
            indexEnd = acl.indexOf(GWConstants.ACL_XML_GRANT_END, indexStart);
            if (indexEnd < 0) {
                logger.info("can not find {}", GWConstants.ACL_XML_GRANT_END);
                throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
            }
            String aclGrant = acl.substring(indexStart, indexEnd);
            indexEnd += GWConstants.ACL_XML_GRANT_END.length();

            logger.debug("grant : {}", aclGrant);
            grant = new AccessControlPolicy.AccessControlList.Grant();
            grantee = new AccessControlPolicy.AccessControlList.Grant.Grantee();
            grantIndexStart = aclGrant.indexOf(GWConstants.CANONICAL_USER);
            if (grantIndexStart < 0) {
                grantIndexStart = aclGrant.indexOf(GWConstants.GROUP);
                if (grantIndexStart < 0) {
                    logger.info("can not find {}, {}", GWConstants.CANONICAL_USER, GWConstants.GROUP);
                    throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
                }
                grantee.type = GWConstants.GROUP;
                grantIndexStart += GWConstants.GROUP.length();
                grantIndexStart = aclGrant.indexOf(GWConstants.ACL_XML_GRANTEE_URI_START, grantIndexStart);
                if (grantIndexStart < 0) {
                    logger.info("can not find {}", GWConstants.ACL_XML_GRANTEE_URI_START);
                    throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
                }
                grantIndexStart += GWConstants.ACL_XML_GRANTEE_URI_START.length();
                grantIndexEnd = aclGrant.indexOf(GWConstants.ACL_XML_GRANTEE_URI_END, grantIndexEnd);
                if (grantIndexEnd < 0) {
                    logger.info("can not find {}", GWConstants.ACL_XML_GRANTEE_URI_END);
                    throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
                }
                String uri = aclGrant.substring(grantIndexStart, grantIndexEnd);
                logger.debug("uri : {}", uri);
                grantee.uri = uri;
            } else {
                grantee.type = GWConstants.CANONICAL_USER;
                grantIndexStart += GWConstants.CANONICAL_USER.length();
                grantIndexStart = aclGrant.indexOf(GWConstants.ACL_XML_GRANTEE_ID_START, grantIndexStart);
                if (grantIndexStart < 0) {
                    logger.info("can not find {}", GWConstants.ACL_XML_GRANTEE_ID_START);
                    throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
                }
                grantIndexStart += GWConstants.ACL_XML_GRANTEE_ID_START.length();
                grantIndexEnd = aclGrant.indexOf(GWConstants.ACL_XML_GRANTEE_ID_END, grantIndexStart);
                if (grantIndexEnd < 0) {
                    logger.info("can not find {}", GWConstants.ACL_XML_GRANTEE_ID_END);
                    throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
                }
                String id = aclGrant.substring(grantIndexStart, grantIndexEnd);
                logger.debug("id : {}", id);
                grantee.id = id;
                grantIndexEnd += GWConstants.ACL_XML_GRANTEE_ID_END.length();
                grantIndexStart = aclGrant.indexOf(GWConstants.ACL_XML_GRANTEE_DISPLAYNAME_START, grantIndexEnd);
                if (grantIndexStart > 0) {
                    grantIndexStart += GWConstants.ACL_XML_GRANTEE_DISPLAYNAME_START.length();
                    grantIndexEnd = aclGrant.indexOf(GWConstants.ACL_XML_GRANTEE_ID_END, grantIndexStart);
                    if (grantIndexEnd < 0) {
                        logger.info("can not find {}", GWConstants.ACL_XML_GRANTEE_ID_END);
                        throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
                    }
                    String displayName = aclGrant.substring(grantIndexStart, grantIndexEnd);
                    logger.debug("display name : {}", displayName);
                    grantee.displayName = displayName;
                }
            }
            grant.grantee = grantee;
            grantIndexStart = aclGrant.indexOf(GWConstants.ACL_XML_GRANT_PERMISSION_START, grantIndexStart);
            if (grantIndexStart < 0) {
                logger.info("can not find {}", GWConstants.ACL_XML_GRANT_PERMISSION_START);
                throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
            }
            grantIndexStart += GWConstants.ACL_XML_GRANT_PERMISSION_START.length();
            grantIndexEnd = aclGrant.indexOf(GWConstants.ACL_XML_GRANT_PERMISSION_END, grantIndexStart);
            if (grantIndexEnd < 0) {
                logger.info("can not find {}", GWConstants.ACL_XML_GRANT_PERMISSION_END);
                throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
            }
            String permission = aclGrant.substring(grantIndexStart, grantIndexEnd);
            logger.debug("permission : {}", permission);
            grant.permission = permission;
            accessControlPolicy.aclList.grants.add(grant);
        }
        
        return accessControlPolicy;
    }

    public String getAclJson(AccessControlPolicy accessControlPolicy) {
        return accessControlPolicy.toString();
    }

    public String getAclXml(AccessControlPolicy accessControlPolicy) {
        return accessControlPolicy.toXml();
    }
}
