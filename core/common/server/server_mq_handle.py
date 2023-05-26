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
#from common.common.mqutils import Mq
from server.server_api import GetServerUsage
import mqmanage.mq
from const.common import *
from const.server import ServerUsageItems, ServerStateItems
from common.base_utils import GetConf, WaitAgentConfComplete, GetAgentConfig
from const.mq import MqVirtualHost, RoutKeyServerUsage, ExchangeName, RoutKeyServerState, \
    RoutKeyServerUpdateFinder, RoutKeyServerAddFinder, RoutKeyServerDelFinder
import socket
import time
import logging
import inspect

def MonUpdateServerUsage(Conf, logger):

    Conf = WaitAgentConfComplete(inspect.stack()[1][3], logger)
    conf = GetAgentConfig(Conf)
    ServerUsageMq = mqmanage.mq.Mq(conf.MQHost, int(conf.MQPort), MqVirtualHost, conf.MQUser, conf.MQPassword, RoutKeyServerUsage, ExchangeName, logger=logger)
    ServerStateMq = mqmanage.mq.Mq(conf.MQHost, int(conf.MQPort), MqVirtualHost, conf.MQUser, conf.MQPassword, RoutKeyServerState, ExchangeName, logger=logger)
    ServerMonitorInterval = int(conf.ServerMonitorInterval)/1000
    while True:
        svr = GetServerUsage(conf.ServerId)
        svr.Get()
        server = ServerUsageItems()
        server.Set(conf.ServerId, svr.LoadAverage1M, svr.LoadAverage5M, svr.LoadAverage15M, svr.MemoryUsed)
        Mqsend = jsonpickle.encode(server, make_refs=False, unpicklable=False)
        print(Mqsend)
        Mqsend = json.loads(Mqsend)
        ServerUsageMq.Sender(Mqsend)

        ServerState = ServerStateItems(conf.ServerId, 'Online')
        Mqsend = jsonpickle.encode(ServerState, make_refs=False, unpicklable=False)
        print(Mqsend)
        Mqsend = json.loads(Mqsend)
        ServerStateMq.Sender(Mqsend)

        time.sleep(ServerMonitorInterval)
