namespace PortalData.Response.S3Log
{
	/// <summary>리퀘스트 지불 규칙 응답 클래스</summary>
	public class ResponseS3ErrorUsage
	{
		/// <summary>사용자명</summary>
		public string UserName { get; set; } = "";

		/// <summary>버킷명</summary>
		public string BucketName { get; set; } = "";

		/// <summary> 4XX 에러 갯수 </summary>
		public long ClientError { get; set; } = 0;

		/// <summary> 5XX 에러 갯수 </summary>
		public long ServerError { get; set; } = 0;
	}
}