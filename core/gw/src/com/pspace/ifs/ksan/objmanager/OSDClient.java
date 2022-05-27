/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.ksan.objmanager;

import com.pspace.ifs.ksan.mq.MQResponse;
import com.pspace.ifs.ksan.mq.MQSender;
import java.io.IOException;
import org.json.simple.JSONObject;

/**
 *
 * @author legesse
 */
public class OSDClient {
    private final MQSender mqSender;
    
    public OSDClient() throws IOException, Exception{
        ObjManagerConfig config = new ObjManagerConfig();
        System.out.format(" hosts : %s exchange %s\n", config.mqHost, config.mqOsdExchangename);
        mqSender = new MQSender(config.mqHost, config.mqOsdExchangename, "topic", ""); 
    }
    
    public OSDClient(MQSender mqSender){
        this.mqSender = mqSender;
    }
    
    public int removeObject(Metadata mt) throws Exception{
        JSONObject obj;
        String bindingKey;
        String bindingKeyPref = "*.servers.unlink.";
        
        
        obj = new JSONObject();
        obj.put("ObjId", mt.getObjId());
        obj.put("Path", mt.getPath());
        obj.put("DiskId", mt.getPrimaryDisk().getId());
        obj.put("DiskPath", mt.getPrimaryDisk().getPath());
        bindingKey = bindingKeyPref + mt.getPrimaryDisk().getId();
        mqSender.send(obj.toString(), bindingKey);

        if (mt.isReplicaExist()){
            obj.replace("DiskId", mt.getReplicaDisk().getId());
            obj.replace("DiskPath", mt.getReplicaDisk().getPath());
            bindingKey = bindingKeyPref + mt.getReplicaDisk().getId();
            mqSender.send(obj.toString(), bindingKey);
        }    
        return 0;
    }
    
    public int moveObject(String bucket, String objId, String srcDIskId, DISK desDisk) throws Exception{
        JSONObject obj;
        String bindingKey;
        String bindingKeyPref = "*.servers.move.";
        
        
        obj = new JSONObject();
        obj.put("BucketName", bucket);
        obj.put("ObjId", objId);
        obj.put("SRCDiskId", srcDIskId);
        obj.put("DESDiskId", desDisk.getId());
        obj.put("DESPath", desDisk.getPath());
        obj.put("DESOSD", desDisk.getOsdIp());
        bindingKey = bindingKeyPref + srcDIskId;
        System.out.println("bindingKey :> " + bindingKey);
        mqSender.send(obj.toString(), bindingKey);    
        return 0;
    }
    
    public String getObjectAttr(String bucket, String objId, String versionId, String diskId) throws Exception{
        JSONObject obj;
        String bindingKey;
        String bindingKeyPref = "*.servers.getattr.";
        
        
        obj = new JSONObject();
        obj.put("BucketName", bucket);
        obj.put("ObjId", objId);
        obj.put("diskId", diskId);
        obj.put("versionId", versionId);
      
        bindingKey = bindingKeyPref + diskId;
        
        String res = mqSender.sendWithResponse(obj.toString(), bindingKey);
        if (res.isEmpty())
            return res;
        
        MQResponse ret = new MQResponse(res);
        //System.out.println("[getObjectAttr] bindingKey :> " + bindingKey + " res :> " + res + " >Message :>" + ret.getMessage());
        return ret.getMessage();
    }
}
