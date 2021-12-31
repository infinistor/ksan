/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.KSAN.MQ.testcode;

import java.util.Date;

import com.pspace.ifs.KSAN.MQ.*;
import com.pspace.ifs.KSAN.ObjManger.Metadata;
import com.pspace.ifs.KSAN.ObjManger.ObjManager;
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
