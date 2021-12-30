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
from ksan.common.display import *
from ksan.common.shcommand import *
from ksan.server.server_manage import *
from optparse import OptionParser
import re
import psutil
import jsonpickle

ParsingDiskInode = re.compile("([\d\w_\-/]+)[\s]+([\d]+)[\s]+([\d]+)[\s]+([\d]+)[\s]+([\d])\%")


class Disk(AddDiskObject):
    def __init__(self, Path):
        super().__init__()
        self.Path = Path

    @catch_exceptions()
    def GetInode(self):
        inode_cmd = 'df -i %s' % self.Path
        out, err = shcall(inode_cmd)
        if err:
            return ResEtcErrorCode, err
        disk_stat = ParsingDiskInode.search(out)
        if disk_stat:
            (DevName, TotalInode, UsedInode, FreeInode, Percentage) = disk_stat.groups()
            self.TotalInode = int(TotalInode)
            self.UsedInode = int(UsedInode)
            self.ReservedInode = int(TotalInode) - int(UsedInode) - int(FreeInode)
            return ResOk, ''
        else:
            return ResNotFoundCode, ResNotFoundMsg

    @catch_exceptions()
    def GetUsage(self):
        if not os.path.exists(self.Path):
            return ResNotFoundCode, ResNotFoundMsg
        disk_stat = psutil.disk_usage(self.Path)
        self.TotalSize = disk_stat.total
        self.UsedSize = disk_stat.used
        self.ReservedSize = disk_stat.total - disk_stat.used - disk_stat.free
        return ResOk, ''


def CheckDiskMount(Path):
    if os.path.exists(Path):
        return True
    else:
        return False


def WriteDiskId(Path, DiskId):
    if os.path.exists(Path + '/DiskId'):
        return False, 'Alread Exists'
    with open(Path + '/DiskId', 'w') as f:
        f.write(DiskId)

    return True, ''



@catch_exceptions()
def get_disk_info(ip, port, ServerId, DiskId=None, disp=False, logger=None):
    """
    get server info all or specific server info with Id
    :param ip:
    :param port:
    :param ServerId
    :param NicId
    :param disp:
    :param logger
    :return:
    """
    if ServerId is None:
        return ResInvalidCode, ResInvalidMsg + 'Serverid is required', None
    if DiskId is not None:
        Url = "/api/v1/Servers/%s/Disks/%s" % (ServerId, DiskId)
    else:
        Url = "/api/v1/Servers/%s/Disks" % ServerId
    Params = dict()
    Params['countPerPage'] = 100
    Conn = RestApi(ip, port, Url, params=Params, logger=logger)
    Res, Errmsg, Data = Conn.get()
    if Res == ResOk:
        Ret = Conn.parsing_result(Data)
        if disp is True:
            if Ret.Header.Result == ResultSuccess:
                if Ret.ItemHeader is None:
                    disp_disk_info(Ret.Data, DiskId=DiskId)
                else:
                    disp_disk_info(Ret.ItemHeader)
        return Res, Errmsg, Ret
    else:
        return Res, Errmsg, None


@catch_exceptions()
def AddDisk(Ip, Port, ServerId, Path, logger=None):
    disk = AddDiskObject()
    disk.Set(Path, 'Stop', 0, 0, 0, 0, 0, 0, 'ReadWrite')
    Url = '/api/v1/Servers/%s/Disks' % ServerId
    body = jsonpickle.encode(disk, make_refs=False)
    Conn = RestApi(Ip, Port, Url, params=body, logger=logger)
    Res, Errmsg, Data = Conn.post(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data


@catch_exceptions()
def UpdateDiskInfo(Ip, Port, DiskId, DiskPoolId=None, Path=None, Name=None, Description=None, State=None, logger=None):
    Res, Errmsg, Disk, ServerId = GetDiskInfoWithId(Ip, Port, DiskId, logger=logger)
    if Res != ResOk:
        return Res, Errmsg, None

    if DiskPoolId is not None:
        Disk.DiskPoolId = DiskPoolId
    if Path is not None:
        Disk.Path = Path
    if State is not None:
        Disk.State = State
    if Description is not None:
        Disk.Description = Description
    if Name is not None:
        Disk.Name = Name

    #Url = '/api/v1/Servers/%s/Disks/%s' % (ServerId, DiskId)
    Url = '/api/v1/Servers/%s/Disks/%s' % (ServerId, DiskId)
    body = jsonpickle.encode(Disk, make_refs=False)
    Conn = RestApi(Ip, Port, Url, params=body, logger=logger)
    Res, Errmsg, Ret = Conn.put(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    if Res == ResOk:
        return Res, Errmsg, Ret
    else:
        return Res, Errmsg, None


@catch_exceptions()
def RemoveDiskInfo(ip, port, ServerId, DiskId, logger=None):
    # get network interface info
    if not (ServerId and DiskId):
        return ResInvalidCode, ResInvalidMsg + 'Server id and Disk id are required', None
    Url = '/api/v1/Servers/%s/Disks/%s' % (ServerId, DiskId)
    Conn = RestApi(ip, port, Url, logger=logger)
    Res, Errmsg, Ret = Conn.delete()
    return Res, Errmsg, Ret


@catch_exceptions()
def update_disk_state(Ip, Port, ServerId, DiskId, State, logger=None):

    Url = '/api/v1/Servers/%s/Disks/%s/State/%s' % (ServerId, DiskId, State)
    Conn = RestApi(Ip, Port, Url, logger=logger)
    Ret, Errmsg, Data = Conn.put()
    if Ret == ResOk:
        Header, Itemheader, Data = Conn.parsing_result(Data)
        return Ret, Errmsg, Header
    else:
        return Ret, Errmsg, None


@catch_exceptions()
def update_disk_hastate(Ip, Port, ServerId, DiskId, HaAction, logger=None):

    Url = '/api/v1/Servers/%s/Disks/%s/HaAction/%s' % (ServerId, DiskId, HaAction)
    Conn = RestApi(Ip, Port, Url, logger=logger)
    Ret, Errmsg, Data = Conn.put()
    if Ret == ResOk:
        Header, Itemheader, Data = Conn.parsing_result(Data)
        return Ret, Errmsg, Header
    else:
        return Ret, Errmsg, None


@catch_exceptions()
def UpdateDiskSize(Ip, Port, ServerId, DiskId, TotalSize=None, UsedSize=None,
                   TotalInode=None, UsedInode=None, logger=None):
    Res, Errmgs, Ret, Disk = GetDiskInfo(Ip, Port, ServerId, DiskId=DiskId, logger=logger)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            Disk = Disk[0]
            NewDisk = UpdateDiskSizeObject()
            NewDisk.Set(Disk.Id, Disk.ServerId, Disk.DiskNo, Disk.TotalInode, Disk.ReservedInode, Disk.UsedInode,
                        Disk.TotalSize, Disk.ReservedSize, Disk.UsedSize)
            if TotalSize is not None:
                NewDisk.TotalSize = TotalSize
            if UsedSize is not None:
                NewDisk.UsedSize = UsedSize
            if TotalInode is not None:
                NewDisk.TotalInode = TotalInode
            if UsedInode is not None:
                NewDisk.UsedInode = UsedInode

            Url = '/api/v1/Disks/Size'
            body = jsonpickle.encode(NewDisk, make_refs=False)
            Conn = RestApi(Ip, Port, Url, params=body, logger=logger)
            res, errmsg, ret = Conn.put(ItemsHeader=False, ReturnType=ResponseHeaderModule)
            if res == ResOk:
                return res, errmsg, ret
            else:
                return res, errmsg, None
        else:
            return Ret, Errmgs, Ret
    else:
        return Ret, Errmgs, None


@catch_exceptions()
def ChangeDiskMode(Ip, Port, ServerId, DiskId, RwMode, logger=None):

    if RwMode not in [DiskModeRw, DiskModeRd]:
        return ResInvalidCode, ResInvalidMsg, None
    Url = '/api/v1/Servers/%s/Disks/%s/RwMode/%s' % \
          (ServerId, DiskId, RwMode)
    Conn = RestApi(Ip, Port, Url, logger=logger)
    Res, Errmsg, Ret = Conn.put()
    return Res, Errmsg, Ret


@catch_exceptions()
def StartStopDisk(Ip, Port, ServerId, DiskId, Action, logger=None):

    Url = '/api/v1/Servers/%s/Disks/%s/State/%s' % \
          (ServerId, DiskId, Action)
    Conn = RestApi(Ip, Port, Url, logger=logger)
    Res, Errmsg, Ret = Conn.put()
    return Res, Errmsg, Ret


def GetDiskInfo(Ip, Port, ServerId=None, DiskId=None, logger=None):
    """
    Get All Disk Info with Server Info
    :param Ip:
    :param Port:
    :param Disp:
    :param logger:
    :return:
    """
    ItemsHeader = True
    if DiskId is not None:
        Url = "/api/v1/Disks/%s/%s" % (ServerId, DiskId)
        ReturnType = DiskDetailModule
        ItemsHeader = False
    else:
        Url = "/api/v1/Disks"
        ReturnType = AllDiskItemsDetailModule

    Params = dict()
    Params['countPerPage'] = 100
    Conn = RestApi(Ip, Port, Url, params=Params, logger=logger)

    Res, Errmsg, Ret= Conn.get(ItemsHeader=ItemsHeader, ReturnType=ReturnType)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            if DiskId is not None:
                return Res, Errmsg, Ret, [Ret.Data]
            else:
                return Res, Errmsg, Ret, Ret.Data.Items
        else:
            return Res, Errmsg, Ret, None
    else:
        return Res, Errmsg, None, None


@catch_exceptions()
def GetDiskInfoWithId(Ip, Port, DiskId, logger=None):
    AllServersDetail = list()
    Res, Errmsg, Ret, Servers = GetAllServerDetailInfo(Ip, Port, logger=None)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            for Svr in Servers:
                for Disk in Svr.Disks:
                    if Disk.Id == DiskId:
                        return ResOk, Errmsg, Disk, Svr.Id
            return ResNotFoundCode, ResNotFoundMsg, None, None
        else:
            return Ret.Code, Ret.Message, None, None
    else:
        return Res, Errmsg, None, None


@catch_exceptions()
def GetDiskInfoPerServer(Ip, Port, ServerId=None, logger=None):
    """
    get all disk info per server.
    :param Ip:
    :param Port:
    :param ServerId: server id used to get specific server's disk
    :param logger:
    :return: tuple. Res, Errmsg, AllDiskInfoPerServerList(DiskInfoPerServer Object list)
    """
    AllDiskInfoPerServerList = list()
    Res, Errmsg, Ret = GetServerInfo(Ip, Port, logger=logger)
    if Res == ResOk and Ret.Header.Result == ResultSuccess:
        for svr in Ret.ItemHeader.Items:
            _svr = ServerItems()
            _svr.Set(svr)
            if ServerId is not None:
                if _svr.Id != ServerId:
                    continue

            TmpServerDict = DiskInfoPerServer(_svr.Id, _svr.Name)
            Res, Errmsg, Ret = get_disk_info(Ip, Port, _svr.Id, logger=logger)
            if Res == ResOk:
                if Ret.Header.Result == ResultSuccess:
                    for disk in Ret.ItemHeader.Items:
                        _disk = DiskItems(disk)
                        _disk.Set(disk)
                        TmpServerDict.DiskList.append(_disk)
                else:
                    TmpServerDict.Res = Ret.Header.Result
            else:
                TmpServerDict.Res = Res

            AllDiskInfoPerServerList.append(TmpServerDict)

    return Res, Errmsg, AllDiskInfoPerServerList


@catch_exceptions()
def GetServerInfoWithDiskId(AllDiskList, DiskId):
    """
    get Server info with Disk Id
    :param AllDiskInfoPerServerList:
    :return: ServerItems Object
    """
    for disk in AllDiskList:
        if disk.Id == DiskId:
            return ResOk, '', disk.Server

    return ResNotFoundCode, ResNotFoundMsg, None


def GetDiskIdWithdiskNo(Ip, Port, DiskNo, logger=None):
    """
    Get Disk Id(uuid) and Disk ServerId with DiskNo value(uuid hash)
    :param DiskNo:
    :param AllDiskInfoPerServerList:
    :return: Tuple, Res, Errmsg, DiskItems Object
    """
    Url = "/api/v1/Disks/Find/%s" % DiskNo
    Params = dict()
    Params['countPerPage'] = 100
    Conn = RestApi(Ip, Port, Url, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.get()
    if Res == ResOk:
        if Ret.Header.Result == ResultSuccess:
            Ret = DiskIdServerId(Ret.Data)
            return Res, Errmsg, Ret
        else:
            return ResultFail, Ret.Header.Message, None
    else:
        return Res, Errmsg, None


@catch_exceptions()
def ShowDiskInfo(DiskList, ServerId=None, DiskId=None, Detail=False):
    """
    Display Disk list
    :param DiskList: DiskItems object list
    :param ServerId:
    :param DiskId:
    :param Detail:
    :return:
    """

    if Detail is True:
        DiskTitleLine = '%s' % ('=' * 235)
        DiskDataLine = '%s' % ('-' * 235)
        title = "|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|" % ('ServerName'.center(15),  'DiskId'.center(38), 'Path'.center(15),
               'State'.center(10), 'Total'.center(20), 'Used'.center(20), 'Free'.center(20), 'RwMode'.center(10), 'DiskPoolId'.center(38), 'ServerId'.center(38))  # 'ModeDate'.center(20), 'ModId'.center(20), 'ModName'.center(20), 'Id'.center(30))
    else:
        DiskTitleLine = '%s' % ('=' * 83)
        DiskDataLine = '%s' % ('-' * 83)
        title = "|%s|%s|%s|%s|" % ('ServerName'.center(15), 'DiskId'.center(38), 'Path'.center(15),'State'.center(10))
    print(DiskTitleLine)
    print(title)
    print(DiskTitleLine)

    for disk in DiskList:

        #if DiskId is not None and disk.Id != DiskId:
        #    continue
        #    svr = GetDataFromBody(disk.Server, ServerItemsModule)
        #if ServerId is not None and ServerId != svr.Id:
        #    continue
        if Detail is True:
            _dsp = "|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|" % (disk.Server.Name.center(15),
                                       str(disk.Id).center(38),
                                       '{:15.15}'.format(disk.Path.center(15)),
                                       disk.State.center(10), str(int(disk.TotalSize)).center(20),
                                       str(int(disk.UsedSize)).center(20), str(int(disk.TotalSize - disk.UsedSize - disk.ReservedSize)).center(20),
                                       disk.RwMode.center(10), str(disk.DiskPoolId).center(38), disk.Server.Id.center(38))  # svr.ModDate.center(20), svr.ModId.center(20), svr.ModName.center(20), svr.Id.center(30))
        else:

            _dsp = "|%s|%s|%s|%s|" % (disk.Server.Name.center(15),
                                             str(disk.Id).center(38), disk.Path.center(15),
                                             disk.State.center(10))

        print(_dsp)
        print(DiskDataLine)