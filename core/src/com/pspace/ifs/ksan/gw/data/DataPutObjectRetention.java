package com.pspace.ifs.ksan.gw.data;

import java.util.Collections;

import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

import org.slf4j.LoggerFactory;

public class DataPutObjectRetention extends S3DataRequest {
    private String versionId;

    private String contentMd5;
    private String bypassGovernanceRetention;
	private String requestPayer;
	private String expectedBucketOwner;
	private String retentionXml;

    public DataPutObjectRetention(S3Parameter s3Parameter) throws GWException {
        super(s3Parameter);
        logger = LoggerFactory.getLogger(DataPutObjectRetention.class);
    }

    @Override
    public void extract() throws GWException {
        versionId = s3Parameter.getRequest().getParameter(GWConstants.VERSION_ID);
		if (Strings.isNullOrEmpty(versionId)) {
			logger.debug(GWConstants.LOG_DATA_VERSION_ID_NULL);
		}
        
        for (String headerName : Collections.list(s3Parameter.getRequest().getHeaderNames())) {
			if (headerName.equalsIgnoreCase(GWConstants.CONTENT_MD5)) {
				contentMd5 = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_BYPASS_GOVERNANCE_RETENTION)) {
				bypassGovernanceRetention = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_REQUEST_PAYER)) {
				requestPayer = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_EXPECTED_BUCKET_OWNER)) {
				expectedBucketOwner = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			}
		}
    }

    public String getVersionId() {
        return versionId;
    }

    public String getContentMd5() {
        return contentMd5;
    }

    public String getBypassGovernanceRetention() {
        return bypassGovernanceRetention;
    }

    public String getRequestPayer() {
        return requestPayer;
    }

    public String getExpectedBucketOwner() {
        return expectedBucketOwner;
    }
    
    public String getRetentionXml() throws GWException {
        retentionXml = readXml();
		return retentionXml;
    }
}
