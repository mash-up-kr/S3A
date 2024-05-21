# Kafka 7주차

# 카프카 버전 업그레이드와 확장

카프카의 최신 버전이 릴리스 될 때마다 바로 업그레이드 한 다는 것은 말처럼 쉽지 않다. 그렇다고 오래된 카프카 버전을 사용하기에는 오류나 치명적인 버그에 취약할 것이다. 결국 카프카의 버전을 업그레이드 해야 하는 시기가 올 텐데 운영 중인 카프카의 버전을 올릴 때 운영중인 클러스터에 미치는 영향을 최소로 줄이면서 버전을 업그레이드 하는 방법과 주의사항에 대해 알아보자.

# 카프카 버전 업그레이드를 위한 준비

1. 현재 사용중인 카프카의 버전 확인
    1. `[kafka-topics.sh](http://kafka-topics.sh)` 명령어를 이용해 카프카 버전 정보 확인
        
        `/usr/local/kafka/bin/kafka-topics.sh --version`
        
    2. libs 하위 디렉토리에서 kafka의 jar 파일을 이용해 버전 정보 확인
        
        `ls -l /usr/local/kafka/libs/kafka_*`
        
2. 업그레이드 할 카프카 버전 확인
    1. 카프카에서 사용하는 시멘틱 버저닝은 {MAJOR}.{MINOR}.{PATCH} 로 되어있음
    2. Major의 변경이 있는 경우 minor, patch 보다 더 긴밀한 검토가 필요함
3. 다운타임을 갖을 수 있는지 확인
    1. 다운타임을 갖을 수 있다면 카프카 클러스터를 모두 종료한 후 업그레이드 하면 되어 매우 간단하게 업그레이드 가능
    2. 대다수의 경우에 다운타임을 갖을 수 없을 텐데 이때 카프카 브로커 한 대씩 롤링 업그레이드를 진행

# 주키퍼 의존성이 있는 카프카 롤링 업그레이드

### 우선 주키퍼의 의존성이 있는 환경에서 카프카 버전을 2.1에서 2.6으로 올려보자

- 현재 사용중인 카프카를 종료하기에 앞서, 토픽들을 먼저 삭제
    - `/usr/local/kafka/bin/kafka-topics.sh --bootstrap-server [peter-kafka01.foo.bar:9092](http://peter-kafka01.foo.bar:9092) --delete --topic --topic peter-test06`
- 현재 설치된 카프카를 모두 종료
    - `sudo systemctl stop kafka-server`
- ============= 현재 버전 (2.6) 의 카프카 종료 ==================
- 2.1 버전의 카프카 서버 설치
    - `cd chapter2/ansible_playbook && ansible-playbook -i hosts kafka2.1.yml`
- ============= 2.1 버전의 카프카 설치 완료 ===================
- peter-version2-1 토픽 설치
    - `/usr/local/kafka/bin/kafka-topics.sh --zookeeper [peter-zk01.foo.bar](http://peter-zk01.foo.bar) --create --topic peter-version2-1 --partitions 1 --replication-factor 3`

## 최신 버전의 카프카 다운로드와 설정

- 우선 업그레이드하고자 하는 대상 카프카 버전을 다운로드
- /usr/local 경로로 이동한 후 현재 디렉토리의 상태를 확인
    - `ll /usr/local`
    - 현재 링크가 kafka 2.12-2.1.0 으로 걸려있음. 이후 2.6 버전으로 업그레이드 시 kafka 디렉토리의 링크만 2.6으로 변경할 예정
- 2.1 버전의 설정 파일을 2.6 버전 설정 파일 경로로 복사
    - `sudo cp kafka_2.12-2.1.0/config/server.properties kafka_2.12-2.6.0/config/server.properties`
- 카프카 2.6 버전의 kafka_2.12-2.6.0/config/server.properties 설정 파일에 inter.broker.protocol.version과 log.message.format.version 항목 추가
    - `sudo vi kafka_2.12-2.6.0/config/server.properties`
    
    ```yaml
    inter.broker.protocol.version=2.1
    log.message.format.version=2.1
    ```
    
    - 2.6 버전의 카프카가 실행되어도 브로커 간의 내부 통신은 2.1 버전 기반으로 통신하며 메시지 포맷도 2.1을 유지한다는 의미의 설정이다. 위 설정을 적용하지 않고 2.6 버전의 브로커를 실행한다면, 이미 실행중인 2.1 버전 브로커들과의 통신이 불가능할 것이다.

## 브로커 버전 업그레이드

- 이제 각 브로커를 순차적으로 업그레이드 하자.
    - `sudo systemlctl stop kafka-server`
    - 브로커가 종료되어도 카프카가 제공하는 리플리케이션 기능을 통해 서비스는 장애 없이 모두 정상 작동한다.
- 2.1 버전으로 연결되어 있는 kafka 심볼릭 링크를 2.6버전으로 변경
    - `sudo rm -rf kafka`
    - `sudo ln -sf kafka_2.12-2.6.0 kafka`
- 브로커 실행
    - `sudo systemctl start kafka-server`
- 토픽 상세보기 명령어를 통해 리플리케이션과 ISR이 잘 동작하는지 확인
    - `/usr/local/kafka/bin/kafka-topics.sh --bootstrap-server [peter-kafka01.foo.bar:9092](http://peter-kafka01.foo.bar:9092) --topic peter-version2-1 --describe`
- 나머지 브로커도 동일하게 진행

→ 현재 브로커 프로토콜 버전과 메시지 포맷 버전이 2.1로 동작하고 있으니 이 부분을 2.6버전을 사용하게 변경하자

## 브로커 설정 변경

- 모든 브로커에 접속한 후 프로토콜 버전과 메시지 포맷 버전의 내용을 삭제하자. 삭제하는 대신 2.6으로 직접 정의해도 되지만 따로 명시하지 않으면 기본값이 적용되므로 삭제하는것을 추천한다.
    - `sudo vi /usr/local/kafka/config/server.properties`
    - 아래 두 줄 삭제
    
    ```yaml
    inter.broker.protocol.version=2.1
    log.message.format.version=2.1
    ```
    
- 삭제한 내용을 반영하기 위해 브로커 재시작
    - `sudo systemlctl restart kafka-server`
- 검증작업
    - 새로운 메시지를 잘 주고 받는지
    - 과거의 메시지를 잘 읽어올 수 있는지
    - 기존 컨슈머 그룹의 오프셋을 기억하고 있는지

## 업그레이드 작업 시 주의사항

- 업그레이드를 하기 전 운영 환경과 동일한 카프카 버전으로 개발용 카프카를 구성해보고 개발용 카프카의 버전 업그레이드를 수행해보자
- 작업 시간이 오래 걸릴 수 있으니 카프카 사용량이 가장 적은 시간대에 업그레이드를 수행하자
- 프로듀서의 `ack=1` 옵션을 사용하는 경우 카프카의 롤링 재시작으로 인해 메시지 손실 가능성이 있으니 면밀히 검토한 후 버전 업그레이드를 실시하자

# 카프카의 확장

초기 카프카 구성 시 어느정도의 트래픽을 처리할지, 어느 정도의 메시지를 주고받을지 등을 산정해 클러스터의 규모를 산정하지만 이를 예측하기란 매우 어렵다. 카프카는 이런 경우를 위해 폭발적으로 사용량이 증가하는 경우를 고려해 안전하고 손쉽게 확장할 수 있도록 디자인됐다.

- 우선 브로커를 한 대 추가한다.
- 토픽을 하나 추가한다.
- 새로 추가된 토픽의 경우 새로운 브로커에 파티션이 제대로 할당되지만 기존의 토픽들은 여전히 기존의 파티션을 재할당 하지 않아 특정 브로커에 작업 할당이 몰입되어 있는 걸 볼 수 있다.
- 따라서 부하 분산이 목적인 경우 브로커만 추가했다고 끝나는 것이 아니라 새롭게 추가된 브로커에도 기존의 파티션들을 할당해야 한다.

## 브로커 부하 분산

- `kafka-reassign-partitions.sh` 명령어를 이용해 분산시킬 대상 토픽에 대한 JSON 파일을 전달해 해당 토픽의 파티션을 분산시킬 브로커 리스트를 지정
    
    ```yaml
    {"topics": [{"topic": "peter-scaleout1"}], "version":1} // 단수의 경우
    {"topics": [{"topic": "peter-scaleout1"}, {"topic": "peter-scaleout2"], "version":1}
    // 복수의 경우
    ```
    
    - `/usr/local/kafka/bin/kafka-reassign-partitions.sh --bootstrap-server peter-kafka01.foo.bar:9092 --generate --topics-to-move-json-file reassign-partitions-topic.json --broker-list "1,2,3,4"`
    - 결과로 현재 설정된 파티션 배치를 먼저 보여주고, 이후에 제안하는 파티션 배치를 출력한다. 이제 이 설정을 복사해 새로운 `move.json` 파일을 생성한다
- `move.json` 파일
    
    ```json
    {
    	"version": 1,
    	"partitions": [
    		{
    			"topic": "peter-scaleout1",
    			"partition": 0,
    			"replicas": [ 2 ],
    			"log_dirs": [ "any" ],
    		},
    		{
    			"topic": "peter-scaleout1",
    			"partition": 1,
    			"replicas": [ 3 ],
    			"log_dirs": [ "any" ],
    		},
    		{
    			"topic": "peter-scaleout1",
    			"partition": 2,
    			"replicas": [ 4 ],
    			"log_dirs": [ "any" ],
    		},
    		{
    			"topic": "peter-scaleout1",
    			"partition": 3,
    			"replicas": [ 1 ],
    			"log_dirs": [ "any" ],
    		},
    	]
    }
    ```
    
- `kafka-reassign-partitions.sh` 명령어와 `—reassignment-json-file` 옵션으로 파티션 배치를 실행
    - `/usr/local/kafka/bin/kafka-reassign-partitions.sh --bootstrap-server peter-kafka01.foo.bar:9092 --reassignment-json-file move.json --execute`

<aside>
💡 카프카 클러스터에 브로커를 추가한다고 카프카의 로드가 자동으로 분산되지 않는다. 브로커 간의 부하 분산 및 밸런스를 맞추려면 기존 파티션들이 모든 브로커에 고르게 분산되도록 수동으로 분산 작업을 진행해야 한다.

</aside>

## 분산 배치 작업 시 주의사항

- 카프카의 사용량이 낮은 시간에 진행하자
    - 분산 배치 작업 수행 시 카프카 클러스터 내부에서 리플리케이션이 일어나므로 최대한 클러스터 사용량이 낮은 시간대에 진행해서 안정성을 높이자
- 용량이 큰 토픽의 파티션의 경우 보관 주기를 단축해 데이터 다이어트를 시키자
    - 예로 700GB의 1주일 보관기간을 갖는 브로커의 경우 최신 데이터만 필요한 토픽의 경우 보관기간을 1일로 지정해 100GB로 다이어트하고 분산 배치 할 경우 리소스를 많이 절약할 수 있다.
- 파티션 재배치 작업 시 한 번에 하나의 토픽만 진행하자
    - 재배치 작업 시 여러개의 토픽에 대해 분산 배치를 시행하면 당연히 많은 부하가 브로커에게 그대로 전달되니 한 번에 하나씩 진행해 안정성을 높이자.