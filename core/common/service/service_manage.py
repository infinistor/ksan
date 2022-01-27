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

import pdb

import psutil
import sys, os
sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from ksan.server.server_manage import *


@catch_exceptions()
def AddService(MgsIp, Port, Name, ServiceType, ServerId, GroupId='', Description='', logger=None):
    VlanIds = GetVlanIdListFromServerDetail(MgsIp, Port, ServerId, logger=logger)
    if VlanIds is None:
        return ResEtcErrorCode, ResFailToGetVlainId, None

    Service = AddServiceInfoObject()
    Service.Set(Name, ServiceType, GroupId, VlanIds, State='Offline', Description=Description, HaAction='Initializing')
    if ServiceType is None:
        return ResInvalidCode, ResInvalidMsg + 'ServiceType is required', None

    Url = "/api/v1/Services"
    body = jsonpickle.encode(Service, make_refs=False)
    Conn = RestApi(MgsIp, Port, Url, params=body, logger=logger)
    Res, Errmsg, Data = Conn.post(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data


@catch_exceptions()
def DeleteService(ip, port, ServiceId, logger=None):
    """
    delete serviceinfo from server pool
    :param ip:
    :param port:
    :param Id:
    :param logger:
    :return:tuple(error code, error msg, ResponseHeader class)
    """
    Url = '/api/v1/Services/%s' % ServiceId
    Conn = RestApi(ip, port, Url, logger=logger)
    Res, Errmsg, Ret = Conn.delete()
    return Res, Errmsg, Ret


@catch_exceptions()
def GetServiceInfo(MgsIp, Port, ServiceId=None, logger=None):
    """
    get service info all or specific service info with Id
    :param ip:
    :param port:
    :param disp:
    :return:
    """
    if ServiceId is not None:
        Url = "/api/v1/Services/%s" % ServiceId
        ItemsHeader = False
        ReturnType = ServiceDetailModule
    else:
        Url = "/api/v1/Services/"
        ItemsHeader = True
        ReturnType = ServiceItemsDetailModule
    Params = dict()
    Params['countPerPage'] = 100
    Conn = RestApi(MgsIp, Port, Url, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.get(ItemsHeader=ItemsHeader, ReturnType=ReturnType)
    if Res == ResOk:
        AllServiceInfo = list()
        if Ret.Result == ResultSuccess:
            if ServiceId is not None:
                #GetDataFromBodyReculsive(Ret.Data, ServiceDetailModule)
                #ServiceDetailInfo = jsonpickle.decode(json.dumps(Ret.Data))
                return Res, Errmsg, Ret, Ret.Data
            else:
                return Res, Errmsg, Ret, Ret.Data.Items
        return Res, Errmsg, Ret, None
    else:
        return Res, Errmsg, None, None


def GetServiceConfig(ServiceType):
    try:
        if ServiceType == TypeS3:
            with open(SampleS3ConfFile, 'r') as f:
                Conf = f.read()
                return Conf
        elif ServiceType == TypeHaproxy:
            with open(SampleHaproxyConfFile, 'r') as f:
                Conf = f.read()
                return Conf
        else:
            return None
    except Exception as err:
        print(err)
        return None


@catch_exceptions()
def UpdateServiceInfo(MgsIp, Port, ServiceId, Name=None, GroupId=None, Description=None, ServiceType=None,
                      HaAction=None, State=None, logger=None):
    Res, Errmsg, Ret, Service = GetServiceInfo(MgsIp, Port, ServiceId=ServiceId, logger=logger)
    if Res != ResOk:
        return Res, Errmsg, None
    else:
        if Ret.Result != ResultSuccess:
            return Res, Errmsg, Ret

    VlanIds = GetVlanIdListFromVlanList(Service.Vlans)
    NewService = UpdateServiceInfoObject()
    NewService.Set(Service.Name, Service.ServiceType, Service.GroupId, VlanIds, Service.State,
                   Service.Description, Service.HaAction)

    if Name is not None:
        NewService.Name = Name
    if GroupId is not None:
        NewService.GroupId = GroupId
    if Description is not None:
        NewService.Description = Description
    if ServiceType is not None:
        NewService.ServiceType = ServiceType
    if HaAction is not None:
        NewService.HaAction = HaAction
    if VlanIds is not None:
        NewService.VlanIds = VlanIds
    if State is not None:
        NewService.State = State


    Url = "/api/v1/Services/%s" % ServiceId
    body = jsonpickle.encode(NewService, make_refs=False)
    Conn = RestApi(MgsIp, Port, Url, params=body, logger=logger)
    Res, Errmsg, Data = Conn.put(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data



@catch_exceptions()
def UpdateServiceState(MgsIp, Port, ServiceId, State, logger=None):

    Url = "/api/v1/Services/%s/State/%s" % (ServiceId, State)
    body = dict()
    Conn = RestApi(MgsIp, Port, Url, params=body, logger=logger)
    Res, Errmsg, Data = Conn.put(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data


@catch_exceptions()
def UpdateServiceUsage(MgsIp, Port, ServiceId, logger=None):
    Usage = UpdateServiceUsageObject()
    Usage.Set(ServiceId, 48, 55555, 88)
    Url = "/api/v1/Services/Usage"
    body = jsonpickle.encode(Usage, make_refs=False)
    Conn = RestApi(MgsIp, Port, Url, params=body, logger=logger)
    Res, Errmsg, Data = Conn.put(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data


@catch_exceptions()
def ControlService(MgsIp, Port, ServiceId, Control, logger=None):
    Res, Errmsg, Ret, Data = GetServiceInfo(MgsIp, Port, ServiceId=ServiceId, logger=logger)
    if Res != ResOk:
        return Res, Errmsg, None
    else:
        if Ret.Result != ResultSuccess:
            return Res, Errmsg, Ret

    ServiceType = Data.ServiceType
    Url = "/api/v1/Services/%s/%s" % (ServiceId, Control)
    body = dict()
    Conn = RestApi(MgsIp, Port, Url, params=body, logger=logger)
    Res, Errmsg, Data = Conn.post()
    return Res, Errmsg, Data


@catch_exceptions()
def UpdateServiceConf(MgsIp, Port, ServiceId, logger=None):
    Res, Errmsg, Ret, Data = GetServiceInfo(MgsIp, Port, ServiceId=ServiceId, logger=logger)
    if Res != ResOk:
        return Res, Errmsg, None
    else:
        if Ret.Result != ResultSuccess:
            return Res, Errmsg, Ret
    pdb.set_trace()
    ServiceType = Data.ServiceType
    Conf = GetServiceConfig(ServiceType)
    if Conf is None:
        return ResNotFoundCode, ResNotFoundCode, None
    if ServiceType == TypeHaproxy:
        Url = "/api/v1/Services/%s/Config/HaProxy/String" % ServiceId
    elif ServiceType == TypeS3:
        Url = "/api/v1/Services/%s/Config/GW/String" % ServiceId
    elif ServiceType == TypeOSD:
        Url = "/api/v1/Services/%s/Config/OSD/String" % ServiceId
    else:
        return ResInvalidCode, ResInvalidMsg, None
    S3ProxyConf = S3ProxyConfig()
    S3ProxyConf.Set(Conf)
    body = jsonpickle.encode(S3ProxyConf, make_refs=False)
    Conn = RestApi(MgsIp, Port, Url, params=body, logger=logger)
    Res, Errmsg, Data = Conn.post()
    return Res, Errmsg, Data


@catch_exceptions()
def ShowServiceInfo(Data, Detail=False):

        #if Detail:
        #    ServiceTitle = "|%s|%s|%s|%s|%s|%s|%s|%s|%s|" % ('Name'.center(10), 'Id'.center(38),
        #                               'GruopId'.center(38), 'State'.center(10), 'Type'.center(10),
        #                                   'CpuUsage'.center(20), 'MemoryUsed'.center(20),'ThreadCount'.center(10), 'ServerId'.center(38))
        #    ServiceTitleLine = '%s' % ('=' * 212)
        #else:
        ServiceTitle ="|%s|%s|%s|%s|%s|" % ('Name'.center(20), 'State'.center(10), 'Id'.center(38), 'Type'.center(10), 'GroupId'.center(38))
        ServiceTitleLine = '%s' % ('=' * 122)

        print(ServiceTitleLine)
        print(ServiceTitle)
        print(ServiceTitleLine)

        for idx, svc in enumerate(Data):
            #if Detail:
            #    _svc ="|%s|%s|%s|%s|%s|%s|%s|%s|%s|" % \
            #          (svc.Name.center(10),
            #           svc.Id.center(38), str(svc.GroupId).center(38),
            #           svc.State.center(10), str(svc.ServiceType).center(10), str(svc.CpuUsage).center(20) ,
            #           str(svc.MemoryUsed).center(20), str(int(svc.ThreadCount)).center(10), svc.ServerId.center(38))
            #    ServiceDataLine = '%s' % ('-' * 209)
            #else:
            _svc = "|%s|%s|%s|%s|%s|" % \
                   (svc.Name.center(20), svc.State.center(10), svc.Id.center(38), svc.ServiceType.center(10), str(svc.GroupId).center(38))
            ServiceDataLine = '%s' % ('-' * 122)
            print(_svc)
            print(ServiceDataLine)

def TmpGetServiceInfoList(Ip, Port):
    Res, Errmsg, Ret = GetServiceInfo(Ip, Port)
    if Res == ResOk:
        if Ret == ResultSuccess:
            return Res, Errmsg, Ret.Data
        else:
            return Ret.Result, Ret.Message, None
    return Res, Errmsg, None


@catch_exceptions()
def ShowServiceInfoWithServerInfo(ServerList, Detail=False, Ip=None, Port=None):
        #Ret, Errmsg , Data = TmpGetServiceInfoList(Ip, Port)
        #if Ret != ResOk:
        #    print('Fail to get Service Info %s' % Errmsg)
        #    return

        ServiceTitleLine = '%s' % ('=' * 235)
        ServiceTitle ="|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|" % ('ServerName'.center(20), 'ServiceName'.center(20), 'ServiceId'.center(38),
                                       'ServiceGruopId'.center(38), 'State'.center(10), 'Type'.center(10),
                                           'CpuUsage'.center(20), 'MemoryUsed'.center(20),'Thread Cnt'.center(10), 'ServerId'.center(38))
        print(ServiceTitleLine)
        print(ServiceTitle)
        print(ServiceTitleLine)

        ServiceDataLine = '%s' % ('-' * 235)
        for Svr in ServerList:
            for Svc in Svr.Services:

                _Svc ="|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|" % \
                      (Svr.Name.center(20), str(Svc.Name).center(20),
                       Svc.Id.center(38), str(Svc.GroupId).center(38),
                       Svc.State.center(10), str(Svc.ServiceType).center(10), str(Svc.CpuUsage).center(20) ,
                       str(Svc.MemoryUsed).center(20), str(int(Svc.ThreadCount)).center(10), Svr.Id.center(38))
                print(_Svc)
                print(ServiceDataLine)


@catch_exceptions()
def GetVlanIdListFromServerDetail(MgsIp, PortarPort, ServerId, logger):
    retry = 0
    VlanIds = list()
    while True:
        retry += 1
        Res, Errmsg, Ret, Data = GetServerInfo(MgsIp, PortarPort, ServerId=ServerId, logger=logger)
        if Res == ResOk:
            if Ret.Result == ResultSuccess:
                for net in Data.NetworkInterfaces:
                    for vlan in net.NetworkInterfaceVlans:
                        VlanIds.append(vlan.Id)
            break
        else:
            if retry > 3:
                break
            else:
                time.sleep(1)
    return VlanIds


@catch_exceptions()
def GetVlanIdListFromVlanList(VlanList):
    VlanIds = list()
    for Vlan in VlanList:
        VlanIds.append(Vlan.Id)
    return VlanIds


@catch_exceptions()
def AddServiceGroup(Ip, Port, Name, ServiceType, ServiceIpAddress=None, ServiceIds=[], Description='', logger=None):

    Group = AddServiceGroupItems()
    Group.Set(Name, ServiceType, ServiceIpAddress=ServiceIpAddress, ServiceIds=ServiceIds)

    Url = "/api/v1/ServiceGroups"
    body = jsonpickle.encode(Group, make_refs=False)
    Conn = RestApi(Ip, Port, Url, params=body, logger=logger)
    Res, Errmsg, Data = Conn.post(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data


@catch_exceptions()
def RemoveServiceGroup(ip, port, ServiceId, logger=None):
    """
    delete serviceinfo from server pool
    :param ip:
    :param port:
    :param Id:
    :param logger:
    :return:tuple(error code, error msg, ResponseHeader class)
    """
    Url = '/api/v1/ServiceGroups/%s' % ServiceId
    Conn = RestApi(ip, port, Url, logger=logger)
    Res, Errmsg, Ret = Conn.delete()
    return Res, Errmsg, Ret

@catch_exceptions()
def UpdateServiceGroupConf(MgsIp, Port, GroupId, logger=None):
    Res, Errmsg, Ret, Data = GetServiceInfo(MgsIp, Port, ServiceId=GroupId, logger=logger)
    if Res != ResOk:
        return Res, Errmsg, None
    else:
        if Ret.Result != ResultSuccess:
            return Res, Errmsg, Ret
    pdb.set_trace()
    ServiceType = Data.ServiceType
    Conf = GetServiceConfig(ServiceType)
    if Conf is None:
        return ResNotFoundCode, ResNotFoundCode, None
    Url = "/api/v1/ServiceGroups/%s/Config" % GroupId
    S3ProxyConf = S3ProxyConfig()
    S3ProxyConf.Set(Conf)

    body = jsonpickle.encode(S3ProxyConf, make_refs=False)
    Conn = RestApi(MgsIp, Port, Url, params=body, logger=logger)
    Res, Errmsg, Data = Conn.post()
    return Res, Errmsg, Data


@catch_exceptions()
def UpdateServiceGroup(Ip, Port, GroupId, Name=None, Description=None, ServiceType=None,
                       ServiceIds=None, addServiceIds=None, removeServiceIds=None, logger=None):
    Res, Errmsg, Ret,Group = GetServiceGroup(Ip, Port, GroupId=GroupId, logger=logger)
    if Res != ResOk:
        return Res, Errmsg, None
    else:
        if Ret.Result != ResultSuccess:
            return Res, Errmsg, Ret

    ExistServiceIds = GetServiceIdList(Group)
    if Name is not None:
        Group.Name = Name
    if Description is not None:
        Group.Description = Description
    if ServiceType is not None:
        Group.ServiceType = ServiceType
    if ServiceIds is not None:
        ExistServiceIds = ServiceIds
    if addServiceIds is not None:
        for new in addServiceIds:
            if new in ExistServiceIds:
                return ResDuplicateCode, ResDuplicateMsg, None

        ExistServiceIds += addServiceIds
    if removeServiceIds is not None:
        for ServiceId in removeServiceIds:
            ExistServiceIds.remove(ServiceId)

    NewGroup = AddServiceGroupItems()
    NewGroup.Set(Group.Name, Group.ServiceType, ServiceIpAddress=Group.ServiceIpAddress, ServiceIds=ExistServiceIds)

    Url = "/api/v1/ServiceGroups/%s" % GroupId
    body = jsonpickle.encode(NewGroup, make_refs=False)
    Conn = RestApi(Ip, Port, Url, params=body, logger=logger)
    Res, Errmsg, Data = Conn.put(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data


def ControlSerivceGroup(Ip, Port, GroupId, Action, logger=None):
    Url = "/api/v1/ServiceGroups/%s/%s" % (GroupId, Action)
    body = dict()
    Conn = RestApi(Ip, Port, Url, params=body, logger=logger)
    Res, Errmsg, Data = Conn.post(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data

def StopSerivceGroup(Ip, Port, GroupId, logger=None):
    Url = "/api/v1/ServiceGroups/%s/Stop" % GroupId
    body = dict()
    Conn = RestApi(Ip, Port, Url, params=body, logger=logger)
    Res, Errmsg, Data = Conn.post(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data


def GetServiceIdList(GroupDetail):
    ServiceListOfGroup = list()
    for Service in GroupDetail.Services:
        ServiceListOfGroup.append(Service.Id)
    return ServiceListOfGroup


def GetServiceGroup(Ip, Port, GroupId=None, logger=None):
    if GroupId is not None:
        Url = "/api/v1/ServiceGroups/%s" % GroupId
        ItemsHeader = False
        ReturnType = ServiceGroupItemsModule
    else:
        Url = "/api/v1/ServiceGroups"
        ItemsHeader = True
        ReturnType = ResponseItemsHeaderModule
    Params = dict()
    Params['countPerPage'] = 100
    Conn = RestApi(Ip, Port, Url, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.get(ItemsHeader=ItemsHeader, ReturnType=ReturnType)
    if Res == ResOk:
        AllServiceInfo = list()
        if Ret.Result == ResultSuccess:
            if GroupId is not None:
                #GetDataFromBodyReculsive(Ret.Data, ServiceDetailModule)
                #ServiceDetailInfo = jsonpickle.decode(json.dumps(Ret.Data))
                return Res, Errmsg, Ret, Ret.Data
            else:
                return Res, Errmsg, Ret, Ret.Data.Items
        return Res, Errmsg, Ret, None
    else:
        return Res, Errmsg, None, None


def GetAllServiceGroups(Ip, Port, logger=None):
    Res, Errmsg, Ret, Groups = GetServiceGroup(Ip, Port, logger=logger)
    if Res == ResOk:
        AllServiceGroupList = list()
        if Ret.Result == ResultSuccess:
            for Grp in Groups:
                res, errmsg, ret, GroupDetail = GetServiceGroup(Ip, Port, GroupId=Grp.Id, logger=logger)
                if res == ResOk:
                    if ret.Result == ResultSuccess:
                        AllServiceGroupList.append(GroupDetail)
                else:
                    print('fail to get Service Group Info', ret.Message)
            return Res, Errmsg, Ret, AllServiceGroupList
        else:
            print('fail to get Service Group List', Ret.Message)
            return Res, Errmsg, Ret, None
    else:
        return Res, Errmsg, None, None


@catch_exceptions()
def ShowServiceGroup(Groups, Detail=False):

        TopTitleLine = '%s' % ('=' * 175)
        GroupTitleLine = '%s' % ('-' * 175)
        print(TopTitleLine)
        GroupTitle ="|%s|%s|%s|%s|%s|%s|" % ('Name'.center(14), 'Description'.center(20), 'Id'.center(40),
                                       'ServiceType'.center(15), 'ServiceIpaddress'.center(20), ' ' * 59)
        print(GroupTitle)
        print(TopTitleLine)

        ServiceTitleLine = '%s%s' % (' ' *10, '-' * 165)
        for Grp in Groups:
            _grp = "|%s|%s|%s|%s|%s|%s|" % ( Grp.Name.center(14), str(Grp.Description).center(20), Grp.Id.center(40),
                               Grp.ServiceType.center(15), str(Grp.ServiceIpAddress).center(20), ' ' * 59)

            print(_grp)
            print(GroupTitleLine)
            if Detail:
                ServiceTitle = "%s|%s|%s|%s|%s|%s|%s|%s|%s|" % (' ' * 10, 'Service Name'.center(16), 'Description'.center(20),
                                    'Service Id'.center(40), 'State'.center(15), 'Service Type'.center(15),
                                                'Cpu Usage'.center(20), 'Memoy Used'.center(20), 'Thread Cnt'.center(10))
                print(ServiceTitle)
                if len(Grp.Services) >= 0:
                    print(ServiceTitleLine)
                else:
                    print(GroupTitleLine)
                for idx, svc in enumerate(Grp.Services):
                    _svc ="%s|%s|%s|%s|%s|%s|%s|%s|%s|" % \
                          (' ' * 10, svc.Name.center(16), '{:20.20}'.format(str(svc.Description).center(20)),
                           svc.Id.center(40), svc.State.center(15), str(svc.ServiceType).center(15),
                           str(svc.CpuUsage).center(20), str(svc.MemoryUsed).center(20), str(svc.ThreadCount).center(10))

                    print(_svc)
                    if len(Grp.Services) -1 == idx:
                        print(GroupTitleLine)
                    else:
                        print(ServiceTitleLine)







class MyOptionParser(OptionParser):
    def print_help(self):
        Usage = """
        Usage: service_manage.py {add|delete|start|stop|set|list} [option]
                add       -S <server id>  -N <name> -G <group id> -T <service type[Haproxy|S3|Osd|Objmanager]>        : Add disk
                delete     -I <service id>                                             : Delete Service
                set       -I <disk id> [Rw|Ro]                                         : Set disk mode. Ro:ReadOnly, Rw: ReadWrite
                start     -I <disk id>                                                 : Start disk 
                stop      -I <disk id>                                                 : Stop disk 
                list                                                                   : Display disk info
        [options]
                -a                                                                     : show all server info
                -T                                                                     : show information in detail
                -h, --help                                                             : show this help message and exit
"""
        print(Usage)

"""
if __name__ == '__main__':
    usage = "Usage: %prog {init|add|delete|update|list|status} [option]"
    parser = MyOptionParser(usage=usage)
    parser.add_option('-I', "--Id", dest="ServerId", help='server id')
    parser.add_option('-N', "--Name", dest="Name", default='', help='server description')
    parser.add_option('-d', "--Debug", dest="debug", action='store_true', default=False, help='debug mode')
    parser.add_option('-G', "--GroupId", dest="GroupId", help='Group Id')
    parser.add_option('-T', "--ServiceType", dest="ServiceType", help='Service Type')
    parser.add_option('-V', "--VlanId", dest="VlanId", help='Vlan Id')
    parser.add_option('-l', "--Detail", dest="Detail", action='store_true', help='server info in detail')

    options, args = parser.parse_args()
    if len(args) != 1:
        parser.print_help()
        sys.exit(-1)

    IfsPortalIp = '127.0.0.1'
    IfsPortalPort = 5443
    IfsMqPort = 5672
    ret, conf = read_conf(MonServicedConfPath)
    if ret is True:
        IfsPortalIp = conf['mgs']['MgsIp']
        IfsPortalPort = int(conf['mgs']['IfsPortalPort'])
        IfsMqPort = int(conf['mgs']['MqPort'])

    logger = None
    if options.debug is True:
        logger = Logging(loglevel='debug')
        logger = logger.create()

    if args[0] == 'add':
        Res, Errmsg, Ret = AddService(IfsPortalIp, IfsPortalPort, options.Name, options.ServiceType, options.ServerId,
                                      options.GroupId, logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)
    elif args[0] == 'delete':
        Res, Errmsg, Ret = DeleteService(IfsPortalIp, IfsPortalPort, options.ServerId, logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)
    elif args[0] == 'list':
        Res, Errmsg, Ret, Data = GetServiceInfo(IfsPortalIp, IfsPortalPort,logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
            ShowServiceInfo(Data, Detail=options.Detail)
        else:
            print(Errmsg)
"""
