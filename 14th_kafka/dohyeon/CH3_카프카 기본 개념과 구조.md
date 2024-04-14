# Quiz



# 1. 카프카 기초 다지기

## 1.0 용어 정리

- ### `주키퍼`
  - 카프카의 메타데이터 관리/브로커의 상태 점검을 담당한다.
- ### `카프카/카프카 클러스터`
  - 여러 대의 브로커를 구성한 클러스터를 말한다.
- ### `브로커`
  - 카프카 애플리케이션이 설치된 서버/노드를 말한다.
- ### `프로듀서`
  - 카프카로 메세지를 보내는 클라이언트이다.
- ### `컨슈머`
  - 카프카에서 메세지를 꺼내는 클라이언트이다.
- ### `토픽`
  - 카프카는 메세지 피드들을 토픽으로 구분한다. 각 토픽의 이름은 카프카 내에서 고유하다.
- ### `파티션`
  - 병렬 처리를 위해 하나의 토픽을 여러 개로 나눈 것을 말한다.
- ### `세그먼트`
  - 프로듀서가 전송한 실제 메세지가 브로커의 로컬 디스크에 저장되는 파일을 말한다.
- ### `메세지, 레코드`
  - 프로듀서가 브로커로 전송하거나 컨슈머가 읽어가는 데이터 조각을 말한다.

---

## 1.1 리플리케이션

리플리케이션이란 각 메세제들을 여러 개로 복제해서 브로커들에게 분산시키는 동작이다. 이로인해 하나의 브로커가 다운되더라도 다른 브로커에 의해 정상적인 카프카 생태계를 유지할 수 있다.

`--replication-factor 3`이라는 옵션으로 리플리케이션 갯수를 지정할 수 있고 원본을 포함한 리플리케이션이 총 3개가 있다는 뜻이다.

리플리케이션은 토픽의 파티션이 리플리케이션되는 것이다. 따라서 리플리케이션 팩터 수가 커지면 안정성은 높아지지만 브로커의 리소스를 많이 사용하게 된다.

권장 팩터 수는 아래와 같다.

- 테스트나 개발 환경 : 1
- 운영 환경(로그성 메세지로 약간의 유실을 허용할 때) : 2
- 운영 환경 (유실을 허용하지 않을 때) : 3

---

## 1.2 파티션

하나의 토픽이 한 번에 처리할 수 있는 한계를 높이기 위해 토픽을 분할한 것을 파티션이라고 한다.

파티션만큼 컨슈머를 연결할 수 있고 파티션 당 컨슈머는 최대 1개이먀, 파티션 번호는 0부터 시작한다.

파티션을 늘린다는 것은 컨슈머가 많아지고 그만큼 처리량이 높아진다는 것인데 이 역시도 주의해야할 점이 있다.

파티션을 늘리는 것은 가능하지만 줄이는 것은 불가능하기 때문에 적절한 갯수로 운영해야한다. 따라서 처리량/컨슈머의 LAG등을 모니터링하면서 적절하게 갯수를 조절해야할 필요가 있다.

`LAG` = `프로듀서가 보낸 메세지 수(카프카에 남아 있는 메세지 수) - 컨슈머가 가져간 메세지 수`이다.

---

## 1.3 세그먼트

세그먼트는 파티션 단위로 영구 저장되는 데이터이다.

즉, 토픽 T에 파티션이 3개라면, `T-P0`, `T-P1`, `T-P2` 와 같이 `토픽-파티션 번호`의 꼴로 디렉터리가 만들어져 저장되게 된다.

![토픽, 파티션, 세그먼트 관계도](https://github.com/mash-up-kr/S3A/blob/master/14th_kafka/dohyeon/image/3_1.png?raw=true)

1. 프로듀서가 메세지를 발행한다.
2. 프로듀서에게 받은 메세지를 파티션 세그먼트 로그 파일에 저장한다.
3. 세그먼트 로그 파일에 저장된 메세지는 컨슈머가 읽어갈 수 있는 상태가 되었다.

---

# 2. 카프카의 핵심 개념 7가지

카프카가 높은 처리량, 빠른 응답 속도, 안정성이 높다고 한다. 그러면 왜 이런 장점들을 갖고 있는지 알아보자.

## 2.1 분산 시스템

카프카는 기본적으로 분산 시스템 기반이다. 즉, 최초로 구성한 클러스터의 리소스가 한계에 다다르면 브로커를 늘려 수평적으로 확장하여 문제를 해결할 수 있다.

쉽게 확장 가능하다는 점이 카프카의 큰 장점 중 하나이다.

## 2.2 페이지 캐시

높은 처리량을 달성하기 위해 `페이지 캐시`기능을 활용하고 있다. 직접 디스크에 읽고 쓰기보다 물리 메모리 중 잔여 메모리를 활용하여 디스크 I/O를 최소화한다.

[페이지 캐시 흐름](https://medium.com/sjk5766/kafka-disk-i-o가-빠른-이유-899c4da5084)
![페이지 캐시 흐름](https://miro.medium.com/v2/resize:fit:1400/format:webp/1*rjlyW4hhBaJGqVJfKu7dSw.png)

위 그림에 따르면 프로듀서와 컨슈머가 메세지를 R/W하는 장소가 페이지 캐시이다.

## 2.3 배치 전송 처리

카프카는 다량의 통신을 묶어서 단건으로 통신하여 네트워크 오버헤드를 줄이고자한다.

[배치 전송 옵션](https://magpienote.tistory.com/251)

- Kafka는 메세지를 보낼 때 ACK 로직을 타게 돼는데 메세지당 한 번의 ACK로직이 발생한다.
- Producer가 보내는 메세지의 량이 많아지게 돼면 메세지 처리 속도가 늦어져 Lag이 걸리는 latency가 발생한다.
- 이를 해결하기 위한 Producer의 방식은 Batch 처리를 이용한다. batch를 이용하면 메세지를 묶음 으로 보내기 때문에 replica처리 로직이 줄어들어 Latency를 방지 할 수 있다. 즉, 메세지 send 처리가 대기 줄일 수 있다.
- batch 설정 옵션은 `batch.size`와 `linger.ms`이 있다. 
  - `batch.size(default 16kb)` : size를 정의 하여 메세지의 용량이 size에 도달 할 때 까지 기다렸다가 보낸다.
  - `linger.ms(default 0)` : batch.size가 도달하지 않으면 메세지를 보내지 않기 때문에 마냥 기다릴 수는 없어 해당 시간을 설정하여 size가 도달하지 않더라도 시간이 초과하면 메세지를 보내게 된다.

## 2.4 압축 전송

`gzip`, `snappy`, `lz4`, `zstd` 등의 압축 타입을 지원한다. 압축 기법은 배치 전송처리와 결합하면 더 좋은 성능을 기대할 수 있는데

1개의 파일을 압축하는 것 보다 여러 개의 파일을 압축하는 것이 압축 효율이 좋기 때문이다.

압축 타입마다 특징을 갖고 있어서 `높은 압축률`이 필요한 경우 `gzip`, `zstd`를 권장하고 빠른 응답 속도가 필요하다면 `lz4`, `snappy`을 권장한다.

하지만 메세지 형식, 크기에 따라 다른 결과를 나타낼 수 있으니 직접 메세지를 테스트하는 것이 중요하다.

---

## 2.5 토픽, 파티션, 오프셋

카프카는 토픽이라는 곳에 데이터를 저장한다. 토픽은 병렬 처리를 위해 여러 개의 파티션이라는 단위로 분할할 수 있다.

카프카는 하나의 토픽이여도 여러 개의 파티션을 배치하여 높은 처리량을 기대할 수 있다. 파티연의 메세지가 저장되는 위치를 오프셋이라고 부르며 오프셋은 순차적으로 증가하는 숫자(64bit)형태로 되어있다.

![카프카 토픽, 파티션, 오프셋](https://sookocheff.com/post/kafka/kafka-in-a-nutshell/log-anatomy.png)

카프카는 오프셋을 통해 메세지의 순서를 보장하고 컨슈머에서는 마지막까지 읽은 위치를 알 수 있다.

---

## 2.6 고가용성 보장

카프카는 전신이 분산 시스템이기 때문에 하나의 서버/노드가 다운되어도 다른 서버/노드가 장애가 발생한 서버의 역할을 대신하여 안정적인 서비스를 제공한다.

카프카에서 제공하는 리플리케이션은 토픽 자체를 복제하는 것이 아니라 토픽의 파티션을 복제한다. 토픽을 생성할 때 옵션으로 리플리케이션 팩터 수를 지정할 수 있다.

원본가 리플리케이션을 구분하기 위해 `Leader`, `Follower`라고 구분한다.

| 리플리케이션 팩터 수 | 리더 수 | 팔로워 수 |
|-------------|------|-------|
| 2           | 1    | 1     |
| 3           | 1    | 2     |
| 4           | 1    | 3     |

리더의 숫자는 1을 유지하고 팔로워수를 2로 유지하도록하여 리플리케이션 팩터 수를 3으로 유지하는게 이상적인 권장사항이라고 한다.

- 리더는 프로듀서, 컨슈머로부터 오는 R/W요청을 처리한다.
- 팔로워는 오직 리더로부터 리플리케이션한다.

---

## 2.7 주키퍼 의존성

주키퍼는 여러 대의 서버를 앙상블(클러스터)로 구성하고, 살아 있는 노드 수가 과반수 이상이 유지되어야 지속적인 서비스가 가능한 구조이다.

따라서 주키퍼는 반드시 홀수로 구성해야한다.

Znode를 이용해 카프카의 메타 정보가 주키퍼에 기록되며, 주키퍼는 이러한 Znode를 이용해 브로커의 `노드`, `토픽`, `컨트롤러` 관리를 담당한다.

---

# 3. 프로듀서

## 3.1 프로듀서 디자인

[DZone-Kafka Deep Dive](https://dzone.com/articles/take-a-deep-dive-into-kafka-producer-api)
![프로듀서 디자인 개요](https://jashangoyal.files.wordpress.com/2019/03/producer.png?w=810)

1. `ProducerRecord` 형태로 메세지를 발행한다.
2. 직렬화를 수행한다.
3. 파티셔너가 레코드를 통해 파티션을 확인하고 명시한 파티션이 없다면 라운드 로빈으로 파티션을 선택하여 저장한다.
4. 레코드들은 파티션별로 모이게 되는데, 프로듀서가 카프카로 전송하기 전 배치 전송을 하기 위함이다.
5. 브로커에 메세지를 전송한다. 
   6. 이 과정이 실패 시 재시도한다.

`ProducerRecord`는 카프카로 전송하기 위한 실제 데이터이며 구성요소는 아래와 같다.

- `토픽` : `필수` 값으로 어떤 토픽에 메세지를 발행할 것인지 명시해야한다.
- `파티션` : `선택사항` 값으로 토픽의 어떤 파티션에 메세지를 발행할 것인지 명시해야한다.
- `키` : `선택사항` 값으로 메세지의 값을 구분하기 위한 식별자를 명시해야한다.
- `밸류` : `필수` 값으로 메세지의 값을 구분하기 위한 식별자를 명시해야한다.

---

## 3.2 프로듀서 주요 옵션

메세지 손실, 메세지 전송 속도를 조정해야할 경우 아래 주요 옵션을 조절해야할 필요가 있다.

### `bootstrap.servers`
- 클라이언트가 카프카 클러스터에 처음 연결하기 위한 `호스트`, `포트` 정보를 나타낸다.
- 클러스터 내 모든 서버가 클라이언트의 요청을 받을 수 있으므로 처음 누가 받을지를 정하는 것이다.

### `client.dns.lookup`
- 하나의 호스트에 여러 IP를 매핑 사용하는 환경에서 하나의 IP에 연결하지 못할경우 다른 IP로 시도하는 설정이다.
- 이 값은 기본값으로 DNS에 할당된 모든 IP를 저장하여 다음 IP로 접근할 수 있또록 한다.

### `acks`
- 전송 보장 옵션으로, 프로듀서가 카프카 토픽 리더에 메세지를 전송한 후 카프카 생태계에 도착 여부를 확인할 것인지를 결정하는 옵션이다.
- `0` : 프로듀서는 메세지를 보내면 확인 응답을 받지 않는다. (속도 GOOD, 안정성 BAD)
- `1` : 리더가 메세지를 받았는지 확인하지만 모든 팔로워를 전부 확인하지 않는다. (속도 NOT BAD, 안정성 NOT BAD)
- `-1` : 팔로워가 전부 메세지를 받았는지 확인한다. 팔로워가 하나라도 있으면 메세지는 손실되지 않는다. (속도 BAD, 안정성 GOOD)

### `buffer.memory`
- 프로듀서가 카프카 서버로 데이터를 보낼 때 잠시 대기할 수 있는 전체 메모리이다. 단위는 byte이다.
- 배치 전송, 딜레이 등에 의해 사용되는 공간이다.

### `compression.type`
- 프로듀서가 메세지 전송 시 선택할 수 있는 압축 타입이다.
- `none`, `gzip`, `snappy`, `lz4`, `zstd` 중 원하는 타입을 선택할 수 있다.

### `enable.idempotence`
- 이 설정을 `true`로 하면 `중복 없는 전송`이 가능하다. 
- 이 설정을 사용하기 위해선 `max.in.flight.requests.per.connection`옵션 값은 5이하, `retries`옵션 값은 0 이상, `acks`는 -1로 설정해야한다.

### `max.in.flight.requests.per.connection`
- 하나의 커넥션에서 프로듀서가 ACK없이 전송할 수 있는 최대 요청 수 이다.
- 메세지의 순서가 중요하다면 1로 설정할 것을 권장하지만 성능이 떨어진다.

### `retries`
- 전송에 실패한 데이터를 다시 보내는 횟수이다.

### `batch.size`
- 동일한 파티션으로 보내는 여러 데이터를 배치로 보내도록한다. 적절하게 배치 크기를 설정하면 성능에 도움을 준다.

### `linger.ms`
- 배치 형태의 메세지를 보내기 전에 추가적인 메세를 기다리는 시간을 조정할 수 있는 옵션이다.
- 배츠 크기에 도달하지 못했을 때 `linger.ms` 제한시간에 도달했을 때 메세지를 전송한다.

### `transactional.id`
- `정확히 한 번 전송`을 위해 사용하는 옵션이다.
- 동일한 TransactionalId에 한해 정확히 한 번을 보장한다.
- 옵션을 사용하기 전 `enable.idempotence`를 true로 설정해야한다.

---

## 3.3 프로듀서 예제

프로듀서는 `메세지를 보내고 확인하지 않기`, `동기 전송`, `비동기 전송`으로 전송할 수 있고 각각 처리량과 데이터 손실의 Trade-Off를 가지고 있다.

### 3.3.1 메세지를 보내고 확인하지 않기

아래 코드는 메세지를 보내고 확인하지 않는 전송 방식의 예시이다. `send()`메서드를 사용해 메세지를 전송한 후 `Future`객체로 RecordMetadata를 리턴받지만 리턴값을 무시하므로 메세지가 성공적으로 전송됐는지 알 수 없다.

카프카 브로커에 메세지를 전송한 후의 에러는 무시하지만 전송 전에 에러가 발생하면 이에 대한 예외는 처리할 수 있다.

```java
public class ProducerFireForgot {
  public static void main(String[] args) {
    KafkaProducer<String, String> producer = new KafkaProducer<>(props);
    // ++ 기타 카프카 셋팅
    
    producer.send(record);
  }
}
```

### 3.3.2 동기 전송

아래 코드는 메세지를 보내고 확인하는 동기 전송 방식의 예시이다. `send()`메서드를 사용해 메세지를 전송한 후 `Future`객체로 RecordMetadata를 리턴받는다.

카프카 브로커에 메세지를 전송한 후의 에러는 무시하지만 전송 전에 에러가 발생하면 이에 대한 예외는 처리할 수 있다.

```java
public class ProducerSync {
  public static void main(String[] args) {
    KafkaProducer<String, String> producer = new KafkaProducer<>(props);
    // ++ 기타 카프카 셋팅
    
    Producer producer = new Producer();
    RecordMetadata recordMetadata = producer.send(record).get();
    System.out.println(recordMetadata.topic());
    System.out.println(recordMetadata.partition());
    System.out.println(recordMetadata.offset());
  }
}
```

---

### 3.3.3 비동기 전송

아래 코드는 비동기로 전송하는 예시이다. 카프카가 오류를 리턴하면 onCompletion()은 예외를 갖게되어 애플리케이션 측에서 추가적인 예외처리를 해주면 된다.

프로듀서 코드에는 콜백함수를 전달해주는 방식이다.

```java
public class ProducerAsync {
  public static void main(String[] args) {
    KafkaProducer<String, String> producer = new KafkaProducer<>(props);
    // ++ 기타 카프카 셋팅
    
    producer.send(record, new DigerProducerCallback(record));
  }
}

public class DigerProducerCallback implements CallBack {
  private ProducerRecord<String, String> record;
  
  // 생성자
  
  @Override
  public void onCompletion(RecordMetadata recordMetadata, Exception e) {
    if (e != null) {
      e.printStackTrace();
    } else {
      System.out.println(recordMetadata.topic());
      System.out.println(recordMetadata.partition());
      System.out.println(recordMetadata.offset());
    }
  }
}
```

---

# 4. 컨슈머

컨슈머는 단순하게 메세지만 가져오는 것 뿐만 아니라, `컨슈머 그룹`, `리밸런싱`등 여러 동작을 수행한다.

프로듀서가 아무리 빠르게 카프카로 메세지를 전송하더라도 컨슈머가 카프카로부터 빠르게 메세지를 읽지 못하면 결국 지연된다.

## 4.1 컨슈머 기본 동작

프로듀서가 카프카의 토픽으로 메세지를 전송하면 해당 메세지들은 브로커들의 로컬 디스크에 저장된다.

`컨슈머 그룹`은 하나 이상의 컨슈머들이 모여있는 그룹을 의미하고, `컨슈머는 반드시 컨슈머 그룹`에 속한다.

컨슈머 그룹은 각 파티션의 리더에게 카프카 토픽에 저장된 메세지를 가져오는 요청을 보낸다.

`하나의 컨슈머 그룹 내의 컨슈머 수`와 파티션 수는 `1:1`로 매핑되는 것이 이상적이다. (즉, 파티션이 5개라면 컨슈머 그룹 내의 컨슈머는 5개)

파티션 수보다 컨슈머 수가 많으면 더 많은 수의 컨슈머들이 대기 상태로만 존재하기 때문에 리소스 낭비가 발생하게 된다. 

만약 컨슈머가 장애가 발생하는 것을 대비하여 초과하여 두는 것이라면 `컨슈머 그룹`내의 `리밸런싱` 동작을 통해 **다른 컨슈머가 그 동작을 대신 수행**하므로 추가적인 리소스를 소비할 필요가 없다.

---

## 4.2 컨슈머 주요 옵션

### `bootstrap.servers`
- 클라이언트가 카프카 클러스터에 처음 연결하기 위한 `호스트`, `포트` 정보를 나타낸다.
- 클러스터 내 모든 서버가 클라이언트의 요청을 받을 수 있으므로 처음 누가 받을지를 정하는 것이다.

### `fetch.min.bytes`
- 한 번에 가져올 수 있는 최소 데이터 크기이다.
- 지정한 크기보다 작은 경우, 요청에 응답하지 않고 데이터가 누적될 때까지 기다린다.

### `group.id`
- 컨슈머가 속한 컨슈머 그룹을 식별하는 식별자이다.
- 동일한 그룹 내의 컨슈머 정보는 상호 공유된다.

### `heartbeart.interval.ms`
- 하트비트가 있다는 것은 컨슈머의 상태가 active임을 의미한다.
- `session.timeout.ms`와 관계가 있어 `session.timeout.ms보다 낮은 값`으로 설정해야한다.

### `max.partition.fetch.bytes`
- 파티션당 가져올 수 있는 최대 크기를 의미한다.

### `session.timeout.ms`
- 이 시간으로 컨슈머가 종료된 것인지 판단한다.
- 컨슈머는 주기적으로 하트비트를 보내야한다.
  - 컨슈머가 종료된 것으로 판단되면 리밸런싱을 시작한다.

### `enable.auto.commit`
- 백그라운드로 주기적으로 오프셋을 커밋한다.

### `fetch.max.bytes`
- 한 번의 fetch로 가져올 수 있는 최대 크기이다.

### `group.instance.id`
- 컨슈머의 고유한 식별자이다.
- 이 옵션을 설정하면 static멤버로 간주되어 불필요한 리밸런싱을 수행하지 않게 된다.

### `isolation.level`
- 트랜잭션 컨슈머에서 사용되는 옵션이다.
- 기본값인 `read_uncommitted`는 으로 모든 메세지를 읽고, `read_comitted`는 트랜잭션이 완료된 메세지만 읽는다.

### `max.poll.records`
- 한 번의 poll()요청으로 가져오는 최대 메세지의 수를 지정한다.

### `partition.assignment.strategy`
- 파티션 할당 전략으로 기본값은 range이다.

### `fetch.max.wait.mx`
- 가져오려는 데이터의 크기가 `fetch.min.bytes`에 의해 설정된 크기보다 작을 때, 요청에 대한 응답을 기다리는 최대 시간이다.

---

## 4.3 컨슈머 예제

컨슈머는 `오토 커밋`, `동기 가져오기`, `비동기 가져오기`으로 전송할 수 있고 각각 처리량과 데이터 손실의 Trade-Off를 가지고 있다.

### 4.3.1 오토 커밋

기본값으로 가장 많이 사용되는 옵션이다.

- 오프셋을 주기적으로 커밋하여 오프셋을 따로 관리하지 않아도 되는 장점이 있다.
- 하지만 컨슈머 종료가 빈번히 일어나면 일부 메세지를 못 가져오거나 중복으로 가져오는 단점이 있다.

`.poll(1000)`은 poll의 인터벌을 지정해준 것이다.

```java
public class ProducerSync {
  public static void main(String[] args) {
    // 오토 커밋 설정
    props.put("enable.auto.commit", "true");
    
    KafkConsumer<String, String> consumer = new KafkaConsumer<>(props);
    consumer.subscribe(Arrays.asList("Topic-Name01"));
    // ++ 기타 카프카 셋팅

    ConsumerRecords<String, String> records = consumer.poll(1000);
    
    for-loop {
      System.out.println(record.topic());
      System.out.println(record.partition());
      System.out.println(record.offset());  
    }
  }
}
```

### 4.3.2 동기 가져오기

poll()을 이용해 메세지를 가져온 후 처리까지 완료하고 현재 오프셋을 커밋한다.

- 속도가 느리다.
- 메세지 손실 가능성이 낮다.

메세지가 손실되면 안되는 중요한 처리에는 동기 방식을 권장한다. 하지만 메세지 중복 이슈는 피할 수 없다.

```java
public class ProducerSync {
  public static void main(String[] args) {
    props.put("enable.auto.commit", "false");
    KafkConsumer<String, String> consumer = new KafkaConsumer<>(props);
    consumer.subscribe(Arrays.asList("Topic-Name01"));
    // ++ 기타 카프카 셋팅

    ConsumerRecords<String, String> records = consumer.poll(1000);
    
    for-loop {
      System.out.println(record.topic());
      System.out.println(record.partition());
      System.out.println(record.offset());
      consumer.commitSync();
    }
  }
}
```

### 4.3.3 비동기 가져오기

비동기 커밋은 재시도를 수행하지 않는다. 그 이유는 아래 흐름에 의해 설명된다.

1. 1번 오프셋의 메세지를 읽은 후 1번 오프셋을 비동기 커밋한다. (커밋 시도 직후 마지막 오프셋은 1이다.)
2. 2번 오프셋의 메세지를 읽은 후 2번 오프셋을 비동기 커밋했지만 실패한다. (커밋 시도 직후 마지막 오프셋은 1이다.)
3. 3번 오프셋의 메세지를 읽은후 3번 오프셋을 비동기 커밋한다. (커밋 시도 직후 마지막 오프셋은 3이다.)

이 때 실패했던 2번 커밋을 시도해서 성공하면 마지막 오프셋은 2번이된다. 따라서 오프셋의 무결성이 무너지게 된다.

그래서 비동기 커밋이 실패하더라도 마지막의 비동기 커밋만 성공한다면 안정적으로 오프셋을 커밋할 수 있으므로 콜백을 사용하는 경우도 있다.

```java
public class ProducerSync {
  public static void main(String[] args) {
    props.put("enable.auto.commit", "false");
    KafkConsumer<String, String> consumer = new KafkaConsumer<>(props);
    consumer.subscribe(Arrays.asList("Topic-Name01"));
    // ++ 기타 카프카 셋팅

    ConsumerRecords<String, String> records = consumer.poll(1000);
    
    for-loop {
      System.out.println(record.topic());
      System.out.println(record.partition());
      System.out.println(record.offset());
      consumer.commitAsync();
    }
  }
}
```

### 4.4.4 컨슈머 그룹

![img.png](https://github.com/mash-up-kr/S3A/blob/master/14th_kafka/dohyeon/image/4_1.png?raw=true)

컨슈머들은 토픽의 파티션과 1:1로 매핑되어 메세지를 가져온다. 

만약 `컨슈머 01`이 문제가 생겨 `종료`되었다면 `컨슈머 02`, 혹은 `컨슈머 03`은 컨슈머 01 ₩대신 파티션 0을 컨슘한다.
