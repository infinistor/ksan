using MTLib.CommonData;

namespace PortalData.Requests.S3
{
	/// <summary>접근 아이피 주소 정보 요청 클래스</summary>
	public class RequestS3BucketLogging : CommonRequestData
	{
		/// <summary>URL</summary>
		public string URL { get; set; }

		/// <summary>Access Key</summary>
		public string AccessKey { get; set; }

		/// <summary>Secret Key</summary>
		public string SecretKey { get; set; }

		/// <summary>버킷 이름</summary>
		public string SourceBucketName { get; set; }

		/// <summary>버킷 이름</summary>
		public string TargetBucketName { get; set; }

	}
}