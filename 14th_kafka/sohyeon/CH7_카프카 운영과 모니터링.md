# 7장 카프카 운영과 모니터링
- 카프카 클러스터를 구축할 때의 고려사항과 카프카 클러스터를 구축한 후 안정적인 운영을 위한 모니터링 방법을 살펴본다.
- 그라파나(Grafana)와 프로메테우스(Prometheus)를 기반으로 카프카와 하드웨어 리소스를 모니터링한다.

<br/>

## 7.1 안정적인 운영을 위한 주키퍼와 카프카 구성
- 카프카 클러스터 운영을 위해 고려할 사항들을 알아본다.

<br/>

### 7.1.1 주키퍼 구성
- 주키퍼는 파티션과 브로커의 메타데이터를 저장하고 컨트롤러 서버를 선출하는 동작을 수행한다.

<br/>

> **주키퍼 서버 수량**

- 주키퍼는 기본적으로 쿼럼(과반수) 구성을 기반으로 동작하므로 반드시 **홀수로 구성**해야 한다.
- 최소 서버 수량은 3으로, 과반수인 2를 충족할 수 있는 최대 1대까지의 주키퍼 장애를 허용한다. (5일 경우 최대 2대까지의 장애 허용)
- 카프카의 사용량이 높지 않으며 카프카가 매우 중요한 클러스터가 아니라면 주키퍼는 3대로 구성하는 것이 적합하다.
- 핵심 중앙 데이터 파이프라인으로 카프카를 이용 중이거나 카프카의 사용량도 높다면 주키퍼는 5대로 구성하여 안정성을 높이는 것이 권장된다.

<br/>

> **주키퍼 하드웨어**

- **물리적인 메모리 크기는 4~8GB**, **디스크는 240G 또는 480SSD** 사용이 권장된다. (높은 하드웨어 리소스를 요구하지 않음)
- 주키퍼 서버에 과도한 물리 메모리를 장착하는 것은 오히려 메모리를 낭비하는 일이 될 수 있다.
- 트랜잭션이나 스냅샷 로그들은 로컬 디스크에 저장하는데, 쓰기 성능이 좋은 SSD 디스크를 추천한다.
- 네트워크 카드는 1G 이더넷 카드로 구성하면 된다. (주키퍼와 카프카 간 메타데이터 정도만 주고받으므로 네트워크 사용량이 높지 않음)

<br/>

> **주키퍼 배치**

- 주키퍼를 각기 다른 랙에 분산 배치하는 방안을 권장하며, 이와 동시에 전원 이중화나 스위치 이중화 등도 고려해야 한다.
- AWS에서도 분산 배치를 위해 가용 영역을 운영하므로 가능한 한 2개 또는 3개의 가용 영역에 분산해 구성하는 것을 추천한다.

<br/>

### 7.1.2 카프카 구성
- 안정적인 카프카 클러스터를 구성하기 위한 고려사항을 살펴본다.

<br/>

> **카프카 서버 수량**

- 쿼럼 방식의 구성이 필요하지 않으므로 카프카 클러스터의 수가 반드시 홀수일 필요는 없다.
- 카프카의 최소 구성에서 3대가 가장 적당하다. (안정적인 리플리케이션 팩터 수는 3)

<br/>

> **카프카 하드웨어**

- CPU 사용률이 높은 편이므로, 코어 수가 많은 CPU로 구성할 것이 권장된다.
- 어느 정도 메모리 여유가 있어야 성능에 도움이 된다.
  - 최소 32GB 이상 구성하는 것을 추천한다.
  - 힙 크기를 제외한 나머지 물리 메모리는 모두 페이지 캐시로 사용해서 빠른 처리를 돕는다.
 
- 저성능의 SATA 디스크를 사용해도 카프카는 높은 성능을 보장할 수 있다.
  - 로그 마지막에 순차적으로 쓰는 방식으로 로그를 기록한다.
  - 병렬 처리를 위해 약 10개 정도의 디스크를 장착한다.
  - 토픽의 보관 주기를 충분하게 설정하려면 4TB 용량 이상의 디스크로 선정하는 것을 추천한다.
 
- 네트워크 카드는 10G 이더넷 카드로 구성하는 것을 추천한다.
  - 브로커 한 대당 네트워크 사용량 비율이 50%가 넘지 않도록 최대한 토픽을 분산해 운영해야 한다.
  - 디스크의 장애 복구 또는 신규 브로커 추가로 인해 카프카 클러스터 간 대량의 데이터 이동이 발생할 수 있으므로 네트워크 대역폭은 충분히 확보되어야 한다.
 
<br/>

> **카프카 배치**

- 여러 랙에 분산시켜 카프카 서버를 배치하는 방식을 추천한다.
- AWS에서 구성할 경우 멀티 가용 영역으로 구성하는 것을 추천한다.

<br/>

## 7.2 모니터링 시스템 구성
- 오픈소스 기반의 카프카 모니터링 시스템을 직접 구성해보며 카프카를 모니터링하는 방법을 살펴본다.
- 대표적으로 애플리케이션 로그 분석과 JMX를 이용해 브로커들의 메트릭 정보를 확인할 수 있다.

<br/>

### 7.2.1 애플리케이션으로서 카프카의 로그 관리와 분석
- 카프카는 애플리케이션 로그 관리를 위해 log4j를 이용한다.
- log4j는 애플리케이션의 레벨별로 로깅이 가능하며, 관리자는 로그의 레벨을 보고 상황의 심각성 등을 유추할 수 있다.
  |로그 레벨|설명|
  |---|---|
  |TRACE|DEBUG보다 상세한 로그를 기록함|
  |DEBUG|내부 애플리케이션 상황에 대한 로그를 기록함|
  |INFO|로그 레벨의 기본값이며, 일반적인 정보 수준의 로그를 기록함|
  |WARN|INFO 로그 레벨보다 높은 개념으로, 경고 수준의 로그를 기록함|
  |ERROR|경고 수준을 넘어 런타임 에러나 예상하지 못한 에러 로그를 기록함|
  |FATAL|로그 레벨 중 최종 단계이며, 심각한 오류로 인한 애플리케이션 중지 등의 로그를 기록함|

  🔼 log4j 로그 레벨

<br/>

> **실습**

```
$ cat /usr/local/kafka/config/log4j.properties
```
🔼 (브로커 서버 접속 중) log4j 파일 내용 확인

<br/>

```
...

log4j.logger.kafka=INFO
log4j.logger.org.apache.kafka=INFO

...
```
🔼 출력 결과
- 로그 레벨이 INFO로 적용됐음을 알 수 있다.
- 로그 레벨을 변경하고 싶다면 INFO 대신 다른 레벨 값으로 수정하면 된다.

<br/>

```
$ sudo systemctl restart kafka-server
$ cat /usr/local/kafka/logs/server.log
```
🔼 브로커 재시작 후 server.log 로그 파일 확인
- 로그 파일을 확인해보면 DEBUG 레벨로 기록된 로그를 확인할 수 있다.
- 로그 레벨이 낮을수록 로그의 양이 증가하므로 여유 디스크 공간이 충분한지 확인해야 한다.

<br/>

|로그 파일 이름|설명|
|---|---|
|server.log|브로커 설정 정보와 정보성 로그 등을 기록함|
|state-change.log|컨트롤러로부터 받은 정보를 기록함|
|kafka-request.log|클라이언트로부터 받은 정보를 기록함|
|log-cleaner.log|로그 컴팩션 동작들을 기록함|
|controller.log|컨트롤러 관련 정보를 기록함|
|kafka-authorizer.log|인증과 관련된 정보를 기록함|

🔼 카프카 애플리케이션의 로그 파일 종류와 역할

<br/>

### 7.2.2 JMX를 이용한 카프카 메트릭 모니터링
- JMX는 애플리케이션의 모니터링을 위한 도구를 제공하는 자바 API로서, MBean이라는 객체로 표현된다.
- 관리자는 JMX를 이용해 카프카의 주요 메트릭들을 그래프와 같은 형태로 한눈에 확인할 수 있다.

<br/>

> **카프카 JMX 설정 방법**

```
$ cat /usr/local/kafka/config/jmx
```
🔼 환경 변수 옵션 확인

```
JMX_PORT=9999
```
🔼 출력 결과
- 9999포트로 JMX가 적용되어 있음을 알 수 있다.

<br/>

```
$ netstat -ntl | grep 9999
```
🔼 JMX 포트가 활성화됐는지 확인

```
tcp6    0    0:::9999     :::*    LISTEN
```
🔼 출력 결과
- JMX 포트인 9999가 리스닝 상태임을 알 수 있다.

<br/>

> **프로메테우스 설치**

- 프로메테우스는 메트릭 기반 모니터링 시스템이다.
- 도커를 이용해 프로메테우스를 설치할 수 있다.
  ```
  $ sudo cp kafka2/chapter7/prometheus.yml /etc/prometheus/
  $ sudo docker run -d --network host -p 9090:9090 -v etc/prometheus/promethus.yml:/etc/prometheus/prometheus.yml --name prometheus prom/prometheus
  ```
  🔼 프로메테우스 설치 (https://github.com/onlybooks/kafka2 레포지토리 클론 필요)
  <br/>
  ```
  $ sudo docker ps
  ```
  🔼 docker 명령어를 통해 상태 확인
  - 프로메테우스 상태가 UP이라고 표시되면 정상적으로 실행된 것이다.
 
<br/>

> **그라파나 설치**

- 전체 시스템에 관한 대시보드를 보여주거나, 사용자가 손쉽게 대시보드를 만들 수 있도록 도와주는 대표적인 도구이다.

<br/>

```
sudo docker run -d --network host -p 3000:3000 --name grafana grafana/grafana:7.3.7
```
🔼 그라파나 설치
- `docker ps` 명령어의 출력 결과에서 상태에 UP이라고 표시되면 정상적으로 실행된 것이다.

<br/>

> **익스포터 설치**

- 프로메테우스의 모니터링 방식은 풀(pull) 방식이다.
- 모니터링하고자 하는 대상 서버에 자신의 메트릭 정보를 보여줄 수 있는 익스포터를 설치해야 한다.
- 익스포터 설치 후 웹 브라우저를 열어 대상 서버 주소로 접근하면, 익스포터에서 보여주는 다양한 메트릭 정보를 확인할 수 있다.
- 프로메테우스로 모니터링할 대상 서버와 포트 정보를 프로메테우스 환경 설정 파일에 등록하면, 주기적으로 대상 서버의 메트릭 값을 가져와 자신의 DB에 저장한다.

<br/>

```
$ sudo cp kafka2/chapter7/jmx_prometheus_httpserver-0.13.1-SNAPSHOT-jar-with-dependencies.jar /usr/local/jmx/
$ sudo cp kafka2/chapter7/jmx_prometheus_httpserver.yml /usr/local/jmx/
```
🔼 익스포터를 실행하기 위한 파일 복사

<br/>

```
hostPort: 127.0.0.1:9999 # JMX가 실행되고 있는 IP와 포트 정보를 입력한다.
ssl: false # SSL 사용 유무를 나타낸다.
rules: # 순서대로 적용할 규칙의 리스트를 나타낸다. 일치하지 않는 속성들은 수집되지 않는다.
  - pattern: ".*"
```
🔼 jmx_promethues_httpserver.yml 예제 파일

<br/>

```
$ sudo systemctl start jmx-exporter
$ sudo systemctl status jmx-exporter
```
🔼 JMX 익스포터 실행
- `Active: active (running)`을 확인할 수 있으면 익스포터는 정상적으로 실행된 것이다.
- JMX 익스포터는 카프카 클러스터 내 모든 브로커에 설치해야 한다.

<br/>

```
$ wget https://github.com/promethues/node_exporter/releases/download/v1.0.1/node_exporter-1.0.1.linux-386.tar.gz
$ sudo tar zxf node_exporter-1.0.1.linux-386.tar.gz -C /usr/local/
$ sudo systemctl start node-exporter
$ sudo systemctl status node-exporter
```
🔼 노드 익스포터 다운로드

<br/>

```
# prometheus config
global: # 프로메테우스의 전반적인 설정과 관련된 부분
  ...

scrap_configs:
  - job_name: 'peter-jmx-kafka' # 프로메테우스에서 메트릭을 수집할 대상을 설정하는 부분 (jmx 익스포터에 대한 항목)
    ...

  - job_name: 'peter-kafa-nodes' # 프로메테우스에서 메트릭을 수집할 대상을 설정하는 부분 (노드 익스포터에 대한 설정)
    ...
```
🔼 prometheus.yml 파일

<br/>

> **그라파나 대시보드 생성**

- 그라파나를 설치한 호스트의 3000번 포트로 접근하면 된다.
- Ex. `http://peter-ansible01.foo.bar:3000` 접속

<br/>

![image](https://github.com/mash-up-kr/S3A/assets/55437339/36fbe180-2014-4a72-9702-9df1356957d8)

- 초기 암호는 admin/admin이다.
- 로그인 성공 후 새로운 암호로 변경할 수 있다.

<br/>

![image](https://github.com/mash-up-kr/S3A/assets/55437339/c86f0c6b-6142-4d07-9671-afcffacf69b6)

- 최초 로그인 이후, 연동된 데이터 소스가 없으므로 대시보드 또는 그래프를 그릴 수 없다.
- 데이터 소스로 프로메테우스를 추가하기 위해 `Add your first data source`를 선택한다.

<br/>

![image](https://github.com/mash-up-kr/S3A/assets/55437339/d5192c18-53db-44f7-86fa-c9e19a8d6c00)

- 최상단에 있는 Prometheus를 선택한다.

<br/>

![image](https://github.com/mash-up-kr/S3A/assets/55437339/df2a8794-cf9c-4c51-b562-a077bbdc501d)

- URL을 입력하는 칸에 프로메테우스 주소를 입력한다. (http://peter-ansible01.foo.bar:9000)

<br/>

![image](https://github.com/mash-up-kr/S3A/assets/55437339/d880474a-69f6-4f2e-b517-821e4c20e071)

- 프로메테우스 정보 입력 후 하단의 Save & Test 버튼을 클릭한다.
- 연동에 성공한다면 Data source is working이라는 메시지가 표시된다.

<br/>

![image](https://github.com/mash-up-kr/S3A/assets/55437339/dc2bf1b1-a354-48f7-8e74-a0114fd4d1f5)

- 좌측 상단 메뉴 중 + 버튼을 눌러 import 버튼을 클릭한다. (이미 만들어진 카프카 메트릭 대시보드를 추가할 예정)

<br/>

![image](https://github.com/mash-up-kr/S3A/assets/55437339/441d4415-4137-419a-b978-3001d79d8839)

- JSON 형태로 저장된 그라파나 대시보드를 추가할 수 있는 화면이다.
- 먼저 노드 익스포터를 위한 대시보드를 임포트한다. (그라파나 공식 홈페이지에서 제공하는 대시보드 이용)
  - import via grafana.com 메뉴에서 1860이라고 입력 후, 우측의 Load 버튼을 클릭한다.
 
<br/>

![image](https://github.com/mash-up-kr/S3A/assets/55437339/0531f375-9c5c-448a-b09b-7e10fe9a665d)

- 대시보드 추가의 마지막 단계이다.
- 대시보드의 설정을 변경하는 화면이다.
- 맨 마지막 항목의 Prometheus 드롭다운 메뉴에서 Prometheus를 선택한 후 import 버튼을 클릭해서 마무리한다.
- 이후 JMX 익스포터 대시보드도 추가한다. (/home/ec2-user/kafka2/chapter7 경로의 kafka_metrics.json 파일)

<br/>

![image](https://github.com/mash-up-kr/S3A/assets/55437339/3f409745-30d3-46ef-9c97-991fbab8d203)

- JSON 파일의 대시보드를 임포트한다.
- 해당 파일의 내용을 리눅스 cat 명령어로 확인한 뒤 내용을 복사한 후 Import via panel json 창에 붙여넣는다.
  ```
  $ cat kafa2/chapter7/kafka_metrics.json
  ```

<br/>

![image](https://github.com/mash-up-kr/S3A/assets/55437339/9138860c-8b74-4fe8-a5fb-5aa29ffafeea)

- 노드 익스포터 대시보드 화면이다.
- CPU, 메모리, 네트워크, 디스크 항목별로 상세 모니터링이 가능하다.

<br/>

![image](https://github.com/mash-up-kr/S3A/assets/55437339/60600805-af3e-4c25-8559-90aa62cc45a6)

- JMX 익스포터를 이용한 카프카 대시보드 화면으로 총 5개의 카테고리로 나뉜다.
  - 전체 브로커 수, 토픽 수, 파티션 수, 현재 실시간 초당 인입 메시지 수 및 응답 시간 등 주요 항목들
  - 브로커의 세부 정보
  - 응답 시간과 관련된 그래프를 볼 수 있는 구역으로 각각 프로듀서, 컨슈머, 팔로워에 관해 전체적인 지연시간이 발생할 경우 어느 구간에서 지연이 발생하는지 상세하게 추적하고 확인할 수 있다.
 
<br/>

> **JMX 모니터링 지표**

|JMX 매트릭 항목|설명|
|---|---|
|TopicCount|카프카 클러스터 전체의 토픽 개수|
|PartitionCount|카프카 클러스터 전체의 파티션 개수|
|ActiveControllerCount|카프카 클러스터 내 컨트롤러 수|
|UnderReplicatedPartitions|카프카 클러스터 내에서 복제가 되지 않은 파티션 수|
|UnderMinIsrPartitionCount|안정적인 메시지 전송을 위해 유지해야 하는 최소 ISR 수를 지정하는 경우가 있다. 유지해야 하는 최소 ISR 수보다 작은 수|
|MessagesInPerSec|브로커로 전송되는 초당 메시지 수|
|BytesInPerSec|브로커로 전송되는 초당 바이트 수|
|BytesOutPerSec|브로커에서 나가는 초당 바이트 수|
|RequestPerSec|프로듀서, 컨슈머, 팔로워들의 요청 비율|
|LeaderCount|브로커가 갖고 있는 리더의 수|
|PartitionCount|브로커가 갖고 있는 파티션 수|
|IsrShrinksPerSec|파티션의 리플리케이션 동작에 문제가 있는지 유무 등을 확인할 때 사용하는 지표|
|RequestQueueSize|요청 큐의 크기(크기가 크다는 것은 처리되지 못하는 요청들이 많다는 의미)|
|ResponseQueueSize|응답 큐의 크기(크기가 크다는 것은 처리되지 못하는 응답들이 많다는 의미)|
|RequestHandlerAvgIdlePercent|요청 핸들러 스레드가 유휴 상태인 평균 시간|
|NetworkProcessorAvgIdlePercent|네트워크 프로세서 스레드가 유휴 상태인 평균 시간|
|RequestQueueTimeMs|요청 큐에서의 대기시간|
|LocalTimeMs|리더에서 요청을 처리하는 시간|
|RemoteTimeMs|팔로워들을 기다리는 시간|
|ResponseQueueTimeMs|응답 큐에서의 대기시간|
|ResponseSendTimeMs|응답을 보내는 시간|

🔼 JMX 주요 항목과 설명

<br/>

### 7.2.3 카프카 익스포터
