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



import jsonpickle
import json

data = {
        "Server": {
          "Id": "1dec24c0-6e14-4a12-a0ca-c62c68377f56",
          "Name": "Dev Server 3",
        },
        "Id": "a10f5043-5973-477d-9556-f0ec2f60f75b",
        "ServerId": "1dec24c0-6e14-4a12-a0ca-c62c68377f56",
        "DiskPoolId": '',
        "DiskNo": "1366638474",
}

data1 = {"Id":"1", "Name":"2", "py/object": "__main__.Data1"}

class Server:
    def __init__(self, Id, Name):
        self.Id = Id
        self.Name = Name

class Disk:
    def __init__(self):
        self.Server = Server
        self.Id = ''
        self.DiskPoolId = ''
        self.DiskNo = ''
        self.a = ''

class Data1:
    def __init__(self, Id, Name):
        self.Id = Id
        self.Name = Name
#aa= jsonpickle.encode(data, make_refs=True)
#print(aa)

data.update({"py/object":"__main__.Disk"})

aa = jsonpickle.decode(json.dumps(data))
print(aa)
import pdb
pdb.set_trace()

class SubObject:
    def __init__(self, sub_name, sub_age):
        self.sub_name = sub_name
        self.sub_age = sub_age


class TestClass:

    def __init__(self, name, age, sub_object):
        self.name = name
        self.age = age
        self.sub_object = sub_object


john_junior = SubObject("John jr.", 2)

john = TestClass("John", 21, john_junior)

file_name = 'JohnWithSon' + '.json'

john_string = jsonpickle.encode(john)

with open(file_name, 'w') as fp:
    fp.write(john_string)

john_from_file = open(file_name).read()

test_class_2 = jsonpickle.decode(john_from_file)

print(test_class_2.name)
print(test_class_2.age)
print(test_class_2.sub_object.sub_name)


