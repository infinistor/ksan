#!/usr/bin/python3
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
import re

def IsDaemonRunning(PidPath, CmdLine=None):
    """
    Check if pid exists in /proc and cmdline is valid.
    PidPath: pid file under /var/run
    return True for running, False for not not running & pid
    """
    if not os.path.exists(PidPath):
        return False, None

    Pid = None
    with open(PidPath, 'r') as f:
        Pid = f.read()

    if CmdLine:
        if Pid.endswith('\n'):
            Pid = Pid[:-1]
        ProcFdCmdLine = '/proc/%s/cmdline' % Pid
        if not os.path.exists(ProcFdCmdLine):
            return False, None

        with open(ProcFdCmdLine, 'r') as f:
            cmd_line = f.read()
            if CmdLine in cmd_line:
                return True, Pid
            else:
                return False, None
    else:
        ProcFd = '/proc/%s' % Pid
        if os.path.exists(ProcFd):
            return True, Pid
        else:
            return False, None

def IsIpValid(Ip):
    IpFinder = re.compile("([\d]{1,3}\.[\d]{1,3}\.[\d]{1,3}\.[\d]{1,3})")
    ThreeDigitNumberFiler = re.compile("([\d]+)")
    try:
        Found = IpFinder.search(Ip)
        if Found:
            Found = Found.groups()[0]
            if len(Ip) != len(Found):
                return False
            Found = ThreeDigitNumberFiler.findall(Ip)

            if int(Found[0]) < 1:
                return False
            for Digit in Found:
                if int(Digit) > 254:
                    return False
                if len(Digit) > 1 and Digit.startswith('0'):
                    return False

            return True
        else:
            return False
    except Exception as err:
        print(err)
        return False

def IsUrlValid(Url):
    UrlFinder = re.compile("http[s]{0,1}://0.0.0.0:[\d]+")
    if UrlFinder.search(Url):
        return True
    else:
        return False

def CheckParams(parms, keys):
    omitted = list()
    for key in keys:
        if key not in parms: omitted.append(key);
    if len(omitted) > 0:
        return ','.join(omitted)
    else:
        return None
