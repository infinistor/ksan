namespace PortalData.Response.S3Log
{
	/// <summary>스토리지 지불 규칙 응답 클래스 </summary>
	public class ResponseS3RequestUsage
	{
		/// <summary>사용자명</summary>
		public string UserName { get; set; } = "";

		/// <summary>버킷명</summary>
		public string BucketName { get; set; } = "";

		/// <summary>GET 사용량(Count)</summary>
		public long Get { get; set; } = 0;

		/// <summary>PUT 사용량(Count)</summary>
		public long Put { get; set; } = 0;

		/// <summary>DELETE 사용량(Count)</summary>
		public long Delete { get; set; } = 0;

		/// <summary>POST 사용량(Count)</summary>
		public long Post { get; set; } = 0;

		/// <summary>LIST 사용량(Count)</summary>
		public long List { get; set; } = 0;

		/// <summary>HEAD 사용량(Count)</summary>
		public long Head { get; set; } = 0;
	}
}