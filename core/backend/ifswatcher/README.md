# Watcher (S3GW Metering Backend)

## 개요

### 용도
* 로깅 데이터를 수집하여 metering 정보를 저장

### 주요 기능
* bucket별 filecount, 용량 저장
* bucket별 upload, download 스트림 크기 저장

### watcher.conf (KSAN/etc/watcher.conf)
``` shell
dbhost=                // s3 db host ip
dbs3=                  // s3 db name
dbport=                // s3 db port
dbuser=                // s3 db user
dbpass=                // s3 db password
```

## 실행 예시(CLI)
``` shell
java -jar -Dlogback.configurationFile=/usr/local/ksan/etc/ifs-watcher.xml ifs-watcher &
```

## 로그 파일
* 설정 파일 : ifs-watcher.xml (KSAN/etc/ifs-watcher.xml)
* 위치
  * /var/log/watcher/watcher.log

## 구동 환경

* OS : CentOS Linux release 7.5 이상
* JDK : 11 이상

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

* pom.xml 파일이 있는 위치(KSAN/core/watcher)에서 
``` shell
mvn package
```
* 위의 명령어를 실행하면 빌드가 되고, 빌드가 완료되면 target이라는 폴더에 ifs-watcher가 생성됩니다.

## How to Use (빌드한 경우)

* Watcher를 실행시키기 위하여 필요한 파일은 4개입니다.
 * KSAN/core/watcher/target/ifs-watcher - 소스 빌드 후, 생성된 실행 파일	
 * KSAN/etc/watcher.conf - 설정 파일
 * KSAN/etc/ifs-watcher.xml - log파일 관련 설정
 
* ifs-watcher를 /usr/local/ksan/bin 에 복사합니다.
* watcher.conf, ifs-watcher.xml 를 /usr/local/ksan/etc 에 복사합니다.

* ifs-watcher의 실행 권한을 확인합니다.
 * ifs-watcher의 실행 권한이 없는 경우 실행권한을 부여합니다. <br>
``` shell
chmod +x ifs-watcher
```

* ifs-watcher.jar를 실행합니다. (/usr/local/ksan/bin)
``` shell 
java -jar -Dlogback.configurationFile=/usr/local/ksan/etc/ifs-watcher.xml ifs-watcher &
```
