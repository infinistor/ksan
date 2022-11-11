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
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author legesse
 */
public class RestoreObjects {
    private final DataRepository dbm;
    private static Logger logger;
    
    public RestoreObjects(DataRepository dbm){
        logger = LoggerFactory.getLogger(Objects.class);
        this.dbm = dbm;
    }
    
    public int insertRequest(String bucketName, String key, String objId, String versionId, String request) throws SQLException{
        return dbm.insertRestoreObjectRequest(bucketName, key, objId, versionId, request);
    }
    
    public String getRequest(String bucketName, String key, String versionId) throws SQLException{
        Metadata mt = new Metadata(bucketName, key);
        return dbm.getRestoreObjectRequest(bucketName, mt.getObjId(), versionId);
    }
    
    public void removeRequest(String bucketName, String key, String versionI) throws SQLException{
        Metadata mt = new Metadata(bucketName, key);
        dbm.deleteRestoreObjectRequest(bucketName, mt.getObjId(), versionI);
    }
}
