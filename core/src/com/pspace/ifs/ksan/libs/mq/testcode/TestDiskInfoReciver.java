/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version
* 3 of the License. See LICENSE for details
*
* All materials such as this program, related source codes, and documents are provided as they are.
* Developers and developers of the KSAN project are not responsible for the results of using this program.
* The KSAN development team has the right to change the LICENSE method for all outcomes related to KSAN development without prior notice, permission, or consent.
*/
package com.pspace.ifs.ksan.libs.mq.testcode;

import com.pspace.ifs.ksan.libs.mq.MQCallback;
import com.pspace.ifs.ksan.libs.mq.MQReceiver;
import com.pspace.ifs.ksan.libs.mq.MQResponse;
import com.pspace.ifs.ksan.libs.mq.MQResponseType;

/**
 *
 * @author legesse
 */

class CallbackTest implements MQCallback{

    @Override
    public MQResponse call(String routingKey, String body) {
        System.out.format("BiningKey : %s body : %s\n", routingKey, body);
        return new MQResponse(MQResponseType.SUCCESS, "", "", 0);
    }    
}

public class TestDiskInfoReciver {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    { 
        String host = "192.168.11.76";
        String queueN = "diskQueue1";//"disk";
        String exchange = "diskExchange"; //"disk"; 
        String option ="fanout";//"topic";
        String message;
      
        try
        {
            MQCallback mq = new CallbackTest();
            MQReceiver mq1ton = new MQReceiver(host, queueN, exchange, false, option, "*.services.disks", mq);
            mq1ton.addCallback(mq);
            /*(while(true){
                message = mq1ton.get();
                System.out.println(message);
                Thread.sleep(10000);
            }*/
        } catch (Exception ex){
             System.out.println(ex);
             System.out.println("--->Error : " + ex.getMessage() );
        }
    }
}
