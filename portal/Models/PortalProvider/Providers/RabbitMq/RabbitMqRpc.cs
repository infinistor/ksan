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
using System.Collections.Concurrent;
using System.Threading;
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
using RabbitMQ.Client.Events;

namespace PortalProvider.Providers.RabbitMq
{
	/// <summary>Rabbit MQ로 정보를 송/수신하는 클래스</summary>
	public class RabbitMqRpc : IRabbitMqRpc
	{
		/// <summary>Rabbit MQ 연결 객체</summary>
		private IConnection m_connection;
		/// <summary>Rabbit MQ 채널 객체</summary>
		private IModel m_channel;
		/// <summary>결과 수신 객체</summary>
		private EventingBasicConsumer m_receiver;
		/// <summary>Rabbit MQ 설정 객체</summary>
		private readonly RabbitMqConfiguration m_config;
		/// <summary>로거</summary>
		protected readonly ILogger m_logger;
		/// <summary>기본 속성 객체</summary>
		protected readonly IBasicProperties m_properties;
		/// <summary>응답 Rabbit MQ 큐 이름</summary>
		private readonly string m_receiverQueueName;
		/// <summary>응답 큐</summary>
		private readonly BlockingCollection<string> m_responseQueue = new BlockingCollection<string>();

		/// <summary>생성자</summary>
		/// <param name="rabbitMqOptions">Rabbit MQ 설정 옵션 객체</param>
		/// <param name="logger">로거</param>
		public RabbitMqRpc(
			IOptions<RabbitMqConfiguration> rabbitMqOptions,
			ILogger<RabbitMqRpc> logger
		)
		{
			try
			{
				// 설정 복사
				m_config = new RabbitMqConfiguration();
				m_config.CopyValueFrom(rabbitMqOptions.Value);
				// 로거
				m_logger = logger;

				// Rabbit MQ 초기화
				InitializeRabbitMqListener();

				// 응답 관련 설정
				m_properties = m_channel.CreateBasicProperties();
				string correlationId = Guid.NewGuid().ToString();
				m_properties.CorrelationId = correlationId;
				m_receiverQueueName = m_channel.QueueDeclare().QueueName;
				m_properties.ReplyTo = m_receiverQueueName;

				// 결과 수신 객체 생성
				m_receiver = new EventingBasicConsumer(m_channel);
				m_receiver.Received += (_, ea) =>
				{
					if (ea != null)
					{
						// 연관 아이디가 동일한 경우, 응답 큐에 넣는다.
						if (ea.BasicProperties.CorrelationId == correlationId)
							m_responseQueue.Add(ea.Body.ToArray().GetString());
					}
				};
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
				ConnectionFactory factory = new ConnectionFactory
				{
					ClientProvidedName = m_config.Name,
					HostName = m_config.Host,
					Port = m_config.Port,
					VirtualHost = m_config.VirtualHost,
					UserName = m_config.User,
					Password = m_config.Password
				};

				// 연결 객체 생성
				m_connection = factory.CreateConnection();
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
		/// <param name="WaitForResponseTimeoutSec">응답 대기 타임 아웃 시간 (초)</param>
		/// <returns>전송 결과 응답 객체</returns>
		public ResponseData<string> Send(string RoutingKey, object SendingObject, int WaitForResponseTimeoutSec)
		{
			ResponseData<string> Result = new ResponseData<string>();

			var CancellationTokenSource = new CancellationTokenSource();
			try
			{
				// 객체가 유효하지 않은 경우
				if (SendingObject == null)
					return new ResponseData<string>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 문자열로 변환
				var Message = JsonConvert.SerializeObject(SendingObject);

				// 메세지 전송
				m_channel.BasicPublish(exchange: m_config.ExchangeName,
					routingKey: RoutingKey,
					basicProperties: m_properties,
					body: Message.GetBytes());

				m_logger.LogDebug($"[Rabbit MQ] RPC Data transfer was successful and wait for Response. (exchange: {m_config.ExchangeName}, routingKey: {RoutingKey}, Message: {Message})");

				// 응답 수신
				m_channel.BasicConsume(
					consumer: m_receiver,
					queue: m_receiverQueueName,
					autoAck: true);

				// N초 후 취소
				CancellationTokenSource.CancelAfter(1000 * WaitForResponseTimeoutSec);

				try
				{
					// 응답 데이터를 가져온다.
					Result.Data = m_responseQueue.Take(CancellationTokenSource.Token);
					Result.Result = EnumResponseResult.Success;
				}
				catch (Exception /*e*/)
				{
					m_logger.LogError($"[Rabbit MQ] Response timed out for RPC Data transfer. (exchange: {m_config.ExchangeName}, routingKey: {RoutingKey}, Message: {Message})");

					Result.Code = Resource.EC_COMMON__EXCEPTION;
					Result.Message = Resource.EM_COMMON__COMMUNICATION_TIMEOUT;
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			finally
			{
				CancellationTokenSource.Dispose();
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