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
        InputStream is = new FileInputStream("/usr/local/ksan/etc/ksanMon.conf");
        prop.load(is);
        portalHost = getStringConfig("MgsIp", "127.0.0.1");
        portalPort = getLongConfig("IfsPortalPort", 5443);
        portalKey = getStringConfig("IfsPortalKey", "");
        mqUser = getStringConfig("MqUser", "guest");
        mqPassword = getStringConfig("MqPassword", "guest");
        mqPort = getLongConfig("MqPort", 0);
    }
    
    private String getString(JSONObject jsonObject, String key){
        Object obj = jsonObject.get(key);
        if (obj == null)
            return "";
        
        return (String)obj;
    }
    
    private JSONObject parseGetSingleItem(String response, int index) throws ParseException{
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
        
        JSONObject jsonItem = (JSONObject)jsonItems.get(index);
      
        return jsonItem;
    }
    
    private JSONObject parseConfigResponse(String response) throws ParseException{
        JSONParser parser = new JSONParser();
        
        JSONObject jsonItem = parseGetSingleItem(response, 0);
        if (jsonItem == null)
            return null;
        
        String config = getString(jsonItem,"Config");
        if (config.isEmpty())
            return null;
        return (JSONObject)parser.parse(config);
    }
    
    private String pareUserListResponse(String userName, String response) throws ParseException{
        int idx = 0;
        JSONObject jsonItem;
        String name;
        String diskPoolId;
      
        jsonItem = parseGetSingleItem(response, idx);
        if (jsonItem == null)
            return null;

        name = (String)jsonItem.get("Name");
        if ((userName.equalsIgnoreCase(name)) ){ 
            diskPoolId = (String)jsonItem.get("DefaultDiskPoolId");
            return diskPoolId;
        } 
        
        return null;
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
    
    private SERVER parseServerResponse(String serverId, String response) throws ParseException{
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
        
        String serverName = (String)jsonData.get("Name");
        String status =  (String)jsonData.get("State");
        int rack =  Integer.parseInt((String)jsonData.get("Rack"));
        
        JSONArray jsonNetworkInterfaces = (JSONArray)jsonData.get("NetworkInterfaces");
        if (jsonNetworkInterfaces.isEmpty())
            return null;
        
        JSONObject jsonNetworkInterface = (JSONObject)jsonNetworkInterfaces.get(0);
        if (jsonNetworkInterface.isEmpty())
            return null;
        
        JSONArray jsonNetworkInterfaceVlans = (JSONArray)jsonNetworkInterface.get("NetworkInterfaceVlans");
        if (jsonNetworkInterfaceVlans.isEmpty())
            return null;
        
        JSONObject jsonNetworkInterfaceVlan = (JSONObject)jsonNetworkInterfaceVlans.get(0);
        if (jsonNetworkInterfaceVlans.isEmpty())
            return null;
        
        String ipAddress = (String)jsonNetworkInterfaceVlan.get("IpAddress");
        SERVER svr = new SERVER(serverId, ipaddrToLong(ipAddress), serverName);
        svr.setRack(rack);
        if (status.equalsIgnoreCase("Online"))
            svr.setStatus(ServerStatus.ONLINE);
        else if (status.equalsIgnoreCase("Offline"))
            svr.setStatus(ServerStatus.OFFLINE);
        else if (status.equalsIgnoreCase("timeout"))
            svr.setStatus(ServerStatus.TIMEOUT);
        else
            svr.setStatus(ServerStatus.UNKNOWN);
        return svr;
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
        
        JSONObject jsonData = (JSONObject)jsonObject.get("Data");
        if (jsonData == null){
            return;
        }
        
        if (jsonData.isEmpty())
            return;
        
        JSONArray jsonItems = (JSONArray)jsonData.get("Items");
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
        String content = get("/api/v1/Config/List/S3");
        JSONObject jsonConfig = parseConfigResponse(content);
        if (jsonConfig == null)
            return -1;
          
        objc.dbRepository = (String)jsonConfig.get("objM.db_repository");
        objc.dbHost = (String)jsonConfig.get("objM.db_host");
        objc.dbport = Long.valueOf(jsonConfig.get("objM.db_port").toString());
        objc.dbName = (String)jsonConfig.get("objM.db_name");
        objc.dbUsername = (String)jsonConfig.get("objM.db_user");
        objc.dbPassword = (String)jsonConfig.get("objM.db_password");
        objc.mqHost = portalHost;
        objc.mqUsername = mqUser;
        objc.mqPassword = mqPassword;
        objc.mqPort = mqPort;
        objc.mqOsdExchangename = "osdExchange"; //Fixme
        objc.mqExchangename = "diskPoolExchange"; //Fime
        objc.mqQueeuname = "diskPoolQueeu";
        return 0;
    }
        
    public SERVER getOSDServer(String serverId) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, ParseException{
         String content = get("/api/v1/Servers/" + serverId);
         System.out.println("[getOSDServer] content >" + content);
         return parseServerResponse(serverId, content);
    }
    
    public void loadDiskPoolList(ObjManagerCache omc) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, ParseException{
        String content = get("/api/v1/DiskPools/Details");
        if (content == null)
            return;
        
        parseDiskPoolsResponse(omc, content);
        omc.dumpCacheInFile();
    }
    
    public String getUserDefaultDiskPoolId(String userName) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, ParseException{
        String content = get("/api/v1/KsanUsers?skip=0&countPerPage=100&searchFields=Name&searchKeyword=" + userName);
        if (content == null)
            return null;
        
        System.out.println("[getUserDefaultDiskPoolId] content >" + content);
        return pareUserListResponse(userName, content);
    }
}
