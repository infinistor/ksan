namespace PortalData.Response.S3Log
{
	/// <summary>리퀘스트 지불 규칙 응답 클래스</summary>
	public class ResponseS3TransferUsage
	{
		/// <summary>사용자명</summary>
		public string UserName { get; set; } = "";

		/// <summary>버킷명</summary>
		public string BucketName { get; set; } = "";

		/// <summary> Upload </summary>
		public long UploadUsage { get; set; } = 0;

		/// <summary>Download</summary>
		public long DownloadUsage { get; set; } = 0;

	}
}