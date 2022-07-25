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

from common.init import *
from common.utils import IsDaemonRunning
from common.define import DictToObject
from common.init import GetConf, GetHostInfo
from common.shcommand import *
from service.service_manage import GetServiceMongoDBConfig
import logging
import xml.etree.ElementTree as ET
import signal


class KsanMongoDB:
    def __init__(self, logger):
        self.logger = logger
        self.ObjmanagerConf = None
        self.apache_pid_path = ''
        self.s3gw_pid_path = ''
        self.MonConf = None
        self.DbConf = None
        self.isLocalPrimary = False
        self.GetMonConf()

    def GetMonConf(self):
        if not os.path.exists(MonServicedConfPath):
            return False, '%s is not found' % MonServicedConfPath

        ret, self.MonConf = GetConf(MonServicedConfPath)
        if ret is False:
            return False, 'fail to get %s' % MonServicedConfPath
        else:
            return True, ''

    def GetMongoDBConf(self):
        Retry = 0
        while True:
            Retry += 1
            Res, ErrMsg, Ret, Data = GetServiceMongoDBConfig(self.MonConf.mgs.PortalIp, int(self.MonConf.mgs.PortalPort),
                                                             self.MonConf.mgs.PortalApiKey, logger=self.logger)
            if Res == ResOk:
                if Ret.Result == ResultSuccess:
                    DbConf = json.loads(Data.Config)
                    self.DbConf = DictToObject(DbConf)
                    self.isLocalPrimayNode()
                    return True, ''
                else:
                    if Retry > 2:
                        return False, 'fail to get MongoDB config %s' % ErrMsg
                    else:
                        time.slee(1)
            else:
                if Retry > 2:
                    return False, 'fail to get MongoDB config'
                else:
                    time.sleep(1)

    def isLocalPrimayNode(self):
        Ret, Hostname, Ip = GetHostInfo()
        if self.DbConf.PrimaryHostName == Hostname:
            self.isLocalPrimary = True

    def PreRequest(self):
        # java version chck:
        if not os.path.exists(KsanMongDbManagerBinPath):
            return False, '%s is not installed' % KsanMongDbManagerBinPath

        MongoScriptDir = '/usr/local/ksan/mongo'
        isValid = True
        for scrip in ['configd.start', 'configd.stop', 'mongos.start', 'mongos.stop', 'shard.start', 'shard.stop']:
            full_path = '%s/%s' % (MongoScriptDir, scrip)
            if not os.path.exists(full_path):
                isValid = False

        if isValid is False:
            Shard1Port = str(self.DbConf.Shard1Port)
            Shard2Port = str(self.DbConf.Shard2Port)
            ConfigServerPort = str(self.DbConf.ConfigServerPort)
            MongoDbPort = str(self.DbConf.MongoDbPort)
            HomeDir = self.DbConf.HomeDir

            Option = " -P %s -S %s -C %s -D %s -H %s %s " % (Shard1Port, Shard2Port, ConfigServerPort, MongoDbPort,
                                                             HomeDir, '-R' if self.isLocalPrimary is True else '' )
            InitCmd = '%s init %s' % (KsanMongDbManagerBinPath, Option)
            out, err = shcall(InitCmd)
            logging.log(logging.INFO, "%s %s %s" % (InitCmd, str(out), str(err)))

            isValid = True

        if not os.path.exists('/usr/bin/mongos'):
            isValid = False

        if isValid is False:
            return False, 'mongodb is not initialized'

        return True, ''

    def Start(self):
        #ret, errmsg = self.PreRequest()
        #if ret is False:
        #    return ret, errmsg
        MongoDbStartCmd = 'nohup %s start &' % KsanMongDbManagerBinPath
        os.system(MongoDbStartCmd)
        return True, 'MongoDB ... Ok'


    def Stop(self):
        MongoDbStopCmd = '%s stop '% KsanMongDbManagerBinPath
        out, err = shcall(MongoDbStopCmd)
        RetryCnt = 0
        while True:
            RetryCnt += 1
            if 'Ok' not in out:
                return True, 'MongoDB ... Ok'
            else:
                if RetryCnt > 5:
                    return False, 'MongoDB ... Not Ok'
                else:
                    time.sleep(2)

    def Restart(self):
        MongoDbRestartCmd = '%s stop; %s start' % (KsanMongDbManagerBinPath, KsanMongDbManagerBinPath)
        out, err = shcall(MongoDbRestartCmd)
        RetryCnt = 0
        while True:
            RetryCnt += 1
            if 'Ok' in out:
                return True, 'MongoDB ... Ok'
            else:
                if RetryCnt > 5:
                    return False, 'MongoDB ... Not Ok'
                else:
                    time.sleep(2)

    def Status(self):
        #Ret, Pid = IsDaemonRunning(KsanGwPidPath, CmdLine=KsanGwBinaryName)
        MongoDbStatusCmd = '%s status' % KsanMongDbManagerBinPath
        out, err = shcall(MongoDbStatusCmd)
        if 'Ok' in out:
            print('MongoDB ... Ok')
            return True, 'MongoDB ... Ok'
        else:
            print('MongoDB ... Not Ok')
            return False, 'MongoDB ... Not Ok'

