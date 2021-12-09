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
package com.pspace.ifs.ksan.mq;

/**
 *
 * @author legesse
 */

public class MQSender extends MessageQ{
    private final boolean is1To1;
    public MQSender(String host, String qname, boolean qdurablity) throws Exception{
        super(host, qname, qdurablity);
        is1To1 = true;
    }
  
    public MQSender(String host, String exchangeName, String exchangeOption, String routingKey) 
            throws Exception{
        super(host, exchangeName, exchangeOption, routingKey);
        is1To1 = false;
    }
    
    public int send(String mesg) throws Exception {
        if (is1To1 == true)
            super.sendToQueue(mesg);
        else
            super.sendToExchange(mesg, "");
        return 0;
    } 
    
    public int send(String mesg, String routingKey) throws Exception{
        super.sendToExchange(mesg, routingKey);
        return 0;
    }
}