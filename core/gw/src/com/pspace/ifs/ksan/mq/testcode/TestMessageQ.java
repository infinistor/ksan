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
package com.pspace.ifs.ksan.mq.testcode;

import com.pspace.ifs.ksan.mq.MQReceiver;
import com.pspace.ifs.ksan.mq.MQSender;

/**
 *
 * @author legesse
 */

// 1 to 1 sender class
class Test1To1MessageQSender extends Thread{
    private final MQSender mq1to1;
    
    public Test1To1MessageQSender(String host, String queueName) throws Exception{
        mq1to1 = new MQSender(host, queueName, false);
    }
    
    private void Send(){
        String msg;
        int i = 0;
        
        try{
            while(true){
                msg = "test message " + i++;
                mq1to1.send(msg);
                System.out.println("[1To1Sender : " + Thread.currentThread().getId() +"] Message sent : " + msg);
                Thread.sleep(4000);
            }
        } catch(Exception ex){
            System.out.println("Failed to send message 1To1 -> " +  ex.getMessage()); 
        }
    }
    
    @Override
    public void run(){
        this.Send();
    }
}

// 1 to 1 Reciver class
class Test1To1MessageQReciver extends Thread{
    private MQReceiver mq1to1;
    
    public Test1To1MessageQReciver(String host, String queueName) throws Exception{
        mq1to1 = new MQReceiver(host, queueName, false, null);
    }
    
    private void get(){
        try{
            while(true){
                System.out.println("[1To1Reciver : " + Thread.currentThread().getId() +" ] Received :" + mq1to1.get());
                Thread.sleep(4000);
            }
        } catch(Exception ex){
            System.out.println("Failed to recive message -> " +  ex.getMessage());
        }
    }
    
    @Override
    public void run(){
        this.get();
    } 
}


// 1 to many sender class
class Test1ToNMessageQSender extends Thread{
    private final MQSender mq1ton;
    
    public Test1ToNMessageQSender(String host, String exchange, String option) throws Exception{
        mq1ton = new MQSender(host, exchange, option, "");
    }
    
    private void Send(){
        String msg;
        int i = 0;
        try{
            while(true){
                msg = "test message " + i++;
                mq1ton.send(msg);
                System.out.println("[1ToNSender : " + Thread.currentThread().getId() +"] Message sent : " + msg);
                Thread.sleep(4000);
            }
        } catch(Exception ex){
            System.out.println("Failed to send message 1ToN -> " +  ex.getMessage()); 
        }
    }
    
    @Override
    public void run(){
        this.Send();
    }
}

// 1 to many Reciver class
class Test1ToNMessageQReciver extends Thread{
    private final MQReceiver mq1ton;
    
    public Test1ToNMessageQReciver(String host, String queueName, String exchange, String option) throws Exception{
        mq1ton = new MQReceiver(host, queueName, exchange, false, option, "", null);
    }
    
    private void get(){
        try{
            while(true){
                System.out.println("[1ToNReciver : " + Thread.currentThread().getId() + "] Received :" + mq1ton.get());
                Thread.sleep(4000);
            }
        } catch(Exception ex){
            System.out.println("Failed to recive message -> " +  ex.getMessage());
        }
    }
    
    @Override
    public void run(){
        this.get();
    } 
}

public class TestMessageQ {
 
    private static void oneToOneTest(String host) throws Exception{
        // create 1 To 1 sender threads to a queue name test1121a
        Test1To1MessageQSender s1t1 = new Test1To1MessageQSender(host, "testq121a");
        s1t1.start();
            
        // create 1 To 1 reciver thread from queue test1121a
        Test1To1MessageQReciver r1t1 = new Test1To1MessageQReciver(host, "testq121a");
        r1t1.start();
    }
    
    private static void oneToManyTest(String host) throws Exception{
        // create 1 to many sender threads to exchange named dataEx
        Test1ToNMessageQSender s1tn = new Test1ToNMessageQSender(host, "dataEX", "");
        s1tn.start();

        // create two recivers threads with queue names testq12na and testq12nb
        Test1ToNMessageQReciver r1tna = new Test1ToNMessageQReciver(host, "testq12na", "dataEX", "");
        r1tna.start();

        Test1ToNMessageQReciver r1tnb = new Test1ToNMessageQReciver(host, "testq12nb", "dataEX", "");
        r1tnb.start(); 
    }
    
    /**
     * @param args the command line arguments
     */
    
    public static void main(String[] args) {
        int i = 0;
         
        try{
            
            oneToOneTest("192.168.11.76");
            
            oneToManyTest("192.168.11.76");
            
        } catch (Exception ex){
             System.out.println("--->Error : " + ex.getMessage() + " L. msg :" + ex.getLocalizedMessage());
        }
    }
}
