# 프로듀서의 내부 동작 원리와 구현

프로듀서의 기본 역할은 소스에 있는 메시지들을 카프카의 토픽으로 전송하는 것

프로듀서가 전송하는 메시지들은 프로듀서의 `send()` 메서드를 통해 시리얼라이저, 파ㅣ셔너를 거쳐 카프카로 전송됨

## 5.1 파티셔너

카프카의 토픽은 성능 향상을 위한 병렬  처리가 가능하도록 하기 위해 파티션으로 나뉘고 최소 1 또는 2 이상의 파티션으로 구성됨

프로듀서가 카프카로 전송한 메시지는 해당 토픽 내 각 파티션의 로그 세그먼트에 저장됨

프로듀서는 토픽으로 메시지를 보낼 때 해당 토픽의 어느 파티션으로 보낼지 결정하는 것이 **파티셔너**

메시지의 키를 해시처리하여 파티션을 구하는 알고리즘을 사용함

키값이 같다? 같은 파티션으로 전송

예상치 못하게 많은 양이 인입될때 처리하기 위해 토픽의 파티션을 늘릴 수 있는 기능을 제공함

![5-1_파티션수증가에따른해시변경방식](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch5/5-1_파티션수증가에따른해시변경방식.png)

메시지의 키를 이용해 카프카로 메시지를 전송할 때 의도와는 다른 방식이 일어날 수 있으니 파티션 수는 변경하지 말자

### 5.1.1 라운드 로빈 전략

![5-2_라운드로빈전략](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch5/5-2라운드로진번략.png)

키값을 지정하지 않으면 키값은 null, 기본값인 라운드 로빈 알고리즘을 사용해 목적지 토픽의 파티션들로 레코들을 랜덤 전송

즉시 카프카로 전송되지 않고 배치 전송을 위해 프로듀서의 버퍼 메모리에서 잠시 대기함

키값이 null인 레코드들이 총 5개 전송되고 라운드 로빈 전략에 의해 각 파티션에 하나씩 순차적으로 할당

파티션별 최소 레코드 수의 기준을 충족하지 못하면 계속 대기함

파티션별 최소 레코드 수의 기준 없이 매순간 전송하면 비효율적이므로 **스티키 파티셔닝 전략**을 사용

### 5.1.2 스티키 파티셔닝 전략

![5-3_스티키파티셔닝전략](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch5/5-3_스티키파티셔닝전략.png)

하나의 파티션에 레코드 수를 먼저 채워서 카프카로 빠르게 배치 전송하는 전략

스티키 파티셔닝 전략을 적용함으로써 기본 설정에 비해 약 30%이상 지연시간이 감소하고 CPU 사용률도 주는 효과

메시지의 순서가 중요하지 않다면 스티키 파티셔닝 전략을 적용하라

## 5.2 프로듀서의 배치

![5-4_프로두서의배치구성도](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch5/5-4_프로두서의배치구성도.png)

`buffer.memory`: 프로듀서의 버퍼 메모리 옵션, 점선 박스로 둘러싼 부분. 기본값 32MB

`batch.size`: 배치 전송을 위해 메시지들을 묶는 단위를 설정하는 배치 크기 옵션, 토픽A-파티션1 하단의 점선 박스로 둘러싼 부분. 기본값 16KB

`linger.ms`: 배치 전송을 위해 버퍼 메모리에서 대기하는 메시지들의 최대 대기시간을 설정하는 옵션, 기본값 0, 단위는 밀리초

프로듀서의 배치 전송 방식은 단건의 메시지를 전송하는 것이 아닌 한 번에 다량의 메시지를 묶어서 전송하는 방법

배치 전송이 무조건 좋은 것은 아님. 처리량을 높일지, 지연 없는 전송을 해야 할지에 따라 ㄷ다름

처리량을 높이려면 `batch.size`와 `linger.ms`의 값을 크게 설정해야하고, 지연 없는 전송이 목표라면 작게 설정해야함

처리량은 처리하는 작업의 양을 나타냄

지연시간은 작업을 처리하는 데 소요되는 시간을 의미

처리량이 높아지면 지연시간이 길어지고, 처리량이 낮아지면 지연시간은 짧아짐

**높은 처리량을 목표로 배치 전송을 설정하는 경우 버퍼 메모리 크기가 충분히 커야함**

buffet.memory 크기는 반드시 batch.size보다 커야함

buffer.memory의 최소 크기는 파티션 갯수 * batch.size

프로듀서는 전송에 실패하면 재시도를 수행하는데, 이러한 부분을 고려할 때 버퍼 메모리는 `파티션 갯수 * batch.size` 보다 커야함

배치 전송과 압축 기능을 같이 사용하면 효율적으로 카프카로 전송할 수 잇음

높은 압축률을 선호한다면 gzip, zstd, 낮은 지연시간을 선호한다면 lz4, snappy

## 5.3 중복 없는 전송(멱등성 전송)

~~~
멱등성이란?
여러 번 수행하더라도 결과가 달라지지 않는 것을 의미
~~~

다양한 전송 방식

1. 적어도 한 번 전송, at-least-once
2. 최대 한 번 전송, at-most-once
3. 정확히 한 번 전송, exactly-once

![5-5_적어도한번전송](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch5/5-5_적어도한번전송.png)

1. 프로듀서가 브로커의 특정 토픽으로 메시지A 전송
2. 브로커는 메시지A를 기록하고 잘 받았다는 ACK를 프로듀서에게 응답
3. 브로커의 ACK를 받은 프로듀서는 다음 메시지인 메시지B를 브로커에게 전송
4. 브로커는 메시지B를 기록하고 ACK를 전송하려는데 실패함
5. 메시지B를 전송했지만 ACK를 받지 못했으므로 메시지B를 재전송

메시지B는 잘 받았지만 ACK 전송을 실패했으므로, 브로커 입장에서 메시지를 못 받은 것인지 ACK를 못 보낸 것인지 알 수 없음

재전송시 메시지B 중복 저장

카프카는 기본적으로 이와 같은 적어도 한 번 전송 방식을 기반으로 동작

## 최대 한 번 전송

![5-6_최대한번전송과정](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch5/5-6_최대한번전송과정.png)

1. 프로듀서가 브로커의 특정 포틱으로 메시지A 전송
2. 브로커는 메시지A를 기록하고 잘 받았다는 ACK를 프로듀서에게 응답
3. 브로커의 ACK를 받은 프로듀서는 다음 메시지인 메시지B를 브로커에게 전송 
4. 브로커는 메시지B를 기록하고 ACK를 전송하려는데 실패함
5. 프로듀서는 브로커가 메시지B를 받았다고 가정하고 메시지C를 전송

ACK를 받지 못해도 재전송을 하지 않음

일부 메시지의 손실을 감안하더라도 중복 전송은 하지 않음

높은 처리량을 필요로하는 대량의 로그 수집이나 IoT 환경에서 사용함

## 중복 없는 전송
![5-7_중복없는전송](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch5/5-7_중복없는전송.png)

1. 프로듀서가 브로커의 특정 토픽으로 메시지A 전송, 이때 PID 0과 메시지 번호 0을 헤더에 포함해 전송
2. 브로커는 메시지A를 저장, PID와 메시지 번호 0을 메모리에 기록, 그 후 ACK를 프로듀서에게 응답
3. 프로듀서는 다음 메시지인 메시지B를 브로커에게 전송. PID는 동일하게 0, 메시지 번호는 1증가하여 1
4. 브로커는 메시지B를 저장하고, PID와 메시지 번호 1을 메모리에 기록. ACK 전송 실패
5. 메시지B 재전송

재전송을 받아도 PID와 메시지 번호를 비교하여 중복 저장을 피함

메시지 번호를 시퀀스 번호라고도 함

PID는 프로듀서에 의해 자동 생성됨

프로듀서가 보낸 메시지의 시퀀스 번호가 브로커가 갖고 있는 시퀀스 번호보다 정확하게 하나 큰 경우가 아니라면 브로커는 프로듀서의 메시지를 저장하지 않음

PID와 시퀀스 번호는 브로커의 메모리에 유지, 리플리케이션 로그에도 저장됨

따라서 리더가 변경되도 PID와 시퀀스 번호를 알 수 있으므로 중복 없는 메시지 전송이 가능함

중복 없는 전송을 적용한 후 기존 대비 최대 약 20% 정도만 성능이 감소함

![5-8_프로듀서설정](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch5/5-8_프로듀서설정.png)

```yaml
enable.idempotence=true
max.in.flight.request.per.connection=5
retries=5
```
일부러 acks 옵션을 추가하지 않음. 중복 없는 전송을 위해 필수값인 acks 옵션을 추가하면 어떻게 될까?

![5-9_acks](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch5/5-9_acks.png)

전송을 위한 일부 조건이 충족되지 않았기 때문에 ConfigException 발생, 추가함

```yaml
enable.idempotence=true
max.in.flight.request.per.connection=5
retries=5
acks=all
```

![5-10_PID](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch5/5-10_PID.png)

PID와 시퀀스 번호, 오프셋 정보 등이 보임

## 5.4 정확히 한 번 전송

중복 없는 전송이 정확히 한 번 전송하는 것은 아님

중복 없는 전송은 정확히 한 번 전송의 일부 기능

정확히 한 번 처리를 담당하기 위해 트랜잭션 API를 사용

### 5.4.1 디자인

프로듀서가 카프카로 전확힣 한 번 방식으로 전송할 때 메시지들은 원자적으로 처리되어 전송에 성공 혹은 실패함

카프카에는 컨슈머 그룹 코디네이터와 동일한 개념으로 트랜잭션 코디네이터라는 것이 서버 측에 존재

코디네이터의 역할은 프로듀서에 의해 전송된 메시지를 관리하며 커밋 또는 중단 등을 표시

카프카에서는 컨슈머 오프셋 관리를 위해 오프셋 정보를 카프카의 내부 토픽에 저장하는데, 트랜잭션도 동일하게 트랜잭션 로그를 카프카의 내부 토픽인 __transaction_state에 저장함

__transaction_state는 카프카의 내부 토픽이지만 토픽은 토픽이므로 파티션 수와 리플리케이션 팩터 수가 존재하며 브로커의 설정을 통해 관리자가 설정할 수 있음

기본값은 아래와 같음

```yaml
transaction_state.log.num.partitions=50
transaction_state.log.replication.factor=3
```

프로듀서는 트랜잭션 관련 정보를 트랜잭션 코디네이터에게 알리고, 모든 정보 로그는 트랜잭션 코디네이터가 직접 기록함

### 5.4.2 프로듀서 예제 코드

```java
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import java.util.Properties;

public class ExactlyOnceProducer {
    public static void main(String[] args) {
        String bootstrapServers = "peter-kafka01.foo.bar:9092";
        Properties props = new Properties ();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        
        /* 정확히 한 번 전송을 위한 설정 시작 */
        props.setProperty(ProducerConfig. ENABLE_IDEMPOTENCE_CONFIG, "true");
        props.setProperty(ProducerConfig.ACKS_CONFIG, "all");

        props.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");
        props.setProperty(ProducerConfig.RETRIES_CONFIG, "5");
        props.setProperty(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "peter-transaction-01");
        /* 정확히 한 번 전송을 위한 설정 끝 */
        
        Producer<String, String> producer = new KafkaProducer (props);

        // 프로듀서 트랜잭션 초기화
        producer.initTransactions();
        // 프로듀서 트랜잭션 시작
        producer.beginTransaction();

        try {
            for (int i = 0; i < 1; i++) {
                ProducerRecord<String, String> record = new ProducerRecord<>("peter- test05",
                        "Apache Kafka is a distributed streaming platform - " + i);
                producer.send(record);
                producer.flush();
                System.out.println("Message sent successfully");
            }
        } catch (Exception e){
        // 프로듀서 트랜잭션 중단
        producer.abortTransaction();
        e.printStackTrace();
        } finally {
        // 프로듀서 트랜잭션 커밋
         producer.commitTransaction();
         producer.close();
        }
    }
}
```

중복 없는 전송과 정확히 한 번 전송아ㅢ 옵션 설정에서 가장 큰 차이점의자 주의해야할 설정은 `TRANSACTIONAL_ID_CONFIG`

프로듀서의 `TRANSACTIONAL_ID_CONFIG` 옵션은 실행하는 프로듀서 프로세스마다 고유한 ID로 설저앻야함

### 5.4.3 단계별 동작

![5-11_단계별동작](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch5/5-11_단계별동작.png)

트랜잭션 API를 이용하므로 가장 먼저 트랜잭션 코디네이터를 찾음

프로듀서는 브로커에게 FindCoordinator Request를 보내 트랜잭션 코디네이터의 위치를 찾음

컨슈머 코디네이터와 유사한 역할을 하는 트랜잭션 코디네이터는 브로커에 위치

트랜잭션 코디네이터의 주 역할은 PID와 transaction.id를 매핑하고 해당 트랜잭션 전체를 관리

만약 트랜잭션 코디네이터가 존재하지 않는다면 신규 트랜잭션 코디네이터가 생성됨

__transaction_state 토픽의 파티션 번호는 transaction.id를 기반으로 해시하여 결정됨

파티션의 리더가 있는 브로커가 트랜잭션 코디네이터의 브로커로 최종 선정됨

transaction.id가 정확히 하나의 코디네이터만 갖고 있는 다는 것을 의미함

![5-12_프로듀서초기화](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch5/5-12_프로듀서초기화.png)

프로듀서는 initTransactions() 메서드를 이용해 트랜잭션 전송을 위한 InitPidRequest를 트랜잭션 코디네이터로 보냄

TIP(transaction.id)가 설정된 경우에는 initPidRequest와 함께 TID가 트랜잭션 코디네이터에게 전송

트랜잭션 코디네이터는 TID, PID를 매핑하고 해당 정보를 트랜잭션 로그에 기록

PID와 이전 에포크에 대한 쓰기 요청은 무시, 에포크를 활용하는 이유는 신뢰성 있는 메시지 전송을 하기 위함

![5-13_트랜잭션시작](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch5/5-13_트랜잭션시작.png)

트랜잭션 시작 동작

프로듀서는 begindTransaction() 메서드를 이용해 새로운 트랜잭션의 시작을 알림

프로듀서는 내부적으로 트랜잭션이 시작됐음을 기록하지만, 트랜잭션 코디네이터 관점에서는 첫 번째 레코드가 전송될 떄까지 트랜잭션이 시작된 겂은 아님

![5-14_트랜잭션상태추가](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch5/5-14_트랜잭션상태추가.png)

트랜잭션 코디네이터는 전체 트랜잭션을 관리

프로듀서는 토픽 파티션 정보를 트랜잭션 코디네이터에게 전달, 트랜잭션 코디네이터는 해당 정보를 트랜잭션 로그에 기록함

트랜잭션의 현재 상태를 Ongoing으로 표시

기본값으로 1분 동안 트랜잭션 상태에 대한 업데이트가 없으면 해당 트랜잭션은 실패함

![5-15_메시지전송](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch5/5-15_메시지전송.png)

메시지 전송 동작

트랜잭션 코디네이터가 있는 브로커와 프로듀서가 전송하는 메시지를 받는 브로커가 서로 다르기 때문에 브로커는 2개 존재

![5-16_트랜잭션종료요청](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch5/5-16_트랜잭션종료요청.png)

트랜잭션 종료 요청

메시지 전송이 완료한 프로듀서는 commit Transaction() 메서드 또는 abortTransaction() 메서드 중 하나를 반드시 호출해야함

해당 메서드의 호출을 통해 트랜잭션이 완료됨을 트랜잭션 코디네이터에게 알림

트랜잭션 코디네이터는 두 단계 커밋 과정을 시작하게 되며 첫 번째 단계로 트랜잭션 로그에 해당 트랜잭션에 대한 PrepareCommit 또는 PrepareAbort를 기록

![5-17_사용자토픽에표시요청](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch5/5-17_사용자토픽에표시요청.png)

사용자가 토픽에 표시하는 단계

트랜잭션 코디네이터는 두 번째 단계로서 트랜잭션 로그에 기록된 토픽의 파티션에 트랜잭션 커밋 표시를 ㅣㄱ록함

여기서 기록하는 메시지가 컨트롤 메시지

메시지는 해당 PID의 메시지가 제대로 전송했는지 여부를 컨슈머에게 나타내는 용도로도 사용됨

트랜잭션이 커밋이 끝나지 않은 메시지는 컨슈머에게 반환하지 않으며 오프셋의 순서 보장을 위해 트랜잭션 성공 또는 실패를 나타내는 LSO라는 오프셋을 유지

![5-18_트랜잭션완료](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch5/5-18_트랜잭션완료.png)

마지막 단계인 트랜잭션 완료 단계

트랜잭션 코디네이터는 완료됨이라고 트랜잭션 로그에 기록함

프로듀서에게 해당 트랜잭션이 완료됨을 알린 다음 해당 트랜잭션에 대한 처리는 모두 마무리됨

트랜잭션을 이용하는 컨슈머는 read_committed 설정을 하면 트랜잭션에 성공한 메시지들만 읽을 수 있게 됨