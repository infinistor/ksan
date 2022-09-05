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

DiskItemsDetailModule = 'const.disk.DiskItemsDetail'
DiskDetailModule = 'const.disk.DiskDetail'
AllDiskItemsDetailModule = 'const.disk.AllDiskItemsDetail'
DiskPoolItemsModule = 'const.disk.DiskPoolItems'
DiskPoolDetailModule = 'const.disk.DiskPoolDetail'

class DiskItemsDetail:
    def __init__(self):
        self.Id = ''
        self.ServerId = ''
        self.DiskPoolId = ''
        self.DiskNo = ''
        self.Path = ''
        self.State = 'Good'
        self.TotalInode = 0
        self.ReservedInode = 0
        self.UsedInode = 0
        self.TotalSize = 0
        self.ReservedSize = 0
        self.UsedSize = 0
        self.RwMode = None

    def Set(self, Id, ServerId, DiskPoolId, DiskNo, Path, State, TotalInode,
            ReservedInode,
            UsedInode, TotalSize, ReservedSize, UsedSize, RwMode):
        self.Id = Id
        self.ServerId = ServerId
        self.DiskPoolId = DiskPoolId
        self.DiskNo = DiskNo
        self.Path = Path
        self.State = State
        self.TotalInode = TotalInode
        self.ReservedInode = ReservedInode
        self.UsedInode = UsedInode
        self.TotalSize = TotalSize
        self.ReservedSize = ReservedSize
        self.UsedSize = UsedSize
        self.RwMode = RwMode

class AllDiskItemsDetail:
    def __init__(self):
        self.Server = None
        self.Id = ''
        self.ServerId = ''
        self.DiskPoolId = ''
        self.DiskNo = ''
        self.Path = ''
        self.State = None
        self.TotalInode = 0
        self.ReservedInode = 0
        self.UsedInode = 0
        self.TotalSize = 0
        self.ReservedSize = 0
        self.UsedSize = 0
        self.RwMode = None

    def Set(self, Server, Id, ServerId, DiskPoolId, DiskNo, Path, State, TotalInode,
            ReservedInode,
            UsedInode, TotalSize, ReservedSize, UsedSize, RwMode):
        self.Server = Server
        self.Id = Id
        self.ServerId = ServerId
        self.DiskPoolId = DiskPoolId
        self.DiskNo = DiskNo
        self.Path = Path
        self.State = State
        self.TotalInode = TotalInode
        self.ReservedInode = ReservedInode
        self.UsedInode = UsedInode
        self.TotalSize = TotalSize
        self.ReservedSize = ReservedSize
        self.UsedSize = UsedSize
        self.RwMode = RwMode




class DiskDetailMqBroadcast:
    def __init__(self):
        self.Id = ''
        self.ServerId = ''
        self.State = None
        self.TotalInode = 0
        self.ReservedInode = 0
        self.UsedInode = 0
        self.TotalSize = 0
        self.ReservedSize = 0
        self.UsedSize = 0
        self.Read = 0
        self.Write = 0

    def Set(self, Id, ServerId, State, TotalInode, ReservedInode,
            UsedInode, TotalSize, ReservedSize, UsedSize, Read, Write):
        self.Id = Id
        self.ServerId = ServerId
        self.State = State
        self.TotalInode = TotalInode
        self.ReservedInode = ReservedInode
        self.UsedInode = UsedInode
        self.TotalSize = TotalSize
        self.ReservedSize = ReservedSize
        self.UsedSize = UsedSize
        self.Read = Read
        self.Write = Write


class AddDiskObject:
    def __init__(self):
        self.DiskPoolId = ''
        self.Name = ''
        self.Path = ''
        self.State = 'Stop'
        self.TotalInode = 0
        self.ReservedInode = 0
        self.UsedInode = 0
        self.TotalSize = 0
        self.ReservedSize = 0
        self.UsedSize = 0
        self.Rwmode = 'ReadOnly'

    def Set(self, Name, Path, State, TotalInode, ReservedInode, UsedInode, TotalSize, ReservedSize, UsedSize, RwMode, DiskPoolId=''):

        self.DiskPoolId = DiskPoolId
        self.Name = Name
        self.Path = Path
        self.State = State
        self.TotalInode = TotalInode
        self.ReservedInode = ReservedInode
        self.UsedInode = UsedInode
        self.TotalSize = TotalSize
        self.ReservedSize = ReservedSize
        self.UsedSize = UsedSize
        self.Rwmode = RwMode

class UpdateDiskSizeObject:
    def __init__(self):
        self.Id = None
        self.ServerId = None
        self.DiskNo = None
        self.TotalInode = 0
        self.ReservedInode = 0
        self.UsedInode = 0
        self.TotalSize = 0
        self.ReservedSize = 0
        self.UsedSize = 0

    def Set(self, Id, ServerId, DiskNo, TotalInode, ReservedInode, UsedInode, TotalSize, ReservedSize, UsedSize):
        self.Id = Id
        self.ServerId = ServerId
        self.DiskNo = DiskNo
        self.TotalInode = TotalInode
        self.ReservedInode = ReservedInode
        self.UsedInode = UsedInode
        self.TotalSize = TotalSize
        self.ReservedSize = ReservedSize
        self.UsedSize = UsedSize

class RequestDiskPool:
    def __init__(self):
        self.Name = None
        self.Description = None
        self.DiskIds = []
        self.DiskPoolType = 'STANDARD'
        self.ReplicationType = 'OnePlusZero'

    def Set(self, Name, Descrition, DiskIds, DiskPoolType=None, ReplicationType=None):
        self.Name = Name
        self.Description = Descrition
        self.DiskIds = DiskIds
        self.DiskPoolType = DiskPoolType
        self.ReplicationType = ReplicationType

class DiskPoolDetail:
    def __init__(self):
        self.Volumes = None
        self.Disks = None
        self.Id = None
        self.Name = None
        self.Description = None
        self.ModDate = None
        self.ModId = None
        self.modName = None
        self.RegDate = None
        self.RegId = None
        self.RegName = None

class DiskPoolItems:
    def __init__(self):
        self.Id = None
        self.Name = None
        self.Description = None
        self.ModDate = None
        self.ModId = None
        self.ModName = None
        self.RegDate = None
        self.RegId = None
        self.RegName = None

    def Set(self, Id, Name, Description, ModDate, ModId, ModName, RegDate, RegId, RegName):
        self.Id = Id
        self.Name = Name
        self.Description = Description
        self.ModDate = ModDate
        self.ModId = ModId
        self.ModName = ModName
        self.RegDate = RegDate
        self.RegId = RegId
        self.RegName = RegName

class DiskAdd2DiskPoolItems:
    def __init__(self):
        self.Disks = list()

    def Set(self, Disks:list):
        self.Disks = Disks
