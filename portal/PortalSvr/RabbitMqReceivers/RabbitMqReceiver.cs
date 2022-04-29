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
using System.Threading;
using System.Threading.Tasks;
using PortalData;
using PortalProvider.Providers.RabbitMq;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using MTLib.Core;
using MTLib.Reflection;
using Newtonsoft.Json;
using RabbitMQ.Client;
using RabbitMQ.Client.Events;

// ReSharper disable TemplateIsNotCompileTimeConstantProblem

namespace PortalSvr.RabbitMqReceivers
{
	/// <summary>Rabbit MQ로 부터 정보를 수신하는 클래스</summary>
	public abstract class RabbitMqReceiver : BackgroundService
	{
		/// <summary>Rabbit MQ 연결 객체</summary>
		private IConnection m_connection;
		/// <summary>Rabbit MQ 채널 객체</summary>
		private IModel m_channel;
		/// <summary>큐 이름</summary>
		private readonly string m_queueName;
		/// <summary>Exchange 이름</summary>
		private readonly string m_exchangeName;
		/// <summary>바인딩 키 목록</summary>
		private readonly string[] m_bindingKeys;
		/// <summary>Rabbit MQ 설정 객체</summary>
		private readonly RabbitMqConfiguration m_config = new RabbitMqConfiguration();
		/// <summary>로거</summary>
		protected readonly ILogger m_logger;
		/// <summary>서비스 팩토리</summary>
		protected readonly IServiceScopeFactory m_serviceScopeFactory;

		/// <summary>생성자</summary>
		/// <param name="queueName">큐 이름</param>
		/// <param name="exchangeName">Exchange 이름</param>
		/// <param name="bindingKeys">바인딩 키 목록</param>
		/// <param name="rabbitMqOptions">Rabbit MQ 설정 옵션 객체</param>
		/// <param name="logger">로거</param>
		/// <param name="serviceScopeFactory">서비스 팩토리</param>
		public RabbitMqReceiver(
			string queueName,
			string exchangeName,
			string[] bindingKeys,
			IOptions<RabbitMqConfiguration> rabbitMqOptions,
			ILogger logger,
			IServiceScopeFactory serviceScopeFactory
		)
		{
			try
			{
				// 큐 이름 저장
				m_queueName = queueName;
				// Exchange 이름 저장
				m_exchangeName = exchangeName;
				// 바인딩 키 목록 저장
				m_bindingKeys = bindingKeys;
				// 설정 복사
				m_config.CopyValueFrom(rabbitMqOptions.Value);
				// 로거
				m_logger = logger;
				// 서비스 팩토리 저장
				m_serviceScopeFactory = serviceScopeFactory;
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
				// 연결 해제 이벤트 설정
				m_connection.ConnectionShutdown += (_, _) =>
				{
					m_logger.LogInformation($"[Rabbit MQ] Connection is shutdown. {m_queueName}");
				};
				// 채널 생성
				m_channel = m_connection.CreateModel();
				// 큐 설정
				m_channel.QueueDeclare(queue: m_queueName, durable: true, exclusive: false, autoDelete: false, arguments: null);
				// Exchange 설절
				m_channel.ExchangeDeclare(exchange: m_exchangeName, type: "topic");
				// 모든 바인딩 키 처리
				if (m_bindingKeys != null)
				{
					foreach (string bindingKey in m_bindingKeys)
						m_channel.QueueBind(queue: m_queueName, exchange: m_exchangeName, routingKey: bindingKey);
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
		}

		/// <summary>서비스 실행</summary>
		/// <param name="stoppingToken">실행 취소 토큰 객체</param>
		/// <returns></returns>
		protected override Task ExecuteAsync(CancellationToken stoppingToken)
		{
			try
			{
				stoppingToken.ThrowIfCancellationRequested();

				// Consumer 생성
				EventingBasicConsumer consumer = new EventingBasicConsumer(m_channel);
				// 데이터 수신 이벤트 설정
				consumer.Received += async (_, ea) =>
				{
					try
					{
						// 수신 객체가 유효한 경우, 해당 데이터 처리
						if (ea != null)
						{
							ResponseMqData responseHandleMessage = await HandleMessage(ea.RoutingKey, ea.Body.ToArray());

							// 처리된 건인 경우
							if (responseHandleMessage.IsProcessed)
							{
								m_logger.LogDebug($"[Process] MQ Message Received on [{{0}}] : {{1}}, data = {{2}}", m_queueName, ea.RoutingKey, ea.Body.ToArray().GetString());

								// 응답 연관 아이디가 존재하는 경우
								IBasicProperties properties = ea.BasicProperties;
								if (properties != null && !properties.CorrelationId.IsEmpty())
								{
									// 답으로 보낼 속성 생성하고 연관 아이디 저장
									IBasicProperties replyProps = m_channel.CreateBasicProperties();
									replyProps.CorrelationId = properties.CorrelationId;

									// 결과를 json으로 변환
									string json = JsonConvert.SerializeObject(responseHandleMessage);

									// 응답 전송
									m_channel.BasicPublish(exchange: "",
										routingKey: properties.ReplyTo,
										basicProperties: replyProps,
										body: json.GetBytes());
								}

								// 승인 처리
								m_channel.BasicAck(ea.DeliveryTag, false);
							}
							// 처리되지 않은 건인 경우
							else
							{
								m_logger.LogDebug($"[Reject] MQ Message Received on [{{0}}] : {{1}}, data = {{2}}", m_queueName, ea.RoutingKey, ea.Body.ToArray().GetString());

								// 처리 거부
								m_channel.BasicReject(ea.DeliveryTag, false);
							}
						}
					}
					catch (Exception ex)
					{
						NNException.Log(ex);
					}
				};

				// 이벤트 설정
				consumer.Registered += OnConsumerRegistered;
				consumer.Unregistered += OnConsumerUnregistered;
				consumer.ConsumerCancelled += OnConsumerCancelled;
				consumer.Shutdown += OnConsumerShutdown;

				m_channel.BasicConsume(m_queueName, false, consumer);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}

			return Task.CompletedTask;
		}

		/// <summary>메시지 처리</summary>
		/// <param name="routingKey">라우팅 키</param>
		/// <param name="body">내용</param>
		protected abstract Task<ResponseMqData> HandleMessage(string routingKey, byte[] body);

		/// <summary>메세지 수신 등록 시 이벤트</summary>
		/// <param name="sender">이벤트 발송 객체</param>
		/// <param name="e">이벤트 데이터</param>
		private void OnConsumerRegistered(object sender, ConsumerEventArgs e)
		{
			m_logger.LogInformation($"[Rabbit MQ] Receiver is registered. {m_queueName}");
		}

		/// <summary>메세지 수신 해제 시 이벤트</summary>
		/// <param name="sender">이벤트 발송 객체</param>
		/// <param name="e">이벤트 데이터</param>
		private void OnConsumerUnregistered(object sender, ConsumerEventArgs e)
		{
			m_logger.LogInformation($"[Rabbit MQ] Receiver is unregistered. {m_queueName}");
		}

		/// <summary>메세지 수신 취소 시 이벤트</summary>
		/// <param name="sender">이벤트 발송 객체</param>
		/// <param name="e">이벤트 데이터</param>
		private void OnConsumerCancelled(object sender, ConsumerEventArgs e)
		{
			m_logger.LogInformation($"[Rabbit MQ] Receiver is cancelled. {m_queueName}");
		}

		/// <summary>메세지 수신 종료 시 이벤트</summary>
		/// <param name="sender">이벤트 발송 객체</param>
		/// <param name="e">이벤트 데이터</param>
		private void OnConsumerShutdown(object sender, ShutdownEventArgs e)
		{
			m_logger.LogInformation($"[Rabbit MQ] Receiver is shutdown. {m_queueName}");
		}
	}
}