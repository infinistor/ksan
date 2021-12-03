/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
using System;
using System.Threading.Tasks;
using PortalProviderInterface;
using MTLib.Core;

namespace PortalSvr.Services
{
	/// <summary>권한 초기화 클래스</summary>
	public class ClaimInitializer
	{
		/// <summary>프로바이더</summary>
		private readonly IRoleProvider m_provider = null;

		/// <summary>생성자</summary>
		/// <param name="provider">계정에 대한 프로바이더 객체</param>
		public ClaimInitializer(IRoleProvider provider)
		{
			m_provider = provider;
		}

		/// <summary>권한들을 초기화 한다.</summary>
		public async Task InitializeClaims()
		{
			try
			{
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.dashboard", "UL_COMMON__CLAIM_NAME_CSSP_DASHBOARD", 1, "01000000");
				// await m_provider.AddClaimTitle("Permission", "cssp.dashboard.all", "UL_COMMON__CLAIM_NAME_CSSP_DASHBOARD_ALL", 2, "01010000");
				//
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.systems", "UL_COMMON__CLAIM_NAME_CSSP_SYSTEMS", 1, "02000000");
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.systems.system", "UL_COMMON__CLAIM_NAME_CSSP_SYSTEMS_SYSTEM", 2, "02010000");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.system.view", "UL_COMMON__CLAIM_NAME_CSSP_SYSTEMS_SYSTEM_VIEW", 3, "02010100");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.system.remove", "UL_COMMON__CLAIM_NAME_CSSP_SYSTEMS_SYSTEM_REMOVE", 3, "02010101");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.system.mds_disk_add", "UL_COMMON__CLAIM_NAME_CSSP_SYSTEMS_SYSTEM_MDS_DISK_ADD", 3, "02010200");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.system.osd_start", "UL_COMMON__CLAIM_NAME_CSSP_SYSTEMS_SYSTEM_OSD_START", 3, "02010300");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.system.osd_disk_add", "UL_COMMON__CLAIM_NAME_CSSP_SYSTEMS_SYSTEM_OSD_DISK_ADD", 3, "02010400");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.system.osd_disk_update", "UL_COMMON__CLAIM_NAME_CSSP_SYSTEMS_SYSTEM_OSD_DISK_UPDATE", 3, "02010500");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.system.osd_disk_remove", "UL_COMMON__CLAIM_NAME_CSSP_SYSTEMS_SYSTEM_OSD_DISK_REMOVE", 3, "02010600");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.system.osd_disk_start", "UL_COMMON__CLAIM_NAME_CSSP_SYSTEMS_SYSTEM_OSD_DISK_START", 3, "02010700");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.system.auth_start", "UL_COMMON__CLAIM_NAME_CSSP_SYSTEMS_SYSTEM_AUTH_START", 3, "02010801");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.system.rest_start", "UL_COMMON__CLAIM_NAME_CSSP_SYSTEMS_SYSTEM_REST_START", 3, "02010802");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.system.s3_start", "UL_COMMON__CLAIM_NAME_CSSP_SYSTEMS_SYSTEM_S3_START", 3, "02010803");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.system.smb_start", "UL_COMMON__CLAIM_NAME_CSSP_SYSTEMS_SYSTEM_SMB_START", 3, "02010804");
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.systems.users", "UL_COMMON__CLAIM_NAME_CSSP_SYSTEMS_USERS", 2, "02030000");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.users.list", "UL_COMMON__CLAIM_NAME_CSSP_SYSTEMS_USERS_LIST", 3, "02030100");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.users.view", "UL_COMMON__CLAIM_NAME_CSSP_SYSTEMS_USERS_VIEW", 3, "02030200");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.users.add", "UL_COMMON__CLAIM_NAME_CSSP_SYSTEMS_USERS_ADD", 3, "02030300");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.users.update", "UL_COMMON__CLAIM_NAME_CSSP_SYSTEMS_USERS_UPDATE", 3, "02030400");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.users.remove", "UL_COMMON__CLAIM_NAME_CSSP_SYSTEMS_USERS_REMOVE", 3, "02030500");
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.systems.fsck", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_FSCK", 2, "02040000");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.fsck.list", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_FSCK_LIST", 3, "02040100");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.fsck.view", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_FSCK_VIEW", 3, "02040200");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.fsck.add", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_FSCK_ADD", 3, "02040300");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.fsck.remove", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_FSCK_REMOVE", 3, "02040400");
				// await m_provider.AddClaimTitle("Permission", "cssp.systems.fsck.stop", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_FSCK_STOP", 3, "02040500");
				//
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.dr", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_DR", 1, "11000000");
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.dr.source", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_DR_SOURCE", 2, "11010000");
				// await m_provider.AddClaimTitle("Permission", "cssp.dr.source.view", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_DR_SOURCE_VIEW", 3, "11010100");
				// await m_provider.AddClaimTitle("Permission", "cssp.dr.source.update", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_DR_SOURCE_UPDATE", 3, "11010200");
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.dr.target", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_DR_TARGET", 2, "11020000");
				// await m_provider.AddClaimTitle("Permission", "cssp.dr.target.list", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_DR_TARGET_LIST", 3, "11020100");
				// await m_provider.AddClaimTitle("Permission", "cssp.dr.target.add", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_DR_TARGET_ADD", 3, "11020200");
				// await m_provider.AddClaimTitle("Permission", "cssp.dr.target.remove", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_DR_TARGET_REMOVE", 3, "11020300");
				// await m_provider.AddClaimTitle("Permission", "cssp.dr.target.apply", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_DR_TARGET_APPLY", 3, "11020400");
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.dr.jobs", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_DR_JOBS", 2, "11030000");
				// await m_provider.AddClaimTitle("Permission", "cssp.dr.jobs.list", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_DR_JOBS_LIST", 3, "11030100");
				// await m_provider.AddClaimTitle("Permission", "cssp.dr.jobs.add", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_DR_JOBS_ADD", 3, "11030200");
				// await m_provider.AddClaimTitle("Permission", "cssp.dr.jobs.remove", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_DR_JOBS_REMOVE", 3, "11030300");
				// await m_provider.AddClaimTitle("Permission", "cssp.dr.jobs.start", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_DR_JOBS_START", 3, "11030400");
				//
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.nas", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE", 1, "12000000");
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.nas.smb", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_SMB", 2, "12010000");
				// await m_provider.AddClaimTitle("Permission", "cssp.nas.smb.list", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_SMB_LIST", 3, "12010001");
				// await m_provider.AddClaimTitle("Permission", "cssp.nas.smb.add", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_SMB_ADD", 3, "12010002");
				// await m_provider.AddClaimTitle("Permission", "cssp.nas.smb.update", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_SMB_UPDATE", 3, "12010003");
				// await m_provider.AddClaimTitle("Permission", "cssp.nas.smb.remove", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_SMB_REMOVE", 3, "12010004");
				// await m_provider.AddClaimTitle("Permission", "cssp.nas.smb.start", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_SMB_START", 3, "12010005");
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.nas.smb.ep", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_SMB_EXPORT_POINT", 2, "12010100");
				// await m_provider.AddClaimTitle("Permission", "cssp.nas.smb.ep.list", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_SMB_EXPORT_POINT_LIST", 3, "12010101");
				// await m_provider.AddClaimTitle("Permission", "cssp.nas.smb.ep.add", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_SMB_EXPORT_POINT_ADD", 3, "12010102");
				// await m_provider.AddClaimTitle("Permission", "cssp.nas.smb.ep.update", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_SMB_EXPORT_POINT_UPDATE", 3, "12010103");
				// await m_provider.AddClaimTitle("Permission", "cssp.nas.smb.ep.remove", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_SMB_EXPORT_POINT_REMOVE", 3, "12010104");
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.nas.smb.network-manager", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_SMB_NETWORK_MANAGER", 2, "12010200");
				// await m_provider.AddClaimTitle("Permission", "cssp.nas.smb.network-manager.list", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_SMB_NETWORK_MANAGER_LIST", 3, "12010201");
				// await m_provider.AddClaimTitle("Permission", "cssp.nas.smb.network-manager.add", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_SMB_NETWORK_MANAGER_ADD", 3, "12010202");
				// await m_provider.AddClaimTitle("Permission", "cssp.nas.smb.network-manager.update", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_SMB_NETWORK_MANAGER_UPDATE", 3, "12010203");
				// await m_provider.AddClaimTitle("Permission", "cssp.nas.smb.network-manager.remove", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_SMB_NETWORK_MANAGER_REMOVE", 3, "12010204");
				//
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.nas.s3", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_S3", 2, "12020000");
				// await m_provider.AddClaimTitle("Permission", "cssp.nas.s3.list", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_S3_LIST", 3, "12020001");
				// await m_provider.AddClaimTitle("Permission", "cssp.nas.s3.view", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_S3_VIEW", 3, "12020002");
				// await m_provider.AddClaimTitle("Permission", "cssp.nas.s3.add", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_S3_ADD", 3, "12020003");
				// await m_provider.AddClaimTitle("Permission", "cssp.nas.s3.update", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_S3_UPDATE", 3, "12020004");
				// await m_provider.AddClaimTitle("Permission", "cssp.nas.s3.remove", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_S3_REMOVE", 3, "12020005");
				// await m_provider.AddClaimTitle("Permission", "cssp.nas.s3.start", "UL_COMMON__CLAIM_NAME_CSSP_STORAGE_S3_START", 3, "12020006");
				//
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.lb", "UL_COMMON__CLAIM_NAME_CSSP_LOAD_BALANCER", 2, "13000000");
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.lb.ha-proxy", "UL_COMMON__CLAIM_NAME_CSSP_LOAD_BALANCER_HA_PROXY", 2, "13010000");
				// await m_provider.AddClaimTitle("Permission", "cssp.lb.ha-proxy.list", "UL_COMMON__CLAIM_NAME_CSSP_LOAD_BALANCER_HA_PROXY_LIST", 3, "13010001");
				// await m_provider.AddClaimTitle("Permission", "cssp.lb.ha-proxy.view", "UL_COMMON__CLAIM_NAME_CSSP_LOAD_BALANCER_HA_PROXY_VIEW", 3, "13010002");
				// await m_provider.AddClaimTitle("Permission", "cssp.lb.ha-proxy.add", "UL_COMMON__CLAIM_NAME_CSSP_LOAD_BALANCER_HA_PROXY_ADD", 3, "13010003");
				// await m_provider.AddClaimTitle("Permission", "cssp.lb.ha-proxy.update", "UL_COMMON__CLAIM_NAME_CSSP_LOAD_BALANCER_HA_PROXY_UPDATE", 3, "13010004");
				// await m_provider.AddClaimTitle("Permission", "cssp.lb.ha-proxy.remove", "UL_COMMON__CLAIM_NAME_CSSP_LOAD_BALANCER_HA_PROXY_REMOVE", 3, "13010005");
				// await m_provider.AddClaimTitle("Permission", "cssp.lb.ha-proxy.start", "UL_COMMON__CLAIM_NAME_CSSP_LOAD_BALANCER_HA_PROXY_START", 3, "13010006");
				//
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.securage", "UL_COMMON__CLAIM_NAME_CSSP_APPLICATION", 1, "21000000");
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.securage.filetype", "UL_COMMON__CLAIM_NAME_CSSP_APPLICATION_FILETYPE", 2, "21010000");
				// await m_provider.AddClaimTitle("Permission", "cssp.securage.filetype.list", "UL_COMMON__CLAIM_NAME_CSSP_APPLICATION_FILETYPE_LIST", 3, "21010100");
				// await m_provider.AddClaimTitle("Permission", "cssp.securage.filetype.view", "UL_COMMON__CLAIM_NAME_CSSP_APPLICATION_FILETYPE_VIEW", 3, "21010200");
				// await m_provider.AddClaimTitle("Permission", "cssp.securage.filetype.add", "UL_COMMON__CLAIM_NAME_CSSP_APPLICATION_FILETYPE_ADD", 3, "21010300");
				// await m_provider.AddClaimTitle("Permission", "cssp.securage.filetype.remove", "UL_COMMON__CLAIM_NAME_CSSP_APPLICATION_FILETYPE_REMOVE", 3, "21010400");
				// await m_provider.AddClaimTitle("Permission", "cssp.securage.filetype.update", "UL_COMMON__CLAIM_NAME_CSSP_APPLICATION_FILETYPE_UPDATE", 3, "21010500");
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.securage.app", "UL_COMMON__CLAIM_NAME_CSSP_APPLICATION_APP", 2, "21020000");
				// await m_provider.AddClaimTitle("Permission", "cssp.securage.app.list", "UL_COMMON__CLAIM_NAME_CSSP_APPLICATION_APP_LIST", 3, "21020100");
				// await m_provider.AddClaimTitle("Permission", "cssp.securage.app.view", "UL_COMMON__CLAIM_NAME_CSSP_APPLICATION_APP_VIEW", 3, "21020200");
				// await m_provider.AddClaimTitle("Permission", "cssp.securage.app.add", "UL_COMMON__CLAIM_NAME_CSSP_APPLICATION_APP_ADD", 3, "21020300");
				// await m_provider.AddClaimTitle("Permission", "cssp.securage.app.remove", "UL_COMMON__CLAIM_NAME_CSSP_APPLICATION_APP_REMOVE", 3, "21020400");
				// await m_provider.AddClaimTitle("Permission", "cssp.securage.app.update", "UL_COMMON__CLAIM_NAME_CSSP_APPLICATION_APP_UPDATE", 3, "21020500");
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.securage.unlockfile", "UL_COMMON__CLAIM_NAME_CSSP_APPLICATION_UNLOCK_FILE", 2, "21030000");
				// await m_provider.AddClaimTitle("Permission", "cssp.securage.unlockfile.list", "UL_COMMON__CLAIM_NAME_CSSP_APPLICATION_UNLOCK_FILE_LIST", 3, "21030100");
				// await m_provider.AddClaimTitle("Permission", "cssp.securage.unlockfile.view", "UL_COMMON__CLAIM_NAME_CSSP_APPLICATION_UNLOCK_FILE_VIEW", 3, "21030200");
				// await m_provider.AddClaimTitle("Permission", "cssp.securage.unlockfile.update", "UL_COMMON__CLAIM_NAME_CSSP_APPLICATION_UNLOCK_FILE_UPDATE", 3, "21030300");
				// await m_provider.AddClaimTitle("Permission", "cssp.securage.unlockfile.request", "UL_COMMON__CLAIM_NAME_CSSP_APPLICATION_UNLOCK_FILE_REQUEST", 3, "21030400");
				//
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.sync", "UL_COMMON__CLAIM_NAME_CSSP_SYNC", 1, "31000000");
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.sync.job", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_JOB", 2, "31010000");
				// await m_provider.AddClaimTitle("Permission", "cssp.sync.job.list", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_JOB_LIST", 3, "31010100");
				// await m_provider.AddClaimTitle("Permission", "cssp.sync.job.view", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_JOB_VIEW", 3, "31010200");
				// await m_provider.AddClaimTitle("Permission", "cssp.sync.job.add", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_JOB_ADD", 3, "31010300");
				// await m_provider.AddClaimTitle("Permission", "cssp.sync.job.remove", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_JOB_REMOVE", 3, "31010400");
				// await m_provider.AddClaimTitle("Permission", "cssp.sync.job.update", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_JOB_UPDATE", 3, "31010500");
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.sync.whitelist", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_WHITELIST", 2, "31020000");
				// await m_provider.AddClaimTitle("Permission", "cssp.sync.whitelist.list", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_WHITELIST_LIST", 3, "31020100");
				// await m_provider.AddClaimTitle("Permission", "cssp.sync.whitelist.view", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_WHITELIST_VIEW", 3, "31020200");
				// await m_provider.AddClaimTitle("Permission", "cssp.sync.whitelist.add", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_WHITELIST_ADD", 3, "31020300");
				// await m_provider.AddClaimTitle("Permission", "cssp.sync.whitelist.remove", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_WHITELIST_REMOVE", 3, "31020400");
				// await m_provider.AddClaimTitle("Permission", "cssp.sync.whitelist.update", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_WHITELIST_UPDATE", 3, "31020500");
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.sync.user", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_USER", 2, "31030000");
				// await m_provider.AddClaimTitle("Permission", "cssp.sync.user.list", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_USER_LIST", 3, "31030100");
				// await m_provider.AddClaimTitle("Permission", "cssp.sync.user.view", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_USER_VIEW", 3, "31030200");
				// await m_provider.AddClaimTitle("Permission", "cssp.sync.user.add", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_USER_ADD", 3, "31030300");
				// await m_provider.AddClaimTitle("Permission", "cssp.sync.user.remove", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_USER_REMOVE", 3, "31030400");
				// await m_provider.AddClaimTitle("Permission", "cssp.sync.user.update", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_USER_UPDATE", 3, "31030500");
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.sync.senderconfig", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_SENDER_CONFIGURATION", 2, "31040000");
				// await m_provider.AddClaimTitle("Permission", "cssp.sync.senderconfig.list", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_SENDER_CONFIGURATION_LIST", 3, "31040100");
				// await m_provider.AddClaimTitle("Permission", "cssp.sync.senderconfig.view", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_SENDER_CONFIGURATION_VIEW", 3, "31040200");
				// await m_provider.AddClaimTitle("Permission", "cssp.sync.senderconfig.add", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_SENDER_CONFIGURATION_ADD", 3, "31040300");
				// await m_provider.AddClaimTitle("Permission", "cssp.sync.senderconfig.remove", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_SENDER_CONFIGURATION_REMOVE", 3, "31040400");
				// await m_provider.AddClaimTitle("Permission", "cssp.sync.senderconfig.update", "UL_COMMON__CLAIM_NAME_CSSP_SYNC_SENDER_CONFIGURATION_UPDATE", 3, "31040500");
				//
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.logs", "UL_COMMON__CLAIM_NAME_CSSP_LOGS", 1, "81000000");
				// await m_provider.AddClaimTitle("Permission", "cssp.logs.authlogs", "UL_COMMON__CLAIM_NAME_CSSP_LOGS_AUTHLOGS", 2, "81010000");
				// await m_provider.AddClaimTitle("Permission", "cssp.logs.restcontrollogs", "UL_COMMON__CLAIM_NAME_CSSP_LOGS_REST_CONTROL_LOGS", 2, "81020000");
				// await m_provider.AddClaimTitle("Permission", "cssp.logs.restiologs", "UL_COMMON__CLAIM_NAME_CSSP_LOGS_REST_IO_LOGS", 2, "81030000");
				// await m_provider.AddClaimTitle("Permission", "cssp.logs.drcontrollogs", "UL_COMMON__CLAIM_NAME_CSSP_LOGS_DR_CONTROL_LOGS", 2, "81040000");
				// await m_provider.AddClaimTitle("Permission", "cssp.logs.driologs", "UL_COMMON__CLAIM_NAME_CSSP_LOGS_DR_IO_LOGS", 2, "81050000");
				// await m_provider.AddClaimTitle("Permission", "cssp.logs.portallogs", "UL_COMMON__CLAIM_NAME_CSSP_LOGS_PORTAL_LOGS", 2, "81060000");
				// await m_provider.AddClaimTitle("Permission", "cssp.logs.syslogs", "UL_COMMON__CLAIM_NAME_CSSP_LOGS_SYSLOGS", 2, "81070000");
				// await m_provider.AddClaimTitle("Permission", "cssp.logs.smb-syslogs", "UL_COMMON__CLAIM_NAME_CSSP_LOGS_SMB_SYSLOGS", 2, "81080000");
				// await m_provider.AddClaimTitle("Permission", "cssp.logs.alertmessages", "UL_COMMON__CLAIM_NAME_CSSP_LOGS_ALERT_MESSAGES", 2, "81090000");
				//
				// await m_provider.AddClaimTitle("PermissionGroup", "common.account", "UL_COMMON__CLAIM_NAME_COMMON_ACCOUNT", 1, "91000000");
				// await m_provider.AddClaimTitle("PermissionGroup", "common.account.users", "UL_COMMON__CLAIM_NAME_COMMON_ACCOUNT_USERS", 2, "91010000");
				// await m_provider.AddClaimTitle("Permission", "common.account.users.list", "UL_COMMON__CLAIM_NAME_COMMON_ACCOUNT_USERS_LIST", 3, "91010100");
				// await m_provider.AddClaimTitle("Permission", "common.account.users.view", "UL_COMMON__CLAIM_NAME_COMMON_ACCOUNT_USERS_VIEW", 3, "91010200");
				// await m_provider.AddClaimTitle("Permission", "common.account.users.add", "UL_COMMON__CLAIM_NAME_COMMON_ACCOUNT_USERS_CREATE", 3, "91010300");
				// await m_provider.AddClaimTitle("Permission", "common.account.users.update", "UL_COMMON__CLAIM_NAME_COMMON_ACCOUNT_USERS_UPDATE", 3, "91010400");
				// await m_provider.AddClaimTitle("Permission", "common.account.users.remove", "UL_COMMON__CLAIM_NAME_COMMON_ACCOUNT_USERS_DELETE", 3, "91010500");
				// await m_provider.AddClaimTitle("PermissionGroup", "common.account.roles", "UL_COMMON__CLAIM_NAME_COMMON_ACCOUNT_ROLES", 2, "91020000");
				// await m_provider.AddClaimTitle("Permission", "common.account.roles.list", "UL_COMMON__CLAIM_NAME_COMMON_ACCOUNT_ROLES_LIST", 3, "91020100");
				// await m_provider.AddClaimTitle("Permission", "common.account.roles.view", "UL_COMMON__CLAIM_NAME_COMMON_ACCOUNT_ROLES_VIEW", 3, "91020200");
				// await m_provider.AddClaimTitle("Permission", "common.account.roles.add", "UL_COMMON__CLAIM_NAME_COMMON_ACCOUNT_ROLES_CREATE", 3, "91020300");
				// await m_provider.AddClaimTitle("Permission", "common.account.roles.update", "UL_COMMON__CLAIM_NAME_COMMON_ACCOUNT_ROLES_UPDATE", 3, "91020400");
				// await m_provider.AddClaimTitle("Permission", "common.account.roles.remove", "UL_COMMON__CLAIM_NAME_COMMON_ACCOUNT_ROLES_DELETE", 3, "91020500");
				//
				// //
				// // await m_provider.AddClaimTitle("PermissionGroup", "common.account.access_ips", "UL_COMMON__CLAIM_NAME_COMMON_ACCESS_IPS");
				// // await m_provider.AddClaimTitle("Permission", "common.account.access_ips.list", "UL_COMMON__CLAIM_NAME_COMMON_ACCESS_IPS_LIST");
				// // await m_provider.AddClaimTitle("Permission", "common.account.access_ips.add", "UL_COMMON__CLAIM_NAME_COMMON_ACCESS_IPS_CREATE");
				// // await m_provider.AddClaimTitle("Permission", "common.account.access_ips.remove", "UL_COMMON__CLAIM_NAME_COMMON_ACCESS_IPS_DELETE");
				// //
				// // await m_provider.AddClaimTitle("PermissionGroup", "common.logs.useractionlogs", "UL_COMMON__CLAIM_NAME_COMMON_LOGS_USERACTIONLOGS");
				// // await m_provider.AddClaimTitle("Permission", "common.logs.useractionlogs.list", "UL_COMMON__CLAIM_NAME_COMMON_LOGS_USERACTIONLOGS_LIST");
				// //
				// // await m_provider.AddClaimTitle("PermissionGroup", "common.logs.systemlogs", "UL_COMMON__CLAIM_NAME_COMMON_LOGS_SYSTEMLOGS");
				// // await m_provider.AddClaimTitle("Permission", "common.logs.systemlogs.list", "UL_COMMON__CLAIM_NAME_COMMON_LOGS_SYSTEMLOGS_LIST");
				//
				// await m_provider.AddClaimTitle("PermissionGroup", "cssp.settings", "UL_COMMON__CLAIM_NAME_CSSP_SETTINGS", 1, "09000000");
				// await m_provider.AddClaimTitle("Permission", "cssp.settings.update", "UL_COMMON__CLAIM_NAME_CSSP_SETTINGS_UPDATE", 2, "09010000");
				//
				// await m_provider.AddClaimTitle("PermissionGroup", "common.apikey", "UL_COMMON__CLAIM_NAME_COMMON_APIKEY", 1, "10000000");
				// await m_provider.AddClaimTitle("Permission", "common.apikey.list", "UL_COMMON__CLAIM_NAME_COMMON_APIKEY_LIST", 2, "10010000");
				// await m_provider.AddClaimTitle("Permission", "common.apikey.view", "UL_COMMON__CLAIM_NAME_COMMON_APIKEY_VIEW", 2, "10020000");
				// await m_provider.AddClaimTitle("Permission", "common.apikey.add", "UL_COMMON__CLAIM_NAME_COMMON_APIKEY_CREATE", 2, "10030000");
				// await m_provider.AddClaimTitle("Permission", "common.apikey.remove", "UL_COMMON__CLAIM_NAME_COMMON_APIKEY_DELETE", 2, "10040000");
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
		}

		/// <summary>주어진 역할 아이디에 Supervisor 권한을 생성한다.</summary>
		/// <param name="roleId">역할아이디</param>
		public async Task InitializeSupervisor(string roleId)
		{
			try
			{
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.dashboard" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.dashboard.all" });
				//
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.system" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.system.view" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.system.remove" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.system.mds_disk_add" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.system.osd_start" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.system.osd_disk_add" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.system.osd_disk_update" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.system.osd_disk_remove" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.system.osd_disk_start" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.system.auth_start" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.system.rest_start" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.system.s3_start" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.system.smb_start" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.users" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.users.list" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.users.view" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.users.add" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.users.update" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.users.remove" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.fsck" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.fsck.list" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.fsck.view" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.fsck.add" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.fsck.remove" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.systems.fsck.stop" });
				//
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.dr" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.dr.source" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.dr.source.view" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.dr.source.update" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.dr.target" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.dr.target.list" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.dr.target.add" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.dr.target.remove" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.dr.target.apply" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.dr.jobs" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.dr.jobs.list" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.dr.jobs.add" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.dr.jobs.remove" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.dr.jobs.start" });
				//
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.smb" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.smb.list" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.smb.add" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.smb.update" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.smb.remove" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.smb.start" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.smb.ep" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.smb.ep.list" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.smb.ep.add" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.smb.ep.update" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.smb.ep.remove" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.smb.network-manager" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.smb.network-manager.list" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.smb.network-manager.add" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.smb.network-manager.update" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.smb.network-manager.remove" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.s3" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.s3.list" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.s3.view" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.s3.add" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.s3.update" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.s3.remove" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.nas.s3.start" });
				//
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.lb" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.lb.ha-proxy" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.lb.ha-proxy.list" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.lb.ha-proxy.view" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.lb.ha-proxy.add" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.lb.ha-proxy.update" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.lb.ha-proxy.remove" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.lb.ha-proxy.start" });
				//
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.securage" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.securage.filetype" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.securage.filetype.list" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.securage.filetype.view" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.securage.filetype.add" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.securage.filetype.remove" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.securage.filetype.update" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.securage.app" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.securage.app.list" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.securage.app.view" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.securage.app.add" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.securage.app.remove" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.securage.app.update" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.securage.unlockfile" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.securage.unlockfile.list" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.securage.unlockfile.view" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.securage.unlockfile.update" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.securage.unlockfile.request" });
				//
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.job" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.job.list" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.job.view" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.job.add" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.job.remove" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.job.update" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.whitelist" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.whitelist.list" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.whitelist.view" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.whitelist.add" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.whitelist.remove" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.whitelist.update" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.user" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.user.list" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.user.view" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.user.add" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.user.remove" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.user.update" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.senderconfig" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.senderconfig.list" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.senderconfig.view" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.senderconfig.add" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.senderconfig.remove" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.sync.senderconfig.update" });
				//
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.logs" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.logs.authlogs" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.logs.restcontrollogs" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.logs.restiologs" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.logs.drcontrollogs" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.logs.driologs" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.logs.portallogs" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.logs.syslogs" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.logs.smb-syslogs" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.logs.alertmessages" });
				//
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.account" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.account.users" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.account.users.list" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.account.users.view" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.account.users.add" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.account.users.update" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.account.users.remove" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.account.roles" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.account.roles.list" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.account.roles.view" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.account.roles.add" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.account.roles.update" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.account.roles.remove" });
				//
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.settings" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.settings.update" });
				//
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.apikey" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.apikey.list" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.apikey.view" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.apikey.add" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.apikey.remove" });
				//
				// // await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.account.access_ips" });
				// // await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.account.access_ips.list" });
				// // await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.account.access_ips.add" });
				// // await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.account.access_ips.remove" });
				// //
				// // await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.logs.useractionlogs" });
				// // await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.logs.useractionlogs.list" });
				// //
				// // await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.logs.systemlogs" });
				// // await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "common.logs.systemlogs.list" });
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
		}

		/// <summary>주어진 역할 아이디에 InternalService 권한을 생성한다.</summary>
		/// <param name="roleId">역할아이디</param>
#pragma warning disable CS1998
		public async Task InitializeInternalService(string roleId)
		{
			try
			{
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.logs" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.logs.restcontrollogs" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.logs.restiologs" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.logs.drcontrollogs" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.logs.driologs" });
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
		}
#pragma warning restore CS1998

		/// <summary>주어진 역할 아이디에 User 권한을 생성한다.</summary>
		/// <param name="roleId">역할아이디</param>
#pragma warning disable CS1998
		public async Task InitializeUser(string roleId)
		{
			try
			{
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.logs" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.logs.restcontrollogs" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.logs.restiologs" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.logs.drcontrollogs" });
				// await m_provider.AddClaimToRole(roleId, new RequestAddClaimToRole() { ClaimValue = "cssp.logs.driologs" });
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
		}
#pragma warning restore CS1998
	}
}
