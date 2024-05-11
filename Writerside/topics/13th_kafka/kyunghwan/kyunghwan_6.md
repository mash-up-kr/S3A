# 6. 카프카 운영 가이드

## 6.1 필수 카프카 명령어
- 이런건 변경될 가능성이 좀 있어서 버전별로 찾아봐야 할듯...?
- 토픽 생성
    - bin/kafka-topics.sh --create \
--topic <topic-name> --bootstrap-server <bootstrap-server:port> \
--partition 1 --replication-factor 3
- 토픽 정보
    - bin/kafka-topics.sh --describe \
--topic <topic-name> --bootstrap-server <bootstrap-server:port>
- 토픽 보관 주기 설정
    - bin/kafka-configs --alter \
--bootstrap-server <bootstrap-server:port> \
--entity-type topics --entity-name <topic-name> \
--add-config retention.ms=<retention_in_ms>
- 토픽 보관 주기 삭제
    - bin/kafka-configs --alter \
--bootstrap-server <bootstrap-server:port> \
--entity-type topics --entity-name <topic-name> \
--delete-config retention.ms


## 6.2 스케일 아웃
- 이런 부분도 버전별로 차이가 있을 듯
- 주키퍼 스케일 아웃 : zoo.cfg 서버 정보 추가 후 재시작
- 브로커 스케일 아웃: broker.id 만 겹치지 않게 설정한다.
- 브로커 스케일 아웃 후 kafka-reassign-partitions.sh 이용해 리밸런싱 필요
- 리밸런싱 시 네트워크 사용량 고려하자.

## 6.4 카프카 모니터링
- 주로 카프카 JMX를 사용
- kafka-server-start.sh 에 JMX_PORT=9999 설정해주자.
