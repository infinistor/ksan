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
from common.shcommand import *
from common.utils import Byte2HumanValue, DisplayDiskState, DisplayDiskMode
from const.disk import AddDiskObject, UpdateDiskSizeObject, DiskDetailModule, AllDiskItemsDetailModule
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
    if os.path.exists(Path + DiskIdFileName):
        return False, ResDiskAlreadyExists
    with open(Path + DiskIdFileName, 'w') as f:
        f.write(DiskId)

    return True, ''


@catch_exceptions()
def AddDisk(Ip, Port, ApiKey, Path, DiskName, ServerId=None, ServerName=None, DiskPoolId='',logger=None):

    if ServerId is not None:
        TargetServer = ServerId
    elif ServerName is not None:
        TargetServer = ServerName
    else:
        return ResInvalidCode, ResInvalidMsg + ErrMsgServerNameMissing, None

    Url = '/api/v1/Disks/%s' % TargetServer
    disk = AddDiskObject()
    disk.Set(DiskName, Path, DiskStop, 0, 0, 0, 0, 0, 0, DiskModeRw, DiskPoolId=DiskPoolId)
    body = jsonpickle.encode(disk, make_refs=False)
    Conn = RestApi(Ip, Port, Url, authkey=ApiKey, params=body, logger=logger)
    Res, Errmsg, Data = Conn.post(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data


@catch_exceptions()
def UpdateDiskInfo(Ip, Port, ApiKey, DiskId=None, DiskPoolId=None, Path=None, Name=None, Description=None, State=None, logger=None):
    Res, Errmsg, Disk, ServerId = GetDiskInfoWithId(Ip, Port, ApiKey, DiskId=DiskId, Name=Name, logger=logger)
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
        return ResInvalidCode, ResInvalidMsg + ErrMsgDiskNameMissing, None

    #Url = '/api/v1/Servers/%s/Disks/%s' % (ServerId, DiskId)
    Url = '/api/v1/Disks/%s' % TargetDisk
    body = jsonpickle.encode(Disk, make_refs=False)
    Conn = RestApi(Ip, Port, Url, authkey=ApiKey, params=body, logger=logger)
    Res, Errmsg, Ret = Conn.put(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    if Res == ResOk:
        return Res, Errmsg, Ret
    else:
        return Res, Errmsg, None


@catch_exceptions()
def RemoveDiskInfo(ip, port, ApiKey, DiskId=None, Name=None, logger=None):
    # get network interface info
    if DiskId is not None:
        TargetDisk = DiskId
    elif Name is not None:
        TargetDisk = Name
    else:
        return ResInvalidCode, ResInvalidMsg + ErrMsgDiskNameMissing, None


    Url = '/api/v1/Disks/%s' % TargetDisk
    Conn = RestApi(ip, port, Url, authkey=ApiKey, logger=logger)
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
def UpdateDiskSize(Ip, Port,  ApiKey, DiskId, TotalSize=None, UsedSize=None,
                   TotalInode=None, UsedInode=None, logger=None):
    Res, Errmgs, Ret, Disk = GetDiskInfo(Ip, Port, ApiKey, DiskId=DiskId, logger=logger)
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
            Conn = RestApi(Ip, Port, Url, authkey=ApiKey, params=body, logger=logger)
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
def ChangeDiskMode(Ip, Port, ApiKey, RwMode, DiskId=None, Name=None, logger=None):

    if DiskId is not None:
        TargetDisk = DiskId
    elif Name is not None:
        TargetDisk = Name
    else:
        return ResInvalidCode, ResInvalidMsg + ErrMsgDiskNameMissing, None

    if RwMode not in [DiskModeRw, DiskModeRo]:
        return ResInvalidCode, ResInvalidMsg, None
    Url = '/api/v1/Disks/%s/RwMode/%s' % \
          (TargetDisk, RwMode)
    Conn = RestApi(Ip, Port, Url, authkey=ApiKey, logger=logger)
    Res, Errmsg, Ret = Conn.put()
    return Res, Errmsg, Ret


@catch_exceptions()
def StartStopDisk(Ip, Port, ApiKey, Action, DiskId=None, Name=None, logger=None):

    if DiskId is not None:
        TargetDisk = DiskId
    elif Name is not None:
        TargetDisk = Name
    else:
        return ResInvalidCode, ResInvalidMsg, None

    Url = '/api/v1/Disks/%s/State/%s' % \
          (TargetDisk, Action)
    Conn = RestApi(Ip, Port, Url, authkey=ApiKey, logger=logger)
    Res, Errmsg, Ret = Conn.put()
    return Res, Errmsg, Ret


def GetDiskInfo(Ip, Port, ApiKey, DiskId=None, Name=None, logger=None):
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
    Conn = RestApi(Ip, Port, Url, authkey=ApiKey, params=Params, logger=logger)

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
def GetDiskInfoWithId(Ip, Port, ApiKey, DiskId=None, Name=None, logger=None):

    Res, Errmsg, Ret, Servers = GetAllServerDetailInfo(Ip, Port, ApiKey, logger=logger)
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


@catch_exceptions()
def ShowDiskInfo(DiskList, ServerId=None, DiskId=None, Detail=None, Continue=None):
    """
    Display Disk list
    :param DiskList: DiskItems object list
    :param ServerId:
    :param DiskId:
    :param Detail:
    :return:
    """

    if Detail is DetailInfo:
        DiskTitleLine = '%s' % ('=' * 316)
        DiskDataLine = '%s' % ('-' * 316)
        title = "|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|" % ('ServerName'.center(15), 'DiskName'.center(15), 'Path'.center(15),
               'TotalSize'.center(9), 'UsedSize'.center(8), 'FreeSize'.center(8),
                'TotalInode'.center(20), 'UsedInode'.center(20), 'ReservedInode'.center(20), 'Read'.center(6), 'Write'.center(6),
                    'RwMode'.center(6), 'DiskId'.center(38), 'DiskPoolId'.center(38), 'State'.center(7))  # 'ModeDate'.center(20), 'ModId'.center(20), 'ModName'.center(20), 'Id'.center(30))
    elif Detail is SimpleInfo:
        DiskTitleLine = '%s' % ('=' * 106)
        DiskDataLine = '%s' % ('-' * 106)
        title = "|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|" % ('ServerName'.center(15), 'DiskName'.center(15), 'Path'.center(15),
                'TotalSize'.center(9), 'UsedSize'.center(8), 'FreeSize'.center(8),
                'Read'.center(6), 'Write'.center(6),
                    'RwMode'.center(6), 'State'.center(7))  # 'ModeDate'.center(20), 'ModId'.center(20), 'ModName'.center(20), 'Id'.center(30))
    else:
        DiskTitleLine = '%s' % ('=' * 64)
        DiskDataLine = '%s' % ('-' * 64)
        title = "|%s|%s|%s|%s|%s|" % ('ServerName'.center(15), 'DiskName'.center(15), 'Path'.center(15), 'RwMode'.center(6), 'State'.center(7))
    print(DiskTitleLine)
    print(title)
    print(DiskTitleLine)

    for disk in DiskList:

        #if DiskId is not None and disk.Id != DiskId:
        #    continue
        #    svr = GetDataFromBody(disk.Server, ServerItemsModule)
        #if ServerId is not None and ServerId != svr.Id:
        #    continue
        if Detail == DetailInfo:
            _dsp = "|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|" % (disk.Server.Name.center(15), disk.Name.center(15),
                                       '{:15.15}'.format(disk.Path.center(15)),
                                        str(Byte2HumanValue(int(disk.TotalSize), 'TotalSize')).center(9),
                                       str(Byte2HumanValue(int(disk.UsedSize), 'UsedSize')).center(8), str(Byte2HumanValue(int(disk.TotalSize - disk.UsedSize - disk.ReservedSize), 'FreeSize')).center(8),
                                        str(int(disk.TotalInode)).center(20), str(int(disk.UsedInode)).center(20), str(int(disk.ReservedInode)).center(20),
                                        str(Byte2HumanValue(disk.Read, 'DiskRW')).center(6), str(Byte2HumanValue(disk.Write, 'DiskRw')).center(6),
                                        DisplayDiskMode(disk.RwMode).center(6),str(disk.Id).center(38) ,str(disk.DiskPoolId).center(38), DisplayDiskState(disk.State).center(7))  # svr.ModDate.center(20), svr.ModId.center(20), svr.ModName.center(20), svr.Id.center(30))
        elif Detail == SimpleInfo:
            _dsp = "|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|" % (disk.Server.Name.center(15), disk.Name.center(15),
                                       '{:15.15}'.format(disk.Path.center(15)),
                                        str(Byte2HumanValue(int(disk.TotalSize), 'TotalSize')).center(10),
                                       str(Byte2HumanValue(int(disk.UsedSize), 'UsedSize')).center(10), str(Byte2HumanValue(int(disk.TotalSize - disk.UsedSize - disk.ReservedSize), 'FreeSize')).center(10),
                                        str(Byte2HumanValue(disk.Read, 'DiskRw')).center(6), str(Byte2HumanValue(disk.Write, 'DiskRw')).center(6),
                                        DisplayDiskMode(disk.RwMode).center(6), DisplayDiskState(disk.State).center(7))  # svr.ModDate.center(20), svr.ModId.center(20), svr.ModName.center(20), svr.Id.center(30))
        else:

            _dsp = "|%s|%s|%s|%s|%s|" % (disk.Server.Name.center(15), disk.Name.center(15),
                                             disk.Path.center(15), DisplayDiskMode(disk.RwMode).center(6),
                                             DisplayDiskState(disk.State).center(7))

        print(_dsp)
        print(DiskDataLine)


def DiskUtilHandler(Conf, Action, Parser, logger):

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
        if not (options.ServerName and options.DiskPath and options.DiskName) :
            Parser.print_help()
            sys.exit(-1)
        Res, Errmsg, Ret = AddDisk(PortalIp, PortalPort, PortalApiKey, options.DiskPath, options.DiskName,
                                   ServerName=options.ServerName, logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)
    elif Action.lower() == 'remove':
        if not options.DiskName:
            Parser.print_help()
            sys.exit(-1)
        Res, Errmsg, Ret = RemoveDiskInfo(PortalIp, PortalPort, PortalApiKey, Name=options.DiskName, logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)
    elif Action.lower() == 'update':
        if not options.DiskName:
            Parser.print_help()
            sys.exit(-1)
        Res, Errmsg, Ret = UpdateDiskInfo(PortalIp, PortalPort, PortalApiKey, Path=options.Path,
                                            DiskPoolId=options.DiskPoolId, Name=options.DiskName,
                                          Description=options.Description, logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)


    elif Action.lower() == 'update_size':
        if not options.DiskName:
            Parser.print_help()
            sys.exit(-1)
        Res, Errmsg, Ret = UpdateDiskSize(PortalIp, PortalPort, PortalApiKey, options.DiskName,
                                            TotalSize=options.TotalSize, UsedSize=options.TotalSize, logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)

    elif Action.lower().startswith('set'):
        if len(args) != 2 or not (options.DiskName and options.Mode):
            Parser.print_help()
            sys.exit(-1)
        else:
            if options.Mode.lower() not in ['ro', 'readonly', 'rw', 'readwrite']:
                Parser.print_help()
                sys.exit(-1)
            DiskMode = DiskModeRo if options.Mode in ['ro', 'readonly'] else DiskModeRw
            Res, Errmsg, Ret = ChangeDiskMode(PortalIp, PortalPort,PortalApiKey, DiskMode,
                                              Name=options.DiskName, logger=logger)
            if Res == ResOk:
                print(Ret.Result, Ret.Message)
            else:
                print(Errmsg)

    elif Action.lower() == 'start' or Action.lower() == 'stop':
        if not options.DiskName or Action.lower() not in ['start', 'stop']:
            Parser.print_help()
            sys.exit(-1)
        Action = DiskStart if Action.lower() == 'start' else DiskStop
        Res, Errmsg, Ret = StartStopDisk(PortalIp, PortalPort, PortalApiKey, Action, Name=options.DiskName,
                                         logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)
    elif Action.lower() == 'list':
        while True:
            Res, Errmsg, Ret, AllDisks = GetDiskInfo(PortalIp, PortalPort, PortalApiKey, logger=logger)
            if Res != ResOk:
                print(Errmsg)
            else:
                if options.Detail:
                    Detail = DetailInfo
                elif options.Simple:
                    Detail = SimpleInfo
                else:
                    Detail = None
                ShowDiskInfo(AllDisks, Detail=Detail, Continue=options.Continue)
            if options.Continue is None:
                break
            else:
                time.sleep(int(options.Continue))
    else:
        Parser.print_help()
