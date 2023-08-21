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

import pdb

import psutil
import sys, os
import socket
import time
if os.path.dirname(os.path.abspath(os.path.dirname(__file__))) not in sys.path:
    sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from const.common import *
import xml.etree.ElementTree as ET
from const.service import UpdateServicesStateObject, SystemdServicePidFinder
import mqmanage.mq
from portal_api.apis import *



class Process:
    def __init__(self,ServiceType):
        self.ServiceType = ServiceType
        self.SystemdServiceName = ServiceTypeSystemdServiceMap[ServiceType] if ServiceType in ServiceTypeSystemdServiceMap else None
        self.Pid = None
        self.CpuUsage = 0
        self.MemoryUsed = 0
        self.ThreadCount = 0
        self.State = 'Offline'
        self.GetPidWithServiceType()
        self.GetState()
        self.GetUsage()

    def GetPidWithServiceType(self):
        if self.SystemdServiceName is not None:
            SystemdCmd = 'systemctl status %s' % self.SystemdServiceName
            out, err = shcall(SystemdCmd)
            Pid = SystemdServicePidFinder.search(out)
            if not Pid:
                self.Pid = None
            else:
                self.Pid = int(Pid.groups()[0])
        else:
            if self.ServiceType == TypeServiceAgent:
                Ret, Pid = IsDaemonRunning(KsanAgentPidFile, CmdLine=KsanAgentName)
                if Ret is True:
                    self.Pid = int(Pid)
                else:
                    self.Pid = None

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
def ServiceMonitoring(Conf, GlobalFlag, logger):

    Conf = WaitAgentConfComplete(inspect.stack()[1][3], logger, CheckNetworkDevice=True, CheckNetworkId=True)

    conf = GetAgentConfig(Conf)

    #KsanServiceRegister(conf, TypeServiceAgent, logger)

    MqServiceUsage = mqmanage.mq.Mq(conf.MQHost, int(conf.MQPort), MqVirtualHost, conf.MQUser, conf.MQPassword,
                        RoutKeyServiceUsage, ExchangeName, QueueName='', logger=logger)
    MqServiceState = mqmanage.mq.Mq(conf.MQHost, int(conf.MQPort), MqVirtualHost, conf.MQUser, conf.MQPassword,
                                    RoutKeyServiceState, ExchangeName, QueueName='', logger=logger)

    ServiceMonitorInterval = int(conf.ServiceMonitorInterval)/1000
    # local service list
    LocalServices = list() # [{ 'Id': ServiceId, 'Type': 'Osd', 'ProcessObject':Object, 'IsEnable': True, 'GroupId': GroupId, 'Status': 'Online'}]
    while True:
        Res, Errmgs, Ret, ServerDetail = GetServerInfo(conf.PortalHost, conf.PortalPort, conf.PortalApiKey, conf.ServerId, logger=logger)
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

                        #ret, errlog = isKsanServiceIdFileExists(Service.ServiceType)
                        #if ret is False:
                        #    logger.debug('ServiceId file is not found. %s %s' % (Service.Name, errlog))
                        #    ret, errlog = SaveKsanServiceIdFile(Service.ServiceType, Service.Id)
                        #    if ret is False:
                        #        logger.error(errlog)
                        #else:
                        #    logger.debug('ServiceId file exists %s' % Service.Name)
                        #logging.log(logging.INFO, 'Service %s is added' % Service.ServiceType)
                    except Exception as err:
                        logger.error("fail to get service info %s " % str(err))

                while True:

                    if GlobalFlag['ServiceUpdated'] == Updated:
                        GlobalFlag['ServiceUpdated'] = Checked
                        #logging.log(logging.INFO,'Service Info is Updated')
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
                            logger.debug(Usage)
                            Usage = json.loads(Usage)
                            MqServiceUsage.Sender(Usage)
                            logger.debug(Usage)
                        except Exception as err:
                            logger.error("fail to get service info %s" % str(err))
                            continue

                        if ServiceType == TypeServiceAgent:
                            State = UpdateServicesStateObject()
                            #State.Set(ServiceId, ProcObject.State)
                            State.Set(ServiceId, ONLINE)
                            State = jsonpickle.encode(State, make_refs=False, unpicklable=False)
                            State = json.loads(State)
                            logger.debug(State)
                            MqServiceState.Sender(State)
                            ServiceStatus = ProcObject.State
                            #CreateServicePoolXmlFile(ServiceTypePool)
                            #UpdateServiceInfoDump(ServiceTypePool, ServiceId, ServiceType, GroupId, ServiceStatus)


                    root = CreateServicePoolXmlFile(ServiceTypePool)
                    indent(root)
                    tree = ET.ElementTree(root)
                    tree.write(ServicePoolXmlPath, encoding="utf-8", xml_declaration=True)

                    time.sleep(ServiceMonitorInterval)
        time.sleep(IntervalMiddle)


@catch_exceptions()
def UpdateServiceUsage(Ip, Port, ServiceId, Usage, logger=None):

    Url = "/api/v1/Services/Usage" % ServiceId
    body = jsonpickle.encode(Usage, make_refs=False)
    Conn = RestApi(Ip, Port, Url, params=body, logger=logger)
    Res, Errmsg, Data = Conn.put(ItemsHeader=False, ReturnType=ResponseHeaderModule)
    return Res, Errmsg, Data



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


def UnknownServiceRestart(ServiceList):
    ServiceBinMap = dict()
    ServiceBinMap[KsanGWName] = 'systemctl restart %s.service' % KsanGWName
    ServiceBinMap[KsanOSDName] = 'systemctl restart %s.service' % KsanOSDName
    ServiceBinMap[KsanLifecycleManagerName] = 'systemctl restart %s.service' % KsanLifecycleManagerName
    ServiceBinMap[KsanLogManagerName] = 'systemctl restart %s.service' % KsanLogManagerName
    ServiceBinMap[KsanReplicationManagerName] = 'systemctl restart %s.service' % KsanReplicationManagerName
    ServiceBinMap[KsanAgentName] = '/usr/local/ksan/sbin/ksanAgent start'

    for Svc in ServiceList:
        if Svc.State in ['Unknown', 'Offline', 'Timeout']:
            Cmd = ServiceBinMap[Svc.ServiceType]
            out, err = shcall(Cmd)
            print(out, err)



def ServiceUtilHandler(Conf, Action, Parser, logger):
    #if Action == 'init'
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
        if not (options.ServerName and options.ServiceName and options.ServiceType):
            Parser.print_help()
            sys.exit(-1)
        Res, Errmsg, Ret = AddService(PortalIp, PortalPort, PortalApiKey, options.ServiceName, options.ServiceType,
                                      ServerName=options.ServerName, logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)
    elif Action.lower() == 'remove':
        if not options.ServiceName:
            Parser.print_help()
            sys.exit(-1)
        Res, Errmsg, Ret = DeleteService(PortalIp, PortalPort, PortalApiKey,
                                         ServiceName=options.ServiceName, logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)
    elif Action.lower() == 'list':
        #if options.Detail:
        #Res, Errmsg, Ret, Data = GetAllServerDetailInfo(PortalIp, PortalPort, PortalApiKey,logger=logger)
        #else:
        Res, Errmsg, Ret, Data = GetServiceInfo(PortalIp, PortalPort, PortalApiKey, logger=logger)

        if Res == ResOk:
            if Ret.Result == ResultSuccess:
                if options.MoreDetail:
                    Detail = MoreDetailInfo
                elif options.Detail:
                    Detail = DetailInfo
                else:
                    Detail = SimpleInfo
                ShowServiceInfoWithServerInfo(Data, Detail=Detail)
                    #ShowServiceInfo(Data, Detail=options.Detail)
                #else:
                #    ShowServiceInfo(Data, Detail=options.Detail)
            else:
                print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)

    elif Action.lower() == 'update':
        if not (options.ServiceId or options.ServiceName):
            Parser.print_help()
            sys.exit(-1)
        Res, Errmsg, Ret = UpdateServiceInfo(PortalIp, PortalPort, PortalApiKey, ServiceId=options.ServiceId, ServiceName=options.ServiceName,
                                             ServiceType=options.ServiceType,
                                             GroupId=options.GroupId, Description=options.Description,
                                              logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)

    elif Action.lower() == 'update_state':
        Res, Errmsg, Ret = UpdateServiceState(PortalIp, PortalPort, PortalApiKey, options.ServiceId,
                                              logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)

    elif Action.lower() == 'update_usage':
        Res, Errmsg, Ret = UpdateServiceUsage(PortalIp, PortalPort, PortalApiKey, options.ServiceId,
                                              logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)

    elif Action.lower() in ['start', 'stop', 'restart']:
        if not options.ServiceName:
            Parser.print_help()
            sys.exit(-1)

        Res, Errmsg, Ret = ControlService(PortalIp, PortalPort, PortalApiKey, Action.lower(),
                                          ServiceName=options.ServiceName, logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)

    elif Action.lower() == 'unknown_restart':
        #if options.Detail:
        #Res, Errmsg, Ret, Data = GetAllServerDetailInfo(PortalIp, PortalPort, PortalApiKey,logger=logger)
        #else:
        Res, Errmsg, Ret, Data = GetServiceInfo(PortalIp, PortalPort, PortalApiKey, logger=logger)

        if Res == ResOk:
            if Ret.Result == ResultSuccess:
                UnknownServiceRestart(Data)
                    #ShowServiceInfo(Data, Detail=options.Detail)
                #else:
                #    ShowServiceInfo(Data, Detail=options.Detail)
            else:
                print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)

    else:
        Parser.print_help()


