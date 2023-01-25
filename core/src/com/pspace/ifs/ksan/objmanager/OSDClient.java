/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.ksan.objmanager;

import com.pspace.ifs.ksan.libs.mq.MQResponse;
import com.pspace.ifs.ksan.libs.mq.MQResponseCode;
import com.pspace.ifs.ksan.libs.mq.MQSender;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.json.simple.JSONObject;

/**
 *
 * @author legesse
 */
public class OSDClient {
    private final MQSender mqSender;
    private boolean debugOn;
    
    public OSDClient() throws IOException, Exception{
        debugOn = false;
        ObjManagerConfig config = new ObjManagerConfig();
        //System.out.format(" hosts : %s exchange %s\n", config.mqHost, config.mqOsdExchangename);
        mqSender = new MQSender(config.mqHost, (int)config.mqPort, config.mqUsername, config.mqPassword, config.mqOsdExchangename, "direct", ""); 
    }
    
    public OSDClient(ObjManagerConfig config) throws Exception{
        debugOn = false;
        this.mqSender = new MQSender(config.mqHost, (int)config.mqPort, config.mqUsername, config.mqPassword, config.mqOsdExchangename, "direct", ""); 
    }
    
    public void setDebugModeOn(){
        this.debugOn = true;
    }
    
    public void log(String format, Object... args ){
        if (debugOn)
            System.out.format(format, args);
    }
    
    public int removeObject(String objId, String versionId, DISK dsk)throws Exception{
        JSONObject obj;
        String bindingKey;
        
        obj = new JSONObject();
        obj.put("ObjId", objId);
        obj.put("VersionId", versionId);
        obj.put("DiskId", dsk.getId());
        obj.put("DiskPath", dsk.getPath());
        bindingKey = String.format("*.services.osd.%s.object.unlink", dsk.getOSDServerId());
       log("[removeObject] bindingKey : %s obj : %s ExchangeName : %s \n", bindingKey, obj.toJSONString(), mqSender.getExchangeName());
        mqSender.send(obj.toString(), bindingKey);
        return 0;
    }
    
    public int removeObject(Metadata mt) throws Exception{
     
        removeObject(mt.getObjId(), mt.getVersionId(), mt.getPrimaryDisk());
        if (mt.isReplicaExist()){
            removeObject(mt.getObjId(), mt.getVersionId(), mt.getReplicaDisk());
        }    
        return 0;
    }
    
    public int moveObject(String bucket, String objId, String versionId, DISK srcDisk, DISK desDisk) throws InterruptedException, IOException, TimeoutException{
        JSONObject obj;
        String bindingKey;
        
        obj = new JSONObject();
        obj.put("ObjId", objId);
        obj.put("VersionId", versionId);
        obj.put("SourceDiskId", srcDisk.getId());
        obj.put("SourceDiskPath", srcDisk.getPath());
        obj.put("TargetDiskId", desDisk.getId());
        obj.put("TargetDiskPath", desDisk.getPath());
        obj.put("TargetOSDIP", desDisk.getOsdIp());
        bindingKey = String.format("*.services.osd.%s.object.move", srcDisk.getOSDServerId());
        log("[moveObject] bindingKey : %s obj : %s ExchangeName : %s \n", bindingKey, obj.toJSONString(), mqSender.getExchangeName());
        String res = mqSender.sendWithResponse(obj.toString(), bindingKey, 10000);
        MQResponse ret;
        if (!res.isEmpty())
            ret = new MQResponse(res);
        else
            return -1;
        
        if (ret.getResult().equalsIgnoreCase("Success"))
            return 0;
        else
            return -1;
    }
    
    public int copyObject(String bucket, String objId, String versionId, DISK srcDisk, DISK desDisk) throws InterruptedException, IOException, TimeoutException {
        JSONObject obj;
        String bindingKey;
        
        obj = new JSONObject();
        obj.put("ObjId", objId);
        obj.put("VersionId", versionId);
        obj.put("SourceDiskId", srcDisk.getId());
        obj.put("SourceDiskPath", srcDisk.getPath());
        obj.put("TargetDiskId", desDisk.getId());
        obj.put("TargetDiskPath", desDisk.getPath());
        obj.put("TargetOSDIP", desDisk.getOsdIp());
        bindingKey = String.format("*.services.osd.%s.object.copy", srcDisk.getOSDServerId());
       log("[copyObject] bindingKey : %s obj : %s ExchangeName : %s \n", bindingKey, obj.toJSONString(), mqSender.getExchangeName());
        String res = mqSender.sendWithResponse(obj.toString(), bindingKey, 10000);
        MQResponse ret;
        if (!res.isEmpty())
            ret = new MQResponse(res);
        else{
           log("[copyObject] failed obj : %s response empty\n", obj.toString());
            return -1;
        }
        
        if (ret.getResult().equalsIgnoreCase("Success"))
            return 0;
        else{
           log("[copyObject] failed obj : %s response: %s\n", obj.toString(), ret);
            return -1;
        }
    }
    
    public OSDResponseParser getObjectAttr(String bucket, String objId, String versionId, String diskId, String diskPath, String osdServerId) throws IOException, InterruptedException, TimeoutException {
        JSONObject obj;
        String bindingKey; 
        
        obj = new JSONObject();
        obj.put("BucketName", bucket);
        obj.put("ObjId", objId);
        obj.put("DiskId", diskId);
        obj.put("DiskPath", diskPath);
        obj.put("VersionId", versionId);
        bindingKey = String.format("*.services.osd.%s.object.getattr", osdServerId);
       
        //System.out.format("[getObjectAttr] bindingKey : %s obj : %s ExchangeName : %s \n", bindingKey, obj.toJSONString(), mqSender.getExchangeName());
        String res = mqSender.sendWithResponse(obj.toString(), bindingKey, 3000);
        //String res = ""; mqSender.send(obj.toString(), bindingKey);
        if (res.isEmpty())
            return new OSDResponseParser("", "\"" + MQResponseCode.MQ_UNKNOWN_ERROR + "\"");
        
        MQResponse ret = new MQResponse(res);
        OSDResponseParser osdr = new OSDResponseParser(ret.getMessage(), "\"" + ret.getCode() + "\"");
        
        //System.out.println("[getObjectAttr] bindingKey :> " + bindingKey + " res :> " + res + " >Message :>" + ret);
        //System.out.format("[getObjectAttr] request :> %s raw response :> %s  response :> %s \n", obj.toJSONString(), ret.getMessage(), osdr.toString());
        return osdr;
    }
}
