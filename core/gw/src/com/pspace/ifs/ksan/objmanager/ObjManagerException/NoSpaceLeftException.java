/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.ksan.objmanager.ObjManagerException;

/**
 * It could be thrown when there is no space left in the bucket quota 
 * or all OSD disks are either full or ReadOnly 
 *   
 * @author legesse
 */
public class NoSpaceLeftException extends Exception {

    /**
     * Creates a new instance of <code>ObjMangerException</code> without detail
     * message.
     */
    public NoSpaceLeftException() {
    }

    /**
     * Constructs an instance of <code>ObjMangerException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public NoSpaceLeftException(String msg) {
        super(msg);
    }
    
    /**
     * Constructs an instance of <code>ObjMangerException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     * @param err the root exception.
     */
    public NoSpaceLeftException(String msg, Throwable err) {
        super(msg, err);
    }
}
