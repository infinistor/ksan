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
from ksan.common.network import GetNetwork
from ksan.common.display import *
from ksan.server.server_manage import *


def ManageNetworkInterface():
    return True, ''

def GetNetworkInterface(ip, port, ServerId, InterfaceId=None, disp=False, logger=None):
    """
    get server info all or specific server info with Id
    :param ip:
    :param port:
    :param ServerId
    :param NicId
    :param disp:
    :param logger
    :return:
    """
    ReturnType = NetworkInterfaceItemsModule
    ItemsHeader = True
    if ServerId is None:
        return ResInvalidCode, ResInvalidMsg + 'Serverid is required', None
    if InterfaceId is not None:
        Url = "/api/v1/Servers/%s/NetworkInterfaces/%s" % (ServerId, InterfaceId)
        ItemsHeader = False
    else:
        Url = "/api/v1/Servers/%s/NetworkInterfaces" % ServerId
    Params = dict()
    Params['countPerPage'] = 100
    Conn = RestApi(ip, port, Url, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.get(ItemsHeader=ItemsHeader, ReturnType=ReturnType)
    if Res == ResOk:
        AllNetworks = list()
        if Ret.Result == ResultSuccess:
            if InterfaceId is not None:
                return Res, Errmsg, Ret, Ret.Data
            else:
                return Res, Errmsg, Ret, Ret.Data.Items
        else:
            return Res, Errmsg, Ret, AllNetworks
    else:
        return Res, Errmsg, None, None


@catch_exceptions()
def GetAllNetworkInterface(Ip, Port, ServerId=None, logger=None):
    Res, Errmsg , Ret, ServerList = GetServerInfo(Ip, Port, logger=logger)
    if Res != ResOk:
        return Res, Errmsg, None, None
    else:
        AllNetworkList = list()
        for Svr in ServerList:
            Res, Errmsg ,Ret, NetworkList = GetNetworkInterface(Ip, Port, Svr.Id, logger=logger)
            if Res != ResOk:
                print('fail to get network interface info in %s: %s' % (Svr.Name, Errmsg))
            else:
                if Ret.Result != ResultSuccess:
                    print('fail to get network interface info in %s: %s' % (Svr.Name, Ret.Message))
                else:
                    AllNetworkList += NetworkList

        return Res, Errmsg, Ret, AllNetworkList


@catch_exceptions()
def GetNetworkInterfaceDetailWithId(Ip, Port, InterfaceId, logger=None):
    """
    Get Server Detail info with Interface Id
    :param InterfaceId:
    :param logger:
    :return:If Success, ServerItemsDetailModule ojbect, otherwise None
    """
    Res, Errmsg, Ret, Data = GetAllServerDetailInfo(Ip, Port, logger=logger)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            for Svr in Data:
                for Net in Svr.NetworkInterfaces:
                    if InterfaceId == Net.Id:
                        return ResOk, Errmsg, Net, Svr.Id
            return ResNotFoundCode, ResNotFoundMsg, None, None
        else:
            return Ret.Result, Ret.Message, None, None
    else:
        return Res, Errmsg, None, None



@catch_exceptions()
def ShowNetworkInterfaceInfo(ServersDetail, InterfaceId=None, Detail=False):
    """
    Display Network Interface Info
    :param InterfaceList: NetworkInterfaceItems class list
    :param NicId:
    :return:
    """
    if Detail:
        title ="%s%s%s%s%s%s%s%s%s%s" % ('ServerName'.center(40), 'Interface Name'.center(20),   'Interface Id'.center(40),
                                 'Description'.center(30), 'IpAddress'.center(20), 'MacAddress'.center(20),
                                 'LinkStatus'.center(20),'BandWidth'.center(15),'Rx'.center(10),'Tx'.center(10)) # 'ModeDate'.center(20), 'ModId'.center(20), 'ModName'.center(20), 'Id'.center(30))
    else:
        title ="%s%s%s%s%s%s" % ('ServerName'.center(40), 'Interface Name'.center(20), 'LinkStatus'.center(20),'BandWidth'.center(15),'Rx'.center(10),'Tx'.center(10))

    print(title)
    for Svr in ServersDetail:
        for Net in Svr.NetworkInterfaces:
            if InterfaceId is not None and Net.Id != InterfaceId:
                continue
            if Detail:
                _Net ="%s%s%s%s%s%s%s%s%s%s" % (Svr.Name.center(40), Net.Name.center(20),
                                        Net.Id.center(40),'{:30.30}'.format(str(Net.Description).center(30)),
                                        '{:20.20}'.format(Net.IpAddress.center(20)), Net.MacAddress.center(20),
                                        str(Net.LinkState).center(20), str(Net.BandWidth).center(15),
                                        str(Net.Rx).center(10), str(Net.Tx).center(10))
            else:
                _Net ="%s%s%s%s%s%s" % (Svr.Name.center(40), Net.Name.center(20),str(Net.LinkState).center(20), str(Net.BandWidth).center(15),
                                        str(Net.Rx).center(10), str(Net.Tx).center(10))
            print(_Net)

            #for Vlan in Net.NetworkInterfaceVlans:
            #_nic ="%s%s%s%s%s%s%s%s" % (Svr.Id.center(40), Svr.Net.Name.center(40),
            #                              '{:20.20}'.format(interface.Name.center(20)),'{:30.30}'.format(str(interface.Description).center(30)), '{:20.20}'.format(interface.IpAddress.center(20)), interface.MacAddress.center(20), str(interface.LinkState).center(20), '{:40.40}'.format(interface.ServerId.center(40))) # svr.ModDate.center(20), svr.ModId.center(20), svr.ModName.center(20), svr.Id.center(30))


@catch_exceptions()
def GetVlanNetworkInterfaces(ip, port, ServerId, NicId, VlanId=None, disp=False, logger=None):
    """
    get all vlan network info or specific vlan network info with Id
    :param ip:
    :param port:
    :param ServerId
    :param NicId
    :param disp:
    :param logger
    :return:
    """
    if ServerId is None or NicId is None:
        return ResInvalidCode, ResInvalidMsg + 'Serverid is required', None
    if VlanId is not None:
        Url = "/api/v1/Servers/%s/NetworkInterfaces/%s/Vlans/%s" % (ServerId, NicId, VlanId)
    else:
        Url = "/api/v1/Servers/%s/NetworkInterfaces/%s/Vlans" % (ServerId, NicId)
    Params = dict()
    Params['countPerPage'] = 100
    Conn = RestApi(ip, port, Url, params=Params, logger=logger)
    Ret, Errmsg, Data = Conn.get()
    if Ret == ResOk:
        Header, Itemheader, Data = Conn.parsing_result(Data)
        if disp is True:
            if Header.Result == ResultSuccess:
                if Itemheader is None:
                    disp_vlan_network_interfaces(Data, NicId=NicId)
                else:
                    disp_vlan_network_interfaces(Itemheader)
        return Ret, Errmsg, Data
    else:
        return Ret, Errmsg, None


@catch_exceptions()
def AddNetworkInterfaceOld(ip, port, ServerId, NicName, Description=None, logger=None):
    """
    add network interface with name
    :param ip:
    :param port:
    :param ServerId:
    :param NicName:
    :param Description:
    :param logger:
    :return:
    """
    # get network interface info
    netif = GetNetwork(logger=logger)
    nicinfo = netif.GetNicInfoWithNicName(NicName)
    if nicinfo is None:
        return ResNotFoundCode, ResNotFoundMsg, None
    netif = RequestNetworkInterfaceItems(NicName, Description=Description, logger=logger)
    netif.Set(nicinfo)
    body = jsonpickle.encode(netif, make_refs=False)
    Url = '/api/v1/Servers/%s/NetworkInterfaces' % ServerId
    ReturnType = NetworkInterfaceItemsModule

    Params = body
    Conn = RestApi(ip, port, Url, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.post(ItemsHeader=False, ReturnType=ReturnType)
    return Res, Errmsg, Ret

@catch_exceptions()
def AddNetworkInterface(ip, port, ServerId, NetworkInfo, logger=None):
    """
    add network interface with name
    :param ip:
    :param port:
    :param ServerId:
    :param NicName:
    :param Description:
    :param logger:
    :return:
    """
    # get network interface info
    body = jsonpickle.encode(NetworkInfo, make_refs=False)
    Url = '/api/v1/Servers/%s/NetworkInterfaces' % ServerId
    ReturnType = NetworkInterfaceItemsModule

    Params = body
    Conn = RestApi(ip, port, Url, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.post(ItemsHeader=False, ReturnType=ReturnType)
    return Res, Errmsg, Ret


@catch_exceptions()
def add_vlan_network_interface(ip, port, ServerId, NicId, Tag, IpAddress, SubnetMask, Gateway, logger=None):
    # get network interface info
    Vlaninfo = dict()

    netif = RequestVlanNetworkInterfaceInfo(Tag=Tag, IpAddress=IpAddress, SubnetMask=SubnetMask, Gateway=Gateway)
    body = jsonpickle.encode(netif, make_refs=False)
    Url = '/api/v1/Servers/%s/NetworkInterfaces/%s/Vlans' % (ServerId, NicId)
    Params = body
    Conn = RestApi(ip, port, Url, params=Params, logger=logger)
    Ret, Errmsg, Data = Conn.post()
    if Ret == ResOk:
        Header, Itemheader, Data = Conn.parsing_result(Data)
        return Ret, Errmsg, Header
    else:
        return Ret, Errmsg, None



@catch_exceptions()
def UpdateNetworkInterfaceInfo(Ip, Port, InterfaceId, Name=None, Description=None, Dhcp=None, MacAddress=None,
                               IpAddress=None, SubnetMask=None, Gateway=None, Dns1=None, Dns2=None, BandWidth=None,
                               IsManagement=None, logger=None):

    # get network interface info
    if InterfaceId is None:
        return ResInvalidCode, ResInvalidMsg + 'Interface Id are required', None
    Res, Errmsg, Net, ServerId = GetNetworkInterfaceDetailWithId(Ip, Port, InterfaceId)
    if Res == ResOk:
        if Name is not None:
            Net.Name = Name
        if Description is not None:
            Net.Description = Description
        if Dhcp is not None:
            Net.Dhcp = Dhcp
        body = jsonpickle.encode(Net, make_refs=False)
        Url = '/api/v1/Servers/%s/NetworkInterfaces/%s' % (ServerId, InterfaceId)
        Params = body
        Conn = RestApi(Ip, Port, Url, params=Params, logger=logger)
        Ret, Errmsg, Data = Conn.put(ItemsHeader=False, ReturnType=ResPonseHeader)
        return Ret, Errmsg, Data
    else:
        return Res, Errmsg, None


@catch_exceptions()
def update_vlan_network_interface_info(ip, port, ServerId, NicId, VlanId, Tag, IpAddress, SubnetMask, Gateway, logger=None):
    # get network interface info
    vlan = RequestVlanNetworkInterfaceInfo(Tag=Tag, IpAddress=IpAddress, SubnetMask=SubnetMask, Gateway=Gateway)
    body = jsonpickle.encode(vlan, make_refs=False)
    Url = '/api/v1/Servers/%s/NetworkInterfaces/%s/Vlans/%s' % (ServerId, NicId, VlanId)
    Params = body
    Conn = RestApi(ip, port, Url, params=Params, logger=logger)
    Ret, Errmsg, Data = Conn.put()
    if Ret == ResOk:
        Header, Itemheader, Data = Conn.parsing_result(Data)
        return Ret, Errmsg, Header
    else:
        return Ret, Errmsg, None


@catch_exceptions()
def UpdateNetworkInterfaceLinkState(ip, port, ServerId, NicId, State, logger=None):
    """
    update network interface status
    :param ip:
    :param port:
    :param ServerId:
    :param NicId:
    :param State: Up/Down
    :param logger:
    :return:
    """
    # get network interface info
    if ServerId is None or NicId is None:
        print(ServerId, NicId)
        return ResInvalidCode, ResInvalidMsg + 'Server id and Nic Id are required', None
    netif = NetworkInterfaceLinkStateItems(NicId, ServerId, State)
    body = jsonpickle.encode(netif, make_refs=False)
    Url = '/api/v1/Servers/NetworkInterfaces/LinkStatus'
    Params = body
    Conn = RestApi(ip, port, Url, params=Params, logger=logger)
    Ret, Errmsg, Data = Conn.put()
    if Ret == ResOk:
        Header, Itemheader, Data = Conn.parsing_result(Data)
        return Ret, Errmsg, Header
    else:
        return Ret, Errmsg, None

@catch_exceptions()
def DeleteNetworkInterface(Ip, Port, InterfaceId, logger=None):
    # get network interface info
    if not InterfaceId:
        return ResInvalidCode, ResInvalidMsg + 'Network Interface Id is required', None
    Res, Errmsg, Net, ServerId = GetNetworkInterfaceDetailWithId(Ip, Port, InterfaceId)
    if Res == ResOk:
        Url = '/api/v1/Servers/%s/NetworkInterfaces/%s' % (ServerId, InterfaceId)
        Conn = RestApi(Ip, Port, Url, logger=logger)
        Res, Errmsg, Ret = Conn.delete(ItemsHeader=False, ReturnType=ResponseHeaderModule)
        return Res, Errmsg, Ret


@catch_exceptions()
def delete_vlan_network_interface(ip, port, ServerId, NicId, VlanId, logger=None):
    # get network interface info
    if not (ServerId and NicId and VlanId):
        return ResInvalidCode, ResInvalidMsg + 'Server id and Nic id and VlanId is required', None
    Url = '/api/v1/Servers/%s/NetworkInterfaces/%s/Vlans/%s' % (ServerId, NicId, VlanId)
    Conn = RestApi(ip, port, Url, logger=logger)
    Ret, Errmsg, Data = Conn.delete()
    if Ret == ResOk:
        Header, Itemheader, Data = Conn.parsing_result(Data)
        return Ret, Errmsg, Header
    else:
        return Ret, Errmsg, None

@catch_exceptions()
def check_exist_network_interface(ip, port, ServerId, NicName, NicId=None, logger=None):
    """

    :param ip:
    :param port:
    :param ServerId:
    :param NicName:
    :param logger:
    :return:tuple(error code, error msg, Success to get result:True/False, fail to get result: None)
    """
    # get network interface info
    if NicName is None or ServerId is None:
        return ResInvalidCode, ResInvalidMsg + 'Serverid and Nic name are required', None
    netif = RequestNetworkInterfaceCheck(NicName, logger=logger)
    body = jsonpickle.encode(netif, make_refs=False)
    if NicId is not None:
        Url = '/api/v1/Servers/%s/NetworkInterfaces/Exist/%s' % (ServerId, NicId)
    else:
        Url = '/api/v1/Servers/%s/NetworkInterfaces/Exist' % ServerId

    Params = body
    Conn = RestApi(ip, port, Url, params=Params, logger=logger)
    Ret, Errmsg, Data = Conn.post()
    if Ret == ResOk:
        Header, Itemheader, Data = Conn.parsing_result(Data)
        return Ret, Errmsg, Header.Data
    else:
        return Ret, Errmsg, None


@catch_exceptions()
def check_exist_vlan_network_interface(ip, port, ServerId, NicId, Tag, VlanId=None, logger=None):
    """

    :param ip:
    :param port:
    :param ServerId:
    :param NicName:
    :param logger:
    :return:tuple(error code, error msg, Success to get result:True/False, fail to get result: None)
    """
    # get network interface info
    if NicId is None or ServerId is None:
        return ResInvalidCode, ResInvalidMsg + 'ServerId and NicId are required', None
    netif = RequestVlanNetworkInterfaceCheck(Tag, logger=logger)
    body = jsonpickle.encode(netif, make_refs=False)
    if VlanId is not None:
        Url = '/api/v1/Servers/%s/NetworkInterfaces/%s/Vlans/Exist/Vlans/Exist/%s' % (ServerId, NicId, VlanId)
    else:
        Url = '/api/v1/Servers/%s/NetworkInterfaces/%s/Vlans/Exist' % (ServerId, NicId)

    Params = body
    Conn = RestApi(ip, port, Url, params=Params, logger=logger)
    Ret, Errmsg, Data = Conn.post()
    if Ret == ResOk:
        Header, Itemheader, Data = Conn.parsing_result(Data)
        return Ret, Errmsg, Header.Data
    else:
        return Ret, Errmsg, None
class MyOptionParser(OptionParser):
    def print_help(self):
        Usage = """
        Usage: network.py {add|delete|update|show|status|exists_check} [option]
                init                                                                          : init management server ip and port
                add       -n <Interface Name>  -S <Server id>                                       : register nic to the management network interface pool
                addvlan   -V <Vlan Id>  -i <Ipaddress> -m <Subnetmaksk> -g <gateway>          : register vlan network to the vlan network interface pool
                remove    -S <server id> -I <Interface or Vlan Id>                                     : unregister local server from management servers pool
                remove_vlan   -I <server id> -N <nic id> -V <Vlan Id>                     : unregister vlan network from vlan network pool
                update    -n <nicname> -N <nic id> -I <server id>  : update specific nic info 
                update_status   -N <nic id> -I <server id>  : update specific network interface status 
                list      -I <server id>                     : show the registered network interface info with server id
                check     -I <server id> -n <nic name> -N <nic id> : check if a regisetered network interface exists with nic name. exclude with nic id
                check_vlan     -I <server id> -n <nic name> -N <nic id> -V <Vlan Id>: check if a regisetered vlan network interface exists with nic name. exclude with nic id
        [options]
                -a                                           : show all server info, default is true
                -V <Vlan id>                               : show specific vlan info with vlan id    
                -h, --help                                   : show this help message and exit
        print(Usage)

"""
