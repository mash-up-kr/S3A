# 3장 카프카 기본 개념과 구조
## 3.1 카프카 기초 다지기
- 주키퍼(ZooKeeper): 카프카의 메타데이터 관리 및 브로커의 정상상태 점검 담당
- 카프카(Kafka)/카프카 클러스터(Kafka cluster): 여러 대의 브로커를 구성한 클러스터
- 브로커(broker): 카프카 애플리케이션이 설치된 서버 또는 노드
- 프로듀서(producer): 카프카로 메시지를 보내는 역할을 하는 클라이언트 총칭
- 컨슈머(consumer): 카프카에서 메시지를 꺼내는 역할을 하는 클라이언트 총칭
- 토픽(topic): 카프카는 메시지 피드들을 토픽으로 구분하고, 각 토픽의 이름은 카프카 내에서 고유
- 파티션(partition): 병렬 처리 및 고성능을 얻기 위해 하나의 토픽을 여러 개로 나눈 것
- 세그먼트(segment): 프로듀서가 전송한 실제 메시지가 브로커의 로컬 디스크에 저장되는 파일
- 메시지(message)/레코드(record): 프로듀서가 브로커로 전송하거나 컨슈머가 읽어가는 데이터 조각

<br/>

### 3.1.1 리플리케이션(replication)
- 각 메시지들을 여러 개로 복제해서 카프카 클러스터 내 브로커들에 분산시키는 동작
- 하나의 브로커가 종료되더라도 카프카는 안정성을 유지할 수 있음
- `replication-factor`: 토픽 생성 명령어 일부
  - 1이라면 리플리케이션이 1개 있음을 의미
  - 3이라면 원본을 포함한 리플리케이션이 총 3개가 있음을 의미

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/ca9304fb-f7b1-4d2d-b7ed-20bc910aa888" />

🔼 토픽의 리플리케이션 배치
- peter-overview01 토픽을 replication-factor 수 3으로 설정한 후 각 브로커에 배치된 상태
- replication-factor 수가 커지면 안정성은 높아지지만 그만큼 브로커 리소스를 많이 사용하게 된다.
- 따라서 복제에 대한 오버헤드를 줄여서 최대한 브로커를 효율적으로 사용하는 것이 권장된다.

<br/>

**효율적인 replication-factor 수 설정 기준**
- 테스트나 개발 환경: replication-factor 수 1로 설정
- 운영 환경(로그성 메시지로서 약간의 유실 허용): replication-factor 수 2로 설정
- 운영 환경(유실 허용하지 않음): replication-factor 수 3으로 설정

<br/>

### 3.1.2 파티션(partition)
- 하나의 토픽이 한 번에 처리할 수 있는 한계를 높이기 위해 토픽 하나를 여러 개로 나눠 병렬 처리가 가능하게 만든 것
- 하나를 여러 개로 나누면 분산 처리가 가능하다. (나뉜 파티션 수만큼 컨슈머 연결 가능)

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/de5f67b4-b272-4f6f-b555-9ff1ad120677" />

🔼 토픽과 파티션의 관계
- 카프카 클러스터에 있는 토픽을 파티션으로 나눈 그림
- 토픽1은 1개, 토픽3을 3개 파티션으로 구성되어 있다. (파티션 번호는 0부터 시작)
- 파티션 수는 초기 생성 후 언제든지 늘릴 수 있지만, 한 번 늘리면 절대로 다시 줄일 수 없다. (신중하게 늘릴 것)
- 초기에 2~4 정도로 생성한 후, 모니터링하면서 조금씩 늘려가는 방법이 가장 좋다.
- [적절한 파티션 수를 산정하기 위해 계산해주는 공식을 제공하는 컨플루언트 사이트](https://eventsizer.io)

<br/>

### 3.1.3 세그먼트(segment)
- 프로듀서에 의해 브로커로 전송된 메시지는 토픽의 파티션에 저장되며, 각 메시지들은 세그먼트라는 로그 파일의 형태로 브로커의 로컬 디스크에 저장된다.

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/86094f6f-eed6-4f12-92f1-ad0de4faa7ec" />

🔼 파티션과 세그먼트의 관계
- 각 파티션별로 세그먼트를 나눠본 그림
- 각 파티션마다 N개의 세그먼트 로그 파일들이 존재한다.

<br/>

**전송한 메시지가 세그먼트에 남아 있는지 확인해보기**
```
$ cd /data/kafka-logs/
$ ls
```
- 카프카 토픽(peter-kafka01) 서버로 접근
- `ls`로 디렉토리 내 파일 리스트 확인
- 출력 내용 중 peter-overview01-0(토픽의 0번 파티션) 디렉토리 확인

```
$ cd peter-overview01-0
$ ls
```
- peter-overview01-0 디렉토리로 이동 후 디렉토리의 리스트 확인

```
$ xxd 00000000000000000000.log
```
- 00000000000000000000.log 파일 내용 확인
- `xxd` : hexdump 조회 명령어
- 출력 결과에서 전송했던 First message라는 메시지를 확인할 수 있다.

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/aa18eacc-6e60-4c23-a2f1-f58deefceaeb" />

🔼 토픽, 파티션, 세그먼트의 관계도
1. 프로듀서는 카프카의 peter-overview01 토픽으로 메시지를 전송한다.
2. peter-overview01 토픽은 파티션이 하나뿐이므로, 프로듀서로부터 받은 메시지를 파티션0의 세그먼트 로그 파일에 저장한다.
3. 브로커의 세그먼트 로그 파일에 저장된 메시지는 컨슈머가 읽어갈 수 있다.

<br/>

---

## 3.2 카프카의 핵심 개념
- 카프카가 높은 처리량과 안정성을 지니게 된 특성들을 살펴본다.

<br/>

### 3.2.1 분산 시스템
- 네트워크상에서 연결된 컴퓨터들의 그룹
- 단일 시스템이 갖지 못한 높은 성능을 목표로 한다.
- 👍 하나의 서버/노드 등에 장애가 발생할 때 다른 서버/노드가 대신 처리하므로 장애 대응이 탁월하다.
- 👍 부하가 높은 경우 시스템 확장이 용이하다.
- 카프카도 분산 시스템이므로 더 높은 메시지 처리량이 필요할 경우, 브로커를 추가하는 방식으로 확장이 가능하다.

<br/>

### 3.2.2 페이지 캐시(page cache)
- 직접 디스크에 읽고 쓰는 대신 물리 메모리 중 애플리케이션이 사용하지 않는 일부 잔여 메모리를 활용한다.
- 페이지 캐시를 이용하면 디스크 I/O에 대한 접근이 줄어들므로 성능을 높일 수 있다.

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/2c134a5b-0e0c-48c9-84de-c0380822c551" />

🔼 카프카와 페이지 캐시
- 카프카, 페이지 캐시, 디스크의 읽고 쓰기를 표현한 그림
- 카프카가 OS의 페이지 캐시를 이용한다는 것은 카프카가 직접 디스크에서 읽고 쓰기를 하지 않고 페이지 캐시를 통해 읽고 쓰기를 한다.

<br/>

### 3.2.3 배치(batch) 전송 처리
- 수많은 통신을 묶어서 처리할 수 있다면, 네트워크 오버헤드를 줄일 수 있고, 장기적으로 더욱 빠르고 효율적으로 처리할 수 있다.
- 카프카에서는 배치 전송을 권장한다.

<br/>

|그룹 분류|탑승 방식|기차에 탑승하는 승객 수|필요한 기차 수|
|---|---|---|---|
|실시간 그룹|승객 개개인이 도착하자마자 출발|1명|10대|
|배치 그룹|모든 승객을 기다린 후 출발|10명|1대|

🔼 실시간 그룹과 배치 그룹 비교

- 배치 그룹이 효율적이라는 사실을 확인할 수 있다.

<br/>

### 3.2.4 압축 전송
- 카프카는 메시지 전송 시 좀 더 성능이 높은 압축 전송을 사용하는 것을 권장한다.
- gzip, snappy, lz4, zstd 등의 압축 타입을 지원한다.
- 압축만으로 네트워크 대역폭이나 회선 비용 등을 줄일 수 있다.
- 높은 압축률이 필요하다면 gzip 또는 zstd를, 빠른 응답 속도가 필요하다면 lz4 또는 snappy가 권장된다.
- 실제로 메시지를 전송해보면서 압축 타입별로 직접 테스트를 해보고 결정하는 것이 가장 좋다.

<br/>

### 3.2.5 토픽(topic), 파티션(partition), 오프셋(offset)
- 토픽은 병렬 처리를 위해 여러 개의 파티션이라는 단위로 다시 나뉜다.
- 카프카에서는 파티셔닝을 통해 단 하나의 토픽이라도 높은 처리량을 수행할 수 있다.
- 파티션의 메시지가 저장되는 위치를 오프셋이라고 하고, 오프셋은 순차적으로 증가하는 숫자 형태로 되어 있다.

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/9f56f6b7-b959-4a35-a8da-1b5197e16ace" />

🔼 파티션과 오프셋
- 하나의 토픽이 총 3개의 파티션으로 나뉜다.
- 프로듀서로부터 전송되는 메시지들의 쓰기 동작이 각 파티션별로 이뤄진다.
- 각 파티션들마다 순차적으로 증가하는 숫자들이 오프셋이다.
- 오프셋을 통해 메시지들의 순서를 보장하고 컨슈머에서는 마지막까지 읽은 위치를 알 수 있다.

<br/>

### 3.2.6 고가용성 보장
- 카프카는 분산 시스템이기 때문에 안정적인 서비스가 가능하다.
- 고가용성을 보장하기 위해 리플리케이션 기능을 제공한다.
- 카프카에서 제공하는 리플리케이션은 **토픽의 파티션을 복제**하는 것이다.
- 원본과 리플리케이션을 구분하기 위해 리더(leader)와 팔로워(follower)라고 부른다.

<br/>

| 리플리케이션 팩터수 | 리더 수 | 팔로워 수 |
|----------------|-------|---------|
|     2          |   1   |   1     |
|     3          |   1   |   2     |
|     4          |   1   |   3     |

🔼 리플리케이션 팩터 수에 따른 리더와 팔로워 수
- 리플리케이션 팩터 수에 따른 리더와 팔로워 수의 관계
- 리더의 수는 1을 유지한 채 팔로워 수만 증가한다.
- 일반적으로 카프카에서는 리플리케이션 팩터 수를 3으로 구성하도록 권장한다.
- 리더는 프로듀서/컨슈머로부터 오는 모든 읽기/쓰기 요청을 처리하고, 팔로워는 오직 리더로부터 리플리케이션을 한다.

<br/>

### 3.2.7 주키퍼의 의존성
- 많은 분산 애플리케이션에서 코디네이터 역할을 하는 애플리케이션으로 사용된다.
- 주키퍼는 여러 대의 서버를 앙상블(클러스터)로 구성하고, 살아 있는 노드 수가 과반수 이상 유지된다면 지속적인 서비스가 가능한 구조이다.
- 주키퍼는 반드시 홀수로 구성해야 한다.
- 지노드(znode)를 이용해 카프카의 메타 정보가 주키퍼에 기록되고, 이를 통해 브로커의 노드 관리, 토픽 관리, 컨트롤러 관리 등 중요한 역할을 수행한다.
- 최근 카프카에서 주키퍼에 대한 의존성을 제거하려는 움직임이 진행 중이다.

<br/>

---

## 3.3 프로듀서의 기본 동작과 예제 맛보기
- 프로듀서는 카프카의 토픽으로 메시지를 전송하는 역할을 담당한다.
- 프로듀서는 여러 옵션을 제공하여, 원하는 형태에 따라 옵션을 변경하면서 다양한 방법으로 카프카로 메시지를 전송할 수 있다.

<br/>

### 3.3.1 프로듀서 디자인
- 프로듀서가 어떻게 디자인되어 있는지 살펴본다.

<br/>

<img width="550" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/314d3216-b3ae-4ceb-886d-8475b20b5324" />

🔼 프로듀서 디자인
- ProducerRecord 부분은 카프카로 전송하기 위한 실제 데이터로, 토픽, 파티션, 키, 밸류로 구성된다.
- 레코드에서 토픽/밸류(메시지 내용)은 필수값이며, 파티션/키는 선택사항(옵션)이다.
- 각 레코드들은 프로듀서의 send() 메서드를 통해 시리얼라이저(serializer), 파티셔너(partitioner)를 거치게 된다.
- 파티션을 지정했다면, 지정된 파티션으로 전달되고, 아니면 키를 가지고 파티션을 선택해 레코드를 전달한다. (기본적으로 RR방식 동작)
- 레코드들을 파티션별로 모아두는 이유는, 카프카로 전송하기 전 배치 전송을 하기 위함이다.
  - 전송이 실패하면 재시도 동작이 이뤄진다.
  - 지정된 횟수만큼 재시도가 실패하면 최종 실패를 전달한다.
  - 전송이 성공하면 메타데이터를 리턴한다.

<br/>

### 3.3.2 프로듀서의 주요 옵션
- 프로듀서를 잘 파악하고 다뤄야 좀 더 효율적이고 안정적으로 사용할 수 있다.

<br/>

|프로듀서 옵션|설명|
|---|---|
|bootstrap.servers|클라이언트가 카프카 클러스터에 처음 연결하기 위한 호스트와 포트 정보|
|client.dns.lookup|클라이언트가 하나의 IP와 연결하지 못할 경우 다른 IP로 시도하는 설정|
|acks|프로듀서가 카프카 토픽의 리더 측에 메시지를 요청한 후 요청을 완료하기를 결정하는 옵션|
|buffer.memory|프로듀서가 카프카 서버로 데이터를 보내기 위해 잠시 대기할 수 있는 전체 메모리 바이트|
|compression.type|프로듀서가 메시지 전송 시 선택할 수 있는 압축 타입|
|enable.idempotence|설정을 true로 하는 경우 중복 없는 전송이 가능 <br/>max.in.flight.requests.per.connection은 5 이하<br/>retries는 0 이상<br/>acks는 all|
|max.in.flight.requests.per.connection|하나의 커넥션에서 프로듀서가 최대한 ACK 없이 전송할 수 있는 요청 수|
|retries|일시적인 오류로 인해 전송에 실패한 데이터를 다시 보내는 횟수|
|batch.size|프로듀셔는 동일한 파티션으로 보내는 여러 데이터를 함께 배치로 보내려고 시도하는데, 그 배치 크기|
|linger.ms|배치 형태의 메시지를 보내기 전에 추가적인 메시지를 위해 기다리는 시간<br/>배치 크기에 도달하지 못한 상황에서 제한 시간에 도달했을 때 메시지 전송|
|transactional.id|정확히 한 번 전송을 위해 사용하는 옵션<br/>동일한 TransactionId에 한해 정확히 한 번을 보장|

🔼 주요 프로듀서 옵션

<br/>

### 3.3.3 프로듀서 예제
- 프로듀서의 전송 방법은 3가지 방식으로 크게 나뉜다.
  - 메시지를 보내고 확인하지 않기
  - 동기 전송
  - 비동기 전송

<br/>

```java
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class ProducerFireForgot {
	public static void main(String[] args) {
		Properties props = new Properties(); // 1
		props.put("bootstrap.server", "peter-kafka01.foo.bar:9092,peter-kafka02.foo.bar:9092,peter-kafka03.foo.bar:9092"); // 2
		
		// 3
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

		KafkaProducer<String, String> producer = new KafkaProducer<>(props); // 4
		
		try {
			for (int i = 0; i < 3; i++) {
				ProducerRecord<String, String> record = new ProducerRecord<>(
						"peter-basic01", "Apache Kafka is a distributed streaming platform - " + i); //5
				producer.send(record); // 6
			}
		} catch (Exception e) {
			e.printStackTrace(); // 7
		} finally {
			producer.close(); // 8
		}
	}
}
```
🔼 메시지를 보내고 확인하지 않기 예제 (ProducerFireForgot.java)
1. Properties 객체 생성
2. 브로커 리스트 정의
3. 메시지 키와 밸류는 문자열 타입이므로 카프카의 기본 StringSerializer를 지정
4. Properties 객체를 전달해 새 프로듀서 생성
5. ProducerRecord 객체 생성
6. send() 메서드를 사용해 메시지를 전송한 후 자바 Future 객체로 RecordMetadata를 리턴받지만, 리턴값을 무시하므로 메시지가 성공적으로 전송됐는지 알 수 없음
7. 카프카 브로커에게 메시지를 전송한 후의 에러는 무시하지만, 전송 전에 에러가 발생하면 예외를 처리할 수 있음
8. 프로듀서 종료

<br/>

- 실제 운영에서 사용하는 것은 추천하지 않는다.
- 하지만 대부분은 성공적으로 메시지가 전송된다.

<br/>

```java
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public class ProducerSync {
	public static void main(String[] args) {
		Properties props = new Properties(); // 1
		props.put("bootstrap.server", "peter-kafka01.foo.bar:9092,peter-kafka02.foo.bar:9092,peter-kafka03.foo.bar:9092"); // 2

		// 3
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

		KafkaProducer<String, String> producer = new KafkaProducer<>(props); // 4

		try {
			for (int i = 0; i < 3; i++) {
				ProducerRecord<String, String> record = new ProducerRecord<>(
						"peter-basic01", "Apache Kafka is a distributed streaming platform - " + i); //5
				
				RecordMetadata metadata = producer.send(record).get();// 6

				System.out.println("Topic: " + metadata.topic()
						+ ", Partition: " + metadata.partition()
						+ ", Offset: " + metadata.offset()
						+ ", Key: " + record.key()
						+ ", Received Message: " + record.value());
			}
		} catch (Exception e) {
			e.printStackTrace(); // 7
		} finally {
			producer.close(); // 8
		}
	}
}
```
🔼 동기 전송(ProducerSync.java)
1. Properties 객체 생성
2. 브로커 리스트 정의
3. 메시지 키와 밸류는 문자열 타입이므로 카프카의 기본 StringSerializer를 지정
4. Properties 객체를 전달해 새 프로듀서 생성
5. ProducerRecord 객체 생성
6. get() 메서드를 이용해 카프카의 응답을 기다림. 메시지가 성공적으로 전송되지 않으면 예외가 발생하고, 에러가 없다면 RecordMetadata를 얻음
7. 카프카로 메시지를 보내기 전과 보내는 동안 에러가 발생하면 예외가 발생함
8. 프로듀서 종료

<br/>

- ProducerRecord 전송이 성공하고 나면 RecordMetadata를 읽어 들여 파티션과 오프셋 정보를 확인할 수 있다.
- 메시지 전달의 성공 여부를 파악할 수 있다.
- 신뢰성 있는 메시지 전달 과정의 핵심이다.

<br/>

```java
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public class PeterProducerCallback implements Callback { // 1

	private ProducerRecord<String, String> record;

	public PeterProducerCallback(ProducerRecord<String, String> record) {
		this.record = record;
	}

	@Override
	public void onCompletion(RecordMetadata metadata, Exception exception) {
		if (exception != null) {
			exception.printStackTrace(); // 2
		} else {
			System.out.println("Topic: " + metadata.topic()
					+ ", Partition: " + metadata.partition()
					+ ", Offset: " + metadata.offset()
					+ ", Key: " + record.key()
					+ ", Received Message: " + record.value());
		}
	}
}
```
🔼 콜백 예제(PeterProducerCallback.java)
1. 콜백을 사용하기 위해 org.apache.kakfa.clients.producer.Callback을 구현하는 클래스가 필요함
2. 카프카가 오류를 리턴하면 onCompletion()은 예외를 갖게 되며, 실제 운영 환경에서는 추가적인 예외 처리가 필요함

<br/>

```java
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class ProducerAsync {
	public static void main(String[] args) {
		Properties props = new Properties(); // 1
		props.put("bootstrap.server", "peter-kafka01.foo.bar:9092,peter-kafka02.foo.bar:9092,peter-kafka03.foo.bar:9092"); // 2

		// 3
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

		KafkaProducer<String, String> producer = new KafkaProducer<>(props); // 4

		try {
			for (int i = 0; i < 3; i++) {
				ProducerRecord<String, String> record = new ProducerRecord<>(
						"peter-basic01", "Apache Kafka is a distributed streaming platform - " + i); //5

				producer.send(record, new PeterProducerCallback(record));// 6
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			producer.close(); // 7
		}
	}
}
```
🔼 비동기 전송(ProducerAsync.java)
1. Properties 객체 생성
2. 브로커 리스트 정의
3. 메시지 키와 밸류는 문자열 타입이므로 카프카의 기본 StringSerializer를 지정
4. Properties 객체를 전달해 새 프로듀서 생성
5. ProducerRecord 객체 생성
6. 프로듀서에서 레코드를 보낼 때 콜백 객체를 같이 보냄
7. 프로듀서 종료

<br/>

- 프로듀서는 send() 메서드와 콜백을 함께 호출한다.
- 비동기 방식으로 전송하면 빠른 전송이 가능하고, 메시지 전송이 실패한 경우라도 예외를 처리할 수 있어서 이후 에러 로그 등에 기록할 수도 있다.

<br/>

---

## 3.4 컨슈머의 기본 동작과 예제 맛보기
