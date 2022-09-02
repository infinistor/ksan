/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.ksan.libs.mq;

/**
 *
 * @author legesse
 */
public enum MQResponseCode {
    MQ_SUCESS("MQ_SUCESS"), 
    MQ_OBJECT_NOT_FOUND("MQ_OBJECT_NOT_FOUND"), 
    MQ_OBJECT_EXIST("MQ_OBJECT_EXIST"), 
    MQ_NOSPACE_LEFT_IN_DISK("MQ_NOSPACE_LEFT_IN_DISK"), 
    MQ_READONLY_DISK("MQ_READONLY_DISK"), 
    MQ_DISK_NOTFOUND("MQ_DISK_NOTFOUND"), 
    MQ_INVALID_REQUEST("MQ_INVALID_REQUEST"), 
    MQ_UNKNOWN_ERROR("MQ_UNKNOWN_ERROR");
    private final String name;
    MQResponseCode(String name){
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
