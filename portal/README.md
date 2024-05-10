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
- Portal User Management
- Swagger를 통한 Api 테스트 기능 제공(접속 주소 : `https://<ip>:<port>/api`)

## 빌드 가이드

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
docker build -t infinistor/aspnetcore_for_api:latest .
```
##### dotnet 구성이 안될 경우
``` shell
# 업데이트
yum update
# 필요한 Tool 설치
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

### ksan-portal-bridge 빌드
``` shell
cd setup/bridge
docker build -t infinistor/ksan-portal-bridge:latest .
docker save -o ~/Downloads/ksan-portal-bridge.tar infinistor/ksan-portal-bridge
```

### ksan-api-portal 빌드
``` shell
#!/bin/bash
cp ./PortalSvr/.dockerignore ./.dockerignore
docker build --rm -t infinistor/ksan-api-portal:latest -f ./PortalSvr/Dockerfile .
docker save -o ~/Downloads/ksan-api-portal.tar infinistor/ksan-api-portal
```

### ksan-portal 빌드
``` shell
#!/bin/bash
docker build --rm -t infinistor/ksan-portal:latest -f ./Portal/Dockerfile ./Portal
docker save -o ~/Downloads/ksan-portal.tar infinistor/ksan-portal

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

#### 공유용 폴더 생성
``` shell
mkdir /usr/local/ksan
mkdir /usr/local/ksan/etc
mkdir /usr/local/ksan/ssl
mkdir /usr/local/ksan/bin
mkdir /usr/local/ksan/sbin
mkdir /usr/local/ksan/share
mkdir /usr/local/ksan/custom
mkdir /usr/local/ksan/data
mkdir /usr/local/ksan/session
mkdir /var/log/ksan
mkdir /var/log/ksan/rabbitmq
chmod 775 /var/log/ksan/rabbitmq
```

### Mariadb 설치 (docker 이용)
``` shell
# mariadb 설치 밎 실행
# MYSQL_ROOT_HOST => 접속 호스트 제한
# MYSQL_ROOT_PASSWORD => root 권한자의 비밀번호
# MYSQL_DATABASE => 최소 생성시 생성할 DB명
docker run -d \
--net=host \
-e MYSQL_ROOT_HOST=% \
-e MYSQL_ROOT_PASSWORD=YOUR_DB_PASSWORD \
-e MYSQL_DATABASE=ksan \
-v /etc/localtime:/etc/localtime:ro \
-v /MYSQL:/var/lib/mysql \
--name mariadb \
mariadb
```

#### mariaDB 접속 방법
```shell
# UserName : 기본값 => root
# Password: 설치시 설정한 비밀번호
# IP : 설치한 서버의 주소
mysql -u UserName -p Password -h IP
```

### rabbitmq 설치(docker 이용)
``` shell
# rabbitmq 설치 및 실행
# RABBITMQ_DEFAULT_USER => 최초 생성시 유저 아이디
# RABBITMQ_DEFAULT_PASS => 최초 생성시 유저 비밀번호
docker run -d \
--net=host \
-e RABBITMQ_DEFAULT_USER=ksanmq \
-e RABBITMQ_DEFAULT_PASS=YOUR_MQ_PASSWORD \
-v /etc/localtime:/etc/localtime:ro \
-v /var/log/ksan/rabbitmq/:/var/log/rabbitmq/ \
--name rabbitmq \
rabbitmq:3-management
```

#### rabbitmq 접속방법
- 서버주소 : http://<ip>:15672
- username : RABBITMQ_DEFAULT_USER 값. 기본값 = guest
- password : RABBITMQ_DEFAULT_PASS 값. 기본값 = guest

### Portal 설치
#### docker 기본 이미지 로드하기 (빌드 할 경우)
``` shell
docker load -i ksan-portal-bridge.tar
docker load -i ksan-api-portal.tar
docker load -i ksan-portal.tar
```

#### docker 내부에서 사용할 아이피 생성
``` shell
docker network create --subnet=172.10.0.0/24 ksannet
```

#### ksan-portal-bridge (nginx) 컨테이너 생성
``` shell
docker create --net=host \
-v /etc/localtime:/etc/localtime:ro \
--name ksan-portal-bridge \
infinistor/ksan-portal-bridge:latest
```

#### ksan-portal 컨테이너 생성
``` shell
docker create -i -t \
--net ksannet \
--ip 172.10.0.11 \
-v /etc/localtime:/etc/localtime:ro \
-v /var/log/ksan/:/app/logs \
-v /usr/local/ksan:/usr/local/ksan \
--name ksan-portal \
infinistor/ksan-portal:latest
```

#### ksan-api-portal 컨테이너 생성
``` shell
docker create -i -t \
--net ksannet \
--ip 172.10.0.21 \
-v /etc/localtime:/etc/localtime:ro \
-v /var/log/ksan/:/app/logs \
-v /usr/local/ksan:/usr/local/ksan \
--name ksan-api-portal \
infinistor/ksan-api-portal:latest
```

#### 파일 복사 및 권한 수정
``` shell
cp ./setup/ksan-api-portal.service /etc/systemd/system/ksan-api-portal.service
cp ./setup/ksan-portal.service /etc/systemd/system/ksan-portal.service
cp ./setup/ksan-portal-bridge.service /etc/systemd/system/ksan-portal-bridge.service

chmod 777 /etc/systemd/system/ksan-api-portal.service
chmod 777 /etc/systemd/system/ksan-portal.service
chmod 777 /etc/systemd/system/ksan-portal-bridge.service
```

#### 인증서 발급
- 사설 인증기관에서 발급 받을 경우 pfx파일을 다운받아 사용하면 됩니다.
#### 생성된 인증서 파일을 업로드
docker cp infinistor.pfx ksan-api-portal:/app
```

#### Api 설정
- `./PortalSvr/appsettings sample.json`에 설정 예시가 존재합니다.
- 예시에 맞게 설정한 뒤 파일명을 `appsettings.json`으로 변경해야합니다.
- 이후 아래의 명령어로 적용 가능합니다.
  ``` shell
  docker cp appsettings.json ksan-api-portal:/app
  ```
- 기존 설정을 변경 하고 싶을 경우
  ``` shell
  docker cp appsettings.json ksan-api-portal:/app
  systemctl restart ksan-api-portal
  ```
- RabbitMQ, mariaDB의 접속 정보를 변경할 경우 모든 서비스의 설정에 반영해야 합니다.
- appsettings.json
  - PortalDatabase
    - Mgs Ip : 포탈이 설치되는 서버의 Ipaddress.	예시 : `192.168.10.1`
    - Database Name : 포탈의 메인 DB명	예시 : `ksan`
  - SharedAuthTicketKey
    - SharedAuthTicketKeyCertificateFilePath : ssl 인증서 파일의 이름. 발급 방법은 ssl 발급기관에서 직접 발급 받으면 됩니다.
    - SharedAuthTicketKeyCertificatePassword : 발급 받은 인증서의 암호.
  - RabbitMQ
    - Host : 포탈이 설치되는 서버의 Ipaddress	예시 : `192.168.10.1`
    - Port : RabbitMQ 통신 포트 번호 기본 값.
    - User, Password : 기본값은 guest, guest. RabbitMQ 설정에 따라 이 값은 변경될 수 있습니다.
``` json
{
	"AppSettings": {
		"Host": "ksan_mgnt",
		"Domain": "",
		"SharedAuthTicketKeyPath": "/usr/local/ksan/session",
		"SharedAuthTicketKeyCertificateFilePath": "sample.pfx",
		"SharedAuthTicketKeyCertificatePassword": "<Password>",
		"ExpireMinutes": 1440,
		"CreateAccountWhenNotExist": true,
		"RabbitMQ": {
			"Name": "PortalSvr",
			"Host": "<Mgs Ip>",
			"Port": 5672,
			"VirtualHost": "/",
			"User": "<RabbitMQ User>",
			"Password": "<RabbitMQ Password>",
			"Enabled": true
		}
	},
	"MariaDB":{
		"Host": "localhost",
		"Name": "ksan",
		"Port": 3306,
		"User":"<User>",
		"Password": "<Password>"
	},
	"MongoDB": {
		"Host": "localhost",
		"Name": "ksan",
		"Port": 3306,
		"User":"<User>",
		"Password": "<Password>"
	},
	"Logging": {
		"LogLevel": {
			"Default": "Information",
			"Microsoft": "Warning",
		}
	},
	"AllowedHosts": "*"
}
```

#### Portal 서비스 등록 및 실행
``` shell
# 서비스 등록
systemctl enable ksan-api-portal.service
systemctl enable ksan-portal.service
systemctl enable ksan-portal-bridge.service

# 서비스 실행
systemctl start ksan-api-portal
systemctl start ksan-portal
systemctl start ksan-portal-bridge

# ksan-portal-bridge는 80포트를 사용하기 때문에 해당 포트를 사용중인 프로그램을 종료
# httpd를 종료하고 ksan-portal-bridge 재실행
systemctl stop httpd
systemctl start ksan-portal-bridge
```
#### Swagger 접속 주소
- `https://<ip>:<port>/api`
- 별다른 설정 변경이 없을 시 : `https://localhost:5443/api`


## 업데이트 가이드

### API 업데이트
``` shell
# 서비스 정지
# docker로 시작했을 경우
docker stop ksan-api-portal
# service로 시작했을 경우
systemctl stop ksan-api-portal

# 기존 설정 백업
docker cp ksan-api-portal:/app/appsettings.json .

# 기존 컨테이너, 이미지 제거
docker rm ksan-api-portal
docker rmi infinistor/ksan-api-portal

# 새 이미지 업로드 및 컨테이너 생성
docker load -i ksan-api-portal.tar
docker create -i -t \
--net ksannet \
--ip 172.10.0.21 \
-v /etc/localtime:/etc/localtime:ro \
-v /var/log/ksan/:/app/logs \
-v /usr/local/ksan:/usr/local/ksan \
--name ksan-api-portal \
infinistor/ksan-api-portal:latest

# 설정 복구
docker cp appsettings.json ksan-api-portal:/app

# 서비스 시작
# docker로 시작했을 경우
docker start ksan-api-portal
# service로 시작했을 경우
systemctl start ksan-api-portal
```

### Portal 업데이트
``` shell
# 서비스 정지
# docker로 시작했을 경우
docker stop ksan-portal
# service로 시작했을 경우
systemctl stop ksan-portal

# 기존 컨테이너, 이미지 제거
docker rm ksan-portal
docker rmi infinistor/ksan-portal

# 새 이미지 업로드 및 컨테이너 생성
docker load -i ksan-portal.tar
docker create -i -t \
--net ksannet \
--ip 172.10.0.11 \
-v /etc/localtime:/etc/localtime:ro \
-v /var/log/ksan/:/app/logs \
-v /usr/local/ksan:/usr/local/ksan \
--workdir="/app" \
--name ksan-portal \
infinistor/ksan-portal:latest

# 서비스 시작
# docker로 시작했을 경우
docker start ksan-portal
# service로 시작했을 경우
systemctl start ksan-portal
```