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
import sys
import socket, platform
import psutil
from common.log import catch_exceptions
from const.common import ServerStateOffline

ServerItemsModule = 'const.server.ServerItems'
ServerItemsDetailModule = 'const.server.ServerItemsDetail'
ServerUsageItemsModule = 'const.server.ServerUsageItems'
ServerStateItemsModule = 'const.server.ServerStateItems'


class ServerItems(object):
    def __init__(self, Description='', State=ServerStateOffline):
        self.Id = ''
        self.Name = ''
        self.Description = Description
        self.CpuModel = ''
        self.Clock = 0
        self.State = State
        self.Rack = ''
        self.CpuUsage = ''
        self.LoadAverage1M = ''
        self.LoadAverage5M = ''
        self.LoadAverage15M = ''
        self.MemoryTotal = ''
        self.MemoryUsed = ''
        self.ModDate = ''
        self.ModId = ''
        self.ModName = ''

    @catch_exceptions()
    def Set(self, dic):
        self.Id = dic["Id"]
        self.Name = dic["Name"]
        self.Description = dic["Description"]
        self.CpuModel = dic["CpuModel"]
        self.Clock = dic["Clock"]
        self.State = dic["State"]
        self.Rack = dic["Rack"]
        self.CpuUsage = dic["CpuUsage"]
        self.LoadAverage1M = dic['LoadAverage1M']
        self.LoadAverage5M = dic['LoadAverage5M']
        self.LoadAverage15M = dic['LoadAverage15M']
        self.MemoryTotal = dic["MemoryTotal"]
        self.MemoryUsed = dic["MemoryUsed"]
        self.ModDate = dic["ModDate"]
        self.ModId = dic["ModId"]
        self.ModName = dic["ModName"]


class ServerItemsDetail(object):
    def __init__(self, Description='', State=ServerStateOffline):
        self.NetworkInterfaces = None
        self.Disks = None
        self.Services = None
        self.Id = None
        self.Name = ''
        self.Description = Description
        self.CpuModel = ''
        self.Clock = 0
        self.State = State
        self.Rack = ''
        self.CpuUsage = ''
        self.LoadAverage1M = ''
        self.LoadAverage5M = ''
        self.LoadAverage15M = ''
        self.MemoryTotal = ''
        self.MemoryUsed = ''
        self.ModDate = ''
        self.ModId = ''
        self.ModName = ''


class ServerUsageItems(object):
    """
    Used for Update Server Usage info
    """
    def __init__(self):
        self.Id = ''
        self.LoadAverage1M = 0
        self.LoadAverage5M = 0
        self.LoadAverage15M = 0
        self.MemoryUsed = 0

    def Set(self, Id, LoadAverage1M, LoadAverage5M, LoadAverage15M, MemoryUsed):
        self.Id = Id
        self.LoadAverage1M = LoadAverage1M
        self.LoadAverage5M = LoadAverage5M
        self.LoadAverage15M = LoadAverage15M
        self.MemoryUsed = MemoryUsed


class ServerStateItems(object):
    """
    Used for Update Server State info
    """
    def __init__(self, Id, State):
        self.Id = Id
        self.State = State


# ####  Serverinfo of Request Body Class #####
class RequestServerInfo(object):
    def __init__(self, Description, logger=None):
        try:
            self.Name = socket.gethostname()
            self.Description = Description
            self.CpuModel = platform.processor()
            self.Clock = 0
            self.State = 'Online'
            self.Rack = 0
            self.MemoryTotal = psutil.virtual_memory().total
            self.ValidationErrorCode = ''
            self.ValidationErrorMessage = ''
        except Exception as err:
            if logger is not None:
                logger.error(err, sys.exc_info()[2].tb_lineno)
            else:
                print(err, sys.exc_info()[0].tb_lineno)


class RequestServerInitInfo(object):
    def __init__(self):
        self.ServerIp = ''
        self.PortalHost = ''
        self.PortalPort = 0
        self.UserName = ''
        self.Password = ''

    def Set(self, ServerIp, PortalHost, PortalPort, UserName, Password):
        self.ServerIp = ServerIp
        self.PortalHost = PortalHost
        self.PortalPort = PortalPort
        self.UserName = UserName
        self.Password = Password


class RequestServerExistCheck(object):
    def __init__(self, Name, logger=None):
        try:
            self.Name = Name
            self.ValidationErrorCode = ''
            self.ValidationErrorMessage = ''
        except Exception as err:
            if logger is not None:
                logger.error(err, sys.exc_info()[2].tb_lineno)
            else:
                print(err, sys.exc_info()[0].tb_lineno)


class UpdateServerInfoItems:
    def __init__(self):
        self.Name = None
        self.Description = None
        self.CpuModel = None
        self.Clock = None
        self.State = None
        self.Rack = None
        self.MemoryTotal = None

    def Set(self, Name, Descrition, CpuModel, Clock, State, Rack, MemoryTotal):
        self.Name = Name
        self.Description = Descrition
        self.CpuModel = CpuModel
        self.Clock = Clock
        self.State = State
        self.Rack = Rack
        self.MemoryTotal = MemoryTotal
