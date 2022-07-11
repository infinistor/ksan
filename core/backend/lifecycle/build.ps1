#!/bin/bash
docker rmi pspace/ksanLifecycle:latest
docker build --rm -t pspace/ksanLifecycle:latest -f DockerFile .
del C:\Potal\KSAN\ksanLifecycle.tar
docker save -o C:\Potal\KSAN\ksanLifecycle.tar pspace/ksanLifecycle
docker rmi $(docker images -f "dangling=true" -q)