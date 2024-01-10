using System.ComponentModel.DataAnnotations;
using MTLib.CommonData;
using PortalResources;

namespace PortalData.Requests.S3
{
	/// <summary>접근 아이피 주소 정보 요청 클래스</summary>
	public class RequestS3AccessIp : CommonRequestData
	{
		/// <summary>사용자 아이디</summary>
		[Required(ErrorMessageResourceName = "EM_S3_ACCESS_IP_REQUIRE_USER_ID", ErrorMessageResourceType = typeof(Resource))]
		public string UserId { get; set; }

		/// <summary>입력 아이피 주소</summary>
		[Required(ErrorMessageResourceName = "EM_S3_ACCESS_IP_REQUIRE_IP_ADDRESS", ErrorMessageResourceType = typeof(Resource))]
		public string IpAddress { get; set; }

		/// <summary>입력 버킷 이름</summary>
		[Required(ErrorMessageResourceName = "EM_S3_ACCESS_IP_REQUIRE_BUCKET_NAME", ErrorMessageResourceType = typeof(Resource))]
		public string BucketName { get; set; }
	}
}