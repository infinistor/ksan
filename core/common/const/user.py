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

UserObjectModule = 'const.user.UserObject'
S3UserObjectModule = 'const.user.S3UserObject'

class AddUserObject:
    def __init__(self):
        self.LoginId = None
        self.Email = None
        self.Name = None
        self.Code = None
        self.Roles = None
        self.Status = None

    def Set(self, LoginId, Email, Name, Code, Roles, Status):
        self.LoginId = LoginId
        self.Email = Email
        self.Name = Name
        self.Code = Code
        self.Roles = Roles
        self.Status = Status


class UserObject:
    def __init__(self):
        self.Id = None
        self.LoginId = None
        self.Email = None
        self.Name = None
        self.DisplayName = None
        self.Code = None
        self.Status = None
        self.JoinDate = None
        self.WithdrawDate = None
        self.LoginCount = None

    def Set(self, Id, LoginId, Email, Name, DisplayName, Code, Status, JoinDate, WithdrawDate, LoginCount):
        self.Id = Id
        self.LoginId = LoginId
        self.Email = Email
        self.Name = Name
        self.DisplayName = DisplayName
        self.Code = Code
        self.Status = Status
        self.JoinDate = JoinDate
        self.WithdrawDate = WithdrawDate
        self.LoginCount = LoginCount



class S3UserObject:
    def __init__(self):
        self.Id = None
        self.Name = None
        self.StandDiskPoolId = None
        self.Email = None
        self.AccessKey = None
        self.SecretKey = None

    def Set(self, Name, DiskPoolId, Email, Id=None, AccessKey=None, SecretKey=None):
        self.Id = Id
        self.Name = Name
        self.StandardDiskPoolId = DiskPoolId
        self.Email = Email
        self.AccessKey = AccessKey
        self.SecretKey = SecretKey


class S3UserUpdateObject:
    def __init__(self):
        self.Name = None
        self.Email = None

    def Set(self, Name, Email):
        self.Name = Name
        self.Email = Email


class S3UserStorageClassObject:
    def __init__(self):
        self.UserId = None
        self.DiskPoolId = None
        self.StorageClass = None

    def Set(self, UserId, DiskPoolId, StorageClass):
        self.UserId = UserId
        self.DiskPoolId = DiskPoolId
        self.StorageClass = StorageClass
