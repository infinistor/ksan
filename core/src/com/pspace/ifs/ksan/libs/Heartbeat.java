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
package com.pspace.ifs.ksan.libs;

import com.pspace.ifs.ksan.libs.mq.MQSender;
import org.json.simple.JSONObject;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class Heartbeat implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DiskManager.class);
    private String serviceId;
    private String mqHost;
    private int mqPort;
    private String mqUser;
    private String mqPassword;
    private String mqExchangeName;

    private final MQSender mqSender;

    public static final String EXCHANGE_OPTION = "direct";

    Heartbeat(String serviceId, String host, int port, String user, String password, String exchangeName) throws IOException, Exception {
        this.serviceId = serviceId;
        this.mqHost = host;
        this.mqPort = port;
        this.mqUser = user;
        this.mqPassword = password;
        this.mqExchangeName = exchangeName;

        mqSender = new MQSender(mqHost, mqPort, mqUser, mqPassword, mqExchangeName, EXCHANGE_OPTION, "");
    }

    @Override
    public void run() {
        try {
            JSONObject obj;
        
            obj = new JSONObject();
            obj.put(Constants.HEARTBEAT_ID, serviceId);
            obj.put(Constants.HEARTBEAT_STATE, Constants.HEARTBEAT_STATE_ONLINE);
            mqSender.send(obj.toString(), Constants.HEARTBEAT_BINDING_KEY);
        } catch (Exception e) {
            logger.error("Heartbeat error: {}", e.getMessage());
        }
    }
}
