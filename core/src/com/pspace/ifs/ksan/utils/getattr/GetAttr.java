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

package com.pspace.ifs.ksan.utils.getattr;

import ch.qos.logback.classic.LoggerContext;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.OSDClient;
import com.pspace.ifs.ksan.objmanager.OSDResponseParser;
import com.pspace.ifs.ksan.objmanager.ObjManagerUtil;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author legesse
 */

public class GetAttr {
    
    @Option(name="--BucketName", usage="Specify the name of the bucket")
    private  String bucketName = "";
    
    @Option(name="--Key", usage="Specify the object key")
    private String ObjectPath = "";
    
    @Option(name="--ObjId", usage="Specify the object Id if you with to display with Id rather than object key")
    private String objId = "";
    
    @Option(name="--VersionId", usage="Specify the object version Id if you wish particular version of an object")
    private String versionId ="null";
    
    @Option(name="--Checksum", usage="To display the checksum and size of the object from OSD")
    private boolean checksum = false;
     
   // @Option(name="--isBucket", usage="set it if you wish to display the attribute of a bucket")
    private boolean isFile = true;
    
    @Option(name="--Help",usage="To display this help menu")
    public boolean getHelp = false;
    
    private CmdLineParser parser;
     
    private OSDResponseParser primaryOSD;
    
    private OSDResponseParser secondOSD;
    
    private String [] makeCaseInsensitive(String[] args){
        String prefx;
        List<String> refArgs = new ArrayList();
        for(String opt : args){
            prefx = opt.split(" ")[0].toLowerCase();
            if (prefx.equalsIgnoreCase("--BucketName")){
               refArgs.add(opt.replaceFirst(prefx, "--BucketName"));
            }
            else if (prefx.equalsIgnoreCase("--Key")){
               refArgs.add(opt.replaceFirst(prefx, "--Key"));
            }
            else if (prefx.equalsIgnoreCase("--ObjId")){
               refArgs.add(opt.replaceFirst(prefx, "--ObjId"));
            }
            else if (prefx.equalsIgnoreCase("--VersionId")){
               refArgs.add(opt.replaceFirst(prefx, "--VersionId"));
            }
            
            else if (prefx.equalsIgnoreCase("--Checksum")){
               refArgs.add(opt.replaceFirst(prefx, "--Checksum"));
            }
            
            else if (prefx.equalsIgnoreCase("--Help")){
               refArgs.add(opt.replaceFirst(prefx, "--Help"));
            }
            else
                refArgs.add(opt);
        }
        return refArgs.toArray(new String[0]);
    }
    
    int parseArgs(String[] args){
        parser = new CmdLineParser(this);
        try{
            String []cisArgs = makeCaseInsensitive(args);
            parser.parseArgument(cisArgs);
 
        } catch( CmdLineException ex ) {
           System.err.println(ex.getMessage());
           System.err.format("%s --BucketName <bucket Name> [--Key <object path> |--ObjId <object id>] [--VersionId <versionId>] --Checksum\n", getProgramName());
           return -1;
        }
        
        if (getHelp)
           return 0;
        
        if (!bucketName.isEmpty()){
            if (ObjectPath.isEmpty() || objId.isEmpty())
                return 0;
        }
       
        System.err.format("Invalid argument is given \n");
        return -1;
    }

    static String getProgramName(){
        return new File(GetAttr.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath())
                .getName().replaceFirst("[.][^.]+$", "").replaceFirst("-jar-with-dependencies", "");
    }
     
    void howToUse(){
        if (parser == null)
            return;
        
        parser.printUsage(System.err);
        System.err.println();
        System.err.format("  Example: %s --BucketName bucket1 --Key file1.txt \n", getProgramName());
        System.err.format("  Example: %s --BucketName bucket1 --Key file1.txt --Checksum \n", getProgramName());
        System.err.format("  Example: %s --BucketName bucket1 --Key file1.txt --VersionId 526554498818254 \n", getProgramName());
        System.err.format("  Example: %s --BucketName bucket1 --ObjId bd01856bfd2065d0d1ee20c03bd3a9af \n", getProgramName());
        System.err.format("  Example: %s --BucketName bucket1 --ObjId bd01856bfd2065d0d1ee20c03bd3a9af --VersionId 526554498818254 \n", getProgramName());
        System.err.println();
    }
     
    void displayMeta(Metadata mt){
        String RESET  = "\u001B[0m";
        String GREEN  = "\u001B[32m";
        String RED    = "\u001B[31m";
        String YELLOW = "\u001B[33m";
        String BLUE   = "\u001B[34m";
        
        String dskMsg;
        String osdMsg;
        String replicaOSDIP;
        
        try {
            replicaOSDIP= mt.getReplicaDisk().getOsdIp();
            dskMsg = String.format(" PrimaryDisk>  diskId : %-37s(%-16s)  diskPath : %s (%s%7s%s)\n  ReplicaDisk>  diskId : %-37s(%-16s)  diskPath : %s (%s%7s%s)",
                    mt.getPrimaryDisk().getId(), 
                    mt.getPrimaryDisk().getOsdIp(),
                    mt.getPrimaryDisk().getPath(), GREEN,
                    mt.getPrimaryDisk().getStatus(), RESET,
                    mt.getReplicaDisk().getId(), 
                    replicaOSDIP,
                    mt.getReplicaDisk().getPath(), GREEN,
                    mt.getReplicaDisk().getStatus(), RESET);
        } catch (ResourceNotFoundException ex) {
            dskMsg = String.format(" PrimaryDisk>  diskId : %-37s(%-16s)  diskPath : %s (%s%s%s)\n",
                    mt.getPrimaryDisk().getId(), 
                    mt.getPrimaryDisk().getOsdIp(),
                    mt.getPrimaryDisk().getPath(), GREEN,
                    mt.getPrimaryDisk().getStatus(), RESET);
                    replicaOSDIP = "";
        }
        
        if (checksum){
            if (mt.isReplicaExist())
                osdMsg = String.format("\nOSDInfo :\n PrimaaryOSD (%-16s) >  MD5 : %-40s  Size : %d \n ReplicaOSD  (%-16s) >  MD5 : %-40s  Size : %d \n ", 
                        mt.getPrimaryDisk().getOsdIp(), primaryOSD.md5, primaryOSD.size, replicaOSDIP, secondOSD.md5, secondOSD.size);
            else
                osdMsg = String.format("\nOSDInfo :\nPrimaaryOSD >  MD5 : %s  Size : %d \n ", primaryOSD.md5, primaryOSD.size);
        }
        else
           osdMsg =""; 
        
        System.out.format("\n bucketName : %s \n ObjectKey  : %s \n objId      : %s \n VersionId  : %s \n Size       : %d  \n NumReplica   : %d"
               + "\n %s \n %s"
               , bucketName, mt.getPath(), mt.getObjId(), mt.getVersionId(), mt.getSize(), mt.getReplicaCount(), dskMsg, osdMsg);
        System.out.println();
    }
    
    void displayNothing(){
        System.out.println("No Information avaliable!\n");
    }
    
    void getOSDObjectAttr(Metadata mt) throws Exception{
        if (!checksum)
            return;
       
        String res;
        OSDClient osdc = new OSDClient();
        primaryOSD = osdc.getObjectAttr(bucketName, mt.getObjId(), mt.getVersionId(), mt.getPrimaryDisk().getId(), mt.getPrimaryDisk().getPath(), mt.getPrimaryDisk().getOSDServerId());
        //primaryOSD = new OSDResponseParser(res);
        if (mt.isReplicaExist()){
            secondOSD = osdc.getObjectAttr(bucketName, mt.getObjId(), mt.getVersionId(), mt.getReplicaDisk().getId(), mt.getReplicaDisk().getPath(), mt.getReplicaDisk().getOSDServerId());
            //secondOSD = new OSDResponseParser(res);
        }
    }
    
    void getObjects(){
        Metadata mt;
        
        try {
            ObjManagerUtil obmu = new ObjManagerUtil();
            if (!ObjectPath.isEmpty())
                mt = obmu.getObjectWithPath(bucketName, ObjectPath, versionId);
            else
                mt = obmu.getObject(bucketName, objId, versionId);
            
            getOSDObjectAttr(mt);
            
            displayMeta(mt);
        } catch (ResourceNotFoundException ex) {
            displayNothing();
        } catch (Exception ex) {
            Logger.getLogger(GetAttr.class.getName()).log(Level.SEVERE, null, ex);
        }
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
        
        GetAttr gattr = new GetAttr();
        if (gattr.parseArgs(args) != 0){
            gattr.howToUse();
          return;
        }
        
        if (gattr.getHelp){
            gattr.howToUse();
          return;
        }
        
        try {
            gattr.getObjects();
            System.exit(0);  
        } catch (Exception ex) {
            System.out.println(ex);
            Logger.getLogger(GetAttr.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
