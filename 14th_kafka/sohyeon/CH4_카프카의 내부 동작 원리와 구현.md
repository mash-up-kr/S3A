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
- 카프카는 내부적으로 모두 동일한 리플리케이션들을 **리더와 팔로워**로 구분한다.
- 리더는 리플리케이션 중 하나가 선정된다.
- 모든 읽기/쓰기는 리더를 통해서만 가능하다.
- 컨슈머도 오직 리더로부터 메시지를 가져온다.

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/04c2b756-5445-409f-b5c7-c675380d96e6" />

🔼 리더와 팔로워의 관계
- peter-test01 토픽의 파티션 수는 1이고, 리플리케이션 팩터 수는 3이다.
- 프로듀서는 0번 파티션의 리더로 메시지를 보낸다.
- 컨슈머는 0번 파티션의 리더로부터 메시지를 가져온다.
- 팔로워들은 리더에 문제가 있을 경우를 대비해 언제든지 새로운 리더가 될 준비를 한다.
- 지속적으로 파티션의 리더가 새로운 메시지를 받았는지 확인하고, 새로운 메시지가 있다면 해당 메시지를 리더로부터 복제한다.

<br/>

### 4.1.3 복제 유지와 커밋
- 리더와 팔로워는 ISR(InSyncReplica)이라는 논리적인 그룹으로 묶여있다.
- 기본적으로 해당 그룹 안에 속한 팔로워들만이 새로운 리더의 자격을 가질 수 있다.
- ISR 내의 **팔로워**들은 리더와의 데이터 일치를 유지하기 위해 지속적으로 리더의 데이터를 따라가게 되고, **리더**는 모든 팔로워가 메시지를 받을 때까지 기다린다.
- 파티션의 리더는 팔로워들이 뒤처지지 않고 리플리케이션 동작을 잘하고 있는지 감시한다. (뒤처지지 않는 팔로워들만이 새로운 리더의 자격을 가질 수 있음)
- 팔로워가 특정 주기의 시간만큼 복제 요청을 하지 않는다면, 문제가 발생했다고 판단되고 ISR 그룹에서 추방당한다.
- ISR 내에서 모든 팔로워의 복제가 완료되면, 리더는 내부적으로 커밋되었다는 표시를 하게 된다.
- 마지막 커밋 오프셋 위치는 **하이워터마크(high water mark)** 라고 부른다.
- 커밋된 메시지만 컨슈머가 읽어갈 수 있다. (메시지의 일관성 유지)

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/eaf6ce6a-ae44-45d0-9cb5-979affcb71ca" />

🔼 커밋 메시지
- peter-test01 토픽을 표현 : 1개의 파티션과 3개의 리플리케이션 팩터로 설정
- 프로듀서가 "test message1"이라는 메시지를 토픽으로 보냈고, 모든 팔로워가 리플리케이션 동작을 통해 모두 저장하고 커밋까지 완료된 상태이다.
- 프로듀서가 "test message2"이라는 메시지를 토픽으로 보냈고, 리더만 저장했고, 팔로워들은 리플리케이션 동작 전이다.

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/8ff5085e-2b1c-4098-95b4-71c6c1aa0780" />

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/dfb3a991-790b-4e5e-bacc-a9449a9dfdc8" />

🔼 리더 선출 전후 컨슘된 메시지 확인
- 커밋되기 전 메시지를 컨슈머가 읽을 수 있다고 가정한다.
- 각기 다른 컨슈머가 메시지를 컨슘하는 동안 파티션의 리더 선출이 발생한 경우이다.
1. 컨슈머 A는 peter-test01 토픽을 컨슘한다.
2. 컨슈머 A는 peter-test01 토픽의 파티션 리더로부터 메시지를 읽어간다. 읽어간 메시지는 test message1, 2이다.
3. peter-test01 토픽의 파티션 리더가 있는 브로커에 문제가 발생해 팔로워 중 하나가 새로운 리더가 된다.
4. 프로듀서가 보낸 test message2 메시지는 아직 팔로워들에게 리플리케이션 되지 않은 상태에서 새로운 리더로 변경됐으므로, 새로운 리더는 test message1 메시지만 갖고 있다.
5. 새로운 컨슈머 B가 peter-test01 토픽을 컨슘한다.
6. 새로운 리더로부터 메시지를 읽어가고, 읽어간 메시지는 오직 test message1이다.

<br/>

```
$ cat /data/kafka-logs/replication-offset-checkpoint
```
🔼 replication-offset-checkpoint 파일 확인
- 커밋된 메시지를 유지하기 위해 로컬 디스크의 replication-offset-checkpoint라는 파일에 마지막 커밋 오프셋 위치를 저장한다.

<br/>

```
peter-test01 0 1
```
🔼 출력 결과
- peter-test01은 앞서 생성한 토픽 이름
- 0은 파티션 번호, 1은 커밋된 오프셋 번호
- "test message2" 메시지도 커밋되면 오프셋 번호는 1에서 2로 변경된다.

<br/>

### 4.1.4 리더와 팔로워의 단계별 리플리케이션 동작
