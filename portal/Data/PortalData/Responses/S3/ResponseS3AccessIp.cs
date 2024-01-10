using System;

namespace PortalData.Response.S3
{
	/// <summary>접근 아이피 주소 정보 응답 클래스</summary>
	public class ResponseS3AccessIp
	{
		/// <summary>허용주소 아이디</summary>
		public string AddressId { get; set; }

		/// <summary>사용자 아이디</summary>
		public string UserId { get; set; }

		/// <summary>Bucket 이름</summary>
		public string BucketName { get; set; }

		/// <summary>허용 시작 아이피 값</summary>
		public long StartIpNo { get; set; }

		/// <summary>허용 시작 아이피 주소</summary>
		public string StartIpAddress { get; set; }

		/// <summary>허용 종료 아이피 값</summary>
		public long EndIpNo { get; set; }

		/// <summary>허용 종료 아이피 주소</summary>
		public string EndIpAddress { get; set; }

		/// <summary>입력 아이피 주소</summary>
		public string IpAddress { get; set; }

		/// <summary>등록일시</summary>
		public DateTime RegDate { get; set; }

		/// <summary>등록자명</summary>
		public string RegName { get; set; }

		/// <summary>등록 아이디</summary>
		public string RegId { get; set; }
	}
}