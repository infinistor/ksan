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
package com.pspace.ifs.ksan.utils.OSDDummy;

import com.pspace.ifs.ksan.mq.MQCallback;
import com.pspace.ifs.ksan.mq.MQReceiver;
import com.pspace.ifs.ksan.mq.MQResponse;
import com.pspace.ifs.ksan.mq.MQResponseType;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author legesse
 */

class OsdReceiverCallback implements MQCallback{
        @Override
        public MQResponse call(String routingKey, String body) {
             String res = "";
            System.out.format("BiningKey : %s body : %s\n", routingKey, body);
            
            if (routingKey.contains(".servers.move."))
                moveObject(body);
            else if (routingKey.contains(".servers.unlink."))
                removeObject(body);
            else if (routingKey.contains(".servers.getattr.")){
                res = getAttr(body);
            }
            
            return new MQResponse(MQResponseType.SUCCESS, "", res, 0);
        }
        
        private String getAttr(String body){
            try {
                Message request = new Message(body);
                JSONObject res = new JSONObject();
                res.put("BucketName", request.getBucketName());
                res.put("objId" ,  request.getObjId());
                res.put("versionId" ,  request.getVersionId());
                res.put("md5", "md5value1111");
                res.put("size", String.valueOf(10));
                return res.toString();
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(OsdReceiverCallback.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            return ""; 
        }
        
        private int moveObject(String body){
            try {
                System.out.println("=======================Client====================================");
                Message msg = new Message(body);
                SimpleFileClient fc = new SimpleFileClient(msg.getDesOSDIP());
         
                long len = (new File(msg.getSRCOSDPath())).length();
                
                Message header = new Message(msg.getBucketName(), msg.getObjId(), "move", msg.getSRCDiskId(), msg.getSRCDiskPath(), msg.getDesDiskId(), msg.getDesDiskPath(), len);
                
                fc.wirte(header.toString());
                fc.sendFile(msg.getSRCOSDPath()); //"/tmp/test.org"
                fc.close();
                removeObject(body);
                System.out.println("=======================End Client====================================");
            } catch (IOException ex) {
                 System.out.println("=======================End Client with error====================================");
                Logger.getLogger(OsdReceiverCallback.class.getName()).log(Level.SEVERE, null, ex);
            }
            return 0;
        }
        
        private int removeObject(String body){
            try {
                Message msg = new Message(body);
                File f = new File(msg.getSRCOSDPath());
                f.delete();
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(OsdReceiverCallback.class.getName()).log(Level.SEVERE, null, ex);
            }
            return 0;
        }
}

class Message{
    private JSONObject header;
    
    public Message(byte [] bufbytearray) throws UnsupportedEncodingException{
        JSONParser parser = new JSONParser();
        try {
            String msg = new String(bufbytearray, "utf-8");
            header = (JSONObject) parser.parse(msg);
        } catch (ParseException ex) {
            header = new JSONObject();
        }
    }
    
    public Message(String msg) throws UnsupportedEncodingException{
        JSONParser parser = new JSONParser();
        try {
            header = (JSONObject) parser.parse(msg);
        } catch (ParseException ex) {
            header = new JSONObject();
        }
    }
    public Message(String bucketName, String objId, String action, String srcDiskId, String srcPath, String desDiskId, String desPath, long msg_size){
        header = new JSONObject();
        header.put("BucketName", bucketName);
        header.put("ObjId", objId);
        header.put("action", action);
        header.put("DESDiskId", desDiskId);
        header.put("DESPath", desPath);
        header.put("SCRDiskId", srcDiskId);
        header.put("SCRPath", srcPath);
        header.put("msg_size", msg_size);
    }
    
    public byte [] getHeader(){
        try {
            return (header.toString()).getBytes("utf-8");
        } catch (UnsupportedEncodingException ex) {
            return new byte[0];
        }
    }
    
    private String get(String key){
        Object val = header.get(key);
        if (val == null)
            return "";
        return val.toString();
    }
    
    private long getLong(String key){
        String val = get(key);
        if (val.isEmpty())
            return 0;
        
        return Long.parseLong(val);
    }
    
    public String getObjId(){
        return get("ObjId");
    }
    
    public String getBucketName(){
        return get("BucketName");
    }
    
    public String getDesOSDIP(){
        return get("DESOSD");
    }
    
    public String getDesDiskId(){
        return get("DESDiskId");
    }
    
    public String getSRCDiskId(){
        return get("SRCDiskId");
    }
    
    public String getSRCDiskPath(){
        return get("SRCPath");
    }
    
    public String getDesDiskPath(){
        return get("DESPath");
    }
    
    public int getObjectSize(){
       return (int)getLong("msg_size");
    }
    
    public String getVersionId(){
        return get("VersionId");
    }
    
    private String makeDirectoryName(String objId) {
        byte[] path = new byte[6];
        byte[] byteObjId = objId.getBytes();

        path[0] = '/';
        int index = 1;

        path[index++] = byteObjId[0];
        path[index++] = byteObjId[1];
        path[index++] = '/';
        path[index++] = byteObjId[2];
        path[index] = byteObjId[3];

        return new String(path);
    }

    private String getOSDPath(String diskPath){
        return diskPath + "/" + "obj" + makeDirectoryName(getObjId()) + "/" + getObjId() + "_" + getVersionId();
    }
    
    public String getSRCOSDPath(){
        return getOSDPath(getSRCDiskPath());
    }
    
    public String getDESOSDPath(){
        return getOSDPath(getDesDiskPath());
    }
    
    @Override
    public String toString(){
        return header.toJSONString();
    }
}

class SimpleTCPIO{
    private Socket sock = null;
    private String serverIp;
    private int port;
    private OutputStream os = null;
    private InputStream is;
    private ServerSocket servsock;
     
    public SimpleTCPIO(String sIP, int port) throws IOException{
        serverIp = sIP;
        this.port = port;
        System.out.println("serverIp :> " + serverIp + " port : >" + this.port);
        try{
            sock = new Socket(serverIp, this.port);
        } catch(ConnectException ex){
            try {
                Thread.sleep(2);
                sock = new Socket(serverIp, this.port);
            } catch (InterruptedException ex1) {
                Logger.getLogger(SimpleTCPIO.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
        os = this.sock.getOutputStream();
        is = this.sock.getInputStream();
    }
    
    public SimpleTCPIO(int port) throws IOException{
        serverIp = null;
        this.port = port;
        System.out.println(" port : >" + this.port);
        servsock = new ServerSocket(this.port);
    }
    
    public void acceptConnection() throws IOException{
        sock = servsock.accept();
        os = this.sock.getOutputStream();
        is = this.sock.getInputStream();
    }
    
    public void close(){
        if (sock != null)
            try {
                sock.close();
        } catch (IOException ex) {
            Logger.getLogger(SimpleTCPIO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public Socket getSockt(){
        return sock;
    }
    
    public void write(byte [] mybytearray) throws IOException{
        os.write(mybytearray, 0,mybytearray.length);
        os.flush();
    }
    
    public byte [] read() throws IOException{
        int bytesRead;
        byte [] mybytearray = new byte[1000];
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
       
        while((bytesRead = is.read(mybytearray, 0, mybytearray.length)) != -1){
            buffer.write(mybytearray, 0, bytesRead);
            if (bytesRead < mybytearray.length)
                break; // to avoid blocking
        }
        buffer.flush();
        return buffer.toByteArray();
    }
   
    public void sendFile(String path) throws IOException{
        File myFile = new File (path);
        byte [] mybytearray  = new byte[(int)myFile.length()];
        FileInputStream fis = new FileInputStream(myFile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        bis.read(mybytearray, 0, mybytearray.length);
        System.out.println("Sending " + path + "(" + mybytearray.length + " bytes)");
        
        os.write(mybytearray, 0, mybytearray.length);
        os.flush();
    }
    
    public long getfileSize(String path){
        return (new File (path)).length();
    }
    public void reciveFile(String path, int file_size) throws IOException{
      FileOutputStream fos = new FileOutputStream(path);
      BufferedOutputStream bos = new BufferedOutputStream(fos);
      byte [] mybytearray  = read();
      bos.write(mybytearray, 0 , file_size);
      bos.flush();
      System.out.println("File " + path
          + " downloaded (" + file_size + " bytes read)"); 
    }
}

class SimpleFileServer {
    private final static int SOCKET_PORT = 55010;
    private SimpleTCPIO tcpIO;
    
    public SimpleFileServer() throws IOException {
        try{
             byte []dataToRead;
             tcpIO = new SimpleTCPIO(SOCKET_PORT);
            while (true) {
                System.out.println("Waiting..."); 
                tcpIO.acceptConnection();
                System.out.println("Accepted connection : " + tcpIO.getSockt());
                System.out.println("=======================Server====================================");
                dataToRead = tcpIO.read();
                if (dataToRead.length == 0){
                    System.out.println("1=======================Ingnored Server====================================");
                    continue;
                }
                
                String dataInStr = new String(dataToRead); 
                if (dataInStr.isEmpty()){
                     System.out.println("=======================Ingnored Server====================================");
                    continue;
                }
                
                //System.out.println("Data recived via TCP : " + dataInStr);
                Message head = new Message(dataToRead);
                tcpIO.reciveFile(head.getDESOSDPath(), head.getObjectSize());
                System.out.println("=======================End Server====================================");
            }
        }  finally { 
            tcpIO.close();
        }
    }
 
}

class SimpleFileClient {
    private int port;
    private  String osdIp;
    private SimpleTCPIO tcpIO;
  
    public SimpleFileClient(String IPaddr) throws IOException {
        osdIp = IPaddr;
        port = 55010;
        tcpIO = new SimpleTCPIO(osdIp, port);
    }
    
    public void wirte(String message) throws IOException{
        tcpIO.write(message.getBytes());
    }
    
    public byte [] read() throws IOException{
        return tcpIO.read();
    } 
    
    public void sendFile(String path) throws IOException{
        tcpIO.sendFile(path);
    }
    
    public void close(){
        tcpIO.close();
    }
}

public class OSDDummy {
   
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String exchange = "OSDExchange";
        String queueName = "osdThrashQueue";
        String host = "192.168.11.76";
        String option = "";   
        MQReceiver mq1ton;  
        OsdReceiverCallback callback;
        String searchDiskIdStr = "*";
        
        try{ 
            if (args.length == 1){
                searchDiskIdStr = args[0];
            }
            
            callback = new OsdReceiverCallback();
            mq1ton = new MQReceiver(host, queueName, exchange, false, option, "*.servers.*." + searchDiskIdStr, callback);
            SimpleFileServer fs = new SimpleFileServer();
        } catch (Exception ex){
             System.out.println("--->Error : " + ex.getMessage() + " L. msg :" + ex.getLocalizedMessage());
        }
    }
    
}
