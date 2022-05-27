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
package com.pspace.ifs.ksan.gw.format;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

/** Represent an Amazon AccessControlPolicy for a bucket or object. */
// CHECKSTYLE:OFF
public final class AccessControlPolicyJson {
    public AccessControlPolicyJson() {
        super();
    }

    @JsonProperty(GWConstants.JSON_AB_OW)
	public Ow ow;
    @JsonProperty(GWConstants.JSON_AB_ACE)
	public ACS acs;

    public static final class Ow {
        public Ow() {
            super();
        }

        @JsonProperty(GWConstants.ID)
		public String id;
        @JsonProperty(GWConstants.JSON_AB_DN)
        public String dN;
    }

    public static final class ACS {
        public ACS() {
            super();
        }

        @JsonProperty(GWConstants.JSON_AB_GT)
		public Collection<Gt> gt;

        public static final class Gt {
            public Gt() {
                super();
            }

            @JsonProperty(GWConstants.JSON_AB_GTE)
			public Gte gte;
            @JsonProperty(GWConstants.JSON_AB_PERM)
			public String perm;

            public static final class Gte {
                public Gte() {
                    super();
                }

                @JsonProperty(GWConstants.JSON_AB_TYPE)
                public String type;
                @JsonProperty(GWConstants.ID)
				public String id;
                @JsonProperty(GWConstants.JSON_AB_DDN)
                public String ddN;
                @JsonProperty(GWConstants.JSON_AB_EA)
                public String eA;
                @JsonProperty(GWConstants.URI)
				public String uri;
            }
        }
    }
}
// CHECKSTYLE:ON