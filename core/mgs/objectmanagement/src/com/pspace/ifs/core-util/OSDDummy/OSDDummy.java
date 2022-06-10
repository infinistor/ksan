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
package com.pspace.ifs.ksan.utility.OSDDummy;

import com.pspace.ifs.ksan.libs.mq.MQCallback;
import com.pspace.ifs.ksan.libs.mq.MQReceiver;
import com.pspace.ifs.ksan.libs.mq.MQResponse;
import com.pspace.ifs.ksan.libs.mq.MQResponseType;

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
        
        private String getMD5(){
            return "";
        }
        
        private int copyObject(){
            return 0;
        }
        
        private int removeObject(){
            return 0;
        }
}

public class OSDDummy {
   
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String exchange = "OSDExchange";
        String queueName = "osdThrashQueue";
        String host = "192.168.11.76";
        String option = "";   
        MQReceiver mq1ton;  
        OsdReceiverCallback callback;
        
        try{
            callback = new OsdReceiverCallback();
            mq1ton = new MQReceiver(host, queueName, exchange, false, option, "", callback);
   
        } catch (Exception ex){
             System.out.println("--->Error : " + ex.getMessage() + " L. msg :" + ex.getLocalizedMessage());
        }
    }
    
}
