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

import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.google.common.io.BaseEncoding;
import com.google.common.net.HttpHeaders;
import com.google.common.net.PercentEscaper;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final public class S3Signature {
    private Logger logger;
    
    private final PercentEscaper AWS_URL_PARAMETER_ESCAPER =
            new PercentEscaper(GWConstants.PERCENT_ESCAPER_SIGNATURE, false);
    
    private final Set<String> SIGNED_SUBRESOURCES = ImmutableSet.of(
            GWConstants.PARAMETER_ACL,
            GWConstants.PARAMETER_LIFECYCLE,
            GWConstants.PARAMETER_LOCATION,
            GWConstants.PARAMETER_LOGGING,
            GWConstants.PART_NUMBER,
            GWConstants.PARAMETER_POLICY,
            GWConstants.PARAMETER_REQUEST_PAYMENT,
            GWConstants.RESPONSE_CACHE_CONTROL,
            GWConstants.RESPONSE_CONTENT_DISPOSITION,
            GWConstants.RESPONSE_CONTENT_ENCODING,
            GWConstants.RESPONSE_CONTENT_LANGUAGE,
            GWConstants.RESPONSE_CONTENT_TYPE,
            GWConstants.RESPONSE_EXPIRES,
            GWConstants.PARAMETER_TORRENT,
            GWConstants.PARAMETER_UPLOAD_ID,
            GWConstants.PARAMETER_UPLOADS,
            GWConstants.PARAMETER_VERSION_ID,
            GWConstants.PARAMETER_VERSIONING,
            GWConstants.PARAMETER_VERSIONS,
            GWConstants.PARAMETER_WEBSITE,
            GWConstants.PARAMETER_CORS,
            GWConstants.PARAMETER_TAGGING,
            GWConstants.PARAMETER_REPLICATION,
            GWConstants.PARAMETER_DELETE,
            GWConstants.PARAMETER_TAG_INDEX,
            GWConstants.PARAMETER_RESTORE
    );

    S3Signature() { 
    	logger = LoggerFactory.getLogger(S3Signature.class);
    }

    /**
     * Create Amazon V2 signature.  Reference:
     * http://docs.aws.amazon.com/general/latest/gr/signature-version-2.html
     */
    public String createAuthorizationSignature(
            HttpServletRequest request, 
            String uri, 
            String credential,
            boolean queryAuth, 
            boolean bothDateHeader) {
        // sort Amazon headers
        SortedSetMultimap<String, String> canonicalizedHeaders = TreeMultimap.create();
        for (String headerName : Collections.list(request.getHeaderNames())) {
            Collection<String> headerValues = Collections.list(request.getHeaders(headerName));
            headerName = headerName.toLowerCase();
            if (!headerName.startsWith(GWConstants.START_WITH_X_AMZ) || (bothDateHeader && headerName.equalsIgnoreCase(GWConstants.X_AMZ_DATE_LOWER))) {
                continue;
            }
            
            if (headerValues.isEmpty()) {
                canonicalizedHeaders.put(headerName, "");
            }
            
            for (String headerValue : headerValues) {
                canonicalizedHeaders.put(headerName, Strings.nullToEmpty(headerValue));
            }
        }

        // Build string to sign
        StringBuilder builder = new StringBuilder();
        
        if (queryAuth) {
            String expires = request.getParameter(GWConstants.EXPIRES);
            String contentmd5 = request.getParameter(HttpHeaders.CONTENT_MD5);
            String contenttype = request.getParameter(HttpHeaders.CONTENT_TYPE);

            builder.append(request.getMethod())
                .append(GWConstants.CHAR_NEWLINE)
                .append(Strings.nullToEmpty(contentmd5))
                .append(GWConstants.CHAR_NEWLINE)
                .append(Strings.nullToEmpty(contenttype))
                .append(GWConstants.CHAR_NEWLINE);
            // If expires is  not nil, then it is query string sign
            // If expires is nil,maybe alse query string sign
            // So should check other accessid para ,presign to judge.
            // not the expires
            builder.append(Strings.nullToEmpty(expires));
        } else {
            builder.append(request.getMethod())
                .append(GWConstants.CHAR_NEWLINE)
                .append(Strings.nullToEmpty(request.getHeader(HttpHeaders.CONTENT_MD5)))
                .append(GWConstants.CHAR_NEWLINE)
                .append(Strings.nullToEmpty(request.getHeader(HttpHeaders.CONTENT_TYPE)))
                .append(GWConstants.CHAR_NEWLINE);
        	if (!bothDateHeader) {
        		if (canonicalizedHeaders.containsKey(GWConstants.X_AMZ_DATE_LOWER)) {
        			builder.append(GWConstants.EMPTY_STRING);
        		} else {
        			if (request.getHeader(HttpHeaders.DATE) != null) {
        				logger.info(GWConstants.LOG_S3SIGNATURE_DATE, request.getHeader(HttpHeaders.DATE));
        				builder.append(request.getHeader(HttpHeaders.DATE));
        			}
        		}
        	}
        }
        
        /*
        else {
        	// fixed by s3-test by jeong 2019-07-26
        	if (request.getHeader(HttpHeaders.DATE) != null) {
        		logger.info("Date2 : " + request.getHeader(HttpHeaders.DATE));
        		builder.append("x-amz-date:"+request.getHeader(HttpHeaders.DATE));
        	}
        }
        */
        
        builder.append(GWConstants.CHAR_NEWLINE);
        for (Map.Entry<String, String> entry : canonicalizedHeaders.entries()) {
            builder.append(entry.getKey()).append(GWConstants.CHAR_COLON)
                    .append(entry.getValue()).append(GWConstants.CHAR_NEWLINE);
        }

        builder.append(uri);

        char separator = GWConstants.CHAR_QUESTION;
        List<String> subresources = Collections.list(request.getParameterNames());
        Collections.sort(subresources);
        for (String subresource : subresources) {
            if (SIGNED_SUBRESOURCES.contains(subresource)) {
                builder.append(separator).append(subresource);

                String value = request.getParameter(subresource);
                if (!GWConstants.EMPTY_STRING.equals(value)) {
                    builder.append(GWConstants.CHAR_EQUAL).append(value);
                }
                separator = GWConstants.CHAR_AMPERSAND;
            }
        }

        String stringToSign = builder.toString();
        logger.debug(GWConstants.LOG_S3SIGNATURE_SIGN, stringToSign);

        // Sign string
        Mac mac;
        try {
            mac = Mac.getInstance(GWConstants.HMAC_SHA1);
            mac.init(new SecretKeySpec(credential.getBytes(StandardCharsets.UTF_8), GWConstants.HMAC_SHA1));
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return BaseEncoding.base64().encode(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] signMessage(byte[] data, byte[] key, String algorithm)
            throws InvalidKeyException, NoSuchAlgorithmException {
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(data);
    }

    private String getMessageDigest(byte[] payload, String algorithm)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] hash = md.digest(payload);
        return BaseEncoding.base16().lowerCase().encode(hash);
    }

    @Nullable
    private List<String> extractSignedHeaders(String authorization) {
        int index = authorization.indexOf(GWConstants.SIGNEDHEADERS_EQUAL);
        if (index < 0) {
            return null;
        }
        int endSigned = authorization.indexOf(GWConstants.CHAR_COMMA, index);
        if (endSigned < 0) {
            return null;
        }
        int startHeaders = authorization.indexOf(GWConstants.CHAR_EQUAL, index);
        return Splitter.on(GWConstants.CHAR_SEMICOLON).splitToList(authorization.substring(
                startHeaders + 1, endSigned));
    }

    private String buildCanonicalHeaders(HttpServletRequest request,
            List<String> signedHeaders) {
        List<String> headers = new ArrayList<>();
        for (String header : signedHeaders) {
            headers.add(header.toLowerCase());
        }
        Collections.sort(headers);
        List<String> headersWithValues = new ArrayList<>();
        for (String header : headers) {
            List<String> values = new ArrayList<>();
            StringBuilder headerWithValue = new StringBuilder();
            headerWithValue.append(header);
            headerWithValue.append(GWConstants.COLON);
            for (String value : Collections.list(request.getHeaders(header))) {
                value = value.trim();
                if (!value.startsWith(GWConstants.DOUBLE_QUOTE)) {
                    value = value.replaceAll(GWConstants.BACKSLASH_S_PLUS, GWConstants.SPACE);
                }
                values.add(value);
            }
            headerWithValue.append(Joiner.on(GWConstants.COMMA).join(values));
            headersWithValues.add(headerWithValue.toString());
        }

        return Joiner.on(GWConstants.NEWLINE).join(headersWithValues);
    }

    private String buildCanonicalQueryString(HttpServletRequest request)
            throws UnsupportedEncodingException {
        // The parameters are required to be sorted
        List<String> parameters = Collections.list(request.getParameterNames());
        Collections.sort(parameters);
        List<String> queryParameters = new ArrayList<>();

        for (String key : parameters) {
            if (key.equals(GWConstants.X_AMZ_SIGNATURE)) {
                continue;
            }
            // re-encode keys and values in AWS normalized form
            String value = request.getParameter(key);
            queryParameters.add(AWS_URL_PARAMETER_ESCAPER.escape(key) +
                    GWConstants.EQUAL + AWS_URL_PARAMETER_ESCAPER.escape(value));
        }
        return Joiner.on(GWConstants.AMPERSAND).join(queryParameters);
    }

    private String createCanonicalRequest(HttpServletRequest request,
                                                 String uri, byte[] payload,
                                                 String hashAlgorithm)
            throws IOException, NoSuchAlgorithmException {
        String authorizationHeader = request.getHeader(GWConstants.AUTHORIZATION);
        String xAmzContentSha256 = request.getHeader(GWConstants.X_AMZ_CONTENT_SHA256);
        if (xAmzContentSha256 == null) {
            xAmzContentSha256 = request.getParameter(GWConstants.X_AMZ_SIGNEDHEADERS);
        }
        String digest;
        if (authorizationHeader == null) {
            digest = GWConstants.UNSIGNED_PAYLOAD;
        } else if (GWConstants.STREAMING_AWS4_HMAC_SHA256_PAYLOAD.equals(xAmzContentSha256)) {
            digest = GWConstants.STREAMING_AWS4_HMAC_SHA256_PAYLOAD;
        } else if (GWConstants.UNSIGNED_PAYLOAD.equals(xAmzContentSha256)) {
            digest = GWConstants.UNSIGNED_PAYLOAD;
        } else {
        	if(xAmzContentSha256 == null)
        		digest = getMessageDigest(payload, hashAlgorithm);
        	else
        		digest = xAmzContentSha256;
        }
        
        List<String> signedHeaders;
        if (authorizationHeader != null) {
            signedHeaders = extractSignedHeaders(authorizationHeader);
        } else {
            signedHeaders = Splitter.on(GWConstants.COMMA).splitToList(request.getParameter(GWConstants.X_AMZ_SIGNEDHEADERS));
        }
        
        String method = request.getMethod();
        if (GWConstants.METHOD_OPTIONS.equals(method)) {
            String corsMethod = request.getHeader(
                    HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
            if (corsMethod != null) {
                method = corsMethod;
            }
        }

        logger.info("pre  : " + uri);
        uri = uri.replaceAll("\\$", "%24");
        uri = uri.replaceAll("'", "%27");
        uri = uri.replaceAll("!", "%21");
        uri = uri.replaceAll("\\(", "%28");
        uri = uri.replaceAll("\\)", "%29");
        uri = uri.replaceAll("\\*", "%2A");
        uri = uri.replaceAll(":", "%3A");
        uri = uri.replaceAll("\\[", "%5B");
        uri = uri.replaceAll("\\]", "%5D");
        logger.info("post : " + uri);
        
        String canonicalRequest = "";
        if(digest == null) {
        	canonicalRequest = Joiner.on(GWConstants.NEWLINE).join(
                    method,
                    uri,
                    buildCanonicalQueryString(request),
                    buildCanonicalHeaders(request, signedHeaders) + GWConstants.NEWLINE,
                    Joiner.on(GWConstants.CHAR_SEMICOLON).join(signedHeaders));
        } else {
        	canonicalRequest = Joiner.on(GWConstants.NEWLINE).join(
                    method,
                    uri,
                    buildCanonicalQueryString(request),
                    buildCanonicalHeaders(request, signedHeaders) + GWConstants.NEWLINE,
                    Joiner.on(GWConstants.CHAR_SEMICOLON).join(signedHeaders),
                    digest);
        }

        logger.info(canonicalRequest);

        return getMessageDigest(
                canonicalRequest.getBytes(StandardCharsets.UTF_8),
                hashAlgorithm);
    }

    /**
     * Create v4 signature.  Reference:
     * http://docs.aws.amazon.com/general/latest/gr/signature-version-4.html
     */
    public String createAuthorizationSignatureV4(
            HttpServletRequest request, S3AuthorizationHeader authHeader,
            byte[] payload, String uri, String credential)
            throws InvalidKeyException, IOException, NoSuchAlgorithmException,
            GWException {
        String canonicalRequest = createCanonicalRequest(request, uri, payload, authHeader.hashAlgorithm);
        String algorithm = authHeader.hmacAlgorithm;
        byte[] dateKey = signMessage(
                authHeader.date.getBytes(StandardCharsets.UTF_8),
                (GWConstants.AWS4 + credential).getBytes(StandardCharsets.UTF_8),
                algorithm);
        byte[] dateRegionKey = signMessage(
                authHeader.region.getBytes(StandardCharsets.UTF_8), 
                dateKey,
                algorithm);
        byte[] dateRegionServiceKey = signMessage(
                authHeader.service.getBytes(StandardCharsets.UTF_8),
                dateRegionKey, 
                algorithm);
        byte[] signingKey = signMessage(
                GWConstants.AWS4_REQUEST.getBytes(StandardCharsets.UTF_8),
                dateRegionServiceKey, 
                algorithm);
        String date = request.getHeader(GWConstants.X_AMZ_DATE_LOWER);
        if (date == null) {
            date = request.getParameter(GWConstants.X_AMZ_DATE);
        }
        
        String signatureString = GWConstants.AWS4_HMAC_SHA256 + GWConstants.NEWLINE +
                date + GWConstants.NEWLINE +
                authHeader.date + GWConstants.SLASH + authHeader.region +
                GWConstants.S3_AWS4_REQUEST + GWConstants.NEWLINE +
                canonicalRequest;
        logger.info(signatureString);
        
        byte[] signature = signMessage(
                signatureString.getBytes(StandardCharsets.UTF_8),
                signingKey, 
                algorithm);
        return BaseEncoding.base16().lowerCase().encode(signature);
    }

    private String createCanonicalRequestXFF(HttpServletRequest request,
            String uri, byte[] payload, String hashAlgorithm, String xffvalue)
            throws IOException, NoSuchAlgorithmException {
        String authorizationHeader = request.getHeader(GWConstants.AUTHORIZATION);
        String xAmzContentSha256 = request.getHeader(GWConstants.X_AMZ_CONTENT_SHA256);
        if (xAmzContentSha256 == null) {
            xAmzContentSha256 = request.getParameter(GWConstants.X_AMZ_SIGNEDHEADERS);
        }
        String digest;
        if (authorizationHeader == null) {
            digest = GWConstants.UNSIGNED_PAYLOAD;
        } else if (GWConstants.STREAMING_AWS4_HMAC_SHA256_PAYLOAD.equals(xAmzContentSha256)) {
            digest = GWConstants.STREAMING_AWS4_HMAC_SHA256_PAYLOAD;
        } else if (GWConstants.UNSIGNED_PAYLOAD.equals(xAmzContentSha256)) {
            digest = GWConstants.UNSIGNED_PAYLOAD;
        } else {
            if (xAmzContentSha256 == null)
                digest = getMessageDigest(payload, hashAlgorithm);
            else
                digest = xAmzContentSha256;
        }

        List<String> signedHeaders;
        if (authorizationHeader != null) {
            signedHeaders = extractSignedHeaders(authorizationHeader);
        } else {
            signedHeaders = Splitter.on(GWConstants.CHAR_SEMICOLON).splitToList(request.getParameter(GWConstants.X_AMZ_SIGNEDHEADERS));
        }

        String method = request.getMethod();
        if (GWConstants.METHOD_OPTIONS.equals(method)) {
            String corsMethod = request.getHeader(
                    HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
            if (corsMethod != null) {
                method = corsMethod;
            }
        }

        String canonicalRequest = "";
        if (digest == null) {
            canonicalRequest = Joiner.on(GWConstants.NEWLINE).join(
                    method,
                    uri,
                    buildCanonicalQueryString(request),
                    buildCanonicalHeadersXFF(request, signedHeaders, xffvalue) + GWConstants.NEWLINE,
                    Joiner.on(GWConstants.CHAR_SEMICOLON).join(signedHeaders));
        } else {
            canonicalRequest = Joiner.on(GWConstants.NEWLINE).join(
                    method,
                    uri,
                    buildCanonicalQueryString(request),
                    buildCanonicalHeadersXFF(request, signedHeaders, xffvalue) + GWConstants.NEWLINE,
                    Joiner.on(GWConstants.CHAR_SEMICOLON).join(signedHeaders),
                    digest);
        }

        logger.info(canonicalRequest);
        return getMessageDigest(
                canonicalRequest.getBytes(StandardCharsets.UTF_8),
                hashAlgorithm);
    }

    private String buildCanonicalHeadersXFF(HttpServletRequest request,
            List<String> signedHeaders, String xffvalue) {
        List<String> headers = new ArrayList<>();
        for (String header : signedHeaders) {
            headers.add(header.toLowerCase());
        }

        Collections.sort(headers);
        List<String> headersWithValues = new ArrayList<>();
        for (String header : headers) {
            if (header.equalsIgnoreCase(GWConstants.X_FORWARDED_FOR)) {
                StringBuilder headerWithValue = new StringBuilder();
                headerWithValue.append(header);
                headerWithValue.append(GWConstants.COLON);
                headerWithValue.append(xffvalue);
                headersWithValues.add(headerWithValue.toString());
            } else {

                List<String> values = new ArrayList<>();
                StringBuilder headerWithValue = new StringBuilder();
                headerWithValue.append(header);
                headerWithValue.append(GWConstants.COLON);
                for (String value : Collections.list(request.getHeaders(header))) {
                    value = value.trim();
                    if (!value.startsWith(GWConstants.DOUBLE_QUOTE)) {
                        value = value.replaceAll(GWConstants.BACKSLASH_S_PLUS, GWConstants.SPACE);
                    }

                    // 2021-04-14 UTF-8 signing fix
                    value = value.replaceAll(Constants.CHARSET_UTF_8, GWConstants.CHARSET_UTF_8_LOWER);

                    values.add(value);
                }
                headerWithValue.append(Joiner.on(GWConstants.COMMA).join(values));
                headersWithValues.add(headerWithValue.toString());
            }
        }

        return Joiner.on(GWConstants.NEWLINE).join(headersWithValues);
    }

    public String createAuthorizationSignatureV4XFF(
            HttpServletRequest request, S3AuthorizationHeader authHeader,
            byte[] payload, String uri, String credential, String xffvalue, S3Parameter s3Parameter)
            throws GWException {

        try {
            String canonicalRequest;
            canonicalRequest = createCanonicalRequestXFF(request, uri, payload, authHeader.hashAlgorithm, xffvalue);

            String algorithm = authHeader.hmacAlgorithm;
            byte[] dateKey = signMessage(
                    authHeader.date.getBytes(StandardCharsets.UTF_8),
                    (GWConstants.AWS4 + credential).getBytes(StandardCharsets.UTF_8),
                    algorithm);
            byte[] dateRegionKey = signMessage(
                    authHeader.region.getBytes(StandardCharsets.UTF_8), dateKey,
                    algorithm);
            byte[] dateRegionServiceKey = signMessage(
                    authHeader.service.getBytes(StandardCharsets.UTF_8),
                    dateRegionKey, algorithm);
            byte[] signingKey = signMessage(
                    GWConstants.AWS4_REQUEST.getBytes(StandardCharsets.UTF_8),
                    dateRegionServiceKey, algorithm);
            String date = request.getHeader(GWConstants.X_AMZ_DATE_LOWER);
            if (date == null) {
                date = request.getParameter(GWConstants.X_AMZ_DATE);
            }

            String signatureString = GWConstants.AWS4_HMAC_SHA256 + GWConstants.NEWLINE +
                    date + GWConstants.NEWLINE +
                    authHeader.date + GWConstants.SLASH + authHeader.region +
                    GWConstants.S3_AWS4_REQUEST + GWConstants.NEWLINE +
                    canonicalRequest;

            logger.info(authHeader.hmacAlgorithm);
            logger.info(signatureString);

            byte[] signature = signMessage(
                    signatureString.getBytes(StandardCharsets.UTF_8),
                    signingKey, algorithm);

            return BaseEncoding.base16().lowerCase().encode(signature);

        } catch (NoSuchAlgorithmException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.INVALID_ARGUMENT, e, s3Parameter);
        } catch (IOException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, e, s3Parameter);
        } catch (InvalidKeyException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.INVALID_ARGUMENT, e, s3Parameter);
        }
    }

    public String createAuthorizationSignatureGoogle(HttpServletRequest request, String uri, String credential) {
        StringBuilder builder = new StringBuilder();
        
        builder.append(request.getMethod())
               .append(GWConstants.CHAR_NEWLINE)
               .append(Strings.nullToEmpty(request.getParameter(HttpHeaders.CONTENT_MD5)))
               .append(GWConstants.CHAR_NEWLINE)
               .append(Strings.nullToEmpty(request.getParameter(HttpHeaders.CONTENT_TYPE)))
               .append(GWConstants.CHAR_NEWLINE)
               .append(request.getHeader(HttpHeaders.DATE))
               .append(GWConstants.CHAR_NEWLINE)
               .append(GWConstants.GCS_HEADER_API_VERSION)
               .append(GWConstants.CHAR_COLON)
               .append(request.getHeader(GWConstants.GCS_HEADER_API_VERSION))
               .append(GWConstants.CHAR_NEWLINE);
        if (!Strings.isNullOrEmpty(request.getHeader(GWConstants.GCS_HEADER_PROJECT_ID))) {
            builder.append(GWConstants.GCS_HEADER_PROJECT_ID)
                   .append(GWConstants.CHAR_COLON)
                   .append(request.getHeader(GWConstants.GCS_HEADER_PROJECT_ID))
                   .append(GWConstants.CHAR_NEWLINE);
        }
        builder.append(uri);

        char separator = GWConstants.CHAR_QUESTION;
        List<String> subresources = Collections.list(request.getParameterNames());
        Collections.sort(subresources);
        for (String subresource : subresources) {
            if (SIGNED_SUBRESOURCES.contains(subresource)) {
                builder.append(separator).append(subresource);

                String value = request.getParameter(subresource);
                if (!GWConstants.EMPTY_STRING.equals(value)) {
                    builder.append(GWConstants.CHAR_EQUAL).append(value);
                }
                separator = GWConstants.CHAR_AMPERSAND;
            }
        }

        String stringToSign = builder.toString();
        logger.debug(GWConstants.LOG_S3SIGNATURE_SIGN, stringToSign);

        // Sign string
        Mac mac;
        try {
            mac = Mac.getInstance(GWConstants.HMAC_SHA1);
            mac.init(new SecretKeySpec(credential.getBytes(StandardCharsets.UTF_8), GWConstants.HMAC_SHA1));
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return BaseEncoding.base64().encode(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
    }
}
