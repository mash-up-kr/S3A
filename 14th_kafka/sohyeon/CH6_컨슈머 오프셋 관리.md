# 6장 컨슈머 오프셋 관리
- 컨슈머의 주요 역할은 **카프카에 저장된 메시지를 가져오는 것**이다.

<br/>

## 6.1 컨슈머 오프셋 관리
- 컨슈머가 **메시지를 어디까지 가져왔는지 표시**하는 것은 중요하다.
- 오프셋(offset)
  - 메시지의 위치를 나타내는 위치 (숫자 형태)
  - `_consumer_offsets` 토픽에 각 컨슈머 그룹별로 위치 정보가 기록된다.

<br/>

<img width="700" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/fe56cee7-f6d7-47ed-9f38-478daf4905bf">

🔼 컨슈머 기본 동작
- 컨슈머들은 지정된 토픽의 메시지를 읽은 뒤, 읽어온 위치와 오프셋 정보를 `_consumer_offsets`에 기록한다.
  - 오프셋값: 컨슈머가 다음으로 읽어야 할 위치
  - 1️⃣ 2번 오프셋 메시지 C까지 읽었다.
  - 2️⃣ 그 다음으로 읽어야 할 3번 오프셋 위치를 저장한다.
- 모든 컨슈머 그룹의 정보가 저장되는 `_consumer_offsets` 토픽은 파티션 수와 리플리케이션 팩터 수를 가진다.
  - offsets.topic.num.partitions: 기본값 50
  - offsets.topic.replication.factor: 기본값 3

<br/>

## 6.2 그룹 코디네이터
