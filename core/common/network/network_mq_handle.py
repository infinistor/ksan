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
import time
if os.path.dirname(os.path.abspath(os.path.dirname(__file__))) not in sys.path:
    sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))

#from const.common import *
from const.network import RequestNetworkInterfaceStat, NetworkInterfaceLinkStateItems
#from const.mq import RoutKeyNetworkUsage, ExchangeName, RoutKeyNetworkLinkState, RoutKeyNetworkAddFinder, \
#    RoutKeyNetworkUpdateFinder, RoutKeyNetworkAddedFinder
from common.utils import *
from const.common import *
from mqmanage.mq import *
from portal_api.apis import *


@catch_exceptions()
def UpdateNetworkStat(Conf, GlobalFlag, logger):
    """

    :param Conf: g_Conf
    :param GlobalFlag:
    :param logger:
    :return:
    """
    Conf = WaitAgentConfComplete(inspect.stack()[1][3], logger, CheckNetworkDevice=True, CheckNetworkId=True)

    conf = GetAgentConfig(Conf)
    # Create Sender
    #mqSender = RabbitMqSender(config)
    #mqSender.send(config.exchangeName, "*", data)
    LocalDev = GetNetwork()

    NetworkUsageMq = Mq(conf.MQHost, int(conf.MQPort), '/', conf.MQUser, conf.MQPassword, RoutKeyNetworkUsage, ExchangeName, logger=logger)
    NetworkLinkStateMq = Mq(conf.MQHost, int(conf.MQPort), '/', conf.MQUser, conf.MQPassword, RoutKeyNetworkLinkState, ExchangeName, logger=logger)
    #Res, Errmsg, Ret, AllNetDevs = GetNetworkInterface(conf.mgs.MgsIp, int(conf.mgs.IfsPortalPort), conf.mgs.ServerId)
    NetworkMonitorInterval = int(conf.NetworkMonitorInterval)/1000
    while True:
        Res, Errmsg, Ret, Svr = GetServerInfo(conf.PortalHost, int(conf.PortalPort), conf.PortalApiKey, ServerId=conf.ServerId, logger=logger)
        if Res == ResOk:
            if Ret.Result == ResultSuccess:
                while True:
                    if GlobalFlag['NetworkUpdated'] == Updated:
                        GlobalFlag['NetworkUpdated'] = Checked
                        logging.log(logging.INFO,'Network Info is Updated')
                        break

                    for dev in Svr.NetworkInterfaces:
                        try:
                            #stat1 = LocalDev.IoCounterPerNic
                            stat1 = psutil.net_io_counters(pernic=True)
                            #LocalDev.ReNewalNicStat()
                            time.sleep(1)
                            # Update Network Io(Rx/Tx) bytes per 1 sec
                            #stat2 = LocalDev.IoCounterPerNic
                            stat2 = psutil.net_io_counters(pernic=True)
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

                    time.sleep(NetworkMonitorInterval)

            else:
                print('fail to get the registered network info', Ret.Message)
        else:
            print('fail to get the registered network info', Errmsg)

        time.sleep(IntervalMiddle)

