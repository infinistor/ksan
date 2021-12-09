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
package com.pspace.ifs.ksan.objmanager;

import java.io.File;
import java.io.FileInputStream;
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
    
    public ObjManagerConfig() throws Exception{
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
            System.out.println("Error-->" + e);
        }
    }
    
    @Override
    public String toString(){
        return String.format(
                "{ dbRepository : %s dbName : %s dbUsername : %s dbPassword : %s mqHost : %s mqQueeuname : %s mqExchangename : %s }", 
                dbRepository, dbName, dbUsername, dbPassword, mqHost, mqQueeuname, mqExchangename);
    }
}
