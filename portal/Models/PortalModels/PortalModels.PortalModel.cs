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
using System.Linq;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.Extensions.Configuration;

namespace PortalModels
{

	public partial class PortalModel : DbContext
	{

		public PortalModel() :
			base()
		{
			OnCreated();
		}

		public PortalModel(DbContextOptions<PortalModel> options) :
			base(options)
		{
			OnCreated();
		}

		protected override void OnConfiguring(DbContextOptionsBuilder optionsBuilder)
		{
			if (!optionsBuilder.IsConfigured ||
				(!optionsBuilder.Options.Extensions.OfType<RelationalOptionsExtension>().Any(ext => !string.IsNullOrEmpty(ext.ConnectionString) || ext.Connection != null) &&
				 !optionsBuilder.Options.Extensions.Any(ext => !(ext is RelationalOptionsExtension) && !(ext is CoreOptionsExtension))))
			{
				optionsBuilder.UseMySql(GetConnectionString("PortalDatabase"), MySqlServerVersion.LatestSupportedServerVersion);
			}
			CustomizeConfiguration(ref optionsBuilder);
			base.OnConfiguring(optionsBuilder);
		}

		private static string GetConnectionString(string connectionStringName)
		{
			var configurationBuilder = new ConfigurationBuilder().AddJsonFile("appsettings.json", optional: true, reloadOnChange: false);
			var configuration = configurationBuilder.Build();
			return configuration.GetConnectionString(connectionStringName);
		}

		partial void CustomizeConfiguration(ref DbContextOptionsBuilder optionsBuilder);

		public virtual DbSet<NetworkInterface> NetworkInterfaces { get; set; }
		public virtual DbSet<NetworkInterfaceVlan> NetworkInterfaceVlans { get; set; }
		public virtual DbSet<Service> Services { get; set; }
		public virtual DbSet<ServiceNetworkInterfaceVlan> ServiceNetworkInterfaceVlans { get; set; }
		public virtual DbSet<Server> Servers { get; set; }
		public virtual DbSet<ServiceGroup> ServiceGroups { get; set; }
		public virtual DbSet<ApiKey> ApiKeys { get; set; }
		public virtual DbSet<Config> Configs { get; set; }
		public virtual DbSet<SystemLog> SystemLogs { get; set; }
		public virtual DbSet<AllowedConnectionIp> AllowedConnectionIps { get; set; }
		public virtual DbSet<UserLoginHistory> UserLoginHistories { get; set; }
		public virtual DbSet<UserClaim> UserClaims { get; set; }
		public virtual DbSet<Role> Roles { get; set; }
		public virtual DbSet<RoleClaim> RoleClaims { get; set; }
		public virtual DbSet<ClaimName> ClaimNames { get; set; }
		public virtual DbSet<UserRole> UserRoles { get; set; }
		public virtual DbSet<User> Users { get; set; }
		public virtual DbSet<UserActionLog> UserActionLogs { get; set; }
		public virtual DbSet<Disk> Disks { get; set; }
		public virtual DbSet<ServiceDisk> ServiceDisks { get; set; }
		public virtual DbSet<DiskPool> DiskPools { get; set; }
		public virtual DbSet<ServerUsage> ServerUsages { get; set; }
		public virtual DbSet<NetworkInterfaceUsage> NetworkInterfaceUsages { get; set; }
		public virtual DbSet<ServiceUsage> ServiceUsages { get; set; }
		public virtual DbSet<ServiceConfig> ServiceConfigs { get; set; }
		public virtual DbSet<KsanUser> KsanUsers { get; set; }
		public virtual DbSet<UserDiskPool> UserDiskPools { get; set; }
		public virtual DbSet<DiskUsage> DiskUsages { get; set; }
		public virtual DbSet<Region> Regions { get; set; }
		
		protected override void OnModelCreating(ModelBuilder modelBuilder)
		{
			base.OnModelCreating(modelBuilder);

			this.NetworkInterfaceMapping(modelBuilder);
			this.CustomizeNetworkInterfaceMapping(modelBuilder);

			this.NetworkInterfaceVlanMapping(modelBuilder);
			this.CustomizeNetworkInterfaceVlanMapping(modelBuilder);

			this.ServiceMapping(modelBuilder);
			this.CustomizeServiceMapping(modelBuilder);

			this.ServiceNetworkInterfaceVlanMapping(modelBuilder);
			this.CustomizeServiceNetworkInterfaceVlanMapping(modelBuilder);

			this.ServerMapping(modelBuilder);
			this.CustomizeServerMapping(modelBuilder);

			this.ServiceGroupMapping(modelBuilder);
			this.CustomizeServiceGroupMapping(modelBuilder);

			this.ApiKeyMapping(modelBuilder);
			this.CustomizeApiKeyMapping(modelBuilder);

			this.ConfigMapping(modelBuilder);
			this.CustomizeConfigMapping(modelBuilder);

			this.SystemLogMapping(modelBuilder);
			this.CustomizeSystemLogMapping(modelBuilder);

			this.AllowedConnectionIpMapping(modelBuilder);
			this.CustomizeAllowedConnectionIpMapping(modelBuilder);

			this.UserLoginHistoryMapping(modelBuilder);
			this.CustomizeUserLoginHistoryMapping(modelBuilder);

			this.UserClaimMapping(modelBuilder);
			this.CustomizeUserClaimMapping(modelBuilder);

			this.RoleMapping(modelBuilder);
			this.CustomizeRoleMapping(modelBuilder);

			this.RoleClaimMapping(modelBuilder);
			this.CustomizeRoleClaimMapping(modelBuilder);

			this.ClaimNameMapping(modelBuilder);
			this.CustomizeClaimNameMapping(modelBuilder);

			this.UserRoleMapping(modelBuilder);
			this.CustomizeUserRoleMapping(modelBuilder);

			this.UserMapping(modelBuilder);
			this.CustomizeUserMapping(modelBuilder);

			this.UserActionLogMapping(modelBuilder);
			this.CustomizeUserActionLogMapping(modelBuilder);

			this.DiskMapping(modelBuilder);
			this.CustomizeDiskMapping(modelBuilder);

			this.ServiceDiskMapping(modelBuilder);
			this.CustomizeServiceDiskMapping(modelBuilder);

			this.DiskPoolMapping(modelBuilder);
			this.CustomizeDiskPoolMapping(modelBuilder);

			this.ServerUsageMapping(modelBuilder);
			this.CustomizeServerUsageMapping(modelBuilder);

			this.NetworkInterfaceUsageMapping(modelBuilder);
			this.CustomizeNetworkInterfaceUsageMapping(modelBuilder);

			this.ServiceUsageMapping(modelBuilder);
			this.CustomizeServiceUsageMapping(modelBuilder);

			this.ServiceConfigMapping(modelBuilder);
			this.CustomizeServiceConfigMapping(modelBuilder);

			this.KsanUserMapping(modelBuilder);
			this.CustomizeKsanUserMapping(modelBuilder);

			this.UserDiskPoolMapping(modelBuilder);
			this.CustomizeUserDiskPoolMapping(modelBuilder);

			this.DiskUsageMapping(modelBuilder);
			this.CustomizeDiskUsageMapping(modelBuilder);

			this.RegionMapping(modelBuilder);
			this.CustomizeRegionMapping(modelBuilder);

			RelationshipsMapping(modelBuilder);
			CustomizeMapping(ref modelBuilder);
		}

		#region NetworkInterface Mapping

		private void NetworkInterfaceMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<NetworkInterface>().ToTable(@"NETWORK_INTERFACES");
			modelBuilder.Entity<NetworkInterface>().Property(x => x.Id).HasColumnName(@"ID").IsRequired().ValueGeneratedNever().HasMaxLength(36);
			modelBuilder.Entity<NetworkInterface>().Property(x => x.ServerId).HasColumnName(@"SERVER_ID").HasColumnType(@"char(36)").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<NetworkInterface>().Property(x => x.Name).HasColumnName(@"NAME").IsRequired().ValueGeneratedNever().HasMaxLength(100);
			modelBuilder.Entity<NetworkInterface>().Property(x => x.Description).HasColumnName(@"DESCRIPTION").ValueGeneratedNever().HasMaxLength(4000);
			modelBuilder.Entity<NetworkInterface>().Property(x => x.Dhcp).HasColumnName(@"DHCP").ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<NetworkInterface>().Property(x => x.MacAddress).HasColumnName(@"MAC_ADDRESS").ValueGeneratedNever().HasMaxLength(20);
			modelBuilder.Entity<NetworkInterface>().Property(x => x.LinkState).HasColumnName(@"LINK_STATE").ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<NetworkInterface>().Property(x => x.IpAddress).HasColumnName(@"IP_ADDRESS").ValueGeneratedNever().HasMaxLength(20);
			modelBuilder.Entity<NetworkInterface>().Property(x => x.SubnetMask).HasColumnName(@"SUBNET_MASK").ValueGeneratedNever().HasMaxLength(20);
			modelBuilder.Entity<NetworkInterface>().Property(x => x.Gateway).HasColumnName(@"GATEWAY").ValueGeneratedNever().HasMaxLength(20);
			modelBuilder.Entity<NetworkInterface>().Property(x => x.Dns1).HasColumnName(@"DNS1").ValueGeneratedNever().HasMaxLength(20);
			modelBuilder.Entity<NetworkInterface>().Property(x => x.Dns2).HasColumnName(@"DNS2").ValueGeneratedNever().HasMaxLength(20);
			modelBuilder.Entity<NetworkInterface>().Property(x => x.BandWidth).HasColumnName(@"BAND_WIDTH").ValueGeneratedNever().HasMaxLength(12).HasDefaultValueSql(@"0");
			modelBuilder.Entity<NetworkInterface>().Property(x => x.IsManagement).HasColumnName(@"IS_MANAGEMENT").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<NetworkInterface>().Property(x => x.Rx).HasColumnName(@"RX").ValueGeneratedNever().HasMaxLength(12).HasDefaultValueSql(@"0");
			modelBuilder.Entity<NetworkInterface>().Property(x => x.Tx).HasColumnName(@"TX").ValueGeneratedNever().HasMaxLength(12).HasDefaultValueSql(@"0");
			modelBuilder.Entity<NetworkInterface>().Property(x => x.RegId).HasColumnName(@"REG_ID").ValueGeneratedNever();
			modelBuilder.Entity<NetworkInterface>().Property(x => x.RegName).HasColumnName(@"REG_NAME").ValueGeneratedOnAdd().HasMaxLength(255);
			modelBuilder.Entity<NetworkInterface>().Property(x => x.RegDate).HasColumnName(@"REG_DATE").ValueGeneratedNever();
			modelBuilder.Entity<NetworkInterface>().Property(x => x.ModId).HasColumnName(@"MOD_ID").ValueGeneratedNever();
			modelBuilder.Entity<NetworkInterface>().Property(x => x.ModName).HasColumnName(@"MOD_NAME").ValueGeneratedOnAdd().HasMaxLength(255);
			modelBuilder.Entity<NetworkInterface>().Property(x => x.ModDate).HasColumnName(@"MOD_DATE").ValueGeneratedNever();
			modelBuilder.Entity<NetworkInterface>().HasKey(@"Id");
		}

		partial void CustomizeNetworkInterfaceMapping(ModelBuilder modelBuilder);

		#endregion

		#region NetworkInterfaceVlan Mapping

		private void NetworkInterfaceVlanMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<NetworkInterfaceVlan>().ToTable(@"NETWORK_INTERFACE_VLANS");
			modelBuilder.Entity<NetworkInterfaceVlan>().Property(x => x.Id).HasColumnName(@"ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<NetworkInterfaceVlan>().Property(x => x.InterfaceId).HasColumnName(@"INTERFACE_ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<NetworkInterfaceVlan>().Property(x => x.Tag).HasColumnName(@"TAG").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<NetworkInterfaceVlan>().Property(x => x.IpAddress).HasColumnName(@"IP_ADDRESS").ValueGeneratedNever();
			modelBuilder.Entity<NetworkInterfaceVlan>().Property(x => x.SubnetMask).HasColumnName(@"SUBNET_MASK").ValueGeneratedNever();
			modelBuilder.Entity<NetworkInterfaceVlan>().Property(x => x.Gateway).HasColumnName(@"GATEWAY").ValueGeneratedNever();
			modelBuilder.Entity<NetworkInterfaceVlan>().Property(x => x.RegId).HasColumnName(@"REG_ID").ValueGeneratedNever();
			modelBuilder.Entity<NetworkInterfaceVlan>().Property(x => x.RegName).HasColumnName(@"REG_NAME").ValueGeneratedOnAdd().HasMaxLength(255);
			modelBuilder.Entity<NetworkInterfaceVlan>().Property(x => x.RegDate).HasColumnName(@"REG_DATE").ValueGeneratedNever();
			modelBuilder.Entity<NetworkInterfaceVlan>().Property(x => x.ModId).HasColumnName(@"MOD_ID").ValueGeneratedNever();
			modelBuilder.Entity<NetworkInterfaceVlan>().Property(x => x.ModName).HasColumnName(@"MOD_NAME").ValueGeneratedOnAdd().HasMaxLength(255);
			modelBuilder.Entity<NetworkInterfaceVlan>().Property(x => x.ModDate).HasColumnName(@"MOD_DATE").ValueGeneratedNever();
			modelBuilder.Entity<NetworkInterfaceVlan>().HasKey(@"Id");
		}

		partial void CustomizeNetworkInterfaceVlanMapping(ModelBuilder modelBuilder);

		#endregion

		#region Service Mapping

		private void ServiceMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<Service>().ToTable(@"SERVICES");
			modelBuilder.Entity<Service>().Property(x => x.Id).HasColumnName(@"ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<Service>().Property(x => x.GroupId).HasColumnName(@"GROUP_ID").ValueGeneratedNever();
			modelBuilder.Entity<Service>().Property(x => x.Name).HasColumnName(@"NAME").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<Service>().Property(x => x.Description).HasColumnName(@"DESCRIPTION").ValueGeneratedNever();
			modelBuilder.Entity<Service>().Property(x => x.ServiceType).HasColumnName(@"SERVICE_TYPE").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<Service>().Property(x => x.HaAction).HasColumnName(@"HA_ACTION").IsRequired().ValueGeneratedNever().HasDefaultValueSql(@"-1");
			modelBuilder.Entity<Service>().Property(x => x.State).HasColumnName(@"STATE").IsRequired().ValueGeneratedNever().HasDefaultValueSql(@"-2");
			modelBuilder.Entity<Service>().Property(x => x.CpuUsage).HasColumnName(@"CPU_USAGE").ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<Service>().Property(x => x.MemoryTotal).HasColumnName(@"MEMORY_TOTAL").ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<Service>().Property(x => x.MemoryUsed).HasColumnName(@"MEMORY_USED").ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<Service>().Property(x => x.ThreadCount).HasColumnName(@"THREAD_COUNT").IsRequired().ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<Service>().Property(x => x.RegId).HasColumnName(@"REG_ID").ValueGeneratedNever();
			modelBuilder.Entity<Service>().Property(x => x.RegName).HasColumnName(@"REG_NAME").ValueGeneratedOnAdd().HasMaxLength(255);
			modelBuilder.Entity<Service>().Property(x => x.RegDate).HasColumnName(@"REG_DATE").ValueGeneratedNever();
			modelBuilder.Entity<Service>().Property(x => x.ModId).HasColumnName(@"MOD_ID").ValueGeneratedNever();
			modelBuilder.Entity<Service>().Property(x => x.ModName).HasColumnName(@"MOD_NAME").ValueGeneratedOnAdd().HasMaxLength(255);
			modelBuilder.Entity<Service>().Property(x => x.ModDate).HasColumnName(@"MOD_DATE").ValueGeneratedNever();
			modelBuilder.Entity<Service>().HasKey(@"Id");
			modelBuilder.Entity<Service>().HasIndex(@"Name").IsUnique(true);
		}

		partial void CustomizeServiceMapping(ModelBuilder modelBuilder);

		#endregion

		#region ServiceNetworkInterfaceVlan Mapping

		private void ServiceNetworkInterfaceVlanMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<ServiceNetworkInterfaceVlan>().ToTable(@"SERVICE_NETWORK_INTERFACE_VLANS");
			modelBuilder.Entity<ServiceNetworkInterfaceVlan>().Property(x => x.ServiceId).HasColumnName(@"SERVICE_ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<ServiceNetworkInterfaceVlan>().Property(x => x.VlanId).HasColumnName(@"VLAN_ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<ServiceNetworkInterfaceVlan>().HasKey(@"ServiceId", @"VlanId");
		}

		partial void CustomizeServiceNetworkInterfaceVlanMapping(ModelBuilder modelBuilder);

		#endregion

		#region Server Mapping

		private void ServerMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<Server>().ToTable(@"SERVERS");
			modelBuilder.Entity<Server>().Property(x => x.Id).HasColumnName(@"ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<Server>().Property(x => x.Name).HasColumnName(@"NAME").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<Server>().Property(x => x.Description).HasColumnName(@"DESCRIPTION").ValueGeneratedNever();
			modelBuilder.Entity<Server>().Property(x => x.CpuModel).HasColumnName(@"CPU_MODEL").ValueGeneratedNever();
			modelBuilder.Entity<Server>().Property(x => x.Clock).HasColumnName(@"CLOCK").ValueGeneratedNever();
			modelBuilder.Entity<Server>().Property(x => x.State).HasColumnName(@"STATE").IsRequired().ValueGeneratedNever().HasDefaultValueSql(@"-2");
			modelBuilder.Entity<Server>().Property(x => x.Rack).HasColumnName(@"RACK").ValueGeneratedNever();
			modelBuilder.Entity<Server>().Property(x => x.LoadAverage1M).HasColumnName(@"LOAD_AVERAGE1M").ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<Server>().Property(x => x.LoadAverage5M).HasColumnName(@"LOAD_AVERAGE5M").ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<Server>().Property(x => x.LoadAverage15M).HasColumnName(@"LOAD_AVERAGE15M").ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<Server>().Property(x => x.MemoryTotal).HasColumnName(@"MEMORY_TOTAL").ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<Server>().Property(x => x.MemoryUsed).HasColumnName(@"MEMORY_USED").ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<Server>().Property(x => x.ModId).HasColumnName(@"MOD_ID").ValueGeneratedNever();
			modelBuilder.Entity<Server>().Property(x => x.ModName).HasColumnName(@"MOD_NAME").ValueGeneratedOnAdd().HasMaxLength(255);
			modelBuilder.Entity<Server>().Property(x => x.ModDate).HasColumnName(@"MOD_DATE").ValueGeneratedNever();
			modelBuilder.Entity<Server>().HasKey(@"Id");
			modelBuilder.Entity<Server>().HasIndex(@"Name").IsUnique(true);
		}

		partial void CustomizeServerMapping(ModelBuilder modelBuilder);

		#endregion

		#region ServiceGroup Mapping

		private void ServiceGroupMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<ServiceGroup>().ToTable(@"SERVICE_GROUPS");
			modelBuilder.Entity<ServiceGroup>().Property(x => x.Id).HasColumnName(@"ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<ServiceGroup>().Property(x => x.Name).HasColumnName(@"NAME").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<ServiceGroup>().Property(x => x.Description).HasColumnName(@"DESCRIPTION").ValueGeneratedNever();
			modelBuilder.Entity<ServiceGroup>().Property(x => x.ServiceType).HasColumnName(@"SERVICE_TYPE").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<ServiceGroup>().Property(x => x.ServiceIpAddress).HasColumnName(@"SERVICE_IP_ADDRESS").ValueGeneratedNever();
			modelBuilder.Entity<ServiceGroup>().Property(x => x.RegId).HasColumnName(@"REG_ID").ValueGeneratedNever();
			modelBuilder.Entity<ServiceGroup>().Property(x => x.RegName).HasColumnName(@"REG_NAME").ValueGeneratedOnAdd().HasMaxLength(255);
			modelBuilder.Entity<ServiceGroup>().Property(x => x.RegDate).HasColumnName(@"REG_DATE").ValueGeneratedNever();
			modelBuilder.Entity<ServiceGroup>().Property(x => x.ModId).HasColumnName(@"MOD_ID").ValueGeneratedNever();
			modelBuilder.Entity<ServiceGroup>().Property(x => x.ModName).HasColumnName(@"MOD_NAME").ValueGeneratedOnAdd().HasMaxLength(255);
			modelBuilder.Entity<ServiceGroup>().Property(x => x.ModDate).HasColumnName(@"MOD_DATE").ValueGeneratedNever();
			modelBuilder.Entity<ServiceGroup>().HasKey(@"Id");
		}

		partial void CustomizeServiceGroupMapping(ModelBuilder modelBuilder);

		#endregion

		#region ApiKey Mapping

		private void ApiKeyMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<ApiKey>().ToTable(@"API_KEYS");
			modelBuilder.Entity<ApiKey>().Property(x => x.KeyId).HasColumnName(@"KEY_ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<ApiKey>().Property(x => x.UserId).HasColumnName(@"USER_ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<ApiKey>().Property(x => x.KeyName).HasColumnName(@"KEY_NAME").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<ApiKey>().Property(x => x.ExpireDate).HasColumnName(@"EXPIRE_DATE").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<ApiKey>().Property(x => x.KeyValue).HasColumnName(@"KEY_VALUE").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<ApiKey>().HasKey(@"KeyId");
		}

		partial void CustomizeApiKeyMapping(ModelBuilder modelBuilder);

		#endregion

		#region Config Mapping

		private void ConfigMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<Config>().ToTable(@"CONFIGS");
			modelBuilder.Entity<Config>().Property(x => x.Key).HasColumnName(@"KEY").IsRequired().ValueGeneratedNever().HasMaxLength(255);
			modelBuilder.Entity<Config>().Property(x => x.Value).HasColumnName(@"VALUE").ValueGeneratedNever().HasMaxLength(255);
			modelBuilder.Entity<Config>().HasKey(@"Key");
		}

		partial void CustomizeConfigMapping(ModelBuilder modelBuilder);

		#endregion

		#region SystemLog Mapping

		private void SystemLogMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<SystemLog>().ToTable(@"SYSTEM_LOGS");
			modelBuilder.Entity<SystemLog>().Property(x => x.LogId).HasColumnName(@"LOG_ID").IsRequired().ValueGeneratedOnAdd().HasPrecision(20, 0);
			modelBuilder.Entity<SystemLog>().Property(x => x.LogLevel).HasColumnName(@"LOG_LEVEL").IsRequired().ValueGeneratedNever().HasPrecision(6, 0);
			modelBuilder.Entity<SystemLog>().Property(x => x.RegDate).HasColumnName(@"REG_DATE").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<SystemLog>().Property(x => x.Message).HasColumnName(@"MESSAGE").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<SystemLog>().HasKey(@"LogId");
			modelBuilder.Entity<SystemLog>().HasIndex(@"LogId").IsUnique(true);
		}

		partial void CustomizeSystemLogMapping(ModelBuilder modelBuilder);

		#endregion

		#region AllowedConnectionIp Mapping

		private void AllowedConnectionIpMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<AllowedConnectionIp>().ToTable(@"ALLOWED_CONNECTION_IPS");
			modelBuilder.Entity<AllowedConnectionIp>().Property(x => x.Id).HasColumnName(@"ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<AllowedConnectionIp>().Property(x => x.RoleId).HasColumnName(@"ROLE_ID").ValueGeneratedNever();
			modelBuilder.Entity<AllowedConnectionIp>().Property(x => x.IpAddress).HasColumnName(@"IP_ADDRESS").IsRequired().ValueGeneratedNever().HasMaxLength(50);
			modelBuilder.Entity<AllowedConnectionIp>().Property(x => x.StartAddress).HasColumnName(@"START_ADDRESS").IsRequired().ValueGeneratedNever().HasPrecision(20, 0);
			modelBuilder.Entity<AllowedConnectionIp>().Property(x => x.EndAddress).HasColumnName(@"END_ADDRESS").IsRequired().ValueGeneratedNever().HasPrecision(20, 0);
			modelBuilder.Entity<AllowedConnectionIp>().Property(x => x.RegId).HasColumnName(@"REG_ID").ValueGeneratedNever();
			modelBuilder.Entity<AllowedConnectionIp>().Property(x => x.RegName).HasColumnName(@"REG_NAME").IsRequired().ValueGeneratedNever().HasMaxLength(1000);
			modelBuilder.Entity<AllowedConnectionIp>().Property(x => x.RegDate).HasColumnName(@"REG_DATE").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<AllowedConnectionIp>().HasKey(@"Id");
		}

		partial void CustomizeAllowedConnectionIpMapping(ModelBuilder modelBuilder);

		#endregion

		#region UserLoginHistory Mapping

		private void UserLoginHistoryMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<UserLoginHistory>().ToTable(@"USER_LOGIN_HISTORIES");
			modelBuilder.Entity<UserLoginHistory>().Property(x => x.Id).HasColumnName(@"ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<UserLoginHistory>().Property(x => x.LoginDate).HasColumnName(@"LOGIN_DATE").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<UserLoginHistory>().HasKey(@"Id", @"LoginDate");
		}

		partial void CustomizeUserLoginHistoryMapping(ModelBuilder modelBuilder);

		#endregion

		#region UserClaim Mapping

		private void UserClaimMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<UserClaim>().ToTable(@"USER_CLAIMS");
			modelBuilder.Entity<UserClaim>().Property(x => x.Id).HasColumnName(@"Id").IsRequired().ValueGeneratedOnAdd().HasPrecision(11, 0);
			modelBuilder.Entity<UserClaim>().Property(x => x.ClaimType).HasColumnName(@"ClaimType").ValueGeneratedNever().HasMaxLength(255);
			modelBuilder.Entity<UserClaim>().Property(x => x.ClaimValue).HasColumnName(@"ClaimValue").ValueGeneratedNever().HasMaxLength(255);
			modelBuilder.Entity<UserClaim>().Property(x => x.UserId).HasColumnName(@"UserId").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<UserClaim>().HasKey(@"Id");
			modelBuilder.Entity<UserClaim>().HasIndex(@"Id").IsUnique(true);
		}

		partial void CustomizeUserClaimMapping(ModelBuilder modelBuilder);

		#endregion

		#region Role Mapping

		private void RoleMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<Role>().ToTable(@"ROLES");
			modelBuilder.Entity<Role>().Property(x => x.Id).HasColumnName(@"Id").HasColumnType(@"char(36)").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<Role>().Property(x => x.ConcurrencyStamp).HasColumnName(@"ConcurrencyStamp").HasColumnType(@"varchar(255)").ValueGeneratedNever().HasMaxLength(255);
			modelBuilder.Entity<Role>().Property(x => x.Name).HasColumnName(@"Name").ValueGeneratedNever().HasMaxLength(255);
			modelBuilder.Entity<Role>().Property(x => x.NormalizedName).HasColumnName(@"NormalizedName").ValueGeneratedNever().HasMaxLength(255);
			modelBuilder.Entity<Role>().HasKey(@"Id");
			modelBuilder.Entity<Role>().HasIndex(@"NormalizedName").IsUnique(true);
		}

		partial void CustomizeRoleMapping(ModelBuilder modelBuilder);

		#endregion

		#region RoleClaim Mapping

		private void RoleClaimMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<RoleClaim>().ToTable(@"ROLE_CLAIMS");
			modelBuilder.Entity<RoleClaim>().Property(x => x.Id).HasColumnName(@"Id").IsRequired().ValueGeneratedOnAdd().HasPrecision(11, 0);
			modelBuilder.Entity<RoleClaim>().Property(x => x.ClaimType).HasColumnName(@"ClaimType").ValueGeneratedNever().HasMaxLength(255);
			modelBuilder.Entity<RoleClaim>().Property(x => x.ClaimValue).HasColumnName(@"ClaimValue").ValueGeneratedNever().HasMaxLength(255);
			modelBuilder.Entity<RoleClaim>().Property(x => x.RoleId).HasColumnName(@"RoleId").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<RoleClaim>().HasKey(@"Id");
			modelBuilder.Entity<RoleClaim>().HasIndex(@"Id").IsUnique(true);
		}

		partial void CustomizeRoleClaimMapping(ModelBuilder modelBuilder);

		#endregion

		#region ClaimName Mapping

		private void ClaimNameMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<ClaimName>().ToTable(@"CLAIM_NAMES");
			modelBuilder.Entity<ClaimName>().Property(x => x.Id).HasColumnName(@"Id").IsRequired().ValueGeneratedOnAdd().HasPrecision(11, 0);
			modelBuilder.Entity<ClaimName>().Property(x => x.ClaimTitle).HasColumnName(@"ClaimTitle").IsRequired().ValueGeneratedNever().HasMaxLength(2000);
			modelBuilder.Entity<ClaimName>().Property(x => x.ClaimType).HasColumnName(@"ClaimType").IsRequired().ValueGeneratedNever().HasMaxLength(200);
			modelBuilder.Entity<ClaimName>().Property(x => x.ClaimValue).HasColumnName(@"ClaimValue").IsRequired().ValueGeneratedNever().HasMaxLength(200);
			modelBuilder.Entity<ClaimName>().Property(x => x.Depth).HasColumnName(@"Depth").ValueGeneratedNever();
			modelBuilder.Entity<ClaimName>().Property(x => x.OrderNo).HasColumnName(@"OrderNo").ValueGeneratedNever();
			modelBuilder.Entity<ClaimName>().HasKey(@"Id");
			modelBuilder.Entity<ClaimName>().HasIndex(@"Id").IsUnique(true);
			modelBuilder.Entity<ClaimName>().HasIndex(@"ClaimType", @"ClaimValue").IsUnique(true).HasDatabaseName(@"KEY1");
		}

		partial void CustomizeClaimNameMapping(ModelBuilder modelBuilder);

		#endregion

		#region UserRole Mapping

		private void UserRoleMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<UserRole>().ToTable(@"USER_ROLES");
			modelBuilder.Entity<UserRole>().Property(x => x.UserId).HasColumnName(@"UserId").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<UserRole>().Property(x => x.RoleId).HasColumnName(@"RoleId").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<UserRole>().HasKey(@"UserId", @"RoleId");
		}

		partial void CustomizeUserRoleMapping(ModelBuilder modelBuilder);

		#endregion

		#region User Mapping

		private void UserMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<User>().ToTable(@"USERS");
			modelBuilder.Entity<User>().Property(x => x.Id).HasColumnName(@"Id").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<User>().Property(x => x.AccessFailedCount).HasColumnName(@"AccessFailedCount").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<User>().Property(x => x.Code).HasColumnName(@"Code").ValueGeneratedNever().HasMaxLength(255);
			modelBuilder.Entity<User>().Property(x => x.ConcurrencyStamp).HasColumnName(@"ConcurrencyStamp").IsRequired().ValueGeneratedNever().HasMaxLength(255);
			modelBuilder.Entity<User>().Property(x => x.Email).HasColumnName(@"Email").ValueGeneratedNever().HasMaxLength(255);
			modelBuilder.Entity<User>().Property(x => x.EmailConfirmed).HasColumnName(@"EmailConfirmed").IsRequired().ValueGeneratedNever().HasPrecision(1);
			modelBuilder.Entity<User>().Property(x => x.LockoutEnabled).HasColumnName(@"LockoutEnabled").IsRequired().ValueGeneratedNever().HasPrecision(1);
			modelBuilder.Entity<User>().Property(x => x.LockoutEnd).HasColumnName(@"LockoutEnd").ValueGeneratedOnAdd();
			modelBuilder.Entity<User>().Property(x => x.Name).HasColumnName(@"Name").ValueGeneratedNever().HasMaxLength(255);
			modelBuilder.Entity<User>().Property(x => x.NormalizedEmail).HasColumnName(@"NormalizedEmail").ValueGeneratedNever().HasMaxLength(255);
			modelBuilder.Entity<User>().Property(x => x.NormalizedUserName).HasColumnName(@"NormalizedUserName").ValueGeneratedNever().HasMaxLength(255);
			modelBuilder.Entity<User>().Property(x => x.PasswordHash).HasColumnName(@"PasswordHash").ValueGeneratedNever().HasMaxLength(255);
			modelBuilder.Entity<User>().Property(x => x.PhoneNumber).HasColumnName(@"PhoneNumber").ValueGeneratedNever().HasMaxLength(255);
			modelBuilder.Entity<User>().Property(x => x.PhoneNumberConfirmed).HasColumnName(@"PhoneNumberConfirmed").IsRequired().ValueGeneratedNever().HasPrecision(1);
			modelBuilder.Entity<User>().Property(x => x.SecurityStamp).HasColumnName(@"SecurityStamp").ValueGeneratedNever().HasMaxLength(255);
			modelBuilder.Entity<User>().Property(x => x.TwoFactorEnabled).HasColumnName(@"TwoFactorEnabled").IsRequired().ValueGeneratedNever().HasPrecision(1);
			modelBuilder.Entity<User>().Property(x => x.LoginId).HasColumnName(@"LoginId").ValueGeneratedNever().HasMaxLength(255);
			modelBuilder.Entity<User>().Property(x => x.IsDeleted).HasColumnName(@"IsDeleted").IsRequired().ValueGeneratedNever().HasPrecision(1, 0).HasDefaultValueSql(@"0");
			modelBuilder.Entity<User>().Property(x => x.PasswordChangeDate).HasColumnName(@"PasswordChangeDate").ValueGeneratedNever();
			modelBuilder.Entity<User>().HasKey(@"Id");
			modelBuilder.Entity<User>().HasIndex(@"NormalizedUserName").IsUnique(true);
		}

		partial void CustomizeUserMapping(ModelBuilder modelBuilder);

		#endregion

		#region UserActionLog Mapping

		private void UserActionLogMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<UserActionLog>().ToTable(@"USER_ACTION_LOGS");
			modelBuilder.Entity<UserActionLog>().Property(x => x.LogId).HasColumnName(@"LOG_ID").IsRequired().ValueGeneratedOnAdd().HasPrecision(20, 0);
			modelBuilder.Entity<UserActionLog>().Property(x => x.LogLevel).HasColumnName(@"LOG_LEVEL").IsRequired().ValueGeneratedNever().HasPrecision(6, 0);
			modelBuilder.Entity<UserActionLog>().Property(x => x.IpAddress).HasColumnName(@"IP_ADDRESS").IsRequired().ValueGeneratedNever().HasMaxLength(50);
			modelBuilder.Entity<UserActionLog>().Property(x => x.Message).HasColumnName(@"MESSAGE").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<UserActionLog>().Property(x => x.UserId).HasColumnName(@"USER_ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<UserActionLog>().Property(x => x.UserName).HasColumnName(@"USER_NAME").IsRequired().ValueGeneratedNever().HasMaxLength(1000);
			modelBuilder.Entity<UserActionLog>().Property(x => x.RegDate).HasColumnName(@"REG_DATE").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<UserActionLog>().HasKey(@"LogId");
			modelBuilder.Entity<UserActionLog>().HasIndex(@"LogId").IsUnique(true);
		}

		partial void CustomizeUserActionLogMapping(ModelBuilder modelBuilder);

		#endregion

		#region Disk Mapping

		private void DiskMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<Disk>().ToTable(@"DISKS");
			modelBuilder.Entity<Disk>().Property(x => x.Id).HasColumnName(@"ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<Disk>().Property(x => x.DiskPoolId).HasColumnName(@"DISK_POOL_ID").ValueGeneratedNever();
			modelBuilder.Entity<Disk>().Property(x => x.DiskNo).HasColumnName(@"DISK_NO").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<Disk>().Property(x => x.ServerId).HasColumnName(@"SERVER_ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<Disk>().Property(x => x.Path).HasColumnName(@"PATH").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<Disk>().Property(x => x.State).HasColumnName(@"STATE").IsRequired().ValueGeneratedNever().HasDefaultValueSql(@"-2");
			modelBuilder.Entity<Disk>().Property(x => x.TotalInode).HasColumnName(@"TOTAL_INODE").IsRequired().ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<Disk>().Property(x => x.ReservedInode).HasColumnName(@"RESERVED_INODE").IsRequired().ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<Disk>().Property(x => x.UsedInode).HasColumnName(@"USED_INODE").IsRequired().ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<Disk>().Property(x => x.TotalSize).HasColumnName(@"TOTAL_SIZE").IsRequired().ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<Disk>().Property(x => x.ReservedSize).HasColumnName(@"RESERVED_SIZE").IsRequired().ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<Disk>().Property(x => x.UsedSize).HasColumnName(@"USED_SIZE").IsRequired().ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<Disk>().Property(x => x.Read).HasColumnName(@"READ").IsRequired().ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<Disk>().Property(x => x.Write).HasColumnName(@"WRITE").IsRequired().ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<Disk>().Property(x => x.RwMode).HasColumnName(@"RW_MODE").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<Disk>().HasKey(@"Id");
		}

		partial void CustomizeDiskMapping(ModelBuilder modelBuilder);

		#endregion

		#region ServiceDisk Mapping

		private void ServiceDiskMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<ServiceDisk>().ToTable(@"SERVICE_DISKS");
			modelBuilder.Entity<ServiceDisk>().Property(x => x.ServiceId).HasColumnName(@"SERVICE_ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<ServiceDisk>().Property(x => x.DiskId).HasColumnName(@"DISK_ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<ServiceDisk>().HasKey(@"ServiceId", @"DiskId");
		}

		partial void CustomizeServiceDiskMapping(ModelBuilder modelBuilder);

		#endregion

		#region DiskPool Mapping

		private void DiskPoolMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<DiskPool>().ToTable(@"DISK_POOLS");
			modelBuilder.Entity<DiskPool>().Property(x => x.Id).HasColumnName(@"ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<DiskPool>().Property(x => x.Name).HasColumnName(@"NAME").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<DiskPool>().Property(x => x.Description).HasColumnName(@"DESCRIPTION").ValueGeneratedNever();
			modelBuilder.Entity<DiskPool>().Property(x => x.RegId).HasColumnName(@"REG_ID").ValueGeneratedNever();
			modelBuilder.Entity<DiskPool>().Property(x => x.RegName).HasColumnName(@"REG_NAME").ValueGeneratedOnAdd().HasMaxLength(255);
			modelBuilder.Entity<DiskPool>().Property(x => x.RegDate).HasColumnName(@"REG_DATE").ValueGeneratedNever();
			modelBuilder.Entity<DiskPool>().Property(x => x.ModId).HasColumnName(@"MOD_ID").ValueGeneratedNever();
			modelBuilder.Entity<DiskPool>().Property(x => x.ModName).HasColumnName(@"MOD_NAME").ValueGeneratedOnAdd().HasMaxLength(255);
			modelBuilder.Entity<DiskPool>().Property(x => x.ModDate).HasColumnName(@"MOD_DATE").ValueGeneratedNever();
			modelBuilder.Entity<DiskPool>().Property(x => x.ClassTypeId).HasColumnName(@"CLASS_TYPE_ID").IsRequired().ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<DiskPool>().Property(x => x.ReplicationType).HasColumnName(@"REPLICATION_TYPE").IsRequired().ValueGeneratedNever().HasDefaultValueSql(@"1");
			modelBuilder.Entity<DiskPool>().HasKey(@"Id");
			modelBuilder.Entity<DiskPool>().HasIndex(@"Name").IsUnique(true);
		}

		partial void CustomizeDiskPoolMapping(ModelBuilder modelBuilder);

		#endregion

		#region ServerUsage Mapping

		private void ServerUsageMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<ServerUsage>().ToTable(@"SERVER_USAGES");
			modelBuilder.Entity<ServerUsage>().Property(x => x.Id).HasColumnName(@"ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<ServerUsage>().Property(x => x.RegDate).HasColumnName(@"REG_DATE").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<ServerUsage>().Property(x => x.LoadAverage1M).HasColumnName(@"LOAD_AVERAGE1M").ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<ServerUsage>().Property(x => x.LoadAverage5M).HasColumnName(@"LOAD_AVERAGE5M").ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<ServerUsage>().Property(x => x.LoadAverage15M).HasColumnName(@"LOAD_AVERAGE15M").ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<ServerUsage>().Property(x => x.MemoryTotal).HasColumnName(@"MEMORY_TOTAL").ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<ServerUsage>().Property(x => x.MemoryUsed).HasColumnName(@"MEMORY_USED").ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<ServerUsage>().HasKey(@"Id", @"RegDate");
		}

		partial void CustomizeServerUsageMapping(ModelBuilder modelBuilder);

		#endregion

		#region NetworkInterfaceUsage Mapping

		private void NetworkInterfaceUsageMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<NetworkInterfaceUsage>().ToTable(@"NETWORK_INTERFACE_USAGES");
			modelBuilder.Entity<NetworkInterfaceUsage>().Property(x => x.Id).HasColumnName(@"ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<NetworkInterfaceUsage>().Property(x => x.RegDate).HasColumnName(@"REG_DATE").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<NetworkInterfaceUsage>().Property(x => x.BandWidth).HasColumnName(@"BAND_WIDTH").ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<NetworkInterfaceUsage>().Property(x => x.Rx).HasColumnName(@"RX").ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<NetworkInterfaceUsage>().Property(x => x.Tx).HasColumnName(@"TX").ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<NetworkInterfaceUsage>().HasKey(@"Id", @"RegDate");
		}

		partial void CustomizeNetworkInterfaceUsageMapping(ModelBuilder modelBuilder);

		#endregion

		#region ServiceUsage Mapping

		private void ServiceUsageMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<ServiceUsage>().ToTable(@"SERVICE_USAGES");
			modelBuilder.Entity<ServiceUsage>().Property(x => x.Id).HasColumnName(@"ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<ServiceUsage>().Property(x => x.RegDate).HasColumnName(@"REG_DATE").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<ServiceUsage>().Property(x => x.CpuUsage).HasColumnName(@"CPU_USAGE").ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<ServiceUsage>().Property(x => x.MemoryTotal).HasColumnName(@"MEMORY_TOTAL").ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<ServiceUsage>().Property(x => x.MemoryUsed).HasColumnName(@"MEMORY_USED").ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<ServiceUsage>().Property(x => x.ThreadCount).HasColumnName(@"THREAD_COUNT").IsRequired().ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<ServiceUsage>().HasKey(@"Id", @"RegDate");
		}

		partial void CustomizeServiceUsageMapping(ModelBuilder modelBuilder);

		#endregion

		#region ServiceConfig Mapping

		private void ServiceConfigMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<ServiceConfig>().ToTable(@"SERVICE_CONFIGS");
			modelBuilder.Entity<ServiceConfig>().Property(x => x.Type).HasColumnName(@"TYPE").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<ServiceConfig>().Property(x => x.Version).HasColumnName(@"VERSION").IsRequired().ValueGeneratedOnAdd();
			modelBuilder.Entity<ServiceConfig>().Property(x => x.Config).HasColumnName(@"CONFIG").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<ServiceConfig>().Property(x => x.RegDate).HasColumnName(@"REG_DATE").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<ServiceConfig>().Property(x => x.LastVersion).HasColumnName(@"LAST_VERSION").IsRequired().ValueGeneratedNever().HasPrecision(1, 0).HasDefaultValueSql(@"1");
			modelBuilder.Entity<ServiceConfig>().HasKey(@"Type", @"Version");
		}

		partial void CustomizeServiceConfigMapping(ModelBuilder modelBuilder);

		#endregion

		#region KsanUser Mapping

		private void KsanUserMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<KsanUser>().ToTable(@"KSAN_USERS");
			modelBuilder.Entity<KsanUser>().Property(x => x.AccessKey).HasColumnName(@"ACCESS_KEY").IsRequired().ValueGeneratedNever().HasMaxLength(20);
			modelBuilder.Entity<KsanUser>().Property(x => x.SecretKey).HasColumnName(@"SECRET_KEY").IsRequired().ValueGeneratedNever().HasMaxLength(40);
			modelBuilder.Entity<KsanUser>().Property(x => x.Id).HasColumnName(@"ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<KsanUser>().Property(x => x.Name).HasColumnName(@"NAME").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<KsanUser>().Property(x => x.Email).HasColumnName(@"EMAIL").ValueGeneratedNever();
			modelBuilder.Entity<KsanUser>().HasKey(@"AccessKey", @"SecretKey");
			modelBuilder.Entity<KsanUser>().HasIndex(@"Id").IsUnique(true);
		}

		partial void CustomizeKsanUserMapping(ModelBuilder modelBuilder);

		#endregion

		#region UserDiskPool Mapping

		private void UserDiskPoolMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<UserDiskPool>().ToTable(@"USER_DISK_POOLS");
			modelBuilder.Entity<UserDiskPool>().Property(x => x.UserId).HasColumnName(@"USER_ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<UserDiskPool>().Property(x => x.DiskPoolId).HasColumnName(@"DISK_POOL_ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<UserDiskPool>().Property(x => x.StorageClass).HasColumnName(@"STORAGE_CLASS").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<UserDiskPool>().HasKey(@"UserId", @"DiskPoolId", @"StorageClass");
		}

		partial void CustomizeUserDiskPoolMapping(ModelBuilder modelBuilder);

		#endregion

		#region DiskUsage Mapping

		private void DiskUsageMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<DiskUsage>().ToTable(@"DISK_USAGES");
			modelBuilder.Entity<DiskUsage>().Property(x => x.Id).HasColumnName(@"ID").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<DiskUsage>().Property(x => x.RegDate).HasColumnName(@"REG_DATE").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<DiskUsage>().Property(x => x.UsedInode).HasColumnName(@"USED_INODE").IsRequired().ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<DiskUsage>().Property(x => x.UsedSize).HasColumnName(@"USED_SIZE").IsRequired().ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<DiskUsage>().Property(x => x.Read).HasColumnName(@"READ").IsRequired().ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<DiskUsage>().Property(x => x.Write).HasColumnName(@"WRITE").IsRequired().ValueGeneratedNever().HasDefaultValueSql(@"0");
			modelBuilder.Entity<DiskUsage>().HasKey(@"Id", @"RegDate");
		}

		partial void CustomizeDiskUsageMapping(ModelBuilder modelBuilder);

		#endregion

		#region Region Mapping

		private void RegionMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<Region>().ToTable(@"REGIONS");
			modelBuilder.Entity<Region>().Property(x => x.Name).HasColumnName(@"NAME").IsRequired().ValueGeneratedNever().HasMaxLength(50);
			modelBuilder.Entity<Region>().Property(x => x.Address).HasColumnName(@"ADDRESS").IsRequired().ValueGeneratedNever().HasMaxLength(15);
			modelBuilder.Entity<Region>().Property(x => x.Port).HasColumnName(@"PORT").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<Region>().Property(x => x.SSLPort).HasColumnName(@"SSLPORT").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<Region>().Property(x => x.AccessKey).HasColumnName(@"ACCESS_KEY").HasColumnType(@"char(20)").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<Region>().Property(x => x.SecretKey).HasColumnName(@"SECRET_KEY").HasColumnType(@"char(40)").IsRequired().ValueGeneratedNever();
			modelBuilder.Entity<Region>().HasKey(@"Name");
		}

		partial void CustomizeRegionMapping(ModelBuilder modelBuilder);

		#endregion

		private void RelationshipsMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<NetworkInterface>().HasOne(x => x.Server).WithMany(op => op.NetworkInterfaces).HasForeignKey(@"ServerId").IsRequired(true);
			modelBuilder.Entity<NetworkInterface>().HasOne(x => x.ModUser).WithMany(op => op.ModNetworkInterfaces).HasForeignKey(@"ModId").IsRequired(false);
			modelBuilder.Entity<NetworkInterface>().HasMany(x => x.NetworkInterfaceVlans).WithOne(op => op.NetworkInterface).HasForeignKey(@"InterfaceId").IsRequired(true);
			modelBuilder.Entity<NetworkInterface>().HasOne(x => x.RegUser).WithMany(op => op.RegNetworkInterfaces).HasForeignKey(@"RegId").IsRequired(false);
			modelBuilder.Entity<NetworkInterface>().HasMany(x => x.NetworkInterfaceUsages).WithOne(op => op.NetworkInterface).HasForeignKey(@"Id").IsRequired(true);

			modelBuilder.Entity<NetworkInterfaceVlan>().HasOne(x => x.RegUser).WithMany(op => op.RegNetworkInterfaceVlans).HasForeignKey(@"RegId").IsRequired(false);
			modelBuilder.Entity<NetworkInterfaceVlan>().HasOne(x => x.ModUser).WithMany(op => op.ModNetworkInterfaceVlans).HasForeignKey(@"ModId").IsRequired(false);
			modelBuilder.Entity<NetworkInterfaceVlan>().HasOne(x => x.NetworkInterface).WithMany(op => op.NetworkInterfaceVlans).HasForeignKey(@"InterfaceId").IsRequired(true);
			modelBuilder.Entity<NetworkInterfaceVlan>().HasMany(x => x.ServiceNetworkInterfaceVlans).WithOne(op => op.NetworkInterfaceVlan).HasForeignKey(@"VlanId").IsRequired(true);

			modelBuilder.Entity<Service>().HasMany(x => x.Vlans).WithOne(op => op.Service).HasForeignKey(@"ServiceId").IsRequired(true);
			modelBuilder.Entity<Service>().HasOne(x => x.ServiceGroup).WithMany(op => op.Services).HasForeignKey(@"GroupId").IsRequired(false);
			modelBuilder.Entity<Service>().HasOne(x => x.RegUser).WithMany(op => op.RegServices).HasForeignKey(@"RegId").IsRequired(false);
			modelBuilder.Entity<Service>().HasOne(x => x.ModUser).WithMany(op => op.ModServices).HasForeignKey(@"ModId").IsRequired(false);
			modelBuilder.Entity<Service>().HasMany(x => x.ServiceDisks).WithOne(op => op.Service).HasForeignKey(@"ServiceId").IsRequired(true);
			modelBuilder.Entity<Service>().HasMany(x => x.ServiceUsages).WithOne(op => op.Service).HasForeignKey(@"Id").IsRequired(true);

			modelBuilder.Entity<ServiceNetworkInterfaceVlan>().HasOne(x => x.Service).WithMany(op => op.Vlans).HasForeignKey(@"ServiceId").IsRequired(true);
			modelBuilder.Entity<ServiceNetworkInterfaceVlan>().HasOne(x => x.NetworkInterfaceVlan).WithMany(op => op.ServiceNetworkInterfaceVlans).HasForeignKey(@"VlanId").IsRequired(true);

			modelBuilder.Entity<Server>().HasMany(x => x.NetworkInterfaces).WithOne(op => op.Server).HasForeignKey(@"ServerId").IsRequired(true);
			modelBuilder.Entity<Server>().HasOne(x => x.ModUser).WithMany(op => op.ModServers).HasForeignKey(@"ModId").IsRequired(false);
			modelBuilder.Entity<Server>().HasMany(x => x.Disks).WithOne(op => op.Server).HasForeignKey(@"ServerId").IsRequired(true);
			modelBuilder.Entity<Server>().HasMany(x => x.ServerUsages).WithOne(op => op.Server).HasForeignKey(@"Id").IsRequired(true);

			modelBuilder.Entity<ServiceGroup>().HasMany(x => x.Services).WithOne(op => op.ServiceGroup).HasForeignKey(@"GroupId").IsRequired(false);
			modelBuilder.Entity<ServiceGroup>().HasOne(x => x.RegUser).WithMany(op => op.RegServiceGroups).HasForeignKey(@"RegId").IsRequired(false);
			modelBuilder.Entity<ServiceGroup>().HasOne(x => x.ModUser).WithMany(op => op.ModServiceGroups).HasForeignKey(@"ModId").IsRequired(false);

			modelBuilder.Entity<AllowedConnectionIp>().HasOne(x => x.Role).WithMany(op => op.AllowedConnectionIps).OnDelete(DeleteBehavior.Restrict).HasForeignKey(@"RoleId").IsRequired(false);
			modelBuilder.Entity<AllowedConnectionIp>().HasOne(x => x.RegUser).WithMany(op => op.RegAllowedConnectionIps).OnDelete(DeleteBehavior.Cascade).HasForeignKey(@"RegId").IsRequired(false);

			modelBuilder.Entity<UserLoginHistory>().HasOne(x => x.User).WithMany(op => op.UserLoginHistories).HasForeignKey(@"Id").IsRequired(true);

			modelBuilder.Entity<UserClaim>().HasOne(x => x.User).WithMany(op => op.UserClaims).OnDelete(DeleteBehavior.Cascade).HasForeignKey(@"UserId").IsRequired(true);

			modelBuilder.Entity<Role>().HasMany(x => x.RoleClaims).WithOne(op => op.Role).OnDelete(DeleteBehavior.Cascade).HasForeignKey(@"RoleId").IsRequired(true);
			modelBuilder.Entity<Role>().HasMany(x => x.AllowedConnectionIps).WithOne(op => op.Role).OnDelete(DeleteBehavior.Restrict).HasForeignKey(@"RoleId").IsRequired(false);
			modelBuilder.Entity<Role>().HasMany(x => x.UserRoles).WithOne(op => op.Role).OnDelete(DeleteBehavior.Cascade).HasForeignKey(@"RoleId").IsRequired(true);

			modelBuilder.Entity<RoleClaim>().HasOne(x => x.Role).WithMany(op => op.RoleClaims).OnDelete(DeleteBehavior.Cascade).HasForeignKey(@"RoleId").IsRequired(true);

			modelBuilder.Entity<UserRole>().HasOne(x => x.Role).WithMany(op => op.UserRoles).OnDelete(DeleteBehavior.Cascade).HasForeignKey(@"RoleId").IsRequired(true);
			modelBuilder.Entity<UserRole>().HasOne(x => x.User).WithMany(op => op.UserRoles).OnDelete(DeleteBehavior.Cascade).HasForeignKey(@"UserId").IsRequired(true);

			modelBuilder.Entity<User>().HasMany(x => x.RegAllowedConnectionIps).WithOne(op => op.RegUser).OnDelete(DeleteBehavior.Cascade).HasForeignKey(@"RegId").IsRequired(false);
			modelBuilder.Entity<User>().HasMany(x => x.UserRoles).WithOne(op => op.User).OnDelete(DeleteBehavior.Cascade).HasForeignKey(@"UserId").IsRequired(true);
			modelBuilder.Entity<User>().HasMany(x => x.UserActionLogs).WithOne(op => op.User).OnDelete(DeleteBehavior.Cascade).HasForeignKey(@"UserId").IsRequired(true);
			modelBuilder.Entity<User>().HasMany(x => x.UserClaims).WithOne(op => op.User).OnDelete(DeleteBehavior.Cascade).HasForeignKey(@"UserId").IsRequired(true);
			modelBuilder.Entity<User>().HasMany(x => x.UserLoginHistories).WithOne(op => op.User).HasForeignKey(@"Id").IsRequired(true);
			modelBuilder.Entity<User>().HasMany(x => x.RegServiceGroups).WithOne(op => op.RegUser).HasForeignKey(@"RegId").IsRequired(false);
			modelBuilder.Entity<User>().HasMany(x => x.ModServiceGroups).WithOne(op => op.ModUser).HasForeignKey(@"ModId").IsRequired(false);
			modelBuilder.Entity<User>().HasMany(x => x.RegServices).WithOne(op => op.RegUser).HasForeignKey(@"RegId").IsRequired(false);
			modelBuilder.Entity<User>().HasMany(x => x.ModServices).WithOne(op => op.ModUser).HasForeignKey(@"ModId").IsRequired(false);
			modelBuilder.Entity<User>().HasMany(x => x.RegNetworkInterfaceVlans).WithOne(op => op.RegUser).HasForeignKey(@"RegId").IsRequired(false);
			modelBuilder.Entity<User>().HasMany(x => x.ModNetworkInterfaceVlans).WithOne(op => op.ModUser).HasForeignKey(@"ModId").IsRequired(false);
			modelBuilder.Entity<User>().HasMany(x => x.ModNetworkInterfaces).WithOne(op => op.ModUser).HasForeignKey(@"ModId").IsRequired(false);
			modelBuilder.Entity<User>().HasMany(x => x.ModServers).WithOne(op => op.ModUser).HasForeignKey(@"ModId").IsRequired(false);
			modelBuilder.Entity<User>().HasMany(x => x.RegNetworkInterfaces).WithOne(op => op.RegUser).HasForeignKey(@"RegId").IsRequired(false);

			modelBuilder.Entity<UserActionLog>().HasOne(x => x.User).WithMany(op => op.UserActionLogs).OnDelete(DeleteBehavior.Cascade).HasForeignKey(@"UserId").IsRequired(true);

			modelBuilder.Entity<Disk>().HasOne(x => x.Server).WithMany(op => op.Disks).HasForeignKey(@"ServerId").IsRequired(true);
			modelBuilder.Entity<Disk>().HasMany(x => x.ServiceDisks).WithOne(op => op.Disk).HasForeignKey(@"DiskId").IsRequired(true);
			modelBuilder.Entity<Disk>().HasOne(x => x.DiskPool).WithMany(op => op.Disks).HasForeignKey(@"DiskPoolId").IsRequired(false);
			modelBuilder.Entity<Disk>().HasMany(x => x.DiskUsages).WithOne(op => op.Disk).HasForeignKey(@"Id").IsRequired(true);

			modelBuilder.Entity<ServiceDisk>().HasOne(x => x.Service).WithMany(op => op.ServiceDisks).HasForeignKey(@"ServiceId").IsRequired(true);
			modelBuilder.Entity<ServiceDisk>().HasOne(x => x.Disk).WithMany(op => op.ServiceDisks).HasForeignKey(@"DiskId").IsRequired(true);

			modelBuilder.Entity<DiskPool>().HasMany(x => x.Disks).WithOne(op => op.DiskPool).HasForeignKey(@"DiskPoolId").IsRequired(false);
			modelBuilder.Entity<DiskPool>().HasMany(x => x.UserDiskPools).WithOne(op => op.DiskPool).HasForeignKey(@"DiskPoolId").IsRequired(true);

			modelBuilder.Entity<ServerUsage>().HasOne(x => x.Server).WithMany(op => op.ServerUsages).HasForeignKey(@"Id").IsRequired(true);

			modelBuilder.Entity<NetworkInterfaceUsage>().HasOne(x => x.NetworkInterface).WithMany(op => op.NetworkInterfaceUsages).HasForeignKey(@"Id").IsRequired(true);

			modelBuilder.Entity<ServiceUsage>().HasOne(x => x.Service).WithMany(op => op.ServiceUsages).HasForeignKey(@"Id").IsRequired(true);

			modelBuilder.Entity<KsanUser>().HasMany(x => x.UserDiskPools).WithOne(op => op.S3User).HasPrincipalKey(@"Id").HasForeignKey(@"UserId").IsRequired(true);

			modelBuilder.Entity<UserDiskPool>().HasOne(x => x.S3User).WithMany(op => op.UserDiskPools).HasPrincipalKey(@"Id").HasForeignKey(@"UserId").IsRequired(true);
			modelBuilder.Entity<UserDiskPool>().HasOne(x => x.DiskPool).WithMany(op => op.UserDiskPools).HasForeignKey(@"DiskPoolId").IsRequired(true);

			modelBuilder.Entity<DiskUsage>().HasOne(x => x.Disk).WithMany(op => op.DiskUsages).HasForeignKey(@"Id").IsRequired(true);
		}

		partial void CustomizeMapping(ref ModelBuilder modelBuilder);

		public bool HasChanges()
		{
			return ChangeTracker.Entries().Any(e => e.State == Microsoft.EntityFrameworkCore.EntityState.Added || e.State == Microsoft.EntityFrameworkCore.EntityState.Modified || e.State == Microsoft.EntityFrameworkCore.EntityState.Deleted);
		}

		partial void OnCreated();
	}
}
