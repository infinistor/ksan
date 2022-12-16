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

import sys, os
if os.path.dirname(os.path.abspath(os.path.dirname(__file__))) not in sys.path:
    sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))

from portal_api.apis import *
from const.service import AddServiceInfoObject, \
     UpdateServiceInfoObject, UpdateServiceUsageObject, \
    AddServiceGroupItems
from const.common import ServiceGroupItemsModule, ServiceDetailModule, ServiceItemsDetailModule
from service.control import ServiceUnit

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

OsdDefaultConfigInfo = [{'key': 'osd.pool_size', 'value': OsdDefaultPoolsize, 'type': int, 'question': 'Insert connection pool size'},
                  {'key': 'osd.port', 'value': OsdDefaultPort, 'type': int, 'question': 'Insert ksanOSD port'},
                  {'key': 'osd.ec_check_interval', 'value': OsdDefaultEcScheduleMinute, 'type': int, 'question': 'Insert EC check interval(ms)'},
                  {'key': 'osd.ec_wait_time', 'value': OsdDefaultEcApplyMinute, 'type': int, 'question': 'Insert EC wait time(ms)'},
                  {'key': 'osd.ec_min_size', 'value': OsdDefaultEcFileSize, 'type': int, 'question': 'Insert EC file size'},
                  {'key': 'osd.cache_diskpath', 'value': OsdDefaultCacheDisk, 'type': str, 'question': 'Insert cache disk path', 'value_command': 'disable: NULL, '},
                  {'key': 'osd.cache_check_interval', 'value': OsdDefaultCacheScheduleMinute, 'type': int, 'question': 'Insert cache check interval(ms)'},
                  {'key': 'osd.cache_expire', 'value': OsdDefaultCacheLimitMinute, 'type': int, 'question': 'Insert cache expire(ms)'},
                  {'key': 'osd.trash_check_interval', 'value': OsdDefaultTrashScheduleMinute, 'type': int, 'question': 'Insert trash check interval(ms)'}]


S3DefaultDbRepository = TypeServiceMariaDB
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
S3DefaultGwLogging = 'on'
S3DefaultGwReplication = 2
S3DefaultGwOsdPort = 8000
S3DefaultGwJettyMaxThreads = 1000
S3DefaultGwJettyMaxIdleTime = 600000
S3DefaultGwOsdClientCount = 100
S3DefaultGwObjmanagerCount = 100
S3DefaultGwPerformanceMode = 'NO_OPTION'
S3DefaultCacheDisk = ''
S3DefaultCacheFileSize = 1024

ObjManagerDefaultDbRepository = TypeServiceMariaDB
ObjManagerDefaultDbHost = '127.0.0.1'
ObjManagerDefaultDatabase = 'ksan'
ObjManagerDefaultDbPort = 3306
ObjManagerDefaultDbUser = 'root'
ObjManagerDefaultDbPassword = 'YOUR_DB_PASSWORD'
ObjManagerDefaultMqHost = '127.0.0.1'
ObjManagerDefaultMqName = 'disk'
ObjManagerDefaultMqExchangeName = 'ksan.system'
ObjManagerDefaultMqOsdExchangeName = 'OSDExchange'

S3DefaultConfigInfo = [
    #{'key': 'gw.db_repository', 'value': S3DefaultDbRepository, 'type': str, 'question': 'Insert ksanGW DB repository', 'valid_answer_list': [TypeServiceMariaDB, TypeServiceMongoDB]},
    #{'key': 'gw.db_host', 'value': S3DefaultDbHost, 'type': str, 'question': 'Insert ksanGW DB host'},
    #{'key': 'gw.db_name', 'value': S3DefaultDatabase, 'type': str, 'question': 'Insert ksanGW DB name'},
    #{'key': 'gw.db_port', 'value': S3DefaultDbPort, 'type': int, 'question': 'Insert ksanGW DB port'},
    #{'key': 'gw.db_user', 'value': S3DefaultDbUser, 'type': str, 'question': 'Insert ksanGW DB user'},
    #{'key': 'gw.db_password', 'value': S3DefaultDbPassword, 'type': str, 'question': 'Insert ksanGW DB password'},
    #{'key': 'gw.db_pool_size', 'value': S3DefaultDbPoolSize, 'type': int, 'question': 'Insert ksanGW DB pool size'},
    {'key': 'gw.authorization', 'value': S3DefaultGwAuthrization, 'type': str, 'question': 'Insert authorization'},
    {'key': 'gw.endpoint', 'value': S3DefaultGwEndpoint, 'type': 'url', 'question': 'Insert endpoint url'},
    {'key': 'gw.secure_endpoint', 'value': S3DefaultGwSecureEndpoint, 'type': 'url',
     'question': 'Insert secure endpoint url'},
    {'key': 'gw.keystore_path', 'value': S3DefaultGwKeyStorePath, 'type': str, 'question': 'Insert keystore path'},
    {'key': 'gw.keystore_password', 'value': S3DefaultGwtKeyStorePassword, 'type': str,
     'question': 'Insert keystore password'},
    {'key': 'gw.max_file_size', 'value': S3DefaultGwMaxFileSize, 'type': int, 'question': 'Insert max file size'},
    {'key': 'gw.max_list_size', 'value': S3DefaultGwMaxListSize, 'type': int, 'question': 'Insert max sist size'},
    {'key': 'gw.max_timeskew', 'value': S3DefaultGwMaxTimeSkew, 'type': int, 'question': 'Insert max time skew'},
    {'key': 'gw.logging', 'value': S3DefaultGwLogging, 'type': str, 'question': 'Insert logging on/off', 'valid_answer_list': ['on', 'off']},
    {'key': 'gw.osd_port', 'value': S3DefaultGwOsdPort, 'type': int, 'question': 'Insert ksanOSD port'},
    {'key': 'gw.jetty_max_threads', 'value': S3DefaultGwJettyMaxThreads, 'type': int,
     'question': 'Insert Jetty max thread count'},
    {'key': 'gw.jetty_max_idle_timeout', 'value': S3DefaultGwJettyMaxIdleTime, 'type': int,
     'question': 'Insert Jetty max idle time'},
    {'key': 'gw.osd_client_count', 'value': S3DefaultGwOsdClientCount, 'type': int,
     'question': 'Insert ksanOSD client count'},
    {'key': 'gw.objmanager_count', 'value': S3DefaultGwObjmanagerCount, 'type': int,
     'question': 'Insert ksanObjManager count'},
    {'key': 'gw.performance_mode', 'value': S3DefaultGwPerformanceMode, 'type': str,
     'question': 'Insert performance mode'},
    {'key': 'gw.cache_diskpath', 'value': S3DefaultCacheDisk, 'type': str, 'question': 'Insert cache disk path', 'value_command': 'disable: NULL, '},
    {'key': 'gw.cache_file_size', 'value': S3DefaultCacheFileSize, 'type': int, 'question': 'Insert cache file size'},
    {'key': 'objM.db_repository', 'value': ObjManagerDefaultDbRepository, 'type': str,
     'question': 'Insert object DB repository', 'valid_answer_list': [TypeServiceMariaDB, TypeServiceMongoDB]},
    #{'key': 'objM.db_host', 'value': ObjManagerDefaultDbHost, 'type': str, 'question': 'Insert object DB host', 'valid_answer_list': [TypeServiceMariaDB, TypeServiceMongoDB]},
    {'key': 'objM.db_host', 'value': ObjManagerDefaultDbHost, 'type': str, 'question': 'Insert object DB host'},
    {'key': 'objM.db_name', 'value': ObjManagerDefaultDatabase, 'type': str, 'question': 'Insert object DB name'},
    {'key': 'objM.db_port', 'value': ObjManagerDefaultDbPort, 'type': int, 'question': 'Insert object DB port'},
    {'key': 'objM.db_user', 'value': ObjManagerDefaultDbUser, 'type': str, 'question': 'Insert object DB user'},
    {'key': 'objM.db_password', 'value': ObjManagerDefaultDbPassword, 'type': str, 'question': 'Insert object DB password'}
    #{'key': 'objM.mq_host', 'value': ObjManagerDefaultMqHost, 'type': str, 'question': 'Insert MQ host', 'Activate': False},
    #{'key': 'objM.mq_queue_name', 'value': ObjManagerDefaultMqName, 'type': str, 'question': 'Insert MQ name', 'Activate': False},
    #{'key': 'objM.mq_exchange_name', 'value': ObjManagerDefaultMqExchangeName, 'type': str, 'question': 'Insert MQ exchange name', 'Activate': False},
    #{'key': 'objM.mq_osd_exchange_name', 'value': ObjManagerDefaultMqOsdExchangeName, 'type': str, 'question': 'Insert MQ ksanOSD exchange name', 'Activate': False}
    #{'key': 'objM.mq.host', 'value': ObjManagerDefaultMqHost, 'type': 'ip', 'question': 'Insert MQ Host'},
    #{'key': 'objM.mq.diskpool.queuename', 'value': ObjManagerDefaultMqDiskpoolQueueName, 'type': str,
    # 'question': 'Insert MQ Disk pool Queue Name'},
    #{'key': 'objM.mq.diskpool.exchangename', 'value': ObjManagerDefaultMqDiskpoolExchangeName, 'type': str,
    # 'question': 'Insert MQ Disk pool Exchange Name'},
    #{'key': 'objM.mq.osd.exchangename=OSDExchange', 'value': ObjManagerDefaultMqOsdExchangeName, 'type': str,
    # 'question': 'Insert MQ ksanOSD Exchange Name'}
]










MongoDbDefaultShard1Port = 20001
MongoDbDefaultShard2Port = 20002
MongoDbDefaultConfigServerPort = 50000
MongoDbDefaultDbPort = 27017
MongoDbDefaultHomeDir = '/var/lib/mongo'
MongoDbDefaultPrimaryHost = socket.gethostname()

MongoDbConfigInfo = [
    {'key': 'Shard1Port', 'value': MongoDbDefaultShard1Port, 'type': int, 'question': 'Insert MongoDB shard1 port'},
    {'key': 'Shard2Port', 'value': MongoDbDefaultShard2Port, 'type': int, 'question': 'Insert MongoDB shard2 port'},
    {'key': 'ConfigServerPort', 'value': MongoDbDefaultConfigServerPort, 'type': int, 'question': 'Insert MongoDB config server port'},
    {'key': 'MongoDbPort', 'value': MongoDbDefaultDbPort, 'type': int,
     'question': 'Insert MongoDB DB port'},
    {'key': 'HomeDir', 'value': MongoDbDefaultHomeDir, 'type': str,
     'question': 'Insert MongoDB home directory'},
    {'key': 'PrimaryHostName', 'value': MongoDbDefaultPrimaryHost, 'type': str,
     'question': 'Insert MongoDB primary cluster node hostname'}
]

lifecycleDefaultDbRepository = TypeServiceMariaDB
lifecycleDefaultDbHost = '127.0.0.1'
lifecycleDefaultDbPort = 3306
lifecycleDefaultDbName = 'ksan'
lifecycleDefaultDbUser = 'root'
lifecycleDefaultDbPassword = 'YOUR_DB_PASSWORD'
lifecycleDefaultRegion = 'kr-ksan-1'
lifecycleDefaultRuntime = '01:00'
lifecycleDefaultCheckInterval = 5000

LifecycleDefaultConfigInfo = [{'key': 'objM.db_repository', 'value': lifecycleDefaultDbRepository, 'type': str, 'question': 'Insert ksanGW DB repository'},
                  {'key': 'objM.db_host', 'value': lifecycleDefaultDbHost, 'type': str, 'question': 'Insert ksanGW DB host'},
                  {'key': 'objM.db_port', 'value': lifecycleDefaultDbPort, 'type': int, 'question': 'Insert ksanGW DB port'},
                  {'key': 'objM.db_name', 'value': lifecycleDefaultDbName, 'type': str, 'question': 'Insert ksanGW DB name'},
                  {'key': 'objM.db_user', 'value': lifecycleDefaultDbUser, 'type': str, 'question': 'Insert ksanGW DB user'},
                  {'key': 'objM.db_password', 'value': lifecycleDefaultDbPassword, 'type': str, 'question': 'Insert ksanGW DB password'},
                  {'key': 'ksan.region', 'value': lifecycleDefaultRegion, 'type': str, 'question': 'Insert region'},
                  {'key': 'lifecycle.schedule', 'value': lifecycleDefaultRuntime, 'type': str, 'question': 'Insert runtime'},
                  {'key': 'lifecycle.check_interval', 'value': lifecycleDefaultCheckInterval, 'type': int, 'question': 'Insert check interval'}]


ReplicationDefaultUploadThreadCount = 20
ReplicationDefaultMultipartSize = 5242880
ReplicationDefaultConfigInfo = [

    {'key': 'objM.db_repository', 'value': S3DefaultDbRepository, 'type': str, 'question': 'Insert object DB repository', 'valid_answer_list': [TypeServiceMariaDB, TypeServiceMongoDB]},
    {'key': 'objM.db_host', 'value': ObjManagerDefaultDbHost, 'type': str, 'question': 'Insert object DB host'},
    {'key': 'objM.db_port', 'value': ObjManagerDefaultDbPort, 'type': int, 'question': 'Insert object DB port'},
    {'key': 'objM.db_name', 'value': ObjManagerDefaultDatabase, 'type': str, 'question': 'Insert object DB name'},
    {'key': 'objM.db_user', 'value': ObjManagerDefaultDbUser, 'type': str, 'question': 'Insert object DB user'},
    {'key': 'objM.db_password', 'value': ObjManagerDefaultDbPassword, 'type': str,
     'question': 'Insert object DB password'},
    {'key': 'ksan.region', 'value': lifecycleDefaultRegion, 'type': str, 'question': 'Insert region'},
    {'key': 'replication.upload_thread_count', 'value': ReplicationDefaultUploadThreadCount, 'type': int, 'question': 'Insert upload thread count'},
    {'key': 'replication.multipart_size', 'value': ReplicationDefaultMultipartSize, 'type': int,
     'question': 'Insert multipart size'}
]

DefaultDbLogExpireDays = 30
LogManagerDefaultCheckInterval = 300000
LogManagerDefaultConfigInfo = [
    {'key': 'ksan.region', 'value': lifecycleDefaultRegion, 'type': str, 'question': 'Insert region'},
    {'key': 'logM.db_repository', 'value': S3DefaultDbRepository, 'type': str,
     'question': 'Insert object DB repository',
     'valid_answer_list': [TypeServiceMariaDB, TypeServiceMongoDB]},
    {'key': 'logM.db_host', 'value': ObjManagerDefaultDbHost, 'type': str,
     'question': 'Insert object DB host'},
    {'key': 'logM.db_port', 'value': ObjManagerDefaultDbPort, 'type': int, 'question': 'Insert object DB port'},
    {'key': 'logM.db_name', 'value': ObjManagerDefaultDatabase, 'type': str, 'question': 'Insert object DB name'},
    {'key': 'logM.db_user', 'value': ObjManagerDefaultDbUser, 'type': str,
     'question': 'Insert object DB user'},
    {'key': 'logM.db_password', 'value': ObjManagerDefaultDbPassword, 'type': str,
     'question': 'Insert object DB password'},
    {'key': 'logM.db_pool_size', 'value': S3DefaultDbPoolSize, 'type': str, 'question': 'Insert object DB pool size'},
    {'key': 'logM.db_expires', 'value': DefaultDbLogExpireDays, 'type': str, 'question': 'Insert object DB log expire days'},
    {'key': 'logM.check_interval', 'value': LogManagerDefaultCheckInterval, 'type': str, 'question': 'Insert check interval'},
]


BackendConfigInfo = dict()
BackendConfigInfo[TypeServiceLifecycle] = LifecycleDefaultConfigInfo
BackendConfigInfo[TypeServiceReplication] = ReplicationDefaultConfigInfo
BackendConfigInfo[TypeServiceLogManager] = LogManagerDefaultConfigInfo

@catch_exceptions()
def AddService(MgsIp, Port, ApiKey, ServiceName, ServiceType, ServerId=None, VlanIds=[], ServerName=None, GroupId='', Description='', logger=None):
    #VlanIds = GetVlanIdListFromServerDetail(MgsIp, Port, ApiKey, ServerId=ServerId, ServerName=ServerName, logger=logger)
    #if VlanIds is None:
    #    return ResEtcErrorCode, ResFailToGetVlainId, None
    if ServerId is not None:
        TargetServer = ServerId
    elif ServerName is not None:
        TargetServer = ServerName
    else:
        return ResInvalidCode, ResInvalidMsg + 'ServerName is required', None


    Service = AddServiceInfoObject()
    Service.Set(ServiceName, TargetServer, ServiceType, GroupId, VlanIds, State='Offline', Description=Description, HaAction='Initializing')
    if ServiceType is None:
        return ResInvalidCode, ResInvalidMsg + 'ServiceType is required', None

    Url = "/api/v1/Services"
    body = jsonpickle.encode(Service, make_refs=False)
    Conn = RestApi(MgsIp, Port, Url, authkey=ApiKey, params=body, logger=logger)
    Res, Errmsg, Data = Conn.post(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data

def RegisterService(conf, ServiceType, logger, ServiceNicname=None):
    """

    :param Conf:
    :param ServiceType:
    :param logger:
    :return:
    """
    out, err = shcall('hostname')
    ServiceName = '%s_%s' % (ServiceNicname, out[:-1])
    Res, Errmsg, Ret = AddService(conf.PortalHost, conf.PortalPort, conf.PortalApiKey,  ServiceName, ServiceType,
                                  conf.ServerId, logger=logger)
    if Res == ResOk:
        if Ret.Result != ResultSuccess:
            if Ret.Result != CodeDuplicated:
                logger.error("%s %s" % (str(Ret.Result), str(Ret.Message)))
        else:
            logging.log(logging.INFO, "%s is registered. %s %s" % (ServiceType, str(Ret.Result), str(Ret.Message)))
    else:
        logger.error("%s %s" % (str(Ret.Result), str(Ret.Message)))

def KsanServiceRegister(conf, ServiceType, logger):
    Retry = 0
    while True:
        Retry += 1
        Res, Errmsg, Ret, Data = GetServerInfo(conf.PortalHost, conf.PortalPort,conf.PortalApiKey , ServerId=conf.ServerId, logger=logger)
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
        if Retry > ServiceContolRetryCount:
            isNetworkAdded = False
            break
        time.sleep(IntervalMiddle)

    if isNetworkAdded is True:
        RegisterService(conf, ServiceType, logger)

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


def TmpGetServiceInfoList(Ip, Port):
    Res, Errmsg, Ret = GetServiceInfo(Ip, Port)
    if Res == ResOk:
        if Ret == ResultSuccess:
            return Res, Errmsg, Ret.Data
        else:
            return Ret.Result, Ret.Message, None
    return Res, Errmsg, None


@catch_exceptions()
def ShowServiceInfoWithServerInfo(ServiceList, Detail=DetailInfo, Ip=None, Port=None, SysinfoDisp=False):
        #Ret, Errmsg , Data = TmpGetServiceInfoList(Ip, Port)
        #if Ret != ResOk:
        #    print('Fail to get Service Info %s' % Errmsg)
        #    return

        ServiceList = ServiceListOrdering(ServiceList, OrderingBase='ServerName')
        if SysinfoDisp is True:
            ServiceTitleLine = '%s' % ('=' * 105)
            ServiceTitle ="|%s|%s|%s|%s|%s|" % ('ServerName'.ljust(25), 'ServiceName'.ljust(30), 'Status'.ljust(10), 'Type'.ljust(30), ' ' * 4)
            ServiceDataLine = '%s' % ('-' * 105)
        else:
            if Detail == MoreDetailInfo:

                ServiceTitleLine = '%s' % ('=' * 163)
                ServiceDataLine = '%s' % ('-' * 163)
                ServiceTitle = "|%s|%s|%s|%s|%s|%s|%s|%s|" % (
                    'ServerName'.ljust(20), 'ServiceName'.ljust(30), 'Status'.ljust(10),
                    'Type'.ljust(30), 'CpuUsage'.ljust(8), 'MemUsed'.ljust(8), 'Thread Cnt'.ljust(10),
                    'ServiceId'.ljust(38))
            elif Detail == DetailInfo:
                ServiceTitleLine = '%s' % ('=' * 124)
                ServiceDataLine = '%s' % ('-' * 124)
                ServiceTitle = "|%s|%s|%s|%s|%s|%s|%s|" % (
                    'ServerName'.ljust(20), 'ServiceName'.ljust(30),
                    'Status'.ljust(10), 'Type'.ljust(30), 'CpuUsage'.ljust(8), 'MemUsed'.ljust(8),
                    'Thread Cnt'.ljust(10))
            else:
                ServiceTitleLine = '%s' % ('=' * 95)
                ServiceDataLine = '%s' % ('-' * 95)
                ServiceTitle = "|%s|%s|%s|%s|" % ('ServerName'.ljust(20), 'ServiceName'.ljust(30),
                    'Status'.ljust(10), 'Type'.ljust(30))

        print(ServiceTitleLine)
        print(ServiceTitle)
        print(ServiceTitleLine)

        if len(ServiceList) <= 0:
            print('No service data')
            print(ServiceDataLine)
        else:
            for Svc in ServiceList:
                if SysinfoDisp is True:
                    _Svc = "|%s|%s|%s|%s|%s|" % (Svc.Server.Name.ljust(25), str(Svc.Name).ljust(30), DisplayState(Svc.State).ljust(10), str(Svc.ServiceType).ljust(30), ' ' * 4)
                else:
                    if Detail == MoreDetailInfo:
                        _Svc = "|%s|%s|%s|%s|%s|%s|%s|%s|" % \
                               ( Svc.Server.Name.ljust(20), str(Svc.Name).ljust(30),
                                DisplayState(Svc.State).ljust(10), str(Svc.ServiceType).ljust(30),
                                "{:,}".format(Svc.CpuUsage).rjust(8),
                                Byte2HumanValue(str(Svc.MemoryUsed), 'UsedSize').rjust(10), "{:,}".format(int(Svc.ThreadCount)).rjust(10), Svc.Id.ljust(38))
                    elif Detail == DetailInfo:
                        _Svc = "|%s|%s|%s|%s|%s|%s|%s|" % \
                               (Svc.Server.Name.ljust(20), str(Svc.Name).ljust(30),
                                DisplayState(Svc.State).ljust(10), str(Svc.ServiceType).ljust(30),
                                "{:,}".format(Svc.CpuUsage).rjust(8),
                                Byte2HumanValue(str(float(Svc.MemoryUsed)), 'UsedSize').rjust(10), "{:,}".format(int(Svc.ThreadCount)).rjust(10))
                    else:
                        _Svc ="|%s|%s|%s|%s|" % \
                            (Svc.Server.Name.ljust(20), str(Svc.Name).ljust(30), DisplayState(Svc.State).ljust(10), str(Svc.ServiceType).ljust(30))

                print(_Svc)
                print(ServiceDataLine)

def ServiceListOrdering(ServiceList, OrderingBase='ServerName'):
    TotalNewServiceList = list()

    if OrderingBase == 'ServerName':
        ServerNameDict = dict()
        for serviceinfo in ServiceList:
            ServerName = serviceinfo.Server.Name
            if ServerName not in ServerNameDict:
                ServerNameDict[ServerName] = list()
                ServerNameDict[ServerName].append(serviceinfo)
            else:
                ServerNameDict[ServerName].append(serviceinfo)

        for servername in sorted(ServerNameDict.keys(), key=str.casefold):
            servicelist = ServerNameDict[servername]
            ServiceNameDict = dict()
            for idx, service in enumerate(servicelist):
                ServiceNameDict[service.Name] = service

            newservicelist = list()
            for servicename in sorted(ServiceNameDict.keys(), key=str.casefold) :
                service = ServiceNameDict[servicename]
                newservicelist.append(service)

            TotalNewServiceList += newservicelist
    else:
        ServiceNameDict = dict()
        for serviceinfo in ServiceList:
            servicename = serviceinfo.Name
            ServiceNameDict[servicename] = serviceinfo

        for servicename in sorted(ServiceNameDict.keys(), key=str.casefold):
            serviceinfo = ServiceNameDict[servicename]
            TotalNewServiceList.append(serviceinfo)


    return TotalNewServiceList



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
def GetSeviceConfStringForDisplay(Conf, Prefix, ConfFile=None):
    Conf = json.loads(Conf, strict=False)
    StrConf = ''
    if ConfFile is None:
        for key1, val1 in Conf.items():
            if isinstance(val1, dict):
                for key2, val2 in val1.items():
                    ConfLineStr = "%s_%s=%s" % (key1, key2, str(val2))
                    StrConf += "%s%s|\n" % (Prefix, ConfLineStr.ljust(100-13))
            else:
                ConfLineStr = "%s=%s" % (key1, str(val1))
                StrConf += '%s%s|\n' % (Prefix, ConfLineStr.ljust(100-13))
    else:
        for key1, val1 in Conf.items():
            if isinstance(val1, dict):
                for key2, val2 in val1.items():
                    StrConf += "%s_%s=%s\n" % (key1, key2, str(val2))
            else:
                StrConf += "%s=%s\n" % (key1, str(val1))

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
    Res, Errmsg, Ret, Data = GetServerInfo(conf.PortalHost, conf.PortalPort, conf.PortalApiKey, conf.ServerId, logger=logger)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            for service in Data.Services:
                ServiceList['IdList'].append(service.Id)
                ServiceList['Details'][service.Id] = service
            for Interface in Data.NetworkInterfaces:
                LocalIpList.append(Interface.IpAddress)
            return True
        else:
            logger.error('fail to get service list with serverId %s. %s' % (conf.ServerId, Ret.Message))
            return False
    else:
        logger.error('fail to get service list with serverId %s. %s' % (conf.ServerId, Errmsg))
        return False

def IsValidServiceIdOfLocalServer(conf, logger, TargetServiceId):
    ServiceList = dict()
    LocalIpList = list()
    ServiceList['IdList'] = list()
    ServiceList['Details'] = dict()
    RetryCnt = 3
    while True:
        RetryCnt -= 1
        Ret = LoadServiceList(conf, ServiceList, LocalIpList, logger)
        if Ret is True:
            break
        if RetryCnt < 0:
            logger.error('fail to check if the added serviced %s ' % TargetServiceId)
            return False
        else:
            time.sleep(IntervalShort)

    if TargetServiceId in ServiceList['IdList']:
        return True
    else:
        logger.error('the added serviceId %s is not local server\'s' % TargetServiceId)
        return False

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

def DsPServiceConf(Config, TopTitleLine=False, ConfFile=None):
    """
    Display service config with type
    :param Config:
    :param TopTitleLine:
    :return:
    """
    Type = Config.Type
    Version = Config.Version
    Conf = Config.Config
    ConfPartPrefix = '|%s|' % (' ' * 10)
    RegiDate = Config.RegDate
    StrConf = GetSeviceConfStringForDisplay(Conf, ConfPartPrefix, ConfFile=ConfFile)
    if ConfFile is None:
        if StrConf is None:
            print('Invalid Conf. version:%d \n%s' % (Version, str(Conf)))
        else:
            #if TopTitleLine is not True:
            #    print(ConfigCompartLine)
            ConfigStr = """%s
|%s|%s|
%s 
|%s|%s| 
%s
|%s|%s|         
%s
|%s|%s| 
%s%s """ % (ConfigCompartLine, 'Type'.center(10), Type.center(ConfigMaxValueLen), ConfigTitleLine, 'Version'.center(10),
               str(Version).center(ConfigMaxValueLen), ConfigTitleLine, 'Date'.center(10), RegiDate.center(ConfigMaxValueLen),
               ConfigTitleLine,
               'Conf'.center(10), ' ' * (100 - 13), StrConf.center(ConfigMaxValueLen), ConfigCompartLine)
            print(ConfigStr)
    else:
        with open(ConfFile, 'w') as f:
            f.write(StrConf)


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
def SetServiceConfig(IfsPortalIp, IfsPortalPort, ApiKey, ServiceType, ConfFile=None, logger=None):
    # Get Current Service config
    Ret, Conf = GetConfig(IfsPortalIp, IfsPortalPort, ApiKey, ServiceType, logger, ConfFile=ConfFile)
    if Ret is False:
        return Ret, Conf, None

    Url = "/api/v1/Config/%s" % ServiceType
    body = Conf
    Conn = RestApi(IfsPortalIp, IfsPortalPort, Url, authkey=ApiKey, params=body, logger=logger)
    Res, Errmsg, Data = Conn.post(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data


@catch_exceptions()
def GetLatestServiceConfig(PortalIp, PortalPort, PortalApiKey, ServiceType, logger=None):
    ServiceType = ServiceTypeConversion[ServiceType.lower()]
    Res, Errmsg, Ret, Data = GetServiceConfigList(PortalIp, PortalPort, PortalApiKey, ServiceType, logger=logger)
    if Res != ResOk:
        print(Errmsg)
        return None, Errmsg
    else:
        if Ret.Result == ResultSuccess:
            LatestVersionId = 0
            LatestConfig = None
            for Config in Data.Items:
                if Config.Version > LatestVersionId:
                    LatestVersionId = Config.Version
                    LatestConfig = Config
            return LatestConfig, ''
        else:
            return None, Ret.Message

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


def GetConfig(IfsPortalIp, IfsPortalPort, ApiKey, ServiceType, logger, ConfFile=None):
    if ConfFile is None:
        Conf = GetConfigFromUser(IfsPortalIp, IfsPortalPort, ApiKey, ServiceType, logger)
        Conf = json.dumps(Conf)
        Conf = Conf.replace('"', '\\"')
        Conf = '"'+ Conf + '"'
        return True, Conf
    else:
        if os.path.exists(ConfFile):
            Ret, Conf = GetConf(ConfFile, FileType='non-ini', ReturnType='dict')
            if Ret is True:
                Conf = json.dumps(Conf)
                Conf = Conf.replace('"', '\\"')
                Conf = '"' + Conf + '"'
                return True, Conf
            else:
                return Ret, Conf
        else:
            return False, '%s is not found' % ConfFile


def GetConfigFromUser(IfsPortalIp, IfsPortalPort, ApiKey, ServiceType, logger):

    ConfigInfo = GetServiceCurrentConfigInfo(IfsPortalIp, IfsPortalPort, ApiKey, ServiceType, logger)

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
        if 'Activate' in info:
            if info['Activate'] is False:
                Conf[ConfKey] = info['value']
                continue
        ValueComment = ''
        if 'value_command' in info:
            ValueComment = info['value_command']

        Conf[ConfKey]= get_input(QuestionString, ValueType, DefaultValue, ValidAnsList=ValueAnswerList, ValueComment=ValueComment)
    return Conf


def GetServiceCurrentConfigInfo(PortalIp, PortalPort, PortalApiKey, ServiceType, logger):
    # Get MqConfig
    """
    Get Current Config if exists, or Default Config if not exists.
    :param PortalIp:
    :param PortalPort:
    :param PortalApiKey:
    :param ServiceType:
    :param logger:
    :return:
    """

    Res, Errmsg, Ret, Data = GetServiceConfig(PortalIp, PortalPort, PortalApiKey, ServiceType, logger=logger)
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
            for info in OsdDefaultConfigInfo:
                try:
                    ConfKey = info['key']
                    info['value'] = CurrentConf[ConfKey]
                except Exception as err:
                    pass
        ConfigInfo = OsdDefaultConfigInfo

    elif ServiceType == TypeServiceGW:

        if Data:

            CurrentConf = json.loads(Data.Config)
            for info in S3DefaultConfigInfo:
                try:
                    ConfKey = info['key']
                    info['value'] = CurrentConf[ConfKey]
                except Exception as err:
                    pass
        else:

            # Get RaggitMq Config
            Ret, MqConfig = GetBaseConfig(PortalIp, PortalPort, PortalApiKey, TypeServiceRabbitMq, logger)
            if Ret is False:
                return None
            CurrentMqConfig = json.loads(MqConfig.Config)

            # Get MariaDB Config
            Ret, MariaDBConfig = GetBaseConfig(PortalIp, PortalPort, PortalApiKey, TypeServiceMariaDB, logger)
            if Ret is False:
                return None

            CurrentMariaDBConfig = json.loads(MariaDBConfig.Config)

            UpdateGwBaseConfig(S3DefaultConfigInfo, CurrentMqConfig, CurrentMariaDBConfig)

        ConfigInfo = S3DefaultConfigInfo

    elif ServiceType == TypeServiceMongoDB:
        if Data:
            CurrentConf = json.loads(Data.Config)
            for info in OsdDefaultConfigInfo:
                try:
                    ConfKey = info['key']
                    if ConfKey == 'objM.mq_host':
                        info['value'] = CurrentConf['Host']
                    elif ConfKey == 'objM.mq_osd_exchange_name':
                        info['value'] = CurrentConf['ExchangeName']
                except Exception as err:
                    pass
        ConfigInfo = MongoDbConfigInfo

    elif ServiceType in [TypeServiceLifecycle, TypeServiceReplication, TypeServiceLogManager]:
        if Data:
            CurrentConf = json.loads(Data.Config)
            for info in BackendConfigInfo[ServiceType]:
                try:
                    ConfKey = info['key']
                    info['value'] = CurrentConf[ConfKey]
                except Exception as err:
                    pass
        ConfigInfo = BackendConfigInfo[ServiceType]


    return ConfigInfo

def GetBaseConfig(PortalIp, PortalPort, PortalApiKey, ConfigType, logger ):
    """
    Get Base(Mariadb, RabbitMq) Config
    :param PortalIp:
    :param PortalPort:
    :param PortalApiKey:
    :param ConfigType:
    :param logger:
    :return:
    """

    Res, Errmsg, Ret, MqConfig = GetServiceConfig(PortalIp, PortalPort, PortalApiKey, ConfigType, logger=logger)
    if Res != ResOk:
        logger.error('fail to get %s config %s' % (ConfigType, Errmsg))
    else:
        if (Ret.Result == ResultSuccess):
            #DsPServiceConf(MqConfig)
            return True, MqConfig
        else:
            logger.error('fail to get %s config %s' % (ConfigType, Ret.Message))
    return False, None

def UpdateGwBaseConfig(GwConfig, MqConfig, MariaDBConfig):
    """
    Merge GwConfig and MqConfig.
    :param GwConfig:
    :param MqConfig:
    :return:
    """
    MqKeyMap = {"objM.mq_host": "Host", "objM.mq_exchange_name": "ExchangeName"}
    MariaDBKeyMap = {"gw.db_host": "Host", "gw.db_name": "Name", "gw.db_port": "Port", "gw.db_user": "User",
                  "gw.db_password": "Password", "objM.db_host": "Host", "objM.db_name": "Name", "objM.db_port": "Port",
                     "objM.db_user=root": "User", "objM.db_password": "Password"}


    for info in GwConfig:
        try:
            ConfKey = info['key']
            if ConfKey in MqKeyMap:
                BaseKey = MqKeyMap[ConfKey]
                info['value'] = MqConfig[BaseKey]
            elif ConfKey in MariaDBKeyMap:
                BaseKey = MariaDBKeyMap[ConfKey]
                info['value'] = MariaDBConfig[BaseKey]

        except Exception as err:
            print('fail to update ksanGW Mq & MariaDB Config %s' % str(err))

def isKsanServiceIdFileExists(ServiceType):
    """
    check if systemd service file exists or not in local server or docker
    :param ServiceType:
    :return: if File exists: True, else False
    """
    if ServiceType in ServiceTypeServiceHiddenPathMap:
        isDockerService = False

        # check if the service is docker support or not from system service file
        if ServiceType in ServiceTypeSystemdServiceMap:
            SystemdServiceUnitPath = ServiceTypeSystemdServiceMap[ServiceType]
            try:
                with open('/etc/systemd/system/'+ SystemdServiceUnitPath, 'r') as f:
                    SystemdServiceContens = f.read()
                    if 'Requires=docker.service' in SystemdServiceContens:
                        isDockerService = True
            except Exception as err:
                print('fail to read system service file %s %s.' % (SystemdServiceUnitPath, str(err)))
                return False, 'fail to read system service file %s %s.' % (SystemdServiceUnitPath, str(err))


        ServiceHiddenPath = ServiceTypeServiceHiddenPathMap[ServiceType]

        if isDockerService is False:
            if os.path.exists(ServiceHiddenPath):
                return True, ''
            else:
                return False, ''
        else:
            # copy from docker to local and check if serviceid file exists or not
            CopiedServiceIdFile = '/tmp/.TmpServiceId%d' % int(time.time())
            ServiceContainerName = ServiceTypeDockerServiceContainerNameMap[ServiceType]
            DockerGetServiceFileCmd = 'docker cp %s:%s %s' % (ServiceContainerName, ServiceHiddenPath, CopiedServiceIdFile)
            shcall(DockerGetServiceFileCmd)
            if os.path.exists(CopiedServiceIdFile):
                return True, ''
            else:
                return False, ''
    else:
        return True, ''


def SaveKsanServiceIdFile(ServiceType, ServiceId):
    if ServiceType in ServiceTypeServiceHiddenPathMap:
        ServiceHiddenPath = ServiceTypeServiceHiddenPathMap[ServiceType]
        isDockerService = False

        if ServiceType in ServiceTypeSystemdServiceMap:
            SystemdServiceUnitPath = ServiceTypeSystemdServiceMap[ServiceType]
            try:
                with open('/etc/systemd/system/'+ SystemdServiceUnitPath, 'r') as f:
                    SystemdServiceContens = f.read()
                    if 'Requires=docker.service' in SystemdServiceContens:
                        isDockerService = True
            except Exception as err:
                return False, 'fail to read systemd service file %s %s' % (SystemdServiceUnitPath, str(err))

        try:
            with open(ServiceHiddenPath, 'w') as f:
                f.write(ServiceId)
                f.flush()
        except Exception as err:
            return False, 'fail to create ServiceId file  %s %s' % (ServiceHiddenPath, str(err))

        if isDockerService is True:
            ServiceContainerName = ServiceTypeDockerServiceContainerNameMap[ServiceType]
            try:
                SystemdServiceUnitPath = ServiceTypeSystemdServiceMap[ServiceType]
                ServiceStopCmd = 'systemctl stop %s' % SystemdServiceUnitPath
                shcall(ServiceStopCmd)
                DockerCopyCmd = 'docker cp %s %s:%s' % (ServiceHiddenPath, ServiceContainerName, ServiceHiddenPath)
                shcall(DockerCopyCmd)
                os.unlink(ServiceHiddenPath)
                return True, ''
            except Exception as err:
                return False, 'fail to create ServiceId file  %s:%s %s' % (ServiceContainerName, ServiceHiddenPath, str(err))
        else:
            return True, ''
    else:
        return False, 'service type %s is invalid' % ServiceType

@catch_exceptions()
def MqServiceHandler(MonConf, RoutingKey, Body, Response, ServerId, ServiceList, LocalIpList, GlobalFlag, logger):
    """
    Message Queue Service Handler
    """
    logger.debug("%s %s %s" % (str(RoutingKey), str(Body), str(Response)))
    ResponseReturn = MqReturn(ResultSuccess)
    Body = Body.decode('utf-8')
    Body = json.loads(Body)
    body = DictToObject(Body)

    # Service Control Handle
    if RoutKeyServiceControlFinder.search(RoutingKey):
        logging.log(logging.INFO, "%s" % str(Body))
        #if body.Id not in ServiceList['IdList']:
        #    LoadServiceList(MonConf, ServiceList, LocalIpList, logger)

        Response.IsProcessed = True
        #if body.Id not in ServiceList['Details']:
        #    logger.error('Invalid Service Id %s' % body.Id)
        #    return ResponseReturn
        ServiceType = body.ServiceType
        #if ServiceType == TypeHaproxy:
        #    Ret = Hap.StartStop(Body)
        #elif ServiceType == TypeS3:
        #    Ret = S3.StartStop(Body)
        service = ServiceUnit(logger, ServiceType)
        if body.Control == START:
            Ret, ErrMsg = service.Start()
        elif body.Control == STOP:
            Ret, ErrMsg = service.Stop()
        elif body.Control == RESTART:
            Ret, ErrMsg = service.Restart()
        elif ServiceType.lower() == TypeServiceAgent.lower():
            Ret = True
            ErrMsg = ''
        else:
            Ret = False
            ErrMsg = 'Invalid Serivce Type'

        if Ret is False:
            ResponseReturn = MqReturn(Ret, Code=1, Messages=ErrMsg)
            logger.error(ResponseReturn)
        else:
            logging.log(logging.INFO, ResponseReturn)
        return ResponseReturn
    # Serivce Config Handle
    elif ".config." in RoutingKey:
        if body.Id in ServiceList['IdList']:
            Response.IsProcessed = True
            logger.debug(ResponseReturn)
        return ResponseReturn
    elif RoutingKey.endswith((".added", ".updated", ".removed")):
        ServiceType = body.ServiceType
        GlobalFlag['ServiceUpdated'] = Updated

        if RoutingKey.endswith(".added"):
            ret, errlog = isKsanServiceIdFileExists(ServiceType)
            if ret is False:
                logger.debug('ServiceId file is not found. %s %s' % (body.Id, errlog))

                ret = IsValidServiceIdOfLocalServer(MonConf, logger, body.Id)
                if ret is True:
                    ret, errlog = SaveKsanServiceIdFile(ServiceType, body.Id)
                    if ret is False:
                        logger.error(errlog)

        Response.IsProcessed = True
        logger.debug(ResponseReturn)
        return ResponseReturn
    else:
        Response.IsProcessed = True
        return ResponseReturn

