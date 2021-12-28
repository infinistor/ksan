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

import logging
import sys, os, traceback
from functools import wraps
import pdb


class Logging(object):
    def __init__(self, instance=__name__, logfile='/var/log/ksan-util.log', loglevel='error'):
        try:
            self.logfile = logfile
            if loglevel == 'debug':
                self.loglevel = logging.DEBUG
            else:
                self.loglevel = logging.ERROR
            self.logger = logging.getLogger(instance)
            self.set_loglevel()
            formatter = logging.Formatter('%(asctime)s %(filename)s(%(funcName)s:%(lineno)d) %(levelname)s - %(message)s')
            self.fh = logging.FileHandler(logfile)
            self.fh.setFormatter(formatter)
            self.logger.addHandler(self.fh)
        except Exception as err:
            print(err, sys.exc_info()[2].tb_lineno)

    def set_loglevel(self):
        self.logger.setLevel(self.loglevel)
        logging.addLevelName(45, 'INFO')
        logging.INFO = 45

    def create(self):
        return self.logger


def catch_exceptions():
    logger = logging.getLogger('common.log')

    def wrapper(func):
        @wraps(func)
        def decorateor(*args, **kwargs):
            try:
                result = func(*args, **kwargs)
                return result
            except Exception as err:
                exc_type, exc_obj, exc_tb = sys.exc_info()
                logger.error("%s %s %s " % (str(err), traceback.extract_tb(exc_tb)[-1][0], traceback.extract_tb(exc_tb)[-1][1]))
                return None
        return decorateor
    return wrapper
