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
import { showProgressIcon } from "../../models/utils/showProgressIcon";
import { loadServers } from "../../models/load/loadServers";
import { loadDiskPools } from "../../models/load/loadDiskPools";
import { getDiskStateToColor } from "../../models/utils/getDiskStateToColor";
import { getDiskMode } from "../../models/enum/enum-disk-mode";
import { getDiskModeToValue } from "../../models/enum/enum-disk-mode";
import { moveLogin } from "../../models/utils/moveLogin";

const DISKS_URL = "/api/v1/Disks";
const DISKS_TITLE = "disk_title";
const DISKS_ADD_WINDOW = "disk_add_window";
const DISKS_DELETE_WINDOW = "disk_delete_window";
const DISKS_START_WINDOW = "disk_start_window";
const DISKS_STOP_WINDOW = "disk_stop_window";
const DISKS_RW_WINDOW = "disk_rw_window";
const DISKS_DELETE_BUTTON = "disk_delete_button";
const DISKS_START_BUTTON = "disk_start_button";
const DISKS_STOP_BUTTON = "disk_stop_button";
const DISKS_RW_BUTTON = "disk_rw_button";
const DISKS_TABLE = "disk";

var MyList = [];

export default class DiskView extends JetView {
	config() {
		return {
			rows: [
				{
					view: "toolbar",
					paddingX: 20,
					elements: [{ view: "label", id: DISKS_TITLE, label: "All Disks", height: 0 }],
					height: 50,
					borderless: true,
				},
				{
					height: 35,
					type: "clean",
					css: "default_layout",
					cols: [
						{
							view: "icon",
							icon: "mdi mdi-plus",
							tooltip: "추가",
							autowidth: true,
							borderless: true,
							popup: DISKS_ADD_WINDOW,
						},
						{
							id: DISKS_DELETE_BUTTON,
							view: "icon",
							icon: "mdi mdi-delete",
							tooltip: "삭제",
							disabled: true,
							autowidth: true,
							borderless: true,
							popup: DISKS_DELETE_WINDOW,
						},
						{
							id: DISKS_START_BUTTON,
							view: "icon",
							icon: "mdi mdi-play",
							tooltip: "시작",
							disabled: true,
							autowidth: true,
							borderless: true,
							popup: DISKS_START_WINDOW,
						},
						{
							id: DISKS_STOP_BUTTON,
							view: "icon",
							icon: "mdi mdi-stop",
							tooltip: "중단",
							disabled: true,
							autowidth: true,
							borderless: true,
							popup: DISKS_STOP_WINDOW,
						},
						{
							id: DISKS_RW_BUTTON,
							view: "icon",
							icon: "mdi mdi-read",
							tooltip: "RW 변경",
							disabled: true,
							autowidth: true,
							borderless: true,
							popup: DISKS_RW_WINDOW,
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
					id: DISKS_TABLE,
					sort: "multi",
					select: "row",
					tooltip: true,
					scroll: "auto",
					multiselect: true,
					resizeColumn: true,
					headerRowHeight: 25,
					checkboxRefresh: true,
					css: "webix_header_border",
					columns: [
						{ id: "Id", header: "Id", hidden: true },
						{ id: "Check", header: { content: "masterCheckbox" }, template: "{common.checkbox()}", width: 40 },
						{ id: "DiskPoolId", header: "DiskPoolId", hidden: true },
						{ id: "ServerName", header: "Server Name", width: 130, sort: "string" },
						{ id: "Name", header: "Disk Name", fillspace: true, minWidth: 130, sort: "string" },
						{ id: "Path", header: "Path", width: 150, sort: "string" },
						{
							header: "Mode",
							sort: "string",
							id: "RwMode",
							width: 100,
							tooltip: false,
							template: (obj) => {
								var Name = "";
								switch (obj.RwMode) {
									case "ReadOnly":
										Name = "Read Only";
										break;
									case "ReadWrite":
										Name = "Read/Write";
										break;
									case "Maintenance":
										Name = "Maintenance";
										break;
								}
								return Name;
							},
						},
						{
							id: "State",
							header: "Status",
							sort: "string",
							width: 80,
							tooltip: false,
							template: (obj) => getDiskStateToColor(obj),
						},
						{ id: "DiskPoolName", header: "DiskPool Name", width: 150, sort: "string" },
						{
							id: "CapacityUsage",
							header: "Capacity Usage",
							width: 150,
							tooltip: (obj) => {
								return `TotalSize : ${sizeToString(obj.TotalSize)}<br>UsedSize : ${sizeToString(obj.UsedSize)}`;
							},
							template: (obj) => `
							<div class="main_disk_list">
								<div class="main_disk_usage">
									<span style="color:#1395F5;"> ${sizeToString(obj.UsedSize)} / </span>
									<span> ${sizeToString(obj.TotalSize)} </span>
								</div>
							</div>
							<div class="main_disk_progress_bar">
								<div class="progress_result ${(obj.UsedSize / obj.TotalSize) * 100 > 85 ? "Week" : "Good"}" style="width:${(obj.UsedSize / obj.TotalSize) * 100 + "%"}"></div>
							</div>`,
						},
						{
							id: "Read",
							header: [{ text: "Disk", colspan: 2 }, "Read"],
							width: 80,
							sort: "string",
							css: "number_align",
							template: (obj) => {
								return sizeToString(obj.Read);
							},
						},
						{
							id: "Write",
							header: ["", "Write"],
							width: 80,
							sort: "string",
							css: "number_align",
							template: (obj) => {
								return sizeToString(obj.Write);
							},
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
					},
					on: {
						onSelectChange: function () {
							unchecked();
							$$(DISKS_DELETE_BUTTON).enable();
							$$(DISKS_START_BUTTON).enable();
							$$(DISKS_STOP_BUTTON).enable();
							$$(DISKS_RW_BUTTON).enable();

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
							if (state) $$(DISKS_TABLE).select(row, true);
							else $$(DISKS_TABLE).unselect(row, true);
						},
					},
				},
			],
		};
	}
	init() {
		const table = this.$$(DISKS_TABLE);
		const disks = this.$$(DISKS_TITLE);
		this.on(this.app, "diskpool:select", (diskpool) => {
			table.filter(function (obj) {
				return obj.DiskPoolId === diskpool.Id;
			});
			disks.setValue(diskpool.Name);
		});
		this.on(this.app, "diskpool:unselect", () => {
			table.filter("");
			disks.setValue("All Disks");
		});

		if ($$(DISKS_ADD_WINDOW) == null)
			webix.ui({
				id: DISKS_ADD_WINDOW,
				view: "popup",
				head: "디스크 추가",
				width: 350,
				body: {
					rows: [
						{ view: "label", label: "디스크 추가", align: "center" },
						{ view: "label", template: "<div class='popup_title_line' />", height: 2 },
						{
							view: "form",
							borderless: true,
							elementsConfig: {
								labelWidth: 100,
							},
							elements: [
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
								{ view: "text", label: "Name", name: "Name" },
								{ view: "text", label: "Path", name: "Path" },
								{
									view: "richselect",
									label: "Disk Pool",
									name: "DiskPoolId",
									options: {
										body: {
											url: function () {
												return loadDiskPools();
											},
										},
									},
								},
								{ view: "textarea", label: "Description", name: "Description", height: 200, labelPosition: "top" },
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
													addDisk(this.getFormView().getValues());
												} else webix.alert({ type: "error", text: "Form data is invalid" });
											},
										},
									],
								},
							],
							rules: {
								ServerId: webix.rules.isNotEmpty,
								Name: webix.rules.isNotEmpty,
								Path: webix.rules.isNotEmpty,
							},
						},
					]
				},
				on: {
					onShow: function () {
						this.getBody().getChildViews()[2].clear();
					}
				}
			});

		if ($$(DISKS_DELETE_WINDOW) == null)
			webix.ui({
				id: DISKS_DELETE_WINDOW,
				view: "popup",
				head: "디스크 삭제",
				width: 250,
				body: {
					rows: [
						{ view: "label", label: "디스크 삭제", align: "center" },
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
										deleteDisks();
									},
								},
							],
						},
					],
				},
			});

		if ($$(DISKS_START_WINDOW) == null)
			webix.ui({
				id: DISKS_START_WINDOW,
				view: "popup",
				head: "디스크 시작",
				width: 250,
				body: {
					rows: [
						{ view: "label", label: "디스크 시작", align: "center" },
						{ view: "label", template: "<div class='popup_title_line' />", height: 2 },
						{ view: "label", label: "정말 시작하시겠습니까?", align: "center" },
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
									value: "시작",
									hotkey: "enter",
									click: function () {
										startDisks();
									},
								},
							],
						},
					],
				},
			});

		if ($$(DISKS_STOP_WINDOW) == null)
			webix.ui({
				id: DISKS_STOP_WINDOW,
				view: "popup",
				head: "디스크 정지",
				width: 250,
				body: {
					rows: [
						{ view: "label", label: "디스크 정지", align: "center" },
						{ view: "label", template: "<div class='popup_title_line' />", height: 2 },
						{ view: "label", label: "정말 정지하시겠습니까?", align: "center" },
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
									value: "정지",
									hotkey: "enter",
									click: function () {
										stopDisks();
									},
								},
							],
						},
					],
				},
			});
		if ($$(DISKS_RW_WINDOW) == null)
			webix.ui({
				id: DISKS_RW_WINDOW,
				view: "popup",
				head: "Disk Mode 변경",
				width: 350,
				body: {
					rows: [
						{ view: "label", label: "Disk Mode 변경", align: "center" },
						{ view: "label", template: "<div class='popup_title_line' />", height: 2 },
						{
							view: "form",
							borderless: true,
							elementsConfig: {
								labelWidth: 100,
							},
							elements: [
								{ view: "richselect", label: "RW Mode", name: "rwMode", options: getDiskMode(), value: "Read Only", },
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
											value: "변경",
											hotkey: "enter",
											click: function () {
												changeDisks(this.getFormView().getValues());
											},
										},
									],
								},]
						},
					]
				},
			});
		set();
	}
}
function set() {
	webix
		.ajax()
		.get(DISKS_URL)
		.then(
			function (data) {
				var response = data.json();
				if (response.Result == "Error") webix.message({ text: response.Message, type: "error", expire: 5000 });
				else {
					MyList = response.Data.Items;
					$$(DISKS_TABLE).parse(MyList);
				}
			},
			function (error) {
				var response = JSON.parse(error.response);
				webix.message({ text: response.Message, type: "error", expire: 5000 });
				moveLogin("/#!/main/diskpools");
			}
		);
}
function unchecked() {
	MyList.forEach(function (item) {
		item.Check = false;
	});
}

/**
 * 디스크를 등록한다.
 * @param form json:{ServerId:"Server Id", DiskPoolId:"Disk Pool Id", Name:"Name", State:"State", RwMode:"RwMode", Description:"Description"}
 */
function addDisk(form) {
	showProgressIcon(DISKS_TABLE);
	webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.post(`${DISKS_URL}/${form.ServerId}`, JSON.stringify(form))
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
 * 디스크 목록에서 선택한 디스크를 삭제한다.
 */
function deleteDisks() {
	var items = $$(DISKS_TABLE).getSelectedItem();
	if (items == null) webix.message({ text: "디스크를 선택해야 합니다.", type: "error", expire: 5000 });
	else if (Array.isArray(items)) {
		var result = false;
		items.forEach(function (item) {
			if (deleteDisk(item.Id)) result = true;
		});
		if (result) window.location.reload(true);
	} else {
		deleteDisk(items.Id);
		window.location.reload(true);
	}
}
/**
 * 디스크를 삭제한다.
 * @param {Guid} id Disk Id
 */
function deleteDisk(id) {
	showProgressIcon(DISKS_TABLE);
	return webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.del(`${DISKS_URL}/${id}`)
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
 * 디스크 목록에서 선택한 디스크를 시작한다.
 */
function startDisks() {
	var items = $$(DISKS_TABLE).getSelectedItem();
	if (items == null) webix.message({ text: "디스크를 선택해야 합니다.", type: "error", expire: 5000 });
	else if (Array.isArray(items)) {
		var result = false;
		items.forEach(function (item) {
			if (startDisk(item.Id)) result = true;
		});
		if (result) window.location.reload(true);
	} else {
		if (startDisk(items.Id)) window.location.reload(true);
	}
}

/**
 * 디스크를 시작한다.
 * @param {Guid} id DiskId
 */
function startDisk(id) {
	showProgressIcon(DISKS_TABLE);
	return webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.put(`${DISKS_URL}/${id}/State/Good`)
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
 * 디스크 목록에서 선택한 디스크를 중지한다.
 */
function stopDisks() {
	var items = $$(DISKS_TABLE).getSelectedItem();
	if (items == null) webix.message({ text: "디스크를 선택해야 합니다.", type: "error", expire: 5000 });
	else if (Array.isArray(items)) {
		var result = false;
		items.forEach(function (item) {
			if (stopDisk(item.Id)) result = true;
		});
		if (result) window.location.reload(true);
	} else {
		if (stopDisk(items.Id)) window.location.reload(true);
	}
}

/**
 * 디스크를 중지한다.
 * @param {Guid} id Disk Id
 */
function stopDisk(id) {
	showProgressIcon(DISKS_TABLE);
	return webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.put(`${DISKS_URL}/${id}/State/Stop`)
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
 * 디스크 목록에서 선택한 디스크의 RW모드를 변경한다.
 */
function changeDisks(form) {
	var items = $$(DISKS_TABLE).getSelectedItem();
	var rwMode = getDiskModeToValue(form.rwMode);
	if (items == null) webix.message({ text: "디스크를 선택해야 합니다.", type: "error", expire: 5000 });
	else if (Array.isArray(items)) {
		var result = false;
		items.forEach(function (item) {
			if (item.RwMode != rwMode) if (changeDisk(item.Id, rwMode)) result = true;
		});
		if (result) window.location.reload(true);
	} else {
		if (items.RwMode != rwMode) {
			if (changeDisk(items.Id, rwMode)) window.location.reload(true);
		}
		else
			webix.message({ text: `해당 디스크는 이미 ${rwMode} 입니다.`, type: "debug", expire: 5000 });
	}
}
/**
 * 
 * @param {Guid} id DIsk Id
 * @param {DiskRwMode} rwMode Disk RW Modw
 * @returns 
 */
function changeDisk(id, rwMode) {
	showProgressIcon(DISKS_TABLE);
	return webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.put(`${DISKS_URL}/${id}/RwMode/${rwMode}`)
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
