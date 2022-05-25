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
package com.pspace.ifs.ksan.utility.Recovery;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.CmdLineException;

/**
 *
 * @author legesse
 */
public class RecoveryMain {
    
    private CmdLineParser parser;
    
    @Option(name="--help",usage="To display this help menu")
    public boolean getHelp = false;
    
    @Option(name="--daemon",usage="To run the program in daemon mode")
    public boolean isDaemon = false;
    
    static String getProgramName(){
        return new File(RecoveryMain.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName().replaceFirst("[.][^.]+$", "");
    }
    
    int parseArgs(String[] args){
        
        parser = new CmdLineParser(this);
        try{
             parser.parseArgument(args);
        } catch( CmdLineException ex ) {
           System.err.println(ex.getMessage());
           System.err.format(" %s --daemon\n", getProgramName());
           return -1;
        }
        
        if (getHelp)
            return 0;
       
        //System.err.format("Invalid argument is given \n");
        return 0;
    }
   
    void howToUse(){
        if (parser == null)
            return;
        
        parser.printUsage(System.err);
        System.err.println();
       
        System.err.format("          %s --daemon \n", getProgramName());
      
        System.err.println();
    }
    
    String getPid(){
        File proc_self = new File("/proc/self");
        
        if(proc_self.exists()) 
            try {
                return proc_self.getCanonicalFile().getName();
            }
            catch(IOException e) {
                /// Continue on fall-back
            }
        
        File bash = new File("/bin/bash");
        if(bash.exists()) {
            ProcessBuilder pb = new ProcessBuilder("/bin/bash","-c","echo $PPID");
            try {
                Process p = pb.start();
                BufferedReader rd = new BufferedReader(new InputStreamReader(p.getInputStream()));
                return rd.readLine();
            }
            catch(IOException e) {
                return String.valueOf(Thread.currentThread().getId());
            }
        }
        return String.valueOf(Thread.currentThread().getId());
    }
    
    void runInBackground(){
        try {
            Recovery rc = new Recovery();
        } catch (Exception ex) {
            System.out.println("Unable initate recovery process!\n");
        }
    }
    
    /**
     * @param args the command line arguments
     */
    
    public static void main(String[] args) {
        RecoveryMain rc = new RecoveryMain();
        
        if (rc.parseArgs(args) != 0){
            rc.howToUse();
          return;
        }
        
        if (rc.getHelp){
            rc.howToUse();
          return;
        }
        
        rc.runInBackground();
        
        if (rc.isDaemon)
            rc.getPid();
        //System.exit(0);
    }
}
