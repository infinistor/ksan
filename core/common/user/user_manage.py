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
from server.server_manage import *
from disk.diskpool_manage import GetDefaultDiskPool


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


def GetS3UserInfo(ip, port, ApiKey, UserId=None, UserName=None, logger=None):
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
    if UserId is not None:
        TargetUser = UserId
    elif UserName is not None:
        TargetUser = UserName
    else:
        TargetUser = None

    ItemsHeader = True
    ReturnType = S3UserObjectModule
    if TargetUser is not None:
        Url = "/api/v1/KsanUsers/%s" % TargetUser
        ItemsHeader = False
    else:
        Url = "/api/v1/KsanUsers"
    Params = dict()
    Params['countPerPage'] = 100
    Conn = RestApi(ip, port, Url, authkey=ApiKey, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.get(ItemsHeader=ItemsHeader, ReturnType=ReturnType)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            if TargetUser is not None:
                return Res, Errmsg, Ret, [Ret.Data]
            else:
                return Res, Errmsg, Ret, Ret.Data.Items
        return Res, Errmsg, Ret, None
    else:
        return Res, Errmsg, None, None



@catch_exceptions()
def AddS3User(ip, port, ApiKey, Name, DiskPoolId=None, DiskPoolName=None, Email='user@example.com', logger=None):
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
    if DiskPoolId is not None:
        TargetDiskPool = DiskPoolId
    elif DiskPoolName is not None:
        TargetDiskPool = DiskPoolName
    else:
        return ResInvalidCode, ResInvalidMsg + ' DiskPoolId or DiskPoolName is required', None

    user = S3UserObject()
    user.Set(Name, TargetDiskPool, Email, "", "")
    body = jsonpickle.encode(user, make_refs=False)
    Url = '/api/v1/KsanUsers'
    ReturnType = ResponseHeaderModule

    Params = body
    Conn = RestApi(ip, port, Url, authkey=ApiKey, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.post(ItemsHeader=False, ReturnType=ReturnType)
    return Res, Errmsg, Ret


@catch_exceptions()
def UpdateS3UserInfo(Ip, Port, ApiKey, UserId=None, UserName=None, Email=None, logger=None):

    if UserId is not None:
        TargetUser = UserId
    elif UserName is not None:
        TargetUser = UserName
    else:
        return ResInvalidCode, ResInvalidMsg + ' UserId or UserName is required', None


    Res, Errmsg, Ret, User = GetS3UserInfo(Ip, Port, ApiKey, UserId=UserId, UserName=UserName, logger=logger)
    if Res == ResOk:
        if Ret.Result == ResultSuccess:
            User = User[0]
            if Email is not None:
                User.Email = Email
            if UserName is not None:
                User.Name = UserName
            NewUser = S3UserUpdateObject()
            NewUser.Set(User.Name, User.Email)

            body = jsonpickle.encode(NewUser, make_refs=False)
            Url = '/api/v1/KsanUsers/%s' % TargetUser
            Params = body
            Conn = RestApi(Ip, Port, Url, authkey=ApiKey, params=Params, logger=logger)
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
def RemoveS3User(Ip, Port, ApiKey, UserId=None, UserName=None, logger=None):
    """
    delete server info from server pool
    :param ip:
    :param port:
    :param Id:
    :param logger:
    :return:tuple(error code, error msg, ResponseHeader class)
    """
    if UserId is not None:
        TargetUser = UserId
    elif UserName is not None:
        TargetUser = UserName
    else:
        return ResInvalidCode, ResInvalidMsg + ' UserId or UserName is required', None


    Url = '/api/v1/KsanUsers/%s' % TargetUser
    ReturnType = ResponseHeaderModule
    Conn = RestApi(Ip, Port, Url, authkey=ApiKey, logger=logger)
    Res, Errmsg, Ret = Conn.delete(ReturnType=ReturnType)
    return Res, Errmsg, Ret


@catch_exceptions()
def AddS3UserStorageClass(ip, port, ApiKey, StorageClass, UserId=None, UserName=None, DiskPoolId=None, DiskPoolName=None, logger=None):
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

    if DiskPoolId is not None:
        TargetDiskPool = DiskPoolId
    elif DiskPoolName is not None:
        TargetDiskPool = DiskPoolName
    else:
        return ResInvalidCode, ResInvalidMsg + ' DiskPoolId or DiskPoolName is required', None
    if UserId is not None:
        TargetUser = UserId
    elif UserName is not None:
        TargetUser = UserName
    else:
        return ResInvalidCode, ResInvalidMsg + ' UserId or UserName is required', None


    user = S3UserStorageClassObject()
    user.Set(TargetUser, TargetDiskPool, StorageClass)
    body = jsonpickle.encode(user, make_refs=False)
    Url = '/api/v1/KsanUsers/StorageClass'
    ReturnType = ResponseHeaderModule

    Params = body
    Conn = RestApi(ip, port, Url, authkey=ApiKey, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.post(ItemsHeader=False, ReturnType=ReturnType)
    return Res, Errmsg, Ret


@catch_exceptions()
def RemoveS3UserStorageClass(ip, port, ApiKey, StorageClass, UserId=None, UserName=None, DiskPoolId=None, DiskPoolName=None, logger=None):
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

    if DiskPoolId is not None:
        TargetDiskPool = DiskPoolId
    elif DiskPoolName is not None:
        TargetDiskPool = DiskPoolName
    else:
        return ResInvalidCode, ResInvalidMsg + ' DiskPoolId or DiskPoolName is required', None
    if UserId is not None:
        TargetUser = UserId
    elif UserName is not None:
        TargetUser = UserName
    else:
        return ResInvalidCode, ResInvalidMsg + ' UserId or UserName is required', None


    user = S3UserStorageClassObject()
    user.Set(TargetUser, TargetDiskPool, StorageClass)
    body = jsonpickle.encode(user, make_refs=False)
    Url = '/api/v1/KsanUsers/StorageClass'
    ReturnType = ResponseHeaderModule

    Params = body
    Conn = RestApi(ip, port, Url, authkey=ApiKey, params=Params, logger=logger)
    Res, Errmsg, Ret = Conn.delete(ItemsHeader=False, ReturnType=ReturnType)
    return Res, Errmsg, Ret



@catch_exceptions()
def ShowS3UserInfo(UserList, Detail=None):
    """
    Display Network Interface Info
    :param InterfaceList: NetworkInterfaceItems class list
    :param NicId:
    :return:
    """
    if Detail is None:
        UserTitleLine = '=' * 92
        UserDataLine = '-' * 92
        title ="|%s|%s|%s|" % ('Name'.center(20), 'Email'.center(30), 'Id'.center(38))
        UserDiskPoolStorageClassLine = ''
        UserDiskPoolStorageClassTitle = ''
    else:
        UserTitleLine = '=' * 166
        UserDataLine = '-' * 166
        title = "|%s|%s|%s|%s|%s|" % ('Name'.center(20), 'AccessKey'.center(30),
                                      'SecretKey'.center(42), 'Email'.center(30), 'Id'.center(38))

        UserDiskPoolStorageClassLine = '%s%s' % (' ' * 101, '-' * 65)
        UserDiskPoolStorageClassTitle = "%s|%s|%s|%s|" % (' ' * 101, "DiskPoolName".center(20), "StorageClass".center(20), " "*21)

    print(UserTitleLine)
    print(title)
    print(UserTitleLine)
    for user in UserList:
        _userdiskpool = ''
        if Detail is None:
            _user ="|%s|%s|%s|" % ('{:20.20}'.format(str(user.Name).center(20)), user.Email.center(30), user.Id.center(38))
        else:
            _user ="|%s|%s|%s|%s|%s|" % ('{:20.20}'.format(str(user.Name).center(20)),
                                        user.AccessKey.center(30), user.SecretKey.center(42), user.Email.center(30), user.Id.center(38))
            for diskpool in user.UserDiskPools:
                _userdiskpool += "%s|%s|%s|%s|\n" % (' ' * 101, diskpool['DiskPoolName'].center(20), diskpool['StorageClass'].center(20), " "*21)
                _userdiskpool += "%s%s" % (' ' * 101, '-' * 65)

        print(_user)
        print(UserDataLine)
        if len(_userdiskpool) > 0:
            print(UserDiskPoolStorageClassTitle)
            print(UserDiskPoolStorageClassLine)
            print(_userdiskpool)
            print(UserDataLine)


def UserUtilHandler(Conf, Action, Parser, logger):

    options, args = Parser.parse_args()
    PortalIp = Conf.mgs.PortalIp
    PortalPort = Conf.mgs.PortalPort
    PortalApiKey = Conf.mgs.PortalApiKey
    MqPort = Conf.mgs.MqPort
    MqPassword = Conf.mgs.MqPassword

    if Action is None:
        Parser.print_help()
        sys.exit(-1)

    if Action.lower() == 'add':
        if not (options.UserName and options.Email):
            print('User Name and Email Info are required')
            sys.exit(-1)
        DefaultDiskpoolName = None
        Res, Errmsg, Ret =  GetDefaultDiskPool(PortalIp, PortalPort, PortalApiKey, logger=logger)
        if Res == ResOk:
            if Ret.Data is not None:
                DefaultDiskpoolName = Ret.Data.Name

        if options.DefaultDiskpool:
            DefaultDiskpoolName = options.DefaultDiskpool

        if DefaultDiskpoolName is None:
            print('Default Diskpool is not configured')
            sys.exit(-1)

        Res, Errmsg, Ret = AddS3User(PortalIp, PortalPort, PortalApiKey, options.UserName,
                                     DiskPoolName=DefaultDiskpoolName, Email=options.Email, logger=logger)
        if Res == ResOk:
            print(Ret.Result)
        else:
            print(Errmsg)
    elif Action.lower() == 'remove':
        if not options.UserName:
            Parser.print_help()
            sys.exit(-1)

        Conf = dict()
        Conf['isRemove']= get_input('Are you sure to remove user(yes|no)?', str, 'no', ValidAnsList=['yes', 'no'])
        if Conf['isRemove'] == 'yes':
            Res, Errmsg, Ret = RemoveS3User(PortalIp, PortalPort, PortalApiKey, UserName=options.UserName, logger=logger)
            if Res == ResOk:
                print(Ret.Result)
            else:
                print(Errmsg)

    elif Action.lower() == 'add2storageclass':
        if not (options.UserName and options.StorageClass and options.DiskpoolName):
            sys.exit(-1)

        Res, Errmsg, Ret = AddS3UserStorageClass(PortalIp, PortalPort, PortalApiKey ,options.StorageClass,
                                                 UserName=options.UserName, DiskPoolName=options.DiskpoolName, logger=logger)
        if Res == ResOk:
            print(Ret.Result)
        else:
            print(Errmsg)

    elif Action.lower() == 'remove2storageclass':
        if not (options.UserName and options.StorageClass and options.DiskpoolName):
            print('User Name, Default DiskPoolName and Email Info are required')
            sys.exit(-1)

        Res, Errmsg, Ret = RemoveS3UserStorageClass(PortalIp, PortalPort, PortalApiKey, options.StorageClass, UserName=options.UserName, DiskPoolName=options.DiskpoolName,
                                      logger=logger)
        if Res == ResOk:
            print(Ret.Result)
        else:
            print(Errmsg)


    elif Action.lower() == 'set':
        if not ((options.UserId or options.UserName) and options.Email ):
            Parser.print_help()
            sys.exit(-1)
        Res, Errmsg, Ret = UpdateS3UserInfo(PortalIp, PortalPort, PortalApiKey, UserName=options.UserName,
                                            Email=options.Email, logger=logger)
        if Res == ResOk:
            print(Ret.Result)
        else:
            print(Errmsg)

    elif Action.lower() == 'list':
        Res, Errmsg, Ret, Users = GetS3UserInfo(PortalIp, PortalPort, PortalApiKey, logger=logger)
        if Res != ResOk:
            print(Errmsg)
        else:
            if Ret.Result != ResultSuccess:
                print(Ret.Message)
            else:
                ShowS3UserInfo(Users, Detail=options.Detail)
    else:
        Parser.print_help()

