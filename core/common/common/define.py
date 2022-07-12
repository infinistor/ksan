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

import json
import sys
import platform
import socket
import psutil
import time
import inspect
import re
import requests
from common.log import catch_exceptions

"""
######### environment configuration define ########
"""

KsanEtcPath = '/usr/local/ksan/etc'
KsanBinPath = '/usr/local/ksan/bin'

##### global variable #####
Updated = 1
Checked = 0

##### configuration path #####
MonServicedConfPath = '/usr/local/ksan/etc/ksanMon.conf'
OsdServiceConfPath = '/usr/local/ksan/etc/ksanOsd.conf'
GwServiceConfPath = '/usr/local/ksan/etc/ksanGW.conf'
ObjmanagerServiceConfPath = '/usr/local/ksan/etc/objmanger.conf'
DiskPoolXmlPath = '/usr/local/ksan/etc/diskpools.xml'
ServicePoolXmlPath = '/usr/local/ksan/etc/servicepools.xml'
OsdXmlFilePath = '/usr/local/ksan/etc/ksan-osd-log.xml'
GwXmlFilePath = '/usr/local/ksan/etc/ksan-gw-log.xml'

##### service binary #####
KsanOsdBinaryName = 'ksan-osd.jar'
KsanGwBinaryName = 'ksan-gw.jar'
KsanRecovery = 'ksanRecovery'


NormalCatalinaScriptPath = '/usr/local/ksan/bin/catalina.sh'
NormalTomcatStartShellPath = '/usr/local/ksan/bin/startup.sh'
NormalTomcatShutdownShellPath = '/usr/local/ksan/bin/shutdown.sh'
NormalTomcatWebXmlPath = '/usr/local/ksan/bin/web.xml'
S3WarFilePath = '/usr/local/ksan/bin/S3.war'

##### default ip, port #####
DefaultMgsIp = '127.0.0.1'
DefaultIfPortalPort = 5443
DefaultIfMqPort = 5672

##### Pid File Path #####
KsanOsdPidFile = '/var/run/ksanOsd.pid'
KsanGwPidFile = '/var/run/ksanGw.pid'
KsanMonPidFile = '/var/run/ksanMon.pid'
KsanEdgePidFile = '/var/run/ksanEdge.pid'
KsanMongosPidFile = '/var/run/mongod.pid'

##### INTERVAL #####
DiskMonitorInterval = 10
ProcessMonitorInterval = 10
ServerMonitorInterval = 10
ServiceMonitorInterval = 10
NetworkMonitorInterval = 10


"""
######## protocol ########
"""
GET = 1
POST = 2
PUT = 3
DELETE = 4

##### mq service status #####
START = 'Start'
STOP = 'Stop'
RESTART = 'Restart'
ONLINE = 'Online'
OFFLINE = 'Offline'

##### return code & messages define #####
ResOk = 0
ResNotFoundCode = 2
ResNotFoundMsg = 'Not found '
ResInvalidCode = 22
ResInvalidMsg = 'Invalid Error '
ResConnectionErrorCode = 111
ResConnectionErrorMsg ='Connection Error '
ResTimeErrorCode = 11
ResTimeErrorCodeMsg = 'Timeout Error '
ResDuplicateCode = 17
ResDuplicateMsg = 'Duplicated'

ResEtcErrorCode = 1
ResEtcErrorMsg = 'Other Error '
ResFailToGetVlainId = 'Fail to get Vlan Id'

ResultFail = 1  # fail to get data

ResultSuccess = 'Success'

CodeDuplicated = 'EC036'

"""
######### mq info define #########
"""
MqVirtualHost = '/'
MqUser = 'ksanmq'
MqPassword = 'YOUR_MQ_PASSWORD'

DiskStart = 'Good'
DiskStop = 'Stop'
MqDiskQueueName = 'disk'
MqDiskQueueExchangeName = 'disk'
MqDiskQueueRoutingKey = "*.services.disks.control"

## server routing key
RoutKeyServerAdd = '*.servers.added'
RoutKeyServerAddFinder = re.compile('.servers.added')
RoutKeyServerDel = '*.servers.removed'
RoutKeyServerDelFinder = re.compile('.servers.removed')
RoutKeyServerUpdate = '*.servers.updated'
RoutKeyServerUpdateFinder = re.compile('.servers.updated')
RoutKeyServerState = '*.servers.state'
RoutKeyServerUsage = '*.servers.usage'

## network routing key
RoutKeyNetwork = '.servers.interfaces.'
RoutKeyNetworkLinkState = '*.servers.interfaces.linkstate'
RoutKeyNetworkUsage = '*.servers.interfaces.usage'
RoutKeyNetworkVlanUsage = '*.servers.interfaces.vlans.usage'

RoutKeyNetworkRpcFinder = re.compile('.servers.[\d\w-]+.interfaces.')
RoutKeyNetworkAddFinder = re.compile('.servers.[\d\w-]+.interfaces.add')
RoutKeyNetworkAddedFinder = re.compile('.servers.interfaces.added')
RoutKeyNetworkUpdateFinder = re.compile('.servers.[\d\w-]+.interfaces.update')

## disk routing key
RoutKeyDisk = '.servers.disks.'
RoutKeyDiskAdded = '.servers.disks.added'
RoutKeyDiskDel = '.servers.disks.removed'
RoutKeyDiskState = '.servers.disks.state'
RoutKeyDiskHaAction = '.servers.disks.haaction'
RoutKeyDiskUsage = '.servers.disks.usage'
RoutKeyDiskUpdated = '.servers.disks.updated'
RoutKeyDiskGetMode = '.servers.disks.rwmode'
RoutKeyDiskSetMode = '.servers.disks.rwmode.update'
RoutKeyDiskStartStop = '.servers.disks.control'
## rpc
RoutKeyDiskRpcFinder = re.compile('.servers.[\w\d-]+.disks')
RoutKeyDiskCheckMountFinder = re.compile('.servers.[\w\d-]+.disks.check_mount')
RoutKeyDiskWirteDiskIdFinder = re.compile('.servers.[\w\d-]+.disks.write_disk_id')

## disk pool routing key
RoutKeyDiskPool = 'servers.diskpools.'
RoutKeyDiskPoolAdd = 'servers.diskpools.added'
RoutKeyDiskPoolDel = 'servers.diskpools.removed'
RoutKeyDiskPoolUpdate = 'servers.diskpools.updated'

## service routing key
RoutKeyService = '.services.'
RoutKeyServiceRpcFinder = re.compile('.services.[\d\w-]+.')
RoutKeyServiceState = '.services.state'
RoutKeyServiceHaAction = '.services.haaction'
RoutKeyServiceUsage = '.services.usage'
RoutKeyServiceControlFinder = re.compile('.services.[\d\w-]+.control')
RoutKeyServiceOsdConfLoadFinder = re.compile('.services.[\d\w-]+.config.osd.load')
RoutKeyServiceOsdConfSaveFinder = re.compile('.services.[\d\w-]+.config.osd.save')
RoutKeyServiceGwConfLoadFinder = re.compile('.services.[\d\w-]+.config.gw.load')
RoutKeyServiceGwConfSaveFinder = re.compile('.services.[\d\w-]+.config.gw.save')

"""
EdgeRoutingKeyList = [ "*.servers.updated", "*.servers.removed", "*.servers.stat", "*.servers.usage",
                       "*.servers.interfaces.added", "*.servers.interfaces.updated", "*.servers.interfaces.removed",
                       "*.servers.interfaces.linkstate", "*.servers.interfaces.usage", "*.servers.interfaces.vlans.added",
                       "*.servers.interfaces.vlans.updated", "*.servers.interfaces.vlans.removed",
                       "*.servers.disks.added", "*.servers.disks.updated", "*.servers.disks.removed", "*.servers.disks.state",
                       "*.servers.disks.size", "*.servers.disks.rwmode", "*.servers.diskpools.added", "*.servers.diskpools.updated",
                       "*.servers.diskpools.removed",
                       "*.services.state", "*.services.stat", "*.services.haaction", "*.services.usage"]
"""


EdgeRoutingKeyList = [ "*.servers.updated", "*.servers.removed", "*.servers.added",
                       "*.servers.interfaces.added", "*.servers.interfaces.updated", "*.servers.interfaces.removed",
                       "*.servers.interfaces.vlans.added",
                       "*.servers.interfaces.vlans.updated", "*.servers.interfaces.vlans.removed",
                       "*.servers.disks.added", "*.servers.disks.updated", "*.servers.disks.removed",
                        "*.servers.disks.rwmode", "*.servers.diskpools.added", "*.servers.diskpools.updated",
                       "*.servers.diskpools.removed"]

MonRoutingKeyList = ["*.servers.updated", "*.servers.removed", "*.servers.interfaces.added", "*.servers.added",
                     "*.servers.interfaces.updated", "*.servers.interfaces.removed", "*.servers.interfaces.vlans.added"
                    , "*.servers.interfaces.vlans.updated", "*.servers.interfaces.vlans.removed",
                     "*.servers.disks.added", "*.servers.disks.updated", "*.servers.disks.removed",
                     "*.servers.disks.rwmode", "*.servers.diskpools.added", "*.servers.diskpools.updated",
                     "*.servers.diskpools.removed", "*.services.added", "*.services.updated"]

## Exchange Name
ExchangeName = 'ksan.system'

##Queue Name
MonservicedServers = 'monserviced.servers'
MonservicedServices = 'monserviced.services'
MonservicedDisks = 'monserviced.disks'
MonservicedNetwork = 'monserviced.networks'

"""
######### disk define  ###########
"""
DiskTypeMds = 'Mds'
DiskTypeOsd = 'Osd'
DiskStatOnline = 'Online'
DiskStatOffline = 'Offline'
DiskModeRw = 'ReadWrite'
DiskModeRd = 'ReadOnly'
DiskHaActionInit = 'Initializing'

class MgsConf:
    def __init__(self, MgsIp, IfsPortalPort, MqPortal):
        self.MgsIp = MgsIp
        self.IfsPortalPort = IfsPortalPort
        self.MqPortal = MqPortal

class MonservicdConf:
    def __init__(self, dic):
        pass


#####  Conversion Dict to Object Class #####
@catch_exceptions()
class DictToObject(object):

    def __init__(self, myDict):
        for key, value in myDict.items():
            if type(value) == dict:
                setattr(self, key, DictToObject(value))
            else:
                if isinstance(value, str) and value.isdigit():
                    value = int(value)
                setattr(self, key, value)


@catch_exceptions()
class GetApiResult:
    def __init__(self, Header, ItemHeader, Items):
        self.Header = Header
        self.ItemHeader = ItemHeader
        self.Items = Items


class ResponseHeader(object):
    """
    Parsing Response without "Data" value is single value like True/False
    """
    def __init__(self):
        self.IsNeedLogin = None
        self.AccessDenied = None
        self.Result = None
        self.Code = None
        self.Message = None

    def Set(self, IsNeedLogin, AccessDenied, Result, Code, Message):
        self.IsNeedLogin = IsNeedLogin
        self.AccessDenied = AccessDenied
        self.Result = Result
        self.Code = Code
        self.Message = Message

class ResponseHeaderWithData(object):
    """
    Parsing Response with "Data" value is multi value like dict or list
    """
    def __init__(self):
        self.IsNeedLogin = None
        self.AccessDenied = None
        self.Result = None
        self.Code = None
        self.Message = None
        self.Data = None

    def Set(self, IsNeedLogin, AccessDenied, Result, Code, Message, Data):
        self.IsNeedLogin = IsNeedLogin
        self.AccessDenied = AccessDenied
        self.Result = Result
        self.Code = Code
        self.Message = Message
        self.Data = Data


class ResponseItemsHeader(object):
    def __init__(self):
        self.TotalCount = ''
        self.Skips = ''
        self.PageNo = ''
        self.CountPerPage = ''
        self.PagePerSection = ''
        self.TotalPage = ''
        self.StartPageNo = ''
        self.EndPageNo = ''
        self.PageNos = ''
        self.HavePreviousPage = ''
        self.HaveNextPage = ''
        self.HavePreviousPageSection = ''
        self.HaveNextPageSection = ''
        self.Items = ''

class DiskItemsDetail:
    def __init__(self):
        self.Id = ''
        self.ServerId = ''
        self.DiskPoolId = ''
        self.DiskNo = ''
        self.Path = ''
        self.State = 'Good'
        self.TotalInode = 0
        self.ReservedInode = 0
        self.UsedInode = 0
        self.TotalSize = 0
        self.ReservedSize = 0
        self.UsedSize = 0
        self.RwMode = None

    def Set(self, Id, ServerId, DiskPoolId, DiskNo, Path, State, TotalInode,
            ReservedInode,
            UsedInode, TotalSize, ReservedSize, UsedSize, RwMode):
        self.Id = Id
        self.ServerId = ServerId
        self.DiskPoolId = DiskPoolId
        self.DiskNo = DiskNo
        self.Path = Path
        self.State = State
        self.TotalInode = TotalInode
        self.ReservedInode = ReservedInode
        self.UsedInode = UsedInode
        self.TotalSize = TotalSize
        self.ReservedSize = ReservedSize
        self.UsedSize = UsedSize
        self.RwMode = RwMode

class AllDiskItemsDetail:
    def __init__(self):
        self.Server = None
        self.Id = ''
        self.ServerId = ''
        self.DiskPoolId = ''
        self.DiskNo = ''
        self.Path = ''
        self.State = None
        self.TotalInode = 0
        self.ReservedInode = 0
        self.UsedInode = 0
        self.TotalSize = 0
        self.ReservedSize = 0
        self.UsedSize = 0
        self.RwMode = None

    def Set(self, Server, Id, ServerId, DiskPoolId, DiskNo, Path, State, TotalInode,
            ReservedInode,
            UsedInode, TotalSize, ReservedSize, UsedSize, RwMode):
        self.Server = Server
        self.Id = Id
        self.ServerId = ServerId
        self.DiskPoolId = DiskPoolId
        self.DiskNo = DiskNo
        self.Path = Path
        self.State = State
        self.TotalInode = TotalInode
        self.ReservedInode = ReservedInode
        self.UsedInode = UsedInode
        self.TotalSize = TotalSize
        self.ReservedSize = ReservedSize
        self.UsedSize = UsedSize
        self.RwMode = RwMode



class DiskDetailMqBroadcast:
    def __init__(self):
        self.Id = ''
        self.ServerId = ''
        self.State = None
        self.TotalInode = 0
        self.ReservedInode = 0
        self.UsedInode = 0
        self.TotalSize = 0
        self.ReservedSize = 0
        self.UsedSize = 0
        self.Read = 0
        self.Write = 0

    def Set(self, Id, ServerId, State, TotalInode, ReservedInode,
            UsedInode, TotalSize, ReservedSize, UsedSize, Read, Write):
        self.Id = Id
        self.ServerId = ServerId
        self.State = State
        self.TotalInode = TotalInode
        self.ReservedInode = ReservedInode
        self.UsedInode = UsedInode
        self.TotalSize = TotalSize
        self.ReservedSize = ReservedSize
        self.UsedSize = UsedSize
        self.Read = Read
        self.Write = Write


class RequestDiskPool:
    def __init__(self):
        self.Name = None
        self.Description = None
        self.DiskIds = []
        self.DiskPoolType = 'STANDARD'
        self.ReplicationType = 'OnePlusZero'

    def Set(self, Name, Descrition, DiskIds, DiskPoolType=None, ReplicationType=None):
        self.Name = Name
        self.Description = Descrition
        self.DiskIds = DiskIds
        self.DiskPoolType = DiskPoolType
        self.ReplicationType = ReplicationType

class DiskPoolDetail:
    def __init__(self):
        self.Volumes = None
        self.Disks = None
        self.Id = None
        self.Name = None
        self.Description = None
        self.ModDate = None
        self.ModId = None
        self.modName = None
        self.RegDate = None
        self.RegId = None
        self.RegName = None


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

class NetworkInterfaceItemsDetail:
    def __init__(self):
        self.Server = ''
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
        self.ModDate = ''
        self.ModId = ''
        self.ModName = ''



@catch_exceptions()
class ResPonseHeader(object):
    def __init__(self, dic, logger=None):
        try:
            self.IsNeedLogin = dic['IsNeedLogin']
            self.AccessDenied = dic['AccessDenied']
            self.Result = dic['Result']
            self.Code = dic['Code']
            self.Message = dic['Message']
            if 'Data' in dic:
                self.Data = dic['Data']
        except KeyError as err:
            if logger is not None:
                logger.error(err, sys.exc_info()[2].tb_lineno)
            else:
                print(err, sys.exc_info()[0].tb_lineno)


#####  Response Body Decoder Class #####
class ResPonseHeaderDecoder(json.JSONDecoder):
    def __init__(self, logger=None, *args, **kwargs):
        # json.JSONDecoder.__init__(self, object_hook=self.object_hook, *args, **kwargs)
        self.logger=logger
        json.JSONDecoder.__init__(self, object_hook=self.object_hook, *args, **kwargs)

    def object_hook(self, dct):
        try:
            print(">>>>", dct)
            if 'IsNeedLogin' in dct:
                obj = ResPonseHeader(dct)
                if 'Data' in dct:
                    print(dct['Data'])
                return obj
        except KeyError as err:
            if self.logger is not None:
                self.logger.error(err, sys.exc_info()[2].tb_lineno)
            else:
                print(err, sys.exc_info()[0].tb_lineno)


#####  Items Header of Response Body Class #####
class ResPonseItemsHeader(object):
    def __init__(self, dic, logger=None):
        try:
            self.TotalCount = dic['TotalCount']
            self.Skips = dic['Skips']
            self.PageNo = dic['PageNo']
            self.CountPerPage = dic['CountPerPage']
            self.PagePerSection = dic['PagePerSection']
            self.TotalPage = dic['TotalPage']
            self.StartPageNo = dic['StartPageNo']
            self.EndPageNo = dic['EndPageNo']
            self.PageNos = dic['PageNos']
            self.HavePreviousPage = dic['HavePreviousPage']
            self.HaveNextPage = dic['HaveNextPage']
            self.HavePreviousPageSection = dic['HavePreviousPageSection']
            self.HaveNextPageSection = dic['HaveNextPageSection']
            self.Items = dic['Items']
        except KeyError as err:
            if logger is not None:
                logger.error(err, sys.exc_info()[2].tb_lineno)
            else:
                print(err, sys.exc_info()[0].tb_lineno)


"""
######### disk define  ###########
"""
ServerStateOnline = 'Online'
ServerStateOffline = 'Offline'
# @@@@@@@@@@ Server Class @@@@@@@@@@@@@@
#####  Server info of Response Body Class #####


class ServerItems(object):
    def __init__(self, Description='', State=ServerStateOffline):
        self.Id = ''
        self.Name = ''
        self.Description = Description
        self.CpuModel = ''
        self.Clock = 0
        self.State = State
        self.Rack = ''
        self.CpuUsage = ''
        self.LoadAverage1M = ''
        self.LoadAverage5M = ''
        self.LoadAverage15M = ''
        self.MemoryTotal = ''
        self.MemoryUsed = ''
        self.ModDate = ''
        self.ModId = ''
        self.ModName = ''

    @catch_exceptions()
    def Set(self, dic):
        self.Id = dic["Id"]
        self.Name = dic["Name"]
        self.Description = dic["Description"]
        self.CpuModel = dic["CpuModel"]
        self.Clock = dic["Clock"]
        self.State = dic["State"]
        self.Rack = dic["Rack"]
        self.CpuUsage = dic["CpuUsage"]
        self.LoadAverage1M = dic['LoadAverage1M']
        self.LoadAverage5M = dic['LoadAverage5M']
        self.LoadAverage15M = dic['LoadAverage15M']
        self.MemoryTotal = dic["MemoryTotal"]
        self.MemoryUsed = dic["MemoryUsed"]
        self.ModDate = dic["ModDate"]
        self.ModId = dic["ModId"]
        self.ModName = dic["ModName"]


class ServerItemsDetail(object):
    def __init__(self, Description='', State=ServerStateOffline):
        self.NetworkInterfaces = None
        self.Disks = None
        self.Services = None
        self.Id = None
        self.Name = ''
        self.Description = Description
        self.CpuModel = ''
        self.Clock = 0
        self.State = State
        self.Rack = ''
        self.CpuUsage = ''
        self.LoadAverage1M = ''
        self.LoadAverage5M = ''
        self.LoadAverage15M = ''
        self.MemoryTotal = ''
        self.MemoryUsed = ''
        self.ModDate = ''
        self.ModId = ''
        self.ModName = ''


class ServerUsageItems(object):
    """
    Used for Update Server Usage info
    """
    def __init__(self):
        self.Id = ''
        self.LoadAverage1M = 0
        self.LoadAverage5M = 0
        self.LoadAverage15M = 0
        self.MemoryUsed = 0

    def Set(self, Id, LoadAverage1M, LoadAverage5M, LoadAverage15M, MemoryUsed):
        self.Id = Id
        self.LoadAverage1M = LoadAverage1M
        self.LoadAverage5M = LoadAverage5M
        self.LoadAverage15M = LoadAverage15M
        self.MemoryUsed = MemoryUsed


class ServerStateItems(object):
    """
    Used for Update Server State info
    """
    def __init__(self, State):
        self.Id = ''
        self.State = State


# ####  Serverinfo of Request Body Class #####
class RequestServerInfo(object):
    def __init__(self, Description, logger=None):
        try:
            self.Name = socket.gethostname()
            self.Description = Description
            self.CpuModel = platform.processor()
            self.Clock = 0
            self.State = 'Online'
            self.Rack = 0
            self.MemoryTotal = psutil.virtual_memory().total
            self.ValidationErrorCode = ''
            self.ValidationErrorMessage = ''
        except Exception as err:
            if logger is not None:
                logger.error(err, sys.exc_info()[2].tb_lineno)
            else:
                print(err, sys.exc_info()[0].tb_lineno)


class RequestServerInitInfo(object):
    def __init__(self):
        self.ServerIp = ''
        self.MgsIp = ''
        self.MQPort = 0
        self.MgsPort = 0
        self.UserName = ''
        self.Password = ''

    def Set(self, ServerIp, MgsIp, MQPort, MgsPort, UserName, Password):
        self.ServerIp = ServerIp
        self.MgsIp = MgsIp
        self.MQPort = MQPort
        self.MgsPort = MgsPort
        self.UserName = UserName
        self.Password = Password


class RequestServerExistCheck(object):
    def __init__(self, Name, logger=None):
        try:
            self.Name = Name
            self.ValidationErrorCode = ''
            self.ValidationErrorMessage = ''
        except Exception as err:
            if logger is not None:
                logger.error(err, sys.exc_info()[2].tb_lineno)
            else:
                print(err, sys.exc_info()[0].tb_lineno)


class UpdateServerInfoItems:
    def __init__(self):
        self.Name = None
        self.Description = None
        self.CpuModel = None
        self.Clock = None
        self.State = None
        self.Rack = None
        self.MemoryTotal = None

    def Set(self, Name, Descrition, CpuModel, Clock, State, Rack, MemoryTotal):
        self.Name = Name
        self.Description = Descrition
        self.CpuModel = CpuModel
        self.Clock = Clock
        self.State = State
        self.Rack = Rack
        self.MemoryTotal = MemoryTotal


# @@@@@@@@@@ Network Class @@@@@@@@@@@@@@
@catch_exceptions()
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


# ####  Network Interface info of Response Body Class #####
@catch_exceptions()
class ResponseNetworkInterfaceItems(object):
    def __init__(self, dic):
        self.Id = dic["Id"]
        self.ServerId = dic["ServerId"]
        self.Name = dic["Name"]
        self.Description = dic["Description"]
        self.Dhcp = dic["Dhcp"]
        self.MacAddress = dic["MacAddress"]
        self.LinkState = dic["LinkState"]
        self.IpAddress = dic["IpAddress"]
        self.SubnetMask = dic["SubnetMask"]
        self.Gateway = dic["Gateway"]
        self.Dns1 = dic["Dns1"]
        self.Dns2 = dic["Dns2"]
        self.ModDate = dic["ModDate"]
        self.ModId = dic["ModId"]
        self.ModName = dic["ModName"]


@catch_exceptions()
class NetworkInterfaceItemsDetail:
    def __init__(self):
        self.NetworkInterfaceVlans = None
        self.Id = None
        self.ServerId = None
        self.Name = None
        self.Description = None
        self.Dhcp = None
        self.MacAddress = None
        self.LinkState = None
        self.IpAddress = None
        self.SubnetMask = None
        self.Gateway = None
        self.Dns1 = None
        self.Dns2 = None
        self.ModDate = None
        self.ModId = None
        self.ModName = None


@catch_exceptions()
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


@catch_exceptions()
class RequestNetworkInterfaceCheck(object):
    """
    request to register network info to ifsportalsvr
    """
    def __init__(self, NicName, logger=None):
        self.Name = NicName


@catch_exceptions()
class NetworkInterfaceLinkStateItems(object):
    """
    request to update network interface status to ifsportalsvr
    """

    def __init__(self, InterfaceId, ServerId, LinkState):
        self.ServerId = ServerId
        self.InterfaceId = InterfaceId
        self.LinkState = LinkState


@catch_exceptions()
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

@catch_exceptions()
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


@catch_exceptions()
class RequestVlanNetworkInterfaceCheck(object):
    """
    request to register network info to ifsportalsvr
    """
    def __init__(self, Tag, logger=None):
        self.Tag = int(Tag)

"""
########## disk request ###################
"""
class AddDiskObject:
    def __init__(self):
        self.DiskPoolId = ''
        self.Name = ''
        self.Path = ''
        self.State = 'Stop'
        self.TotalInode = 0
        self.ReservedInode = 0
        self.UsedInode = 0
        self.TotalSize = 0
        self.ReservedSize = 0
        self.UsedSize = 0
        self.Rwmode = 'ReadOnly'

    def Set(self, Name, Path, State, TotalInode, ReservedInode, UsedInode, TotalSize, ReservedSize, UsedSize, RwMode, DiskPoolId=''):

        self.DiskPoolId = DiskPoolId
        self.Name = Name
        self.Path = Path
        self.State = State
        self.TotalInode = TotalInode
        self.ReservedInode = ReservedInode
        self.UsedInode = UsedInode
        self.TotalSize = TotalSize
        self.ReservedSize = ReservedSize
        self.UsedSize = UsedSize
        self.Rwmode = RwMode

class UpdateDiskSizeObject:
    def __init__(self):
        self.Id = None
        self.ServerId = None
        self.DiskNo = None
        self.TotalInode = 0
        self.ReservedInode = 0
        self.UsedInode = 0
        self.TotalSize = 0
        self.ReservedSize = 0
        self.UsedSize = 0

    def Set(self, Id, ServerId, DiskNo, TotalInode, ReservedInode, UsedInode, TotalSize, ReservedSize, UsedSize):
        self.Id = Id
        self.ServerId = ServerId
        self.DiskNo = DiskNo
        self.TotalInode = TotalInode
        self.ReservedInode = ReservedInode
        self.UsedInode = UsedInode
        self.TotalSize = TotalSize
        self.ReservedSize = ReservedSize
        self.UsedSize = UsedSize


class DiskItems:
    def __init__(self, dic, HaAction=DiskHaActionInit, State=DiskStatOffline, Mode=DiskModeRw):
        self.Id = ''
        self.ServerId = ''
        self.DiskNo = ''
        self.Path = ''
        self.HaAction = HaAction
        self.State = State
        self.DiskType = ''
        self.TotalSize = 0
        self.ReservedSize = 0
        self.UsedSize = 0
        self.RwMode = Mode

    def Set(self, dic):
        self.Id = dic['Id']
        self.ServerId = dic['ServerId']
        self.DiskNo = dic['DiskNo']
        self.Path = dic['Path']
        self.HaAction = dic['HaAction']
        self.State = dic['State']
        self.DiskType = dic['DiskType']
        self.TotalSize = dic['TotalSize']
        self.ReservedSize = dic['ReservedSize']
        self.UsedSize = dic['UsedSize']
        self.RwMode = dic['RwMode']


class DiskPoolItems:
    def __init__(self):
        self.Id = None
        self.Name = None
        self.Description = None
        self.ModDate = None
        self.ModId = None
        self.ModName = None
        self.RegDate = None
        self.RegId = None
        self.RegName = None

    def Set(self, Id, Name, Description, ModDate, ModId, ModName, RegDate, RegId, RegName):
        self.Id = Id
        self.Name = Name
        self.Description = Description
        self.ModDate = ModDate
        self.ModId = ModId
        self.ModName = ModName
        self.RegDate = RegDate
        self.RegId = RegId
        self.RegName = RegName


class DiskInfoPerServer:
    def __init__(self, ServerId, Name):
        self.ServerId = ServerId
        self.Name = Name
        self.DiskList = list()
        self.Res = ResOk


@catch_exceptions()
class DiskIdServerId:
    def __init__(self):
        self.ServerId = ''
        self.DiskId = ''


class MonservicedConf:
    def __init__(self):
        self.MgsIp = ''
        self.IfsPortalPort = ''
        self.MqPort = ''
        self.ServerId = ''
        self.ManagementNetDev = ''
        self.DefaultNetworkId = ''
        self.SshUser = ''
        self.SshPassword = ''

    def Set(self, Ip, PortalPort, MqPort, ServerId, ManagementNetDev, DefaultNetworkId, SshUser, SshPassword):
        self.MgsIp = Ip
        self.IfsPortalPort = PortalPort
        self.MqPort = MqPort
        self.ServerId = ServerId
        self.ManagementNetDev = ManagementNetDev
        self.DefaultNetworkId = DefaultNetworkId
        self.SshUser = ''
        self.SshPassword = ''


class DiskSizeItems:
    def __init__(self):
        self.Id = ''
        self.ServerId = ''
        self.DiskPoolId = ''
        self.DiskNo = 0
        self.TotalInode = 0
        self.ReservedInode = 0
        self.UsedInode = 0
        self.TotalSize = 0
        self.ReservedSize = 0
        self.UsedSize = 0

    def Set(self, Id, ServerId, DiskPoolId, DiskNo, TotalInode, UsedInode, ReservedInode, TotalSize, UsedSize, ReservedSize):
        self.Id = Id
        self.ServerId = ServerId
        self.DiskPoolId = DiskPoolId
        self.DiskNo = DiskNo
        self.TotalInode = TotalInode
        self.ReservedInode = ReservedInode
        self.UsedInode = UsedInode
        self.TotalSize = TotalSize
        self.ReservedSize = ReservedSize
        self.UsedSize = UsedSize

class DiskUsage:
    def __init__(self):
        self.Id = ''
        self.ServerId = ''
        self.DiskNo = 0
        self.UsedSize = 0
        self.UsedInode = 0
        self.Read = 0
        self.Write = 0

    def Set(self, Id, ServerId, UsedSize, UsedInode, Read, Write):
        self.Id = Id
        self.ServerId = ServerId
        self.UsedSize = UsedSize
        self.UsedInode = UsedInode
        self.Read = Read
        self.Write = Write

######### SERVICE #########
TypeS3 = 'IfsS3'
TypeTomcat = 'tomcat'
TypeServiceOSD = 'OSD'
TypeServiceS3 = 'S3'
TypeServiceMONGODB = 'MongoDB'
TypeServiceMARIADB = 'MariaDB'
TypeServiceObjmanager = 'OBJMANAGER'
TypeServiceS3Backend = 'S3Backend'
TypeServiceMonitor = 'Monitor'
TypeServiceEdge = 'Edge'
TypeServiceHaproxy = 'HaProxy'
SampleS3ConfFile = './objmanager.conf'
S3ConfFile = '/opt/objmanager.conf'
SampleHaproxyConfFile = './haproxy.cfg'
HaproxyConfFile = '/opt/haproxy.cfg'
ServiceStart = 'Start'
ServiceStop = 'Stop'
ServiceRestart = 'Restart'


KsanMongDbManagerBinPath = '/usr/local/ksan/bin/ksanMongoDBManager'

######### VOLUME ##########
VolumeStateOnline = 'Online'
VolumeStateOffline = 'Offline'

DiskPoolReplica1 = 'OnePlusZero'
DiskPoolReplica2 = 'OnePlusOne'
DiskPoolReplica3 = 'OnePlusTwo'

DiskPoolClassStandard = 'STANDARD'
DiskPoolClassArchive = 'ARCHIVE'

class AddServiceInfoObject:
    def __init__(self):
        self.GroupId = None
        self.Name = None
        self.Description = None
        self.ServiceType = None
        self.HaAction = "Initializing"
        self.State = "Offline"
        self.VlanIds = None

    def Set(self, Name, ServiceType, GroupId, VlanIds, State='Offline', Description='', HaAction='Initializing'):
        self.GroupId = GroupId
        self.Name = Name
        self.Description = Description
        self.ServiceType = ServiceType
        self.HaAction = HaAction
        self.State = State
        self.VlanIds = VlanIds


class UpdateServiceInfoObject:
    def __init__(self):
        self.GroupId = None
        self.Name = None
        self.Description = None
        self.ServiceType = None
        self.HaAction = "Initializing"
        self.State = "Offline"
        self.VlanIds = None

    def Set(self, Name, ServiceType, GroupId, VlanIds, State, Description, HaAction):
        self.GroupId = GroupId
        self.Name = Name
        self.Description = Description
        self.ServiceType = ServiceType
        self.HaAction = HaAction
        self.State = State
        self.VlanIds = VlanIds


class AddServiceGroupItems:
    def __init__(self):
        self.Name = None
        self.Description = None
        self.ServiceType = None
        self.ServiceIpAddress = None
        self.ServiceIds = None

    def Set(self, Name, ServiceType, ServiceIpAddress=None, ServiceIds=None):
        self.Name = Name
        self.ServiceType = ServiceType
        self.ServiceIpAddress = ServiceIpAddress
        self.ServiceIds = ServiceIds


class ServiceGroupItems:
    def __init__(self):
        self.Id = None
        self.Name = None
        self.Description = None
        self.ServiceType = None
        self.ServiceIpAddress = None
        self.ModDate = None
        self.ModId = None
        self.ModName = None
        self.RegDate = None
        self.RegId = None
        self.RegName = None

    def Set(self, Id, Name, Description, ServiceType, ServiceIpAddress, ModDate, ModId, ModName, RegDate, RegId, RegName):
        self.Id = Id
        self.Name = Name
        self.Description = Description
        self.ServiceType = ServiceType
        self.ServiceIpAddress = ServiceIpAddress
        self.ModDate = ModDate
        self.ModId = ModId
        self.ModName = ModName
        self.RegDate = RegDate
        self.RegId = RegId
        self.RegName = RegName


class ServiceGroupDetail:
    def __init__(self):
        self.Services = None
        self.Id = None
        self.Name = None
        self.Description = None
        self.ServiceType = None
        self.ServiceIpAddress = None
        self.ModDate = None
        self.ModId = None
        self.ModName = None
        self.RegDate = None
        self.RegId = None
        self.RegName = None


class ServiceItems:
    def __init__(self):
        self.Id = None
        self.GroupId = None
        self.Name = None
        self.Description = None
        self.ServiceType = None
        self.HaAction = None
        self.State = None
        self.CpuUsage = None
        self.MemoryTotal = None
        self.MemoryUsed = None
        self.ThreadCount = None
        self.ModDate = None
        self.ModId = None
        self.ModName = None
        self.RegDate = None
        self.RegId = None
        self.RegName = None

    def Set(self, Id, GroupId, Name, Description, ServiceType, HaAction, State, CpuUsage, MemoryTotal, MemoryUsed,
            ThreadCount, ModeDate, ModId, ModeName, RegDate, RegId, RegName):
        self.Id = Id
        self.GroupId = GroupId
        self.Name = Name
        self.Description = Description
        self.ServiceType = ServiceType
        self.HaAction = HaAction
        self.State = State
        self.CpuUsage = CpuUsage
        self.MemoryTotal = MemoryTotal
        self.MemoryUsed = MemoryUsed
        self.ThreadCount = ThreadCount
        self.ModDate = ModeDate
        self.ModId = ModId
        self.ModName = ModeName
        self.RegDate = RegDate
        self.RegId = RegId
        self.RegName = RegName


class ServiceItemsDetail:
    def __init__(self):
        self.ServiceGroup = None
        self.Id = None
        self.GroupId = None
        self.Name = None
        self.Description = None
        self.ServiceType = None
        self.HaAction = None
        self.State = None
        self.CpuUsage = None
        self.MemoryTotal = None
        self.MemoryUsed = None
        self.ThreadCount = None
        self.ModDate = None
        self.ModId = None
        self.ModName = None
        self.RegDate = None
        self.RegId = None
        self.RegName = None


class ServiceDetail:
    def __init__(self):
        self.Vlans = None
        self.ServiceGroup = None
        self.Id = None
        self.GroupId = None
        self.Name = None
        self.Description = None
        self.ServiceType = None
        self.HaAction = None
        self.State = None
        self.CpuUsage = None
        self.MemoryTotal = None
        self.MemoryUsed = None
        self.ThreadCount = None
        self.ModDate = None
        self.ModId = None
        self.ModName = None
        self.RegDate = None
        self.RegId = None
        self.RegName = None


class UpdateServiceUsageObject:
    def __init__(self):
        self.Id = None
        self.CpuUsage = None
        self.MemoryUsed = None
        self.ThreadCount = None

    def Set(self, Id, CpuUsage, MemoryUsed, ThreadCount):
        self.Id = Id
        self.CpuUsage = CpuUsage
        self.MemoryUsed = MemoryUsed
        self.ThreadCount = ThreadCount


class UpdateServicesStateObject:
    def __init__(self):
        self.Id = None
        self.State = None

    def Set(self, Id, State):
        self.Id = Id
        self.State = State



class ServiceControl:
    def __init__(self):
        self.Id = None
        self.Name = None
        self.IpAddresses = None
        self.Control = None

    def Set(self, Id, Name, IpAddresses, Control):
        self.Id = Id
        self.Name = Name
        self.IpAddresses = IpAddresses
        self.Control = Control

class ServiceConfigItems:
    def __init__(self):
        self.Type = ''
        self.Version = 0
        self.Config = ''
        self.RegDate = ''

    def Set(self, Type, Version, Config, RegDate):
        self.Type = Type
        self.Version = Version
        self.Config = Config
        self.RegDate = RegDate

class ServiceMongoDBConfig:
    def __init__(self, PrimaryHostName):
        self.Shard1Port = 20001
        self.Shard2Port = 20002
        self.ConfigServerPort = 50000
        self.MongoDbPort = 27017
        self.HomeDir = '/var/lib/mongo'
        self.PrimaryHost = PrimaryHostName

    def Set(self, Shard1Port, Shard2Port, ConfigServerPort, MongoDbPort, HomeDir, PrimaryHostName):
        self.Shard1Port = Shard1Port
        self.Shard2Port = Shard2Port
        self.ConfigServerPort = ConfigServerPort
        self.MongoDbPort = MongoDbPort
        self.HomeDir = HomeDir
        self.PrimaryHostName = PrimaryHostName



######## HAPROXY CONFIG DEFINE ########
class HaproxyConf:
    def __init__(self):
        self.ConfigGlobal = None
        self.ConfigDefault = None
        self.ConfigListens = None

    def Set(self, ConfigGlobal, ConfigDefault, ConfigListens):
        self.ConfigGlobal = ConfigGlobal
        self.ConfigDefault = ConfigDefault
        self.ConfigListens = ConfigListens


class HaproxyConfigGlobal:
    def __init__(self):
        self.LogIpAddress = None
        self.LogHost = None
        self.Chroot = None
        self.PidFile = None
        self.MaxConn = None
        self.Daemon = None
        self.NbProc = 0
        self.NbThread = 0

    def Set(self, LogIpAddress, LogHost, Chroot, PidFile, MaxConn, Daemon, NbProc, NbThread):
        self.LogIpAddress = LogIpAddress
        self.LogHost = LogHost
        self.Chroot = Chroot
        self.PidFile = PidFile
        self.MaxConn = MaxConn
        self.Daemon = Daemon
        self.NbProc = NbProc
        self.NbThread = NbThread


class HaproxyConfigDefault:
    def __init__(self):
        self.TimeoutConnect = None
        self.TimeoutClient = None
        self.TimeoutServer = None

    def Set(self, TimeoutConnect, TimeoutClient, TimeoutServer):
        self.TimeoutConnect = TimeoutConnect
        self.TimeoutClient = TimeoutClient
        self.TimeoutServer = TimeoutServer


class HaproxyConfigListens:
    def __init__(self):
        self.Name = None
        self.BindPort = 0
        self.Mode = 0
        self.Balance = 0
        self.Servers = None

    def Set(self, Name, BindPort, Mode, Balance, Servers):
        self.Name = Name
        self.BindPort = BindPort
        self.Mode = Mode
        self.Balance = Balance
        self.Servers = Servers


class HaproxyListenServer:
    def __init__(self):
        self.Host = None
        self.IpAddress = None
        self.Port = None
        self.Param = None

    def Set(self, Host, IpAddress, Port, Param):
        self.Host = Host
        self.IpAddress = IpAddress
        self.Port = Port
        self.Param = Param

class HaProxyConfig:
    def __init__(self):
        self.Config = None

    def Set(self, Config):
        self.Config = Config

######### S3PROXY CONFIG ###########

class S3ProxyConfig:
    def __init__(self):
        self.Config = None

    def Set(self, Config):
        self.Config = Config


########## VOLUME ############
class AddVolumeItems:
    def __init__(self):
        self.DiskPoolId = None
        self.VolumeUserId = None
        self.Name = None
        self.Description = None
        self.VolumeUserDescription = None
        self.Password = None
        self.State = 'Offline'
        self.TotalSize = None
        self.ReplicationType = None
        self.EnavleDerService = False
        self.Permission = None
        self.OwnerId = 0
        self.GroupId = 0

    def Set(self, DiskPoolId, Name, Description, VolumeUserDescription, Password, State, QuotaSize, ReplicationType, EnableDrService,
                 Permission, OwnerId=0, GroupId=0, VolumeUserId=None):
        self.DiskPoolId = DiskPoolId
        self.VolumeUserId = VolumeUserId
        self.Name = Name
        self.Description = Description
        self.VolumeUserDescription = VolumeUserDescription
        self.Password = Password
        self.State = State
        self.TotalSize = QuotaSize
        self.ReplicationType = ReplicationType
        self.EnableDrService = EnableDrService
        self.Permission = Permission
        self.OwnerId = OwnerId
        self.GroupId = GroupId


class VolumeItems:
    def __init__(self):
        self.Id = None
        self.DiskPoolId = None
        self.Name = None
        self.Description = None
        self.Password = None
        self.State = None
        self.TotalSize = None
        self.UsedSize = None
        self.UsageRate = None
        self.UsedInode = None
        self.ReplicationType = None
        self.EnavleDerService = None
        self.Permission = None
        self.OwnerId = None
        self.GroupId = None
        self.ModDate = None
        self.ModId = None
        self.ModName = None
        self.RegDate = None
        self.RegId = None
        self.RegName = None

    def Set(self, Id, DiskPoolId, Name, Description, Password, State, TotalSize, UsedSize, UsageRate,
            UsedInode, ReplicationType, EnableDrService, Permission, OwnerId=0, GroupId=0):
        self.Id = Id
        self.DiskPoolId = DiskPoolId
        self.Name = Name
        self.Description = Description
        self.Password = Password
        self.State = State
        self.TotalSize = TotalSize
        self.UsedSize = UsedSize
        self.UsageRate = UsageRate
        self.UsedInode = UsedInode
        self.ReplicationType = ReplicationType
        self.EnableDrService = EnableDrService
        self.Permission = Permission
        self.OwnerId = OwnerId
        self.GroupId = GroupId
        self.ModDate = ''
        self.ModId = ''
        self.ModName = ''
        self.RegDate = ''
        self.RegId = ''
        self.RegName = ''


class VolumeObject:
    def __init__(self):
        self.DiskPool = None
        self.Id = None
        self.DiskPoolId = None
        self.Name = None
        self.Description = None
        self.Password = None
        self.State = None
        self.TotalSize = None
        self.UsedSize = None
        self.UsageRate = None
        self.UsedInode = None
        self.ReplicationType = None
        self.EnavleDerService = None
        self.Permission = None
        self.OwnerId = None
        self.GroupId = None
        self.ModDate = None
        self.ModId = None
        self.ModName = None
        self.RegDate = None
        self.RegId = None
        self.RegName = None


class UpdateVolumeObject:
    def __init__(self):
        self.DiskPoolId = None
        self.Name = None
        self.Description = None
        self.Password = None
        self.State = None
        self.TotalSize = None
        self.ReplicationType = None
        self.EnableDrService = None
        self.OwnerId = None
        self.GroupId = None

    def Set(self, DiskPoolId, Name, Description, Password, State, QuotaSize, ReplicationType, EnableDrService, OwnerId, GroupId):
        self.DiskPoolId = DiskPoolId
        self.Name = Name
        self.Description = Description
        self.Password = Password
        self.State = State
        self.TotalSize = QuotaSize
        self.ReplicationType = ReplicationType
        self.EnableDrService = EnableDrService
        self.OwnerId = OwnerId
        self.GroupId = GroupId


class UpdateVolumeStateObject:
    def __init__(self):
        self.State = None

    def Set(self, State):
        self.State = State


class UpdateVolumeUsageObject:
    def __init__(self):
        self.UsedInode = None
        self.UsedSize = None

    def Set(self, UsedInode, UsedSize):
        self.UsedInode = UsedInode
        self.UsedSize = UsedSize


class UpdateVolumeUserObject:
    def __init__(self):
        self.UserId = None
        self.UserType = None
        self.Description = None
        self.TotalSize = 0

    def Set(self, UserId, UserType, Description, TotalSize):
        self.UserId = UserId
        self.UserType = UserType
        self.Description = Description
        self.TotalSize = TotalSize



######## USER #########

class AddUSerItems:
    def __init__(self):
        self.Protocol = None
        self.Host = None
        self.LoginId = None
        self.Email = None
        self.Name = None
        self.Password = None
        self.ConfirmPassword = None
        self.PhoneNumber = None
        self.IsAgreeMarketing = None

    def Set(self, LoginId, Name, Password, ConfirmPassword, Email='', Host='', Protocol='', PhoneNumber='', IsAgreeMarketing=True):
        self.Protocol = Protocol
        self.Host = Host
        self.LoginId = LoginId
        self.Email = Email
        self.Name = Name
        self.Password = Password
        self.ConfirmPassword = ConfirmPassword
        self.PhoneNumber = PhoneNumber
        self.IsAgreeMarketing = IsAgreeMarketing

class UserItems:

    def __init__(self):
        self.Id = None
        self.LoginId = None
        self.Email = None
        self.Name = None
        self.DisplayName = None
        self.PasswordChangeDate = None
        self.PhoneNumber = None
        self.ReceiveSms = True
        self.ReceiveEmail = True
        self.Roles = None
        self.LastLoginDateTime = None
        self.ProductType = None

    def Set(self,Id, LoginId, Email, Name, PhoneNumber=''):
        self.Id = Id
        self.LoginId = LoginId
        self.Email = Email
        self.Name = Name
        self.DisplayName = None
        self.PasswordChangeDate = None
        self.PhoneNumber = PhoneNumber
        self.ReceiveSms = True
        self.ReceiveEmail = True
        self.Roles = None
        self.LastLoginDateTime = None
        self.ProductType = None


class AddUserObject:
    def __init__(self):
        self.LoginId = None
        self.Email = None
        self.Name = None
        self.Code = None
        self.Roles = None
        self.Status = None

    def Set(self, LoginId, Email, Name, Code, Roles, Status):
        self.LoginId = LoginId
        self.Email = Email
        self.Name = Name
        self.Code = Code
        self.Roles = Roles
        self.Status = Status

class UserDiskPoolsInfo:
    def __init__(self):
        self.UserId = None
        self.DiskPoolId = None
        self.StorageClass = None

    def Set(self, UserId, DiskPoolId, StorageClass):
        self.UserId = UserId
        self.DiskPoolId = DiskPoolId
        self.StorageClass = StorageClass


class UserOfVolumeObject:
    def __init__(self):
        self.VolumeId = None
        self.UserId = None
        self.UserType = None
        self.Description = None
        self.User = None
        self.ModDate = None
        self.ModId = None
        self.ModName = None
        self.RegDate = None
        self.RegId = None
        self.RegName = None


######## VOLUME USER ############
class VolumeUserObject:
    def __init__(self):
        self.VolumeId = None
        self.UserId = None
        self.Description = None
        self.TotalSize = None
        self.UsedSize = None
        self.UsageRate = None
        self.User = None
        self.ModDate = None
        self.modId = None
        self.ModName = None
        self.RegDate = None
        self.RegId = None
        self.RegName = None


class UserObject:
    def __init__(self):
        self.Id = None
        self.LoginId = None
        self.Email = None
        self.Name = None
        self.DisplayName = None
        self.Code = None
        self.Status = None
        self.JoinDate = None
        self.WithdrawDate = None
        self.LoginCount = None

    def Set(self, Id, LoginId, Email, Name, DisplayName, Code, Status, JoinDate, WithdrawDate, LoginCount):
        self.Id = Id
        self.LoginId = LoginId
        self.Email = Email
        self.Name = Name
        self.DisplayName = DisplayName
        self.Code = Code
        self.Status = Status
        self.JoinDate = JoinDate
        self.WithdrawDate = WithdrawDate
        self.LoginCount = LoginCount


class UpdateUserObject:
    def __init__(self):
        self.Email = None
        self.Name = None
        self.Code = None
        self.Status = None
        self.Roles = None

    def Set(self, Email, Name, Code, Status, Roles):
        self.Email = Email
        self.Name = Name
        self.Code = Code
        self.Status = Status
        self.Roles = Roles

class UpdateUserRolesObject:
    def __init__(self):
        self.RoleName = None

    def Set(self, RoleName):
        self.RoleName = RoleName


class ChangeUserPasswordObject:
    def __init__(self):
        self.NewPassword = None
        self.NewConfirmPassword = None

    def Set(self, NewPassword, NewConfirmPassword):
        self.NewPassword = NewPassword
        self.NewConfirmPassword = NewConfirmPassword


class S3UserObject:
    def __init__(self):
        self.Id = None
        self.Name = None
        self.StandDiskPoolId = None
        self.Email = None
        self.AccessKey = None
        self.SecretKey = None

    def Set(self, Name, DiskPoolId, Email, Id=None, AccessKey=None, SecretKey=None):
        self.Id = Id
        self.Name = Name
        self.StandardDiskPoolId = DiskPoolId
        self.Email = Email
        self.AccessKey = AccessKey
        self.SecretKey = SecretKey

class S3UserUpdateObject:
    def __init__(self):
        self.Name = None
        self.Email = None

    def Set(self, Name, Email):
        self.Name = Name
        self.Email = Email

class S3UserInfoItems:
    def __init__(self):
        self.Id = None
        self.Name = None
        self.Email = None
        self.AccessKey = None
        self.SecretKey = None
        self.UserDiskPools = None

    def Set(self, Id, Name, DiskPoolId, Email, AccessKey, SecretKey):
        self.Id = Id
        self.Name = Name
        self.StandardDiskPoolId = DiskPoolId
        self.Email = Email
        self.AccessKey = AccessKey
        self.SecretKey = SecretKey

class S3UserStorageClassObject:
    def __init__(self):
        self.UserId = None
        self.DiskPoolId = None
        self.StorageClass = None

    def Set(self, UserId, DiskPoolId, StorageClass):
        self.UserId = UserId
        self.DiskPoolId = DiskPoolId
        self.StorageClass = StorageClass


class KsanMonConfig:
    def __init__(self):
        self.OisIp = '127.0.0.1'
        self.OisPort = 5443
        self.MqPort = 5672
        self.ManagementNetDev = None

class KcanOsdConfig:
    def __init__(self):
        self.PoolSize = 650
        self.Port = 8000
        self.osd.ec_schedule_minutes = 30000




#####  Header of Response Body Class #####
ResponseItemsHeaderModule = 'common.define.ResponseItemsHeader'

ResponseHeaderModule = 'common.define.ResponseHeader'
ResponseHeaderWithDataModule = 'common.define.ResponseHeaderWithData'


ServerItemsModule = 'common.define.ServerItems'
ServerItemsDetailModule = 'common.define.ServerItemsDetail'
ServerUsageItemsModule = 'common.define.ServerUsageItems'
ServerStateItemsModule = 'common.define.ServerStateItems'

DiskItemsDetailModule = 'common.define.DiskItemsDetail'
DiskDetailModule = 'common.define.DiskDetail'
AllDiskItemsDetailModule = 'common.define.AllDiskItemsDetail'
DiskPoolItemsModule = 'common.define.DiskPoolItems'
DiskPoolDetailModule = 'common.define.DiskPoolDetail'

NetworkInterfaceItemsModule = 'common.define.NetworkInterfaceItems'
VlanNetworkInterfaceItemsModule = 'common.define.VlanNetworkInterfaceItems'


MonservicdConfModule = 'common.define.MonservicedConf'
RequestNetworkInterfaceItemsModule = 'common.define.RequestNetworkInterfaceItems'

ServiceItemsDetailModule = 'common.define.ServiceItemsDetail'
ServiceDetailModule = 'common.define.ServiceDetail'
ServiceControlModule = 'common.define.ServiceControl'
ServiceGroupItemsModule = 'common.define.ServiceGroupItems'
ServiceGroupDetailModule = 'common.define.ServiceGroupDetail'
ServiceConfigModule = 'common.define.ServiceConfigItems'

VolumeItemsModule = 'common.define.VolumeItems'
VolumeObjectModule = 'common.define.VolumeObject'

UserItemsModule = 'common.define.UserItems'
VolumeUserObjectModule = 'common.define.VolumeUserObject'
UserObjectModule = 'common.define.UserObject'
S3UserObjectModule = 'common.define.S3UserObject'



Parsing = dict()

Parsing['Server'] = ServerItemsModule
Parsing['NetworkInterfaces'] = NetworkInterfaceItemsModule
Parsing['NetworkInterfaceVlans'] = VlanNetworkInterfaceItemsModule
Parsing['Vlans'] = VlanNetworkInterfaceItemsModule
Parsing['Disks'] = DiskItemsDetailModule
Parsing['DiskPool'] = DiskPoolItemsModule
Parsing['Volumes'] = VolumeItemsModule
Parsing['Services'] = ServiceItemsDetailModule
Parsing['ServiceGroup'] = ServiceGroupItemsModule
Parsing['User'] = UserObjectModule


