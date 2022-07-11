mvn clean package
docker rmi pspace/ksanLifecycle:latest
docker build --rm -t pspace/ksanLifecycle:latest -f DockerFile .
del C:\Potal\KSAN\ksanLifecycle.tar
docker save -o C:\Potal\KSAN\ksanLifecycle.tar pspace/ksanLifecycle
docker rmi $(docker images -f "dangling=true" -q)

scp C:\Potal\KSAN\ksanLifecycle.tar root@192.168.31.231:/root/docker/
ssh root@192.168.31.231 "docker stop ksanLifecycle"
ssh root@192.168.31.231 "docker rm ksanLifecycle"
ssh root@192.168.31.231 "docker rmi pspace/ksanLifecycle"
ssh root@192.168.31.231 "docker load -i /root/docker/ksanLifecycle.tar"
ssh root@192.168.31.231 "docker create -i -t --net ksannet --ip 172.10.0.31 -v /home/ksan/logs:/app/logs -v /etc/localtime:/etc/localtime:ro -v /home/ksan/share:/home/share -v /home/ksan/custom:/app/wwwroot/custom -v /usr/local/ksan/etc:/app/config --workdir=/app --name ksanLifecycle pspace/ksanLifecycle:latest"
ssh root@192.168.31.231 "docker start ksanLifecycle"
