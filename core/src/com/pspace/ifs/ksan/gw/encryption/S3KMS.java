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
package com.pspace.ifs.ksan.gw.encryption;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.WebClientDevWrapper;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;

import com.pspace.ifs.ksan.libs.PrintStack;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3KMS {
    private final Logger logger = LoggerFactory.getLogger(S3KMS.class);
    private String enckey;
    private String encindex;
    private S3Parameter s3p;

    public S3KMS() {
    }

    public String getEnckey() {
        return enckey;
    }

    public String getEncindex() {
        return encindex;
    }
    
    public void createKey(S3Parameter s3p, String masterKeyId, String path) throws GWException {
        // KMS Configuration Check
        // KMS Type 1. Hashicorp, 2. Redhat KMS
        if (Strings.isNullOrEmpty(s3p.getKmsType())) {
            logger.error(GWErrorCode.INVALID_ARGUMENT.getMessage());
            throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3p);
        } else if (s3p.getKmsType().equalsIgnoreCase("hashicorp")) {
            try {
                RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(5000)
                        .setConnectTimeout(5000).setExpectContinueEnabled(false).build();

                String kmsUrl = s3p.getKmsEndpoint() + "/v1/transit/keys/" + path;
                JsonObject body = new JsonObject();
                body.addProperty("type", "aes256-gcm96");
                body.addProperty("derived", true);
                body.addProperty("exportable", true);
                logger.info(body.toString());
                logger.info(kmsUrl);

                HttpClient clnt = WebClientDevWrapper.wrapClient();
                HttpPost createKMS = new HttpPost(kmsUrl);
                createKMS = WebClientDevWrapper.wrapHttpPost(new HttpPost(kmsUrl), requestConfig);
                createKMS.setHeader("X-Vault-Token", masterKeyId);
                createKMS.setEntity(new StringEntity(body.toString()));
                HttpResponse createKMSRes = clnt.execute(createKMS);

                if (createKMSRes.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT) {
                    InputStream input;

                    input = new ByteArrayInputStream(EntityUtils.toString(createKMSRes.getEntity())
                            .replaceAll("&", "&amp;").getBytes("UTF-8"));
                    String kmsResponse = IOUtils.toString(input, StandardCharsets.UTF_8);
                    input.close();

                    logger.error("create KMS Post Status Code : " + createKMSRes.getStatusLine().getStatusCode());
                    logger.info(kmsResponse);
                    throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3p);
                }
            } catch (IOException e) {
                PrintStack.logging(logger, e);
                throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3p);
            }

        } else if (s3p.getKmsType().equalsIgnoreCase("redhat")) {

        }
    }

    public void exportKey(S3Parameter s3p, String masterKeyId, String path, String index) throws GWException {
        // KMS Configuration Check
        // KMS Type 1. Hashicorp, 2. Redhat KMS
        if (Strings.isNullOrEmpty(s3p.getKmsType())) {
            logger.error(GWErrorCode.INVALID_ARGUMENT.getMessage());
            throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3p);
        } else if (s3p.getKmsType().equalsIgnoreCase("hashicorp")) {
            try {
                RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(5000)
                        .setConnectTimeout(5000).setExpectContinueEnabled(false).build();

                String kmsUrl = s3p.getKmsEndpoint() + "/v1/transit/export/encryption-key/" + path;

                HttpClient clnt = WebClientDevWrapper.wrapClient();
                HttpGet exportKMS = new HttpGet(kmsUrl);
                logger.info(kmsUrl);
                exportKMS = WebClientDevWrapper.wrapHttpGet(new HttpGet(kmsUrl), requestConfig);
                exportKMS.setHeader("X-Vault-Token", masterKeyId);
                HttpResponse exportKMSRes = clnt.execute(exportKMS);

                if (exportKMSRes.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    InputStream input;

                    input = new ByteArrayInputStream(EntityUtils.toString(exportKMSRes.getEntity())
                            .replaceAll("&", "&amp;").getBytes("UTF-8"));
                    String kmsResponse = IOUtils.toString(input, StandardCharsets.UTF_8);
                    logger.info(kmsResponse);
                    input.close();

                    JSONObject jsonRoot = new JSONObject(kmsResponse);
                    JSONObject jsonData = jsonRoot.getJSONObject("data");
                    JSONObject jsonKeys = jsonData.getJSONObject("keys");
                    for ( String key : JSONObject.getNames(jsonKeys) ) {
                        String value = (String)jsonKeys.get(key);
                        enckey = value;
                        encindex = key;

                        if(!Strings.isNullOrEmpty(index) && index.equalsIgnoreCase(key)) {
                            break;
                        }
                    }
                    
                    enckey = new String(Base64.getDecoder().decode(enckey.getBytes()), StandardCharsets.UTF_8);
                } else {
                    InputStream input;

                    input = new ByteArrayInputStream(EntityUtils.toString(exportKMSRes.getEntity())
                            .replaceAll("&", "&amp;").getBytes("UTF-8"));
                    String kmsResponse = IOUtils.toString(input, StandardCharsets.UTF_8);
                    input.close();

                    logger.error("create KMS Post Status Code : " + exportKMSRes.getStatusLine().getStatusCode());
                    logger.info(kmsResponse);
                    throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3p);
                }
            } catch (IOException e) {
                PrintStack.logging(logger, e);
                throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3p);
            }
        } else if (s3p.getKmsType().equalsIgnoreCase("redhat")) {

        }
    }
}
