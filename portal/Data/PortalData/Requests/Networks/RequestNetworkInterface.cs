/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version
* 3 of the License.  See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
using System.ComponentModel.DataAnnotations;
using PortalData.Enums;
using PortalData.ValidationAttributes;
using PortalResources;
using MTLib.CommonData;
using MTLib.Core;

namespace PortalData.Requests.Networks
{
	/// <summary>네트워크 인터페이스 정보 요청 클래스</summary>
	public class RequestNetworkInterface : CommonRequestData
	{
        /// <summary>인터페이스명</summary>
        [Required(ErrorMessageResourceName = "EM_NETWORKS_NETWORK_INTERFACE_REQUIRE_NAME", ErrorMessageResourceType = typeof(Resource))]
        public string Name
        {
	        get => m_name;
	        set => m_name = value.IsEmpty() ? "" : value.Trim();
        }
        private string m_name;

        /// <summary>설명</summary>
        public string Description { get; set; }

        /// <summary>DHCP 사용 여부</summary>
        public EnumYesNo? Dhcp { get; set; }

        /// <summary>맥주소</summary>
        public string MacAddress { get; set; }

        /// <summary>네트워크 연결 상태</summary>
        public EnumNetworkLinkState? LinkState { get; set; }

        /// <summary>아이피 주소</summary>
        [IpAddress(ErrorMessageResourceName = "EM_COMMON__INVALID_IP_ADDRESS", ErrorMessageResourceType = typeof(Resource))]
        public string IpAddress { get; set; }

        /// <summary>서브넷 마스크</summary>
        [IpAddress(ErrorMessageResourceName = "EM_COMMON__INVALID_SUBNET_MASK", ErrorMessageResourceType = typeof(Resource))]
        public string SubnetMask { get; set; }

        /// <summary>게이트웨이</summary>
        [IpAddress(ErrorMessageResourceName = "EM_COMMON__INVALID_GATEWAY", ErrorMessageResourceType = typeof(Resource))]
        public string Gateway { get; set; }

        /// <summary>DNS #1</summary>
        [IpAddress(ErrorMessageResourceName = "EM_COMMON__INVALID_DNS_1", ErrorMessageResourceType = typeof(Resource))]
        public string Dns1 { get; set; }

        /// <summary>DNS #2</summary>
        [IpAddress(ErrorMessageResourceName = "EM_COMMON__INVALID_DNS_2", ErrorMessageResourceType = typeof(Resource))]
        public string Dns2 { get; set; }
        
        /// <summary>BandWidth</summary>
        public decimal BandWidth { get; set; }
        
        /// <summary>관리용 인터페이스인지 여부</summary>
        public bool IsManagement { get; set; }
	}
}