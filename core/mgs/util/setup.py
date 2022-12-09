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

os.system("rm -f /usr/local/ksan/bin/ksan")
os.system("rm -f /usr/local/ksan/bin/util/ksanCbalance.jar")
os.system("rm -f /usr/local/ksan/bin/util/ksanFsck.jar")
os.system("rm -f /usr/local/ksan/bin/util/ksanGetAttr.jar")

setup(
    name='ksan-mgs-util',
    version=Version,
    data_files=[('/usr/local/ksan/bin', [\
                 './ksan']), ('/usr/local/ksan/bin/util', ['ksanCbalance.jar', 'ksanFsck.jar', 'ksanGetAttr.jar'])],
)
os.system("chmod 755 /usr/local/ksan/bin/ksan*")

