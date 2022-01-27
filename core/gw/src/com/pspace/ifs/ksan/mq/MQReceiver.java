/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.ksan.mq;

/**
 *
 * @author legesse
 */

public class MQReceiver extends MessageQ{
    public MQReceiver(String host, String qname, boolean qdurablity, MQCallback callback) throws Exception{
        super(host, qname, qdurablity, callback);
    }
    
    /* public MessageQ(String host, String qname, String exchangeName, boolean qdurablity, 
            String exchangeOption, String routingKey, MQCallback callback) throws Exception*/
    public MQReceiver(String host, String qname, String exchangeName, boolean queeuDurableity, String exchangeOption, String routingKey, MQCallback callback) 
            throws Exception{
        super(host, qname, exchangeName, queeuDurableity, exchangeOption, routingKey, callback);
    }
}
