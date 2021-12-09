/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.ifs.ksan.mq;

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
