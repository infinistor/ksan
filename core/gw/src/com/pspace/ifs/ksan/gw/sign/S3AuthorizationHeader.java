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
package com.pspace.ifs.ksan.gw.sign;

import java.util.List;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

public final class S3AuthorizationHeader {
    private final ImmutableMap<String, String> DIGEST_MAP =
            ImmutableMap.<String, String>builder()
            .put(GWConstants.SHA256, GWConstants.SHA_256)
            .put(GWConstants.SHA1, GWConstants.SHA_1)
            .put(GWConstants.MD5, GWConstants.MD5)
            .build();

    // TODO: these fields should have accessors
    // CHECKSTYLE:OFF
    public final AuthenticationType authenticationType;
    public final String hmacAlgorithm;
    public final String hashAlgorithm;
    public final String region;
    public final String date;
    public final String service;
    public final String identity;
    public final String signature;
    // CHECKSTYLE:ON

    S3AuthorizationHeader(String header) {
    	if (header ==  null) {
    		throw new IllegalArgumentException(GWConstants.INVALID_HEADER);
    	}
    	
        if (header.startsWith(GWConstants.AWS_SPACE)) {
            authenticationType = AuthenticationType.AWS_V2;
            hmacAlgorithm = null;
            hashAlgorithm = null;
            region = null;
            date = null;
            service = null;
            List<String> fields = Splitter.on(GWConstants.CHAR_SPACE).splitToList(header);
            if (fields.size() != GWConstants.AUTH_FIELD_SIZE) {
                throw new IllegalArgumentException(GWConstants.INVALID_HEADER);
            }
            List<String> identityTuple = Splitter.on(GWConstants.CHAR_COLON).splitToList(fields.get(GWConstants.SIGNATURE_INDEX));
            if (identityTuple.size() != GWConstants.AUTH_FIELD_SIZE) {
                throw new IllegalArgumentException(GWConstants.INVALID_HEADER);
            }
            identity = identityTuple.get(GWConstants.IDENTITY_INDEX);
            signature = identityTuple.get(GWConstants.SIGNATURE_INDEX);
        } else if (header.startsWith(GWConstants.AWS4_HMAC)) {
            authenticationType = AuthenticationType.AWS_V4;
            signature = extractSignature(header);

            int credentialIndex = header.indexOf(GWConstants.CREDENTIAL_FIELD);
            if (credentialIndex < 0) {
                throw new IllegalArgumentException(GWConstants.INVALID_HEADER);
            }
            int credentialEnd = header.indexOf(GWConstants.CHAR_COMMA, credentialIndex);
            if (credentialEnd < 0) {
                throw new IllegalArgumentException(GWConstants.INVALID_HEADER);
            }
            String credential = header.substring(credentialIndex + GWConstants.CREDENTIAL_FIELD.length(), credentialEnd);
            List<String> fields = Splitter.on(GWConstants.CHAR_SLASH).splitToList(credential);
            if (fields.size() != GWConstants.AWS4_AUTH_FIELD_SIZE) {
                throw new IllegalArgumentException(GWConstants.INVALID_CREDENTIAL + credential);
            }
            identity = fields.get(GWConstants.AWS4_IDENTITY_FIELD_INDEX);
            date = fields.get(GWConstants.AWS4_DATE_INDEX);
            region = fields.get(GWConstants.AWS4_REGION_INDEX);
            service = fields.get(GWConstants.AWS4_SERVICE_INDEX);
            String awsSignatureVersion = header.substring(0, header.indexOf(GWConstants.CHAR_SPACE));
            hashAlgorithm = DIGEST_MAP.get(Splitter.on(GWConstants.CHAR_DASH).splitToList(awsSignatureVersion).get(GWConstants.AWS4_REGION_INDEX));
            hmacAlgorithm = GWConstants.HMAC + Splitter.on(GWConstants.CHAR_DASH).splitToList(awsSignatureVersion).get(GWConstants.AWS4_REGION_INDEX);
        } else {
            throw new IllegalArgumentException(GWConstants.INVALID_HEADER);
        }
    }

    @Override
    public String toString() {
        return "Identity: " + identity +
                "; Signature: " + signature +
                "; HMAC algorithm: " + hmacAlgorithm +
                "; Hash algorithm: " + hashAlgorithm +
                "; region: " + region +
                "; date: " + date +
                "; service " + service;
    }

    private String extractSignature(String header) {
        int signatureIndex = header.indexOf(GWConstants.SIGNATURE_FIELD);
        if (signatureIndex < 0) {
            throw new IllegalArgumentException(GWConstants.INVALID_SIGNATURE);
        }
        signatureIndex += GWConstants.SIGNATURE_FIELD.length();
        int signatureEnd = header.indexOf(GWConstants.CHAR_COMMA, signatureIndex);
        if (signatureEnd < 0) {
            return header.substring(signatureIndex);
        } else {
            return header.substring(signatureIndex, signatureEnd);
        }
    }
}
