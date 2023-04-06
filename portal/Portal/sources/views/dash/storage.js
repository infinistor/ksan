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
import { moveLogin } from "../../models/utils/moveLogin";

const MY_URL = "/api/v1/DiskPools";

export default class StorageView extends JetView {
	config() {
		return {
			type: "abslayout",
			minWidth: 540,
			height: 300,
			borderless: true,
			rows: [
				{
					view: "label",
					label: "<span class='card_title'>Storage Capacity</span>",
					borderless: true,
				},
				{
					cols: [
						{
							view: "dataview",
							height: 300,
							minWidth: 540,
							borderless: true,
							scroll: false,
							prerender: true,
							type: {
								width: 1200,
								height: 300,
								template: (obj) => {
									const takenUnits = obj.TotalSize > 0 ? Math.floor((obj.UsedSize / obj.TotalSize) * 100) : 0;
									const curve = drawCurve(120, 120, 90, takenUnits);
									return `
							<div>
								<div>
								<div class="main_chart_legend">
									<span class="main_chart_rate">${takenUnits}%</span>
								</div>
								<div class="main_category">
									<span class="main_category_title">Used : </span>
									<span class="main_unit_value">${sizeToString(obj.UsedSize)} </span>
								</div>
								<div>
									<span class="main_category_title">Available : </span>
									<span class="main_unit_value_normal">${sizeToString(obj.TotalSize - obj.UsedSize)} </span>
								</div>
								<div>
									<span class="main_category_title">Total : </span>
									<span class="main_unit_value_normal">${sizeToString(obj.TotalSize)} </span>
								</div>
								<svg class="main_units_chart" height="300" width="600">
									<circle cx="120" cy="120" r="90" stroke="#DADEE0" stroke-width="40" fill="none" />
									<path d="${curve}" stroke="#1395F5" stroke-width="40" fill="none" />
									Sorry, your browser does not support inline SVG.
								</svg>
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
												return null;
											} else {
												var TotalSize = 0;
												var UsedSize = 0;
												response.Data.Items.forEach((item) => {
													TotalSize += item.TotalSize;
													UsedSize += item.UsedSize;
												});
												return [{ TotalSize: TotalSize, UsedSize: UsedSize }];
											}
										},
										function (error) {
											// var response = JSON.parse(error.response);
											// webix.message({ text: response.Message, type: "error", expire: 5000 });
											moveLogin();
											// return null;
										}
									);
							},
						},
					],
				},
			],
		};
	}
	init() {}
}
