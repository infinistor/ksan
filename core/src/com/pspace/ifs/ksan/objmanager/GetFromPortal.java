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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
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

/**
 *
 * @author legesse
 */
public class GetFromPortal {
    private Properties prop;
    private String portalHost;
    private long portalPort;
    private String portalKey;
    private String mqUser;
    private long mqPort;
    private String mqPassword;
    private static Logger logger;
    
    // constants
    private final String KSANMONCONFIFILE =  "/usr/local/ksan/etc/ksanMonitor.conf";
    private final String DEFAULTIP = "127.0.0.1";
    private final long DEFAULTPORTALPORT = 5443;
    private final String PORTAIP = "PortalIp";
    private final String PORTALPORT = "PortalPort";
    private final String PORTAAPIKEY = "PortalApiKey";
    
    private final String DBREPOSITORY = "objM.db_repository";
    private final String DBHOST = "objM.db_host";
    private final String DBPORT = "objM.db_port";
    private final String DBNAME = "objM.db_name";
    private final String DBUSER = "objM.db_user"; 
    private final String DBPASSWORD = "objM.db_password";  
       
    private final String MQHOST="objM.mq_host";
    private final String MQUSER="MqUser";
    private final String DEFAULTMQUSER="guest";
    private final String MQPASSWORD="MqPassword";
    private final String MQPORT="MqPort";
    
    private final String KSANGWCONFIAPI = "/api/v1/Config/KsanGw";
    private final String DISKPOOLSAPI = "/api/v1/DiskPools/Details";
    private final String GETDISKLISTAPI = "/api/v1/Disks";
    
    private final String DATA_TAG = "Data";
    private final String ITEM_TAG = "Items";
    private final String CONFIG_TAG = "Config";
    
    public GetFromPortal() throws IOException{
        getPortaConfig();
        logger = LoggerFactory.getLogger(GetFromPortal.class);
    }
    
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
    
    private void getPortaConfig() throws IOException{
        prop = new Properties();
        InputStream is = new FileInputStream(KSANMONCONFIFILE);
        prop.load(is);
        portalHost = getStringConfig(PORTAIP, DEFAULTIP);
        portalPort = getLongConfig(PORTALPORT, DEFAULTPORTALPORT);
        portalKey = getStringConfig(PORTAAPIKEY, "");
        mqUser = getStringConfig(MQUSER, DEFAULTMQUSER);
        mqPassword = getStringConfig(MQPASSWORD, DEFAULTMQUSER);
        mqPort = getLongConfig(MQPORT, 0);
    }
    
    private String getString(JSONObject jsonObject, String key){
        Object obj = jsonObject.get(key);
        if (obj == null)
            return "";
        
        return (String)obj;
    }
        
    private JSONObject parseGetItem(String response) throws ParseException{
        if (response == null)
            return null;
        
        if (response.isEmpty())
            return null;
        
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject)parser.parse(response);
        if (jsonObject == null)
            return null;
        
        JSONObject jsonData = (JSONObject)jsonObject.get(DATA_TAG);
        if (jsonData == null)
            return null;
        
        if (jsonData.isEmpty())
            return null;
        
        return jsonData;
    }
    
    private JSONObject parseGetSingleItem(String response, int index) throws ParseException{
        JSONObject jsonData = parseGetItem(response);
        if (jsonData == null)
            return null;
        
        if (jsonData.isEmpty())
            return null;
        
        JSONArray jsonItems = (JSONArray)jsonData.get(ITEM_TAG);
        if (jsonItems == null)
            return null;
        
        if (jsonItems.isEmpty())
            return null;
        
        if (jsonItems.size() <= index)
            return null;
        
        JSONObject jsonItem = (JSONObject)jsonItems.get(index);
      
        return jsonItem;
    }
    
    private JSONObject parseConfigResponse(String response) throws ParseException{
        JSONParser parser = new JSONParser();
        
        JSONObject jsonData = parseGetItem(response);
        if (jsonData == null)
            return null;
        
        String config = getString(jsonData, CONFIG_TAG);
        if (config.isEmpty())
            return null;
      
        return (JSONObject)parser.parse(config);
    }
        
    private  long ipaddrToLong(String ipaddr){
        long res = 0;
        long ip;
        String[] ipAddressInArray = ipaddr.split("\\.");
        
        for (int i = 3; i >= 0; i--) {
           ip = Long.parseLong(ipAddressInArray[3 - i]); 
           res |= ip << (i * 8);
        }
        return res;
    }
        
    private  SERVER parseDiskResponse(SERVER svr, JSONArray disks){
        for (int idx =0; idx < disks.size(); idx++){
            JSONObject disk = (JSONObject)disks.get(idx);
            String diskId = (String)disk.get("Id");
            String path = (String)disk.get("Path");
            String status = (String)disk.get("State");
            String mode = (String)disk.get("RwMode");
            double totalInode = (double)disk.get("TotalInode");
            double usedInode = (double)disk.get("UsedInode");
            //double reserverdInode = (double)disk.get("ReservedInode");
            double totalSize = (double)disk.get("TotalSize");
            double usedSize = (double)disk.get("UsedSize");
            double reserverdSize = (double)disk.get("ReservedSize");
            DISK dsk = new DISK();
            dsk.setId(diskId);
            dsk.setPath(path);
            dsk.setSpace(totalSize, usedSize, reserverdSize);
            dsk.setInode(totalInode, usedInode);
            dsk.setOSDIP(svr.ipaddrToString(svr.getIpAddress()));
            dsk.setOSDServerId(svr.getId());
            if (status.equalsIgnoreCase("Good"))
                dsk.setStatus(DiskStatus.GOOD);
            else if (status.equalsIgnoreCase("stop"))
                dsk.setStatus(DiskStatus.STOPPED);
            else if (status.equalsIgnoreCase("broken"))
                dsk.setStatus(DiskStatus.BROKEN);
            else
                dsk.setStatus(DiskStatus.UNKNOWN);
            
            if (mode.equalsIgnoreCase("ReadWrite"))
                dsk.setMode(DiskMode.READWRITE);
            else
                dsk.setMode(DiskMode.READONLY);
            //logger.debug("DISKS {}", dsk.toString());
            svr.addDisk(dsk);   
        }
        return svr;
    }
    
    private DISKPOOL parseDiskPoolResponse(DISKPOOL dskp, JSONArray servers){
        for (int idx =0; idx < servers.size(); idx++){
            JSONObject server = (JSONObject)servers.get(idx);
            JSONArray disks = (JSONArray)server.get("Disks");
            JSONArray netInterfaces = (JSONArray)server.get("NetworkInterfaces");
            JSONObject netInterface = (JSONObject)netInterfaces.get(0);
            String osdIP = (String)netInterface.get("IpAddress");
            //String osdName = (String)server.get("Name");
            String status = (String)server.get("State");
            String  serverId = (String)server.get("Id");
            int rack =  Integer.parseInt((String)server.get("Rack"));
            SERVER svr = new SERVER(serverId, ipaddrToLong(osdIP), osdIP);
            svr.setRack(rack);
            if (status.equalsIgnoreCase("Online"))
                svr.setStatus(ServerStatus.ONLINE);
            else if (status.equalsIgnoreCase("Offline"))
                svr.setStatus(ServerStatus.OFFLINE);
            else if (status.equalsIgnoreCase("timeout"))
                svr.setStatus(ServerStatus.TIMEOUT);
            else
                svr.setStatus(ServerStatus.UNKNOWN);
            
            svr = parseDiskResponse(svr, disks);
            dskp.addServer(svr);
            //logger.debug("SERVERS {}", svr.toString());
        }
        return dskp;
    }
    
    private void parseDiskPoolsResponse(ObjManagerCache omc, String response) throws ParseException{
        if (response.isEmpty())
            return;
        
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject)parser.parse(response);
        if (jsonObject == null)
            return;
        
        JSONObject jsonData = (JSONObject)jsonObject.get(DATA_TAG);
        if (jsonData == null){
            return;
        }
        
        if (jsonData.isEmpty())
            return;
        
        JSONArray jsonItems = (JSONArray)jsonData.get(ITEM_TAG);
        if (jsonItems.isEmpty())
            return;
        
        for(int idx = 0; idx < jsonItems.size(); idx++){
            JSONObject item = (JSONObject)jsonItems.get(idx);
            JSONArray servers = (JSONArray)item.get("Servers");
            String diskpoolId = (String)item.get("Id");
            String diskpoolName = (String)item.get("Name");
            String replication = (String)item.get("ReplicationType");
            DISKPOOL dp = new DISKPOOL(diskpoolId, diskpoolName);
            if (replication.equalsIgnoreCase("OnePlusOne"))
                dp.setDefaultReplicaCount(2);
            else
                dp.setDefaultReplicaCount(1);
            
            dp = parseDiskPoolResponse(dp, servers);
            omc.setDiskPoolInCache(dp);
            //logger.debug("DISKPOOL {}", dp.toString());
        }
    }
    
    private String get(String key) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException{
        HttpClient client = HttpClients.custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
	
        HttpGet getRequest = new HttpGet("https://" + portalHost + ":" + portalPort + key);
			getRequest.addHeader("Authorization", portalKey);

	HttpResponse response = client.execute(getRequest);
	if (response.getStatusLine().getStatusCode() == 200) {
            ResponseHandler<String> handler = new BasicResponseHandler();
            return handler.handleResponse(response);
        }
        
        return null;
    }
    
    public int getConfigFromPortal(ObjManagerConfig objc) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, ParseException{    
        String content = get(KSANGWCONFIAPI);
        if (content == null)
            return -1;
        
        JSONObject jsonConfig = parseConfigResponse(content);
        if (jsonConfig == null)
            return -1;
          
        objc.dbRepository = (String)jsonConfig.get(DBREPOSITORY);
        objc.dbHost = (String)jsonConfig.get(DBHOST);
        objc.dbport = Long.valueOf(jsonConfig.get(DBPORT).toString());
        objc.dbName = (String)jsonConfig.get(DBNAME);
        objc.dbUsername = (String)jsonConfig.get(DBUSER);
        objc.dbPassword = (String)jsonConfig.get(DBPASSWORD);
        objc.mqHost = (String)jsonConfig.get(MQHOST);
        objc.mqUsername = mqUser;
        objc.mqPassword = mqPassword;
        objc.mqPort = mqPort;
        objc.mqOsdExchangename = "ksan.osdExchange";
        objc.mqExchangename = "ksan.system";  
        objc.mqQueeuname = "disk";  
        return 0;
    }
            
    public void loadDiskPoolList(ObjManagerCache omc) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, ParseException{
        String content = get(DISKPOOLSAPI);
        if (content == null)
            return;
        
        parseDiskPoolsResponse(omc, content);
        omc.dumpCacheInFile();
    }
    
    public String getDiskId(String diskName) throws IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException, ParseException{
        int idx = 0;
        JSONObject diskObj;
        
        String content = get(GETDISKLISTAPI);
        if (content == null)
            return "";
        
        if (content.isEmpty())
            return "";
        
        do{
            diskObj = parseGetSingleItem(content, idx);
            if (diskObj == null)
                return "";

            if (diskObj.isEmpty())
                return "";
            
            String diskN = (String)diskObj.get("Name");
            if (diskN.equalsIgnoreCase(diskName)){
                return (String)diskObj.get("Id");
            }
            idx++;
        } while(true);
   
    }
}
