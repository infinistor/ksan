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
from server.server_manage import *
from common.init import get_input
from common.shcommand import shcall
import logging

ConfigMaxWidthLen = 100
ConfigMaxValueLen = 100 - 13
ConfigCompartLine = '%s' % ('=' * ConfigMaxWidthLen)
ConfigTitleLine = '%s' % ('-' * ConfigMaxWidthLen)


OsdDefaultPoolsize = 650
OsdDefaultPort = 8000
OsdDefaultEcScheduleMinute = 30000
OsdDefaultEcApplyMinute = 3000000
OsdDefaultEcFileSize = 100000
OsdDefaultCacheDisk = ''
OsdDefaultCacheScheduleMinute = 2
OsdDefaultCacheFileSize = 1024
OsdDefaultCacheLimitMinute = 5
OsdDefaultTrashScheduleMinute = 5
OsdDefaultPerformanceMode = 'NO_OPTION'

OsdConfigInfo = [{'key': 'osd.pool_size', 'value': OsdDefaultPoolsize, 'type': int, 'question': 'Insert Connection Pool size'},
                  {'key': 'osd.port', 'value': OsdDefaultPort, 'type': int, 'question': 'Insert Osd Port'},
                  {'key': 'osd.ec_schedule_minutes', 'value': OsdDefaultEcScheduleMinute, 'type': int, 'question': 'Insert EC Schedule Minutes'},
                  {'key': 'osd.ec_apply_minutes', 'value': OsdDefaultEcApplyMinute, 'type': int, 'question': 'Insert EC Apply Minutes'},
                  {'key': 'osd.ec_file_size', 'value': OsdDefaultEcFileSize, 'type': int, 'question': 'Insert EC File Size'},
                  {'key': 'osd.cache_disk', 'value': OsdDefaultCacheDisk, 'type': int, 'question': 'Insert Cache Disk'},
                  {'key': 'osd.cache_schedule_minutes', 'value': OsdDefaultCacheScheduleMinute, 'type': int, 'question': 'Insert Cache Schedule Minutes'},
                  {'key': 'osd.cache_file_size', 'value': OsdDefaultCacheFileSize, 'type': int, 'question': 'Insert Cache File Size'},
                  {'key': 'osd.cache_limit_minutes', 'value': OsdDefaultCacheLimitMinute, 'type': int, 'question': 'Insert Cache Limit Minutes'},
                  {'key': 'osd.trash_schedule_minutes', 'value': OsdDefaultTrashScheduleMinute, 'type': int, 'question': 'Insert Trash Schedule Minute'}]


S3DefaultDbRepository = TypeServiceMARIADB
S3DefaultDbHost = '127.0.0.1'
S3DefaultDatabase = 'ksan'
S3DefaultDbPort = 3306
S3DefaultDbUser = 'root'
S3DefaultDbPassword = 'YOUR_DB_PASSWORD'
S3DefaultDbPoolSize = 20
S3DefaultGwAuthrization = 'AWS_V2_OR_V4'
S3DefaultGwEndpoint = 'http://0.0.0.0:8080'
S3DefaultGwSecureEndpoint = 'https://0.0.0.0:8443'
S3DefaultGwKeyStorePath = '/usr/local/ksan/ssl/pspace.jks'
S3DefaultGwtKeyStorePassword = 'YOUR_JKS_PASSWORD'
S3DefaultGwMaxFileSize = 3221225472
S3DefaultGwMaxListSize = 200000
S3DefaultGwMaxTimeSkew = 9000
S3DefaultGwReplication = 2
S3DefaultGwOsdPort = 8000
S3DefaultGwJettyMaxThreads = 1000
S3DefaultGwOsdClientCount = 100
S3DefaultGwObjmanagerCount = 100
S3DefaultGwPerformanceMode = 'NO_OPTION'
S3DefaultCacheDisk = ''
S3DefaultCacheFileSize = 1024

ObjManagerDefaultDbRepository = TypeServiceMARIADB
ObjManagerDefaultDbHost = '127.0.0.1'
ObjManagerDefaultDatabase = 'ksan'
ObjManagerDefaultDbPort = 3306
ObjManagerDefaultDbUser = 'root'
ObjManagerDefaultDbPassword = 'YOUR_DB_PASSWORD'
ObjManagerDefaultMqHost = '127.0.0.1'
ObjManagerDefaultMqName = 'disk'
ObjManagerDefaultMqDiskpoolQueueName = 'disk'
ObjManagerDefaultMqDiskpoolExchangeName = 'disk'
ObjManagerDefaultMqExchangeName = 'disk'
ObjManagerDefaultMqOsdExchangeName = 'OSDExchange'

S3ConfigInfo = [
    {'key': 'gw.db_repository', 'value': S3DefaultDbRepository, 'type': str, 'question': 'Insert Gw DB Repository', 'valid_answer_list': [TypeServiceMARIADB, TypeServiceMONGODB]},
    {'key': 'gw.db_host', 'value': S3DefaultDbHost, 'type': 'ip', 'question': 'Insert Gw DB Host'},
    {'key': 'gw.db_name', 'value': S3DefaultDatabase, 'type': str, 'question': 'Insert Gw DB Name'},
    {'key': 'gw.db_port', 'value': S3DefaultDbPort, 'type': int, 'question': 'Insert Gw DB Port'},
    {'key': 'gw.db_user', 'value': S3DefaultDbUser, 'type': str, 'question': 'Insert Gw DB User'},
    {'key': 'gw.db_password', 'value': S3DefaultDbPassword, 'type': str, 'question': 'Insert Gw DB Password'},
    {'key': 'gw.db_pool_size', 'value': S3DefaultDbPoolSize, 'type': int, 'question': 'Insert Gw DB Pool Size'},
    {'key': 'gw.authorization', 'value': S3DefaultGwAuthrization, 'type': str, 'question': 'Insert Authorization'},
    {'key': 'gw.endpoint', 'value': S3DefaultGwEndpoint, 'type': 'url', 'question': 'Insert Endpoint Url'},
    {'key': 'gw.secure_endpoint', 'value': S3DefaultGwSecureEndpoint, 'type': 'url',
     'question': 'Insert Secure Endpoint Url'},
    {'key': 'gw.keystore_path', 'value': S3DefaultGwKeyStorePath, 'type': str, 'question': 'Insert KeyStore Path'},
    {'key': 'gw.keystore_password', 'value': S3DefaultGwtKeyStorePassword, 'type': str,
     'question': 'Insert KeyStore Password'},
    {'key': 'gw.max_file_size', 'value': S3DefaultGwMaxFileSize, 'type': int, 'question': 'Insert Max File Size'},
    {'key': 'gw.max_list_size', 'value': S3DefaultGwMaxListSize, 'type': int, 'question': 'Insert Max List Size'},
    {'key': 'gw.max_timeskew', 'value': S3DefaultGwMaxTimeSkew, 'type': int, 'question': 'Insert Max Time Skew'},
    {'key': 'gw.osd_port', 'value': S3DefaultGwOsdPort, 'type': int, 'question': 'Insert Osd Port'},
    {'key': 'gw.jetty_max_threads', 'value': S3DefaultGwJettyMaxThreads, 'type': int,
     'question': 'Insert Jetty Max Threads'},
    {'key': 'gw.osd_client_count', 'value': S3DefaultGwOsdClientCount, 'type': int,
     'question': 'Insert Osd Client Count'},
    {'key': 'gw.objmanager_count', 'value': S3DefaultGwObjmanagerCount, 'type': int,
     'question': 'Insert Objmanager Count'},
    {'key': 'gw.performance_mode', 'value': S3DefaultGwPerformanceMode, 'type': str,
     'question': 'Insert Performance Mode'},
    {'key': 'gw.cache_disk', 'value': S3DefaultCacheDisk, 'type': int, 'question': 'Insert Cache Disk'},
    {'key': 'gw.cache_file_size', 'value': S3DefaultCacheFileSize, 'type': int, 'question': 'Insert Cache File Size'},
    {'key': 'objM.db_repository', 'value': ObjManagerDefaultDbRepository, 'type': str,
     'question': 'Insert Object DB Repository'},
    {'key': 'objM.db_host', 'value': ObjManagerDefaultDbHost, 'type': 'ip', 'question': 'Insert Object DB Host', 'valid_answer_list': [TypeServiceMARIADB, TypeServiceMONGODB]},
    {'key': 'objM.db_name', 'value': ObjManagerDefaultDatabase, 'type': str, 'question': 'Insert Object DB Name'},
    {'key': 'objM.db_port', 'value': ObjManagerDefaultDbPort, 'type': int, 'question': 'Insert Object DB Port'},
    {'key': 'objM.db_user', 'value': ObjManagerDefaultDbUser, 'type': str, 'question': 'Insert Object DB User'},
    {'key': 'objM.db_password', 'value': ObjManagerDefaultDbPassword, 'type': str, 'question': 'Insert Object DB Password'},
    {'key': 'objM.mq_host', 'value': ObjManagerDefaultMqHost, 'type': str, 'question': 'Insert MQ Host'},
    {'key': 'objM.mq_queue_name', 'value': ObjManagerDefaultMqName, 'type': str, 'question': 'Insert MQ Name'},
    {'key': 'objM.mq_exchange_name', 'value': ObjManagerDefaultMqExchangeName, 'type': str, 'question': 'Insert MQ Exchange Name'},
    {'key': 'objM.mq_osd_exchange_name', 'value': ObjManagerDefaultMqOsdExchangeName, 'type': str, 'question': 'Insert MQ Osd Exchange Name'}
    #{'key': 'objM.mq.host', 'value': ObjManagerDefaultMqHost, 'type': 'ip', 'question': 'Insert MQ Host'},
    #{'key': 'objM.mq.diskpool.queuename', 'value': ObjManagerDefaultMqDiskpoolQueueName, 'type': str,
    # 'question': 'Insert MQ Disk pool Queue Name'},
    #{'key': 'objM.mq.diskpool.exchangename', 'value': ObjManagerDefaultMqDiskpoolExchangeName, 'type': str,
    # 'question': 'Insert MQ Disk pool Exchange Name'},
    #{'key': 'objM.mq.osd.exchangename=OSDExchange', 'value': ObjManagerDefaultMqOsdExchangeName, 'type': str,
    # 'question': 'Insert MQ Osd Exchange Name'}
]

MongoDbDefaultShard1Port = 20001
MongoDbDefaultShard2Port = 20002
MongoDbDefaultConfigServerPort = 50000
MongoDbDefaultDbPort = 27017
MongoDbDefaultHomeDir = '/var/lib/mongo'
MongoDbDefaultPrimaryHost = socket.gethostname()

MongoDbConfigInfo = [
    {'key': 'Shard1Port', 'value': MongoDbDefaultShard1Port, 'type': int, 'question': 'Insert MongoDB Shard1 Port'},
    {'key': 'Shard2Port', 'value': MongoDbDefaultShard2Port, 'type': int, 'question': 'Insert MongoDB Shard2 Port'},
    {'key': 'ConfigServerPort', 'value': MongoDbDefaultConfigServerPort, 'type': int, 'question': 'Insert MongoDB Config Server Port'},
    {'key': 'MongoDbPort', 'value': MongoDbDefaultDbPort, 'type': int,
     'question': 'Insert MongoDB DB Port'},
    {'key': 'HomeDir', 'value': MongoDbDefaultHomeDir, 'type': str,
     'question': 'Insert MongoDB Home Directory'},
    {'key': 'PrimaryHostName', 'value': MongoDbDefaultPrimaryHost, 'type': str,
     'question': 'Insert MongoDB Primary Cluster Node Hostname'}
]


@catch_exceptions()
def AddService(MgsIp, Port, ApiKey, ServiceName, ServiceType, ServerId=None, ServerName=None, GroupId='', Description='', logger=None):
    VlanIds = GetVlanIdListFromServerDetail(MgsIp, Port, ApiKey, ServerId=ServerId, ServerName=ServerName, logger=logger)
    if VlanIds is None:
        return ResEtcErrorCode, ResFailToGetVlainId, None

    Service = AddServiceInfoObject()
    Service.Set(ServiceName, ServiceType, GroupId, VlanIds, State='Offline', Description=Description, HaAction='Initializing')
    if ServiceType is None:
        return ResInvalidCode, ResInvalidMsg + 'ServiceType is required', None

    Url = "/api/v1/Services"
    body = jsonpickle.encode(Service, make_refs=False)
    Conn = RestApi(MgsIp, Port, Url, authkey=ApiKey, params=body, logger=logger)
    Res, Errmsg, Data = Conn.post(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data

def RegisterService(Conf, ServiceType, logger):
    out, err = shcall('hostname')
    ServiceName = out[:-1] + '_%s' % ServiceType
    Res, Errmsg, Ret = AddService(Conf.mgs.MgsIp, Conf.mgs.IfsPortalPort, Conf.mgs.IfsPortalKey,  ServiceName, ServiceType,
                                  Conf.mgs.ServerId, logger=logger)
    if Res == ResOk:
        if Ret.Result != ResultSuccess:
            logger.error("%s %s" % (str(Ret.Result), str(Ret.Message)))
        else:
            logging.log(logging.INFO, "%s is registered. %s %s" % (ServiceType, str(Ret.Result), str(Ret.Message)))
    else:
        logger.error("%s %s" % (str(Ret.Result), str(Ret.Message)))

def KsanServiceRegister(Conf, ServiceType, logger):
    Retry = 0
    while True:
        Retry += 1
        Res, Errmsg, Ret, Data = GetServerInfo(Conf.mgs.MgsIp, Conf.mgs.IfsPortalPort,Conf.mgs.IfsPortalKey , ServerId=Conf.mgs.ServerId, logger=logger)
        if Res == ResOk:
            if Ret.Result == ResultSuccess:
                NetworkInterfaces = Data.NetworkInterfaces
                if len(NetworkInterfaces) > 0:
                    isNetworkAdded = True
                    break
            else:
                logger.error('fail to get network interface %s' % Ret.Message)
        else:
            logger.error('fail to get network interface %s' % Errmsg)

        time.sleep(10)

    if isNetworkAdded is True:
        RegisterService(Conf, ServiceType, logger)

@catch_exceptions()
def DeleteService(ip, port, ApiKey, ServiceId=None, ServiceName=None, logger=None):
    """
    delete serviceinfo from server pool
    :param ip:
    :param port:
    :param Id:
    :param logger:
    :return:tuple(error code, error msg, ResponseHeader class)
    """
    if ServiceId is not None:
        TargetServcie = ServiceId
    elif ServiceName is not None:
        TargetServcie = ServiceName
    else:
        return ResInvalidCode, ResInvalidMsg, None

    Url = '/api/v1/Services/%s' % TargetServcie
    Conn = RestApi(ip, port, Url, authkey=ApiKey, logger=logger)
    Res, Errmsg, Ret = Conn.delete()
    return Res, Errmsg, Ret


@catch_exceptions()
def GetServiceInfo(MgsIp, Port, ApiKey, ServiceId=None, ServiceName=None, logger=None):
    """
    get service info all or specific service info with Id
    :param ip:
    :param port:
    :param disp:
    :return:
    """
    if ServiceId is not None:
        TargetService = ServiceId
    elif ServiceName is not None:
        TargetService = ServiceName
    else:
        TargetService = None

    if TargetService is not None:
        Url = "/api/v1/Services/%s" % TargetService
        ItemsHeader = False
        ReturnType = ServiceDetailModule
    else:
        Url = "/api/v1/Services/"
        ItemsHeader = True
        ReturnType = ServiceItemsDetailModule
    Params = dict()
    Params['countPerPage'] = 100
    Conn = RestApi(MgsIp, Port, Url, authkey=ApiKey, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.get(ItemsHeader=ItemsHeader, ReturnType=ReturnType)
    if Res == ResOk:
        AllServiceInfo = list()
        if Ret.Result == ResultSuccess:
            if TargetService is not None:
                return Res, Errmsg, Ret, Ret.Data
            else:
                return Res, Errmsg, Ret, Ret.Data.Items
        return Res, Errmsg, Ret, None
    else:
        return Res, Errmsg, None, None

@catch_exceptions()
def GetServiceMongoDBConfig(MgsIp, Port, ApiKey, logger=None):
    """
    get service info all or specific service info with Id
    :param ip:
    :param port:
    :param disp:
    :return:
    """
    Url = "/api/v1/Config/MongoDB"
    ItemsHeader = False
    ReturnType = ServiceConfigModule

    Params = dict()
    Params['countPerPage'] = 100
    Conn = RestApi(MgsIp, Port, Url, authkey=ApiKey, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.get(ItemsHeader=ItemsHeader, ReturnType=ReturnType)
    if Res == ResOk:
        AllServiceInfo = list()
        if Ret.Result == ResultSuccess:
            return Res, Errmsg, Ret, Ret.Data
        return Res, Errmsg, Ret, None
    else:
        return Res, Errmsg, None, None



def GetServiceConfigFromFile(ServiceType):
    try:
        if ServiceType == TypeS3:
            with open(SampleS3ConfFile, 'r') as f:
                Conf = f.read()
                return Conf
        elif ServiceType == TypeServiceHaproxy:
            with open(SampleHaproxyConfFile, 'r') as f:
                Conf = f.read()
                return Conf
        else:
            return None
    except Exception as err:
        print(err)
        return None


@catch_exceptions()
def UpdateServiceInfo(MgsIp, Port, ApiKey, ServiceId=None, ServiceName=None, GroupId=None, Description=None, ServiceType=None,
                      HaAction=None, State=None, logger=None):
    Res, Errmsg, Ret, Service = GetServiceInfo(MgsIp, Port, ApiKey, ServiceId=ServiceId, ServiceName=ServiceName, logger=logger)
    if Res != ResOk:
        return Res, Errmsg, None
    else:
        if Ret.Result != ResultSuccess:
            return Res, Errmsg, Ret

    VlanIds = GetVlanIdListFromVlanList(Service.Vlans)
    NewService = UpdateServiceInfoObject()
    NewService.Set(Service.Name, Service.ServiceType, Service.GroupId, VlanIds, Service.State,
                   Service.Description, Service.HaAction)

    if ServiceName is not None:
        NewService.Name = ServiceName
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

    if ServiceId is not None:
        TargetServcie = ServiceId
    elif ServiceName is not None:
        TargetServcie = ServiceName
    else:
        return ResInvalidCode, ResInvalidMsg, None

    Url = "/api/v1/Services/%s" % TargetServcie
    body = jsonpickle.encode(NewService, make_refs=False)
    Conn = RestApi(MgsIp, Port, Url, authkey=ApiKey, params=body, logger=logger)
    Res, Errmsg, Data = Conn.put(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data



@catch_exceptions()
def UpdateServiceState(MgsIp, Port, ApiKey, ServiceId, State, logger=None):

    Url = "/api/v1/Services/%s/State/%s" % (ServiceId, State)
    body = dict()
    Conn = RestApi(MgsIp, Port, Url, authkey=ApiKey, params=body, logger=logger)
    Res, Errmsg, Data = Conn.put(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data


@catch_exceptions()
def UpdateServiceUsage(MgsIp, Port, ApiKey, ServiceId, logger=None):
    Usage = UpdateServiceUsageObject()
    Usage.Set(ServiceId, 48, 55555, 88)
    Url = "/api/v1/Services/Usage"
    body = jsonpickle.encode(Usage, make_refs=False)
    Conn = RestApi(MgsIp, Port, Url, authkey=ApiKey, params=body, logger=logger)
    Res, Errmsg, Data = Conn.put(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data


@catch_exceptions()
def ControlService(MgsIp, Port, ApiKey, Control, ServiceId=None, ServiceName=None, logger=None):
    Res, Errmsg, Ret, Data = GetServiceInfo(MgsIp, Port, ApiKey, ServiceId=ServiceId, ServiceName=ServiceName, logger=logger)
    if Res != ResOk:
        return Res, Errmsg, None
    else:
        if Ret.Result != ResultSuccess:
            return Res, Errmsg, Ret

    if ServiceId is not None:
        TargetServcie = ServiceId
    elif ServiceName is not None:
        TargetServcie = ServiceName
    else:
        return ResInvalidCode, ResInvalidMsg, None

    Url = "/api/v1/Services/%s/%s" % (TargetServcie, Control)
    body = dict()
    Conn = RestApi(MgsIp, Port, Url, authkey=ApiKey,params=body, logger=logger)
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
    Conf = GetServiceConfigFromFile(ServiceType)
    if Conf is None:
        return ResNotFoundCode, ResNotFoundCode, None
    if ServiceType == TypeServiceHaproxy:
        Url = "/api/v1/Services/%s/Config/HaProxy/String" % ServiceId
    elif ServiceType == TypeS3:
        Url = "/api/v1/Services/%s/Config/GW/String" % ServiceId
    elif ServiceType == TypeServiceOSD:
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
        #ServiceTitle ="|%s|%s|%s|%s|%s|" % ('Name'.center(20), 'State'.center(10), 'Id'.center(38), 'Type'.center(10), 'GroupId'.center(38))
        ServiceTitle ="|%s|%s|%s|%s|" % ('Name'.center(20), 'State'.center(10), 'Id'.center(38), 'Type'.center(10))
        ServiceTitleLine = '%s' % ('=' * 83)

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
            _svc = "|%s|%s|%s|%s|" % \
                   (svc.Name.center(20), svc.State.center(10), svc.Id.center(38), svc.ServiceType.center(10))
            #_svc = "|%s|%s|%s|%s|%s|" % \
            #       (svc.Name.center(20), svc.State.center(10), svc.Id.center(38), svc.ServiceType.center(10), str(svc.GroupId).center(38))
            ServiceDataLine = '%s' % ('-' * 83)
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
def GetVlanIdListFromServerDetail(MgsIp, PortarPort, ApiKey, ServerId=None, ServerName=None, logger=None):
    retry = 0
    VlanIds = list()
    while True:
        retry += 1
        Res, Errmsg, Ret, Data = GetServerInfo(MgsIp, PortarPort, ApiKey, ServerId=ServerId, Name=ServerName, logger=logger)
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
    Conf = GetServiceConfigFromFile(ServiceType)
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


@catch_exceptions()
def GetSeviceConfStringForDisplay(Conf, Prefix):
    Conf = json.loads(Conf)
    StrConf = ''
    for key1, val1 in Conf.items():
        if isinstance(val1, dict):
            for key2, val2 in val1.items():
                ConfLineStr = "%s_%s=%s" % (key1, key2, str(val2))
                StrConf += "%s%s|\n" % (Prefix, ConfLineStr.ljust(100-13))
        else:
            ConfLineStr = "%s=%s" % (key1, str(val1))
            StrConf += '%s%s|\n' % (Prefix, ConfLineStr.ljust(100-13))
    return StrConf

@catch_exceptions()
def GetServiceConfString(Conf):
    Conf = json.loads(Conf)
    StrConf = ''
    for key1, val1 in Conf.items():
        if isinstance(val1, dict):
            for key2, val2 in val1.items():
                StrConf += "%s_%s=%s" % (key1, key2, str(val2))
        else:
            StrConf += "%s=%s" % (key1, str(val1))
    return StrConf


@catch_exceptions()
def GetDspConfString(Conf, Prefix):
    Conf = json.loads(Conf)
    StrConf = ''
    for key1, val1 in Conf.items():
        if isinstance(val1, dict):
            for key2, val2 in val1.items():
                ConfLineStr = "%s_%s=%s" % (key1, key2, str(val2))
                StrConf += "%s%s|\n" % (Prefix, ConfLineStr.ljust(100-13))
        else:
            ConfLineStr = "%s=%s" % (key1, str(val1))
            StrConf += '%s%s|\n' % (Prefix, ConfLineStr.ljust(100-13))
    return StrConf


@catch_exceptions()
def GetServiceConfigList(PortalIp, PortalPort, ApiKey, ServiceType, logger=None):
    """
    Get config list from portal
    :param PortalIp:
    :param PortalPort:
    :param ServiceType:
    :param logger:
    :return:
    """
    Url = '/api/v1/Config/List/%s' % ServiceType
    ItemsHeader = True
    ReturnType = ResponseItemsHeaderModule
    Params = dict()
    Params['countPerPage'] = 100

    Conn = RestApi(PortalIp, PortalPort, Url, authkey=ApiKey, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.get(ItemsHeader=ItemsHeader, ReturnType=ReturnType)
    if Res == ResOk:
        ConfigList = list()
        if Ret.Result == ResultSuccess:
            return Res, Errmsg, Ret, Ret.Data
        else:
            return Res, Errmsg, Ret, None
    else:
        return Res, Errmsg, None, None

@catch_exceptions()
def LoadServiceList(conf, ServiceList, LocalIpList, logger):
    Res, Errmsg, Ret, Data = GetServerInfo(conf.mgs.MgsIp, conf.mgs.IfsPortalPort, conf.mgs.ServerId, logger=logger)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            for service in Data.Services:
                ServiceList['IdList'].append(service.Id)
                ServiceList['Details'][service.Id] = service
            for Interface in Data.NetworkInterfaces:
                LocalIpList.append(Interface.IpAddress)
    print(ServiceList)
    print(LocalIpList)


@catch_exceptions()
def ShowConfigList(ConfigList, Detail=False):
    """
    Display all config info with type
    :param ConfigList:
    :param Detail:
    :return:
    """
    print(ConfigCompartLine)
    for Config in ConfigList.Items:
        DsPServiceConf(Config)

def DsPServiceConf(Config, TopTitleLine=False):
    """
    Display service config with type
    :param Config:
    :param TopTitleLine:
    :return:
    """
    if TopTitleLine is not True:
        print(ConfigCompartLine)
    Type = Config.Type
    Version = Config.Version
    Conf = Config.Config
    ConfPartPrefix = '|%s|' % (' ' * 10)
    RegiDate = Config.RegDate
    StrConf = GetSeviceConfStringForDisplay(Conf, ConfPartPrefix)
    if StrConf is None:
        print('Invalid Conf. version:%d \n%s' % (Version, str(Conf)))
    else:
        ConfigStr = """|%s|%s|
%s 
|%s|%s| 
%s
|%s|%s|         
%s
|%s|%s| 
%s%s 
""" % ('Type'.center(10), Type.center(ConfigMaxValueLen), ConfigTitleLine, 'Version'.center(10),
           str(Version).center(ConfigMaxValueLen), ConfigTitleLine, 'Date'.center(10), RegiDate.center(ConfigMaxValueLen),
           ConfigTitleLine,
           'Conf'.center(10), ' ' * (100 - 13), StrConf.center(ConfigMaxValueLen), ConfigCompartLine)
        print(ConfigStr)


@catch_exceptions()
def GetServiceConfig(PortalIp, PortalPort, ApiKey, ServiceType, Version=None, logger=None):
    Url = '/api/v1/Config/%s' % ServiceType
    if Version is not None:
        Url += '/%s' % Version

    ItemsHeader = False
    ReturnType = ServiceGroupItemsModule

    Params = dict()
    Conn = RestApi(PortalIp, PortalPort, Url, authkey=ApiKey, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.get(ItemsHeader=ItemsHeader, ReturnType=ReturnType)
    if Res == ResOk:
        AllServiceInfo = list()
        if Ret.Result == ResultSuccess:
            return Res, Errmsg, Ret, Ret.Data
        else:
            return Res, Errmsg, Ret, None
    else:
        return Res, Errmsg, None, None

@catch_exceptions()
def UpdateServiceConfigVersion(PortalIp, PortalPort, ApiKey, ServiceType, Version, logger=None):
    """
    Update config version
    :param PortalIp:
    :param PortalPort:
    :param ServiceType:
    :param ConfigFilePath:
    :param logger:
    :return:
    """

    Url = "/api/v1/Config/%s/%s" % (ServiceType, Version)
    body = json.dumps(dict())
    Conn = RestApi(PortalIp, PortalPort, Url, authkey=ApiKey, params=body, logger=logger)
    Res, Errmsg, Data = Conn.put(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data


@catch_exceptions()
def SetServiceConfig(IfsPortalIp, IfsPortalPort, ApiKey, ServiceType, ConfigFilePath=None, logger=None):
    Conf = GetConfig(IfsPortalIp, IfsPortalPort, ApiKey, ServiceType, logger, ConfigFilePath=ConfigFilePath)

    Url = "/api/v1/Config/%s" % ServiceType
    body = Conf
    Conn = RestApi(IfsPortalIp, IfsPortalPort, Url, authkey=ApiKey, params=body, logger=logger)
    Res, Errmsg, Data = Conn.post(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data


@catch_exceptions()
def UpdateServiceConfig(ServiceType, Version=None, ConfigFile=False):
    pass

@catch_exceptions()
def RemoveServiceConfig(PortalIp, PortalPort, ApiKey, ServiceType, Version, logger=None):
    """
    delete service config from config list
    :param ip:
    :param port:
    :param Id:
    :param logger:
    :return:tuple(error code, error msg, ResponseHeader class)
    """
    Url = '/api/v1/Config/%s/%s' % (ServiceType, Version)
    Conn = RestApi(PortalIp, PortalPort, Url, authkey=ApiKey, logger=logger)
    Res, Errmsg, Ret = Conn.delete()
    return Res, Errmsg, Ret


def GetConfig(IfsPortalIp, IfsPortalPort, ApiKey, ServiceType, logger, ConfigFilePath=None):
    if ConfigFilePath is None:
        Conf = GetConfigFromUser(IfsPortalIp, IfsPortalPort, ApiKey, ServiceType, logger)
        Conf = json.dumps(Conf)
        Conf = Conf.replace('"', '\\"')
        Conf = '"'+ Conf + '"'
        return Conf
    else:
        pass


def GetConfigFromUser(IfsPortalIp, IfsPortalPort, ApiKey, ServiceType, logger):

    ConfigInfo = GetServiceConfigInfo(IfsPortalIp, IfsPortalPort, ApiKey, ServiceType, logger)

    Conf = dict()
    for info in ConfigInfo:
        ConfKey = info['key']
        QuestionString = info['question']
        ValueType = info['type']
        DefaultValue = info['value']
        if 'valid_answer_list' in info:
            ValueAnswerList = info['valid_answer_list']
        else:
            ValueAnswerList = None
        Conf[ConfKey]= get_input(QuestionString, ValueType, DefaultValue, ValidAnsList=ValueAnswerList)
    return Conf


def GetServiceConfigInfo(IfsPortalIp, IfsPortalPort, ApiKey, ServiceType, logger):
    Res, Errmsg, Ret, Data = GetServiceConfig(IfsPortalIp, IfsPortalPort, ApiKey, ServiceType, logger=logger)
    if Res != ResOk:
        logger.error('fail to get config %s' % Errmsg)
    else:
        if (Ret.Result == ResultSuccess):
            DsPServiceConf(Data)
        else:
            logger.error('fail to get config %s' % Ret.Message)

    ConfigInfo = None
    if ServiceType == TypeServiceOSD:
        if Data:
            CurrentConf = json.loads(Data.Config)
            for info in OsdConfigInfo:
                try:
                    ConfKey = info['key']
                    info['value'] = CurrentConf[ConfKey]
                except Exception as err:
                    pass
        ConfigInfo = OsdConfigInfo

    elif ServiceType == TypeServiceS3:
        if Data:
            CurrentConf = json.loads(Data.Config)
            for info in OsdConfigInfo:
                try:
                    ConfKey = info['key']
                    info['value'] = CurrentConf[ConfKey]
                except Exception as err:
                    pass
        ConfigInfo = S3ConfigInfo

    elif ServiceType == TypeServiceMONGODB:
        if Data:
            CurrentConf = json.loads(Data.Config)
            for info in OsdConfigInfo:
                try:
                    ConfKey = info['key']
                    info['value'] = CurrentConf[ConfKey]
                except Exception as err:
                    pass
        ConfigInfo = MongoDbConfigInfo

    return ConfigInfo


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
