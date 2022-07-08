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
import pdb
sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from common.define import *
from configparser import ConfigParser
from common.shcommand import GetHostInfo
from common.log import *
from common.utils import IsIpValid, IsUrlValid


def get_input(Description, type, default=None, ValidAnsList=None):
    try:
        while True:
            ans = input(Description + '(default: %s):' % str(default) if default is not None else ':')
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
                    if type in ['pwd', 'net_device']:
                        print('Invalid %s type' % type)
                        continue

                if default is not None:
                    return default
    except Exception as err:
        print(err, sys.exc_info()[2].tb_lineno)


@catch_exceptions()
def InitMonServicedConf():
    Ret, HostName, LocalIp = GetHostInfo()
    if Ret is False:
        print('Fail to get host by name')
        sys.exit(-1)
    conf = MonservicedConf()
    conf.MgsIp = LocalIp
    conf.IfsPortal = DefaultIfPortalPort
    conf.MqPort = DefaultIfMqPort
    conf.ServerId = ''
    conf.ManagementNetDev = ''

    ret, oldconfig = GetConf(MonServicedConfPath)
    if ret is True:
        conf.MgsIp = oldconfig.mgs.MgsIp
        conf.IfsPortal = int(oldconfig.mgs.IfsPortalPort)
        conf.MqPort = int(oldconfig.mgs.MqPort)
        conf.ServerId = oldconfig.mgs.ServerId
        conf.ManagementNetDev = oldconfig.mgs.ManagementNetDev

    conf.MgsIp = get_input('Insert Mgs Ip', str, default=conf.MgsIp)
    conf.IfsPortalPort = get_input('Insert Mgs Port', int, default=conf.IfsPortal)
    conf.MqPort = get_input('Insert Mq Port', int, default=conf.MqPort)
    conf.ManagementNetDev = get_input('Insert Management Network device', 'net_device', default=conf.ManagementNetDev)
    if not os.path.exists(KsanEtcPath):
        os.makedirs(KsanEtcPath)

    config = ConfigParser()
    config.optionxform = str
    config['mgs'] = conf.__dict__
    #config['mgs']['MgsIp'] = mgsip
    #config['mgs']['IfsPortalPort'] = str(mgsport)
    #config['mgs']['MqPort'] = str(mqport)
    #config['mgs']['ManagementNetDev'] = ManagementNetDev
    #config['mgs']['ServerId'] = conf.ServerId
    with open(MonServicedConfPath, 'w') as configfile:
        config.write(configfile)


@catch_exceptions()
def KsanInitConf(ConfPath, ConfClass, FileType='INI', ReturnType='Object'):
    """
    ConfPath: Conf file to read or write
    ConfClass: Configuration Class
    FileType: INI for ini file. Normal for key-value file
    ReturnType: 'Object' for object(class). 'Dict' for dictionary type
    """

    if not os.path.exists(KsanEtcPath):
        os.makedirs(KsanEtcPath)

    if os.path.exists(ConfPath):
        return GetConf(ConfPath, FileType=FileType, ReturnType=ReturnType)
    else:
        # return Default configuration
        return True, ConfClass




def readconf_type1(path, fo=None, lock_sh=False):
    config = dict()
    openfile = True
    if fo: openfile = False
    if openfile:
        try:
            fo = open(path)
        except IOError as err:
            return config
    if lock_sh: fcntl.flock(fo.fileno(), fcntl.LOCK_SH)
    lines = fo.readlines()
    for line in lines:
        mo = re.search("([\d\w_\-]+)[\s]{0,3}=[\s]{0,3}([\d\w_\-])")
        if mo:
            config[mo.group(1)] = mo.group(2)
    if lock_sh: fcntl.flock(fo.fileno(), fcntl.LOCK_UN)
    if openfile: fo.close()
    return config


@catch_exceptions()
def read_conf(path):
    """
    ini type conf file
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
def GetConf(filename, FileType='INI', ReturnType='Object'):
    """
    filename: configuration file Path
    """
    if not os.path.exists(filename):
        return False, None
    if FileType == 'INI':
        Res, Conf = read_conf(filename)
    else:
        Res, Conf = read_conf_normal(filename)

    if Res is True:
        if ReturnType == 'Object':
            return Res, DictToObject(Conf)
        else:
            # return dictionary type
            return Res, Conf

    else:
        return Res, None


def UpdateConf(Path, Section, Key, Value, Force=True):
    res, conf = read_conf(Path)
    if res is True:
        if Section not in conf:
            print('Invalid Section: %s in %s' % (Section, Path))
            return False
        if Key not in conf[Section] and Force is False:
            print('Invalid Key: %s in %s' % (Key, Path))
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





if __name__ == '__main__':
    InitMonServicedConf()
    res, ret = read_conf(MonServicedConfPath)
    print(ret)
