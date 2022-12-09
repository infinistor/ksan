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
from portal_api.apis import *
from disk.diskpool_manage import GetDiskPoolInfo, ShowDiskPoolInfoNew
from user.user_manage import GetS3UserInfo, ShowS3UserInfo
from const.common import ResOk, ResultSuccess, MoreDetailInfo, SimpleInfo, DetailInfo

def ShowSystemInfo(PortalIp, PortalPort, PortalApiKey, logger, Detail=False):
    # Get Server Info
    if Detail is False or Detail is None:
        SysinfoDisp = True
    else:
        SysinfoDisp = False

    DispLevel = True
    print("\n[KSAN Server Information]")
    Res, errmsg, Ret, Data = GetAllServerDetailInfo(PortalIp, PortalPort, PortalApiKey, logger=logger)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            ShowServerInfo(Data, Detail=DispLevel, SysinfoDisp=SysinfoDisp)
        else:
            print(Ret.Result, Ret.Message)
    else:
        print(errmsg)

    # Get Service Info
    print("\n[KSAN Service Information]")
    #Res, Errmsg, Ret, Data = GetAllServerDetailInfo(PortalIp, PortalPort, PortalApiKey, logger=logger)
    Res, Errmsg, Ret, Data = GetServiceInfo(PortalIp, PortalPort, PortalApiKey, logger=logger)

    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            ShowServiceInfoWithServerInfo(Data, Detail=DispLevel, SysinfoDisp=SysinfoDisp)
        else:
            print(Ret.Result, Ret.Message)
    else:
        print(Errmsg)

    # Diskpool info
    print("\n[KSAN Diskpool Information]")
    Res, Errmsg, Ret, DiskPoolList = GetDiskPoolInfo(PortalIp, PortalPort, PortalApiKey, logger=logger)
    if Res != ResOk:
        print(Errmsg)
    else:
        Detail = SimpleInfo
        ShowDiskPoolInfoNew(DiskPoolList, Detail=Detail, SysinfoDsp=True)

    print("\n[KSAN User Information]")
    Res, Errmsg, Ret, Users = GetS3UserInfo(PortalIp, PortalPort, PortalApiKey, logger=logger)
    if Res != ResOk:
        print(Errmsg)
    else:
        if Ret.Result != ResultSuccess:
            print(Ret.Message)
        else:
            ShowS3UserInfo(Users, Detail=True, SysinfoDisp=SysinfoDisp)
