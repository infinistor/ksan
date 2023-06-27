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
package com.pspace.ifs.ksan.gw.data.gcs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.libs.PrintStack;

public class GCSRequestData {
    protected String contentLength;
	protected S3Parameter s3Parameter;
    protected Logger logger;
    
    public GCSRequestData(S3Parameter s3Parameter) {
		this.s3Parameter = s3Parameter;
		logger = LoggerFactory.getLogger(GCSRequestData.class);
	}

    public String getContentLength() throws GWException {
		String contentLength = s3Parameter.getRequest().getHeader(HttpHeaders.CONTENT_LENGTH);
		if (!Strings.isNullOrEmpty(contentLength)) {
			long length = Long.parseLong(contentLength);
			if (length < 0) {
				throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
			}
		}
		return contentLength;
	}

    public String getContentRange() {
        String contentRange = s3Parameter.getRequest().getHeader(HttpHeaders.CONTENT_RANGE);
        return contentRange;
    }

    public String getBucketName() throws GWException {
        String jsonString = readBody();
        logger.debug("jsonString: {}", jsonString);
        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject)parser.parse(jsonString);
            String bucketName = (String)jsonObject.get("name");
            return bucketName;
        } catch (ParseException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
        }
    }

    public String getObjectName(String data) throws GWException {
        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject)parser.parse(data);
            String objectInfo = (String)jsonObject.get("name");
            return objectInfo;
        } catch (ParseException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
        }
    }

	public String readBody() throws GWException {
		String ret = null;

		try {
            String contentEncoding = s3Parameter.getRequest().getHeader(GWConstants.CONTENT_ENCODING);
            if (Strings.isNullOrEmpty(contentEncoding)) {
                byte[] json = s3Parameter.getInputStream().readAllBytes();
                s3Parameter.addRequestSize(json.length);
                ret = new String(json);
            } else {
                if (contentEncoding.equals("gzip")) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(s3Parameter.getInputStream())));
                    StringBuilder sb = new StringBuilder();
                    for (String line; (line = br.readLine())!= null;) {
                        sb.append(line);
                    }
                    ret = sb.toString();
                } else {
                    byte[] json = s3Parameter.getInputStream().readAllBytes();
                    s3Parameter.addRequestSize(json.length);
                    ret = new String(json);
                }
            }
            
            logger.debug("readBody: {}", ret);
			
		} catch (IOException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}
		
		if (Strings.isNullOrEmpty(ret)) {
			logger.warn(GWErrorCode.INVALID_ARGUMENT.getMessage());
			throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
		}
		
		return ret;
	}

    public String getBoundary() {
        String contentType = s3Parameter.getRequest().getHeader(GWConstants.CONTENT_TYPE);
        String boundary = null;
        if (!Strings.isNullOrEmpty(contentType)) {
            String[] split = contentType.split(";");
            for (String str : split) {
                if (str.contains("boundary")) {
                    if (str.contains("'")) {
                        String[] split2 = str.split("'");
                        if (split2.length == 2) {
                            boundary = split2[1];
                        }
                    } else {
                        String[] split2 = str.split("=");
                        if (split2.length == 2) {
                            boundary = split2[1];
                        }
                    }
                }
            }
        }
        return boundary;
    }

    public String getFields() {
        String fields = s3Parameter.getRequest().getParameter("fields");
        return fields;
    }

    public String getProjection() {
        String projection = s3Parameter.getRequest().getParameter("projection");
        return projection;
    }

    public String getUploadType() {
        String uploadType = s3Parameter.getRequest().getParameter("uploadType");
        return uploadType;
    }

    public String getUpladContentsLength() {
        String uploadContentsLength = s3Parameter.getRequest().getHeader("x-upload-content-length");
        return uploadContentsLength;
    }

    public String getUploadId() {
        String uploadId = s3Parameter.getRequest().getHeader("upload_id");
        return uploadId;
    }

    public String getParamUploadId() {
		return s3Parameter.getRequest().getParameter("upload_id");
	}

    public String getAlt() {
        return s3Parameter.getRequest().getParameter("alt");
    }

    public String getRange() {
		return s3Parameter.getRequest().getHeader(GWConstants.RANGE);
	}
}
