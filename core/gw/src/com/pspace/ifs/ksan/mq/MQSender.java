/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.KSAN.MQ;

/**
 *
 * @author legesse
 */

public class MQSender extends MessageQ{
    private final boolean is1To1;
    public MQSender(String host, String qname, boolean qdurablity) throws Exception{
        super(host, qname, qdurablity);
        is1To1 = true;
    }
  
    public MQSender(String host, String exchangeName, String exchangeOption, String routingKey) 
            throws Exception{
        super(host, exchangeName, exchangeOption, routingKey);
        is1To1 = false;
    }
    
    public int send(String mesg) throws Exception {
        if (is1To1 == true)
            super.sendToQueue(mesg);
        else
            super.sendToExchange(mesg, "");
        return 0;
    } 
    
    public int send(String mesg, String routingKey) throws Exception{
        super.sendToExchange(mesg, routingKey);
        return 0;
    }
}