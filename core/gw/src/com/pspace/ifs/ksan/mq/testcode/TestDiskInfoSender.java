/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.KSAN.MQ.testcode;

import java.io.File;
import java.util.Date;

import com.pspace.ifs.KSAN.MQ.MQSender;

/**
 *
 * @author legesse
 */
public class TestDiskInfoSender {
    private static String getDiskInfo(String diskpath)
    {
        String message;
        File file = new File(diskpath);
        Date dt = new Date();
        message = String.format("{ path : %s total : %d bytes free : %d bytes usable : %d bytes timestamp : %d}", 
                diskpath, file.getTotalSpace(), file.getFreeSpace(), file.getUsableSpace(), dt.getTime());
        return message;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    { 
        String host = "192.168.11.76";
        String exchange = "diskExchange";
        String option ="fanout";
        String message;
      
        try
        { 
            MQSender mq1ton = new MQSender(host, exchange, option, "");
            while(true){
                message = getDiskInfo("/DATA");
                mq1ton.send(message);
                System.out.println(message);
                Thread.sleep(10000);
            }
        } catch (Exception ex){
            System.out.println(ex);
        }
    }
}
