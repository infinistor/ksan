#!/bin/bash
docker rmi pspace/ksanMetering:latest
docker build --rm -t pspace/ksanMetering:latest -f DockerFile .
del C:\Potal\KSAN\ksanMetering.tar
docker save -o C:\Potal\KSAN\ksanMetering.tar pspace/ksanMetering
docker rmi $(docker images -f "dangling=true" -q)

scp C:\Potal\KSAN\ksanMetering.tar root@192.168.31.231:/root/docker/
ssh root@192.168.31.231 "docker stop ksanMetering"
ssh root@192.168.31.231 "docker rm ksanMetering"
ssh root@192.168.31.231 "docker rmi pspace/ksanMetering"
ssh root@192.168.31.231 "docker load -i /root/docker/ksanMetering.tar"
ssh root@192.168.31.231 "docker create -i -t --net ksannet --ip 172.10.0.32 -v /home/ksan/logs:/app/logs -v /etc/localtime:/etc/localtime:ro -v /home/ksan/share:/home/share -v /home/ksan/custom:/app/wwwroot/custom -v /usr/local/ksan/etc:/app/config --workdir=/app --name ksanMetering pspace/ksanMetering:latest"
# ssh root@192.168.31.231 "docker start ksanMetering"

scp C:\Potal\KSAN\ksanMetering.tar root@192.168.13.15:/root/docker/
ssh root@192.168.13.15 "docker stop ksanMetering"
ssh root@192.168.13.15 "docker rm ksanMetering"
ssh root@192.168.13.15 "docker rmi pspace/ksanMetering"
ssh root@192.168.13.15 "docker load -i /root/docker/ksanMetering.tar"
ssh root@192.168.13.15 "docker create -i -t --net ksannet --ip 172.10.0.32 -v /home/ksan/logs:/app/logs -v /etc/localtime:/etc/localtime:ro -v /home/ksan/share:/home/share -v /home/ksan/custom:/app/wwwroot/custom -v /usr/local/ksan/etc:/app/config --workdir=/app --name ksanMetering pspace/ksanMetering:latest"
# ssh root@192.168.13.15 "docker start ksanMetering"

scp C:\Potal\KSAN\ksanMetering.tar root@192.168.13.189:/opt/
ssh root@192.168.13.189 "systemctl stop ksanMetering"
ssh root@192.168.13.189 "docker rm ksanMetering"
ssh root@192.168.13.189 "docker rmi pspace/ksanMetering"
ssh root@192.168.13.189 "docker load -i /opt/ksanMetering.tar"
ssh root@192.168.13.189 "docker create -i -t --net ksannet --ip 172.10.0.32 -v /etc/localtime:/etc/localtime:ro -v /DATA/ksan/logs:/app/logs -v /DATA/ksan/share:/home/share -v /DATA/ksan/custom:/app/wwwroot/custom -v /usr/local/ksan/etc:/app/config --workdir=/app --name ksanMetering pspace/ksanMetering:latest"
# ssh root@192.168.13.189 "docker start ksanMetering"
