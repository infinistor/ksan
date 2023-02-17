
from common.utils import *
from common.define import *
from const.common import *


def WaitAgentConfComplete(FuncName, logger, CheckServerId=True, CheckNetworkDevice=False, CheckNetworkId=False):
    while True:
        ret, conf = GetConf(MonServicedConfPath)
        if ret is True:
            if conf is not None:
                if CheckServerId is True:
                    if hasattr(conf.__dict__[KeyCommonSection], 'ServerId'):
                        if not (conf.__dict__[KeyCommonSection].__dict__[KeyServerId] is None or conf.__dict__[KeyCommonSection].__dict__[KeyServerId] == ''):
                            if CheckNetworkDevice is False:
                                return conf
                            else:
                                if not (conf.__dict__[KeyCommonSection].__dict__[KeyManagementNetDev] is None or conf.__dict__[KeyCommonSection].__dict__[KeyManagementNetDev] == ''):
                                    if CheckNetworkId is False:
                                        return conf
                                    else:
                                        if not (conf.__dict__[KeyCommonSection].__dict__[KeyDefaultNetworkId] is None or conf.__dict__[KeyCommonSection].__dict__[KeyDefaultNetworkId] == ''):
                                            return conf
                else:
                    return conf

        logger.debug('%s wail for %s to be configured. Check = ServerId:True, NetworkDevice:%s CheckNetworkId:%s' %
                     (FuncName, MonServicedConfPath, str(CheckNetworkDevice), str(CheckNetworkId)))
        time.sleep(IntervalLong)


def GetAgentConfig(Conf):
    if isinstance(Conf, dict):
        portalhost = Conf[KeyCommonSection].PortalHost
        portalport = Conf[KeyCommonSection].PortalPort
        portalapikey = Conf[KeyCommonSection].PortalApiKey
        mqhost = Conf[KeyCommonSection].MQHost
        mqport = Conf[KeyCommonSection].MQPort
        mqpassword = Conf[KeyCommonSection].MQPassword
        mquser = Conf[KeyCommonSection].MQUser
        serverid = Conf[KeyCommonSection].ServerId
        managementnetdev = Conf[KeyCommonSection].ManagementNetDev
        defaultnetworkid = Conf[KeyCommonSection].DefaultNetworkId

        serverMonitorInterval = Conf[KeyMonitorSection].ServerMonitorInterval
        networkMonitorInterval = Conf[KeyMonitorSection].NetworkMonitorInterval
        diskMonitorInterval = Conf[KeyMonitorSection].DiskMonitorInterval
        serviceMonitorInterval = Conf[KeyMonitorSection].ServiceMonitorInterval


    else:
        portalhost = Conf.__dict__[KeyCommonSection].__dict__[KeyPortalHost]
        portalport = Conf.__dict__[KeyCommonSection].__dict__[KeyPortalPort]
        portalapikey = Conf.__dict__[KeyCommonSection].__dict__[KeyPortalApiKey]
        mqhost = Conf.__dict__[KeyCommonSection].__dict__[KeyMQHost]
        mqport = Conf.__dict__[KeyCommonSection].__dict__[KeyMQPort]
        mqpassword = Conf.__dict__[KeyCommonSection].__dict__[KeyMQPassword]
        mquser = Conf.__dict__[KeyCommonSection].__dict__[KeyMQUser]
        serverid = Conf.__dict__[KeyCommonSection].__dict__[KeyServerId]
        managementnetdev = Conf.__dict__[KeyCommonSection].__dict__[KeyManagementNetDev]
        defaultnetworkid = Conf.__dict__[KeyCommonSection].__dict__[KeyDefaultNetworkId]

        serverMonitorInterval = Conf.__dict__[KeyMonitorSection].__dict__[KeyServerMonitorInterval]
        networkMonitorInterval = Conf.__dict__[KeyMonitorSection].__dict__[KeyNetworkMonitorInterval]
        diskMonitorInterval = Conf.__dict__[KeyMonitorSection].__dict__[KeyDiskMonitorInterval]
        serviceMonitorInterval = Conf.__dict__[KeyMonitorSection].__dict__[KeyServiceMonitorInterval]

    # Key conversion(ksanAgen.conf -> Conf object attribute
    class Config: pass
    setattr(Config, 'PortalHost', portalhost)
    setattr(Config, 'PortalPort' , portalport )
    setattr(Config, 'PortalApiKey', portalapikey)
    setattr(Config, 'MQHost', mqhost)
    setattr(Config, 'MQPort', mqport)
    setattr(Config, 'MQPassword', mqpassword)
    setattr(Config, 'MQUser', mquser)
    setattr(Config, 'ServerId', serverid)
    setattr(Config, 'ManagementNetDev', managementnetdev)
    setattr(Config, 'DefaultNetworkId', defaultnetworkid)

    setattr(Config, 'ServerMonitorInterval', serverMonitorInterval)
    setattr(Config, 'NetworkMonitorInterval', networkMonitorInterval)
    setattr(Config, 'DiskMonitorInterval', diskMonitorInterval)
    setattr(Config, 'ServiceMonitorInterval', serviceMonitorInterval)

    return Config




def Byte2HumanValue(Byte, Title, Color=True):
    Byte = float(Byte)
    if Title == 'TotalSize':
        if (int(Byte) / OneTBUnit) > 0.99: # TB
            TB = int(Byte) / OneTBUnit
            if Color is True:
                return (clr.okbl + "%8.1f" % round(TB, 1) + 'T' + clr.end)
            else:
                return "%8.1f" % round(TB, 1) + 'T'
        elif (int(Byte) / OneGBUnit) > 0.99: # GB
            GB = int(Byte) / OneGBUnit
            if Color is True:
                return (clr.okbl + "%8.1f" % round(GB, 1) + 'G' + clr.end)
            else:
                return "%8.1f" % round(GB, 1) + 'G'
        elif (int(Byte) / OneMBUnit) > 0.99: # MB
            MB = int(Byte) / OneMBUnit
            if Color is True:
                return (clr.okgr + "%8.1f" % round(MB, 1)+'M' + clr.end)
            else:
                return "%8.1f" % round(MB, 1) + 'M'

        elif (int(Byte) / OneKBUnit) > 0.99:  # MB
            MB = int(Byte) / OneKBUnit
            if Color is True:
                return (clr.warnye + "%8.1f" % round(MB, 1) + 'K' + clr.end)
            else:
                return "%8.1f" % round(MB, 1) + 'K'
        else:# KB
            KB = int(Byte)
            if Color is True:
                return (clr.badre + "%8.1f" % round(KB, 1)+'B' + clr.end)
            else:
                return "%8.1f" % round(KB, 1) + 'B'
    elif Title == 'UsedSize' or Title == 'FreeSize':
        if (int(Byte) / OneTBUnit) > 0.99:  # TB
            TB = int(Byte) / OneTBUnit
            if Color is True:
                return (clr.okbl + "%7.1f" % round(TB, 1) + 'T' + clr.end)
            else:
                return "%7.1f" % round(TB, 1) + 'T'

        elif (int(Byte) / OneGBUnit) > 0.99:  # GB
            GB = int(Byte) / OneGBUnit
            if Color is True:
                return (clr.okbl + "%7.1f" % round(GB, 1) + 'G' + clr.end)
            else:
                return "%7.1f" % round(GB, 1) + 'G'
        elif (int(Byte) / OneMBUnit) > 0.99:  # MB
            MB = int(Byte) / OneMBUnit
            if Color is True:
                return (clr.okgr + "%7.1f" % round(MB, 1) + 'M' + clr.end)
            else:
                return "%7.1f" % round(MB, 1) + 'M'

        elif (int(Byte) / OneKBUnit) > 0.99:  # MB
            MB = int(Byte) / OneKBUnit
            if Color is True:
                return (clr.warnye + "%7.1f" % round(MB, 1) + 'K' + clr.end)
            else:
                return "%7.1f" % round(MB, 1) + 'K'
        else:  # KB
            KB = int(Byte)
            if Color is True:
                return (clr.badre + "%7.1f" % round(KB, 1) + 'B' + clr.end)
            else:
                return "%7.1f" % round(KB, 1) + 'B'
    elif Title == 'DiskRw':
        if (int(Byte) / OneTBUnit) > 0.99:  # TB
            TB = int(Byte) / OneTBUnit
            if Color is True:
                return (clr.okbl + "%7.1f" % round(TB, 1) + 'T' + clr.end)
            else:
                return "%7.1f" % round(TB, 1) + 'T'
        elif (int(Byte) / OneGBUnit) > 0.99:  # GB
            GB = int(Byte) / OneGBUnit
            if Color is True:
                return (clr.okbl + "%7.1f" % round(GB, 1) + 'G' + clr.end)
            else:
                return "%7.1f" % round(GB, 1) + 'G'
        elif (int(Byte) / OneMBUnit) > 0.99:  # MB
            MB = int(Byte) / OneMBUnit
            if Color is True:
                return (clr.okgr + "%7.1f" % round(MB, 1) + 'M' + clr.end)
            else:
                return "%7.1f" % round(MB, 1) + 'M'

        elif (int(Byte) / OneKBUnit) > 0.99:  # MB
            MB = int(Byte) / OneKBUnit
            if Color is True:
                return (clr.warnye + "%7.1f" % round(MB, 1) + 'K' + clr.end)
            else:
                return "%7.1f" % round(MB, 1) + 'K'
        else:  # KB
            KB = int(Byte)
            if Color is True:
                return (clr.badre + "%7.1f" % round(KB, 1) + 'B' + clr.end)
            else:
                return "%7.1f" % round(KB, 1) + 'B'


def DisplayDiskState(State):
    if State == DiskStart:
        return (clr.okgr + "%-7s" % State + clr.end)
    elif State == DiskStop:
        return (clr.badre + "%-7s" % State + clr.end)
    elif State == DiskWeak:
        return (clr.warnye + "%-7s" % State + clr.end)
    elif State == DiskDisable:
        return (clr.badre + "%-7s" % State + clr.end)

def DisplayState(State, WhiteSpace=10):
    if State in [ServiceStateOnline, ServerStateOnline]:
        return (clr.okgr + "%-10s" % State + clr.end)
    elif State in [ServerStateOffline, ServerStateTimeout, ServiceStateOffline, ServiceStateUnkown]:
        return (clr.badre + "%-10s" % State + clr.end)



def DisplayDiskMode(Mode):
    if Mode == DiskModeRw:
        return DiskModeRwShort
    elif Mode == DiskModeRo:
        return DiskModeRoShort
    elif Mode == DiskModeMaintenance:
        return DiskModeMaintenanceShort
    else:
        return 'InvalidMode'



@catch_exceptions()
class GetApiResult:
    def __init__(self, Header, ItemHeader, Items):
        self.Header = Header
        self.ItemHeader = ItemHeader
        self.Items = Items


class ResponseHeader(object):
    """
    Parsing Response without "Data" value is single value like True/False
    """
    def __init__(self):
        self.IsNeedLogin = None
        self.AccessDenied = None
        self.Result = None
        self.Code = None
        self.Message = None

    def Set(self, IsNeedLogin, AccessDenied, Result, Code, Message):
        self.IsNeedLogin = IsNeedLogin
        self.AccessDenied = AccessDenied
        self.Result = Result
        self.Code = Code
        self.Message = Message

class ResponseHeaderWithData(object):
    """
    Parsing Response with "Data" value is multi value like dict or list
    """
    def __init__(self):
        self.IsNeedLogin = None
        self.AccessDenied = None
        self.Result = None
        self.Code = None
        self.Message = None
        self.Data = None

    def Set(self, IsNeedLogin, AccessDenied, Result, Code, Message, Data):
        self.IsNeedLogin = IsNeedLogin
        self.AccessDenied = AccessDenied
        self.Result = Result
        self.Code = Code
        self.Message = Message
        self.Data = Data


class ResponseItemsHeader(object):
    def __init__(self):
        self.TotalCount = ''
        self.Skips = ''
        self.PageNo = ''
        self.CountPerPage = ''
        self.PagePerSection = ''
        self.TotalPage = ''
        self.StartPageNo = ''
        self.EndPageNo = ''
        self.PageNos = ''
        self.HavePreviousPage = ''
        self.HaveNextPage = ''
        self.HavePreviousPageSection = ''
        self.HaveNextPageSection = ''
        self.Items = ''


@catch_exceptions()
class ResPonseHeader(object):
    def __init__(self, dic, logger=None):
        try:
            self.IsNeedLogin = dic['IsNeedLogin']
            self.AccessDenied = dic['AccessDenied']
            self.Result = dic['Result']
            self.Code = dic['Code']
            self.Message = dic['Message']
            if 'Data' in dic:
                self.Data = dic['Data']
        except KeyError as err:
            if logger is not None:
                logger.error(err, sys.exc_info()[2].tb_lineno)
            else:
                print(err, sys.exc_info()[0].tb_lineno)


#####  Response Body Decoder Class #####
class ResPonseHeaderDecoder(json.JSONDecoder):
    def __init__(self, logger=None, *args, **kwargs):
        # json.JSONDecoder.__init__(self, object_hook=self.object_hook, *args, **kwargs)
        self.logger=logger
        json.JSONDecoder.__init__(self, object_hook=self.object_hook, *args, **kwargs)

    def object_hook(self, dct):
        try:
            print(">>>>", dct)
            if 'IsNeedLogin' in dct:
                obj = ResPonseHeader(dct)
                if 'Data' in dct:
                    print(dct['Data'])
                return obj
        except KeyError as err:
            if self.logger is not None:
                self.logger.error(err, sys.exc_info()[2].tb_lineno)
            else:
                print(err, sys.exc_info()[0].tb_lineno)


#####  Items Header of Response Body Class #####
class ResPonseItemsHeader(object):
    def __init__(self, dic, logger=None):
        try:
            self.TotalCount = dic['TotalCount']
            self.Skips = dic['Skips']
            self.PageNo = dic['PageNo']
            self.CountPerPage = dic['CountPerPage']
            self.PagePerSection = dic['PagePerSection']
            self.TotalPage = dic['TotalPage']
            self.StartPageNo = dic['StartPageNo']
            self.EndPageNo = dic['EndPageNo']
            self.PageNos = dic['PageNos']
            self.HavePreviousPage = dic['HavePreviousPage']
            self.HaveNextPage = dic['HaveNextPage']
            self.HavePreviousPageSection = dic['HavePreviousPageSection']
            self.HaveNextPageSection = dic['HaveNextPageSection']
            self.Items = dic['Items']
        except KeyError as err:
            if logger is not None:
                logger.error(err, sys.exc_info()[2].tb_lineno)
            else:
                print(err, sys.exc_info()[0].tb_lineno)


