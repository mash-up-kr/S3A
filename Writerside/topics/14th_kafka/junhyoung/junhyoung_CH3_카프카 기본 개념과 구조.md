# 3장 카프카 기본 개념과 구조

## 3.1 카프카 기초 다지기

### 카프카를 구성하는 주요 요소

**주키퍼(ZooKeeper)**: 아파치 프로젝트 애플리케이션 이름. 카프카의 메타데이터 관리 및 브로커의 정상상태 점검을 담당

**카프카(Kafka)또는 카프카 클러스터(Kafka Cluster)**: 아파치 프로젝트 애플리케이션 이름. 여러 대의 브로커를 구성한 클러스터를 의미

**브로커(Broker)**: 카프카 애플리케이션이 설치된 서버 또는 노드

**프로듀서(Producer)**: 카프카로 메시지를 보내는 역할을 하는 클라이언트

**컨슈머(consumer)**: 카프카에서 메시지를 꺼내가는 역할을 하는 클라이언트

**토픽(Topic)**: 카프카는 **메시지 피드들을 토픽으로 구분**하고, 각 토픽의 이름은 카프카 내에서 고유함

**파티션(Partition)**: 병렬 처리 및 고성능을 얻기 위해 **하나의 토픽을 여러 개**로 나눈 것

**세그먼트(segment)**: 프로듀서가 전송한 실제 메시지가 브로커의 **로컬 디스크에 저장되는 파일**

**메시지(message) 또는 레코드(record)**: 프로듀서가 브로커로 전송하거나 컨슈머가 읽어가는 데이터 조각

### 3.1.1 리플리케이션
~~~
리플리케이션이란? 
각 메시지들을 여러 개로 복제해서 카프카 클러스터 내 브로커들에게 분산시키는 동작
리플리케이션을 활용하면 하나의 브로커가 종료되도 카프카는 안정성을 유지함
~~~

```
--partition 1, --replication-factor

--replication-factor: 카프카 내 몇 개의 리플리케이션을 유지하겠다는 의미

replication-factor가 1이라면 리플리케이션이 1개 있다는 뜻이며, 
3이라면 원본을 포함한 리플리케이션이 총 3개 있다는 뜻
```
#### 토픽의 리플리케이션 배치
![토픽의리플리케이션배치](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch3/토픽의리플리케이션배치.png)

위 그림에서 peter-overview01 토픽을 리플리케이션 팩터 수 3으로 설정 후 각 브로커에 빛된 상태이다.

peter-overview01 토픽은 원본을 포함해 총 3개 있다. (정확히 말하자면 카프카에서 토픽이 리플리케이션이 되는 것이 아닌 토픽의 파티션이 리플리케이션이 된다.)

~~~
일반적으로
테스트나 개발 환경: 리플리케이션 팩터 수를 1개로 설정
운영 환경(로그성 메시지로서 약간의 유실 허용): 리플리케이션 팩터 수를 2개로 설정
운영 환경(유실 허용 안함): 리플리케이션 팩터 수를 3개로 설정

보통 리플리케이션 팩터 수가 3개면 충분히 메시지 안정성도 보장하고 적절한 디스크 공간을 사용할 수 있음
~~~

### 3.1.2 파티션
~~~
파티션이란?
하나의 토픽이 한 번에 처리할 수 있는 한계를 높이기 위해 여러개로 나눠 병렬 처리가 가능하게 만든 것
하나를 여러 개로 나누면 분산 처리가 가능하며 나뉜 파티션 수만큼 컨슈머를 연결할 수 있음
~~~

#### 토픽과 파티션의 관계
![토픽과파티션의관계](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch3/토픽과파티션의관계.png)

위 그림은 카프카 클러스터에 있는 토픽을 파티션으로 나눈 것

#### 파티션 수는 초기 생성 후 언제든디 늘릴 수 있지만, 반대로 한 번 늘린 파티션 수는 절대 줄일 수 없음

~~~
컨슈머의 LAG이란?
프로듀서가 보낸 메시지 수(카프카에 남아 있는 메시지 수) - 컨슈머가 가져간 메시지 수
프로듀서가 5개 메시지를 전송하고 컨슈머가 4개의 메시지를 가져갔다면 LAG는 1
컨슈머가 지연 없이 모두 가져갔다면 0
~~~

### 3.1.2 세그먼트

파티션에서 좀 더 확장된 것이 세그멘트

#### 메시지 저장 방식
1. 프로듀서에 의해 브로커로 전송된 메시지는 토픽의 파티션에 저장되며, 
2. 각 메시지들은 세그멘트라는 로그 파일의 형태로 브로커의 로컬 디스크에 저장된다.

![파티션과세그먼트의관계](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch3/파티션과세그먼트의관계.png)

각 파티션마다 N개의 세그먼트 로그 파일이 존재함

![로그목록조회](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch3/로그목록조회.png)
~~~
디렉토리 내의 파일리스트 조회
[~]$ cd /data/kafka-logs/
[kafka-logs]$ ls

디렉토리 이름은 글자 그대로 peter-overview01이라는 토픽의 0번 파티션 디렉토리를 의미
만약 파티션이 2개로 설정됐다면 1번 파티션도 추가되어 peter-overview01-1도 있었을 것
~~~

~~~
그 후 peter-overview01-0 디렉토리의 리스트를 조회
[kafka-logs]$ cd peter-overview01-0
[peter-overview01-0]$ ls
~~~
![010디렉토리조회](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch3/010디렉토리조회.png)

다른 파일들의 로그는 4.3 절에서 확인함

~~~
xxd 0000000000.log
~~~

![처음에사용했던메시지](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch3/처음에사용했던메시지.png)
이 전에 보냈던 First message를 확인할 수 있음

![토픽파티션세그먼트](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch3/토픽파티션세그먼트.png)

~~~
위 관계도 설명
1. 프로듀서는 카프카의 peter-overview01토픽으로 메시지 전송
2. peter-overview01 토픽은 파티션이 하나뿐이므로, 프로듀서로부터 받은 메시지를 파티션0의 세그먼트 로그 파일에 저장
3. 브로커의 세그먼트 로그 파일에 저장된 메시지는 컨슈머가 읽어갈 수 있음

결론적으로 컨슈머는 peter-overview01 토픽을 컨슘해서 해당 토픽 내 파티션0의 세그먼트 로그 파일에서 메시지를 가져옴
~~~

### 3.2 카프카의 핵심 개념
카프카의 높은 처리량과 안정성을 지니게 된 특성들을 살펴봄

### 3.2.1 분산 시스템
부하가 높은 경우에 브로커를 추가하는 방식으로 해결할 수 있음

### 3.2.2 페이지 캐시
높은 처리량을 얻기 위한 기능 들 중 대표적인 것이 **페이지 캐시**

운영체제는 성능을 높이기 위해 페이지 캐시 활용이 대표적인데, 카프카도 비슷하게 설꼐되어 있음

페이지 캐시는 직접 디스크에 읽고 쓰는 대신 물리 메모리 중 애플리케이션이 사용하지 않는 일부 잔여 메모리를 활용함

![카프카와페이지캐시](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch3/카프카와페이지캐시.png)

카프카가 직접 디스크에서 읽고 쓰기를 하지 않고 페이지 캐시를 통해 읽고 쓰기를 함

### 3.2.3 배치 전송 처리

데이터 전송에 고비용이 든다고 가정할 때, 똑같이 N개의 데이터를 전달해야한다면 
결국 N개까지 기다렸다가 한 번에 보내는 배치 방식이 훨씬 효율적임

### 3.2.4 압축 전송

배치와 결합해서 사용하면 더더욱 강력함
높은 압축률이 필요하다면 gzip이나 zstd를 권장하고
빠른 응답 속도가 필요하다면 lz4나 snappy를 권장함

### 3.2.5 토픽, 파티션, 오프셋

카프카는 토픽에 데이터를 저장함. 메일 전송 시스템에서 이메일 주소 정도의 개념으로 이해하면 쉬움

토픽은 병렬 처리를 위해 여러 개의 파티션이라는 단위로 다시 나눔

카프카는 이와 같은 파티셔닝을 통해 단 하나의 토픽이라도 높은 처리량을 수행할 수 있음

이 파티션의 메시지가 저장되는 위치를 **오프셋**(offset)이라고 부르며 오프셋은 순차적으로 증가하는 숫자 형태로 되어있음

![파티션과오프셋](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch3/파티션과오프셋.png)

그림에서 보면 하나의 토픽이 총 3개의 파티션으로 나뉘며 프로듀서로부터 전송되는 메시지들의 쓰기 동작이 각 파티션별로 이뤄짐을 알 수 있음

각 파티션마다 순차적으로 증가하는 숫자들이 오프셋, 고유한 숫자

오프셋을 통해 메시지의 순서를 보장하고 컨슈머에서는 마지막까지 읽은 위치를 알 수도 있음

### 3.2.6 고가용성 보장
하나의 서버나 노드가 다운되더라도 다른 서버 또는 노드가 대신 맡아서 안정적인 서비스가 가능함 

이러한 고가용성을 제공하기 위해 **리플리케이션** 기능을 제공함

토픽 자체를 복제하는 것이 아니라 토픽의 파티션을 복제함

토픽을 생성할 때 옵션으로 리플리케이션 팩터 수를 지정할 수 있으며 숫자에 따라 리플리케이션들이 존재함

카프카에서는 리더와 팔로워라고 부름

![리더팔로워](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch3/리더팔로워.png)

일반적으로 리플리케이션 팩터 수를 3으로 권장함. 4장에서 자세히 다룸

### 3.2.7 주키퍼의 의존성

분산 어플리케이션에서 코디네이터 역할을 하는 애플리케이션으로 사용됨

주키퍼는 여러 대의 서버를 앙상블(클러스터)로 구성하고 살아 있는 노드 수가 과반수 이상 유지된다면 지속적인 서비스가 가능한 구조

**따라서 주키퍼는 반드시 홀수로 구성해야함**

지노드를 이용해 카프카의 메타 정보가 주키퍼에 기록됨

주키퍼는 이러한 지노드를 이용해 브로커의 노드 관리, 토픽 관리, 컨트롤러 관리 등 카프카의 중요한 메타데이터를 저장하고 각 브로커를 관리하는 중요한 역할을 담당

## 3.3 프로듀서의 기본 동작과 예제 맛보기


### 3.3.1 프로듀서 디자인

#### 프로듀서의 전체 흐름
![프로듀서전체흐름](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch3/프로듀서전체흐름.png)
**ProducerRecord**
~~~
카프카로 전송하기 위한 실제 데이터
토픽, 파티션, 키 벨류로 구성
카프카의 특정 토픽으로 메시지 전송하므로 레코드에서 토픽과 밸류(메시지 내용)는 필숫값
특정 파티션을 지정하기 위한 레코드의 파티션과 특정 파티션에 레코드들을 정렬하기 위한 레코드의 키는 필숫값이 아닌 선택사항
~~~

ProducerRecord에서 send() 메서드를 통해 **시리얼라이즈, 파티셔너**를 거침
```java
if(파티션 지정){
    파티셔너 동작 x, 지정된 파티션으로 레코드 전달
}
if(파티션 지정 안함){
    키를 가지고 파티션을 선택해 레코드를 전달 (기본적으로 라운드 로빈 방식으로 동작)
}
```
send() 메서드 이후에 배치 전송하기 위해 레코드들을 파티션별로 잠시 모아둠

전송이 실패하면 재시도 동작, 지정된 횟수만큼 재시도가 실패하면 최종 실패, 전송이 성공하면 메타데이터 리턴

### 3.3.2 프로듀서의 주요 옵션

대부분 프로듀서를 기본값으로 사용함

![프로듀서옵션1](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch3/프로듀서옵션1.png)
![프로듀서옵션2](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch3/프로듀서옵션2.png)

### 3.3.3 프로듀서 예제

프로듀서의 전송 방법
1. 메시지를 보내고 확인하지 않기
2. 동기 전송
3. 비동기 전송

### 메시지를 보내고 확인하지 않는 방법
```java
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class ProducerFireForgot {
        
    public static void main(String[] args) {
        Properties props = new Properties(); // 1. Properties 객체 생성
        
        // 2. 브로커 리스트 정의
        props.put( "bootstrap.servers", "peter-kafka01.foo.bar:9092, peter-kafka02. foo. bar:9092, peter-kafka03.foo.bar:9092");
        
        // 3. 메시지 키와 밸류는 문자열 타입이므로 카프카의 기본 StringSerializer를 지정
        props.put ("key serializer", "org.apache. kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        
        // 4. Properties 객체를 전달해 새 프로듀서 생성
        Producer<String, String> producer = new KafkaProducer(props);
        
        try {
            for (int i = 0; i < 3; i++) {
                // 5. ProducerRecord 객체 생성
                ProducerRecord<String, String> record = new ProducerRecord<>("peter- basic01",
                        "Apache Kafka is a distributed streaming platform - " + i);
                
                // 6. send() 메서드를 사용해 메시지를 전송한 후 자바 Future 객체로 
                // RecordMetadata를 리턴 받지만 리턴값을 사용안하므로 성공됐는지 알 수 없음
                producer.send(record);
            }
        } catch (Exception e){
            // 7. 에러 무시, 에러 출력
            e.printStackTrace();
        } finally {
            // 8. 프로듀서 종료
            producer.close();
        }
    }
}
```
위 코드는 프로듀서에서 카프카의 토픽으로 메시지를 전송하고 성공했는지 확인하지 않으므로 실제 운영 환경에서 사용되지 않음

하지만 대부분 카프카는 항상 살아 있고 프로듀서 또한 자동으로 재시작하므로 대부분은 성공적으로 메시지가 전송됨

### 동기 전송

```java
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Properties;

public class ProducerSync {

    public static void main(String[] args) {
        // 1. Properties 객체 생성
        Properties props = new Properties();

        // 2. 브로커 리스트 정의
        props.put("bootstrap.servers",
                "peter-kafka01.foo.bar:9092,peter-kafka02. foo.bar:9092,peter-kafka03.foo.bar:9092");
        
        // 3. 메시지 키와 밸류는 문자열 타입이므로 카프카의 기본 StringSerializer 지정 
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        // 4. Properties 객체를 전달해 새 프로듀서 생성
        Producer<String, String> producer = new KafkaProducer(props);
        try {
            for (int i = 0; i < 3; i++) {
                // 5. ProducerRecord 객체 생성
                ProducerRecord<String, String> record = new ProducerRecord("peter- basic01",
                        "Apache Kafka is a distributed streaming platform - " + i);
                // 6. get() 메소드를 이용해 카프카의 응답을 기다림. 메시지가 성공적으로 전송되지 않으면 예외가 발생하고 에러가 없다면 RecordMetadata를 얻음
                RecordMetadata metadata = producer.send(record).get();
                System.out.printf("Topic: 85, Partition: 8d, Offset: 8d, Key: 85, Received Message: &5\n"
                        , metadata.topic(), metadata.partition(), metadata.offset(), record.key(), record.value());
            }
        } catch (Exception e) {
            // 7. 메시지를 보내기 전과 보내는 동안 에러가 발생하면 예외가 발생함
            e.printStackTrace();
        } finally {
            // 8. 프로듀서 종료
            producer.close();
        }
    }
}
```
send 메소드의 Future 객체를 반환하여 get() 메소드를 이용해 Future를 기다린 후 send()가 성공했는지 실패했는지 여부를 확인

ProducerRecord 전송이 성공하고 나면 Record Metadata를 읽어 들여 파티션과 오프셋 정보를 확인할 수 있음

이 방법으로 메시지 전달의 성공 여부를 파악할 수 있으며 동기 전송 방식은 **신뢰성 있는 메시지 전달 과정**이 핵심

### 비동기 전송
#### 콜백을 사용하기 위해 Callback을 구현하는 클래스가 필요함
#### 카프카가 오류를 반환하면 onCompletion()은 예외를 갖게 되며 실제 운영 환경에서는 추가적인 예외 처리가 필요
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
    public void onCompletion (RecordMetadata metadata, Exception e){
        if (e != null) {
            e.printStackTrace();
        } else {
            System.out.printf("Topic: 85, Partition: %d, Offset: &d, Key: &s, Received Message: &5\n"
                    , metadata.topic(), metadata.partition(), metadata.offset(), record.key(), record.value());
        }
    }
}

public class ProducerAsync {
    public static void main(Stringl[] args) {
        // 1. Properties 객체를 생성
        Properties props = new Properties();
        // 2. 브로커 리스트 정의
        props.put("bootstrap.servers", "peter-kafka01.foo.bar:9092, peter-kafka02. foo. bar:9092, peter-kafka03.foo.bar:9092");
        // 3. 메시지 키와 밸류는 문자열 타입이므로 카프카의 기본 StringSerializer 지정 
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        // 4. Properties 객체를 전달해 새 프로듀서 생성
        Producer<String, String> producer = new KafkaProducere(props);
        try {
            for (int i = 0; i < 3; i++) {
                // 5. ProducerRecord 객체 생성
                ProducerRecord<String, String> record = 
                        new ProducerRecord<>("peter- basic01", "Apache Kafka is a distributed streaming platform - " + i); 
                
                // 6. 프로듀서에서 레코드를 보낼 때 콜백 객체를 같이 보냄
                producer.send(record, new PeterProducerCallback(record));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 7. 프로듀서 종료
            producer.close();
        }
    }
}
```
프로듀서는 send()메소드와 콜백을 함께 호출함

만약 동기 전송과 같이 프로듀서가 보낸 모든 메시지에 대해 응답을 기다리면 많은 시간을 소비하게 되므로 빠른 정송을 할 수 없음

비동기 방식으로 전송하면 빠른 전송이 가능하고 메시지 전송이 실패해도 예외를 처리할 수 있어 이후 에러 로그 등에 기록 가능함. 프로듀서에 대해 4장에서 자세히 다룸

## 3.4 컨슈머의 기본 동작과 예제 맛보기

컨슈머는 단순하게 카프카로부터 메시지만 가져오는 것이 아니라 내부적으로 컨슈머 그룹, 리밸런싱 등 여러 동작을 수행함

프로듀서가 아무리 빠르게 카프카로 메시지를 보내도 컨슈머가 빠르게 읽어오지 못하면 결국 지연 발생

매우 중요하므로 잘 이해해야함

### 3.4.1 컨슈머의 기본 동작

프로듀서가 카프카의 토픽으로 메시지를 전송하면 해당 메시지들은 브로커들의 로컬 디스크에 저장됨

컨슈머 그룹은 하나 이상의 컨슈머들이 모여 있는 그룹, 컨슈머는 반드시 컨슈머 그룹에 속함

컨슈머 그룹은 각 파티션의 리더에게 카프카 토픽에 저장된 메시지를 가져오기 위해 요청을 보냄

이 때 파티션 수와 컨슈머 수는 일대일로 매핑되는 것이 이상적

### 3.4.2 컨슈머의 주요 옵션

컨슈머를 사용하는 목적: 최대한 안정적이며 지연이 없도록 카프카로부터 메시지를 가져오는 것

![컨슈머주요옵션1](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch3/컨슈머주요옵션1.png)
![컨슈머주요옵션2](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch3/컨슈머주요옵션2.png)

### 3.4.3 컨슈머 예제

컨슈머에서 메시지를 가져오는 방식
1. 오토 커밋
2. 동기 가져오기
3. 비동기 가져오기

```java

import org.apache.kafka.clients.consumer.ConsumerRecord; 
import org.apache.kafka.clients.consumer.ConsumerRecords; 
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Arrays;
import java.util.Properties;

public class ConsumerAuto {
    public static void main(String[] args) {
        // 1. Properties 객체 생성
        Properties props = new Properties();
        
        // 2. 브로커 리스트 정의
        props.put("bootstrap.servers",
                "peter-kafka01.foo.bar:9092,peter-kafka02.foo. bar:9092, peter-kafka03.foo.bar:9092");
        // 3. 컨슈머 그룹 아이디 정의
        props.put("group. id", "peter-consumer01");
        // 4. 오토 커밋 적용
        props.put("enable.auto.commit", "true");
        // 5. 컨슈머 오프셋을 찾지 못하는 경우 Latest로 초기화하며 가장 최근부터 메시지를 가져옴
        props.put("auto.offset.reset", "latest");
        // 6. 문자열을 사용했으므로 StringDeserializer 지정
        props.put("key.deserializer", "org.apache. kafka.common.serialization. StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization. StringDeserializer");
        // 7. Properties 객체를 전달해 새 컨슈머 생성
        KafkaConsumer<String, String> consumer = new KafkaConsumer(props);
        // 8. 구독할 토픽을 지정
        consumer.subscribe(Arrays.asList("peter-basic01"));
        try {
            
            while (true) { // 9. 무한루프 시작 메시지를 가져오기 위해 카프카에 지속적으로 poll()을 함
                // 10. 컨슈머는 폴링하는 것을 계속 유지하며, 타임아웃 주기를 설정. 해당 시간만큼 블록
                ConsumerRecords<String, String> records = consumer.poll(1000);
                // 11. poll()은 레코드 전체를 리턴하고, 하나의 메시지만 가져오는 것이 아니므로 반복문 처리
                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf("Topic: 85, Partition: 85, Offset: &d, Key: %5, Value: s\n"
                            , record.topic(), record.partition(), record.offset(), record.key(), record.value());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 12. 컨슈머 종료
            consumer.close();
        }
    }
}
```
컨슈머 애플리케이션들의 기본 값으로 가장 많이 사용되고 있는 것이 오토 커밋

오토 커밋은 오프셋을 주기적으로 커밋하므로 관리자가 오프셋을 따로 관리하지 않아도 되는 장점이 있음

하지만 컨슈머 종료 등이 자주 일어나면 일부 메시지를 못 갖고 오거나 중복으로 가져옴

하지만 카프카는 안정적. 카프카 짱. 근야 쓰면됨 오토 커밋 자주 씀

### 동기 가져오기
```java
import org.apache.kafka.clients.consumer.ConsumerRecord; 
import org.apache.kafka.clients.consumer.ConsumerRecords; 
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Arrays;
import java.util.Properties;

public class ConsumerSync {
    public static void main(String[] args) {
        // 1. Properties 객체 생성
        Properties props = new Properties();
        // 2. 브로커 리스트 정의
        props.put("bootstrap.servers",
                "peter-kafka01.foo.bar:9092,peter-kafka02.foo.bar:9092,peter-kafka03.foo.bar:9092");
        // 3. 컨슈머 그룹 아이디 정의
        props.put("group.id", "peter-consumer01");
        // 4. 오토 커밋을 사용하지 않음
        props.put("enable.auto.commit", "false");
        // 5. 컨슈머 오프셋을 찾지 못하는 경우 latest로 초기화, 가장 최근부터 메시지를 가져옴
        props.put("auto.offset.reset", "latest");
        // 6. 문자열을 사용했으므로 StringDeserializer 지정
        props.put("key.deserializer", "org.apache.kafka.common.serialization. StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization. StringDeserializer");
        // 7. Properties 객체를 전달해 새 컨슈머를 생성
        KafkaConsumer<String, String> consumer = new KafkaConsumere(props);
        // 8. 구독할 토픽을 지정
        consumer.subscribe(Arrays.asList("peter-basic01"));

        try {
            while (true) {   // 9. 무한 루프 시작. 메시지를 가져오기 위해 카프카에 지속적으로 poll()을 함
                // 10. 컨슈머는 폴링하는 것을 계속 유지하며 타임아웃 주기를 설정. 해당 시간만큼 블록
                ConsumerRecords<String, String> records = consumer.poll(1000);
                // 11. poll()은 레코드 전체를 리턴하고, 하나의 메시지만 가져오는 것이 아니므로 반복무 처리
                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf("Topic: %s, Partition: %s, Offset: %d, Key: 85, Value: &5\n"
                            , record.topic(), record.partition(), record.offset(), record.key(), record.value());
                }
                consumer.commitSync();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 12. 컨슈머 종료
            consumer.close();
        }
    }
}
```
오토 커밋과 달리 poll()을 이용해 메시지를 가져온 후 처리까지 완료하고 현재의 오프셋을 커밋함

속도는 느리지만 메시지 손실은 거의 발생하지 않음

여기서 메시지 손실은 실제로 토픽에는 메시지가 존재하지만 잘못된 오프셋 커밋으로 인한 위치 변경으로 컨슈머가 메시지를 가져오지 못하는 경우를 말함

메시지가 손실되면 안 되는 중요한 처리 작업들은 본 방법으로 하는 것을 권장하지만 중복 문제는 여전하다.

### 비동기 가져오기

```java
import org.apache.kafka.clients.consumer.ConsumerRecord; 
import org.apache.kafka.clients.consumer.ConsumerRecords; 
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Arrays;
import java.util.Properties;

public class ConsumerAsync {
    public static void main(String[] args) {
        // 1. Properties 객체 생성 
        Properties props = new Properties();
        // 2. 브로커 리스트 정의
        props.put("bootstrap.servers", "peter-kafka01.foo.bar: 9092,peter-kafka02. foo. bar:9092, peter-kafka03.foo.bar:9092");
        // 3. 컨슈머 그룹 아이디 정의
        props.put("group.id", "peter-consumer01");
        // 4. 오토 커밋을 사용하지 않음
        props.put("enable.auto.commit", "false");
        // 5. 컨슈머 오프셋을 찾지 못하는 경우 latest로 초기화. 가장 최근부터 메시지를 가져옴
        props.put("auto.offset.reset", "latest");
        // 6. 문자열을 사용했으므로 StringDeserializer 지정
        props.put("key.deserializer", "org.apache.kafka.common.serialization. StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization. StringDeserializer");
        // 7. Properties 객체를 전달해 새 컨슈머를 생성
        KafkaConsumer<String, String> consumer = new KafkaConsumer(props);
        // 8. 구독할 토픽을 지정
        consumer.subscribe(Arrays.asList("peter-basic01"));
        try {
            while (true) {  // 9. 무한 루프 시작. 메시지를 가져오기 위해 카프카에 지속적으로 poll()을 함
                // 10. 컨슈머는 폴링하는 것을 계속 유지하며, 타임아웃 주기를 설정. 해당 시간만큼 블록함
                ConsumerRecords<String, String> records = consumer.poll(1000);
                // 11. poll()은 레코드 전체를 리턴하고, 하나의 메시지만 가져오는 것이 아니므로 반복문 처리
                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf("Topic: %s, Partition: %s, Offset: 8d, Key: 8s, Value: 85\n"
                            , record.topic(), record.partition(), record.offset(), record.key(), record.value());
                }
                // 12. 현재 배치를 통해 읽은 모든 메시지를 처리한 후, 추가 메시지를 폴링하기 전 현재의 오프셋을 비동기 커밋
                consumer.commitAsync();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 13. 컨슈머 종료
            consumer.close();
        }
    }
}
```
비동기 가져오기와 동기 가져오기의 가장 큰 차이점은 `consumer.commitAsync();`이다. 

`commitAsync()`는 `commitSync()`와 달리 오프셋 커밋을 실패하더라고 재시도하지 않는다.

비동기 커밋 재시도로 인해 수많은 메시지가 중복될 수 있으므로 비동기인 경우에는 커밋 재시도를 시도하지 않는다.

비동기 커밋만 성공한다면 안정적으로 오프셋을 커밋함

### 3.4.4 컨슈머 그룹의 이해

컨슈머는 컨슈머 그룹 안에 속한 것이 일반적인 구조이며 하나의 컨슈머 그룹 안에 여러 개의 컨슈머가 구성될 수 있다.

토픽의 파티션과 일대일로 매핑되어 메시지를 가져오게 된다.

![컨슈머그룹](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch3/컨슈머그룹.png)

그룹 내의 컨슈머들은 서로의 정보를 공유함

컨슈머01이 문제가 생겨 종료됐다면 컨슈머 02또는 컨슈머03은 컨슈머01이 하던 일을 대신해 peter-01 토픽의 파티션을 컨슘하기 시작함 - 이후 6장에서 자세히 다룸

## 3.5 정리

배치 전송, 페이지 캐시, 압축 사용 등의 기능들을 통해 카프카는 높은 성능을 갖게 됐음

다음 장에서 좀 더 상세하게 카프카의 내부 동작 원리와 구현을 살펴봄

