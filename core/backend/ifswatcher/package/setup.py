# distutils build script
try:
    from setuptools import setup
    from setuptools.dist import Distribution
except ImportError:
    from distutils.core import setup
    from distutils.dist import Distribution
from distutils.core import Extension
import os
import sys
import glob

PSPACE='/usr/local/pspace'

packages = [] 

setup (name = 'InfiniStor-WATCHER',
    version = '3.2',
    author = 'hmjung',
    license = 'apache license 2.0',
    author_email = 'hmjung@pspace.co.kr',
    long_description = 'InfiniStor WATCHER Server',
    description = 'InfiniStor WATCHER Server',
    data_files=[
        ('/usr/lib/systemd/system/', ['systemd/ifs-watcher.service']),
    ],
    scripts=['watcher/ifs-watcher',
	     'util/ifs_watcher',
	     'watcher/ifs-watcher.xml',
        ],
    packages = packages,)