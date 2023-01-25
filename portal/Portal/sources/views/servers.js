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
import { sizeToString } from "../models/utils/sizeToString";
import { showProgressIcon } from "../models/utils/showProgressIcon";
import { moveLogin } from "../models/utils/moveLogin";

const MY_URL = "/api/v1/Servers";
const MY_ADD_WINDOW = "server_add_window";
const MY_DELETE_WINDOW = "server_delete_window";
const MY_TABLE = "server";
const NUMBER_FORMAT = webix.Number.numToStr({
	groupDelimiter: ",",
	groupSize: 3,
	decimalDelimiter: ".",
	decimalSize: 0,
});

var MyList = [];

export default class ServerView extends JetView {
	config() {
		return {
			rows: [
				{
					height: 35,
					cols: [
						{
							view: "button",
							type: "icon",
							icon: "mdi mdi-plus",
							label: "추가",
							autowidth: true,
							borderless: true,
							popup: MY_ADD_WINDOW,
						},
						{
							view: "button",
							type: "icon",
							icon: "mdi mdi-delete",
							label: "삭제",
							autowidth: true,
							borderless: true,
							popup: MY_DELETE_WINDOW,
						},
						{ view: "spacer" },
						{
							view: "button",
							type: "icon",
							icon: "mdi mdi-reload",
							label: "새로고침",
							autowidth: true,
							borderless: true,
							click: function () {
								window.location.reload(true);
							},
						},
					],
				},
				{
					view: "datatable",
					id: MY_TABLE,
					sort: "multi",
					select: "row",
					multiselect: true,
					resizeColumn: true,
					checkboxRefresh: true,
					headerRowHeight: 25,
					css: "webix_header_border",
					columns: [
						{ id: "Id", header: "Id", hidden: true },
						{ id: "Check", header: { content: "masterCheckbox" }, template: "{common.checkbox()}", width: 40 },
						{ id: "Name", header: "Server Name", fillspace: true, sort: "string" },
						{ id: "Ip", header: "Ip Address", width: 180, sort: "string" },
						{
							id: "State",
							header: "Status",
							width: 100,
							sort: "string",
							template: (obj) => {
								var color = "gray";
								switch (obj.State) {
									case "Online":
										color = "green";
										break;
									case "Offline":
										color = "#E63031";
										break;
									case "Timeout":
										color = "blue";
										break;
									default:
										color = "gray";
								}
								return `<span class="${color}">${obj.State}</span>`;
							},
						},
						{
							id: "LoadAverage1M",
							header: [{ text: "CPU", colspan: 3 }, { text: "1M" }],
							width: 80,
							sort: "int",
							css: "number_align",
							format: NUMBER_FORMAT,
						},
						{
							id: "LoadAverage5M",
							header: ["", "5M"],
							width: 80,
							sort: "int",
							css: "number_align",
							format: NUMBER_FORMAT,
						},
						{
							id: "LoadAverage15M",
							header: ["", "15M"],
							width: 80,
							sort: "int",
							css: "number_align",
							format: NUMBER_FORMAT,
						},
						{
							id: "MemoryTotal",
							header: [{ text: "RAM", colspan: 3 }, "Total"],
							width: 80,
							sort: "string",
							css: "number_align",
							template: (obj) => {
								return sizeToString(obj.MemoryTotal);
							},
						},
						{
							id: "MemoryFree",
							header: ["", "Free"],
							width: 80,
							sort: "string",
							css: "number_align",
							template: (obj) => {
								return sizeToString(obj.MemoryFree);
							},
						},
						{
							id: "MemoryUsed",
							header: ["", "Used"],
							width: 80,
							sort: "string",
							css: "number_align",
							template: (obj) => {
								return sizeToString(obj.MemoryUsed);
							},
						},
					],
					ready: function () {
						this.sort([{ by: "Name", dir: "asc" }]);
						this.markSorting("Name", "asc", true);
						webix.extend(this, webix.ProgressBar);
					},
					on: {
						onSelectChange: function () {
							unchecked();
							var items = this.getSelectedItem();
							if (items != null) {
								if (Array.isArray(items)) {
									items.forEach(function (item) {
										item.Check = true;
									});
									this.refresh();
								} else {
									items.Check = true;
									this.refresh();
								}
							}
						},
						onCheck: function (row, column, state) {
							if (state) $$(MY_TABLE).select(row, true);
							else $$(MY_TABLE).unselect(row, true);
						},
					},
				},
			],
		};
	}
	init() {
		if ($$(MY_ADD_WINDOW) == null)
			webix.ui({
				id: MY_ADD_WINDOW,
				view: "popup",
				head: "Add",
				width: 380,
				body: {
					view: "form",
					borderless: true,
					elements: [
						{ view: "text", label: "Host", name: "ServerIp" },
						{
							cols: [
								{
									view: "button",
									css: "webix_secondary",
									value: "취소",
									click: function () {
										this.getTopParentView().hide();
									},
								},
								{
									view: "button",
									css: "webix_primary",
									value: "추가",
									hotkey: "enter",
									click: function () {
										if (this.getParentView().getParentView().validate()) {
											addServer(this.getFormView().getValues());
										} else webix.alert({ type: "error", text: "Form data is invalid" });
									},
								},
							],
						},
					],
					rules: {
						ServerIp: webix.rules.isNotEmpty,
					},
				},
			});
		if ($$(MY_DELETE_WINDOW) == null)
			webix.ui({
				id: MY_DELETE_WINDOW,
				head: "Delete",
				width: 280,
				body: {
					rows: [
						{ view: "label", label: "정말 삭제하시겠습니까?", align: "center" },
						{
							cols: [
								{
									view: "button",
									css: "webix_secondary",
									value: "취소",
									click: function () {
										this.getTopParentView().hide();
									},
								},
								{
									view: "button",
									css: "webix_primary",
									value: "삭제",
									hotkey: "enter",
									click: function () {
										deleteServer();
									},
								},
							],
						},
					],
				},
			});
		set();
	}
}
function set() {
	webix
		.ajax()
		.get(MY_URL + "/Details")
		.then(
			function (data) {
				var response = data.json();
				if (response.Result == "Error") webix.message({ text: response.Message, type: "error", expire: 5000 });
				else {
					MyList = response.Data.Items;
					MyList.forEach(function (item) {
						if (item.NetworkInterfaces != null && item.NetworkInterfaces.length > 0) item.Ip = item.NetworkInterfaces[0].IpAddress;
						else item.Ip = "";
					});
					$$(MY_TABLE).parse(MyList);
				}
			},
			function (error) {
				var response = JSON.parse(error.response);
				webix.message({ text: response.Message, type: "error", expire: 5000 });
				moveLogin("/#!/main/servers");
				return null;
			}
		);
}
function unchecked() {
	MyList.forEach(function (item) {
		item.Check = false;
	});
}

/**
 * 서버 Host 주소 값을 받아 해당 주소의 서버를 등록한다.
 * @param form json data : {"ServerIp":"string"}
 */
function addServer(form) {
	showProgressIcon(MY_TABLE);
	webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.post(MY_URL + "/Initialize", JSON.stringify(form))
		.then(
			function (data) {
				var response = data.json();
				if (response.Result == "Error") webix.message({ text: response.Message, type: "error", expire: 5000 });
				else window.location.reload(true);
			},
			function (error) {
				var response = JSON.parse(error.response);
				webix.message({ text: response.Message, type: "error", expire: 5000 });
			}
		);
}

/**
 * 서버 목록에서 선택한 서버를 삭제한다.
 */
function deleteServer() {
	var item = $$(MY_TABLE).getSelectedItem();
	if (item == null) {
		webix.alert({ type: "error", text: "서버를 선택해야 합니다." });
		return;
	}
	var DeleteUrl = MY_URL + "/" + item.Id;

	showProgressIcon(MY_TABLE);
	webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.del(DeleteUrl)
		.then(
			function (data) {
				var response = data.json();
				if (response.Result == "Error") webix.message({ text: response.Message, type: "error", expire: 5000 });
				else window.location.reload(true);
			},
			function (error) {
				var response = JSON.parse(error.response);
				webix.message({ text: response.Message, type: "error", expire: 5000 });
			}
		);
}
