#!/usr/bin/env python3
"""
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
"""


import os, sys
if os.path.dirname(os.path.abspath(os.path.dirname(__file__))) not in sys.path:
    sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
#from service.service_mq_handle import MqServiceHandler
#import mqmanage
from mqmanage.RabbitMqConfiguration import RabbitMqConfiguration
from portal_api.apis import *

import json
import pika


class RabbitMqReceiver:
    """ 생성자
    Args:
        config: Rabbit MQ 설정 객체
    Returns:
        없음
    """

    def __init__(self, config: RabbitMqConfiguration, queueName: str, exchangeName: str, bindingKeys: tuple,
                 ServerId=None, ServiceList=None, LocalIpList=None, logger=None, MonConf=None, GlobalFlag=None):
        self.m_config = config
        self.m_queue_name = queueName
        self.m_exchange_name = exchangeName
        self.m_binding_keys = bindingKeys
        self.m_connection = None
        self.m_channel = None
        self.m_response = None
        self.m_correlation_id = None
        self.ServerId = ServerId
        self.ServiceList = ServiceList
        self.LocalIpList = LocalIpList
        self.logger = logger
        self.MonConf = MonConf
        self.GlobalFlag = GlobalFlag  # {'ServiceUpdated': Updated, 'DiskUpdated': Updated, 'NetworkUpdated': Checked}
        # Rabbit MQ 리스너 초기화
        while True:
            try:
                self.initializeRabbitMqListener(self.m_config)

                #result = self.m_channel.queue_declare(queue='', exclusive=True)
                #self.m_callback_queue = result.method.queue
                print(self.m_queue_name)
                self.m_channel.basic_consume(
                    queue=self.m_queue_name,
                    on_message_callback=self.on_response,
                    auto_ack=False)

                self.m_channel.start_consuming()
            except pika.exceptions.AMQPChannelError as err:
                logger.error('1rabbitmq server connection fail %s %d' % (str(err), sys.exc_info()[2].tb_lineno))
                time.sleep(5)
                continue
            except Exception as err:
                logger.error('2rabbitmq server connection fail %s %d' % (str(err), sys.exc_info()[2].tb_lineno))
                time.sleep(5)


    """ Rabbit MQ 리스너 초기화
    Args:
        config: Rabbit MQ 설정 객체
    Returns:
        없음
    """

    def initializeRabbitMqListener(self, config: RabbitMqConfiguration):

        # 계정 정보 생성
        credentials = pika.PlainCredentials(username=config.user, password=config.password)

        # 연결 파라미터 생성
        connectionParams = pika.ConnectionParameters(
            host=config.host
            , port=config.port
            , virtual_host=config.virtualHost
            , credentials=credentials
        )

        # 연결 객체 생성
        self.m_connection = pika.BlockingConnection(connectionParams)
        # 채널  생성
        self.m_channel = self.m_connection.channel()

        # 큐 설정
        mq_args = {}
        mq_args["x-queue-type"] = "quorum"
        mq_args["x-single-active-consumer"] = True

        self.m_channel.queue_declare(queue=self.m_queue_name,
                                     durable=True, exclusive=False, auto_delete=False, arguments=mq_args)
        # Exchange 설정
        self.m_channel.exchange_declare(exchange=self.m_exchange_name, exchange_type="topic")
        # 모든 바인딩 키 처리
        if len(self.m_binding_keys) > 0:
            for binding_key in self.m_binding_keys:
                self.m_channel.queue_bind(queue=self.m_queue_name, exchange=self.m_exchange_name,
                                          routing_key=binding_key)

    """ 소멸자
    Args:
        없음
    Returns:
        없음
    """

    def __del__(self):
        # 채널 연결이 되어 있는 경우, 채널 연결 해제
        if self.m_channel is not None:
            self.m_channel.close()
            self.m_channel = None

        # 연결이 되어 있는 경우, 연결 해제
        if self.m_connection is not None:
            self.m_connection.close()
            self.m_connection = None

    """ 메세지 수신 시 호출되는 콜백
    """

    def on_response(self, ch, method, props, body):
        #print(" [x] routing_key: %r, body: %r" % (method.routing_key, body))

        response = self.handleMessage(method.routing_key, body)
        ResponseReturn = self.MqReceiverHanlder(method.routing_key, body, response)

        if response.IsProcessed:
            # 연관 아이디가 있는 경우
            if props.correlation_id is not None:
                if len(props.correlation_id) > 0:
                    #response = json.dumps({"Result": "Success", "Code": "", "Messsage": ""})
                    #print(response)
                    ch.basic_publish(exchange='',
                                     routing_key=props.reply_to,
                                     properties=pika.BasicProperties(correlation_id=props.correlation_id),
                                     body=str(ResponseReturn))

            self.m_channel.basic_ack(delivery_tag=method.delivery_tag, multiple=False)  # in case job is processed, return
        else:
            self.m_channel.basic_reject(delivery_tag=method.delivery_tag, requeue=True)  # requeue

    def handleMessage(self, RoutingKey, Body) -> ResponseMqData:
        """
        Create ResponseMqData Format
        """
        response = ResponseMqData(EnumResponseResult.Error, "", "")
        #self.RabbitMqReceiver(RoutingKey, Body, response)
        return response

    def MqReceiverHanlder(self, RoutingKey, body, Response):
        """
        Message Queue Handler
        1. Disk
        2. Serivce
        3. Network
        """
        #print(RoutingKey, body)

        if RoutKeyDisk in RoutingKey or RoutKeyDiskRpcFinder.search(RoutingKey) or RoutKeyDiskPool in RoutingKey:
            ResponseReturn = MqDiskHandler(RoutingKey, body, Response, self.ServerId, self.GlobalFlag, self.logger)
            return ResponseReturn
            #Response.IsProcessed = True
        elif RoutKeyService in RoutingKey or RoutKeyServiceRpcFinder.search(RoutingKey):
            ResponseReturn = MqServiceHandler(self.MonConf, RoutingKey, body, Response, self.ServerId, self.ServiceList, self.LocalIpList, self.GlobalFlag,  self.logger)
            #Response.IsProcessed = True
            return ResponseReturn
        elif RoutKeyNetwork in RoutingKey or RoutKeyNetworkRpcFinder.search(RoutingKey):
            ResponseReturn = MqNetworkHandler(self.MonConf, RoutingKey, body, Response, self.ServerId, self.ServiceList, self.GlobalFlag, self.logger)
            Response.IsProcessed = True
            return ResponseReturn
        elif RoutKeyServerUpdate in RoutingKey or RoutKeyServerDel in RoutingKey or RoutKeyServerAdd in RoutingKey:
            ResponseReturn = MqServerHandler(self.MonConf, RoutingKey, body, Response, self.ServerId, self.logger)
            Response.IsProcessed = True
            return ResponseReturn
        else:
            self.logger.debug("Skip RouteKey: %s" % str(RoutingKey))
            Response.IsProcessed = True
            ResponseReturn = MqReturn(ResultSuccess)
            return ResponseReturn


#print(body)


"""
class Mq:
    def __init__(self, Host, Port, VirtualHost, User, Password, RoutingKey, ExchangeName, QueueName=None):
        self.config = RabbitMqConfiguration()
        self.config.host = Host
        self.config.port = Port
        self.config.virtualHost = VirtualHost
        self.config.user = User
        self.config.password = Password
        self.config.exchangeName = ExchangeName
        self.routingkey = RoutingKey
        self.queuename = QueueName

    def Sender(self, Data):
        mqSender = RabbitMqSender(self.config)
        mqSender.send(self.config.exchangeName, self.routingkey, Data)

    def Receiver(self):
        RabbitMqReceiver(self.config, self.queuename, self.config.exchangeName, bindingKeys=self.routingkey)

    def Rpc(self, Data):
        mqRpc = RabbitMqRpc(self.config)
        mqRpc.send(self.config.exchangeName, self.config.exchangeName, Data)
"""
