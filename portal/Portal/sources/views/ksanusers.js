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
import { showProgressIcon } from "../models/utils/showProgressIcon";
import { loadDiskPools } from "../models/load/loadDiskPools";
import { moveLogin } from "../models/utils/moveLogin";

const MY_URL = "/api/v1/KsanUsers";
const MY_ADD_WINDOW = "ksan_user_add_window";
const MY_DELETE_WINDOW = "ksan_user_delete_window";
const MY_TABLE = "ksan_user";
const NUMBER_FORMAT = webix.Number.numToStr({
	groupDelimiter: ",",
	groupSize: 3,
	decimalDelimiter: ".",
	decimalSize: 0,
});

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
							popup: MY_ADD_WINDOW,
						},
						{
							view: "icon",
							icon: "mdi mdi-delete",
							tooltip: "삭제",
							autowidth: true,
							borderless: true,
							popup: MY_DELETE_WINDOW,
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
					id: MY_TABLE,
					sort: "multi",
					select: "row",
					multiselect: true,
					resizeColumn: true,
					checkboxRefresh: true,
					columns: [
						{ id: "Id", header: "Id", hidden: true },
						{ id: "Check", header: { content: "masterCheckbox" }, checkValue: 'on', uncheckValue: 'off', template: "{common.checkbox()}", width: 40 },
						{ id: "Name", header: "User Name", fillspace: true, minWidth: 130, sort: "string" },
						{ id: "AccessKey", header: "Access Key", width: 200, sort: "string" },
						{ id: "SecretKey", header: "Secret Key", width: 350, sort: "string" },
						{ id: "Email", header: "Email", width: 150, sort: "string" },
					],
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
						{ view: "text", label: "Name", name: "Name" },
						{ view: "text", label: "Email", name: "Email" },
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
										deleteKsanUser();
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
 * 유저를 등록한다.
 * @param form json:{ServerId:"Server Id", DiskPoolId:"Disk Pool Id", Name:"Name", State:"State", RwMode:"RwMode", Description:"Description"}
 */
function addKsanUser(form) {
	showProgressIcon(MY_TABLE);
	webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.post(MY_URL, JSON.stringify(form))
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
 * 유저 목록에서 선택한 유저를 삭제한다.
 */
function deleteKsanUser() {
	var item = $$(MY_TABLE).getSelectedItem();
	if (item == null) {
		webix.alert({ type: "error", text: "유저를 선택해야 합니다." });
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
