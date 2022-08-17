#!/bin/env python3
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

import os
import sys
sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
import time
import datetime
import psutil
import netifaces
import dns.resolver
from common.httpapi import *


class GetNetwork(object):
    def __init__(self, Ip=None, logger=None):
        self.logger = logger
        self.NicInfo = None
        self.nicinfo_list = list()
        self.GetNicInfo(Ip=Ip)
        self.IoCounterPerNic = psutil.net_io_counters(pernic=True)

    @catch_exceptions()
    def GetNicInfo(self, Ip=None):
        """
        get all detail nic info list.
        :return:
        """
        # get dns info
        dnsresolvers = dns.resolver.Resolver()

        # get status of each nic(isup(tru/false), duplex, speed, mtu)
        netif_stat = psutil.net_if_stats()
        # get nic info. netif_addresses includes {'eno1':..., 'eno2':...}
        netif_addresses = psutil.net_if_addrs()
        for nic in netif_addresses.keys():
            if nic == 'lo' or netifaces.AF_INET not in netifaces.ifaddresses(nic):
                continue
            tmp_dic = dict()
            tmp_dic['Name'] = nic
            tmp_dic['Dhcp'] = 'No'  # must 'No'
            tmp_dic['MacAddress'] = netifaces.ifaddresses(nic)[netifaces.AF_LINK][0]['addr']
            tmp_dic['LinkState'] = 'Up' if netif_stat[nic].isup is True else 'Down'
            tmp_dic['IpAddress'] = netifaces.ifaddresses(nic)[netifaces.AF_INET][0]['addr']
            tmp_dic['SubnetMask'] = netif_addresses[nic][0].netmask
            tmp_dic['BandWidth'] = netif_stat[nic].speed
            tmp_dic['Gateway'] = self.GetGatewayWithNic(nic)
            tmp_dic['Dns1'] = ''
            tmp_dic['Dns2'] = ''
            for idx, nameserver in enumerate(dnsresolvers.nameservers, start=1):
                key = 'Dns%d' % idx
                tmp_dic[key] = nameserver

            if tmp_dic['Dns2'] == '':
                tmp_dic['Dns2'] = tmp_dic['Dns1']


            self.nicinfo_list.append(tmp_dic)
            if Ip is not None:
                if Ip == tmp_dic['IpAddress']:
                    self.NicInfo = tmp_dic
                    return tmp_dic

    @catch_exceptions()
    def GetGatewayWithNic(self, NicName):
        gw = netifaces.gateways()
        for gateway, nic, is_default in gw[netifaces.AF_INET]:
            if NicName == nic:
                return gateway
        return ''


    @catch_exceptions()
    def GetNicInfoWithNicName(self, NicName):
        """
        return specific nic info with nic name
        :return: success to get nic info {'name', <nic name>, 'Dhcp': <dhcp ip>, 'MacAddress':<MacAdress ip>, 'LinkState': <true/false>,
        'addr', <address ip>, 'netask': <netmask>, 'gateway': <gateway>, 'Dns1': <dns1 ip>, 'Dns2': <dns2 ip>, 'Dns3':...}, fail to get nic info: None
        """
        if NicName is None:
            print('nic is None')
        else:
            for nic in self.nicinfo_list:
                if nic['Name'] == NicName:
                    return nic
            return None


    def ReNewalNicStat(self):
        """
        Update Nic stat
        :return:
        """
        self.IoCounterPerNic = psutil.net_io_counters(pernic=True)
        self.GetNicInfo()




