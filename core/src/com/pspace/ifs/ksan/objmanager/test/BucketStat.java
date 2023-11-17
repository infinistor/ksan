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
package com.pspace.ifs.ksan.objmanager.test;

import ch.qos.logback.classic.LoggerContext;
import com.pspace.ifs.ksan.objmanager.Bucket;
import com.pspace.ifs.ksan.objmanager.ObjManagerUtil;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 *
 * @author legesse
 */
public class BucketStat {
    static String bucketName;
    
    static void displaySatat() throws Exception{
        List<Bucket> blst;
        ObjManagerUtil omu = new ObjManagerUtil();
        System.out.format("%10.50s   %10.30s   %10.30s \n", "BucketName", "FileCounts",   "UsedSpace");
        
        if (bucketName.contains("ALL"))
            blst = omu.getBucketList();
        else{
            blst = new ArrayList();
            blst.add(omu.getBucket(bucketName));
        }
        
        for(Bucket bt :  blst){
            System.out.format("%10.50s   %30d   %30d \n", bt.getName(), bt.getFileCount(), bt.getUsedSpace());
        }
    }
    
    static void disableDebuglog(String driver){
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(driver);
        rootLogger.setLevel(ch.qos.logback.classic.Level.OFF);
    }
    
    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        disableDebuglog("org.mongodb.driver");
        disableDebuglog("com.pspace.ifs.ksan.objmanager");
        
        if (args.length == 0)
            bucketName = "ALL";
        else {
            bucketName=args[0];
        }
        
        System.out.println("Bucket Name : " + bucketName + " len " + args.length);
        
        displaySatat();
        System.exit(0);
    }
}
