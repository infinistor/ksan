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
package com.pspace.ifs.ksan.mq.testcode;

//import java.util.Random;
import java.util.Date;

import com.pspace.ifs.ksan.mq.*;

/**
 *
 * @author legesse
 */
class OsdReceiverCallback implements MQCallback{

    @Override
    public MQResponse call(String routingKey, String body) {
        System.out.format("BiningKey : %s body : %s\n", routingKey, body);
        return new MQResponse(MQResponseType.SUCCESS, "", "", 0);
    }    
}

public class TestOsdReciver {
    /**
     * @param args the command line arguments
     */
    
    public static void main(String[] args) {
        int i = 0;
        String exchange = "OSDExchange";
        String option = "";
        String msg;
        int n;
        
        Date dt = new Date();
        n = 1;//(int)(dt.getTime() % 3);
        String queueName = "osdThrashQueue" + n;
        try{
            OsdReceiverCallback callback = new OsdReceiverCallback();
            MQReceiver mq1ton = new MQReceiver("192.168.11.76", queueName, exchange, false, option, "", callback);  
   
        } catch (Exception ex){
             System.out.println("--->Error : " + ex.getMessage() + " L. msg :" + ex.getLocalizedMessage());
        }
    }
}
