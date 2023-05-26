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
import { moveLogin } from "../models/utils/moveLogin";
import { sizeToString } from "../models/utils/sizeToString";
import { showProgressIcon } from "../models/utils/showProgressIcon";
import { getStatusToColor } from "../models/utils/getStatusToColorTable";

const SERVER_URL = "/api/v1/Servers";
const SERVER_ADD_WINDOW = "server_add_window";
const SERVER_DELETE_WINDOW = "server_delete_window";
const SERVER_DELETE_BUTTON = "server_delete_button";
const SERVER_TABLE = "server";
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
					view: "toolbar",
					css: "webix_dark",
					paddingX: 20,
					elements: [{ view: "label", label: "Server", height: 0 }],
					height: 50,
					borderless: true,
				},
				{
					height: 35,
					cols: [
						{
							view: "icon",
							icon: "mdi mdi-plus",
							tooltip: "추가",
							autowidth: true,
							borderless: true,
							popup: SERVER_ADD_WINDOW,
						},
						{
							view: "icon",
							icon: "mdi mdi-delete",
							tooltip: "삭제",
							disabled: true,
							autowidth: true,
							borderless: true,
							id: SERVER_DELETE_BUTTON,
							popup: SERVER_DELETE_WINDOW,
						},
						{
							view: "icon",
							icon: "mdi mdi-table-refresh",
							tooltip: "새로고침",
							autowidth: true,
							borderless: true,
							click: function () { reload(); },
						},
						{},
					],
				},
				{
					view: "datatable",
					id: SERVER_TABLE,
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
						{ id: "Name", header: "Server Name", fillspace: true, minWidth: 130, sort: "string" },
						{ id: "Ip", header: "Ip Address", width: 180, sort: "string" },
						{
							id: "State",
							header: "Status",
							width: 100,
							sort: "string",
							template: (obj) => {
								return getStatusToColor(obj.State);
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
							$$(SERVER_DELETE_BUTTON).enable();
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
							if (state) $$(SERVER_TABLE).select(row, true);
							else $$(SERVER_TABLE).unselect(row, true);
						},
					},
				},
			],
		};
	}
	init() {
		load();
		if ($$(SERVER_ADD_WINDOW) == null)
			webix.ui({
				id: SERVER_ADD_WINDOW,
				view: "popup",
				head: "서버 추가",
				width: 380,
				body: {
					rows: [
						{ view: "label", label: "서버 추가", align: "center" },
						{ view: "label", template: "<div class='popup_title_line' />", height: 2 },
						{
							view: "form",
							borderless: true,
							elements: [
								{ view: "text", label: "Host", name: "ServerIp", required: true, },
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
													this.getTopParentView().hide();
												}
											},
										},
									],
								},
							],
							rules: {
								ServerIp: webix.rules.isNotEmpty,
							},
						}]
				},
				on: {
					onShow: function () {
						this.getBody().getChildViews()[2].clear();
					}
				}
			});
		if ($$(SERVER_DELETE_WINDOW) == null)
			webix.ui({
				id: SERVER_DELETE_WINDOW,
				view: "popup",
				head: "Delete",
				width: 280,
				body: {
					rows: [
						{ view: "label", label: "서버 삭제", align: "center" },
						{ view: "label", template: "<div class='popup_title_line' />", height: 2 },
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
										deleteServers();
										this.getTopParentView().hide();
									},
								},
							],
						},
					],
				},
			});
	}
}

/**
 * 서버 목록을 가져온다.
 */
function load() {
	webix
		.ajax()
		.get(SERVER_URL + "/Details")
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
					$$(SERVER_TABLE).unselectAll();
					$$(SERVER_TABLE).clearAll();
					$$(SERVER_TABLE).parse(MyList);
				}
			},
			function (error) {
				moveLogin("/#!/main/servers");
			}
		);
}

/**
 * 서버 목록을 새로고침한다.
 */
function reload(message) {
	if (message != null) webix.message({ text: message, type: "success", expire: 5000 });
	const DELAY = 1000;
	showProgressIcon(SERVER_TABLE, DELAY);
	setTimeout(function () { load(); }, DELAY);
}

/**
 * 선택한 서버들을 선택해제한다.
 */
function unchecked() {
	MyList.forEach(function (item) {
		item.Check = false;
	});
}

/**
 * 서버 Host 주소 값을 받아 해당 주소의 서버를 등록한다.
 * @param {"ServerIp":"string"} form 서버 Host 주소
 */
function addServer(form) {
	webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.post(SERVER_URL + "/Initialize", JSON.stringify(form))
		.then(
			function (data) {
				var response = data.json();
				if (response.Result == "Error") webix.message({ text: response.Message, type: "error", expire: 5000 });
				else reload("서버를 추가했습니다.");
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
function deleteServers() {
	var items = $$(SERVER_TABLE).getSelectedItem();
	if (items == null) webix.alert({ text: "서버를 선택해야 합니다.", type: "error", expire: 5000 });
	else if (Array.isArray(items)) {
		var result = false;
		items.forEach(function (item) {
			if (deleteServer(item)) result = true;
		});
		if (result) reload("선택한 서버들을 삭제했습니다.");

	} else {
		if (deleteServer(items)) reload("선택한 서버를 삭제했습니다.");
	}
}
/**
 * 특정 서버를 삭제한다.
 * @return {boolean} 삭제 결과
 */
function deleteServer(item) {
	return webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.del(`${SERVER_URL}/${item.Id}`)
		.then(
			function (data) {
				var response = data.json();
				console.log(response);
				if (response.Result == "Error") { webix.message({ text: response.Message, type: "error", expire: 5000 }); return false; }
				else return true;
			},
			function (error) {
				var response = JSON.parse(error.response);
				webix.message({ text: response.Message, type: "error", expire: 5000 });
				return false;
			}
		);
}
