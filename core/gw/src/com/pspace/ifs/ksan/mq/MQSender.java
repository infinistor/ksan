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
package com.pspace.ifs.ksan.mq;

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
    
    public String sendWithResponse(String mesg, String routingKey) throws Exception{
        return super.sendToExchangeWithResponse(mesg, routingKey);
    }
    
    public String sendWithResponse(String mesg) throws Exception{
        return super.sendToQueueWithResponse(mesg);
    }
}
