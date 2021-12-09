/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.ifs.ksan.utility.Recovery;

import com.pspace.ifs.ksan.mq.MQCallback;
import com.pspace.ifs.ksan.mq.MQReceiver;
import com.pspace.ifs.ksan.mq.MQResponse;
import com.pspace.ifs.ksan.mq.MQResponseType;
import com.pspace.ifs.ksan.objmanager.ObjManagerConfig;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.utility.ObjectMover;

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
            JSONParser parser = new JSONParser();
            System.out.format("BiningKey : %s body : %s\n", routingKey, body);
            
            try {
                JSONObject JO = (JSONObject) parser.parse(body);
                bucketName = (String)JO.get("bucket");
                pdiskId = (String)JO.get("pdiskId");
                objId   = (String)JO.get("objId");
                fdiskId = (String)JO.get("fdiskId");
            
                om.moveObject(bucketName, pdiskId, objId);
           
            } catch (ParseException ex) {
               logger.debug("unable to parse : {}", body);
            } catch (ResourceNotFoundException ex) {
               logger.debug("Object {} to recover not exist!", body);
            } catch (AllServiceOfflineException ex) {
               logger.error("All osd servers are offline while processing {}", body);
               return new MQResponse(MQResponseType.ERROR, "", "", 0);
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
