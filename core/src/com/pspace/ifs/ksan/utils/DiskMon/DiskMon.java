/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.ksan.utils.DiskMon;

import com.pspace.ifs.ksan.objmanager.GetFromPortal;
import com.pspace.ifs.ksan.objmanager.ObjManagerConfig;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


/**
 *
 * @author legesse
 */

class MountPoint{
    private final String diskPath;
    private String device;
    private String fsType;
    private String options;
    private int rx;
    private int tx;
    private long current;
    
    public MountPoint(String diskPath) throws IOException{
        this.diskPath = diskPath;
        rx = 0;
        tx = 0;
        current = System.currentTimeMillis();
        getMounts(); 
    }
    
    private void getMounts() throws IOException{
        InputStreamReader reader = new InputStreamReader(
					new FileInputStream("/proc/mounts"),
					Charset.defaultCharset());
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            do {
                line = bufferedReader.readLine();
                if (line == null){
                    throw new RuntimeException("Mount point assocated with " + diskPath + " not found!"); 
                }

                String[] parts = line.split(" ");
                if (parts.length < 6)
                    continue;

                String mountpoint = parts[1];
                if (!mountpoint.equals(diskPath)){
                    continue;
                }
                device = parts[0];
                fsType = parts[2];
                options = parts[3];
                bufferedReader.close();
                break;
            } while(true);
        }
    }
    public String getPath(){
        return diskPath;
    }
    
    public String getDevice(){
        return device;
    }
    
    public String getFSType(){
        return fsType;
    }
    
    public String getOptions(){
        return options;
    }
    
    public int getRX(){
        return rx;
    }
    
    public int getTX(){
        return tx;
    }
    
    public long getTimestamp(){
        return current;
    } 
    
    public void setRxTx(int rx, int tx){
        this.rx = rx;
        this.tx = tx;
        current = System.currentTimeMillis();
    } 
    
}

class DiskCheck extends Thread {
    private final MountPoint mountPoint;
    private final String errMessag;
    private PortalInterface pf;
    private String disk_status;
    private final String DISK_WEAK    = "Weak";
    private final String DISK_BAD     = "Bad";
    private final String DISK_DISABLE = "Disable";
    
    public DiskCheck(PortalInterface pf, String diskPath) throws IOException{
        mountPoint = new MountPoint(diskPath);
        errMessag = "";
        this.pf = pf;
        disk_status="";
    }
    
    public String getDiskPath(){
        return mountPoint.getPath();
    }
    
    public String getErrorMessage(){
        return errMessag;
    }
    
    private int checkMountPoint(){
        
        try {
            MountPoint mp = new MountPoint(getDiskPath());
            //System.out.println("[checkMountPoint]  >>" + getDiskPath() + " ... OK");
        } catch (IOException ex) {
            System.out.println("[checkMountPoint]  >>" + getDiskPath() + " ... Lost!");
            return -1;
        }
        return 0;
    }
    
    private int checkSpace(){
        //System.out.println("[checkSpace] >>" + getDiskPath());
        File dsk = new File(getDiskPath());
        if (dsk.getUsableSpace() == 0)
            return -1;
        
        return 0;
    }
    
    private int getDiskIO(){
        InputStreamReader reader = null;
        int rx = 0;
        int tx = 0;
        try {
            //System.out.println("[checkSpace] >>" + getDiskPath());
            reader = new InputStreamReader(
                    new FileInputStream("/proc/diskstats"),
                    Charset.defaultCharset());
            try (BufferedReader bufferedReader = new BufferedReader(reader)) {
                String line;
                do {
                    line = bufferedReader.readLine();
                    if (line == null){
                        break;
                    }
                    
                    String[] parts = line.split(" ");
                    if (parts.length < 10)
                        continue;
                    
                    String device = parts[2];
                    if (!device.equals(mountPoint.getDevice())){
                        continue;
                    }
                    rx = Integer.valueOf(parts[5]);
                    tx = Integer.valueOf(parts[9]);
                    bufferedReader.close();
                    break;
                } while(true);
            } catch (IOException ex) {
                Logger.getLogger(DiskCheck.class.getName()).log(Level.SEVERE, null, ex);
            }
            return 0;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DiskCheck.class.getName()).log(Level.SEVERE, null, ex);
        }
finally {
            try {
                if (reader != null)
                  reader.close();
            } catch (IOException ex) {
                Logger.getLogger(DiskCheck.class.getName()).log(Level.SEVERE, null, ex);
            }
        } 
        mountPoint.setRxTx(rx, tx);
        return 0;
    }
    
    private int makeIO2Disk(){ 
        FileOutputStream fos = null;
        String ioTest="test disk IO";
        try {
            fos = new FileOutputStream(mountPoint.getPath() + "/.iotest");
            try (PrintStream ps = new PrintStream(fos)) {
                ps.println(ioTest);
                ps.close();
            }
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DiskCheck.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        } finally {
            try {
                fos.close();
            } catch (IOException ex) {
                Logger.getLogger(DiskCheck.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        FileReader fr;
        try {
            fr = new FileReader(mountPoint.getPath() + "/.iotest");
            try (BufferedReader br = new BufferedReader(fr)) {
                String res = br.readLine();
                br.close();
                if (res.equals(ioTest)){
                    //System.out.println("[makeIO2Disk] >>" + getDiskPath());
                    return 0;
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DiskCheck.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DiskCheck.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }
    
    private int checkDiskIO(){
        int rx1 = mountPoint.getRX();
        int tx1 = mountPoint.getTX();
        long prev = mountPoint.getTimestamp();
        long diff;
        
        getDiskIO();
        diff = mountPoint.getTimestamp() - prev;
        diff = diff > 0 ? diff : 1;
        if ((mountPoint.getTX() - tx1)/ diff > 0)
           return 0;
        
        if ((mountPoint.getRX() - rx1)/ diff > 0)
           return 0;
        
        return makeIO2Disk();
    }
    
    private int checkDiskAlive(){
        //System.out.println("[checkDiskAlive] >>" + getDiskPath());
        return 0;
    }
    
    private int check(){
        int ret;
        System.out.println("[check] <<=========================" + getDiskPath() + "========================>>");
        ret = checkMountPoint();
        if ( ret != 0){
            disk_status = DISK_DISABLE;
            System.out.format("[checkMountPoint] Path >> %10s   %s\n",  getDiskPath(),  " failed!");
            return ret;
        }
        System.out.format("[checkMountPoint] Path >> %10s   %s\n",  getDiskPath(),  " OK");
        
        ret = checkDiskAlive();
        if ( ret != 0)
            return ret;
        
        ret = checkSpace();
        if ( ret != 0){
            disk_status = DISK_WEAK;
            System.out.format("[checkSpace]      Path >> %10s   %s\n",  getDiskPath(),  " failed!");
            return ret;
        }
        System.out.format("[checkSpace]      Path >> %10s   %s\n",  getDiskPath(),  " OK");
        
        ret = checkDiskIO();
        if ( ret != 0){
            disk_status = DISK_BAD;
            System.out.format("[checkDiskIO]     Path >> %10s   %s\n",  getDiskPath(),  " failed!");
            return ret;
        }
        System.out.format("[checkDiskIO]     Path >> %10s   %s\n",  getDiskPath(),  " OK");
        
        System.out.println("[check]<<=======================================================>>");
        disk_status = "";
        return 0;
    }
   
    @Override
    public void run(){
        int ret;
        int failer_counter = 0;
        
        while(true){
            try {
                ret = check();
                if (ret != 0){
                    if (failer_counter++ > 2){
                        pf.reportDiskStatus(getDiskPath(), disk_status);
                    }
                    System.out.println("Report failure!");
                }
                failer_counter = 0;
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                break;
            }
        }
    }
}

class DiskInfo{
    public String Id;
    public String path;
    public String diskName;
    public String status;
    public boolean isOnChecking;
    
    public DiskInfo(){
        Id = "";
        path = "";
        diskName = "";
        status = "";
        isOnChecking = false;
    }
}

class PortalInterface{
    private Properties prop;
    private String portalHost;
    private long portalPort;
    private String portalKey;
    private String serverId;
    private ObjManagerConfig obmc;
    // constants
    private final String KSANMONCONFIFILE =  "/usr/local/ksan/etc/ksanAgent.conf";
    private final String DEFAULTIP = "127.0.0.1";
    private final long DEFAULTPORTALPORT = 5443;
    private final String PORTAIP = "PortalHost";
    private final String PORTALPORT = "PortalPort";
    private final String PORTAAPIKEY = "PortalApiKey";
    private final String SERVERID = "ServerId";
    private final String GETDISKLISTAPI = "/api/v1/Disks/";
    private final String GET = "GET";
    private final String PUT = "PUT";
    private final String DATA_TAG = "Data";
    private final String ITEM_TAG = "Items";
    private final String CONFIG_TAG = "Config";
    private final String NETWORKINTERFACE_TAG = "NetworkInterfaces";
    private final String DISKS_TAG = "Disks";
    
    public PortalInterface() throws IOException{
        this.getPortaConfig();
        obmc = new ObjManagerConfig();
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
        serverId = getStringConfig(SERVERID, "");
        if (serverId.isEmpty())
            throw new IOException("ServerId not found in the config " + KSANMONCONFIFILE);
        System.out.format("[getPortaConfig] portalHost : %s portalPort : %s  portalKey : %s serverId : %s\n", portalHost, portalPort, portalKey, serverId);
    }
    
    private String execute(String requestType, String key) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException{
        HttpUriRequest gpRequest;
        HttpClient client = HttpClients.custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
	if (requestType.equalsIgnoreCase(GET))
            gpRequest = new HttpGet("https://" + portalHost + ":" + portalPort + key);
        else
            gpRequest = new HttpPut("https://" + portalHost + ":" + portalPort + key);
        gpRequest.addHeader("Authorization", portalKey);
                        
	HttpResponse response = client.execute(gpRequest);
	if (response.getStatusLine().getStatusCode() == 200) {
            if (!requestType.equalsIgnoreCase(GET))
                return "OK"; 
            ResponseHandler<String> handler = new BasicResponseHandler();
            return handler.handleResponse(response);
        }
        
        return null;
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
    
    private List<DiskInfo> parseGetAllItem(String response) throws ParseException{
        int idx = 0;
        List<DiskInfo> list = new ArrayList();
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
        
        for(idx = 0; idx < jsonItems.size(); idx++){
            JSONObject jsonItem = (JSONObject)jsonItems.get(idx);
            DiskInfo dsk = new DiskInfo();
            dsk.diskName = jsonItem.get("Name").toString();
            dsk.Id   = jsonItem.get("Id").toString();
            dsk.path = jsonItem.get("Path").toString();
            dsk.status = jsonItem.get("State").toString();
            list.add(dsk);
        }
      
        return list;
    }
    
    public List<DiskInfo> getDiskList() throws IOException{
        try {
            String key = GETDISKLISTAPI + serverId;
            String content = execute(GET, key);
            List<DiskInfo> list = parseGetAllItem(content);
            
            return list;
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | ParseException ex) {
            Logger.getLogger(PortalInterface.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public int reportDiskStatus(String diskId, String status){
        try {
            this.obmc.getPortalHandel().updateDiskStatus(diskId, status);
        } catch (IOException ex) {
            return -1;
        }
        return 0;
    }
    
    public GetFromPortal getPortalHandel(){
        return obmc.getPortalHandel();
    }
}
public class DiskMon {
    static List<DiskInfo> diskList = new ArrayList();
    static PortalInterface pf;
    
    void sendStopEvent(){
        Thread shutdownhook;
        shutdownhook = new Thread(){
            @Override
            public void run() {
                try {
                    pf.getPortalHandel().postStartStopEvent("Stop", pf.getPortalHandel().getHostServerId(), "KsanDiskMon");
                } catch (IOException ex) {
                    return;
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownhook);
    }
    
    int sendStartEvent(){
        try {
            pf.getPortalHandel().postStartStopEvent("Start", pf.getPortalHandel().getHostServerId(), "KsanDiskMon");
        } catch (IOException ex) {
            return -1;
        }
        return 0;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        try {
            pf = new PortalInterface();
            diskList = pf.getDiskList();
        } catch (IOException ex) {
            Logger.getLogger(DiskMon.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        diskList.forEach(dsk -> {
            try {
                DiskCheck dc = new DiskCheck(pf, dsk.path);
                dc.start();
                dsk.isOnChecking = true;
            }
            catch (IOException | RuntimeException ex ) {
                Logger.getLogger(DiskMon.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

    }
    
}
