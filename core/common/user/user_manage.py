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


import os
import sys
sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from ksan.server.server_manage import *


def GetUserInfo(ip, port, UserId=None, logger=None):
    """
    get server info all or specific server info with Id
    :param ip:
    :param port:
    :param ServerId
    :param NicId
    :param disp:
    :param logger
    :return:
    """
    ItemsHeader = True
    ReturnType = UserObjectModule
    if UserId is not None:
        Url = "/api/v1/Users/%s" % UserId
        ItemsHeader = False
    else:
        Url = "/api/v1/Users"
    Params = dict()
    Params['countPerPage'] = 100
    Conn = RestApi(ip, port, Url, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.get(ItemsHeader=ItemsHeader, ReturnType=ReturnType)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            if UserId is not None:
                return Res, Errmsg, Ret, [Ret.Data]
            else:
                return Res, Errmsg, Ret, Ret.Data.Items
        return Res, Errmsg, Ret, None
    else:
        return Res, Errmsg, None, None

@catch_exceptions()
def ShowUserInfo(UserList, Detail=False):
    """
    Display Network Interface Info
    :param InterfaceList: NetworkInterfaceItems class list
    :param NicId:
    :return:
    """
    UserTitleLine = '-' * 100
    title ="%s%s%s%s%s" % ('Id'.center(40), 'Name'.center(20), 'LoginId'.center(20),  'Email'.center(30), 'Roles'.center(10))
    print(title)
    print(UserTitleLine)
    for user in UserList:
        Roles = ' '.join(user.Roles)
        _nic ="%s%s%s%s%s" % (user.Id.center(40), '{:20.20}'.format(str(user.Name).center(20)), user.LoginId.center(20),
                             user.Email.center(30), Roles.center(10))
        print(_nic)



@catch_exceptions()
def AddUser(ip, port, LoginId, Name, Roles, Email='user@example.com',Code='', Status='Locked', logger=None):
    """
    add network interface with name
    :param ip:
    :param port:
    :param ServerId:
    :param NicName:
    :param Description:
    :param logger:
    :return:
    """
    # get network interface info
    user = AddUserObject()
    user.Set(LoginId, Email, Name, Code, Roles, Status)
    body = jsonpickle.encode(user, make_refs=False)
    Url = '/api/v1/Users'
    ReturnType = ResponseHeaderModule

    Params = body
    Conn = RestApi(ip, port, Url, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.post(ItemsHeader=False, ReturnType=ReturnType)
    return Res, Errmsg, Ret

@catch_exceptions()
def RemoveUser(Ip, Port, UserId, logger=None):
    """
    delete server info from server pool
    :param ip:
    :param port:
    :param Id:
    :param logger:
    :return:tuple(error code, error msg, ResponseHeader class)
    """
    Url = '/api/v1/Users/%s' % UserId
    ReturnType = ResponseHeaderModule
    Conn = RestApi(Ip, Port, Url, logger=logger)
    Res, Errmsg, Ret = Conn.delete(ReturnType=ReturnType)
    return Res, Errmsg, Ret


@catch_exceptions()
def UpdateUserInfo(Ip, Port, UserId, Email=None, Name=None, Code=None, Roles=None, Status=None, logger=None):

    Res, Errmsg, Ret, User = GetUserInfo(Ip, Port, UserId, logger=logger)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            User = User[0]
            if Email is not None:
                User.Email = Email
            if Name is not None:
                User.Name = Name
            if Code is not None:
                User.Code = Code
            if Roles is not None:
                User.Roles = Roles
            if Status is not None:
                User.Status = Status
            NewUser = UpdateUserObject()
            NewUser.Set(User.Email, User.Name, User.Code, User.Status, User.Roles)

            body = jsonpickle.encode(NewUser, make_refs=False)
            Url = '/api/v1/Users/%s' % UserId
            Params = body
            Conn = RestApi(Ip, Port, Url, params=Params, logger=logger)
            res, errmsg, ret = Conn.put()
            if res == ResOk:
                return res, errmsg, ret
            else:
                return res, errmsg, None
        else:
            return Res, Errmsg, Ret
    else:
        return Res, Errmsg, None


@catch_exceptions()
def AddUserRoles(ip, port, UserId, Roles, logger=None):
    """
    add network interface with name
    :param ip:
    :param port:
    :param ServerId:
    :param NicName:
    :param Description:
    :param logger:
    :return:
    """
    # get network interface info
    user = UpdateUserRolesObject()
    user.Set(Roles)
    body = jsonpickle.encode(user, make_refs=False)
    Url = '/api/v1/Users/%s/Roles' % UserId
    ReturnType = ResponseHeaderModule

    Params = body
    Conn = RestApi(ip, port, Url, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.post(ItemsHeader=False, ReturnType=ReturnType)
    return Res, Errmsg, Ret


@catch_exceptions()
def RemoveUserRoles(ip, port, UserId, Roles, logger=None):
    """
    add network interface with name
    :param ip:
    :param port:
    :param ServerId:
    :param NicName:
    :param Description:
    :param logger:
    :return:
    """
    # get network interface info
    body = dict()
    Url = '/api/v1/Users/%s/Roles/%s' % (UserId, Roles)
    ReturnType = ResponseHeaderModule
    Params = body
    Conn = RestApi(ip, port, Url, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.delete(ItemsHeader=False, ReturnType=ReturnType)
    return Res, Errmsg, Ret


@catch_exceptions()
def ChangeUserPassword(Ip, Port, UserId, NewPassword, NewConfirmPassword, logger=None):

    Password = ChangeUserPasswordObject()
    Password.Set(NewPassword, NewConfirmPassword)

    body = jsonpickle.encode(Password, make_refs=False)
    Url = '/api/v1/Users/%s/ChangePassword' % UserId
    Params = body
    Conn = RestApi(Ip, Port, Url, params=Params, logger=logger)
    res, errmsg, ret = Conn.put()
    if res == ResOk:
        return res, errmsg, ret
    else:
        return res, errmsg, None


def GetS3UserInfo(ip, port, UserId=None, logger=None):
    """
    get server info all or specific server info with Id
    :param ip:
    :param port:
    :param ServerId
    :param NicId
    :param disp:
    :param logger
    :return:
    """
    ItemsHeader = True
    ReturnType = S3UserObjectModule
    if UserId is not None:
        Url = "/api/v1/KsanUsers/%s" % UserId
        ItemsHeader = False
    else:
        Url = "/api/v1/KsanUsers"
    Params = dict()
    Params['countPerPage'] = 100
    Conn = RestApi(ip, port, Url, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.get(ItemsHeader=ItemsHeader, ReturnType=ReturnType)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            if UserId is not None:
                return Res, Errmsg, Ret, [Ret.Data]
            else:
                return Res, Errmsg, Ret, Ret.Data.Items
        return Res, Errmsg, Ret, None
    else:
        return Res, Errmsg, None, None



@catch_exceptions()
def AddS3User(ip, port, Name, Email='user@example.com', logger=None):
    """
    add network interface with name
    :param ip: portal ip
    :param port: portal port
    :param Name: S3 User Name
    :param Description:
    :param logger:
    :return:
    """
    # get network interface info
    user = S3UserObject()
    user.Set(Name, Email, "", "")
    body = jsonpickle.encode(user, make_refs=False)
    Url = '/api/v1/KsanUsers'
    ReturnType = ResponseHeaderModule

    Params = body
    Conn = RestApi(ip, port, Url, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.post(ItemsHeader=False, ReturnType=ReturnType)
    return Res, Errmsg, Ret


@catch_exceptions()
def UpdateS3UserInfo(Ip, Port, UserId, Name=None, Email=None, logger=None):

    Res, Errmsg, Ret, User = GetS3UserInfo(Ip, Port, UserId, logger=logger)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            User = User[0]
            if Email is not None:
                User.Email = Email
            if Name is not None:
                User.Name = Name
            NewUser = S3UserUpdateObject()
            NewUser.Set(User.Name, User.Email)

            body = jsonpickle.encode(NewUser, make_refs=False)
            Url = '/api/v1/KsanUsers/%s' % UserId
            Params = body
            Conn = RestApi(Ip, Port, Url, params=Params, logger=logger)
            res, errmsg, ret = Conn.put()
            if res == ResOk:
                return res, errmsg, ret
            else:
                return res, errmsg, None
        else:
            return Res, Errmsg, Ret
    else:
        return Res, Errmsg, None


@catch_exceptions()
def RemoveS3User(Ip, Port, UserId, logger=None):
    """
    delete server info from server pool
    :param ip:
    :param port:
    :param Id:
    :param logger:
    :return:tuple(error code, error msg, ResponseHeader class)
    """
    Url = '/api/v1/KsanUsers/%s' % UserId
    ReturnType = ResponseHeaderModule
    Conn = RestApi(Ip, Port, Url, logger=logger)
    Res, Errmsg, Ret = Conn.delete(ReturnType=ReturnType)
    return Res, Errmsg, Ret


@catch_exceptions()
def ShowS3UserInfo(UserList, Detail=False):
    """
    Display Network Interface Info
    :param InterfaceList: NetworkInterfaceItems class list
    :param NicId:
    :return:
    """
    if Detail is False:
        UserTitleLine = '=' * 92
        UserDataLine = '-' * 92
        title ="|%s|%s|%s|" % ('Id'.center(38), 'Name'.center(20), 'Email'.center(30))
    else:
        UserTitleLine = '=' * 166
        UserDataLine = '-' * 166
        title = "|%s|%s|%s|%s|%s|" % ('Id'.center(38), 'Name'.center(20), 'Email'.center(30), 'AccessKey'.center(30), 'SecretKey'.center(42))
    print(UserTitleLine)
    print(title)
    print(UserTitleLine)
    for user in UserList:
        if Detail is False:
            _nic ="|%s|%s|%s|" % (user.Id.center(38), '{:20.20}'.format(str(user.Name).center(20)), user.Email.center(30))
        else:
            _nic ="|%s|%s|%s|%s|%s|" % (user.Id.center(38), '{:20.20}'.format(str(user.Name).center(20)),
                                        user.Email.center(30), user.AccessKey.center(30), user.SecretKey.center(42))
        print(_nic)
        print(UserDataLine)

