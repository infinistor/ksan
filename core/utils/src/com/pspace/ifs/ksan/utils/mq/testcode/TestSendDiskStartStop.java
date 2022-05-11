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
package com.pspace.ifs.ksan.utils.mq.testcode;

import java.io.File;
import java.util.Date;

import com.pspace.ifs.ksan.utils.mq.MQSender;

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
