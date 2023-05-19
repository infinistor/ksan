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
import { drawCurve } from "../../models/draw/drawCurve";
import { getStatusToColor } from "../../models/utils/getStatusToColor";
import { getReplicationType } from "../../models/utils/getReplicationType";
import { moveLogin } from "../../models/utils/moveLogin";

const MY_URL = "/api/v1/DiskPools";
const MY_TABLE = "diskpool_summary";
const MY_STATUS = "diskpool_status";

export default class DiskpoolView extends JetView {
	config() {
		return {
			type: "abslayout",
			minWidth: 540,
			height: 300,
			rows: [
				{ id: MY_STATUS, view: "label" },
				{
					view: "list",
					borderless: true,
					minWidth: 540,
					xCount: 1,
					id: MY_TABLE,
					scroll: "auto",
					css: "my_list",
					type: {
						height: 85,
						template: (obj) => {
							const takenUnits = obj.TotalSize > 0 ? Math.floor((obj.UsedSize / obj.TotalSize) * 100) : 0;
							const x = 50;
							const y = 40;
							const r = 25;
							const curve = drawCurve(x, y, r, takenUnits);
							return `
							<div style="padding-left:6px;">
								<svg class="diskpools_unit" height="85" width="100">
								<circle cx="${x}" cy="${y}" r="${r}" stroke="#DADEE0" stroke-width="15" fill="none" />
								<path d="${curve}" stroke="#1395F5" stroke-width="15" fill="none" />
								Sorry, your browser does not support inline SVG.
								</svg>
								<div class="diskpools_title">
									<span class="diskpools_rate">${takenUnits}%</span>
								</div>
								<div class="diskpools_list">
									<span> ${obj.Name} </span>
									${getStatusToColor(obj)} <br>
									<span style="color:#1395F5;"> ${sizeToString(obj.UsedSize)} / </span>
									<span> ${sizeToString(obj.UsedSize + obj.FreeSize)} </span>
									<span style='padding-left:20px; font-weight:normal;'> Tolerance : </span>
									<span>${getReplicationType(obj)}</span>
								</div>
							</div>`;
						},
					},
					url: function () {
						return webix
							.ajax()
							.get(MY_URL)
							.then(
								function (data) {
									var response = data.json();
									if (response.Result == "Error") {
										webix.message({ text: response.Message, type: "error", expire: 5000 });
										return [];
									} else {
										var g_status = true;
										response.Data.Items.forEach((item) => {
											var status = true;
											if (item.Disks != null && item.Disks.length > 0) {
												item.Disks.forEach((disk) => {
													if (disk.State != "Good") { status = false; g_status = false; }
												})
											}
											item.State = "Online";
										});
										if (g_status == true) $$(MY_STATUS).setValue("<span class='card_title'>Disk Pools</span> <span class='status_marker healthy'>Healthy</span>");
										else $$(MY_STATUS).setValue("<span class='card_title'>Disk Pools</span> <span class='status_marker unhealthy'>Unhealthy</span>");
										return response.Data.Items;
									}
								},
								function (error) {
									if (error.status != 401) {
										var response = JSON.parse(error.response);
										webix.message({ text: response.Message, type: "error", expire: 5000 });
									}
									moveLogin();
									return [];
								}
							);
					},
				},
			],
		};
	}
	init() { }
}
