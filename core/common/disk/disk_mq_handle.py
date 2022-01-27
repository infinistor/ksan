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
from ksan.disk.disk_manage import *
from ksan.common.define import DiskPoolXmlPath
from ksan.common.init import GetConf
import ksan.mqmanage.mq
import time
import json
import xml.etree.ElementTree as ET



@catch_exceptions()
def UpdateDiskStat(conf):
    mq = ksan.mqmanage.mq.Mq(conf.mgs.MgsIp, int(conf.mgs.MqPort), MqVirtualHost, MqUser, MqPassword, RoutKeyDiskUsage, ExchangeName,
            QueueName='')
    Res, Errmsg, Ret, AllDiskList = GetDiskInfo(conf.mgs.MgsIp, int(conf.mgs.IfsPortalPort), conf.mgs.ServerId)
    print(AllDiskList)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            while True:
                for disk in AllDiskList:
                    if conf.mgs.ServerId != disk.ServerId:
                        continue
                    print(disk)
                    stat = Disk(disk.Path)
                    stat.GetInode()
                    stat.GetUsage()
                    DiskSize = DiskSizeItems()
                    DiskSize.Set(disk.Id, disk.ServerId, disk.DiskPoolId, disk.DiskNo, stat.TotalInode, stat.UsedInode,
                                 stat.ReservedInode, stat.TotalSize, stat.UsedSize, stat.ReservedSize)
                    Mqsend = jsonpickle.encode(DiskSize, make_refs=False, unpicklable=False)
                    print(Mqsend)
                    Mqsend = json.loads(Mqsend)
                    mq.Sender(Mqsend)
                time.sleep(DiskMonitorInterval)


def MqDiskHandler(RoutingKey, Body, Response, ServerId):
    print(RoutingKey, Body)
    if RoutKeyDiskCheckMountFinder.search(RoutingKey):
        ResponseReturn = ksan.mqmanage.mq.MqReturn(ResultSuccess)
        Body = Body.decode('utf-8')
        Body = json.loads(Body)
        body = DictToObject(Body)
        if ServerId == body.ServerId:
            ret = CheckDiskMount(body.Path)
            if ret is False:
                ResponseReturn = ksan.mqmanage.mq.MqReturn(ret, Code=1, Messages='No such disk is found')
            Response.IsProcessed = True
        print(ResponseReturn)
        return ResponseReturn
    elif RoutKeyDiskWirteDiskIdFinder.search(RoutingKey):
        ResponseReturn = ksan.mqmanage.mq.MqReturn(ResultSuccess)
        Body = Body.decode('utf-8')
        Body = json.loads(Body)
        body = DictToObject(Body)
        if ServerId == body.ServerId:
            ret, errmsg = WriteDiskId(body.Path, body.Id)
            if ret is False:
                ResponseReturn = ksan.mqmanage.mq.MqReturn(ret, Code=1, Messages=errmsg)
            Response.IsProcessed = True
        print(ResponseReturn)
        return ResponseReturn
    elif RoutingKey.endswith(RoutKeyDiskAdded):
        ResponseReturn = ksan.mqmanage.mq.MqReturn(ResultSuccess)
        Body = Body.decode('utf-8')
        Body = json.loads(Body)
        body = DictToObject(Body)
        Response.IsProcessed = True
        print(ResponseReturn)
        return ResponseReturn
    elif RoutingKey.endswith(RoutKeyDiskPoolUpdate) or \
            RoutingKey.endswith(RoutKeyDiskStartStop) or \
            RoutingKey.endswith(RoutKeyDiskState):
        ResponseReturn = ksan.mqmanage.mq.MqReturn(ResultSuccess)
        Body = Body.decode('utf-8')
        Body = json.loads(Body)
        body = DictToObject(Body)
        Response.IsProcessed = True
        print(ResponseReturn)
        UpdateDiskPoolXml()
        return ResponseReturn
    else:
        ResponseReturn = ksan.mqmanage.mq.MqReturn(ResultSuccess)
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
    Res, Errmsg, Ret, Servers = GetAllServerDetailInfo(conf.mgs.MgsIp, int(conf.mgs.IfsPortalPort), logger=None)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            for Svr in Servers:
                ServerId = Svr.Id
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

