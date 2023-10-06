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
from subprocess import Popen, PIPE
import socket
import signal
import logging
from functools import wraps
import traceback
from common.define import *
from const.common import *
from configparser import ConfigParser
import fcntl
import getpass
import requests
import urllib3
import json, jsonpickle
import netifaces
import dns.resolver
import atexit



LogFilePath = '/var/log/ksan/agent/agent.log'

class Logging(object):
    def __init__(self, instance=__name__, logfile=LogFilePath, loglevel='error'):
        try:
            self.logfile = logfile
            if loglevel == 'debug':
                self.loglevel = logging.DEBUG
            else:
                self.loglevel = logging.ERROR
            self.logger = logging.getLogger(instance)
            self.set_loglevel()
            formatter = logging.Formatter('%(asctime)s %(filename)s(%(funcName)s:%(lineno)d) %(levelname)s - %(message)s')
            self.fh = logging.FileHandler(logfile)
            self.fh.setFormatter(formatter)
            self.logger.addHandler(self.fh)
        except Exception as err:
            print(err, sys.exc_info()[2].tb_lineno)

    def set_loglevel(self):
        self.logger.setLevel(self.loglevel)
        logging.addLevelName(45, 'INFO')
        logging.INFO = 45

    def create(self):
        return self.logger

    def get_logger(self, name=__name__):
        return logging.getLogger(name)


def catch_exceptions():
    logger = logging.getLogger('common.log')

    def wrapper(func):
        @wraps(func)
        def decorateor(*args, **kwargs):
            try:
                result = func(*args, **kwargs)
                return result
            except Exception as err:
                exc_type, exc_obj, exc_tb = sys.exc_info()
                logger.error("%s %s %s " % (str(err), traceback.extract_tb(exc_tb)[-1][0], traceback.extract_tb(exc_tb)[-1][1]))
                return None
        return decorateor
    return wrapper


def AvoidSigInt():
    signal.signal(signal.SIGINT, SigIntHandler)

def SigIntHandler(signum, frame):
    signal_received = (signum, frame)
    sys.exit(-1)



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

def isStringIp(String):
    IpFinder = re.compile("([\d]{1,3}\.[\d]{1,3}\.[\d]{1,3}\.[\d]{1,3})")
    IpType = IpFinder.search(String)
    if IpType:
        return True
    else:
        return False

def IdFinder(String):
    IdFinder = re.compile("([\d\w]{8}-[\d\w]{4}-[\d\w]{4}-[\d\w]{4}-[\d\w]{12})")
    IdType = IdFinder.search(String)
    if IdType:
        Id = IdType.groups()[0]
        return True, Id
    else:
        return False, None


def GetIpFromHostname(String):
    """
    if String is ip type, return String otherwise return ip from String
    :param hostname:
    :return:
    """
    if isStringIp(String):
        return String
    else:
        return GetHostInfo(hostname=String)


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
class clr:
    okpur = '\033[95m'
    okbl = '\033[94m'
    warnye = '\033[93m'
    okgr = '\033[92m'
    badre = '\033[91m'
    end = '\033[0m'



#####  Conversion Dict to Object Class #####
@catch_exceptions()
class DictToObject(object):

    def __init__(self, myDict):
        for key, value in myDict.items():
            if type(value) == dict:
                setattr(self, key, DictToObject(value))
            else:
                if isinstance(value, str) and value.isdigit():
                    value = int(value)
                setattr(self, key, value)


@catch_exceptions()
def read_conf(path):
    """
    read conf file
    """
    if not os.path.exists(path):
        print('init first')
        return False, None

    config = ConfigParser(delimiters="=", strict=True, allow_no_value=True)
    config.optionxform = str
    config.read(path)
    conf = dict()
    for section in config.sections():
        conf[section] = dict()
        for option in config.options(section):
            conf[section][option] = config.get(section, option)
    return True, conf


@catch_exceptions()
def read_conf_normal(path, fo=None, lock_sh=False):
    """
    normal key=value type conf file
    :param path:
    :param fo:
    :param lock_sh:
    :return: dict
    """

    config = {}
    openfile = True
    if fo: openfile = False
    if openfile:
        try:
            fo = open(path)
        except IOError as err:
            return False, config
    if lock_sh: fcntl.flock(fo.fileno(), fcntl.LOCK_SH)
    lines = fo.readlines()
    for line in lines:
        #mo = re.search('(\w+)\s*=\s*((?:\w|/|\.\-_)+)', line)
        #if mo is None and not line.isspace():
        #    #mo = re.search('(\w*)\s*=\s*\"((?:\w|\s|-|:|\(|\)\._\-)+)\"', line)
        if line.endswith("=\n") or line.endswith("= \n"):
            mo = re.search("([\d\w_\-\.]+)[\s]{0,2}=[\s]{0,10}", line)
            if mo:
                config[mo.group(1)] = ''
        else:
            mo = re.search("([\d\w_\-\.]+)[\s]{0,2}=[\s]{0,2}([/\d\w_\-\.:]+)", line)
            if mo:
                config[mo.group(1)] = mo.group(2)

    if lock_sh: fcntl.flock(fo.fileno(), fcntl.LOCK_UN)
    if openfile: fo.close()
    return True, config



@catch_exceptions()
def GetConf(filename, FileType=ConfigTypeINI, ReturnType=ConfigTypeObject):
    """
    filename: configuration file Path
    """
    if not os.path.exists(filename):
        return False, None
    if FileType == ConfigTypeINI:
        Res, Conf = read_conf(filename)
    else:
        Res, Conf = read_conf_normal(filename)

    if Res is True:
        if ReturnType == ConfigTypeObject:
            return Res, DictToObject(Conf)
        else:
            # return dictionary type
            return Res, Conf

    else:
        return Res, None



def get_input(Description, type, default=None, ValidAnsList=None, ValueComment='', force=True):
    """
    get info from user
    :param Description:
    :param type:
    :param default:
    :param ValidAnsList:
    :return:
    """
    try:
        while True:
            if type == 'pwd':
                ans = getpass.getpass(Description + '(%sdefault: %s):' % (ValueComment, str(default) if default is not None else ':'))
            else:
                ans = input(Description + '(%sdefault: %s):' % (ValueComment, str(default) if default is not None else ':'))

            if ans in ['', None]:
                if type in ['host']:
                    ans = default

            if ans:
                if '"' in ans or '\\' in ans:
                    print(" %s is invalid character" % ans)
                    continue
                if type == int:
                    if ans.isdigit():
                        return int(ans)
                    else:
                        print('Invalid Digit type')
                elif type == 'ip':
                    if IsIpValid(ans):
                        return ans
                    else:
                        print('Invalid Ip Type')
                elif type == 'host':
                    if IsIpValid(ans):
                        ret, hostname, ip = GetHostInfo(ip=ans)
                    else:
                        ret, hostname, ip = GetHostInfo(hostname=ans)

                    if ret is False:
                        print('Invalid Host')
                        continue
                    else:
                        return ip

                elif type == 'url':
                    if IsUrlValid(ans):
                        return ans
                    else:
                        print('Invalid Url Type')
                else:
                    if ValidAnsList is not None:
                        if ans not in ValidAnsList:
                            print('Invalid Answer Type. Only %s is supported' % ', '.join(ValidAnsList))
                            continue
                    else:
                        if ans.lower() == 'null':
                            return ''

                    return ans
            else:
                if default == '':
                    if type in ['pwd', 'net_device', 'host']:
                        print('Invalid %s type' % type)
                        continue

                if default is not None:
                    return default

                return ans
    except Exception as err:
        print(err, sys.exc_info()[2].tb_lineno)


def isSystemdServiceRunning(ServiceName):
    status = "systemctl status %s|grep Active:" % ServiceName
    out, err = shcall(status)
    if 'running' in out:
        return True
    else:
        return False


def isValidConfig(ConfPath, *CheckKeys, **kwargs):
    """
    check if conf is valid with CheckKey
    :param ConfPath: file path
    :param **kwargs: check if the value is valid with CheckKeys
    :return: bool
    """

    ret, conf = GetConf(ConfPath)
    if ret is False:
        return ret, conf
    else:
        isValid = True
        for key in CheckKeys:
            if conf.mgs.__dict__[key] is None or conf.mgs.__dict__[key] == '':
                isValid = False

        for key, val in kwargs:
            if conf.mgs.__dict__[key] != val:
                isValid = False

        return isValid, conf



def UpdateConf(Path, Section, Key, Value, logger, Force=True):
    """
    Update NetowrkId and ServerId in ksanMonitor.conf
    :param Path:
    :param Section:
    :param Key:
    :param Value:
    :param Force:
    :return:
    """
    res, conf = read_conf(Path)
    if res is True:
        if Section not in conf:
            logger.error('Invalid Section: %s in %s' % (Section, Path))
            return False
        if Key not in conf[Section] and Force is False:
            logger.error('Invalid Key: %s in %s' % (Key, Path))
            return False
        conf[Section][Key] = Value
        config = ConfigParser()
        config.optionxform = str
        for section in conf.keys():
            config[section] = dict()
            for key, val in conf[section].items():
                config[section][key] = val

        with open(Path, 'w') as configfile:
            config.write(configfile)
        return True
    else:
        return False



@catch_exceptions()
class ResPonseHeader(object):
    def __init__(self, dic, logger=None):
        try:
            self.IsNeedLogin = dic['IsNeedLogin']
            self.AccessDenied = dic['AccessDenied']
            self.Result = dic['Result']
            self.Code = dic['Code']
            self.Message = dic['Message']
            if 'Data' in dic:
                self.Data = dic['Data']
        except KeyError as err:
            if logger is not None:
                logger.error(err, sys.exc_info()[2].tb_lineno)
            else:
                print(err, sys.exc_info()[0].tb_lineno)


#####  Items Header of Response Body Class #####
class ResPonseItemsHeader(object):
    def __init__(self, dic, logger=None):
        try:
            self.TotalCount = dic['TotalCount']
            self.Skips = dic['Skips']
            self.PageNo = dic['PageNo']
            self.CountPerPage = dic['CountPerPage']
            self.PagePerSection = dic['PagePerSection']
            self.TotalPage = dic['TotalPage']
            self.StartPageNo = dic['StartPageNo']
            self.EndPageNo = dic['EndPageNo']
            self.PageNos = dic['PageNos']
            self.HavePreviousPage = dic['HavePreviousPage']
            self.HaveNextPage = dic['HaveNextPage']
            self.HavePreviousPageSection = dic['HavePreviousPageSection']
            self.HaveNextPageSection = dic['HaveNextPageSection']
            self.Items = dic['Items']
        except KeyError as err:
            if logger is not None:
                logger.error(err, sys.exc_info()[2].tb_lineno)
            else:
                print(err, sys.exc_info()[0].tb_lineno)


@catch_exceptions()
class GetApiResult:
    def __init__(self, Header, ItemHeader, Items):
        self.Header = Header
        self.ItemHeader = ItemHeader
        self.Items = Items


#AuthKey = "5de46d7ccd5d0954fad7d11ffc22a417e2784cbedd9f1dae3992a46e97b367e8"
IfsPortalIp = None
IfsPortalPort = None

# http response status
CONTINUE = 100
SWITCHING_PROTOCOLS = 101
PROCESSING = 102
OK = 200
CREATED = 201
ACCEPTED = 202
NON_AUTHORITATIVE_INFORMATION = 203
NO_CONTENT = 204
RESET_CONTENT = 205
PARTIAL_CONTENT = 206
MULTI_STATUS = 207
IM_USED = 226
MULTIPLE_CHOICES = 300
MOVED_PERMANENTLY = 301
FOUND = 302
SEE_OTHER = 303
NOT_MODIFIED = 304
USE_PROXY = 305
TEMPORARY_REDIRECT = 307
BAD_REQUEST = 400
UNAUTHORIZED = 401
PAYMENT_REQUIRED = 402
FORBIDDEN = 403
NOT_FOUND = 404
METHOD_NOT_ALLOWED = 405
NOT_ACCEPTABLE = 406
PROXY_AUTHENTICATION_REQUIRED = 407
REQUEST_TIMEOUT = 408
CONFLICT = 409
GONE = 410
LENGTH_REQUIRED = 411
PRECONDITION_FAILED = 412
REQUEST_ENTITY_TOO_LARGE = 413
REQUEST_URI_TOO_LONG = 414
UNSUPPORTED_MEDIA_TYPE = 415
REQUESTED_RANGE_NOT_SATISFIABLE = 416
EXPECTATION_FAILED = 417
UNPROCESSABLE_ENTITY = 422
LOCKED = 423
FAILED_DEPENDENCY = 424
UPGRADE_REQUIRED = 426
PRECONDITION_REQUIRED = 428
TOO_MANY_REQUESTS = 429
REQUEST_HEADER_FIELDS_TOO_LARGE = 431
INTERNAL_SERVER_ERROR = 500
NOT_IMPLEMENTED = 501
BAD_GATEWAY = 502
SERVICE_UNAVAILABLE = 503
GATEWAY_TIMEOUT = 504
HTTP_VERSION_NOT_SUPPORTED = 505
INSUFFICIENT_STORAGE = 507
NOT_EXTENDED = 510
NETWORK_AUTHENTICATION_REQUIRED = 511


class RestApi:

    def __init__(self, ip, port, url, header=None, params=None, protocol='https', authkey=None, logger=None):
        self._ip = ip
        self._port = port
        self._params = params
        self._protocol = protocol
        self._url = url
        self.logger = logger
        if self.logger is None:
            self.logger = Logging().get_logger()

        if header is None:
            header = dict()
        header[HeaderAuth] = authkey

        self._header = header
        if params is None:
            params = dict()
        self._params = params

    def get(self, ItemsHeader=True, ReturnType=None):
        url = "%s://%s:%d%s" % (self._protocol, self._ip, self._port, self._url)
        urllib3.disable_warnings()
        err_msg = ''
        try:
            r = requests.get(url, params=self._params, verify=False, headers=self._header)
            self.logging_request_info(GetMethod, r)
            ret = r.content.decode(UTF8)
            Data = json.loads(ret)
            Ret = self.GetResponse(Data,ItemsHeader=ItemsHeader, ReturnType=ReturnType)
            return ResOk, '', Ret
        except requests.ConnectionError as err:
            self.logger.error("Connection Error %s" % err)
            return ResConnectionErrorCode, ResConnectionErrorMsg,None
        except requests.URLRequired as err:
            self.logger.error("UrlRequired Error %s" % err)
            return ResInvalidCode, ResInvalidMsg, None
        except (requests.ConnectTimeout, requests.ReadTimeout, requests.Timeout) as err:
            self.logger.error("Timeout Error %s" % err)
            return ResTimeErrorCode, ResTimeErrorCodeMsg, None
        except Exception as err:
            self.logger.error("Other Error %s" % err)
            return ResEtcErrorCode, ResEtcErrorMsg, None

    def post(self,ItemsHeader=True, ReturnType=None):
        url = "%s://%s:%d%s" % (self._protocol, self._ip, self._port, self._url)
        urllib3.disable_warnings()
        try:
            self._header[HeaderContentType] = 'application/json'
            # self._params = json.dumps(self._params)
            r = requests.post(url=url, data=self._params, headers=self._header, verify=False)
            self.logging_request_info(PostMethod, r)
            ret = r.content.decode(UTF8)
            Data = json.loads(ret)
            Ret = self.GetResponse(Data,ItemsHeader=ItemsHeader, ReturnType=ReturnType)
            return ResOk, '', Ret

        except requests.ConnectionError as err:
            self.logger.error("Connection Error %s" % err)
            return ResConnectionErrorCode, ResTimeErrorCodeMsg,None
        except requests.URLRequired as err:
            self.logger.error("UrlRequired Error %s" % err)
            return ResInvalidCode, ResInvalidMsg, None
        except (requests.ConnectTimeout, requests.ReadTimeout, requests.Timeout) as err:
            self.logger.error("Timeout Error %s" % err)
            return ResTimeErrorCode, ResTimeErrorCodeMsg, None
        except Exception as err:
            self.logger.error("Other Error %s" % err)
            return ResEtcErrorCode, ResEtcErrorMsg, None

    def put(self,ItemsHeader=True, ReturnType=None):
        url = "%s://%s:%d%s" % (self._protocol, self._ip, self._port, self._url)
        urllib3.disable_warnings()
        try:
            self._header[HeaderContentType] = 'application/json-patch+json'
            #self._params = json.dumps(self._params)
            r = requests.put(url=url, data=self._params, headers=self._header, verify=False)
            self.logging_request_info(PutMethod, r)
            ret = r.content.decode(UTF8)
            Data = json.loads(ret)
            Ret = self.GetResponse(Data,ItemsHeader=ItemsHeader, ReturnType=ReturnType)
            return ResOk, '', Ret
        except requests.ConnectionError as err:
            self.logger.error("Connection Error %s" % err)
            return ResConnectionErrorCode, ResTimeErrorCodeMsg,None
        except requests.URLRequired as err:
            self.logger.error("UrlRequired Error %s" % err)
            return ResInvalidCode, ResInvalidMsg, None
        except (requests.ConnectTimeout, requests.ReadTimeout, requests.Timeout) as err:
            self.logger.error("Timeout Error %s" % err)
            return ResTimeErrorCode, ResTimeErrorCodeMsg, None
        except Exception as err:
            self.logger.error("Other Error %s" % err)
            return ResEtcErrorCode, ResEtcErrorMsg, None

    @catch_exceptions()
    def delete(self, ItemsHeader=True, ReturnType=None):
        url = "%s://%s:%d%s" % (self._protocol, self._ip, self._port, self._url)
        urllib3.disable_warnings()
        try:
            self._header[HeaderContentType] = 'application/json-patch+json'
            #self._params = json.dumps(self._params)
            r = requests.delete(url=url, data=self._params, headers=self._header, verify=False)
            self.logging_request_info(DeleteMethod, r)
            ret = r.content.decode(UTF8)
            Data = json.loads(ret)
            Ret = self.GetResponse(Data,ItemsHeader=ItemsHeader, ReturnType=ReturnType)
            return ResOk, '', Ret

        except requests.ConnectionError as err:
            self.logger.error("Connection Error %s" % err)
            return ResConnectionErrorCode, ResTimeErrorCodeMsg,None
        except requests.URLRequired as err:
            self.logger.error("UrlRequired Error %s" % err)
            return ResInvalidCode, ResInvalidMsg, None
        except (requests.ConnectTimeout, requests.ReadTimeout, requests.Timeout) as err:
            self.logger.error("Timeout Error %s" % err)
            return ResTimeErrorCode, ResTimeErrorCodeMsg, None
        except Exception as err:
            self.logger.error("Other Error %s" % err)
            return ResEtcErrorCode, ResEtcErrorMsg, None
    @catch_exceptions()
    def logging_request_info(self, method, req):
        try:
            self.logger.debug("Request %s url >>>>> %s" % (method, req.url))
            self.logger.debug("Header : %s" % req.request.headers)
            self.logger.debug("Body : %s" % req.request.body)
        except Exception as err:
            print(err)

    def logging_response_info(self, Data):
        try:
            self.logger.debug("Response Header <<<<< %s" % str(Data))
        except Exception as err:
            print(err)

    @catch_exceptions()
    def GetResponse(self, Ret, ItemsHeader=False, ReturnType=None):
        """
        Convert Dict Result to ResponseHeader Class
        There are three kinds of ResponseHeader
         - single type1(data:True/False)
         - sigle type2()
         - multi value(dict))
        """
        self.logging_response_info(Ret)
        if 'Data' in Ret:
            Ret.update({"py/object": ResponseHeaderWithDataModule})
            if Ret['Data'] is None: # if data items not exists.
                Ret = jsonpickle.decode(json.dumps(Ret))
                return Ret

            if ItemsHeader is True: # if items header exists
                Ret['Data'].update({"py/object": ResponseItemsHeaderModule})
                for Item in Ret['Data']['Items']:
                    DeserializeResponseReculsive(Item, ReturnType)
            else:
                DeserializeResponseReculsive(Ret['Data'], ReturnType)

        else:
            # Return
            DeserializeResponseReculsive(Ret, ResponseHeaderModule)

        Ret = jsonpickle.decode(json.dumps(Ret))
        return Ret

    def GetResponse1(self, Data, ItemsHeader=False):
        self.logging_response_info(Data)
        Data.update({"py/object": ResponseHeaderModule})
        if ItemsHeader is True:
            Data['Data'].update({"py/object": ResponseItemsHeaderModule})

        Ret = jsonpickle.decode(json.dumps(Data))
        return Ret

    @catch_exceptions()
    def parsing_result(self, header):
        '''
        cast total result dict to class(header, item header, data)
        :param header: dict
        :return: GetApiResult Object
        '''
        Header = None
        ItemsHeader = None
        Data = None
        try:
            self.logger.debug("Response << %s" % str(header))
            Header = ResPonseHeader(header)
            if "Data" in Header.__dict__:
                if isinstance(Header.Data, dict):
                    if "Items" in Header.Data:
                        ItemsHeader = ResPonseItemsHeader(Header.Data)
                    Data = DictToObject(Header.Data)
                else:
                    Data = Header.Data
            # print header attr
            self.logger.debug("Header: %s" % str(Header.__dict__))

            # print ItemsHeader attr
            if hasattr(ItemsHeader, '__dict__'):
                dict_attr = str(ItemsHeader.__dict__)
            else:
                dict_attr = 'None'
            self.logger.debug("ItemsHeader: %s" % dict_attr)

            # print Data attr

            if hasattr(Data, '__dict__'):
                dict_attr = str(Data.__dict__)
            else:
                dict_attr = 'None'
            self.logger.debug("Data: %s" % dict_attr)

        except Exception as err:
            self.logger.error("fail to parsing response data: %s line:%d" % (str(err), sys.exc_info()[2].tb_lineno))
        finally:
            return GetApiResult(Header, ItemsHeader, Data)


@catch_exceptions()
def DeserializeResponse(Data, ObjectType):
    """
    Get Data parsing with ObjectType Class
    :param Data: Dict type,
    :param ObjectType: Class name
    :return: ObjectType class
    """
    Data.update({"py/object": ObjectType})
    Ret = jsonpickle.decode(json.dumps(Data))
    return Ret


def DeserializeResponseReculsive(Data, ObjectType):
    """
    Get Data parsing with ObjectType Class
    :param Data: Dict type,
    :param ObjectType: Class name
    :return: ObjectType class
    """
    if ObjectType is not None:
        Data.update({"py/object": ObjectType})
    for key, val in Data.items():
        if key in Parsing.keys():
            if isinstance(val, list):
                for val1 in Data[key]:
                    DeserializeResponseReculsive(val1, Parsing[key])
            else:
                if isinstance(val, dict):
                    Data[key].update({"py/object": Parsing[key]})


def get_res(res):

    print("IsNeedLogin:", res.IsNeedLogin)
    print("AccessDenied:", res.AccessDenied)
    print("Result:", res.Result)
    print("Code:", res.Code)
    print("Message:", res.Message)
    if 'Data' in res.__dict__:
        if not isinstance(res.Data, dict):
            print("Data:", res.Data)



class GetNetwork(object):
    def __init__(self, Ip=None, logger=None):
        self.logger = logger
        self.NicInfo = None
        self.nicinfo_list = list()
        self.GetNicInfo(Ip=Ip)
        self.IoCounterPerNic = psutil.net_io_counters(pernic=True)

    @catch_exceptions()
    def GetNicInfo(self, Ip=None):
        """
        get all detail nic info list.
        :return:
        """
        # get dns info
        dnsresolvers = dns.resolver.Resolver()

        # get status of each nic(isup(tru/false), duplex, speed, mtu)
        netif_stat = psutil.net_if_stats()
        # get nic info. netif_addresses includes {'eno1':..., 'eno2':...}
        netif_addresses = psutil.net_if_addrs()
        for nic in netif_addresses.keys():
            if nic == 'lo' or netifaces.AF_INET not in netifaces.ifaddresses(nic):
                continue
            tmp_dic = dict()
            tmp_dic['Name'] = nic
            tmp_dic['Dhcp'] = 'No'  # must 'No'
            tmp_dic['MacAddress'] = netifaces.ifaddresses(nic)[netifaces.AF_LINK][0]['addr']
            tmp_dic['LinkState'] = 'Up' if netif_stat[nic].isup is True else 'Down'
            tmp_dic['IpAddress'] = netifaces.ifaddresses(nic)[netifaces.AF_INET][0]['addr']
            tmp_dic['SubnetMask'] = netif_addresses[nic][0].netmask
            tmp_dic['BandWidth'] = netif_stat[nic].speed
            tmp_dic['Gateway'] = self.GetGatewayWithNic(nic)
            tmp_dic['Dns1'] = ''
            tmp_dic['Dns2'] = ''
            for idx, nameserver in enumerate(dnsresolvers.nameservers, start=1):
                key = 'Dns%d' % idx
                tmp_dic[key] = nameserver

            if tmp_dic['Dns2'] == '':
                tmp_dic['Dns2'] = tmp_dic['Dns1']


            self.nicinfo_list.append(tmp_dic)
            if Ip is not None:
                if Ip == tmp_dic['IpAddress']:
                    self.NicInfo = tmp_dic
                    return tmp_dic

    @catch_exceptions()
    def GetGatewayWithNic(self, NicName):
        gw = netifaces.gateways()
        for gateway, nic, is_default in gw[netifaces.AF_INET]:
            if NicName == nic:
                return gateway
        return ''


    @catch_exceptions()
    def GetNicInfoWithNicName(self, NicName):
        """
        return specific nic info with nic name
        :return: success to get nic info {'name', <nic name>, 'Dhcp': <dhcp ip>, 'MacAddress':<MacAdress ip>, 'LinkState': <true/false>,
        'addr', <address ip>, 'netask': <netmask>, 'gateway': <gateway>, 'Dns1': <dns1 ip>, 'Dns2': <dns2 ip>, 'Dns3':...}, fail to get nic info: None
        """
        if NicName is None:
            print('nic is None')
        else:
            for nic in self.nicinfo_list:
                if nic['Name'] == NicName:
                    return nic
            return None


    def ReNewalNicStat(self):
        """
        Update Nic stat
        :return:
        """
        self.IoCounterPerNic = psutil.net_io_counters(pernic=True)
        self.GetNicInfo()




class Daemon(object):
    def __init__(self, PidFile, DaemonName):
        self.PidFile = PidFile
        self.DaemonName = DaemonName
        self.redirect = '/dev/null'
        self.pid = -1

    def daemonize(self):
        try:
            pid = os.fork()
            if pid > 0:
                # exit first parent
                sys.exit(0)
        except OSError as e:
            sys.stderr.write("fork failed: %d (%s)\n" % (e.errno, e.strerror))
            sys.exit(1)

        # decouple from parent environment
        os.chdir("/")
        os.setsid()
        os.umask(0)

        # do second fork
        try:
            pid = os.fork()
            if pid > 0:
                # exit from second parent
                sys.exit(0)
        except OSError as e:
            self.pid = pid
            sys.stderr.write("fork failed: %d (%s)\n" % (e.errno, e.strerror))
            sys.exit(1)

        # write pidfile
        atexit.register(self.delpid)
        self.pid = str(os.getpid())
        if self.PidFile:
            with open(self.PidFile, 'w+') as f:
                f.write("%s" % self.pid)

        # redirect standard file descriptors
        sys.stdout.flush()
        sys.stderr.flush()
        sio = open(self.redirect, 'a+')
        os.dup2(sio.fileno(), sys.stdin.fileno())
        os.dup2(sio.fileno(), sys.stdout.fileno())
        os.dup2(sio.fileno(), sys.stderr.fileno())

        sio.close()

    def delpid(self):
        if self.PidFile and os.path.exists(self.PidFile):
            os.remove(self.PidFile)

    def start(self):
        # Start the daemon
        self.daemonize()
        self.run()

    def stop(self):
        Ret, Pid = IsDaemonRunning(self.PidFile, self.DaemonName)
        if Ret is False:
            return
        try:
            while 1:
                os.kill(int(Pid), signal.SIGKILL)
                time.sleep(0.1)
        except OSError as err:
            err = str(err)
            if err.find("No such process") > 0:
                if os.path.exists(self.PidFile):
                    os.remove(self.PidFile)
            else:
                sys.exit(1)

    def restart(self):
        self.stop()
        self.start()

    def run(self):
        sys.stderr.write("run:\n")
        pass
