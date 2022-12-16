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
import os, sys
from fastapi import FastAPI
import uvicorn

if os.path.dirname(os.path.abspath(os.path.dirname(__file__))) not in sys.path:
    sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))

from const.rest import RestReturn
from const.common import AgentConf
from server.server_manage import RestHandlerServer


def RestServer(port):
    app = FastAPI()

    """
    @app.get("/")
    async def read_root():
        return {"Hello": "World"}

    @app.get("/items/{item_id}")
    async def read_item(item_id: int, q: Union[str, None] = None):
        return RestReturn
    """

    @app.post(path="/api/v1/Servers/Initialize", description="시스템 접속 정보를 초기화한다.", response_model=RestReturn)
    async def ServerInitialize(conf: AgentConf):
        rest = RestHandlerServer()
        return rest.InitAgentConf(conf)

    @app.post("/api/v1/Servers")
    #async def ServerAdd(LocalIp: str, PortalHost: str, PortalPort: int, MQHost: str, MQPort: int, MQPassword: str,
    #          MQUser: str, PortalApikey: str):
    async def ServerAdd(conf: AgentConf):
        rest = RestHandlerServer()
        return rest.AddServer(conf)

    return app

app = FastAPI()

"""
@app.get("/")
async def read_root():
    return {"Hello": "World"}

@app.get("/items/{item_id}")
async def read_item(item_id: int, q: Union[str, None] = None):
    return RestReturn
"""

@app.post(path="/api/v1/Servers/Initialize", description="시스템 접속 정보를 초기화한다.", response_model=RestReturn)
async def ServerInitialize(conf: AgentConf):
    rest = RestHandlerServer()
    return rest.InitAgentConf(conf)

@app.post(path="/api/v1/Servers", description="시스템 접속 정보를 초기화 하고 해당 서버를 시스템에 등록한다.", response_model=RestReturn)
#async def ServerAdd(LocalIp: str, PortalHost: str, PortalPort: int, MQHost: str, MQPort: int, MQPassword: str,
#          MQUser: str, PortalApikey: str):
async def ServerAdd(conf: AgentConf):
    rest = RestHandlerServer()
    return rest.AddServer(conf)


def StartRestServer(Port, AgentConf, logger=None):
    #App = RestServer(Port)

    uvicorn.run(app, host='0.0.0.0', port=Port)