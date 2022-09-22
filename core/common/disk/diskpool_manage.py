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
import pdb
import time

if os.path.dirname(os.path.abspath(os.path.dirname(__file__))) not in sys.path:
    sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from common.httpapi import *
from const.common import *
from common.utils import Byte2HumanValue, DisplayDiskState, DisplayDiskMode
from const.disk import RequestDiskPool, DiskPoolDetailModule, DiskPoolItemsModule, DiskAdd2DiskPoolItems
from const.http import ResponseHeaderModule
from server.server_manage import GetAllServerDetailInfo
import jsonpickle


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
    Res, Errmsg, Ret = Conn.post(ItemsHeader=False, ReturnType=ReturnType)
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
    Res, Errmsg, Data = Conn.put(ItemsHeader=False, ReturnType=ReturnType)
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
def UpdateDiskPool(Ip, Port, ApiKey, DiskPoolId=None, AddDiskIds=None, DelDiskIds=None, DiskPoolName=None, Description='', logger=None):
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


@catch_exceptions()
def ShowDiskPoolInfo(DiskPoolList, ServerDetailInfo, Detail=False, SysinfoDisp=False):
    """
    Display Disk list
    :param DiskList: DiskItems object list
    :param ServerId:
    :param DiskId:
    :param Detail:
    :return:
    """

    DiskPools = list()

    for pool in DiskPoolList:
        TmpPool = dict()
        TmpPool['Name'] = pool.Name
        TmpPool['Id'] = pool.Id
        TmpPool['Description'] = pool.Description
        TmpPool['DiskPoolType'] = pool.DiskPoolType
        TmpPool['ReplicationType'] = pool.ReplicationType
        TmpPool['DiskList'] = list()
        DiskPools.append(TmpPool)

    for pool in DiskPools:
        PoolId = pool['Id']
        for svr in ServerDetailInfo:
            ServerName = svr.Name
            for disk in svr.Disks:
                if disk.DiskPoolId != PoolId:
                    continue

                TmpDisk = dict()
                TmpDisk['DiskName'] = disk.Name
                TmpDisk['DiskId'] = disk.Id
                TmpDisk['ServerName'] = ServerName
                TmpDisk['Path'] = disk.Path
                TmpDisk['TotalSize'] = disk.TotalSize
                TmpDisk['UsedSize'] = disk.UsedSize
                TmpDisk['Read'] = disk.Read
                TmpDisk['Write'] = disk.Write
                TmpDisk['State'] = disk.State
                TmpDisk['RwMode'] = disk.RwMode
                pool['DiskList'].append(TmpDisk)

    if SysinfoDisp is True:
        TopTitleLine = "%s" % ("=" * 105)
        DiskPoolTitleLine = "%s" % ("-" * 105)
        print(TopTitleLine)
        DiskPoolTitle = "|%s|%s|%s|%s|" % (
        'Diskpool'.center(25), 'DiskPoolType'.center(15), 'ReplicationType'.center(17), " " * 43)
        print(DiskPoolTitle)
        print(TopTitleLine)

        DiskTitleLine = "%s%s" % (" " * 4, "-" * 101)
        for pool in DiskPools:
            _pool = "|%s|%s|%s|%s|" % (pool['Name'].center(25),
                                          str(pool['DiskPoolType']).center(15),
                                          GetReplicationDspType(str(pool['ReplicationType'])).center(17), " " * 43)
            print(_pool)
            print(DiskPoolTitleLine)
            DiskTitle = "%s|%s|%s|%s|%s|%s|%s|%s|" % (' ' * 4, 'Disk'.center(21), 'Server'.center(15),
                                                            'DiskPath'.center(17), 'TotalSize'.center(11),
                                                            'UsedSize'.center(10),
                                                             'RwMode'.center(12),
                                                            'State'.center(7))
            print(DiskTitle)

            if len(pool['DiskList']) > 0:
                print(DiskTitleLine)
                for idx, disk in enumerate(pool['DiskList']):
                    _disk = "%s|%s|%s|%s|%s|%s|%s|%s|" % (
                    ' ' * 4,  disk['DiskName'].center(21), disk['ServerName'].center(15),
                    disk['Path'].center(17), str(Byte2HumanValue(int(disk['TotalSize']), 'TotalSize', Color=False)).center(11),
                    str(Byte2HumanValue(int(disk['UsedSize']), 'UsedSize', Color=False)).center(10),
                    str(disk['RwMode']).center(12),
                    str(disk['State']).center(7))

                    print(_disk)
                    if len(pool['DiskList']) - 1 == idx:
                        print(DiskPoolTitleLine)
                    else:
                        print(DiskTitleLine)
            else:
                print(DiskTitleLine)
                print("%s|%s|" % (' ' * 4, 'No disk data'.center(99)))
                print(DiskPoolTitleLine)

    else:

        if Detail in [DetailInfo, MoreDetailInfo]:
            TopTitleLine = "%s" % ("=" * 173)
            DiskPoolTitleLine = "%s" % ("-" * 173)
            print(TopTitleLine)
            DiskPoolTitle = "|%s|%s|%s|%s|%s|" % ('Name'.center(26),  'PoolId'.center(38), 'DiskPoolType'.center(15), 'ReplicationType'.center(15), " " * 73)
            print(DiskPoolTitle)
            print(TopTitleLine)

            DiskTitleLine = "%s%s" % (" " * 27, "-" * 146)
            for pool in DiskPools:
                _pool = "|%s|%s|%s|%s|%s|" % (pool['Name'].center(26), str(pool['Id']).center(38),
                                               str(pool['DiskPoolType']).center(15), GetReplicationDspType(str(pool['ReplicationType'])).center(15), " " * 73)
                print(_pool)
                print(DiskPoolTitleLine)
                DiskTitle = "%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|" % (' ' * 27, 'HostName'.center(15), '' 'DiskName'.center(20),
                                            'DiskPath'.center(20), 'TotalSize'.center(9), 'UsedSize'.center(8),
                                                          'Read'.center(6), 'Write'.center(6), 'RwMode'.center(6), 'State'.center(7), 'DiskId'.center(38))
                print(DiskTitle)
                if len(pool['DiskList']) > 0:
                    print(DiskTitleLine)
                    for idx, disk in enumerate(pool['DiskList']):
                        _disk = "%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|" % (' ' * 27, disk['ServerName'].center(15), disk['DiskName'].center(20),
                                                  disk['Path'].center(20), str(Byte2HumanValue(int(disk['TotalSize']), 'TotalSize')).center(9),
                                                  str(Byte2HumanValue(int(disk['UsedSize']), 'UsedSize')).center(8), str(Byte2HumanValue(int(disk['Read']), 'DiskRw')).center(6),
                                                  str(Byte2HumanValue(int(disk['Write']), 'DiskRw')).center(6), DisplayDiskMode(disk['RwMode']).center(6), DisplayDiskState(disk['State']).center(7), disk['DiskId'].center(38))

                        print(_disk)
                        if len(pool['DiskList']) - 1 == idx:
                            print(DiskPoolTitleLine)
                        else:
                            print(DiskTitleLine)
                else:
                    print(DiskTitleLine)
                    print("%s|%s|" % (' ' * 27, 'No disk data'.center(144)))
                    print(DiskPoolTitleLine)

        elif Detail == SimpleInfo:
            TopTitleLine = "%s" % ("=" * 135)
            DiskPoolTitleLine = "%s" % ("-" * 135)
            print(TopTitleLine)
            DiskPoolTitle = "|%s|%s|%s|%s|%s|" % ('Name'.center(26),  'PoolId'.center(38), 'DiskPoolType'.center(15), 'ReplicationType'.center(15), " " * 35)
            print(DiskPoolTitle)
            print(TopTitleLine)

            DiskTitleLine = "%s%s" % (" " * 28, "-" * 107)
            for pool in DiskPools:
                _pool = "|%s|%s|%s|%s|%s|" % (pool['Name'].center(26), str(pool['Id']).center(38),
                                               str(pool['DiskPoolType']).center(15), GetReplicationDspType(str(pool['ReplicationType'])).center(15), " " * 35)
                print(_pool)
                print(DiskPoolTitleLine)
                DiskTitle = "%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|" % (' ' * 27, 'HostName'.center(16), '' 'DiskName'.center(20),
                                            'DiskPath'.center(20), 'TotalSize'.center(9), 'UsedSize'.center(8),
                                                          'Read'.center(6), 'Write'.center(6), 'RwMode'.center(6), 'State'.center(7))
                print(DiskTitle)

                if len(pool['DiskList']) > 0:
                    print(DiskTitleLine)
                    for idx, disk in enumerate(pool['DiskList']):
                        _disk = "%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|" % (' ' * 27, disk['ServerName'].center(16), disk['DiskName'].center(20),
                                                  disk['Path'].center(20), str(Byte2HumanValue(int(disk['TotalSize']), 'TotalSize')).center(9),
                                                  str(Byte2HumanValue(int(disk['UsedSize']), 'UsedSize')).center(8), str(Byte2HumanValue(int(disk['Read']), 'DiskRw')).center(6),
                                                  str(Byte2HumanValue(int(disk['Write']), 'DiskRw')).center(6), DisplayDiskMode(disk['RwMode']).center(6), DisplayDiskState(disk['State']).center(7))

                        print(_disk)
                        if len(pool['DiskList']) - 1 == idx:
                            print(DiskPoolTitleLine)
                        else:
                            print(DiskTitleLine)
                else:
                    print(DiskTitleLine)
                    print("%s|%s|" % (' ' * 28, 'No disk data'.center(105)))
                    print(DiskPoolTitleLine)

        else:
            PoolTitleLine = '%s' % ('=' * 93)
            PoolDataLine = '%s' % ('-' * 93)
            DiskPoolTitle = "|%s|%s|%s|%s|" % ('Name'.center(20),  'PoolId'.center(38), 'DiskPoolType'.center(15), 'ReplicationType'.center(15))
            print(PoolTitleLine)
            print(DiskPoolTitle)
            print(PoolTitleLine)

            if len(DiskPoolList) > 0:
                for pool in DiskPoolList:
                    _pool = "|%s|%s|%s|%s|" % (pool.Name.center(20), str(pool.Id).center(38),
                                               str(pool.DiskPoolType).center(15), GetReplicationDspType(str(pool.ReplicationType)).center(15))
                    print(_pool)
                    print(PoolDataLine)
            else:
                print('No diskpool data')
                print(PoolDataLine)


def GetReplicationDspType(StringType):
    if StringType == 'OnePlusOne':
        return '1+1'
    elif StringType == 'OnePlusZero':
        return '1+0'
    elif StringType == 'OnePlusTwo':
        return '1+2'
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
        if options.RepType not in ['1', '2', 'ec'] or options.DiskpoolType.lower() not in [DiskPoolClassStandard.lower(), DiskPoolClassArchive.lower()]:
            Parser.print_help()
            sys.exit(-1)
        if options.RepType == '1':
            ReplicationType = DiskPoolReplica1
        elif options.RepType == '2':
            ReplicationType = DiskPoolReplica2
        else:
            #ReplicationType = DiskPoolReplica1
            print('Not supported yet!')
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

    elif Action.lower() in ['update']:
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

        Res, Errmsg, Ret = UpdateDiskPool(PortalIp, PortalPort, PortalApiKey, AddDiskIds=AddDiskIds, DelDiskIds=DelDiskIds,
                                           DiskPoolName=options.DiskpoolName, logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)
    elif Action.lower() == 'list':
        while True:
            Res, Errmsg, Ret, DiskPoolList = GetDiskPoolInfo(PortalIp, PortalPort, PortalApiKey, logger=logger)
            if Res != ResOk:
                print(Errmsg)
            else:
                if options.MoreDetail:
                    Detail = MoreDetailInfo
                elif options.Detail:
                    Detail = DetailInfo
                else:
                    Detail = SimpleInfo

                Res, Errmsg, Ret, ServerDetailInfo = GetAllServerDetailInfo(PortalIp, PortalPort, PortalApiKey, logger=logger)
                if Res != ResOk:
                    print(Errmsg)
                else:
                    ShowDiskPoolInfo(DiskPoolList, ServerDetailInfo, Detail=Detail)
            if options.Continue is None:
                break
            else:
                time.sleep(int(options.Continue))
    else:
        Parser.print_help()

