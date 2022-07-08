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
import org.slf4j.LoggerFactory;

/**
 *
 * @author legesse
 */
public class CBalanceMain {
    private CmdLineParser parser;
    
    @Option(name="--target", usage="Specify the target of the operation as moving a single object or specfic amount of objects move or empty disk")
    public String target = "";
    
    @Option(name="--bucketName", usage="Specify the name of the bucket you wish to balance")
    public String bucketName = "";
    
    @Option(name="--key", usage="Specify the object path")
    public String key = "";
   
    @Option(name="--objId", usage="Specify the object Id insted of object key")
    public String objId = "";
    
    @Option(name="--versionId", usage="Specify the object version Id if you wish particular version of an object")
    private String versionId ="null";
    
    @Option(name="--SrcDiskId", usage="Specify the source disk Id")
    public String SrcDiskId = "";
    
    @Option(name="--DstDiskId", usage="Specify the distination disk Id")
    public String DstDiskId = "";
    
    @Option(name="--size", usage="Specify the capacity to move")
    public String cpacityToMove = "";
    
    @Option(name="--help",usage="To display this help menu")
    public boolean getHelp = false;
    
    public long amountToMove;
    
    static String getProgramName(){
        return new File(CBalanceMain.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName().replaceFirst("[.][^.]+$", "");
    }
    
    int getSizeInByte(){
        if (cpacityToMove.endsWith("TB"))
            amountToMove = Long.parseUnsignedLong(cpacityToMove.replace("TB", ""))* 1024* 1024* 1024* 1024;
        else if(cpacityToMove.endsWith("T")) 
            amountToMove = Long.parseUnsignedLong(cpacityToMove.replace("T", ""))* 1024* 1024* 1024* 1024;
        else if(cpacityToMove.endsWith("GB") ) 
            amountToMove = Long.parseUnsignedLong(cpacityToMove.replace("GB", ""))* 1024* 1024* 1024;
        else if( cpacityToMove.endsWith("G")) 
            amountToMove = Long.parseUnsignedLong(cpacityToMove.replace("G", ""))* 1024* 1024* 1024;
        else if(cpacityToMove.endsWith("MB") ) 
            amountToMove = Long.parseUnsignedLong(cpacityToMove.replace("MB", ""))* 1024* 1024;
        else if(cpacityToMove.endsWith("M")) 
            amountToMove = Long.parseUnsignedLong(cpacityToMove.replace("M", ""))* 1024* 1024;
        else if(cpacityToMove.endsWith("KB")) 
            amountToMove = Long.parseUnsignedLong(cpacityToMove.replace("KB", ""))* 1024;
        else if( cpacityToMove.endsWith("K")) 
            amountToMove = Long.parseUnsignedLong(cpacityToMove.replace("K", ""))* 1024;
        else{
            amountToMove = Long.parseUnsignedLong(cpacityToMove);
        }
        return 0; 
    }
    
    int parseArgs(String[] args){
        
        parser = new CmdLineParser(this);
        try{
             parser.parseArgument(args);
        } catch( CmdLineException ex ) {
           System.err.println(ex.getMessage());
           System.err.format("%s --target <object | size | empty > --bucketName <bucket Name> --key <object path> --versionId <object version Id> --size <capacity to move> --SrcDiskId <Source disk id> --DstDiskId <Destination disk Id>\n", getProgramName());
           return -1;
        }
        
        if (getHelp)
            return 0;
        
        if (!target.isEmpty()){
            if (!bucketName.isEmpty() && (!key.isEmpty() || !objId.isEmpty()) && !SrcDiskId.isEmpty())
                return 0;
            
            if (target.equalsIgnoreCase("empty") && !SrcDiskId.isEmpty())
                return 0;
            
            getSizeInByte();
            if (!SrcDiskId.isEmpty() && !cpacityToMove.isEmpty())
                return 0;
        }
        
        System.err.format("Invalid argument is given \n");
        return -1;
    }
   
    void howToUse(){
        if (parser == null)
            return;
        
        parser.printUsage(System.err);
        System.err.println();
        System.err.println("Example : To move a single object and the object can be idetified either key or object Id");
        System.err.format("          %s --target object --bucketName bucket1 --key file1.txt --SrcDiskId disk111 \n", getProgramName());
        System.err.format("          %s --target object --bucketName bucket1 --key file1.txt --versionId 526554498818254 --SrcDiskId disk111 \n", getProgramName());
        System.err.format("          %s --target object --bucketName bucket1 --objId bd01856bfd2065d0d1ee20c03bd3a9af --versionId 526554498818254 --SrcDiskId disk11 \n", getProgramName());
        System.err.format("          %s --target object --bucketName bucket1 --objId bd01856bfd2065d0d1ee20c03bd3a9af --SrcDiskId disk11 \n", getProgramName());
        System.err.format("          %s --target object --bucketName bucket1 --key file1.txt --DstDiskId disk222\n", getProgramName());
        System.err.format("          %s --target object --bucketName bucket1 --key file1.txt --versionId 526554498818254 --DstDiskId disk222\n", getProgramName());
        System.err.println("\nExample : To move a spefic amount of object from one disk to others");
        System.err.format("          %s --target size --SrcDiskId disk111  --DstDiskId disk222 --size 2GB \n", getProgramName());
        System.err.format("          %s --target size --SrcDiskId disk111 --size 2GB \n", getProgramName());
        System.err.println("\nExample : To empty a disk");
        System.err.format("          %s --target empty --SrcDiskId disk111 \n", getProgramName());
        System.err.println();
    }
    
    void runInBackground() throws Exception{
        long moved_amount = 0;
        int ret;
     
        CBalance cb;
        try {
            cb = new CBalance();
        } catch (Exception ex) {
            //Logger.getLogger(CBalanceMain.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("[" + CBalanceMain.class.getName() + "]" + ex);
            return;
        }
        
        if (target.equals("object")){
            try {
                if (key.isEmpty() && !objId.isEmpty()){
                    if (!DstDiskId.isEmpty() && !SrcDiskId.isEmpty())
                        ret = cb.moveSingleObject(bucketName, objId, versionId, SrcDiskId, DstDiskId);
                    else if (!SrcDiskId.isEmpty())
                        ret = cb.moveSingleObject(bucketName, objId, versionId, SrcDiskId);
                    else
                        ret = cb.moveSingleObject(bucketName, objId, versionId);
                    
                    if (ret  >= 0)
                        System.out.format("A Single Object(bucket : %s, objid : %s) moved\n", bucketName, objId);
                    else
                        System.err.format("failed to move single Object(bucket : %s, objid : %s) ret : %d\n", bucketName, objId, ret);
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
                        System.out.format("A Single Object(bucket : %s, key : %s) moved\n", bucketName, key);
                    else
                        System.err.format("failed to move single Object(bucket : %s, key : %s)\n", bucketName, key);
                }
                
            } catch (ResourceNotFoundException ex) {
                System.err.println("Object not exist!");
            } catch (AllServiceOfflineException ex) {
                System.err.println("All service are offline!");
            }
            return;
        }
        else if(target.equals("size")){
            try {
                if (!SrcDiskId.isEmpty() && !DstDiskId.isEmpty())
                    moved_amount = cb.moveWithSize(bucketName, SrcDiskId, amountToMove, DstDiskId);
                else if (!SrcDiskId.isEmpty())
                    moved_amount = cb.moveWithSize(bucketName, SrcDiskId, amountToMove);
                else
                    moved_amount = cb.moveWithSize(SrcDiskId, amountToMove);
                
                System.out.format("Object(s) with amount %d moved\n", moved_amount);
            } catch (ResourceNotFoundException ex) {
                System.err.println("Bucket or osd disk not exist!");
            } catch (AllServiceOfflineException ex) {
                System.err.println("All service are offline!");
            }
            return;
        }
        else if(target.equals("empty")){
            if (SrcDiskId.isEmpty()) 
               System.err.format("Source disk Id not provided! \n");
            else{
                moved_amount =cb.emptyDisk(SrcDiskId);
                if (moved_amount > 0)
                    System.out.format("Disk(%s) emptied with a total of %d objects moved\n", SrcDiskId, moved_amount);
                else
                    System.out.format("Disk(%s) nothing moved!\n", SrcDiskId);
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
