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
import { loadServers } from "../models/load/loadServers";
import { loadDiskPools } from "../models/load/loadDiskPools";
import { getDiskStateToColor } from "../models/utils/getDiskStateToColor";
import { moveLogin } from "../models/utils/moveLogin";

const MY_URL = "/api/v1/Disks";
const MY_ADD_WINDOW = "disk_add_window";
const MY_DELETE_WINDOW = "disk_delete_window";
const MY_START_WINDOW = "disk_start_window";
const MY_STOP_WINDOW = "disk_stop_window";
const MY_RESTART_WINDOW = "disk_restart_window";
const MY_CONTENT_MENU = "disk_content_menu";
const MY_TABLE = "disk";
const NUMBER_FORMAT = webix.Number.numToStr({
	groupDelimiter: ",",
	groupSize: 3,
	decimalDelimiter: ".",
	decimalSize: 0,
});

var MyList = [];

export default class DiskView extends JetView {
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
						{
							view: "button",
							type: "icon",
							icon: "mdi mdi-play",
							label: "시작",
							autowidth: true,
							borderless: true,
							popup: MY_START_WINDOW,
						},
						{
							view: "button",
							type: "icon",
							icon: "mdi mdi-stop",
							label: "중단",
							autowidth: true,
							borderless: true,
							popup: MY_STOP_WINDOW,
						},
						{
							view: "button",
							type: "icon",
							icon: "mdi mdi-replay",
							label: "재시작",
							autowidth: true,
							borderless: true,
							popup: MY_RESTART_WINDOW,
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
					headerRowHeight: 25,
					checkboxRefresh: true,
					css: "webix_header_border",
					columns: [
						{ id: "Id", header: "Id", hidden: true },
						{ id: "Check", header: { content: "masterCheckbox" }, template: "{common.checkbox()}", width: 40 },
						{ id: "DiskPoolId", header: "DiskPoolId", hidden: true },
						{ id: "ServerName", header: "Server Name", width: 150, sort: "string" },
						{ id: "Name", header: "Disk Name", fillspace: true, sort: "string" },
						{ id: "Path", header: "Path", width: 150, sort: "string" },
						{
							id: "RwMode",
							header: "Mode",
							width: 100,
							sort: "string",
							template: (obj) => {
								var Name = "";
								switch (obj.RwMode) {
									case "ReadOnly":
										Name = "Read Only";
										break;
									case "ReadWrite":
										Name = "Read/Write";
										break;
								}
								return Name;
							},
						},
						{
							id: "State",
							header: "Status",
							width: 100,
							sort: "string",
							template: (obj) => getDiskStateToColor(),
						},
						{ id: "DiskPoolName", header: "DiskPool Name", width: 200, sort: "string" },
						// {
						// 	id: "TotalSize",
						// 	header: "Total Size",
						// 	width: 100,
						// 	sort: "string",
						// 	css: "number_align",
						// 	template: (obj) => {
						// 		return sizeToString(obj.TotalSize);
						// 	},
						// },
						// {
						// 	id: "UsedSize",
						// 	header: "Used Size",
						// 	width: 100,
						// 	sort: "string",
						// 	css: "number_align",
						// 	template: (obj) => {
						// 		return sizeToString(obj.UsedSize);
						// 	},
						// },
						{
							id: "TotalSize",
							header: "Capacity Usage",
							width: 150,
							template: (obj) => "<svg viewBox='0 0 140 50'><rect y='20' rx='5' ry='5' width='140' height='11' style='fill:#CCD7E6;' />" + "<rect y='20' rx='5' ry='5' width='" + obj.compl * 1.4 + "' height='11' style='fill: #94a1b3;' />" + "<rect y='20' rx='5' ry='5' width='" + obj.achiev * 1.4 + "' height='11' style='fill:#55CD97;' />" + "Sorry, your browser does not support inline SVG." + "</svg>",
						},
						{
							id: "Read",
							header: [{ text: "Disk", colspan: 2 }, "Read"],
							width: 100,
							sort: "string",
							css: "number_align",
							template: (obj) => {
								return sizeToString(obj.Read);
							},
						},
						{
							id: "Write",
							header: ["", "Write"],
							width: 100,
							sort: "string",
							css: "number_align",
							template: (obj) => {
								return sizeToString(obj.Write);
							},
						},
						// {
						// 	id: "TotalInode",
						// 	header: "Total Inode",
						// 	width: 100,
						// 	sort: "int",
						// 	css: "number_align",
						// 	format: NUMBER_FORMAT,
						// },
						// {
						// 	id: "UsedInode",
						// 	header: "Used Inode",
						// 	width: 100,
						// 	sort: "int",
						// 	css: "number_align",
						// 	format: NUMBER_FORMAT,
						// },
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
						$$(MY_CONTENT_MENU).attachTo($$(MY_TABLE));
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
				width: 350,
				body: {
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
			});

		if ($$(MY_DELETE_WINDOW) == null)
			webix.ui({
				id: MY_DELETE_WINDOW,
				view: "popup",
				head: "Delete",
				width: 250,
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
										deleteDisk();
									},
								},
							],
						},
					],
				},
			});
		if ($$(MY_CONTENT_MENU) == null)
			webix.ui({
				view: "contextmenu",
				id: MY_CONTENT_MENU,
				data: ["Stop", "ReadOnly"],
				on: {
					onItemClick: function (id) {
						var context = this.getContext();
						var list = context.obj;
						var listId = context.id;
						webix.message("List item: <i>" + list.getItem(listId).title + "</i> <br/>Context menu item: <i>" + this.getItem(id).value + "</i>");
					},
				},
			});
		set();
	}
}
function set() {
	webix
		.ajax()
		.get(MY_URL)
		.then(
			function (data) {
				var response = data.json();
				if (response.Result == "Error") webix.message({ text: response.Message, type: "error", expire: 5000 });
				else {
					MyList = response.Data.Items;
					$$(MY_TABLE).parse(MyList);
				}
			},
			function (error) {
				var response = JSON.parse(error.response);
				webix.message({ text: response.Message, type: "error", expire: 5000 });
				moveLogin("/#!/main/disks");
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
	var AddUrl = MY_URL + "/" + form.ServerId;
	showProgressIcon(MY_TABLE);
	webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.post(AddUrl, JSON.stringify(form))
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
function deleteDisk() {
	var item = $$(MY_TABLE).getSelectedItem();
	if (item == null) {
		webix.alert({ type: "error", text: "디스크를 선택해야 합니다." });
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

/**
 * 디스크 목록에서 선택한 디스크를 시작한다.
 */
function startDisk() {
	var item = $$(MY_TABLE).getSelectedItem();
	if (item == null) {
		webix.alert({ type: "error", text: "디스크를 선택해야 합니다." });
		return;
	}
	var StartUrl = MY_URL + "/" + item.Id + "/State/Good";

	showProgressIcon(MY_TABLE);

	webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.put(StartUrl)
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
 * 디스크 목록에서 선택한 디스크를 중지한다.
 */
function stopDisk() {
	var item = $$(MY_TABLE).getSelectedItem();
	if (item == null) {
		webix.alert({ type: "error", text: "디스크를 선택해야 합니다." });
		return;
	}
	var StopUrl = MY_URL + "/" + item.Id + "/State/Stop";

	showProgressIcon(MY_TABLE);

	webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.put(StopUrl)
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
