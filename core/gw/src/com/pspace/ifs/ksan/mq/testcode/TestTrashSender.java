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
package com.pspace.ifs.ksan.mq.testcode;

import java.util.Date;

import com.pspace.ifs.ksan.mq.*;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjManager;
/**
 *
 * @author legesse
 */
public class TestTrashSender {
    /**
     * @param args the command line arguments
     */
    
    public static void main(String[] args) {
        int i = 0;
       //String queueName = "osdThrashQueue";
        String exchange = "OSDExchange";
        String option = "";
        String msg;
        String fileName;
        Metadata mt;
        int idx = 0;
        
        try{ 
            MQSender mq1ton = new MQSender("192.168.11.76", exchange, option, "");
            ObjManager om = new ObjManager();
            while (true){
                Date dt = new Date();
                fileName = "testfile" + idx +".txt";
                mt =om.create("testv1", fileName);
                
                msg = "{id : "+dt.getTime()+" path: "+ fileName+" diskpath1 : "+ mt.getPrimaryDisk().getPath() +" diskpath2 : "+ mt.getReplicaDisk().getPath()+"}";
                mq1ton.send(msg, "");
                System.out.format("Sent Message (to %s) :-> %s\n", mq1ton.getExchangeName(), msg);
                Thread.sleep(10000);
                idx++;
            }
            
        } catch (Exception ex){
             System.out.println("--->Error : " + ex.getMessage() + " L. msg :" + ex.getLocalizedMessage());
        }
    }
}
