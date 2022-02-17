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
package com.pspace.ifs.ksan.objmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

/**
 *
 * @author legesse
 */
public class ObjManagerConfig {
    private final Properties prop;
    public String dbHost;
    public long dbport;
    public String dbName;
    public String dbUsername;
    public String dbPassword;
    public String dbRepository;
    public String mqHost;
    public String mqQueeuname;
    public String mqExchangename;
    public String mqOsdExchangename;
    //public String mqRoutingkey4add;
    //public String mqRoutingkey4update;
    //public String mqRoutingkey4remove;
    //public String allocAlgorithm;
    private Logger logger;
    
    private String getStringConfig(String key, String def){
        String value;
        value = prop.getProperty(key);
        if (value == null && def !=null)
            value = def;
        return value;
    }
    
    private long getLongConfig(String key, long def){
        String value;
        value = prop.getProperty(key);
        if (value == null)
            return def;
        return Long.parseLong(value);
    }
    
    public ObjManagerConfig() throws IOException {
        prop = new Properties();
        InputStream is = new FileInputStream("/usr/local/ksan/etc/objmanger.conf");
        prop.load(is);
        // load each members 
        dbRepository= getStringConfig("db.repository", "MYSQL");
        dbHost = getStringConfig("db.host", "localhost");
        dbport = getLongConfig("db.port", 3306);
  
        dbName = getStringConfig("db.name", "");
        dbUsername = getStringConfig("db.username", "");
        dbPassword = getStringConfig("db.password", "");
       
        mqHost = getStringConfig("mq.host", "localhost");
        mqQueeuname = getStringConfig("mq.diskpool.queuename", "");
        mqExchangename = getStringConfig("mq.diskpool.exchangename", "");
        mqOsdExchangename = getStringConfig("mq.osd.exchangename", "");
        //mqRoutingkey4add = prop.getProperty("mq.routingkey.add");
        //mqRoutingkey4update = prop.getProperty("mq.routingkey.update");
        //mqRoutingkey4remove = prop.getProperty("mq.routingkey.remove");
        //allocAlgorithm = prop.getProperty("alloc.algorithm");

        logger = LoggerFactory.getLogger(ObjManager.class);
        
    }
    
    /*public void loadBucketList(ObjManagerCache omc){
        try{ 
            File fXmlFile = new File("/usr/local/pspace/etc/bucketList.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("BUCKET");
            for (int idx = 0; idx < nList.getLength(); idx++) {
                Bucket bt;
           
                Node dpoolNode = nList.item(idx);
                bt = new Bucket(((Element)dpoolNode).getAttribute("name"), 
                        ((Element)dpoolNode).getAttribute("id"),
                        ((Element)dpoolNode).getAttribute("diskPoolId"));
                
                omc.setBucketInCache(bt);
            }
        }catch (Exception e){
            System.out.println("Error-->" + e);
        }
    }*/
    
    public void loadDiskPools(ObjManagerCache omc){
        try{ 
            File fXmlFile = new File("/usr/local/ksan/etc/diskpools.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("DISKPOOL");
            for (int idx = 0; idx < nList.getLength(); idx++) {
                DISKPOOL dp;
                //SERVER s;
                Node dpoolNode = nList.item(idx);
                dp = new DISKPOOL(((Element)dpoolNode).getAttribute("id"), ((Element)dpoolNode).getAttribute("name"));
                logger.debug("disk pool : id {}, name {}", dp.getId(), dp.getName());
                NodeList serverNodeList = ((Element)dpoolNode).getElementsByTagName("SERVER");
                int sidx = 0;
                SERVER s[] = new SERVER[serverNodeList.getLength()];
                while (sidx < serverNodeList.getLength()){
                    
                    Element elemS = (Element)((Element)serverNodeList.item(sidx));
                    s[sidx] = new SERVER(elemS.getAttribute("id"), 0, elemS.getAttribute("ip"));

                    NodeList diskNodeList = elemS.getElementsByTagName("DISK");
                    int didx = 0;
                    while(didx < diskNodeList.getLength()){
                        Element elemD = ((Element)diskNodeList.item(didx));
                        s[sidx].addDisk(elemD.getAttribute("path"), elemD.getAttribute("id"), 0, DiskStatus.GOOD);
                        //System.out.format("Disk id : %s path : %s status : %s\n",  elemD.getAttribute("id"), elemD.getAttribute("path"), elemD.getAttribute("status"));
                        logger.debug("disk id : {}, path : {}", elemD.getAttribute("id"), elemD.getAttribute("path"));
                        didx++; 
                    }
                    dp.addServer(s[sidx]);
                    sidx++;
                }
               omc.setDiskPoolInCache(dp);
            }
        }catch (Exception e){
            System.out.println("Error loading diskpool-->" + e);
        }
    }
    
    @Override
    public String toString(){
        return String.format(
                "{ dbRepository : %s dbName : %s dbUsername : %s dbPassword : %s mqHost : %s mqQueeuname : %s mqExchangename : %s }", 
                dbRepository, dbName, dbUsername, dbPassword, mqHost, mqQueeuname, mqExchangename);
    }
}
