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
package com.pspace.ifs.ksan.gw.object;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Strings;
import com.google.common.hash.HashCode;
import com.google.common.io.BaseEncoding;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.ServerSideEncryption;
import com.pspace.ifs.ksan.gw.format.ServerSideEncryption.Rule;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3ServerSideEncryption extends S3Encryption {
    private final Logger logger = LoggerFactory.getLogger(S3ServerSideEncryption.class);

    public S3ServerSideEncryption(String encryptionXml, S3Metadata s3Metadata, S3Parameter s3Parameter) {
        super(s3Metadata, s3Parameter);
        this.encryptionXml = encryptionXml;
    }

    public void srcbuild() throws GWException {
        if (!Strings.isNullOrEmpty(customerAlgorithm) && customerAlgorithm.equalsIgnoreCase(GWConstants.AES256) == true) {
            if (!Strings.isNullOrEmpty(customerKey) && !Strings.isNullOrEmpty(customerKeyMD5)) {
                String md5 = makeMD5(customerKey);			
                if (md5.compareTo(customerKeyMD5) != 0) {
                    logger.error(GWErrorCode.INVALID_DIGEST.getMessage() + GWConstants.LOG_S3SERVER_SIDE_ENCRYPTION_CALC_KEY + md5 + GWConstants.LOG_S3SERVER_SIDE_ENCRYPTION_SOURCE_KEY + customerKeyMD5);
                    throw new GWException(GWErrorCode.INVALID_DIGEST, s3Parameter);
                }
            } else {
                customerKey = GWConstants.INFINISTOR;
            }
            enableSSECustomer = true;
            return;
        }
        
        if (!Strings.isNullOrEmpty(algorithm) && algorithm.equalsIgnoreCase(GWConstants.AES256) == true) {
            enableSSEServer = true;
            customerKey = GWConstants.INFINISTOR;
            return;
        }
    }

    public void build() throws GWException {
        if (!Strings.isNullOrEmpty(customerAlgorithm) && customerAlgorithm.equalsIgnoreCase(GWConstants.AES256) == true) {
            if (!Strings.isNullOrEmpty(customerKey) && !Strings.isNullOrEmpty(customerKeyMD5)) {
                String MD5 = makeMD5(customerKey);			
                if (MD5.compareTo(customerKeyMD5) != 0) {
                    logger.error(GWErrorCode.INVALID_DIGEST.getMessage() + GWConstants.LOG_S3SERVER_SIDE_ENCRYPTION_CALC_KEY + MD5 + GWConstants.LOG_S3SERVER_SIDE_ENCRYPTION_SOURCE_KEY + customerKeyMD5);
                    throw new GWException(GWErrorCode.INVALID_DIGEST, s3Parameter);
                }
            } else {
                customerKey = GWConstants.INFINISTOR;
            }
            enableSSECustomer = true;
            return;
        }
        
        if (!Strings.isNullOrEmpty(algorithm) && algorithm.equalsIgnoreCase(GWConstants.AES256) == true) {
            enableSSEServer = true;
            customerKey = GWConstants.INFINISTOR;
            return;
        }

        // Check bucket encryption
        if(!Strings.isNullOrEmpty(encryptionXml)) {
            try {
                ServerSideEncryption sse = new XmlMapper().readValue(encryptionXml, ServerSideEncryption.class);
                if (sse.rules.size() > 0) {
                    for (Rule r : sse.rules) {
                        if (r.apply.SSEAlgorithm.compareTo(GWConstants.AES256) == 0) {
                            enableSSEServer = true;
                            customerKey = GWConstants.INFINISTOR;
                            return;
                        }
                    }
                }
            } catch (JsonMappingException e) {
                PrintStack.logging(logger, e);
                throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
            } catch (JsonProcessingException e) {
                PrintStack.logging(logger, e);
                throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
            }
        }
    }
}
