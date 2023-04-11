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
if os.path.dirname(os.path.abspath(os.path.dirname(__file__))) not in sys.path:
    sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
#from server.server_api import *
from const.common import *
from common.utils import *


def CbalanceUtilHandler(Conf, Action, Parser, logger):

    options, args = Parser.parse_args()
    PortalIp = Conf.PortalHost
    PortalPort = Conf.PortalPort
    PortalApiKey = Conf.PortalApiKey
    MqPort = Conf.MQPort
    MqPassword = Conf.MQPassword

    if Action is None:
        Parser.print_help()
        sys.exit(-1)

    if Action.lower() == 'cbalance':
        isValid = True
        if not (options.BucketName or options.SrcDiskName or options.EmptyDisk):
            #print('Bucket Name is required')
            isValid = False
        else:
            if options.BucketName:
                if not(options.Key or options.ObjId):
                    isValid = False
                if not(options.DstDiskName or options.SrcDiskName):
                    isValid = False

            elif options.SrcDiskName:
                if not(options.DstDiskName or options.Size):
                    isValid = False
            elif options.EmptyDisk:
                if not(options.SrcDiskName):
                    isValid = False
            else:
                isValid = False

        if isValid is False:
            Parser.print_help()
            sys.exit(-1)

        CbalanceCmd = 'java -jar %s/%s.jar ' % (KsanUtilDirPath, TypeServiceCbalance)

        if options.BucketName:
            CbalanceCmd += '--BucketName %s' % options.BucketName
        if options.EmptyDisk:
            CbalanceCmd += ' --EmptyDisk '

        if options.Key:
            CbalanceCmd += ' --Key %s' % options.Key
        if options.ObjId:
            CbalanceCmd += ' --ObjId %s' % options.ObjId
        if options.VersionId:
            CbalanceCmd += ' --VersionId %s' % options.VersionId
        if options.DstDiskName:
            CbalanceCmd += ' --DstDiskName %s' % options.DstDiskName
        if options.SrcDiskName:
            CbalanceCmd += ' --SrcDiskName %s' % options.SrcDiskName
        if options.Size:
            CbalanceCmd += ' --Size %s' % options.Size

        out, err = shcall(CbalanceCmd)
        print(out, err)
    else:
        Parser.print_help()

