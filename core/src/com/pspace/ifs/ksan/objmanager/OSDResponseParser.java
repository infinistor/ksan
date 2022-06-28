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

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author legesse
 */
public class OSDResponseParser {
    public String bucketName;
    public String diskId;
    public String objId;
    public String versionId;
    public String diskPath;
    public String osdIP = "";
    public String md5;
    public long size;
    private JSONParser parser;

    public OSDResponseParser(String body){
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
