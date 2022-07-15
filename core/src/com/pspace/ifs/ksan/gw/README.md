# GW (Ksan S3 gateway)

## 개요

### 용도
* AWS S3 호환 API를 처리

### 주요 AWS S3 호환 API ([S3 Compatible API](docs/s3-compatible-api.pdf))
* AbortMultipartUpload
* CompleteMultipartUpload
* CopyObject
* CreateBucket
* CreateMultipartUpload
* DeleteBucket
* DeleteBucketCors
* DeleteBucketEncryption
* DeleteBucketLifecycle
* DeleteBucketObjectLock
* DeleteBucketPolicy
* DeleteBucketReplication
* DeleteBucketTagging
* DeleteBucketWebsite
* DeleteObject
* DeleteObjects
* DeleteObjectTagging
* DeletePublicAccessBlock
* GetBucketAcl
* GetBucketCors
* GetBucketEncryption
* GetBucketLifecycleConfiguration
* GetBucketLocation
* GetBucketPolicy
* GetBucketPolicyStatus
* GetBucketReplication
* GetBucketTagging
* GetBucketVersioning
* GetBucketWebsite
* GetObject
* GetObjectAcl
* GetObjectLockConfiguration
* GetObjectRetention
* GetObjectTagging
* GetPublicAccessBlock
* HeadBucket
* HeadObject
* ListBuckets
* ListMultipartUploads
* ListObjects
* ListObjectsV2
* ListObjectVersions
* ListParts
* OptionsObject
* PutBucketAcl
* PutBucketCors
* PutBucketEncryption
* PutBucketLifecycleConfiguration
* PutBucketPolicy
* PutBucketReplication
* PutBucketTagging
* PutBucketVersioning
* PutBucketWebsite
* PutObject
* PutObjectAcl
* PutObjectLockConfiguration
* PutObjectTagging
* PutPublicAccessBlock

## 실행 예시(CLI)
```bash
java -jar -Dlogback.configurationFile=/usr/local/ksan/etc/ksan-gw-log.xml ksan-gw.jar &
```

## 로그 파일
* 설정 파일 : ksan-gw-log.xml (KSAN/etc/ksan-gw-log.xml)
* 위치
  * /var/log/ksan/s3gw/s3gw.log

## 구동 환경

* OS : CentOS Linux release 7.5 이상
* JDK : 11 이상

## How to Build

### Maven 설치
* Maven이 설치되어 있는지 확인해야 합니다.

* <kbd>mvn -v</kbd> 로 설치되어 있는지 확인하세요.

* 설치가 되어 있지 않으면 다음 명령어로 설치를 해야 합니다. <br> 
<kbd>sudo apt install maven</kbd>

### Build
* gw를 build하기 위해서 ksan-libs.jar, ksan-objmanager.jar가 필요합니다. 
```bash
cd ksan/core/src/com/pspace/ifs/ksan/libs
mvn clean package
mvn install
cd ksan/core/src/com/pspace/ifs/ksan/objmanager
mvn clean package
mvn install
```
* pom.xml 파일이 있는 위치(ksan/core/src/com/pspace/ifs/ksan/gw)에서 <kbd>mvn package</kbd> 명령어를 입력하시면 빌드가 되고, 빌드가 완료되면 target이라는 폴더에 ksan-gw.jar가 생성됩니다.
```bash
cd ksan/core/src/com/pspace/ifs/ksan/gw
mvn clean package
```

## How to Use (빌드한 경우)

* gw를 실행시키기 위하여 필요한 파일은 2개입니다.
```bash
ksan/core/src/com/pspace/ifs/ksan/gw/target/ksan-gw.jar // 소스 빌드 후, 생성된 실행 파일	
/usr/local/ksan/etc/ksan-gw-log.xml //log파일 관련 설정
``` 
* ksan-gw.jar를 /usr/local/ksan/bin 에 복사합니다.
* ksan-gw-log.xml를 /usr/local/ksan/etc 에 복사합니다.

* ksan-gw.jar의 실행 권한을 확인합니다.
 * ksan-gw.jar의 실행 권한이 없는 경우 실행권한을 부여합니다. <br>
 <kbd>chmod +x ksan-gw.jar</kbd>
 
* ksan-gw.jar를 실행합니다. (/usr/local/ksan/bin)
<kbd>java -jar -Dlogback.configurationFile=/usr/local/ksan/etc/ksan-gw-log.xml ksan-gw.jar &</kbd>
