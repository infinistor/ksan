/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.ksan.util.recovery;

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
        JO.put("pdiskId",  "1");
        JO.put("objId",    "dadadadadasdasdas");
        JO.put("fdiskId",  "2");
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
