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

/** Represent an Amazon Object Legal Hold. */
// CHECKSTYLE:OFF
public final class BucketInventoryConfiguration {
	@JacksonXmlProperty(localName = "InventoryConfiguration")
	@JacksonXmlElementWrapper(useWrapping = false)
	public Collection<InventoryConfiguration> inventoryConfigurations;

	public static final class InventoryConfiguration {
		@JacksonXmlProperty(localName = "IsEnabled")
		public String isEnabled;

		@JacksonXmlProperty(localName = "Id")
		public String id;

		@JacksonXmlProperty(localName = "IncludedObjectVersions")
		public String includedObjectVersions;

		@JacksonXmlProperty(localName = "Filter")
		@JacksonXmlElementWrapper(useWrapping = false)
		public Filter filter;

		public static final class Filter {
			@JacksonXmlProperty(localName = "Prefix")
			public String prefix;
		}

		@JacksonXmlProperty(localName = "OptionalFields")
		@JacksonXmlElementWrapper(useWrapping = false)
		public OptionalFields optionalFields;

		public static final class OptionalFields {
			@JacksonXmlProperty(localName = "Field")
			@JacksonXmlElementWrapper(useWrapping = false)
			public Collection<String> fields;
		}

		@JacksonXmlProperty(localName = "Schedule")
		@JacksonXmlElementWrapper(useWrapping = false)
		public Schedule schedule;

		public static final class Schedule {
			@JacksonXmlProperty(localName = "Frequency")
			public String frequency;
		}

		@JacksonXmlProperty(localName = "Destination")
		@JacksonXmlElementWrapper(useWrapping = false)
		public Destination destination;

		public static final class Destination {
			@JacksonXmlProperty(localName = "S3BucketDestination")
			public S3BucketDestination s3BucketDestination;

			public static final class S3BucketDestination {
				@JacksonXmlProperty(localName = "Format")
				public String format;

				@JacksonXmlProperty(localName = "AccountId")
				public String accountId;

				@JacksonXmlProperty(localName = "Bucket")
				public String bucket;

				@JacksonXmlProperty(localName = "Prefix")
				public String prefix;

				@JacksonXmlProperty(localName = "Encryption")
				public Encryption encryption;

				public static final class Encryption {
					@JacksonXmlProperty(localName = "SSE-KMS")
					public SSEKMS ssekms;

					public static final class SSEKMS {
						@JacksonXmlProperty(localName = "KeyId")
						public String keyId;
					}
				}
			}
		}
	}
}
// CHECKSTYLE:ON
