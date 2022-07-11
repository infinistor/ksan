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

import os, sys
import psutil
sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
import jsonpickle
from common.httpapi import *
from common.httpapi import RestApi, disp_serverinfo, get_res
from common.define import *
from common.init import get_input
from optparse import OptionParser

"""
############## Servers ###############
"""

class GetServerUsage(object):
    def __init__(self, ServerId):
        self.ServerId = ServerId
        self.LoadAverage1M = None
        self.LoadAverage5M = None
        self.LoadAverage15M = None
        self.MemoryTotal = 0
        self.MemoryUsed = 0

    def Get(self):
        load = psutil.getloadavg()
        self.LoadAverage1M = load[0]
        self.LoadAverage5M = load[1]
        self.LoadAverage15M = load[2]
        Mem = psutil.virtual_memory()
        self.MemoryTotal = Mem.total
        self.MemoryUsed = Mem.used


@catch_exceptions()
def GetServerInfo(Ip, Port, ApiKey, ServerId=None, Name=None, logger=None):
    """
    get server info all or specific server info with Id
    :param ip:
    :param port:
    :param disp:
    :return: if Id is None, ServerItemsDetail object is returned. otherwise ServerItems list returned
    """

    if ServerId is not None:
        TargetServer = ServerId
    elif Name is not None:
        TargetServer = Name
    else:
        TargetServer = None

    ItemsHeader = True
    if TargetServer is not None:
        Url = "/api/v1/Servers/%s" % TargetServer
        ItemsHeader = False
        ReturnType = ServerItemsDetailModule
    else:
        Url = "/api/v1/Servers"
        ReturnType = ServerItemsModule
    Params = dict()
    Params['countPerPage'] = 100
    Conn = RestApi(Ip, Port, Url, authkey=ApiKey, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.get(ItemsHeader=ItemsHeader, ReturnType=ReturnType)
    if Res == ResOk:
        AllServerInfo = list()
        if Ret.Result == ResultSuccess:
            if TargetServer is not None:
                return Res, Errmsg, Ret, Ret.Data
            else:
                return Res, Errmsg, Ret, Ret.Data.Items
        else:
            return Res, Errmsg, Ret, AllServerInfo
    else:
        return Res, Errmsg, None, None


def GetAllServerDetailInfo(Ip, Port, Apikey, logger=None):
    Res, Errmsg, Ret, Servers = GetServerInfo(Ip, Port, Apikey, logger=logger)
    if Res == ResOk:
        AllServerDetailInfo = list()
        if Ret.Result == ResultSuccess:
            for Svr in Servers:
                Res, Errmsg, Ret, Detail = GetServerInfo(Ip, Port, Apikey, ServerId=Svr.Id, logger=logger)
                if Res == ResOk:
                    AllServerDetailInfo.append(Detail)
                else:
                    print(Errmsg)
            return Res, Errmsg, Ret, AllServerDetailInfo
        else:
            print('fail to get Server List', Ret.Message)
            return Res, Errmsg, Ret, None
    else:
        print('fail to get Server List', Errmsg)
        return Res, Errmsg, Ret, None


@catch_exceptions()
def ShowServerInfo(Data, Id=None, Detail=False):
        if Detail:
            ServerTitleLine = '%s' % ('=' * 148)
            ServerDataLine = '%s' % ('-' * 148)
            title ="|%s|%s|%s|%s|%s|%s|%s|" % ('Name'.center(20), 'IpAddress'.center(15), 'Status'.center(10), 'LoadAvg 1M 5M 15M'.center(15), 'MemTotal'.center(20), 'MemUsed'.center(20), 'Id'.center(38))
        else:
            ServerTitleLine = '%s' % ('=' * 88)
            ServerDataLine = '%s' % ('-' * 88)
            title ="|%s|%s|%s|%s|" % ('Name'.center(20), 'IpAddress'.center(15), 'Status'.center(10), 'Id'.center(38))
        print(ServerTitleLine)
        print(title)
        print(ServerTitleLine)
        if Id is None:
            for svr in Data:
                if len(svr.NetworkInterfaces) > 0:
                    ManagementIp = svr.NetworkInterfaces[0].IpAddress
                else:
                    ManagementIp = '-'
                if Detail:
                    _svr ="|%s|%s|%s|%s|%s|%s|%s|%s|%s|" % \
                          (svr.Name.center(20), ManagementIp.center(15),
                           svr.State.center(10), str(svr.LoadAverage1M).center(5), str(svr.LoadAverage5M).center(5) ,
                           str(svr.LoadAverage15M).center(5), str(int(svr.MemoryTotal)).center(20),
                           str(int(svr.MemoryUsed)).center(20),  svr.Id.center(38))
                else:
                    _svr = "|%s|%s|%s|%s|" % \
                           (svr.Name.center(20), ManagementIp.center(15), svr.State.center(10), svr.Id.center(38))

                print(_svr)
                print(ServerDataLine)

        else:
            svr = Data[0]
            if Detail:
                _svr = "|%s|%s|%s|%s|%s|%s|" % \
                       (svr.Name.center(20), '{:30.30}'.format(svr.Description.center(30)),
                        '{:30.30}'.format(svr.CpuModel.center(30)), str(svr.Clock).center(20),
                        svr.State.center(20), svr.Id.center(30))
            else:
                _svr = "|%s|%s|%s|" % (svr.Name.center(20), svr.State.center(20), svr.Id.center(30))
            print(_svr)


@catch_exceptions()
def AddServer(ip, port, ApiKey,  Description, logger=None):
    """
    register server info
    :param ip: string
    :param port: integer
    :param Description:
    :param logger:
    :return:tuple(error code, error msg, Success to get result:header, fail to get result: None)
    """
    server = RequestServerInfo(Description)
    body = jsonpickle.encode(server, make_refs=False)
    Url = '/api/v1/Servers'
    ReturnType = ResponseHeaderModule
    Params = body
    Conn = RestApi(ip, port, Url, authkey=ApiKey, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.post(ReturnType=ReturnType)
    return Res, Errmsg, Ret



@catch_exceptions()
def ServerInit(ip, port, TargetServerIp, MqPort, ApiKey, logger=None):
    """
    register server info
    :param ip: string
    :param port: integer
    :param Description:
    :param logger:
    :return:tuple(error code, error msg, Success to get result:header, fail to get result: None)
    """
    SshUser = get_input('Insert Ssh User', str, default='root')
    SshPassword = get_input('Insert Ssh Password', 'pwd', default='')
    server = RequestServerInitInfo()
    server.Set(TargetServerIp, ip, MqPort, port, SshUser, SshPassword)
    body = jsonpickle.encode(server, make_refs=False)
    Url = '/api/v1/Servers/Initialize'
    ReturnType = ResponseHeaderModule
    Params = body
    Conn = RestApi(ip, port, Url, authkey=ApiKey, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.post(ReturnType=ReturnType)
    return Res, Errmsg, Ret


@catch_exceptions()
def RegisterServer(ip, port, ApiKey, Description, Name=None, logger=None):
    """
    register server info to server pool
    :param ip: string
    :param port: integer
    :param Description:
    :param Name : In case of already exists, Search the server info with Name(hostname)
    :param logger:
    :return:if success or alredy registered, return tuple(error code, error msg, Header, ServerItems object), otherwise tuple(Res, Errmsg, None, None)
    """
    server = RequestServerInfo(Description)
    body = jsonpickle.encode(server, make_refs=False)
    Url = '/api/v1/Servers'
    ItemsHeader = False
    ReturnType = ServerItemsDetailModule
    Params = body
    Conn = RestApi(ip, port, Url, authkey=ApiKey, params=Params, logger=logger)

    Res, Errmsg, Ret = Conn.post(ItemsHeader=ItemsHeader, ReturnType=ReturnType)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            return Res, Errmsg, Ret, Ret.Data
        elif Ret.Code == CodeDuplicated:
            res, errmsg, ret, AllServerInfo = GetServerInfo(ip, port, ApiKey, logger=logger)
            if res == ResOk:
                for svr in ret.Data.Items:
                    if svr.Name == Name:
                        return Res, Errmsg, Ret, svr

    return Res, Errmsg, Ret, None


@catch_exceptions()
def UpdateServerInfo(Ip, Port, Authkey, ServerId=None, Name=None, Description=None, State=None, logger=None):
    """
    update server info
    :param ip:
    :param port:
    :param Description:
    :param Id:
    :param logger:
    """
    if ServerId is not None:
        TargetServer = ServerId
    elif Name is not None:
        TargetServer = Name
    else:
        return ResInvalidCode, ResInvalidMsg, None

    Res, Errmsg, Ret, Data = GetServerInfo(Ip, Port, Authkey, ServerId=TargetServer, logger=logger)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            if Name is not None:
                Data.Name = Name
            if Description is not None:
                Data.Description = Description
            if State is not None:
                Data.State = State

            Url = '/api/v1/Servers/%s' % TargetServer
            server = UpdateServerInfoItems()
            server.Set(Data.Name, Data.Description, Data.CpuModel, Data.Clock, Data.State, Data.Rack, Data.MemoryTotal)
            body = jsonpickle.encode(server, make_refs=False)
            Params = body
            Conn = RestApi(Ip, Port, Url, authkey=Authkey, params=Params, logger=logger)
            Ret, Errmsg, Data = Conn.put(ItemsHeader=False, ReturnType=ResPonseHeader)
            return Ret, Errmsg, Data
        else:
            return Res, Errmsg, Ret
    else:
        return Res, Errmsg, None


@catch_exceptions()
def UpdateServerUsage(Ip, Port, ServerId, LoadAverage1M=None, LoadAverage5M=None, LoadAverage15M=None, MemoryUsed=None, State=None, logger=None):
    """
    update server info
    :param ip:
    :param port:
    :param Description:
    :param Id:
    :param logger:
    """
    Res, Errmsg, Ret, Data = GetServerInfo(Ip, Port, ServerId=ServerId, logger=logger)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            if LoadAverage1M is not None:
                Data.LoadAverage1M = LoadAverage1M
            if LoadAverage5M is not None:
                Data.LoadAverage5M = LoadAverage5M
            if LoadAverage15M is not None:
                Data.LoadAverage15M = LoadAverage15M
            if MemoryUsed is not None:
                Data.MemoryUsed = MemoryUsed

            if State is not None:
                Data.State = State

            Url = '/api/v1/Servers/%s' % ServerId
            server = UpdateServerInfoItems()
            server.Set(Data.Name, Data.Description, Data.CpuModel, Data.Clock, Data.State, Data.Rack, Data.MemoryTotal)
            body = jsonpickle.encode(server, make_refs=False)
            Params = body
            Conn = RestApi(Ip, Port, Url, params=Params, logger=logger)
            Ret, Errmsg, Data = Conn.put(ItemsHeader=False, ReturnType=ResPonseHeader)
            return Ret, Errmsg, Data
        else:
            return Res, Errmsg, Ret
    else:
        return Res, Errmsg, None


@catch_exceptions()
def UpdateServerState(Ip, Port, ServerId, State, logger=None):
    """
    update server info
    :param ip:
    :param port:
    :param Description:
    :param Id:
    :param logger:
    """
    Url = '/api/v1/Servers/%s/State/%s' % (ServerId, State)
    body = dict()
    Params = body
    Conn = RestApi(Ip, Port, Url, params=Params, logger=logger)
    Ret, Errmsg, Data = Conn.put(ItemsHeader=False, ReturnType=ResPonseHeader)
    return Ret, Errmsg, Data


@catch_exceptions()
def RemoveServer(ip, port, ApiKey, ServerId=None, Name=None, logger=None):
    """
    delete server info from server pool
    :param ip:
    :param port:
    :param Id:
    :param logger:
    :return:tuple(error code, error msg, ResponseHeader class)
    """

    if ServerId is not None:
        TargetServer = ServerId
    elif Name is not None:
        TargetServer = Name
    else:
        return ResInvalidCode, ResInvalidMsg + ' Id and Name are all None', None

    Url = '/api/v1/Servers/%s' % TargetServer

    ReturnType = ResponseHeaderModule
    Conn = RestApi(ip, port, Url, authkey=ApiKey, logger=logger)
    Res, Errmsg, Ret = Conn.delete(ReturnType=ReturnType)
    return Res, Errmsg, Ret


@catch_exceptions()
def server_exist_check_with_servername(ip, port, Name, Authkey, ExcludeServerId=None, logger=None):
    """
    check if a specific server exists with Name.
    :param ip:
    :param port:
    :param Name:
    :param ExcludeServerId: to exclude the server whose id is ExcludeServerId
    :param logger:
    :return:tuple(error code, error msg, Success to get result:True/False, fail to get result: None)
    """
    server = RequestServerExistCheck(Name)
    body = jsonpickle.encode(server, make_refs=False)
    if ExcludeServerId is not None:
        Url = '/api/v1/Servers/Exist/%s' % ExcludeServerId
    else:
        Url = '/api/v1/Servers/Exist'
    Params = body
    Conn = RestApi(ip, port, Url, authkey=Authkey, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.post()
    return Res, Errmsg, Ret


class MyOptionParser(OptionParser):
    def print_help(self):
        Usage = """
        Usage: ifs_objstorage {init|add|delete|update|show|status} [option]
                init                                         : init management server ip and port
                add                                          : register local server to the management servers pool
                delete    -I <server id>                     : unregister local server from management servers pool
                update    -I <server id>                     : update server info 
                show      -I <server id>                     : show the registered server info
                check      -n <name>  -I <server id>         : check if the server exist. the server whose id is server id is excluded
        [options]
                -a                                           : show all server info, default is true
                -I <server id>                               : show specific server info with server id    
                -h, --help                                   : show this help message and exit
"""
        print(Usage)
