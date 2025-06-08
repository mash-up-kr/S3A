# Redis, Kafka, Elasticsearch Network

## Code

- Redis
    
    ```c
    [initServer()]
          |
          | -- aeCreateFileEvent(server.el, server.ipfd, AE_READABLE, acceptTcpHandler, NULL)
          |
    [main()]
          |
          | -- aeMain(server.el) ------------------------------------
                                 |
                                 | -- aeProcessEvents() -- OS 이벤트 대기(epoll_wait)
                                 |
                                 | -- 이벤트 발생 --> 등록된 콜백 실행
                                            |
                                            |-- acceptTcpHandler() 호출 (클라이언트 연결 수락)
    
    ```
    
    - 이벤트 핸들러를 등록하는 메서드
        - aeCreateFileEvent
            - connSetWriteHandler (내부적으로는 aeCreateFileEvent가 쓰임)
            - connSetReadHandler (내부적으로는 aeCreateFileEvent가 쓰임)
    - server
        - `server.c`
            - anetTcpServer
                - tcp 소켓을 열고 listen 상태로
                
                ```c
                for (p = servinfo; p != NULL; p = p->ai_next) {
                        if ((s = socket(p->ai_family,p->ai_socktype,p->ai_protocol)) == -1)
                            continue;
                
                        if (af == AF_INET6 && anetV6Only(err,s) == ANET_ERR) goto error;
                        if (anetSetReuseAddr(err,s) == ANET_ERR) goto error;
                        if (anetListen(err,s,p->ai_addr,p->ai_addrlen,backlog,0) == ANET_ERR) s = ANET_ERR;
                        goto end;
                    }
                ```
                
            - servinfo는 addrinfo 구조체 리스트
                - 하나의 호스트/서비스 이름이 여러가지 네트워크 주소를 가질 수 있기 때문.
                    - IPv4, IPv6
                    - 여러 네트워크 인터페이스
                    - 다양한 프로토콜/소켓
            - backlog → 대기열 큐
                - listen()을 호출할 때 지정하는 큐의 크기.
                - 클라이언트가 접속을 시도했을 때 커널이 얼마나 많은 연결 요청을 대기 상태로 보관할 수 있는지를 나타냄
            - `ae.c`
                - `aeProcessEvents`
                
                ```c
                 int numevents = aeApiPoll(eventLoop, tvp); // OS 레벨에서 이벤트 감지
                    ...
                    for (j = 0; j < numevents; j++) {
                		    aeFileEvent *fe = &eventLoop->events[fired[i].fd];
                        int mask = fired[i].mask;
                        
                        if (mask & AE_READABLE)
                            fe->rfileProc(eventLoop, fd, fe->clientData, mask);
                        if (mask & AE_WRITABLE)
                            fe->wfileProc(eventLoop, fd, fe->clientData, mask);
                    }
                ```
                
            - `aeCreateFileEvent(server.el, server.ipfd, AE_READABLE, acceptTcpHandler, NULL);`
                - ipfd → listen 소켓 파일 디스크립터
    - client
        - 호출처 (조립하는 곳)  `example-ae.c`
            - https://github.com/redis/redis/blob/7f60945bc60845fdc3bdc53d642a32c65f21e57c/deps/hiredis/examples/example-ae.c
            - `hiredis`는 **C 언어로 작성된 Redis의 공식 클라이언트 라이브러리**입니다.
        - C코드 읽기 어렵다 - 경환 +1
    - `networking.c`
        - anetTcpAccept
        - acceptCommonHandler
            - `socket.c`
        - `processCommand`
- Kafka
    - 내부적으로 여러 listener가 존재함.
        - 다른 포트/프로토콜/보안 설정이 가능
        - `KafkaConfig#effectiveAdvertisedControllerListeners`
    - 개념: `dataPlane`, `controller`
        - dataPlane → produce/fetch/consumer/topic 데이터 처리
        - controller → 토픽 생성/삭제, 파티션 리더 선출, 클러스터 상태 변화 감지
    - `RequestChannel`
        - `Processor`
            
            ```scala
                     configureNewConnections()
                      // register any new responses for writing
                      processNewResponses()
                      poll()
                      processCompletedReceives()
                      processCompletedSends()
                      processDisconnected()
                      closeExcessConnections()
            ```
            
    - *연결 `BrokerServer#socketServer`*
    
    ```scala
     private def accept(key: SelectionKey): Option[SocketChannel] = {
        val serverSocketChannel = key.channel().asInstanceOf[ServerSocketChannel]
        val socketChannel = serverSocketChannel.accept()
        val listenerName = ListenerName.normalised(endPoint.listener)
        ...
      }
    ```
    
    - 컨슈머 → 브로커 형태 `FetchRequest`, `ClientRequest`
        - 테스트 `KafkaConsumerTest#testSimpleProduceRequest`
    - 프로듀서 → 브로커 형태 `ProduceRequest`
        - `ProduceRequestTest#testSimpleProduceRequest`
- Elasticsearch
    - NodeClient
        - 클러스터 내부 클라이언트, 같은 JVM 안에서 실행
        - `TrasnportService#sendLocalRequest`
    - RestClient (외부 HTTP 클라이언트)
        - `#performRequest -> selectNodes`
    - `SourceFetchingIT#testSourceDefaultBehavior`
    
    ```java
     public Transport.Connection getConnection(DiscoveryNode node) {
            if (isLocalNode(node)) {
                return localNodeConnection;
            } else {
                return connectionManager.getConnection(node);
            }
        }
    ```
    

## Question

- Redis는 코드가 매우 정직해서 익숙해지니 오히려 보기 편했다. (like Go)
- 카프카에서 연결할 때 여러 보안 방법을 지원하게 된 이유는 뭐였을까? `advertised.listener` 와 `listener` 가 나뉜 이유는 뭘까?
- 요즘은 `select()` 를 사용안할텐데 자바에서 `Selector` 라는 이름을 사용하게 된 이유는 뭘까? (헷갈림)
    - 왜 Kafka에서는 Acceptor가 `listen`, `accept`를 전부 다 하는가
        
        ```java
         def start(): Unit = synchronized {
            try {
              if (!shouldRun.get()) {
                throw new ClosedChannelException()
              }
              if (serverChannel == null) {
                serverChannel = openServerSocket(endPoint.host, endPoint.port, listenBacklogSize)
                ...
              }
              ...
            }
        ```
        
        ```java
        override def run(): Unit = {
            serverChannel.register(nioSelector, SelectionKey.OP_ACCEPT)
            try {
              while (shouldRun.get()) {
                try {
                  acceptNewConnections()
                  closeThrottledConnections()
                }
              ...
            }
          }
        ```
        
- Elasticsearch는 클러스터 내부 로컬 통신/네트워크 통신 나눠놓은 게 신기했다.
- Kafka -> 멀티스레드 계층 구조, ES -> 샤드 단위 스레드 풀 병렬 분산으로 병렬 처리하지만 Redis는 단일 스레드인데 병렬 시스템 보다 좋은 성능을 낼 수 있을까?
    - ES
        - https://github.com/elastic/elasticsearch/blob/main/server/src/test/java/org/elasticsearch/rest/RestControllerTests.java
        - RestController → RestHandler → ThreadPool 실행 → RestChannel 응답
    - Kafka
        - Acceptor ← Selector ← Proㄴcessor
    - Redis
        - https://github.com/redis/redis/blob/unstable/src/replication.c
    - 멀티쓰레딩
        - 오히려 성능이 떨어짐
        - 큰 요청이 아니면 유의미하지 않을수도
        - 동기화 비용이 크다는 점 고려
