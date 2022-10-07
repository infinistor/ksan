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
package com.pspace.backend.libs.s3format;

import java.util.Collection;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** Represent an Amazon Tagging for a bucket or object. */
// CHECKSTYLE:OFF
public final class Tagging {
	@JacksonXmlProperty(localName = "TagSet")
	public TagSet tagset;

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(TagSet.class.getSimpleName()).append('{');
		sb.append("tagset=").append(tagset);
		return sb.append('}').toString();
	}

	public static final class TagSet {
		@JacksonXmlElementWrapper(useWrapping = false)
		@JacksonXmlProperty(localName = "Tag")
		public Collection<Tag> tags;

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(Tag.class.getSimpleName()).append('{');
			sb.append("tags=").append(tags);
			return sb.append('}').toString();
		}

		public static final class Tag {
			@Override
			public String toString() {
				StringBuilder sb = new StringBuilder();
				sb.append(Tag.class.getSimpleName()).append('{');
				sb.append("Key=").append(key);
				sb.append(",Vaule=").append(value);
				return sb.append('}').toString();
			}

			@JacksonXmlProperty(localName = "Key")
			public String key;

			@JacksonXmlProperty(localName = "Value")
			public String value;
		}
	}
}
// CHECKSTYLE:ON
