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

import java.io.File;
import java.util.Date;

import com.pspace.ifs.ksan.mq.MQSender;

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
