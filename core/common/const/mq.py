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
from enum import Enum
from const.common import *
import json
import pika


class EnumResponseResult(Enum):
    Error = -1
    Warning = 0
    Success = 1



from typing import TypeVar, Generic, Type

T = TypeVar('T')

class ResponseData:
    def __init__(self, result: EnumResponseResult = EnumResponseResult.Error, code: str = "", messsage: str = "") -> None:
        self.Result = result
        self.Code = code
        self.Message = messsage

    def load(self, data: dict):
        self.Result = data[RetKeyResult]
        self.Code = data[RetKeyCode]
        self.Message = data[RetKeyMessage]



def MqReturn(Result, Code=0, Messages='', Data=None):
    Ret = dict()
    if Result is True or Result == ResultSuccess:
        Ret['Result'] = 'Success'
    else:
        Ret['Result'] = 'Error'
    Ret['Code'] = Code

    Ret['Message'] = Messages
    if Data:
        Ret['Data'] = Data
    #Ret['Data'] = json.dumps(Ret)
    return json.dumps(Ret)

def RemoveQueue(QueueHost, QueueName):
    connection = pika.BlockingConnection(pika.ConnectionParameters(QueueHost))
    channel = connection.channel()

    channel.queue_delete(queue=QueueName)
    connection.close()




class ResponseDataWithData(Generic[T]):
    def __init__(self, result: EnumResponseResult = EnumResponseResult.Error, code: str = "", message: str = "", value: T = None) -> None:
        super().__init__()
        self.Result = result
        self.Code = code
        self.Message = message
        self.Data = value

    def load(self, data: dict):
        self.__dict__ = data.copy()


class ResponseMqData:
    def __init__(self, result: EnumResponseResult = EnumResponseResult.Error, code: str = "", messsage: str = "") -> None:
        self.Result = result
        self.Code = code
        self.Message = messsage
        self.IsProcessed = False

    def load(self, data: dict):
        self.Result = data[RetKeyResult]
        self.Code = data[RetKeyCode]
        self.Message = data[RetKeyMessage]
