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
using System;
using PortalData;
using PortalProviderInterface;
using PortalResources;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using MTLib.CommonData;
using MTLib.Core;
using MTLib.Reflection;
using Newtonsoft.Json;
using RabbitMQ.Client;

namespace PortalProvider.Providers.RabbitMq
{
	/// <summary>Rabbit MQ로 정보를 전송하는 클래스</summary>
	public class RabbitMqSender : IRabbitMqSender
	{
		/// <summary>Rabbit MQ 연결 객체</summary>
		private IConnection m_connection;
		/// <summary>Rabbit MQ 채널 객체</summary>
		private IModel m_channel;
		/// <summary>Rabbit MQ 설정 객체</summary>
		private readonly RabbitMqConfiguration m_config;
		/// <summary>로거</summary>
		protected readonly ILogger m_logger;

		/// <summary>생성자</summary>
		/// <param name="rabbitMqOptions">Rabbit MQ 설정 옵션 객체</param>
		/// <param name="logger">로거</param>
		public RabbitMqSender(
			IOptions<RabbitMqConfiguration> rabbitMqOptions,
			ILogger<RabbitMqSender> logger
		)
		{
			try
			{
				// 설정 복사
				m_config = new RabbitMqConfiguration();
				m_config.CopyValueFrom(rabbitMqOptions.Value);
				// 로거
				m_logger = logger;
				// Rabbit MQ 리스너 초기화
				InitializeRabbitMqListener();
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
				throw;
			}
		}

		/// <summary>Rabbit MQ 리스너 초기화</summary>
		private void InitializeRabbitMqListener()
		{
			try
			{
				var Factory = new ConnectionFactory
				{
					ClientProvidedName = m_config.Name,
					HostName = m_config.Host,
					Port = m_config.Port,
					VirtualHost = m_config.VirtualHost,
					UserName = m_config.User,
					Password = m_config.Password
				};

				// 연결 객체 생성
				m_connection = Factory.CreateConnection();
				// 채널 생성
				m_channel = m_connection.CreateModel();
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
		}

		/// <summary>객체를 Rabbit MQ로 전송한다.</summary>
		/// <param name="Exchange">Exchange 명</param>
		/// <param name="RoutingKey">라우팅 키</param>
		/// <param name="SendingObject">전송할 객체</param>
		/// <returns>전송 결과 응답 객체</returns>
		public ResponseData Send(string Exchange, string RoutingKey, object SendingObject)
		{
			var Result = new ResponseData();

			try
			{
				// 객체가 유효하지 않은 경우
				if (SendingObject == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 문자열로 변환
				string Message = JsonConvert.SerializeObject(SendingObject);

				// 메세지 전송
				m_channel.BasicPublish(exchange: Exchange,
					routingKey: RoutingKey,
					basicProperties: null,
					body: Message.GetBytes());

				Result.Result = EnumResponseResult.Success;

				m_logger.LogDebug("[Rabbit MQ] Data transfer was successful. (exchange: {Exchange}, routingKey: {RoutingKey}, Message: {Message})", Exchange, RoutingKey, Message);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>연결 종료</summary>
		public void Close()
		{
			m_channel?.Dispose();
			m_connection?.Dispose();

			m_channel = null;
			m_connection = null;
		}

		/// <summary>객체 해제</summary>
		public void Dispose()
		{
			Close();
		}
	}
}