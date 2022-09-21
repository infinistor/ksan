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
package com.pspace.ifs.ksan.utils.recovery;

import ch.qos.logback.classic.LoggerContext;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author legesse
 */
public class RecoveryMain {
    
    private CmdLineParser parser;
    
    private static String SERVICEID_PATH = "/usr/local/ksan/sbin/.ksanRecovery.ServiceId";
    
    private String serviceId;
    
    @Option(name="--Help",usage="To display this help menu")
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
    
    void getServiceId() throws IOException{
        BufferedReader reader = new BufferedReader(new FileReader(SERVICEID_PATH));
        serviceId = reader.readLine();
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
            Recovery rc = new Recovery(serviceId);
        } catch (Exception ex) { 
            System.out.println("Unable initate recovery process!\n");
            ex.printStackTrace();
        }
    }
    
    static void disableDebuglog(){
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
        rootLogger.setLevel(ch.qos.logback.classic.Level.OFF);
    }
    /**
     * @param args the command line arguments
     */
    
    public static void main(String[] args) {
        disableDebuglog();
        
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
