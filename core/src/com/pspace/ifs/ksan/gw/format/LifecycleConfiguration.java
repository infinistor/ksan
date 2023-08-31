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
public final class LifecycleConfiguration {
	@JacksonXmlElementWrapper(useWrapping=false)
	@JacksonXmlProperty(localName = GWConstants.XML_RULE)
	public
	Collection<Rule> rules;
	
	public static final class Rule {
		@JacksonXmlProperty(localName = GWConstants.XML_ID)
		public
		String id;
		
		@JacksonXmlProperty(localName = GWConstants.XML_STATUS)
		public
		String status;
		
		@JacksonXmlProperty(localName = GWConstants.XML_FILTER)
		public
		Filter filter;
		
		public static final class Filter {
			@JacksonXmlProperty(localName = GWConstants.XML_AND)
            public And and;

            public static final class And{
                @JacksonXmlProperty(localName = GWConstants.XML_PREFIX)
                public String prefix;

                @JacksonXmlProperty(localName = GWConstants.XML_TAG)
                @JacksonXmlElementWrapper(useWrapping = false)
                public Collection<Tag> tag;

                public static final class Tag{
                    @JacksonXmlProperty(localName = GWConstants.XML_KEY)
                    public String key;
    
                    @JacksonXmlProperty(localName = GWConstants.XML_VALUE)
                    public String value;
                }
            }

			@JacksonXmlProperty(localName = GWConstants.XML_OBJECT_SIZE_GREATER_THAN)
			public
			String objectSizeGreaterThan;

			@JacksonXmlProperty(localName = GWConstants.XML_OBJECT_SIZE_LESS_THAN)
			public
			String objectSizeLessThan;

			@JacksonXmlProperty(localName = GWConstants.XML_PREFIX)
			public
			String prefix;

			@JacksonXmlProperty(localName = GWConstants.XML_TAG)
            public Tag tag;

            public static final class Tag{
                @JacksonXmlProperty(localName = GWConstants.XML_KEY)
                public String key;

                @JacksonXmlProperty(localName = GWConstants.XML_VALUE)
                public String value;
            }
		}
		
		@JacksonXmlProperty(localName = GWConstants.XML_TRANSITION)
		public
		Transition transition;
		
		public static final class Transition {
			@JacksonXmlProperty(localName = GWConstants.XML_DAYS)
			public
			String days;
			
			@JacksonXmlProperty(localName = GWConstants.XML_DATE)
			public
			String date;
			
			@JacksonXmlProperty(localName = GWConstants.STORAGE_CLASS)
			public
			String storageClass;
		}
		
		@JacksonXmlProperty(localName = GWConstants.XML_EXPIRATION)
		public
		Expiration expiration;
		
		public static final class Expiration {
			@JacksonXmlProperty(localName = GWConstants.XML_DAYS)
			public
			String days;
			
			@JacksonXmlProperty(localName = GWConstants.XML_DATE)
			public
			String date;
			
			@JacksonXmlProperty(localName = GWConstants.XML_EXPIRED_OBJECT_DELETE_MARKER)
			public
			String expiredObjectDeleteMarker;
		}
		
		@JacksonXmlProperty(localName = GWConstants.XML_NON_CURRENT_VERSION_EXPIRATION)
		public
		NoncurrentVersionExpiration versionexpiration;
		
		public static final class NoncurrentVersionExpiration {
			@JacksonXmlProperty(localName = GWConstants.XML_NON_CURRENT_DAYS)
			public
			String noncurrentDays;
		}
		
		@JacksonXmlProperty(localName = GWConstants.XML_NON_CURRENT_VERSION_TRANSITION)
		public
		NoncurrentVersionTransition versiontransition;
		
		public static final class NoncurrentVersionTransition {
			@JacksonXmlProperty(localName = GWConstants.XML_NON_CURRENT_DAYS)
			public
			String noncurrentDays;
			
			@JacksonXmlProperty(localName = GWConstants.STORAGE_CLASS)
			public
			String storageClass;
		}

		@JacksonXmlProperty(localName = GWConstants.XML_ABORT_INCOMPLETE_MULTIPART_UPLOAD)
		public
		AbortIncompleteMultipartUpload abortincompletemultipartupload;
		
		public static final class AbortIncompleteMultipartUpload {
			@JacksonXmlProperty(localName = GWConstants.XML_DAYS_AFTER_INITIATION)
			public
			String daysAfterInitiation;
			
			@JacksonXmlProperty(localName = GWConstants.STORAGE_CLASS)
			public
			String storageClass;
		}
		
		@JacksonXmlProperty(localName = GWConstants.XML_PREFIX)
		public
		String prefix;
	}
}
// CHECKSTYLE:ON
