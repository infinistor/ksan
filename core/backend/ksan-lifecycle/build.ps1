#!/bin/bash
docker rmi pspace/ksan-lifecycle:latest
docker build --rm -t pspace/ksan-lifecycle:latest -f DockerFile .
del C:\Potal\KSAN\ksan-lifecycle.tar
docker save -o C:\Potal\KSAN\ksan-lifecycle.tar pspace/ksan-lifecycle
docker rmi $(docker images -f "dangling=true" -q)