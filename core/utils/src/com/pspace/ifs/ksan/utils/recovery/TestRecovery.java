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
package com.pspace.ifs.ksan.utils.recovery;

import com.pspace.ifs.ksan.mq.MQSender;

import org.json.simple.JSONObject;

/**
 *
 * @author legesse
 */
public class TestRecovery {
    
    static String getDataToSend(){
        JSONObject JO = new JSONObject();
        JO.put("bucket",   "testbucket1");
        JO.put("diskId",  "1");
        JO.put("objId",    "dadadadadasdasdas_" + System.nanoTime());
        JO.put("versionId",  "null");
        return JO.toJSONString();
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String host = "192.168.11.76";
        String exchange = "UtilityExchange";
        String option ="fanout";
        String bindingKey = "*.utility.recovery.*";
        String message;
      
        try
        { 
            MQSender mq1ton = new MQSender(host, exchange, option, bindingKey);
            while(true){
                message = getDataToSend();
                mq1ton.send(message);
                System.out.println(message);
                Thread.sleep(10000);
            }
        } catch (Exception ex){
            System.out.println(ex);
        }
        
    }
    
}
