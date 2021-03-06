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
package com.pspace.ifs.ksan.utility.GetAttr;

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
