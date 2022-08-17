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

Updated = 1 # disk, network, service, server changed flag
Checked = 0

##### CONFIG FILE TYPE #####
ConfigTypeINI = 'INI'
ConfigTypeObject = 'Object'


##### PATH #####
KsanEtcPath = '/usr/local/ksan/etc'
KsanBinPath = '/usr/local/ksan/bin'

MonServicedConfPath = '/usr/local/ksan/etc/ksanMonitor.conf'
DiskPoolXmlPath = '/usr/local/ksan/etc/diskpools.xml'
ServicePoolXmlPath = '/usr/local/ksan/etc/servicepools.xml'
OsdXmlFilePath = '/usr/local/ksan/etc/ksan-osd-log.xml'
GwXmlFilePath = '/usr/local/ksan/etc/ksan-gw-log.xml'
KsanMongDbManagerBinPath = '/usr/local/ksan/bin/ksanMongoDBManager'

##### ksanMonitor.conf #####
DiskMonitorInterval = 10
ProcessMonitorInterval = 10
ServerMonitorInterval = 10
ServiceMonitorInterval = 10
NetworkMonitorInterval = 10
IntervalShort = 1
IntervalMiddle = 5
IntervalLong = 10


"""
##### HTTP #####
"""
### http method ###
GetMethod = 'GET'
PostMethod = 'POST'
PutMethod = 'PUT'
DeleteMethod = 'DELETE'

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
ResutlNotFound = 'EC014'

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
### disk stat ###
DiskStatOnline = 'Online'
DiskStatOffline = 'Offline'
DiskModeRw = 'ReadWrite'
DiskModeRo = 'ReadOnly'
DiskHaActionInit = 'Initializing'

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
##### SERVICE #####
"""
### service binamry name ###
KsanOsdBinaryName = 'ksan-osd.jar'
KsanGwBinaryName = 'ksan-gw.jar'
KsanRecovery = 'ksanRecovery'
KsanMonitorBinaryName = 'ksanMonitor'
KsanAgentBinaryName = 'ksanAgent'

### service pid file ###
KsanOsdPidFile = '/var/run/ksanOsd.pid'
KsanGwPidFile = '/var/run/ksanGw.pid'
KsanMonitorPidFile = '/var/run/ksanMonitor.pid'
KsanAgentPidFile = '/var/run/%s.pid' % KsanAgentBinaryName
KsanMongosPidFile = '/var/run/mongod.pid'

###  service status ###
START = 'Start'
STOP = 'Stop'
RESTART = 'Restart'
ONLINE = 'Online'
OFFLINE = 'Offline'

### service action retry count ###
ServiceContolRetryCount = 3

### service type ###
TypeS3 = 'IfsS3'
TypeTomcat = 'tomcat'
TypeServiceOSD = 'KsanOsd'
TypeServiceS3 = 'KsanGw'
TypeServiceMongoDB = 'MongoDB'
TypeServiceMariaDB = 'MariaDB'
TypeServiceObjmanager = 'OBJMANAGER'
TypeServiceS3Backend = 'S3Backend'
TypeServiceMonitor = 'KsanMonitor'
TypeServiceAgent = 'KsanAgent'
TypeServiceRabbitMq = 'RabbitMq'
TypeServiceHaproxy = 'HaProxy'
SampleS3ConfFile = './objmanager.conf'
S3ConfFile = '/opt/objmanager.conf'
SampleHaproxyConfFile = './haproxy.cfg'
HaproxyConfFile = '/opt/haproxy.cfg'
ServiceStart = 'Start'
ServiceStop = 'Stop'
ServiceRestart = 'Restart'

ServiceTypeConversion = dict()
ServiceTypeConversion['ksanosd'] = TypeServiceOSD
ServiceTypeConversion['ksangw'] = TypeServiceS3
ServiceTypeConversion['mongodb'] = TypeServiceMongoDB
ServiceTypeConversion['mariadb'] = TypeServiceMariaDB
ServiceTypeConversion['ksanmonitor'] = TypeServiceMonitor
ServiceTypeConversion['ksanagent'] = TypeServiceAgent
ServiceTypeConversion['rabbitmq'] = TypeServiceRabbitMq
ServiceTypeConversion['haproxy'] = TypeServiceHaproxy

