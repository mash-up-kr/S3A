# 5장 프로듀서의 내부 동작 원리와 구현
## 5.1 파티셔너(partitioner)
- 프로듀서는 토픽으로 메시지를 보낼 때 해당 토픽의 어느 파티션으로 메시지를 보내야 할 지 결정한다. (이때 파티셔너 사용)
- 메시지의 키값이 동일하면 해당 메시지들은 모두 같은 파티션으로 전송된다. (키값을 해시처리)
- 메시지의 키를 이용해 카프카로 전송하는 경우, 관리자의 의도와 다른 방식으로 전송이 이뤄질 수 있으므로 되도록 파티션 수를 변경하지 않는 것이 권장된다.

<br/>

### 5.1.1 라운드 로빈(round-robin) 전략
- 키값을 지정하지 않으면, 라운드 로빈 알고리즘(기본값)을 사용해 토픽의 파티션들로 레코드들이 전송된다.
- 파티셔너를 거친 후의 레코드들은 배치 처리를 위해 프로듀서의 버퍼 메모리 영역에서 잠시 대기 후 전송되는데, 효율이 저하될 수 있다.

<br/>

<img width="688" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/3c068984-e5eb-4c14-b2df-7841b50d2c2f">

🔼 키값이 null인 라운드 로빈 전략
- 라운드 로빈 전략을 사용하는 프로듀서 내부의 처리 과정을 보여준다.
- 토픽A는 3개의 파티션으로 구성, 각 파티션별로 배치 전송을 위해 필요한 레코드 수는 3이다.
- 키값이 null인 레코드들이 총 5개 전송되고, 라운드 로빈 전략에 의해 각 파티션에 하나씩 순차적으로 할당된다.
  - 배치 전송을 위한 최소 레코드 수(3)을 충족하지 못했으므로 프로듀서의 버퍼 메모리에서 대기
  - 특정 시간을 초과하면 전송하도록 설정할 수 있지만, 토픽A-파티션2와 같이 배치, 압축의 효과 없이 레코드 하나만 전송되는 것은 비효율
  - 비효율 전송을 보완하기 위해 스티키 파티셔닝 전략 공개
 
<br/>

### 5.1.2 스티키 파티셔닝(sticky partitioning) 전략
- 라운드 로빈 전략에서 지연시간이 불필요하게 증가되는 비효율적인 전송을 개선한다.
- 하나의 파티션에 레코드 수를 먼저 채워서 카프카로 빠르게 배치 전송한다.

<br/>

<img width="700" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/258d81de-c533-4f25-8603-5c1db660f3eb">

🔼 스티키 파티셔닝 전략
- 토픽의 파티션 수는 3, 배치를 위한 최소 레코드 수는 3이다.
- 파티셔너는 배치를 위한 레코드 수에 도달할 때까지 동일한 파티션으로 레코드를 담아놓는다.
- 토픽A-파티션0에 최소 레코드 수를 충족했으므로 즉시 카프카로 배치 전송이 수행된다.
- 👍 기본 설정에 비해 약 30% 이상 지연시간이 감소했다.
- 👍 프로듀서의 CPU 사용률이 감소했다.
- 메시지의 순서가 크게 중요하지 않다면 해당 전략을 사용할 것이 권장된다.

<br/>

## 5.2 프로듀서의 배치
- 프로듀서에서는 카프카로 전송하기 전, 배치(batch) 전송을 위해 토픽의 파티션별로 레코드들을 잠시 보관한다.
- 배치 전송을 위해 옵션들을 제공한다.
  - buffer.memory: 카프카로 메시지를 전송하기 위해 담아두는 프로듀서의 버퍼 메모리 옵션 (기본값 32MB)
  - batch.size: 배치 전송을 위해 레코드들을 묶는 단위를 설정하는 배치 크기 옵션 (기본값 16KB)
  - linger.ms: 배치 전송을 위해 버퍼 메모리에서 대기하는 레코드들의 최대 대기시간을 설정하는 옵션 (단위 ms, 기본값 0)
- 처리량을 높이려면 batch.size와 linger.ms의 값을 크게 설정하고, 지연 없는 전송이 목표라면 batch.size와 linger.ms의 값을 작게 설정한다.
- **높은 처리량**을 목표로 할 경우, 버퍼 메모리 크기가 충분히 커야 한다. (buffer.memory > batch.size)

<br/>

## 5.3 중복 없는 전송
- 중복 없이 전송할 수 있는 기능을 제공한다.

<br/>

### 적어도 한 번 전송

<img width="437" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/e4447460-1ba9-4081-b19a-16f9ef0a72a3">

🔼 적어도 한 번 전송 과정
1. 프로듀서가 브로커의 특정 토픽으로 메시지A를 전송한다.
2. 브로커는 메시지A를 기록하고, 잘 받았다는 ACK를 프로듀서에게 응답한다.
3. 브로커의 ACK를 받은 프로듀서는 다음 메시지인 메시지B를 브로커에게 전송한다.
4. 브로커는 메시지B를 기록하고, ACK를 프로듀서에게 전송하려고 한다. 이때 어떤 장애가 발생하여 프로듀서는 메시지B에 대한 ACK를 받지 못한다.
5. 메시지B를 전송한 후 브로커로부터 ACK를 받지 못한 프로듀서는 브로커가 메시지B를 받지 못했다고 판단해 메시지B를 재전송한다.

<br/>

- 메시지B를 브로커에 기록한 후 ACK를 보내지 못했기 때문에, 메시지B는 중복 전송된다. (4-5)
- 최소한 하나의 메시지는 반드시 보장한다는 것이 해당 방식이다.
- 카프카는 기본적으로 적어도 한 번 전송 방식을 기반으로 동작한다.

<br/>

### 최대 한 번 전송

<img width="426" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/c55cf454-ae7d-42f7-a3d4-22d2573e1cb9">

🔼 최대 한 번 전송 과정
1. 프로듀서가 브로커의 특정 토픽으로 메시지A를 전송한다.
2. 브로커는 메시지A를 기록하고, 잘 받았다는 ACK를 프로듀서에게 응답한다.
3. 프로듀서는 다음 메시지인 메시지B를 브로커에게 전송한다.
4. 브로커는 메시지B를 기록하지 못하고, ACK를 프로듀서에게 전송하지 못한다.
5. 프로듀서는 브로커가 메시지B를 받았다고 가정하고, 다음 메시지C를 전송한다.

<br/>

- ACK를 받지 못하더라도 재전송을 하지 않는다. (실제 ACK 응답 과정은 필요 X)
- 메시지 손실 가능성은 있지만 메시지 중복 가능성은 없다.

<br/>

### 중복 없는 전송 과정

<img width="424" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/0aa632a9-1e20-49bd-b6dc-1780829eb687">

🔼 중복 없는 전송 과정
1. 프로듀서가 브로커의 특정 토픽으로 메시지A를 전송한다. 이때 PID 0과 메시지 번호 0을 헤더에 포함해 함께 전송한다.
2. 브로커는 메시지A를 저장하고, PID와 메시지 번호(0)를 메모리에 기록한다. 이후 프로듀서에게 ACK로 응답한다.
3. 프로듀서는 다음 메시지B를 브로커에게 전송한다. PID는 동일하게 0이고, 메시지 번호는 1(0에서 증가)이다.
4. 브로커는 메시지B를 저장하고, PID와 메시지 번호(1)를 기록한다. 이후 장애가 발생하여 ACK로 응답하지 못한다.
5. 브로커로부터 ACK를 받지 못한 프로듀서는 브로커가 메시지B를 받지 못했다고 판단해 메시지B를 재전송한다.

<br/>

- 브로커는 PID(0)와 메시지 번호(1)를 비교해서, 이미 기록되어 있으면 ACK만 응답한다. (중복 해결 ⭐️)
- 메시지 번호를 시퀀스 번호라고도 한다.
- PID는 프로듀서에 의해 자동 생성된다.
- 프로듀서가 보낸 메시지의 시퀀스 번호가 브로커가 갖고 있는 시퀀스 번호보다 정확하게 하나 큰 경우가 아니라면, 브로커는 프로듀서의 메시지를 저장하지 않는다.
- 리더가 변경되더라도 새로운 리더가 PID와 시퀀스 번호를 정확히 알 수 있으므로 중복 발생을 막을 수 있다. (PID/시퀀스 번호 브로커의 메모리에 유지, 리플리케이션 로그에 저장)
- 기존 대비 약 20% 정도만 성능이 감소했다.

<br/>

|프로듀서 옵션|값|설명|
|---|---|---|
|enable.idempotence|true|프로듀서가 중복 없는 전송을 허용할지 결정하는 옵션|
|max.in.flight.requests.per.connection|1 ~ 5|ACK를 받지 않은 상태에서 하나의 커넥션에서 보낼 수 있는 최대 요청 수|
|acks|all|프로듀서 acks와 관련된 옵션|
|retries|5|ACK를 받지 못한 경우 재시도 해야 하므로 0보다 큰 값으로 설정|

🔼 중복 없는 전송을 위한 프로듀서 설정

<br/>

```
$ vi /home/ec2-user/producer.config
```
🔼 프로듀서의 설정 파일(producer.config) 생성
- 토픽(peter-test04)은 미리 생성해야 한다. (파티션 수 1, 리플리케이션 팩터 수 3)

<br/>

```
enable.idempotence=true
max.in.flight.requests.per.connection=5
retries=5
```
🔼 중복 없는 전송을 위한 producer.config 파일
- acks 옵션은 제외되었다. (추가하지 않을 때의 상황을 알아보기 위함)

<br/>

```
$ /usr/local/kafka/bin/kafka-console-producer.sh --bootstrap-server peter-kafka01.foo.bar:9092 --topic peter-test04 --producer.config /home/ec2-user/producer.config
```
- 생성한 프로듀서 설정 파일을 로드하여 실행한다.
- acks 옵션이 제외되었으므로 ConfigException 예외가 발생한다.

<br/>

```
enable.idempotence=true
max.in.flight.requests.per.connection=5
retries=5
acks=all
```
🔼 중복 없는 전송을 위해 acks=all을 추가한 producer.config 파일
- 다시 실행해보면 에러 없이 메시지를 입력할 수 있는 커맨드(>)가 나타난다.
- 메시지를 전송한다. (exatly one1)

<br/>

```
$ cd /data/kafka-logs/peter-test04-0/
$ ls
```
- 브로커의 peter-test04 토픽 메시지가 저장된 경로로 이동한 후, 파일 리스트를 확인한다.
- snapshot 파일이 있는지 확인한다. (snapshot: 브로커가 PID와 시퀀스 번호를 주기적으로 저장하는 파일)

<br/>

```
$ /usr/local/kafka/bin/kafka-dump-log.sh --print-data-log --files /data/kafka-logs/peter-test04-0/000000000000001.snapshot
```
- 카프카의 dump 명령어를 통해 snapshot 파일을 확인한다.
- 출력 내용을 보면 PID, 시퀀스 번호(first, last)와 마지막 오프셋 정보 등이 기록된 것을 확인할 수 있다.

<br/>

## 5.4 정확히 한 번 전송
- 트랜잭션과 같은 전체적인 프로세스 처리를 의미한다.
- 중복 없는 전송은 정확히 한 번 전송의 일부 기능이라고 할 수 있다.
- 정확히 한 번 처리를 담당하는 별도의 프로세스가 있는데, 이를 **트랜잭션 API**라고 한다.

<br/>

### 5.4.1 디자인
- 트랜잭션 코디네이터(transaction coordinator)
  - 프로듀서에 의해 전송된 메시지를 관리한다.
  - 커밋 또는 중단 등을 표시한다.
- _transaction_state
  - 카프카의 내부 토픽
  - 트랜잭션 로그를 저장한다.
  - 기본 값
    - transaction.state.log.num.partitions=50
    - transaction.state.log.replication.factor=3
- 컨트롤 메시지
  - 클라이언트들이 메시지들을 식별하기 위한 정보로서 사용된다.
  - 브로커와 클라이언트 통신에서만 사용된다.
 
<br/>

### 5.4.2 프로듀서 예제 코드
```java
package producer;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public class ExactlyOnceProducer {
	public static void main(String[] args) {
		String bootstrapServers = "peter-kafka01.foo.bar:9092";
		Properties props = new Properties();
		props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		
		// 정확히 한 번 전송을 위한 설정
		props.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
		props.setProperty(ProducerConfig.ACKS_CONFIG, "all");
		props.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");
		props.setProperty(ProducerConfig.RETRIES_CONFIG, "5");
		props.setProperty(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "peter-transaction-01");
		
		Producer<String, String> producer = new KafkaProducer<>(props);
		
		producer.initTransactions(); // 프로듀서 트랜잭션 초기화
		producer.beginTransaction(); // 프로듀서 트랜잭션 시작
		
		try {
			for (int i = 0; i < 1; i++) {
				ProducerRecord<String, String> record = new ProducerRecord<>(
						"peter-test05", 
						"Apache Kafka is a distributed streaming platform - " + i);
				producer.send(record);
				producer.flush();
				System.out.println("Message sent successfully");
			}
		} catch (Exception e) {
			producer.abortTransaction(); // 프로듀서 트랜잭션 중단
			producer.close();
		} finally {
			producer.commitTransaction(); // 프로듀서 트랜잭션 커밋
			producer.close();
		}
	}
}
```
🔼 트랜잭션 프로듀서 예제 코드 (ExactlyOnceProducer.java)
- TRANSACTIONAL_ID_CONFIG
  - 중복 없는 전송과 정확히 한 번 전송의 옵션 설정에서 가장 큰 차이점이자 주의해야 할 설정
  - 실행하는 프로듀서 프로세스마다 고유한 아이디로 설정해야 한다. (2개 프로듀서가 있다면 두 프로듀서마다 다른 아이디로 설정)
 
<br/>

### 5.4.3 단계별 동작

<img width="600" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/6d3943bb-6c34-4edf-8623-9cb0c5fb3297">

🔼 트랜잭션 코디네이터 찾기
- 트랜잭션 API를 이용하기 위해 트랜잭션 코디네이터를 찾는다.
  - 브로커에 위치한다.
  - PID와 transaction.id를 매핑하고 해당 트랜잭션 전체를 관리한다.
- 파티션의 리더가 있는 브로커가 트랜잭션 코디네이터의 브로커로 최종 선정된다.
  - _transaction_state 토픽의 파티션 번호는 transaction.id를 기반으로 해시하여 결정된다.
- transaction.id가 정확히 하나의 코디네이터만 갖고 있다.

<br/>

<img width="600" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/7def4ef0-ef20-43ec-af3e-6b7fd33c7019">

🔼 프로듀서 초기화
- 트랜잭션 전송을 위한 InitPidRequest를 트랜잭션 코디네이터로 보낸다. (TID가 설정된 경우 같이 전송)
- 트랜잭션 코디네이터는 TID, PID를 매핑하고 해당 정보를 트랜잭션 로그에 기록한다.
- PID 에포크를 한 단계 올리고, 이전의 동일한 PID와 이전 에포크에 대한 쓰기 요청은 무시된다.

<br/>

<img width="600" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/e3851444-186c-4d50-84a0-fed8dd9a3022">

🔼 트랜잭션 시작
- 프로듀서는 새로운 트랜잭션의 시작을 알린다.
- 트랜잭션의 코디네이터 관점에서 첫 번째 레코드가 전송될 때까지 트랜잭션이 시작된 것은 아니다.

<br/>

<img width="600" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/f36f38f8-7371-462d-99cb-5f052b7a1b75">

🔼 트랜잭선 상태 추가
- 프로듀서는 토픽 파티션 정보를 트랜잭션 코디네이터에게 전달하고, 트랜잭션 코디네이터는 해당 정보를 트랜잭션 로그에 기록한다.
- 트랜잭션의 현재 상태를 Ongoing으로 표시한다.
- 기본값으로 1분 동안 트랜잭션 상태에 대한 업데이트가 없다면, 해당 트랜잭셔은 실패로 처리된다.

<br/>

<img width="600" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/5233b839-2dcb-4b7d-af9b-9d275d361c43">

🔼 메시지 전송
- 프로듀서는 대상 토픽의 파티션으로 메시지를 전송한다.
- P0(파티션0)으로 메시지를 전송했고, 해당 메시지에 PID, 에포크, 시퀀스 번호가 함께 포함되는 상황이다.
- 브로커가 2개 있는 이유는 트랜잭션 코디네이터가 있는 브로커와 프로듀서가 전송하는 메시지를 받는 브로커가 서로 다르기 때문이다.

<br/>

<img width="600" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/91eb1fac-1e54-45ec-9bb9-7eecf8886d58">

🔼 트랜잭션 종료 요청
- 메시지 전송을 완료한 프로듀서는 commitTransaction() 메서드 또는 abortTransaction() 메서드 중 하나를 호출한다.
- 해당 메서드 호출을 통해 트랜잭션이 완료되었음을 트랜잭션 코디네이터에게 알린다.
- 트랜잭션 코디네이터는 **첫번째 단계**로 트랜잭션 로그에 해당 트랜잭션에 대한 PrepareCommit 또는 PrepareAbort를 기록한다.

<br/>

<img width="600" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/e30f6be2-f634-4ed5-adf2-d2b1ebf342c9">

🔼 사용자 토픽에 표시 요청
- 트랜잭션 코디네이터는 **두번째 단계**로 트랜잭션 로그에 기록된 토픽의 파티션에 트랜잭션 커밋 표시를 기록한다.
  - 컨트롤 메시지가 기록한다.
- 트랜잭션 커밋이 끝나지 않은 메시지는 컨슈머에게 반환하지 않으며, 오프셋의 순서 보장을 위해 트랜잭션 성공/실패를 나타내는 LSO(Last Stable Offset)라는 오프셋을 유지한다.

<br/>

<img width="600" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/7ff67fa5-670f-4e9f-8e93-a067f0b28aad">

🔼 트랜잭션 완료
- 트랜잭션 코디네이터는 완료됨(commited)이라고 트랜잭션 로그에 기록한다.
- 프로듀서에게 해당 트랜잭션이 완료됨을 알린 다음 해당 트랜잭션에 대한 처리는 모두 마무리된다.

<br/>

### 5.4.4 예제 실습

```
$ /usr/local/kafka/bin/kafka-topics.sh --bootstrap-server peter-kafka01.foo.bar:9092 --create --topic peter-test05 --partitions 1 --replication-factor 3
```
- 토픽(peter-test05)을 생성한다.
- 파티션 수는 1, 리플리케이션 팩터 수는 3이다.

<br/>

```
$ cd ${ExactlyOnceProducer.jar 경로}
$ java -jar ExactlyOnceProducer.jar
```
- 위에서 작성한 ExactlyOnceProducer 파일을 실행한다. (메시지 전송)

<br/>

```
[main] INFO ...ProducerConfig - ProducerConfig values:
...생략...
Message send successfully
...생략...
```
- 출력 결과에서 프로듀서의 옵션 정보와 함께 `Message send successfully`라는 메시지 내용을 확인할 수 있다.

<br/>

```
$ /usr/local/kafka/bin/kafka-topics.sh --bootstrap-server peter-kafka01.foo.bar:9092 --list
```
- 카프카의 전체 토픽 리스트를 확인한다.

<br/>

```
_consumer_offsets
_transaction_state
peter-test01
...생략...
peter-test05
```
- 출력 결과에서 토픽 리스트 중 `_transaction_state`라는 토픽을 확인할 수 있다.
  - 트랜잭션 로그를 기록하는 카프카의 내부 토픽

<br/>

![image](https://github.com/mash-up-kr/S3A/assets/55437339/ae2775f5-3077-470e-bb1c-81ee4754d134)

🔼 _transaction_state 토픽 내용
1. 정확히 한 번 단계별 동작 중 **트랜잭션 초기화**에 해당하는 로그
2. 정확히 한 번 단계별 동작 중 **상태 표시 및 메시지 전송**에 해당하는 로그
   - `state=Ongoing`을 통해 트랜잭션이 시작되었음을 알 수 있다.
3. 정확히 한 번 단계별 동작 중 **트랜잭션 종료 요청**에 해당하는 로그
   - `state=PrepareCommit`으로 변경됐다. (트랜잭션 코디네이터의 1단계 커밋)
4. 정확히 한 번 단계별 동작 중 **트랜잭션 완료**에 해당하는 로그
   - `state=CompleteCommit`으로 변경됐다. (트랜잭션 코디네이터의 2단계 커밋)
   - 트랜잭션 단계가 최종적으로 완료됐다.
  
<br/>

![image](https://github.com/mash-up-kr/S3A/assets/55437339/06f9e214-8ce2-40da-ac73-b6a7c7a83185)

🔼 peter-test05 토픽의 로그 파일
1. 트랜잭션 프로듀서가 메시지를 전송한 내용
   - `isTransactional: true`를 통해 해당 메시지가 트랜잭션 메시지임을 알 수 있다.
2. 컨트롤 메시지
   - 사용자 토픽에 트랜잭션 완료 유무 표시와 관련된 내용이다.
   - endTxnMarker:COMMIT 내용을 확인할 수 있다.
   - 트랜잭션 컨슈머의 경우 해당 메시지가 있어야 앞의 메시지를 읽을 수 있다.
