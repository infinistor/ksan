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

import com.google.common.hash.HashCode;
import com.google.common.io.BaseEncoding;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

public abstract class S3Encryption {
	protected S3Metadata s3Metadata;
    protected S3Parameter s3Parameter;
    protected boolean enableSSEServer;
	protected boolean enableSSECustomer;
    protected String algorithm;               // x-amz-server-side-encryption
    protected String customerAlgorithm;       // x-amz-server-side-encryption-customer-algorithm
    protected String customerKey;
    protected String customerKeyMD5;
    protected String encryptionXml;

    public S3Encryption(S3Metadata s3Metadata, S3Parameter s3Parameter) {
        this.s3Metadata = s3Metadata;
        this.s3Parameter = s3Parameter;
        this.customerAlgorithm = s3Metadata.getCustomerAlgorithm();
        this.customerKey = s3Metadata.getCustomerKey();
        this.customerKeyMD5 = s3Metadata.getCustomerKeyMD5();
        this.algorithm = s3Metadata.getServersideEncryption();
    }

    public S3Encryption(String serverSideEncryption, String customerAlgorithm, String customerKey, String customerKeyMD5, S3Parameter s3Parameter) {
        this.s3Parameter = s3Parameter;
        this.customerAlgorithm = customerAlgorithm;
        this.customerKey = customerKey;
        this.customerKeyMD5 = customerKeyMD5;
        this.algorithm = serverSideEncryption;
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

    public boolean isEncryptionEnabled() {
        return enableSSEServer || enableSSECustomer;
    }

    public boolean isEnableSSEServer() {
        return enableSSEServer;
    }

    public void setEnableSSEServer(boolean enableSSEServer) {
        this.enableSSEServer = enableSSEServer;
    }

    public boolean isEnableSSECustomer() {
        return enableSSECustomer;
    }

    public void setEnableSSECustomer(boolean enableSSECustomer) {
        this.enableSSECustomer = enableSSECustomer;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getCustomerAlgorithm() {
        return customerAlgorithm;
    }

    public void setCustomerAlgorithm(String customerAlgorithm) {
        this.customerAlgorithm = customerAlgorithm;
    }

    public String getCustomerKey() {
        if (enableSSECustomer || enableSSEServer) {
            return customerKey;
        }

        return null;
    }

    public void setCustomerKey(String customerKey) {
        this.customerKey = customerKey;
    }

    public String getCustomerKeyMD5() {
        return customerKeyMD5;
    }

    public void setCustomerKeyMD5(String customerKeyMD5) {
        this.customerKeyMD5 = customerKeyMD5;
    }
}
