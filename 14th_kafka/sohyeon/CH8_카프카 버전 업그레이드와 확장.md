# 8장 카프카 버전 업그레이드와 확장
- 운영 중인 카프카 클러스터에 미치는 영향을 최소로 줄이면서 버전을 업그레이드하는 과정을 알아본다.

<br/>

## 8.1 카프카 버전 업그레이드를 위한 준비
- 카프카의 버전을 확인해야 한다.
  - (1) 명령어 이용
    ```
    $ /usr/local/kafka/bin/kafka-topics.sh --version
    ```
  - (2) 카프카가 설치된 경로에서 jar 파일 확인
    ```
    $ ls -l /usr/local/kafka/libs/kafka_*
    ```

- 카프카의 상위 버전은 클라이언트들의 하위 호환성을 갖고 있으므로 대부분 클라이언트 이슈는 없다.
- 하지만 서비스가 종료된 경우(스칼라 컨슈머/프로듀서 등)도 있으므로 전체적으로 카프카의 릴리스 노트를 확인해야 한다.
  - [카프카 공식 홈페이지](https://kafka.apache.org/20/documentation/#upgrade_200_notable)
  - 메이저 버전: (Ex) 0.x 또는 1.x에서 2.x 대상으로 업그레이드하는 경우
  - 마이너 버전: (Ex) 2.1에서 2.6 대상으로 업그레이드하는 경우
 
<br/>

## 8.2 주키퍼 의존성이 있는 카프카 롤링 업그레이드
- 실행 중인 카프카가 있다면 삭제하고 2.1 버전으로 재설치한다.
  ```
  $ /usr/local/kafka/bin/kafka-topics.sh --bootstrap-server peter-kafka01.foo.bar:9092 --delete --topic peter-test06
  $ sudo systemctl stop kafka-server
  $ cd chapter2/ansible_playbook
  $ ansible-playbook -i hosts kafka2.1.yml
  ```
  - 토픽 삭제
  - 카프카 종료
  - 배포 서버에 접근하여 설치 경로로 이동
  - 앤서블 명령어를 이용해 peter-kafka01부터 peter-kafka3 서버까지 카프카 설치

- 토픽을 생성한다.
  ```
  $ /usr/local/kafka/bin/kafka-topics.sh --zookeeper peter-zk01.foo.bar --create --topic peter-version2-1 --partitions 1 --replication-factor 3
  ```
  - 카프카 2.1 버전에서 bootstrap-server 옵션을 사용할 수 없으므로 주키퍼를 이용하는 옵션으로 토픽을 생성한다.

- 콘솔 프로듀서를 이용해 메시지를 전송한다.
  ```
  $ /usr/local/kafka/bin/kafka-console-producer.sh --broker-list peter-kafka01.foo.bar:9092 --topic peter-version2-1
  >version2-1-message1
  >version2-1-message2
  >version2-1-message3
  ```
  - 브로커 지정 시 --broker-list 사용 (--bootstrap-server 옵션 사용 불가)

- 콘솔 컨슈머를 이용해 메시지를 잘 가져오는지 확인한다.
  ```
  $ /usr/local/kafka/bin/kafka-console-consumer.sh --bootstrap-server peter-kafka01.foo.bar:9092 --topic peter-version2-1 --from-beginning --group peter-consumer
  ```
  - 동일한 컨슈머 그룹 이름으로 실행하고자 group 옵션을 이용해 컨슈머 그룹 아이디를 지정한다.
  - 명령어를 실행해보면 출력 결과를 통해 메시지를 잘 가져오는 것을 확인할 수 있다.
 
<br/>

### 8.2.1 최신 버전의 카프카 다운로드와 설정
- 업그레이드하고자 하는 대상 카프카 버전을 다운로드해야 한다.
- 카프카 다운로드는 모든 브로커에서 진행해야 한다.
- 2.1 버전의 설정 파일을 2.6 버전 설정 파일 경로로 복사한다.
  ```
  $ sudo cp kafka_2.12-2.1.0/config/server.properties kafka_2.12-2.6.0/config/server.properties
  ```
  - 현재 운영 중인 2.1 버전의 카프카 설정과 새롭게 설치한 2.6 버전의 카프카 설정이 일치해야 한다.

- 카프카 2.6 버전의 설정 파일에 2개 항목을 추가한다.
  ```
  $ sudo vi kafka_2.12-2.6.0/config/server.properties
  ```
  ```
  inter.broker.protocol.version=2.1 # 브로커 간의 내부 통신은 2.1 버전 기반으로 통신한다.
  log.message.format.version=2.1 # 메시지 포맷도 2.1을 유지한다.
  ```
  🔼 2.6 버전 업그레이드 전 설정 파일
  - 브로커 설정 파일에 적용하지 않으면 업그레이드 후 이미 실행 중인 2.1 버전 브로커들과의 통신이 불가능하다.
 
<br/>

### 8.2.2 브로커 버전 업그레이드
- 브로커 버전 업그레이드는 한 대씩 순차적으로 진행한다.
- 카프카가 설치된 디렉토리로 이동 후 카프카를 종료한다.
  ```
  $ cd /usr/local
  $ sudo systemctl stop kafka-server
  ```
  - 카프카 클러스터가 현재 운영 중인 상태라면, 클라이언트는 일시적으로 리더를 찾지 못하는 에러가 발생하거나 타임아웃 등이 발생한다.
  - 카프카 클라이언트 내부적으로 재시도 로직이 있으므로 모든 클라이언트는 변경된 새로운 리더가 있는 브로커를 바라보게 된다. (정상 작동)
 
- 2.1 버전으로 연결되어 있는 kafka 심볼릭 링크를 2.6 버전으로 변경한다.
  ```
  $ sudo rm -rf kafka
  $ sudo ln -sf kafka_2.12-2.6.0 kafka
  $ ll
  ```
  - 출력 내용을 통해 kafka의 링크가 2.6 버전으로 변경됐음을 알 수 있다.
 
- 브로커를 실행한다.
  ```
  $ sudo systemctl start kafka-server
  ```
  <img alt="image" width="500" src="https://github.com/mash-up-kr/S3A/assets/55437339/3a5c777b-4b18-429e-aecb-e026e76af068"/>

  🔼 peter-kafka01만 업그레이드한 상태
  - 설정 파일에서 내부 브로커 프로토콜과 메시지 포맷 버전은 2.1로 설정했으므로, 다른 2.1 버전의 브로커들과 정상적으로 통신할 수 있다.
 
- 브로커 실행 후 토픽 상세보기 명령어를 입력해 리플리케이션과 ISR이 잘 동작하는지 확인한다.
  ```
  $ /usr/local/kafka/bin/kafka-topics.sh --bootstrap-server peter-kafka01.foo.bar:9092 --topic peter-version2-1 --describe
  ```
  - 2.6 버전으로 업그레이드했으므로 bootstrap-server 옵션도 잘 동작한다.
  - 토픽 상세보기를 통해 리플리케이션이 정상이고 ISR도 모두 잘 동작하는 것을 알 수 있다.
  - 이후 2번 브로커, 3번 브로커도 동일한 방법으로 업그레이드를 진행한다.
 
- 2번 브로커의 업그레이드 작업은 다음과 같은 순서로 진행된다.
  1. 2번 브로커 접속
  2. /usr/local 경로로 이동
  3. 브로커 종료
  4. kafka 링크 삭제
  5. kafka 링크 2.6 버전으로 재생성
  6. 설정 파일 복사 및 옵션 설정
  7. 브로커 시작
 
- 2번 브로커 업그레이드 작업 후 3번 브로커도 동일하게 업그레이드 작업을 수행한다.

<br/>

<img alt="image" width="600" src="https://github.com/mash-up-kr/S3A/assets/55437339/69e79c3a-58bd-411a-9b56-0c2f49834ae4" />

🔼 모든 브로커를 업그레이드한 이후의 상태

<br/>

### 8.2.3 브로커 설정 변경
- 2.1 버전으로 통신하게 했던 설정을 제거해서 브로커들이 2.6 버전으로 통신할 수 있도록 변경한다.
- 모든 브로커에 접속하여 프로토콜 버전과 메시지 포맷 버전의 내용을 삭제한다.
  ```
  $ sudo vi /usr/local/kafka/config/server.properties

  inter.broker.protocol.version=2.1 # 삭제
  log.message.format.version=2.1 # 삭제
  ```

- 변경한 설정 내용을 반영하기 위해 브로커를 한 대씩 재시작한다.
  ```
  $ sudo systemctl restart kafka-server
  ```

- 콘솔 프로듀서를 이용해 메시지를 전송한다.
  ```
  $ /usr/local/kafka/bin/kafka-console-producer.sh --bootstrap-server peter-kafka01.foo.bar:9092 --topic peter-version2-1
  > version2-1-message4
  > version2-1-message5
  ```

- 콘솔 컨슈머를 통해 메시지를 가져온다.
  ```
  $ /usr/local/kafka/bin/kafka-console-consumer.sh --bootstrap-server peter-kafka-1.foo.bar:9092 --topic peter-version2-1 --group peter-consumer
  ```
  - 앞서 컨슈머 그룹의 오프셋 위치를 잘 기억하는지 확인하기 위해 위에서 사용했던 peter-consumer 그룹 아이디를 이용해 메시지를 가져온다.
  - 버전 업그레이드 이후 전송한 메시지를 잘 가져오는 것을 출력 결과를 통해 확인할 수 있다.
 
- 콘솔 프로듀서를 이용해 버전 업그레이드하기 전의 메시지도 가져올 수 있는지 확인해본다.
  ```
  $ /usr/local/kafka/bin/kafka-console-consumer.sh --bootstrap-server peter-kafka01.foo.bar:9092 --topic peter-version2-1 --from-beginning
  ```
  - 출력 결과를 통해 버전 업그레이드 전 전송했던 메시지도 컨슈머가 모두 가져온 것을 확인할 수 있다.
 
<br/>

### 8.2.4 업그레이드 작업 시 주의사항
- 예상치 못한 여러 가지 문제를 마주치기 전에 미리 충분한 테스트를 해둬야 한다.
  - 업그레이드 전, 운영 환경과 동일한 카프카 버전으로 개발용 카프카를 구성해보고 카프카의 버전 업그레이드를 수행한다.
 
- 경험이 없는 관리자라면 되도록 카프카의 사용량이 적은 시간대를 골라 업그레이드 작업을 실시하는 것이 권장된다.
  - 카프카 사용량이 적은 시간대에 리플리케이션을 일치시키는 카프카의 내부 동작이 빠르게 이뤄질 것이다.
 
- 그 외 프로듀서의 ack=1 옵션을 사용하는 경우 카프카의 롤링 재시작으로 인해 일부 메시지가 손실될 수도 있다.
  - 옵션에 대해서도 꼼꼼하게 검토한 후 버전 업그레이드를 실시한다.

<br/>

## 8.3 카프카의 확장

## 
