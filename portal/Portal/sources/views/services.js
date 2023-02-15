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
const SERVICE_START_WINDOW = "service_start_window";
const SERVICE_STOP_WINDOW = "service_stop_window";
const SERVICE_RESTART_WINDOW = "service_restart_window";
const SERVICE_CONTENT_MENU = "service_content_menu";
const SERVICE_TABLE = "service";
const NUMBER_FORMAT = webix.Number.numToStr({
	groupDelimiter: ",",
	groupSize: 3,
	decimalDelimiter: ".",
	decimalSize: 0,
});

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
							autowidth: true,
							borderless: true,
							popup: SERVICE_DELETE_WINDOW,
						},
						{
							view: "icon",
							icon: "mdi mdi-play",
							tooltip: "시작",
							autowidth: true,
							borderless: true,
							popup: SERVICE_START_WINDOW,
						},
						{
							view: "icon",
							icon: "mdi mdi-stop",
							tooltip: "중단",
							autowidth: true,
							borderless: true,
							popup: SERVICE_STOP_WINDOW,
						},
						{
							view: "icon",
							icon: "mdi mdi-replay",
							tooltip: "재시작",
							autowidth: true,
							borderless: true,
							popup: SERVICE_RESTART_WINDOW,
						},
						{ view: "spacer" },
						{
							view: "icon",
							icon: "mdi mdi-reload",
							tooltip: "새로고침",
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
		if ($$(SERVICE_ADD_WINDOW) == null)
			webix.ui({
				id: SERVICE_ADD_WINDOW,
				view: "popup",
				head: "Add",
				width: 350,
				body: {
					rows: [
						{ view: "label", label: "서비스 추가", align: "center" },
						{ view: "label", template:"<div class='popup_title_line' />", height:2 },
						{
							view: "form",
							borderless: true,
							elementsConfig: {
								labelWidth: 100,
							},
							elements: [
								// { view: "text", label: "GroupId", name: "GroupId" },
								{ view: "text", label: "Name", name: "Name" },
								{
									view: "richselect",
									label: "Server",
									name: "ServerId",
									options: {
										body: {
											url: function () {
												return loadServers();
											},
										},
									},
								},
								{ view: "richselect", label: "Service Type", name: "ServiceType", options: getServiceType() },
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
												} else webix.message({ type: "error", expire: 5000, text: "Form data is invalid" });
											},
										},
									],
								},
							],
							rules: {
								Name: webix.rules.isNotEmpty,
								ServerId: webix.rules.isNotEmpty,
								ServiceType: webix.rules.isNotEmpty,
							},
						}]
				},
			});
		if ($$(SERVICE_DELETE_WINDOW) == null)
			webix.ui({
				id: SERVICE_DELETE_WINDOW,
				view: "popup",
				head: "Delete",
				width: 250,
				body: {
					rows: [
						{ view: "label", label: "서비스 삭제", align: "center" },
						{ view: "label", css:"popup_title_line" },
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
				head: "Start",
				width: 250,
				body: {
					rows: [
						{ view: "label", label: "서비스 시작", align: "center" },
						{ view: "label", css:"popup_title_line" },
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
				head: "Stop",
				width: 250,
				body: {
					rows: [
						{ view: "label", label: "서비스 실행", align: "center" },
						{ view: "label", css:"popup_title_line" },
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
										stopServices();
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
				head: "Restart",
				width: 250,
				body: {
					rows: [
						{ view: "label", label: "서비스 재시작", align: "center" },
						{ view: "label", css:"popup_title_line" },
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
		set();
	}
}
function set() {
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
					$$(SERVICE_TABLE).parse(MyList);
				}
			},
			function (error) {
				var response = JSON.parse(error.response);
				webix.message({ text: response.Message, type: "error", expire: 5000 });
				moveLogin("/#!/main/services");
			}
		);
}
function unchecked() {
	MyList.forEach(function (item) {
		item.Check = false;
	});
}

/**
 * 서비스를 등록한다.
 * @param form json:{GroupId:"Group Id", Name:"Name", ServerId:"Server Id", ServiceType:"Service Type", HaAction:"Ha Action", State:"State", Description:"Description"}
 */
function addService(form) {
	showProgressIcon(SERVICE_TABLE);
	webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.post(SERVICE_URL, JSON.stringify(form))
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
 * 목록에서 선택된 서비스를 삭제한다.
 */
function deleteServices() {
	var items = $$(SERVICE_TABLE).getSelectedItem();
	if (items == null) {
		webix.alert({ type: "error", text: "서비스를 선택해야 합니다." });
	} else if (Array.isArray(items)) {
		var result = false;
		items.forEach(function (item) {
			if (deleteService(item.Id)) result = true;
		});
		if (result) window.location.reload(true);
	} else {
		if (deleteService(items.Id)) window.location.reload(true);
	}
}

/**
 * 특정 서비스를 삭제한다.
 * @returns 성공 / 실패 여부
 */
function deleteService(id) {
	showProgressIcon(SERVICE_TABLE);
	return webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.del(`${SERVICE_URL}/${id}`)
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
 * 목록에서 선택된 서비스를 시작한다.
 */
function startServices() {
	var items = $$(SERVICE_TABLE).getSelectedItem();
	if (items == null) {
		webix.alert({ type: "error", text: "서비스를 선택해야 합니다." });
	} else if (Array.isArray(items)) {
		var result = false;
		items.forEach(function (item) {
			if (startService(item.Id)) result = true;
		});
		if (result) window.location.reload(true);
	} else {
		if (startService(items.Id)) window.location.reload(true);
	}
}

/**
 * 특정 서비스를 시작한다.
 * @returns 성공 / 실패 여부
 */
function startService(id) {
	showProgressIcon(SERVICE_TABLE);
	return webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.post(`${SERVICE_URL}/${id}/Start`)
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
			if (stopService(item.Id)) result = true;
		});
		if (result) window.location.reload(true);
	} else {
		if (stopService(items.Id)) window.location.reload(true);
	}
}

/**
 * 특정 서비스를 중지한다.
 * @returns 성공 / 실패 여부
 */
function stopService(Id) {
	showProgressIcon(SERVICE_TABLE);
	return webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.post(`${SERVICE_URL}/${id}/Stop`)
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
	if (items == null) {
		webix.alert({ type: "error", text: "서비스를 선택해야 합니다." });
	} else if (Array.isArray(items)) {
		var result = false;
		items.forEach(function (item) {
			if (restartService(item.Id)) result = true;
		});
		if (result) window.location.reload(true);
	} else {
		if (restartService(items.Id)) window.location.reload(true);
	}
}

/**
 * 특정 서비스를 재시작한다.
 * @returns 성공 / 실패 여부
 */
function restartService(Id) {
	showProgressIcon(SERVICE_TABLE);
	return webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.post(`${SERVICE_URL}/${id}/Restart`)
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
