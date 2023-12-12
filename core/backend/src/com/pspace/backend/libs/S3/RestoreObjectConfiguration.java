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

public class RestoreObjectConfiguration {
	@JacksonXmlProperty(localName = "Days")
	public int days;

	@JacksonXmlProperty(localName = "GlacierJobParameters")
	@JacksonXmlElementWrapper(useWrapping = false)
	public GlacierJobParameter glacierJobParameters;

	public static final class GlacierJobParameter {
		@JacksonXmlProperty(localName = "Tier")
		public String tier;
	}

	@JacksonXmlProperty(localName = "Type")
	public String type;

	@JacksonXmlProperty(localName = "tier")
	public String tier;

	@JacksonXmlProperty(localName = "Description")
	public String description;

	@JacksonXmlProperty(localName = "SelectParameters")
	@JacksonXmlElementWrapper(useWrapping = false)
	public SelectParameter SelectParameters;

	public static final class SelectParameter {
		@JacksonXmlProperty(localName = "Expression")
		public String expression;
		@JacksonXmlProperty(localName = "ExpressionType")
		public String expressionType;

		@JacksonXmlProperty(localName = "InputSerialization")
		public InputSerialization inputSerialization;

		public static final class InputSerialization {
			@JacksonXmlProperty(localName = "CompressionType")
			public String compressionType;

			@JacksonXmlProperty(localName = "CSV")
			public CSV csv;

			public static final class CSV {
				@JacksonXmlProperty(localName = "AllowQuotedRecordDelimiter")
				public boolean allowQuotedRecordDelimiter;
				@JacksonXmlProperty(localName = "Comments")
				public String comments;
				@JacksonXmlProperty(localName = "FieldDelimiter")
				public String fieldDelimiter;
				@JacksonXmlProperty(localName = "FileHeaderInfo")
				public String fileHeaderInfo;
				@JacksonXmlProperty(localName = "QuoteCharacter")
				public String quoteCharacter;
				@JacksonXmlProperty(localName = "QuoteEscapeCharacter")
				public String quoteEscapeCharacter;
				@JacksonXmlProperty(localName = "RecordDelimiter")
				public String recordDelimiter;
			}

			@JacksonXmlProperty(localName = "JSON")
			public JSON json;

			public static final class JSON {
				@JacksonXmlProperty(localName = "Type")
				public String type;
			}
		}

		@JacksonXmlProperty(localName = "OutputSerialization")
		public OutputSerialization outputSerialization;

		public static final class OutputSerialization {
			@JacksonXmlProperty(localName = "CompressionType")
			public String compressionType;

			@JacksonXmlProperty(localName = "CSV")
			public CSV csv;

			public static final class CSV {
				@JacksonXmlProperty(localName = "FieldDelimiter")
				public String fieldDelimiter;
				@JacksonXmlProperty(localName = "QuoteCharacter")
				public String quoteCharacter;
				@JacksonXmlProperty(localName = "QuoteEscapeCharacter")
				public String quoteEscapeCharacter;
				@JacksonXmlProperty(localName = "QuoteFields")
				public String quoteFields;
				@JacksonXmlProperty(localName = "RecordDelimiter")
				public String recordDelimiter;
			}

			@JacksonXmlProperty(localName = "JSON")
			public JSON json;

			public static final class JSON {
				@JacksonXmlProperty(localName = "RecordDelimiter")
				public String recordDelimiter;
			}
		}
	}

	@JacksonXmlProperty(localName = "OutputLocation")
	@JacksonXmlElementWrapper(useWrapping = false)
	public OutputLocation outputLocation;

	public static final class OutputLocation {

		@JacksonXmlProperty(localName = "S3")
		@JacksonXmlElementWrapper(useWrapping = false)
		public S3 s3;

		public static final class S3 {
			@JacksonXmlProperty(localName = "AccessControlList")
			@JacksonXmlElementWrapper(useWrapping = false)
			public AccessControlList accessControlList;

			public static final class AccessControlList {
				@JacksonXmlProperty(localName = "Grantee")
				public Grantee grantee;

				public static final class Grantee {
					@JacksonXmlProperty(localName = "DisplayName")
					public String displayName;
					@JacksonXmlProperty(localName = "EmailAddress")
					public String emailAddress;
					@JacksonXmlProperty(localName = "ID")
					public String id;
					@JacksonXmlProperty(localName = "xsi:type")
					public String type;
					@JacksonXmlProperty(localName = "URI")
					public String uri;
				}

				@JacksonXmlProperty(localName = "Permission")
				public String permission;
			}

			@JacksonXmlProperty(localName = "BucketName")
			public String bucketName;
			@JacksonXmlProperty(localName = "CannedACL")
			public String cannedACL;
			@JacksonXmlProperty(localName = "Encryption")
			public Encryption encryption;

			public static final class Encryption {
				@JacksonXmlProperty(localName = "EncryptionType")
				public String encryptionType;
				@JacksonXmlProperty(localName = "KMSContext")
				public String kMSContext;
				@JacksonXmlProperty(localName = "KMSKeyId")
				public String kMSKeyId;
			}

			@JacksonXmlProperty(localName = "Prefix")
			public String prefix;

			@JacksonXmlProperty(localName = "StorageClass")
			public String storageClass;

			@JacksonXmlProperty(localName = "Tagging")
			@JacksonXmlElementWrapper(useWrapping = false)
			public Tagging tagging;

			public static final class Tagging {
				@JacksonXmlProperty(localName = "TagSet")
				@JacksonXmlElementWrapper(useWrapping = false)
				public TagSet tagSet;

				public static final class TagSet {
					@JacksonXmlProperty(localName = "Tag")
					@JacksonXmlElementWrapper(useWrapping = false)
					public Collection<Tag> tags;

					public static final class Tag {
						@JacksonXmlProperty(localName = "Key")
						public String key;
						@JacksonXmlProperty(localName = "Value")
						public String value;
					}
				}
			}

			@JacksonXmlProperty(localName = "UserMetadata")
			@JacksonXmlElementWrapper(useWrapping = false)
			public UserMetadata userMetadata;

			public static final class UserMetadata {
				@JacksonXmlProperty(localName = "MetadataEntry")
				@JacksonXmlElementWrapper(useWrapping = false)
				public MetadataEntry metadataEntry;

				public static final class MetadataEntry {
					@JacksonXmlProperty(localName = "Name")
					public String name;
					@JacksonXmlProperty(localName = "Value")
					public String value;
				}
			}
		}
	}
}
