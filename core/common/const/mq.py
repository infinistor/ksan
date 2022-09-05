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
import re


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
                       "*.servers.diskpools.removed", "*.services.added", "*.services.updated", "*.services.removed"]

MonRoutingKeyList = ["*.servers.updated", "*.servers.removed", "*.servers.interfaces.added", "*.servers.added",
                     "*.servers.interfaces.updated", "*.servers.interfaces.removed", "*.servers.interfaces.vlans.added"
                    , "*.servers.interfaces.vlans.updated", "*.servers.interfaces.vlans.removed",
                     "*.servers.disks.added", "*.servers.disks.updated", "*.servers.disks.removed",
                     "*.servers.disks.rwmode", "*.servers.diskpools.added", "*.servers.diskpools.updated",
                     "*.servers.diskpools.removed", "*.services.added", "*.services.updated"]

## Exchange Name
ExchangeName = 'ksan.system'