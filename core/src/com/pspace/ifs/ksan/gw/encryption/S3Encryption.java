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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.google.common.hash.HashCode;
import com.google.common.io.BaseEncoding;
import com.google.common.base.Strings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.format.ServerSideEncryption;
import com.pspace.ifs.ksan.gw.format.ServerSideEncryption.Rule;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.libs.PrintStack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3Encryption {
    private final Logger logger = LoggerFactory.getLogger(S3Encryption.class);
    private String algorithm; // x-amz-server-side-encryption
    private String customerAlgorithm; // x-amz-server-side-encryption-customer-algorithm
    private String customerKey;
    private String customerKeyMD5;
    private String encryptionXml; // bucket encryption Xml
    private String kmsMasterKeyId;
    private String bucketKeyEnabled;
    private S3Parameter s3Parameter;
    private String encryptionKey;
    private String kmsKeyIndex;
    private String kmsKeyPath;
    private String op;

    private final String ALGORITHM_AES256 = "AES256";

    // bucket xml, x-amz-server-side-encryption, x-amz-customer-algorithm,
    // x-amz-customer-key, x-amz-customer-key-MD5, upload metadata
    // awk:kms
    public S3Encryption(String customerAlgorithm, String customerKey, String customerKeyMD5, S3Parameter s3Parameter) {
        this.s3Parameter = new S3Parameter(s3Parameter);

        if (!Strings.isNullOrEmpty(customerAlgorithm))
            this.customerAlgorithm = customerAlgorithm;

        if (!Strings.isNullOrEmpty(customerKey))
            this.customerKey = customerKey;

        if (!Strings.isNullOrEmpty(customerKeyMD5))
            this.customerKeyMD5 = customerKeyMD5;
    }

    public S3Encryption(String bucketEncryption, String serverSideEncryption,
            String kmsMasterKeyId, String bucketKeyEnabled,
            S3Parameter s3Parameter) {
        op = "put";
        this.s3Parameter = new S3Parameter(s3Parameter);

        if (!Strings.isNullOrEmpty(bucketEncryption))
            this.encryptionXml = bucketEncryption;

        if (!Strings.isNullOrEmpty(serverSideEncryption))
            this.algorithm = serverSideEncryption;

        if (!Strings.isNullOrEmpty(kmsMasterKeyId))
            this.kmsMasterKeyId = kmsMasterKeyId;

        if (!Strings.isNullOrEmpty(bucketKeyEnabled))
            this.bucketKeyEnabled = bucketKeyEnabled;
    }

    public S3Encryption(String option, S3Metadata s3metadata, S3Parameter s3Parameter) {
        this.s3Parameter = new S3Parameter(s3Parameter);
        if(option.equalsIgnoreCase("upload")) {
            op = "put";
            this.customerAlgorithm = s3metadata.getCustomerAlgorithm();
            this.customerKey = s3metadata.getCustomerKey();
            this.customerKeyMD5 = s3metadata.getCustomerKeyMD5();
            this.algorithm = s3metadata.getServerSideEncryption();
            this.kmsMasterKeyId = s3metadata.getKmsKeyId();
            this.bucketKeyEnabled = s3metadata.getBucketKeyEnabled();
            this.kmsKeyIndex = s3metadata.getKmsKeyIndex();
            this.kmsKeyPath = s3metadata.getKmsKeyPath();
        } else {
            op = "get";
            this.algorithm = s3metadata.getServerSideEncryption();
            this.kmsMasterKeyId = s3metadata.getKmsKeyId();
            this.bucketKeyEnabled = s3metadata.getBucketKeyEnabled();
            this.kmsKeyIndex = s3metadata.getKmsKeyIndex();
            this.kmsKeyPath = s3metadata.getKmsKeyPath();
        }
    }

    public void build() throws GWException {
        String bucket = s3Parameter.getBucketName();
		String object = s3Parameter.getObjectName();

        // 1. customer key
        if (!Strings.isNullOrEmpty(customerAlgorithm) && customerAlgorithm.equalsIgnoreCase(ALGORITHM_AES256) == true) {
            if (!Strings.isNullOrEmpty(customerKey) && !Strings.isNullOrEmpty(customerKeyMD5)) {
                String MD5 = makeMD5(customerKey);
                if (MD5.compareTo(customerKeyMD5) != 0) {
                    logger.error(GWErrorCode.INVALID_DIGEST.getMessage() + " encryption calc key : " + MD5
                            + ", encryption source key : " + customerKeyMD5);
                    throw new GWException(GWErrorCode.INVALID_DIGEST, s3Parameter);
                }

                encryptionKey = customerKey;
            } else if ( !Strings.isNullOrEmpty(customerKey) && Strings.isNullOrEmpty(customerKeyMD5)){
                encryptionKey = customerKey;
            }

            return;
        }

        // 2. aws-kms
        if (!Strings.isNullOrEmpty(kmsMasterKeyId)) {
            logger.debug("awskms");
            if(op.equals("get")) {
                getKmsKey(kmsMasterKeyId, kmsKeyPath, kmsKeyIndex);
            } else if (op.equals("put")) {
                if ( !Strings.isNullOrEmpty(bucketKeyEnabled) && bucketKeyEnabled.equalsIgnoreCase("true") ) {
                    kmsKeyPath = bucket;
                } else {
                    kmsKeyPath = bucket + "/" + object;
                }
                
                putKmsKey(kmsMasterKeyId, kmsKeyPath);
                getKmsKey(kmsMasterKeyId, kmsKeyPath, kmsKeyIndex);
            }

            return;
        }

        // 3. server side encryption
        if (!Strings.isNullOrEmpty(algorithm) && algorithm.equalsIgnoreCase(ALGORITHM_AES256) == true) {
            logger.debug("enable server side encryption");
            encryptionKey = "INFINISTOR";
            return;
        }

        // 4. bucket setting
        if (!Strings.isNullOrEmpty(encryptionXml)) {
            try {
                ServerSideEncryption sse = new XmlMapper().readValue(encryptionXml, ServerSideEncryption.class);
                if (sse.rules.size() > 0) {
                    for (Rule r : sse.rules) {
                        if (r.apply.SSEAlgorithm.compareTo(ALGORITHM_AES256) == 0) {
                            algorithm = ALGORITHM_AES256;
                            encryptionKey = "INFINISTOR";
                            return;
                        }

                        if (!Strings.isNullOrEmpty(r.apply.KMSMasterKeyID)) {
                            kmsMasterKeyId = r.apply.KMSMasterKeyID;
                            if (!Strings.isNullOrEmpty(r.BucketKeyEnabled)
                                    && r.BucketKeyEnabled.equalsIgnoreCase("true") == true) {
                                bucketKeyEnabled = "true";
                                kmsKeyPath = bucket;
                                if(op.equals("get")) {
                                    getKmsKey(kmsMasterKeyId, kmsKeyPath, kmsKeyIndex);
                                } else if (op.equals("put")) {
                                    putKmsKey(kmsMasterKeyId, kmsKeyPath);
                                    getKmsKey(kmsMasterKeyId, kmsKeyPath, kmsKeyIndex);
                                }
                            } else {
                                kmsKeyPath = bucket + "/" + object;
                                if(op.equals("get")) {
                                    getKmsKey(kmsMasterKeyId, kmsKeyPath, kmsKeyIndex);
                                } else if (op.equals("put")) {
                                    putKmsKey(kmsMasterKeyId, kmsKeyPath);
                                    getKmsKey(kmsMasterKeyId, kmsKeyPath, kmsKeyIndex);
                                }
                            }

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

    protected String makeMD5(String str) {
		String md5 = GWConstants.EMPTY_STRING; 
		
		try {
			MessageDigest md = MessageDigest.getInstance(GWConstants.MD5);
			md.update(BaseEncoding.base64().decode(str)); 
			byte byteData[] = md.digest();
			HashCode actualHashCode = HashCode.fromBytes(byteData);			
			md5 = BaseEncoding.base64().encode(actualHashCode.asBytes());
		} catch (NoSuchAlgorithmException e){
			e.printStackTrace(); 
			md5 = null; 
		}

		return md5;
	}

    public void getKmsKey(String kMKId, String kKP, String kKI) throws GWException {
        S3KMS s3KMS = new S3KMS();
        s3KMS.exportKey(s3Parameter, kMKId, kKP, kKI);
        kmsKeyIndex = s3KMS.getEncindex();
        encryptionKey = s3KMS.getEnckey();
        return;
    }

    public void putKmsKey(String kMKId, String kKP) throws GWException {
        S3KMS s3KMS = new S3KMS();
        s3KMS.createKey(s3Parameter, kMKId, kKP);
        encryptionKey = s3KMS.getEnckey();
        kmsKeyIndex = s3KMS.getEncindex();
        kmsKeyPath = kKP;
        kmsMasterKeyId = kMKId;
        return;
    }

    public String getEncKey() {
        return encryptionKey;
    }

    public boolean isEnabledEncryption() {
        if( Strings.isNullOrEmpty(encryptionKey) ) {
            return false;
        }

        return true;
    }

    public String getKmsMasterKeyId() {
        return kmsMasterKeyId;
    }

    public String getKmsKeyPath() {
        return kmsKeyPath;
    }

    public String getCustomerAlgorithm() {
        return customerAlgorithm;
    }

    public String getKmsKeyIndex() {
        return kmsKeyIndex;
    }

    public String getCustomerKey() {
        return customerKey;
    }

    public String getCustomerKeyMD5() {
        return customerKeyMD5;
    }

    public String getServerSideEncryption() {
        return algorithm;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }
}
