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

# -*- coding: utf-8 -*-
import os, sys
sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from disk.disk_manage import *
from const.common import DiskPoolXmlPath
from const.disk import DiskDetailMqBroadcast
from common.init import GetConf, WaitAgentConfComplete, GetAgentConfig
from const.mq import MqVirtualHost, RoutKeyDiskUsage, ExchangeName, RoutKeyDiskCheckMountFinder, \
    RoutKeyDiskWirteDiskIdFinder, RoutKeyDiskAdded, RoutKeyDiskPoolUpdate, RoutKeyDiskStartStop, RoutKeyDiskState, \
    RoutKeyDiskDel
import mqmanage.mq
import time
import json
import logging
import inspect
import xml.etree.ElementTree as ET

def GetDiskReadWrite(DiskStatInfo, CurrentDiskIo):
    # get disk partition info
    Now = time.time()
    TimeInterval = int(Now - DiskStatInfo['LapTime'])
    DiskStatInfo['LapTime'] = Now

    Device = DiskStatInfo['Device']
    PrevWrite = DiskStatInfo['Write']
    PrevRead = DiskStatInfo['Read']
    if TimeInterval >= 1:
        WritePerSec = (CurrentDiskIo[Device].write_bytes - PrevWrite) / TimeInterval
        ReadPerSec = (CurrentDiskIo[Device].read_bytes - PrevRead) / TimeInterval
        DiskStatInfo['Write'] = CurrentDiskIo[Device].write_bytes
        DiskStatInfo['Read'] = CurrentDiskIo[Device].read_bytes
    else:
        WritePerSec = 0
        ReadPerSec = 0
    return ReadPerSec, WritePerSec


def ReportDiskIo(Conf, DiskStatInfo, GlobalFlag, logger):
    """
    Get Disk IO per seconds
    :param conf:
    :param DiskIoList: list [{'Path':, 'Device':, 'Id':, 'ServerId':, 'State':, 'Read':, 'Write':, 'LapTime':   }]
    :param GlobalFlag:
    :param logger:
    :return:
    """

    Conf = WaitAgentConfComplete(inspect.stack()[1][3], logger)
    conf = GetAgentConfig(Conf)
    MqDiskUpdated = mqmanage.mq.Mq(conf.MQHost, int(conf.MQPort), MqVirtualHost, conf.MQUser, conf.MQPassword, RoutKeyDiskUsage, ExchangeName,
                                   QueueName='')
    DiskMonitorInterval = int(conf.DiskMonitorInterval)/1000

    Res, Errmsg, Ret, DiskList = GetDiskInfo(conf.PortalHost, int(conf.PortalPort), conf.PortalApiKey)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            DiskStatInfo['list'] = UpdateDiskPartitionInfo(DiskList)

    while True:
        for disk in DiskStatInfo['list']:
            try:
                if disk['ServerId'] != conf.ServerId:
                    continue
                Id = disk['Id']
                ServerId = disk['ServerId']
                State = disk['State']
                TotalInode = disk['TotalInode']
                UsedInode = disk['UsedInode']
                ReservedInode = disk['ReservedInode']
                TotalSize = disk['TotalSize']
                UsedSize = disk['UsedSize']
                ReservedSize = disk['ReservedSize']
                ReadPerSec = disk['ReadPerSec']
                WritePerSec = disk['WritePerSec']

                DiskStat = DiskDetailMqBroadcast()
                DiskStat.Set(Id, ServerId, State, TotalInode, ReservedInode,
                            UsedInode, TotalSize, ReservedSize, UsedSize, ReadPerSec, WritePerSec)
                Mqsend = jsonpickle.encode(DiskStat, make_refs=False, unpicklable=False)
                logger.debug(Mqsend)
                Mqsend = json.loads(Mqsend)
                MqDiskUpdated.Sender(Mqsend)
            except Exception as err:
                logger.error('fail to get Disk Info %s' % str(err))

        time.sleep(DiskMonitorInterval)



def UpdateDiskPartitionInfo(DiskList):
    """
    return DiskStatInfo.

    :param DiskList: [class AllDiskItemsDetail, ...]
    :param LapTime:
    :return: [{'Path': '/DISK1', 'Device': 'sdb1', 'Stat': DiskDetailMqBroadcast Class}, 'Read': 0, 'Write': 0, 'LapTime': now, ...]
    """
    DiskStatInfo = list()
    for disk in DiskList:
        for part in psutil.disk_partitions():
            TmpDiskInfo = dict()
            if disk.Path == part.mountpoint:
                Device = re.sub('/dev/', '', part.device)
                TmpDiskInfo['Path'] = disk.Path
                TmpDiskInfo['Device'] = Device
                TmpDiskInfo['Id'] = disk.Id
                TmpDiskInfo['ServerId'] = disk.ServerId
                TmpDiskInfo['State'] = disk.State
                TmpDiskInfo['Read'] = 0
                TmpDiskInfo['Write'] = 0
                TmpDiskInfo['ReadPerSec'] = 0
                TmpDiskInfo['WritePerSec'] = 0
                TmpDiskInfo['LapTime'] = time.time()
                DiskStatInfo.append(TmpDiskInfo)

    return DiskStatInfo

def GetDiskIoFromProc(DiskStatInfo, logger):
    #Device = ['sda1', 'sda2', 'sdb1', 'sdb2']
    dev = DiskStatInfo['Device']
    stat = None
    with open(ProcDiskStatsPath, 'r') as f :
        stat = f.read()
    Now = time.time()

    StatFinder = re.compile("%s[\s][\d]+[\s][\d]+[\s]([\d]+[\s])[\d]+[\s][\d]+[\s][\d]+[\s]([\d]+[\s])" % dev)
    Stat = StatFinder.search(stat)
    if Stat:
        Readn, Writen = Stat.groups()
        Interval = Now - DiskStatInfo['LapTime']
        DiskStatInfo['LapTime'] = Now
        DiskStatInfo['ReadPerSec'] = ((int(Readn) - DiskStatInfo['Read']) * 512 ) / Interval  # Byte
        DiskStatInfo['WritePerSec'] = ((int(Writen) - DiskStatInfo['Write']) * 512 ) / Interval # Byte

        DiskStatInfo['Read'] = int(Readn)
        DiskStatInfo['Write'] = int(Writen)


        Writen = DiskStatInfo['WritePerSec']
        Readn = DiskStatInfo['ReadPerSec']
        RetWriten = Writen
        RetReadn = Readn
        print(Writen)
        if  Writen/ (1024 * 1024 * 1024) > 0.99:
            Writen = Writen / (1024 * 1024)
            WUnit = 'GB'
        elif Writen / (1024*1024) > 0.99:
            Writen = Writen / (1024 * 1024)
            WUnit = 'MB'
        else:
            Writen = Writen/(1024)
            WUnit = 'KB'

        if  Readn/ (1024 * 1024) > 0.99:
            Readn = Readn / (1024 * 1024)
            RUnit = 'GB'
        elif Readn / (1024) > 0.99:
            Readn = Readn / (1024)
            RUnit = 'MB'
        else:
            Readn = Readn
            RUnit = 'KB'

        logger.debug("Lap:%d, Dev:%s, Read/s:%d%s Write/s:%d%s" % (DiskStatInfo['LapTime'], dev, Readn, RUnit, Writen, WUnit))
        return RetReadn, RetWriten
    else:
        logger.error('fail to get %s info from %s' % (dev, ProcDiskStatsPath))


@catch_exceptions()
def DiskUsageMonitoring(Conf, DiskStatInfo, GlobalFlag, logger):

    Conf = WaitAgentConfComplete(inspect.stack()[1][3], logger)

    conf = GetAgentConfig(Conf)
    DiskMonitorInterval = int(conf.DiskMonitorInterval)/1000
    while True:
        Res, Errmsg, Ret, DiskList = GetDiskInfo(conf.PortalHost, int(conf.PortalPort), conf.PortalApiKey)
        if Res == ResOk:
            if Ret.Result == ResultSuccess:
                DiskStatInfo['list'] = UpdateDiskPartitionInfo(DiskList)
                while True:
                    if GlobalFlag['DiskUpdated'] == Updated:
                        GlobalFlag['DiskUpdated'] = Checked
                        logging.log(logging.INFO,'Disk Info is Updated')
                        break

                    DiskIo = psutil.disk_io_counters(perdisk=True)
                    for disk in DiskStatInfo['list']:
                        if disk['ServerId'] != conf.ServerId:
                            continue
                        #Id = disk['Id']
                        #ServerId = disk['ServerId']
                        #State = disk['State']
                        Usage = Disk(disk['Path'])
                        Usage.GetUsage()
                        Usage.GetInode()
                        disk['TotalSize'] = Usage.TotalSize
                        disk['UsedSize'] = Usage.UsedSize
                        disk['ReservedSize'] = Usage.ReservedSize
                        disk['TotalInode'] = Usage.TotalInode
                        disk['UsedInode'] = Usage.UsedInode
                        disk['ReservedInode'] = Usage.ReservedInode


                        #ReadPerSec, WritePerSec = GetDiskReadWrite(disk, DiskIo)
                        ReadPerSec, WritePerSec = GetDiskIoFromProc(disk, logger)
                        disk['ReadPerSec'] = ReadPerSec
                        disk['WritePerSec'] = WritePerSec

                        #DiskStat = DiskDetailMqBroadcast()
                        #DiskStat.Set(Id, ServerId, State, Usage.TotalInode, Usage.ReservedInode,
                        #             Usage.UsedInode, Usage.TotalSize, Usage.ReservedSize, Usage.UsedSize, ReadPerSec, WritePerSec)
                        #Mqsend = jsonpickle.encode(DiskStat, make_refs=False, unpicklable=False)
                        #logger.debug(Mqsend)
                        #Mqsend = json.loads(Mqsend)
                        #MqDiskUpdated.Sender(Mqsend)

                    time.sleep(DiskMonitorInterval)
            else:
                logger.error('fail to get Disk Info %s' % Ret.Message)
        else:
            logger.error('fail to get Disk Info %s' % Errmsg)

        time.sleep(IntervalMiddle)


def MqDiskHandler(RoutingKey, Body, Response, ServerId, GlobalFlag, logger):
    logger.debug("%s %s" % (str(RoutingKey), str(Body)))
    if RoutKeyDiskCheckMountFinder.search(RoutingKey):
        ResponseReturn = mqmanage.mq.MqReturn(ResultSuccess)
        Body = Body.decode('utf-8')
        Body = json.loads(Body)
        body = DictToObject(Body)
        if ServerId == body.ServerId:
            ret = CheckDiskMount(body.Path)
            if ret is False:
                ResponseReturn = mqmanage.mq.MqReturn(ret, Code=1, Messages='No such disk is found')
            Response.IsProcessed = True
        logger.debug(ResponseReturn)
        return ResponseReturn
    elif RoutKeyDiskWirteDiskIdFinder.search(RoutingKey):
        ResponseReturn = mqmanage.mq.MqReturn(ResultSuccess)
        Body = Body.decode('utf-8')
        Body = json.loads(Body)
        body = DictToObject(Body)
        if ServerId == body.ServerId:
            ret, errmsg = WriteDiskId(body.Path, body.Id)
            if ret is False:
                ResponseReturn = mqmanage.mq.MqReturn(ret, Code=1, Messages=errmsg)
            Response.IsProcessed = True
        logger.debug(ResponseReturn)
        return ResponseReturn
    elif RoutingKey.endswith(RoutKeyDiskAdded):
        GlobalFlag['DiskUpdated'] = Updated
        logging.log(logging.INFO, "Disk Info is Added")
        ResponseReturn = mqmanage.mq.MqReturn(ResultSuccess)
        Body = Body.decode('utf-8')
        Body = json.loads(Body)
        body = DictToObject(Body)
        Response.IsProcessed = True
        logger.debug(ResponseReturn)
        return ResponseReturn
    elif RoutingKey.endswith(RoutKeyDiskPoolUpdate) or \
            RoutingKey.endswith(RoutKeyDiskStartStop) or \
            RoutingKey.endswith(RoutKeyDiskState) or \
            RoutingKey.endswith(RoutKeyDiskDel):
        logging.log(logging.INFO, "Disk Info is Updated")
        GlobalFlag['DiskUpdated'] = Updated
        GlobalFlag['DiskPoolUpdated'] = Updated
        ResponseReturn = mqmanage.mq.MqReturn(ResultSuccess)
        Body = Body.decode('utf-8')
        Body = json.loads(Body)
        body = DictToObject(Body)
        Response.IsProcessed = True
        logger.debug(ResponseReturn)
        #UpdateDiskPoolXml()
        return ResponseReturn
    else:
        ResponseReturn = mqmanage.mq.MqReturn(ResultSuccess)
        Response.IsProcessed = True
        return ResponseReturn


@catch_exceptions()
def UpdateDiskPoolXml():
    ret, conf = GetConf(MonServicedConfPath)
    if ret is False:
        print('Init conf first')
        return
    if not os.path.exists(KsanEtcPath):
        print('%s is not found' % KsanEtcPath)
        return
    # get root node
    #root = doc.getroot()
    #root = ET.Element('DISKPOOLLIST')



    # DiskPools = [{"PoolName":"pool1", "PoolId":"abc123", "Servers": [{"Id": "", "Ip":"", "Status":"Online", "Disks":
    # [{"DiskId": "", "Mode":"Rw", "Path": "", "Status": "GOOD"}]}]}]
    DiskPools = list()
    Res, Errmsg, Ret, Servers = GetAllServerDetailInfo(conf.PortalHost, int(conf.PortalPort), conf.PortalApiKey, logger=None)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            for Svr in Servers:
                ServerId = Svr.Id
                if len(Svr.NetworkInterfaces) == 0:
                    continue
                ServerIp = Svr.NetworkInterfaces[0].IpAddress
                DiskList = list()
                for Disk in Svr.Disks:
                    DiskId = Disk.Id
                    DiskMode = Disk.RwMode
                    DiskPath = Disk.Path
                    DiskStatus = Disk.State
                    DiskPoolId = Disk.DiskPoolId
                    DiskPoolName = Disk.DiskPoolName
                    if DiskPoolId is not None:
                        CreateDiskPoolInfo(DiskPools, ServerId, ServerIp, DiskId, DiskMode, DiskPath, DiskStatus,
                                       DiskPoolId, DiskPoolName)

            root = CreateDiskPoolXmlFile(DiskPools)
            indent(root)
            tree = ET.ElementTree(root)
            tree.write(DiskPoolXmlPath, encoding="utf-8", xml_declaration=True)

def CreateDiskPoolXmlFile(DiskPools):
    root = ET.Element('DISKPOOLLIST')

    for DiskPool in DiskPools:
        PoolTag = CreateDiskPoolElement(DiskPool['PoolId'], DiskPool['PoolName'])
        for Server in DiskPool['Servers']:
            ServerTag = CreateServerElement(Server['ServerId'], Server['ServerIp'], Server['Status'])
            for Disk in Server['Disks']:
                DiskTag = CreateDiskElement(Disk['DiskId'], Disk['Path'], Disk['Mode'], Disk['Status'])
                ServerTag.append(DiskTag)
            PoolTag.append(ServerTag)
        root.append(PoolTag)
    return root

def CreateDiskElement(DiskId, Path, RwMode, State):
    NewDiskTag = ET.Element("DISK")
    # last_updated의 text를 지정한다
    #NewDiskTag.text = '\n\t'
    NewDiskTag.attrib["id"] = DiskId
    NewDiskTag.attrib["path"] = Path
    NewDiskTag.attrib["mode"] = RwMode
    NewDiskTag.attrib["status"] = State
    return NewDiskTag

def CreateServerElement(ServerId, ServerIp, Status):
    NewServerTag = ET.Element("SERVER")
    NewServerTag.text = '\n\t'
    NewServerTag.attrib["id"] = ServerId
    NewServerTag.attrib["ip"] = ServerIp
    NewServerTag.attrib["status"] = Status
    return NewServerTag

def CreateDiskPoolElement(PoolId, PoolName):
    NewDiskPoolTag = ET.Element('DISKPOOL')
    NewDiskPoolTag.text = '\n\t'
    NewDiskPoolTag.attrib["id"] = PoolId
    NewDiskPoolTag.attrib["name"] = PoolName
    return NewDiskPoolTag



# DiskPools = [{"PoolName":"pool1", "PoolId":"abc123", "Servers": [{"ServerId": "", "ServerIp":"", "ServerStatus":"Online", "Disks":
# [{"DiskId": "", "DiskMode":"Rw", "DiskPath": "", "DiskStatus": "GOOD"}]}]}]
def CreateDiskPoolInfo(DiskPools, ServerId, ServerIp, DiskId, DiskMode, DiskPath, DiskStatus, DiskPoolId, DiskPoolName):
    TmpDisk = {"DiskId": DiskId, "Mode": DiskMode, "Path": DiskPath, "Status": DiskStatus}
    for Pool in DiskPools:
        if Pool['PoolId'] == DiskPoolId:
            for Server in Pool['Servers']:
                if Server['ServerId'] == ServerId:
                    Server['Disks'].append(TmpDisk)
                    return
            TmpServer = {"ServerId": ServerId, "ServerIp": ServerIp, "Status": "Online", "Disks": []}
            TmpServer['Disks'].append(TmpDisk)
            Pool['Servers'].append(TmpServer)
            return
    TmpPool = {"PoolName": DiskPoolName, "PoolId": DiskPoolId, "Servers": []}
    TmpServer = {"ServerId": ServerId, "ServerIp": ServerIp, "Status": "Online", "Disks": []}
    TmpServer['Disks'].append(TmpDisk)
    TmpPool['Servers'].append(TmpServer)
    DiskPools.append(TmpPool)


def indent(elem, level=0):
    i = "\n" + level*"  "
    if len(elem):
        if not elem.text or not elem.text.strip():
            elem.text = i + "  "
        if not elem.tail or not elem.tail.strip():
            elem.tail = i
        for elem in elem:
            indent(elem, level+1)
        if not elem.tail or not elem.tail.strip():
            elem.tail = i
    else:
        if level and (not elem.tail or not elem.tail.strip()):
            elem.tail = i

