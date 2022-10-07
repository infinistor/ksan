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

/** Represent an Amazon ReplicationConfiguration. */
// CHECKSTYLE:OFF
public final class ReplicationConfiguration {
	@JacksonXmlProperty(localName = "Role")
	public String role;

	@JacksonXmlProperty(localName = "Rule")
	@JacksonXmlElementWrapper(useWrapping = false)
	public Collection<Rule> rules;

	public static final class Rule {

		@JacksonXmlProperty(localName = "DeleteMarkerReplication")
		public DeleteMarkerReplication deleteMarkerReplication;

		public static final class DeleteMarkerReplication {
			@JacksonXmlProperty(localName = "Status")
			public String Status;
		}

		@JacksonXmlProperty(localName = "Destination")
		public Destination destination;

		public static final class Destination {
			@JacksonXmlProperty(localName = "AccessControlTranslation")
			public AccessControlTranslation accessControlTranslation;

			public static final class AccessControlTranslation {
				@JacksonXmlProperty(localName = "Owner")
				public String owner;
			}

			@JacksonXmlProperty(localName = "Account")
			public String account;

			@JacksonXmlProperty(localName = "Bucket")
			public String bucket;

			@JacksonXmlProperty(localName = "EncryptionConfiguration")
			public EncryptionConfiguration encryptionConfiguration;

			public static final class EncryptionConfiguration {
				@JacksonXmlProperty(localName = "ReplicaKmsKeyID")
				public String replicaKmsKeyID;
			}

			@JacksonXmlProperty(localName = "Metrics")
			public Metrics metrics;

			public static final class Metrics {
				@JacksonXmlProperty(localName = "EventThreshold")
				public EventThreshold eventThreshold;

				public static final class EventThreshold {
					@JacksonXmlProperty(localName = "Minutes")
					public int minutes;
				}

				@JacksonXmlProperty(localName = "Status")
				public String status;
			}

			@JacksonXmlProperty(localName = "ReplicationTime")
			public ReplicationTime replicationTime;

			public static final class ReplicationTime {
				@JacksonXmlProperty(localName = "Time")
				public Time time;

				public static final class Time {
					@JacksonXmlProperty(localName = "Minutes")
					public int minutes;
				}

				@JacksonXmlProperty(localName = "Status")
				public String status;
			}

			@JacksonXmlProperty(localName = "StorageClass")
			public String storageClass;
		}

		@JacksonXmlProperty(localName = "ExistingObjectReplication")
		public ExistingObjectReplication existingObjectReplication;

		public static final class ExistingObjectReplication {
			@JacksonXmlProperty(localName = "Status")
			public String status;
		}

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
				public Collection<com.pspace.backend.libs.s3format.ReplicationConfiguration.Rule.Filter.Tag> tag;

				public static final class Tag {
					@JacksonXmlProperty(localName = "Key")
					public String key;

					@JacksonXmlProperty(localName = "Value")
					public String value;
				}
			}

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

		@JacksonXmlProperty(localName = "ID")
		public String id;

		@JacksonXmlProperty(localName = "Prefix")
		public String prefix;

		@JacksonXmlProperty(localName = "Priority")
		public int priority;

		@JacksonXmlProperty(localName = "SourceSelectionCriteria")
		public SourceSelectionCriteria sourceSelectionCriteria;

		public static final class SourceSelectionCriteria {
			@JacksonXmlProperty(localName = "ReplicaModifications")
			public ReplicaModifications replicaModifications;

			public static final class ReplicaModifications {
				@JacksonXmlProperty(localName = "Status")
				public String status;
			}

			@JacksonXmlProperty(localName = "SseKmsEncryptedObjects")
			public SseKmsEncryptedObjects sseKmsEncryptedObjects;

			public static final class SseKmsEncryptedObjects {
				@JacksonXmlProperty(localName = "Status")
				public String status;
			}
		}

		@JacksonXmlProperty(localName = "Status")
		public String status;
	}
}
// CHECKSTYLE:ON