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
#from common.log import catch_exceptions
#from const.common import *


class NetworkInterfaceItems:
    def __init__(self):
        self.Id = ''
        self.ServerId = ''
        self.Name = ''
        self.Description = ''
        self.Dhcp = ''
        self.MacAddress = ''
        self.LinkState = ''
        self.IpAddress = ''
        self.SubnetMask = ''
        self.Gateway = ''
        self.Dns1 = ''
        self.Dns2 = ''
        self.BandWidth = ''
        self.IsManagement = None
        self.Rx = None
        self.Tx = None
        self.ModDate = ''
        self.ModId = ''
        self.ModName = ''


class RequestNetworkInterfaceItems(object):
    """
    request to register network info to ifsportalsvr
    """
    def __init__(self, NicName, Description='', logger=None):
        self.Name = NicName
        self.Description = Description
        self.Dhcp = 'No'
        self.MacAddress = ''
        self.LinkState = ''
        self.IpAddress = ''
        self.SubnetMask = ''
        self.BandWidth = ''
        self.Gateway = ''
        self.Dns1 = ''
        self.Dns2 = ''

    def Set(self, Nic):
        self.Name = Nic['Name']
        self.Description = self.Description
        self.Dhcp = Nic['Dhcp']
        self.MacAddress = Nic['MacAddress']
        self.LinkState = Nic['LinkState']
        self.IpAddress = Nic['IpAddress']
        self.SubnetMask = Nic['SubnetMask']
        self.BandWidth = Nic['BandWidth']
        self.Gateway = Nic['Gateway']
        self.Dns1 = Nic['Dns1']
        self.Dns2 = Nic['Dns2']


class VlanNetworkInterfaceItems:
    def __init__(self):
        self.Id = None
        self.ServerId = None
        self.InterfaceId = None
        self.Tag = None
        self.IpAddress = None
        self.SubnetMask = None
        self.Gateway = None
        self.BandWidth = None
        self.Rx = 0
        self.Tx = 0
        self.ModDate = None
        self.ModId = None
        self.ModName = None
        self.RegDate = None
        self.RegId = None
        self.RegName = None


class RequestNetworkInterfaceCheck(object):
    """
    request to register network info to ifsportalsvr
    """
    def __init__(self, NicName, logger=None):
        self.Name = NicName


class NetworkInterfaceLinkStateItems(object):
    """
    request to update network interface status to ifsportalsvr
    """

    def __init__(self, InterfaceId, ServerId, LinkState):
        self.ServerId = ServerId
        self.InterfaceId = InterfaceId
        self.LinkState = LinkState


class RequestNetworkInterfaceStat(object):
    """
    request to update network interface stat to ifsportalsvr
    """

    def __init__(self, NicId, ServerId, Rx, Tx):
        self.ServerId = ServerId
        self.InterfaceId = NicId
        self.Rx = Rx
        self.Tx = Tx

class RequestVlanNetworkInterfaceInfo(object):
    """
    request to register vlan network info to ifsportalsvr
    """
    def __init__(self, Tag=None, IpAddress=None, SubnetMask=None, Gateway=None):
        self.Tag = ''
        self.IpAddress = ''
        self.SubnetMask = ''
        self.Gateway = ''
        if Tag is not None:
            self.Tag = Tag
            self.IpAddress = IpAddress
            self.SubnetMask = SubnetMask
            self.Gateway = Gateway

class ResPonseVlanNetworkInterfaceItems(object):
    def __init__(self, dic):
            self.Id = dic["Id"]
            self.InterfaceId = dic["InterfaceId"]
            self.Tag = dic["Tag"]
            self.IpAddress = dic["IpAddress"]
            self.SubnetMask = dic["SubnetMask"]
            self.Gateway = dic["Gateway"]
            self.ModDate = dic["ModDate"]
            self.ModId = dic["ModId"]
            self.ModName = dic["ModName"]
            self.RegDate = dic["RegDate"]
            self.RegId = dic["RegId"]
            self.RegName = dic["RegName"]


class RequestVlanNetworkInterfaceCheck(object):
    """
    request to register network info to ifsportalsvr
    """
    def __init__(self, Tag, logger=None):
        self.Tag = int(Tag)
