#!/usr/bin/env python3
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

import os, sys
sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
from ksan.common.init import *
from ksan.common.utils import IsDaemonRunning, CheckParams
from ksan.common.shcommand import *
from ksan.service.service_manage import AddService
import xml.etree.ElementTree as ET


class KsanGWConfig:
    def __init__(self, LocalIp):
        self.dbrepository = 'MariaDB'
        self.dbhost = LocalIp
        self.database = 'ksan'
        self.dbport = '3306'
        self.dbuser = 'ksan'
        self.dbpass = ''
        self.dbpoolsize = 20

        self.gw_authorization = 'AWS_V2_OR_V4'
        self.gw_endpoint = 'http://0.0.0.0:8080'
        self.gw_secure_endpoint = 'https://0.0.0.0:8443'
        self.gw_keystore_password = 'pwd'
        self.gw_max_file_size = '3221225472'
        self.gw_max_list_size = '200000'
        self.gw_maxtimeskew = '9000'
        self.gw_replication = '1'
        self.gw_osd_port = '8000'
        self.gw_osd_client_count = 10
        self.gw_objmanager_count = 10
        self.gw_localip = LocalIp
        self.apache_path = '/opt/apache-tomcat-9.0.53'

    def Set(self, DbReposigory, DbHost, DbName, DbPort, DbUser, DbPassword, S3Authorization, S3Endpoint, S3SecureEndpoint,
            S3KeystorPassword, S3MaxFileSize, S3MaxListSize, S3MaxTimeSkew, S3Replication, S3OsdPort,
            OsdClientCount, ObjmanagerCount, S3LocalIp, ApachPath):
        self.dbrepository = DbReposigory
        self.dbhost = DbHost
        self.database = DbName
        self.dbport = DbPort
        self.dbuser = DbUser
        self.dbpass = DbPassword
        self.gw_authorization = S3Authorization
        self.gw_endpoint = S3Endpoint
        self.gw_secure_endpoint = S3SecureEndpoint
        self.gw_keystore_password = S3KeystorPassword
        self.gw_max_file_size = S3MaxFileSize
        self.gw_max_list_size = S3MaxListSize
        self.gw_maxtimeskew = S3MaxTimeSkew
        self.gw_replication = S3Replication
        self.gw_osd_port = S3OsdPort
        self.gw_osd_client_count = OsdClientCount
        self.gw_objmanager_count = ObjmanagerCount
        self.gw_localip = S3LocalIp
        self.apache_path = ApachPath


class KsanObjmanagerConfig:
    def __init__(self, LocalIp):
        self.dbrepository = 'MYSQL'
        self.dbhost = LocalIp
        self.dbport = '3306'
        self.database = 'ksan'
        self.dbuser = 'ksan'
        self.dbpass = ''
        self.mqhost = LocalIp
        self.diskpool_queue = 'disk'
        self.diskpool_exchange = 'disk'
        self.osd_exchange = 'OSDExchange'

    def Set(self, DbReposigory, DbHost, DbName, DbPort, DbUser, DbPassword, MqHost, DiskPoolQueue, DiskPoolExchange, OSDExchange):
        self.dbrepository = DbReposigory
        self.dbhost = DbHost
        self.database = DbName
        self.dbport = DbPort
        self.dbuser = DbUser
        self.dbpass = DbPassword
        self.mqhost = MqHost
        self.diskpool_queue = DiskPoolQueue
        self.diskpool_exchange = DiskPoolExchange
        self.osd_exchange = OSDExchange


class KsanGW:
    def __init__(self, logger):
        self.logger = logger
        self.MonConf = None
        self.GwConf = None
        self.ObjmanagerConf = None
        self.apache_pid_path = '/opt/apache-tomcat-9.0.53/bin/catalina.pid'

    def ReadConf(self):
        Ret, HostName, LocalIp = GetHostInfo()
        if Ret is False:
            print('Fail to get host by name')
            sys.exit(-1)

        GwConf = KsanGWConfig(LocalIp)
        Ret, Conf = KsanInitConf(GwServiceConfPath, GwConf, FileType='Normal', ReturnType='Dict')
        if isinstance(Conf,dict):
            Omitted = CheckParams(Conf, ('dbrepository','dbhost', 'database', 'dbport', 'dbuser', 'dbpass',
                                         'gw.authorization', 'gw.endpoint', 'gw.secure-endpoint',
                                         'gw.keystore-password', 'gw.max-file-size', 'gw.max-list-size',
                                         'gw.maxtimeskew', 'gw.replication', 'gw.osd-port', 'gw.osd-client-count',
                                         'gw.local-ip'))
            if Omitted:
                print('%s is omitted in %s' % (Omitted, GwServiceConfPath))
                sys.exit(-1)

            GwConf.Set(Conf['dbrepository'], Conf['dbhost'], Conf['database'], Conf['dbport'], Conf['dbuser'],
                         Conf['dbpass'], Conf['dbpoolsize'], Conf['gw.authorization'], Conf['gw.endpoint'], Conf['gw.secure-endpoint'],
                         Conf['gw.keystore-password'],  Conf['gw.max-file-size'], Conf['gw.max-list-size'],
                         Conf['gw.maxtimeskew'], Conf['gw.replication'],Conf['gw.osd-port'], Conf['gw.osd-client-count']
                         , Conf['gw.local-ip'], Conf['apache-path'])
            self.GwConf = GwConf
        else:
            self.GwConf = Conf

        self.apache_pid_path = '%s/bin/catalina.pid' % self.GwConf.apache_path

        Ret, Conf = KsanInitConf(MonServicedConfPath, Conf)
        self.MonConf = Conf

        ObjConf = KsanObjmanagerConfig(LocalIp)
        Ret, Conf = KsanInitConf(ObjmanagerServiceConfPath, ObjConf, FileType='Normal', ReturnType='Dict')
        if isinstance(Conf,dict):
            Omitted = CheckParams(Conf, ('db.repository','db.host', 'db.name', 'db.port', 'db.username', 'db.password',
                                         'mq.host', 'mq.diskpool.queuename', 'mq.diskpool.exchangename', 'mq.osd.exchangename'))
            if Omitted:
                print('%s is omitted in %s' % (Omitted, ObjmanagerServiceConfPath))
                sys.exit(-1)

            ObjConf.Set(Conf['db.repository'], Conf['db.host'], Conf['db.name'], Conf['db.port'], Conf['db.username'],
            Conf['db.password'], Conf['mq.host'], Conf['mq.diskpool.queuename'], Conf['mq.diskpool.exchangename'], Conf['mq.osd.exchangename'])
            self.ObjConf = ObjConf
        else:
            self.ObjConf = Conf


    def Start(self):
        self.ReadConf()
        self.PreRequisite()

        if self.RegisterService() is False:
            sys.exit(-1)

        Ret, Pid = IsDaemonRunning(self.apache_pid_path, CmdLine='apache-tomcat')
        if Ret is True:
            print('Already Running')
            return False, 'Apache tomcat is alread running'
        else:

            StartCmd = 'cd %s; ./bin/startup.sh' % self.GwConf.apache_path
            os.system(StartCmd)
            time.sleep(2)

            Ret, Pid = IsDaemonRunning(self.apache_pid_path, CmdLine='apache-tomcat')
            if Ret is True:
                return True, ''
            else:
                return False, 'Fail to start Apache tomcat'

    def Stop(self):
        self.ReadConf()
        Ret, Pid = IsDaemonRunning(self.apache_pid_path, CmdLine='apache-tomcat')
        if Ret is False:
            return False, 'Apache tomcat is not running'
        else:
            try:
                StopCmd = 'cd %s; ./bin/shutdown.sh' % self.GwConf.apache_path
                os.system(StopCmd)
                Ret, Pid = IsDaemonRunning(self.apache_pid_path, CmdLine='apache-tomcat')
                if Ret is False:
                    return True, 'Done'
                else:
                    return False, 'Fail to stop Apache tomcat'
            except OSError as err:
                return False, 'Fail to stop. %s' % err

    def Restart(self):
        Ret, ErrMsg = self.Stop()
        if Ret is not True:
            print(ErrMsg)
        Ret, ErrMsg = self.Start()
        if Ret is not True:
            print(ErrMsg)
        else:
            print('Done')


    def Status(self):
        self.ReadConf()
        Ret, Pid = IsDaemonRunning(self.apache_pid_path, CmdLine='apache-tomcat')
        if Ret is True:
            print('Apache tomcat ... Ok')
            return True, 'Apache tomcat ... Ok'
        else:
            print('Apache tomcat ... Not Ok')
            return False, 'Apache tomcat ... Not Ok'

    def UpdateConf(self, Body):
        Conf = self.ReadConf()
        #Conf.Set(Body['Config'])
        #with open(S3ConfFile, 'w') as f:
        #    f.write(Conf.Config)
        #print("Update S3 Conf")
        return True, ''


    def RegisterService(self):
        ServiceName = self.GetHostName() + '_GW'
        ServiceType = 'GW'
        Res, Errmsg, Ret = AddService(self.MonConf.mgs.MgsIp, self.MonConf.mgs.IfsPortalPort, ServiceName, ServiceType, self.MonConf.mgs.ServerId)
        if Res == ResOk:
            print(Ret.Result, Ret.Message)
            return True
        else:
            print(Errmsg)
            return False

    def Init(self):
        #Conf = KsanOsdConfig()
        #Ret, Conf = KsanInitConf(OsdServiceConfPath, Conf, FileType='Normal')
        #if Ret is True:
        self.ReadConf()

        self.GwConf.dbrepository = get_input('Insert Logging DB Type', str, default=self.GwConf.dbrepository)
        self.GwConf.dbhost = get_input('Insert Logging DB Host', 'ip', default=self.GwConf.dbhost)
        self.GwConf.database = get_input('Insert Logging DB Name', int, default=self.GwConf.database)
        self.GwConf.dbport = get_input('Insert Logging DB Port', int, default=self.GwConf.dbport)
        self.GwConf.dbuser = get_input('Insert Logging DB User', int, default=self.GwConf.dbuser)
        self.GwConf.dbpass = get_input('Insert Logging DB Password', 'pwd', default=self.GwConf.dbpass)
        self.GwConf.gw_endpoint = get_input('Insert S3 End Point Url', 'url', default=self.GwConf.gw_endpoint)
        self.GwConf.gw_secure_endpoint = get_input('Insert S3 Secure End Point Url', 'url', default=self.GwConf.gw_secure_endpoint)
        self.GwConf.gw_replication = get_input('Insert S3 Replication', int, default=self.GwConf.gw_replication)
        self.GwConf.gw_osd_port = get_input('Insert Osd Port', int, default=self.GwConf.gw_osd_port)
        self.GwConf.gw_localip = get_input('Insert Local Ip', 'ip', default=self.GwConf.gw_localip)
        self.GwConf.apache_path = get_input('Insert Apache tomcat path', str, default=self.GwConf.apache_path)
        if not os.path.exists(self.GwConf.apache_path):
            print("%s is not found. Install tomcat first" % self.GwConf.apache_path)
            sys.exit(-1)


        self.ObjConf.dbrepository = get_input('Insert Object DB Type', str, default=self.ObjConf.dbrepository)
        self.ObjConf.dbhost = get_input('Insert Object DB Host', 'ip', default=self.ObjConf.dbhost)
        self.ObjConf.database = get_input('Insert Object DB Name', int, default=self.ObjConf.database)
        self.ObjConf.dbport = get_input('Insert Object DB Port', int, default=self.ObjConf.dbport)
        self.ObjConf.dbuser = get_input('Insert Object DB User', int, default=self.ObjConf.dbuser)
        self.ObjConf.dbpass = get_input('Insert Object DB Password', 'pwd', default=self.ObjConf.dbpass)
        self.ObjConf.mqhost = get_input('Insert Mq Server Host', 'ip', default=self.ObjConf.mqhost)
        self.ObjConf.diskpool_queue = get_input('Insert Mq DiskPool Queue Name', int, default=self.ObjConf.diskpool_queue)
        self.ObjConf.diskpool_exchange = get_input('Insert Mq DiskPool Exchange Name', int, default=self.ObjConf.diskpool_exchange)
        self.ObjConf.osd_exchange = get_input('Insert Osd Exchange Name', int, default=self.ObjConf.osd_exchange)

        self.SetTomcatServerConf()
        self.UpdateTomcatScript()

        StrGwConf = """
dbrepository=%s
dbhost=%s
database=%s
dbport=%s
dbuser=%s
dbpass=%s
dbpoolsize=%s
gw.authorization=%s
gw.endpoint=%s
gw.secure-endpoint=%s
gw.keystore-password=%s
gw.max-file-size=%s
gw.max-list-size=%s
gw.maxtimeskew=%s
gw.replication=%s
gw.osd-port=%s
gw.osd-client-count=%s
gw.local-ip=%s
apache-path=%s

""" % (self.GwConf.dbrepository, self.GwConf.dbhost, self.GwConf.database, self.GwConf.dbport, self.GwConf.dbuser,
       self.GwConf.dbpass, self.GwConf.dbpoolsize, self.GwConf.gw_authorization, self.GwConf.gw_endpoint, self.GwConf.gw_secure_endpoint,
       self.GwConf.gw_keystore_password,self.GwConf.gw_max_file_size,self.GwConf.gw_max_list_size,self.GwConf.gw_maxtimeskew,
       self.GwConf.gw_replication,self.GwConf.gw_osd_port,self.GwConf.gw_osd_client_count,self.GwConf.gw_localip, self.GwConf.apache_path)
        try:
            with open(GwServiceConfPath, 'w') as configfile:
                configfile.write(StrGwConf)
            print('Done')
        except (IOError, OSError) as err:
            print('fail to get config file: %s' % GwServiceConfPath)
            self.logger.error('fail to get config file: %s' % GwServiceConfPath)


        StrObjConf = """
db.repository=%s
db.host=%s
db.name=%s
db.port=%s
db.username=%s
db.password=%s
mq.host=%s 
mq.diskpool.queuename=%s 
mq.diskpool.exchangename=%s 
mq.osd.exchangename=%s 
""" % (self.ObjConf.dbrepository, self.ObjConf.dbhost, self.ObjConf.database, self.ObjConf.dbport, self.ObjConf.dbuser,
           self.ObjConf.dbpass, self.ObjConf.mqhost, self.ObjConf.diskpool_queue, self.ObjConf.diskpool_exchange, self.ObjConf.osd_exchange)

        try:
            with open(ObjmanagerServiceConfPath, 'w') as configfile:
                configfile.write(StrObjConf)
        except (IOError, OSError) as err:
            print('fail to get config file: %s' % ObjmanagerServiceConfPath)
            self.logger.error('fail to get config file: %s' % ObjmanagerServiceConfPath)

    def PreRequisite(self):
        IsConfGood = True
        if not os.path.exists(GwServiceConfPath):
            print('ksanGW.conf is not found. Init first')
            IsConfGood = False
        else:
            if self.MonConf is not None:
                if self.MonConf.mgs.ServerId == '':
                    print('Local server is not registered. Excute ksanEdge register first')
                    IsConfGood = False

                elif self.MonConf.mgs.DefaultNetworkId == '':
                    print('Local default network is not registered. Check if DefaultNetworkId is configured')
                    IsConfGood = False

        if not os.path.exists(self.GwConf.apache_path):
            print('Apache path is not found. Install apache first.')
            IsConfGood = False

        out, err = shcall('hostname -I')
        LocalIps = out.rsplit()
        if self.GwConf.gw_localip not in LocalIps:
            print('%s is invalid local ip' % self.GwConf.gw_localip)
            IsConfGood = False

        if IsConfGood != True:
            sys.exit(-1)

    def GetHostName(self):
        out, err = shcall('hostname')
        return out[:-1]

    def SetTomcatServerConf(self):
        ServerXmlPath = '%s/conf/server.xml' % self.GwConf.apache_path
        if not os.path.exists(ServerXmlPath):
            print('%s is not found. Check if Apache tomcat is installed' % ServerXmlPath)
            return False
        tree = ET.parse(ServerXmlPath)
        root = tree.getroot()
        Updated = False
        for EngineTag in root.iter('Engine'):
            EngineAttr = EngineTag.attrib
            EnginName = EngineAttr['name']
            if EnginName != 'Catalina':
                continue
            HostTag = EngineTag.find('Host')
            ContextTag = HostTag.find('Context')
            if ContextTag is None:
                ContextTag = ET.Element('Context')
                ContextTag.attrib['docBase'] = 'S3'
                ContextTag.attrib['path'] = '/'
                ContextTag.attrib['reloadable'] = 'true'

                HostTag.append(ContextTag)
                Updated = True
            else:
                ContextAttr = ContextTag.attrib
                if not (ContextAttr['docBase'] == 'S3' and ContextAttr['path'] == '/' and ContextAttr['reloadable'] == 'true'):
                    HostTag.remove(ContextTag)
                    ContextTag = ET.Element('Context')
                    ContextTag.attrib['docBase'] = 'S3'
                    ContextTag.attrib['path'] = '/'
                    ContextTag.attrib['reloadable'] = 'true'

                    HostTag.append(ContextTag)
                    Updated = True

        if Updated is True:
            tree.write(ServerXmlPath, encoding='utf-8', xml_declaration=True)
        return True


    def UpdateTomcatScript(self):
        """
        apache-tomcat***/bin/catalina.sh JAVA_OPTS change - JAVA_OPTS="$JAVA_OPTS -Xms8G -Xmx8G $JSSE_OPTS"  // minumym 8G,maximum 8G
        startup.sh updte: apache-tomcat***/bin/startup.sh add  export CATALINA_PID="$PRGDIR"/catalina.pid
        shutdown.sh update: apache-tomcat***/bin/shutdown.sh add export CATALINA_PID="$PRGDIR"/catalina.pid, exec "$PRGDIR"/"$EXECUTABLE" stop -force "$@"  (add force option)
        """
        ValidCheck = True
        if not os.path.exists(NormalTomcatStartShellPath):
            print("%s is not found" % NormalTomcatStartShellPath)
            ValidCheck = False
        if not os.path.exists(NormalTomcatShutdownShellPath):
            print("%s is not found" % NormalTomcatShutdownShellPath)
            ValidCheck = False

        if ValidCheck is False:
            return

        ExportCatalinaPidOptionFinder = re.compile("export CATALINA_PID=\"\$PRGDIR\"/catalina.pid")
        ShutdownForceOptionFinder = re.compile("exec \"$PRGDIR\"/\"\$EXECUTABLE\" stop -force \"\$@\"")
        InitGwServletFinder = re.compile("<servlet-name>[\s]+initGW")

        CatalinaShellFile = '%s/bin/catalina.sh' % self.GwConf.apache_path
        JavaMemoryOptionFinder = re.compile("JAVA_OPTS=\"\$JAVA_OPTS -Xms8G -Xmx8G \$JSSE_OPTS\"")
        with open(CatalinaShellFile, 'r') as f:
            Conf = f.read()
            CurrentJavaOpt = JavaMemoryOptionFinder.search(Conf)
            if not CurrentJavaOpt:
                #UpdateCatalinaShllCmd = 'rm -f %s; cp %s %s' % (CatalinaShellFile, NormalCatalinaScriptPath, CatalinaShellFile)
                UpdateCatalinaShllCmd = "sed -i  's/JAVA_OPTS=\"$JAVA_OPTS $JSSE_OPTS\"/JAVA_OPTS=\"$JAVA_OPTS -Xms8G -Xmx8G $JSSE_OPTS\"/g' %s" % CatalinaShellFile
                shcall(UpdateCatalinaShllCmd)

        # add catalina.pid to startup.sh
        StartShllFile = '%s/bin/startup.sh' % self.GwConf.apache_path
        with open(StartShllFile, 'r') as f:
            Conf = f.read()
            Found = ExportCatalinaPidOptionFinder.search(Conf)
            if not Found:
                #UpdateStartShellCmd = 'rm -f %s; cp %s %s' % (StartShllFile, NormalTomcatStartShellPath, StartShllFile)
                UpdateStartShellCmd = "sed -i 's/exec \"$PRGDIR\"\/\"$EXECUTABLE\" start \"$@\"/export CATALINA_PID=\"$PRGDIR\"\/catalina.pid\nexec \"$PRGDIR\"\/\"$EXECUTABLE\" start \"$@\"/g' %s" % StartShllFile
                shcall(UpdateStartShellCmd)
        # add catalina.pid and stop force option to bin/shutdown.sh
        ShutdownShllFile = '%s/bin/shutdown.sh' % self.GwConf.apache_path
        with open(ShutdownShllFile, 'r') as f:
            Conf = f.read()
            ForceOptionFound = ShutdownForceOptionFinder.search(Conf)
            CatalinaPidFound = ExportCatalinaPidOptionFinder.search(Conf)
            if not (ForceOptionFound and CatalinaPidFound):
                #UpdateShutdownShellCmd = 'rm -f %s; cp %s %s' % (ShutdownShllFile, NormalTomcatShutdownShellPath, ShutdownShllFile)
                UpdateShutdownShellCmd = "sed -i 's/exec \"$PRGDIR\"\/\"$EXECUTABLE\" stop \"$@\"/export CATALINA_PID=\"$PRGDIR\"\/catalina.pid\nexec \"$PRGDIR\"\/\"$EXECUTABLE\" stop -force \"$@\"/g' %s " %  ShutdownShllFile
                shcall(UpdateShutdownShellCmd)

        # web.xml update
        TomcatWebXmlFile = '%s/conf/web.xml' % self.GwConf.apache_path
        with open(TomcatWebXmlFile, 'r') as f:
            Conf = f.read()
            Found = InitGwServletFinder.search(Conf)
            if not Found:
                UpdateTomcatWebXmlFileCmd = 'rm -f %s; cp %s %s' % (TomcatWebXmlFile, NormalTomcatWebXmlPath, TomcatWebXmlFile)
                shcall(UpdateTomcatWebXmlFileCmd)

        # S3war update
        TomcatS3WarFile = '%s/webapps/S3.war' % self.GwConf.apache_path
        if os.path.exists(TomcatS3WarFile):
            os.unlink(TomcatS3WarFile)

        UpdateTomcatS3WarFileCmd = 'rm -f %s; cp %s %s' % (TomcatS3WarFile, S3WarFilePath, TomcatS3WarFile)
        shcall(UpdateTomcatS3WarFileCmd)
