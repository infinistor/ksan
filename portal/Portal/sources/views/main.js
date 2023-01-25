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
import { JetView, plugins } from "webix-jet";
import { moveLogin } from "../models/utils/moveLogin";

export default class TopView extends JetView {
	config() {
		return {
			type: "clean",
			rows: [
				{
					view: "toolbar",
					padding: 5,
					height: 50,
					cols: [
						{ view: "icon", icon: "mdi mdi-menu", click: () => this.app.callEvent("menu:toggle") },
						{ view: "label", label: "KSAN", css: "header_label" },
						{},
						{
							view: "icon",
							icon: "mdi mdi-bell",
							tooltip: "Open latest notifications",
							click: function () {
								this.$scope.notifications.showWin(this.$view);
							},
						},
						{ width: 8 },
						{ view: "icon", icon: "mdi mdi-settings" },
						{
							view: "icon",
							icon: "mdi mdi-logout",
							click: function () {
								webix.storage.cookie.clear();
								moveLogin();
							},
						},
					],
				},
				{
					cols: [
						{
							localId: "side:menu",
							view: "sidebar",
							width: 200,
							data: [
								{ id: "dash", value: "Dashboard", icon: "mdi mdi-view-dashboard" },
								{ id: "servers", value: "Servers", icon: "mdi mdi-server-network" },
								{ id: "services", value: "Services", icon: "mdi mdi-puzzle" },
								{ id: "disks", value: "Disks", icon: "mdi mdi-harddisk" },
								{ id: "diskpools", value: "Diskpools", icon: "mdi mdi-cloud" },
								{ id: "ksanusers", value: "Users", icon: "mdi mdi-account-box" },
							],
						},
						{ $subview: true },
					],
				},
			],
		};
	}

	init() {
		this.use(plugins.Menu, this.$$("side:menu"));
		this.on(this.app, "menu:toggle", () => this.$$("side:menu").toggle());
		webix.attachEvent("onBeforeAjax", function (mode, url, data, request, headers, files, promise) {
			request.withCredentials = true;
			headers["Content-type"] = "application/json; charset=utf-8";
		});
	}
}
