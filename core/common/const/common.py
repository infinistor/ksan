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
from common.log import catch_exceptions
from pydantic import BaseModel

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

### Conf Key ###
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


##### CONFIG FILE TYPE #####
ConfigTypeINI = 'INI'
ConfigTypeObject = 'Object'

##### display option ######
MoreDetailInfo = 'MoreDetail'
DetailInfo = 'Detail'
SimpleInfo = 'Simple'

##### PATH #####
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

ProcDiskStatsPath = '/proc/diskstats'

##### ksanMonitor.conf #####
DiskMonitorInterval = 10
ServerMonitorInterval = 10
ServiceMonitorInterval = 10
NetworkMonitorInterval = 10
IntervalShort = 1
IntervalMiddle = 5
IntervalLong = 10
### service action retry count ###
ServiceContolRetryCount = 3


"""
##### HTTP #####
"""
### http header key ###
HeaderContentType = 'Content-Type'
HeaderAuth = 'Authorization'


### http method ###
GetMethod = 'GET'
PostMethod = 'POST'
PutMethod = 'PUT'
DeleteMethod = 'DELETE'

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



##### ENCODING #####
UTF8 = 'utf-8'

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

### disk stat ###
DiskStatOnline = 'Online'
DiskStatOffline = 'Offline'
DiskModeRw = 'ReadWrite'
DiskModeRwShort = 'RW'
DiskModeRo = 'ReadOnly'
DiskModeRoShort = 'RO'
DiskHaActionInit = 'Initializing'
DiskStop = 'Stop'
DiskStart = 'Good'
DiskWeak = 'Weak'
DiskDisable = 'Disable'

### replica type ###
DiskPoolReplica1 = 'OnePlusZero'
DiskPoolReplica2 = 'OnePlusOne'
DiskPoolReplica3 = 'OnePlusTwo'

### diskpool type ###
DiskPoolClassStandard = 'STANDARD'
DiskPoolClassArchive = 'ARCHIVE'

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
### service binamry name ###
KsanOsdBinaryName = 'ksan-osd.jar'
KsanGwBinaryName = 'ksan-gw.jar'
KsanRecoveryBinaryName = 'ksanRecovery'
KsanMonitorBinaryName = 'ksanMonitor'
KsanAgentBinaryName = 'ksanAgent'
KsanUtilName = 'ksan'
KsanServerRegister = 'ksanServerRegister'

KsanUtilPath = '%s/ksan' % KsanBinPath
KsanAgentPath = '%s/%s' % (KsanSbinPath, KsanAgentBinaryName)
KsanGwPath = '%s/%s' % (KsanSbinPath, KsanGwBinaryName)
KsanOsdPath = '%s/%s' % (KsanSbinPath, KsanOsdBinaryName)
KsanRecoveryPath = '%s/%s' % (KsanSbinPath, KsanRecoveryBinaryName)
KsanServerRegisterPath = '%s/%s' % (KsanUtilDirPath, KsanServerRegister)


### service state ###
ServiceStateOnline = 'Online'
ServiceStateOffline = 'Offline'
ServiceStateUnkown = 'Unknown'

### service pid file ###

KsanOsdPidFile = '/var/run/ksanOsd.pid'
KsanGwPidFile = '/var/run/ksanGw.pid'
KsanMonitorPidFile = '/var/run/ksanMonitor.pid'
KsanAgentPidFile = '/var/run/ksanAgent.pid'
KsanMongosPidFile = '/var/run/mongod.pid'


###  service status ###
START = 'Start'
STOP = 'Stop'
RESTART = 'Restart'
ONLINE = 'Online'
OFFLINE = 'Offline'


### service type ###
TypeServiceOSD = 'ksanOSD'
TypeServiceGW = 'ksanGW'
TypeServiceMongoDB = 'MongoDB'
TypeServiceMariaDB = 'MariaDB'
TypeServiceObjmanager = 'OBJMANAGER'
TypeServiceS3Backend = 'S3Backend'
TypeServiceMonitor = 'ksanMonitor'
TypeServiceAgent = 'ksanAgent'
TypeServiceRabbitMq = 'RabbitMq'
TypeServiceHaproxy = 'HaProxy'
TypeServiceFsck = 'ksanFsck.jar'
TypeServiceGetAttr = 'ksanGetAttr.jar'
TypeServiceCbalance = 'ksanCbalance.jar'
TypeServiceLifecycle = 'ksanLifecycle'
TypeServiceRecovery = 'ksanRecovery'

SampleS3ConfFile = './objmanager.conf'
S3ConfFile = '/opt/objmanager.conf'
SampleHaproxyConfFile = './haproxy.cfg'
HaproxyConfFile = '/opt/haproxy.cfg'
ServiceStart = 'Start'
ServiceStop = 'Stop'
ServiceRestart = 'Restart'


### service id path define ###
KsanAgentServiceIdHiddenPath = '/usr/local/ksan/sbin/.ksanAgent.ServiceId'
KsanOSDServiceIdHiddenPath = '/usr/local/ksan/sbin/.ksanOSD.ServiceId'
KsanGWServiceIdHiddenPath = '/usr/local/ksan/sbin/.ksanGW.ServiceId'
KsanLifecycleServiceIdHiddenPath = '/usr/local/ksan/sbin/.ksanLifecycle.ServiceId'
KsanRecoveryServiceIdHiddenPath = '/usr/local/ksan/sbin/.ksanRecovery.ServiceId'

ServiceTypeServiceHiddenPathMap = dict()
ServiceTypeServiceHiddenPathMap[TypeServiceAgent] = KsanAgentServiceIdHiddenPath
ServiceTypeServiceHiddenPathMap[TypeServiceOSD] = KsanOSDServiceIdHiddenPath
ServiceTypeServiceHiddenPathMap[TypeServiceGW] = KsanGWServiceIdHiddenPath
ServiceTypeServiceHiddenPathMap[TypeServiceLifecycle] = KsanLifecycleServiceIdHiddenPath
ServiceTypeServiceHiddenPathMap[TypeServiceRecovery] = KsanRecoveryServiceIdHiddenPath

### service unit ###
SystemdKsanGWServiceName = 'ksangw.service'
SystemdKsanOSDServiceName = 'ksanosd.service'
SystemdKsanAgentServiceName = 'ksanagent.service'
SystemdKsanLifecycleServiceName = 'ksanlifecycle.service'
SystemdKsanRecoveryServiceName = 'ksanrecovery.service'
ServiceTypeSystemdServiceMap = dict()
ServiceTypeSystemdServiceMap[TypeServiceGW] = SystemdKsanGWServiceName
ServiceTypeSystemdServiceMap[TypeServiceOSD] = SystemdKsanOSDServiceName
ServiceTypeSystemdServiceMap[TypeServiceLifecycle] = SystemdKsanLifecycleServiceName
ServiceTypeSystemdServiceMap[TypeServiceRecovery] = SystemdKsanRecoveryServiceName

### docker container servicename map ###
ServiceTypeDockerServiceContainerNameMap = dict()
ServiceTypeDockerServiceContainerNameMap[TypeServiceOSD] = 'ksan-osd'
ServiceTypeDockerServiceContainerNameMap[TypeServiceGW] = 'ksan-gw'
ServiceTypeDockerServiceContainerNameMap[TypeServiceLifecycle] = 'ksan-lifecycle'
ServiceTypeDockerServiceContainerNameMap[TypeServiceRecovery] = 'ksan-recovery'


### service type converion ###
ServiceTypeConversion = dict()
ServiceTypeConversion['ksanosd'] = TypeServiceOSD
ServiceTypeConversion['ksangw'] = TypeServiceGW
#ServiceTypeConversion['mongodb'] = TypeServiceMongoDB
#ServiceTypeConversion['mariadb'] = TypeServiceMariaDB
#ServiceTypeConversion['ksanmonitor'] = TypeServiceMonitor
ServiceTypeConversion['ksanagent'] = TypeServiceAgent
ServiceTypeConversion['ksanlifecycle'] = TypeServiceLifecycle
ServiceTypeConversion['ksanrecovery'] = TypeServiceRecovery
#ServiceTypeConversion['rabbitmq'] = TypeServiceRabbitMq
#ServiceTypeConversion['haproxy'] = TypeServiceHaproxy


class AgentConf(BaseModel):
    LocalIp: str
    PortalHost: str
    PortalPort: int
    MQHost: str
    MQPort: int
    MQPassword: str
    MQUser: str
    PortalApiKey: str
