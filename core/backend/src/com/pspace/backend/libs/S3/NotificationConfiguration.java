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
package com.pspace.backend.Libs.S3;

import java.util.Collection;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** Represent an Amazon NotificationConfiguration. */
// CHECKSTYLE:OFF
public final class NotificationConfiguration {
	@JacksonXmlElementWrapper(useWrapping = false)
	@JacksonXmlProperty(localName = "TopicConfiguration")
	public Collection<TopicConfiguration> topics;

	public static final class TopicConfiguration {
		@JacksonXmlProperty(localName = "Event")
		public String event;

		@JacksonXmlProperty(localName = "Id")
		public String Id;

		@JacksonXmlProperty(localName = "Topic")
		public String topic;

		@JacksonXmlProperty(localName = "Filter")
		public Filter filter;

		public static final class Filter {
			@JacksonXmlProperty(localName = "S3Key")
			public S3Key s3key;

			public static final class S3Key {
				@JacksonXmlElementWrapper(useWrapping = false)
				@JacksonXmlProperty(localName = "FilterRule")
				public Collection<FilterRule> filterrule;

				public static final class FilterRule {
					@JacksonXmlProperty(localName = "Name")
					public String name;

					@JacksonXmlProperty(localName = "Value")
					public String value;
				}
			}
		}
	}

	@JacksonXmlElementWrapper(useWrapping = false)
	@JacksonXmlProperty(localName = "QueueConfiguration")
	public Collection<QueueConfiguration> queue;

	public static final class QueueConfiguration {
		@JacksonXmlProperty(localName = "Event")
		public String event;

		@JacksonXmlProperty(localName = "Id")
		public String Id;

		@JacksonXmlProperty(localName = "Queue")
		public String queue;

		@JacksonXmlProperty(localName = "Filter")
		public Filter filter;

		public static final class Filter {
			@JacksonXmlProperty(localName = "S3Key")
			public S3Key s3key;

			public static final class S3Key {
				@JacksonXmlElementWrapper(useWrapping = false)
				@JacksonXmlProperty(localName = "FilterRule")
				public Collection<FilterRule> filterrule;

				public static final class FilterRule {
					@JacksonXmlProperty(localName = "Name")
					public String name;

					@JacksonXmlProperty(localName = "Value")
					public String value;
				}
			}
		}
	}

	@JacksonXmlElementWrapper(useWrapping = false)
	@JacksonXmlProperty(localName = "CloudFunctionConfiguration")
	public Collection<CloudFunctionConfiguration> cloud;

	public static final class CloudFunctionConfiguration {
		@JacksonXmlProperty(localName = "Event")
		public String event;

		@JacksonXmlProperty(localName = "Id")
		public String Id;

		@JacksonXmlProperty(localName = "CloudFunction")
		public String cloudfunction;

		@JacksonXmlProperty(localName = "Filter")
		public Filter filter;

		public static final class Filter {
			@JacksonXmlProperty(localName = "S3Key")
			public S3Key s3key;

			public static final class S3Key {
				@JacksonXmlElementWrapper(useWrapping = false)
				@JacksonXmlProperty(localName = "FilterRule")
				public Collection<FilterRule> filterrule;

				public static final class FilterRule {
					@JacksonXmlProperty(localName = "Name")
					public String name;

					@JacksonXmlProperty(localName = "Value")
					public String value;
				}
			}
		}
	}
}
// CHECKSTYLE:ON
