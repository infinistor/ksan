#!/usr/bin/python3

import sys
import os
from glob import glob
from os.path import basename, splitext
from setuptools import find_packages, setup
from distutils.command.install_scripts import install_scripts
import site
import pdb

UserSitePackagePath = site.getusersitepackages()

Version = '0.8.0'

setup(
    name='ksan-osd-util',
    version=Version,
    data_files=[('/usr/local/ksan/bin', [\
                 './target/ksan-osd.jar']),
    ('/usr/local/ksan/etc', ['././ksan-osd-log.xml'])
    ],
)

os.system("chmod 755 /usr/local/ksan/bin/ksan*")

