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

### gateway 빌드
``` shell
cd setup/gateway
docker build -t pspace/ksangateway:latest .
docker save -o ~/Downloads/ksangateway.tar pspace/ksangateway
```

### ksanapi 빌드
``` shell
#!/bin/bash
#scripts/docker-build-api.sh
docker rmi pspace/ksanapi:latest
cp ./PortalSvr/.dockerignore ./.dockerignore
docker build --rm -t pspace/ksanapi:latest -f ./PortalSvr/Dockerfile .
docker save -o ~/Downloads/ksanapi.tar pspace/ksanapi
docker rmi $(docker images -f "dangling=true" -q)
```

### ksanportal 빌드
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

### Mariadb 설치 (docker 이용)
``` shell
# mariadb 설치 밎 실행
# MYSQL_ROOT_HOST => 접속 호스트 제한
# MYSQL_ROOT_PASSWORD => root 권한자의 비밀번호
# MYSQL_DATABASE => 최소 생성시 생성할 DB명
docker run -d -p 3306:3306 \
-e MYSQL_ROOT_HOST=% \
-e MYSQL_ROOT_PASSWORD=Password \
-e MYSQL_DATABASE=ksan \
-v /MYSQL:/var/lib/mysql \
--restart=unless-stopped \
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
-p 5672:5672 \
-p 15672:15672 \
-e RABBITMQ_DEFAULT_USER=guest \
-e RABBITMQ_DEFAULT_PASS=guest \
--restart=unless-stopped \
--name rabbitmq \
rabbitmq:3-management

```
#### rabbitmq 접속방법
- 서버주소 : http://<ip>:15672
- username : RABBITMQ_DEFAULT_USER 값. 기본값 = guest
- password : RABBITMQ_DEFAULT_PASS 값. 기본값 = guest

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
cp ./setup/ksanapi.service /etc/systemd/system/ksanapi.service
cp ./setup/ksanportal.service /etc/systemd/system/ksanportal.service
cp ./setup/ksangateway.service /etc/systemd/system/ksangateway.service

chmod 777 /etc/systemd/system/ksanapi.service
chmod 777 /etc/systemd/system/ksanportal.service
chmod 777 /etc/systemd/system/ksangateway.service
```

#### 인증서 발급
``` shell
# 사설 인증기관에서 발급 받을 경우 pfx파일을 다운받아 사용하면 됩니다.
# 로컬내에서 인증서 발급
/usr/local/ksan/ssl/ifs_objstorage_sign init

Enter Domain Name (default=PSPACE.KSAN): 
Enter Orig Unit (default=KSAN): 
Enter Alias (default=PSPACE): 
Enter Location (default=SEOUL): 
Enter Country (default=KOREA): 
Enter External DNS (default=DNS:localhost): 
Enter External IP (default=IP:127.0.0.1,IP:::1):
Enter key store password (default=37b46b57dbe0862f5737eb7117d0f107): YOUR_JKS_PASSWORD
Enter expire days (default=36500): 
Creating new ssl configuration.

# 생성된 인증서 파일을 pfx 파일로 변환
# 생성될 파일명 : pspace.pfx

cd /usr/local/ksan/ssl

keytool -importkeystore \
-srckeystore pspace.jks\
 -srcstoretype JKS \
-srcstorepass YOUR_JKS_PASSWORD \
-destkeystore pspace.pfx \
-deststoretype PKCS12 \
-deststorepass YOUR_PFX_PASSWORD

# 생성된 인증서 파일을 업로드
docker cp /usr/local/ksan/ssl/pspace.pfx ksanapi:/app
```

#### Api 설정
- `./PortalSvr/appsettings sample.json`에 설정 예시가 존재합니다.
- 예시에 맞게 설정한 뒤 파일명을 `appsettings.json`으로 변경해야합니다.
- 이후 아래의 명령어로 적용 가능합니다.
  ``` shell
  docker cp appsettings.json ksanapi:/app
  ```
- 기존 설정을 변경 하고 싶을 경우
  ``` shell
  docker cp appsettings.json ksanapi:/app
  systemctl restart ksanapi
  ```
- RabbitMQ, mariaDB의 접속 정보를 변경할 경우 모든 서비스의 설정에 반영해야 합니다.
- appsettings.json
  - PortalDatabase
    - Mgs Ip : 포탈이 설치되는 서버의 Ipaddress.	예시 : `192.168.10.1`
    - Database Name : 포탈의 메인 DB명	예시 : `ksan`
  - SharedAuthTicketKey
    - SharedAuthTicketKeyCertificateFilePath : ssl 인증서 파일의 이름. 발급 방법은 ssl 발급기관에서 직접 발급 받으면 됩니다.
    - SharedAuthTicketKeyCertificatePassword : 발급 받은 인증서의 암호.
  - RabbitMq
    - Host : 포탈이 설치되는 서버의 Ipaddress	예시 : `192.168.10.1`
    - Port : RabbitMq 통신 포트 번호 기본 값.
    - User, Password : 기본값은 guest, guest. RabbitMq 설정에 따라 이 값은 변경될 수 있습니다.
``` json
{
	"ConnectionStrings": {
		"PortalDatabase": "Server=<Mgs Ip>;Port=3306;Database=<Database Name>;Uid=<User>;Password=<Password>;CharSet=utf8;Pooling=True;Max Pool Size=100;"
	},
	"AppSettings": {
		"Host": "",
		"Domain": "",
		"SharedAuthTicketKeyPath": "/home/session",
		"SharedAuthTicketKeyCertificateFilePath": "sample.pfx",
		"SharedAuthTicketKeyCertificatePassword": "<Password>",
		"ExpireMinutes": 1440,
		"CreateAccountWhenNotExist": true,
		"RabbitMq": {
			"Name": "PortalSvr",
			"Host": "<Mgs Ip>",
			"Port": 5672,
			"VirtualHost": "/",
			"User": "<RabbitMq User>",
			"Password": "<RabbitMq Password>",
			"Enabled": true
		}
	},
	"Logging": {
		"LogLevel": {
			"Default": "Information",
			"Microsoft": "Warning",
			"Microsoft.Hosting.Lifetime": "Information",
			"PortalProvider": "Information"
		}
	},
	"AllowedHosts": "*"
}
```

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

# ksangateway는 80포트를 사용하기 때문에 해당 포트를 사용중인 프로그램을 종료
# httpd를 종료하고 ksangateway 재실행
systemctl stop httpd
systemctl start ksangateway
```
#### Swagger 접속 주소
- `https://<ip>:<port>/api`
- 별다른 설정 변경이 없을 시 : `https://localhost:5443/api`


## 업데이트 가이드

### API 업데이트
``` shell
# 서비스 정지
# docker로 시작했을 경우
docker stop ksanapi
# service로 시작했을 경우
systemctl stop ksanapi

# 기존 설정 백업
docker cp ksanapi:/app/appsettings.json .

# 기존 컨테이너, 이미지 제거
docker rm ksanapi
docker rmi pspace/ksanapi

# 새 이미지 업로드 및 컨테이너 생성
docker load -i ksanapi.tar
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

# 설정 복구
docker cp appsettings.json ksanapi:/app

# 서비스 시작
# docker로 시작했을 경우
docker start ksanapi
# service로 시작했을 경우
systemctl start ksanapi
```

### Portal 업데이트
``` shell
# 서비스 정지
# docker로 시작했을 경우
docker stop ksanportal
# service로 시작했을 경우
systemctl stop ksanportal

# 기존 컨테이너, 이미지 제거
docker rm ksanportal
docker rmi pspace/ksanportal

# 새 이미지 업로드 및 컨테이너 생성
docker load -i ksanportal.tar
docker create --net=host \
-p 80:80 \
-p 443:443 \
-v /etc/localtime:/etc/localtime:ro \
-v /home/ksan/share:/home/share \
--name ksangateway \
pspace/ksangateway:latest

# 서비스 시작
# docker로 시작했을 경우
docker start ksanportal
# service로 시작했을 경우
systemctl start ksanportal
```