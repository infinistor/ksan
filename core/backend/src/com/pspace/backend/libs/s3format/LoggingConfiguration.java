/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License. See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.backend.libs.s3format;

import java.util.Collection;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** Represent an Amazon LoggingConfiguration. */
// CHECKSTYLE:OFF
public final class LoggingConfiguration {
	@JacksonXmlElementWrapper(useWrapping = false)
	@JacksonXmlProperty(localName = "LoggingEnabled")
	public LoggingEnabled loggingEnabled;

	public static final class LoggingEnabled {
		@JacksonXmlProperty(localName = "TargetBucket")
		public String targetBucket;

		@JacksonXmlProperty(localName = "TargetPrefix")
		public String targetPrefix;

		@JacksonXmlProperty(localName = "TargetGrants")
		public TargetGrants targetGrants;

		public static final class TargetGrants {
			@JacksonXmlProperty(localName = "Grant")
			public Collection<Grant> grants;

			public static final class Grant {
				@JacksonXmlProperty(localName = "Grantee")
				public Grantee grantee;
				@JacksonXmlProperty(localName = "Permission")
				public String permission;

				public static final class Grantee {
					@JacksonXmlProperty(namespace = "http://www.w3.org/2001/XMLSchema-instance", localName = "type", isAttribute = true)
					public String type;
					@JacksonXmlProperty(localName = "ID")
					public String id;
					@JacksonXmlProperty(localName = "DisplayName")
					public String displayName;
					@JacksonXmlProperty(localName = "EmailAddress")
					public String emailAddress;
					@JacksonXmlProperty(localName = "URI")
					public String uri;
				}
			}
		}
	}
}
// CHECKSTYLE:ON
