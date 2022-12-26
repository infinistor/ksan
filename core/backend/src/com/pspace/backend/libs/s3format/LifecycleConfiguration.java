/*
* Copyright 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE.md for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.backend.libs.s3format;

import java.util.Collection;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** Represent an Amazon LifecycleConfiguration. */
// CHECKSTYLE:OFF
public final class LifecycleConfiguration {
	@JacksonXmlElementWrapper(useWrapping = false)
	@JacksonXmlProperty(localName = "Rule")
	public Collection<Rule> rules;

	public static final class Rule {
		@JacksonXmlProperty(localName = "ID")
		public String id;

		@JacksonXmlProperty(localName = "Status")
		public String status;

		@JacksonXmlProperty(localName = "Filter")
		public Filter filter;

		public static final class Filter {
			@JacksonXmlProperty(localName = "And")
			public And and;

			public static final class And {
				@JacksonXmlProperty(localName = "Prefix")
				public String prefix;

				@JacksonXmlProperty(localName = "Tag")
				@JacksonXmlElementWrapper(useWrapping = false)
				public Collection<Tag> tag;

				public static final class Tag {
					@JacksonXmlProperty(localName = "Key")
					public String key;

					@JacksonXmlProperty(localName = "Value")
					public String value;
				}
			}

			@JacksonXmlProperty(localName = "ObjectSizeGreaterThan")
			public String objectSizeGreaterThan;

			@JacksonXmlProperty(localName = "ObjectSizeLessThan")
			public String objectSizeLessThan;

			@JacksonXmlProperty(localName = "Prefix")
			public String prefix;

			@JacksonXmlProperty(localName = "Tag")
			public Tag tag;

			public static final class Tag {
				@JacksonXmlProperty(localName = "Key")
				public String key;

				@JacksonXmlProperty(localName = "Value")
				public String value;
			}
		}

		@JacksonXmlProperty(localName = "Transition")
		public Transition transition;

		public static final class Transition {
			@JacksonXmlProperty(localName = "Days")
			public String days;

			@JacksonXmlProperty(localName = "Date")
			public String date;

			@JacksonXmlProperty(localName = "StorageClass")
			public String StorageClass;
		}

		@JacksonXmlProperty(localName = "Expiration")
		public Expiration expiration;

		public static final class Expiration {
			@JacksonXmlProperty(localName = "Days")
			public String days;

			@JacksonXmlProperty(localName = "Date")
			public String date;

			@JacksonXmlProperty(localName = "ExpiredObjectDeleteMarker")
			public String ExpiredObjectDeleteMarker;
		}

		@JacksonXmlProperty(localName = "NoncurrentVersionExpiration")
		public NoncurrentVersionExpiration versionexpiration;

		public static final class NoncurrentVersionExpiration {
			@JacksonXmlProperty(localName = "NoncurrentDays")
			public String NoncurrentDays;
		}

		@JacksonXmlProperty(localName = "NoncurrentVersionTransition")
		public NoncurrentVersionTransition versiontransition;

		public static final class NoncurrentVersionTransition {
			@JacksonXmlProperty(localName = "NoncurrentDays")
			public String NoncurrentDays;

			@JacksonXmlProperty(localName = "StorageClass")
			public String StorageClass;
		}

		@JacksonXmlProperty(localName = "AbortIncompleteMultipartUpload")
		public AbortIncompleteMultipartUpload abortincompletemultipartupload;

		public static final class AbortIncompleteMultipartUpload {
			@JacksonXmlProperty(localName = "DaysAfterInitiation")
			public String DaysAfterInitiation;

			@JacksonXmlProperty(localName = "StorageClass")
			public String StorageClass;
		}

		@JacksonXmlProperty(localName = "Prefix")
		public String prefix;
	}
}
// CHECKSTYLE:ON
