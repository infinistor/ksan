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

import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.LifecycleConfiguration;
import com.pspace.ifs.ksan.gw.format.LifecycleConfiguration.Rule;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.libs.PrintStack;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Strings;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.slf4j.LoggerFactory;

public class PutBucketLifecycle extends S3Request {
    public PutBucketLifecycle(S3Parameter s3Parameter) {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(PutBucketLifecycle.class);
	}

	@Override
	public void process() throws GWException {
        logger.info(GWConstants.LOG_PUT_BUCKET_LIFECYCLE_START);
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);

		GWUtils.checkCors(s3Parameter);

		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}

		if (!checkPolicyBucket(GWConstants.ACTION_PUT_LIFECYCLE_CONFIGURATION, s3Parameter)) {
			checkGrantBucket(true, GWConstants.GRANT_WRITE_ACP);
		}

		String lifecycleXml = s3RequestData.getLifecycleXml();
		logger.info(GWConstants.LOG_PUT_BUCKET_LIFECYCLE_XML, lifecycleXml);
		checkBucketLifecycle(lifecycleXml);
		updateBucketLifecycle(bucket, lifecycleXml);

		s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
	}

	private void checkBucketLifecycle(String lifecycleXml) throws GWException {
		XmlMapper xmlMapper = new XmlMapper();
		LifecycleConfiguration lc;
		try {
			lc = xmlMapper.readValue(lifecycleXml, LifecycleConfiguration.class);
		} catch (JsonMappingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		Map<String, String> id = new HashMap<String, String>();

		if (lc.rules != null) {
			logger.warn("{}", lc.rules.toString());
			for (Rule rl : lc.rules) {
				logger.info("rule.id : {}", rl.id);
				logger.info("rule.prefix : {}", rl.prefix);

				// Id 정합성 체크
				if (!Strings.isNullOrEmpty(rl.id)) {
					if (rl.id.length() > 255) {
						logger.error(GWErrorCode.INVALID_ARGUMENT.getMessage());
						throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
					}
					id.put(rl.id, rl.id);
				} else {
					byte[] array = new byte[7]; // length is bounded by 7
					new Random().nextBytes(array);
					String generatedString = new String(array, Charset.forName(Constants.CHARSET_UTF_8));
					id.put(generatedString, generatedString);
				}

				// filter 정합성 체크
				if (rl.filter != null) {
					logger.info("rule.filter : {}", rl.filter);
					if (rl.filter.and != null)
						logger.info("rule.filter.and : {}", rl.filter.and);

					if (rl.filter.and != null) {
						logger.info("rule.filter.and.prefix : {}", rl.filter.and.prefix);
						if (rl.filter.and.tag != null)
							logger.info("rule.filter.and.tag : {}", rl.filter.and.tag);

						if (rl.filter.and.tag != null) {
							for (LifecycleConfiguration.Rule.Filter.And.Tag r : rl.filter.and.tag) {
								logger.info("rule.filter.and.tag.key : {}", r.key);
								logger.info("rule.filter.and.tag.value : {}", r.value);
							}
						}
					}

					// 음수값 체크
					if (!Strings.isNullOrEmpty(rl.filter.objectSizeGreaterThan)
							&& Long.parseLong(rl.filter.objectSizeGreaterThan) < 0) {
						logger.error(GWErrorCode.INVALID_ARGUMENT.getMessage());
						throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
					}

					// 음수값 체크
					if (!Strings.isNullOrEmpty(rl.filter.objectSizeLessThan)
							&& Long.parseLong(rl.filter.objectSizeLessThan) < 0) {
						logger.error(GWErrorCode.INVALID_ARGUMENT.getMessage());
						throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
					}
				}

				// transition 정합성 체크
				if (rl.transition != null) {
					// 음수값 체크 및 0 체크
					if (!Strings.isNullOrEmpty(rl.transition.days) && Long.parseLong(rl.transition.days) <= 0) {
						logger.error(GWErrorCode.INVALID_ARGUMENT.getMessage());
						throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
					}
				}

				// expiration 정합성 체크
				if (rl.expiration != null) {
					// 음수값 체크 및 0 체크
					if (!Strings.isNullOrEmpty(rl.expiration.days) && Long.parseLong(rl.expiration.days) <= 0) {
						logger.error(GWErrorCode.INVALID_ARGUMENT.getMessage());
						throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
					}
				}

				// non currentversion expiration 정합성 체크
				if (rl.versionexpiration != null) {
					// noncurrentversiontransition 정합성 체크, 0 체크
					if (!Strings.isNullOrEmpty(rl.versionexpiration.noncurrentDays)
							&& Long.parseLong(rl.versionexpiration.noncurrentDays) <= 0) {
						logger.error(GWErrorCode.INVALID_ARGUMENT.getMessage());
						throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
					}
				}

				// non currentversion transition 정합성 체크
				if (rl.versiontransition != null) {
					// noncurrentversiontransition 정합성 체크, 0 체크
					if (!Strings.isNullOrEmpty(rl.versiontransition.noncurrentDays)
							&& Long.parseLong(rl.versiontransition.noncurrentDays) <= 0) {
						logger.error(GWErrorCode.INVALID_ARGUMENT.getMessage());
						throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
					}
				}
			}

			if (lc.rules.size() > id.size()) {
				logger.error(GWErrorCode.INVALID_ARGUMENT.getMessage());
				throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
			}
		}
	}
}
