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
import { getDiskPoolType } from "../../models/enum/enum-disk-pool-type";
import { showProgressIcon } from "../../models/utils/showProgressIcon";
import { getStatusToColor } from "../../models/utils/getStatusToColor";
import { getReplicationType } from "../../models/utils/getReplicationType";
import { getDiskPoolReplicationType } from "../../models/enum/enum-disk-pool-replication-type";
import { drawCurve } from "../../models/draw/drawCurve";
import { moveLogin } from "../../models/utils/moveLogin";

const DISKPOOL_URL = "/api/v1/DiskPools";
const DISKPOOL_ADD_WINDOW = "diskpool_add_window";
const DISKPOOL_DELETE_WINDOW = "diskpool_delete_window";
const DISKPOOL_DELETE_BUTTON = "diskpool_delete_button";
const DISKPOOL_TO_DISK_WINDOW = "diskpool_to_disk_window";
const DISKPOOL_TO_DISK_BUTTON = "diskpool_to_disk_button";
const DISKPOOL_TO_DEFAULT_WINDOW = "diskpool_to_default_window";
const DISKPOOL_TO_DEFAULT_BUTTON = "diskpool_to_default_button";
const DISKPOOL_TABLE = "diskpool";
const SUB_DISK_TABLE = "sub_disk";

var Disks = [];

export default class DiskPoolView extends JetView {
	config() {
		return {
			rows: [
				{
					view: "toolbar",
					paddingX: 20,
					elements: [{ view: "label", label: "Diskpools", height: 0 }],
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
							popup: DISKPOOL_ADD_WINDOW,
						},
						{
							id: DISKPOOL_DELETE_BUTTON,
							disabled: true,
							view: "icon",
							icon: "mdi mdi-delete",
							tooltip: "삭제",
							autowidth: true,
							borderless: true,
							popup: DISKPOOL_DELETE_WINDOW,
						},
						{
							id: DISKPOOL_TO_DISK_BUTTON,
							disabled: true,
							view: "icon",
							icon: "mdi mdi-harddisk-plus",
							tooltip: "디스크 관리",
							autowidth: true,
							borderless: true,
							popup: DISKPOOL_TO_DISK_WINDOW,
						},
						{
							id: DISKPOOL_TO_DEFAULT_BUTTON,
							disabled: true,
							view: "icon",
							icon: "mdi mdi-star",
							tooltip: "기본 디스크풀로 설정",
							autowidth: true,
							borderless: true,
							popup: DISKPOOL_TO_DEFAULT_WINDOW,
						},
						{ view: "spacer" },
						{
							view: "icon",
							icon: "mdi mdi-close",
							tooltip: "선택해제",
							autowidth: true,
							borderless: true,
							click: function () {
								$$(DISKPOOL_TABLE).callEvent("onUnselect");
							},
						},
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
					view: "list",
					borderless: true,
					width: 450,
					xCount: 1,
					id: DISKPOOL_TABLE,
					select: true,
					scroll: "auto",
					type: {
						height: 100,
						template: (obj) => {
							const totalUnits = obj.TotalSize;
							const takenUnits = totalUnits > 0 ? Math.floor((obj.UsedSize / totalUnits) * 100) : 0;
							const curve = drawCurve(50, 50, 25, takenUnits);
							return `
							<div>
								<svg class="diskpools_unit" height="100" width="100">
								<circle cx="50" cy="50" r="25" stroke="#DADEE0" stroke-width="15" fill="none" />
								<path d="${curve}" stroke="#1395F5" stroke-width="15" fill="none" />
								Sorry, your browser does not support inline SVG.
								</svg>
								<div class="diskpools_title">
									<span class="main_diskpools_rate">${takenUnits}%</span>
								</div>
								<div class="main_diskpools_list">
									<span> ${obj.Name} </span>
									${getStatusToColor(obj)} ${obj.DefaultDiskPool ? "<span class='default_marker'>Default</span>" : ""}
									<br>
									<span style="display: inline-block; width:140px;">
										<span style="color:#1395F5;"> ${sizeToString(obj.UsedSize)} / </span>
										<span> ${sizeToString(obj.UsedSize + obj.FreeSize)} </span>
									</span>
									<span style='font-weight:normal;'> Tolerance : </span>
									<span>${getReplicationType(obj)}</span>
								</div>
							</div>`;
						},
					},
					tooltip: {
						template: (obj) => `Total Size : ${sizeToString(obj.TotalSize)}<br>Used Size : ${sizeToString(obj.UsedSize)}<br>Tolerance : ${getReplicationType(obj)}<br>DiskPool Type : ${obj.DiskPoolType}`,
					},
					url: function () {
						return webix
							.ajax()
							.get(DISKPOOL_URL)
							.then(
								function (data) {
									var response = data.json();
									if (response.Result == "Error") {
										webix.message({ text: response.Message, type: "error", expire: 5000 });
										return null;
									} else {
										response.Data.Items.forEach((item) => {
											var status = true;
											if (item.Disks != null && item.Disks.length > 0) {
												item.Disks.forEach((disk) => {
													if (disk.State != "Good") status = false;
												});
											}
											item.State = "Online";
										});
										return response.Data.Items;
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
					ready: function () {
						webix.extend(this, webix.ProgressBar);
					},
					on: {
						onAfterSelect: (id) => {
							const diskpool = $$(DISKPOOL_TABLE).getItem(id);
							this.app.callEvent("diskpool:select", [diskpool]);
							$$(DISKPOOL_DELETE_BUTTON).enable();
							$$(DISKPOOL_TO_DISK_BUTTON).enable();
							if (diskpool.DefaultDiskPool) $$(DISKPOOL_TO_DEFAULT_BUTTON).disable();
							else $$(DISKPOOL_TO_DEFAULT_BUTTON).enable();
						},
						onUnselect: () => {
							$$(DISKPOOL_TABLE).unselectAll();
							$$(DISKPOOL_DELETE_BUTTON).disable();
							$$(DISKPOOL_TO_DISK_BUTTON).disable();
							$$(DISKPOOL_TO_DEFAULT_BUTTON).disable();
							this.app.callEvent("diskpool:unselect");
						}
					},
				},
			],
		};
	}
	init() {
		if ($$(DISKPOOL_ADD_WINDOW) == null)
			webix.ui({
				id: DISKPOOL_ADD_WINDOW,
				view: "popup",
				head: "디스크풀 추가",
				width: 350,
				body: {
					rows: [
						{ view: "label", label: "디스크풀 추가", align: "center" },
						{ view: "label", template:"<div class='popup_title_line' />", height:2 },
						{
							view: "form",
							borderless: true,
							elementsConfig: {
								labelWidth: 100,
							},
							elements: [
								{ view: "text", label: "Name", name: "Name" },
								{ view: "richselect", label: "DiskPool Type", name: "DiskPoolType", options: getDiskPoolType(), value: "STANDARD" },
								{ view: "richselect", label: "Tolerance", name: "ReplicationType", options: getDiskPoolReplicationType(), value: "OnePlusZero" },
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
													addDiskPool(this.getFormView().getValues());
												} else webix.alert({ type: "error", text: "Form data is invalid" });
											},
										},
									],
								},
							],
							rules: {
								Name: webix.rules.isNotEmpty,
								DiskPoolType: webix.rules.isNotEmpty,
								ReplicationType: webix.rules.isNotEmpty,
							},
						}]
				},
			});
		if ($$(DISKPOOL_DELETE_WINDOW) == null)
			webix.ui({
				id: DISKPOOL_DELETE_WINDOW,
				view: "popup",
				head: "디스크풀 삭제",
				width: 250,
				body: {
					rows: [
						{ view: "label", label: "디스크풀 삭제", align: "center" },
						{ view: "label", template:"<div class='popup_title_line' />", height:2 },
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
										deleteDiskPool();
									},
								},
							],
						},
					],
				},
			});
		if ($$(DISKPOOL_TO_DEFAULT_WINDOW) == null)
			webix.ui({
				id: DISKPOOL_TO_DEFAULT_WINDOW,
				view: "popup",
				head: "기본 디스크풀로 설정",
				width: 250,
				body: {
					rows: [
						{ view: "label", label: "기본 디스크풀로 설정", align: "center" },
						{ view: "label", template:"<div class='popup_title_line' />", height:2 },
						{ view: "label", label: "기본 디스크풀로 설정하시겠습니까?", align: "center" },
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
										setDefaultDiskPool();
									},
								},
							],
						},
					],
				},
			});
		if ($$(DISKPOOL_TO_DISK_WINDOW) == null)
			webix.ui({
				id: DISKPOOL_TO_DISK_WINDOW,
				view: "popup",
				head: "디스크 관리",
				width: 550,
				height: 500,
				body: {
					rows: [
						{ view: "label", label: "디스크 관리", align: "center" },
						{ view: "label", template:"<div class='popup_title_line' />", height:2 },
						{
							view: "datatable",
							id: SUB_DISK_TABLE,
							sort: "multi",
							tooltip: true,
							scroll: "y",
							resizeColumn: true,
							checkboxRefresh: true,
							css: "webix_header_border",
							columns: [
								{ id: "Id", header: "Id", hidden: true },
								{ id: "Check", header: { content: "masterCheckbox" }, template: "{common.checkbox()}", tooltip: false, width: 40 },
								{ id: "DiskPoolId", header: "DiskPoolId", hidden: true },
								{ id: "ServerName", header: "Server Name", width: 130 },
								{ id: "Name", header: "Disk Name", fillspace: true, width: 130 },
								{ id: "Path", header: "Path", width: 150 },
							],
							ready: function () {
								// apply sorting
								this.sort([
									{ by: "Check", dir: "desc", as: "int" },
									{ by: "Name", dir: "asc", as: "string" },
								]);
								webix.extend(this, webix.ProgressBar);
							},
							on: {
								onCheck: () => {
									$$(SUB_DISK_TABLE).sort([
										{ by: "Check", dir: "desc", as: "int" },
										{ by: "Name", dir: "asc", as: "string" },
									]);
								}
							}
						},
						{ height: 10 },
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
									click: function () {
										DiskPoolUpdateDisks();
									},
								},
							],
						},
					],
				},
				on: {
					onShow: () => {
						var diskPoolId = $$(DISKPOOL_TABLE).getSelectedItem();
						if (diskPoolId != null) {
							$$(SUB_DISK_TABLE).clearAll();
							webix
								.ajax()
								.get(`/api/v1/DiskPools/${diskPoolId.Id}/AvailableDisks`)
								.then(
									function (data) {
										var response = data.json();
										if (response.Result == "Error") {
											webix.message({ text: response.Message, type: "error", expire: 5000 });
										} else {
											Disks = [];
											response.Data.Items.forEach((item) => {
												Disks.push({
													Id: item.Id,
													Check: item.DiskPoolId == diskPoolId.Id ? true : false,
													Name: item.Name,
													ServerName: item.ServerName,
													Path: item.Path,
												});
											});
											$$(SUB_DISK_TABLE).parse(Disks);
											$$(SUB_DISK_TABLE).sort([
												{ by: "Check", dir: "desc", as: "int" },
												{ by: "Name", dir: "asc", as: "string" },
											]);
										}
									},
									function (error) {
										var response = JSON.parse(error.response);
										webix.message({ text: response.Message, type: "error", expire: 5000 });
										moveLogin("/#!/main/ksanusers");
										return null;
									}
								);
						}
					},
				}
			});
	}
}

/**
 * 디스크풀을 등록한다.
 * @param form json:{Name:"Name", DiskPoolType:"DiskPool Type", ReplicationType:"Replication Type", Description:"Description"}
 */
function addDiskPool(form) {
	showProgressIcon(DISKPOOL_TABLE);
	webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.post(DISKPOOL_URL, JSON.stringify(form))
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
 * 디스크풀 목록에서 선택한 디스크풀을 삭제한다.
 */
function deleteDiskPool() {
	var item = $$(DISKPOOL_TABLE).getSelectedItem();
	if (item == null) {
		webix.alert({ type: "error", text: "디스크풀을 선택해야 합니다." });
		return;
	}
	showProgressIcon(DISKPOOL_TABLE);
	webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.del(`${DISKPOOL_URL}/${item.Id}`)
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
 * 디스크풀 목록에서 선택한 디스크풀을 기본 디스크풀로 설정한다.
 */
function setDefaultDiskPool() {
	var item = $$(DISKPOOL_TABLE).getSelectedItem();
	if (item == null) {
		webix.alert({ type: "error", text: "디스크풀을 선택해야 합니다." });
		return;
	}
	showProgressIcon(DISKPOOL_TABLE);
	webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.put(`${DISKPOOL_URL}/Default/${item.Id}`)
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
 * 선택된 디스크풀에서 디스크를 할당하거나 제거한다.
 */
function DiskPoolUpdateDisks() {
	var DiskPool = $$(DISKPOOL_TABLE).getSelectedItem();
	var checkDisks = [];
	Disks.forEach(item => {
		if (item.Check == true) checkDisks.push(item.Id);
	})
	console.log(checkDisks);
	showProgressIcon(SUB_DISK_TABLE);
	webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.put(`${DISKPOOL_URL}/Disks/${DiskPool.Id}`, JSON.stringify({ Disks: checkDisks }))
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

