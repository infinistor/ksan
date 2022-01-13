/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.ksan.ObjManger.ObjManagerException;

/**
 * Resource could be Objects, Bucket, Disk, Server and Diskpool 
 * and the exception will be thrown when one of resource listed 
 * before already registered in the system.  
 * @author legesse
 */
public class ResourceAlreadyExistException extends Exception {

    /**
     * Creates a new instance of <code>ObjMangerException</code> without detail
     * message.
     */
    public ResourceAlreadyExistException() {
    }

    /**
     * Constructs an instance of <code>ObjMangerException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public ResourceAlreadyExistException(String msg) {
        super(msg);
    }
    
    /**
     * Constructs an instance of <code>ObjMangerException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     * @param err the root exception.
     */
    public ResourceAlreadyExistException(String msg, Throwable err) {
        super(msg, err);
    }
}
