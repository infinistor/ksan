namespace PortalData.Response.S3Log
{
	/// <summary>리퀘스트 지불 규칙 응답 클래스</summary>
	public class ResponseS3RequestCount
	{
		/// <summary>사용자명</summary>
		public string UserName { get; set; } = "";

		/// <summary>버킷명</summary>
		public string BucketName { get; set; } = "";

		/// <summary> 리퀘스트 종류 </summary>
		public string Operation { get; set; } = "";

		/// <summary> 파일 갯수 </summary>
		public long Count { get; set; } = 0;
	}
}