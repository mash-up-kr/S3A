# Kafka 2주차

# 3장. 카프카 기본 개념과 구조

카프카의 기본 개념과 구조를 다루고, 카프카의 처리량을 높이기 위해 설계된 분산 시스템, 페이지 캐시, 배치 전송 등을 살펴본 후 주키퍼의 역할에 대해서 좀 더 파고들어보자.

## 3.1 카프카 기초 다지기

### 카프카를 구성하는 주요 요소

- 주키퍼 : 아파치 프로젝트 애플리케이션 이름이다. 카프카의 메타데이터 관리 및 브로커의 정상상태 점검(Health Check)를 담당한다.
- 카프카 or 카프카 클러스터: 아파치 프로젝트 애플리케이션 이름이다. 여러 대의 브로커를 구성한 클러스터를 의미한다.
- 브로커: 카프카 애플리케이션이 설치된 서버 또는 노드를 말한다.
- 프로듀서: 카프카로 메시지를 보내는 역할을 하는 클라이언트.
- 컨슈머: 카프카에서 메시지를 꺼내가는 역할을 하는 클라이언트.
- 토픽: 카프카는 메시지 피드들을 토픽으로 구분하고, 각 토픽의 이름은 카프카 내에서 고유하다.
- 파티션: 병렬 처리 및 고성능을 얻기 위해 하나의 토픽을 여러 개로 나눈 것.
- 세그먼트: 프로듀서가 전송한 실제 메시지가 브로커의 로컬 디스크에 저장되는 파일.
- 메시지 or 레코드: 프로듀서가 브로커로 전송하거나 컨슈머가 읽어가는 데이터 조각을 말함

### 리플리케이션

카프카에서 리플리케이션이란 각 메시지들을 여러 개로 복제해서 카프카 클러스터 내 브로커들에 분산시키는 동작을 의미한다. 이를 통해 하나의 브로커가 종료되더라도 카프카는 안정성을 유지할 수 있다. 즉, `--replication-factor 3` 라는 옵션은 원본을 포함한 리플리케이션이 총 3개가 있다는 뜻이다.

정확하게 말하면, 토픽이 복제되는 것이 아니라 토픽의 파티션이 복제되는 것이다.

안정성을 토대로 리플리케이션 갯수를 정할 텐데 아래의 요건을 살펴서 갯수를 정한다.

1. 테스트나 개발 환경: 리플리케이션 팩터 수를 1로 설정
2. 운영 환경(로그성 메시지로서 유실 허용): 리플리케이션 팩터 수를 2로 설정
3. 운영 환경(유실 허용하지 않음): 리플리케이션 팩터 수를 3으로 설정.

물론 안정성을 더욱 높이고자 하는 경우 리플리케이션 팩터 수를 4, 5 혹은 그 이상으로 설정할 수 있지만 팩터 수 3에서도 메시지 안정성도 보장하고 적절한 디스크 공간을 사용할 수 있었다. 4, 5, 그 이상일 경우 많은 리소스를 차지함으로 이 점을 염두에 두어야 한다.

### 파티션

하나의 토픽이 한 번에 처리할 수 잇는 한계를 높이기 위해 토픽 하나를 여러 개로 나눠 병렬 처리가 가능하게 만든 것을 파티션이라고 한다. 이렇게 하나를 여러 개로 나누면 분산 처리가 가능해지고 파티션 수 만큼의 컨슈머를 연결할 수 있다. 파티션 수도 토픽을 생성할 때 옵션으로 선택할 수 있는데 파티션 수를 결정하는 기준이 분명하지 않다. 다만, 파티션 수는 운영 중에 늘릴 수는 있으나 줄일 수 는 없기에 초기에는 2 혹은 4 정도로 설정하고 LAG 등을 모니터링하면서 조금씩 늘려가는 방법이 가장 좋다.

> LAG란 프로듀서가 보낸 메시지 수 - 컨슈머가 가져간 메시지 수를 나타낸다. 즉, 지연 전송 되어 묶여있는 상태의 메시지의 수를 말한다.
> 

### 세그먼트

우리가 이전 실습에서 메시지를 보냈는데 이 메시지는 그럼 어디에 저장되어 있는걸까? 우리가 프로듀서로 보낸 토픽의 파티션0에 저장되어 있다. 이 메시지를 찾기 위해 peter-kafka01 서버로 접근한 후 `/data/kafka-logs` 로 접근하면 각 파티션 디렉토리가 존재한다. (이 경우 `peter-overview01-0` )

해당 디렉토리로 이동 한 후 `ls` 명령어로 확인해보면 00000000000000.log 파일이 존재한다. 이를 dump 떠서 실행해보면 우리가 보낸 메시지를 확인할 수 있다.

즉, 이 과정을 순서대로 표현해보자면

1. 프로듀서는 카프카의 peter-overview01 토픽으로 메시지를 전송한다.
2. peter-overview01 토픽은 파티션이 하나뿐이므로, 프로듀서로부터 받은 메시지를 파티션0의 세그먼트 로그 파일에 저장한다.
3. 브로커의 세그먼트 로그 파일에 저장된 메시지는 컨슈머가 읽어갈 수 있다.

## 카프카의 핵심 개념

카프카가 높은 처리량과 안정성을 지니게 된 특성들을 살펴보며 좀 더 파고들어 보자.

### 분산 시스템

네트워크 상에서 연결된 컴퓨터들의 그룹을 의미하며 단일 시스템이 갖지 못한 높은 성능을 목표로 하는 시스템이며 여러 특징을 갖는다. 해당 특징들은

- 하나의 서버 또는 노드 등에 장애가 발생할 때 다른 서버 또는 노드가 대신  처리하므로 장애 대응이 탁월하다
- 부하가 높은 경우에는 시스템 확장이 용이하다

이런 특징들을 카프카도 갖고 있으며 최초 구성한 클러스터의 리소스가 한계치에 도달해 더욱 높은 메시지 처리량이 필요한 경우 브로커를 추가하는 방식으로 확장이 가능하다.

### 페이지 캐시

카프카는 높은 처리량을 얻기 위해 몇 가지 기능을 추가했는데 대표적인 페이지 캐시이다. 운영체제에서 사용하는 페이지 캐시는 직접 디스크에 읽고 쓰는 대신 물리 메모리 중 애플리케이션이 사용하지 않는 일부 잔여 메모리를 환ㄹ용한다. 이렇게 페이지 캐시를 사용하면 디스크 I/O 에 대한 접근이 줄어들어 성능을 높일 수 있다.

### 배치 전송 처리

카프카는 프로듀서, 컨슈머 클라이언트들과 서로 통신하며, 이들 사이에서 수많은 메시지를 주고받는다. 이 때 발생하는 수많은 통신을 묶어서 처리할 수 있다면 단건으로 통신할 때에 비해 네트워크 오버헤드를 줄일 수 있을 뿐 아니라 장기적으로 더욱 빠르고 효율적으로 처리할 수 있다.

카프카는 이 배치 전송 처리를 적극 권장하고 있으며, 비즈니스 요구사항 상 실시간으로 처리하는 것이 아닌 로그를 남겨야 한다는 등의 배치 성격의 기능들은 배치 전송 처리하는 것을 권장한다.

### 압축 전송

카프카는 메시지 전송 시 좀 더 높은 성능을 위해 압축 전송을 사용하는 것을 권장하며 지원하는 압축 타입으로 gzip, snappy, lz4, zstd 등을 지원한다. 압축의 특성 상 소량을 여러번 압축하는 것보다 대량을 한번에 압축하는게 좋기 때문에 앞서 설명한 배치 전송 처리와 함께 쓰면 네트워크 대역폭이나 회신 비용등을 크게 줄일 수 있다.

**압축 타입 선택 기준**

- 일반적으로 높은 압축률이 필요한 경우라면 gzip or zstd
- 빠른 응답 속도가 필요하다면  lz4 or snappy

위의 기준은 일반적이므로 운영 환경에 적용하기 전에 실제로 메시지를 전송해보면서 압축 타입별로 직접 테스트 해보고 결정하는 것이 가장 좋다!

### 토픽, 파티션, 오프셋

카프카는 토픽이라는 곳에 데이터를 저장하는데 이를 이메일 주소 정도의 개념으로 이해하면 쉽다. 또 이 토픽은 병렬 처리를 위해 여러 개의 파티션이라는 단위로 나뉜다. 카프카에서는 이와 같은 파티셔닝으로 하나의 토픽이라도 높은 처리량을 수행할 수 있다. 이 파티션의 메시지가 저장된 위치를 오피셋이라 부르며 오프셋은 순차적으로 증가하는 숫자(64비트 정수)형태로 되어 있다. 이 오프셋을 통해 카프카는 메시지의 순서를 보장하고 컨슈머에서는 마지막까지 읽은 위치를 알 수 있다.

### 고가용성 보장

카프카는 분산 시스템이기 때문에 하나의 서버나 노드가 다운되어도 다른 서버 또는 노드가 장애가 발생한 서버의 역할을 대신해 안정적인 서비스를 제공할 수 있다. 이런 고가용성을 보장하기 위해 카프카는 리플리케이션 기능을 제공하는데 카프카에서 제공하는 리플리케이션은 토픽 자체의 복제가 아닌 토픽의 파티션 복제를 통해 리플리케이션을 제공한다. 이런 원본-리플리케이션을 구분하기 위해 다른 시스템에서는 마스터, 미러와 같은 용어를 쓰는데 카프카는 리더와 팔로워라고 부른다.

리더는 리플리케이션 팩터 수가 증가해도 항상 1개이고, 나머지는 모두 팔로워다. 리더는 프로듀서, 컨슈머로부터 오는 모든 읽기와 쓰기 요청을 처리하며, 팔로워는 오직 리더로부터 리플리케이션하게 된다.

### 주키퍼의 의존성

주키퍼는 여러 대의 서버를 앙상블로 구성하고 살아 있는 노드 수가 과반 수 이상 유지된다면 지속적인 서비스가 가능한 구조이다. 주키퍼는 항상 홀수로 구성해야 한다.

지노드(Znode)를 이용해 카프카의 메타 정보가 주키퍼에 기록되며, 주키퍼는 이러한 지노드를 이용해 브로커의 노드 관리, 토픽 관리, 컨트롤러 관리 등 매우 중요한 역할을 하고 있다. 요즘에는 주키퍼의 성능 한계가 드러나 주키퍼의 한계를 극복하고자 카프카에서는 주키퍼에 대한 의존성을 제거하고 있다.

## 프로듀서의 기본 동작과 예제 맛보기

자바를 이용해 프로듀서를 직접 다뤄보자

### 프로듀서 디자인

![Untitled](https://github.com/mash-up-kr/S3A/blob/master/14th_kafka/jaemin/image/image3.png?raw=true)

ProducerRecord라고 표시된 부분은 카프카로 전송하기 위한 실제 데이터이며, 토픽, 파티션, 키, 밸류로 구성된다. 프로듀서가 카프카로 레코드를 전송할 때 특정 토픽을 명시해야 하며, 밸류에 메시지 내용을 넣기에 이 둘은 필숫값이다. 특정 파티션을 지정하기 위한 파티션과 레코드들을 정렬하기 위한 레코드의 키는 선택사항이다. 그 이유는 Send 메소드를 통해 메시지를 전송할 때 진행하는 시리얼라이저, 파티셔너를 거치며 채워지게 된다. 파티셔너 과정에서 레코드에 이미 파티션이 지정되어 있으면 파티셔너는 생략되며 파티션을 지정하지 않는 경우에는 키를 가지고 파티션을 선택해 레코드를 전달하는데 기본적으로 라운드 로빈 방식으로 동작한다. 이렇게 프로듀서 내부에서는 send 이후 레코드들을 파티션별로 잠시 모아두고 그 이유는 배치 전송을 하기 위함이다. 전송이 실패하면 재시도 동작이 이뤄지고 지정된 횟수만큼의 재시도가 실패하면 최종 실패를 전달하며, 전송이 성공하면 메타데이터를 리턴한다.

### 프로듀서의 주요 옵션

- bootstrap.servers
    - 카프카 클러스터는 클러스터 마스터라는 개념이 없으므로, 클러스터 내 모든 서버가 클라이언트의 요청을 받을 수 있다. 클라이언트가 카프카 클러스터에 처음 연결하기 위한 호스트와 포트 정보를 나타낸다.
- acks
    - 0, 1, all(-1) 로 표현하며, 0은 빠른 전송, 일부 메시지 손실 가능
    - 1은 리더가 메시지를 받았는지 확인하지만 팔로워는 확인하지 않음
    - all(-1)은 팔로워까지 메시지를 받았는지 여부를 확인. 다소 느릴순 있어도 하나의 팔로워가 있는 한 메시지 손실되지 않는다
- buffer.memory
    - 프로듀서가 카프카 서버로 데이터를 보내기 위해 잠시 대기할 수 있는 전체 메모리 바이트
- compression.type
    - 메시지 압축 타입
- enable.idempotence
    - true로 하는 경우 중복 없는 전송이 가능
    - max.in.flight.requets.per.connection은 5이하, retries는 0 이상, ack는 all로 설정해야 함
- max.in.flight.requets.per.connection
    - 하나의 커넥션에서 프로듀서가 최대한 ACK 없이 전송할 수 있는 요청 수
    - 메시지의 순서가 중요하다면 1로 설정할 것을 권장하지만 성능은 다소 떨어질 수 있다.
- retries
    - 일시적인 오류로 인해 전송에 실패한 데이터를 다시 보내는 횟수
- batch.size
    - 배치 크기
- linger.ms
    - 배치가 다 차지 않아 기다리는 시간을 조정
- transactional.id
    - 정확히 한 번 전송을 위해 사용하는 옵션
    - enable.idempotence가 true여야 한다.

### 프로듀서 예제

자바를 이용해 직접 카프카 클러스터와 연결해 프로듀서의 역할을 코드로 작성해보자.

메시지를 보내고 확인하지 않는 방법, 동기, 비동기 방법이 있다.

메시지를 보내고 확인하지 않는 방법

```java
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class ProducerFireForgot {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", 
                "peter-kafka01.foo.bar:9092,peter-kafka02.foo.bar:0902,peter-kafka03.foo.bar:9092"
        );
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerialzer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerialzer");
        
        Producer<String, String> producer = new KafkaProducer<>(props);
        
        try {
            for(int i = 0; i < 3; i++) {
                ProducerRecord<String, String> record = new ProducerRecord<>("peter-basic01", "Apache Kafka is a distributed streaming platform - " + i);
                producer.send(record);
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            producer.close();
        }
    }
}
```

**메시지를 보내고 확인하지 않는 방법의 특징**

- 카프카는 항상 살아 있고 프로듀서 또한 자동으로 재시작하므로 대부분은 성공하나 운영 환경에서는 사용하지 않아야 한다

동기 방법

```java
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Properties;

public class ProducerSync {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers",
                "peter-kafka01.foo.bar:9092,peter-kafka02.foo.bar:0902,peter-kafka03.foo.bar:9092"
        );
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerialzer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerialzer");

        Producer<String, String> producer = new KafkaProducer<>(props);

        try {
            for(int i = 0; i < 3; i++) {
                ProducerRecord<String, String> record = new ProducerRecord<>("peter-basic01", "Apache Kafka is a distributed streaming platform - " + i);
                RecordMetadata metadata = producer.send(record).get();
                System.out.printf("Topic: %s, Partition: %d, offset: %d, key: %s, ReceivedMessage: $s\n", metadata.topic(), metadata.partition(), metadata.offset(), record.key(), record.value());
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            producer.close();
        }
    }
}
```

**동기 방식의 특징**

- `send()` 메서드의 Future 객체를 리턴하며 `get()` 을 이용해 Future가 완료되길 기다리고 성공했는지 여부를 확인한다
- 신뢰성 있는 메시지 전달 과정의 핵심이다

비동기 방법

```java
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public class PeterProducerCallback implements Callback {
    
    private ProducerRecord<String, String> record;
    
    public PeterProducerCallback(ProducerRecord<String, String> record) {
        this.record = record;
    }
    
    @Override
    public void onCompletion(RecordMetadata metadata, Exceptione) {
        if(e != null) {
            e.printStackTrace();
        } else {
            System.out.printf("Topic: %s, Partition: %d, Offset: %d, Key: %s, Received Message: %s\n", metadata.topic(), metadata.partition(), metadata.offset(), record.key(), record.value());
        }
    }
}

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class ProducerAsync {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers",
                "peter-kafka01.foo.bar:9092,peter-kafka02.foo.bar:0902,peter-kafka03.foo.bar:9092"
        );
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerialzer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerialzer");

        Producer<String, String> producer = new KafkaProducer<>(props);

        try {
            for(int i = 0; i < 3; i++) {
                ProducerRecord<String, String> record = new ProducerRecord<>("peter-basic01", "Apache Kafka is a distributed streaming platform - " + i);
                producer.send(record, new PeterProducerCallback(record));
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            producer.close();
        }
    }
}
```

**비동기 방식의 특징**

- 콜백을 통한 비동기 방식의 동작
- 결과를 기다리지 않기에 빠른 전송이 가능하고, 전송이 실패한 경우라도 예외를 처리해 이후 예외 에러 로그 등에 기록할 수 있음.

## 컨슈머의 기본 동작과 예제 맛보기

컨슈머가 단순하게 카프카로부터 메시지만 가져오는 것이 아닌 내부적으로는 컨슈머 그룹, 리밴런싱 등 여러 동작을 수행한다. 이런 컨슈머의 기본 동작과 옵션을 이해하지 못하면 원하는 형태로 운영이 불가능하다.

### 컨슈머의 기본 동작

프로듀서가 카프카의 토픽으로 메시지를 전송하면 해당 메시지들은 브로커들의 로컬 리스크에 저장된다. 그 후 우린 컨슈머를 통해 토픽에 저장된 메시지를 갖고 올 수 있다. 컨슈머 그룹은 하나 이상의 컨슈머들이 모여 있는 그룹을 의미하고, 컨슈머는 반드시 컨슈머 그룹에 속하게 된다. 이 컨슈머 그룹은 각 파티션의 리더에게 카프카 토픽에 저장된 메시지를 갖고 오기 위한 요청을 보낸다. 이때 파티션 수와 컨슈머 수는 일대일로 매핑되는 것이 이상적이다. 컨슈머 수가 파티션 수보다 많다고 해서 처리량이 높아지는 것도 아니고 더 잘 장애를 해결하는 것도 아니기에 일대일로 배치하자.

### 컨슈머의 주요 옵션

- bootstrap.server
    - 프로듀서와 동일하게 브로커의 정보를 입력
- fetch.min.bytes
    - 한 번에 가져올 수 있는 최소 데이터 크기. 지정한 크기보다 작은 경우 대기가 발생
- group.id
    - 컨슈머가 속한 컨슈머 그룹을 식별하는 식별자
- heartbeat.interval.ms
    - 컨슈머의 상태가 active인지 확인하기 위한 heartbeat 체크의 시간 주기. 일반적으로 [session.timeout.ms](http://session.timeout.ms) 의 1/3으로 설정
- max.partition.fetch.byte
    - 파티션당 가져올 수 있는 최대 크기
- session.timeout.ms
    - 컨슈머가 종료된 것인지 판단. 이 시간까지 하트비트가 발생하지 않으면 컨슈머는 종료된 것으로 간주한다. 종료된 걸로 간주된 컨슈머는 컨슈머 그룹에서 제외하고 리밸런싱 시작
- enable.auto.commit
    - 백그라운드로 주기적으로 오프셋을 커밋
- auto.offset.reset
    - 직전 offset를 찾기 못한 경우 offset을 어떻게 reset 할 지 지정
    - earliest: 초기의 오프셋으로 설정
    - latest: 가장 마지막의 오프셋으로 설정
    - none : 이전 오프셋을 찾지 못하면 에러 발생
- fetch.max.bytes
    - 한 번의 가져오기 요청으로 가져올 수 있는 최대 크기
- group.instance.id
    - 컨슈머의 고유 식별자. 설정시 static 멤버로 간주되어 불필요한 리밸런싱을 하지 않는다.
- islocation.level
    - 트랜잭션 컨슈머에서 사용되는 옵션으로
    - read_uncomitted은 기본값으로 모든 메시지를 읽고
    - read_committed은 트랜잭션이 완료된 메시지만 읽는다.
- max.poll.records
    - 한 번의 poll()요청으로 가져오는 최대 메시지 수
- partition.assignment.strategy
    - 파티션 할당 전략이며 기본값은 range다
- fetch.max.wait.ms
    - fetch.min.byters에 의해 설정된 데이터보다 적은 경우 요청에 대한 응답을 기다리는 최대 시간

### 컨슈머 예제

자바로 컨슈머 사용하는 코드를 작성해보자. 오토 커밋을 사용한 경우, 동기 방식, 비동기 방식으로 구현해보자

오토 커밋 방식

```java
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.util.Arrays;
import java.util.Properties;

public class ConsumerAuto {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers",
                "peter-kafka01.foo.bar:9092,peter-kafka02.foo.bar:0902,peter-kafka03.foo.bar:9092"
        );
        props.put("group.id", "peter-consumer01");
        props.put("enable.auto.commit", "true");
        props.put("auto.offset.reset", "latest");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerialzer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerialzer");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("peter-basic01"));

        try {
            while(true) {
                ConsumerRecords<String, String> records = consumer.poll(1000);
                for(ConsumerRecord<String, String> record: records) {
                    System.out.printf("Topic: %s, Partition: %s, Offset: %d, Key: %s, Value: %s\n", record.topic(), record.partition(), record.offset(), record.key(), record.value());
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            consumer.close();
        }
    }
}
```

**오토 커밋 방식의 특징**

- 기본값으로 가장 많이 사용되고 있는 것
- 주기적으로 커밋해 관리자가 오프셋을 따로 관리하지 않아도 된다
- 컨슈머 종료 등이 빈번히 일어나면 일부 메시지를 못가져오거나 중복으로 가져오는 경우가 있음

동기 방식

```java
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.util.Arrays;
import java.util.Properties;

public class ConsumerSync {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers",
                "peter-kafka01.foo.bar:9092,peter-kafka02.foo.bar:0902,peter-kafka03.foo.bar:9092"
        );
        props.put("group.id", "peter-consumer01");
        props.put("enable.auto.commit", "false");
        props.put("auto.offset.reset", "latest");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerialzer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerialzer");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("peter-basic01"));

        try {
            while(true) {
                ConsumerRecords<String, String> records = consumer.poll(1000);
                for(ConsumerRecord<String, String> record: records) {
                    System.out.printf("Topic: %s, Partition: %s, Offset: %d, Key: %s, Value: %s\n", record.topic(), record.partition(), record.offset(), record.key(), record.value());
                }
                consumer.commitSync();
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            consumer.close();
        }
    }
}
```

**동기 방식의 특징**

- 속도는 느리지만 메시지 손실은 거의 발생하지 않음
- 메시지의 중복 이슈는 피할 수 없다

비동기 방식

```java
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.util.Arrays;
import java.util.Properties;

public class ConsumerAsync {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers",
                "peter-kafka01.foo.bar:9092,peter-kafka02.foo.bar:0902,peter-kafka03.foo.bar:9092"
        );
        props.put("group.id", "peter-consumer01");
        props.put("enable.auto.commit", "false");
        props.put("auto.offset.reset", "latest");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerialzer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerialzer");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("peter-basic01"));

        try {
            while(true) {
                ConsumerRecords<String, String> records = consumer.poll(1000);
                for(ConsumerRecord<String, String> record: records) {
                    System.out.printf("Topic: %s, Partition: %s, Offset: %d, Key: %s, Value: %s\n", record.topic(), record.partition(), record.offset(), record.key(), record.value());
                }
                consumer.commitAsync();
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            consumer.close();
        }
    }
}
```

**비동기 방식의 특징**

동기 방식과의 차이점은 `consumer.commitAsync()` 이다. `commitAsync` 는 `commitSync` 와 달리 오프셋 커밋을 실패해도 재시도 하지 않는다. 비동기 커밋이 계속 실패하더라도 마지막의 비동기 커밋만 성공하면 안정적으로 오프셋을 커밋하게 된다. 오히려 재시도 이후의 커밋을 시도하면 오프셋이 꼬여 메시지 중복이 발생할 수 있다.

### 컨슈머 그룹의 이해

컨슈머는 컨슈머 그룹 안에 속한 것이 일반적인 구조로, 하나의 컨슈머 그룹 안에 여러 개의 컨슈머가 구성될 수 있고 컨슈머들은 토픽의 파티션과 일대일로 매핑되어 메시지를 가져오게 된다.

하나의 컨슈머 그룹 내에 속해있는 컨슈머들은 서로의 정보를 공유한다. 예를 들어 컨슈머01이 문제가 생겨 종료되면 컨슈머02, 컨슈머03은 컨슈머01이 하던 일을 대신해 peter-01 토픽의 파티션0을 컨슘하기 시작한다.