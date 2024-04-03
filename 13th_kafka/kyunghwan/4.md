# 4. 카프카 프로듀서

## 4.1 콘솔 프로듀서로 메세지 보내기

- auto.create.topics.enable = true 설정되어 있으면 토픽 자동 생성
- 토픽 생성 커맨드
    - bin/kafka-topics.sh --create \
--topic topic-name --bootstrap-server bootstrap-server:port
- 토픽에 메세지 쓰기
    - bin/kafka-console-producer.sh \
--topic topic-name --bootstrap-server bootstrap-server:port
- 토픽에서 메세지 읽기
    - bin/kafka-console-consumer.sh \
--topic topic-name --bootstrap-server bootstrap-server:port
--partition 0 --from-beginning

## 4.4 프로듀서 주요 옵션
- bootstrap.servers : 클러스터 연결을 위한 정보, 모두 입력하자.
- ack: 0,1,all로 구성, all일시 ISR 모두 기다림
- buffer.memory: 프로듀서 버퍼 메모리 설정
- compression.type: 압축 포맷
- retries : 재시도 횟수
- batch.size : 전송 배치 사이즈
- linger.ms : 배치 전송 대기 시간