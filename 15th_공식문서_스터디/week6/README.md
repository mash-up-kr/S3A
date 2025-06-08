# Kafka

## Document

- https://kafka.apache.org/documentation/#design

## Code

- https://github.com/apache/kafka/blob/b5cceb43e5eb9a19cf090e48bc97267b1c6e78a4/core/src/main/scala/kafka/log/LogManager.scala#L62-L62
- https://github.com/apache/kafka/blob/b5cceb43e5eb9a19cf090e48bc97267b1c6e78a4/core/src/main/scala/kafka/log/LogManager.scala#L1007-L1007
- `BaseProducerSendTest#testSendOffset`부터 분석하면 좋을듯
    - https://github.com/apache/kafka/blob/b5cceb43e5eb9a19cf090e48bc97267b1c6e78a4/core/src/test/scala/integration/kafka/api/BaseProducerSendTest.scala

## Topic

> Using the filesystem and relying on pagecache is superior to maintaining an in-memory cache or other structure
> 
- 메모리에 데이터를 저장하는 Redis VS 디스크에 데이터를 저장하는 Kafka
    - 성능
        - 디스크를 주기억 장치를 이용해도 최대한 캐싱을 이용해서 데이터를 인메모리에 주로 저장하는 방식을 최대한 성능적으로 따라잡으려고 함
    - 철학

## Question

- 서비스 재시작 시에도 캐시 데이터가 유지되는 이유는?
    - 비슷한 개념들 RDB redo log, lucene trasaction log, redis aof
- (skip) Exactly once 는 메시지 손실이 없는가? → 4.7에 있네요.
    - https://kafka.apache.org/documentation/#design
- 쿼터 설정 전략은 어떤 게 있을까? (ex. 네트워크 대역폭, 요청 처리율)
    - 카프카를 제대로 안 써봤는데, 실무에서 써보신 분 어떤 전략들을 사용하시는지..?
        - 쿼터.. 한 번 안써봄..
            - 깃 커밋 로그를 찾아서 PR/Issue를 찾아보면 왜 만들었는지 알 수 있다.
                - 의사결정 과정 확인
- 모든 데이터를 카프카를 활용해서 저장할 수 없을까?
    - → 프로젝트를 처음 시작할 때 이런 구조를 가져갈 수 있을까?
        - 데이터 파이프라인
        - Event sourcing
            - 공감대를 만들 수 있나????
                - 추상화된 개념 잘 짜기 어렵다..
            - 이벤트 순서
            - 기술적으로 뛰어난 회사의 레퍼런스가 있을까?
    - LSM 트리를 사용하는 데이터베이스
        - Cassandra
        - Hbase
- Kafka가 디스크 기반인데도 빠른 이유는?
    - append only
    - OS 페이지 캐시 최대한 활용
    - 비동기 코드 활용 (flush() 등)
        
        ```java
        private def flushDirtyLogs(): Unit = {
            debug("Checking for dirty logs to flush...")
        
            for ((topicPartition, log) <- currentLogs.asScala.toList ++ futureLogs.asScala.toList) {
              try {
                val timeSinceLastFlush = time.milliseconds - log.lastFlushTime
                debug(s"Checking if flush is needed on ${topicPartition.topic} flush interval ${log.config.flushMs}" +
                      s" last flushed ${log.lastFlushTime} time since last flush: $timeSinceLastFlush")
                if (timeSinceLastFlush >= log.config.flushMs)
                  log.flush(false)
              } catch {
                case e: Throwable =>
                  error(s"Error flushing topic ${topicPartition.topic}", e)
              }
            }
          }
        ```
        
- KafkaProducer가 프로듀싱 후 네트워크 흐름은 어떻게 될까?
    - Producer → 브로커에 메세지 전송
    - 브로커 → 메세지 받아서 저장 및 offset 부여, ACK 응답
        - Sender 스레드 → KafkaProducer 내부에서 실제 네트워크 I/O 처리
        - TCP Connection → Kafka는 브로커의 영속 TCP 연결 유지
    - 브로커에서 로그 세그먼트에 기록 (LogManager → Log.append)
        - ISR (In Sync Replica) 모두 복제되었는지 확인 (acks=all)
        - 모든 조건 만족시 ACK 응답
        - recoveryPoints 관련
            
            ```java
            private def checkpointRecoveryOffsetsInDir(logDir: File, logsToCheckpoint: Map[TopicPartition, UnifiedLog]): Unit = {
                try {
                  recoveryPointCheckpoints.get(logDir).foreach { checkpoint =>
                    val recoveryOffsets: Map[TopicPartition, JLong] = logsToCheckpoint.map { case (tp, log) => tp -> long2Long(log.recoveryPoint) }
                    // checkpoint.write calls Utils.atomicMoveWithFallback, which flushes the parent
                    // directory and guarantees crash consistency.
                    checkpoint.write(recoveryOffsets.asJava)
                  }
                } catch {
                  case e: KafkaStorageException =>
                    error(s"Disk error while writing recovery offsets checkpoint in directory $logDir: ${e.getMessage}")
                  case e: IOException =>
                    logDirFailureChannel.maybeAddOfflineLogDir(logDir.getAbsolutePath,
                      s"Disk error while writing recovery offsets checkpoint in directory $logDir: ${e.getMessage}", e)
                }
              }
            ```
            
- 프로듀서, 브로커, 컨슈머 모두 TCP Connection 기반으로 연결되어 있는데, 무한정 keep-alive일까? 아니면 idle timeout을 지정해서 관리할까?
    - Os 설정과 카프카 설정의 조합에 따라 연결은 언젠가 닫힌다.
    - socket.keepalive.enable = true이어야 os-level keepalive 패킷이 주기적으로 전달됨
    - os 레벨에서 tcp_keepalive_time, tcp_keepalive_intvl, probes 등 값에 따라 실제 유지 여부 결정됨
    - `NetworkClient`
- 만약에 연결이 끊긴다면 연결 재시도를 어떤 식으로 진행할까?
    - idle로 인해서 끊긴건지, 리더 브로커가 죽어서 그런건지는 예외 종류보고 결정하려나?
    - `InterBrokerSendThread`
- 브로커들은 컨슈머 그룹에서의 수많은 폴링 요청들을 어떻게 최적화해서 응답할까? → 찾아보기
- 카프카 개발자들은 system call 장인인데, 어떻게 JVM을 우회할 수 있지?
    - zero copy **FileChannel.transferTo()**
        - `private static final FileDispatcher nd = new FileDispatcherImpl();`
        - `n = *nd*.transferTo(fd, position, icount, targetFD, append);`
    - 많은 시스템 콜은 이미 **JDK의 표준 라이브러리**로 추상화 → system call을 정리하고 공부해보자
- 카프카는…deep dive 해보면 재밌을듯
    - 구현의 교과서 느낌으로 활용해보기
        - 네트워크
        - I/O
        - 분산
            - 복제
            - 파티셔닝
            - 합의 (리더 선출)

---

## Next Week

**엘라스틱서치** 하기

딥하게 공부하고 싶은 사람 vs 지식을 넓고 얕게 알고싶은 사람
- 네트워크 → 다다음시간
    - 카프카
    - 레디스
- 엘라스틱서치? lucene?
    - https://github.com/elastic/elasticsearch
    - 코드 쉽게
    - 다음 시간
    - 우리 다 모름..

---

## FeedBack

- 개선점 → 다다음시간 (네트워크) 이후에 진행해보고
    - 딥한 게? → 하나의 기술을 하나의 측면 (ex, 네트워크)
    - 적당히 딥하고 넓게 하고 싶은지? → 하나 기술 다양한 측면에서 (ex, Kafka)
- 계속 할 것인가? (언제까지 할 것인가?)
    - 일단 **2025년 6월 28일 (토)** 까지 해보기
        - 다양한 방식과 형태로 시험
    - 목적? 2025년 6월 28일 (토)에 더 얘기해보기