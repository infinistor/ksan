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
 * 서버 목록을 가져온다.
 * @returns Servers. json:{id:"Server Id", value:"Server Name"}
 */
export function loadServers() {
	return webix
		.ajax()
		.get("/api/v1/Servers")
		.then(
			function (data) {
				var response = data.json();
				if (response.Result == "Error") {
					webix.message({ text: response.Message, type: "error", expire: 5000 });
					return [];
				} else {
					var Servers = [];
					response.Data.Items.forEach((item) => {
						Servers.push({ id: item.Id, value: item.Name });
					});
					return Servers;
				}
			},
			function (error) {
				if (error.status != 401) {
					var response = JSON.parse(error.response);
					webix.message({ text: response.Message, type: "error", expire: 5000 });
				}

				return [];
			}
		);
}
