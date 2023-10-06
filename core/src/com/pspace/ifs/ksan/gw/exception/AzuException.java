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
package com.pspace.ifs.ksan.gw.exception;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.pspace.ifs.ksan.gw.identity.AzuParameter;

@SuppressWarnings("serial")
public final class AzuException extends Exception {
    private final AzuErrorCode error;
    private final Map<String, String> elements;
    private AzuParameter parameter = null;

    public AzuException(AzuErrorCode error) {
        this(error, error.getMessage(), (Throwable) null,
                ImmutableMap.<String, String>of());
    }

    public AzuException(AzuErrorCode error, String message, Throwable cause,
                Map<String, String> elements) {
        super(requireNonNull(message), cause);
        this.error = requireNonNull(error);
        this.elements = ImmutableMap.copyOf(elements);
    }
    
    public AzuException(AzuErrorCode error, Throwable cause, AzuParameter parameter) {
        this(error, error.getMessage(), cause,
                ImmutableMap.<String, String>of(), parameter);
    }

    public AzuException(AzuErrorCode error, String message, AzuParameter parameter) {
        this(error, message, (Throwable) null,
                ImmutableMap.<String, String>of(), parameter);
    }

    public AzuException(AzuErrorCode error, String message, Throwable cause, Map<String, String> elements, AzuParameter parameter) {
        super(requireNonNull(message), cause);
        this.parameter = new AzuParameter(parameter);
        if(this.parameter != null) {
            this.parameter.setErrorCode(error.getErrorCode());
            this.parameter.setStatusCode(error.getHttpStatusCode());
        }
            
        this.error = requireNonNull(error);
        this.elements = ImmutableMap.copyOf(elements);
    }

    public AzuException(AzuErrorCode error, AzuParameter parameter) {
        this(error, error.getMessage(), (Throwable) null, ImmutableMap.<String, String>of(), parameter);
    }

    public AzuErrorCode getError() {
        return error;
    }

    public Map<String, String> getElements() {
        return elements;
    }

    public AzuParameter getAZUParameter() {
        return new AzuParameter(parameter);
    }

    @Override
    public String toString() {
        return error + " " + elements;
    }
}

