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

package com.pspace.ifs.ksan.utils.cbalance;

import java.io.File;

import com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.CmdLineException;
import ch.qos.logback.classic.LoggerContext;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 *
 * @author legesse
 */
public class CBalanceMain {
    private CmdLineParser parser;
      
    @Option(name="--EmptyDisk",  usage="set to enable empty disk operation")
    public boolean emptyDisk =false;
    
    @Option(name="--BucketName",  usage="Specify the name of the bucket you wish to balance")
    public String bucketName = "";
    
    @Option(name="--Key", usage="Specify the object key")
    public String key = "";
   
    @Option(name="--ObjId", usage="Specify the object Id insted of object key")
    public String objId = "";
    
    @Option(name="--VersionId", usage="Specify the object version Id if you wish particular version of an object")
    private String versionId ="null";
    
    @Option(name="--SrcDiskName",  usage="Specify the source disk Name")
    public String SrcDiskName = "";
    
    @Option(name="--DstDiskName",  usage="Specify the distination disk Name")
    public String DstDiskName = "";
    
    @Option(name="--Size", usage="Specify the capacity to move in KB/KiB, MB/MiB, GB/GiB, TB/TiB, and PB/PiB units. ")
    public String cpacityToMove = "";
    
    @Option(name="--OkLocalMove",  usage="To allow to move to local another disk")
    public boolean localMoveAllowed = false;
    
    @Option(name="--Help", usage="To display this help menu")
    public boolean getHelp = false;
    
    @Option(name="--Debug", usage="To display debug log to the terminal")
    public boolean debug = false;
    
    public long amountToMove;
    
    static String getProgramName(){
        return new File(CBalanceMain.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName().replaceFirst("[.][^.]+$", "");
    }
    
    int getSizeInByte(){
        long nByte = 1024;
        long nBite = 1000;
        
        if (cpacityToMove.isEmpty())
            return 0; // ignore
        
        if (cpacityToMove.endsWith("PiB"))
            amountToMove = Long.parseUnsignedLong(cpacityToMove.replace("PiB", ""))* nByte* nByte* nByte * nByte * nByte;
        else if (cpacityToMove.endsWith("PB"))
            amountToMove = Long.parseUnsignedLong(cpacityToMove.replace("PB", ""))* nBite* nBite* nBite * nBite * nBite;
        else if (cpacityToMove.endsWith("TiB"))
            amountToMove = Long.parseUnsignedLong(cpacityToMove.replace("TiB", ""))* nByte* nByte* nByte * nByte;
        else if(cpacityToMove.endsWith("TB") ) 
            amountToMove = Long.parseUnsignedLong(cpacityToMove.replace("TB", ""))* nBite* nBite* nBite * nBite;
        else if(cpacityToMove.endsWith("GiB") ) 
            amountToMove = Long.parseUnsignedLong(cpacityToMove.replace("GiB", ""))* nByte* nByte * nByte;
        else if( cpacityToMove.endsWith("GB")) 
            amountToMove = Long.parseUnsignedLong(cpacityToMove.replace("GB", ""))* nBite* nBite * nBite ;
        else if(cpacityToMove.endsWith("MiB") ) 
            amountToMove = Long.parseUnsignedLong(cpacityToMove.replace("MiB", ""))* nByte * nByte;
        else if(cpacityToMove.endsWith("MB")) 
            amountToMove = Long.parseUnsignedLong(cpacityToMove.replace("MB", ""))* nBite * nBite;
        else if(cpacityToMove.endsWith("KiB")) 
            amountToMove = Long.parseUnsignedLong(cpacityToMove.replace("KiB", "")) * nByte;
        else if( cpacityToMove.endsWith("KB")) 
            amountToMove = Long.parseUnsignedLong(cpacityToMove.replace("KB", "")) * nBite;
        else{
            amountToMove = Long.parseUnsignedLong(cpacityToMove);
        }
        return 0; 
    }
    
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
            else if (prefx.equalsIgnoreCase("--EmptyDisk")){
               refArgs.add(opt.replaceFirst(prefx, "--EmptyDisk"));
            }
            else if (prefx.equalsIgnoreCase("--SrcDiskName")){
               refArgs.add(opt.replaceFirst(prefx, "--SrcDiskName"));
            }
            else if (prefx.equalsIgnoreCase("--DstDiskName")){
               refArgs.add(opt.replaceFirst(prefx, "--DstDiskName"));
            }
            else if (prefx.equalsIgnoreCase("--Size")){
               refArgs.add(opt.replaceFirst(prefx, "--Size"));
            }
            else if (prefx.equalsIgnoreCase("--OkLocalMove")){
               refArgs.add(opt.replaceFirst(prefx, "--OkLocalMove"));
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
            String[] cisArgs = makeCaseInsensitive(args);
            parser.parseArgument(cisArgs);
        } catch( CmdLineException ex ) {
           System.err.println(ex.getMessage());
           System.err.format("%s --Emptydisk --BucketName <bucket Name> --Key <object key> --VersionId <object version Id> --Size <capacity to move> --SrcDiskName <Source disk name> --DstDiskName <Destination disk Name> --OkLocalMove \n", getProgramName());
           return -1;
        }
        
        if (getHelp)
            return 0;
        
        
        if ((!bucketName.isEmpty() && (!key.isEmpty()) || (!objId.isEmpty()) && !SrcDiskName.isEmpty()))
            return 0;

        if (emptyDisk && !SrcDiskName.isEmpty())
            return 0;

        getSizeInByte();
        if (!SrcDiskName.isEmpty() && !cpacityToMove.isEmpty())
            return 0;
        
        System.err.format("Invalid argument is given \n");
        return -1;
    }
   
    void howToUse(){
        if (parser == null)
            return;
        
        parser.printUsage(System.err);
        System.err.println();
        System.err.println("Example : To move a single object and the object can be idetified either key or object Id");
        System.err.format("          %s  --BucketName bucket1 --Key file1.txt --SrcDiskName osd1_disk1 \n", getProgramName());
        System.err.format("          %s  --BucketName bucket1 --Key file1.txt --VersionId 526554498818254 --SrcDiskName osd1_disk1 \n", getProgramName());
        System.err.format("          %s  --BucketName bucket1 --ObjId bd01856bfd2065d0d1ee20c03bd3a9af --VersionId 526554498818254 --SrcDiskName osd1_disk1 \n", getProgramName());
        System.err.format("          %s  --BucketName bucket1 --ObjId bd01856bfd2065d0d1ee20c03bd3a9af --SrcDiskName osd1_disk1 \n", getProgramName());
        System.err.format("          %s  --BucketName bucket1 --Key file1.txt --DstDiskName osd2_disk2\n", getProgramName());
        System.err.format("          %s  --BucketName bucket1 --Key file1.txt --VersionId 526554498818254 --DstDiskName osd3_disk2\n", getProgramName());
        System.err.println("\nExample : To move a spefic amount of object from one disk to others");
        System.err.format("          %s  --SrcDiskName osd1_disk1  --DstDiskName osd2_disk2 --Size 2GB \n", getProgramName());
        System.err.format("          %s  --SrcDiskName osd1_disk1 --Size 2GB \n", getProgramName());
        System.err.println("\nExample : To empty a disk");
        System.err.format("          %s --EmptyDisk --SrcDiskName osd1_disk1 \n", getProgramName());
        System.err.println();
    }
    
    void runInBackground() throws Exception{
        String SrcDiskId;
        String DstDiskId;
        long moved_amount = 0;
        int ret;
     
        CBalance cb;
        try {
            cb = new CBalance(localMoveAllowed);
            SrcDiskId = cb.getDiskIdWithName(SrcDiskName);
            DstDiskId = cb.getDiskIdWithName(DstDiskName);
            if (debug)
                cb.setDebugModeOn();
        } catch (Exception ex) {
            System.err.println("[" + CBalanceMain.class.getName() + "]" + ex);
            return;
        }
        
        if (!bucketName.isEmpty()){
            try {
                if (!objId.isEmpty()){
                    if (!DstDiskId.isEmpty() && !SrcDiskId.isEmpty())
                        ret = cb.moveSingleObject(bucketName, objId, versionId, SrcDiskId, DstDiskId);
                    else if (!SrcDiskId.isEmpty())
                        ret = cb.moveSingleObject(bucketName, objId, versionId, SrcDiskId);
                    else
                        ret = cb.moveSingleObject(bucketName, objId, versionId);
                    
                    if (ret  >= 0)
                        System.out.format("A Single Object(bucket : %s, objid : %s versionId : %s) moved\n", bucketName, objId, versionId);
                    else
                        System.err.format("failed to move single Object(bucket : %s, objid : %s versionId : %s ) ret : %d\n", bucketName, objId, versionId, ret);
                }
                else if (!key.isEmpty()){
                    if (!DstDiskId.isEmpty() && !SrcDiskId.isEmpty()){
                        ret = cb.moveSingleObjectWithKey(bucketName, key, versionId, SrcDiskId, DstDiskId);
                    } else if (!SrcDiskId.isEmpty())
                        ret = cb.moveSingleObjectWithKey(bucketName, key, versionId, SrcDiskId);
                    else{
                        ret = cb.moveSingleObjectWithKey(bucketName, key, versionId);
                    }
                
                    if (ret  >= 0)
                        System.out.format("A Single Object(bucket : %s, key : %s versionId : %s ) moved\n", bucketName,  key, versionId);
                    else
                        System.err.format("failed to move single Object(bucket : %s, key : %s versionId : %s )\n", bucketName, key, versionId);
                }
                
            } catch (ResourceNotFoundException ex) {
                System.err.println("Object not exist!");
            } catch (AllServiceOfflineException ex) {
                System.err.println("All service are offline!");
            }
            return;
        }
        else if(!cpacityToMove.isEmpty()){
            try {
                if (!SrcDiskId.isEmpty() && !DstDiskId.isEmpty())
                    moved_amount = cb.moveWithSize(bucketName, SrcDiskId, amountToMove, DstDiskId);
                else if (!SrcDiskId.isEmpty())
                    moved_amount = cb.moveWithSize(bucketName, SrcDiskId, amountToMove);
                else
                    moved_amount = cb.moveWithSize(SrcDiskId, amountToMove);
                if (DstDiskId.isEmpty())
                    System.out.format("Object(s) with amount %d bytes from %s disk are moved\n", moved_amount, SrcDiskName);
                else
                    System.out.format("Object(s) with amount %d bytes from %s disk to %s disk are moved\n", moved_amount, SrcDiskName, DstDiskName);
            } catch (ResourceNotFoundException ex) {
                //ex.printStackTrace();
                System.err.println(" osd disk not exist!");
            } catch (AllServiceOfflineException ex) {
                System.err.println("All service are offline!");
            }
            return;
        }
        else if(emptyDisk){
            if (SrcDiskId.isEmpty()) 
               System.err.format("Source disk Id not provided! \n");
            else{
                moved_amount =cb.emptyDisk(SrcDiskId);
                if (moved_amount > 0)
                    System.out.format("Disk(%s) emptied with a total of %d objects moved\n", SrcDiskName, moved_amount);
                else
                    System.out.format("Disk(%s) nothing moved!\n", SrcDiskName);
            }
            return;
        }
        
        System.err.format("Not supported argument is given \n");
        howToUse(); 
    }

    static void disableDebuglog(String driver){
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(driver);
        rootLogger.setLevel(ch.qos.logback.classic.Level.OFF);
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        disableDebuglog("org.mongodb.driver");
        disableDebuglog("com.pspace.ifs.ksan.objmanager");
        
        CBalanceMain cb = new CBalanceMain();
        
        if (cb.parseArgs(args) != 0){
            cb.howToUse();
          return;
        }
        
        if (cb.getHelp){
            cb.howToUse();
          return;
        }
        
        cb.runInBackground();
        System.exit(0);
    }
}
