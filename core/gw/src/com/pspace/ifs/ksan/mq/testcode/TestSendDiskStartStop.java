/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.ksan.mq.testcode;

import java.io.File;
import java.util.Date;

import com.pspace.ifs.ksan.mq.MQSender;

import org.json.simple.JSONObject;

/**
 *
 * @author legesse
 */
public class TestSendDiskStartStop {
    private static String getDiskInfo(String diskpath, String action)
    {
        JSONObject obj = new JSONObject();
        File file = new File(diskpath);
        Date dt = new Date();
        
        obj.put("path", diskpath);
        obj.put("TotalSize", file.getTotalSpace());
        obj.put("FreeSpace", file.getFreeSpace());
        obj.put("UsedSpace", file.getUsableSpace());
        obj.put("Timestamp", dt.getTime());
        obj.put("Action", action);
        
        return obj.toString();
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    { 
        String host = "192.168.11.76";
        String queueName = "diskControl";
        String action = "START";
        String message;
      
        try
        { 
            MQSender mq1to1 = new MQSender(host, queueName, false);
            while(true){
                message = getDiskInfo("/DATA", action);
                mq1to1.send(message);
                System.out.println(message);
                Thread.sleep(10000);
                if (action.contains("START"))
                    action = "STOP";
                else
                     action = "START";
            }
        } catch (Exception ex){
            System.out.println(ex);
        }
    }
}
