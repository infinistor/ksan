# OSD (Object-based Storage)

## 개요

### 용도
* s3gw의 요청을 받아 object에 대한 put, get, delete 수행

### 주요 기능
* s3gw put 요청을 받아 Local diisk에 object를 저장
* s3gw get 요청을 받아 Local disk에 있는 object를 전송
* s3gw delete 요청을 받아 Local disk에 있는 object를 삭제
* s3gw copy 요청을 받아 Local disk에 있는 object를 복사
* s3gw upload part 요청을 받아 Local disk에 part를 저장
* s3gw upload part copy 요청을 받아 Local disk에 있는 object를 복사하여 part로 저장
* s3gw complete multipart 요청을 받아 저장되어 있는 parts를 object로 저장
* s3gw abort multipart 요청을 받아 저장되어 있는 parts를 삭제

## 실행 예시(CLI)
``` shell
java -jar -Dlogback.configurationFile=/usr/local/ksan/etc/ksan-osd.xml ksanOsd.jar &
```

## 로그 파일
* 설정 파일 : ksanOsdLog.xml (KSAN/etc/ksanOsdLog.xml)
* 위치
  * /var/log/osd/osd.log

## 구동 환경

* OS : CentOS Linux release 7.5 이상
* JDK : 1.8 이상

## How to Build

### Maven 설치
* Maven이 설치되어 있는지 확인해야 합니다.

``` shell
mvn -v
```
* 위의 명령어로 확인하세요.

* 설치가 되어 있지 않으면 다음 명령어로 설치를 해야 합니다.
``` shell
sudo apt install maven
```

### Build

* pom.xml 파일이 있는 위치(KSAN/osd)에서 
``` shell
mvn package
```
* 위의 명령어를 실행하면 빌드가 되고, 빌드가 완료되면 target이라는 폴더에 ksanOsd.jar가 생성됩니다.

``` shell
mvn install
```

* 위의 명령어를 실해하면 local repository에 ksanOsd.jar가 저장됩니다. s3gw에서 참조합니다.

## How to Use (빌드한 경우)

* OSD를 실행시키기 위하여 필요한 파일은 4개입니다.
``` shell
  KSAN/osd/target/ksanOsd.jar // 소스 빌드 후, 생성된 실행 파일	
  KSAN/etc/ksanOsd.conf       // 설정 파일
  KSAN/etc/ksanOsdLog.xml     // log파일 관련 설정
  KSAN/etc/diskpools.xml      // disk pool 관련 설정
```
 
* ksanOsd.jar를 /usr/local/ksan/bin 에 복사합니다.
* ksanOsd.conf, ksanOsdLog.xml, diskpools.xml 를 /usr/local/ksan/etc 에 복사합니다.

* ksanOsd.jar의 실행 권한을 확인합니다.
* ksanOsd.jar의 실행 권한이 없는 경우 실행권한을 부여합니다.
``` shell
chmod +x ksanOsd.jar
```

* ksanOsd.jar를 실행합니다. (/usr/local/ksan/bin)
``` shell 
java -jar -Dlogback.configurationFile=/usr/local/ksan/etc/ksan-osd.xml ksanOsd.jar &
```
