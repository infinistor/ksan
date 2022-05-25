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
package com.pspace.ifs.ksan.utility.disk;

import java.io.File;
import javax.xml.parsers.ParserConfigurationException;

import com.pspace.ifs.ksan.objmanager.ObjManagerUtil;
import com.pspace.ifs.ksan.utility.UtilDataStorage;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.xml.sax.SAXException;

/**
 *
 * @author legesse
 */
public class DiskPoolUtilMain {
    private CmdLineParser parser;
   
    @Option(name="--name",usage="Disk pool name")
    public String name;
    
    @Option(name="--action",usage="Action to be taken like create/remove/addDisk/removeDisk/addUser/removeUserlist")
    public String action;
    
    @Option(name="--poolId",usage="A number that will identify the disk pool to remove/addDisk/removeDisk")
    public String poolId;
    
    @Option(name="--diskId",usage="A number that will identify the disk to add or remove to the pool")
    public String diskId;
    
    @Option(name="--help",usage="To display this help menu")
    public boolean getHelp = false;
    
    @Option(name="--userId",usage="A userId to add or remove from diskpool")
    public String userId;
    
    @Option(name="--replicaCount",usage="A number of replica either 1 as 1+ 0 or 2 as 1 + 1")
    public int replicaCount = 1;
    
    static String getProgramName(){
        return new File(DiskUtilMain.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
    }
    
    int parseArgs(String[] args){
        action = "";
        parser = new CmdLineParser(this);
        try{
             parser.parseArgument(args);
        } catch( CmdLineException ex ) {
           System.err.println(ex.getMessage());
           System.err.format("java -jar %s --action <create | remove | list | addDisk | removeDisk > --name <disk pool name> --poolId <disk pool Id> --diskId <disk ID>\n", getProgramName());
           return -1;
        }
        
        if (getHelp)
            return 0;
        
        if (!action.isEmpty()){ 
            if (action.compareToIgnoreCase("create") == 0 && !name.isEmpty())
                return 0;
            
             if (action.compareToIgnoreCase("remove") == 0 && !poolId.isEmpty())
                return 0;
            
             if ((action.compareToIgnoreCase("addDisk") == 0 || 
                    action.compareToIgnoreCase("removeDisk") == 0) && !poolId.isEmpty() && !diskId.isEmpty())
                return 0;
             
            if (action.compareToIgnoreCase("list") == 0)
                return 0;
            
            if ((action.compareToIgnoreCase("addUser") == 0) && !poolId.isEmpty() && !userId.isEmpty())
                return 0;
            
            if ((action.compareToIgnoreCase("removeUser") == 0) && !poolId.isEmpty() && !userId.isEmpty())
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
        System.err.println("Example : To create disk pool");
        System.err.format("          java -jar %s --action create --name fastDisk \n", getProgramName());
        System.err.println("\nExample : To remove osd disk");
        System.err.format("          java -jar %s --action remove ----poolId dadadwq123 \n", getProgramName());
        System.err.println("\nExample : Add disk to diskpool");
        System.err.format("          java -jar %s --action addDisk  --poolId asa11qww ---diskId dadadwq123  \n", getProgramName());
        System.err.println("\nExample : Remove disk from diskpool");
        System.err.format("          java -jar %s --action removeDisk  --poolId asa11qww ---diskId dadadwq123 \n", getProgramName());
        System.err.println("\nExample : Add User to diskpool");
        System.err.format("          java -jar %s --action addUser  --poolId asa11qww ---userId legesse --replicaCount 2 \n", getProgramName());
        System.err.println("\nExample : Remove User from diskpool");
        System.err.format("          java -jar %s --action removeUser  --poolId asa11qww ---userId legesse \n", getProgramName());
        System.err.println();
    }
    
    int runOperation() throws ParserConfigurationException, SAXException{
        String Id ;
        int ret = -1;
        UtilDataStorage du = new UtilDataStorage();
        
        if (action.equalsIgnoreCase("create")){
            Id = du.createDiskPool(name);
            if (Id.isEmpty())
                ret = -1;
            else 
                ret = 0;
        } else if (action.equalsIgnoreCase("remove"))
            ret = du.removeDiskPool(poolId);
        else if (action.equalsIgnoreCase("addDisk")){
            ret = du.addDiskToDiskPool(diskId, poolId);
        } else if (action.equalsIgnoreCase("removeDisk")){
            ret = du.removeDiskFromPool(diskId, poolId); 
        }
        else if(action.equalsIgnoreCase("list")){
            ret = du.list("DISKPOOL");
        }
        /*else if(action.equalsIgnoreCase("addUser")){
            try {
                ObjManagerUtil obmu = new ObjManagerUtil();
                ret = obmu.addUserDiskPool(userId, poolId, replicaCount);
            } catch (Exception ex) {
                ret = -1;
            }
        } 
        else if(action.equalsIgnoreCase("removeUser")){
            try {
                ObjManagerUtil obmu = new ObjManagerUtil();
                ret = obmu.removeUserDiskPool(userId, poolId);
            } catch (Exception ex) {
                ret = -1;
            }
        }*/
        
        if (ret == 0)
            System.out.format("%d @DONE\n" , ret);
        else
            System.err.format("%d @FAILD\n", ret);
        return ret;
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
       DiskPoolUtilMain dskpm = new DiskPoolUtilMain();
        
       if (dskpm.parseArgs(args) != 0){
           dskpm.howToUse();
         return;
       }

       if (dskpm.getHelp){
           dskpm.howToUse();
         return;
       }
       
       try {
            dskpm.runOperation();
       } catch (ParserConfigurationException | SAXException ex) {
            System.err.println("failed to excute operation!");
            System.exit(-1);
       }
        
       System.exit(0);
    }
    
}
