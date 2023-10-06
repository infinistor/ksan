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
import java.nio.charset.StandardCharsets;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author legesse
 */
public class MQResponse {
    private String result;
    private MQResponseCode code;
    private String message;
    private int qAck;
   
    public MQResponse(MQResponseType res, MQResponseCode code, String message, int qAck){
        this.setResult(res);
        this.code = code;
        this.message = message;
        this.qAck = qAck;
    }
    
    /*public MQResponse(MQResponseType res, int code, String message, int qAck){
        this.setResult(res);
        this.code = Integer.toString(code);
        this.message = message;
        this.qAck = qAck;
    }*/
    
    public MQResponse(String res){
        JSONParser parser = new JSONParser();
        JSONObject obj;
        
        result = "";
        code = MQResponseCode.MQ_SUCCESS;
        message = "";
        qAck = -1;
        try {
            String res1 = parseEnum(res);
            obj = (JSONObject) parser.parse(res1);
            result = (String)obj.get("Result");
            String qackStr = (String)obj.get("qAck");
            message = obj.get("Message").toString();
            qAck = Integer.parseInt(qackStr);
            code = MQResponseCode.valueOf(obj.get("Code").toString().replaceAll("\"", ""));
        } catch (ParseException ex) {
            // do nothing
            ex.printStackTrace();
        }
    }
    
    private String parseEnum(String res){
        int begin = res.indexOf("\"Code\":");
        int end  = res.indexOf(",", begin);
        String enumValue = res.substring(begin + 7, end);
        //System.out.println("enumValue >>" + enumValue);
        if (!enumValue.startsWith("\""))
            res = res.replace(enumValue, "\"" + enumValue +"\"");
        /*System.out.println(">>>" + res.contains("MQ_SUCESS"));
        if (res.contains("MQ_SUCESS")){
            res = res.replace("MQ_SUCESS", "\"MQ_SUCESS\"");
        }*/
        
        //System.out.println("res >> " + res);
        return res;
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
    
    public MQResponseCode getCode(){
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
        return this.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    @Override
    public String toString(){
         JSONObject obj = new JSONObject();
         obj.put("Result", result);
         obj.put("Code", "\"" + code + "\"");
         obj.put("qAck", String.valueOf(qAck));
         obj.put("Message", message);
         return obj.toString();
        /*return String.format("{ Result : '%s', Code : '%s', Message : '%s',  qAck : %d}"
                , result, code, message, qAck);*/
    }
}
