/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.KSAN.ObjManger.ObjManagerException;

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
