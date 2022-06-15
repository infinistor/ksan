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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
    private Properties prop = null;
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
    public String portalHost;
    public long portalPort;
    public String portalKey;
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
    
    public ObjManagerConfig(String dbRepository, String dbHost, long dbport, 
            String dbName, String dbUsername, String dbPassword, 
            String mqHost, String mqQueeuname, String mqExchangename, 
            String mqOsdExchangename){
        this.dbRepository = dbRepository;
        this.dbHost = dbHost;
        this.dbport = dbport;
        this.dbName = dbName;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        this.mqHost = mqHost;
        this.mqQueeuname = mqQueeuname;
        this.mqExchangename = mqExchangename;
        this.mqOsdExchangename = mqOsdExchangename;
        logger = LoggerFactory.getLogger(ObjManagerConfig.class);
    }
    
    /*public ObjManagerConfig() throws IOException, NoSuchAlgorithmException {
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
    }*/
    public ObjManagerConfig() throws IOException {
        prop = new Properties();
        getPortaConfig();
        logger = LoggerFactory.getLogger(ObjManagerConfig.class);
        try {
            if (getConfigFromPortal()!= 0){
                throw new IOException("Unable to get configuration from portal!!!");
            }
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | ParseException ex) {
            throw new IOException(ex);
        }
    }
        
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
                //logger.debug("disk pool : id {}, name {}", dp.getId(), dp.getName());
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
                        //logger.debug("disk id : {}, path : {}", elemD.getAttribute("id"), elemD.getAttribute("path"));
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
    
    private void getPortaConfig() throws IOException{
        prop = new Properties();
        InputStream is = new FileInputStream("/usr/local/ksan/etc/ksanMon.conf");
        prop.load(is);
        portalHost = getStringConfig("MgsIp", "127.0.0.1");
        portalPort = getLongConfig("IfsPortalPort", 5443);
        portalKey = getStringConfig("IfsPortalKey", "");
        mqHost = portalHost;
    }
    
  
    private JSONObject parseResponse(String response) throws ParseException{
        if (response.isEmpty())
            return null;
        
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject)parser.parse(response);
        if (jsonObject == null)
            return null;
        
        JSONObject jsonData = (JSONObject)jsonObject.get("Data");
        if (jsonData == null)
            return null;
        
        if (jsonData.isEmpty())
            return null;
        
        JSONArray jsonItems = (JSONArray)jsonData.get("Items");
        if (jsonItems.isEmpty())
            return null;
        
        JSONObject jsonItem = (JSONObject)jsonItems.get(0);
        String config = (String)jsonItem.get("Config");
        if (config.isEmpty())
            return null;
        return (JSONObject)parser.parse(config);
    }
    
    private int getConfigFromPortal() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, ParseException{
        HttpClient client = HttpClients.custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
	
        HttpGet getRequest = new HttpGet("https://" + portalHost + ":" + portalPort + "/api/v1/Config/List/S3");
			getRequest.addHeader("Authorization", portalKey);

	HttpResponse response = client.execute(getRequest);
	if (response.getStatusLine().getStatusCode() == 200) {
            ResponseHandler<String> handler = new BasicResponseHandler();
            String content = handler.handleResponse(response);
            JSONObject jsonConfig = parseResponse(content);
            if (jsonConfig == null)
                return -1;
            
            dbRepository = (String)jsonConfig.get("objM.db_repository");
            dbHost = (String)jsonConfig.get("objM.db_host");
            dbport = Long.valueOf(jsonConfig.get("objM.db_port").toString());
            dbName = (String)jsonConfig.get("objM.db_name");
            dbUsername = (String)jsonConfig.get("objM.db_user");
            dbPassword = (String)jsonConfig.get("objM.db_password");
            mqOsdExchangename = "osdExchange"; //Fixme
            mqExchangename = "diskPoolExchange"; //Fime
            mqQueeuname = "diskPoolQueeu";
            return 0;
        }
        return -1;
    }
    @Override
    public String toString(){
        return String.format(
                "{ dbRepository : %s dbName : %s dbUsername : %s dbPassword : %s mqHost : %s mqQueeuname : %s mqExchangename : %s }", 
                dbRepository, dbName, dbUsername, dbPassword, mqHost, mqQueeuname, mqExchangename);
    }
}
