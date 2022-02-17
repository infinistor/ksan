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
