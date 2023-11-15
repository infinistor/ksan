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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author legesse
 */
public class BucketAnalytics {
    private List<String> config;
    private boolean isTruncated1;
    private String contunation_lastId;
    
    public BucketAnalytics(){
        config = new ArrayList();
        isTruncated1 = false;
        contunation_lastId = "";
    }
    
    public List<String> getConfig(){
        return config;
    }
    
    public boolean isTruncated(){
        return isTruncated1;
    }
    
    public String get_lastId(){
        return contunation_lastId;
    }
    
    public void setTruncated(boolean truncate){
        isTruncated1 = truncate;
    }
    
    public void setLastId(String lastId){
        contunation_lastId = lastId;
    }
}
