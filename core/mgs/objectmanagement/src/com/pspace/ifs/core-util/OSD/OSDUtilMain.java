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
package com.pspace.ifs.ksan.utility.OSD;

import java.io.File;
import javax.xml.parsers.ParserConfigurationException;

import com.pspace.ifs.ksan.utility.UtilDataStorage;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.xml.sax.SAXException;

/**
 *
 * @author legesse
 */
public class OSDUtilMain {

    private CmdLineParser parser;
   
    @Option(name="--ip",usage="IP address of OSD server to add/remove/start/stop")
    public String IPadd;
    
    @Option(name="--action",usage="Action to be taken like add/remove/start/stop")
    public String action;
    
    @Option(name="--id",usage="A number that will identify the osd server to remove/start/stop")
    public String Id;
    
    @Option(name="--help",usage="To display this help menu")
    public boolean getHelp = false;
    
    static String getProgramName(){
        return new File(OSDUtilMain.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
    }
    
    int parseArgs(String[] args){
        action = "";
        parser = new CmdLineParser(this);
        try{
             parser.parseArgument(args);
        } catch( CmdLineException ex ) {
           System.err.println(ex.getMessage());
           System.err.format("java -jar %s --action <add | remove | start | stop > [--ip <IP address> |--id <Server Id>]\n", getProgramName());
           return -1;
        }
        
        if (getHelp)
            return 0;
        
        if (!action.isEmpty()){
            if (action.compareToIgnoreCase("list") == 0)
                return 0;
            if (!IPadd.isEmpty() || !Id.isEmpty())
                return 0;
            
            if (action.compareToIgnoreCase("add") == 0 || 
                    action.compareToIgnoreCase("remove") == 0 || 
                    action.compareToIgnoreCase("start") == 0 || 
                    action.compareToIgnoreCase("stop") == 0 )
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
        System.err.println("Example : To add osd server");
        System.err.format("          java -jar %s --action add --ip 192.168.12.16 \n", getProgramName());
        System.err.println("\nExample : To remove osd server");
        System.err.format("          java -jar %s --action remove --ip 192.168.12.16\n", getProgramName());
        System.err.println("\nExample : To start osd server");
        System.err.format("          java -jar %s --action start --ip 192.168.12.16\n", getProgramName());
        System.err.println("\nExample : To stop osd server");
        System.err.format("          java -jar %s --action stop --ip 192.168.12.16\n", getProgramName());
        System.err.println();
    }
    
    int runOperation() throws ParserConfigurationException, SAXException{
        int ret  = -1;
        UtilDataStorage du = new UtilDataStorage();
        if (action.equalsIgnoreCase("add"))
            ret = du.addServer(IPadd, "ONLINE");
        else if (action.equalsIgnoreCase("remove"))
            ret = du.removeServer(IPadd);
        else if (action.equalsIgnoreCase("start"))
            ret = du.updateServerStatus(IPadd, "ONLINE");
        else if (action.equalsIgnoreCase("stop"))
            ret = du.updateServerStatus(IPadd, "OFFLINE");
         else if (action.equalsIgnoreCase("list"))
            ret = du.list("SERVER");
    
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
        try {
            OSDUtilMain osdm = new OSDUtilMain();
            
            if (osdm.parseArgs(args) != 0){
                osdm.howToUse();
                return;
            }
            
            if (osdm.getHelp){
                osdm.howToUse();
                return;
            }
            
            osdm.runOperation();
            System.exit(0);
        } catch (ParserConfigurationException | SAXException ex) {
            System.err.println("failed to excute operation!");
            System.exit(-1);
        }
    }
    
}
