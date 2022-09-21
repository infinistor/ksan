# OSD (Object-based Storage)

## 개요

### 용도
* ksanGW의 요청을 받아 object에 대한 put, get, delete 수행

### 주요 기능
* ksanGW put 요청을 받아 Local diisk에 object를 저장
* ksanGW get 요청을 받아 Local disk에 있는 object를 전송
* ksanGW delete 요청을 받아 Local disk에 있는 object를 삭제
* ksanGW copy 요청을 받아 Local disk에 있는 object를 복사
* ksanGW upload part 요청을 받아 Local disk에 part를 저장
* ksanGW upload part copy 요청을 받아 Local disk에 있는 object를 복사하여 part로 저장
* ksanGW complete multipart 요청을 받아 저장되어 있는 parts를 object로 저장
* ksanGW abort multipart 요청을 받아 저장되어 있는 parts를 삭제

## 실행 예시(CLI)
```bash
java -jar -Dlogback.configurationFile=/usr/local/ksan/etc/ksanOSD_log_conf.xml ksanOSD.jar &
```

## 로그 파일
* 설정 파일 : ksan-osd-log.xml (/usr/local/ksan/etc/ksanOSD_log_conf.xml)
* 로그 파일 위치
  * /var/log/ksan/osd/osd.log

## 구동 환경

* OS : CentOS Linux release 7.5 이상
* JDK : 1.8 이상

## How to Build

### Maven 설치
* Maven이 설치되어 있는지 확인해야 합니다.

* <kbd>mvn -v</kbd> 로 설치되어 있는지 확인하세요.

* 설치가 되어 있지 않으면 다음 명령어로 설치를 해야 합니다. <br> 
<kbd>sudo apt install maven</kbd>

### Build
* OSD를 build하기 위해서 ksan-libs.jar가 필요합니다. 
```bash
cd ksan/core/src/com/pspace/ifs/ksan/libs
mvn clean package
mvn install
```
* pom.xml 파일이 있는 위치(ksan/core/src/com/pspace/ifs/ksan/osd)에서 <kbd>mvn package</kbd> 명령어를 입력하시면 빌드가 되고, 빌드가 완료되면 target이라는 폴더에 ksanOSD.jar가 생성됩니다.
```bash
cd ksan/core/src/com/pspace/ifs/ksan/osd
mvn clean package
```

## How to Use (빌드한 경우)

* OSD를 실행시키기 위하여 필요한 파일은 2개입니다.
```bash
ksan/core/src/com/pspace/ifs/ksan/osd/target/ksanOSD.jar // 소스 빌드 후, 생성된 실행 파일	
ksan/core/src/com/pspace/ifs/ksan/osd/ksanOSD_log_conf.xml // log 설정 파일
```

* ksanOSD.jar를 /usr/local/ksan/sbin 에 복사합니다.
* ksanOSD_log_conf.xml를 /usr/local/ksan/etc 에 복사합니다.

* ksanOSD.jar의 실행 권한을 확인합니다.
 * ksanOSD.jar의 실행 권한이 없는 경우 실행권한을 부여합니다. <br>
 <kbd>chmod +x ksanOSD.jar</kbd>
 
* ksanOSD.jar를 실행합니다. (/usr/local/ksan/sbin)
<kbd>java -jar -Dlogback.configurationFile=/usr/local/ksan/etc/ksanOSD_log_conf.xml ksanOSD.jar &</kbd>
