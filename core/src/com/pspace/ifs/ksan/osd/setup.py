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
composer_mode = False
if 'composer' in sys.argv: # install composer mode install
    index = sys.argv.index('composer')
    composer_mode = sys.argv.pop(index)  # Returns the element after the 'composer'

    install_list = [
    ('/usr/local/ksan/etc', ['./ksan-osd-log.xml'])
    ]
else:
    install_list = [('/usr/local/ksan/sbin/', [\
                 './ksan-osd.jar']),
    ('/usr/local/ksan/etc', ['./ksan-osd-log.xml'])
    ]

setup(
    name='ksan-osd-util',
    version=Version,
    data_files=install_list,
)
if composer_mode is False:
    os.system("chmod 755 /usr/local/ksan/sbin/ksan*")

