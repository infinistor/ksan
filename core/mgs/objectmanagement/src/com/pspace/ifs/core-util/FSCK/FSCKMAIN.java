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
//package objfsck;
package com.pspace.ifs.ksan.utility.FSCK;

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
