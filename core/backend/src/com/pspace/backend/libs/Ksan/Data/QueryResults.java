/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License. See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.backend.libs.ksan.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QueryResults <T> {

	@JsonProperty("HavePreviousPageSection")
	public boolean HavePreviousPageSection;

	@JsonProperty("HaveNextPage")
	public boolean HaveNextPage;

	@JsonProperty("HavePreviousPage")
	public boolean HavePreviousPage;

	@JsonProperty("PageNos")
	public List<Integer> PageNos;

	@JsonProperty("EndPageNo")
	public int EndPageNo;

	@JsonProperty("StartPageNo")
	public int StartPageNo;

	@JsonProperty("PagePerSection")
	public int PagePerSection;

	@JsonProperty("HaveNextPageSection")
	public boolean HaveNextPageSection;

	@JsonProperty("CountPerPage")
	public int CountPerPage;

	@JsonProperty("PageNo")
	public int PageNo;

	@JsonProperty("Skips")
	public int Skips;

	@JsonProperty("TotalCount")
	public int TotalCount;

	@JsonProperty("TotalPage")
	public int TotalPage;

	@JsonProperty("Items")
	public List<T> Items;

	
}
