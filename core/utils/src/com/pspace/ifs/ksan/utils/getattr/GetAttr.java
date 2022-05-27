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

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjManagerUtil;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.CmdLineException;

/**
 *
 * @author legesse
 */

public class GetAttr {
    
    @Option(name="--bucketName", usage="Specify the name of the bucket")
    private  String bucketName = "";
    
    @Option(name="--key", usage="Specify the object path")
    private String ObjectPath = "";
    
    @Option(name="--objId", usage="Specify the object Id if you with to display with Id rather than object key")
    private String objId = "";
    
    @Option(name="--version", usage="Specify the object version Id if you wish particula version of the object")
    private String version = "";
    
    @Option(name="--isBucket", usage="set it if you wish to display the attribute of a bucket")
    private boolean isFile = true;
    
    @Option(name="--help",usage="To display this help menu")
    public boolean getHelp = false;
    
    private CmdLineParser parser;
     
    int parseArgs(String[] args){
        parser = new CmdLineParser(this);
        try{
             parser.parseArgument(args);
 
        } catch( CmdLineException ex ) {
           System.err.println(ex.getMessage());
           System.err.format("%s --bucketName <bucket Name> [--key <object path> |--objId <object id>] [--version <versionId>]\n", getProgramName());
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
        return new File(GetAttr.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName().replaceFirst("[.][^.]+$", "");
    }
     
    void howToUse(){
        if (parser == null)
            return;
        
        parser.printUsage(System.err);
        System.err.println();
        System.err.format("  Example: %s --bucketName bucket1 --key file1.txt \n", getProgramName());
        System.err.format("  Example: %s --bucketName bucket1 --objid bd01856bfd2065d0d1ee20c03bd3a9af \n", getProgramName());
        System.err.println();
    }
     
    void displayMeta(Metadata mt){
        String RESET  = "\u001B[0m";
        String GREEN  = "\u001B[32m";
        String RED    = "\u001B[31m";
        String YELLOW = "\u001B[33m";
        String BLUE   = "\u001B[34m";
        
        String dskMsg;
        
        try {
            dskMsg = String.format(" PrimaryDisk>  diskId : %-15s  diskPath : %s (%s%7s%s)\n  ReplicaDisk>  diskId : %-15s  diskPath : %s (%s%7s%s)",
                    mt.getPrimaryDisk().getId(), 
                    mt.getPrimaryDisk().getPath(), GREEN,
                    mt.getPrimaryDisk().getStatus(), RESET,
                    mt.getReplicaDisk().getId(), 
                    mt.getReplicaDisk().getPath(), GREEN,
                    mt.getReplicaDisk().getStatus(), RESET);
        } catch (ResourceNotFoundException ex) {
            dskMsg = String.format(" PrimaryDisk>  diskId : %s  diskPath : %s (%s%s%s)\n",
                    mt.getPrimaryDisk().getId(), 
                    mt.getPrimaryDisk().getPath(), GREEN,
                    mt.getPrimaryDisk().getStatus(), RESET);
        }
       System.out.format("\n bucketName : %s \n ObjectKey  : %s \n objId      : %s"
               + "\n %s \n"
               , bucketName, mt.getPath(), mt.getObjId(), dskMsg);
       System.out.println();
    }
    
    void displayNothing(){
        System.out.println("No Information avaliable!\n");
    }
    
    void getObjects(){
        Metadata mt;
        
        try {
            ObjManagerUtil obmu = new ObjManagerUtil();
            if (!ObjectPath.isEmpty())
                mt = obmu.getObjectWithPath(bucketName, ObjectPath);
            else
                mt = obmu.getObject(bucketName, objId);
            
             displayMeta(mt);
        } catch (ResourceNotFoundException ex) {
            displayNothing();
        } catch (Exception ex) {
            Logger.getLogger(GetAttr.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * @param args the command line arguments
     */
        
    public static void main(String[] args) {
        
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
            
        } catch (Exception ex) {
            Logger.getLogger(GetAttr.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}