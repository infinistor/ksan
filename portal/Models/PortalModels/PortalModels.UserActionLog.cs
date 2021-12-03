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
using System;

namespace PortalModels
{
    /// <summary>사용자 행동 로그</summary>
    public partial class UserActionLog {

        public UserActionLog()
        {
            OnCreated();
        }

        /// <summary>로그 아이디</summary>
        public virtual long LogId { get; set; }

        /// <summary>로그 레벨</summary>
        public virtual EnumDbLogLevel LogLevel { get; set; }

        /// <summary>접속아이피</summary>
        public virtual string IpAddress { get; set; }

        /// <summary>메세지</summary>
        public virtual string Message { get; set; }

        /// <summary>사용자 아이디</summary>
        public virtual Guid UserId { get; set; }

        /// <summary>사용자명</summary>
        public virtual string UserName { get; set; }

        /// <summary>등록일시</summary>
        public virtual DateTime RegDate { get; set; }

        /// <summary>사용자 정보</summary>
        public virtual User User { get; set; }

        #region Extensibility Method Definitions

        partial void OnCreated();

        #endregion
    }

}
