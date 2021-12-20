# ksanMgmt

## 개요

### 용도
- S3 Compatibility User, Disk, Service 관리 툴

### 주요 기능
- S3
  - S3 User Management
  - Disk Management
  - Diskpool Management
- Network
  - Network Interface Management
  - Network Interface Vlan Management
- Service Management
- Server Management
- Master Management
- Swigger를 통한 Api 테스트 기능 제공(접속 주소 : `https://<ip>:<port>/api`)


## 빌드 가이드

### 주의사항
- DB 접속을 위해 [Devart](https://www.devart.com/dotconnect/eula.html)에서 제공하는 Devart.Data.MySql.EFCore 패키지를 이용하고 있습니다.
- 빌드하여 사용하기 위해서는 평가판을 사용하시거나 라이센스를 구입하여 키를 발급 받은 뒤 appsettings.json의 DEVART_LICENSE_KEY 부분에 입력하여야 합니다.

### 환경 구성

#### docker 설치
``` shell
yum update
yum -y install docker
systemctl enable docker
systemctl start docker
```

#### dotnet 구성
``` shell
cd setup/aspnetcore_for_api
docker build -t pspace/aspnetcore_for_api:latest .
```
##### dotnet 구성이 안될 경우
``` shell
# docker 버전 확인
docker -v
Docker version 1.13.1, build 0be3e21/1.13.1
# 버전이 1.13.1일 경우 버전 업데이트
yum update
# 기존 버전 삭제
yum remove -y docker-common
# Docker Update에 필요한 Tool 설치
yum install -y yum-utils device-mapper-persistent-data lvm2
# Docker 공식 Repository 추가
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

  # peer's certificate issuer is not recognized에러 발생시
  yum install ca-certificates
  update-ca-trust force-enable
  # 다시 Docker 공식 Repository 추가
  yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

# 설치가능한 Docker version 확인
yum list docker-ce --showduplicates | sort -r
# 최신 Docker version 설치
yum install -y docker-ce
# Docker를 부팅시 실행되도록 설정
systemctl enable docker
# Docker 시작
systemctl start docker
```

### 빌드
- 모든 스크립트는 git에 다운 받은 폴더가 작업 시작지점입니다.
- ~/Downloads에 모든 이미지가 저장됩니다.

#### gateway 빌드
``` shell
cd setup/gateway
docker build -t pspace/ksangateway:latest .
docker save -o ~/Downloads/ksangateway.tar pspace/ksangateway
```

#### ksanapi 빌드
``` shell
#!/bin/bash
#scripts/docker-build-api.sh
docker rmi pspace/ksanapi:latest
cp ./PortalSvr/.dockerignore ./.dockerignore
docker build --rm -t pspace/ksanapi:latest -f ./PortalSvr/Dockerfile .
docker save -o ~/Downloads/ksanapi.tar pspace/ksanapi
docker rmi $(docker images -f "dangling=true" -q)
```

#### ksanportal 빌드
``` shell
#!/bin/bash
#scripts/docker-build-portal.sh
docker rm ksanportal
docker rmi pspace/ksanportal:latest
docker build --rm -t pspace/ksanportal:latest -f ./Portal/Dockerfile ./Portal
docker save -o ~/Downloads/ksanportal.tar pspace/ksanportal
docker rmi $(docker images -f "dangling=true" -q)

```

## 설치 가이드

###  docker 설치 및 설정  

#### docker 설치
``` shell
yum update
yum -y install docker
systemctl enable docker
systemctl start docker
```

#### docker 방화벽 설정
- Docker에서 80, 443 포트 접속이 가능하도록 추가
``` shell
sudo firewall-cmd --zone=public --permanent --add-port=443/tcp
sudo firewall-cmd --zone=public --permanent --add-port=80/tcp
sudo firewall-cmd --reload
```
#### docker 위치 변경
 !!! docker의 이미지 및 컨테이너 이용 폴더가 기본적으로 루트이기 때문에 나중에 용량 부족 등이 발생할 수 있음
``` shell
sudo systemctl stop docker
sudo mv /var/lib/docker /home/docker
sudo ln -s /home/docker /var/lib/docker
sudo systemctl start docker
```

### Portal 설치
#### docker 기본 이미지 로드하기 (오프라인)
``` shell
docker load -i ksangateway.tar
docker load -i ksanapi.tar
docker load -i ksanportal.tar
```

#### docker 내부에서 사용할 아이피 생성
``` shell
docker network create --subnet=172.10.0.0/24 ksannet
```

#### session 공유용 폴더 생성
``` shell
mkdir /home/ksan
mkdir /home/ksan/ksan
mkdir /home/ksan/session
```

#### web 컨테이너 생성
``` shell
docker create -i -t \
--net ksannet \
--ip 172.10.0.11 \
-v /etc/localtime:/etc/localtime:ro \
-v /home/ksan/logs:/app/logs \
-v /home/ksan/share:/home/share \
-v /home/ksan/custom:/app/wwwroot/custom \
-v /home/ksan/session:/home/session \
--workdir="/app" \
--name ksanportal \
pspace/ksanportal:latest
```

#### api 컨테이너 생성
``` shell
docker create -i -t \
--net ksannet \
--ip 172.10.0.21 \
-v /etc/localtime:/etc/localtime:ro \
-v /home/ksan/logs:/app/logs \
-v /home/ksan/share:/home/share \
-v /home/ksan/custom:/app/wwwroot/custom \
-v /home/ksan/data:/app/wwwroot/data \
-v /home/ksan/session:/home/session \
--workdir="/app" \
--name ksanapi \
pspace/ksanapi:latest
```

#### gateway (nginx) 컨테이너 생성
``` shell
docker create --net=host \
-p 80:80 \
-p 443:443 \
-v /etc/localtime:/etc/localtime:ro \
-v /home/ksan/share:/home/share \
--name ksangateway \
pspace/ksangateway:latest
```

#### 파일 복사 및 권한 수정
``` shell
cp /home/ksan/share/ksanapi.service /setup/systemd/system/ksanapi.service
cp /home/ksan/share/ksanportal.service /setup/systemd/system/ksanportal.service
cp /home/ksan/share/ksangateway.service /setup/systemd/system/ksangateway.service

chmod 777 /etc/systemd/system/ksanapi.service
chmod 777 /etc/systemd/system/ksanportal.service
chmod 777 /etc/systemd/system/ksangateway.service
```

#### Api 설정
docker cp /root/appsettings.json ksanapi:/app

#### Portal 서비스 등록 및 실행
``` shell
# 서비스 등록
systemctl enable ksanapi.service
systemctl enable ksanportal.service
systemctl enable ksangateway.service

# 서비스 실행
systemctl start ksanapi
systemctl start ksanportal
systemctl start ksangateway
```
#### swigger 접속 주소
- `https://<ip>:<port>/api`
- 별다른 설정 변경이 없을 시 : `https://localhost:5443/api`

### Mariadb 설치 (docker 이용)
``` shell
# [ mariadb 이미지 다운로드 ]
docker pull mariadb
# [mariadb container 실행] 
docker container run -d -p 3306:3306 \
-e MYSQL_ROOT_HOST=root@'%' \
-e MYSQL_ROOT_PASSWORD=qwe123 \
-e MYSQL_DATABASE=ksan \
-v /MYSQL:/var/lib/mysql --name mariadb mariadb 
```

### rabbitmq 설치(docker 이용)
``` shell
# rabbitmq 설치 및 실행
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 --restart=unless-stopped rabbitmq:3-management
```
#### rabbitmq 접속방법
- 서버주소 : http://<ip>:15672
- username : guest
- password : guest
