# Lifecycle Manager

## 개요

### 용도

-   일정 시간마다 버킷의 수명주기 설정을 읽고, 해당 버킷에 반영하기 위한 프로그램

### 주요기능

-   버킷의 오브젝트를 수명주기 설정에 따라 삭제
-   버킷의 오브젝트 버전을 수명주기 설정에 따라 삭제
-   버킷의 삭제마커를 수명주기 설정에 따라 삭제

### 구현 예정

-   MongoDB 지원

## 구동환경

-   Docker : 20.10.0 이상

## How to Build

### Docker 설치

``` shell
yum update
yum -y install docker
systemctl enable docker
systemctl start docker
```

#### Centos7 Docker 버전 업데이트

``` shell
# docker 버전 확인
docker -v
Docker version 1.13.1, build 0be3e21/1.13.1
# 버전이 1.13.1일 경우 버전 업데이트
yum update -y
# 기존 버전 삭제
yum remove -y docker-common
# Docker Update에 필요한 Tool 설치
yum install -y yum-utils device-mapper-persistent-data lvm2
# Docker 공식 Repository 추가
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

# peer's certificate issuer is not recognized에러 발생시
  yum install -y ca-certificates
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

### Build

- 실행후 ksan-lifecycle.tar 이미지 파일이 생성됩니다.

```shell
#!/bin/bash
docker build --rm -t pspace/ksan-lifecycle:latest -f DockerFile .
docker save -o ksan-lifecycle.tar pspace/ksan-lifecycle
```

## How to Use

- KsanMon, KsanEdge, KsanPortal, MariaDB, KsanGw가 모두 동작하고 있어야 정상적으로 동작합니다.

### Lifecycle Manager 설정

#### 경로

-   등록 : **POST** `/api/v1/Config/Lifecycle`
-   목록 조회 : **GET** `/api/v1/Config/Lifecycle`
-   설정 삭제 : **DELETE** `/api/v1/Config/Lifecycle/{Version}`
-   특정버전을 최신버전으로 설정 : **PUT** `/api/v1/Config/Lifecycle/{Version}`

#### 옵션

-   값을 string 형태로 저장해야 합니다.

```json
{
	"Host": "192.168.13.10",
	"Port": 3306,
	"User": "root",
	"Password": "qwe123",
	"DatabaseName": "ksan",
	"Region": "kr-east-1"
}
```

#### 로그 설정

-   파일명 : `ksan-lifecycle.xml`
-   경로 : `/app/ksan-livecycle.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="true" scanPeriod="30 seconds">
	<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
		<layout class="ch.qos.logback.classic.PatternLayout">
			<pattern>%-5level %d{yyyy-MM-dd HH:mm:ss.SSS} [%thread{5}][%logger{10}.%method:%line] : %msg%n</pattern>
		</layout>
	</appender>
	<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
		<file>/app/log/lifecycle.log</file>
		<rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
			<fileNamePattern>/app/log/lifecycle.log.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
			<maxHistory>7</maxHistory>
			<maxFileSize>100MB</maxFileSize>
		</rollingPolicy>
		<encoder>
			<pattern>%-5level %d{yyyy-MM-dd HH:mm:ss.SSS} [%thread{5}][%logger{10}.%method:%line] : %msg%n</pattern>
		</encoder>
	</appender>
	<root level="info">
		<appender-ref ref="FILE"/>
	</root>
</configuration>
```

#### 프로그램 설치
``` shell
# 이미지 로드
docker load -i /root/docker/ksan-lifecycle.tar
# 컨테니어 생성
docker create -i -t --net ksannet --ip 172.10.0.31 -v /home/ksan/logs:/app/logs -v /etc/localtime:/etc/localtime:ro -v /home/ksan/share:/home/share -v /home/ksan/custom:/app/wwwroot/custom -v /usr/local/ksan/etc:/app/config --workdir=/app --name ksan-lifecycle pspace/ksan-lifecycle:latest
```

#### 실행예시(CLI)

```bash
docker start ksan-lifecycle
```

#### Crontab 설정
- 매일 1회 실행이 가장 이상적이며 필요에 의해 간격을 조정 할 수 있습니다.

``` shell
# 등록
crontab -e
# 매일 오전 1시에 동작
0 1 * * * docker start ksan-lifecycle

# 확인
crontab -l
```