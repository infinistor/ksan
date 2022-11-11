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
package com.pspace.ifs.ksan.gw.mq;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.ifs.ksan.libs.PrintStack;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class ChannelFactory implements PooledObjectFactory<Channel> {
    private String host;
    private int port;
    private String username;
    private String password;
    private String exchangeName;
    private String exchangeOption;
    private String routingKey;

    private final static Logger logger = LoggerFactory.getLogger(ChannelFactory.class);

    public ChannelFactory(String host, int port, String username, String password, String exchangeName, String exchangeOption, String routingKey) {
        try {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
            this.exchangeName = exchangeName;;
            this.exchangeOption = exchangeOption;
            this.routingKey = routingKey;
        } catch (Exception e) {
            PrintStack.logging(logger, e);
        }
    }

    public PooledObject<Channel> makeObject() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setPort(port);
            factory.setAutomaticRecoveryEnabled(true);
            factory.setNetworkRecoveryInterval(10000);
            factory.setConnectionTimeout(10000);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.basicQos(0);
        return new DefaultPooledObject<Channel>(channel);
    }

    public void destroyObject(PooledObject<Channel> pooledObject) throws Exception {
        final Channel channel = pooledObject.getObject();
        if (channel.isOpen()) {
            try {
                channel.close();
            } catch (Exception e) {
            }
        }
    }

    public boolean validateObject(PooledObject<Channel> pooledObject) {
        final Channel channel = pooledObject.getObject();
        return channel.isOpen();
    }

    public void activateObject(PooledObject<Channel> pooledObject) throws Exception {

    }

    public void passivateObject(PooledObject<Channel> pooledObject) throws Exception {

    }
}