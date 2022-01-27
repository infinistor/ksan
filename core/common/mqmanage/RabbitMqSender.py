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

import os, sys
sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
import pika
import json
from ksan.Enums.EnumResponseResult import EnumResponseResult
from ksan.common.ResponseData import ResponseData
from ksan.mqmanage.RabbitMqConfiguration import RabbitMqConfiguration


class RabbitMqSender:
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

		# Rabbit MQ 리스너 초기화
		self.initializeRabbitMqListener(self.m_config)

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
			return ResponseData(EnumResponseResult.Error, "MQ001", "Invalid Request")

		# 전송할 데이터로 변환
		message = json.dumps(sendingObject)

		# 메시지 전송
		self.m_channel.basic_publish(
			exchange=exchange
			, routing_key=routingKey
			, body=message)

		print("[Rabbit MQ] Data transfer was successful. (exchange: %r, routingKey: %r, message: %r)" % (exchange, routingKey, message))

		return ResponseData(EnumResponseResult.Success, "", "")

