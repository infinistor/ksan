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
import re

SystemdServicePidFinder = re.compile('Main PID: ([\d]+)')

class AddServiceInfoObject:
    def __init__(self):
        self.GroupId = None
        self.Name = None
        self.ServerId = None
        self.Description = None
        self.ServiceType = None
        self.HaAction = "Initializing"
        self.State = "Offline"
        self.VlanIds = None

    def Set(self, Name, ServerId, ServiceType, GroupId, VlanIds, State='Offline', Description='', HaAction='Initializing'):
        self.GroupId = GroupId
        self.Name = Name
        self.ServerId = ServerId
        self.Description = Description
        self.ServiceType = ServiceType
        self.HaAction = HaAction
        self.State = State
        self.VlanIds = VlanIds


class UpdateServiceInfoObject:
    def __init__(self):
        self.GroupId = None
        self.Name = None
        self.Description = None
        self.ServiceType = None
        self.HaAction = "Initializing"
        self.State = "Offline"
        self.VlanIds = None

    def Set(self, Name, ServiceType, GroupId, VlanIds, State, Description, HaAction):
        self.GroupId = GroupId
        self.Name = Name
        self.Description = Description
        self.ServiceType = ServiceType
        self.HaAction = HaAction
        self.State = State
        self.VlanIds = VlanIds


class AddServiceGroupItems:
    def __init__(self):
        self.Name = None
        self.Description = None
        self.ServiceType = None
        self.ServiceIpAddress = None
        self.ServiceIds = None

    def Set(self, Name, ServiceType, ServiceIpAddress=None, ServiceIds=None):
        self.Name = Name
        self.ServiceType = ServiceType
        self.ServiceIpAddress = ServiceIpAddress
        self.ServiceIds = ServiceIds


class ServiceGroupItems:
    def __init__(self):
        self.Id = None
        self.Name = None
        self.Description = None
        self.ServiceType = None
        self.ServiceIpAddress = None
        self.ModDate = None
        self.ModId = None
        self.ModName = None
        self.RegDate = None
        self.RegId = None
        self.RegName = None

    def Set(self, Id, Name, Description, ServiceType, ServiceIpAddress, ModDate, ModId, ModName, RegDate, RegId, RegName):
        self.Id = Id
        self.Name = Name
        self.Description = Description
        self.ServiceType = ServiceType
        self.ServiceIpAddress = ServiceIpAddress
        self.ModDate = ModDate
        self.ModId = ModId
        self.ModName = ModName
        self.RegDate = RegDate
        self.RegId = RegId
        self.RegName = RegName


class ServiceGroupDetail:
    def __init__(self):
        self.Services = None
        self.Id = None
        self.Name = None
        self.Description = None
        self.ServiceType = None
        self.ServiceIpAddress = None
        self.ModDate = None
        self.ModId = None
        self.ModName = None
        self.RegDate = None
        self.RegId = None
        self.RegName = None


class ServiceItemsDetail:
    def __init__(self):
        self.ServiceGroup = None
        self.Id = None
        self.GroupId = None
        self.Name = None
        self.Description = None
        self.ServiceType = None
        self.HaAction = None
        self.State = None
        self.CpuUsage = None
        self.MemoryTotal = None
        self.MemoryUsed = None
        self.ThreadCount = None
        self.Server = None
        self.ModDate = None
        self.ModId = None
        self.ModName = None
        self.RegDate = None
        self.RegId = None
        self.RegName = None


class ServiceDetail:
    def __init__(self):
        self.Vlans = None
        self.ServiceGroup = None
        self.Id = None
        self.GroupId = None
        self.Name = None
        self.Description = None
        self.ServiceType = None
        self.HaAction = None
        self.State = None
        self.CpuUsage = None
        self.MemoryTotal = None
        self.MemoryUsed = None
        self.ThreadCount = None
        self.ModDate = None
        self.ModId = None
        self.ModName = None
        self.RegDate = None
        self.RegId = None
        self.RegName = None


class UpdateServiceUsageObject:
    def __init__(self):
        self.Id = None
        self.CpuUsage = None
        self.MemoryUsed = None
        self.ThreadCount = None

    def Set(self, Id, CpuUsage, MemoryUsed, ThreadCount):
        self.Id = Id
        self.CpuUsage = CpuUsage
        self.MemoryUsed = MemoryUsed
        self.ThreadCount = ThreadCount


class UpdateServicesStateObject:
    def __init__(self):
        self.Id = None
        self.State = None

    def Set(self, Id, State):
        self.Id = Id
        self.State = State


class ServiceControl:
    def __init__(self):
        self.Id = None
        self.Name = None
        self.IpAddresses = None
        self.Control = None

    def Set(self, Id, Name, IpAddresses, Control):
        self.Id = Id
        self.Name = Name
        self.IpAddresses = IpAddresses
        self.Control = Control

class ServiceConfigItems:
    def __init__(self):
        self.Type = ''
        self.Version = 0
        self.Config = ''
        self.RegDate = ''

    def Set(self, Type, Version, Config, RegDate):
        self.Type = Type
        self.Version = Version
        self.Config = Config
        self.RegDate = RegDate


