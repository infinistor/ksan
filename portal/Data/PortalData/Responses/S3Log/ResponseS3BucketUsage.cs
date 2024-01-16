namespace PortalData.Response.S3Log
{
	/// <summary>리퀘스트 지불 규칙 응답 클래스</summary>
	public class ResponseS3BucketUsage
	{
		/// <summary>사용자명</summary>
		public string UserName { get; set; } = "";

		/// <summary>버킷명</summary>
		public string BucketName { get; set; } = "";

		/// <summary> 사용량 </summary>
		public long UsedSize { get; set; } = 0;

		/// <summary> 파일 갯수 </summary>
		public long FileCount { get; set; } = 0;
	}
}