Rabbit MQ Server is running at IP 192.168.11.76 

Rabbit MQ Receiver is running at IP xxx.xxx.xxx.a, xxx.xxx.xxx.b, xxx.xxx.xxx.c, xxx.xxx.xxx.c

Test MessageBox are one2oneBox for 1:1 testing

Test MessageBox are one2ManyBox for 1:N testing
                    
Use MQTest1to1 and MQTest1toMany for testing

//**************************************************************************/
We provide two class separate class for one to one and one to many date exchange

1. To import class
   -> for sender class
    import com.pspace.ifs.DSAN.MQ.MQSender;
   -> for receiver class 
    import com.pspace.ifs.DSAN.MQ.MQReceiver;

2. To create objects
   -> for one to one sender
	MQSender mq1to1= new MQSender(host, queueName, durablity);
        where:
           host  : is the host address of the rabbit MQ server
           queueName  : is the name of data queue will be used for exchange
           durablity  : the data sent or received on the queue is durable or not
       e.g,
        MQSender mq1to1= new MQSender("192.168.11.76", "testqueue1", false);
        *the data exchanged on "testqueue1" is will not long lasting on the queue

        MQSender mq1to1= new MQSender("192.168.11.76", "testqueue2", true);
        *the data exchanged on "testqueue2" is long lasting on the queue

-> for one to one receiver
	MQReceiver mq1to1= new MQReceiver(host, queueName, durablity, MQCallback callback);
        where:
           host  : is the host address of the rabbit MQ server
           queueName  : is the name of data queue will be used for exchange
           durablity  : the data sent or received on the queue is durable or not
           callback   : callback calss to receive the data
       e.g,
        MQReceiver mq1to1= new MQReceiver("192.168.11.76", "testqueue1", false, callback);
        *Assume the callback class already defined

  -> for one to many sender
      MQSender mq1ton = new MQSender(String host, String exchangeName, String exchangeOption, String routingKey);
     where:
         host refere to the 1-1 description above
         exchangeName : the name of exchager used to bind with the queeu 
         exchangeOption: the exchanger working mechanism direct, fanout,.. etc 
         routingKey   : the key used to routing to the specfic queue

      e.g.
         MQSender mq1tona = new MQSender("192.168.11.76", osdExchanger, fanout, "osd");
   
  -> for one to many receiver
      MQReceiver mq1ton = new MQReceiver(String host, String qname, String exchangeName, boolean queeuDurableity, String exchangeOption, String routingKey, MQCallback callback);
     where:
         host, qname,  refer to the 1-1 description above
         exchangeName : the name of exchager used to bind with the queeu 
         exchangeOption: the exchanger working mechanism direct, fanout,.. etc 
         routingKey   : the key used to routing to the specfic queue
         callback   : callback calss to receive the data
     e.g.
         MQReceiver mq1tonb = new MQReceiver("192.168.11.76", "testqueue1", osdExchanger, false, fanout, "key1", callback);
         MQReceiver mq1tonc = new MQReceiver("192.168.11.76", "testqueue1", osdExchanger, false, fanout, "key2", callback);
         *Two receiver with routing key key1 and key2

3. Send data
   for 1-1 and 1-n  data exchange
    mq1to1.send(message);
    where :
          message : the data to be sent 
    
  one additional send for 1-n data exchange
    mq1ton.send(message, routingKey);
    where:
         routingkey  : the message will be deliverd based on the routing key 
       
4. Receive data
   If there is no callback class defined, you can get the message as follow: 
   String msg = mq1to1.get();
    or  
   String msg = mq1ton.get();
   

TEST Programs:
File Name              class/ method                       function
TestMessageQ.java       oneToOneTest("host")     send and recive message 1-1
TestMessageQ.java       oneToManyTest("host")    send and recive message 1-n
TestTrashSender.java    TestTrashSender          alloc 2 osd disk and send the disk and path information
TestOsdReceiver.java     TestOsdReceiver           receive disk and path information and display
TestDiskInfoSender.java TestDiskInfoSender       Send disk information in json format 
TestDiskInfoReciver.java TestDiskInfoReciver     Receive disk information(you can run multiple instance of this program) 
