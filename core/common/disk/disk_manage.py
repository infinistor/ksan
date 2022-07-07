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
from common.display import *
from common.shcommand import *
from server.server_manage import *
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
def AddDisk(Ip, Port, Path, DiskName, ServerId=None, ServerName=None, DiskPoolId='',logger=None):

    if ServerId is not None:
        TargetServer = ServerId
    elif ServerName is not None:
        TargetServer = ServerName
    else:
        return ResInvalidCode, ResInvalidMsg + ' ServerId or ServerName is required', None

    Url = '/api/v1/Disks/%s' % TargetServer
    disk = AddDiskObject()
    disk.Set(DiskName, Path, 'Stop', 0, 0, 0, 0, 0, 0, 'ReadWrite', DiskPoolId=DiskPoolId)
    body = jsonpickle.encode(disk, make_refs=False)
    Conn = RestApi(Ip, Port, Url, params=body, logger=logger)
    Res, Errmsg, Data = Conn.post(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data


@catch_exceptions()
def UpdateDiskInfo(Ip, Port, DiskId=None, DiskPoolId=None, Path=None, Name=None, Description=None, State=None, logger=None):
    Res, Errmsg, Disk, ServerId = GetDiskInfoWithId(Ip, Port, DiskId=DiskId, Name=Name, logger=logger)
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

    if DiskId is not None:
        TargetDisk = DiskId
    elif Name is not Name:
        TargetDisk = Name
    else:
        return ResInvalidCode, ResInvalidMsg + ' DiskId and Name are all None', None

    #Url = '/api/v1/Servers/%s/Disks/%s' % (ServerId, DiskId)
    Url = '/api/v1/Disks/%s' % TargetDisk
    body = jsonpickle.encode(Disk, make_refs=False)
    Conn = RestApi(Ip, Port, Url, params=body, logger=logger)
    Res, Errmsg, Ret = Conn.put(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    if Res == ResOk:
        return Res, Errmsg, Ret
    else:
        return Res, Errmsg, None


@catch_exceptions()
def RemoveDiskInfo(ip, port, DiskId=None, Name=None, logger=None):
    # get network interface info
    if DiskId is not None:
        TargetDisk = DiskId
    elif Name is not None:
        TargetDisk = Name
    else:
        return ResInvalidCode, ResInvalidMsg + ' Server id and Disk id are required', None


    Url = '/api/v1/Disks/%s' % TargetDisk
    Conn = RestApi(ip, port, Url, logger=logger)
    Res, Errmsg, Ret = Conn.delete()
    return Res, Errmsg, Ret


@catch_exceptions()
def update_disk_state(Ip, Port, ServerId, DiskId, State, logger=None):

    Url = '/api/v1/Disks/%s/%s/State/%s' % (ServerId, DiskId, State)
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
def UpdateDiskSize(Ip, Port,  DiskId, TotalSize=None, UsedSize=None,
                   TotalInode=None, UsedInode=None, logger=None):
    Res, Errmgs, Ret, Disk = GetDiskInfo(Ip, Port,  DiskId=DiskId, logger=logger)
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
def ChangeDiskMode(Ip, Port, RwMode, DiskId=None, Name=None, logger=None):

    if DiskId is not None:
        TargetDisk = DiskId
    elif Name is not None:
        TargetDisk = Name
    else:
        return ResInvalidCode, ResInvalidMsg + ' DiskId and Name are all None', None

    if RwMode not in [DiskModeRw, DiskModeRd]:
        return ResInvalidCode, ResInvalidMsg, None
    Url = '/api/v1/Disks/%s/RwMode/%s' % \
          (TargetDisk, RwMode)
    Conn = RestApi(Ip, Port, Url, logger=logger)
    Res, Errmsg, Ret = Conn.put()
    return Res, Errmsg, Ret


@catch_exceptions()
def StartStopDisk(Ip, Port, Action, DiskId=None, Name=None, logger=None):

    if DiskId is not None:
        TargetDisk = DiskId
    elif Name is not None:
        TargetDisk = Name
    else:
        return ResInvalidCode, ResInvalidMsg, None

    Url = '/api/v1/Disks/%s/State/%s' % \
          (TargetDisk, Action)
    Conn = RestApi(Ip, Port, Url, logger=logger)
    Res, Errmsg, Ret = Conn.put()
    return Res, Errmsg, Ret


def GetDiskInfo(Ip, Port, DiskId=None, Name=None, logger=None):
    """
    Get All Disk Info with Server Info
    :param Ip:
    :param Port:
    :param Disp:
    :param logger:
    :return:
    """
    if DiskId is not None:
        TargetDisk = DiskId
    elif Name is not None:
        TargetDisk = Name
    else:
        TargetDisk = None

    ItemsHeader = True
    if TargetDisk is not None:
        Url = "/api/v1/Disks/%s" % TargetDisk
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
def GetDiskInfoWithId(Ip, Port, DiskId=None, Name=None, logger=None):

    Res, Errmsg, Ret, Servers = GetAllServerDetailInfo(Ip, Port, logger=logger)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            for Svr in Servers:
                for Disk in Svr.Disks:
                    if Disk.Id == DiskId or Disk.Name == Name:
                        return ResOk, Errmsg, Disk, Svr.Id
            return ResNotFoundCode, ResNotFoundMsg, None, None
        else:
            return Ret.Code, Ret.Message, None, None
    else:
        return Res, Errmsg, None, None


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
        DiskTitleLine = '%s' % ('=' * 355)
        DiskDataLine = '%s' % ('-' * 355)
        title = "|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|" % ('ServerName'.center(15), 'DiskName'.center(15), 'DiskId'.center(38), 'Path'.center(15),
               'State'.center(10), 'TotalSize'.center(20), 'UsedSize'.center(20), 'FreeSize'.center(20),
                'TotalInode'.center(20), 'UsedInode'.center(20), 'ReservedInode'.center(20), 'Read'.center(20), 'Write'.center(20),
                    'RwMode'.center(10), 'DiskPoolId'.center(38), 'ServerId'.center(38))  # 'ModeDate'.center(20), 'ModId'.center(20), 'ModName'.center(20), 'Id'.center(30))
    else:
        DiskTitleLine = '%s' % ('=' * 99)
        DiskDataLine = '%s' % ('-' * 99)
        title = "|%s|%s|%s|%s|%s|" % ('ServerName'.center(15), 'DiskName'.center(15), 'DiskId'.center(38), 'Path'.center(15),'State'.center(10))
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
            _dsp = "|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|" % (disk.Server.Name.center(15), disk.Name.center(15),
                                       str(disk.Id).center(38),
                                       '{:15.15}'.format(disk.Path.center(15)),
                                       disk.State.center(10), str(int(disk.TotalSize)).center(20),
                                       str(int(disk.UsedSize)).center(20), str(int(disk.TotalSize - disk.UsedSize - disk.ReservedSize)).center(20),
                                        str(int(disk.TotalInode)).center(20), str(int(disk.UsedInode)).center(20), str(int(disk.ReservedInode)).center(20),
                                        str(disk.Read).center(20), str(disk.Write).center(20),
                                        disk.RwMode.center(10), str(disk.DiskPoolId).center(38), disk.Server.Id.center(38))  # svr.ModDate.center(20), svr.ModId.center(20), svr.ModName.center(20), svr.Id.center(30))
        else:

            _dsp = "|%s|%s|%s|%s|%s|" % (disk.Server.Name.center(15), disk.Name.center(15),
                                             str(disk.Id).center(38), disk.Path.center(15),
                                             disk.State.center(10))

        print(_dsp)
        print(DiskDataLine)
