# metering (S3GW Metering Backend)

## 개요

### 용도

-   로깅 데이터를 수집하여 metering 정보를 저장

### 주요 기능

-   bucket별 filecount, 용량 저장
-   bucket별 upload, download 스트림 크기 저장

#### 경로

-   등록 : **POST** `/api/v1/Config/Metering`
-   목록 조회 : **GET** `/api/v1/Config/Metering`
-   설정 삭제 : **DELETE** `/api/v1/Config/Metering/{Version}`
-   특정버전을 최신버전으로 설정 : **PUT** `/api/v1/Config/Metering/{Version}`

## 로그 파일

-   설정 파일 : ksan-metering.xml (/app/ksan-metering.xml)
-   위치
    -   /home/ksan/logs/metering.log

## 구동 환경

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

- 실행후 ksanMetering.tar 이미지 파일이 생성됩니다.

```shell
#!/bin/bash
docker build --rm -t infinistor/ksanmetering:latest -f DockerFile .
docker save -o ksanMetering.tar infinistor/ksanmetering
```

## How to Use

- KsanMon, KsanEdge, KsanPortal, MariaDB, KsanGw가 모두 동작하고 있어야 정상적으로 동작합니다.

#### 프로그램 설치
``` shell
# 빌드 했을 경우 이미지 로드
docker load -i /root/docker/ksanLifecycle.tar

# 컨테니어 생성
docker create -i -t \
--network=host \
-v /etc/localtime:/etc/localtime:ro \
-v /var/log/ksan:/app/logs \
-v /usr/local/ksan/etc:/usr/local/ksan \
--name ksanmetering pspace/ksanmetering:latest
```

### 실행(CLI)

``` shell
docker start ksanmetering
```

### 서비스 등록후 실행
``` shell
cp ksanmetering.service /etc/systemd/system/
systemctl enable ksanmetering
systemctl start ksanmetering
```