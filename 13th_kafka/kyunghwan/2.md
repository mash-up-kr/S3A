# 2장 카프카 설치

- 이번 장에서 배울 내용
 - 주키퍼와 카프카 설치
 - systemd 를 활용한 프로세스 관리 방법

## 2.1 카프카 관리를 위한 주키퍼

- 분산시스템의 여러가지 문제들 (인과성의 오류: ClockWall Time vs Logical Time)
- 분산 시스템의 오류를 잡아줄 코디네이팅 서버 필요 -> 주키퍼
- 카프카 서버 상태 정보는 znode라는 K-V Store에 저장된다 (k8s etcd?)
- 앙상블 홀수대 구성: Split Brain 문제
- IDC 레벨에서 스위치, 랙 등 설비 겹치지 않게 구성해야 한다.

## 2.2 주키퍼 설치

- 주키퍼 설치 의존성: JDK
- LSM Tree Based
- Transaction Log -> fsync and mmap
- 주키퍼 노드를 구분하기 위한 myid 파일 사용
- systemd : 재시작, 실행 순서 보장 등 프로세스 라이프사이클 관리 리눅스 기능
- 대표적으로 After, Restart, ExecStart 등이 있음.
- systemctl start zookeeper-server.service
- systemctl enable zookeeper=server.service

## 2.3 카프카 설치

- 카프카 브로커 다운 후 1) 노드 식별용 아이디, 2) 환경설정이 필요하다.
- 디스크 여러개인 경우 데이터 저장 디렉토리를 여러개 만들어 bandwidth 분산 가능
- 주키퍼 환경설정 시 주키퍼 모든 서버 입력하기.
- 주키퍼의 지노드 경로를 분리해서 주키퍼 앙상블 한 세트가 여러 클러스터를 관리할 수 있다곤 하는데....? 흠
- 카프카 환경설정 정보는 /usr/local/kafka/config/server.properties
- 주요 환경설정
 - 브로커 ID : broker.id
 - 최소 리플리케이션 팩터: min.insync.replicas
 - 토픽 삭제 기능 ON/OFF: delet.topic.enable
 - 로그 저장 위치: log.dirs
 - 카프카에서 허용하는 가장 큰 메세지 크기: message.max.bytes
 - 메시지가 디스크로 플러시 되기 전 메모리에 유지하는 시간: log.flush.interval.ms