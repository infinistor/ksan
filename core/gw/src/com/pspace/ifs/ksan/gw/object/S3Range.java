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

import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3Range {
	private Logger logger;
	private List<Range> rangeList;
	private S3Parameter s3Parameter;
	
	public S3Range(S3Parameter s3Parameter) {
		rangeList = new ArrayList<Range>();
		logger = LoggerFactory.getLogger(Range.class);
		this.s3Parameter = s3Parameter;
	}

	public void parseRange(String range, long fileLength, boolean isCopy) throws GWException {
		if (Strings.isNullOrEmpty(range)) {
			logger.error(GWConstants.LOG_S3RANGE_EMPTY, range);
			return;
		}
		
		if (!range.startsWith(GWConstants.XML_BYTES)) {
			logger.error(GWConstants.LOG_S3RANGE_INVALID, range);
			throw new GWException(GWErrorCode.INVALID_ARGUMENT , s3Parameter);
		}
		
		rangeList.clear();
		
		range = range.substring(GWConstants.XML_BYTES.length());
		range = range.trim();
		String[] ranges = range.split(GWConstants.COMMA);

		for (String rangeValue : ranges) {
			if (!rangeValue.contains(GWConstants.DASH)) {
				logger.error(GWConstants.LOG_S3RANGE_INVALID, rangeValue);
				throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
			}
			rangeValue = rangeValue.trim();
			String[] rangeSubs = rangeValue.split(GWConstants.DASH, 2);

			if (rangeSubs.length >= 1) {
				long startPosition = 0L;
				long endPosition = 0L;
				boolean hasStart = false;
				boolean hasEnd = false;

				if (!Strings.isNullOrEmpty(rangeSubs[0])) {
					startPosition = Long.parseLong(rangeSubs[0]);
					hasStart = true;
				}
				
				if (!Strings.isNullOrEmpty(rangeSubs[1])) {
					endPosition = Long.parseLong(rangeSubs[1]);
					hasEnd = true;
				}

				// check first position, out of file bound
				if (hasStart && (startPosition >= fileLength || startPosition <  0)) {
					logger.error(GWConstants.LOG_S3RANGE_NOT_SATISFIABLE, rangeValue);
					throw new GWException(GWErrorCode.INVALID_RANGE, s3Parameter);
				}

				// check last position
				if (hasEnd) {
					if (endPosition >= fileLength) {
						if (isCopy) {
							logger.error(GWConstants.LOG_S3RANGE_ENDPOSITION_GREATER_THAN);
							throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
						}
						endPosition = fileLength - 1;
					}
				} else {
					endPosition = fileLength;
				}

				if (startPosition > endPosition) {
					logger.error(GWConstants.LOG_S3RANGE_NOT_SATISFIABLE, rangeValue);
						throw new GWException(GWErrorCode.INVALID_RANGE, s3Parameter);
				}

				logger.debug(GWConstants.LOG_S3RANGE_INFO, startPosition, endPosition, endPosition - startPosition + 1, fileLength);
				Range rangeObject = new Range(startPosition, endPosition - startPosition + 1);	// offset, length
				rangeList.add(rangeObject);
			} else {
				logger.error(GWConstants.LOG_S3RANGE_NOT_SATISFIABLE, rangeValue);
				throw new GWException(GWErrorCode.INVALID_RANGE, s3Parameter);
			}
		}
	}

	public List<Range> getListRange() {
		return rangeList;
	}

	public class Range {
		private long offset;
		private long length;
		
		public Range(long offset, long length) {
			this.offset = offset;
			this.length = length;
		}
		
		public long getOffset() {
			return offset;
		}
		
		public long getLength() {
			return length;
		}
	}
}
