#!/bin/env python3
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
import psutil
if os.path.dirname(os.path.abspath(os.path.dirname(__file__))) not in sys.path:
    sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from common.utils import RestApi
from const.common import *
from const.server import ServerItemsDetailModule, ServerItemsModule, RequestServerInfo, RequestServerInitInfo, \
    UpdateServerInfoItems, RequestServerExistCheck
from common.utils import ResPonseHeader
from const.rest import SetRestReturn
from portal_api.apis import *
#from network.network_manage import GetNetworkInterfaceDetailWithId

"""
############## Servers ###############
"""
class RestHandlerServer:
    def __init__(self):
        self.logger = logging.getLogger(KsanAgentBinaryName)


    catch_exceptions()
    def InitAgentConf(self, Conf: AgentConf):

        net = GetNetwork()
        nic = net.GetNicInfo(Ip=Conf.LocalIp)
        if not nic:
            return SetRestReturn(ResultError, Code=CodeFailServerInit, Message=MessageFailServerInit)
        NetDevice = nic['Name']

        ConfString = """[mgs]
%s = %s 
%s = %d 
%s = %s 
%s = %s 
%s = %s 
%s = %d 
%s = %s 
%s = %s
%s = 
%s = 

[monitor]
ServerMonitorInterval = 5000
NetworkMonitorInterval = 5000
DiskMonitorInterval = 5000
ServiceMonitorInterval = 5000        
""" % (KeyPortalHost, Conf.PortalHost, KeyPortalPort, Conf.PortalPort, KeyMQHost, Conf.MQHost,
               KeyMQUser, Conf.MQUser, KeyMQPassword, Conf.MQPassword, KeyMQPort, Conf.MQPort,
               KeyPortalApiKey, Conf.PortalApiKey, KeyManagementNetDev, NetDevice, KeyDefaultNetworkId, KeyServerId)

        with open(MonServicedConfPath, 'w') as f:
            f.write(ConfString)
            f.flush()
        return SetRestReturn(ResultSuccess)


    catch_exceptions()
    def AddServer(self, conf: AgentConf):
        """
        Register Server and update ksanConf
        :param Conf:
        :return:
        """
        self.InitAgentConf(conf)

        #Res, Errmsg, Ret = AddServer(Conf.PortalHost, Conf.PortalPort, Conf.PortalApiKey, '', logger=self.logger)

        #RunServerRegisterCmd = '%s -i %s -m %s -p %d -r %s -q %d -u %s -w %s -k %s ' % (KsanServerRegisterPath,
        #                Conf.LocalIp, Conf.PortalHost, Conf.PortalPort, Conf.MQHost, Conf.MQPort, Conf.MQUser,
        #                Conf.MQPassword, Conf.PortalApiKey)
        #self.logger.debug(RunServerRegisterCmd)
        Hostname = socket.gethostname()
        Res, errmsg, Ret, ServerInfo = RegisterServer(conf.PortalHost, int(conf.PortalPort), conf.PortalApiKey, '',
                                                      Name=Hostname, logger=self.logger)
        Errmsg = ''
        if Res != ResOk:
            Errmsg += 'fail to add Server' + errmsg
            return SetRestReturn(ResultError, Code=CodeFailServerInit, Message=MessageFailServerInit + Errmsg)
        else:
            if Ret.Result == ResultSuccess or Ret.Code == CodeDuplicated:
                UpdateConf(MonServicedConfPath, KeyCommonSection, 'ServerId', ServerInfo.Id, self.logger)
            else:
                Errmsg += 'fail to add Server' + Ret.Message

            return SetRestReturn(Ret.Result, Code=Ret.Code, Message=Ret.Message)


def UpdateServerInfoDetail(PortalIp, PortalPort, PortalApiKey, ServerId, TargetNetwork=None, logger=None):

    # get server info
    Res, Errmsg, Ret, ServerData = GetServerInfo(PortalIp, PortalPort, PortalApiKey, ServerId=ServerId, logger=logger)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:

            if ServerData is not None:
                InputServerInfo = [{'key': 'Name', 'value': ServerData.Name, 'type': str, 'question': 'Insert new server name'},
                                   {'key': 'Description', 'value': ServerData.Description, 'type': str,
                                    'question': 'Insert new server\'s description'}
                                   ]
                for info in InputServerInfo:

                    QuestionString = info['question']
                    ValueType = info['type']
                    DefaultValue = info['value']
                    ServerData.__dict__[info['key']] = get_input(QuestionString, ValueType, DefaultValue)

            Url = '/api/v1/Servers/%s' % ServerId
            server = UpdateServerInfoItems()
            server.Set(ServerData.Name, ServerData.Description, ServerData.CpuModel, ServerData.Clock, ServerData.State, ServerData.Rack,
                       ServerData.MemoryTotal)
            body = jsonpickle.encode(server, make_refs=False)
            Params = body
            Conn = RestApi(PortalIp, PortalPort, Url, authkey=PortalApiKey, params=Params, logger=logger)
            Ret, Errmsg, Data = Conn.put(ItemsHeader=False, ReturnType=ResPonseHeader)
            if Ret != ResOk:
                return Ret, Errmsg
            else:
                if Data.Result != ResultSuccess:
                    return Data.Result, Data.Message
        else:
            return Ret.Result, Ret.Message
    else:
        return Res, Errmsg

    # get network info
    if TargetNetwork is not None:
        Res, Errmsg, NetworkData, ServerId = GetNetworkInterfaceDetailWithId(PortalIp, PortalPort, PortalApiKey, TargetNetwork)
        if Res == ResOk:
            InputNetworkInfo = [{'key': 'Name', 'value': NetworkData.Name, 'type': str, 'question': 'Insert new network name'},
                                {'key': 'Description', 'value': NetworkData.Description, 'type': str,
                                 'question': 'Insert new network\'s description'},
                                {'key': 'MacAddress', 'value': NetworkData.MacAddress, 'type': str,
                                 'question': 'Insert new network\'s mac address'},
                                {'key': 'IpAddress', 'value': NetworkData.IpAddress, 'type': 'ip',
                                 'question': 'Insert new network\'s ipaddress'},
                                {'key': 'SubnetMask', 'value': NetworkData.SubnetMask, 'type': str,
                                 'question': 'Insert new network\'s subnet mask'},
                                {'key': 'Gateway', 'value': NetworkData.Gateway, 'type': 'ip',
                                 'question': 'Insert new network\'s gateway'},
                                {'key': 'Dns1', 'value': NetworkData.Dns1, 'type': str, 'question': 'Insert new network\'s dns1'},
                                {'key': 'Dns2', 'value': NetworkData.Dns2, 'type': str, 'question': 'Insert new network\'s dns2'},
                                {'key': 'Bandwidth', 'value': NetworkData.BandWidth, 'type': int,
                                 'question': 'Insert new network\'s bandwidth'},
                                ]
            for info in InputNetworkInfo:
                QuestionString = info['question']
                ValueType = info['type']
                DefaultValue = info['value']
                NetworkData.__dict__[info['key']] = get_input(QuestionString, ValueType, DefaultValue)

            body = jsonpickle.encode(NetworkData, make_refs=False)
            Url = '/api/v1/Servers/%s/NetworkInterfaces/%s' % (ServerId, TargetNetwork)
            Params = body
            Conn = RestApi(PortalIp, PortalPort, Url, authkey=PortalApiKey, params=Params, logger=logger)
            Ret, Errmsg, Data = Conn.put(ItemsHeader=False, ReturnType=ResPonseHeader)
            if Ret != ResOk:
                return Ret, Errmsg
            else:
                return Data.Result, Data.Message
        else:
            return Res, Errmsg
    else:
        return Res, ResultSuccess




def ServerUtilHandler(Conf, Action, Parser, logger):
    #if Action.lower() == 'init'

    options, args = Parser.parse_args()
    if Conf is None:
        if Action == 'add':
            PortalIp = None
            PortalPort = -1
            PortalApiKey = None
        else:
            print('%s is not configured' % MonServicedConfPath)
            sys.exit(-1)
    else:
        try:
            PortalIp = Conf.PortalHost
            PortalPort = Conf.PortalPort
            PortalApiKey = Conf.PortalApiKey
        except Exception as err:
            print('Fail to get %s %s' % (MonServicedConfPath, str(err)))
            sys.exit(-1)

    if Action is None:
        Parser.print_help()
        sys.exit(-1)

    if Action.lower() == 'add':
        if not options.Host:
            Parser.print_help()
            sys.exit(-1)
        Host = options.Host
        if not IsIpValid(Host):
            Ret, Hostname, Ip = GetHostInfo(hostname=Host)
            if Ret is False:
                print('Invalid Hostname')
                sys.exit(-1)
            else:
                Host = Ip

        Res, Errmsg, Ret = ServerInit(PortalIp, PortalPort, PortalApiKey, Host, logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)
    elif Action.lower() == 'remove':
        if not options.ServerName:
            Parser.print_help()
            sys.exit(-1)
        Res, Errmsg, Ret = RemoveServer(PortalIp, PortalPort, PortalApiKey, Name=options.ServerName, logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)
    elif Action.lower() == 'update':
        if not options.ServerName:
            Parser.print_help()
            sys.exit(-1)

        Res, Errmsg = UpdateServerInfoDetail(PortalIp, PortalPort, PortalApiKey, options.ServerName,
                                            TargetNetwork=options.NetworkName, logger=logger)
        print(Errmsg)

    elif Action.lower() == 'update_state':
        if options.ServerName is None:
            Parser.print_help()
            sys.exit(-1)

        Res, Errmsg, Ret = UpdateServerInfo(PortalIp, PortalPort, PortalApiKey, Name=options.ServerName, State=options.State, logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)
    elif Action.lower() == 'list':
        Detail = False
        if options.Detail:
            Detail = True
        Res, errmsg, Ret, Data = GetAllServerDetailInfo(PortalIp, PortalPort, PortalApiKey, logger=logger)
        if Res == ResOk:
            if Ret.Result == ResultSuccess:
                if options.MoreDetail:
                    Detail = MoreDetailInfo
                elif options.Detail:
                    Detail = DetailInfo
                else:
                    Detail = SimpleInfo

                ShowServerInfo(Data, Detail=Detail)
            else:
                print(Ret.Result, Ret.Message)
        else:
            print(errmsg)
    else:
        Parser.print_help()



