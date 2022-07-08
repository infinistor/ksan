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
from service.service_manage import *
#import ksan.service.s3 as S3
from service.osd import KsanOsd
from common.utils import IsDaemonRunning
from common.define import ServiceMonitorInterval
from service.gw import KsanGW
from service.mongo import KsanMongoDB
import mqmanage.mq
import logging
import xml.etree.ElementTree as ET

@catch_exceptions()
def MqServiceHandler(MonConf, RoutingKey, Body, Response, ServerId, ServiceList, LocalIpList, GlobalFlag, logger):
    """
    Message Queue Service Handler
    """
    logger.debug("%s %s %s" % (str(RoutingKey), str(Body), str(Response)))
    ResponseReturn = mqmanage.mq.MqReturn(ResultSuccess)
    Body = Body.decode('utf-8')
    Body = json.loads(Body)
    body = DictToObject(Body)

    # Service Control Handle
    if RoutKeyServiceControlFinder.search(RoutingKey):
        logging.log(logging.INFO, "%s" % str(Body))
        #if body.Id not in ServiceList['IdList']:
        #    LoadServiceList(MonConf, ServiceList, LocalIpList, logger)

        Response.IsProcessed = True
        #if body.Id not in ServiceList['Details']:
        #    logger.error('Invalid Service Id %s' % body.Id)
        #    return ResponseReturn
        ServiceType = body.ServiceType
        #if ServiceType == TypeHaproxy:
        #    Ret = Hap.StartStop(Body)
        #elif ServiceType == TypeS3:
        #    Ret = S3.StartStop(Body)
        if ServiceType == TypeServiceOSD:
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
        elif ServiceType == TypeServiceS3:
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
        elif ServiceType == TypeServiceMONGODB:
            mongo = KsanMongoDB(logger)
            if body.Control == START:
                Ret, ErrMsg = mongo.Start()
            elif body.Control == STOP:
                    Ret, ErrMsg = mongo.Stop()
            elif body.Control == RESTART:
                Ret, ErrMsg = mongo.Restart()
            else:
                Ret = False
                ErrMsg = 'Invalid Control Code'
        else:
            Ret = False
            ErrMsg = 'Invalid Serivce Type'

        if Ret is False:
            ResponseReturn = mqmanage.mq.MqReturn(Ret, Code=1, Messages=ErrMsg)
            logger.error(ResponseReturn)
        else:
            logging.log(logging.INFO, ResponseReturn)
        return ResponseReturn
    # Serivce Config Handle
    elif ".config." in RoutingKey:
        if body.Id in ServiceList['IdList']:
            Response.IsProcessed = True
            logger.debug(ResponseReturn)
        return ResponseReturn
    elif RoutingKey.endswith((".added", ".updated", ".removed")):
        GlobalFlag['ServiceUpdated'] = Updated
        Response.IsProcessed = True
        logger.debug(ResponseReturn)
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
        if self.ServiceType == TypeServiceS3:
            Ret, Pid = IsDaemonRunning(KsanGwPidFile, CmdLine='ksan-gw')
            if Ret is True:
                self.Pid = int(Pid)
            else:
                self.Pid = None

        elif self.ServiceType == TypeServiceOSD:
            Ret, Pid = IsDaemonRunning(KsanOsdPidFile, CmdLine='ksan-osd')
            if Ret is True:
                self.Pid = int(Pid)
            else:
                self.Pid = None
        elif self.ServiceType == TypeServiceEdge:
            Ret, Pid = IsDaemonRunning(KsanEdgePidFile, CmdLine='ksanEdge')
            if Ret is True:
                self.Pid = int(Pid)
            else:
                self.Pid = None
        elif self.ServiceType == TypeServiceMonitor:
            Ret, Pid = IsDaemonRunning(KsanMonPidFile, CmdLine='ksanMon')
            if Ret is True:
                self.Pid = int(Pid)
            else:
                self.Pid = None

        elif self.ServiceType == TypeServiceMONGODB:
            Ret, Pid = IsDaemonRunning(KsanMongosPidFile, CmdLine='mongos')
            if Ret is True:
                self.Pid = int(Pid)
            else:
                self.Pid = None

        else:
            pass

    def GetUsage(self):
        try:
            p = psutil.Process(pid=self.Pid)
            self.CpuUsage = p.cpu_percent(interval=1)
            m = p.memory_full_info()
            #self.MemoryUsed = m.rss + m.vms + m.shared + m.data
            self.MemoryUsed = m.rss
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



@catch_exceptions()
def ServiceMonitoring(conf, GlobalFlag, logger):
    MqServiceUsage = mqmanage.mq.Mq(conf.mgs.MgsIp, int(conf.mgs.MqPort), MqVirtualHost, MqUser, MqPassword,
                        RoutKeyServiceUsage, ExchangeName, QueueName='')
    MqServiceState = mqmanage.mq.Mq(conf.mgs.MgsIp, int(conf.mgs.MqPort), MqVirtualHost, MqUser, MqPassword,
                                    RoutKeyServiceState, ExchangeName, QueueName='')

    # local service list
    LocalServices = list() # [{ 'Id': ServiceId, 'Type': 'Osd', 'ProcessObject':Object, 'IsEnable': True, 'GroupId': GroupId, 'Status': 'Online'}]
    while True:
        Res, Errmgs, Ret, ServerDetail = GetServerInfo(conf.mgs.MgsIp, conf.mgs.IfsPortalPort, conf.mgs.ServerId, logger=logger)
        if Res == ResOk:
            if Ret.Result == ResultSuccess:
                LocalServices = list()
                # Update Service Info
                for Service in ServerDetail.Services:
                    try:
                        ProcObject = Process(Service.ServiceType)
                        NewService = dict()
                        NewService['Id'] = Service.Id
                        NewService['GroupId'] = '' if Service.GroupId is None else Service.GroupId
                        NewService['Type'] = Service.ServiceType
                        NewService['ProcessObject'] = ProcObject
                        NewService['IsEnable'] = True
                        LocalServices.append(NewService)
                    except Exception as err:
                        logger.error("fail to get service info %s " % str(err))

                while True:

                    if GlobalFlag['ServiceUpdated'] == Updated:
                        GlobalFlag['ServiceUpdated'] = Checked
                        logging.log(logging.INFO,'Service Info is Updated')
                        break

                    ServiceTypePool = list()
                    for Service in LocalServices:
                        try:
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
                            MqServiceUsage.Sender(Usage)
                        except Exception as err:
                            logger.error("fail to get service info %s" % str(err))
                            continue

                        State = UpdateServicesStateObject()
                        State.Set(ServiceId, ProcObject.State)
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

                    time.sleep(ServiceMonitorInterval)
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

