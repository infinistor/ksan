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
from ksan.common.define import *
from ksan.common.init import *
from ksan.common.utils import IsDaemonRunning
from ksan.common.shcommand import *
from ksan.service.service_manage import AddService
import signal

class KsanOsdConfig:
    def __init__(self, LocalIp):
        self.ip = LocalIp
        self.port = 8000
        self.pool_size = 10
        self.obj_dir = 'obj'
        self.trash_dir = 'trash'
        self.write_temp_dir = 'temp'

class KsanOsd:
    def __init__(self, logger):
        self.logger = logger
        self.MonConf = None
        self.OsdConf = None

    def ReadConf(self):
        Ret, HostName, LocalIp = GetHostInfo()
        if Ret is False:
            print('Fail to get host by name')
            sys.exit(-1)

        Conf = KsanOsdConfig(LocalIp)
        Ret, Conf = KsanInitConf(OsdServiceConfPath, Conf, FileType='Normal')
        self.OsdConf = Conf

        Ret, Conf = KsanInitConf(MonServicedConfPath, Conf)
        self.MonConf = Conf

    def Start(self):
        Ret, Pid = IsDaemonRunning(KsanOsdPidPath, CmdLine='ksanOsd.jar')
        if Ret is True:
            print('Already Running')
            return False, 'ksanOsd is alread running'
        else:
            self.ReadConf()
            self.PreRequisite()
            if self.RegisterService() is False:
               sys.exit(-1)

            StartCmd = 'cd %s; nohup java -jar -Dlogback.configurationFile=%s ksanOsd.jar >/dev/null 2>&1 &' % \
                       (KsanBinPath, OsdXmlFilePath)
            os.system(StartCmd)
            time.sleep(2)

            Ret, Pid = IsDaemonRunning(KsanOsdPidPath, CmdLine='ksanOsd.jar')
            if Ret is True:
                return True, ''
            else:
                return False, 'Fail to start ksanOsd'

    def Stop(self):
        Ret, Pid = IsDaemonRunning(KsanOsdPidPath, CmdLine='ksanOsd.jar')
        if Ret is False:
            return False, 'ksanOsd is not running'
        else:
            try:
                os.kill(int(Pid), signal.SIGTERM)
                os.unlink(KsanOsdPidPath)
                print('Done')
                return True, ''
            except OSError as err:
                return False, 'Fail to stop. %s' % err

    def Restart(self):
        self.Stop()
        self.Start()

    def Status(self):
        Ret, Pid = IsDaemonRunning(KsanOsdPidPath, CmdLine='ksanOsd.jar')
        if Ret is True:
            print('ksanOsd ... Ok')
            return True, 'KsanOsd ... Ok'
        else:
            print('ksanOsd ... Not Ok')
            return False, 'KsanOsd ... Not Ok'
    def RegisterService(self):
        ServiceName = self.GetHostName() + '_Osd'
        ServiceType = 'Osd'
        ServiceGroupId = None
        Res, Errmsg, Ret = AddService(self.MonConf.mgs.MgsIp, self.MonConf.mgs.IfsPortalPort, ServiceName, ServiceType, self.MonConf.mgs.ServerId)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
            return True
        else:
            print(Errmsg)
            return False

    def Init(self):
        #Conf = KsanOsdConfig()
        #Ret, Conf = KsanInitConf(OsdServiceConfPath, Conf, FileType='Normal')
        #if Ret is True:
        self.ReadConf()
        self.OsdConf.ip = get_input('Insert Local Ip', 'ip', default=self.OsdConf.ip)
        self.OsdConf.port = get_input('Insert KsanOsd Port', int, default=self.OsdConf.port)
        self.OsdConf.pool_size = get_input('Insert Pool Size', int, default=self.OsdConf.pool_size)
        self.OsdConf.obj_dir = get_input('Insert Object Directory', str, default=self.OsdConf.obj_dir)
        self.OsdConf.trash_dir = get_input('Insert Trash Directory', str, default=self.OsdConf.trash_dir)
        self.OsdConf.write_temp_dir = get_input('Insert Write Temp Directory', str, default=self.OsdConf.write_temp_dir)

        StrOsdConf = """
ip=%s
port=%s
pool_size=%s
obj_dir=%s
trash_dir=%s
write_temp_dir=%s
""" % (self.OsdConf.ip, self.OsdConf.port, self.OsdConf.pool_size, self.OsdConf.obj_dir, self.OsdConf.trash_dir, self.OsdConf.write_temp_dir)
        try:
            with open(OsdServiceConfPath, 'w') as configfile:
                configfile.write(StrOsdConf)
            print('Done')
        except (IOError, OSError) as err:
            print('fail to get config file: %s' % OsdServiceConfPath)
            self.logger.error('fail to get config file: %s' % OsdServiceConfPath)

    def PreRequisite(self):
        IsConfGood = True
        if not os.path.exists(OsdServiceConfPath):
            print('ksanOsd.conf is not found. Init first')
            IsConfGood = False
        else:
            if self.MonConf is not None:
                if self.MonConf.mgs.ServerId == '':
                    print('Local server is not registered. Excute ksanEdge register first')
                    IsConfGood = False
                elif self.MonConf.mgs.DefaultNetworkId == '':
                    print('Local default network is not registered. Excute ksanEdge start first')
                    IsConfGood = False

        out, err = shcall('hostname -I')
        LocalIps = out.rsplit()
        if self.OsdConf.ip not in LocalIps:
            print('%s is invalid local ip' % self.OsdConf.ip)
            IsConfGood = False

        if IsConfGood != True:
            sys.exit(-1)

    def GetHostName(self):
        out, err = shcall('hostname')
        return out[:-1]

    def UpdateConf(self, Body):
        Conf = self.ReadConf()
        #Conf.Set(Body['Config'])
        #with open(S3ConfFile, 'w') as f:
        #    f.write(Conf.Config)
        #print("Update S3 Conf")
        return True, ''
