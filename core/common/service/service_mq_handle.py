#!/usr/bin/env python
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

import psutil
import os, sys
sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from ksan.service.service_manage import *
#import ksan.service.s3 as S3
from ksan.service.osd import KsanOsd
from ksan.common.utils import IsDaemonRunning
from ksan.service.gw import KsanGW
import ksan.mqmanage.mq
import xml.etree.ElementTree as ET

@catch_exceptions()
def MqServiceHandler(RoutingKey, Body, Response, ServerId, ServiceList, LocalIpList, logger=None):
    """
    Message Queue Service Handler
    """
    print(RoutingKey, Body, Response)
    ResponseReturn = ksan.mqmanage.mq.MqReturn(ResultSuccess)
    Body = Body.decode('utf-8')
    Body = json.loads(Body)
    body = DictToObject(Body)

    # Service Control Handle
    if RoutKeyServiceControlFinder.search(RoutingKey):
        if body.Id in ServiceList['IdList']:
            Response.IsProcessed = True
            ServiceType = ServiceList['Details'][body.Id].ServiceType
            #if ServiceType == TypeHaproxy:
            #    Ret = Hap.StartStop(Body)
            #elif ServiceType == TypeS3:
            #    Ret = S3.StartStop(Body)
            if ServiceType == TypeOSD:
                Osd = KsanOsd(logger)
                if body.Control == START:
                    Ret, ErrMsg = Osd.Start()
                elif body.Control == STOP:
                    Ret, ErrMsg = Osd.Stop()
                elif body.Control == RESTART:
                    Ret, ErrMsg = Osd.Restart()
                else:
                    Ret = False
                    ErrMsg = 'Invalid Control Code'
            elif ServiceType == TypeGW:
                Gw = KsanGW(logger)
                if body.Control == START:
                    Ret, ErrMsg = Gw.Start()
                elif body.Control == STOP:
                    Ret, ErrMsg = Gw.Stop()
                elif body.Control == RESTART:
                    Ret, ErrMsg = Gw.Restart()
                else:
                    Ret = False
                    ErrMsg = 'Invalid Control Code'
            else:
                Ret = False
                ErrMsg = 'Invalid Serivce Type'

            if Ret is False:
                ResponseReturn = ksan.mqmanage.mq.MqReturn(Ret, Code=1, Messages=ErrMsg)
            print(ResponseReturn)
        return ResponseReturn
    # Serivce Config Handle
    elif ".config." in RoutingKey:
        if body.Id in ServiceList['IdList']:
            Response.IsProcessed = True
            if RoutKeyServiceOsdConfLoadFinder.search(RoutingKey):
                Osd = KsanOsd(logger)
                Ret, Errmsg, Data = Osd.ReadConf()
                ResponseReturn = ksan.mqmanage.mq.MqReturn(Ret, Messages=Errmsg, Data=Data)
                return ResponseReturn
            elif RoutKeyServiceOsdConfSaveFinder.search(RoutingKey):
                Osd = KsanOsd(logger)
                Ret = Osd.UpdateConf(Body)
            elif RoutKeyServiceGwConfLoadFinder.search(RoutingKey):
                Gw = KsanGW
                Ret, Errmsg, Data = Gw.ReadConf()
                ResponseReturn = ksan.mqmanage.mq.MqReturn(Ret, Messages=Errmsg, Data=Data)
            elif RoutKeyServiceGwConfSaveFinder.search(RoutingKey):
                Gw = KsanGW
                Ret = Gw.UpdateConf(Body)
            else:
                Ret = False
            if Ret is False:
                ResponseReturn = ksan.mqmanage.mq.MqReturn(Ret, Messages='fail')
            print(ResponseReturn)
        return ResponseReturn
    else:
        Response.IsProcessed = True
        return ResponseReturn


class Process:
    def __init__(self,ServiceType):
        self.ServiceType = ServiceType
        self.Pid = None
        self.CpuUsage = 0
        self.MemoryUsed = 0
        self.ThreadCount = 0
        self.State = 'Offline'
        self.GetPidWithServiceType()
        self.GetState()
        self.GetUsage()

    def GetPidWithServiceType(self):
        if self.ServiceType == TypeGW:
            PidPath = self.GetPidFile()
            Ret, Pid = IsDaemonRunning(PidPath, CmdLine='apache-tomcat')
            if Ret is True:
                self.Pid = int(Pid)
            else:
                self.Pid = None

        elif self.ServiceType == TypeOSD:
            Ret, Pid = IsDaemonRunning(KsanOsdPidPath, CmdLine='ksanOsd.jar')
            if Ret is True:
                self.Pid = int(Pid)
            else:
                self.Pid = None
        else:
            pass

    def GetUsage(self):
        try:
            p = psutil.Process(pid=self.Pid)
            self.CpuUsage = p.cpu_percent()
            m = p.memory_full_info()
            self.MemoryUsed = m.rss + m.vms + m.shared + m.data
            self.ThreadCount = p.num_threads()
        except Exception as err:
            self.CpuUsage = 0
            self.MemoryUsed = 0
            self.ThreadCount = 0


    def GetState(self):
        if self.Pid is not None:
            self.State = ONLINE
        else:
            self.State = OFFLINE

    def GetPidFile(self):
        if self.ServiceType == TypeGW:
            gw = KsanGW(None)
            gw.ReadConf()
            return gw.apache_pid_path


@catch_exceptions()
def ServiceMonitoring(conf):
    MqServiceUsage = ksan.mqmanage.mq.Mq(conf.mgs.MgsIp, int(conf.mgs.MqPort), MqVirtualHost, MqUser, MqPassword,
                        RoutKeyServiceUsage, ExchangeName, QueueName='')
    MqServiceState = ksan.mqmanage.mq.Mq(conf.mgs.MgsIp, int(conf.mgs.MqPort), MqVirtualHost, MqUser, MqPassword,
                                    RoutKeyServiceState, ExchangeName, QueueName='')

    # local service list
    LocalServices = list() # [{ 'Id': ServiceId, 'Type': 'Osd', 'ProcessObject':Object, 'IsEnable': True, 'GroupId': GroupId, 'Status': 'Online'}]
    while True:
        Res, Errmgs, Ret, ServerDetail = GetServerInfo(conf.mgs.MgsIp, conf.mgs.IfsPortalPort, conf.mgs.ServerId)
        if Res == ResOk:
            if Ret.Result == ResultSuccess:
                CurrentEnabledServiceIds = list()  # the registered service id list in portal
                TmpLocalServices = list()
                for Service in ServerDetail.Services:
                    CurrentEnabledServiceIds.append(Service.Id)
                    Exist = False
                    for ExistService in LocalServices:
                        if ExistService['Id'] == Service.Id:
                            Exist = True
                            ExistService['IsEnable'] = True
                            TmpLocalServices.append(ExistService)

                    if Exist is False: # append new service to monitoring pool
                        ProcObject = Process(Service.ServiceType)
                        NewService = dict()
                        NewService['Id'] = Service.Id
                        NewService['GroupId'] = '' if Service.GroupId is None else Service.GroupId
                        NewService['Type'] = Service.ServiceType
                        NewService['ProcessObject'] = ProcObject
                        NewService['IsEnable'] = True
                        TmpLocalServices.append(NewService)

                LocalServices = TmpLocalServices

        ServiceTypePool = list()
        for Service in LocalServices:
            ServiceId = Service['Id']
            ProcObject = Service['ProcessObject']
            ServiceType = Service['Type']
            GroupId = Service['GroupId']
            ProcObject.GetPidWithServiceType()
            ProcObject.GetUsage()
            ProcObject.GetState()
            Usage = UpdateServiceUsageObject()
            Usage.Set(ServiceId, ProcObject.CpuUsage, ProcObject.MemoryUsed, ProcObject.ThreadCount)
            Usage = jsonpickle.encode(Usage, make_refs=False, unpicklable=False)
            Usage = json.loads(Usage)
            print(Usage)
            MqServiceUsage.Sender(Usage)

            State = UpdateServicesStateObject()
            State.Set(ServiceId, ProcObject.State)
            print(State)
            State = jsonpickle.encode(State, make_refs=False, unpicklable=False)
            State = json.loads(State)
            MqServiceState.Sender(State)
            ServiceStatus = ProcObject.State
            CreateServicePoolXmlFile(ServiceTypePool)
            UpdateServiceInfoDump(ServiceTypePool, ServiceId, ServiceType, GroupId, ServiceStatus)

        root = CreateServicePoolXmlFile(ServiceTypePool)
        indent(root)
        tree = ET.ElementTree(root)
        tree.write(ServicePoolXmlPath, encoding="utf-8", xml_declaration=True)

        time.sleep(5)


@catch_exceptions()
def UpdateServiceUsage(Ip, Port, ServiceId, Usage, logger=None):

    Url = "/api/v1/Services/Usage" % ServiceId
    body = jsonpickle.encode(Usage, make_refs=False)
    Conn = RestApi(Ip, Port, Url, params=body, logger=logger)
    Res, Errmsg, Data = Conn.put(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data


@catch_exceptions()
def UpdateServiceState(Ip, Port, ServiceId, State, logger=None):

    Url = "/api/v1/Services/State" % ServiceId
    service = UpdateServicesStateObject()
    service.Set(ServiceId, State)
    body = jsonpickle.encode(service, make_refs=False)
    Conn = RestApi(Ip, Port, Url, params=body, logger=logger)
    Res, Errmsg, Data = Conn.put(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data

def UpdateServiceInfoDump(ServiceTypePool, ServiceId, ServiceType, GroupId, Status):
    """
    ServicePool: Current enabled service list [{"ServiceType": type,
    "List": [{"ServiceId": Id, "GroupId": GroupId, "Status": "Online"}}]}]

    """
    for Service in ServiceTypePool:
        if Service['ServiceType'] == ServiceType:
            NewService = {"ServiceId": ServiceId, "GroupId": GroupId, "Status": Status}
            ServiceTypePool.append(NewService)
            return
    NewType = {"ServiceType": ServiceType, "ServiceList": []}
    NewService = {"ServiceId": ServiceId, "GroupId": GroupId, "Status": Status}
    NewType["ServiceList"].append(NewService)
    ServiceTypePool.append(NewType)
    return

def CreateServicePoolXmlFile(ServiceTypePool):
    root = ET.Element('SERVICELIST')
    for Type in ServiceTypePool:
        TypeTag = CreateTypeElement(Type['ServiceType'])
        for Service in Type['ServiceList']:
            ServiceTag = CreateServiceElement(Service['ServiceId'], Service['GroupId'], Service['Status'])
            TypeTag.append(ServiceTag)
        root.append(TypeTag)
    return root


def CreateTypeElement(ServiceType):
    NewTypeTag = ET.Element("SERVICETYPE")
    # last_updated의 text를 지정한다
    # NewDiskTag.text = '\n\t'
    NewTypeTag.attrib["type"] = ServiceType
    return NewTypeTag


def CreateServiceElement(ServiceId, GroupId, Status):
    NewServiceTag = ET.Element("SERVICE")
    # last_updated의 text를 지정한다
    # NewDiskTag.text = '\n\t'
    NewServiceTag.attrib["id"] = ServiceId
    NewServiceTag.attrib["group"] = GroupId
    NewServiceTag.attrib["status"] = Status
    return NewServiceTag


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

