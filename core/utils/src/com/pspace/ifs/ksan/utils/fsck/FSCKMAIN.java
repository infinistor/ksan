/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.pspace.ifs.ksan.util.fsck;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.CmdLineException;
//import static org.kohsuke.args4j.ExampleMode.ALL;

/**
 *
 * @author legesse
 */
public class FSCKMAIN {
   
    @Option(name="--target", usage="Specify the target of the operation as either bucket or disk")
    private String target = "";
    
    @Option(name="--bucketName", usage="Specify the name of the bucket you wish to fix or check")
    private String bucketName = "";
    
    @Option(name="--diskId", usage="Specify the disk Id you wish to fix or check")
    private String diskId="";
    
    @Option(name="--checkOnly",usage="Specify if you wish only to check not to fix")
    private boolean checkOnly = false;
    
    @Option(name="--help",usage="To display this help menu")
    public boolean getHelp = false;
    
    private CmdLineParser parser;
    
    int parseArgs(String[] args){
        parser = new CmdLineParser(this);
        try{
             parser.parseArgument(args);
             /*if( parser.getArguments().isEmpty() ){
                 System.out.format(" target : %s bucketName : %s diskid : %s checkonly : %s", target, bucketName, diskId, checkOnly);
                 System.err.format("No argument is given \n");
                 return -1;
             }*/
        } catch( CmdLineException ex ) {
           System.err.println(ex.getMessage());
           System.err.format(" %s [--checkOnly] --target <bucket | disk > --bucketName <bucket Name> --diskid <osd disk id>\n", getProgramName());
           return -1;
        }
        
        if (getHelp)
           return 0;
        
        if (!target.isEmpty()){
            if (bucketName.isEmpty() || diskId.isEmpty())
                return 0;
        }
        
        System.out.format(" target : %s bucketName : %s diskid : %s checkonly : %s", target, bucketName, diskId, checkOnly);
        System.err.format("Invalid argument is given \n");
        return -1;
    }
    
     void runInBackground(){
        long job_done = 0;
        try {
            FSCK ofsk = new FSCK(checkOnly);
            if (target.equals("bucket")){
                if (diskId.isEmpty())
                    job_done = ofsk.checkEachDisk(bucketName);
                else
                    job_done = ofsk.checkBucketDisk(bucketName, diskId);
            } else if (target.equals("disk")){
                job_done = ofsk.checkEachOneDiskAllBucket(diskId);
            }
        } catch (Exception ex) {
            Logger.getLogger(FSCKMAIN.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Total job done : " + job_done);
    }
    
    static String getProgramName(){
        return new File(FSCKMAIN.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName().replaceFirst("[.][^.]+$", "");
    }
    
    void howToUse(){
        if (parser == null)
            return;
        
        parser.printUsage(System.err);
        System.err.println();
        System.err.format("  Example:  %s --target bucket --bucketName bucket1 \n", getProgramName());
        System.err.format("  Example:  %s --target bucket --bucketName bucket1 --checkOnly \n", getProgramName());
        System.err.format("  Example:  %s --target disk --diskid disk111 \n", getProgramName());
        System.err.format("  Example:  %s --target disk --diskid disk111 --checkOnly \n", getProgramName());
        System.err.println();
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        FSCKMAIN fmain = new FSCKMAIN();
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
