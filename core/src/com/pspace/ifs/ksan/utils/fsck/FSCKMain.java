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

package com.pspace.ifs.ksan.utils.fsck;

import ch.qos.logback.classic.LoggerContext;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.LoggerFactory;
//import static org.kohsuke.args4j.ExampleMode.ALL;

/**
 *
 * @author legesse
 */
public class FSCKMain {
       
    @Option(name="--BucketName", usage="Specify the name of the bucket you wish to fix or check")
    private String bucketName = "";
    
    @Option(name="--DiskName", usage="Specify the disk Name you wish to fix or check")
    private String diskName="";
    
    @Option(name="--CheckOnly",usage="Specify if you wish only to check not to fix")
    private boolean checkOnly = false;
    
    @Option(name="--Help",usage="To display this help menu")
    public boolean getHelp = false;
    
    @Option(name="--Debug", usage="To enable display debug log")
    public boolean debugOn = false;
    
    private CmdLineParser parser;
    
    int parseArgs(String[] args){
        parser = new CmdLineParser(this);
        try{
             parser.parseArgument(args);
        } catch( CmdLineException ex ) {
           System.err.println(ex.getMessage());
           System.err.format(" %s [--CheckOnly] --BucketName <bucket Name> --Diskid <osd disk id>\n", getProgramName());
           return -1;
        }
        
        if (getHelp)
           return 0;
        
        if (!bucketName.isEmpty() || !diskName.isEmpty())
            return 0;
        
        if (debugOn)
           System.out.format(" BucketName : %s Diskid : %s Checkonly : %s \n", bucketName, diskName, checkOnly);
        
        System.err.format("Invalid argument is given \n");
        return -1;
    }
    
     void runInBackground(){
        long job_done = 0;
        String diskId;
        
        try {
            FSCK ofsk = new FSCK(checkOnly);
            diskId = ofsk.getDiskId(diskName);
            ofsk.setDebugModeOn(debugOn);
            
            if (!bucketName.isEmpty() && !diskName.isEmpty())
                job_done = ofsk.checkBucketDisk(bucketName, diskId);
            else if (!bucketName.isEmpty())
                job_done = ofsk.checkEachDisk(bucketName);
            else if (!diskName.isEmpty())
                job_done = ofsk.checkEachOneDiskAllBucket(diskId);
            else {
                System.err.format("Not supported yet!\n");
                return;
            }
                
            ofsk.getResultSummary();
        } catch (ResourceNotFoundException ex){
            System.err.format("Disk Name (%s) not found in the system!\n", diskName);
            return;
        } catch (Exception ex) { 
            ex.printStackTrace();
            Logger.getLogger(FSCKMain.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            return;
        }
        
        System.out.println("DONE!");    
    }
    
    static String getProgramName(){
        return new File(FSCKMain.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName().replaceFirst("[.][^.]+$", "");
    }
    
    void howToUse(){
        if (parser == null)
            return;
        
        parser.printUsage(System.err);
        System.err.println();
        System.err.format("  Example:  %s --BucketName bucket1 \n", getProgramName());
        System.err.format("  Example:  %s --BucketName bucket1 --CheckOnly \n", getProgramName());
        System.err.format("  Example:  %s --DiskId disk111 \n", getProgramName());
        System.err.format("  Example:  %s --DiskId disk111 --CheckOnly \n", getProgramName());
        System.err.println();
    }
    
    static void disableDebuglog(String driver){
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(driver);
        rootLogger.setLevel(ch.qos.logback.classic.Level.OFF);
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        disableDebuglog("org.mongodb.driver");
        disableDebuglog("com.pspace.ifs.ksan.objmanager");
        
        FSCKMain fmain = new FSCKMain();
        if (fmain.parseArgs(args) != 0){
            fmain.howToUse();
          return;
        }
        
        if (fmain.getHelp){
            fmain.howToUse();
          return;
        }
        
        new Thread(()->{
            fmain.runInBackground();
            System.exit(0);
        }).start();
    }
}
