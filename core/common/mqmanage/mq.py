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


# -*- coding: utf-8 -*-

import os, sys
import pdb
if os.path.dirname(os.path.abspath(os.path.dirname(__file__))) not in sys.path:
    sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from ksan.common.define import *
from ksan.mqmanage.RabbitMqReceiver import RabbitMqReceiver
from ksan.mqmanage.RabbitMqConfiguration import RabbitMqConfiguration
from ksan.mqmanage.RabbitMqRpc import RabbitMqRpc
from ksan.mqmanage.RabbitMqSender import RabbitMqSender
import json

class Mq:
    def __init__(self, Host, Port, VirtualHost, User, Password, RoutingKey, ExchangeName, QueueName=None, ServerId=None,
                 ServiceList=None, LocalIpList=None, logger=None):
        self.config = RabbitMqConfiguration()
        self.config.host = Host
        self.config.port = Port
        self.config.virtualHost = VirtualHost
        self.config.user = User
        self.config.password = Password
        self.config.exchangeName = ExchangeName
        self.routingkey = RoutingKey
        self.queuename = QueueName
        self.ServerId = ServerId
        self.ServiceList = ServiceList
        self.LocalIpList = LocalIpList
        self.logger = logger

    def Sender(self, Data):
        mqSender = RabbitMqSender(self.config)
        mqSender.send(self.config.exchangeName, self.routingkey, Data)

    def Receiver(self):
        RabbitMqReceiver(self.config, self.queuename, self.config.exchangeName, bindingKeys=self.routingkey,
                         ServerId=self.ServerId, ServiceList=self.ServiceList, LocalIpList=self.LocalIpList)

    def Rpc(self, Data):
        mqRpc = RabbitMqRpc(self.config)
        mqRpc.send(self.config.exchangeName, self.config.exchangeName, Data)


@catch_exceptions()
def MqReturn(Result, Code=0, Messages='', Data=None):
    Ret = dict()
    if Result is True or Result == ResultSuccess:
        Ret['Result'] = 'Success'
    else:
        Ret['Result'] = 'Error'
    Ret['Code'] = Code

    Ret['Message'] = Messages
    if Data:
        Ret['Data'] = Data
    #Ret['Data'] = json.dumps(Ret)
    return json.dumps(Ret)
