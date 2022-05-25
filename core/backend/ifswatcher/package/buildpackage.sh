#!/bin/sh
cd ../;
mvn clean package;
\cp target/ifs-watcher package/watcher;
cd package;
make buildrpm;
vid=`grep -oP 'VERSION_ID="\K[^"]+' /etc/os-release`
if [ $vid = "8" ]; 
then
mkdir -p /root/rpmbuild/SOURCES/;
rm -rf /root/rpmbuild
cp -a build/bdist.linux-x86_64/rpm/ /root/rpmbuild/
rpmbuild -ba build/bdist.linux-x86_64/rpm/SPECS/InfiniStor-WATCHER.spec --nodebuginfo
cp /root/rpmbuild/RPMS/x86_64/InfiniStor-WATCHER-3.2-`git rev-list --all --count`.x86_64.rpm dist
fi