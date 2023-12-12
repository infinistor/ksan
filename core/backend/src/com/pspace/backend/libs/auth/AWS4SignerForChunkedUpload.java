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
package com.pspace.backend.Libs.Auth;

import java.net.URL;
import java.util.Date;
import java.util.Map;

import com.amazonaws.util.BinaryUtils;

public class AWS4SignerForChunkedUpload extends AWS4SignerBase {
	public static final String STREAMING_BODY_SHA256 = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD";

	private static final String CLRF = "\r\n";
	private static final String CHUNK_STRING_TO_SIGN_PREFIX = "AWS4-HMAC-SHA256-PAYLOAD";
	private static final String CHUNK_SIGNATURE_HEADER = ";chunk-signature=";
	private static final int SIGNATURE_LENGTH = 64;
	private static final byte[] FINAL_CHUNK = new byte[0];

	private String lastComputedSignature;
	private String dateTimeStamp;
	private String scope;
	private byte[] signingKey;

	public AWS4SignerForChunkedUpload(URL endpointUrl, String httpMethod,
			String serviceName, String regionName) {
		super(endpointUrl, httpMethod, serviceName, regionName);
	}

	public String computeSignature(Map<String, String> headers, Map<String, String> queryParameters, String bodyHash, String AccessKey, String SecretKey) {
		Date now = new Date();
		this.dateTimeStamp = dateTimeFormat.format(now);

		headers.put("X-Amz-Date", dateTimeStamp);

		String hostHeader = endpointUrl.getHost();
		int port = endpointUrl.getPort();
		if (port > -1) {
			hostHeader = hostHeader.concat(":" + Integer.toString(port));
		}
		headers.put("Host", hostHeader);

		String canonicalizedHeaderNames = getCanonicalizeHeaderNames(headers);
		String canonicalizedHeaders = getCanonicalizedHeaderString(headers);
		String canonicalizedQueryParameters = getCanonicalizedQueryString(queryParameters);
		String canonicalRequest = getCanonicalRequest(endpointUrl, httpMethod,
				canonicalizedQueryParameters, canonicalizedHeaderNames,
				canonicalizedHeaders, bodyHash);

		String dateStamp = dateStampFormat.format(now);
		this.scope = dateStamp + "/" + regionName + "/" + serviceName + "/" + TERMINATOR;
		String stringToSign = getStringToSign(SCHEME, ALGORITHM, dateTimeStamp, scope, canonicalRequest);

		byte[] kSecret = (SCHEME + SecretKey).getBytes();
		byte[] kDate = sign(dateStamp, kSecret, "HmacSHA256");
		byte[] kRegion = sign(regionName, kDate, "HmacSHA256");
		byte[] kService = sign(serviceName, kRegion, "HmacSHA256");
		this.signingKey = sign(TERMINATOR, kService, "HmacSHA256");
		byte[] signature = sign(stringToSign, signingKey, "HmacSHA256");

		lastComputedSignature = BinaryUtils.toHex(signature);

		String credentialsAuthorizationHeader = "Credential=" + AccessKey + "/" + scope;
		String signedHeadersAuthorizationHeader = "SignedHeaders=" + canonicalizedHeaderNames;
		String signatureAuthorizationHeader = "Signature=" + lastComputedSignature;
		String authorizationHeader = SCHEME + "-" + ALGORITHM + " "
				+ credentialsAuthorizationHeader + ", "
				+ signedHeadersAuthorizationHeader + ", "
				+ signatureAuthorizationHeader;

		return authorizationHeader;
	}

	public static long calculateChunkedContentLength(long originalLength, long chunkSize) {
		if (originalLength <= 0) {
			throw new IllegalArgumentException("Nonnegative content length expected.");
		}

		long maxSizeChunks = originalLength / chunkSize;
		long remainingBytes = originalLength % chunkSize;
		return maxSizeChunks * calculateChunkHeaderLength(chunkSize)
				+ (remainingBytes > 0 ? calculateChunkHeaderLength(remainingBytes) : 0)
				+ calculateChunkHeaderLength(0);
	}

	private static long calculateChunkHeaderLength(long chunkDataSize) {
		return Long.toHexString(chunkDataSize).length()
				+ CHUNK_SIGNATURE_HEADER.length()
				+ SIGNATURE_LENGTH
				+ CLRF.length()
				+ chunkDataSize
				+ CLRF.length();
	}

	public byte[] constructSignedChunk(int userDataLen, byte[] userData) {
		byte[] dataToChunk;
		if (userDataLen == 0) {
			dataToChunk = FINAL_CHUNK;
		} else {
			if (userDataLen < userData.length) {
				// shrink the chunkdata to fit
				dataToChunk = new byte[userDataLen];
				System.arraycopy(userData, 0, dataToChunk, 0, userDataLen);
			} else {
				dataToChunk = userData;
			}
		}

		StringBuilder chunkHeader = new StringBuilder();

		// start with size of user data
		chunkHeader.append(Integer.toHexString(dataToChunk.length));

		// nonsig-extension; we have none in these samples
		String nonsigExtension = "";

		// sig-extension
		String chunkStringToSign = CHUNK_STRING_TO_SIGN_PREFIX + "\n" +
				dateTimeStamp + "\n" +
				scope + "\n" +
				lastComputedSignature + "\n" +
				BinaryUtils.toHex(AWS4SignerBase.hash(nonsigExtension)) + "\n" +
				BinaryUtils.toHex(AWS4SignerBase.hash(dataToChunk));

		String chunkSignature = BinaryUtils.toHex(AWS4SignerBase.sign(chunkStringToSign, signingKey, "HmacSHA256"));
		lastComputedSignature = chunkSignature;

		chunkHeader.append(nonsigExtension + CHUNK_SIGNATURE_HEADER + chunkSignature);
		chunkHeader.append(CLRF);

		try {
			byte[] header = chunkHeader.toString().getBytes("UTF-8");
			byte[] trailer = CLRF.getBytes("UTF-8");
			byte[] signedChunk = new byte[header.length + dataToChunk.length + trailer.length];
			System.arraycopy(header, 0, signedChunk, 0, header.length);
			System.arraycopy(dataToChunk, 0, signedChunk, header.length, dataToChunk.length);
			System.arraycopy(trailer, 0, signedChunk, header.length + dataToChunk.length, trailer.length);

			return signedChunk;
		} catch (Exception e) {
			throw new RuntimeException("Unable to sign the chunked data. " + e.getMessage(), e);
		}
	}
}