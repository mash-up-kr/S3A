# 카프카 버전 업그레이드와 확장

## 8.1 카프카 버전 업그레이드를 위한 준비

카프카 버전 업그레이드 전 본인 카프카 버전 확인

```shell
/usr/local/kafka/bin/kafka-topics.sh --version
```

### ex
~~~
2.6.0 (Commit:62a..)
~~~

### 업그레이드 종류
**메이저 업그레이드**: 1.x or 0.x -> 2.x
**마이너 업그레이드**: 2.0 -> 2.x

메이저 업그레이드는 메시지의 포맷 변경, 브로커에서의 기본값 변화, 과거에는 지원됐던 명령어의 지원 종료, 일부 JMX 메트릭의 변화 등 변경 가능성이 크므로 유의

업그레이드시 카프카는 다운타임을 가질 수도, 안가질 수도 있음

다운타임을 갖는 것은 어렵고, 영향을 최소하하여 롤링 업그레이드를 해야함

---

## 8.2 주키퍼 의존성이 있는 카프카 롤링 업그레이드

버전마다 옵션이 다르므로 유의해야함

2.1 -> 2.6 업그레이드시 다양한 옵션들이 바뀜

브로커 지정시에 사용하는 옵션 bootstrap-server

bootstrap-server 옵션은 2.1에서는 사용 불가능함

--broker-list를 사용

### 8.2.1 최신 버전의 카프카 다운로드와 설정

책에서는 2.1 -> 2.6으로 가정

2.1 kafka 디렉토리에 kafka_2.12-2.10으로 링크가 걸려있음
```shell
sudo cp kafka_2.12-2.1.0/config/server.properties kafka_2.12-2.6.0/config/server.properties
```

설정 파일을 2.12-2.60으로 복사

그 후, 2.60에 복사된 `server.properties`에 들어가 업그레이드 전 설정 파일에 아래와 같이 기입

```yaml
inter.broker.protocol.version=2.1
log.message.format.version=2.1
```

`브로커 간의 내부 통신은 2.1 버전 기반으로 통신하며` `메시지 포멧도 2.1을 유지한다`는 의미

브로커 설정 파일에 적용하지 않고 2.6 버전의 브로커를 실행하면 실행 중인 2.1 버전 브로커들과 통신이 불가능할 것

### 8.2.2 브로커 버전 업그레이드

브로커 버전 업그레이드는 한 대씩 순차적으로 진행됨

1. 첫번째 브로커를 접속한 후 종료함.
2. 브로커 한 대를 종료하면, 종료된 브로커가 갖고 있던 파티션의 리더들이 다른 브로커로 변경됨
3. 따라서 카프카 클러스터가 현재 운영 상태라면 클라이언트는 일시적으로 리더를 찾지 못하는 에러 or 타임아웃이 발생

### 장애 상황이 아닌, 당연한 상황.

4. 카프카 클라이언트 내부적으로 재시도 로직이 있어 모든 클라이언트는 변경된 새로운 리더가 있는 브로커를 바라봄
5. 브로커 한 대를 종료해도, 카프카가 기본적으로 제공하는 리플리케이션 기능으로 서비스는 장애 없이 모두 정상 작동함

```shell
# kafka 종료
cd /usr/local/
sudo systemctl stop kafka-server
```

첫번째 브로커가 종료됐으니 2.1 버전으로 연결되어 있는 kafka 심볼릭 링크를 2.6 버전으로 변경

```shell
# 링크를 2.6 버전으로 변경
sudo rm -rf kafka
sudo ln -sf kafka_2.12-2.6.0 kafka
```

### 현재 상태
![1_현상태.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch8/1_현상태.png)

첫번째 브로커가 2.6으로 업그레이드 되어 bootstrap-server 옵션이 잘 동작함

위와 같은 방법으로 2번, 3번 브로커도 반복

1. 2번 브로커 접속
2. /usr/local 경로로 이동
3. 브로커 종료
4. kalka 링크 삭제
5. kafka 링크 2.6 버전으로 재생성
6. 설정 파일 복사 및 옵션 설정
7. 브로커 시작

### 세 브로커 모두 버전 업 된 상태 
![2_현상태.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch8/2_현상태.png)

카프카 클러스터는 2.6으로 올라갔지만, 브로커 프로토콜 보전과 메시지 포멧 버전은 여전히 2.1이므로 이들을 업그레이드 해야함

### 8.2.3 브로커 설정 변경

2.1 버전을 제거해서 브로커들이 2.6 버전으로 통신하도록 변경

2.1 대신 2.6으로 직접 정의해도 되지만 따로 명시하지 않으면 기본값이 적용되므로 삭제하는 것을 추천

재시작해야만 설정이 반영됨. 브로커들을 순차적으로 재시작

```shell
sudo systemctl restart kafka-server
```

### 잘 가져오는지 테스트
#### 메시지 전송

```shell
/usr/local/kafka/bin/kafka-console-producer.sh --bootstrap-server peter-kafka01.foo.bar:9092 --topic peter-version2-1
> version2-1-message4
> version2-1-message5
```

그 후 8.2절 주키퍼 의존성이 있는 카프카 롤링 업그레이드에서 사용한 컨슈머 그룹 아이디 `peter-consumer`로 메시지를 가져오는 테스트

```shell
/usr/local/kafka/bin/kafka-console-consumer.sh --bootstrap-server peter-kafka01.foo.bar:9092 --topic peter-version2-1 --group peter-consumer
```

#### 정상 출력
~~~
version2-1-message4
version2-1-message5
~~~

#### 업그레이드 하기 전의 메시지를 잘 갖고 올까?
```shell
/usr/local/kafka/bin/kafka-console-consumer.sh --bootstrap-server peter-kafka01.foo.bar:9092 --topic peter-version2-1 --from-beginning
```

#### 정상출력

~~~
version2-1-message1
version2-1-message2
version2-1-message3
version2-1-message4
version2-1-message5
~~~

### 8.2.4 업그레이드 작업 시 주의사항

이 전과 같이 간단하게 업그레이드가 가능한 것을 알았지만 운영에선 예상치 못한 문제를 자주 맞닥뜨림

업그레이드 하기 전 운영 환경과 동일한 카프카 버전으로 개발용 카프카를 구성해보고 개발용 카프카를 업그레이드르 해라~

사용량이 적을 때 업그레이드 해라~

프로듀서의 ack=1 옵션 사용시 카프카의 롤링 재시작으로 인해 극히 드물게 메시지가 손실 될 수 있다~

## 8.3 카프카의 확장

카프카는 사용량이 폭발적으로 증가해도 손쉽게 확장할 수 있도록 디자인됨

### 토픽 생성으로 재현

```shell
/usr/local/kafka/bin/kafka-topics.sh --bootstrap-server peter-kafka01.foo.bar:9092 --create --topic peter-scaleout1 --partitions 4 --replication-factor 1
```

```shell
/usr/local/kafka/bin/kafka-topics.sh --bootstrap-server peter-kafka01.foo.bar:9092 --describe --topic peter-scaleout1
```

출력
~~~
Topic: peter-scaleout1   PartitionCount: 4    ReplicationFactor: 1    Configs: segment.
bytes=1073741824
Topic: peter-scaleout1     Partition: 0       Leader: 3       Replicas: 3   Isr: 3
Topic: peter-scaleout1     Partition: 1       Leader: 1       Replicas: 1   Isr: 1
Topic: peter-scaleout1     Partition: 2       Leader: 2       Replicas: 2   Isr: 2
Topic: peter-scaleout1     Partition: 3       Leader: 3       Replicas: 3   Isr: 3
~~~

![3_토픽배치.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch8/3_토픽배치.png)

카프카 확장 실습을 위해 브로커를 한 대 더 추가

주키퍼 서버 중 하나인 peter-zk03 서버를 이용

설치 후 ㅂ로커 설정 파일을 편집

```shell
sudo vi /usr/local/kafka/config/server.properties
```

이미 각 broker.id는 1, 2, 3으로 설정되어 있기 때문에 고유한 broker.id=4가 올바르게 지정되어있는지 확인

새로운 브로커가 추가됐지만, 분산작업을 하지 않은 상태

### 새로운 브로커가 추가된 상태
![4_브로커추가상태.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch8/4_브로커추가상태.png)

파티션별 브로커 위치를 확인하기 위해 토픽을 추가로 하나 더 생성

```shell
/usr/local/kafka/bin/kafka-topics.sh --bootstrap-server peter-kafka01.foo.bar:9092 --create --topic peter-scaleout2 --partitions 4 --replication-factor 1
```

출력
~~~
Created topic peter-scaleout2.
~~~

어느브로커에 있는지 위치 확인

```shell
/usr/local/kafka/bin/kafka-topics.sh --bootstrap-server peter-kafka01.foo.bar:9092 --describe --topic peter-scaleout2
```

출력
~~~
Topic: peter-scaleout2   PartitionCount: 4    ReplicationFactor: 1    Configs: segment.
bytes=1073741824
Topic: peter-scaleout2     Partition: 0       Leader: 1       Replicas: 1   Isr: 3
Topic: peter-scaleout2     Partition: 1       Leader: 2       Replicas: 2   Isr: 1
Topic: peter-scaleout2     Partition: 2       Leader: 3       Replicas: 3   Isr: 2
Topic: peter-scaleout2     Partition: 3       Leader: 4       Replicas: 4   Isr: 3
~~~

파티션 수는 총 4개로 새롭게 확장한 브로커 4번을 포함해 브로커 ID당 하나씩 고르게 분포되어있음

### peter-scaleout2 토픽을 추가한 후의 상태

### 새로운 브로커에 토픽이 추가된 상태

![5_토픽추가후상태.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch8/5_토픽추가후상태.png)

기존의 파티션은 유지되고, 추가된 토픽만 분산하고 있음. 이 전의 것들을 분산하지 않으면 의미 없음

### 8.3.1 브로커 부하 분산

새로 추가된 브로커를 비롯해 모든 브로커에게 균등하게 파티션ㅇ르 분배해야함

카프카에서 제공하는 kafka-reassign-partitions.sh 도구를 사용하여 파티션을 이동시킬 수 있음

파티션 이동 작업을 위해 정해진 JSON 포맷으로 파일을 생성

#### 하나의 토픽을 정의한 JSON 파일
```json
{
  "topics" : 
  [{"topic" :  "peter-scaleout1"}],
  "version": 1
}
```
하나의 토픽이라 토픽을 하나만 추가했지만, 분산시킬 토픽이 여러개라면 콤마(,)을 사용하여 토픽을 추가

#### 여러 개의 토픽을 정의한 JSON 파일
```json
{
  "topics" : 
  [{"topic" :  "peter-scaleout1"},{"topic" :  "peter-scaleout2"}], 
  "version": 1
}
```

분산시킬 대상 토픽에 대한 JSON 파일이 생성됐따면 `kafka-reassign-partitions.sh`를 사용해 파티션을 분산시킬 브로커 리스트를 지정

#### 1, 2, 3, 4 번 브로커 모두 지정
```shell
/usr/local/kafka/bin/kafka-reassign-partitions.sh
--bootstrap-server peter-kafka01.foo.bar:9092 --generate --topics-to-move-json-file
reassign-partitions-topic.json --broker-list "1,2,3,4"
```

#### 출력
~~~
Current partition replica assignment
{"version":1, "partitions":[{"topic":"peter-scaleout1", "partition":0, "replicas":[3],
"log_dirs" : ["any"1}, {"topic" :"peter-scaleout1", "partition":1, "replicas": [1],
"log_dirs" : ["any"]}, {"topic" :"peter-scaleoutI", "partition":2, "replicas": [2],
"log_dirs" : ["any"1}, {"topic":"peter-scaleout1", "partition":3, "replicas": [3],
"log_dirs" : ["any"1}]}

Proposed partition reassignment configuration {"version":1, "partitions": [{"topic": "peter-scaleout]", "partition":0, "replicas":[2],
"log_dirs" : ["any"1}, {"topic" :"peter-scaleout1", "partition":1, "replicas": [3],
"log_dirs": ["any"1}, {"topic":"peter-scaleout1", "partition":2, "replicas": [4],
"log_dirs": ["any"]},{"topic":"peter-scaleout1", "partition":3, "replicas": [1],
"log_dirs" :["any" ]}]}
~~~

#### 제안된 파티션 배치(move.json)

```json
{
  "version": 1,
  "partitions": [
    {
      "topic": "peter-scaleout1",
      "partition": 0,
      "replicas": [
        2
      ],
      "log_dirs": [
        "any"
      ]
    },
    {
      "topic": "peter-scaleout1",
      "partition": 1,
      "replicas": [
        3
      ],
      "log_dirs": [
        "any"
      ]
    },
    {
      "topic": "peter-scaleout1",
      "partition": 2,
      "replicas": [
        4
      ],
      "log_dirs": [
        "any"
      ]
    },
    {
      "topic": "peter-scaleout1",
      "partition": 3,
      "replicas": [
        1
      ],
      "log_dirs": [
        "any"
      ]
    },
  ]
}
```

kafka-reassign-partitions.sh 도구를 이용해 제안된 파티션 배치의 내용을 복사

move.json 파일을 생성했담녀 이제 peter-scaleout1 토픽에 대해 파티션 배치를 실행

그 후, `kafka-reassign-partitions.sh` 명령어와 `--reassignment-json-file` 옵션으로 `move.json`을 정의해 토픽에 대해 파티션 배치를 실행

#### 출력 - 재배치 성공
~~~
Current partition replica assignment

{"version" :1, "partitions": [{"topic": "peter-scaleout]", "partition":0, "replicas": [3],
"log_dirs": ["any"]}, {"topic": "peter-scaleout1", "partition":1, "replicas":[1],
"log_dirs": ["any"1}, {"topic":"peter-scaleout1", "partition":2, "replicas":[2],
"log_dirs": ["any"|}, {"topic": "peter-scaleout1", "partition":3, "replicas": [3],
"log_dirs": ["any" ]}]}

Save this to use as the --reassignment-json-file option during rollback
Successfully started partition reassignments for peter-scaleout1-0, peter-scaleout1-1, peter-scaleout1-2.peter-scaleout1-3
~~~

#### 4번 브로커까지 고르게 배치됐는가? 

```shell
/usr/local/kafka/bin/kafka-topics.sh --bootstrap-server
peter-kafka01.foo.bar:9092 --describe --topic peter-scaleout1
```

#### 출력 - 2번 파티션이 4번 브로커에 배치됨
~~~
Topic: peter-scaleout1   PartitionCount: 4    ReplicationFactor: 1    Configs: segment.
bytes=1073741824
Topic: peter-scaleout1     Partition: 0       Leader: 2       Replicas: 2   Isr: 2
Topic: peter-scaleout1     Partition: 1       Leader: 3       Replicas: 3   Isr: 3
Topic: peter-scaleout1     Partition: 2       Leader: 4       Replicas: 4   Isr: 4
Topic: peter-scaleout1     Partition: 3       Leader: 1       Replicas: 1   Isr: 1
~~~

### 파티션 재배치 이후 상태, 분산 재배치 완료
![6_파티션재배치이후상태.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch8/6_파티션재배치이후상태.png)

### 8.3.2 분산 배치 작업 시 주의사항

업그레이드와 마찬가지로 사용량이 낮을 때 진행해라~

카프카에서 파티션이 재배치되는 과정은 파티션이 단순히 이동하는 것이 아닌 브로커 내부적으로 리플리케이션하는 동작이 일어나고, 리플리케이션 후 삭제됨

#### 파티션 배치 작업
![7_파티션배치작업.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch8/7_파티션배치작업.png)