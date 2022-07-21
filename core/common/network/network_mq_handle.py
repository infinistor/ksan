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
sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
import mqmanage.mq
from common.shcommand import UpdateEtcHosts
from network.network_manage import *
from mqmanage.mq import *
import jsonpickle
import logging

@catch_exceptions()
def UpdateNetworkStat(conf, GlobalFlag, logger):

    # Create Sender
    #mqSender = RabbitMqSender(config)
    #mqSender.send(config.exchangeName, "*", data)
    LocalDev = GetNetwork()

    NetworkUsageMq = Mq(conf.mgs.PortalIp, int(conf.mgs.MqPort), '/', conf.mgs.MqUser, conf.mgs.MqPassword, RoutKeyNetworkUsage, ExchangeName)
    NetworkLinkStateMq = Mq(conf.mgs.PortalIp, int(conf.mgs.MqPort), '/', conf.mgs.MqUser, conf.mgs.MqPassword, RoutKeyNetworkLinkState, ExchangeName)
    #Res, Errmsg, Ret, AllNetDevs = GetNetworkInterface(conf.mgs.MgsIp, int(conf.mgs.IfsPortalPort), conf.mgs.ServerId)

    while True:
        Res, Errmsg, Ret, Svr = GetServerInfo(conf.mgs.PortalIp, int(conf.mgs.PortalPort), conf.mgs.PortalApiKey, ServerId=conf.mgs.ServerId, logger=logger)
        if Res == ResOk:
            if Ret.Result == ResultSuccess:
                while True:
                    if GlobalFlag['NetworkUpdated'] == Updated:
                        GlobalFlag['NetworkUpdated'] = Checked
                        logging.log(logging.INFO,'Network Info is Updated')
                        break

                    for dev in Svr.NetworkInterfaces:
                        try:
                            stat1 = LocalDev.IoCounterPerNic
                            LocalDev.ReNewalNicStat()
                            time.sleep(1)
                            # Update Network Io(Rx/Tx) bytes per 1 sec
                            stat2 = LocalDev.IoCounterPerNic
                            RxPerSec = stat2[dev.Name].bytes_recv - stat1[dev.Name].bytes_recv
                            TxPerSec = stat2[dev.Name].bytes_sent - stat1[dev.Name].bytes_sent
                            stat = RequestNetworkInterfaceStat(dev.Id, dev.ServerId, RxPerSec, TxPerSec)
                            MqSend = jsonpickle.encode(stat, unpicklable=False)
                            MqSend = json.loads(MqSend)
                            NetworkUsageMq.Sender(MqSend)

                            # Update Network Link Status
                            LinkStat = NetworkInterfaceLinkStateItems(dev.Id, dev.ServerId, 'Up')
                            MqSend = jsonpickle.encode(LinkStat, unpicklable=False)
                            MqSend = json.loads(MqSend)
                            NetworkLinkStateMq.Sender(MqSend)
                        except Exception as err:
                            print(err)

                    time.sleep(int(conf.monitor.NetworkMonitorInterval))

            else:
                print('fail to get the registered network info', Ret.Message)
        else:
            print('fail to get the registered network info', Errmsg)

        time.sleep(IntervalMiddle)

@catch_exceptions()
def MqNetworkHandler(MonConf, RoutingKey, Body, Response, ServerId, ServiceList, GlobalFlag, logger):
    logger.debug("%s %s %s" % (str(RoutingKey), str(Body), str(Response)))
    ResponseReturn = mqmanage.mq.MqReturn(ResultSuccess)
    Body = Body.decode('utf-8')
    Body = json.loads(Body)
    body = DictToObject(Body)
    if RoutKeyNetworkAddFinder.search(RoutingKey) or RoutKeyNetworkUpdateFinder.search(RoutingKey):
        GlobalFlag['NetworkUpdated'] = Updated
        if body.ServerId == ServerId:
            Response.IsProcessed = True
            ret, errmsg = ManageNetworkInterface()
            if ret is False:
                ResponseReturn = mqmanage.mq.MqReturn(ret, Code=1, Messages='fail')
            print(ResponseReturn)
        return ResponseReturn

    elif RoutKeyNetworkAddedFinder.search(RoutingKey):
            ServerId = body.ServerId
            IpAddress = body.IpAddress
            Res, Errmsg , Ret, Data = GetServerInfo(MonConf.mgs.PortalIp, int(MonConf.mgs.PortalPort),MonConf.mgs.PortalApiKey,  ServerId=ServerId, logger=logger)
            if Res == ResOk:
                if Ret.Result == ResultSuccess:
                    HostName = Data.Name
                    HostInfo = [(IpAddress, HostName)]
                    UpdateEtcHosts(HostInfo, 'add')
                    logging.log(logging.INFO, 'new host is added. %s' % str(HostInfo))
                else:
                    logger.error('fail to add hostname to /etc/hosts with ip: %s %s' % (IpAddress, Ret.Message))
            else:
                logger.error('fail to add hostname to /etc/hosts with ip: %s %s' % (IpAddress, Errmsg))



    else:
        Response.IsProcessed = True
        return ResponseReturn
