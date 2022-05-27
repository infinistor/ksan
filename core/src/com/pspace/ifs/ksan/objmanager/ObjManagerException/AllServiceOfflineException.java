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
package com.pspace.ifs.ksan.objmanager.ObjManagerException;

/**
 * Resource could be Objects, Bucket, Disk, Server and Diskpool 
 * and the exception will be thrown when one of resource listed 
 * before not available for  service or exist in the system.  
 * @author legesse
 */
public class AllServiceOfflineException extends Exception {

    /**
     * Creates a new instance of <code>ObjMangerException</code> without detail
     * message.
     */
    public AllServiceOfflineException() {
    }

    /**
     * Constructs an instance of <code>ObjMangerException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public AllServiceOfflineException(String msg) {
        super(msg);
    }
    
    /**
     * Constructs an instance of <code>ObjMangerException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     * @param err the root exception.
     */
    public AllServiceOfflineException(String msg, Throwable err) {
        super(msg, err);
    }
}
