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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.common.io.ByteStreams;
import com.pspace.ifs.ksan.gw.utils.GWConstants;


/**
 * Parse an AWS v4 signature chunked stream.  Reference:
 * https://docs.aws.amazon.com/AmazonS3/latest/API/sigv4-streaming.html
 */


public final class ChunkedInputStream extends FilterInputStream {
    private byte[] chunk;
    private int currentIndex;
    private int currentLength;
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
            value = GWConstants.LOG_CHUNKED_INPUT_STREAM_URF,
            justification = GWConstants.LOG_CHUNKED_JUSTIFICATION)
    private String currentSignature;

    public ChunkedInputStream(InputStream is) {
        super(is);
    }

    @Override
    public int read() throws IOException {
        while (currentIndex == currentLength) {
            String line = readLine(in);
            if (line.equals("")) {
                return -1;
            }
            String[] parts = line.split(GWConstants.SEMICOLON, 2);
            currentLength = Integer.parseInt(parts[0], 16);
            currentSignature = parts[1];
            chunk = new byte[currentLength];
            currentIndex = 0;
            ByteStreams.readFully(in, chunk);
            // TODO: check currentSignature
            if (currentLength == 0) {
                return -1;
            }
            readLine(in);
        }
        return chunk[currentIndex++] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int i;
        for (i = 0; i < len; ++i) {
            int ch = read();
            if (ch == -1) {
                break;
            }
            b[off + i] = (byte) ch;
        }
        if (i == 0) {
            return -1;
        }
        return i;
    }

    /**
     * Read a \r\n terminated line from an InputStream.
     *
     * @return line without the newline or empty String if InputStream is empty
     */


    private String readLine(InputStream is) throws IOException {
        StringBuilder builder = new StringBuilder();
        while (true) {
            int ch = is.read();
            if (ch == GWConstants.CHAR_CARRIAGE_RETURN) {
                ch = is.read();
                if (ch == GWConstants.CHAR_NEWLINE) {
                    break;
                } else {
                    throw new IOException(GWConstants.LOG_CHUNKED_UNEXPECTED_CHAR_AFTER + ch);
                }
            } else if (ch == -1) {
                if (builder.length() > 0) {
                    throw new IOException(GWConstants.LOG_CHUNKED_UNEXPECTED_END);
                }
                break;
            }
            builder.append((char) ch);
        }
        return builder.toString();
    }
    
}