#!/bin/sh

mvn package
if [ $? != 0 ]; then
    echo "Fail to compile ksanGw"
    exit
fi

mvn install
if [ $? != 0 ]; then
    echo "Fail to install ksanGw"
    exit
fi

python setup.py install
if [ $? != 0 ]; then
    echo "Fail to install ksanGw Util"
    exit
fi

