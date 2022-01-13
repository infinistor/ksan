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

class OSDUnlinkCallback implements MQCallback{

    @Override
    public MQResponse call(String routingKey, String body) {
        System.out.format("BiningKey : %s body : %s\n", routingKey, body);
        
        // delete the file here
        return new MQResponse(MQResponseType.SUCCESS, "", "", 0);
    }    
}

public class OSDUnlink {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    { 
        String host = "192.168.11.76";
        String queueN = "diskQueue4";
        String exchange = "OSDExchange"; 
        String option ="fanout";
        String bindingKey = "*.servers.unlink.4";
      
        try
        {
            System.out.println(">>Test1");
            MQCallback mq = new OSDUnlinkCallback();
            System.out.println(">>Test2");
            MQReceiver mq1ton = new MQReceiver(host, queueN, exchange, false, option, bindingKey, mq);
            System.out.println(">>Test3");
            //mq1ton.addCallback(mq);
  
        } catch (Exception ex){
             System.out.println(ex);
             System.out.println("--->Error : " + ex.getMessage() );
        }
    }
}
