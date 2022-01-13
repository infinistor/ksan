/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.ksan.MQ.testcode;

//import java.util.Random;
import java.util.Date;

import com.pspace.ifs.ksan.MQ.*;

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
