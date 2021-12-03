# KSAN : Software Defined Storage for Objects and Files

## Overview

KSAN은 대규모 오브젝트 스토리지 서비스를 안정적이며 효율적으로 제공하기 위해 설계된 소프트웨어 정의 오브젝트 스토리지 시스템입니다.

KSAN 시스템은 기본적으로 이중화된 Management Server(MGS)와 ksanNode 집합으로 구성됩니다.


## Features

### 확장 가능한 오브젝트 스토리지

KSAN 시스템은 스케일-아웃 형태의 확장을 지원합니다. 

관리자는 ksanNode를 KSAN 서비스의 중단없이 필요에 따라 추가할 수 있도록 설계되었습니다. 증설된 ksanNode의 OSD/OSDDISK를 기존 OSDDISK 그룹에 편입하면 즉시 스토리지 풀이 확장됩니다. 또한 증설된 ksanNode의 S3GW는 기존 오브젝트 스토리지 서비스 풀에 즉시 참여 가능하며 서비스 대역폭을 확장하는 역할을 합니다.


### 업계 표준의 다양한 오브젝트 스토리지 서비스 API 지원

KSAN은 오브젝트 스토리지 서비스 시장에서 가장 널리 사용되는 오브젝트 스토리지 서비스 API와 호환성을 제공하여 기존 클라우드 기반의 어플리케이션의 수정없이 사용할 수 있게 합니다.

KSAN은 AWS S3 호환 API를 기본적으로 제공하고 개발 로드맵에 따라 Microsoft Azure API 및 Google Cloud API를 순차적으로 제공할 예정입니다.

현재 KSAN에서 지원하는 AWS S3 호환 API는 이 문서([S3 Compatible API List](http://192.168.11.240:8080/books/ksan-0HA/page/s3-compatible-api-list))를 참조해 주십시오.

:information_source: Microsoft Azure API 및 Google Cloud API는 각각 2022년, 2023년에 단계적으로 지원할 예정입니다.

### 미션 크리티컬 서비스에 즉시 도입 가능한 고가용성을 지원

KSAN의 오브젝트 메타데이터 서브시스템에 배치되는 OIS-DB는 MariaDB와 같은 RDBMS와 mongoDB와 같은 NoSQL DB 등으로 구성할 수 있도록 설계되었으며, 각각의 지원 대상에 따라 최적화된 방식으로 DB 서비스의 고가용성을 지원합니다.

<p class="callout info"> mongoDB와 같은 NoSQL DB 지원은 로드맵에 따라 2022년 3분기 이후에 제공될 예정입니다. </p>

KSAN의 오브젝트 데이터 서브시스템을 구성하는 ODSS에서는 오브젝트 바이너리 데이터를 서로 다른 OSD의 OSDDISK에 복제본을 배치 및 저장하는 방식으로 단일 지점의 물리적인 장애에 대응하도록 설계되었습니다. 또한 ODSS의 S3GW는 HAProxy 등의 서비스 로드밸런서를 이용해 오브젝트 스토리지 서비스의 가용성을 보장합니다.

KSAN 시스템은 재해 상황에 대해서도 서비스 가용성을 보장할 수 있습니다. 2022년 3분기 이후에 공개 및 제공이 예정되어 있는 ksanDR 모듈은 지역적으로 배치되는 KSAN 시스템들 간의 오브젝트 데이터를 실시간으로 동기화하는 기능을 제공합니다.

<p class="callout info"> 재해 상황에 대한 서비스 고가용성 지원을 위한 ksanDR 기능은 2022년 3분기에 공개될 예정입니다. </p>


### 유연하고 효율적인 저장 자원의 관리 지원

이더넷으로 연결된 ODSS의 집합의 백엔드 저장 자원인 OSDDISK의 집합은 특별한 설정없이 모든 사용자가 동시에 사용할 수 있습니다. 특정 버켓의 오브젝트를 삭제하면 즉시 해당 백엔드 저장 자원은 반환되며, 다른 버켓의 오브젝트 저장에 활용할 수 있습니다.

또한 기본적인 1+1 복제 방식으로 OSDDISK에 분산 저장된 오브젝트 데이터는 특정 시간이 지나면 자동으로 Erasure Coding으로 처리해 더 적은 백엔드 저장 자원으로 오브젝트 데이터를 보관할 수 있도록 설계되었습니다.

<p class="callout info"> KSAN의 완전한 Erasure Coding 기능은 2022년 3분기에 공개될 예정입니다. </p>

<br><br>
