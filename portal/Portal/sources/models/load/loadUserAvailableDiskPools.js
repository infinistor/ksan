/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version
* 3 of the License.See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/

/**
 * 디스크풀 목록을 가져온다.
 * @returns DiskPools. json:{id:"DiskPool Id", value:"Server Name"}
 */
export function loadUserAvailableDiskPools(userId, diskPoolId) {
	var url = "";
	if (diskPoolId == undefined) url = `/api/v1/KsanUsers/StorageClass/${userId}/Available`;
	else url = `/api/v1/KsanUsers/StorageClass/${userId}/Available/${diskPoolId}`;
	return webix
		.ajax()
		.get(url)
		.then(
			function (data) {
				var response = data.json();
				if (response.Result == "Error") {
					webix.message({ text: response.Message, type: "error", expire: 5000 });
					return "";
				} else {
					var DiskPools = [];
					response.Data.Items.forEach((item) => {
						DiskPools.push({ id: item.Id, value: item.Name });
					});
					return DiskPools;
				}
			},
			function (error) {
				var response = JSON.parse(error.response);
				if (response.code != "EC003") webix.message({ text: response.Message, type: "error", expire: 5000 });
				return "";
			}
		);
}
