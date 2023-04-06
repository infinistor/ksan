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
const MY_STATUS = "disk_status";

var Sorting = {
	title: "used",
	direction: "desc"

};

export default class DiskView extends JetView {
	config() {
		return {
			type: "abslayout",
			minWidth: 540,
			height: 300,
			rows: [
				{
					cols: [
						{
							view: "label",
							id: MY_STATUS,
						},
						{},
						{
							view: "icon",
							id: "nameSorting",
							icon: "mdi mdi-sort-alphabetical-ascending",
							css: "webix_rpt_sort_icon",
							icons: {
								asc: "mdi mdi-sort-alphabetical-ascending",
								desc: "mdi mdi-sort-alphabetical-descending"
							},
							click: function () {
								return Sort("name");
							}
						},
						{
							view: "icon",
							id: "usedSorting",
							icon: "mdi mdi-sort-ascending",
							css: "webix_rpt_sort_icon webix_rpt_btn_active",
							icons: {
								asc: "mdi mdi-sort-ascending",
								desc: "mdi mdi-sort-descending"
							},
							click: function () {
								return Sort("used");
							}
						},
						{ width: 10 }
					]
				},
				{
					view: "list",
					borderless: true,
					id: MY_TABLE,
					minWidth: 540,
					height: 260,
					css: "my_list",
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
						<div class="progress_bar_element">
							<div class="progress_result ${(obj.UsedSize / obj.TotalSize) * 100 > 85 ? "Week" : "Good"}" style="width:${(obj.UsedSize / obj.TotalSize) * 100 + "%"}"></div>
						</div>`,
					},
					ready: function () {
						this.sort(SortToUsedRate, "desc");
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
										return "";
									} else {
										var status = true;
										response.Data.Items.forEach(item => {
											if (item.State != "Good") status = false;
										});
										if (status == true) $$(MY_STATUS).setValue("<span class='card_title'>Disks</span> <span class='status_marker healthy'>Healthy</span>");
										else $$(MY_STATUS).setValue("<span class='card_title'>Disks</span> <span class='status_marker unhealthy'>Unhealthy</span>");
										return response.Data.Items;
									}
								},
								function (error) {
									// var response = JSON.parse(error.response);
									// webix.message({ text: response.Message, type: "error", expire: 5000 });
									moveLogin();
								}
							);
					},
				},
			],
		};
	}
	init() { }
}

function SortToUsedRate(a, b) {
	var a_rate = a.UsedSize / a.TotalSize;
	var b_rate = b.UsedSize / b.TotalSize;
	return a_rate > b_rate ? 1 : -1;
}

function Sort(title) {
	var old_event = webix.copy(Sorting);
	Sorting = {
		title: title,
		direction: old_event.title == title ? (old_event.direction == "asc" ? "desc" : "asc") : "asc"
	}
	if (Sorting.title == "used") $$(MY_TABLE).sort(SortToUsedRate, Sorting.direction);
	else $$(MY_TABLE).sort("Name", Sorting.direction);
	ChangeSortButtons(Sorting, old_event);
}
function ChangeSortButtons(new_event, old_event) {
	var new_item = $$(new_event.title + "Sorting");
	webix.html.addCss(new_item.$view, "webix_rpt_btn_active");
	new_item.config.icon = new_item.config.icons[new_event.direction];
	new_item.refresh();

	if (new_event.title != old_event.title) {
		var old_item = $$(old_event.title + "Sorting");
		webix.html.removeCss(old_item.$view, "webix_rpt_btn_active")
	}
}