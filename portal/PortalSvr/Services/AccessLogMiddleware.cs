
using System.Threading.Tasks;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Logging;

namespace PortalSvr.Services
{
	/// <summary>접근 로그 미들웨어 클래스</summary>
	public class AccessLogMiddleware
	{
		/// <summary>Request를 처리하는 함수</summary>
		private readonly RequestDelegate _next;

		/// <summary>생성자</summary>
		/// <param name="next">Request에 대한 다음 처리 함수</param>
		public AccessLogMiddleware(RequestDelegate next)
		{
			_next = next;
		}

		/// <summary>처리 함수</summary>
		/// <param name="context">HttpContext 객체</param>
		/// <param name="logger">로그 객체</param>
		public async Task Invoke(HttpContext context, ILogger<AccessLogMiddleware> logger)
		{
			await _next.Invoke(context);
			var message = $"{context.Request.HttpContext.Connection.RemoteIpAddress} \"{context.Request.Method} {context.Request.Path} {context.Request.Protocol}\" {context.Response.StatusCode} {context.Response.ContentLength} {context.Request.Headers["User-Agent"]}";
			logger.LogInformation(message);
		}
	}
	
	/// <summary>요청에 대한 접근 아이피 검사 미들웨어 확장 클래스</summary>
	public static class AccessLogMiddlewareExtensions
	{
		/// <summary>요청에 대한 접근 아이피 검사 미들웨어 사용 처리</summary>
		/// <param name="builder">어플리케이션 빌더 객체</param>
		/// <returns>어플리케이션 빌더 객체</returns>
		public static IApplicationBuilder UseAccessLogMiddleware(this IApplicationBuilder builder)
		{
			return builder.UseMiddleware<AccessLogMiddleware>();
		}
	}
}