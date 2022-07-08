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
sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from common.init import *
from common.utils import IsDaemonRunning, CheckParams
from common.shcommand import *
from service.service_manage import AddService
import xml.etree.ElementTree as ET
import signal





class KsanMetering:
    def __init__(self, logger):
        self.logger = logger
        self.MonConf = None
        self.GwConf = None
        self.ObjmanagerConf = None
        self.apache_pid_path = ''
        self.recovery_pid_path = ''


    def PreRequest(self):
        # java version chck:
        out, err = shcall("java -version")
        out += str(err)
        if 'openjdk version "11.' not in  out:
            return False, 'java11 is not installed'
        KsanGwBinPath = "%s/%s" % (KsanBinPath, KsanGwBinaryName)
        if not os.path.exists(KsanGwBinPath):
            return False, '%s is not found' % KsanGwBinPath

        return True, ''

    def Start(self):
        ret, errmsg = self.PreRequest()
        if ret is False:
            return ret, errmsg
        Ret, Pid = IsDaemonRunning(KsanGwPidFile, CmdLine=KsanGwBinaryName)
        if Ret is True:
            print('Already Running')
            return False, 'ksanGw is alread running'
        else:
            StartCmd = 'cd %s; nohup java -jar -Dlogback.configurationFile=%s %s >/dev/null 2>&1 &' % \
                       (KsanBinPath, GwXmlFilePath, KsanGwBinaryName)
            os.system(StartCmd)
            time.sleep(2)

            Ret, Pid = IsDaemonRunning(KsanGwPidFile, CmdLine=KsanGwBinaryName)
            if Ret is True:
                return True, ''
            else:
                return False, 'Fail to start ksanGw'

    def Stop(self):
        Ret, Pid = IsDaemonRunning(KsanGwPidFile, CmdLine=KsanGwBinaryName)
        if Ret is False:
            return False, 'ksanGw is not running'
        else:

            try:
                os.kill(int(Pid), signal.SIGTERM)
                os.unlink(KsanGwPidFile)
                print('Done')
                return True, ''
            except OSError as err:
                return False, 'Fail to stop. %s' % err

    def Restart(self):
        Ret, ErrMsg = self.Stop()
        if Ret is not True:
            print(ErrMsg)
        Ret, ErrMsg = self.Start()
        if Ret is not True:
            print(ErrMsg)
        else:
            print('Done')


    def Status(self):
        Ret, Pid = IsDaemonRunning(KsanGwPidFile, CmdLine=KsanGwBinaryName)
        if Ret is True:
            print('KsanGw ... Ok')
            return True, 'KsanGw ... Ok'
        else:
            print('KsanGw ... Not Ok')
            return False, 'KsanGw ... Not Ok'
