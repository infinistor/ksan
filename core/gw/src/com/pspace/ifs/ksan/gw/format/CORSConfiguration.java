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
package com.pspace.ifs.ksan.gw.format;

import java.util.Collection;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

/** Represent an Amazon Versioning for a container or object. */
// CHECKSTYLE:OFF
public final class CORSConfiguration {
    
	@JacksonXmlElementWrapper(useWrapping=false)
	@JacksonXmlProperty(localName = GWConstants.CORS_RULE)
	public 
	Collection<CORSRule> CORSRules;
	          
	public static final class CORSRule {
		@JacksonXmlProperty(localName = GWConstants.XML_ID)
		public String id;
		
		@JacksonXmlElementWrapper(useWrapping=false)
		@JacksonXmlProperty(localName = GWConstants.XML_ALLOWED_HEADER)
		public Collection<String> AllowedHeaders;
		
		@JacksonXmlElementWrapper(useWrapping=false)
		@JacksonXmlProperty(localName = GWConstants.XML_ALLOWED_METHOD)
		public Collection<String> AllowedMethods;
		
		@JacksonXmlElementWrapper(useWrapping=false)
		@JacksonXmlProperty(localName = GWConstants.XML_ALLOWED_ORIGIN)
		public Collection<String> AllowedOrigins;
		
		@JacksonXmlElementWrapper(useWrapping=false)
		@JacksonXmlProperty(localName = GWConstants.XML_EXPOSED_HEADER)
		public Collection<String> ExposeHeaders;
		
		@JacksonXmlElementWrapper(useWrapping=false)
		@JacksonXmlProperty(localName = GWConstants.ACCESS_CONTROL_REQUEST_HEADERS)
		public Collection<String> RequestHeaders;
		
		@JacksonXmlElementWrapper(useWrapping=false)
		@JacksonXmlProperty(localName = GWConstants.ACCESS_CONTROL_REQUEST_METHOD)
		public Collection<String> RequesMethod;
		
		@JacksonXmlProperty(localName = GWConstants.MAX_AGE_SECONDS)
		public String MaxAgeSeconds;
		
		@JacksonXmlProperty(localName = GWConstants.ORIGIN)
		public String origin;
		
		@JacksonXmlProperty(localName = GWConstants.ACCESS_CONTROL_ALLOW_CREDENTIALS)
		public String allowcredentials;
	}
}
// CHECKSTYLE:ON
