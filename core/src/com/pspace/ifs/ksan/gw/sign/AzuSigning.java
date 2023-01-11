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
package com.pspace.ifs.ksan.gw.sign;

import org.apache.commons.codec.binary.Base64;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pspace.ifs.ksan.gw.identity.S3User;
import com.pspace.ifs.ksan.gw.data.azure.AzuRequestData;
import com.pspace.ifs.ksan.gw.identity.AzuParameter;
import com.pspace.ifs.ksan.gw.utils.AzuConstants;
import com.pspace.ifs.ksan.gw.utils.S3UserManager;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;

public class AzuSigning {
    private AzuParameter parameter;
    private String stringToSign;
    private String contentEncoding;
    private String contentLanguage;
    private String contentLength;
    private String contentMD5;
    private String contentType;
    private String date;
    private String ifModifiedSince;
    private String ifMatch;
    private String ifNoneMatch;
    private String ifUnmodifiedSince;
    private String range;
    SortedMap<String, String> canonicalizedHeaderList = new TreeMap<String, String>();
    SortedMap<String, String> canonicalizedResourceList = new TreeMap<String, String>();

    private static Logger logger = LoggerFactory.getLogger(AzuSigning.class);

    public AzuSigning(AzuParameter parameter) throws Exception {
        contentEncoding = "";
        contentLanguage = "";
        contentLength = "";
        contentMD5 = "";
        contentType = "";
        date = "";
        ifModifiedSince = "";
        ifMatch = "";
        ifNoneMatch = "";
        ifUnmodifiedSince = "";
        range = "";

        this.parameter = parameter;
        String authentication = parameter.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        if (authentication == null) {
            logger.info("does not have Authorization header ...");
            S3User user = S3UserManager.getInstance().getUserByName(parameter.getUserName());
            if (user == null) {
                logger.error("User not found : {}", parameter.getUserName());
            } else {
                parameter.setUser(user);
            }
            return;
        }
        logger.debug("authentication : {}", authentication);
        String[] auths = authentication.split(" ");
        String[] sharedKeys = auths[1].split(":");
        String account = sharedKeys[0];
        String key = sharedKeys[1];
        logger.debug("account : {}, key : {}", account, key);

        String x_ms_client_request_id = parameter.getRequest().getHeader(AzuConstants.X_MS_CLIENT_REQUEST_ID);
        String x_ms_date = parameter.getRequest().getHeader(AzuConstants.X_MS_DATE);
        String x_ms_version = parameter.getRequest().getHeader(AzuConstants.X_MS_VERSION);

        getHeaderStringToSign();

        stringToSign = parameter.getMethod() + "\n"
            + contentEncoding + "\n"  // Content-Encoding
            + contentLanguage + "\n" // Content-Language// Content-Language
            + contentLength + "\n"  // Content-Length
            + contentMD5 + "\n"  // content-MD5
            + contentType + "\n"  // Content-Type
            + date + "\n"  // Date
            + ifModifiedSince + "\n"  // If-Modified-Since
            + ifMatch + "\n"  // If-Match
            + ifNoneMatch + "\n"  // If-None-Match
            + ifUnmodifiedSince + "\n"  // If-Unmodified-Since
            + range + "\n"  // Range
            + getCanonicalizedHeaderString()
            + getCanonicalizedResourceString(account);

        logger.debug("stringToSign : {}", stringToSign);
        String authKey = getAuthenticationString(account, stringToSign);
        if (authKey == null) {
            throw new Exception();
        }

        if (key.equals(authKey)) {
            logger.info("match key : {}, {}", key, authKey);
        } else {
            logger.info("does not match key : {}, {}", key, authKey);
            throw new Exception();
        }
    }

    private void getHeaderStringToSign() {
        for (String headerName : Collections.list(parameter.getRequest().getHeaderNames())) {
			for (String headerValue : Collections.list(parameter.getRequest().getHeaders(headerName))) {
				if (headerValue != null) {
                    if (headerName.toLowerCase().startsWith(AzuConstants.CANONICAL_HEADER_START_WITH)) {
                        canonicalizedHeaderList.put(headerName, headerValue);
                    } else if (headerName.equals(HttpHeaders.CONTENT_ENCODING)) {
                        contentEncoding = Strings.nullToEmpty(headerValue);
                    } else if (headerName.equals(HttpHeaders.CONTENT_LANGUAGE)) {
                        contentLanguage = Strings.nullToEmpty(headerValue);
                    } else if (headerName.equals(HttpHeaders.CONTENT_LENGTH)) {
                        contentLength = Strings.nullToEmpty(headerValue);
                        if (contentLength.equals("0")) {
                            contentLength = "";
                        }
                    } else if (headerName.equals(HttpHeaders.CONTENT_MD5)) {
                        contentMD5 = Strings.nullToEmpty(headerValue);
                    } else if (headerName.equals(HttpHeaders.CONTENT_TYPE)) {
                        contentType = Strings.nullToEmpty(headerValue);
                    } else if (headerName.equals(HttpHeaders.DATE)) {
                        date = Strings.nullToEmpty(headerValue);
                    } else if (headerName.equals(HttpHeaders.IF_MODIFIED_SINCE)) {
                        ifModifiedSince = Strings.nullToEmpty(headerValue);
                    } else if (headerName.equals(HttpHeaders.IF_MATCH)) {
                        ifMatch = Strings.nullToEmpty(headerValue);
                    } else if (headerName.equals(HttpHeaders.IF_NONE_MATCH)) {
                        ifNoneMatch = Strings.nullToEmpty(headerValue);
                    } else if (headerName.equals(HttpHeaders.IF_UNMODIFIED_SINCE)) {
                        ifUnmodifiedSince = Strings.nullToEmpty(headerValue);
                    } else if (headerName.equals(HttpHeaders.RANGE)) {
                        range = Strings.nullToEmpty(headerValue);
                    }
                }
			}
		}
    }

    private String getHeaderValueToSign(String headerName) {
        String value = parameter.getRequest().getHeader(headerName);
        if (Strings.isNullOrEmpty(value)) {
            value = "";
        }
        return value;
    }

    private String getCanonicalizedHeaderString() {
        String headers = "";
        for (Iterator<Map.Entry<String, String>> it = canonicalizedHeaderList.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, String> entry = it.next();
            headers += entry.getKey() + ":" + entry.getValue() + "\n";
        }

        return headers;
    }

    private String getCanonicalizedResourceString(String account) {
        String resource = "/" + account;
        try {
            String uri = parameter.getRequest().getRequestURI();
            uri = URLDecoder.decode(uri, AzuConstants.CHARSET_UTF_8);
            resource += uri;

            if (!Strings.isNullOrEmpty(parameter.getComp())) {
                if (!Strings.isNullOrEmpty(resource)) {
                    resource += "\n";
                }
                resource += "comp:" + parameter.getComp();
            }
            if (!Strings.isNullOrEmpty(parameter.getDelimiter())) {
                if (!Strings.isNullOrEmpty(resource)) {
                    resource += "\n";
                }
                resource += "delimiter:" + parameter.getDelimiter();
            }
            if (!Strings.isNullOrEmpty(parameter.getInclude())) {
                if (!Strings.isNullOrEmpty(resource)) {
                    resource += "\n";
                }
                resource += "include:" + parameter.getInclude();
            }
            if (!Strings.isNullOrEmpty(parameter.getMaxResults())) {
                if (!Strings.isNullOrEmpty(resource)) {
                    resource += "\n";
                }
                resource += "maxresults:" + parameter.getMaxResults();
            }
            if (!Strings.isNullOrEmpty(parameter.getPrefix())) {
                if (!Strings.isNullOrEmpty(resource)) {
                    resource += "\n";
                }
                resource += "prefix:" + parameter.getPrefix();
            }
            if (!Strings.isNullOrEmpty(parameter.getRestype())) {
                if (!Strings.isNullOrEmpty(resource)) {
                    resource += "\n";
                }
                resource += "restype:" + parameter.getRestype();
            }
            if (!Strings.isNullOrEmpty(parameter.getTimeout())) {
                if (!Strings.isNullOrEmpty(resource)) {
                    resource += "\n";
                }
                resource += "timeout:" + parameter.getTimeout();
            }
        } catch (Exception e) {
            PrintStack.logging(logger, e);
        }

        return resource;
    }

    private String getAuthenticationString(String account, String stringToSign) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            // String key = AzuSharedKeyManager.getInstance().getSharedKey(account);
            S3User user = S3UserManager.getInstance().getUserByName(account);
            if (user == null) {
                logger.error("User not found : {}", account);
                return "";
            }
            parameter.setUser(user);
            String key = user.getAzureKey();
            if (key == null) {
                return null;
            }

            byte[] keyBytes = java.util.Base64.getDecoder().decode(key.getBytes());
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            String authKey = Base64.encodeBase64String(mac.doFinal(stringToSign.getBytes("UTF-8")));
            return authKey;
        } catch (Exception e) {
            PrintStack.logging(logger, e);
        }
        
        return null;
    }

    public S3User getUser() {
        return parameter.getUser();
    }
}

