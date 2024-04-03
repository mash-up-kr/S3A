# 2장 카프카 설치

- 현재는 주키퍼와 컨슈머 양쪽에 오프셋을 저장할 수 있으나 이제는 사라질 예정
- 주키퍼에는 카프카의 메타데이터 정보를 저장하고, 상태관리 목적으로 이용.

## 2.1 주키퍼
- 분산 애플리케이션을 위한 코디네이션 시스템
- 분산 애플리케이션이 안정적인 서비스를 할 수 있도록 정보를 중앙에서 관리하고, 구성 관리, 그룹관리 네이밍, 동기화 제공
- 주키퍼는 여러 서버가 클러스터 형태로 구성되고, 분산 애플리케이션들이 znode에 key-value형태로 상태정보를 저장하고, 각 애플리케이션이 이 데이터를 서로 주고받음
- 과반수를 위해 홀수로 서버를 구성

## 2.3 카프카 환경 설정
 카프카 주요 옵션
- broker.id: 브로커를 구분하기 위한 id
- delete.topic.enable: 토픽 삭제 기능을 on/off
- default.replication.factor
- min.insync.replicas: 최소 리플리케이션 팩터
- auto.create.topics.enable: 퍼블리셔가 메시지를 보냈을대 자동으로 토픽 생성
- num.partitions: 파티션 안주었을때 기본값
- log.retention.hours: 로그 보관 주기
- log.flush.interval.ms: 메시지가 디스크로 플러시되기 전 메모리에 유지하는 기간

## 로컬
1) 카프카 파일 다운

`https://www.apache.org/dyn/closer.cgi?path=/kafka/3.1.0/kafka_2.13-3.1.0.tgz`

2) 압축 해제

`tar xvf kafka_2.13-2.8.0.tgz`

3) 주키퍼 실행

`bin/zookeeper-server-start.sh config/zookeeper.properties`

4) 카프카 실행

`bin/kafka-server-start.sh config/server.properties`

5) jps로 카프카 실행 확인

<img width="240" alt="image" src="https://user-images.githubusercontent.com/46064193/226172048-73c332a6-57c5-44cb-a280-4711bb8df90f.png">

6) 카프카 토픽 생성

`bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic mashup`

7) 메시지 Consume

`bin/kafka-console-consumer.sh --topic mashup --from-beginning --bootstrap-server localhost:9092`

8) 메시지 Produce

`bin/kafka-console-producer.sh --topic mashup --bootstrap-server localhost:9092`

<img width="751" alt="image" src="https://user-images.githubusercontent.com/46064193/226181970-86a3c646-3d27-464f-9880-d1a994dcf8cc.png">
