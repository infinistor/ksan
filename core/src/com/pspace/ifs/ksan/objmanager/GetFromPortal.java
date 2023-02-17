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
import java.util.logging.Level;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
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
    private String hostServerId;
    private String mqHost;
    private String mqUser;
    private long mqPort;
    private String mqPassword;
    private static Logger logger;
    
    // constants
    private final String KSANMONCONFIFILE =  "/usr/local/ksan/etc/ksanAgent.conf";
    private final String DEFAULTIP = "127.0.0.1";
    private final long DEFAULTPORTALPORT = 5443;
    private final String PORTAIP = "PortalHost";
    private final String PORTALPORT = "PortalPort";
    private final String PORTAAPIKEY = "PortalApiKey";
    private final String SERVERID = "ServerId";
    
    private final String DBREPOSITORY = "objM.db_repository";
    private final String DBHOST = "objM.db_host";
    private final String DBPORT = "objM.db_port";
    private final String DBNAME = "objM.db_name";
    private final String DBUSER = "objM.db_user"; 
    private final String DBPASSWORD = "objM.db_password";  
       
    private final String MQHOST="MQHost";//"objM.mq_host";
    private final String MQUSER="MQUser";
    private final String DEFAULTMQUSER="guest";
    private final String MQPASSWORD="MQPassword";
    private final String MQPORT="MQPort";
    
    private final String KSANGWCONFIAPI = "/api/v1/Config/KsanGw";
    private final String DISKPOOLSAPI = "/api/v1/DiskPools/Details";
    private final String GETDISKLISTAPI = "/api/v1/Disks";
    private final String GETSERVERTAPI = "/api/v1/Servers/";
    private final String SERVICEEVENTAPI = "/api/v1/Services/Event";

    private final String SERVICEEVENT_ID = "Id";
    private final String SERVICEEVENT_TYPE = "EventType";
    private final String SERVICEEVENT_MESSAGE = "Message";
    private final String SERVICENAME = "ServiceType";
    private final String SERVICEEVENT_START = "start";
    private final String SERVICEEVENT_STOP = "stop";
    private final String SERVICEEVENT_SIGTERM = "SIGTERM";
    
    private final String DATA_TAG = "Data";
    private final String ITEM_TAG = "Items";
    private final String CONFIG_TAG = "Config";
    private final String NETWORKINTERFACE_TAG = "NetworkInterfaces";
    private final String DISKS_TAG = "Disks";
    
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
        mqHost = getStringConfig(MQHOST, DEFAULTIP);
        mqUser = getStringConfig(MQUSER, DEFAULTMQUSER);
        mqPassword = getStringConfig(MQPASSWORD, DEFAULTMQUSER);
        mqPort = getLongConfig(MQPORT, 0);
        hostServerId = getStringConfig(SERVERID, "");
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
        
    private  SERVER parseDiskResponse(SERVER svr, JSONArray disks, String dskPoolId){
        for (int idx =0; idx < disks.size(); idx++){
            JSONObject disk = (JSONObject)disks.get(idx);
            Object dskpoolIdObj = disk.get("DiskPoolId");
            if (dskpoolIdObj == null)
                continue;
    
            if (!dskpoolIdObj.equals(dskPoolId))
                continue;
            
            String diskId = (String)disk.get("Id");
            String path = (String)disk.get("Path");
            String status = (String)disk.get("State");
            String mode = (String)disk.get("RwMode");
            String diskName = (String)disk.get("Name");
            double totalInode = (double)disk.get("TotalInode");
            double usedInode = (double)disk.get("UsedInode");
            //double reserverdInode = (double)disk.get("ReservedInode");
            double totalSize = (double)disk.get("TotalSize");
            double usedSize = (double)disk.get("UsedSize");
            double reserverdSize = (double)disk.get("ReservedSize");
            DISK dsk = new DISK();
            dsk.setId(diskId);
            dsk.setPath(path);
            dsk.setHostName(diskName);
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
            else if (mode.equalsIgnoreCase("Maintenance"))
                dsk.setMode(DiskMode.MAINTENANCE);
            else
                dsk.setMode(DiskMode.READONLY);
            //logger.debug("DISKS {}", dsk.toString());
            svr.addDisk(dsk);   
        }
        return svr;
    }
    
    private DISKPOOL parseDiskPoolResponse(DISKPOOL dskp, JSONArray servers){
        int rack;
        for (int idx =0; idx < servers.size(); idx++){
            JSONObject server = (JSONObject)servers.get(idx);
            JSONArray disks = (JSONArray)server.get("Disks");
            JSONArray netInterfaces = (JSONArray)server.get("NetworkInterfaces");
            JSONObject netInterface = (JSONObject)netInterfaces.get(0);
            String osdIP = (String)netInterface.get("IpAddress");
            //String osdName = (String)server.get("Name");
            String status = (String)server.get("State");
            String  serverId = (String)server.get("Id");
            rack = 0;
            if (server.containsKey("Rack")){
                logger.debug(">>>Server >> {}", server);
                Object rackStr = server.get("Rack");
                if (rackStr != null)
                    rack =  Integer.valueOf((String)rackStr);
            }
                
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
            
            svr = parseDiskResponse(svr, disks, dskp.getId());
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
            else if (replication.equalsIgnoreCase("ErasureCode"))
                dp.setDefaultReplicaCount(2);
            else
                dp.setDefaultReplicaCount(1);
            
            dp = parseDiskPoolResponse(dp, servers);
            omc.setDiskPoolInCache(dp);
            //logger.debug("DISKPOOL {}", dp.toString());
        }
    }
    
    private SERVER parseServerDiskResponse(String response, String dskPoolId) throws ParseException{
        if (response.isEmpty())
            return null;
        
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject)parser.parse(response);
        if (jsonObject == null)
            return null;
        
        JSONObject jsonData = (JSONObject)jsonObject.get(DATA_TAG);
        if (jsonData == null){
            return null;
        }
        
        if (jsonData.isEmpty())
            return null;
        
        JSONArray jsonServer = (JSONArray)jsonData.get(NETWORKINTERFACE_TAG);
        if (jsonServer.isEmpty())
            return null;
        
        JSONObject netInterface = (JSONObject)jsonServer.get(0);
        String osdIpAddress = (String)netInterface.get("IpAddress");
        String serverId = (String)netInterface.get("ServerId");
        SERVER svr = new SERVER(serverId, ipaddrToLong(osdIpAddress), osdIpAddress);
        
        JSONArray jsonDisks = (JSONArray)jsonData.get(DISKS_TAG);
        if (jsonDisks.isEmpty())
            return svr;
        
        return parseDiskResponse(svr, jsonDisks, dskPoolId);
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
    
    private String post(String key, String data) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException{
        HttpClient client = HttpClients.custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
	
        HttpPost postRequest = new HttpPost("https://" + portalHost + ":" + portalPort + key);
	postRequest.addHeader("Authorization", portalKey);
        StringEntity entity = new StringEntity(data, ContentType.APPLICATION_JSON);
        postRequest.setEntity(entity);
        
	HttpResponse response = client.execute(postRequest);
	if (response.getStatusLine().getStatusCode() == 200) {
            ResponseHandler<String> handler = new BasicResponseHandler();
            return handler.handleResponse(response);
        }
        
        return null;
    }
    
    private String put(String key, String data) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException{
        HttpClient client = HttpClients.custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
	
        HttpPut putRequest = new HttpPut("https://" + portalHost + ":" + portalPort + key);
	putRequest.addHeader("Authorization", portalKey);
        StringEntity entity = new StringEntity(data, ContentType.APPLICATION_JSON);
        putRequest.setEntity(entity);
        
	HttpResponse response = client.execute(putRequest);
	if (response.getStatusLine().getStatusCode() == 200) {
            ResponseHandler<String> handler = new BasicResponseHandler();
            return handler.handleResponse(response);
        }
        
        return null;
    }
    
    public String getHostServerId(){
        return hostServerId;
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
        objc.dbPort = Long.valueOf(jsonConfig.get(DBPORT).toString());
        objc.dbName = (String)jsonConfig.get(DBNAME);
        objc.dbUsername = (String)jsonConfig.get(DBUSER);
        objc.dbPassword = (String)jsonConfig.get(DBPASSWORD);
        objc.mqHost = mqHost; //(String)jsonConfig.get(MQHOST);
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
    
    public SERVER loadOSDserver(String serverId, String dskPoolId){
        try {
            String content = get(GETSERVERTAPI + serverId);
            if (content == null)
                return null;
            
            if (content.isEmpty())
                return null;
            
            return parseServerDiskResponse(content, dskPoolId);
        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException | ParseException ex) {
            logger.debug("[loadOSDserver] unable to load server({}) info from portal with exception > {}", serverId, ex);
            return null;
        }
    }
    
    public void postStartStopEvent(String eventType, String serviceId, String serviceName) throws IOException{
        try {
    
            JSONObject event= new JSONObject();
            event.put(SERVICEEVENT_ID, serviceId);
            event.put(SERVICENAME, serviceName);
            if (eventType.equalsIgnoreCase(SERVICEEVENT_START)){
                event.put(SERVICEEVENT_TYPE, SERVICEEVENT_START);
                event.put(SERVICEEVENT_MESSAGE, "");
            }
            else{
                event.put(SERVICEEVENT_TYPE, SERVICEEVENT_STOP);
                event.put(SERVICEEVENT_MESSAGE, SERVICEEVENT_SIGTERM);
            }
            this.post(SERVICEEVENTAPI, event.toString());
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException ex) {
            logger.error("[postStartStopEvent] operation(eventType : {}, serverId : {} service Name : {}) failed with {}", 
                    eventType, serviceId, serviceName, ex.getMessage());
        }
    }
    
    public void updateDiskStatus(String diskId, String status)throws IOException{
        String key = GETDISKLISTAPI + "/" + diskId + "/State/" + status;
         try {
            if (put(key, "") == null)
                throw new IOException("[updateDiskStatus] failed to send or get result from portal!");
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException ex) {
            logger.error("[updateDiskStatus] operation(diskId : {}, status : {}) failed with {}", 
                    diskId, status, ex.getMessage());
        }
    }
}
