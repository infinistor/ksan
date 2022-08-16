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
from server.server_manage import *
import mqmanage.mq
from const.common import *
from const.server import ServerUsageItems, ServerStateItems
from common.init import GetConf
from common.shcommand import UpdateEtcHosts
from const.mq import MqVirtualHost, RoutKeyServerUsage, ExchangeName, RoutKeyServerState, \
    RoutKeyServerUpdateFinder, RoutKeyServerAddFinder, RoutKeyServerDelFinder
import socket
import time
import logging

def MonUpdateServerUsage(conf, logger):
    ServerUsageMq = mqmanage.mq.Mq(conf.mgs.PortalIp, int(conf.mgs.MqPort), MqVirtualHost, conf.mgs.MqUser, conf.mgs.MqPassword, RoutKeyServerUsage, ExchangeName)
    ServerStateMq = mqmanage.mq.Mq(conf.mgs.PortalIp, int(conf.mgs.MqPort), MqVirtualHost, conf.mgs.MqUser, conf.mgs.MqPassword, RoutKeyServerState, ExchangeName)
    while True:
        svr = GetServerUsage(conf.mgs.ServerId)
        svr.Get()
        server = ServerUsageItems()
        server.Set(conf.mgs.ServerId, svr.LoadAverage1M, svr.LoadAverage5M, svr.LoadAverage15M, svr.MemoryUsed)
        Mqsend = jsonpickle.encode(server, make_refs=False, unpicklable=False)
        print(Mqsend)
        Mqsend = json.loads(Mqsend)
        ServerUsageMq.Sender(Mqsend)

        ServerState = ServerStateItems(conf.mgs.ServerId, 'Online')
        Mqsend = jsonpickle.encode(ServerState, make_refs=False, unpicklable=False)
        print(Mqsend)
        Mqsend = json.loads(Mqsend)
        ServerStateMq.Sender(Mqsend)

        time.sleep(conf.monitor.ServerMonitorInterval)


def MqServerHandler(MonConf, RoutingKey, Body, Response, ServerId, logger):
    logger.debug("MqServerHandler %s %s" % (str(RoutingKey), str(Body)))
    try:
        ResponseReturn = mqmanage.mq.MqReturn(ResultSuccess)
        Body = Body.decode('utf-8')
        Body = json.loads(Body)
        body = DictToObject(Body)
        if RoutKeyServerUpdateFinder.search(RoutingKey) or RoutKeyServerAddFinder.search(RoutingKey):
            #IpAddress = body.NetworkInterfaces.IpAddress # not available
            pass

        elif RoutKeyServerDelFinder.search(RoutingKey):
            HostName = body.Name
            HostInfo = [('', HostName)]
            UpdateEtcHosts(HostInfo, 'remove')
            logging.log(logging.INFO, 'host is removed. %s' % str(HostInfo))
            if ServerId == body.Id:
                ret, errlog = RemoveQueue()
                if ret is False:
                    logging.error('fail to remove queue %s' % errlog)
                else:
                    logging.log(logging.INFO, 'success to remove queue')
                if os.path.exists(MonServicedConfPath):
                    os.unlink(MonServicedConfPath)
                    logging.log(logging.INFO, 'ksanMonitor.conf is removed')

        '''
        if RoutKeyServerUpdateFinder.search(RoutingKey):
            ResponseReturn = mqmanage.mq.MqReturn(ResultSuccess)
            Body = Body.decode('utf-8')
            Body = json.loads(Body)
            body = DictToObject(Body)
            if ServerId == body.ServerId:
                ret = CheckDiskMount(body.Path)
                if ret is False:
                    ResponseReturn = mqmanage.mq.MqReturn(ret, Code=1, Messages='No such disk is found')
                Response.IsProcessed = True
            print(ResponseReturn)
            return ResponseReturn
        '''
        return ResponseReturn
    except Exception as err:
        print(err)

def RemoveQueue():
    ret, conf = GetConf(MonServicedConfPath)
    if ret is True:
        QueueHost = conf.mgs.PortalIp
        QueueName = 'IfsEdge-%s' % conf.mgs.ServerId
        mqmanage.mq.RemoveQueue(QueueHost, QueueName)
        return True, ''
    else:
        return False, 'fail to read ksanMonitor.conf'

