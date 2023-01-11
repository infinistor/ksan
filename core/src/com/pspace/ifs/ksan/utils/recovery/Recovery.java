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
import com.pspace.ifs.ksan.libs.mq.MQResponseCode;
import com.pspace.ifs.ksan.libs.mq.MQResponseType;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.OSDClient;
import com.pspace.ifs.ksan.objmanager.OSDResponseParser;
import com.pspace.ifs.ksan.objmanager.ObjManagerConfig;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.objmanager.ObjManagerUtil;
import com.pspace.ifs.ksan.utils.ObjectMover;
import java.io.IOException;

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
    private String serviceId;
    private final String SERVICENAME = "ksanRecovery";
    private final String START = "start";
    private final String STOP  = "stop";
    
   
    class RecoveryObjectCallback implements MQCallback{
        
        @Override
        public MQResponse call(String routingKey, String body) {
            int ret = 0;
            System.out.format(">>BiningKey : %s body : %s\n", routingKey, body);
            
            try {
                ret = fixObject(body);
                if (ret ==  0) 
                    return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
            } catch(Exception ex){
                logger.error(ex.getMessage());
                System.out.println(ex);
            }
            return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_INVALID_REQUEST, "", 0); 
        }
        
       
        private int fixObject(String body) throws Exception{
           OSDResponseParser rp = new OSDResponseParser(body, "MQ_SUCESS");
           String serverId;
           try{
                // get object metadata
                Metadata mt = obmu.getObject(rp.bucketName, rp.objId, rp.versionId);
                // get size and md5 from osd
                if (mt.getPrimaryDisk().getOsdIp().equals(rp.osdIP) )
                    serverId = mt.getPrimaryDisk().getOSDServerId();
                else
                    serverId = mt.getReplicaDisk().getOSDServerId();
                OSDResponseParser resP = osdc.getObjectAttr(rp.bucketName, rp.objId, rp.versionId, rp.diskId, rp.diskPath, serverId);
                if (!resP.errorCode.equals("MQ_SUCESS"))
                    return 0; // ignore because object not exist in osd
                
                //OSDResponseParser resP = new OSDResponseParser(res);
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
    
    private void sendStopEvent(){
        Thread shutdownhook = new Thread(){
            @Override
            public void run() {
                try {
                    obmu.getObjManagerConfig().getPortalHandel().postStartStopEvent(STOP, serviceId, SERVICENAME);
                } catch (IOException ex) {
                    logger.error("[sendStopEvent] {}", ex);
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownhook);
    }
    
    private void sendStartEvent(){
        try {
            obmu.getObjManagerConfig().getPortalHandel().postStartStopEvent(START, serviceId, SERVICENAME);
        } catch (IOException ex) {
            logger.error("[sendStartEvent] {}", ex);
        }
    }
    
    public Recovery( String serviceId) throws Exception{
        queueN = "recoveryQueue";
        exchange = "UtilityExchange"; 
        option ="direct";
        bindingKey = "*.services.recoverd.report.fail_of_replication";
        this.serviceId = serviceId;
        
        logger = LoggerFactory.getLogger(Recovery.class);
        ObjManagerConfig config = new ObjManagerConfig();
        obmu = new ObjManagerUtil();
        if (serviceId.isEmpty())
          this.serviceId =   obmu.getObjManagerConfig().getPortalHandel().getHostServerId();
        
        om = new ObjectMover(false, "RECOVERY");
        MQCallback mq = new RecoveryObjectCallback();
        mqReceiver = new MQReceiver(config.mqHost, queueN, exchange, false, option, bindingKey, mq);
        osdc = new OSDClient(config); // *.servers.getattr.*
        sendStartEvent();
        sendStopEvent();
    }      
}