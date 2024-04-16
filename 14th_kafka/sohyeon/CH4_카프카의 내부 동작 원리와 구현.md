# 4장 카프카의 내부 동작 원리와 구현
- 카프카의 내부 동작을 알아본다.
- 리플리케이션 동작
- 리더와 팔로워의 역할
- 리더에포크와 복구 동작

<br/>

## 4.1 카프카 리플리케이션
- 카프카는 파이프라인의 정중앙에 위치하는 메인 허브 역할을 수행한다.
- 중앙 데이터 허브로서 안정적인 서비스가 운영될 수 있도록 구상되었다.
- 안정성을 확보하기 위해 리플리케이션이라는 동작이 수행된다.

<br/>

### 4.1.1 리플리케이션 동작 개요
- 리플리케이션 동작을 위해 토픽 생성 시 필숫값으로 replication-factor라는 옵션을 설정해야 한다.

<br/>

```shell
$ /usr/local/kafka/bin/kafka-topics.sh --bootstrap-server peter-kafka01.foo.bar:9092 --create --topic peter-test01 --partitions 1 --replication-factor 3
```
🔼 토픽 생성
- 파티션 수 1, 리플리케이션 팩터 수 3 설정

<br/>

```shell
$ /usr/local/kafka/bin/kafka-topics.sh --bootstrap-server peter-kafka01.foo.bar:9092 --topic peter-test01 --describe
```
🔼 토픽의 상세보기

<br/>

```
Topic: peter-test01 PartitionCount: 1 ReplicationFactor:3 Configs: segment.bytes=1073741824
Topic: peter-test01 Partition: 0 Leader: 1 Replicas: 1,2,3 Isr: 1,2,3
```
🔼 출력 결과
- 토픽의 파티션 수인 1과 리플리케이션 팩터 수인 3이 표시되어 있다.
- (2줄) 토픽의 파티션0에 대한 상세 내용
- 리더는 브로커 1, 리플리케이션들은 브로커 1, 2, 3에 있음을 나타냄
- 동기화되고 있는 리플리케이션들은 브로커 1, 2, 3
- 실제로 리플리케이션되는 것은 토픽을 구성하는 각각의 **파티션들**

<br/>

```
$ /usr/local/kafka/bin/kafka-console-producer.sh --bootsrap-server peter-kafka01.foo.bar:9092 --topic peter-test01
> test message1
```
🔼 메시지 전송
- "test message1"이라는 메시지를 peter-test01 토픽으로 전송한다.

<br/>

```
$ /usr/local/kafka/bin/kafka-dump-log.sh --print-data-log --files /data/kafka-logs/peter-test01-0/0000000000000000.log
```
🔼 세그먼트 파일 확인
- 해당 메시지가 세그먼트 파일에 저장되어 있는지 확인한다.

<br/>

```
Dumping /data/kafka-logs/peter-test01-0/0000000000000000.log
Starting offset: 0
baseOffset: 0 lastOffset: 0 count: 1 baseSequnece: -1 lastSequence: -1 produceId: -1 producerEpoch: -1 partitionLeaderEpoch: 0 isTransactional: false isControl: false position: 0 CreateTime: 1601008070323 size: 81 magic: 2 compresscodec: NONE src: 3417270022 isvalid: true
| offset: 0 CreateTime: 1601008070323 keysize: -1 valuesize: 13 sequence: -1 headerKeys: []payload: test message1
```
🔼 출력 결과(1)
- 시작 오프셋 위치는 0이다.
- 메시지 카운트는 1임을 알 수 있다.
- 프로듀서를 통해 보낸 메시지는 test message1임을 알 수 있다.

<br/>

- 콘솔 프로듀서로 보낸 메시지 하나를 총 3대의 브로커들이 모두 갖고 있다.
- 리플리케이션 팩터라는 옵션을 이용해 관리자가 지정한 수만큼의 리플리케이션을 가질 수 있다.
- N개의 리플리케이션이 있는 경우 N-1까지의 브로커 장애가 발생해도 메시지 손실 없이 안정적으로 메시지를 주고받을 수 있다.

<br/>

### 4.1.2 리더와 팔로워
