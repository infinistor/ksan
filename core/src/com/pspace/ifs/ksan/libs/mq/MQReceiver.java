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
package com.pspace.ifs.ksan.libs.mq;

/**
 *
 * @author legesse
 */

public class MQReceiver extends MessageQ{
    public MQReceiver(String host, String qname, boolean qdurablity, MQCallback callback) throws Exception{
        super(host, qname, qdurablity, callback);
    }
    
    public MQReceiver(String host, String qname, String exchangeName, boolean singleActiveConsumer, String exchangeOption, String routingKey, MQCallback callback) 
            throws Exception{
        super(host, qname, exchangeName, singleActiveConsumer, exchangeOption, routingKey, callback);

    }
    
    public MQReceiver(String host, int port, String username, String password, String qname, String exchangeName, boolean singleActiveConsumer, String exchangeOption, String routingKey, MQCallback callback) 
            throws Exception{
        super(host, port, username, password, qname, exchangeName, singleActiveConsumer, exchangeOption, routingKey, callback);

    }
}
