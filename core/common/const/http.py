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
import json
from const.server import ServerItemsModule
from const.network import NetworkInterfaceItemsModule, VlanNetworkInterfaceItemsModule
from const.disk import DiskItemsDetailModule, DiskPoolItemsModule
from const.service import ServiceGroupItemsModule, ServiceItemsDetailModule
from const.user import UserObjectModule
from common.log import catch_exceptions

#####  Header of Response Body Class for deserialization #####
ResponseItemsHeaderModule = 'const.http.ResponseItemsHeader'
ResponseHeaderModule = 'const.http.ResponseHeader'
ResponseHeaderWithDataModule = 'const.http.ResponseHeaderWithData'

Parsing = dict()

Parsing['Server'] = ServerItemsModule
Parsing['NetworkInterfaces'] = NetworkInterfaceItemsModule
Parsing['NetworkInterfaceVlans'] = VlanNetworkInterfaceItemsModule
Parsing['Vlans'] = VlanNetworkInterfaceItemsModule
Parsing['Disks'] = DiskItemsDetailModule
Parsing['DiskPool'] = DiskPoolItemsModule
Parsing['Services'] = ServiceItemsDetailModule
Parsing['ServiceGroup'] = ServiceGroupItemsModule
Parsing['User'] = UserObjectModule

@catch_exceptions()
class GetApiResult:
    def __init__(self, Header, ItemHeader, Items):
        self.Header = Header
        self.ItemHeader = ItemHeader
        self.Items = Items


class ResponseHeader(object):
    """
    Parsing Response without "Data" value is single value like True/False
    """
    def __init__(self):
        self.IsNeedLogin = None
        self.AccessDenied = None
        self.Result = None
        self.Code = None
        self.Message = None

    def Set(self, IsNeedLogin, AccessDenied, Result, Code, Message):
        self.IsNeedLogin = IsNeedLogin
        self.AccessDenied = AccessDenied
        self.Result = Result
        self.Code = Code
        self.Message = Message

class ResponseHeaderWithData(object):
    """
    Parsing Response with "Data" value is multi value like dict or list
    """
    def __init__(self):
        self.IsNeedLogin = None
        self.AccessDenied = None
        self.Result = None
        self.Code = None
        self.Message = None
        self.Data = None

    def Set(self, IsNeedLogin, AccessDenied, Result, Code, Message, Data):
        self.IsNeedLogin = IsNeedLogin
        self.AccessDenied = AccessDenied
        self.Result = Result
        self.Code = Code
        self.Message = Message
        self.Data = Data


class ResponseItemsHeader(object):
    def __init__(self):
        self.TotalCount = ''
        self.Skips = ''
        self.PageNo = ''
        self.CountPerPage = ''
        self.PagePerSection = ''
        self.TotalPage = ''
        self.StartPageNo = ''
        self.EndPageNo = ''
        self.PageNos = ''
        self.HavePreviousPage = ''
        self.HaveNextPage = ''
        self.HavePreviousPageSection = ''
        self.HaveNextPageSection = ''
        self.Items = ''





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


#####  Response Body Decoder Class #####
class ResPonseHeaderDecoder(json.JSONDecoder):
    def __init__(self, logger=None, *args, **kwargs):
        # json.JSONDecoder.__init__(self, object_hook=self.object_hook, *args, **kwargs)
        self.logger=logger
        json.JSONDecoder.__init__(self, object_hook=self.object_hook, *args, **kwargs)

    def object_hook(self, dct):
        try:
            print(">>>>", dct)
            if 'IsNeedLogin' in dct:
                obj = ResPonseHeader(dct)
                if 'Data' in dct:
                    print(dct['Data'])
                return obj
        except KeyError as err:
            if self.logger is not None:
                self.logger.error(err, sys.exc_info()[2].tb_lineno)
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


