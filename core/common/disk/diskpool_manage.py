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
from ksan.common.httpapi import *
import jsonpickle

@catch_exceptions()
def AddDiskPool(Ip, Port, Name, Description=None, logger=None):
    pool = RequestDiskPool()
    DefaultDiskIds = list()
    pool.Set(Name, Description, DefaultDiskIds)

    Url = '/api/v1/DiskPools'
    ReturnType = ResponseHeaderModule
    body = jsonpickle.encode(pool, make_refs=False)
    Conn = RestApi(Ip, Port, Url, params=body, logger=logger)
    Res, Errmsg, Data = Conn.post(ItemsHeader=False, ReturnType=ReturnType)
    return Res, Errmsg, Data


@catch_exceptions()
def RemoveDiskPool(ip, port, DiskPoolId, logger=None):
    # get network interface info
    Url = '/api/v1/DiskPools/%s' % DiskPoolId
    ReturnType = ResponseHeaderModule
    Conn = RestApi(ip, port, Url, logger=logger)
    Res, Errmsg, Ret = Conn.delete(ItemsHeader=False, ReturnType=ReturnType)
    return Res, Errmsg, Ret


def GetDiskIdList(PoolDetailInfo):
    DiskListOfPool = list()
    for disk in PoolDetailInfo.Disks:
        DiskListOfPool.append(disk.Id)
    return DiskListOfPool


@catch_exceptions()
def UpdateDiskPool(Ip, Port, PoolId, AddDiskIds=None, DelDiskIds=None, Name=None, Description='', logger=None):
    Res, Errmsg, Ret, PoolDetail = GetDiskPoolInfo(Ip, Port, PoolId=PoolId, logger=logger)
    if Res == ResOk:
        if Ret.Result != ResultSuccess:
            return Res, Errmsg, Ret, None
    DiskIdListOfPool = GetDiskIdList(PoolDetail)
    if AddDiskIds is not None:
        DiskIdListOfPool = DiskIdListOfPool + AddDiskIds
    if DelDiskIds is not None:
        for DiskId in DelDiskIds:
            DiskIdListOfPool.remove(DiskId)
    if Name is not None:
        PoolDetail.Name = Name

    if Description is not '':
        PoolDetail.Description = Description
    Url = '/api/v1/DiskPools/%s' % PoolId
    pool = RequestDiskPool()
    pool.Set(PoolDetail.Name, PoolDetail.Description, DiskIdListOfPool)
    ReturnType = ResponseHeaderModule
    body = jsonpickle.encode(pool, make_refs=False)
    Conn = RestApi(Ip, Port, Url, params=body, logger=logger)
    Res, Errmsg, Data = Conn.put(ItemsHeader=False, ReturnType=ReturnType)
    return Res, Errmsg, Data


def GetDiskPoolInfo(Ip, Port, PoolId=None, logger=None):
    """
    Get All Disk Info with Server Info
    :param Ip:
    :param Port:
    :param Disp:
    :param logger:
    :return:
    """
    if PoolId is not None:
        Url = "/api/v1/DiskPools/%s" % PoolId
        ReturnType = DiskPoolDetailModule
        ItemsHeader = False
    else:
        Url = "/api/v1/DiskPools"
        ReturnType = DiskPoolItemsModule
        ItemsHeader = True

    Params = dict()
    Params['countPerPage'] = 100
    Conn = RestApi(Ip, Port, Url, params=Params, logger=logger)

    Res, Errmsg, Ret = Conn.get(ItemsHeader=ItemsHeader, ReturnType=ReturnType)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            if PoolId is not None:
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
def ShowDiskPoolInfo(DiskPoolList, Detail=False):
    """
    Display Disk list
    :param DiskList: DiskItems object list
    :param ServerId:
    :param DiskId:
    :param Detail:
    :return:
    """
    if Detail is False:
        PoolTitleLine = '%s' % ('=' * 82)
        PoolDataLine = '%s' % ('-' * 82)
        title = "|%s|%s|%s|" % ('Name'.center(20),  'PoolId'.center(38), 'Descrition'.center(20))
        print(PoolTitleLine)
        print(title)
        print(PoolTitleLine)

        for pool in DiskPoolList:
            _pool = "|%s|%s|%s|" % (pool.Name.center(20), str(pool.Id).center(38), str(pool.Description).center(20))

            print(_pool)
            print(PoolDataLine)
    else:
        TopTitleLine = "%s" % ("=" * 98)
        DiskPoolTitleLine = "%s" % ("-" * 98)
        print(TopTitleLine)
        DiskPoolTitle = "|%s|%s|%s|" % ('Name'.center(20),  'PoolId'.center(38), 'Descrition'.center(36))
        print(DiskPoolTitle)
        print(TopTitleLine)

        DiskTitleLine = "%s%s" % (" " * 21, "-" * 77)
        for pool in DiskPoolList:
            _pool = "|%s|%s|%s|" % ( pool.Name.center(20), str(pool.Id).center(38), str(pool.Description).center(36))
            print(_pool)
            #title = "%s%s%s%s%s%s%s%s" % ('VolumeId'.center(40), 'VolumeName'.center(20), 'State'.center(10),
            #                    'TotalSize'.center(20), 'UsedSize'.center(20),'UsedInode'.center(20),
            #                              'ReplicaType'.center(10), 'Permission'.center(10))
            #print(title)
            #for volume in pool.Volumes:
            #    _volume = "%s%s%s%s%s%s%s%s" % (volume.Id.center(40), volume.Name.center(20), volume.State.center(10),
            #                                str(volume.TotalSize).center(20), str(volume.UsedSize).center(20),
            #                                    str(volume.UsedInode).center(20), volume.ReplicationType.center(10),
            #                                    volume.Permission.center(10))
            #    print(_volume)

            print(DiskPoolTitleLine)
            DiskTitle = "%s|%s|%s|" % (' ' * 21, 'DiskId'.center(38), 'DiskPath'.center(36))
            print(DiskTitle)
            if len(pool.Disks) > 0:
                print(DiskTitleLine)
            else:
                print(DiskPoolTitleLine)

            for idx, disk in enumerate(pool.Disks):
                _disk = "%s|%s|%s|" % (' ' * 21, disk.Id.center(38), disk.Path.center(36))
                print(_disk)
                if len(pool.Disks) -1 == idx:
                    print(DiskPoolTitleLine)
                else:
                    print(DiskTitleLine)

