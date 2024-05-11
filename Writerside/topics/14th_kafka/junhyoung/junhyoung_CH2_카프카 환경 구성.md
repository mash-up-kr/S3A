## 2.2 카프카 클러스터 구성
실습 환경에서 AWS 매니지드 서비스가 아닌 EC2 위에 카프카 클러스터를 직접 설치

### EC2 위에 주키퍼와 카프카 설치

앤서블 명령어인 `ansible-playbook`를 사용
#### 주키퍼 설치

`cd kafka2/chapter2/ansible_playbook`

`ansible_playbook]$ ansible-playbook -i hosts zookeeper.yml`

#### 주키퍼 상태 확인

`systemctl status zookeeper-server`
~~~
상태
Active: active (runnung) : 정상
Active: failed (runnung) : 앤서블 재설치
~~~

#### 카프카 설치
앤서블 명령어 사용 후 -i 옵션을 사용하여 peter-kafka 01~03 서버에 모두 설치

`ansible-playbook -i hosts kafka.yml`

#### 카프카 상태 확인

`systemctl status kafka-server`
~~~
상태
Active: active (runnung) : 정상
Active: failed (runnung) : 앤서블 재설치
~~~

### 2.3 5분 만에 카프카 맛보기
#### 2.3.1 카프카의 기본 구성

#### 간단한 구성도
![kafka구성도.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch2/kafka구성도.png)

**프로듀서**: 카프카에 데이터(메시지)를 만들어서 줌

**컨슈머**: 카프카에서 데이터(메시지)를 빼내서 소비함

**주키퍼**: 카프카의 정상 동작을 보장하기 위해 메타데이터를 관리하는 코디네이터

~~~
⚠️브로커 용어 혼동 주의⚠️
카프카는 애플리케이션의 이름 
브로커는 카프카의 애플리케이션이 설치된 서버 또는 노드를 의미
~~~

### 프로듀서의 전송 방식
프로듀서가 카프카로 그냥 전송하는 것이 아니라 카프카의 특정 토픽으로 전송함

### peter-overview01 토픽 생성
주소와 포트는 단순 예시
~~~
/usr/local/kafka/bin/kafka-topics.sh --bootstrap-
server peter-kafka01.foo.bar:9092 ~-create --topic peter-overview01 --partitions 1 
--replication-factor 3
~~~

## 프로듀서, 컨슈머 실습
주소와 포트는 단순 예시
### 컨슈머
~~~
/usr/local/kafka/bin/kafka-console-consumer.sh--bootstrap-
server peter-kafka01.foo.bar:9092 --topic peter-overview01
~~~

### 프로듀서
~~~
/usr/local/kafka/bin/kafka-console-producer.sh--bootstrap-
server peter-kafka01.foo.bar:9092 --topic peter-overview01
~~~

### 자주 사용하는 명령어
~~~
kaka-topics.sh: 토픽을 생성하거나 토픽의 설정 등을 변경하기 위해 사용함 
kafka-console-producer.sh: 토픽으로 메시지를 전송하기 위해 사용함. 기본 옵션 외 추가 옵션을 지정할 수 있고, 이를 통해 다양한 프로듀서 옵션 적용 가능
kafka-console-consumer.sh: 토픽에서 메시지를 가져오기 위해 사용함. 기본 옵션 외 추가 옵션을 지정할 수 있고, 이를 통해 다양한 컨슈머 옵션 적용 가능
kafka-reassign-partitions.sh: 토픽의 파티션과 위치 변경 등을 위해 사용함 kafka-dump-og.sh: 파티션에 저장된 로그 파일의 내용을 확인하기 위해 사용함
~~~