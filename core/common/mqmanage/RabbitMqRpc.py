#!/usr/bin/env python3
"""
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
"""


import uuid
import sys, os
import pika
import json
sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
if os.path.dirname(os.path.abspath(os.path.dirname(__file__))) not in sys.path:
    sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from Enums import EnumResponseResult
from const.mq import *
from mqmanage import RabbitMqConfiguration


class RabbitMqRpc:
	""" 생성자
	Args:
		config: Rabbit MQ 설정 객체
	Returns:
		없음
	"""
	def __init__(self, config: RabbitMqConfiguration):
		self.m_connection = None
		self.m_channel = None
		self.m_config = config
		self.m_response = None
		self.m_correlation_id = str(uuid.uuid4())

		# Rabbit MQ 리스너 초기화
		self.initializeRabbitMqListener(self.m_config)
		self.BasicProperties = pika.BasicProperties(
			reply_to=self.m_callback_queue,
			correlation_id=self.m_correlation_id,
		)

		mq_args = {}
		mq_args["x-queue-type"] = "quorum"
		mq_args["x-single-active-consumer"] = True

		result = self.m_channel.queue_declare(queue="ksanAgent" + self.m_correlation_id,durable=True, exclusive=True, arguments=mq_args)
		self.m_callback_queue = result.method.queue

		self.m_channel.basic_consume(
			queue=self.m_callback_queue,
			on_message_callback=self.on_response,
			auto_ack=True)

	""" 소멸자
	Args:
		없음
	Returns:
		없음
	"""

	def __del__(self):
		# 채널 연결이 되어 있는 경우, 채널 연결 해제
		if self.m_channel is not None:
			self.m_channel.close()
			self.m_channel = None

		# 연결이 되어 있는 경우, 연결 해제
		if self.m_connection is not None:
			self.m_connection.close()
			self.m_connection = None

	""" Rabbit MQ 리스너 초기화
	Args:
		config: Rabbit MQ 설정 객체
	Returns:
		없음
	"""

	def initializeRabbitMqListener(self, config: RabbitMqConfiguration):

		# 계정 정보 생성
		credentials = pika.PlainCredentials(username=config.user, password=config.password)

		# 연결 파라미터 생성
		connectionParams = pika.ConnectionParameters(
			host=config.host
			, port=config.port
			, virtual_host=config.virtualHost
			, credentials=credentials
		)

		# 연결 객체 생성
		self.m_connection = pika.BlockingConnection(connectionParams)
		# 채널  생성
		self.m_channel = self.m_connection.channel()

	""" 메세지 수신 시 호출되는 콜백
	"""
	def on_response(self, ch, method, props, body):
		if self.m_correlation_id == props.correlation_id:
			print(" [x] routing_key: %r, body: %r" % (method.routing_key, body))
			self.m_response = body

	""" 객체를 Rabbit MQ로 전송한다.
	Args:
		exchange: Exchange 명
		routingKey: 라우팅 키
		sendingObject: 전송할 객체
	Returns:
		전송 결과 응답 객체
	"""
	def send(self, exchange, routingKey, sendingObject):

		# 전송할 객체가 존재하지 않는 경우, 에러 반환
		if sendingObject is None:
			return ResponseData(EnumResponseResult.EnumResponseResult.Error, "MQ001", "Invalid Request")

		# 전송할 객체를 json으로 변환
		message = json.dumps(sendingObject)

		self.m_response = None
		self.m_correlation_id = str(uuid.uuid4())

		# 메시지 전송
		self.m_channel.basic_publish(
			exchange=exchange
			, routing_key=routingKey
			, properties=pika.BasicProperties(
				reply_to=self.m_callback_queue,
				correlation_id=self.m_correlation_id,
			)
			, body=message)

		while self.m_response is None:
			self.m_connection.process_data_events()

		print("[Rabbit MQ] Data transfer was successful. (exchange: %r, routingKey: %r, message: %r)" % (
			exchange
			, routingKey
			, message))
		print(self.m_response)
		return ResponseDataWithData(EnumResponseResult.EnumResponseResult.Success, "", "", self.m_response)


