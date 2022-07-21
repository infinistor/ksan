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
if os.path.dirname(os.path.abspath(os.path.dirname(__file__))) not in sys.path:
    sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from common.httpapi import *
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
    Res, Errmsg, Ret = Conn.post(ItemsHeader=False, ReturnType=ReturnType)
    return Res, Errmsg, Ret




def GetDiskIdList(PoolDetailInfo):
    DiskIdListOfPool = list()
    DiskNameListOfPool = list()
    for disk in PoolDetailInfo.Disks:
        DiskIdListOfPool.append(disk.Id)
        DiskNameListOfPool.append(disk.Name)
    return DiskIdListOfPool, DiskNameListOfPool


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
                res, errmsg, ret, DiskPoolDetail = GetDiskPoolInfo(Ip, Port, PoolId=pool.Id, logger=logger)
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
def ShowDiskPoolInfo(DiskPoolList, ServerDetailInfo, Detail=False):
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
                TmpDisk['DiskId'] = disk.Id
                TmpDisk['ServerName'] = ServerName
                TmpDisk['Path'] = disk.Path
                TmpDisk['TotalSize'] = disk.TotalSize
                TmpDisk['UsedSize'] = disk.UsedSize
                TmpDisk['Read'] = disk.Read
                TmpDisk['Write'] = disk.Write
                pool['DiskList'].append(TmpDisk)


    if Detail is False:
        PoolTitleLine = '%s' % ('=' * 93)
        PoolDataLine = '%s' % ('-' * 93)
        title = "|%s|%s|%s|%s|" % ('Name'.center(20),  'PoolId'.center(38), 'DiskPoolType'.center(15), 'ReplicationType'.center(15))
        print(PoolTitleLine)
        print(title)
        print(PoolTitleLine)

        for pool in DiskPoolList:
            _pool = "|%s|%s|%s|%s|" % (pool.Name.center(20), str(pool.Id).center(38),
                                       str(pool.DiskPoolType).center(15), GetReplicationDspType(str(pool.ReplicationType)).center(15))

            print(_pool)
            print(PoolDataLine)
    else:
        TopTitleLine = "%s" % ("=" * 145)
        DiskPoolTitleLine = "%s" % ("-" * 145)
        print(TopTitleLine)
        DiskPoolTitle = "|%s|%s|%s|%s|%s|" % ('Name'.center(36),  'PoolId'.center(38), 'DiskPoolType'.center(15), 'ReplicationType'.center(15), " " * 35)
        print(DiskPoolTitle)
        print(TopTitleLine)

        DiskTitleLine = "%s%s" % (" " * 10, "-" * 135)
        for pool in DiskPools:
            _pool = "|%s|%s|%s|%s|%s|" % (pool['Name'].center(36), str(pool['Id']).center(38),
                                           str(pool['DiskPoolType']).center(15), GetReplicationDspType(str(pool['ReplicationType'])).center(15), " " * 35)
            print(_pool)
            print(DiskPoolTitleLine)
            DiskTitle = "%s|%s|%s|%s|%s|%s|%s|%s|" % (' ' * 10, 'HostName'.center(15), '' 'DiskId'.center(38),
                                        'DiskPath'.center(20), 'TotalSize'.center(15), 'UsedSize'.center(15),
                                                      'Read'.center(12), 'Write'.center(12))
            print(DiskTitle)
            if len(pool['DiskList']) > 0:
                print(DiskTitleLine)
            else:
                print(DiskPoolTitleLine)

            for idx, disk in enumerate(pool['DiskList']):
                _disk = "%s|%s|%s|%s|%s|%s|%s|%s|" % (' ' * 10, disk['ServerName'].center(15), disk['DiskId'].center(38),
                                          disk['Path'].center(20), str(int(disk['TotalSize'])).center(15),
                                          str(int(disk['UsedSize'])).center(15), str(int(disk['Read'])).center(12),
                                          str(int(disk['Write'])).center(12))

                print(_disk)
                if len(pool['DiskList']) - 1 == idx:
                    print(DiskPoolTitleLine)
                else:
                    print(DiskTitleLine)


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
    PortalIp = Conf.mgs.PortalIp
    PortalPort = Conf.mgs.PortalPort
    PortalApiKey = Conf.mgs.PortalApiKey
    MqPort = Conf.mgs.MqPort
    MqPassword = Conf.mgs.MqPassword

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
    elif Action.lower() == 'set-default':
        if not options.DiskpoolName:
            Parser.print_help()
            sys.exit(-1)
        Res, Errmsg, Ret = SetDefaultDiskPool(PortalIp, PortalPort, PortalApiKey, DiskPoolName=options.DiskpoolName, logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)
    elif Action.lower() == 'get-default':
        Res, Errmsg, Ret = GetDefaultDiskPool(PortalIp, PortalPort, PortalApiKey, logger=logger)
        if Res == ResOk:
            if Ret.Data is not None:
                print(Ret.Data.Name)
            else:
                print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)

    elif Action.lower() in ['add-disk', 'remove-disk', 'update']:
        AddDiskIds = None
        DelDiskIds = None
        if Action.lower() == 'add-disk':
            if not (options.DiskpoolName and options.DiskName):
                Parser.print_help()
                sys.exit(-1)
            if options.DiskName is not None:
                AddDiskIds = options.DiskName.split()
            else:
                Parser.print_help()
                print("Disk Ids or Disk Name is required")
                sys.exit(-1)
        elif Action.lower() == 'remove-disk':
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
        Res, Errmsg, Ret, DiskPoolList = GetDiskPoolInfo(PortalIp, PortalPort, PortalApiKey, logger=logger)
        if Res != ResOk:
            print(Errmsg)
        else:
            Res, Errmsg, Ret, ServerDetailInfo = GetAllServerDetailInfo(PortalIp, PortalPort, PortalApiKey, logger=logger)
            if Res != ResOk:
                print(Errmsg)
            else:
                if options.Detail:
                    ShowDiskPoolInfo(DiskPoolList, ServerDetailInfo, Detail=True)
                else:
                    ShowDiskPoolInfo(DiskPoolList, ServerDetailInfo)
    else:
        Parser.print_help()

