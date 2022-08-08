
# KSAN: Quick Start Guide

## 1. KSAN 구성 환경

### 시스템 노드 정보 (본 가이드에서 활용한 서버 구성 예시)
 * mgs: 192.168.0.100
 * osd1: 192.168.0.111
 * osd2: 192.168.0.112

### OS
 * Centos7.5 이상(Centos8.0 미만)
 * mariadb 5.5 
 * java 11 이상

## 2. Portal 설치(MGS 노드)
### docker 설치 및 설정
``` bash
yum update             # has missing requires of  설치 업데이트 실패 하는 경우 yum --skip-broken update 로 진행 
                            # Error: Protected multilib versions 에러 발생 시 해당 package를 yum -y upgrade package* 로 업그레이드 해준다.
yum -y install docker
systemctl enable docker
systemctl start docker
sudo docker run hello-world // 컨테이너에 테스트 이미지를 실행하여 설치가 잘 되었는지 확인
systemctl stop firewalld // docker 에서 udp 사용이 가능하도록 방화벽 stop
```
### dotnet 구성
```bash
yum update
yum -y install docker
systemctl enable docker
systemctl start docker

# docker 방화벽 설정
## Docker에서 80, 443 포트 접속이 가능하도록 추가
sudo firewall-cmd --zone=public --permanent --add-port=443/tcp
sudo firewall-cmd --zone=public --permanent --add-port=80/tcp
sudo firewall-cmd --reload
# docker 위치 변경
## docker의 이미지 및 컨테이너 이용 폴더가 기본적으로 루트이기 때문에 나중에 용량 부족 등이 발생할 수 있음
sudo systemctl stop docker
sudo mv /var/lib/docker /home/docker
sudo ln -s /home/docker /var/lib/docker
sudo systemctl start docker
```

### mariadb 설치
```bash
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

# [mariadb 접속 확인]
# local ip 로 접속
mysql -uroot -pqwe123 -h 192.168.0.100
```

### rabbitmq 설치
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

### 빌드
 * 모든 이미지는 /opt/ksan_build 에 저장 

```bash
# 사전 구성
cd /ksan-master/portal/setup/aspnetcore_for_api
docker build -t pspace/aspnetcore_for_api:latest .

# [bridge 빌드]
cd ksan-master/portal/setup/bridge
docker rmi pspace/ksan-portal-bridge:latest # 기존 이미지 존재하면 삭제
docker build -t pspace/ksan-portal-bridge:latest .
mkdir /opt/ksan_build
docker save -o /opt/ksan_build/ksan-portal-bridge.tar pspace/ksan-portal-bridge  // 빌드 이미지 저장

# [api 빌드]
cd ksan-master/portalSvr
docker rmi pspace/ksan-portal-api:latest # 기존 이미지 존재하면 삭제
cp ./PortalSvr/.dockerignore ./.dockerignore
docker build --rm -t pspace/ksan-portal-api:latest -f ./PortalSvr/Dockerfile .
docker save -o /opt/ksan_build/ksan-portal-api.tar pspace/ksan-portal-api
docker rmi $(docker images -f "dangling=true" -q)  # 에러 발생할 경우 -f 옵션 적용

# [portal 빌드]
cd ksan-master/portal
docker rm ksan-portal # 기존 컨테이너 존재하며 삭제
docker rmi pspace/ksan-portal:latest  # 기존 이미지 존재하면 삭제
docker build --rm -t pspace/ksan-portal:latest -f ./Portal/Dockerfile ./Portal
docker save -o /opt/ksan_build/ksan-portal.tar pspace/ksan-portal
docker rmi $(docker images -f "dangling=true" -q)

```

### portal 이미지 로드 및 내부 ip 생성
```bash
cd /opt/ksan_build
docker load -i ksan-portal-bridge.tar
docker load -i ksan-portal-api.tar
docker load -i ksan-portal.tar

docker network create --subnet=172.10.0.0/24 ksannet
```

### 공유용 폴더 생성
```bash
mkdir /home/ksan
mkdir /home/ksan/ksan
mkdir /home/ksan/session
```

### 컨테이너 생성(실행 위치는 상관 없음)
```bash
# [web 컨테이터 생성]
docker create -i -t \
--net ksannet \
--ip 172.10.0.11 \
-v /etc/localtime:/etc/localtime:ro \
-v /home/ksan/logs:/app/logs \
-v /home/ksan/share:/home/share \
-v /home/ksan/custom:/app/wwwroot/custom \
-v /home/ksan/session:/home/session \
--workdir="/app" \
--name ksan-portal \
pspace/ksan-portal:latest

# [api 컨테이너 생성]
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
--name ksan-portal-api \
pspace/ksan-portal-api:latest

# [bridge(nginx) 컨테이터 생성]
docker create --net=host \
-p 80:80 \
-p 443:443 \
-v /etc/localtime:/etc/localtime:ro \
-v /home/ksan/share:/home/share \
--name ksan-portal-bridge \
pspace/ksan-portal-bridge:latest

# [service 관리 파일 복사 및 권한 수정]
cd ksan-master/portal/
cp ./setup/ksan-portal-api.service /etc/systemd/system/ksan-portal-api.service
cp ./setup/ksan-portal.service /etc/systemd/system/ksan-portal.service
cp ./setup/ksan-portal-bridge.service /etc/systemd/system/ksan-portal-bridge.service

chmod 777 /etc/systemd/system/ksan-portal-api.service
chmod 777 /etc/systemd/system/ksan-portal.service
chmod 777 /etc/systemd/system/ksan-portal-bridge.service

# [portal service 등록 실행]
systemctl enable ksan-portal-api.service
systemctl enable ksan-portal.service
systemctl enable ksan-portal-bridge.service

# [portal service 실행]
# ksanbridge는 80포트를 사용하기 때문에 해당 포트를 사용중인 프로그램을 종료 해야함.
systemctl start ksan-portal-api
systemctl start ksan-portal
systemctl start ksan-portal-bridge
```

### rabbitmq 설치
```bash
# [ rabbitmq 설치 및 실행 ]
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 --restart=unless-stopped rabbitmq:3-management
# x509: certificate signed by unknown authority 에러 발생 시  yum install -y ca-certificates 설치

# [rabbitmq 서버 접속 확인]
http://<ip>:15672 
username: guest
password:guest
```

## 3. 유틸리티를 사용할 가상환경 생성 및 common 모듈 설치(MGS, OSD1, OSD2 노드)
 * ksanOsd, ksanGw 서비스 및 시스템 관리 유틸리티 사용을 위한 python 가상환경을 생성 한다.
 * MGS, OSD1,OSD2 각 노드에 동일하게 유틸리티 관련 모듈을 설치한다. 
 * ksanOsd, ksanGw 빌드 및 설치 전에 수행되어야 함. 

### python3.6 설치 및 가상환경 구성
```bash
# [epel 설치]
yum install http://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
yum repolist
# Error: Cannot retrieve metalink for repository: epel. Please verify its path and try again 발생하는 경우
# /etc/yum.repos.d/epel.repo 파일의 baseurl을 주석 해제하고 metalink 또는 mirrorlist 로 시작하는 라인을 주석 처리
vim /etc/yum.repos.d/epel.repo
baseurl=http://download.example/~ //주석 해제
#metalink=https:~

# [python3.6 설치]
yum -y install python36
yum -y install python36-devel
easy_install-3.6 pip

# [python 가상환경 구성]
```bash
mkdir /root/ksan_system  // 가상환경을 수행할 위치에 ksan_system 디렉토리 생성(디렉토리 이름은 상환 없음)
python3.6 -m venv /root/ksan_system // 가상환경 생성
# Error: Command '['/home/kanta/venv/bin/python3', '-Im', 'ensurepip', '--upgrade', '--default-pip']' returned non-zero exit status 1. 에러 발생 시 python3.6 재설치 해야함.

source /root/ksan_system/bin/activate  // 가상환경 활성화
```
### python dependency 및 common 모듈 설치
```bash 
cd ksan-master/core/common
pip  install -r requirements.txt
# pip 명령어 사용 중 SSL 관련 에러 발생 시 --trusted-host pypi.org --trusted-host pypi.python.org install --trusted-host files.pythonhosted.org 옵션을 사용
# pip 업데이트가 필요하면 수행(pip install --upgrade pip) 
python setup.py install
```

## 4. 시스템 관리 유틸리티 설치(MGS 노드)
### 유틸리티 실행 환경 설정 및 시스템 관리 유틸리티 설치
```bash
# 시스템 관리 유틸리티 설치
cd ksan-master/core/mgs/util
python setup.py install
```

## 5. ksanOsd & ksanGw 빌드 및 설치(OSD1, OSD2 노드)  
 * ksanOsd 빌드 후 ksanGw 빌드 수행해야 함.

### java 버전 업데이트 및 maven 설치
```bash
# [java 버전이 낮을 경우 버전 업데이트]
yum -y install java-11-openjdk.x86_64  java-11-openjdk-devel.x86_64 

# [default java 버전 변경]
/usr/sbin/alternatives --config java
There are 3 programs which provide 'java'.

  Selection    Command
-----------------------------------------------
*  1           java-1.8.0-openjdk.x86_64 (/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.312.b07-1.el7_9.x86_64/jre/bin/java)
   2           java-1.7.0-openjdk.x86_64 (/usr/lib/jvm/java-1.7.0-openjdk-1.7.0.261-2.6.22.2.el7_8.x86_64/jre/bin/java)
+ 3           java-11-openjdk.x86_64 (/usr/lib/jvm/java-11-openjdk-11.0.13.0.8-1.el7_9.x86_64/bin/java)
Enter to keep the current selection[+], or type selection number: 3

# [변경된 java 버전 확인]
java --version
openjdk 11.0.13 2021-10-19 LTS
OpenJDK Runtime Environment 18.9 (build 11.0.13+8-LTS)
OpenJDK 64-Bit Server VM 18.9 (build 11.0.13+8-LTS, mixed mode, sharing)

#[ java 환경 변수 설정]
vim /etc/profile
JAVA_HOME=/usr/lib/jvm/java-11
export JAVA_HOME 
PATH=$PATH:$JAVA_HOME/bin 
export PATH

#[ /etc/profile 적용]
source /etc/profile

# [maven 설치]
yum -y install maven
```

### 빌드 및 설치

```bash
#[ksanOsd 빌드]
cd ksan-master/core/osd
sh install.sh

# [ksanOsd 빌드]
cd ksan-master/core/gw
sh install.sh
```

### object DB 설치(OSD1 노드)
#### docker 설치 및 설정
``` bash
# [docker 설치 및 실행]
yum update             # has missing requires of  설치 업데이트 실패 하는 경우 yum --skip-broken update 로 진행 
                            # Error: Protected multilib versions 에러 발생 시 해당 package를 yum -y upgrade package* 로 업그레이드 해준다.
yum -y install docker
systemctl enable docker
systemctl start docker

# [docker 버전 확인]
# 버전이 1.13.1일 경우 docker 업데이트 해야함.
docker -v
Docker version 1.13.1, build 0be3e21/1.13.1
# [docker 버전 업데이트]
yum update
yum remove -y docker-common # 기존 버전 삭제
yum install -y yum-utils device-mapper-persistent-data lvm2 # Docker Update에 필요한 Tool 설치
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo  # Docker 공식 Repository 추가

# [peer's certificate issuer is not recognized에러 발생시]
yum -y install ca-certificates
update-ca-trust force-enable
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo # 다시 Docker 공식 Repository 추가

# [ 최신 docker 설치]
yum list docker-ce --showduplicates | sort -r  # 설치 가능한 버전 정보 확인
yum install -y docker-ce  
systemctl enable docker  # Docker를 부팅시 실행되도록 설정
systemctl start docker  # Docker 시작

# [docker 위치 변경]
# 기본 위치가 루트로 추후 용량부족이 발생할 수 있음.
sudo systemctl stop docker
sudo mv /var/lib/docker /home/docker
sudo ln -s /home/docker /var/lib/docker
sudo systemctl start docker

# [mariadb 이미지 다운로드] 
docker pull mariadb


# [mariadb container 생성 및 DB 초기화] 
# DB 포트는 3306, DB 는 ksan, DB 패스워드는 'qwe123' , mysql 용 볼륨패스는 local 서버의 /MYSQL 로 설정
# local 서버의 볼륨 패스의 기존 파일들은 모두 삭제
docker container run -d -p 3306:3306 \
-e MYSQL_ROOT_PASSWORD=qwe123 \
-e MYSQL_DATABASE=ksan \
-v /MYSQL:/var/lib/mysql --name mariadb mariadb
# listen tcp4 0.0.0.0:3306: bind: address already in use 등의 이유로 실패할 경우 이슈 해결 후 생성 된 컨테이너를 strt 한다.
docker ps -a  # 컨테이너 조회
CONTAINER ID   IMAGE     COMMAND                  CREATED         STATUS    PORTS     NAMES
1e2c8428fbbe   mariadb   "docker-entrypoint.s…"   5 minutes ago   Created             mariadb

docker start 1e2c8428fbbe # docker start <컨테이너 id> 명령어로 해당 컨테이너 서비스를 실행

# [컨테이너에 vi 설치]
docker exec -it mariadb /bin/bash
apt-get update
apt-get install vim

# [my.cnf 수정]
vim /etc/mysql/my.cnf
[mysqld]                           # 추가
max_connections =1000  # 추가

# [mysql secure 설정]
mysql_secure_installation

NOTE: RUNNING ALL PARTS OF THIS SCRIPT IS RECOMMENDED FOR ALL MariaDB
      SERVERS IN PRODUCTION USE!  PLEASE READ EACH STEP CAREFULLY!

In order to log into MariaDB to secure it, we'll need the current
password for the root user. If you've just installed MariaDB, and
haven't set the root password yet, you should just press enter here.

Enter current password for root (enter for none): 
OK, successfully used password, moving on...

Setting the root password or using the unix_socket ensures that nobody
can log into the MariaDB root user without the proper authorisation.

You already have your root account protected, so you can safely answer 'n'.

Switch to unix_socket authentication [Y/n] n
 ... skipping.

You already have your root account protected, so you can safely answer 'n'.

Change the root password? [Y/n] n
 ... skipping.

By default, a MariaDB installation has an anonymous user, allowing anyone
to log into MariaDB without having to have a user account created for
them.  This is intended only for testing, and to make the installation
go a bit smoother.  You should remove them before moving into a
production environment.

Remove anonymous users? [Y/n] Y
 ... Success!

Normally, root should only be allowed to connect from 'localhost'.  This
ensures that someone cannot guess at the root password from the network.

Disallow root login remotely? [Y/n] n
 ... skipping.

By default, MariaDB comes with a database named 'test' that anyone can
access.  This is also intended only for testing, and should be removed
before moving into a production environment.

Remove test database and access to it? [Y/n] Y
 - Dropping test database...
 ... Success!
 - Removing privileges on test database...
 ... Success!

Reloading the privilege tables will ensure that all changes made so far
will take effect immediately.

Reload privilege tables now? [Y/n] Y
 ... Success!

Cleaning up...

All done!  If you've completed all of the above steps, your MariaDB
installation should now be secure.

Thanks for using MariaDB!

exit

# [mariadb 재시작]
docker stop mariadb
docker start mariadb

# [mariadb 접속 확인]
# local ip 로 접속
mysql -uroot -pqwe123 -h 192.168.0.100
```

## 6. 서버 등록(MGS, OSD1, OSD2 노드)
 * 시스템 정보 설정 및 ksanEdge & ksanMon 데몬은 모든 노드에서 동일하게 수행 되어야 함.
 * 각 노드들이 시스템에 등록된다.

### 시스템 정보 설정 및 ksanEdge & ksanMon 데몬 시작
```bash
# 설정 초기화
/usr/local/ksan/bin/ksanEdge init
Insert Mgs Ip(default: 192.168.0.110):  # MGS ip
Insert Mgs Port(default: 5443):          # portal port
Insert Mq Port(default: 5672):           # rabbitmq port
Insert Management Network device(default: ens192):  # 관리 네트워크 설정

# ksanEdge & ksanMon 데몬 시작
/usr/local/ksan/bin/ksanEdge start
/usr/local/ksan/bin/ksanEdge status
KsanEdge ...  Ok

/usr/local/ksan/bin/ksanMon start
/usr/local/ksan/bin/ksanMon status
KsanMon ...  Ok

# 설정 정보 확인
cat /usr/local/ksan/etc/ksanMon.conf
[mgs]
MgsIp = 192.168.0.110
IfsPortalPort = 5443
MqPort = 5672
ServerId = <server ID>    // 시스템에 등록된 server id
ManagementNetDev = <network device>
DefaultNetworkId = <default network ID>  // 시스템에 등록 된 network id 
IfsPortal = 5443

# 서버 등록 확인
/usr/local/ksan/bin/ksanServer list
==================================================================================
|        Name        |       State        |                  Id                  |
==================================================================================
|       mgs          |       Online       | e40bbdfd-1373-455a-b78c-e1822814d905 |
----------------------------------------------------------------------------------
|       osd1         |       Online       | 1dffd8df-7afc-4087-aeb1-85b69e382db9 |
----------------------------------------------------------------------------------
|       osd2         |       Online       | 46007614-be5f-44c2-99c1-eea42097fd5a |
----------------------------------------------------------------------------------
```

## 6. 디스크 설정(MGS 노드)
 * 디스크 관리를 위한 유틸리티
 
### 추가
 * /usr/local/ksan/bin/ksanDisk add -S [Server Id] -p [Disk 마운트 패스]
 * 서버 Id는 ksanServer list 로확인
 * osd1, osd2 에 디스크 마운트 패스는 각각 /DISK1 로 설정 되어 있는 경우 결과는 아래와 같다.
 
```bash
/usr/local/ksan/bin/ksanDisk add -S 1dffd8df-7afc-4087-aeb1-85b69e382db9 -p /DISK1  # osd1
Success

/usr/local/ksan/bin/ksanDisk add -S 46007614-be5f-44c2-99c1-eea42097fd5a -p /DISK1  # osd2
Success

./ksanDisk list
===================================================================================
|   ServerName  |                DiskId                |      Path     |  State   |
===================================================================================
|     osd1         | 4d6e8258-e7a6-4404-a2c7-044e987d9452 |     /DISK1    |   Stop   |
-----------------------------------------------------------------------------------
|     osd2      | 985b3890-63fd-43e9-8e24-8a8c80038ca6 |     /DISK1    |   Stop   |
-----------------------------------------------------------------------------------
```

### 시작
 * 디스크가 사용 가능하도록 활성화 한다.
 * /usr/local/ksan/bin/ksanDisk start -I [Disk Id]
 
```bash
/usr/local/ksan/bin/ksanDisk start -I 4d6e8258-e7a6-4404-a2c7-044e987d9452 # osd1
Success

/usr/local/ksan/bin/ksanDisk start -I 985b3890-63fd-43e9-8e24-8a8c80038ca6 # osd2
```

### 정지
 * DISK 사용을 중지해야 할 경우 아래와 같이 수행 한다.
 * /usr/local/ksan/bin/ksanDisk stop -I [Disk Id]
 
```bash
/usr/local/ksan/bin/ksanDisk stop -I 985b3890-63fd-43e9-8e24-8a8c80038ca6
Success
```

## 7. 디스크 풀 설정(MGS 노드)
 * 디스크 풀을 관리하는 유틸리티
 
### 생성
 * /usr/local/ksan/bin/ksanDiskpool add -n [Disk Pool Name]
```bash
/usr/local/ksan/bin/ksanDiskpool add -n diskpool1
Success 
```

### 삭제
* /usr/local/ksan/bin/ksanDiskpool remove -I [Disk Pool Id]
```bash
/usr/local/ksan/bin/ksanDiskpool remove -I f1bec273-311c-4b1a-81d1-9b9abe17f182
Success
```

### 조회
```bash
/usr/local/ksan/bin/ksanDiskpool list
==================================================================================
|        Name        |                PoolId                |     Descrition     |
==================================================================================
|     DiskPool1      | e24056f9-bd8c-4631-a888-76ea2660bc23 |        None        |
----------------------------------------------------------------------------------
|     DiskPool2      | 1397f2cf-4b0a-4e69-891c-92f7ef82e6e9 |        None        |
----------------------------------------------------------------------------------

./usr/local/ksan/bin/ksanDiskpool list -L
==================================================================================================
|        Name        |                PoolId                |             Descrition             |
==================================================================================================
|     DiskPool1      | e24056f9-bd8c-4631-a888-76ea2660bc23 |                None                |
--------------------------------------------------------------------------------------------------
                     |                DiskId                |              DiskPath              |
--------------------------------------------------------------------------------------------------
|     DiskPool1      | 1397f2cf-4b0a-4e69-891c-92f7ef82e6e9 |                None                |
--------------------------------------------------------------------------------------------------
                     |                DiskId                |              DiskPath              |
                     -----------------------------------------------------------------------------
```

### 디스크 풀 에 디스크 추가
 * 설정 된 디스크 풀에 신규 디스크를 추가 또는 삭제 한다.
 * /usr/local/ksan/bin/ksanDiskpool addDisk -I [Disk Pool Id] -D [Disk Id]
 
```bash
/usr/local/ksan/bin/ksanDiskpool addDisk -I e24056f9-bd8c-4631-a888-76ea2660bc23 -D 4d6e8258-e7a6-4404-a2c7-044e987d9452
Success

/usr/local/ksan/bin/ksanDiskpool addDisk -I 1397f2cf-4b0a-4e69-891c-92f7ef82e6e9  -D 985b3890-63fd-43e9-8e24-8a8c80038ca6
Success
```

### 디스크 풀 에서 디스크 삭제
 * /usr/local/ksan/bin/ksanDiskpool removeDisk -I [Disk Pool Id] -D [Disk Id]

```bash
/usr/local/ksan/bin/ksanDiskpool removeDisk -I 1397f2cf-4b0a-4e69-891c-92f7ef82e6e9 -D 985b3890-63fd-43e9-8e24-8a8c80038ca6
Success
```

### 조회
 * 디스크 정보를 확인 한다.
 ```bash
 /usr/local/ksan/bin/ksanDisk list -L
===========================================================================================================================================================================================================================================
|   ServerName  |                DiskId                |      Path     |  State   |       Total        |        Used        |        Free        |  RwMode  |              DiskPoolId              |               ServerId               |
===========================================================================================================================================================================================================================================
|     osd1  | 4d6e8258-e7a6-4404-a2c7-044e987d9452 |     /DISK1    |   Good   |         0          |         0          |         0          |ReadWrite |   e24056f9-bd8c-4631-a888-76ea2660bc23    | 1dffd8df-7afc-4087-aeb1-85b69e382db9 |
-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
|     osd2     | 985b3890-63fd-43e9-8e24-8a8c80038ca6 |     /DISK1    |   Good   |         0          |         0          |         0          |ReadWrite |  1397f2cf-4b0a-4e69-891c-92f7ef82e6e9  | 46007614-be5f-44c2-99c1-eea42097fd5a |
-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 ```

## 8. OSD & GW 서비스 설정
### ksanOsd 서비스 추가(OSD1, OSD2)
 * ksanOsd 서비스 설정을 초기화 하고 시작 한다.
 
```bash
/usr/local/ksan/bin/ksanOsd init
Insert Local Ip(default: 192.168.0.111): 
Insert KsanOsd Port(default: 8000):
Insert Pool Size(default: 10):
Insert Object Directory(default: obj):
Insert Trash Directory(default: trash):
Insert Write Temp Directory(default: temp):
Done

/usr/local/ksan/bin/ksanOsd start
Success 
/usr/local/ksan/bin/ksanOsd status
ksanOsd ... Ok
```

### ksanGw 서비스 추가(OSD1, OSD2)
 * ksanGw 서비스 설정을 초기화 하고 서비스를 시작 한다.
 * metadata DB 가 192.168.0.111 에 구성 되어 있다.

```bash
/usr/local/ksan/bin/ksanGw init
Insert Logging DB Type(default: MariaDB):
Insert Logging DB Host(default: 192.168.0.111):
Insert Logging DB Name(default: ksan):
Insert Logging DB Port(default: 3306):
Insert Logging DB User(default: ksan):
Insert Logging DB Password(default: ):qwe123
Insert S3 End Point Url(default: http://0.0.0.0:8080):
Insert S3 Secure End Point Url(default: https://0.0.0.0:8443):
Insert S3 Replication(default: 1):
Insert Osd Port(default: 8000):
Insert Local Ip(default: 192.168.11.216):
Insert Apache tomcat path(default: /opt/apache-tomcat-9.0.53):
Insert Object DB Type(default: MYSQL):
Insert Object DB Host(default: 192.168.0.111):
Insert Object DB Name(default: ksan):
Insert Object DB Port(default: 3306):
Insert Object DB User(default: ksan):
Insert Object DB Password(default: ):qwe123
Insert Mq Server Host(default: 192.168.0.111):
Insert Mq DiskPool Queue Name(default: disk):
Insert Mq DiskPool Exchange Name(default: disk):
Insert Osd Exchange Name(default: OSDExchange):
Done

/usr/local/ksan/bin/ksanGw start
Success 
Using CATALINA_BASE:   /opt/apache-tomcat-9.0.53
Using CATALINA_HOME:   /opt/apache-tomcat-9.0.53
Using CATALINA_TMPDIR: /opt/apache-tomcat-9.0.53/temp
Using JRE_HOME:        /usr
Using CLASSPATH:       /opt/apache-tomcat-9.0.53/bin/bootstrap.jar:/opt/apache-tomcat-9.0.53/bin/tomcat-juli.jar
Using CATALINA_OPTS:   
Using CATALINA_PID:    ./bin/catalina.pid
Existing PID file found during start.
Removing/clearing stale PID file.
Tomcat started.
```

### 조회
* 등록한 서비스 정보를 조회한다.

```bash
/usr/local/ksan/bin/ksanService list 
Success 
==========================================================================================================================
|        Name        |  State   |                  Id                  |   Type   |               GroupId                |
==========================================================================================================================
|     osd1_OSD      | Offline  | 3cc35a3b-7965-48f0-ae71-81e3663b7f54 |   Osd    |                 None                 |
--------------------------------------------------------------------------------------------------------------------------
|     osd2_OSD     | Offline  | 65c5a24c-9ec4-4089-8dfd-4159502ee10a |   S3GW   |                 None                 |
--------------------------------------------------------------------------------------------------------------------------
|  osd1_GW   |  Online  | ee1219ef-0957-4286-bdc5-6e64f5fba971 |   Osd    |                 None                 |
--------------------------------------------------------------------------------------------------------------------------
|  osd2_GW  |  Online  | 7fe05e84-8e71-44db-801e-3d0b1de9e945 |   S3GW   |                 None                 |
--------------------------------------------------------------------------------------------------------------------------
```
## 9. 서비스 그룹 설정(MGS 노드)
### 생성
 * /usr/local/ksan/bin/ksanServicegroup add -n [Group Name] -T [ Service Type: GW|OSD]
 
```bash
/usr/local/ksan/bin/ksanServicegroup add -n OsdGroup1 -T OSD  # OSD 타입 서비스 그룹
Success 

/usr/local/ksan/bin/ksanServicegroup add -n GwGroup1 -T GW  # GW 타입 서비스 그룹
Success
```

### 삭제
 * /usr/local/ksan/bin/ksanServicegroup remove -G [Servcie Group Id]
 
```bash
/usr/local/ksan/bin/ksanServicegroup remove -G 02239aa6-b7b4-411c-bf29-84baeaebeaff
Success
```

### 조회
```bash
./ksanServicegroup list 
Success 
===============================================================================================================================================================================
|     Name     |    Description     |                   Id                   |  ServiceType  |  ServiceIpaddress  |                                                           |
===============================================================================================================================================================================
|  OsdGroup1   |        None        |  5484840b-daed-442e-aaa4-769288468273  |      OSD      |        None        |                                                           |
-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
|  GwGroup1   |        None        |  02239aa6-b7b4-411c-bf29-84baeaebeaff  |      GW      |        None        |                                                           |
-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
```

### 등록 된 서비스 그룹 확인
```bash
./ksanServicegroup list -L
Success 
===============================================================================================================================================================================
|     Name     |    Description     |                   Id                   |  ServiceType  |  ServiceIpaddress  |                                                           |
===============================================================================================================================================================================
|  OsdGroup1   |        None        |  5484840b-daed-442e-aaa4-769288468273  |      Osd      |        None        |                                                           |
-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
          |  Service Name  |    Description     |               Service Id               |     State     |  Service Type |     Cpu Usage      |     Memoy Used     |Thread Cnt|
-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
|  OsdGroup2   |        None        |  02239aa6-b7b4-411c-bf29-84baeaebeaff  |      Osd      |        None        |                                                           |
-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
          |  Service Name  |    Description     |               Service Id               |     State     |  Service Type |     Cpu Usage      |     Memoy Used     |Thread Cnt|
          ---------------------------------------------------------------------------------------------------------------------------------------------------------------------
```


### 그룹에 서비스 추가
 * /usr/local/ksan/bin/ksanServicegroup addService -G [Service Group Id] -D [Service Id]

```bash
/usr/local/ksan/bin/ksanServicegroup addService -G 5484840b-daed-442e-aaa4-769288468273 -D 3cc35a3b-7965-48f0-ae71-81e3663b7f54
Success

/usr/local/ksan/bin/ksanServicegroup addService addService -G 02239aa6-b7b4-411c-bf29-84baeaebeaff -D 65c5a24c-9ec4-4089-8dfd-4159502ee10a
Success
```

### 그룹에서 서비스 삭제
 * /usr/local/ksan/bin/ksanServicegroup removeService -G [Service Group Id] -D [Service Id]

```bash
/usr/local/ksan/bin/ksanServicegroup removeService -G 02239aa6-b7b4-411c-bf29-84baeaebeaff -D 65c5a24c-9ec4-4089-8dfd-4159502ee10a
Success 
```

## 10. 서비스 관리(MGS 노드)
 * 등록된 서비스를 조회/시작/정지/재시작 한다.

### 조회
* 서비스 정보를 조회한다.

```bash
/usr/local/ksan/bin/ksanService list 

==========================================================================================================================
|        Name        |  State   |                  Id                  |   Type   |               GroupId                |
==========================================================================================================================
|     osd1_OSD      | Offline  | 3cc35a3b-7965-48f0-ae71-81e3663b7f54 |   Osd    |                 None                 |
--------------------------------------------------------------------------------------------------------------------------
|     osd2_OSD     | Offline  | 65c5a24c-9ec4-4089-8dfd-4159502ee10a |   S3GW   |                 None                 |
--------------------------------------------------------------------------------------------------------------------------
|  osd1_GW   |  Online  | ee1219ef-0957-4286-bdc5-6e64f5fba971 |   Osd    |                 None                 |
--------------------------------------------------------------------------------------------------------------------------
|  osd2_GW  |  Online  | 7fe05e84-8e71-44db-801e-3d0b1de9e945 |   S3GW   |                 None                 |
--------------------------------------------------------------------------------------------------------------------------

/usr/local/ksan/bin/ksanService list -L
===========================================================================================================================================================================================================================================
|     ServerName     |    ServiceName     |              ServiceId               |            ServiceGruopId            |  State   |   Type   |      CpuUsage      |     MemoryUsed     |Thread Cnt|               ServerId               |
===========================================================================================================================================================================================================================================
|       osd1_OSD        |     dsan3_Osd      | 3cc35a3b-7965-48f0-ae71-81e3663b7f54 | 5484840b-daed-442e-aaa4-769288468273  | Online  |   OSD   |        0.0         |    648589312.0     |    2     | 1dffd8df-7afc-4087-aeb1-85b69e382db9 |
-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
|       osd2_OSD        |     dsan3_S3gw     | 65c5a24c-9ec4-4089-8dfd-4159502ee10a | 5484840b-daed-442e-aaa4-769288468273 | Online  |   GW   |        0.0         |    648617984.0     |    2     | 46007614-be5f-44c2-99c1-eea42097fd5a |
-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
|   osd1_GW    |        osd1        | ee1219ef-0957-4286-bdc5-6e64f5fba971 | 02239aa6-b7b4-411c-bf29-84baeaebeaff |  Online  |   OSD    |        0.0         |        0.0         |    26700063741.0     | 1dffd8df-7afc-4087-aeb1-85b69e382db9 |
-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
|    osd2_GW     |  mds1_single_S3gw  | 7fe05e84-8e71-44db-801e-3d0b1de9e945 | 02239aa6-b7b4-411c-bf29-84baeaebeaff |  Online  |   GW   |        0.0         |   27750043648.0    |    36    | 46007614-be5f-44c2-99c1-eea42097fd5a |
-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
```

### 시작
 * 기존 서비스를 시작한다.
 * /usr/local/ksan/bin/ksanService start -I [Service Id]

```bash
/usr/local/ksan/bin/ksanService start -I 3cc35a3b-7965-48f0-ae71-81e3663b7f54
Success 
```

### 정지
 * 기존 서비스를 정지한다.
 * /usr/local/ksan/bin/ksanService stop -I [Service Id]

```bash
/usr/local/ksan/bin/ksanService stop -I 3cc35a3b-7965-48f0-ae71-81e3663b7f54
Success 
```

### 재시작
 * 기존 서비스를 재시작한다.
 * /usr/local/ksan/bin/ksanService restart -I [Service Id]

```bash
/usr/local/ksan/bin/ksanService restart -I 3cc35a3b-7965-48f0-ae71-81e3663b7f54
Success 
```

### 삭제
 * 기존 서비스를 삭제한다.
 * 서비스를 삭제를 수행하기위해 해당서비스를 중지 해야한다.
 * /usr/local/ksan/bin/ksanService remove -I [Service Id]

```bash
/usr/local/ksan/bin/ksanService stop -I 3cc35a3b-7965-48f0-ae71-81e3663b7f54
Success

/usr/local/ksan/bin/ksanService remove -I 3cc35a3b-7965-48f0-ae71-81e3663b7f54
Success 
```

## 11. 전체 시스템 정보 확인 (MGS)
* 전체 시스템 정보를 확인한다.

```bash
/usr/local/ksan/bin/ksanSysinfo 

[SERVER | NETWORK | DISK] 
=====================================================================================================================================================================================================
|         Name        |      CpuModel      |  Clock   |  State   | LoadAvg(1M 5M 15M)|  MemTotal(MB) |  MemUsed(MB)  |                  Id                  |                                       |
=====================================================================================================================================================================================================
|        mgs        |       x86_64       |    0     | Offline  | 0.03 | 0.04| 0.05 |       0       |      856      | e40bbdfd-1373-455a-b78c-e1822814d905 |
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                     |                 DiskId                 |      Path     |  State   | Total Size(MB)| Used Size(MB) | Free Size(MB) |  Total Inode  |   Used Inode  |   Free Inode  |  RwMode  |
                     --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                                                      |   Network Device   |     IpAddress      |    Rx    |    Tx    |     LinkStatus     |                   Id                   |
                                                                      -------------------------------------------------------------------------------------------------------------------------------
                                                                      |       ens192       |   192.168.0.100   |   2105   |   7761   |         Up         |  0205361b-bb48-4418-9fd3-e5f53f300f7e  |
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
|        osd1        |       x86_64       |    0     |  Online  | 0.0  | 0.01| 0.05 |      3974     |      208      | 1dffd8df-7afc-4087-aeb1-85b69e382db9 |
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                     |                 DiskId                 |      Path     |  State   | Total Size(MB)| Used Size(MB) | Free Size(MB) |  Total Inode  |   Used Inode  |   Free Inode  |  RwMode  |
                     --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                     |  1ab3de01-5e21-426a-a7ee-fb59189b34f3  |     /DISK1    |   Good   |       154685904       |       9495856       |       137309396       |       9830400       |      6462       |       9823938       |ReadWrite |
                     --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                                                      |   Network Device   |     IpAddress      |    Rx    |    Tx    |     LinkStatus     |                   Id                   |
                                                                      -------------------------------------------------------------------------------------------------------------------------------
                                                                      |       ens192       |   192.168.0.111   |    896     |    1072     |         Up         |  37341540-a6ba-40eb-a5e0-bc804fb6c550  |
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
|        osd2        |       x86_64       |    0     |  Online  | 0.0  | 0.0 | 0.0  |      3974     |       100      | 46007614-be5f-44c2-99c1-eea42097fd5a |
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                     |                 DiskId                 |      Path     |  State   | Total Size(MB)| Used Size(MB) | Free Size(MB) |  Total Inode  |   Used Inode  |   Free Inode  |  RwMode  |
                     --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                     |  985b3890-63fd-43e9-8e24-8a8c80038ca6  |    /DISK1    |   Good   |       49017312       |       76444       |       46303044       |       13107200       |       5801       |       13101399       |ReadWrite |
                     --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                                                                      |   Network Device   |     IpAddress      |    Rx    |    Tx    |     LinkStatus     |                   Id                   |
                                                                      -------------------------------------------------------------------------------------------------------------------------------
                                                                      |       ens192       |   192.168.0.112   |    2593     |    1659     |         Up         |  6ef2fcb5-a68d-4ca2-ac3f-905c852a8870  |
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
=====================================================================================================================================================================================================
```
