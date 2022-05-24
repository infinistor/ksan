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

/** Represent an Amazon ReplicationConfiguration. */
// CHECKSTYLE:OFF
public final class ReplicationConfiguration {
    @JacksonXmlProperty(localName = GWConstants.XML_ROLE)
    public String role;

    @JacksonXmlProperty(localName = GWConstants.XML_RULE)
    @JacksonXmlElementWrapper(useWrapping=false)
    public Collection<Rule> rules;

    public static final class Rule {

        @JacksonXmlProperty(localName = GWConstants.DELETE_MARKER_REPLICATION)
        public DeleteMarkerReplication deleteMarkerReplication;

        public static final class DeleteMarkerReplication{
            @JacksonXmlProperty(localName = GWConstants.XML_STATUS)
            public String Status;
        }

        @JacksonXmlProperty(localName = GWConstants.XML_DESTINATION)
        public Destination destination;

        public static final class Destination{
            @JacksonXmlProperty(localName = GWConstants.ACCESS_CONTROL_TRANSLATION)
            public AccessControlTranslation accessControlTranslation;

            public static final class AccessControlTranslation{
                @JacksonXmlProperty(localName = GWConstants.XML_OWNER)
                public String owner;
            }
        
            @JacksonXmlProperty(localName = GWConstants.XML_ACCOUNT)
            public String account;

            @JacksonXmlProperty(localName = GWConstants.BUCKET)
            public String bucket;

            @JacksonXmlProperty(localName = GWConstants.ENCRYPTION_CONFIGURATION)
            public EncryptionConfiguration encryptionConfiguration;

            public static final class EncryptionConfiguration{
                @JacksonXmlProperty(localName = GWConstants.XML_REPLICA_KMS_KEYID)
                public String replicaKmsKeyID;
            }

            @JacksonXmlProperty(localName = GWConstants.XML_METRICS)
            public Metrics metrics;

            public static final class Metrics{
                @JacksonXmlProperty(localName = GWConstants.XML_EVENT_THRESHOLD)
                public EventThreshold eventThreshold;

                public static final class EventThreshold{
                    @JacksonXmlProperty(localName = GWConstants.XML_MINUTES)
                    public String minutes;
                }

                @JacksonXmlProperty(localName = GWConstants.XML_STATUS)
                public String status;
            }

            @JacksonXmlProperty(localName = GWConstants.XML_REPLICATION_TIME)
            public ReplicationTime replicationTime;

            public static final class ReplicationTime{
                @JacksonXmlProperty(localName = GWConstants.XML_TIME)
                public Time time;

                public static final class Time{
                    @JacksonXmlProperty(localName = GWConstants.XML_MINUTES)
                    public String minutes;
                }

                @JacksonXmlProperty(localName = GWConstants.XML_STATUS)
                public String status;
            }
        
            @JacksonXmlProperty(localName = GWConstants.STORAGE_CLASS)
            public String storageClass;
        }
    
        @JacksonXmlProperty(localName = GWConstants.XML_EXISTING_OBJECT_REPLICATION)
        public ExistingObjectReplication existingObjectReplication;

        public static final class ExistingObjectReplication{
            @JacksonXmlProperty(localName = GWConstants.XML_STATUS)
            public String status;
        }
    
        @JacksonXmlProperty(localName = GWConstants.XML_FILTER)
        public Filter filter;

        public static final class Filter{
            @JacksonXmlProperty(localName = GWConstants.XML_AND)
            public And and;

            public static final class And{
                @JacksonXmlProperty(localName = GWConstants.XML_PREFIX)
                public String prefix;

                @JacksonXmlProperty(localName = GWConstants.XML_TAG)
                @JacksonXmlElementWrapper(useWrapping = false)
                public Collection<Tag> tag;

                public static final class Tag{
                    @JacksonXmlProperty(localName = GWConstants.KEY)
                    public String key;
    
                    @JacksonXmlProperty(localName = GWConstants.VALUE)
                    public String value;
                }
            }
        
            @JacksonXmlProperty(localName = GWConstants.XML_PREFIX)
            public String prefix;

            @JacksonXmlProperty(localName = GWConstants.XML_TAG)
            public Tag tag;

            public static final class Tag{
                @JacksonXmlProperty(localName = GWConstants.KEY)
                public String key;

                @JacksonXmlProperty(localName = GWConstants.VALUE)
                public String value;
            }
        }
    
        @JacksonXmlProperty(localName = GWConstants.XML_ID)
        public String id;

        @JacksonXmlProperty(localName = GWConstants.XML_PREFIX)
        public String prefix;

        @JacksonXmlProperty(localName = "Priority")
        public String priority;

        @JacksonXmlProperty(localName = "SourceSelectionCriteria")
        public SourceSelectionCriteria sourceSelectionCriteria;

        public static final class SourceSelectionCriteria{
            @JacksonXmlProperty(localName = "ReplicaModifications")
            public ReplicaModifications replicaModifications;

            public static final class ReplicaModifications{
                @JacksonXmlProperty(localName = GWConstants.XML_STATUS)
                public String status;
            }

            @JacksonXmlProperty(localName = "SseKmsEncryptedObjects")
            public SseKmsEncryptedObjects sseKmsEncryptedObjects;

            public static final class SseKmsEncryptedObjects{
                @JacksonXmlProperty(localName = GWConstants.XML_STATUS)
                public String status;
            }
        }

        @JacksonXmlProperty(localName = GWConstants.XML_STATUS)
        public String status;
    }
}    
// CHECKSTYLE:ON
