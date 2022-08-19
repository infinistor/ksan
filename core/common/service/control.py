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
import time
sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from const.common import *
from common.utils import IsDaemonRunning, CheckParams
from common.shcommand import *
from service.service_manage import AddService
import xml.etree.ElementTree as ET
import signal

class ServiceUnit:
    def __init__(self, logger, ServiceType):
        self.logger = logger
        self.ServiceType = ServiceType
        self.ServiceUnit = None
        self.GetServiceUnit()

    def GetServiceUnit(self):
        if self.ServiceType == TypeServiceS3:
            self.ServiceUnit = ServiceUnitKsanS3
        elif self.ServiceType == TypeServiceOSD:
            self.ServiceUnit = ServiceUnitKsanOSD

    def Start(self):
        Cmd = 'systemctl start %s' % self.ServiceUnit
        self.logger.error(Cmd)
        ret = self.Status()
        if ret is True:
            return False, '%s is already running' % self.ServiceType

        out, err = shcall(Cmd)
        RetryCnt = 0
        while True:
            RetryCnt += 1
            ret = self.Status()
            if ret is True:
                return True, ''
            else:
                if RetryCnt > ServiceContolRetryCount:
                    return False, 'fail to restart %s' % self.ServiceType
                else:
                    time.sleep(IntervalShort)


    def Stop(self):
        Cmd = 'systemctl stop %s' % self.ServiceUnit
        self.logger.error(Cmd)
        ret = self.Status()
        if ret is False:
            return False, '%s is already stopped' % self.ServiceType
        out, err = shcall(Cmd)
        RetryCnt = 0
        while True:
            RetryCnt += 1
            ret = self.Status()
            if ret is False:
                return True, ''
            else:
                if RetryCnt > ServiceContolRetryCount:
                    return False, 'fail to stop %s' % self.ServiceType
                else:
                    time.sleep(IntervalShort)

    def Restart(self):
        Cmd = 'systemctl restart %s' % self.ServiceUnit
        self.logger.error(Cmd)
        out, err = shcall(Cmd)

        RetryCnt = 0
        while True:
            RetryCnt += 1
            ret = self.Status()
            if ret is True:
                return True, ''
            else:
                if RetryCnt > ServiceContolRetryCount:
                    return False, 'fail to restart %s' % self.ServiceType
                else:
                    time.sleep(IntervalShort)

    def Status(self):
        Cmd = 'systemctl status %s' % self.ServiceUnit
        out, err = shcall(Cmd)
        self.logger.error(Cmd + out)
        if 'running' in out:
            return True
        else:
            return False