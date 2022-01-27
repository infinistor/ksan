/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.ksan.mq;

/**
 *
 * @author legesse
 */
public class MQData {
    private String [] keys;
    private Object [] values;
    
    public MQData(){
        
    }
    
    public void setKeys(String ...keys){
        int idx = 0;
        for (String key : keys){
            keys[idx++] = key;
        }
    }
    
    public void setValues(Object ...values){
        int idx = 0;
        for (Object val : values){
            values[idx] = val;
        }
    }
    
    public void encode(){
        
    }
    
    public void decode(){
        
    }
}
