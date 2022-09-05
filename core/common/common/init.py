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
import fcntl
import re
import getpass
import time
import pdb
sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from const.common import *
from configparser import ConfigParser
from common.shcommand import GetHostInfo
from common.log import *
from common.utils import IsIpValid, IsUrlValid


def get_input(Description, type, default=None, ValidAnsList=None, force=True):
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
                ans = getpass.getpass(Description + '(default: %s):' % str(default) if default is not None else ':')
            else:
                ans = input(Description + '(default: %s):' % str(default) if default is not None else ':')

            if ans in ['', None]:
                if type in ['host']:
                    ans = default

            if ans:
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
                    return ans
            else:
                if default == '':
                    if type in ['pwd', 'net_device', 'host']:
                        print('Invalid %s type' % type)
                        continue

                if default is not None:
                    return default
    except Exception as err:
        print(err, sys.exc_info()[2].tb_lineno)


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

def WaitAgentConfComplete(FuncName, logger, CheckNetworkDevice=False, CheckNetworkId=False):
    while True:
        ret, conf = GetConf(MonServicedConfPath)
        if ret is True:
            if conf is not None:
                if hasattr(conf.__dict__[KeyCommonSection], 'ServerId'):
                    if not (conf.__dict__[KeyCommonSection].__dict__[KeyServerId] is None or conf.__dict__[KeyCommonSection].__dict__[KeyServerId] == ''):
                        if CheckNetworkDevice is False:
                            return conf
                        else:
                            if not (conf.__dict__[KeyCommonSection].__dict__[KeyManagementNetDev] is None or conf.__dict__[KeyCommonSection].__dict__[KeyManagementNetDev] == ''):
                                if CheckNetworkId is False:
                                    return conf
                                else:
                                    if not (conf.__dict__[KeyCommonSection].__dict__[KeyDefaultNetworkId] is None or conf.__dict__[KeyCommonSection].__dict__[KeyDefaultNetworkId] == ''):
                                        return conf

        logger.debug('%s wail for %s to be configured. Check = ServerId:True, NetworkDevice:%s CheckNetworkId:%s' %
                     (FuncName, MonServicedConfPath, str(CheckNetworkDevice), str(CheckNetworkId)))
        time.sleep(IntervalLong)


def GetAgentConfig(Conf):
    if isinstance(Conf, dict):
        portalhost = Conf[KeyCommonSection].PortalHost
        portalport = Conf[KeyCommonSection].PortalPort
        portalapikey = Conf[KeyCommonSection].PortalApiKey
        mqhost = Conf[KeyCommonSection].MQHost
        mqport = Conf[KeyCommonSection].MQPort
        mqpassword = Conf[KeyCommonSection].MQPassword
        mquser = Conf[KeyCommonSection].MQUser
        serverid = Conf[KeyCommonSection].ServerId
        managementnetdev = Conf[KeyCommonSection].ManagementNetDev
        defaultnetworkid = Conf[KeyCommonSection].DefaultNetworkId

        serverMonitorInterval = Conf[KeyMonitorSection].ServerMonitorInterval
        networkMonitorInterval = Conf[KeyMonitorSection].NetworkMonitorInterval
        diskMonitorInterval = Conf[KeyMonitorSection].DiskMonitorInterval
        serviceMonitorInterval = Conf[KeyMonitorSection].ServiceMonitorInterval


    else:
        portalhost = Conf.__dict__[KeyCommonSection].__dict__[KeyPortalHost]
        portalport = Conf.__dict__[KeyCommonSection].__dict__[KeyPortalPort]
        portalapikey = Conf.__dict__[KeyCommonSection].__dict__[KeyPortalApiKey]
        mqhost = Conf.__dict__[KeyCommonSection].__dict__[KeyMQHost]
        mqport = Conf.__dict__[KeyCommonSection].__dict__[KeyMQPort]
        mqpassword = Conf.__dict__[KeyCommonSection].__dict__[KeyMQPassword]
        mquser = Conf.__dict__[KeyCommonSection].__dict__[KeyMQUser]
        serverid = Conf.__dict__[KeyCommonSection].__dict__[KeyServerId]
        managementnetdev = Conf.__dict__[KeyCommonSection].__dict__[KeyManagementNetDev]
        defaultnetworkid = Conf.__dict__[KeyCommonSection].__dict__[KeyDefaultNetworkId]

        serverMonitorInterval = Conf.__dict__[KeyMonitorSection].__dict__[KeyServerMonitorInterval]
        networkMonitorInterval = Conf.__dict__[KeyMonitorSection].__dict__[KeyNetworkMonitorInterval]
        diskMonitorInterval = Conf.__dict__[KeyMonitorSection].__dict__[KeyDiskMonitorInterval]
        serviceMonitorInterval = Conf.__dict__[KeyMonitorSection].__dict__[KeyServiceMonitorInterval]

    # Key conversion(ksanAgen.conf -> Conf object attribute
    class Config: pass
    setattr(Config, 'PortalHost', portalhost)
    setattr(Config, 'PortalPort' , portalport )
    setattr(Config, 'PortalApiKey', portalapikey)
    setattr(Config, 'MQHost', mqhost)
    setattr(Config, 'MQPort', mqport)
    setattr(Config, 'MQPassword', mqpassword)
    setattr(Config, 'MQUser', mquser)
    setattr(Config, 'ServerId', serverid)
    setattr(Config, 'ManagementNetDev', managementnetdev)
    setattr(Config, 'DefaultNetworkId', defaultnetworkid)

    setattr(Config, 'ServerMonitorInterval', serverMonitorInterval)
    setattr(Config, 'NetworkMonitorInterval', networkMonitorInterval)
    setattr(Config, 'DiskMonitorInterval', diskMonitorInterval)
    setattr(Config, 'ServiceMonitorInterval', serviceMonitorInterval)

    return Config