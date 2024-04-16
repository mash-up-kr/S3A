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
- 카프카는 리더와 팔로워 간의 리플리케이션 동작을 처리할 때 서로의 통신을 최소화할 수 있도록 설계함으로써 리더의 부하를 줄였다.

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/6b197d5f-ed61-43df-9b30-dd0050fc5e1f" />

🔼 리더와 팔로워의 리플리케이션 과정(1)
- 리더만이 0번 오프셋에 message1이라는 메시지를 갖고 있는 상태
- 리플리케이션 동작 전

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/2e7b3a3f-8c34-462b-8ad4-b33466c22677" />

🔼 리더와 팔로워의 리플리케이션 과정(2)
- 팔로워들은 리더에게 0번 오프셋 메시지 가져오기(fetch) 요청을 보낸 후 새로운 메시지 message1이 있다는 사실을 인지하고 리플리케이션하는 과정
- 리더는 모든 팔로워가 리플리케이션하기 위한 요청을 보낸 사실을 알고 있음

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/ac49243d-b895-4569-8747-d8031253d876" />

🔼 리더와 팔로워의 리플리케이션 과정(3)
- 리더는 1번 오프셋 위치에 2번째 새로운 메시지인 "message2"를 프로듀서로부터 받은 뒤 저장한다.
- 팔로워들은 0번 오프셋에 대한 리플리케이션 동작 후, 1번 오프셋에 대한 리플리케이션을 요청한다.
- 리더는 1번 오프셋에 대한 리플리케이션 요청을 받았을 때, 0번 오프셋에 대한 리플리케이션 동작이 성공했음을 인지하고, 오프셋 0에 대해 커밋 표시를 한 후 **하이워터마크를 증가**시킨다.
- 팔로워들로부터 1번 오프셋 메시지에 대한 리플리케이션 요청을 받은 리더는 응답에 0번 오프셋 message1 메시지가 커밋되었다는 내용도 함께 전달한다.

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/51c1b23c-d11e-4e6c-b5c1-e74c6a1c0512" />

🔼 리더와 팔로워의 리플리케이션 과정(4)
- 리더의 응답을 받은 모든 팔로워는 0번 오프셋 메시지가 커밋되었다는 사실을 인지한다. (리더와 동일하게 커밋 표시)
- 1번 오프셋 메시지인 message2를 리플리케이션한다.
- 이러한 과정들을 통해 파티션 내 리더와 팔로워 간 메시지의 최신 상태를 유지하게 된다.

<br/>

- 카프카는 리플리케이션 동작에서 ACK 통신 단계를 제거했다.
- 리더는 메시지를 주고받는 기능에 더욱 집중할 수 있다.
- 팔로워들이 풀(pull)하는 방식으로 채택한 이유도 리플리케이션 동작에서 리더의 부하를 줄여주기 위함이다.

<br/>

### 4.1.5 리더에포크(LeaderEpoch)와 복구
- 리더에포크: 카프카의 파티션들이 복구 동작을 할 때 메시지의 일관성을 유지하기 위한 용도로 이용된다.
- 리더에포크 정보는 리플리케이션 프로토콜에 의해 전파되고, 새로운 리더가 변경된 후 변경된 리더에 대한 정보는 팔로워에게 전달된다.
- 리더에포크는 복구 동작 시 하이워터마크를 대체하는 수단으로도 활용된다.

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/16e63b23-f938-49e7-96d9-e713592100f5" />

🔼 리더에포크를 사용하지 않은 장애 복구 과정
- 파티션 수는 1, 리플리케이션 팩터 수는 2, min.insync.replicas는 1이다.
1. 리더는 프로듀서로부터 message1 메시지를 받았고, 0번 오프셋에 저장, 팔로워는 리더에게 0번 오프셋에 대한 가져오기 요청을 한다.
2. 가져오기 요청을 통해 팔로워는 message1 메시지를 리더로부터 리플리케이션한다.
3. 리더는 하이워터마크를 1로 올린다.
4. 리더는 프로듀서로부터 다음 메시지인 message2를 받은 뒤 1번 오프셋에 저장한다.
5. 팔로워는 다음 메시지인 message2에 대해 리더에게 가져오기 요청을 보내고, 응답으로 리더의 하이워터마크 변화를 감지하고 자신의 하이워터마크도 1로 올린다.
6. 팔로워는 1번 오프셋의 message2 메시지를 리더로부터 리플리케이션한다.
7. 팔로워는 2번 오프셋에 대한 요청을 리더에게 보내고, 요청을 받은 리더는 하이워터마크를 2로 올린다.
8. 팔로워는 1번 오프셋인 message2 메시지까지 리플리케이션을 완료했지만, 아직 리더로부터 하이워터마크를 2로 올리는 내용은 전달받지 못한 상태이다.
9. 예상하지 못한 장애로 팔로워가 다운된다.

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/3d34c524-a76c-421c-9e86-4d7ef33a528f" />

🔼 장애에서 복구된 팔로워의 상태 (리더에포크 사용하지 않음)
- 장애가 발생한 팔로워가 종료된 후 장애 처리가 완료된 상태 (내부적으로 메시지 복구 동작 수행)
1. 팔로워는 자신이 갖고 있는 메시지들 중에서 자신의 워터마크보다 높은 메시지들은 신뢰할 수 없는 메시지로 판단하고 삭제한다. (1번 오프셋의 message2 삭제)
2. 팔로워는 리더에게 1번 오프셋의 새로운 메시지에 대한 가져오기 요청을 한다.
3. 이 순간 리더였던 브로커가 예상치 못한 장애로 다운되면서, 해당 파티션에 유일하게 남아 있던 팔로워가 새로운 리더로 승격된다.

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/ea95dd9e-5a65-4f10-8b07-12888dc0290c" />

🔼 팔로워가 새로운 리더로 승격된 후의 상태 (리더에포크 사용하지 않음)
- 팔로워가 새로운 리더로 승격된 후의 상태
- 기존의 리더는 1번 오프셋의 message2를 갖고 있었지만, 팔로워는 message2 없이 새로운 리더로 승격된다.
- 최종적으로 1번 오프셋의 message2 메시지는 손실됐다.

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/e5d2c4df-7065-4d4c-a519-838b8ef1d196" />

🔼 장애에서 복구된 팔로워의 상태 (리더에포크 사용)
- 팔로워가 장애로 종료된 후 막 복구된 상태 이후의 과정
- 리더에포크를 사용할 경우, 리더에게 리더에포크 요청을 보낸다.
1. 팔로워는 복구 동작을 하면서 리더에게 리더에포크 요청을 보낸다.
2. 요청을 받은 리더는 리더에포크의 응답으로 "1번 오프셋의 message2까지"라고 팔로워에게 보낸다.
3. 팔로워는 자신의 하이워터마크보다 높은 1번 오프셋의 message2를 삭제하지 않고, 리더의 응답을 확인한 후 message2까지 자신의 하이워터마크를 상향 조정한다.

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/aada4780-f6a7-4ac9-8dba-99fbff34c68c" />

🔼 팔로워가 새로운 리더로 승격된 후의 상태 (리더에포크 사용)
- 리더가 예상치 못한 장애로 다운되면서 팔로워가 새로운 리더로 승격된 후의 상태
- 삭제 동작에 앞서, 리더에포크 요청/응답 과정을 통해 팔로워의 하이워터마크를 올림으로써 메시지 손실은 발생하지 않는다.

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/9a99fd83-1952-44a7-a7ae-63d50d991bc2" />

🔼 리더와 팔로워 종료 직전 상태 (리더에포크 사용하지 않음)
- 리더만 오프셋1까지 저장했고, 팔로워는 아직 1번 오프셋 메시지에 대해 리플리케이션 동작을 완료하지 못한 상태
- 현 시점에서 해당 브로커들의 장애가 발생해 리더와 팔로워 모두 다운됐다고 가정한다.

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/f8c0f7d9-beef-40e6-b386-43fbe6a53ca5" />

🔼 팔로워의 장애 복구 과정 (리더에포크 사용하지 않음)
- 브로커가 모두 종료된 후 팔로워가 있던 브로커만 장애에서 복구된 상태
1. 팔로워였던 브로커가 장애에서 먼저 복구된다.
2. peter-test01 토픽의 0번 파티션에 리더가 없으므로 팔로워는 새로운 리더로 승격된다.
3. 새로운 리더는 프로듀서로부터 다음 메시지 message3을 전달받고 1번 오프셋에 저장한 후, 자신의 하이워터마크를 상향 조정한다.

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/23aa1605-2395-41f1-a319-bfdab11b1658" />

🔼 구 리더의 장애 복구 과정 (리더에포크 사용하지 않음)

1. 구 리더였던 브로커가 장애에서 복구된다.
2. peter-test01 토픽의 0번 파티션에 이미 리더가 있으므로, 복구된 브로커는 팔로워가 된다.
3. 리더와 메시지 정합성 확인을 위해 자신의 하이워터마크를 비교해보니 리더의 하이워터마크와 일치하므로, 브로커는 자신이 갖고 있던 메시지를 삭제하지 않는다. (메시지 불일치)
4. 리더는 프로듀서로부터 message4 메시지를 받은 후 오프셋2의 위치에 저장한다.
5. 팔로워는 오프셋2인 message4를 리플리케이션하기 위해 준비한다.

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/5e285bf7-9601-4a08-a469-da275215b95f" />

🔼 구 리더의 장애 복구 과정 (리더에포크 사용)
- 팔로워가 먼저 복구되어 뉴리더가 되었고, 구 리더였던 브로커가 장애에서 복구된 상태
- 뉴리더가 자신이 팔로워일 때의 하이워터마크와 뉴리더일 때의 하이워터마크를 알고 있다.
1. 구 리더였던 브로커가 장애에서 복구된다.
2. peter-test01 토픽의 0번 파티션에 이미 리더가 있고 자신은 팔로워가 된다.
3. 팔로워는 뉴리더에게 리더에포크 요청을 보낸다.
4. 뉴리더는 0번 오프셋까지 유효하다고 응답한다.
5. 팔로워는 메시지 일관성을 위해 로컬 파일에서 1번 오프셋인 message2를 삭제한다.
6. 팔로워는 리더로부터 1번 오프셋인 message3을 리플리케이션하기 위해 준비한다.

<br/>

```
$ /usr/local/kafka/bin/kafka-topics.sh --bootstrap-server peter-kafka01.foo.bar:9092 --create --topic peter-test02 --partitions 1 --replication-factor 2
```
🔼 토픽 생성
- 토픽 이름 peter-test02, 파티션 수 1, 리플리케이션 팩터 수 2

<br/>

```
$ /usr/local/kafka/bin/kafka-topics.sh --bootstrap-server peter-kafka01.foo.bar:9092 --topic peter-test02 --describe
```
🔼 토픽 상세보기

<br/>

```
Topic: peter-test02 PartitionCount: 1 ReplicationFactor:2 Configs: segment.bytes=1073741824
Topic: peter-test02 Partition: 0 Leader: 1 Replicas: 1,3 Isr: 1,3
```
🔼 출력 결과
- 리더는 1번 브로커이며, 팔로워는 3번 브로커

<br/>

```
$ cat /data/kafka-logs/peter-test02-0/leader-epoch-checkpoint
```
🔼 리더에포크 상태 확인
- 리더는 1번 브로커이므로 ssh를 이용해 peter-kafka01에 접속한다.
- 접속 후 리더에포크 상태를 확인하기 위해 명령어를 실행한다.

<br/>

```
0
1 # 현재의 리더에포크 번호
0 0 # 첫 번째 0은 리더에포크 번호, 두 번째 0은 최종 커밋 후 새로운 메시지를 전송받게 될 오프셋 번호
```
🔼 출력 결과

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/70753f60-75d1-4f15-b783-98238b1496fe" />

🔼 리더에포크 상태(1)
- 출력된 내용을 나타낸 그림

<br/>

```
$ /usr/local/kafka/bin/kafka-console-producer.sh --bootstrap-server peter-kafka01.foo.bar;9092 --topic peter-test02
> message1
```
🔼 메시지 전송
- 메시지 전송 후 다시 리더에포크 상태를 확인해보면 전과 달라진 것이 없다.

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/13fd97cb-0591-4f3d-9b35-98f3f2ce9890" />

🔼 리더에포크 상태(2)
- message1은 0번 파티션의 0번 오프셋에 저장됐고, 리플리케이션 동작으로 팔로워까지 저장됐다.
- 강제로 새로운 리더를 선출하도록 리더가 위치한 브로커1을 종료한다.
- 리더 브로커 1번이 종료되면, 팔로워였던 3번 브로커가 새로운 리더로 승격된다. (리더에포크 업데이트)

<br/>

```
0
2 # 현재의 리더에포크 번호 (리더가 변경될 때마다 증가)
0 0 # 첫 번째 0은 리더에포크 번호, 두 번째 0은 최종 커밋 후 새로운 메시지를 전송받게 될 오프셋 번호
1 1 # 첫 번째 1은 리더에포크 번호, 두 번째 1은 최종 커밋 후 새로운 메시지를 전송받게 될 오프셋 번호
```
🔼 변경된 리더에포크 상태 출력 결과

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/61f39e18-c69e-4684-abbc-62db3a28c354" />

🔼 리더에포크 상태(3)
- 리더 브로커 1번이 다운되면서 리더에포크 번호는 1에서 2로 1만큼 증가했다.
- 리더에포크 번호가 1이었을 때를 기준으로 가장 마지막에 커밋된 후 새로 메시지를 받게 될 오프셋 번호를 기록한다.
- 가장 마지막에 커밋된 오프셋 번호는 0이므로, 오프셋 번호 1을 leader-epoch-checkpoint 파일에 기록한다.
- 다운됐던 브로커는 파일에 기록된 정보를 이용해 복구 동작을 하게 된다. (뉴리더에게 1번 리더에포크에 대한 요청, 뉴리더는 준비된 오프셋 번호가 1이라는 응답)

<br/>

---

## 4.2 컨트롤러
- 리더 선출을 맡고 있는 컨트롤러를 알아본다.
- 카프카 클러스터 중 하나의 브로커가 컨트롤러 역할을 하게 되며, 파티션의 ISR 리스트 중에서 리더를 선출한다.
- 가용성 보장을 위해 ISR 리스트 정보는 주키퍼에 저장되어 있다.
- 클라이언트들이 재시도하는 시간 내에 리더 선출 작업이 빠르게 이뤄져야 한다.

<br/>

|토픽 이름|peter-test02|
|---|---|
|파티션 수|1|
|리플리케이션 팩터 수|2|
|브로커 배치|1, 3번 브로커|
|현재 리더 위치|1번 브로커|

🔼 peter-test02 토픽 정보

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/88611a79-d8ea-4aef-a696-48095b130313" />

🔼 예기치 않은 장애로 인한 리더 선출 과정
1. 파티션 0번의 리더가 있는 브로커 1번이 예기치 않게 다운된다.
2. 주키퍼는 1번 브로커와 연결이 끊어진 후, 0번 파티션의 ISR에서 변화가 생겼음을 감지한다.
3. 컨트롤러는 주키퍼 워치를 통해 0번 파티션에 변화가 생긴 것을 감지하고, 해당 파티션 ISR 중 3번을 새로운 리더로 선출한다.
4. 컨트롤러는 0번 파티션의 새로운 리더가 3이라는 정보를 주키퍼에 기록한다.
5. 이렇게 갱신된 정보는 현재 활성화 상태인 모든 브로커에게 전파된다.

<br/>

- 파티션이 하나인 경우 약 0.2초만에 완료된다.
- 👎 하지만 파티션이 1만개인 경우, 약 30분이 걸린다.
- 버전 1.1.0에서 리더 선출 작업 속도를 보완하여 위 문제를 해결했다.
  - 불필요한 로깅 제거
  - 주키퍼 비동기 API 반영
  - 약 3초만에 작업 완료
 
<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/9c37bc5c-2dc5-4082-b272-a134e06d804c" />

🔼 제어된 종료 과정
1. 관리자가 브로커 종료 명렁어를 실행하고, SIG_TERM 신호가 브로커에게 전달된다.
2. SIG_TERM 신호를 받은 브로커는 컨트롤러에게 알린다.
3. 컨트롤러는 리더 선출 작업을 진행하고, 해당 정보를 주키퍼에 기록한다.
4. 컨트롤러는 새로운 리더 정보를 다른 브로커들에게 전송한다.
5. 컨트롤러는 종료 요청을 보낸 브로커에게 정상 종료한다는 응답을 보낸다.
6. 응답을 받은 브로커는 캐시에 있는 내용을 디스크에 저장하고 종료한다.

<br/>

- 제어된 종료는 급작스러운 종료에 비해 다운타임(downtime)이 최소화된다.
- 로그 복구 시간도 더 짧다.

<br/>

---

## 4.3 로그(로그 세그먼트)
- 카프카의 토픽으로 들어오는 메시지(레코드)는 세그먼트(segment)라는 파일에 저장된다.
- 로그 세그먼트 파일들은 브로커의 로컬 디스크에 보관된다.
- 최대 크기는 1GB이고, 그 크기를 넘어가면 롤링(rolling) 작업을 수행한다.

<br/>

### 4.3.1 로그 세그먼트 삭제
- 기본 값

<br/>

```
$ /usr/local/kafka/bin/kafka-topics.sh --bootstrap-server peter-kafka01.foo.bar:9092 --create --topic peter-test03 --partitions 1 --replication-factor 3
```
🔼 토픽 생성
- 이름은 peter-test03, 파티션 1, 리플리케이션 팩터 3으로 구성

<br/>

```
$ /usr/local/kafka/bin/kafka-console-producer.sh --bootstrap-server peter-kafka01.foo.bar:9092 --topic peter-test03
> log1
```
🔼 프로듀서에서 메시지 전송
- log1 메시지 전송

<br/>

```
$ /usr/local/kafka/bin/kafka-console-consumer.sh --bootstrap-server peter-kafak01.foo.bar:9092 --topic peter-test03 --from-beginning
```
🔼 컨슈머에서 메시지 가져오기
- log1 메시지를 가져온다. (출력됨)

<br/>

```
$ /usr/local/kafka/bin/kafka-configs.sh --bootstrap-server peter-kafka01.foo.bar:9092 --topic peter-test03 --add-config retention.ms=0 --alter
```
🔼 설정 추가
- `retention.ms=0`이라는 설정을 추가한다.
- 로그 세그먼트 보관 시간이 해당 숫자보다 크면 세그먼트를 삭제한다.
- 토픽 상세 정보 조회 명령어를 실행하면, Configs의 retention.ms=0이 추가되어 있는 것을 확인할 수 있다.
- 기본값 5분 간격으로 로그 세그먼트 파일을 체크하면서 삭제 작업이 수행된다.

<br/>

**로그 세그먼트 파일명이 생성되는 규칙**
- 0001.log 파일명에서 1은 오프셋 번호를 의미한다.
- 로그 세그먼트 파일을 생성할 때 오프셋 시작 번호를 이용해 파일 이름을 생성하는 규칙을 따른다.

<br/>

```
$ /usr/local/kafka/bin/kafka-configs.sh --bootstrap-server peter-kafka01.foo.bar:9092 --topic peter-test03 --delete-config retention.ms --alter
```
🔼 설정 삭제
- `retention.ms` 설정이 삭제된다.

<br/>

### 4.3.2 로그 세그먼트 컴팩션
- 컴팩션(compaciton): 카프카에서 제공하는 로그 세그먼트 관리 정책 중 하나로, 로그를 삭제하지 않고 컴팩션하여 보관할 수 있다.
- 기본적으로 로컬 디스크에 저장되어 있는 세그먼트를 대상으로 실행되는데, 현재 활성화된 세그먼트는 제외하고 나머지 세그먼트들을 대상으로 컴팩션이 실행된다.
- 하지만 카프카에서는 메시지 컴팩션 보관보다 좀 더 효율적인 방법으로 컴팩션한다.
- 메시지 키값을 기준으로 과거 정보는 중요하지 않고 가장 마지막 값이 필요한 경우에 사용한다.
- 카프카로 메시지를 전송할 때 키도 필숫값으로 전송해야 한다.

<br/>

> ***_consumer_offset 토픽***

- _consumer_offset 토픽: 카프카의 내부 토픽, 컨슈머 그룹의 정보를 저장하는 토픽
- 키(컨슈머 그룹명, 토픽명)와 밸류(오프셋 커밋 정보) 형태로 메시지가 저장된다.

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/a045d9b6-2350-454b-a96c-5138cdab067f" />

🔼 로그 컴팩션 과정
- 컴팩션 전 로그에서 키값이 K1인 밸류들을 확인해보면, 오프셋 0일 때 V1, 오프셋 2일 때 V3, 오프셋 3일 때 V4로 확인된다. (마지막 밸류는 V4)
- 따라서 컴팩션 후 로그를 보면 K1 키의 밸류는 V4이다. (마지막 메시지만 로컬 디스크에 저장되고, 나머지는 삭제)
- 👍 빠른 장애 복구 가능
- 키값을 기준으로 최종값만 필요한 워크로드에 적용하는 것이 바람직하다.

<br/>

