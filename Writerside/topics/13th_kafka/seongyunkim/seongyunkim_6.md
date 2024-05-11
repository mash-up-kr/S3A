# 06. 카프카 운영 가이드

각 토픽은 여러 개의 파티션으로 분리되어 있으며 각 브로커에 복제되어 분산되어 저장

## 1. 필수 카프카 명령어

[https://kafka.apache.org/quickstart](https://kafka.apache.org/quickstart)

- 토픽 생성

```bash
$ bin/kafka-topics.sh --create \
--topic <topic-name> --bootstrap-server <bootstrap-server:port> \
--partition 1 --replication-factor 3
```

- 토픽 정보(리플리케이션, ISR, 리더, 팔로워) 확인

```bash
$ bin/kafka-topics.sh --describe \
--topic <topic-name> --bootstrap-server <bootstrap-server:port>
```

- 토픽 설정 변경

```bash
# 토픽 보관 주기 설정
$ bin/kafka-configs --alter \
--bootstrap-server <bootstrap-server:port> \
--entity-type topics --entity-name <topic-name> \
--add-config retention.ms=<retention_in_ms>

# 토픽 보관 주기 설정 삭제
$ bin/kafka-configs --alter \
--bootstrap-server <bootstrap-server:port> \
--entity-type topics --entity-name <topic-name> \
--delete-config retention.ms
```

- 토픽 파티션 수 변경

파티션 수가 변경되면 동일한 key의 레코드가 다른 파티션으로 할당될 수 있음

```bash
$ bin/kafka-topics.sh --alter \
--bootstrap-server <bootstrap-server:port> \
--topic <topic-name> --partitions 2
```

- 토픽 리플리케이션 팩터 변경

브로커 추가 후 파티션 재분배를 위해서도 사용

토픽 크기가 크다면 완료까지 시간이 소요될 수 있음

```json
{
  "version": 1,
  "partitions": [
    {"topic": "<topic-name>", "partition": 0, "replicas" [1,2]},
    {"topic": "<topic-name>", "partition": 1, "replicas" [2,3]}
  ]
}
```

```bash
$ bin/kafka-reassign-partitions.sh \
--bootstrap-server <bootstrap-server:port> \
--reassignment-json-file expand-cluster-reassignment.json --execute
```

- 컨슈머 그룹 리스트 확인

```bash
$ bin/kafka-consumer-groups.sh --list \
--bootstrap-server <bootstrap-server:port>
```

- 컨슈머 상태와 오프셋 확인

LAG: 현재 토픽의 저장된 메세지와 컨슈머가 가져간 메세지의 차이

```bash
$ bin/kafka-consumer-groups.sh --describe \
--bootstrap-server localhost:9092 \
--group <group-name>

TOPIC                          PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG        CONSUMER-ID                                       HOST                           CLIENT-ID
my-topic                       0          2               4               2          consumer-1-029af89c-873c-4751-a720-cefd41a669d6   /127.0.0.1                     consumer-1
my-topic                       1          2               3               1          consumer-1-029af89c-873c-4751-a720-cefd41a669d6   /127.0.0.1                     consumer-1
my-topic                       2          2               3               1          consumer-2-42c1abd4-e3b2-425d-a8bb-e1ea49b29bb2   /127.0.0.1                     consumer-2
```

## 2. 주키퍼 스케일 아웃

- 주키퍼 서버 3대 → 5대
    - 처리량 증가
    - 최대 2대 노드 장애까지 지속적인 서비스 가능 (과반수 법칙)
- 기존 서버의 `zoo.cfg` 에 추가된 서버 정보를 추가하고 1대씩 재시작 필요

## 3. 카프카 스케일 아웃

- 새롭게 추가하는 서버의 설정 파일에 `broker.id` 부분만 겹치지 않게 추가 후 실행
- 브로커가 새로 추가되어도 파티션 재배치는 자동으로 되지 않기 때문에 추가된 브로커는 파티션이 없는 상태
- `kafka-reassign-partitions.sh` 를 사용해 파티션 재배치(분산) 가능
    - 파티션 사이즈가 크면 네트워크 사용량을 급증시키고 브로커에 부담
    - 토픽 사용량이 적은 시간에 수행하거나 토픽의 보관 주기를 줄여 사이즈를 축소시키고 실행 권장

## 4. 카프카 모니터링

- 카프카 JMX (Java Managerment eXtensions)
- 설정 방법: 카프카 실행 파일 (`kafka-server-start.sh`) 또는 환경 변수로 `JMX_PORT=9999` 설정
- 모니터링 지표
    - Message in rate: 브로커 서버로 초당 들어오는 메세지 수
    - Byte in rate: 브로커 서버로 초당 들어오는 사이즈
    - Byte out rate: 브로커 서버로 초당 나가는 사이즈
    - under replicated partitions: 복제가 되지 않고 있는 파티션 수 (0이 아닌 경우 알람)
    - is controller active broker: 클러스터 내 컨트롤러 서버는 1, 아니면 0
    - Partition counts: 브로커에 있는 파티션 수
    - Leader counts: 브로커에 있는 리더 수
    - ISR shirink rate: 브로커가 다운되면 일부 파티션 ISR 축소가 발생하고 해당 비율 (0이 아니면 알람)

## 5. 카프카 매니저 활용

- 카프카 매니저: 웹 GUI 로 토픽 추가, 삭제, 설정 변경 등 카프카 운영 기능 제공

## 6. 카프카 운영에 대한 Q&A

- Q1. 카프카 운영 시 옵션을 변경하려면?

운영 중인 상태에서 옵션을 변경해야 한다면, 옵션 변경 후 브로커 1대씩 재시작해야 변경된 옵션이 적용

- Q2. 디스크 사용량이 높다면?

가장 사용량이 많은 토픽을 찾고 토픽 보관 주기(`log.retention.hours`)를 줄이기

- Q3. 디스크를 추가하려면?

브로커 옵션 설정에 `log.dirs` 옵션에 추가된 디스크 경로를 추가한 후 브로커 재시작

- Q4. OS 점검을 하려면?

클러스터에서 브로커 1대를 제외한 후 OS 점검

클러스터 전체가 다운되지는 않으나, 리더가 변경되면서 일부 커넥션 에러는 발생 가능

- Q5. 토픽에 전송한 메세지가 제대로 들어갔는지 확인하는 방법?

컨슈머로 메세지를 가져와 확인하면 프로그래밍 오류 등으로 메세지를 못가져 올 수 있음

가장 정확하게 확인하는 방법은 콘솔 컨슈머를 이용해 토픽의 메세지를 가져오기

- Q6. 카프카 버전 업그레이드는 어떻게 하나요?

한 대씩 내렸다 올리는 롤링 업그레이드 또는 모든 브로커 종료 후 업그레이드 (그럼 무중단이 아닐텐데..?)

- Q7. 롤링 업그레이드는 어떻게 하나요?

브로커 1 대씩 새 버전을 설치하고 `server.properties` 의 `inter.broker.protocol.version`, `log.message.format.version` 를 현재 카프카 버전으로 추가한 후 재시작

- Q8. 카프카 버전을 업그레이드 할 때 주의할 점?

기존 환경 설정 값이 변경될 수 있으니 릴리스 노트를 확인 (버전 호환성 체크)

- Q9. 카프카 힙 메모리 설정?

5~6G 정도로 설정하고 남은 메모리는 페이키 캐시로 사용하기를 권장

## 참고. 지난주 의문

[카프카 컨슈머 애플리케이션 배포 전략](https://medium.com/11st-pe-techblog/%EC%B9%B4%ED%94%84%EC%B9%B4-%EC%BB%A8%EC%8A%88%EB%A8%B8-%EC%95%A0%ED%94%8C%EB%A6%AC%EC%BC%80%EC%9D%B4%EC%85%98-%EB%B0%B0%ED%8F%AC-%EC%A0%84%EB%9E%B5-4cb2c7550a72)

### max.poll.records 옵션

- `max.poll.records` 옵션이 리밸런싱 stop-the-world 시간에 영향을 주는 원리
    - 컨슈머 추가나 삭제로 파티션 리밸런싱이 일어날 때, 각 컨슈머는 poll() 메소드를 호출해 코디네이터에게 조인을 요청함
    - poll() 메소드 호출 간격이 길어지면 poll()을 요청하지 않은 컨슈머를 기다리느라 파티션 리밸런싱이 완료되는데 까지 걸리는 시간이 길어짐 (stop-the-world)
    - poll() 메소드 호출 간격은 (레코드 하나 처리 시간) X (poll() 메소드로 가져온 레코드 수) 인데, (레코드 하나 처리 시간)은 줄일 수 없기 때문에 `max.poll.records` 를 작게 설정해 poll() 메소드 호출 간격을 줄일 수 있음
- 참고
    - `max.poll.records` 값은 항상 작은 것이 좋은가?
        - 애플리케이션이나 비즈니스 특징에 따라 다름. 준 실시간 작업을 카프카를 이용해 비동기로 처리하여 stop-the-world 시간에 예민하다면 작게 설정하는 것이 좋음
    - `max.poll.records` 값이 작으면 성능에 영향이 없는가?
        - 큰 영향은 없음. `max.poll.records` 값만큼 매번 브로커에서 레코드를 가져오는 것이 아니라, `fetch.max.bytes` 크기 만큼 가져오고 Fetcher 클래스에서 관리. `max.poll.records` 값만큼 Fetcher 에서 레코드를 가져옴.

### 컨슈머 스레드의 분리

- 일반적인 애플리케이션은 poll() 메소드 호출하여 레코드를 가져오고 동일한 스레드에서 레코드를 처리. 전부 처리한 후 다시 poll() 메소드를 호출해서 새로운 레코드를 가지고 옴
- 컨슈머 스레드와 레코드를 처리하는 스레드를 분리(using 스레드 풀)하면, 컨슈머 스레드는 레코드가 처리되기 까지 기다릴 필요가 없으므로, 짧은 간격으로 poll() 메소드 호출 가능
- 단, 같은 파티션에 있는 레코드 처리 순서가 바뀔 수 있음
