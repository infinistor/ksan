#!/usr/bin/pyhon3
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
import atexit
import signal
import time
from common.utils import IsDaemonRunning

class Daemon(object):
    def __init__(self, PidFile, DaemonName):
        self.PidFile = PidFile
        self.DaemonName = DaemonName
        self.redirect = '/dev/null'
        self.pid = -1

    def daemonize(self):
        try:
            pid = os.fork()
            if pid > 0:
                # exit first parent
                sys.exit(0)
        except OSError as e:
            sys.stderr.write("fork failed: %d (%s)\n" % (e.errno, e.strerror))
            sys.exit(1)

        # decouple from parent environment
        os.chdir("/")
        os.setsid()
        os.umask(0)

        # do second fork
        try:
            pid = os.fork()
            if pid > 0:
                # exit from second parent
                sys.exit(0)
        except OSError as e:
            self.pid = pid
            sys.stderr.write("fork failed: %d (%s)\n" % (e.errno, e.strerror))
            sys.exit(1)

        # write pidfile
        atexit.register(self.delpid)
        self.pid = str(os.getpid())
        if self.PidFile:
            with open(self.PidFile, 'w+') as f:
                f.write("%s" % self.pid)

        # redirect standard file descriptors
        sys.stdout.flush()
        sys.stderr.flush()
        sio = open(self.redirect, 'a+')
        os.dup2(sio.fileno(), sys.stdin.fileno())
        os.dup2(sio.fileno(), sys.stdout.fileno())
        os.dup2(sio.fileno(), sys.stderr.fileno())

        sio.close()

    def delpid(self):
        if self.PidFile and os.path.exists(self.PidFile):
            os.remove(self.PidFile)

    def start(self):
        # Start the daemon
        self.daemonize()
        self.run()

    def stop(self):
        Ret, Pid = IsDaemonRunning(self.PidFile, self.DaemonName)
        if Ret is False:
            return
        try:
            while 1:
                os.kill(int(Pid), signal.SIGKILL)
                time.sleep(0.1)
        except OSError as err:
            err = str(err)
            if err.find("No such process") > 0:
                if os.path.exists(self.PidFile):
                    os.remove(self.PidFile)
            else:
                sys.exit(1)

    def restart(self):
        self.stop()
        self.start()

    def run(self):
        sys.stderr.write("run:\n")
        pass
