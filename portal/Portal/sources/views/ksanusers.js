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
import { loadUserAvailableDiskPools } from "../models/load/loadUserAvailableDiskPools";
import { showProgressIcon } from "../models/utils/showProgressIcon";
import { loadDiskPools } from "../models/load/loadDiskPools";
import { moveLogin } from "../models/utils/moveLogin";

const USER_URL = "/api/v1/KsanUsers";
const USER_ADD_WINDOW = "ksan_user_add_window";
const USER_DELETE_WINDOW = "ksan_user_delete_window";
const USER_DELETE_BUTTON = "ksan_user_delete_button";
const USER_DISKPOOL_WINDOW = "ksan_user_diskpool_window";
const DISKPOOL_EDIT_TABLE = "diskpool_edit_table";
const DISKPOOL_EDIT_WINDOW = "diskpool_edit_window";
const DISKPOOL_REMOVE_BUTTON = "diskpool_remove_button";
const USER_DISKPOOL_BUTTON = "ksan_user_diskpool_button";
const USER_TABLE = "ksan_user";
const NUMBER_FORMAT = webix.Number.numToStr({
	groupDelimiter: ",",
	groupSize: 3,
	decimalDelimiter: ".",
	decimalSize: 0,
});

var selectedUser = null;
export default class KsanUserView extends JetView {
	config() {
		return {
			rows: [
				{
					view: "toolbar",
					css: "webix_dark",
					paddingX: 20,
					elements: [{ view: "label", label: "Users", height: 0 }],
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
							popup: USER_ADD_WINDOW,
						},
						{
							id: USER_DELETE_BUTTON,
							view: "icon",
							icon: "mdi mdi-delete",
							tooltip: "삭제",
							disabled: true,
							autowidth: true,
							borderless: true,
							popup: USER_DELETE_WINDOW,
						},
						{
							id: USER_DISKPOOL_BUTTON,
							view: "icon",
							icon: "mdi mdi-harddisk",
							tooltip: "스토리지 클래스 관리",
							disabled: true,
							autowidth: true,
							borderless: true,
							popup: USER_DISKPOOL_WINDOW,
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
					id: USER_TABLE,
					sort: "multi",
					select: "row",
					resizeColumn: true,
					checkboxRefresh: true,
					columns: [
						{ id: "Id", header: "Id", hidden: true },
						{ id: "Name", header: "User Name", fillspace: true, minWidth: 130, sort: "string" },
						{ id: "AccessKey", header: "Access Key", width: 200, sort: "string" },
						{ id: "SecretKey", header: "Secret Key", width: 350, sort: "string" },
						{ id: "Email", header: "Email", width: 150, sort: "string" },
					],
					url: function () {
						return webix
							.ajax()
							.get(USER_URL)
							.then(
								function (data) {
									var response = data.json();
									if (response.Result == "Error") {
										webix.message({ text: response.Message, type: "error", expire: 5000 });
										return null;
									} else return response.Data.Items;
								},
								function (error) {
									var response = JSON.parse(error.response);
									webix.message({ text: response.Message, type: "error", expire: 5000 });
									moveLogin("/#!/main/ksanusers");
									return null;
								}
							);
					},
					ready: function () {
						this.sort([{ by: "Name", dir: "asc" }]);
						this.markSorting("Name", "asc", true);
						webix.extend(this, webix.ProgressBar);
					},
					on: {
						onSelectChange: function () {
							$$(USER_DELETE_BUTTON).enable();
							$$(USER_DISKPOOL_BUTTON).enable();
						},
					}
				}
			],
		};
	}
	init() {
		if ($$(USER_ADD_WINDOW) == null)
			webix.ui({
				id: USER_ADD_WINDOW,
				view: "popup",
				head: "사용자 추가",
				width: 350,
				body: {
					rows: [
						{ view: "label", label: "사용자 추가", align: "center" },
						{ view: "label", template: "<div class='popup_title_line' />", height: 2 },
						{
							view: "form",
							borderless: true,
							elementsConfig: {
								labelWidth: 100,
							},
							elements: [
								{ view: "text", label: "Name", name: "Name" },
								{ view: "text", label: "Email", name: "Email" },
								{
									view: "richselect",
									label: "Disk Pool",
									name: "StandardDiskPoolId",
									options: loadDiskPools(),
								},
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
													addKsanUser(this.getFormView().getValues());
												} else webix.alert({ type: "error", text: "Form data is invalid" });
											},
										},
									],
								},
							],
							rules: {
								Name: webix.rules.isNotEmpty,
							},
						}]
				},
			});
		if ($$(USER_DELETE_WINDOW) == null)
			webix.ui({
				id: USER_DELETE_WINDOW,
				view: "popup",
				head: "사용자 삭제",
				width: 250,
				body: {
					rows: [
						{ view: "label", label: "사용자 삭제", align: "center" },
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
										deleteKsanUsers();
									},
								},
							],
						},
					],
				},
			});
		if ($$(USER_DISKPOOL_WINDOW) == null)
			webix.ui({
				view: "popup",
				id: USER_DISKPOOL_WINDOW,
				height: 400,
				width: 400,
				body:
				{
					rows:
						[
							{
								view: "toolbar",
								cols: [
									{},
									{ view: "label", label: "Define User’s StorageClass", width: 245 },
									{
										view: "icon", icon: "mid wxi-plus-circle",
										click: function () {
											$$(DISKPOOL_EDIT_TABLE).add({ UserId: selectedUser, StorageClass: "Undefined", isAdded: true });
										}
									}
								]
							},
							{ view: "label", template: "<div class='popup_title_line' />", height: 2 },
							{
								view: "datatable",
								editable: true,
								scroll: "y",
								id: DISKPOOL_EDIT_TABLE,
								form: DISKPOOL_EDIT_WINDOW,
								columns: [
									{ id: "StorageClass", header: "StorageClass", fillspace: true, minWidth: 130 },
									{ id: "DiskPoolId", header: "DiskPoolName", template: "#DiskPoolName#", width: 200 },
									{
										id: DISKPOOL_REMOVE_BUTTON, header: "", width: 40,
										template: "<span class='webix_icon wxi-minus-circle'></span>",
									}
								],
								on:
								{
									//선택전 필터링 하기위한 이벤트
									onBeforeEditStart: function (cell) {
										// 선택된 아이템을 가져온다.
										var item = this.getItem(cell);

										// 삭제 버튼을 눌렀을때 삭제처리
										if (cell.column == DISKPOOL_REMOVE_BUTTON) {
											console.log("delete item");
											console.log(item);
											item.isDeleted = true;
											$$(DISKPOOL_EDIT_TABLE).updateItem(item.id, item);
											$$(DISKPOOL_EDIT_TABLE).filter(function (obj) { return obj.isDeleted != true; });
											return false;
										}
										// 선택된 아이템에 맞게 디스크풀 목록을 가져온다.
										var list = $$(DISKPOOL_EDIT_WINDOW).getChildViews()[1].getPopup().getList();
										list.clearAll();
										list.parse(loadUserAvailableDiskPools(item.UserId, item.DiskPoolId));
									},
								}
							},
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
										value: "저장",
										hotkey: "enter",
										click: function () {
											changeUserStorageClass();
										},
									},
								],
							},
						]
				},
				on: {
					onBeforeShow() {
						const myTable = $$(DISKPOOL_EDIT_TABLE);
						myTable.clearAll();
						myTable.load(function () {
							var item = $$(USER_TABLE).getSelectedItem();
							selectedUser = item.Id;
							return webix
								.ajax()
								.get(`/api/v1/KsanUsers/StorageClass/${item.Id}`)
								.then(
									function (data) {
										var response = data.json();
										if (response.Result == "Error") {
											webix.message({ text: response.Message, type: "error", expire: 5000 });
											return null;
										} else {
											return response.Data.Items;
										}
									},
									function (error) {
										var response = JSON.parse(error.response);
										webix.message({ text: response.Message, type: "error", expire: 5000 });
										return null;
									}
								);
						});
					},
				}
			});

		if ($$(DISKPOOL_EDIT_WINDOW) == null)
			webix.ui({
				view: "popup",
				body: {
					view: "form",
					id: DISKPOOL_EDIT_WINDOW,
					elements: [
						{ view: "text", name: "StorageClass" },
						{ view: "richselect", name: "DiskPoolId", options: loadDiskPools() },
						{
							view: "button", label: "Save", type: "form", click: function (id) {
								var form = $$(id).getFormView();
								var item = form.getValues();
								item.DiskPoolName = $$(DISKPOOL_EDIT_WINDOW).getChildViews()[1].getPopup().getList().getSelectedItem().value;
								item.isChanged = true;
								$$(DISKPOOL_EDIT_TABLE).updateItem(item.id, item);
								this.getTopParentView().hide();
							}
						}
					]
				},
			});
	}
}

/**
 * 사용자를 등록한다.
 * @param {ServerId:"Server Id", DiskPoolId:"Disk Pool Id", Name:"Name", State:"State", RwMode:"RwMode", Description:"Description"} form
 */
function addKsanUser(form) {
	showProgressIcon(USER_TABLE);
	webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.post(USER_URL, JSON.stringify(form))
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
 * 사용자 목록에서 선택한 사용자를 삭제한다.
 */
function deleteKsanUsers() {
	var items = $$(USER_TABLE).getSelectedItem();
	if (items == null) webix.message({ text: "사용자를 선택해야 합니다.", type: "error", expire: 5000 });
	else if (Array.isArray(items)) {
		var result = false;
		items.forEach(function (item) {
			if (deleteKsanUser(item.Id)) result = true;
		});
		if (result) window.location.reload(true);
	} else {
		if (deleteKsanUser(items.Id)) window.location.reload(true);
	}
}

/**
 * 사용자를 삭제한다.
 * @param {Guid} id 삭제할 사용자의 Id
 * @returns 
 */
function deleteKsanUser(id) {
	showProgressIcon(USER_TABLE);
	return webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.del(`${USER_URL}/${id}`)
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
 * 사용자의 스토리지 클래스를 변경한다.
 */
function changeUserStorageClass() {
	var items = $$(DISKPOOL_EDIT_TABLE).serialize(true);
	console.log(items);
	var result = false;

	items.forEach(function (item) {
		if (item.isDeleted) {
			if (deleteUserStorageClass(item)) result = true;
		}
		else if (item.isAdded && item.isChanged) {
			if (addUserStorageClass(item)) result = true;
		}
		else if (item.isChanged) {
			if (updateUserStorageClass(item)) result = true;
		}
	});

	if (result) window.location.reload(true);
}

/**
 * 사용자의 스토리지 클래스를 추가한다.
 * @param {  "UserId": "string", "DiskPoolId": "string", "StorageClass": "string"} item
 * @returns 추가 결과
 */
function addUserStorageClass(item) {
	showProgressIcon(USER_TABLE);
	return webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.post(`${USER_URL}/StorageClass`, JSON.stringify(item))
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
 * 사용자의 스토리지 클래스를 변경한다.
 * @param {  "UserId": "string", "DiskPoolId": "string", "StorageClass": "string"} item
 * @returns 추가 결과
 */
function updateUserStorageClass(item) {
	showProgressIcon(USER_TABLE);
	return webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.put(`${USER_URL}/StorageClass/${item.Id}`, JSON.stringify(item))
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
 * 사용자의 스토리지 클래스를 삭제한다.
 * @param {  "UserId": "string", "DiskPoolId": "string", "StorageClass": "string"} item 
 * @returns 삭제 결과
 */
function deleteUserStorageClass(item) {
	showProgressIcon(USER_TABLE);
	return webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.del(`${USER_URL}/StorageClass/${item.Id}`)
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