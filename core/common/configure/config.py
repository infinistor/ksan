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
from const.common import *
from service.service_manage import SetServiceConfig, GetServiceConfig, DsPServiceConf, \
    UpdateServiceConfigVersion, RemoveServiceConfig, GetServiceConfigList, ShowConfigList


def ConfigUtilHandler(Conf, Action, Parser, logger):

    options, args = Parser.parse_args()
    PortalIp = Conf.mgs.PortalIp
    PortalPort = Conf.mgs.PortalPort
    PortalApiKey = Conf.mgs.PortalApiKey
    MqPort = Conf.mgs.MqPort
    MqPassword = Conf.mgs.MqPassword

    if Action is None:
        Parser.print_help()
        sys.exit(-1)

    if not options.ServiceType:
        Parser.print_help()
        sys.exit(-1)
    if options.ServiceType.lower() not in ServiceTypeConversion.keys():
        print('Invalid Service Type. Valid Service Type: %s' % ', '.join(ServiceTypeConversion.values()))
        sys.exit(-1)
    if Action.lower() == 'add':
        ServiceType = ServiceTypeConversion[options.ServiceType.lower()]
        ConfFile = options.ConfFile
        Res, Errmsg, Ret = SetServiceConfig(PortalIp, PortalPort, PortalApiKey, ServiceType, ConfFile=ConfFile, logger=logger)
        if Res != ResOk:
            print(Errmsg)
        else:
            print(Ret.Result, Ret.Message)

    elif Action.lower() == 'get':
        ServiceType = ServiceTypeConversion[options.ServiceType.lower()]
        ConfFile = options.ConfFile
        Res, Errmsg, Ret, Data = GetServiceConfig(PortalIp, PortalPort, PortalApiKey, ServiceType, logger=logger)
        if Res != ResOk:
            print(Errmsg)
        else:
            if(Ret.Result == ResultSuccess):
                DsPServiceConf(Data, ConfFile=ConfFile)
            else:
                print(Ret.Message)

    elif Action.lower() == 'apply':
        if not options.ConfigVersionId:
            Parser.print_help()
            print('config version is required.')
            sys.exit(-1)
        Version = options.ConfigVersionId
        ServiceType = ServiceTypeConversion[options.ServiceType.lower()]
        Res, Errmsg, Ret = UpdateServiceConfigVersion(PortalIp, PortalPort, PortalApiKey, ServiceType, Version, logger=logger)
        print(Ret.Result, Ret.Message)

    elif Action.lower() == 'remove':
        Version = options.ConfigVersionId
        ServiceType = ServiceTypeConversion[options.ServiceType.lower()]
        Res, Errmsg, Ret = RemoveServiceConfig(PortalIp, PortalPort, PortalApiKey, ServiceType, Version, logger=None)
        print(Ret.Result, Ret.Message)

    elif Action.lower() == 'list':
        ServiceType = ServiceTypeConversion[options.ServiceType.lower()]
        Res, Errmsg, Ret, Data = GetServiceConfigList(PortalIp, PortalPort, PortalApiKey, ServiceType, logger=logger)
        if Res != ResOk:
            print(Errmsg)
        else:
            if Ret.Result == ResultSuccess:
                ShowConfigList(Data)
            else:
                print(Ret.Result, Ret.Message)
    else:
        Parser.print_help()
        sys.exit(-1)
