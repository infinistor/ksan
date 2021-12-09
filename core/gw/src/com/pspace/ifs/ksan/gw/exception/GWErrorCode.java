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

import javax.servlet.http.HttpServletResponse;

import com.google.common.base.CaseFormat;

/**
 * List of S3 error codes.  Reference:
 * http://docs.aws.amazon.com/AmazonS3/latest/API/ErrorResponses.html
 */
public enum GWErrorCode {
    SERVER_ERROR(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error"),
    ACCESS_DENIED(HttpServletResponse.SC_FORBIDDEN, "Forbidden"),
    BAD_DIGEST(HttpServletResponse.SC_BAD_REQUEST, "Bad Digest"),
    BAD_REQUEST(HttpServletResponse.SC_BAD_REQUEST, "Bad Request"),
    BUCKET_ALREADY_EXISTS(HttpServletResponse.SC_CONFLICT,
            "The requested bucket name is not available." +
            " The bucket namespace is shared by all users of the system." +
            " Please select a different name and try again."),
    BUCKET_ALREADY_OWNED_BY_YOU(HttpServletResponse.SC_CONFLICT,
            "Your previous request to create the named bucket" +
            " succeeded and you already own it."),
    BUCKET_NOT_EMPTY(HttpServletResponse.SC_CONFLICT,
            "The bucket you tried to delete is not empty"),
    ENTITY_TOO_SMALL(HttpServletResponse.SC_BAD_REQUEST,
            "Your proposed upload is smaller than the minimum allowed object size."),
    ENTITY_TOO_LARGE(HttpServletResponse.SC_BAD_REQUEST,
            "Your proposed upload exceeds the maximum allowed object size."),
    INVALID_ACCESS_KEY_ID(HttpServletResponse.SC_FORBIDDEN, "Forbidden"),
    INVALID_ARGUMENT(HttpServletResponse.SC_BAD_REQUEST, "Bad Request"),
    INVALID_BUCKET_NAME(HttpServletResponse.SC_BAD_REQUEST,
            "The specified bucket is not valid."),
    INVALID_BUCKET_STATE(HttpServletResponse.SC_CONFLICT,
            "The specified bucket is not valid."),
    INVALID_DIGEST(HttpServletResponse.SC_BAD_REQUEST, "Invalid Digest"),
    INVALID_LOCATION_CONSTRAINT(HttpServletResponse.SC_BAD_REQUEST,
            "The specified location constraint is not valid. For" +
            " more information about Regions, see How to Select" +
            " a Region for Your Buckets."),
    INVALID_RANGE(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
            "The requested range is not satisfiable"),
    INVALID_PART(HttpServletResponse.SC_BAD_REQUEST,
            "One or more of the specified parts could not be found." +
            "  The part may not have been uploaded, or the specified entity" +
            " tag may not match the part's entity tag."),
    INVALID_REQUEST(HttpServletResponse.SC_BAD_REQUEST, "Bad Request"),
    INVALID_TAG(HttpServletResponse.SC_BAD_REQUEST,
            "The requested tagging key or value exceeds length"),
    INVALID_EXCEED_TAG(HttpServletResponse.SC_BAD_REQUEST,
            "The requested tagging count has not exceed 10"),
    INVALID_CORS_ORIGIN(HttpServletResponse.SC_BAD_REQUEST,
            "Insufficient information. Origin request header needed."),
    INVALID_CORS_METHOD(HttpServletResponse.SC_BAD_REQUEST,
            "The specified Access-Control-Request-Method is not valid."),
    MALFORMED_X_M_L(HttpServletResponse.SC_BAD_REQUEST,
            "The XML you provided was not well-formed or did not validate" +
            " against our published schema."),
    MAX_MESSAGE_LENGTH_EXCEEDED(HttpServletResponse.SC_BAD_REQUEST,
            "Your request was too big."),
    METHOD_NOT_ALLOWED(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
            "Method Not Allowed"),
    MISSING_CONTENT_LENGTH(HttpServletResponse.SC_LENGTH_REQUIRED,
            "Length Required"),
    NO_SUCH_BUCKET(HttpServletResponse.SC_NOT_FOUND,
            "The specified bucket does not exist"),
    NO_SUCH_WEBSITE_CONFIGURATION(HttpServletResponse.SC_NOT_FOUND,
            "The specified bucket does not have a website configuration"),
    NO_SUCH_POLICY_CONFIGURATION(HttpServletResponse.SC_NOT_FOUND,
            "The specified bucket does not have a policy configuration"),
    NO_SUCH_CORS_CONFIGURATION(HttpServletResponse.SC_NOT_FOUND,
            "The specified bucket does not have a CORS configuration"),
    NO_SUCH_TAG_SET_ERROR(HttpServletResponse.SC_NOT_FOUND,
            "The specified bucket does not have a Tagging SET"),
    NO_SUCH_LIFECYCLE_CONFIGURATION(HttpServletResponse.SC_NOT_FOUND,
            "The specified bucket does not have a LifeCycle configuration"),
    NO_SUCH_ENCRYPTION_CONFIGURATION(HttpServletResponse.SC_NOT_FOUND,
            "The specified bucket does not have a Server Side Encryption configuration"),
    NO_SUCH_PUBLICACCESSBLOCK_CONFIGURATION(HttpServletResponse.SC_NOT_FOUND,
            "The specified bucket does not have a Public Access Block configuration"),
    NO_SUCH_REPLICATION_CONFIGURATION(HttpServletResponse.SC_NOT_FOUND,
            "The specified bucket does not have a Replication configuration"),            
    OBJECT_LOCK_CONFIGURATION_NOT_FOUND_ERROR(HttpServletResponse.SC_NOT_FOUND,
            "The specified bucket does not have a Object Lock configuration"),
    ILLEGAL_VERSIONING_CONFIGURATION(HttpServletResponse.SC_BAD_REQUEST, 
            "Indicates that the versioning configuration specified in the request is invalid."),
    INVALID_REPLICATION_REQUEST(HttpServletResponse.SC_BAD_REQUEST, 
            "Versioning must be Enabled on the bucket to apply a replication configuration"),
    NO_SUCH_KEY(HttpServletResponse.SC_NOT_FOUND,
            "The specified key does not exist."),
    NO_SUCH_UPLOAD(HttpServletResponse.SC_NOT_FOUND, "Not Found"),
    NOT_IMPLEMENTED(HttpServletResponse.SC_NOT_IMPLEMENTED,
            "A header you provided implies functionality that is not" +
            " implemented."),
    INSUFFICIENT_STORAGE(507, "S3 Quota has exceeded"), // 507 INSUFFICIENT_STORAGE
    PRECONDITION_FAILED(HttpServletResponse.SC_PRECONDITION_FAILED,
            "At least one of the preconditions you specified did not hold."),
    REQUEST_TIME_TOO_SKEWED(HttpServletResponse.SC_FORBIDDEN, "Forbidden"),
    REQUEST_TIMEOUT(HttpServletResponse.SC_BAD_REQUEST, "Bad Request"),
    SIGNATURE_DOES_NOT_MATCH(HttpServletResponse.SC_FORBIDDEN, "Forbidden"),
    KEY_DOES_NOT_MATCH(HttpServletResponse.SC_FORBIDDEN, "Forbidden"),
    ILLEGAL_RANGE(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE , "Server cannot serve the requested byte range"),
    DOES_NOT_MATCH(HttpServletResponse.SC_NOT_MODIFIED, "not modified"),
    X_AMZ_CONTENT_S_H_A_256_MISMATCH(HttpServletResponse.SC_BAD_REQUEST,
            "The provided 'x-amz-content-sha256' header does not match what" +
            " was computed."),
    INTERNAL_SERVER_DB_ERROR(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "S3 database error has occurred"),
    INTERNAL_SERVER_ERROR(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "S3 server internal server error has occurred");

    private final String errorCode;
    private final int httpStatusCode;
    private final String message;

    GWErrorCode(int httpStatusCode, String message) {
        this.errorCode = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL,
                name());
        this.httpStatusCode = httpStatusCode;
        this.message = requireNonNull(message);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return getHttpStatusCode() + " " + getErrorCode() + " " + getMessage();
    }
}
