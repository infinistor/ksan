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

import java.util.ArrayList;

import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

public class AccessControlPolicy {
    public Owner owner;
    public AccessControlList aclList;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(GWConstants.CHAR_LEFT_BRACE);
        sb.append(GWConstants.ACCESS_OW);
        sb.append(owner.toString());
        sb.append(GWConstants.ACCESS_ACS);
        sb.append(aclList.toString());
        return sb.append(GWConstants.CHAR_RIGHT_BRACE).toString();
    }

    public String toXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><AccessControlPolicy xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">");
        sb.append(owner.toXml());
        sb.append(aclList.toXml());
        return sb.append("</AccessControlPolicy>").toString();
    }

    public static final class Owner {
		public String id;
        public String displayName;

        @Override
        public String toString() {
        	StringBuilder sb = new StringBuilder();
            sb.append(GWConstants.CHAR_LEFT_BRACE);
            if (!Strings.isNullOrEmpty(id)) {
                sb.append(GWConstants.ACCESS_ID).append(GWConstants.DOUBLE_QUOTE + id + GWConstants.DOUBLE_QUOTE);
            }
    
            if (!Strings.isNullOrEmpty(displayName)) {
                sb.append(GWConstants.ACCESS_DN).append(GWConstants.DOUBLE_QUOTE).append(displayName).append(GWConstants.DOUBLE_QUOTE);
            }

        	return sb.append(GWConstants.CHAR_RIGHT_BRACE).toString();
        }

        public String toXml() {
            StringBuilder sb = new StringBuilder();
            sb.append("<Owner>");
            if (!Strings.isNullOrEmpty(id)) {
                sb.append("<ID>");
                sb.append(id);
                sb.append("</ID>");
            }

            if (!Strings.isNullOrEmpty(displayName)) {
                sb.append("<DisplayName>");
                sb.append(displayName);
                sb.append("</DisplayName>");
            }
            sb.append("</Owner>");
            return sb.toString();
        }
    }

    public static final class AccessControlList {
		public ArrayList<Grant> grants;

        @Override
        public String toString() {
        	StringBuilder sb = new StringBuilder();
            sb.append(GWConstants.CHAR_LEFT_BRACE);

            if (grants != null) {
                sb.append(GWConstants.ACCESS_GT).append(grants);
            }
        	
        	return sb.append(GWConstants.CHAR_RIGHT_BRACE).toString();
        }

        public String toXml() {
            StringBuilder sb = new StringBuilder();
            sb.append("<AccessControlList>");
            if (grants != null) {
                for(Grant grant: grants) {
                    sb.append(grant.toXml());
                }
            }
            sb.append("</AccessControlList>");
            return sb.toString();
        }

        public static final class Grant {
			public Grantee grantee;
			public String permission;

            @Override
            public String toString() {
            	StringBuilder sb = new StringBuilder();
                sb.append(GWConstants.CHAR_LEFT_BRACE);

                if(grantee != null) {
                    sb.append(GWConstants.ACCESS_GTE).append(grantee);
                }

                if(!Strings.isNullOrEmpty(permission)) {
                    String tempPermission = "";

                    if(permission.equals(GWConstants.GRANT_FULL_CONTROL)) {
                        tempPermission = GWConstants.GRANT_AB_FULLCONTROL;
                    } else if (permission.equals(GWConstants.GRANT_WRITE)) {
                        tempPermission = GWConstants.GRANT_AB_WRITE;
                    } else if (permission.equals(GWConstants.GRANT_READ)) {
                        tempPermission = GWConstants.GRANT_AB_READ;
                    } else if (permission.equals(GWConstants.GRANT_WRITE_ACP)) {
                        tempPermission = GWConstants.GRANT_AB_WRITE_ACP;
                    } else if (permission.equals(GWConstants.GRANT_READ_ACP)) {
                        tempPermission = GWConstants.GRANT_AB_READ_ACP;
                    }

                    sb.append(GWConstants.ACCESS_PERM).append(GWConstants.DOUBLE_QUOTE).append(tempPermission).append(GWConstants.DOUBLE_QUOTE);
                }

            	return sb.append(GWConstants.CHAR_RIGHT_BRACE).toString();
            }

            public String toXml() {
                StringBuilder sb = new StringBuilder();
                sb.append("<Grant>");
                sb.append(grantee.toXml());
                sb.append("<Permission>");
                sb.append(permission);
                sb.append("</Permission>");
                sb.append("</Grant>");
                return sb.toString();
            }

            public static final class Grantee {
                public String type;
				public String id;
                public String displayName;
				public String uri;

                @Override
                public String toString() {
                	StringBuilder sb = new StringBuilder();
                    sb.append(GWConstants.CHAR_LEFT_BRACE);

                    if(!Strings.isNullOrEmpty(type)) {
                        String tempType = "";
                        if(type.equals(GWConstants.CANONICAL_USER)) {
                            tempType = GWConstants.GRANT_AB_CANONICAL_USER;
                        } else if (type.equals(GWConstants.GROUP)) {
                            tempType = GWConstants.GRANT_AB_GROUP;
                        }

                        sb.append(GWConstants.ACCESS_TYPE).append(GWConstants.DOUBLE_QUOTE).append(tempType).append(GWConstants.DOUBLE_QUOTE);
                    }
    
                    if(!Strings.isNullOrEmpty(id)) {
                        sb.append(GWConstants.ACCESS_COMMA_ID).append(GWConstants.DOUBLE_QUOTE).append(id).append(GWConstants.DOUBLE_QUOTE);
                    }

                    if(!Strings.isNullOrEmpty(displayName)) {
                        sb.append(GWConstants.ACCESS_DDN).append(GWConstants.DOUBLE_QUOTE).append(displayName).append(GWConstants.DOUBLE_QUOTE);
                    }

                	if(!Strings.isNullOrEmpty(uri)) {
                        String tempUri = "";
                        
                        if(uri.equals(GWConstants.AWS_GRANT_URI_ALL_USERS)) {
                            tempUri = GWConstants.GRANT_AB_ALL_USER;
                        } else if(uri.equals(GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS)) {
                            tempUri = GWConstants.GRANT_AB_AUTH_USER;
                        }

                        sb.append(GWConstants.ACCESS_URI).append(GWConstants.DOUBLE_QUOTE).append(tempUri).append(GWConstants.DOUBLE_QUOTE);
                    }
                	
                	return sb.append(GWConstants.CHAR_RIGHT_BRACE).toString();
                }

                public String toXml() {
                    StringBuilder sb = new StringBuilder();
                    if (!Strings.isNullOrEmpty(type)) {
                        if(type.equals(GWConstants.CANONICAL_USER)) {
                            sb.append("<Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\">");
                            sb.append("<ID>");
                            sb.append(id);
                            sb.append("</ID>");
                            if (!Strings.isNullOrEmpty(displayName)) {
                                sb.append("<DisplayName>");
                                sb.append(displayName);
                                sb.append("</DisplayName>");
                            }
                            sb.append("</Grantee>");
                        } else if (type.equals(GWConstants.GROUP)) {
                            sb.append("<Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"Group\">");
                            if (uri.equals(GWConstants.AWS_GRANT_URI_ALL_USERS)) {
                                sb.append("<URI>http://acs.amazonaws.com/groups/global/AllUsers</URI>");
                            } else if (uri.equals(GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS)) {
                                sb.append("<URI>http://acs.amazonaws.com/groups/global/AuthenticatedUsers</URI>");
                            }
                            sb.append("</Grantee>");
                        }
                    }
                    
                    return sb.toString();
                }
            }
        }
    }
}
