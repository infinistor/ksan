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
import { JetView } from "webix-jet";
import { sizeToString } from "../../models/utils/sizeToString";
import { getDiskStateToColor } from "../../models/utils/getDiskStateToColor";
import { getDiskMode } from "../../models/utils/getDiskMode";

const MY_URL = "/api/v1/Disks";
const MY_TABLE = "disk_summary";

export default class DiskView extends JetView {
	config() {
		return {
			type: "abslayout",
			minWidth: 600,
			height: 300,
			rows: [
				{
					view: "label",
					label: "<span class='card_title'>Disks</span>",
				},
				{
					view: "list",
					borderless: true,
					id: MY_TABLE,
					minWidth: 600,
					height: 300,
					type: {
						height: "auto",
						template: (obj) => `
						<div class="disk_list">
							<div class="disk_usage">
								<span style="color:#1395F5;"> ${sizeToString(obj.UsedSize)} / </span>
								<span> ${sizeToString(obj.TotalSize)} </span>
							</div>
							<span class="disk_list_name">${obj.Name}</span>
							(${getDiskStateToColor(obj)}, ${getDiskMode(obj)})
						</div>
						<div class='progress_bar_element'>
							<div class='progress_result ${(obj.UsedSize / obj.TotalSize) * 100 > 85 ? "Week" : "Good"}' style='width:${(obj.UsedSize / obj.TotalSize) * 100 + "%"}'></div>
						</div>`,
					},
					ready: function () {
						// apply sorting
						this.sort(SortToUsedRate, "desc");
					},
					url: function () {
						return webix
							.ajax()
							.get("/api/v1/Disks")
							.then(
								function (data) {
									var response = data.json();
									if (response.Result == "Error") {
										webix.message({ text: response.Message, type: "error", expire: 5000 });
										return null;
									} else {
										return response.Data.Items;
									}
								},
								function (error) {
									var response = JSON.parse(error.response);
									webix.message({ text: response.Message, type: "error", expire: 5000 });
									moveLogin();
									return null;
								}
							);
					},
				},
			],
		};
	}
	init() {}
}

function SortToUsedRate(a, b) {
	var a_rate = a.UsedSize / a.TotalSize;
	var b_rate = b.UsedSize / b.TotalSize;
	return a_rate > b_rate ? 1 : -1;
}
