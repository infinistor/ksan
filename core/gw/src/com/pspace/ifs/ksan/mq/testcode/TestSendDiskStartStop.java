/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
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