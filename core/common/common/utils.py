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

import os, sys
import re
from common.shcommand import shcall
import signal


def AvoidSigInt():
    signal.signal(signal.SIGINT, SigIntHandler)

def SigIntHandler(signum, frame):
    signal_received = (signum, frame)
    sys.exit(-1)


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

def CreatePidFile(Pid, File):
    with open(File, 'w') as f:
        f.write("%d" % Pid)
        f.flush()


def IsDaemonRunningWithSystemd(Daemon):
    PidFinder = re.compile("Main PID: ([\d]+)")
    Cmd = 'systemctl status %si|grep --color=never -e Active: -e "Main PID:"' % Daemon
    out, err = shcall(Cmd)
    Pid = PidFinder.search(out)
    if Pid:
        Pid = Pid.groups()[0]
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


PidInit = None
PidNotFound = -2
MainDaemonList = ['GW', 'OSD', 'RABBITMQ', 'MON', 'EDGE', 'DB']


def GetCmdOfDaemon(daemon):
    global g_isPortalRunning

    if daemon in ['mariadb']:
        return 'systemctl start mariadb'
    elif daemon in ['ksan-osd.jar']:
        return '/usr/local/ksan/bin/ksanOsd start'
    elif daemon in ['ksan-gw.jar']:
        return '/usr/local/ksan/bin/ksanGw start'
    elif daemon in ['ksanMonitor']:
        return '/usr/local/ksan/bin/ksanMonitor start'
    elif daemon in ['ksanAgent']:
        return '/usr/local/ksan/bin/ksanAgent start'
    else:
        return None


def UpdateMonitoringList(MonDaemonDict, DaemonKeyList, daemon_black_list):
    global g_mgs
    """
    Set Each Daemon's property(pid parsing type, command to start, current pid with init)
    :param MonDaemonDict:
    :param DaemonKeyList:
    :return:
    """
    global KeyDaemonMap

    DaemonToMonitor = list()
    # Add New Daemon list
    for Key in DaemonKeyList:  # MGS, MDS, OSD, ...
        if Key not in MainDaemonList or Key in daemon_black_list:
            continue

        for daemon in KeyDaemonMap[Key]: # ifs_mgsd, ifs_mdsd, ifs_osdd, ...
            if daemon in daemon_black_list:
                continue
            DaemonToMonitor.append(daemon)
            if daemon not in MonDaemonDict:
                MonDaemonDict[daemon] = dict()
                if daemon == 'ifss-jmds':
                    MonDaemonDict[daemon]['PidFinder'] = re.compile("([\d]+) java [\s\/\d\w\-_=\.]+%s" % daemon)
                else:
                    MonDaemonDict[daemon]['PidFinder'] = re.compile("([\d]+)[\s][\s\/\d\w\-_=\.]+%s" % daemon)
                MonDaemonDict[daemon]['Pid'] = PidInit
                MonDaemonDict[daemon]['Cmd'] = GetCmdOfDaemon(daemon)
                g_monlogger.error("([\d]+)[\s][\s\/\d\w\-_=\.]+%s %s" % (daemon, GetCmdOfDaemon(daemon)))


    # Delete No need to Monitor list
    for daemon in MonDaemonDict.keys():
        if daemon not in DaemonToMonitor:
            try:
                del MonDaemonDict[daemon]
            except KeyError:
                pass


def GetPsResult():
    InfiniPsCmd = "ps --ppid 1 -o pid,args  |grep -e 'ifs_' -e 'mysql' -e 'mariadbd' -e 'httpd' -e 'dotnet' -e 'memcached' " \
                  "-e 'ifss-' -e 'ifs-' -e 'ifs_logbucket' -e 'smbd' -e '/usr/sbin/haproxy'  " \
                  "-e 'ifs_' |grep -v ' vim ' |grep -v ' vi '"

    out, err = shcall(InfiniPsCmd)
    g_monlogger.error('ps running...')

    return out.rsplit("\n")


def CheckDaemonIsRunning(MonDaemonDict, snmp_trap_daemon_list):
    """
    Check Daemon Status with Checking pid exists in /proc and then check if daemon name exists in /proc/pid/cmdline
    Pid Status are PidInit(new registered), PidNotFound(not exists) otherwise normal.

    :param MonDaemonDict:
    :return: if Pid is not found in /proc/ or New daemon is added return False orthersize True
    """
    global g_monlogger

    TerminatedFlg = False
    for daemon, detail in MonDaemonDict.items():
        if detail['Pid'] == PidInit:
            TerminatedFlg = True
            g_monlogger.error('new daemon %s(init pid :%s) is registered' % (daemon, detail['Pid']))
            continue
        if not os.path.exists("/proc/%s" % detail['Pid']):
            if detail['Pid'] == PidNotFound:
                g_monlogger.error('fail to start %s' % daemon)
            else:
                g_monlogger.error('%s is terminated. Pid %s is not found in /proc' % (daemon, detail['Pid']))


            detail['Pid'] = PidNotFound
            shcall(detail['Cmd'])
            TerminatedFlg = True
        else:
            try:
                with open("/proc/%s/cmdline" % detail['Pid']) as f:
                    CmdLine = f.read()
                    if daemon not in CmdLine:
                        print('%s is terminated. Pid %s is invalid ' % (daemon, detail['Pid']))
                        g_monlogger.error('%s is terminated. Pid %s is invalid ' % (daemon, detail['Pid']))
                        detail['Pid'] = PidNotFound
                        shcall(detail['Cmd'])
                        TerminatedFlg = True
            except Exception as err:
                print('%s is terminated Pid %s is not accessable %s' % (daemon, detail['Pid'], str(err)))
                g_monlogger.error('%s is terminated Pid %s is not accessable %s' % (daemon, detail['Pid'], str(err)))
                detail['Pid'] = PidNotFound
                shcall(detail['Cmd'])
                TerminatedFlg = True

    return TerminatedFlg

def ParsingPidFromPsResult(MonDaemonDict, PsResult, snmp_trap_daemon_list):
    """
    Parsing Pid from ps command(ps --ppid 1 -o pid,args). only to parsing pid whose ppid is 1.
    Looping line by line from ps result and check daemon is in the line and parsing pid.
    :param MonDaemonDict:
    :param PsResult:
    :return:
    """
    for daemon, detail in MonDaemonDict.items():
        if detail['Pid'] == PidInit or detail['Pid'] == PidNotFound:
            Found = False
            for psline in PsResult:
                if daemon in psline:
                    Pid = detail['PidFinder'].search(psline)
                    if Pid:
                        detail['Pid'] = Pid.groups()[0]
                        g_monlogger.error('daemon %s success to get pid %s' % (daemon, detail['Pid']))
                        Found = True
                        break
            if Found is False:
                g_monlogger.error('daemon %s fail to get pid %s' % (daemon, detail['Pid']))
                detail['Pid'] = PidNotFound





