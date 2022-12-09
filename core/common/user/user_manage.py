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
if os.path.dirname(os.path.abspath(os.path.dirname(__file__))) not in sys.path:
    sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from const.common import *
from disk.diskpool_manage import GetDefaultDiskPool
from const.user import AddUserObject, S3UserStorageClassObject, S3UserObject, S3UserUpdateObject
from common.utils import *


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
def ShowS3UserInfo(UserList, Detail=None, SysinfoDisp=False):
    """
    Display Network Interface Info
    :param InterfaceList: NetworkInterfaceItems class list
    :param NicId:
    :return:
    """
    UserList = UserListOrdering(UserList)
    if SysinfoDisp is True:
        UserTitleLine = '=' * 105
        UserDataLine = '-' * 105
        title = "|%s|%s|%s|" % ('UserName'.ljust(25), 'AccessKey'.ljust(32), 'SecretKey'.ljust(44))
        UserDiskPoolStorageClassLine = ''
        UserDiskPoolStorageClassTitle = ''
    else:
        if Detail is None:
            UserTitleLine = '=' * 92
            UserDataLine = '-' * 92
            title ="|%s|%s|%s|" % ('UserName'.ljust(20), 'Email'.ljust(30), 'UserId'.ljust(38))
            UserDiskPoolStorageClassLine = ''
            UserDiskPoolStorageClassTitle = ''
        else:
            UserTitleLine = '=' * 166
            UserDataLine = '-' * 166
            title = "|%s|%s|%s|%s|%s|" % ('UserName'.ljust(20), 'AccessKey'.ljust(30),
                                          'SecretKey'.ljust(42), 'Email'.ljust(30), 'UserId'.ljust(38))

            UserDiskPoolStorageClassLine = '%s%s' % (' ' * 101, '-' * 65)
            UserDiskPoolStorageClassTitle = "%s|%s|%s|%s|" % (' ' * 101, "DiskPoolName".ljust(20), "StorageClass".ljust(20), " "*21)

    print(UserTitleLine)
    print(title)
    print(UserTitleLine)
    if len(UserList) > 0:
        for user in UserList:
            _userdiskpool = ''

            user.Email = user.Email if user.Email is not None else ''
            if SysinfoDisp is True:
                _user = "|%s|%s|%s|" % ('{:25.25}'.format(str(user.Name).ljust(25)), user.AccessKey.ljust(32), user.SecretKey.ljust(44))
            else:
                if Detail is None:
                    _user ="|%s|%s|%s|" % ('{:20.20}'.format(str(user.Name).ljust(20)), user.Email.ljust(30), user.Id.ljust(38))
                else:
                    _user ="|%s|%s|%s|%s|%s|" % ('{:20.20}'.format(str(user.Name).ljust(20)),
                                                user.AccessKey.ljust(30), user.SecretKey.ljust(42), user.Email.ljust(30), user.Id.ljust(38))
                    for diskpool in user.UserDiskPools:
                        _userdiskpool += "%s|%s|%s|%s|\n" % (' ' * 101, diskpool['DiskPoolName'].ljust(20), diskpool['StorageClass'].ljust(20), " "*21)
                        _userdiskpool += "%s%s\n" % (' ' * 101, '-' * 65)

            print(_user)
            print(UserDataLine)
            if len(_userdiskpool) > 0:
                print(UserDiskPoolStorageClassTitle)
                print(UserDiskPoolStorageClassLine)
                print(_userdiskpool)
                print(UserDataLine)
    else:
        print('No user data'.ljust(105))
        print(UserDataLine)


def UserListOrdering(UserList):
    NewUserList = list()
    UserNameDict = dict()
    for userinfo in UserList:
        UserName = userinfo.Name
        UserNameDict[UserName] = userinfo

    for username in sorted(UserNameDict.keys(), key=str.casefold):
        userinfo = UserNameDict[username]
        NewUserList.append(userinfo)

    return NewUserList

def UserUtilHandler(Conf, Action, Parser, logger):

    options, args = Parser.parse_args()
    PortalIp = Conf.PortalHost
    PortalPort = Conf.PortalPort
    PortalApiKey = Conf.PortalApiKey
    MqPort = Conf.MQPort
    MqPassword = Conf.MQPassword

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
            print(Ret.Result, Ret.Message)
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
                print(Ret.Result, Ret.Message)
            else:
                print(Errmsg)

    elif Action.lower() == 'add2storageclass':
        if not (options.UserName and options.StorageClass and options.DiskpoolName):
            Parser.print_help()
            sys.exit(-1)

        if options.StorageClass not in ValidStorageClassList:
            print('Error : Unsupported User-defined Storage Class Name - Please Insert s3-compatible storage class (eg. glacier)')
            sys.exit(-1)

        Res, Errmsg, Ret = AddS3UserStorageClass(PortalIp, PortalPort, PortalApiKey ,options.StorageClass,
                                                 UserName=options.UserName, DiskPoolName=options.DiskpoolName, logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
        else:
            print(Errmsg)

    elif Action.lower() == 'remove2storageclass':
        if not (options.UserName and options.StorageClass and options.DiskpoolName):
            Parser.print_help()
            sys.exit(-1)

        if options.StorageClass not in ValidStorageClassList:
            print('Error : Unsupported User-defined Storage Class Name - Please Insert s3-compatible storage class (eg. glacier)')
            sys.exit(-1)

        Res, Errmsg, Ret = RemoveS3UserStorageClass(PortalIp, PortalPort, PortalApiKey, options.StorageClass, UserName=options.UserName, DiskPoolName=options.DiskpoolName,
                                      logger=logger)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
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

