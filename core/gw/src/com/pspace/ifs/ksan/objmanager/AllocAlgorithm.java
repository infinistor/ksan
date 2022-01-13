/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.ksan.objmanager;

/**
 *
 * @author legesse
 */
public class AllocAlgorithm {
    public static final int NONE         = 0;
    public static final int ROUNDROBIN   = 1;
    public static final int LOCALPRIMARY = 2;  // Allocate primary disk from local osd disk
}
