#!/bin/env python3
"""
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
"""

from ksan.common.define import *
from ksan.common.log import catch_exceptions


@catch_exceptions()
def disp_serverinfo(data, Id=None):

    title ="""
    %s%s%s%s%s%s%s%s%s
    """ % ('Name'.center(20), 'Description'.center(30), 'CpuModel'.center(30), 'Clock'.center(20), 'State'.center(20), 'ModeDate'.center(20), 'ModId'.center(20), 'ModName'.center(20), 'Id'.center(30))
    print(title)
    if Id is None:
        for svrinfo in data.Items:
            svr = ServerItems()
            svr.Set(svrinfo)
            _svr ="%s%s%s%s%s%s%s%s%s" % \
                  (svr.Name.center(20), '{:20.20}'.format(svr.Description.center(30)), '{:20.20}'.format(svr.CpuModel.center(30)), str(svr.Clock).center(20), svr.State.center(20), svr.ModDate.center(20), svr.ModId.center(20), svr.ModName.center(20), svr.Id.center(30))
            print(_svr)
    else:
        _svr = "%-20s%20s%20s%5s%10s%15s%10s%10s%20s" % \
               (data.Name, data.Description, data.CpuModel, data.Clock, data.State, data.ModDate, data.ModId, data.ModName, data.Id)
        print(_svr)

@catch_exceptions()
def disp_network_interfaces(data, NicId=None):
    title ="""
    %s%s%s%s%s%s%s
    """ % ('Id'.center(40), 'ServerId'.center(40),  'Nic'.center(20), 'Description'.center(30), 'IpAddress'.center(20), 'MacAddress'.center(20), 'LinkStatus'.center(20)) # 'ModeDate'.center(20), 'ModId'.center(20), 'ModName'.center(20), 'Id'.center(30))
    print(title)
    if NicId is None:
        for nicinfo in data.Items:
            nic = ResponseNetworkInterfaceItems(nicinfo)
            _nic ="%s%s%s%s%s%s%s" % (nic.Id.center(40), '{:40.40}'.format(nic.ServerId.center(40)),
                                          '{:20.20}'.format(nic.Name.center(20)),'{:30.30}'.format(nic.Description.center(30)), '{:20.20}'.format(nic.IpAddress.center(20)), nic.MacAddress.center(20), str(nic.LinkState).center(20)) # svr.ModDate.center(20), svr.ModId.center(20), svr.ModName.center(20), svr.Id.center(30))
            print(_nic)
    else:

        _nic = "%s%s%s%s%s%s%s" % (data.Id.center(40), '{:40.40}'.format(data.ServerId.center(40)),
                                   '{:20.20}'.format(data.Name.center(20)),
                                   '{:30.30}'.format(data.Description.center(30)),
                                   '{:20.20}'.format(data.IpAddress.center(20)), data.MacAddress.center(20),
                                   data.LinkState.center(
                                       20))  # svr.ModDate.center(20), svr.ModId.center(20), svr.ModName.center(20), svr.Id.center(30))
        print(_nic)


@catch_exceptions()
def disp_vlan_network_interfaces(data, NicId=None):
    title ="""
    %s%s%s%s%s%s%s
    """ % ('Id'.center(40), 'InterfaceId'.center(40),  'Tag'.center(20), 'IpAddress'.center(20), 'SubnetMask'.center(20), 'Gateway'.center(20),  'RegDate'.center(20)) # 'ModeDate'.center(20), 'ModId'.center(20), 'ModName'.center(20), 'Id'.center(30))
    print(title)
    if NicId is None:
        for vlaninfo in data.Items:
            vlan = ResPonseVlanNetworkInterfaceItems(vlaninfo)
            _vlan ="%s%s%s%s%s%s%s" % (vlan.Id.center(40), '{:40.40}'.format(vlan.InterfaceId.center(40)),
                                          '{:20.20}'.format(str(vlan.Tag).center(20)),'{:30.30}'.format(vlan.IpAddress.center(20)), '{:20.20}'.format(vlan.SubnetMask.center(20)), vlan.Gateway.center(20), str(vlan.RegDate).center(20)) # svr.ModDate.center(20), svr.ModId.center(20), svr.ModName.center(20), svr.Id.center(30))
            print(_vlan)
    else:

        _vlan= "%s%s%s%s%s%s%s" % (data.Id.center(40), '{:40.40}'.format(data.InterfaceId.center(40)),
                                    '{:20.20}'.format(str(data.Tag).center(20)),
                                    '{:30.30}'.format(data.IpAddress.center(20)),
                                    '{:20.20}'.format(data.SubnetMask.center(20)), data.Gateway.center(20),
                                    str(data.RegDate).center(
                                        20))  # svr.ModDate.center(20), svr.ModId.center(20), svr.ModName.center(20), svr.Id.center(30))
        print(_vlan)


@catch_exceptions()
def disp_disk_info(data, DiskId=None):
    """
    display disk info with server
    :param data: if DiskId is None, DiskInfoPerServer object list. otherwise DiskItems object.
    :param DiskId:
    :return:
    """
    title ="""
    %s%s%s%s%s%s%s%s%s%s%s
    """ % ('ServerId'.center(40), 'Id'.center(40),  'DiskNo'.center(20), 'Path'.center(20), 'HaAction'.center(20), 'State'.center(20),  'DiskType'.center(20), 'Total'.center(15), 'Used'.center(15), 'Free'.center(15), 'RwMode'.center(10)) # 'ModeDate'.center(20), 'ModId'.center(20), 'ModName'.center(20), 'Id'.center(30))
    print(title)
    if DiskId is None:
        """
        for diskinfo in data.Items:
            disk = DiskItems()
            disk.Set(diskinfo)
            _disk ="%s%s%s%s%s%s%s%s%s%s%s" % (disk.ServerId.center(40), '{:40.40}'.format(disk.Id.center(40)),
                                          '{:20.20}'.format(str(disk.DiskNo).center(20)),
                                             '{:30.30}'.format(disk.Path.center(20)), '{:20.20}'.format(disk.HaAction.center(20)),
                                             disk.State.center(20), str(disk.DiskType).center(20), str(disk.TotalSize).center(15),
                                               str(disk.UsedSize).center(15),str(disk.ReservedSize).center(15),
                                               disk.RwMode.center(10) ) # svr.ModDate.center(20), svr.ModId.center(20), svr.ModName.center(20), svr.Id.center(30))
            print(_disk)
        """
        for svr in data:
            for disk in svr.List:

                _dsp ="%s%s%s%s%s%s%s%s%s%s%s" % (svr.Name.center(40), '{:40.40}'.format(disk.Id.center(40)),
                                               '{:20.20}'.format(str(disk.DiskNo).center(20)),
                                               '{:30.30}'.format(disk.Path.center(20)), '{:20.20}'.format(disk.HaAction.center(20)),
                                               disk.State.center(20), str(disk.DiskType).center(20), str(disk.TotalSize).center(15),
                                               str(disk.UsedSize).center(15),str(disk.ReservedSize).center(15),
                                               disk.RwMode.center(10)) # svr.ModDate.center(20), svr.ModId.center(20), svr.ModName.center(20), svr.Id.center(30))
                print(_dsp)
    else:

        _disk = "%s%s%s%s%s%s%s%s%s%s%s" % (data.ServerId.center(40), '{:40.40}'.format(data.Id.center(40)),
                                          '{:20.20}'.format(str(data.DiskNo).center(20)),
                                          '{:30.30}'.format(data.Path.center(20)),
                                          '{:20.20}'.format(data.HaAction.center(20)),
                                          data.State.center(20), str(data.DiskType).center(20),
                                          str(data.TotalSize).center(15), str(data.UsedSize).center(15),
                                          str(data.ReservedSize).center(15), data.RwMode.center(10))  # svr.ModDate.center(20), svr.ModId.center(20), svr.ModName.center(20), svr.Id.center(30))
        print(_disk)

