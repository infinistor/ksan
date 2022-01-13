/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.ksan.MQ.testcode;

import com.pspace.ifs.ksan.MQ.MQCallback;
import com.pspace.ifs.ksan.MQ.MQReceiver;
import com.pspace.ifs.ksan.MQ.MQResponse;
import com.pspace.ifs.ksan.MQ.MQResponseType;

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
