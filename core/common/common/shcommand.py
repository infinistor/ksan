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


import pdb
from subprocess import Popen, PIPE
import socket
import re

def shcall(command):
    """
    Executes a command and returns the result
    """
    try:
        p = Popen(command, shell=True, stdout=PIPE, stderr=PIPE, close_fds=True, universal_newlines=True)
    except (OSError, ValueError) as err:
        return '', err
    return p.communicate()


def GetHostInfo(hostname=None, ip=None):
    try:
        if hostname is None:
            hostname = socket.gethostname()
        if ip is None:
            ip = socket.gethostbyname(hostname)
        return True, hostname, ip
    except socket.error as err:
        return False, str(err), None


def UpdateEtcHosts(ClusterMembers:list, Mode): # ClusterMembers: [('192.168.11.11', 'osd1')]
    OmittedHostNameList = list()
    lines = ''
    with open("/etc/hosts", 'r') as f:
        lines = f.readlines()

    if Mode == 'add':
        for member in ClusterMembers:
            ip, hostname = member
            IpFinder = re.compile("^%s[\s]" % ip)
            HostNameFinder1 = re.compile("[\s]%s[\s]" % hostname)
            HostNameFinder2 = re.compile("[\s]%s$" % hostname)
            Found = False
            for line in lines:
                trimedlist = line.rsplit()
                trimedstring = " ".join(trimedlist)
                ValidIp = IpFinder.search(trimedstring)
                HostNameType1 = HostNameFinder1.search(trimedstring)
                HostNameType2 = HostNameFinder2.search(trimedstring)
                if ValidIp and (HostNameType1 or HostNameType2):
                    Found = True
                    break
            if Found is False:
                OmittedHostNameList.append("%s %s" % (ip, hostname))
        if len(OmittedHostNameList):
            with open("/etc/hosts", 'a+') as f:
                for hostname in OmittedHostNameList:
                    f.write("%s\n" % hostname)

    elif Mode == 'remove':
        UpdatedLines = ''
        ip, hostname = ClusterMembers[0]
        HostNameFinder1 = re.compile("[\s]%s[\s]" % hostname)
        HostNameFinder2 = re.compile("[\s]%s$" % hostname)

        Removed = False
        for line in lines:
            if line == '\n' or line == ' ':
                continue

            trimedlist = line.rsplit()
            trimedstring = " ".join(trimedlist)

            HostNameType1 = HostNameFinder1.search(trimedstring)
            HostNameType2 = HostNameFinder2.search(trimedstring)
            if HostNameType1 or HostNameType2:
                Removed = True
                continue

            else:
                UpdatedLines += line

        if Removed is True:
            with open("/etc/hosts", 'w') as f:
                f.write("%s\n" % UpdatedLines)
    else:
        print('Invalid Mode')
