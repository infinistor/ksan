# Lifecycle Manager

## 개요

### 용도
- 일정 시간마다 버킷의 수명주기 설정을 읽고, 해당 버킷에 반영하기 위한 프로그램

### 주요기능
- 버킷의 오브젝트를 수명주기 설정에 따라 삭제
- 버킷의 오브젝트 버전을 수명주기 설정에 따라 삭제
- 버킷의 삭제마커를 수명주기 설정에 따라 삭제

## 구동환경
- OS : Centos 7.5 이상
- JDK : 11 이상

## How to Build

### 설치환경 구축

#### Java 11 설치
- 버전 확인
``` shell
java -version
```

- 버전이 11 미만일 경우
``` bash
# 설치된 버전 확인
yum list java*jdk-devel
# 최신버전 설치
yum install java-11-openjdk -y
# 버전변경툴
/usr/sbin/alternatives --config java

 #3번 선택
  Selection    Command
-----------------------------------------------
*  1           java-1.8.0-openjdk.x86_64 (/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.262.b10-1.el7.x86_64/jre/bin/java)
   2           java-1.7.0-openjdk.x86_64 (/usr/lib/jvm/java-1.7.0-openjdk-1.7.0.261-2.6.22.2.el7_8.x86_64/jre/bin/java)
 + 3           java-11-openjdk.x86_64 (/usr/lib/jvm/java-11-openjdk-11.0.13.0.8-1.el7_9.x86_64/bin/java)

# 환경변수 설정
vim /etc/profile.d/java.sh
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.13.0.8-1.el7_9.x86_64
source /etc/profile.d/java.sh
```

#### Maven 3.6.3 설치

``` bash
# 바이너리 다운로드
wget https://downloads.apache.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz -P /tmp

# /opt 폴더에 압축 해제
tar -xzvf /tmp/apache-maven-3.6.3-bin.tar.gz -C /opt

# 소프트링크 생성
ln -s /opt/apache-maven-3.6.3 /opt/maven

# 환경셋업 스크립트 생성
vim /etc/profile.d/maven.sh
#JAVA 설정 되어있다면 무시
#export JAVA_HOME=/usr/lib/jvm/jre-openjdk
export M2_HOME=/opt/maven
export MAVEN_HOME=/opt/maven
export PATH=${M2_HOME}/bin:${PATH}

# 환경셋업 스크립트 실행
source /etc/profile.d/maven.sh

# Maven 버전 확인
mvn -version
Apache Maven 3.6.3 (cecedd343002696d0abb50b32b541b8a6ba2883f)
Maven home: /opt/maven
```

#### Build
``` shell
mvn clean package
```

## How to Use

### 설정

#### Lifecycle Manager 설정
- 파일명 : `ifs-lifecycleManager.conf`
- 경로 : `/usr/local/ksan/etc`

``` ini
[Global]
[DB]
Host = <Database IP>
Port = <Database Port>
User = <Database User Name>
Password = <Database Password>
DatabaseName = <Database Name>

[S3]
URL = http://<IP>:<Port>
AccessKey = <AccessKey>
SecretKey = <SecretKey>
```

#### 로그 설정
- 파일명 : `ifss-s3replicator.xml`
- 경로 : `/usr/local/ksan/etc`

``` xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="true" scanPeriod="30 seconds">
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <layout class="ch.qos.logback.classic.PatternLayout">
            <pattern>%-5level %d{yyyy-MM-dd HH:mm:ss.SSS} [%thread{5}][%logger{10}.%method:%line] : %msg%n</pattern>
        </layout>
    </appender>
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/var/log/backend/lifecycle.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>/var/log/backend/lifecycle.log.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
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

#### 실행예시(CLI)
``` bash
./ifs-lifecycleManager
```
