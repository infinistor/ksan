/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.KSAN.MQ.testcode;

import com.pspace.ifs.KSAN.MQ.MQCallback;
import com.pspace.ifs.KSAN.MQ.MQReceiver;
import com.pspace.ifs.KSAN.MQ.MQResponse;
import com.pspace.ifs.KSAN.MQ.MQResponseType;

/**
 *
 * @author legesse
 */

class DiskStartStopCallbackTest implements MQCallback{

    @Override
    public MQResponse call(String routingKey, String body) {
        System.out.format("BiningKey : %s body : %s\n", routingKey, body);
        return new MQResponse(MQResponseType.SUCCESS, "", "", 0);
    }    
}

public class TestReceiveDiskStartStop {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    { 
        String host = "192.168.11.76";
        String queueN = "diskControl";
        String message;
      
        try
        {
            MQCallback mq = new DiskStartStopCallbackTest();
            MQReceiver mq1to1 = new MQReceiver(host, queueN, false, mq);
            mq1to1.addCallback(mq);
        } catch (Exception ex){
             System.out.println(ex);
             System.out.println("--->Error : " + ex.getMessage() );
        }
    }
}
