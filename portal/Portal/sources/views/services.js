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
import { loadServers } from "../models/load/loadServers";
import { sizeToString } from "../models/utils/sizeToString";
import { getServiceType } from "../models/enum/enum-service-type";
import { showProgressIcon } from "../models/utils/showProgressIcon";
import { getStatusToColor } from "../models/utils/getStatusToColorTable";

const SERVICE_URL = "/api/v1/Services";
const SERVICE_ADD_WINDOW = "service_add_window";
const SERVICE_DELETE_WINDOW = "service_delete_window";
const SERVICE_DELETE_BUTTON = "service_delete_button";
const SERVICE_START_WINDOW = "service_start_window";
const SERVICE_START_BUTTON = "service_start_button";
const SERVICE_STOP_WINDOW = "service_stop_window";
const SERVICE_STOP_BUTTON = "service_stop_button";
const SERVICE_RESTART_WINDOW = "service_restart_window";
const SERVICE_RESTART_BUTTON = "service_restart_button";
const SERVICE_CONTENT_MENU = "service_content_menu";
const SERVICE_TABLE = "service";

var MyList = [];

export default class ServiceView extends JetView {
	config() {
		return {
			rows: [
				{
					view: "toolbar",
					css: "webix_dark",
					paddingX: 20,
					elements: [{ view: "label", label: "Services", height: 0 }],
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
							popup: SERVICE_ADD_WINDOW,
						},
						{
							view: "icon",
							icon: "mdi mdi-delete",
							tooltip: "삭제",
							disabled: true,
							autowidth: true,
							borderless: true,
							id: SERVICE_DELETE_BUTTON,
							popup: SERVICE_DELETE_WINDOW,
						},
						{
							view: "icon",
							icon: "mdi mdi-play",
							tooltip: "시작",
							disabled: true,
							autowidth: true,
							borderless: true,
							id: SERVICE_START_BUTTON,
							popup: SERVICE_START_WINDOW,
						},
						{
							view: "icon",
							icon: "mdi mdi-stop",
							tooltip: "중지",
							disabled: true,
							autowidth: true,
							borderless: true,
							id: SERVICE_STOP_BUTTON,
							popup: SERVICE_STOP_WINDOW,
						},
						{
							view: "icon",
							icon: "mdi mdi-restart",
							tooltip: "재시작",
							disabled: true,
							autowidth: true,
							borderless: true,
							id: SERVICE_RESTART_BUTTON,
							popup: SERVICE_RESTART_WINDOW,
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
					id: SERVICE_TABLE,
					sort: "multi",
					select: "row",
					multiselect: true,
					resizeColumn: true,
					headerRowHeight: 25,
					checkboxRefresh: true,
					css: "webix_header_border",
					columns: [
						{ id: "Id", header: "Id", hidden: true },
						{ id: "Check", header: { content: "masterCheckbox" }, template: "{common.checkbox()}", width: 40 },
						{ id: "ServerName", header: "Server Name", width: 150, sort: "string" },
						{ id: "Name", header: "Service Name", fillspace: true, minWidth: 130, sort: "string" },
						{
							id: "State",
							header: "Status",
							width: 100,
							sort: "string",
							template: (obj) => {
								return getStatusToColor(obj.State);
							},
						},
						{ id: "ServiceType", header: "Service Type", width: 200, sort: "string" },
						{
							id: "CpuUsage",
							header: ["Cpu", "Usage"],
							width: 80,
							sort: "int",
							css: "number_align",
							format: webix.Number.numToStr({
								groupDelimiter: ",",
								groupSize: 3,
								decimalDelimiter: ".",
								decimalSize: 1,
							}),
						},
						{
							id: "MemoryTotal",
							header: [{ text: "RAM", colspan: 2 }, "Total"],
							width: 80,
							sort: "int",
							css: "number_align",
							template: (obj) => {
								return sizeToString(obj.MemoryTotal);
							},
						},
						{
							id: "MemoryUsed",
							header: ["", "Used"],
							width: 80,
							sort: "int",
							css: "number_align",
							template: (obj) => {
								return sizeToString(obj.MemoryUsed);
							},
						},
						{
							id: "ThreadCount",
							header: { text: "Thread<br>Count", css: "multiline" },
							width: 80,
							sort: "int",
							css: "number_align",
							format: webix.Number.numToStr({
								groupDelimiter: ",",
								groupSize: 3,
								decimalDelimiter: ".",
								decimalSize: 0,
							}),
						},
					],
					ready: function () {
						// apply sorting
						this.sort([
							{ by: "ServerName", dir: "asc" },
							{ by: "Name", dir: "asc" },
						]);
						// mark columns
						this.markSorting("ServerName", "asc");
						this.markSorting("Name", "asc", true);
						webix.extend(this, webix.ProgressBar);
						$$(SERVICE_CONTENT_MENU).attachTo($$(SERVICE_TABLE));
					},
					on: {
						onSelectChange: function () {
							unchecked();
							$$(SERVICE_DELETE_BUTTON).enable();
							$$(SERVICE_START_BUTTON).enable();
							$$(SERVICE_STOP_BUTTON).enable();
							$$(SERVICE_RESTART_BUTTON).enable();
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
							if (state) $$(SERVICE_TABLE).select(row, true);
							else $$(SERVICE_TABLE).unselect(row, true);
						},
					},
				},
			],
		};
	}
	init() {
		load();

		if ($$(SERVICE_ADD_WINDOW) == null)
			webix.ui({
				id: SERVICE_ADD_WINDOW,
				view: "popup",
				head: "서비스 추가",
				width: 350,
				body: {
					rows: [
						{ view: "label", label: "서비스 추가", align: "center" },
						{ view: "label", template: "<div class='popup_title_line' />", height: 2 },
						{
							view: "form",
							borderless: true,
							elementsConfig: {
								labelWidth: 100,
							},
							elements: [
								{ view: "text", label: "Name", name: "Name", required: true, invalidMessage: "대/소문자, 숫자, 특수문자(-, _)만 가능합니다." },
								{
									view: "richselect",
									label: "Server",
									name: "ServerId",
									required: true,
									options: loadServers(),
								},
								{ view: "richselect", label: "Service Type", name: "ServiceType", required: true, options: getServiceType() },
								{ view: "textarea", height: 200, label: "Description", labelPosition: "top", name: "Description" },
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
													addService(this.getFormView().getValues());
													this.getTopParentView().hide();
												}
											},
										},
									],
								},
							],
							rules: {
								Name: function (value) { return /([A-Za-z0-9-_]){1,}$/.test(value); },
								ServerId: webix.rules.isNotEmpty,
								ServiceType: webix.rules.isNotEmpty,
							},
						}]
				},
				on: {
					onShow: function () {
						this.getBody().getChildViews()[2].clear();
						var list = this.getBody().getChildViews()[2].getChildViews()[1].getPopup().getList();
						list.clearAll();
						list.parse(loadServers());
					}
				}
			});
		if ($$(SERVICE_DELETE_WINDOW) == null)
			webix.ui({
				id: SERVICE_DELETE_WINDOW,
				view: "popup",
				head: "서비스 삭제",
				width: 250,
				body: {
					rows: [
						{ view: "label", label: "서비스 삭제", align: "center" },
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
										deleteServices();
										this.getTopParentView().hide();
									},
								},
							],
						},
					],
				},
			});
		if ($$(SERVICE_START_WINDOW) == null)
			webix.ui({
				id: SERVICE_START_WINDOW,
				view: "popup",
				head: "서비스 시작",
				width: 250,
				body: {
					rows: [
						{ view: "label", label: "서비스 시작", align: "center" },
						{ view: "label", template: "<div class='popup_title_line' />", height: 2 },
						{ view: "label", label: "정말 실행하시겠습니까?", align: "center" },
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
									value: "실행",
									hotkey: "enter",
									click: function () {
										startServices();
										this.getTopParentView().hide();
									},
								},
							],
						},
					],
				},
			});
		if ($$(SERVICE_STOP_WINDOW) == null)
			webix.ui({
				id: SERVICE_STOP_WINDOW,
				view: "popup",
				head: "서비스 중지",
				width: 250,
				body: {
					rows: [
						{ view: "label", label: "서비스 중지", align: "center" },
						{ view: "label", template: "<div class='popup_title_line' />", height: 2 },
						{ view: "label", label: "정말 중지하시겠습니까?", align: "center" },
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
									value: "중지",
									hotkey: "enter",
									click: function () {
										stopServices();
										this.getTopParentView().hide();
									},
								},
							],
						},
					],
				},
			});
		if ($$(SERVICE_RESTART_WINDOW) == null)
			webix.ui({
				id: SERVICE_RESTART_WINDOW,
				view: "popup",
				head: "서비스 재시작",
				width: 250,
				body: {
					rows: [
						{ view: "label", label: "서비스 재시작", align: "center" },
						{ view: "label", template: "<div class='popup_title_line' />", height: 2 },
						{ view: "label", label: "정말 실행하시겠습니까?", align: "center" },
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
									value: "실행",
									hotkey: "enter",
									click: function () {
										restartServices();
										this.getTopParentView().hide();
									},
								},
							],
						},
					],
				},
			});
		if ($$(SERVICE_CONTENT_MENU) == null)
			webix.ui({
				view: "contextmenu",
				id: SERVICE_CONTENT_MENU,
				data: ["Start", "Stop", "Restart", "Delete"],
				on: {
					onItemClick: function (id) {
						switch (id) {
							case "Start":
								startServices();
								break;
							case "Stop":
								stopServices();
							case "Restart":
								restartServices();
							case "Delete":
								deleteServices();
						}
					},
				},
			});
	}
}

/**
 * 서비스 목록을 가져온다.
 */
function load() {
	webix
		.ajax()
		.get(SERVICE_URL)
		.then(
			function (data) {
				var response = data.json();
				if (response.Result == "Error") webix.message({ text: response.Message, type: "error", expire: 5000 });
				else {
					MyList = response.Data.Items;
					MyList.forEach(function (item) {
						item.ServerName = item.Server.Name;
					});
					$$(SERVICE_TABLE).unselectAll();
					$$(SERVICE_TABLE).clearAll();
					$$(SERVICE_TABLE).parse(MyList);
				}
			},
			function (error) {
				moveLogin("/#!/main/services");
			}
		);
}

/**
 * 서비스 목록을 새로고침한다.
 */
function reload(message) {
	if (message != null) webix.message({ text: message, type: "success", expire: 5000 });
	const DELAY = 1000;
	showProgressIcon(SERVICE_TABLE, DELAY);
	setTimeout(function () { load(); }, DELAY);
}

/**
 * 서비스 목록의 모든 체크를 해제한다.
 */
function unchecked() {
	MyList.forEach(function (item) {
		item.Check = false;
	});
}

/**
 * 서비스를 등록한다.
 * @param {GroupId:"Group Id", Name:"Name", ServerId:"Server Id", ServiceType:"Service Type", HaAction:"Ha Action", State:"State", Description:"Description"} form 서비스 정보
 */
function addService(form) {
	webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.post(SERVICE_URL, JSON.stringify(form))
		.then(
			function (data) {
				var response = data.json();
				if (response.Result == "Error") webix.message({ text: response.Message, type: "error", expire: 5000 });
				else reload("서비스 등록에 성공했습니다.");
			},
			function (error) {
				var response = JSON.parse(error.response);
				webix.message({ text: response.Message, type: "error", expire: 5000 });
			}
		);
}

/**
 * 목록에서 선택된 서비스를 삭제한다.
 */
function deleteServices() {
	var items = $$(SERVICE_TABLE).getSelectedItem();
	if (items == null) {
		webix.alert({ type: "error", text: "서비스를 선택해야 합니다." });
	} else if (Array.isArray(items)) {
		var result = false;
		items.forEach(function (item) {
			if (deleteService(item)) result = true;
		});
		if (result) reload("선택한 서비스들을 삭제했습니다.");
	} else if (deleteService(items)) reload("선택한 서비스를 삭제했습니다.");
}

/**
 * 특정 서비스를 삭제한다.
 * @returns 삭제 결과
 */
function deleteService(item) {
	if (checkAgentService(item)) return false;

	return webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.del(`${SERVICE_URL}/${item.Id}`)
		.then(
			function (data) {
				var response = data.json();
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

/**
 * 목록에서 선택된 서비스를 시작한다.
 */
function startServices() {
	var items = $$(SERVICE_TABLE).getSelectedItem();
	if (items == null) {
		webix.alert({ type: "error", text: "서비스를 선택해야 합니다." });
	} else if (Array.isArray(items)) {
		var result = false;
		items.forEach(function (item) {
			if (startService(item)) result = true;
		});
		if (result) reload("선택한 서비스를 시작했습니다.");
	} else if (startService(items)) reload("선택한 서비스들을 시작했습니다.");
}

/**
 * 특정 서비스를 시작한다.
 * @returns 시작 결과
 */
function startService(item) {
	if (checkAgentService(item)) return false;

	return webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.post(`${SERVICE_URL}/${item.Id}/Start`)
		.then(
			function (data) {
				var response = data.json();
				if (response.Result == "Error") {
					webix.message({ text: response.Message, type: "error", expire: 5000 });
					return false;
				} else return true;
			},
			function (error) {
				var response = JSON.parse(error.response);
				webix.message({ text: response.Message, type: "error", expire: 5000 });
				return false;
			}
		);
}

/**
 * 목록에서 선택된 서비스를 중지한다.
 */
function stopServices() {
	var items = $$(SERVICE_TABLE).getSelectedItem();
	if (items == null) {
		webix.alert({ type: "error", text: "서비스를 선택해야 합니다." });
	} else if (Array.isArray(items)) {
		var result = false;
		items.forEach(function (item) {
			if (stopService(item)) result = true;
		});
		if (result) reload("선택한 서비스들을 중지했습니다.");
	} else if (stopService(items)) reload("선택한 서비스를 중지했습니다.");
}

/**
 * 특정 서비스를 중지한다.
 * @returns 중지 결과
 */
function stopService(item) {
	if (checkAgentService(item)) return false;

	return webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.post(`${SERVICE_URL}/${item.Id}/Stop`)
		.then(
			function (data) {
				var response = data.json();
				if (response.Result == "Error") {
					webix.message({ text: response.Message, type: "error", expire: 5000 });
					return false;
				} else return true;
			},
			function (error) {
				var response = JSON.parse(error.response);
				webix.message({ text: response.Message, type: "error", expire: 5000 });
				return false;
			}
		);
}
/**
 * 목록에서 선택된 서비스를 재시작한다.
 */
function restartServices() {
	var items = $$(SERVICE_TABLE).getSelectedItem();
	if (items == null) webix.alert({ type: "error", text: "서비스를 선택해야 합니다." });
	else if (Array.isArray(items)) {
		var result = false;
		items.forEach(function (item) {
			if (restartService(item)) result = true;
		});
		if (result) reload("선택한 서비스들을 재시작했습니다.");
	} else if (restartService(items)) reload("선택한 서비스를 재시작했습니다.");
}

/**
 * 특정 서비스를 재시작한다.
 * @returns 재시작 결과
 */
function restartService(item) {
	return webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.post(`${SERVICE_URL}/${item.Id}/Restart`)
		.then(
			function (data) {
				var response = data.json();
				if (response.Result == "Error") {
					webix.message({ text: response.Message, type: "error", expire: 5000 });
					return false;
				} else return true;
			},
			function (error) {
				var response = JSON.parse(error.response);
				webix.message({ text: response.Message, type: "error", expire: 5000 });
				return false;
			}
		);
}

/**
 * Agent 서비스는 재시작만 가능하도록 한다.
 * @param {Service} item 서비스 정보
 * @returns {boolean} Agent 서비스 여부
 */
function checkAgentService(item) {
	if (item.ServiceType == "ksanAgent") {
		webix.message({ type: "debug", text: "Agent 서비스는 재시작만 가능합니다." });
		return true;
	} else return false;
}