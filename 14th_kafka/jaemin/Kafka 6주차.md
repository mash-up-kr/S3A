# Kafka 6주차

# 카프카 운영과 모니터링

카프카 애플리케이션은 매우 안정적인 애플리케이션 중 하나이지만 너무 방치하다보면 큰 장애를 만날 수 있다. 장애가 발생하지 않도록 사전에 예방 조치를 취하는 것이 중요하다.

이 장에서 카프카 클러스터 구축 후 안정적인 운영을 위한 모니터링 방법을 알아보고 하드웨어의 리소스 모니터링도 함께 알아보자. 그라파나와 프로메테우스를 기반으로 모니터링하는 방법을 기술한다.

# 안정적인 운영을 위한 주키퍼와 카프카 구성

초기 구성 단계부터 관리자가 꼼꼼하게 단일 장애 지점 등을 제거하고 클러스터를 구성한다면 더욱 안정적인 클러스터를 운영할 수 있다.

## 주키퍼 구성

책을 집필하는 시점에 주키퍼를 제거하는 움직임이 보이는데 우선 주키퍼를 기반으로 구성하는 방법을 배운다.

### 주키퍼 서버 수량

주키퍼는 기본적으로 쿼럼 구성을 기반으로 동작하므로 반드시 홀수로 구성해야한다. 주키퍼 구성 시 최소 수량으로 구성한다고 하면 주키퍼 서버의 수는 3이다. 이 3대는 과반수 2를 충족할 수 있는 최대 1대까지의 주키퍼 장애를 허용한다. 물론 이 수를 늘리면 그만큼 안정적일 것이다.

운영 환경에서 카프카의 사용량이 높지 않고 카프카가 매우 중요한 클러스터가 아니라면 3대, 핵심 중앙 데이터 파이프라인으로 이용중이거나 높은 안정성을 요할경우에는 5대로 구성하자.

### 주키퍼 하드웨어

주키퍼는 높은 하드웨어 리소스를 요구하지 않아 물리적인 메모리 크기는 4~8GM, 디스크는 240G or 480G SSD를 사용하자. 주키퍼에서 필요로 하는 힙 메모리 크기는 일반적으로 1~2GB이며 나머지는 OS영역 등에서 사용되니 메모리 크기를 너무 크게 하면 낭비가 된다. 주키퍼는 트랜잭션이나 스냅샷 로그들을 로컬 디스크에 저장하니 일반적인 SAS 디스크보다는 쓰기 성능이 좋은 SSD를 추천한다. 네트워크 카드도 1G 이더넷 카드로 구성하면 된다. 주키퍼와 카프카 간에는 메타데이터 정도만 주고받기에 주키퍼의 네트워크 사용량이 높지 않다.

### 주키퍼 배치

물리 서버를 배치하는 경우 서로 다른 랙에 분산 배치하는 것은 권장한다. 한 곳에 몰아 배치한다면 매우 위험한 상황이 발생할 수 있다. 최근들어 AWS 같은 퍼블릭 클라우드에서 EC2 인스턴스를 구성해 상요하기도 하는데 AWS에서도 분산 배치를 위해 가용 영역을 운영하므로 가능한 한 2개 또는 3개의 가용 영역에 분산해 구성하자

## 카프카 구성

카프카를 구성할 때 조금 더 고민해 볼 점들이 있다.

### 카프카 서버 수량

카프카는 쿼럼 구성을 하지 않기때문에 반드시 홀수일 필요는 없다. 최소 수량으로 카프카를 구성한다면 카프카에서 권장하는 안정적인 리플리케이션 팩터 수인 3으로 토픽을 구성하기 위해 3대의 브로커로 구성하자. 카프카는 확장에 용이하기에 처음부터 큰 수량을 가져가지 않아도 좋다

### 카프카 하드웨어

주키퍼와 달리 카프카는 CPU 사용률은 높은 편이다. 최신의 고성능 CPU가 아닌 코어 수가 많은 CPU로 구성하자

메모리는 32GB ~ 256GB 까지 다양하게 선택할 수 있는데 카프카에서 요하는 JVM 힙 크기는 일반적으로 6GB이므로 이보다 큰 물리 메모리를 사용하자. 카프카에서 힙 크기를 제외한 나머지 물리 메모리는 모두 페이지 캐시로 사용하기에 어느정도 여유가 있는 것이 좋다. 최소 32GB 이상 구성하자

디스크의 경우 성능이 가장 낮은 SATA를 써도 괜찮다. 그 이유는 로그 마지막에 순차적으로 스는 방식으로 로그를 기록하기 때문이다. 다만 브로커 한 대에서 병렬 처리를 위해 서버에 약 10대 정도의 디스크를 장착한다. 토픽의 보관 주기를 충분하게 설정하려면 4TB 용량 이상의 디스크로 선정하자

NAS 디스크도 하나의 대안이 될 수 있지만 모든 브로커가 하나의 NAS를 바라보고 있기에 NAS가 발생하면 위험한 상황이 발생할 수 있으므로 NAS는 조ㅔ외하자

AWS에서 EC2 인스턴스를 이용해 카프카를 운영할 수 있는데 이때 EBS가 안정적이다.

카프카의 네트워크 카드는 10G 이더넷 카드로 구성하는 것을 추천한다. 브로커 한 대당 네트워크 사용량 비율이 50%가 넘지 않도록 최대한 토픽을 분산해 운영하자. 디스크의 장애 복구나 신규 브로커 추가 등으로 클러스터 내에 대량의 데이터 이동이 발생할 수 있기에 네트워크 대역폭은 충분히 확보하자.

### 카프카 배치

카프카 서버또한 주키퍼 처럼 하나의 랙에 구성하는 것은 위험하다. 이때 전원 이중화, 스위치 이중화 등도 고려해 분산 배치하자. AWS에서 구성한다면 멀티 가용 영역으로 구성하자.

# 모니터링 시스템 구성

카프카를 모니터링하는 방법은 여러가지가 있지만, 그중에서 대표적인 모니터링은 애플리케이션 로그 분석과 JMX를 이용해 브로커들의 메트릭 정보를 확인하는 방법이다.

## 애플리케이션으로서 카프카의 로그 관리와 분석

카프카는 카프카 애플리케이션에서 발생하는 모든 로그를 브로커의 로컬 디스크에 기록하고 있다. 관리자는 이 로그를 활용해 카프카의 현재 상태나 이상 징후를 포착해 이상 증상 발생 시 원인을 찾는다. 카프카는 애플리케이션 로그 관리를 위해 자바 기반 로깅 유틸리티인 아파트 `log4j` 를 이용한다. `log4j` 는 로그마다 레벨을 설정해 심각성 등을 유추할 수 있다.

### 로그 레벨

| 로그 레벨 | 설명 |
| --- | --- |
| TRACE | DEBUG보다 상세한 로그를 기록 |
| DEBUG | 내부 애플리케이션 상황에 대한 로그를 기록함(INFO 로그보다 상세한 로그 기록) |
| INFO | 로그 레벨의 기본값이며, 일반적인 정보 수준의 로그를 기록 |
| WARN | INFO 로그 레벨보다 높은 개념으로, 경고 수준의 로그를 기록 |
| ERROR | 경고 수준을 넘어 런타임 에러나 예상하지 못한 에러 로그를 기록 |
| FATAL | 로그 레벨 중 최종 단계이며, 심각한 오류로 인한 애플리케이션 중지 등의 로그를 기록 |

기본값은 INFO이며, 관리자가 `/kafka/config/log4j.properties` 에서 로그 레벨을 손쉽게 변경가능하다.

### 카프카 애플리케이션의 로그 파일 종류와 역할

각 로그 파일들이 어떤 정보를 기록하고 있는지 살펴보자

| 로그 파일 이름 | 설명 |
| --- | --- |
| server.log | 브로커 설정 정보와 정보성 로그 등을 기록. 브로커를 재시작하는 경우 브로커의 옵션 정보가 기록 |
| state-change.log | 컨트롤러로부터 받은 정보를 기록 |
| kafka-request.log | 클라이언트로부터 받은 정보를 기록 |
| log-cleaner.log | 로그 컴팩션 동작들을 기록 |
| controller.log | 컨트롤러 관련 정보를 기록 |
| kafka-authorizer.log | 인증과 관련된 정보를 기록 |

## JMX를 이용한 카프카 메트릭 모니터링

**JMX**는 `Java Management eXtensions` 의 약자로 자바로 만든 애플리케이션의 모니터링을 위한 도구를 제공하는 자바 API로서, `MBean` 이라는 객체로 표현된다. 카프카 관리자는 **JMX**를 이용해 카프카의 주요 메트릭들을 그래프와 같은 형태로 한눈에 확인 할 수 있다.

먼저 브로커에 JMX 포트를 오픈한 뒤에 JMX에서 제공하는 메트릭 정보를 관리자가 GUI 형태로 볼 수 있도록 구성하자. 여기서는 최근 가장 많이 쓰이는 프로메테우스와 익스포터를 이용해 JMX 모니터링 시스템을 구성해보자

### JMX 모니터링 지표

카프카 모니터링을 위해 JMX에서 제공하는 지표는 굉장히 많다.

| JMX 메트릭 항목 | MBean 사용 유형 | 설명 |
| --- | --- | --- |
| TopicCount | kafka.controller:type=KafkaController, name=GlobalTopicCount | 카프카 클러스터 전체의 토픽 개수 |
| PartitioanCount | kafka.controller:type=KafkaController, name=GlobalPartitionCount | 카프카 클러스터 전체의 파티션 개수 |
| ActiveControllerCount | kafka.controller:type=KafkaController, name=ActiveControllerCount | 카프카 클러스터 내 컨트롤러 수. 클러스터 내에는 반드시 1개의 컨트롤러가 존재해야 함. 알람을 설정해두면, 1이 아닌 경우 알람 발생 |
| UnderRpelicatedPartitions | kafka.server:type=ReplicaManager, name=UnderReplicatedPartitions | 카프카 클러스터 내에서 복제되지 않은 파티션 수. 알람을 설정해두면 0이 아닌 경우 알람이 발생. 오프라인 파티션 모니터링이 필요하면 OfflinePartitionsCount를 참고 |
| UnderMinIsrPartitionCount | kafka.server:type=ReplicaManager, name=UnderMinIsrPartitionCount | 안정적인 메시지 전송을 위해 유지해야 하는 최소 ISR |
| MessagesInPerSec | kafka.server:type=BrokerTopicMetrics, name=MessagesInPerSec | 브로커로 전송되는 초당 메시지 수 |
| BytesInPerSec | kafka.server:type=BrokerTopicMetrics, name=BytesInPerSec | 브로커로 전송되는 초당 바이트 수 |
| BytesOutPerSec | kafka.server:type=BrokerTopicMetrics, name=BytesOutPerSec | 브로커에서 나가는 초당 바이트 수. 일반적으로 브로커로 들어오는 바이트 수보다 나가는게 더 많다. 프로듀서는 하나의 토픽으로 전송하지만 컨슈머는 여러개일 수 있으므로 |
| RequetsPerSec | kafka.server:type=RequestMetrics, name=RequestsPerSec, request={Produce|FetchConsumer|FetchFollower} | 프로듀서, 컨슈머, 팔로워들의 요청 비율 |
| LeaderCount | kafka.server:type=RequestMetrics, name=LeaderCount | 브로커가 갖고 있는 리더의 수. 카프카는 리더가 읽고 쓰기를 하므로 클러스터 전체에 고르게 분산시켜 브로커들이 일을 균등하게 하게 해야함 |
| PartitionCount | kafka.server:type=RequestMetrics, name=PartitionCount | 브로커가 갖고 있는 파티션 수. 파티션 수 역시 고르게 분산되어야 함 |
| IsrShrinksPerSec | kafka.server:type=RequestMetrics, name=IsrShrinksPerSec | 브로커가 다운되거나 리플리케이션 동작에 문제가 발생하며 ISR이 축소된다. 따라서 파티션의 리플리케이션 동작에 문제가 있는지 유무를 확인하기 위한 지표로 사용된다. 브로커가 장애에서 복구되거나 리플리케이션 동작이 정상화 되면 ISR이 확장되는데 IsrExpandsPerSec값을 통해 확인가능 |
| RequestQueueSize | kafka.network.type=RequestChannel,name=RequestQueueSize | 요청 큐의 크기, 큐의 크기가 크다는 것은 처리되지 못하는 요청들이 많다는 의미 |
| ResponseQueueSize | kafka.network.type=RequestChannel,name=ResponseQueueSize | 응답 큐의 크기, 큐의 크기가 크다는 것은 처리되지 못하는 응답들이 많다는 의미 |
| RequestHandlerAvgIdlePercent | kafka.server.type=KafkaRequestHAndlerPool,name=RequestHandlerAvgIdlePercent | 요청 핸들러 스레드가 유휴 상태인 평균 시간을 나타냄. 0인 경우 모든 리소스가 사용된 것이고, 1이면 모든 리소스를 가용할 수 있는 상태 |
| NetworkProcessorAvgIdlePercent | kafka.network:type=SocketServer, name=NetworkProcessorAvgIdlePercent | 네트워크 프로세서 스레드가 유휴상태인 평균 시간을 나타냄, 0인 경우 모든 리소스가 사용된 것이고 1인경우 모든 리소스를 가용할 수 있는 상태 |
| RequestQueueTimeMs | kafka.network.type=RequestMetrics,name=RequestQueueTimeMs, request={Produce|FetchConsumer|FetchFollower} | 요청 큐에서의 대기시간 |
| LocalTimeMs | kafka.network:type=RequestMetrics,name=LocalTimeMs,request={Produce|FetchConsumer|FetchFollower} | 리더에서 요청을 처리하는 시간 |
| RemoteTimeMs | kafka.network.type=RequestMetrics, name=RemoteTimeMs,request={Produce|FetchConsumer|FetchFollower} | 팔로워들을 기다리는 시간. 프로듀서 옵션에서 acks=all 등을 사용하면 시간이 길어질 수 있음 |
| ResponseQueueTimeMs | kafka.network:type=RequestMetrics, name=ResponseQueueTimeMs,request={Produce|FetchConsumer|FetchFollower} | 응답 큐에서의 대기시간 |
| ResponseSendTimeMs | kafka.network:type=RequestMetrics, name=ResponseSendTimeMs,request={Produce|FetchConsumer|FetchFollower} | 응답을 보내는 시간 |

## 카프카 익스포터

컨슈머의 LAG을 모니터링하는 것도 중요하다. 이 또한 프로메테우스와 그파라나를 조합해 모니터링 할 수 있다.

모티터링을 잘 하기 위해서는 자주 관심을 갖고 대시보드를 자주 확인하는 것이다. 자주 들여다보면 평소와 다른 패턴을 나타내는 경우, 의심이 되는 메트릭을 살펴보면서 원인을 파악해보면서 경험치를 쌓자.

처음 모니터링을 한다면 우선 일주일 이상 메트릭을 수집한 뒤 지표들을 매일 살펴보자. 피크치를 확인해보고 이를 가늠하기 힘든, 변화가 잦은 메트릭은 일주일을 기준으로 피크값으로 임계치를 설정해 임계치에 도달하면 알림을 받을 수 있게 한다. 또 일주일간 주기적으로 모니터링하면서 알람의 빈도 수 등을 확인하며 임계치를 재조정하고, 알람이 발생했을 때의 영향도 등을 확인한다. 이렇게 계속해서 임계치를 재조정하다보면 최적화된 수준의 임계치와 알람 설정을 얻을 수 있고 이후부터는 효과적인 모니터링을 할 수 있을 것이다.