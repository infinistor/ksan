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
from common.init import *
from common.utils import IsDaemonRunning
from common.shcommand import *
from service.service_manage import AddService
import signal



class KsanOsdConfig:
    def __init__(self, LocalIp):
        self.pool_size = 650
        self.port = 8000
        self.ec_schedule_minutes = 30000
        self.ec_apply_minutes = 3000000
        self.ec_file_size = 100000
        self.cache_disk = ''
        self.cache_schedule_minutes = 2
        self.cache_file_size = 1024
        self.cache_limit_minutes = 5
        self.trash_schedule_minutes = 5

    def Set(self, pool_size, port, ec_schedule_minutes, ec_apply_minutes, ec_file_size, cache_disk,
            cache_schedule_minutes, cache_file_size, cache_limit_minutes, trash_schedule_minutes):

        self.pool_size = pool_size
        self.port = port
        self.ec_schedule_minutes = ec_schedule_minutes
        self.ec_apply_minutes = ec_apply_minutes
        self.ec_file_size = ec_file_size
        self.cache_disk = cache_disk
        self.cache_schedule_minutes = cache_schedule_minutes
        self.cache_file_size = cache_file_size
        self.cache_limit_minutes = cache_limit_minutes
        self.trash_schedule_minutes = trash_schedule_minutes


class KsanOsd:
    def __init__(self, logger):
        self.logger = logger
        self.MonConf = None
        self.OsdConf = None

    def PreRequest(self):
        # java version chck:
        out, err = shcall("java -version")
        out += str(err)
        if 'openjdk version "11.' not in  out:
            return False, 'java11 is not installed'
        KsanOsdBinPath = "%s/%s" % (KsanBinPath, KsanOsdBinaryName)
        if not os.path.exists(KsanOsdBinPath):
            return False, '%s is not found' % KsanOsdBinPath

        return True, ''


    def Start(self):
        ret, errmsg = self.PreRequest()
        if ret is False:
            return ret, errmsg

        Ret, Pid = IsDaemonRunning(KsanOsdPidFile, CmdLine=KsanOsdBinaryName)
        if Ret is True:
            print('already running')
            return False, 'ksanOsd is already running'
        else:
            StartCmd = 'cd %s; nohup java -jar -Dlogback.configurationFile=%s %s >/dev/null 2>&1 &' % \
                       (KsanBinPath, OsdXmlFilePath, KsanOsdBinaryName)
            self.logger.debug(StartCmd)
            os.system(StartCmd)
            time.sleep(2)

            Ret, Pid = IsDaemonRunning(KsanOsdPidFile, CmdLine=KsanOsdBinaryName)
            if Ret is True:
                return True, ''
            else:
                return False, 'Fail to start ksanOsd'

    def Stop(self, Force=False):
        Ret, Pid = IsDaemonRunning(KsanOsdPidFile, CmdLine=KsanOsdBinaryName)
        if Ret is False:
            if Force is True:
                return True, ''
            else:
                return False, 'ksanOsd is not running'
        else:
            try:
                self.logger.debug('kill %s' % KsanOsdBinaryName)
                os.kill(int(Pid), signal.SIGTERM)
                os.unlink(KsanOsdPidFile)
                print('Done')
                return True, ''
            except OSError as err:
                return False, 'Fail to stop. %s' % err

    def Restart(self):
        RetryCnt = 0
        while True:
            RetryCnt += 1
            Ret, ErrMsg = self.Stop(Force=True)
            if Ret is False:
                if RetryCnt > ServiceContolRetryCount:
                    return False, ErrMsg
                else:
                    time.sleep(IntervalShort)
            else:
                break

        RetryCnt = 0
        while True:
            RetryCnt += 1
            Ret, ErrMsg = self.Start()
            if Ret is False:
                if RetryCnt > ServiceContolRetryCount:
                    return False, ErrMsg
                else:
                    time.sleep(IntervalShort)
            else:
                return Ret, ErrMsg


    def Status(self):
        Ret, Pid = IsDaemonRunning(KsanOsdPidFile, CmdLine=KsanOsdBinaryName)
        if Ret is True:
            print('ksanOsd ... Ok')
            return True, 'KsanOsd ... Ok'
        else:
            print('ksanOsd ... Not Ok')
            return False, 'KsanOsd ... Not Ok'
