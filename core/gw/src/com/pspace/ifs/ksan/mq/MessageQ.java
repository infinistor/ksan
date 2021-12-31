/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.KSAN.MQ;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.AMQP.BasicProperties;
import java.io.IOException;

/**
 *
 * @author legesse
 */
public abstract class MessageQ{
    private final String host;
    private final String qname;
    private final String exchangeName;
    private String bindingKey;
    private Channel channel;
    private String message;
    private boolean readerattached;
    private MQCallback callback;
    
    // for 1-1 sender
    public MessageQ(String host, String qname, boolean qdurablity) throws Exception{
        this.message = "";
        this.host = host;
        this.qname = qname;
        this.exchangeName = ""; 
        this.readerattached = false;
        this.connect();
        this.createQ(qdurablity);
    }
    
    // for 1-1 receiver
    public MessageQ(String host, String qname, boolean qdurablity, MQCallback callback) 
            throws Exception{
        this.message = "";
        this.host = host;
        this.qname = qname;
        this.exchangeName = ""; 
        this.readerattached = false;
        this.connect();
        this.createQ(qdurablity);
        this.callback = callback;
        if (this.callback != null)
            this.readFromQueue();
    }
    
    // for 1-n sender
    public MessageQ(String host, String exchangeName, String exchangeOption, String routingKey) 
            throws Exception{
        this.message = "";
        this.qname = "";
        this.host = host;
        this.bindingKey = routingKey;
        this.exchangeName = exchangeName; 
        this.readerattached = false;
        this.connect();
        if (exchangeOption.isEmpty())
            this.createExchange("fanout");
        else
            this.createExchange(exchangeOption);
    }
    
    // for 1-n receiver
    public MessageQ(String host, String qname, String exchangeName, boolean qdurablity, 
            String exchangeOption, String routingKey, MQCallback callback) throws Exception{
        this.message = "";
        this.host = host;
        this.qname = qname;
        this.bindingKey = routingKey;
        this.exchangeName = exchangeName;
        this.readerattached = false;
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
        
    private int connect() throws Exception{
        /* comment to disable
        int prefetchCount = 1;
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(this.host);
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(10000); // 10 seconds
        Connection connection = factory.newConnection();
        this.channel = connection.createChannel();
        this.channel.basicQos(prefetchCount);
        */
        return 0;
    }
    
    private int createQ(boolean durable) throws Exception{
       /* comment to disable
       if (!this.qname.isEmpty()){
           try{
              this.channel.queueDeclarePassive(this.qname);
           } catch(IOException ex){
               if (!this.channel.isOpen())
                   this.connect();
                this.channel.queueDeclare(this.qname, durable, false, false, null);  
           } 
       }
       */
       return 0;
    }
    
    public void deleteQ(){
        /* comment to disable
        try{
         if (!this.qname.isEmpty())
             this.channel.queueDelete(this.qname);
        } catch(IOException e){
            System.out.println(e);
        }
      */
    }
    
    private void createExchange(String option) throws Exception{
        /* comment to disable
        if (!this.exchangeName.isEmpty()){
            try{
               this.channel.exchangeDeclarePassive(this.exchangeName);
            } catch(IOException ex){
                if (!this.channel.isOpen())
                   this.connect();
                
                this.channel.exchangeDeclare(this.exchangeName, option);
            }
        }
       */
    }
    
    public void deleteExchange(){
        /* comment to disable
        try{
            if (!this.exchangeName.isEmpty())
                this.channel.exchangeDelete(this.exchangeName);
        } catch(IOException e){
            System.out.println(e);
        }
      */
    }
    
    private void bindExchange() throws Exception{
        /* comment to disable
        if (!this.qname.isEmpty() && !this.exchangeName.isEmpty()){
            this.channel.queueBind(this.qname, this.exchangeName, this.bindingKey);
            System.out.println(" Queue Name : " + this.qname + " exchange name : " + this.exchangeName);
        } 
       */
    }    
    
    public int sendToQueue(String mesg) throws Exception{
        /* comment to disable
        String replyQueueName = channel.queueDeclare().getQueue();
        
        BasicProperties props = new BasicProperties
                .Builder()
                .replyTo(replyQueueName)
                .build();
        
        if (!this.channel.isOpen())
            this.connect();
        
        this.channel.basicPublish("", this.qname, props, mesg.getBytes());
       */
        return 0;
    }
    
    public int sendToExchange(String mesg, String routingKey) throws Exception{
        /* comment to disable
        if (!this.channel.isOpen())
            this.connect();
        
        String replyQueueName = channel.queueDeclare().getQueue();
        
        BasicProperties props = new BasicProperties
                .Builder()
                .replyTo(replyQueueName)
                .build();
        
        this.bindExchange();
        this.channel.basicPublish(this.exchangeName, routingKey, props, mesg.getBytes());
        */
        return 0;
    }
    
    private DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        long deliveryTag;
        MQResponse response;
        BasicProperties prop;
        
        message = new String(delivery.getBody(), "UTF-8");
        
        deliveryTag = delivery.getEnvelope().getDeliveryTag();
        prop = delivery.getProperties();
        //System.out.println("[START]  >" + message);
        if (this.callback != null){
            //System.out.println("bindingKey : " + delivery.getEnvelope().getRoutingKey() + " message : " + message);
            response = this.callback.call(delivery.getEnvelope().getRoutingKey(), message);
            if (prop != null){
                String cid = prop.getCorrelationId();
                if (cid != null){
                    if(!cid.isEmpty()) {
                        this.channel.basicPublish("", prop.getReplyTo(), prop, response.getResponseInByte());     
                    }
                }
            }
            //bindingKey :
            if ( response.getQAck()== 0) {              
                this.okAck(deliveryTag);
            }
            else if (response.getQAck() < 0)
                this.rejectAckWithDequeue(deliveryTag);
            else
                this.rejectAckWithRequeue(deliveryTag);
        }
        //System.out.println("[END]");
        //System.out.println("Recived : " + delivery.getEnvelope().getRoutingKey() + " : " + this.message);
    };
    
    private void readFromQueue()throws Exception{
        /* comment to disable
        if (!this.channel.isOpen()){
            this.connect();
        }
        
        if (this.callback != null)
            channel.basicConsume(this.qname, false, deliverCallback, consumerTag -> { });
        else
            channel.basicConsume(this.qname, true, deliverCallback, consumerTag -> { });
       */
    }
      
    public String getQueueName(){
        return this.qname;
    }
    
    public String getExchangeName(){
        return this.exchangeName;
    }
    
    public String get() throws Exception{
        /* comment to disable
        if (this.readerattached == false){
            this.readFromQueue();
            this.readerattached = true;
        }
        */
        String m = this.message;
        this.message="";
        return m;
    }
    
    public void addCallback(MQCallback callback) throws Exception{
        /* comment to disable
        this.callback = callback;
        this.readFromQueue();
       */
    }
    
    public void removeCallback(){
        this.callback = null;
    }
    
    // positively acknowledge a single delivery, the message will be discarded
    public void okAckSingle(long deliveryTag) throws IOException{
        /* comment to disable
        this.channel.basicAck(deliveryTag, false);
       */
    }
    
    // positively acknowledge all deliveries up to this delivery tag
    public void okAck(long deliveryTag) throws IOException{
        /* comment to disable
        this.channel.basicAck(deliveryTag, true);
        */
    }
    // requeue the delivery
    public void rejectAckWithRequeue(long deliveryTag)throws IOException{
        /* comment to disable
        this.channel.basicReject(deliveryTag, true);
        */
    }
     // negatively acknowledge, the message will be discarded
    public void rejectAckWithDequeue(long deliveryTag)throws IOException{
        /* comment to disable
        this.channel.basicReject(deliveryTag, false);
       */
    }
    
    // requeue all unacknowledged deliveries up to this delivery tag
    public void getBackNack(long deliveryTag) throws IOException{
        /* comment to disable
        this.channel.basicNack(deliveryTag, true, true);
        */
    }
}