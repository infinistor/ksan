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
package com.pspace.ifs.ksan.gw.object;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.object.S3Range.Range;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

public class ResultRange {
    private List<String> contentLengthHeaders;
    private S3Range s3Range;
    private int status;
    private long contentsLength;
    private long streamSize;

    public ResultRange(String range, S3Metadata s3Metadata, S3Parameter s3Parameter) throws GWException {
        contentLengthHeaders = new ArrayList<String>();
        contentsLength = s3Metadata.getContentLength();
        streamSize = contentsLength;
        if (Strings.isNullOrEmpty(range)) {
            status = HttpServletResponse.SC_OK;
        } else {
            status = HttpServletResponse.SC_PARTIAL_CONTENT;
            s3Range = new S3Range(s3Parameter);
            s3Range.parseRange(range, s3Metadata.getSize(), false);
            checkRange();
        }
    }

    public void checkRange() {
        streamSize = 0;
        for (Range range : s3Range.getListRange()) {
            streamSize += range.getLength();
            contentLengthHeaders.add(String.format(GWConstants.RANGE_CHECK_FORMET, range.getOffset(), range.getOffset() + range.getLength() - 1, contentsLength));
        }
    }
    public List<String> getContentLengthHeaders() {
        return contentLengthHeaders;
    }

    public void setContentLengthHeaders(List<String> contentLengthHeaders) {
        this.contentLengthHeaders = contentLengthHeaders;
    }

    public S3Range getS3Range() {
        return s3Range;
    }

    public void setS3Range(S3Range s3Range) {
        this.s3Range = s3Range;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getContentsLength() {
        return contentsLength;
    }

    public void setContentsLength(long contentsLength) {
        this.contentsLength = contentsLength;
    }

    public long getStreamSize() {
        return streamSize;
    }

    public void setStreamSize(long streamSize) {
        this.streamSize = streamSize;
    }

    
}
