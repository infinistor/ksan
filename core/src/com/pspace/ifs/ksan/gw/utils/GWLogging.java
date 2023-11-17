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
package com.pspace.ifs.ksan.gw.utils;

import java.util.Date;

import com.pspace.ifs.ksan.libs.config.AgentConfig;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.mq.MQSender;

import com.pspace.ifs.ksan.gw.mq.MessageQueueSender;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.google.common.base.Strings;

public class GWLogging {
    private static final Logger logger = LoggerFactory.getLogger(GWLogging.class);
    private MessageQueueSender MQS;
    // private static MQSender mqSender;

    public static GWLogging getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static final GWLogging INSTANCE = new GWLogging();
    }

    private GWLogging() {
        try {
            MQS = new MessageQueueSender(AgentConfig.getInstance().getMQHost(),
                Integer.parseInt(AgentConfig.getInstance().getMQPort()),
                AgentConfig.getInstance().getMQUser(),
                AgentConfig.getInstance().getMQPassword(),
                GWConstants.MQUEUE_LOG_EXCHANGE_NAME,
                GWConstants.MESSAGE_QUEUE_OPTION_DIRECT,
                GWConstants.MQUEUE_NAME_GW_LOG_ADD,
                20);
        } catch (Exception e) {
            PrintStack.logging(logger, e);
        }
    }

    public void sendLog(S3Parameter s3Parameter) {
        JSONObject object = new JSONObject();

        // bucket owner name
        if (s3Parameter.getBucket() != null && !Strings.isNullOrEmpty(s3Parameter.getBucket().getUserName())) {
            object.put(GWConstants.GW_LOG_USER_NAME, s3Parameter.getBucket().getUserName());
        } else {
            object.put(GWConstants.GW_LOG_USER_NAME, GWConstants.EMPTY_STRING);
        }

        // bucket name
        if (s3Parameter.getBucketName() != null && !Strings.isNullOrEmpty(s3Parameter.getBucketName())) {
            object.put(GWConstants.GW_LOG_BUCKET_NAME, s3Parameter.getBucketName());
        } else {
            object.put(GWConstants.GW_LOG_BUCKET_NAME, GWConstants.EMPTY_STRING);
        }

        // date
        Date date = new Date();
        object.put(GWConstants.GW_LOG_DATE, date.toString());

        // remote host
        if (!Strings.isNullOrEmpty(s3Parameter.getRemoteHost())) {
            object.put(GWConstants.GW_LOG_REMOTE_HOST, s3Parameter.getRemoteHost());
        } else {
            object.put(GWConstants.GW_LOG_REMOTE_HOST, GWConstants.EMPTY_STRING);
        }

        // request user
        if (s3Parameter.getUser() != null && !Strings.isNullOrEmpty(s3Parameter.getUser().getUserName())) {
            object.put(GWConstants.GW_LOG_REQUEST_USER, s3Parameter.getUser().getUserName());
        } else {
            object.put(GWConstants.GW_LOG_REQUEST_USER, GWConstants.EMPTY_STRING);
        }

        // request id
        if (!Strings.isNullOrEmpty(s3Parameter.getRequestID())) {
            object.put(GWConstants.GW_LOG_REQUEST_ID, String.valueOf(s3Parameter.getRequestID()));
        } else {
            object.put(GWConstants.GW_LOG_REQUEST_ID, GWConstants.EMPTY_STRING);
        }

        // operation
        if (!Strings.isNullOrEmpty(s3Parameter.getOperation())) {
            object.put(GWConstants.GW_LOG_OPERATION, s3Parameter.getOperation());
        } else {
            object.put(GWConstants.GW_LOG_OPERATION, GWConstants.EMPTY_STRING);
        }

        // object name
        if (!Strings.isNullOrEmpty(s3Parameter.getObjectName())) {
            object.put(GWConstants.GW_LOG_OBJECT_NAME, s3Parameter.getObjectName());
        } else {
            object.put(GWConstants.GW_LOG_OBJECT_NAME, GWConstants.EMPTY_STRING);
        }

        // request uri
        if (!Strings.isNullOrEmpty(s3Parameter.getRequestURI())) {
			object.put(GWConstants.GW_LOG_REQUEST_URI, s3Parameter.getRequestURI());
        } else {
            object.put(GWConstants.GW_LOG_REQUEST_URI, GWConstants.EMPTY_STRING);
        }

        // reponse status code
        object.put(GWConstants.GW_LOG_STATUS_CODE, s3Parameter.getStatusCode());

        // response error code
        if (!Strings.isNullOrEmpty(s3Parameter.getErrorCode())) {
			object.put(GWConstants.GW_LOG_ERROR_CODE, s3Parameter.getErrorCode());
        } else {
            object.put(GWConstants.GW_LOG_ERROR_CODE, GWConstants.EMPTY_STRING);
        }

        // response length
        object.put(GWConstants.GW_LOG_RESPONSE_LENGTH, s3Parameter.getResponseSize());

		// object length
		if (s3Parameter.getFileSize() > 0) {
			object.put(GWConstants.GW_LOG_OBJECT_LENGTH, s3Parameter.getFileSize());
		} else {
			object.put(GWConstants.GW_LOG_OBJECT_LENGTH, 0L);
		}

        // total time
        object.put(GWConstants.GW_LOG_TOTAL_TIME, System.currentTimeMillis() - s3Parameter.getStartTime());

        // request length
        object.put(GWConstants.GW_LOG_REQUEST_LENGTH, s3Parameter.getRequestSize());

        // referer
        if (!Strings.isNullOrEmpty(s3Parameter.getReferer())) {
			object.put(GWConstants.GW_LOG_REFERER, s3Parameter.getReferer());
        } else {
            object.put(GWConstants.GW_LOG_REFERER, GWConstants.EMPTY_STRING);
        }

        // User Agent
        if (!Strings.isNullOrEmpty(s3Parameter.getUserAgent())) {
			object.put(GWConstants.GW_LOG_USER_AGENT, s3Parameter.getUserAgent());
        } else {
            object.put(GWConstants.GW_LOG_USER_AGENT, GWConstants.EMPTY_STRING);
        }

        // Version id
        if (!Strings.isNullOrEmpty(s3Parameter.getVersionId())) {
			object.put(GWConstants.GW_LOG_VERSION_ID, s3Parameter.getVersionId());
        } else {
            object.put(GWConstants.GW_LOG_VERSION_ID, GWConstants.EMPTY_STRING);
        }

        // Host ID
        if (!Strings.isNullOrEmpty(s3Parameter.getHostID())) {
			object.put(GWConstants.GW_LOG_HOST_ID, s3Parameter.getHostID());
        } else {
            object.put(GWConstants.GW_LOG_HOST_ID, GWConstants.EMPTY_STRING);
        }

        // Sign Version
        if (!Strings.isNullOrEmpty(s3Parameter.getSignVersion())) {
			object.put(GWConstants.GW_LOG_SIGN, s3Parameter.getSignVersion());
        } else {
            object.put(GWConstants.GW_LOG_SIGN, GWConstants.EMPTY_STRING);
        }

        // ssl_group
        object.put(GWConstants.GW_LOG_SSL_GROUP, GWConstants.EMPTY_STRING);

        // sign type
        if (!Strings.isNullOrEmpty(s3Parameter.getAuthorization())) {
			object.put(GWConstants.GW_LOG_SIGN_TYPE, GWConstants.AUTH_HEADER);
        } else if (!Strings.isNullOrEmpty(s3Parameter.getxAmzAlgorithm())) {
			object.put(GWConstants.GW_LOG_SIGN_TYPE, GWConstants.QUERY_STRING);
		} else {
            object.put(GWConstants.GW_LOG_SIGN_TYPE, GWConstants.EMPTY_STRING);
        }

        // endpoint
        if (!Strings.isNullOrEmpty(s3Parameter.getHostName())) {
			object.put(GWConstants.GW_LOG_END_POINT, s3Parameter.getHostName());
        } else {
            object.put(GWConstants.GW_LOG_END_POINT, GWConstants.EMPTY_STRING);
        }

        // tls version
        object.put(GWConstants.GW_LOG_TLS_VERSION, GWConstants.EMPTY_STRING);

        logger.debug("log - {}", object.toString());
        try {
            MQS.send(object.toString(), GWConstants.MQUEUE_NAME_GW_LOG_ADD);
        } catch (Exception e) {
            PrintStack.logging(logger, e);
        }
    }
}
