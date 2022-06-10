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
package com.pspace.ifs.ksan.libs.mq;

import java.io.UnsupportedEncodingException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
    
    public MQResponse(String res){
        JSONParser parser = new JSONParser();
        JSONObject obj;
        
        result = "";
        code = "";
        message = "";
        qAck = -1;
        try {
            obj = (JSONObject) parser.parse(res);
            result = (String)obj.get("Result");
            code = (String)obj.get("Code");
            message = (String)obj.get("Message");
            String qackStr = (String)obj.get("qAck");
            qAck = Integer.parseInt(qackStr);
        } catch (ParseException ex) {
            // do nothing
        }
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
    
    public boolean isResponeDataExist(){
        return !message.isEmpty();
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
         JSONObject obj = new JSONObject();
         obj.put("Result", result);
         obj.put("Code", code);
         obj.put("qAck", String.valueOf(qAck));
         obj.put("Message", message);
         return obj.toString();
        /*return String.format("{ Result : '%s', Code : '%s', Message : '%s',  qAck : %d}"
                , result, code, message, qAck);*/
    }
}
