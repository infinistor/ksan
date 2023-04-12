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


import os, sys

if os.path.dirname(os.path.abspath(os.path.dirname(__file__))) not in sys.path:
    sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from const.common import *
from const.disk import  DiskAdd2DiskPoolItems, RequestDiskPool
from const.common import DiskPoolDetailModule, DiskPoolItemsModule
from common.utils import *
from common.base_utils import *
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from const.http import ResponseHeaderModule


@catch_exceptions()
def AddDiskPool(Ip, Port, ApiKey, Name, DiskPoolType, ReplicationType, Description=None, logger=None):
    pool = RequestDiskPool()
    DefaultDiskIds = list()
    pool.Set(Name, Description, DefaultDiskIds, DiskPoolType=DiskPoolType, ReplicationType=ReplicationType)

    Url = '/api/v1/DiskPools'
    ReturnType = ResponseHeaderModule
    body = jsonpickle.encode(pool, make_refs=False)
    Conn = RestApi(Ip, Port, Url, authkey=ApiKey, params=body, logger=logger)
    Res, Errmsg, Data = Conn.post(ItemsHeader=False, ReturnType=ReturnType)
    return Res, Errmsg, Data


@catch_exceptions()
def RemoveDiskPool(ip, port, ApiKey, DiskPoolId=None, DiskPoolName=None, logger=None):
    # get network interface info
    if DiskPoolName is not None:
        TargetDiskPool = DiskPoolName
    elif DiskPoolId is not None:
        TargetDiskPool = DiskPoolId
    else:
        return ResInvalidCode, ResInvalidMsg + ' DiskPoolId or DiskPoolName is required', None
    Url = '/api/v1/DiskPools/%s' % TargetDiskPool
    ReturnType = ResponseHeaderModule
    Conn = RestApi(ip, port, Url, authkey=ApiKey, logger=logger)
    Res, Errmsg, Ret = Conn.delete(ItemsHeader=False, ReturnType=ReturnType)
    return Res, Errmsg, Ret

@catch_exceptions()
def SetDefaultDiskPool(ip, port, ApiKey, DiskPoolId=None, DiskPoolName=None, logger=None):
    # get network interface info
    if DiskPoolName is not None:
        TargetDiskPool = DiskPoolName
    elif DiskPoolId is not None:
        TargetDiskPool = DiskPoolId
    else:
        return ResInvalidCode, ResInvalidMsg + ' DiskPoolName is required', None
    Url = '/api/v1/DiskPools/Default/%s' % TargetDiskPool
    ReturnType = ResponseHeaderModule
    Conn = RestApi(ip, port, Url, authkey=ApiKey, logger=logger)
    Res, Errmsg, Ret = Conn.put(ItemsHeader=False, ReturnType=ReturnType)
    return Res, Errmsg, Ret

@catch_exceptions()
def GetDefaultDiskPool(ip, port, ApiKey, logger=None):
    # get network interface info
    Url = '/api/v1/DiskPools/Default'
    ReturnType = DiskPoolDetailModule
    Conn = RestApi(ip, port, Url, authkey=ApiKey, logger=logger)
    Res, Errmsg, Ret = Conn.get(ItemsHeader=False, ReturnType=ReturnType)
    return Res, Errmsg, Ret




def GetDiskIdList(PoolDetailInfo):
    DiskIdListOfPool = list()
    DiskNameListOfPool = list()
    for disk in PoolDetailInfo.Disks:
        DiskIdListOfPool.append(disk.Id)
        DiskNameListOfPool.append(disk.Name)
    return DiskIdListOfPool, DiskNameListOfPool


@catch_exceptions()
def AddDisk2DiskPool(Ip, Port, ApiKey, DiskPoolId=None, AddDiskIds=None, DiskPoolName=None, logger=None):
    if DiskPoolId is not None:
        TargetDiskPool = DiskPoolId
    elif DiskPoolName is not None:
        TargetDiskPool = DiskPoolName
    else:
        return ResInvalidCode, ResInvalidMsg + ErrMsgDiskpoolNameMissing, None

    Url = '/api/v1/DiskPools/Disks/%s' % TargetDiskPool
    pool = DiskAdd2DiskPoolItems()
    pool.Set(AddDiskIds)
    ReturnType = ResponseHeaderModule
    body = jsonpickle.encode(pool, make_refs=False)
    Conn = RestApi(Ip, Port, Url, authkey=ApiKey, params=body, logger=logger)
    Res, Errmsg, Data = Conn.post(ItemsHeader=False, ReturnType=ReturnType)
    return Res, Errmsg, Data

@catch_exceptions()
def RemoveDisk2DiskPool(Ip, Port, ApiKey, DiskPoolId=None,DelDiskIds=None, DiskPoolName=None, logger=None):
    if DiskPoolId is not None:
        TargetDiskPool = DiskPoolId
    elif DiskPoolName is not None:
        TargetDiskPool = DiskPoolName
    else:
        return ResInvalidCode, ResInvalidMsg + ErrMsgDiskpoolNameMissing, None

    Url = '/api/v1/DiskPools/Disks/%s' % TargetDiskPool
    pool = DiskAdd2DiskPoolItems()
    pool.Set(DelDiskIds)
    ReturnType = ResponseHeaderModule
    body = jsonpickle.encode(pool, make_refs=False)
    Conn = RestApi(Ip, Port, Url, authkey=ApiKey, params=body, logger=logger)
    Res, Errmsg, Data = Conn.delete(ItemsHeader=False, ReturnType=ReturnType)
    return Res, Errmsg, Data




@catch_exceptions()
def UpdateDiskPool(Ip, Port, ApiKey, DiskPoolId=None, ReplicationType=None, AddDiskIds=None, DelDiskIds=None, DiskPoolName=None, Description='', logger=None):
    if DiskPoolId is not None:
        TargetDiskPool = DiskPoolId
    elif DiskPoolName is not None:
        TargetDiskPool = DiskPoolName
    else:
        return ResInvalidCode, ResInvalidMsg + ' DiskPoolId or DiskPoolName is required', None

    Res, Errmsg, Ret, PoolDetail = GetDiskPoolInfo(Ip, Port, ApiKey, DiskPoolId=DiskPoolId, DiskPoolName=DiskPoolName, logger=logger)
    if Res == ResOk:
        if Ret.Result != ResultSuccess:
            return Res, Errmsg, Ret


    UpdateDiskListOfPool = list()
    DiskIdListOfPool, DiskNameListOfPool = GetDiskIdList(PoolDetail)
    if AddDiskIds is not None:
        DiskIdListOfPool = DiskIdListOfPool + AddDiskIds
        UpdateDiskListOfPool = DiskIdListOfPool

    isDiskIdListOfPoolUpdated = False
    isDiskNameListOfPoolUpdated = False
    if DelDiskIds is not None:
        for DiskId in DelDiskIds:
            if DiskId in DiskIdListOfPool:
                DiskIdListOfPool.remove(DiskId)
                isDiskIdListOfPoolUpdated = True

        for DiskName in DelDiskIds:
            if DiskName in DiskNameListOfPool:
                DiskNameListOfPool.remove(DiskName)
                isDiskNameListOfPoolUpdated = False

    if isDiskIdListOfPoolUpdated is True:
        UpdateDiskListOfPool = DiskIdListOfPool
    else:
        if isDiskNameListOfPoolUpdated is True:
            UpdateDiskListOfPool = DiskNameListOfPool

    if DiskPoolName is not None:
        PoolDetail.Name = DiskPoolName

    if ReplicationType is not None:
        if PoolDetail.ReplicationType == DiskPoolReplicaEC or ReplicationType == DiskPoolReplicaEC:
            return ResInvalidCode, 'Changing DiskPoolType to %s is not supporeted' % DiskPoolReplicaEC, None
        else:
            PoolDetail.ReplicationType = ReplicationType

    if Description is not '':
        PoolDetail.Description = Description
    Url = '/api/v1/DiskPools/%s' % TargetDiskPool
    pool = RequestDiskPool()
    pool.Set(PoolDetail.Name, PoolDetail.Description, UpdateDiskListOfPool, DiskPoolType=PoolDetail.DiskPoolType,
             ReplicationType=PoolDetail.ReplicationType )
    ReturnType = ResponseHeaderModule
    body = jsonpickle.encode(pool, make_refs=False)
    Conn = RestApi(Ip, Port, Url, authkey=ApiKey, params=body, logger=logger)
    Res, Errmsg, Data = Conn.put(ItemsHeader=False, ReturnType=ReturnType)
    return Res, Errmsg, Data


def OldGetDiskPoolInfo(Ip, Port, ApiKey,  DiskPoolId=None, DiskPoolName=None, logger=None):
    """
    Get All Disk Info with Server Info
    :param Ip:
    :param Port:
    :param Disp:
    :param logger:
    :return:
    """
    if DiskPoolId is not None:
        TargetDiskPool = DiskPoolId
    elif DiskPoolName is not None:
        TargetDiskPool = DiskPoolName
    else:
        TargetDiskPool = None

    if TargetDiskPool is not None:
        Url = "/api/v1/DiskPools/%s" % TargetDiskPool
        ReturnType = DiskPoolDetailModule
        ItemsHeader = False
    else:
        Url = "/api/v1/DiskPools/Details"
        ReturnType = DiskPoolItemsModule
        ItemsHeader = True

    Params = dict()
    Params['countPerPage'] = 100
    Conn = RestApi(Ip, Port, Url, authkey=ApiKey, params=Params, logger=logger)

    Res, Errmsg, Ret = Conn.get(ItemsHeader=ItemsHeader, ReturnType=ReturnType)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            if TargetDiskPool is not None:
                return Res, Errmsg, Ret, Ret.Data
            else:
                return Res, Errmsg, Ret, Ret.Data.Items
        else:
            return Res, Errmsg, Ret, None
    else:
        return Res, Errmsg, None, None


def GetDiskPoolInfo(Ip, Port, ApiKey,  DiskPoolId=None, DiskPoolName=None, logger=None):
    """
    Get All Disk Info with Server Info
    :param Ip:
    :param Port:
    :param Disp:
    :param logger:
    :return:
    """
    if DiskPoolId is not None:
        TargetDiskPool = DiskPoolId
    elif DiskPoolName is not None:
        TargetDiskPool = DiskPoolName
    else:
        TargetDiskPool = None

    if TargetDiskPool is not None:
        Url = "/api/v1/DiskPools/%s" % TargetDiskPool
        ReturnType = DiskPoolDetailModule
        ItemsHeader = False
    else:
        Url = "/api/v1/DiskPools"
        ReturnType = DiskPoolItemsModule
        ItemsHeader = True

    Params = dict()
    Params['countPerPage'] = 100
    Conn = RestApi(Ip, Port, Url, authkey=ApiKey, params=Params, logger=logger)

    Res, Errmsg, Ret = Conn.get(ItemsHeader=ItemsHeader, ReturnType=ReturnType)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            if TargetDiskPool is not None:
                return Res, Errmsg, Ret, Ret.Data
            else:
                return Res, Errmsg, Ret, Ret.Data.Items
        else:
            return Res, Errmsg, Ret, None
    else:
        return Res, Errmsg, None, None



def GetAllDiskPoolListDetail(Ip, Port, logger=None):
    Res, Errmsg, Ret, DiskPoolList = GetDiskPoolInfo(Ip, Port, logger=logger)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            DiskPoolDetailList = list()
            for pool in DiskPoolList:
                res, errmsg, ret, DiskPoolDetail = GetDiskPoolInfo(Ip, Port, DiskPoolId=pool.Id, logger=logger)
                if Res == ResOk:
                    if Ret.Result == ResultSuccess:
                        DiskPoolDetailList.append(DiskPoolDetail)
                    else:
                        print('fail to get Disk Pool Detail info %s' % ret.Message)
                else:
                    print('fail to get Disk Pool Detail info %s' % errmsg)
            return Res, Errmsg, Ret, DiskPoolDetailList
        else:
            print('fail to get Disk Pool List %s' % Errmsg)
    else:
        print('fail to get Disk Pool List %s' % Errmsg)
    return Res, Errmsg, None, None



def ShowDiskPoolInfoNew(DiskPoolList, Detail=SimpleInfo, SysinfoDsp=False):

    if SysinfoDsp is True:
        TopTitleLine = "%s" % ("=" * 105)
        DiskPoolTitleLine = "%s" % ("-" * 105)
        print(TopTitleLine)
        DiskPoolTitle = "|%s|%s|%s|%s|" % (
            'DiskPoolName'.ljust(26), 'DiskPoolId'.ljust(38), 'DiskPoolType'.ljust(18), 'Tolerance'.ljust(18))
        print(DiskPoolTitle)
        print(TopTitleLine)

        DiskTitle = "%s|%s|%s|%s|%s|%s|" % (' ' * 27, 'ServerName'.ljust(19), '' 'DiskName'.ljust(20),
                                                           'DiskPath'.ljust(20), 'Mode'.ljust(6), 'Status'.ljust(7))

        DiskTitleLine = "%s%s" % (" " * 27, "-" * 78)


    else:

        if Detail == DetailInfo:
            TopTitleLine = "%s" % ("=" * 177)
            DiskPoolTitleLine = "%s" % ("-" * 177)
            print(TopTitleLine)
            DiskPoolTitle = "|%s|%s|%s|%s|%s|" % (
                'DiskPoolName'.ljust(26), 'DiskPoolId'.ljust(38), 'DiskPoolType'.ljust(18), 'Tolerance'.ljust(18), " " * 71)
            print(DiskPoolTitle)
            print(TopTitleLine)


            DiskTitle = "%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|" % (' ' * 27, 'ServerName'.ljust(16), '' 'DiskName'.ljust(20),
                                                            'DiskPath'.ljust(20), 'TotalSize'.ljust(9),
                                                            'UsedSize'.ljust(8),
                                                            'Read'.ljust(8), 'Write'.ljust(8), 'Mode'.ljust(6),
                                                            'Status'.ljust(7), 'DiskId'.ljust(37))

            DiskTitleLine = "%s%s" % (" " * 27, "-" * 150)

        else:
            TopTitleLine = "%s" % ("=" * 139)
            DiskPoolTitleLine = "%s" % ("-" * 139)
            print(TopTitleLine)
            DiskPoolTitle = "|%s|%s|%s|%s|%s|" % (
                'DiskPoolName'.ljust(26), 'DiskPoolId'.ljust(38), 'DiskPoolType'.ljust(18), 'Tolerance'.ljust(18), " " * 33)
            print(DiskPoolTitle)
            print(TopTitleLine)

            DiskTitle = "%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|" % (' ' * 27, 'ServerName'.ljust(16), '' 'DiskName'.ljust(20),
                                                           'DiskPath'.ljust(20), 'TotalSize'.ljust(9),
                                                           'UsedSize'.ljust(8),
                                                           'Read'.ljust(8), 'Write'.ljust(8), 'Mode'.ljust(6),
                                                           'Status'.ljust(7))
            DiskTitleLine = "%s%s" % (" " * 27, "-" * 112)

    for pool in DiskPoolList:
        if SysinfoDsp is True:
            _pool = "|%s|%s|%s|%s|" % (pool.Name.ljust(26), pool.Id.ljust(38),
                                          str(pool.DiskPoolType).ljust(18),
                                          GetReplicationDspType(str(pool.ReplicationType), EC=pool.EC).ljust(18))
        else:
            if Detail == DetailInfo:
                _pool = "|%s|%s|%s|%s|%s|" % (pool.Name.ljust(26), pool.Id.ljust(38),
                    str(pool.DiskPoolType).ljust(18), GetReplicationDspType(str(pool.ReplicationType), EC=pool.EC).ljust(18),' '* 71)
            else:
                _pool = "|%s|%s|%s|%s|%s|" % (pool.Name.ljust(26), pool.Id.ljust(38),
                                          str(pool.DiskPoolType).ljust(18), GetReplicationDspType(str(pool.ReplicationType), EC=pool.EC).ljust(18),
                                          " " * 33)

        print(_pool)
        print(DiskPoolTitleLine)
        print(DiskTitle)

        TotalDiskList = list()
        ServerNameDict = dict()
        # ordering severname and disk name of diskpool
        DiskNameDict = dict()
        for disk in pool.Disks:
            servername = disk.ServerName
            if servername not in ServerNameDict:
                ServerNameDict[servername] = list()
            diskname = disk.Name
            DiskNameDict[diskname] = disk

        for diskname in sorted(DiskNameDict.keys(), key=str.casefold): # ordering disk name
            diskinfo = DiskNameDict[diskname]
            ServerNameDict[diskinfo.ServerName].append(diskinfo)

        for server in sorted(ServerNameDict.keys(), key=str.casefold): # ordering server name
            TotalDiskList += ServerNameDict[server]

        if len(TotalDiskList) > 0:
            print(DiskTitleLine)
            for idx, disk in enumerate(TotalDiskList):
                #disk = DictToObject(disk)

                if SysinfoDsp is True:
                    _disk = "%s|%s|%s|%s|%s|%s|" % (
                        ' ' * 27, disk.ServerName.ljust(19), disk.Name.ljust(20),
                        disk.Path.ljust(20), DisplayDiskMode(disk.RwMode).ljust(6),
                        DisplayDiskState(disk.State).ljust(7))
                else:
                    if Detail == DetailInfo:
                        _disk = "%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|" % (
                        ' ' * 27, disk.ServerName.ljust(16), disk.Name.ljust(20),
                        disk.Path.ljust(20), str(Byte2HumanValue(int(disk.TotalSize), 'TotalSize')).rjust(9),
                        str(Byte2HumanValue(int(disk.UsedSize), 'UsedSize')).rjust(8),
                        str(Byte2HumanValue(int(disk.Read), 'DiskRw')).rjust(8),
                        str(Byte2HumanValue(int(disk.Write), 'DiskRw')).rjust(8),
                        DisplayDiskMode(disk.RwMode).ljust(6), DisplayDiskState(disk.State).ljust(7), disk.Id.ljust(37))
                    else:
                        _disk = "%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|" % (
                            ' ' * 27, disk.ServerName.ljust(16), disk.Name.ljust(20),
                            disk.Path.ljust(20), str(Byte2HumanValue(int(disk.TotalSize), 'TotalSize')).rjust(9),
                            str(Byte2HumanValue(int(disk.UsedSize), 'UsedSize')).rjust(8),
                            str(Byte2HumanValue(int(disk.Read), 'DiskRw')).rjust(8),
                            str(Byte2HumanValue(int(disk.Write), 'DiskRw')).rjust(8),
                            DisplayDiskMode(disk.RwMode).ljust(6), DisplayDiskState(disk.State).ljust(7))

                print(_disk)
                if len(TotalDiskList) - 1 == idx:
                    print(DiskPoolTitleLine)
                else:
                    print(DiskTitleLine)
        else:
            print(DiskTitleLine)
            if SysinfoDsp is True:
                print("%s|%s|" % (' ' * 27, 'No disk data'.center(76)))
            else:
                if Detail == DetailInfo:
                    print("%s|%s|" % (' ' * 27, 'No disk data'.center(148)))
                else:
                    print("%s|%s|" % (' ' * 27, 'No disk data'.center(110)))
            print(DiskPoolTitleLine)


def GetReplicationDspType(StringType, EC=None):
    if StringType == 'OnePlusOne':
        return 'Replication(1+1)'
    elif StringType == 'OnePlusZero':
        return 'Disable(1+0)'
    elif StringType == 'OnePlusTwo':
        return '1+2'
    elif StringType == 'ErasureCode':
        return 'EC(%d:%d)' % (EC['K'], EC['M'])
    else:
        return 'Invalid Replica'

def DiskpoolUtilHandler(Conf, Action, Parser, logger):

    options, args = Parser.parse_args()
    PortalIp = Conf.PortalHost
    PortalPort = Conf.PortalPort
    PortalApiKey = Conf.PortalApiKey
    MqPort = Conf.MQPort
    MqPassword = Conf.MQPassword

    if Action is None:
        Parser.print_help()
        sys.exit(-1)

    if Action.lower() == 'add':
        if not (options.DiskpoolName and options.RepType and options.DiskpoolType):
            Parser.print_help()
            sys.exit(-1)

        isDiskPoolTypeValid = False
        for diskpooltype in ValidDiskPoolType:
            if options.DiskpoolType.lower() == diskpooltype.lower():
                isDiskPoolTypeValid = True
                break

        if isDiskPoolTypeValid is False:
            print('Error : Unsupported DiskPool Type. Valid DiskPool type is %s' % ','.join(ValidDiskPoolType))
            sys.exit(-1)

        if options.RepType.lower() == 'disable':
            ReplicationType = DiskPoolReplica1
        elif options.RepType.lower() == 'replication':
            ReplicationType = DiskPoolReplica2
        else:
            Ec = ECValueParser.search(options.RepType)
            if Ec:
                isValid = True
                K,M = Ec.groups()
                K = int(K)
                M = int(M)
                if K == 0 or M == 0 or K >= 100 or M >= 100:
                    print('data chunks(k) or coding chunks(m) must be bigger than zero and less than 100')
                    isValid = False
                elif K < M:
                    print('coding chunks(m) must be less than or equal to data chunks(k)')
                    isValid = False
                if isValid is False:
                    sys.exit(-1)

                ReplicationType = 'ErasureCode'
            else:
                Parser.print_help()
                print('Not supported Tolerance type')
                sys.exit(-1)

        Res, Errmsg, Ret = AddDiskPool(PortalIp, PortalPort, PortalApiKey, options.DiskpoolName, options.DiskpoolType,
                                       ReplicationType,
                                       logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)
    elif Action.lower() == 'remove':
        if not options.DiskpoolName:
            Parser.print_help()
            sys.exit(-1)
        Res, Errmsg, Ret = RemoveDiskPool(PortalIp, PortalPort, PortalApiKey, DiskPoolName=options.DiskpoolName, logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)
    elif Action.lower() == 'set':
        if not (options.DefaultDiskpool and options.DiskpoolName):
            Parser.print_help()
            sys.exit(-1)
        Res, Errmsg, Ret = SetDefaultDiskPool(PortalIp, PortalPort, PortalApiKey, DiskPoolName=options.DiskpoolName, logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)
    elif Action.lower() == 'get':
        Res, Errmsg, Ret = GetDefaultDiskPool(PortalIp, PortalPort, PortalApiKey, logger=logger)
        if Res == ResOk:
            if Ret.Data is not None:
                print(Ret.Data.Name)
            else:
                print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)
    elif Action.lower() == 'add2disk':
        AddDiskIds = None
        if not (options.DiskpoolName and options.DiskName):
            Parser.print_help()
            sys.exit(-1)
        if options.DiskName is not None:
            AddDiskIds = options.DiskName.split()
        else:
            Parser.print_help()
            print("Disk Ids or Disk Name is required")
            sys.exit(-1)

        Res, Errmsg, Ret = AddDisk2DiskPool(PortalIp, PortalPort, PortalApiKey, AddDiskIds=AddDiskIds,
                                           DiskPoolName=options.DiskpoolName, logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)
    elif Action.lower() == 'remove2disk':
        if not ((options.DiskpoolName and options.DiskName)):
            Parser.print_help()
            sys.exit(-1)
        DelDiskIds = options.DiskName.split()

        Res, Errmsg, Ret = RemoveDisk2DiskPool(PortalIp, PortalPort, PortalApiKey, DelDiskIds=DelDiskIds,
                                           DiskPoolName=options.DiskpoolName, logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)

    elif Action.lower() in ['update', 'modify']:
        AddDiskIds = None
        DelDiskIds = None
        if Action.lower() == 'add2disk':
            if not (options.DiskpoolName and options.DiskName):
                Parser.print_help()
                sys.exit(-1)
            if options.DiskName is not None:
                AddDiskIds = options.DiskName.split()
            else:
                Parser.print_help()
                print("Disk Ids or Disk Name is required")
                sys.exit(-1)
        elif Action.lower() == 'remove2disk':
            if not ((options.DiskpoolName and options.DiskName)):
                Parser.print_help()
                sys.exit(-1)
            DelDiskIds = options.DiskName.split()
        elif Action.lower() == 'modify':

            if options.RepType.lower() == 'disable':
                ReplicationType = DiskPoolReplica1
            elif options.RepType.lower() == 'replication':
                ReplicationType = DiskPoolReplica2
            else:
                print('Invalid DiskPoolType. Only disable or replication type is supported')
                sys.exit(-1)

        Res, Errmsg, Ret = UpdateDiskPool(PortalIp, PortalPort, PortalApiKey, AddDiskIds=AddDiskIds, DelDiskIds=DelDiskIds,
                                           DiskPoolName=options.DiskpoolName, ReplicationType=ReplicationType, logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)

    elif Action.lower() == 'list':
        while True:
            #Res, Errmsg, Ret, DiskPoolList = GetDiskPoolInfo(PortalIp, PortalPort, PortalApiKey, logger=logger)
            Res, Errmsg, Ret, DiskPoolList = GetDiskPoolInfo(PortalIp, PortalPort, PortalApiKey, logger=logger)
            if Res != ResOk:
                print(Errmsg)
            else:
                #if options.MoreDetail:
                #    Detail = MoreDetailInfo
                if options.Detail:
                    Detail = DetailInfo
                else:
                    Detail = SimpleInfo
                ShowDiskPoolInfoNew(DiskPoolList, Detail=Detail)
            if options.Continue is None:
                break
            else:
                time.sleep(int(options.Continue))

    else:
        Parser.print_help()

