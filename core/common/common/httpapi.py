#!/usr/bin/env python3
# -*- coding: utf-8 -*-
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
import os
import http.client
import ssl
import requests
import urllib3
if os.path.dirname(os.path.abspath(os.path.dirname(__file__))) not in sys.path:
    sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from common.define import *
from common.display import disp_serverinfo
from common.log import Logging, catch_exceptions
import json
import jsonpickle
import pdb

AuthKey = "5de46d7ccd5d0954fad7d11ffc22a417e2784cbedd9f1dae3992a46e97b367e8"
IfsPortalIp = None
IfsPortalPort = None

# http response status
CONTINUE = 100
SWITCHING_PROTOCOLS = 101
PROCESSING = 102
OK = 200
CREATED = 201
ACCEPTED = 202
NON_AUTHORITATIVE_INFORMATION = 203
NO_CONTENT = 204
RESET_CONTENT = 205
PARTIAL_CONTENT = 206
MULTI_STATUS = 207
IM_USED = 226
MULTIPLE_CHOICES = 300
MOVED_PERMANENTLY = 301
FOUND = 302
SEE_OTHER = 303
NOT_MODIFIED = 304
USE_PROXY = 305
TEMPORARY_REDIRECT = 307
BAD_REQUEST = 400
UNAUTHORIZED = 401
PAYMENT_REQUIRED = 402
FORBIDDEN = 403
NOT_FOUND = 404
METHOD_NOT_ALLOWED = 405
NOT_ACCEPTABLE = 406
PROXY_AUTHENTICATION_REQUIRED = 407
REQUEST_TIMEOUT = 408
CONFLICT = 409
GONE = 410
LENGTH_REQUIRED = 411
PRECONDITION_FAILED = 412
REQUEST_ENTITY_TOO_LARGE = 413
REQUEST_URI_TOO_LONG = 414
UNSUPPORTED_MEDIA_TYPE = 415
REQUESTED_RANGE_NOT_SATISFIABLE = 416
EXPECTATION_FAILED = 417
UNPROCESSABLE_ENTITY = 422
LOCKED = 423
FAILED_DEPENDENCY = 424
UPGRADE_REQUIRED = 426
PRECONDITION_REQUIRED = 428
TOO_MANY_REQUESTS = 429
REQUEST_HEADER_FIELDS_TOO_LARGE = 431
INTERNAL_SERVER_ERROR = 500
NOT_IMPLEMENTED = 501
BAD_GATEWAY = 502
SERVICE_UNAVAILABLE = 503
GATEWAY_TIMEOUT = 504
HTTP_VERSION_NOT_SUPPORTED = 505
INSUFFICIENT_STORAGE = 507
NOT_EXTENDED = 510
NETWORK_AUTHENTICATION_REQUIRED = 511

class RestApi:

    def __init__(self, ip, port, url, header=None, params=None, protocol='https', authkey=AuthKey, logger=None):
        self._ip = ip
        self._port = port
        self._params = params
        self._protocol = protocol
        self._url = url
        self.logger = logger
        if self.logger is None:
            self.logger = Logging().get_logger()

        if header is None:
            header = dict()
        header['Authorization'] = authkey

        self._header = header
        if params is None:
            params = dict()
        self._params = params

    def get(self, ItemsHeader=True, ReturnType=None):
        url = "%s://%s:%d%s" % (self._protocol, self._ip, self._port, self._url)
        urllib3.disable_warnings()
        err_msg = ''
        try:
            r = requests.get(url, params=self._params, verify=False, headers=self._header)
            self.logging_request_info('GET', r)
            ret = r.content.decode('utf-8')
            Data = json.loads(ret)
            Ret = self.GetResponse(Data,ItemsHeader=ItemsHeader, ReturnType=ReturnType)
            return ResOk, '', Ret
        except requests.ConnectionError as err:
            self.logger.error("Connection Error %s" % err)
            return ResConnectionErrorCode, ResConnectionErrorMsg,None
        except requests.URLRequired as err:
            self.logger.error("UrlRequired Error %s" % err)
            return ResInvalidCode, ResInvalidMsg, None
        except (requests.ConnectTimeout, requests.ReadTimeout, requests.Timeout) as err:
            self.logger.error("Timeout Error %s" % err)
            return ResTimeErrorCode, ResTimeErrorCodeMsg, None
        except Exception as err:
            self.logger.error("Other Error %s" % err)
            return ResEtcErrorCode, ResEtcErrorMsg, None

    def post(self,ItemsHeader=True, ReturnType=None):
        url = "%s://%s:%d%s" % (self._protocol, self._ip, self._port, self._url)
        urllib3.disable_warnings()
        try:
            self._header['Content-Type'] = 'application/json'
            # self._params = json.dumps(self._params)
            r = requests.post(url=url, data=self._params, headers=self._header, verify=False)
            self.logging_request_info('POST', r)
            ret = r.content.decode('utf-8')
            Data = json.loads(ret)
            Ret = self.GetResponse(Data,ItemsHeader=ItemsHeader, ReturnType=ReturnType)
            return ResOk, '', Ret

        except requests.ConnectionError as err:
            self.logger.error("Connection Error %s" % err)
            return ResConnectionErrorCode, ResTimeErrorCodeMsg,None
        except requests.URLRequired as err:
            self.logger.error("UrlRequired Error %s" % err)
            return ResInvalidCode, ResInvalidMsg, None
        except (requests.ConnectTimeout, requests.ReadTimeout, requests.Timeout) as err:
            self.logger.error("Timeout Error %s" % err)
            return ResTimeErrorCode, ResTimeErrorCodeMsg, None
        except Exception as err:
            self.logger.error("Other Error %s" % err)
            return ResEtcErrorCode, ResEtcErrorMsg, None

    def put(self,ItemsHeader=True, ReturnType=None):
        url = "%s://%s:%d%s" % (self._protocol, self._ip, self._port, self._url)
        urllib3.disable_warnings()
        try:
            self._header['Content-Type'] = 'application/json-patch+json'
            #self._params = json.dumps(self._params)
            r = requests.put(url=url, data=self._params, headers=self._header, verify=False)
            self.logging_request_info('PUT', r)
            ret = r.content.decode('utf8')
            Data = json.loads(ret)
            Ret = self.GetResponse(Data,ItemsHeader=ItemsHeader, ReturnType=ReturnType)
            return ResOk, '', Ret
        except requests.ConnectionError as err:
            self.logger.error("Connection Error %s" % err)
            return ResConnectionErrorCode, ResTimeErrorCodeMsg,None
        except requests.URLRequired as err:
            self.logger.error("UrlRequired Error %s" % err)
            return ResInvalidCode, ResInvalidMsg, None
        except (requests.ConnectTimeout, requests.ReadTimeout, requests.Timeout) as err:
            self.logger.error("Timeout Error %s" % err)
            return ResTimeErrorCode, ResTimeErrorCodeMsg, None
        except Exception as err:
            self.logger.error("Other Error %s" % err)
            return ResEtcErrorCode, ResEtcErrorMsg, None

    @catch_exceptions()
    def delete(self, ItemsHeader=True, ReturnType=None):
        url = "%s://%s:%d%s" % (self._protocol, self._ip, self._port, self._url)
        urllib3.disable_warnings()
        try:
            self._header['Content-Type'] = 'application/json-patch+json'
            self._params = json.dumps(self._params)
            r = requests.delete(url=url, data=self._params, headers=self._header, verify=False)
            self.logging_request_info('DELETE', r)
            ret = r.content.decode('utf-8')
            Data = json.loads(ret)
            Ret = self.GetResponse(Data,ItemsHeader=ItemsHeader, ReturnType=ReturnType)
            return ResOk, '', Ret

        except requests.ConnectionError as err:
            self.logger.error("Connection Error %s" % err)
            return ResConnectionErrorCode, ResTimeErrorCodeMsg,None
        except requests.URLRequired as err:
            self.logger.error("UrlRequired Error %s" % err)
            return ResInvalidCode, ResInvalidMsg, None
        except (requests.ConnectTimeout, requests.ReadTimeout, requests.Timeout) as err:
            self.logger.error("Timeout Error %s" % err)
            return ResTimeErrorCode, ResTimeErrorCodeMsg, None
        except Exception as err:
            self.logger.error("Other Error %s" % err)
            return ResEtcErrorCode, ResEtcErrorMsg, None
    @catch_exceptions()
    def logging_request_info(self, method, req):
        try:
            self.logger.debug("Request %s url >>>>> %s" % (method, req.url))
            self.logger.debug("Header : %s" % req.request.headers)
            self.logger.debug("Body : %s" % req.request.body)
        except Exception as err:
            print(err)

    def logging_response_info(self, Data):
        try:
            self.logger.debug("Response Header <<<<< %s" % str(Data))
        except Exception as err:
            print(err)

    @catch_exceptions()
    def GetResponse(self, Ret, ItemsHeader=False, ReturnType=None):
        """
        Convert Dict Result to ResponseHeader Class
        There are three kinds of ResponseHeader
         - single type1(data:True/False)
         - sigle type2()
         - multi value(dict))
        """
        self.logging_response_info(Ret)
        if 'Data' in Ret:
            Ret.update({"py/object": ResponseHeaderWithDataModule})
            if Ret['Data'] is None: # if data items not exists.
                Ret = jsonpickle.decode(json.dumps(Ret))
                return Ret

            if ItemsHeader is True: # if items header exists
                Ret['Data'].update({"py/object": ResponseItemsHeaderModule})
                for Item in Ret['Data']['Items']:
                    DeserializeResponseReculsive(Item, ReturnType)
            else:
                DeserializeResponseReculsive(Ret['Data'], ReturnType)

        else:
            # Return
            DeserializeResponseReculsive(Ret, ResponseHeaderModule)

        Ret = jsonpickle.decode(json.dumps(Ret))
        return Ret

    def GetResponse1(self, Data, ItemsHeader=False):
        self.logging_response_info(Data)
        Data.update({"py/object": ResponseHeaderModule})
        if ItemsHeader is True:
            Data['Data'].update({"py/object": ResponseItemsHeaderModule})

        Ret = jsonpickle.decode(json.dumps(Data))
        return Ret

    @catch_exceptions()
    def parsing_result(self, header):
        '''
        cast total result dict to class(header, item header, data)
        :param header: dict
        :return: GetApiResult Object
        '''
        Header = None
        ItemsHeader = None
        Data = None
        try:
            self.logger.debug("Response << %s" % str(header))
            Header = ResPonseHeader(header)
            if "Data" in Header.__dict__:
                if isinstance(Header.Data, dict):
                    if "Items" in Header.Data:
                        ItemsHeader = ResPonseItemsHeader(Header.Data)
                    Data = DictToObject(Header.Data)
                else:
                    Data = Header.Data
            # print header attr
            self.logger.debug("Header: %s" % str(Header.__dict__))

            # print ItemsHeader attr
            if hasattr(ItemsHeader, '__dict__'):
                dict_attr = str(ItemsHeader.__dict__)
            else:
                dict_attr = 'None'
            self.logger.debug("ItemsHeader: %s" % dict_attr)

            # print Data attr

            if hasattr(Data, '__dict__'):
                dict_attr = str(Data.__dict__)
            else:
                dict_attr = 'None'
            self.logger.debug("Data: %s" % dict_attr)

        except Exception as err:
            self.logger.error("fail to parsing response data: %s line:%d" % (str(err), sys.exc_info()[2].tb_lineno))
        finally:
            return GetApiResult(Header, ItemsHeader, Data)


@catch_exceptions()
def DeserializeResponse(Data, ObjectType):
    """
    Get Data parsing with ObjectType Class
    :param Data: Dict type,
    :param ObjectType: Class name
    :return: ObjectType class
    """
    Data.update({"py/object": ObjectType})
    Ret = jsonpickle.decode(json.dumps(Data))
    return Ret


def DeserializeResponseReculsive(Data, ObjectType):
    """
    Get Data parsing with ObjectType Class
    :param Data: Dict type,
    :param ObjectType: Class name
    :return: ObjectType class
    """
    if ObjectType is not None:
        Data.update({"py/object": ObjectType})
    for key, val in Data.items():
        if key in Parsing.keys():
            if isinstance(val, list):
                for val1 in Data[key]:
                    DeserializeResponseReculsive(val1, Parsing[key])
            else:
                if isinstance(val, dict):
                    Data[key].update({"py/object": Parsing[key]})


def get_res(res):

    print("IsNeedLogin:", res.IsNeedLogin)
    print("AccessDenied:", res.AccessDenied)
    print("Result:", res.Result)
    print("Code:", res.Code)
    print("Message:", res.Message)
    if 'Data' in res.__dict__:
        if not isinstance(res.Data, dict):
            print("Data:", res.Data)

