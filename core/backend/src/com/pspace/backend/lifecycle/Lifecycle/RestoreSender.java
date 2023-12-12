package com.pspace.backend.Lifecycle.Lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspace.backend.Libs.AdminClient.KsanClient;
import com.pspace.backend.Libs.Data.Constants;
import com.pspace.backend.Libs.Data.Lifecycle.RestoreEventData;
import com.pspace.backend.Libs.Data.Lifecycle.RestoreLogData;
import com.pspace.backend.Libs.Ksan.AgentConfig;
import com.pspace.backend.Libs.Ksan.Data.S3RegionData;
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
		ksanClient = new KsanClient(region.address, region.port, region.accessKey, region.secretKey);
		this.ksanConfig = AgentConfig.getInstance();
		mq = new MQSender(
				ksanConfig.mqHost,
				ksanConfig.mqPort,
				ksanConfig.mqUser,
				ksanConfig.mqPassword,
				Constants.MQ_KSAN_LOG_EXCHANGE,
				Constants.MQ_EXCHANGE_OPTION_TOPIC,
				Constants.MQ_BINDING_RESTORE_LOG);
	}

	@Override
	public MQResponse call(String routingKey, String body) {
		try {
			if (!routingKey.equals(Constants.MQ_BINDING_RESTORE_EVENT))
				return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);

			logger.debug("{} : {}", routingKey, body);

			// 문자열을 ReplicationEventData 클래스로 변환
			var event = Mapper.readValue(body, new TypeReference<RestoreEventData>() {
			});
			logger.info(event.toString());

			// 결과값 초기화
			String Result = "";
			// 3회 시도
			for (int i = 0; i < 3; i++) {
				Result = restoreObject(event.bucketName, event.objectName, "STANDARD", event.versionId);
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

	protected String restoreObject(String bucketName, String objectName, String storageClass, String versionId) {

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
