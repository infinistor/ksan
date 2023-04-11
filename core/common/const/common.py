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
if os.path.dirname(os.path.abspath(os.path.dirname(__file__))) not in sys.path:
    sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from pydantic import BaseModel
import re
import psutil
import time
import inspect
import jsonpickle
import json
import threading
from optparse import OptionParser

"""
##### UNIT ##### 
"""
### byte ###
OneTBUnit = 1024*1024*1024*1024
OneGBUnit = 1024*1024*1024
OneMBUnit = 1024*1024
OneKBUnit = 1024

"""
######### environment configuration define ########
"""



Updated = 1 # disk, network, service, server changed flag
Checked = 0


##### action argument and option #####
ActionAdd = 'add'
ActionRemove = 'remove'
ActionList = 'list'
ActionSet = 'set'
ActionStart = 'start'
ActionStop = 'stop'
ActionRestart = 'restart'
ActionAddDisk2Pool = 'add2disk'
ActionRemoveDisk2Pool = 'remove2disk'

OptServerName1 = '--ServerName'
OptServerName2 = '--servername'

OptServiceName1 = '--ServiceName'
OptServiceName2 = '--servicename'

OptDiskName1 = '--DiskName'
OptDiskName2 = '--diskname'

OptDiskPoolName1 = '--DiskPoolName'
OptDiskPoolName2 = '--diskpoolname'




"""
##### RABBITMQ #####
"""
### Queue Name ###
MonservicedServers = 'monserviced.servers'
MonservicedServices = 'monserviced.services'
MonservicedDisks = 'monserviced.disks'
MonservicedNetwork = 'monserviced.networks'

"""
##### DISK  #####
"""
DiskIdFileName = '/DiskId'
DiskObjDirectory = '/obj'
DiskTempDirectory = '/temp'
DiskTrashDirectory = '/trash'

"""
##### DISKPOOL #####
"""


### disk stat ###
DiskStatOnline = 'Online'
DiskStatOffline = 'Offline'
DiskModeRw = 'ReadWrite'
DiskModeRwShort = 'RW'
DiskModeRo = 'ReadOnly'
DiskModeRoShort = 'RO'
DiskModeMaintenance = 'Maintenance'
DiskModeMaintenanceShort = 'MA'
DiskHaActionInit = 'Initializing'
DiskStop = 'Stop'
DiskStart = 'Good'
DiskWeak = 'Weak'
DiskDisable = 'Disable'

### replica type ###
DiskPoolReplica1 = 'OnePlusZero'
DiskPoolReplica2 = 'OnePlusOne'
DiskPoolReplica3 = 'OnePlusTwo'
DiskPoolReplicaEC = 'ErasureCode'
ECValueParser = re.compile("ec\(([\d]+):([\d]+)\)")


### diskpool type ###
DiskPoolClassStandard = 'STANDARD'
DiskPoolClassArchive = 'ARCHIVE'
DiskPoolClassPerformance = 'PERFORMANCE'

ValidDiskPoolType = [DiskPoolClassStandard, DiskPoolClassArchive, DiskPoolClassPerformance]


"""
##### SERVER #####
"""
### server state ###
ServerStateOnline = 'Online'
ServerStateOffline = 'Offline'
ServerStateTimeout = 'Timeout'


"""
##### SERVICE #####
"""

### service name define ###
KsanApiPortalName = 'ksanApiPortal'
KsanPortal = 'ksanPortal'
KsanPortalBridgeName = 'ksanPortalBridge'
KsanAgentName = 'ksanAgent'
KsanGWName = 'ksanGW'
KsanOSDName = 'ksanOSD'
KsanObjManagerName = 'ksanObjManager'
KsanObjManager = 'ksanObjManager'
KsanLifecycleManagerName = 'ksanLifecycleManager'
KsanReplicationManagerName = 'ksanReplicationManager'
KsanLogManagerName = 'ksanLogManager'
KsanRecoveryName = 'KsanRecovery'

MongoDBName = 'MongoDB'
MariaDBName = 'MariaDB'
RabbitMQName = 'RabbitMQ'


### docker contailner name define ###
KsanOSDContainerName = 'ksan-osd'
KsanGWContainerName = 'ksan-gw'
KsanLogManagerContainerName = 'ksan-log-manager'
KsanLifecycleManagerContainerName = 'ksan-lifecycle-manager'
KsanReplicationMangerContainerName = 'ksan-replication-manager'
KsanApiPortalContainerName = 'ksan-api-portal'
KsanPortalContainerName = 'ksan-portal'
KsanPortalBridgeContainerName= 'ksan-portal-bridge'


### service binamry name ###
KsanOsdBinaryName = '%s.jar' % KsanOSDName
KsanGwBinaryName = '%s.jar' % KsanGWName
KsanRecoveryBinaryName = 'ksanRecovery'
KsanMonitorBinaryName = 'ksanMonitor'
KsanAgentBinaryName = 'ksanAgent'
KsanUtilName = 'ksan'
KsanServerRegister = 'ksanServerRegister'



### service type ###
TypeServiceOSD = KsanOSDName
TypeServiceGW = KsanGWName
TypeServiceObjManager = KsanObjManagerName
TypeServiceMongoDB = 'MongoDB'
TypeServiceMariaDB = 'MariaDB'
TypeServiceS3Backend = 'S3Backend'
TypeServiceMonitor = 'ksanMonitor'
TypeServiceAgent = KsanAgentName
TypeServiceRabbitMq = 'RabbitMQ'
TypeServiceFsck = 'ksanFsck'
TypeServiceGetAttr = 'ksanGetAttr'
TypeServiceCbalance = 'ksanCbalance'
TypeServiceLifecycle = KsanLifecycleManagerName
TypeServiceRecovery = 'ksanRecovery'
TypeServiceReplication = KsanReplicationManagerName
TypeServiceLogManager = KsanLogManagerName


### service state ###
ServiceStateOnline = 'Online'
ServiceStateOffline = 'Offline'
ServiceStateUnkown = 'Unknown'

### service pid file ###
KsanAgentPidFile = '/var/run/ksanAgent.pid'


###  service status ###
START = 'Start'
STOP = 'Stop'
RESTART = 'Restart'
ONLINE = 'Online'
OFFLINE = 'Offline'





### service id path define ###
KsanAgentServiceIdHiddenPath = '/usr/local/ksan/sbin/.%s.ServiceId' % KsanAgentName
KsanOSDServiceIdHiddenPath = '/usr/local/ksan/sbin/.%s.ServiceId' % KsanOSDName
KsanGWServiceIdHiddenPath = '/usr/local/ksan/sbin/.%s.ServiceId' % KsanGWName
KsanLifecycleServiceIdHiddenPath = '/usr/local/ksan/sbin/.%s.ServiceId' % KsanLifecycleManagerName
KsanRecoveryServiceIdHiddenPath = '/usr/local/ksan/sbin/.ksanRecovery.ServiceId'
KsanReplicationServiceIdHiddenPath = '/usr/local/ksan/sbin/.%s.ServiceId' % KsanReplicationManagerName
KsanLogManagerServiceIdHiddenPath = '/usr/local/ksan/sbin/.%s.ServiceId' % KsanLogManagerName

ServiceTypeServiceHiddenPathMap = dict()
ServiceTypeServiceHiddenPathMap[TypeServiceAgent] = KsanAgentServiceIdHiddenPath
ServiceTypeServiceHiddenPathMap[TypeServiceOSD] = KsanOSDServiceIdHiddenPath
ServiceTypeServiceHiddenPathMap[TypeServiceGW] = KsanGWServiceIdHiddenPath
ServiceTypeServiceHiddenPathMap[TypeServiceLifecycle] = KsanLifecycleServiceIdHiddenPath
ServiceTypeServiceHiddenPathMap[TypeServiceRecovery] = KsanRecoveryServiceIdHiddenPath
ServiceTypeServiceHiddenPathMap[TypeServiceReplication] = KsanReplicationServiceIdHiddenPath
ServiceTypeServiceHiddenPathMap[TypeServiceLogManager] = KsanLogManagerServiceIdHiddenPath


### service unit ###
SystemdKsanGWServiceName = '%s.service' % KsanGWName
SystemdKsanOSDServiceName = '%s.service' % KsanOSDName
SystemdKsanAgentServiceName = '%s.service' % KsanAgentName
SystemdKsanLifecycleServiceName = '%s.service' % KsanLifecycleManagerName
SystemdKsanRecoveryServiceName = '%s.service' % KsanRecoveryName
SystemdKsanReplicationServiceName = '%s.service' % KsanReplicationManagerName
SystemdKsanLogManagerServiceName = '%s.service' % KsanLogManagerName


ServiceTypeSystemdServiceMap = dict()
ServiceTypeSystemdServiceMap[TypeServiceGW] = SystemdKsanGWServiceName
ServiceTypeSystemdServiceMap[TypeServiceOSD] = SystemdKsanOSDServiceName
ServiceTypeSystemdServiceMap[TypeServiceLifecycle] = SystemdKsanLifecycleServiceName
ServiceTypeSystemdServiceMap[TypeServiceRecovery] = SystemdKsanRecoveryServiceName
ServiceTypeSystemdServiceMap[TypeServiceReplication] = SystemdKsanReplicationServiceName
ServiceTypeSystemdServiceMap[TypeServiceLogManager] = SystemdKsanLogManagerServiceName

### docker container servicename map ###
ServiceTypeDockerServiceContainerNameMap = dict()
ServiceTypeDockerServiceContainerNameMap[TypeServiceOSD] = KsanOSDContainerName
ServiceTypeDockerServiceContainerNameMap[TypeServiceGW] = KsanGWContainerName
ServiceTypeDockerServiceContainerNameMap[TypeServiceLifecycle] = KsanLifecycleManagerContainerName
ServiceTypeDockerServiceContainerNameMap[TypeServiceRecovery] = KsanRecoveryName
ServiceTypeDockerServiceContainerNameMap[TypeServiceReplication] = KsanReplicationMangerContainerName
ServiceTypeDockerServiceContainerNameMap[TypeServiceLogManager] = KsanLogManagerContainerName


### service type conversion ###
ServiceTypeConversion = dict()
ServiceTypeConversion[KsanOSDName.lower()] = TypeServiceOSD
ServiceTypeConversion[KsanGWName.lower()] = TypeServiceGW
#ServiceTypeConversion['mongodb'] = TypeServiceMongoDB
#ServiceTypeConversion['mariadb'] = TypeServiceMariaDB
#ServiceTypeConversion['ksanmonitor'] = TypeServiceMonitor
ServiceTypeConversion[KsanAgentName.lower()] = TypeServiceAgent
ServiceTypeConversion[KsanLifecycleManagerName.lower()] = TypeServiceLifecycle
#ServiceTypeConversion[KsanRecoveryName.lower()] = TypeServiceRecovery
ServiceTypeConversion[KsanReplicationManagerName.lower()] = TypeServiceReplication
ServiceTypeConversion[KsanLogManagerName.lower()] = TypeServiceLogManager
#ServiceTypeConversion['rabbitmq'] = TypeServiceRabbitMq
#ServiceTypeConversion['haproxy'] = TypeServiceHaproxy
ServiceTypeConversion[KsanObjManager.lower()] = TypeServiceObjManager





##### ksanAgent.conf Key Define #####
KeyCommonSection = 'mgs'
KeyPortalHost = 'PortalHost'
KeyPortalPort = 'PortalPort'
KeyPortalApiKey = 'PortalApiKey'
KeyMQHost = 'MQHost'
KeyMQPort = 'MQPort'
KeyMQUser = 'MQUser'
KeyMQPassword = 'MQPassword'
KeyServerId = 'ServerId'
KeyManagementNetDev = 'ManagementNetDev'
KeyDefaultNetworkId = 'DefaultNetworkId'

KeyMonitorSection = 'monitor'
KeyServerMonitorInterval = 'ServerMonitorInterval'
KeyNetworkMonitorInterval = 'NetworkMonitorInterval'
KeyDiskMonitorInterval = 'DiskMonitorInterval'
KeyServiceMonitorInterval = 'ServiceMonitorInterval'

##### ksanAgent.conf Interval Define #####
DiskMonitorInterval = 10
ServerMonitorInterval = 10
ServiceMonitorInterval = 10
NetworkMonitorInterval = 10
IntervalShort = 1
IntervalMiddle = 5
IntervalLong = 10



##### Utililty Variable Define #####

MoreDetailInfo = 'MoreDetail'
DetailInfo = 'Detail'
SimpleInfo = 'Simple'


##### Path Define #####
KsanEtcPath = '/usr/local/ksan/etc'
KsanBinPath = '/usr/local/ksan/bin'
KsanSbinPath = '/usr/local/ksan/sbin'
KsanSslPath = '/usr/local/ksan/ssl'
KsanUtilDirPath = '/usr/local/ksan/bin/util'

MonServicedConfPath = '/usr/local/ksan/etc/ksanAgent.conf'
DiskPoolXmlPath = '/usr/local/ksan/etc/diskpools.xml'
ServicePoolXmlPath = '/var/log/ksan/agent/servicepools_dump.xml'
OsdXmlFilePath = '/usr/local/ksan/etc/ksan-osd-log.xml'
GwXmlFilePath = '/usr/local/ksan/etc/ksan-gw-log.xml'
KsanMongDbManagerBinPath = '/usr/local/ksan/bin/ksanMongoDBManager'


KsanUtilPath = '%s/ksan' % KsanBinPath
KsanAgentPath = '%s/%s' % (KsanSbinPath, KsanAgentBinaryName)
KsanGwPath = '%s/%s' % (KsanSbinPath, KsanGwBinaryName)
KsanOsdPath = '%s/%s' % (KsanSbinPath, KsanOsdBinaryName)
KsanRecoveryPath = '%s/%s' % (KsanSbinPath, KsanRecoveryBinaryName)
KsanServerRegisterPath = '%s/%s' % (KsanUtilDirPath, KsanServerRegister)


### Service Action Retry Count ###
ServiceContolRetryCount = 3



#####  Header of Response Body Class for deserialization #####
#ResponseItemsHeaderModule = 'const.http.ResponseItemsHeader'
#ResponseHeaderModule = 'const.http.ResponseHeader'
#ResponseHeaderWithDataModule = 'const.http.ResponseHeaderWithData'

ServerItemsModule = 'const.server.ServerItems'
ServerItemsDetailModule = 'const.server.ServerItemsDetail'
ServerUsageItemsModule = 'const.server.ServerUsageItems'
ServerStateItemsModule = 'const.server.ServerStateItems'


ServiceItemsDetailModule = 'const.service.ServiceItemsDetail'
ServiceDetailModule = 'const.service.ServiceDetail'
ServiceControlModule = 'const.service.ServiceControl'
ServiceGroupItemsModule = 'const.service.ServiceGroupItems'
ServiceGroupDetailModule = 'const.service.ServiceGroupDetail'
ServiceConfigModule = 'const.service.ServiceConfigItems'

UserObjectModule = 'const.user.UserObject'
S3UserObjectModule = 'const.user.S3UserObject'


NetworkInterfaceItemsModule = 'const.network.NetworkInterfaceItems'
VlanNetworkInterfaceItemsModule = 'const.network.VlanNetworkInterfaceItems'


DiskItemsDetailModule = 'const.disk.DiskItemsDetail'
DiskDetailModule = 'const.disk.DiskDetail'
AllDiskItemsDetailModule = 'const.disk.AllDiskItemsDetail'
DiskPoolItemsModule = 'const.disk.DiskPoolItems'
DiskPoolDetailModule = 'const.disk.DiskPoolDetail'

OnePlusOne = 'OnePlusOne'
OnePlusZero = 'OnePlusZero'
OnePlusTwo = 'OnePlusTwo'
ErasureCode = 'ErasureCode'

Parsing = dict()

Parsing['Server'] = ServerItemsModule
Parsing['NetworkInterfaces'] = NetworkInterfaceItemsModule
Parsing['NetworkInterfaceVlans'] = VlanNetworkInterfaceItemsModule
Parsing['Vlans'] = VlanNetworkInterfaceItemsModule
Parsing['Disks'] = DiskItemsDetailModule
Parsing['DiskPool'] = DiskPoolItemsModule
Parsing['Services'] = ServiceItemsDetailModule
Parsing['ServiceGroup'] = ServiceGroupItemsModule
Parsing['User'] = UserObjectModule



'''
#### USER ####
'''
ValidStorageClassList = ['standard_ia', 'onezone_ia', 'intelligent_tiering', 'glacier', 'reduced_redundancy', 'deep_archive', 'outposts', 'glacier_ir']

class AgentConf(BaseModel):
    LocalIp: str
    PortalHost: str
    PortalPort: int
    MQHost: str
    MQPort: int
    MQPassword: str
    MQUser: str
    PortalApiKey: str




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


EdgeRoutingKeyList = [ "*.servers.updated", "*.servers.removed", "*.servers.added", "*.servers.disks.state",
                       "*.servers.interfaces.added", "*.servers.interfaces.updated", "*.servers.interfaces.removed",
                       "*.servers.interfaces.vlans.added",
                       "*.servers.interfaces.vlans.updated", "*.servers.interfaces.vlans.removed",
                       "*.servers.disks.added", "*.servers.disks.updated", "*.servers.disks.removed",
                        "*.servers.disks.rwmode", "*.servers.diskpools.added", "*.servers.diskpools.updated",
                       "*.servers.diskpools.removed", "*.services.added", "*.services.updated", "*.services.removed"]

MonRoutingKeyList = ["*.servers.updated", "*.servers.removed", "*.servers.interfaces.added", "*.servers.added",
                     "*.servers.interfaces.updated", "*.servers.interfaces.removed", "*.servers.interfaces.vlans.added"
                    , "*.servers.interfaces.vlans.updated", "*.servers.interfaces.vlans.removed",
                     "*.servers.disks.added", "*.servers.disks.updated", "*.servers.disks.removed",
                     "*.servers.disks.rwmode", "*.servers.diskpools.added", "*.servers.diskpools.updated",
                     "*.servers.diskpools.removed", "*.services.added", "*.services.updated"]

## Exchange Name
ExchangeName = 'ksan.system'


##### return code & messages define #####
RetKeyResult = 'Result'
RetKeyCode = 'Code'
RetKeyMessage = 'Message'




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
ResDiskAlreadyExists = 'Disk Id already exists. Initialize first'

ResEtcErrorCode = 1
ResEtcErrorMsg = 'Other Error '
ResFailToGetVlainId = 'Fail to get Vlan Id'

ResultFail = 1  # fail to get data

ResultSuccess = 'Success'
ResultWarning = 'Warning'
ResultError = 'Error'
CodeSuccess = 'EC000'
MessageNull = ''

CodeFailServerInit = 'EC001'
MessageFailServerInit = 'Fail to init Server'


CodeDuplicated = 'EC036'
ResutlNotFound = 'EC014'

ErrMsgServerNameMissing = 'ServerName is required'
ErrMsgDiskNameMissing = 'DiskName is required'
ErrMsgDiskpoolNameMissing = 'DiskpoolName is required'
ErrMsgUserNameMissing = 'UserName is required'
