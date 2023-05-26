#!/usr/bin/python3

import os
from setuptools import setup
import site
import pdb

UserSitePackagePath = site.getusersitepackages()

Version = '0.8.0'

setup(
    name='ksan-common',
    version=Version,
    data_files=[('/usr/local/ksan/bin/util', []),
                ('/usr/local/ksan/sbin/', ['./ksanAgent']),
                ('/var/log/ksan/rabbitmq', []), ('/var/log/ksan/agent', [])
	],\
    packages=['portal_api','server', 'disk','common', 'configure', 'const' \
               ,'mqmanage','network', \
               'server','service','user', 'util', 'rest'],
)

os.system("chmod 755 /usr/local/ksan/bin/ksan*")
os.system("chmod 755 /usr/local/ksan/bin/util/*")
os.system("chmod 755 /usr/local/ksan/sbin/*")
os.system("chmod 777 /var/log/ksan/rabbitmq")

