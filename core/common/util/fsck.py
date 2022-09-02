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


import os
import sys
sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from common.shcommand import shcall
from server.server_manage import *
from disk.diskpool_manage import GetDefaultDiskPool
from const.http import ResponseHeaderModule
from const.user import AddUserObject, S3UserObjectModule, S3UserStorageClassObject, S3UserObject, S3UserUpdateObject


def FsckUtilHandler(Conf, Action, Parser, logger):

    options, args = Parser.parse_args()
    PortalIp = Conf.PortalHost
    PortalPort = Conf.PortalPort
    PortalApiKey = Conf.PortalApiKey
    MqPort = Conf.MQPort
    MqPassword = Conf.MQPassword

    if Action is None:
        Parser.print_help()
        sys.exit(-1)

    if Action.lower() == 'fsck':
        if not (options.BucketName or options.DiskName):
            print('Bucket Name or Disk Name are required')
            sys.exit(-1)

        FsckCmd = '%s/%s ' % (KsanUtilDirPath, TypeServiceFsck)
        if options.BucketName:
            FsckCmd += '--BucketName %s' % options.BucketName
        else:
            FsckCmd += '--DiskName %s' % options.DiskName

        if options.CheckOnly:
            FsckCmd += ' --CheckOnly'

        out, err = shcall(FsckCmd)
        print(out, err)
    else:
        Parser.print_help()

