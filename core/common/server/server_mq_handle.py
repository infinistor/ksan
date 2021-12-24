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
sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
#from common.common.mqutils import Mq
from ksan.server.server_manage import *
import ksan.mqmanage
from ksan.common.define import *
import time

def MonUpdateServerUsage(conf):
    ServerUsageMq = ksan.mqmanage.mq.Mq(conf.mgs.MgsIp, int(conf.mgs.MqPort), MqVirtualHost, MqUser, MqPassword, RoutKeyServerUsage, ExchangeName)
    ServerStateMq = ksan.mqmanage.mq.Mq(conf.mgs.MgsIp, int(conf.mgs.MqPort), MqVirtualHost, MqUser, MqPassword, RoutKeyServerState, ExchangeName)
    while True:
        svr = GetServerUsage(conf.mgs.ServerId)
        svr.Get()
        server = ServerUsageItems()
        server.Set(conf.mgs.ServerId, svr.LoadAverage1M, svr.LoadAverage5M, svr.LoadAverage15M, svr.MemoryUsed)
        Mqsend = jsonpickle.encode(server, make_refs=False, unpicklable=False)
        print(Mqsend)
        Mqsend = json.loads(Mqsend)
        ServerUsageMq.Sender(Mqsend)

        ServerState = ServerStateItems('Online')
        Mqsend = jsonpickle.encode(ServerState, make_refs=False, unpicklable=False)
        print(Mqsend)
        Mqsend = json.loads(Mqsend)
        ServerStateMq.Sender(Mqsend)

        time.sleep(ServerMonitorInterval)


def MqServerHandler(RoutingKey, Body, Response, ServerId):
    ResponseReturn = ksan.mqmanage.mq.MqReturn(ResultSuccess)
    Response.IsProcessed = True
    return ResponseReturn
