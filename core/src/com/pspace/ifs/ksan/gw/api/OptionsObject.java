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
package com.pspace.ifs.ksan.gw.api;

import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.CORSConfiguration;
import com.pspace.ifs.ksan.gw.format.CORSConfiguration.CORSRule;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;

import org.slf4j.LoggerFactory;

public class OptionsObject extends S3Request {
    public OptionsObject(S3Parameter s3Parameter) {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(OptionsObject.class);
	}

	@Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_OPTIONS_OBJECT_START);
		
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);

		if (Strings.isNullOrEmpty(getBucketInfo().getCors())) {
			throw new GWException(GWErrorCode.NO_SUCH_CORS_CONFIGURATION, s3Parameter);
		} else {
			XmlMapper xmlMapper = new XmlMapper();
			try {
				CORSConfiguration corsConfiguration = xmlMapper.readValue(getBucketInfo().getCors(), CORSConfiguration.class);

				String corsOrigin = s3Parameter.getRequest().getHeader(HttpHeaders.ORIGIN);
				if (Strings.isNullOrEmpty(corsOrigin)) {
					throw new GWException(GWErrorCode.INVALID_CORS_ORIGIN, s3Parameter);
				}

				String corsMethods = s3Parameter.getRequest().getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
				String corsHeaders = s3Parameter.getRequest().getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);

				boolean originpass = false;
				String resOrigin = "";
				String allowMethods = "";
				String allowHeaders = "";
				String maxAges = "";
				String exposeHeaders = "";
				for (CORSRule icors : corsConfiguration.CORSRules) {
					for (String origin : icors.AllowedOrigins) {
						if (GWUtils.likematch(origin, corsOrigin)) {
							if (origin.equals(GWConstants.ASTERISK)) {
								resOrigin = origin;
							} else {
								resOrigin = corsOrigin;
							}
							originpass = true;
						}
					}

					if (originpass == false)
						continue;
					
					int first = 0;
					if (!Strings.isNullOrEmpty(corsMethods) && icors.AllowedMethods != null) {
						for (String corsMethod : corsMethods.split(GWConstants.COMMA)) {
							boolean temp = false;

							// if(icors.AllowedMethods == null) {
							// 	continue;
							// }

							for (String method : icors.AllowedMethods) {
								if (method.compareTo(GWConstants.ASTERISK) == 0 || corsMethod.trim().compareTo(method) == 0) {
									temp = true;
								}
							}

							if (temp == true && first == 0) {
								allowMethods += corsMethod;
								first++;
							} else if (temp == true && first > 0) {
								allowMethods += GWConstants.COMMA + corsMethod;
								first++;
							}
						}
					} else if (Strings.isNullOrEmpty(corsMethods) && icors.AllowedMethods != null) {
						throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
					}

					first = 0;
					if (!Strings.isNullOrEmpty(corsHeaders)) {
						for (String corsHeader : corsHeaders.split(GWConstants.COMMA)) {
							boolean temp = false;

							if(icors.AllowedHeaders == null) {
								continue;
							}

							for (String header : icors.AllowedHeaders) {
								if (header.compareTo(GWConstants.ASTERISK) == 0 || corsHeader.trim().compareTo(header) == 0) {
									temp = true;
								}
							}

							if (temp == true && first == 0) {
								allowHeaders += corsHeader;
								first++;
							} else if (temp == true && first > 0) {
								allowHeaders += GWConstants.COMMA + corsHeader;
								first++;
							}
						}
					}

					first = 0;
					if(icors.ExposeHeaders != null) {
						for (String exposeHeader : icors.ExposeHeaders) {
							if (first == 0) {
								exposeHeaders += exposeHeader;
								first++;
							} else {
								exposeHeaders += GWConstants.COMMA + exposeHeader;
								first++;
							}
						}
					}

					if( !Strings.isNullOrEmpty(icors.MaxAgeSeconds))
						maxAges = icors.MaxAgeSeconds;

					if(originpass == true)
						break;
				}

				if (originpass == false ) {
					throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
				}

				// check
				if (Strings.isNullOrEmpty(allowMethods)) {
					if (!Strings.isNullOrEmpty(corsMethods)) {
						throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
					}
				}

				// check
				if (Strings.isNullOrEmpty(allowHeaders)) {
					if (!Strings.isNullOrEmpty(corsHeaders)) {
						throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
					}
				}

				s3Parameter.getResponse().addHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
				s3Parameter.getResponse().addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, resOrigin);

				if (!Strings.isNullOrEmpty(allowMethods)) {
					s3Parameter.getResponse().addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowMethods);
				}

				if (!Strings.isNullOrEmpty(allowHeaders)) {
					s3Parameter.getResponse().addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowHeaders);
				}

				if (!Strings.isNullOrEmpty(exposeHeaders)) {
					s3Parameter.getResponse().addHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, exposeHeaders);
				}

				if (!maxAges.isEmpty()) {
					s3Parameter.getResponse().addHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, maxAges);
				}

			} catch (JsonProcessingException e) {
				PrintStack.logging(logger, e);
			}
		}

		s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
	}
}
