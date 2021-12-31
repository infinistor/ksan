/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.KSAN.MQ;

import java.io.UnsupportedEncodingException;

/**
 *
 * @author legesse
 */
public class MQResponse {
    private String result;
    private String code;
    private String message;
    private int qAck;
   
    public MQResponse(MQResponseType res, String code, String message, int qAck){
        this.setResult(res);
        this.code = code;
        this.message = message;
        this.qAck = qAck;
    }
    
    public MQResponse(MQResponseType res, int code, String message, int qAck){
        this.setResult(res);
        this.code = Integer.toString(code);
        this.message = message;
        this.qAck = qAck;
    }
    
    private void setResult(MQResponseType res){
        if (null != res)
            switch (res) {
            case SUCCESS:
                this.result = "Success";
                break;
            case WARNING:
                this.result = "Warning";
                break;
            case ERROR:
                this.result = "Error";
                break;
            default:
                break;
        }
    }
    
    public String getCode(){
        return this.code;
    }
    
    public String getMessage(){
        return this.message;
    }
    
    public String getResult(){
        return this.result;
    }
    
    public int getQAck(){
        return qAck;
    }
    
    public String getResponse(){
        return this.toString();
    }
    
    public byte[] getResponseInByte(){
        try {
            return this.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return this.toString().getBytes();
        }
    }
    
    @Override
    public String toString(){
        return String.format("{ Result : '%s', Code : '%s', Message : '%s'}"
                , result, code, message);
    }
}
