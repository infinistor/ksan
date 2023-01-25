package com.pspace.backend.lifecycle.Lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspace.backend.libs.AdminClient.KsanClient;
import com.pspace.backend.libs.Data.Constants;
import com.pspace.backend.libs.Data.Lifecycle.RestoreEventData;
import com.pspace.backend.libs.Data.Lifecycle.RestoreLogData;
import com.pspace.backend.libs.Ksan.AgentConfig;
import com.pspace.backend.libs.Ksan.Data.S3RegionData;
import com.pspace.ifs.ksan.libs.mq.MQCallback;
import com.pspace.ifs.ksan.libs.mq.MQResponse;
import com.pspace.ifs.ksan.libs.mq.MQResponseCode;
import com.pspace.ifs.ksan.libs.mq.MQResponseType;
import com.pspace.ifs.ksan.libs.mq.MQSender;

public class RestoreSender implements MQCallback {
	private final Logger logger = LoggerFactory.getLogger(LifecycleSender.class);
	private final KsanClient ksanClient;
	private final AgentConfig ksanConfig;
	private final ObjectMapper Mapper = new ObjectMapper();
	private final MQSender mq;

	public RestoreSender(S3RegionData region) throws Exception {
		ksanClient = new KsanClient(region.Address, region.Port, region.AccessKey, region.SecretKey);
		this.ksanConfig = AgentConfig.getInstance();
		mq = new MQSender(
				ksanConfig.MQHost,
				ksanConfig.MQPort,
				ksanConfig.MQUser,
				ksanConfig.MQPassword,
				Constants.MQ_KSAN_LOG_EXCHANGE,
				Constants.MQ_EXCHANGE_OPTION_TOPIC,
				Constants.MQ_BINDING_RESTORE_LOG);
	}

	@Override
	public MQResponse call(String routingKey, String body) {
		try {
			logger.debug("{} -> {}", routingKey, body);

			if (!routingKey.equals(Constants.MQ_BINDING_RESTORE_EVENT))
				return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);

			// 문자열을 ReplicationEventData 클래스로 변환
			var event = Mapper.readValue(body, new TypeReference<RestoreEventData>() {
			});
			// 결과값 초기화
			String Result = "";
			// 3회 시도
			for (int i = 0; i < 3; i++) {
				Result = RestoreObject(event.BucketName, event.ObjectName, "STANDARD", event.VersionId);
				// 성공했을 경우 종료
				if (Result.equals(""))
					break;
			}
			// 에러가 발생할 경우
			if (!Result.isBlank()) {
				// 이벤트 저장
				try {
					var item = new RestoreLogData(event, Result);
					mq.send(item.toString(), Constants.MQ_BINDING_LIFECYCLE_LOG);

				} catch (Exception e) {
					logger.error("", e);
				}
			}
			// 성공할 경우
			else {
				mq.send(event.toString(), Constants.MQ_BINDING_LIFECYCLE_LOG);
			}

			return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
		} catch (Exception e) {
			logger.error("", e);
			return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_UNKNOWN_ERROR, e.getMessage(), 0);
		}
	}

	protected String RestoreObject(String bucketName, String objectName, String storageClass, String versionId) {

		var Result = "";
		try {
			ksanClient.StorageMove(bucketName, objectName, storageClass, versionId);
		} catch (Exception e) {
			logger.error("", e);
			Result = e.getMessage();
		}
		return Result;
	}

}
