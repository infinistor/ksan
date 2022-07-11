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

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author legesse
 */
public class ObjManagerConfig {
    public String dbHost;
    public long dbport;
    public String dbName;
    public String dbUsername;
    public String dbPassword;
    public String dbRepository;
    public String mqHost;
    public String mqUsername;
    public String mqPassword;
    public long mqPort;
    public String mqQueeuname;
    public String mqExchangename;
    public String mqOsdExchangename;
    private Logger logger;
    private GetFromPortal portal;
        
    public ObjManagerConfig(String dbRepository, String dbHost, long dbport, 
            String dbName, String dbUsername, String dbPassword, 
            String mqHost, String mqUsername, String mqPassword, long mqPort, String mqQueeuname, String mqExchangename, 
            String mqOsdExchangename) throws IOException{
        this.dbRepository = dbRepository;
        this.dbHost = dbHost;
        this.dbport = dbport;
        this.dbName = dbName;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        this.mqHost = mqHost;
        this.mqUsername = mqUsername;
        this.mqPassword = mqPassword;
        this.mqPort = mqPort;
        this.mqQueeuname = mqQueeuname;
        this.mqExchangename = mqExchangename;
        this.mqOsdExchangename = mqOsdExchangename;
        logger = LoggerFactory.getLogger(ObjManagerConfig.class);
        portal = new GetFromPortal();
    }
    
    public ObjManagerConfig() throws IOException {
        logger = LoggerFactory.getLogger(ObjManagerConfig.class);
     
        portal = new GetFromPortal();
        try {
            if (portal.getConfigFromPortal(this)!= 0){
                throw new IOException("Unable to get configuration from portal!!!");
            }
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | ParseException ex) {
            throw new IOException("Unable to get configuration from portal or parse!!!");
        }
    }
        
    public void loadDiskPools(ObjManagerCache omc) throws Exception{
        portal.loadDiskPoolList(omc);
    }
        
    @Override
    public String toString(){
        return String.format(
                "{ dbRepository : %s dbName : %s dbUsername : %s dbPassword : %s mqHost : %s mqQueeuname : %s mqExchangename : %s }", 
                dbRepository, dbName, dbUsername, dbPassword, mqHost, mqQueeuname, mqExchangename);
    }
}
