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
import com.pspace.ifs.ksan.gw.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.utils.PrintStack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3ObjectEncryption extends S3Encryption {
    private final Logger logger = LoggerFactory.getLogger(S3ObjectEncryption.class);

    public S3ObjectEncryption(S3Parameter s3Parameter, S3Metadata s3Metadata) {
        super(s3Metadata, s3Parameter);
        setEnableSSEServer(false);
        setEnableSSECustomer(false);
    }

    public void build() throws GWException {
        enableSSEServer = false;
        enableSSECustomer = false;

        // customer header
        if (!Strings.isNullOrEmpty(s3Metadata.getCustomerKey()) && !Strings.isNullOrEmpty(s3Metadata.getCustomerKeyMD5())) {
            String md5 = makeMD5(s3Metadata.getCustomerKey());
            
            if (md5.compareTo(s3Metadata.getCustomerKeyMD5()) != 0) {
                logger.error(GWErrorCode.INVALID_DIGEST.getMessage() + GWConstants.LOG_S3SERVER_SIDE_ENCRYPTION_CALC_KEY + md5 + GWConstants.LOG_S3SERVER_SIDE_ENCRYPTION_SOURCE_KEY + s3Metadata.getCustomerKeyMD5());
                throw new GWException(GWErrorCode.INVALID_DIGEST, s3Parameter);
            }
        }

        enableSSEServer = checkBucketEncryption(s3Parameter.getBucket().getEncryption());

        if (enableSSEServer) {
            setCustomerKey(GWConstants.INFINISTOR);
        }

        if (!Strings.isNullOrEmpty(s3Metadata.getEncryption()) && s3Metadata.getEncryption().equalsIgnoreCase(GWConstants.AES256) == true) {
            enableSSEServer = true;
            
            if (Strings.isNullOrEmpty(getCustomerKey())) {
                setCustomerKey(GWConstants.INFINISTOR);
            }
        }

        if (!Strings.isNullOrEmpty(s3Metadata.getServersideEncryption()) && s3Metadata.getServersideEncryption().equalsIgnoreCase(GWConstants.AES256) == true) {
            enableSSEServer = true;
            
            if (Strings.isNullOrEmpty(getCustomerKey())) {
                setCustomerKey(GWConstants.INFINISTOR);
            }
        }

        if (!Strings.isNullOrEmpty(s3Metadata.getCustomerAlgorithm()) && s3Metadata.getCustomerAlgorithm().equalsIgnoreCase(GWConstants.AES256) == true) {
            enableSSECustomer = true;
            
            if (Strings.isNullOrEmpty(getCustomerKey())) {
                setCustomerKey(GWConstants.INFINISTOR);
            }
        }

        logger.info(GWConstants.S3OBJECT_ENCRYPTION_ENABLE_SSE_SERVER, enableSSEServer);
        logger.info(GWConstants.S3OBJECT_ENCRYPTION_ENABLE_SSE_CUSTOMER, enableSSECustomer);
    }

    public boolean checkBucketEncryption(String encryption) throws GWException {
		try {
			if(Strings.isNullOrEmpty(encryption)) {
				return false;
			}

			ServerSideEncryption sse = new XmlMapper().readValue(encryption, ServerSideEncryption.class);

			if (sse.rules.size() > 0) {
				for (Rule r : sse.rules) {
					if( r.apply.SSEAlgorithm.compareTo(GWConstants.AES256) == 0 ) {
						return true;					
					}
				}

				return false;
			} else {
				return false;
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
