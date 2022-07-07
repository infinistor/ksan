#!/usr/bin/python3

import sys
import os
from glob import glob
from os.path import basename, splitext
from setuptools import find_packages, setup
import site
import pdb

UserSitePackagePath = site.getusersitepackages()

Version = '0.8.0'


setup(
    name='ksan-mgs-util',
    version=Version,
    data_files=[('/usr/local/ksan/bin', [\
                 './ksanServer',\
                 './ksanDisk',\
                 './ksanDiskpool',\
                 './ksanService',\
                 './ksanServicegroup',\
                 './ksanUser',\
                 './ksanManager',\
                 './ksanSysinfo',\
                 './ksanNetwork'
    ])],
)
os.system("chmod 755 /usr/local/ksan/bin/ksan*")

