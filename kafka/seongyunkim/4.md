# 04. 카프카 프로듀서

프로듀서: 각각의 메세지를 토픽 파티션에 매핑하고 파티션의 리더에게 메시지 전송 (키를 입력하지 않으면 Round Robin. 버전 따라 다름)

## 1. 콘솔 프로듀서로 메세지 보내기

[https://kafka.apache.org/quickstart](https://kafka.apache.org/quickstart)

- 토픽 생성

```bash
$ bin/kafka-topics.sh --create \
--topic topic-name --bootstrap-server bootstrap-server:port \
--partition 1 --replication-factor 3
```

- 토픽 정보(리플리케이션, ISR, 리더, 팔로워) 확인

```bash
$ bin/kafka-topics.sh --describe \
--topic topic-name --bootstrap-server bootstrap-server:port
```

- 토픽 메세지 쓰기

```bash
$ bin/kafka-console-producer.sh \
--topic topic-name --bootstrap-server bootstrap-server:port
```

- 토픽 메세지 읽기

```bash
$ bin/kafka-console-consumer.sh \
--topic topic-name --bootstrap-server bootstrap-server:port
--partition 0 --from-beginning
```

- 참고: 책에 나오는 —broker-list, —zookeeper 옵션은 deprecated 됨
    - —broker-list: —bootstrap-server 로 대체됨
        - [What is Difference between broker-list and bootstrap-server?](https://stackoverflow.com/a/64112507)
    - —zookeeper: 0.10.0 버전부터 오프셋을 포함한 토픽 메타 데이터를 주키퍼가 아닌 카프카 브로커에서 관리
        - [KIP-500: Replace ZooKeeper with a Self-Managed Metadata Quorum](https://cwiki.apache.org/confluence/display/KAFKA/KIP-500%3A+Replace+ZooKeeper+with+a+Self-Managed+Metadata+Quorum)
        - [KIP-555: Deprecate Direct Zookeeper access in Kafka Administrative Tools](https://cwiki.apache.org/confluence/display/KAFKA/KIP-555%3A+Deprecate+Direct+Zookeeper+access+in+Kafka+Administrative+Tools)
    

## 2. 자바와 파이썬을 이용한 프로듀서

[https://kafka.apache.org/10/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html](https://kafka.apache.org/10/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html)

A Kafka client that publishes records to the Kafka cluster.

The producer is *thread safe* and sharing a single producer instance across threads will generally be faster than having multiple instances.

```java
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("acks", "all");
props.put("retries", 0);
props.put("batch.size", 16384);
props.put("linger.ms", 1);
props.put("buffer.memory", 33554432);
props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

Producer<String, String> producer = new KafkaProducer<>(props);
  for (int i = 0; i < 100; i++)
    // ProducerRecord 파라미터: topic, key, value
    producer.send(new ProducerRecord<String, String>("my-topic", Integer.toString(i), Integer.toString(i)));
```

- send()
    - `public java.util.concurrent.Future<RecordMetadata> send(ProducerRecord<K,V> record)`
    - 메세지를 버퍼에 저장하고 비동기 방식으로 별도의 스레드를 통해 브로커로 전송
    - 동기 전송: Future.get()을 통해 카프카 응답 대기
    - 비동기 전송: `org.apache.kafka.clients.producer.Callback` 구현
    

## 3. 프로듀서 활용 예제

- key 지정을 통해 특정 파티션으로만 메세지를 보낼 수 있음 (key 가 없으면 라운드 로빈 방식)
- 특정 파티션으로 메세지를 보내 메세지 순서 보장 가능
- 특정 파티션에 메세지가 몰려 균등하게 메세지 분배가 되지 않을 수도 있음
- 참고 1: key 로 부터 파티션을 계산하는 로직

  - [BuiltInPartitioner.java](https://github.com/apache/kafka/blob/6e8d0d9850b05fc1de0ceaf77834e68939f782c1/clients/src/main/java/org/apache/kafka/clients/producer/internals/BuiltInPartitioner.java#L327)

```java
/*
* Default hashing function to choose a partition from the serialized key bytes
*/
public static int partitionForKey(final byte[] serializedKey, final int numPartitions) {
  return Utils.toPositive(Utils.murmur2(serializedKey)) % numPartitions;
}
```

- 참고 2: 버전 별 key 미지정 시 파티션 지정 차이

  - [Partitionar when key=null](https://www.conduktor.io/kafka/producer-default-partitioner-and-sticky-partitioner/#Partitioner-when-key=null-1)

  - Round Robin: Kafka 2.3 이하
  - Sticky: Kafka 2.4 이상
    - `linger.ms` 가 경과하거나 `batch.size` 까지 배치가 가득찰 때 까지 고정(sticky) 파티션으로
    - 배치를 보낸 후 고정(sticky) 파티션 변경
    
    → 하나의 배치가 커지고 더 빨리 `batch.size` 에 도달할 수 있으므로 배치 전송까지 대기 시간이 줄어듬 (파티션 개수가 많을 수록 효과가 크다)
    

## 4. 프로듀서 주요 옵션

- `bootstrap.servers`
    - 카프카 클러스터에 처음 연결하기 위한 호스트와 포트 정보
    - 특정 브로커 보다는 전체 리스트를 기재하는 것이 좋음 (특정 호스트 장애 시 클라이언트는 다른 서버로 접속 시도)
- `acks`
    - 프로듀서가 리더에게 메세지를 보낸 후 요청을 완료하기 전 ack(승인) 수
    - 작으면 성능이 좋지만 메세지 손실 가능성이 있음
    - `acks=0`: 프로듀서는 서버로부터 어떠한 ack도 기다리지 않음. 메세지가 손실될 수 있지만 매우 빠름
    - `acks=1`: 리더는 데이터를 기록하지만, 모든 팔로워는 확인하지 않음. 일부 데이터 손실 가능
    - `acks=all`: 리더는 ISR의 팔로워로부터 데이터에 대한 ack를 기다림. 데이터 무손실 강력하게 보장
- `buffer.memory`
    - 배치 전송 시 프로듀서가 데이터를 보내기 위해 잠시 대기할 수 있는 메모리 바이트
- `compression.type`
    - 프로듀서가 데이터 전송 시 압축 포맷 (ex. none, gzip, snappy, lz4)
- `retries`
    - 일시적인 오류로 전송 실패 시 프로듀서가 다시 보내는 재시도 횟수
    - 중복 메세지 발생 가능
        - [Kafka - Idempotent Producer And Consumer](https://medium.com/@shesh.soft/kafka-idempotent-producer-and-consumer-25c52402ceb9)
- `batch.size`
    - 프로듀서는 같은 파티션으로 보내는 여러 데이터를 함께 배치로 보내려고 시도
    - 배치로 한번에 보낼 수 있는 최대 바이트
    - 배치를 보내기 전 장애가 발생하면 메세지는 전달되지 않음
- `linger.ms`
    - 프로듀서가 배치로 메세지를 전송하기 위해 기다리는 시간
    - 배치 사이즈에 도달하면 `linger.ms`설정과 무관하게 즉시 전송
    - 배치 사이즈에 도달하지 못하면 `linger.ms`설정에 도달했을 때 메세지 전송

## 5. 메세지 전송 방법

[Kafka 운영자가 말하는 Producer ACKS](https://www.popit.kr/kafka-%EC%9A%B4%EC%98%81%EC%9E%90%EA%B0%80-%EB%A7%90%ED%95%98%EB%8A%94-producer-acks/)

### 1) acks=0

- 메세지 손실 가능성은 높지만 빠른 전송 가능
- 프로듀서는 카프카 서버 응답을 기다리지 않고 메세지가 보낼 준비가 되면 즉시 다음 요청을 보냄
- 브로커 다운 등 장애 상황에서 손실 가능성이 높음

### 2) acks=1

- 메세지 손실 가능성은 적고 적당한 속도의 전송 가능
- 프로듀서는 메세지를 보내고 카프카가 잘 받았는지 확인
- 리더가 메세지를 저장해 acks를 보냈지만, 팔로워가 복제 전 리더에 장애가 나면 메세지 손실 가능
- 특별한 경우가 아니라면, 속도와 안정성을 확보할 수 있는 `acks=1` 추천

### 3) acks=all

- 전송 속도는 느리지만 메세지 손실이 없어야 하는 경우
- 리더 뿐만 아니라 ISR의 팔로워들까지 메세지가 잘 저장됐는지 확인
- 브로커 옵션 `min.insync.replicas`에 따라 저장을 확인하는 브로커 수가 달라짐
- `min.insync.replicas=1`
    - 리더에만 저장하므로 `acks=1` 과 동일하게 동작 (리더 장애 시 메세지 손실 가능)
- `min.insync.replicas=2`
    - acks를 보내기 전 최소 2개의 리플리케이션을 유지하는지 확인
    - 리더 장애로 신규 리더가 선출되어도 메세지 손실 발생하지 않음
    - 브로커 한 대 장애 발생해 두 대 만 있어도 운영 가능
    - 손실 없는 메세지 전송: acks=all, min.insync.replicas=2, replication factor=3
- `min.insync.replicas=3`
    - 브로커 한 대만 장애 발생해도 리플리케이션 수를 유지할 수 없어 ack 불가
    - 브로커 한 대 장애가 클러스터 장애로
