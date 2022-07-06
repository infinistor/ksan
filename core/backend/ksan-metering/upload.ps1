#!/bin/bash
docker rmi pspace/ksan-metering:latest
docker build --rm -t pspace/ksan-metering:latest -f DockerFile .
del C:\Potal\KSAN\ksan-metering.tar
docker save -o C:\Potal\KSAN\ksan-metering.tar pspace/ksan-metering
docker rmi $(docker images -f "dangling=true" -q)

scp C:\Potal\KSAN\ksan-metering.tar root@192.168.31.231:/root/docker/
ssh root@192.168.31.231 "docker stop ksan-metering"
ssh root@192.168.31.231 "docker rm ksan-metering"
ssh root@192.168.31.231 "docker rmi pspace/ksan-metering"
ssh root@192.168.31.231 "docker load -i /root/docker/ksan-metering.tar"
ssh root@192.168.31.231 "docker create -i -t --net ksannet --ip 172.10.0.32 -v /home/ksan/logs:/app/logs -v /etc/localtime:/etc/localtime:ro -v /home/ksan/share:/home/share -v /home/ksan/custom:/app/wwwroot/custom -v /usr/local/ksan/etc:/app/config --workdir=/app --name ksan-metering pspace/ksan-metering:latest"
# ssh root@192.168.31.231 "docker start ksan-metering"

scp C:\Potal\KSAN\ksan-metering.tar root@192.168.13.15:/root/docker/
ssh root@192.168.13.15 "docker stop ksan-metering"
ssh root@192.168.13.15 "docker rm ksan-metering"
ssh root@192.168.13.15 "docker rmi pspace/ksan-metering"
ssh root@192.168.13.15 "docker load -i /root/docker/ksan-metering.tar"
ssh root@192.168.13.15 "docker create -i -t --net ksannet --ip 172.10.0.32 -v /home/ksan/logs:/app/logs -v /etc/localtime:/etc/localtime:ro -v /home/ksan/share:/home/share -v /home/ksan/custom:/app/wwwroot/custom -v /usr/local/ksan/etc:/app/config --workdir=/app --name ksan-metering pspace/ksan-metering:latest"
# ssh root@192.168.13.15 "docker start ksan-metering"

scp C:\Potal\KSAN\ksan-metering.tar root@192.168.13.189:/opt/
ssh root@192.168.13.189 "systemctl stop ksan-metering"
ssh root@192.168.13.189 "docker rm ksan-metering"
ssh root@192.168.13.189 "docker rmi pspace/ksan-metering"
ssh root@192.168.13.189 "docker load -i /opt/ksan-metering.tar"
ssh root@192.168.13.189 "docker create -i -t --net ksannet --ip 172.10.0.32 -v /etc/localtime:/etc/localtime:ro -v /DATA/ksan/logs:/app/logs -v /DATA/ksan/share:/home/share -v /DATA/ksan/custom:/app/wwwroot/custom -v /usr/local/ksan/etc:/app/config --workdir=/app --name ksan-metering pspace/ksan-metering:latest"
# ssh root@192.168.13.189 "docker start ksan-metering"
