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

import java.util.Date;

import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjManager;
import com.pspace.ifs.ksan.mq.*;
/**
 *
 * @author legesse
 */
public class TestTrashSender {
    /**
     * @param args the command line arguments
     */
    
    public static void main(String[] args) {
        int i = 0;
       //String queueName = "osdThrashQueue";
        String exchange = "OSDExchange";
        String option = "";
        String msg;
        String fileName;
        Metadata mt;
        int idx = 0;
        
        try{ 
            MQSender mq1ton = new MQSender("192.168.11.76", exchange, option, "");
            ObjManager om = new ObjManager();
            while (true){
                Date dt = new Date();
                fileName = "testfile" + idx +".txt";
                mt =om.create("testv1", fileName);
                
                msg = "{id : "+dt.getTime()+" path: "+ fileName+" diskpath1 : "+ mt.getPrimaryDisk().getPath() +" diskpath2 : "+ mt.getReplicaDisk().getPath()+"}";
                mq1ton.send(msg, "");
                System.out.format("Sent Message (to %s) :-> %s\n", mq1ton.getExchangeName(), msg);
                Thread.sleep(10000);
                idx++;
            }
            
        } catch (Exception ex){
             System.out.println("--->Error : " + ex.getMessage() + " L. msg :" + ex.getLocalizedMessage());
        }
    }
}
