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
