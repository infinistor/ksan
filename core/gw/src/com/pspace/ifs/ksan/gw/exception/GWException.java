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
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

@SuppressWarnings("serial")
public final class GWException extends Exception {
    private final GWErrorCode error;
    private final Map<String, String> elements;
    private S3Parameter s3Parameter = null;

    public GWException(GWErrorCode error) {
        this(error, error.getMessage(), (Throwable) null,
                ImmutableMap.<String, String>of());
    }

    public GWException(GWErrorCode error, String message) {
        this(error, message, (Throwable) null,
                ImmutableMap.<String, String>of());
    }

    public GWException(GWErrorCode error, Throwable cause) {
        this(error, error.getMessage(), cause,
                ImmutableMap.<String, String>of());
    }

    public GWException(GWErrorCode error, Throwable cause, S3Parameter s3Parameter) {
        this(error, error.getMessage(), cause,
                ImmutableMap.<String, String>of(), s3Parameter);
    }

    public GWException(GWErrorCode error, String message, S3Parameter s3Parameter) {
        this(error, message, (Throwable) null,
                ImmutableMap.<String, String>of(), s3Parameter);
    }
    
    public GWException(GWErrorCode error, String message, Throwable cause, S3Parameter s3Parameter) {
        this(error, message, cause, ImmutableMap.<String, String>of(), s3Parameter);
    }

    public GWException(GWErrorCode error, String message, Throwable cause, Map<String, String> elements, S3Parameter s3Parameter) {
        super(requireNonNull(message), cause);
        this.s3Parameter = s3Parameter;
        if(this.s3Parameter != null) {
            this.s3Parameter.setErrorCode(error.getErrorCode());
            this.s3Parameter.setStatusCode(error.getHttpStatusCode());
        }
            
        this.error = requireNonNull(error);
        this.elements = ImmutableMap.copyOf(elements);
    }

    public GWException(GWErrorCode error, String message, Throwable cause) {
        this(error, message, cause, ImmutableMap.<String, String>of());
    }

    public GWException(GWErrorCode error, String message, Throwable cause,
                Map<String, String> elements) {
        super(requireNonNull(message), cause);
        this.error = requireNonNull(error);
        this.elements = ImmutableMap.copyOf(elements);
    }

    public GWException(GWErrorCode error, S3Parameter s3Parameter) {
        this(error, error.getMessage(), (Throwable) null, ImmutableMap.<String, String>of(), s3Parameter);
    }

    public GWErrorCode getError() {
        return error;
    }

    public Map<String, String> getElements() {
        return elements;
    }

    @Override
    public String toString() {
        return error + GWConstants.SPACE + elements;
    }
}
