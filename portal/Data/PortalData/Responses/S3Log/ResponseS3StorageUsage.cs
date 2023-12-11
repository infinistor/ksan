namespace PortalData.Response.S3Log
{
	/// <summary>스토리지 지불 규칙 응답 클래스 </summary>
	public class ResponseS3StorageUsage
	{
		/// <summary>사용자명</summary>
		public string UserName { get; set; } = "";

		/// <summary>버킷명</summary>
		public string BucketName { get; set; } = "";

		/// <summary>최대 사용량(Byte)</summary>
		public long MaxUsage { get; set; } = 0;

		/// <summary>최저 사용량(Byte)</summary>
		public long MinUsage { get; set; } = 0;

		/// <summary>평균 사용량(Byte)</summary>
		public long AverageUsage { get; set; } = 0;
	}
}