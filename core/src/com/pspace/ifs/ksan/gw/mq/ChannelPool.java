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

import java.time.Duration;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.ifs.ksan.libs.PrintStack;
import com.rabbitmq.client.Channel;

public class ChannelPool {
    private final static Logger logger = LoggerFactory.getLogger(ChannelFactory.class);
    private GenericObjectPool<Channel> internalPool;
    

    public ChannelPool(String host, int port, String username, String password, String exchangeName, String exchangeOption, String routingKey) {
        this(new ChannelFactory(host, port, username, password, exchangeName, exchangeOption, routingKey));
    }

    public ChannelPool(ChannelFactory factory) {
        if (this.internalPool != null) {
            try {
                closeInternalPool();
            } catch (Exception e) {
                PrintStack.logging(logger, e);
            }
        }

        this.internalPool = new GenericObjectPool<Channel>(factory);
        this.internalPool.setMaxTotal(10);
        this.internalPool.setBlockWhenExhausted(true);
        this.internalPool.setMaxWait(Duration.ofSeconds(30));
    }

    private void closeInternalPool() {
        try {
            internalPool.close();
        } catch (Exception e) {
            PrintStack.logging(logger, e);
        }
    }

    public void returnChannel(Channel channel) {
        try {
            if (channel.isOpen()) {
                internalPool.returnObject(channel);
            } else {
                internalPool.invalidateObject(channel);
            }
        } catch (Exception e) {
            PrintStack.logging(logger, e);
        }
    }

    public Channel getChannel() {
        try {
            return internalPool.borrowObject();
        } catch (Exception e) {
            PrintStack.logging(logger, e);
        }

        return null;
    }
}

