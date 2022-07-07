mvn clean package
docker rmi pspace/ksan-lifecycle:latest
docker build --rm -t pspace/ksan-lifecycle:latest -f DockerFile .
del C:\Potal\KSAN\ksan-lifecycle.tar
docker save -o C:\Potal\KSAN\ksan-lifecycle.tar pspace/ksan-lifecycle
docker rmi $(docker images -f "dangling=true" -q)

scp C:\Potal\KSAN\ksan-lifecycle.tar root@192.168.31.231:/root/docker/
ssh root@192.168.31.231 "docker stop ksan-lifecycle"
ssh root@192.168.31.231 "docker rm ksan-lifecycle"
ssh root@192.168.31.231 "docker rmi pspace/ksan-lifecycle"
ssh root@192.168.31.231 "docker load -i /root/docker/ksan-lifecycle.tar"
ssh root@192.168.31.231 "docker create -i -t --net ksannet --ip 172.10.0.31 -v /home/ksan/logs:/app/logs -v /etc/localtime:/etc/localtime:ro -v /home/ksan/share:/home/share -v /home/ksan/custom:/app/wwwroot/custom -v /usr/local/ksan/etc:/app/config --workdir=/app --name ksan-lifecycle pspace/ksan-lifecycle:latest"
ssh root@192.168.31.231 "docker start ksan-lifecycle"
