# Redis, Kafka, Elasticsearch Network

Redis, Kafka, Elasticsearch의 네트워크
- 개인마다 목표를 세워서 실제로 어떻게 동작하는지 코드를 통해 확인하기
    - ex, 카프카 컨슈머와 프로듀서는 브로커와 어떻게 통신하는가?

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
- ES 에서는 어떤 유의어 검색 동작을 가지는가?
    - https://github.com/elastic/elasticsearch/blob/main/modules/analysis-common/src/main/java/org/elasticsearch/analysis/common/SynonymTokenFilterFactory.java → 직접 코드 열어 보려다가 노트북이 너무 느려져서 실패ㅠ
    - 유의어(Synonym) 필터 생성 클래스
    - SynonymTokenFilterFactory.class
        
        ```java
        /*
         * Elasticsearch의 유의어 필터 팩토리 클래스
         * synonym, synonym_graph 등의 필터를 위한 TokenFilterFactory 구현
         */
        
        package org.elasticsearch.analysis.common;
        
        import org.apache.lucene.analysis.Analyzer;
        import org.apache.lucene.analysis.TokenStream;
        import org.apache.lucene.analysis.synonym.SynonymFilter;
        import org.apache.lucene.analysis.synonym.SynonymMap;
        import org.elasticsearch.common.settings.Settings;
        import org.elasticsearch.env.Environment;
        import org.elasticsearch.index.IndexService.IndexCreationContext;
        import org.elasticsearch.index.IndexSettings;
        import org.elasticsearch.index.IndexVersions;
        import org.elasticsearch.index.analysis.*;
        
        import org.elasticsearch.synonyms.SynonymsManagementAPIService;
        
        import java.io.Reader;
        import java.io.StringReader;
        import java.util.List;
        import java.util.function.Function;
        
        public class SynonymTokenFilterFactory extends AbstractTokenFilterFactory {
        
            // 유의어 설정의 소스 타입을 정의하는 enum
            protected enum SynonymsSource {
                // 설정 파일 내에 직접 정의된 유의어
                INLINE("synonyms") {
                    @Override
                    public ReaderWithOrigin getRulesReader(SynonymTokenFilterFactory factory, IndexCreationContext context) {
                        List<String> rulesList = Analysis.getWordList(factory.environment, factory.settings, SynonymsSource.INLINE.getSettingName());
                        StringBuilder sb = new StringBuilder();
                        for (String line : rulesList) {
                            sb.append(line).append(System.lineSeparator());
                        }
                        return new ReaderWithOrigin(new StringReader(sb.toString()), "'" + factory.name() + "' analyzer settings");
                    }
                },
        
                // Elasticsearch 내부 인덱스를 통해 관리되는 유의어 (search-time에서만 사용 가능)
                INDEX("synonyms_set") {
                    @Override
                    public ReaderWithOrigin getRulesReader(SynonymTokenFilterFactory factory, IndexCreationContext context) {
                        if (factory.analysisMode != AnalysisMode.SEARCH_TIME) {
                            throw new IllegalArgumentException(
                                "Can't apply [" + SynonymsSource.INDEX.getSettingName() + "]! Loading synonyms from index is supported only for search time synonyms!"
                            );
                        }
        
                        String synonymsSet = factory.settings.get(SynonymsSource.INDEX.getSettingName(), null);
                        ReaderWithOrigin reader;
        
                        // 인덱스가 생성 중일 땐 빈 유의어 반환 (메타데이터 검증 시 블로킹 방지)
                        if (context != IndexCreationContext.RELOAD_ANALYZERS) {
                            reader = new ReaderWithOrigin(
                                new StringReader(""),
                                "fake empty [" + synonymsSet + "] synonyms_set in .synonyms index",
                                synonymsSet
                            );
                        } else {
                            // 실제 유의어 로딩 (RELOAD_ANALYZERS 시점에만)
                            reader = new ReaderWithOrigin(
                                Analysis.getReaderFromIndex(synonymsSet, factory.synonymsManagementAPIService, factory.lenient),
                                "[" + synonymsSet + "] synonyms_set in .synonyms index",
                                synonymsSet
                            );
                        }
        
                        return reader;
                    }
                },
        
                // 파일 기반 유의어
                LOCAL_FILE("synonyms_path") {
                    @Override
                    public ReaderWithOrigin getRulesReader(SynonymTokenFilterFactory factory, IndexCreationContext context) {
                        String synonymsPath = factory.settings.get(SynonymsSource.LOCAL_FILE.getSettingName(), null);
                        return new ReaderWithOrigin(
                            Analysis.getReaderFromFile(factory.environment, synonymsPath, SynonymsSource.INLINE.getSettingName()),
                            synonymsPath
                        );
                    }
                };
        
                private final String settingName;
        
                SynonymsSource(String settingName) {
                    this.settingName = settingName;
                }
        
                public abstract ReaderWithOrigin getRulesReader(SynonymTokenFilterFactory factory, IndexCreationContext context);
        
                public String getSettingName() {
                    return settingName;
                }
        
                // 설정 정보에서 어떤 SynonymsSource를 쓸지 결정
                public static SynonymsSource fromSettings(Settings settings) {
                    if (settings.hasValue(INLINE.getSettingName())) {
                        return INLINE;
                    } else if (settings.hasValue(INDEX.getSettingName())) {
                        return INDEX;
                    } else if (settings.hasValue(LOCAL_FILE.getSettingName())) {
                        return LOCAL_FILE;
                    } else {
                        throw new IllegalArgumentException("synonym requires either `" + INLINE.getSettingName() + "`, `" + INDEX.getSettingName() + "` or `" + LOCAL_FILE.getSettingName() + "` to be configured");
                    }
                }
            }
        
            // 설정값들
            private final String format;
            private final boolean expand;
            private final boolean lenient;
            protected final Settings settings;
            protected final Environment environment;
            protected final AnalysisMode analysisMode;
            private final SynonymsManagementAPIService synonymsManagementAPIService;
            protected final SynonymsSource synonymsSource;
        
            // 생성자
            SynonymTokenFilterFactory(
                IndexSettings indexSettings,
                Environment env,
                String name,
                Settings settings,
                SynonymsManagementAPIService synonymsManagementAPIService
            ) {
                super(name);
                this.settings = settings;
        
                this.synonymsSource = SynonymsSource.fromSettings(settings);
                this.expand = settings.getAsBoolean("expand", true); // 확장 여부
                this.format = settings.get("format", ""); // wordnet or solr
                boolean updateable = settings.getAsBoolean("updateable", false);
                this.lenient = settings.getAsBoolean(
                    "lenient",
                    indexSettings.getIndexVersionCreated().onOrAfter(IndexVersions.LENIENT_UPDATEABLE_SYNONYMS) && updateable
                );
                this.analysisMode = updateable ? AnalysisMode.SEARCH_TIME : AnalysisMode.ALL;
                this.environment = env;
                this.synonymsManagementAPIService = synonymsManagementAPIService;
            }
        
            // 분석 모드 반환 (색인 시 or 검색 시)
            @Override
            public AnalysisMode getAnalysisMode() {
                return this.analysisMode;
            }
        
            // 일반 create는 예외 발생 — 체인 분석기를 통해 생성되어야 함
            @Override
            public TokenStream create(TokenStream tokenStream) {
                throw new IllegalStateException("Call createPerAnalyzerSynonymFactory to specialize this factory for an analysis chain first");
            }
        
            // 분석 체인 기반으로 실제 TokenFilterFactory 생성
            @Override
            public TokenFilterFactory getChainAwareTokenFilterFactory(
                IndexCreationContext context,
                TokenizerFactory tokenizer,
                List<CharFilterFactory> charFilters,
                List<TokenFilterFactory> previousTokenFilters,
                Function<String, TokenFilterFactory> allFilters
            ) {
                // 유의어 생성용 분석기 생성
                final Analyzer analyzer = buildSynonymAnalyzer(tokenizer, charFilters, previousTokenFilters);
        
                // 설정된 source에서 유의어 파일/데이터 로딩
                ReaderWithOrigin rulesReader = synonymsSource.getRulesReader(this, context);
        
                // SynonymMap 생성
                final SynonymMap synonyms = buildSynonyms(analyzer, rulesReader);
                final String name = name();
        
                // 실제 필터 팩토리 반환
                return new TokenFilterFactory() {
                    @Override
                    public String name() {
                        return name;
                    }
        
                    @Override
                    public TokenStream create(TokenStream tokenStream) {
                        // FST 기반으로 동작, 유의어 없으면 원본 그대로
                        return synonyms.fst == null ? tokenStream : new SynonymFilter(tokenStream, synonyms, false);
                    }
        
                    @Override
                    public TokenFilterFactory getSynonymFilter() {
                        // SynonymMap 생성 시 중첩 유의어 적용 방지
                        return IDENTITY_FILTER;
                    }
        
                    @Override
                    public AnalysisMode getAnalysisMode() {
                        return analysisMode;
                    }
        
                    @Override
                    public String getResourceName() {
                        return rulesReader.resource();
                    }
                };
            }
        
            // 유의어 분석용 Analyzer 생성
            static Analyzer buildSynonymAnalyzer(
                TokenizerFactory tokenizer,
                List<CharFilterFactory> charFilters,
                List<TokenFilterFactory> tokenFilters
            ) {
                return new CustomAnalyzer(
                    tokenizer,
                    charFilters.toArray(new CharFilterFactory[0]),
                    tokenFilters.stream().map(TokenFilterFactory::getSynonymFilter).toArray(TokenFilterFactory[]::new)
                );
            }
        
            // SynonymMap 생성: Solr/WordNet 포맷 분석 후 Map 빌드
            SynonymMap buildSynonyms(Analyzer analyzer, ReaderWithOrigin rules) {
                try {
                    SynonymMap.Builder parser;
                    if ("wordnet".equalsIgnoreCase(format)) {
                        parser = new ESWordnetSynonymParser(true, expand, lenient, analyzer);
                        ((ESWordnetSynonymParser) parser).parse(rules.reader);
                    } else {
                        parser = new ESSolrSynonymParser(true, expand, lenient, analyzer);
                        ((ESSolrSynonymParser) parser).parse(rules.reader);
                    }
                    return parser.build();
                } catch (Exception e) {
                    throw new IllegalArgumentException("failed to build synonyms from [" + rules.origin + "]", e);
                }
            }
        
            // Reader와 출처 정보를 같이 보유하는 내부 record
            record ReaderWithOrigin(Reader reader, String origin, String resource) {
                ReaderWithOrigin(Reader reader, String origin) {
                    this(reader, origin, null);
                }
            }
        }
        
        ```
        
    - SynonymsSource
        - INLINE
            - 설정파일에 직접 규칙 설정 (ex. synonyms: ["happy, joyful", "fast, quick"])
            - 수정하려면 인덱스를 재설정하거나 재배포 필요
            - 설정에서 특정 키(`synonyms`)로 정의된 리스트를 가져와서 StringReader 래핑 + ReaderWriteOrigin 생성
        - INDEX
            - 유의어를 별도 인덱스(document 기반)으로 관리
            - 검색 시점에만 가능, 인덱스 시점 불가능 …?
                - 검색 시점: 검색 쿼리 실행할 때 (쿼리 들어오면 분석 처리)
                - 인덱스 시점: 문서 색인할 때 (ES에 데이터 저장(insert)할 때 분석 처리)
            - 실시간 동의어 갱신 가능
        - LOCAL_FILE
            - 파일 시스템에서 외부 파일로 분리해서 관리
            - 파일 경로를 통해 파일 내용 읽어옴
            - 파일 수정 시 재시작 또는 리로드 필요 (실시간 반영 어려움)
    - ReaderWithOrigin
        - Reader를 감싼 객체(record)
        - Reader reader, String origin, String resource 구체적인 정보를 담고 있음
            - 디버깅 목적인가?
    - SynonymMap
        - Lucene 라이브러리에서 제공하는 자료구조
        - 유의어 쌍(그룹)을 인덱스화 하여, 빠르게 검색하고 적용할 수 있도록 설계
        - FST(Finite State Transducer) 구조 기반 → 메모리 사용 최적화 + 검색 성능 향상
            - 입력 단어/토큰를 빠르게 검색하고 매칭되는 출력(ex. 유의어)을 찾아내는 자료구조
        - example
            
            ```java
            // 분석기 (tokenizer + filters) - SynonymMap 생성 시 필요
            Analyzer analyzer = new StandardAnalyzer();
            
            // SynonymMap.Builder 생성, true는 expand 옵션 (동의어 확장 여부)
            SynonymMap.Builder builder = new SynonymMap.Builder(true);
            
            // 동의어 추가: "car" 와 "automobile" 을 서로 동의어로 등록
            builder.add(new CharsRef("car"), new CharsRef("automobile"), true);
            
            // 동의어 추가: "quick" 와 "fast" 를 서로 동의어로 등록
            builder.add(new CharsRef("quick"), new CharsRef("fast"), true);
            
            // SynonymMap 빌드
            SynonymMap synonymMap = builder.build();
            
            // FST 탐색 클래스를 써서 SynonymMap을 탐색할 수 있고, 일반적으로 직접 호출X
            ```
            
            ```java
            // 동의어 파서(ESSolrSynonymParser 등)를 사용해 규칙을 읽고 파싱
            SynonymMap.Builder parser = new ESSolrSynonymParser(true, expand, lenient, analyzer);
            parser.parse(rulesReader);
            
            // 빌드하여 SynonymMap 생성
            SynonymMap synonymMap = parser.build();
            
            // SynonymFilter에 SynonymMap 넣어 적용
            TokenStream synonymFilteredStream = new SynonymFilter(inputTokenStream, synonymMap, false);
            ```
            

Kafka
- 소켓 서버로 구성되어 있음.
    - https://github.com/apache/kafka/blob/trunk/core/src/main/scala/kafka/network/SocketServer.scala
    - 그럼 결국 컨슈머나 프로듀서와 소켓 연결을 하고 있다는거고, 어디서 어떻게 관리하고 있을까?
    - addListener (https://github.com/apache/kafka/blob/trunk/core/src/main/scala/kafka/network/SocketServer.scala#L1397)
        - counts라는 mutableMap[ip, count]에 synchronized 걸고 로직 시작
            - scala에는 concurrentMap이 없나?라는 생각
            - ip당 count 관리하는 맵 → counts라는 네이밍 모호하다고 생각
        - quota → 할당량
        - *Data Plane : 패킷 송수신 기능 담당*. (forwarding 기능)
- acceptor (https://github.com/apache/kafka/blob/trunk/core/src/main/scala/kafka/network/SocketServer.scala#L474)
    - socketServer 열고 → SocketServerChannel로 통신
    - ⇒ java.nio의 소켓과 소켓채널을 사용함

- Kafka에 데이터를 produce 할 때 replica 들로 데이터가 어떻게 복제?
    - 클라 요청 → KafkaApis(대장 받아서 저장) → 레플리카가 ReplicaFetcher에서 FetchRequest 보냄? → KafkaApis(대장이 응답)
    - -
        - 클라이언트 ProduceRequest 처리 (대장 broker 측)
            - 관련 클래스: `KafkaApis`
            - 클라이언트가 보낸 ProduceRequest를 받아서 데이터 저장
        
        - Follower broker가 대장에 FetchRequest 보내기
            - 관련 클래스: `ReplicaFetcherThreadBenchmark`??, `ReplicaFetcher`, `FetchSessionHandler`
            - Follower가 leader한테 "데이터 좀 줘잉~" 라고 FetchRequest 보냄
        
        - Leader broker가 FetchRequest 처리
            - 관련 클래스: `KafkaApis`?
            - leader가 요청받은 데이터 찾아서 FetchResponse 만들어서 replica에게 응답
        
        - Follower broker가 FetchResponse 받아서 데이터 저장
            - `ReplicaFetcher` → 내부에서 데이터를 처리하고 local replica에 기록
            - `ReplicaManager`?? `ReplicaQuota`??

- broker properties
    ```
    ############################# Log Flush Policy #############################
    
    # Messages are immediately written to the filesystem but by default we only fsync() to sync
    # the OS cache lazily. The following configurations control the flush of data to disk.
    # There are a few important trade-offs here:
    #    1. Durability: Unflushed data may be lost if you are not using replication.
    #    2. Latency: Very large flush intervals may lead to latency spikes when the flush does occur as there will be a lot of data to flush.
    #    3. Throughput: The flush is generally the most expensive operation, and a small flush interval may lead to excessive seeks.
    # The settings below allow one to configure the flush policy to flush data after a period of time or
    # every N messages (or both). This can be done globally and overridden on a per-topic basis.
    
    # The number of messages to accept before forcing a flush of data to disk
    #log.flush.interval.messages=10000
    
    # The maximum amount of time a message can sit in a log before we force a flush
    #log.flush.interval.ms=1000
    ```

---

## Next Week 
    - 회사에서 개발할 때 궁금했던 내용
        - 카프카 리밸런싱 중 `CooperativeStickyAssignor`
        - fixture-monkey
        - lettuce
        - jackson, gson
        - eclipse collection

## **10주차 lettuce**

- mget, ttl 이 어떻게 하나의 인터페이스에서 일관되게 **redis, redis cluster, redis sentinel로** 요청을 보내는지
    - mget `public RedisFuture<List<KeyValue<K, V>>> mget(K... keys) {    return dispatch(commandBuilder.mgetKeyValue(keys));}`
    - `connection.dispatch`
        - connection 구현체 살펴보기