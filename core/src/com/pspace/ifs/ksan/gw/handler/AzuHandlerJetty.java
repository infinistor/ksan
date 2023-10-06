/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.ifs.ksan.gw.handler;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpHeaders;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.pspace.ifs.ksan.gw.exception.AzuException;

public class AzuHandlerJetty extends AbstractHandler {
    private static final Logger logger = LoggerFactory.getLogger(AzuHandlerJetty.class);
    private final AzuHandler handler;

    public AzuHandlerJetty() {
        handler = new AzuHandler();
    }

    private void sendAZUException(HttpServletRequest request, HttpServletResponse response, AzuException e)
            throws IOException {
        handler.sendSimpleErrorResponse(request, response, e.getError(), e.getMessage(), e.getElements(), e.getAZUParameter());
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        try (InputStream is = request.getInputStream()) {
            handler.doHandle(baseRequest, request, response, is);
            baseRequest.setHandled(true);
        } catch (AzuException e) {
            sendAZUException(request, response, e);
            baseRequest.setHandled(true);
        }
    }
}