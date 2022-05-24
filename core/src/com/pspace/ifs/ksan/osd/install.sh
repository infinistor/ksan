#!/bin/sh

mvn package
if [ $? != 0 ]; then
    echo "Fail to compile ksanOsd"
    exit
fi

mvn install
if [ $? != 0 ]; then
    echo "Fail to install ksanOsd"
    exit
fi

python setup.py install
if [ $? != 0 ]; then
    echo "Fail to install ksanOsd Util"
    exit
fi
