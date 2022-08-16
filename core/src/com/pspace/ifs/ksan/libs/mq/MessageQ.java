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

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.AMQP.BasicProperties;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author legesse
 */
public abstract class MessageQ{
    private final String host;
    private String username = "guest";
    private String password = "guest";
    private int port;
    private final String qname;
    private final String exchangeName;
    private String bindingKey;
    private Channel channel;
    private String message;
    private boolean readerattached;
    private MQCallback callback;
    private long responseDeliveryTag;
    private BasicProperties delivaryProp = null;
    private Logger logger;
    
     // for 1-1 sender  with default username and password
    public MessageQ(String host, String qname, boolean qdurablity) throws Exception{
        this(host, 0, "guest", "guest", qname, qdurablity);
    }
    
    // for 1-1 sender
    public MessageQ(String host,  int port, String username, String password, String qname, boolean qdurablity) throws Exception{
        logger = LoggerFactory.getLogger(MessageQ.class);
        this.message = "";
        this.host = host;
        this.qname = qname;
        this.exchangeName = ""; 
        this.readerattached = false;
        this.username = username;
        this.password = password;
        this.port = 0;
        this.connect();
        this.createQ(qdurablity);
    }
    
    // for 1-1 receiver with default username and password
    public MessageQ(String host, String qname, boolean qdurablity, MQCallback callback) throws Exception{
        this(host, 0, "guest", "guest", qname, qdurablity, callback);
    } 
    // for 1-1 receiver
    public MessageQ(String host, int port, String username, String password, String qname, boolean qdurablity, MQCallback callback) 
            throws Exception{
        logger = LoggerFactory.getLogger(MessageQ.class);
        this.message = "";
        this.host = host;
        this.qname = qname;
        this.exchangeName = ""; 
        this.readerattached = false;
        this.username = username;
        this.password = password;
        this.port = port;
        this.connect();
        this.createQ(qdurablity);
        this.callback = callback;
        if (this.callback != null)
            this.readFromQueue();
    }
    
// for 1-n sender with default username and password
    public MessageQ(String host, String exchangeName, String exchangeOption, String routingKey) throws Exception{
        this(host, 0, "guest", "guest", exchangeName, exchangeOption, routingKey);
    } 
    
    // for 1-n sender
    public MessageQ(String host, int port, String username, String password, String exchangeName, String exchangeOption, String routingKey) 
            throws Exception{
        logger = LoggerFactory.getLogger(MessageQ.class);
        this.message = "";
        this.qname = "";
        this.host = host;
        this.bindingKey = routingKey;
        this.exchangeName = exchangeName; 
        this.readerattached = false;
        this.username = username;
        this.password = password;
        this.port = port;
        this.connect();
        if (exchangeOption.isEmpty())
            this.createExchange("fanout");
        else
            this.createExchange(exchangeOption);
    }
     
    // for 1-n receiver with default username and password
    public MessageQ(String host, String qname, String exchangeName, boolean qdurablity, 
            String exchangeOption, String routingKey, MQCallback callback) throws Exception{
        this(host, 0, "guest", "guest", qname, exchangeName, qdurablity, exchangeOption, routingKey, callback);
    }
    
    // for 1-n receiver
    public MessageQ(String host, int port, String username, String password, String qname, String exchangeName, boolean qdurablity, 
            String exchangeOption, String routingKey, MQCallback callback) throws Exception{
        logger = LoggerFactory.getLogger(MessageQ.class);
        this.message = "";
        this.host = host;
        this.qname = qname;
        this.bindingKey = routingKey;
        this.exchangeName = exchangeName;
        this.readerattached = false; 
        this.username = username;
        this.password = password;
        this.port = port;
        this.connect();
        this.createQ(qdurablity);
        this.callback = callback;
       
        if (this.callback != null)
            this.readFromQueue();
        
        if (exchangeOption.isEmpty())
            this.createExchange("fanout");
        else
            this.createExchange(exchangeOption);
        
        this.bindExchange(); 
    }
    
    private int connect() throws IOException, TimeoutException {
        int prefetchCount = 1;
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(this.host);
        if (!username.equalsIgnoreCase("guest")){
            factory.setUsername(username);
            factory.setPassword(password);
        }
        
        if (port > 0)
          factory.setPort(port);
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(10000); // 10 seconds
        factory.setConnectionTimeout(10000);
        //logger.debug("[MQconnect] from param host {} userName {} password {} port {}", host,  username, password, port);
        //logger.debug("[MQconnect] from factory host {} userName {} password {} port {}", factory.getHost(), factory.getUsername(), factory.getPassword(), factory.getPort());
        Connection connection = factory.newConnection();
        this.channel = connection.createChannel();
        this.channel.basicQos(prefetchCount);
        return 0;
    }
    
    private Map<String, Object> getQuorumQueueMap(){
        Map<String, Object> map = new HashMap<>();
        map.put("x-queue-type", "quorum");
        map.put("x-single-active-consumer", true);
        return map;
    }
    
    private int createQ(boolean durable) throws Exception{
       if (!this.qname.isEmpty()){
           try{
              this.channel.queueDeclarePassive(this.qname);
           } catch(IOException ex){
               if (!this.channel.isOpen())
                   this.connect();
                
                this.channel.queueDeclare(this.qname, true, false, false, getQuorumQueueMap()); 
                //this.channel.queueDeclare(this.qname, durable, false, false, null);
           } 
       }
       return 0;
    }
    
    private String createQurumQueue(String replyQueue) throws IOException {
       return channel.queueDeclare(replyQueue, true, false, false, getQuorumQueueMap()).getQueue();
    } 
    
    public void deleteQ(){
        try{
         if (!this.qname.isEmpty())
             this.channel.queueDelete(this.qname);
        } catch(IOException e){
            System.out.println(e);
        }
    }
    
    private void createExchange(String option) throws Exception{
        if (!this.exchangeName.isEmpty()){
            try{
               this.channel.exchangeDeclarePassive(this.exchangeName);
            } catch(IOException ex){
                if (!this.channel.isOpen())
                   this.connect();
           
                this.channel.exchangeDeclare(this.exchangeName, option);
            }
        }
    }
    
    public void deleteExchange(){
        try{
            if (!this.exchangeName.isEmpty())
                this.channel.exchangeDelete(this.exchangeName);
        } catch(IOException e){
            System.out.println(e);
        }
    }
    
    private void bindExchange() throws IOException {
        if (!this.qname.isEmpty() && !this.exchangeName.isEmpty()){
            this.channel.queueBind(this.qname, this.exchangeName, this.bindingKey);
            //System.out.println(" Queue Name : " + this.qname + " exchange name : " + this.exchangeName);
        } 
       
    }    
    
    public int sendToQueue(String mesg) throws Exception{
        String replyQueueName = this.qname;
        
        if (qname.isEmpty()){
            final String corrId = UUID.randomUUID().toString();
            replyQueueName = createQurumQueue("replyQ." + corrId);
        }
        //String replyQueueName = channel.queueDeclare().getQueue();
        
        BasicProperties props = new BasicProperties
                .Builder()
                .replyTo(replyQueueName)
                .build();
        
        if (!this.channel.isOpen())
            this.connect();
        
        this.channel.basicPublish("", this.qname, props, mesg.getBytes());
       
        return 0;
    }
    
    public int sendToExchange(String mesg, String routingKey) throws Exception{
        
        if (!this.channel.isOpen())
            this.connect();
        
        final String corrId = UUID.randomUUID().toString();
        String replyQueueName = createQurumQueue("replyQ." + corrId);
        //String replyQueueName = channel.queueDeclare().getQueue();
        
        BasicProperties props = new BasicProperties
                .Builder()
                .replyTo(replyQueueName)
                .build();
        
        this.bindExchange();
        this.channel.basicPublish(this.exchangeName, routingKey, props, mesg.getBytes());  
        return 0;
    }
    
    public String sendToExchangeWithResponse(String mesg, String routingKey, int timeoutInMilliSec) throws IOException, InterruptedException, TimeoutException{
        final String corrId = UUID.randomUUID().toString();
        String replyQueueName = createQurumQueue("replyQ." + corrId);
        //String replyQueueName = channel.queueDeclare().getQueue();
        BasicProperties props = new BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();
        
        if (!this.channel.isOpen())
            this.connect();
        
        this.bindExchange();
        this.channel.basicPublish(this.exchangeName, routingKey, props, mesg.getBytes()); 
        String res = this.getReplay(replyQueueName, timeoutInMilliSec);
        return res;
    }
    
    public String sendToQueueWithResponse(String mesg, int timeoutInMilliSec) throws IOException, InterruptedException, TimeoutException{
        final String corrId = UUID.randomUUID().toString();
        //String replyQueueName = channel.queueDeclare().getQueue();
        String replyQueueName = createQurumQueue("replyQ." + corrId);
        BasicProperties props = new BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();
        
        if (!this.channel.isOpen())
            this.connect();
        
        this.channel.basicPublish("", this.qname, props, mesg.getBytes());
        String res = this.getReplay(replyQueueName, timeoutInMilliSec);
        return res; 
    }
    
    private DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        MQResponse response;
        
        message = new String(delivery.getBody(), "UTF-8");
       
        responseDeliveryTag = delivery.getEnvelope().getDeliveryTag();
        delivaryProp = delivery.getProperties();
      
        if (this.callback != null){
            response = this.callback.call(delivery.getEnvelope().getRoutingKey(), message);
            if (delivaryProp != null){
                String cid = delivaryProp.getCorrelationId();
                if (cid != null){
                    if(!cid.isEmpty()) {
                        this.channel.basicPublish("", delivaryProp.getReplyTo(), delivaryProp, response.getResponseInByte()); 
                    }
                }
            }
    
            if ( response.getQAck()== 0) {  
                this.okAck(responseDeliveryTag);
            }
            else if (response.getQAck() < 0)
                this.rejectAckWithDequeue(responseDeliveryTag);
            else
                this.rejectAckWithRequeue(responseDeliveryTag);
        }
    };
    
    private void readFromQueue()throws Exception{
        if (!this.channel.isOpen()){
            this.connect();
        }
        
        if (this.callback != null)
            channel.basicConsume(this.qname, false, deliverCallback, consumerTag -> { });
        else
            channel.basicConsume(this.qname, true, deliverCallback, consumerTag -> { });
    }
      
    public String getQueueName(){
        return this.qname;
    }
    
    public String getExchangeName(){
        return this.exchangeName;
    }
    
    public String get() throws Exception{
        if (this.readerattached == false){
            this.readFromQueue();
            this.readerattached = true;
        }
       
        String m = this.message;
        this.message="";
        return m;
    }
    
    public void addCallback(MQCallback callback) throws Exception{
        this.callback = callback;
        this.readFromQueue();
     
    }
    
    public void removeCallback(){
        this.callback = null;
    }
    
    // positively acknowledge a single delivery, the message will be discarded
    public void okAckSingle(long deliveryTag) throws IOException{
       
        this.channel.basicAck(deliveryTag, false);
    }
    
    // positively acknowledge all deliveries up to this delivery tag
    public void okAck(long deliveryTag) throws IOException{
        
        this.channel.basicAck(deliveryTag, true);
    }
    // requeue the delivery
    public void rejectAckWithRequeue(long deliveryTag)throws IOException{
        
        this.channel.basicReject(deliveryTag, true);
    }
     // negatively acknowledge, the message will be discarded
    public void rejectAckWithDequeue(long deliveryTag)throws IOException{
       
        this.channel.basicReject(deliveryTag, false);
    }
    
    // requeue all unacknowledged deliveries up to this delivery tag
    public void getBackNack(long deliveryTag) throws IOException{
        
        this.channel.basicNack(deliveryTag, true, true);
    }
       
    private String getReplay(String replyQueueName, int timeoutInMilliSec) throws IOException, InterruptedException{
        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);
        String result;
        
        String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
            response.offer(new String(delivery.getBody(), "UTF-8"));
        }, consumerTag -> {
        });
        
        if(timeoutInMilliSec > 0) 
            result = response.poll(timeoutInMilliSec, TimeUnit.MILLISECONDS); // with timeout
        else
            result = response.take();
        
        channel.basicCancel(ctag);
        return result;
    }
}
