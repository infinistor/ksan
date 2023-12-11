namespace PortalData.Response.S3Log
{
	/// <summary>리퀘스트 지불 규칙 응답 클래스</summary>
	public class ResponseS3Transfer
	{
		/// <summary>사용자명</summary>
		public string UserName { get; set; } = "";

		/// <summary>버킷명</summary>
		public string BucketName { get; set; } = "";

		/// <summary> 업로드 </summary>
		public ulong Upload { get; set; } = 0;

		/// <summary> 다운로드 </summary>
		public ulong Download { get; set; } = 0;
	}
}