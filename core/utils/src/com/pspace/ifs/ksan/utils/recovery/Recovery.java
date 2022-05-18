/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.ksan.util.recovery;

import com.pspace.ifs.ksan.mq.MQCallback;
import com.pspace.ifs.ksan.mq.MQReceiver;
import com.pspace.ifs.ksan.mq.MQResponse;
import com.pspace.ifs.ksan.mq.MQResponseType;
import com.pspace.ifs.ksan.objmanager.ObjManagerConfig;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.util.ObjectMover;

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
    
    class RecoveryObjectCallback implements MQCallback{
        
        @Override
        public MQResponse call(String routingKey, String body) {
            String bucketName;
            String pdiskId;
            String objId;
            String fdiskId;
            String versionId;
            JSONParser parser = new JSONParser();
            System.out.format("BiningKey : %s body : %s\n", routingKey, body);
            
            try {
                JSONObject JO = (JSONObject) parser.parse(body);
                bucketName = (String)JO.get("bucket");
                pdiskId = (String)JO.get("pdiskId");
                objId   = (String)JO.get("objId");
                fdiskId = (String)JO.get("fdiskId");
                versionId = (String)JO.get("versionId");
                
                om.moveObject(bucketName, pdiskId, objId, versionId);
            } catch (ParseException ex) {
               logger.debug("unable to parse : {}", body);
            } catch (ResourceNotFoundException ex) {
               logger.debug("Object {} to recover not exist!", body);
            } catch (AllServiceOfflineException ex) {
               logger.error("All osd servers are offline while processing {}", body);
               return new MQResponse(MQResponseType.ERROR, "", "", 0);
            } catch(Exception ex){
                logger.error(ex.getMessage());
            }
            
            return new MQResponse(MQResponseType.SUCCESS, "", "", 0);
        }    
    }
    
    public Recovery() throws Exception{
        logger = LoggerFactory.getLogger(Recovery.class);
        ObjManagerConfig config = new ObjManagerConfig();
        queueN = "recoveryQueue";
        exchange = "UtilityExchange"; 
        option ="fanout";
        bindingKey = "*.utility.recovery.*";
        om = new ObjectMover(false, "RECOVERY");
        MQCallback mq = new RecoveryObjectCallback();
        mqReceiver = new MQReceiver(config.mqHost, queueN, exchange, false, option, bindingKey, mq);
    }
          
}
