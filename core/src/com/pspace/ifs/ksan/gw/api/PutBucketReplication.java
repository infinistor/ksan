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
package com.pspace.ifs.ksan.gw.api;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.data.DataPutBucketReplication;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.ReplicationConfiguration;
import com.pspace.ifs.ksan.gw.format.ReplicationConfiguration.Rule;
import com.pspace.ifs.ksan.gw.format.ReplicationConfiguration.Rule.Filter.And.Tag;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.objmanager.Bucket;

import org.slf4j.LoggerFactory;

public class PutBucketReplication extends S3Request {
    public PutBucketReplication(S3Parameter s3Parameter) {
        super(s3Parameter);
        logger = LoggerFactory.getLogger(PutBucketReplication.class);
    }

    @Override
    public void process() throws GWException {
        logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_START);
        String bucket = s3Parameter.getBucketName();
        initBucketInfo(bucket);

		GWUtils.checkCors(s3Parameter);

		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}

        if (!GWConstants.VERSIONING_ENABLED.equalsIgnoreCase(getBucketVersioning(bucket))) {
            throw new GWException(GWErrorCode.INVALID_REQUEST, s3Parameter);
        }
        
		DataPutBucketReplication dataPutBucketReplication = new DataPutBucketReplication(s3Parameter);
		dataPutBucketReplication.extract();

		if (!checkPolicyBucket(GWConstants.ACTION_PUT_REPLICATION_CONFIGURATION, s3Parameter, dataPutBucketReplication)) {
			checkGrantBucket(true, GWConstants.GRANT_WRITE_ACP);
		}

		String replicationXml = dataPutBucketReplication.getReplicationXml();
        checkBucketReplication(replicationXml);
        updateBucketReplication(bucket, replicationXml);

		s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
    }

    private void checkBucketReplication(String replicationXml) throws GWException {
        XmlMapper xmlMapper = new XmlMapper();
		ReplicationConfiguration rc;
		try {
			rc = xmlMapper.readValue(replicationXml, ReplicationConfiguration.class);
		} catch (JsonMappingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

        Map<String, String> id = new HashMap<String, String>(); 
		
		if(rc.rules != null) {
			logger.warn(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULES, rc.rules.toString());
			for(Rule rl : rc.rules) {
				logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_ID, rl.id);
				logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_PREFIX, rl.prefix);
				logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_PRIORITY, rl.priority);

				if(rl.id != null) { 
					if( rl.id.length() > 255 ) {
						throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
					}
					id.put(rl.id, rl.id);
				} else {
					byte[] array = new byte[7]; // length is bounded by 7
					new Random().nextBytes(array);
					String generatedString = new String(array, Charset.forName(Constants.CHARSET_UTF_8));
					id.put(generatedString, generatedString);
				}
				
				if (rl.existingObjectReplication != null) {
					logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_EXISTING_OBJECT_REPLICATION, rl.existingObjectReplication);
					logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_EXISTING_OBJECT_REPLICATION_STATUS, rl.existingObjectReplication.status);
				}
				
				if (rl.filter != null) {
					logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_FILTER, rl.filter);
					logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_FILTER_AND, rl.filter.and);
					if (rl.filter.and != null) {
						logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_FILTER_AND_PREFIX, rl.filter.and.prefix);
						logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_FILTER_AND_TAG, rl.filter.and.tag);
						if (rl.filter.and.tag != null) {
							for (Tag r : rl.filter.and.tag) {
								logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_FILTER_AND_TAG_KEY, r.key);
								logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_FILTER_AND_TAG_VALUE, r.value);
							}
						}
					}
				}
				
				if (rl.sourceSelectionCriteria != null) {
					logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_SOURCE_SELECTION_CRITERIA, rl.sourceSelectionCriteria);
					logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_SOURCE_SELECTION_CRITERIA_REPLICA_MODIFICATIONS, rl.sourceSelectionCriteria.replicaModifications);
					logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_SOURCE_SELECTION_CRITERIA_REPLICA_MODIFICATIONS_STATUS, rl.sourceSelectionCriteria.replicaModifications.status);
					if (rl.sourceSelectionCriteria.sseKmsEncryptedObjects != null) {
						logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_SOURCE_SELECTION_CRITERIA_SSE_KMS_ENCRYPTED_OBJECTS, rl.sourceSelectionCriteria.sseKmsEncryptedObjects);
						logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_SOURCE_SELECTION_CRITERIA_SSE_KMS_ENCRYPTED_OBJECTS_STATUS, rl.sourceSelectionCriteria.sseKmsEncryptedObjects.status);
					}
				}

				if (rl.deleteMarkerReplication != null && rl.deleteMarkerReplication.Status != null) {
					logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_DELETE_MARKER_REPLICATION, rl.deleteMarkerReplication);
					logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_DELETE_MARKER_REPLICATION_STATUS, rl.deleteMarkerReplication.Status);
					if (rl.deleteMarkerReplication.Status.compareTo(GWConstants.STATUS_ENABLED) != 0 && rl.deleteMarkerReplication.Status.compareTo(GWConstants.STATUS_DISABLED) != 0) {
						logger.error(GWErrorCode.MALFORMED_X_M_L.getMessage());
						throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
					}
				}

				if (rl.destination == null) {
					throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
				} else {
					if (rl.destination.bucket != null) {
						logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_BUCKET, rl.destination.bucket);						
						String[] arnPath = rl.destination.bucket.split(GWConstants.COLON, -1);

						if (arnPath.length != 6) {
							logger.error(GWErrorCode.INVALID_REQUEST.getMessage());
							throw new GWException(GWErrorCode.INVALID_REQUEST, s3Parameter);
						}

                        if (Strings.isNullOrEmpty(arnPath[5])) {
                            logger.error(GWErrorCode.INVALID_REQUEST.getMessage());
                            throw new GWException(GWErrorCode.INVALID_REQUEST, s3Parameter);
                        }

						if (Strings.isNullOrEmpty(arnPath[3])) {
                            if (!isExistBucket(arnPath[5])) {
                                throw new GWException(GWErrorCode.INVALID_REQUEST, s3Parameter);
                            }

                            Bucket destBucket = getSomeBucket(arnPath[5]);
                            if (destBucket.getVersioning() != null) {
                                if (!GWConstants.STATUS_ENABLED.equalsIgnoreCase(destBucket.getVersioning())) {
                                    throw new GWException(GWErrorCode.INVALID_REQUEST, s3Parameter);
                                }
                            } else {
                                throw new GWException(GWErrorCode.INVALID_REQUEST, s3Parameter);
                            }
						}
					} else {
						logger.error(GWErrorCode.INVALID_REQUEST.getMessage());
						throw new GWException(GWErrorCode.INVALID_REQUEST, s3Parameter);
					}

					if (rl.destination.accessControlTranslation != null) {
						logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_ACCESS_CONTROL_TRANSLATION, rl.destination.accessControlTranslation);
						if (rl.destination.accessControlTranslation.owner != null) {
							logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_ACCESS_CONTROL_TRANSLATION_OWNER, rl.destination.accessControlTranslation.owner);
						}
						
						if (rl.destination.encryptionConfiguration != null) {
							logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_ENCRYPTION_CONFIGURATION, rl.destination.encryptionConfiguration);
							logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_ENCRYPTION_CONFIGURATION_REPLICAT_KMS_KEY_ID, rl.destination.encryptionConfiguration.replicaKmsKeyID);
						}

						if (rl.destination.metrics != null) {
							logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_METRICS, rl.destination.metrics);
							logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_METRICS_EVENT_THRESHOLD, rl.destination.metrics.eventThreshold);
							logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_METRICS_EVENT_THRESHOLD_MINUTES, rl.destination.metrics.eventThreshold.minutes);
							logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_METRICS_STATUS, rl.destination.metrics.status);
						}
						
						if (rl.destination.replicationTime != null) {
							logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_REPLICATION_TIME, rl.destination.replicationTime);
							logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_REPLICATION_TIME_STATUS, rl.destination.replicationTime.status);
							logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_REPLICATION_TIME_TIME, rl.destination.replicationTime.time);
						}
					}

					if (rl.destination.storageClass != null) {
						logger.info(GWConstants.LOG_PUT_BUCKET_REPLICATION_RULE_DESTINATION_STORAGE_CLASS, rl.destination.storageClass);
						logger.error(GWErrorCode.NOT_IMPLEMENTED.getMessage());
						throw new GWException(GWErrorCode.NOT_IMPLEMENTED, s3Parameter);
					}
				}

				if (rl.existingObjectReplication != null && rl.existingObjectReplication.status != null) {
					if (rl.existingObjectReplication.status.compareTo(GWConstants.STATUS_ENABLED) != 0 && rl.existingObjectReplication.status.compareTo(GWConstants.STATUS_DISABLED) != 0) {
						logger.error(GWErrorCode.MALFORMED_X_M_L.getMessage());
						throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
					}
				}

				if (rl.existingObjectReplication != null && rl.existingObjectReplication.status != null) {
					if (rl.existingObjectReplication.status.compareTo(GWConstants.STATUS_ENABLED) != 0 && rl.existingObjectReplication.status.compareTo(GWConstants.STATUS_DISABLED) != 0) {
						logger.error(GWErrorCode.MALFORMED_X_M_L.getMessage());
						throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
					}
				}

				if (rl.sourceSelectionCriteria != null) {
					logger.error(GWErrorCode.NOT_IMPLEMENTED.getMessage());
					throw new GWException(GWErrorCode.NOT_IMPLEMENTED, s3Parameter);
				}

				if (rl.status != null && rl.status.compareTo(GWConstants.STATUS_ENABLED) != 0 && rl.status.compareTo(GWConstants.STATUS_DISABLED) != 0) {
					logger.error(GWErrorCode.MALFORMED_X_M_L.getMessage());
					throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
				}
			}

			if( rc.rules.size() > id.size() ) {
				logger.error(GWErrorCode.INVALID_ARGUMENT.getMessage());
				throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
			}
		}
    }
}
