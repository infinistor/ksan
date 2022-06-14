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
package com.pspace.ifs.ksan.utils.recovery;

import com.pspace.ifs.ksan.libs.mq.MQCallback;
import com.pspace.ifs.ksan.libs.mq.MQReceiver;
import com.pspace.ifs.ksan.libs.mq.MQResponse;
import com.pspace.ifs.ksan.libs.mq.MQResponseType;
import com.pspace.ifs.ksan.libs.mq.MQSender;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.OSDClient;
import com.pspace.ifs.ksan.objmanager.ObjManagerConfig;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.objmanager.ObjManagerUtil;
import com.pspace.ifs.ksan.utils.ObjectMover;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 * @author legesse
 */
public class Recovery {
    private String queueN;
    private String exchange; 
    private String option;
    private String bindingKey;
    private MQReceiver mqReceiver;
    private ObjectMover om;
    private static Logger logger;
    private ObjManagerUtil obmu;
    private OSDClient osdc;
    
    class DataParser{
        public String bucketName;
        public String diskId;
        public String objId;
        public String versionId;
        public String diskPath;
        public String osdIP = "";
        public String md5;
        public long size;
        private JSONParser parser;
        
        public DataParser(String body){
            String value;
            parser = new JSONParser();
            try{
                diskId = "";
                md5 = "";
                size = 0;       
                JSONObject JO = (JSONObject) parser.parse(body);
                bucketName = (String)JO.get("bucketName");
                objId   = (String)JO.get("ObjId");
                versionId = (String)JO.get("VersionId");
                
                if (JO.containsKey("DiskId"))
                   diskId = (String)JO.get("DiskId");
                
                if (JO.containsKey("DiskPath"))
                    diskPath = (String)JO.get("DiskPath");
                
                if (JO.containsKey("osdIP"))
                    osdIP = (String)JO.get("osdIP");
                
                if (JO.containsKey("MD5"))
                    md5 = (String)JO.get("MD5");
                
                if (JO.containsKey("Size")){
                    value = (String)JO.get("Size");
                    size = Long.parseLong(value);
                }
            } catch (ParseException ex) {
                bucketName ="";
                diskId = "";
                objId = "";
                versionId = "";
                diskPath = "";
                osdIP = "";
            }
        }
    }
    class RecoveryObjectCallback implements MQCallback{
        
        @Override
        public MQResponse call(String routingKey, String body) {
            int ret = 0;
            System.out.format(">>BiningKey : %s body : %s\n", routingKey, body);
            
            try {
                ret = fixObject(body);
                if (ret ==  0) 
                    return new MQResponse(MQResponseType.SUCCESS, "", "", 0);
            } catch(Exception ex){
                logger.error(ex.getMessage());
                System.out.println(ex);
            }
            return new MQResponse(MQResponseType.ERROR, "", "", 0); 
        }
        
       
        private int fixObject(String body) throws Exception{
           DataParser rp = new DataParser(body);
           try{
                // get object metadata
                Metadata mt = obmu.getObject(rp.bucketName, rp.objId, rp.versionId);
                // get size and md5 from osd
                String res = osdc.getObjectAttr(rp.bucketName, rp.objId, rp.versionId, rp.diskId, rp.diskPath, rp.osdIP);
                if (res.isEmpty())
                    return 0; // ignore because object not exist in osd
                
                DataParser resP = new DataParser(res);
                if (mt.getSize() == resP.size && mt.getEtag().equals(resP.md5))
                    return 0; // the object is normal
                
                om.moveObject(rp.bucketName, rp.diskId, rp.objId, rp.versionId);
                return 0;
           } catch(ResourceNotFoundException ex){
               logger.debug("Object {} to recover not exist!", body);
               return 0;
           } catch (AllServiceOfflineException ex) {
               logger.error("All osd servers are offline while processing {}", body);
           }
           return -1;
        }
    }
    
    public Recovery() throws Exception{
        queueN = "recoveryQueue";
        exchange = "UtilityExchange"; 
        option ="fanout";
        bindingKey = "*.utility.recovery.*";
        
        logger = LoggerFactory.getLogger(Recovery.class);
        ObjManagerConfig config = new ObjManagerConfig();
        obmu = new ObjManagerUtil();
        om = new ObjectMover(false, "RECOVERY");
        MQCallback mq = new RecoveryObjectCallback();
        mqReceiver = new MQReceiver(config.mqHost, queueN, exchange, false, option, bindingKey, mq);
        osdc = new OSDClient(new MQSender(config.mqHost, config.mqOsdExchangename, option, "")); // *.servers.getattr.*
    }
          
}
