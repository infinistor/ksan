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

const REGION_NAME = "label_legion";
const KSAN_AGENT = "label_ksan_agent";
const KSAN_GW = "label_ksan_gw";
const KSAN_OSD = "label_ksan_osd";
const KSAN_LIFECYCLE = "label_ksan_lifecycle";
const KSAN_LOG = "label_ksan_log";
const KSAN_REPLICATION = "label_ksan_replication";
const STATUS_ICON = "status_icon";

const DEFAULT_PREFIX = "<span class='service_name_default'>ksan</span><span class='service_name'>";
var global_status = true;
export default class OverviewView extends JetView {
	config() {
		return {
			type: "abslayout",
			minWidth: 600,
			height: 300,
			cols: [
				{
					rows: [
						{
							cols: [
								{ label: "<span class='card_title'>System Overview</span>", view: "label" },
								{ id: STATUS_ICON, view: "label", width: 60 },
							],
						},
						{ height: 20 },
						{
							cols: [
								{ label: "region", view: "label", width: 100, css: "overview_title" },
								{ id: REGION_NAME, view: "label" },
							],
						},
						{
							cols: [
								{ label: "Services", view: "label", width: 100, css: "overview_title" },
								{ id: KSAN_AGENT, view: "label" },
							],
						},
						{
							cols: [{ width: 100 }, { id: KSAN_GW, view: "label" }],
						},
						{
							cols: [{ width: 100 }, { id: KSAN_OSD, view: "label" }],
						},
						{
							cols: [{ width: 100 }, { id: KSAN_LIFECYCLE, view: "label" }],
						},
						{
							cols: [{ width: 100 }, { id: KSAN_LOG, view: "label" }],
						},
						{
							cols: [{ width: 100 }, { id: KSAN_REPLICATION, view: "label" }],
						},
					],
				},
			],
		};
	}
	init() {
		loadServices();
		loadMainRegion();
	}
}

/**
 * 기본 리전을 조회한다.
 * @returns 기본 리전 정보
 */
function loadMainRegion() {
	return webix
		.ajax()
		.get("/api/v1/Regions/Default")
		.then(
			function (data) {
				var response = data.json();
				if (response.Result == "Error") {
					webix.message({ text: response.Message, type: "error", expire: 5000 });
					return null;
				} else {
					$$(REGION_NAME).setValue(`<span class='region_name'>${response.Data.Name}</span>`);
					return usage;
				}
			},
			function (error) {
				var response = JSON.parse(error.response);
				webix.message({ text: response.Message, type: "error", expire: 5000 });
				return null;
			}
		);
}

/**
 * 서비스 상태를 조회한다.
 * @returns 서비스 정보
 */
function loadServices() {
	return webix
		.ajax()
		.get("/api/v1/Services")
		.then(
			function (data) {
				var response = data.json();
				if (response.Result == "Error") {
					webix.message({ text: response.Message, type: "error", expire: 5000 });
					return null;
				} else {
					$$(KSAN_AGENT).setValue(getKsanAgentString(response.Data.Items));
					$$(KSAN_GW).setValue(getKsanGWString(response.Data.Items));
					$$(KSAN_OSD).setValue(getKsanOSDString(response.Data.Items));
					$$(KSAN_LIFECYCLE).setValue(getKsanLifecycleManagerString(response.Data.Items));
					$$(KSAN_LOG).setValue(getKsanLogManagerString(response.Data.Items));
					$$(KSAN_REPLICATION).setValue(getKsanReplicationManagerString(response.Data.Items));
					if (global_status == true) $$(STATUS_ICON).setValue(`<image width=35px; src="codebase/images/check.png">`);
					else $$(STATUS_ICON).setValue(`<image width=35px; src="codebase/images/alert-yellow.png">`);

					return usage;
				}
			},
			function (error) {
				var response = JSON.parse(error.response);
				webix.message({ text: response.Message, type: "error", expire: 5000 });
				return null;
			}
		);
}

/**
 * KsanAgent 서비스의 상태 정보를 가져온다.
 * @param {json} items KsanAgent Service List
 * @returns 상태 정보를 포함한 서비스 정보
 */
function getKsanAgentString(items) {
	var count = 0;
	var errorCount = 0;
	var flag = true;
	items.forEach((item) => {
		if (item.ServiceType == "ksanAgent") {
			if (item.State != "Online") {
				flag = false;
				errorCount++;
			}
			count++;
		}
	});
	return `${DEFAULT_PREFIX}Agent ${count - errorCount}/${count}</span>${getStatus(flag)}`;
}

/**
 * KsanGW 서비스의 상태 정보를 가져온다.
 * @param {json} items KsanGW Service List
 * @returns 상태 정보를 포함한 서비스 정보
 */
function getKsanGWString(items) {
	var count = 0;
	var errorCount = 0;
	var flag = true;
	items.forEach((item) => {
		if (item.ServiceType == "ksanGW") {
			if (item.State != "Online") {
				flag = false;
				errorCount++;
			}
			count++;
		}
	});
	return `${DEFAULT_PREFIX}GW ${count - errorCount}/${count}</span>${getStatus(flag)}`;
}

/**
 * KsanOSD 서비스의 상태 정보를 가져온다.
 * @param {json} items KsanOSD Service List
 * @returns 상태 정보를 포함한 서비스 정보
 */
function getKsanOSDString(items) {
	var count = 0;
	var errorCount = 0;
	var flag = true;
	items.forEach((item) => {
		if (item.ServiceType == "ksanOSD") {
			if (item.State != "Online") {
				flag = false;
				errorCount++;
			}
			count++;
		}
	});
	return `${DEFAULT_PREFIX}OSD ${count - errorCount}/${count}</span>${getStatus(flag)}`;
}

/**
 * KsanLifecycleManager 서비스의 상태 정보를 가져온다.
 * @param {json} items KsanLifecycleManager Service List
 * @returns 상태 정보를 포함한 서비스 정보
 */
function getKsanLifecycleManagerString(items) {
	var count = 0;
	var errorCount = 0;
	var flag = true;
	items.forEach((item) => {
		if (item.ServiceType == "ksanLifecycleManager") {
			if (item.State != "Online") {
				flag = false;
				errorCount++;
			}
			count++;
		}
	});
	return `${DEFAULT_PREFIX}LifecycleManager ${count - errorCount}/${count}</span>${getStatus(flag)}`;
}

/**
 * KsanLogManager 서비스의 상태 정보를 가져온다.
 * @param {json} items KsanLogManager Service List
 * @returns 상태 정보를 포함한 서비스 정보
 */
function getKsanLogManagerString(items) {
	var count = 0;
	var errorCount = 0;
	var flag = true;
	items.forEach((item) => {
		if (item.ServiceType == "ksanLogManager") {
			if (item.State != "Online") {
				flag = false;
				errorCount++;
			}
			count++;
		}
	});
	return `${DEFAULT_PREFIX}LogManager ${count - errorCount}/${count}</span>${getStatus(flag)}`;
}

/**
 * KsanReplicationManager 서비스의 상태 정보를 가져온다.
 * @param {json} items KsanReplicationManager Service List
 * @returns 상태 정보를 포함한 서비스 정보
 */
function getKsanReplicationManagerString(items) {
	var count = 0;
	var errorCount = 0;
	var flag = true;
	items.forEach((item) => {
		if (item.ServiceType == "ksanReplicationManager") {
			if (item.State != "Online") {
				flag = false;
				errorCount++;
			}
			count++;
		}
	});
	return `${DEFAULT_PREFIX}ReplicationManager ${count - errorCount}/${count}</span>${getStatus(flag)}`;
}

function getStatus(flag) {
	var status = "Healthy";
	var style = "";
	if (flag == false) {
		status = "UnHealthy";
		style = "un_healthy";
		global_status = false;
	}

	return `<span class='service_status ${style}'>(${status})</span>`;
}
