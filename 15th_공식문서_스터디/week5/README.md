# Redis

## Document

- https://aws.amazon.com/ko/nosql/in-memory/
- http://oldblog.antirez.com/post/redis-manifesto.html
- 이벤트 라이브러리 https://redis.io/docs/latest/operate/oss_and_stack/reference/internals/internals-rediseventlib/
- 지속성 http://oldblog.antirez.com/post/redis-persistence-demystified.html
- redis vs memcached https://antirez.com/news/94
- https://blog.wjin.org/posts/redis-event-library.html
- (Optional) https://www.infoworld.com/article/2256888/when-to-use-a-crdt-based-database.html
    - 분산 환경에서의 redis 가 궁금하면 한 번쯤 볼만함 (CRDT)

## Code

- zmalloc https://github.com/redis/redis/blob/bd3c1e1bd7049a879c2a3902aaca8745f18c044f/src/zmalloc.c#L149
    - https://github.com/redis/redis/blob/bd3c1e1bd7049a879c2a3902aaca8745f18c044f/src/zmalloc.c#L118

## Reference

- event library
- durability (persistence)
    - http://oldblog.antirez.com/post/redis-persistence-demystified.html
    - client → database → OS → disk controller → physical media
    - `메모리 → 디스크` 과정에서의 주의점
        - data safety
        - `write` vs `fsync`

## Question

1. fsync가 mmap system call 보다 얼마나 느릴까?
    1. 일반적인 데이터베이스 서버 환경(RAID10) 에서 얼마나 차이가 날까….?
    2. redis뿐만 아니라 DB, ES에서도 fsync와 mmap을 구분해서 사용하는데, 정말로 얼마나 느릴지가 궁금.
    3. fsync가 정말 느리다는 지속성에 관한 글도 13년전 글.
    4. GPT 의 리서치 결과
        1. https://chatgpt.com/c/6814a385-64dc-800c-9e47-e1197ba30c81
    - 리서치 (최신 리눅스에서의 mmap vs fsync 성능 비교 보고서)
    
2. zmalloc_get_allocator_info() 이 함수는 왜 1을 return 하나..?
    1. https://www.youtube.com/watch?v=j6m7D72nOmE 이거 보면 좋을듯.
        1. 경환) 한 줄 요약: 리눅스는 예외를 처리하기 까다롭기 때문에 성공 여부를 메서드에서 명시적으로 반환하는 게 낫다
    2. **0/1을 *불리언 성공표시*로 쓸 뿐, 실패‑코드가 필요 없는 구조**라서 항상 1을 반환한다.
    3. zmalloc_get_allocator_info()= “jemalloc에서 힙·단편화·RSS 통계를 읽어와 INFO memory에 넘겨 주는 함수” return 1; 은*통계 집계가 끝났음을 알리는 단순한 성공 표시*일 뿐, 값 자체는 포인터 매개변수에 채워져 전달된다.
    4. 하위호환성 유지 위한 관례 (C99 부터 bool 타입 추가 됨.)
    
    ```c
    FILE *fp = fopen("data.txt", "r");
    if (!fp) {
        perror("fopen");   // errno 기반 메시지 출력
        return -1;         // 호출자에게 에러 전달
    }
    ```
    
    ```java
    /* Get memory allocation information from allocator.
     *
     * refresh_stats indicates whether to refresh cached statistics.
     * For the meaning of the other parameters, please refer to the function implementation
     * and INFO's allocator_* in redis-doc. */
    int zmalloc_get_allocator_info(int refresh_stats, size_t *allocated, size_t *active, size_t *resident,
                                   size_t *retained, size_t *muzzy, size_t *frag_smallbins_bytes)
    {
        size_t sz;
        *allocated = *resident = *active = 0;
    
        /* Update the statistics cached by mallctl. */
        if (refresh_stats) {
            uint64_t epoch = 1;
            sz = sizeof(epoch);
            je_mallctl("epoch", &epoch, &sz, &epoch, sz);
        }
    
        sz = sizeof(size_t);
        /* Unlike RSS, this does not include RSS from shared libraries and other non
         * heap mappings. */
        je_mallctl("stats.resident", resident, &sz, NULL, 0);
        /* Unlike resident, this doesn't not include the pages jemalloc reserves
         * for re-use (purge will clean that). */
        je_mallctl("stats.active", active, &sz, NULL, 0);
        /* Unlike zmalloc_used_memory, this matches the stats.resident by taking
         * into account all allocations done by this process (not only zmalloc). */
        je_mallctl("stats.allocated", allocated, &sz, NULL, 0);
    
        /* Retained memory is memory released by `madvised(..., MADV_DONTNEED)`, which is not part
         * of RSS or mapped memory, and doesn't have a strong association with physical memory in the OS.
         * It is still part of the VM-Size, and may be used again in later allocations. */
        if (retained) {
            *retained = 0;
            je_mallctl("stats.retained", retained, &sz, NULL, 0);
        }
    
        /* Unlike retained, Muzzy representats memory released with `madvised(..., MADV_FREE)`.
         * These pages will show as RSS for the process, until the OS decides to re-use them. */
        if (muzzy) {
            char buf[100];
            size_t pmuzzy, page;
            snprintf(buf, sizeof(buf), "stats.arenas.%u.pmuzzy", MALLCTL_ARENAS_ALL);
            assert(!je_mallctl(buf, &pmuzzy, &sz, NULL, 0));
            assert(!je_mallctl("arenas.page", &page, &sz, NULL, 0));
            *muzzy = pmuzzy * page;
        }
    
        /* Total size of consumed meomry in unused regs in small bins (AKA external fragmentation). */
        *frag_smallbins_bytes = zmalloc_get_frag_smallbins();
        return 1;
    }
    ```
3. aeCreateTimeEvent 는 링크드 리스트형식으로 event를 관리하는데, TTL을 가진 키가 많아지면 O(N)의 시간 복잡도를 어떻게 처리하지?
    1. redis 에서 TTL 만료 처리는 time event 에 속하지 않는다.
    2. serverCron() 이라는걸 100ms 정도마다(설정가능) timeEvent로 등록을 해놓고 실행시킴
    3. serverCron은 다음과 같은 일을 함 → 일종의 서버 관리 작업
        
        serverCron은 “메인 스레드에서 주기적으로 호출되는 동기 `함수`”다.
        
        · 타임 이벤트로 예약되어 루프가 한 바퀴 돌 때마다 실행되고,
        
        · 무거운 일은 내부적으로 bio 스레드나 io‑threads에 위임하지만,
        
        · 함수 자체가 멀티스레드로 돌아가지는 않는다 → 그래서 Redis는 여전히 “싱글 스레드 코어 로직”을 유지한다.
        
    4. **time event = Redis 이벤트 루프 안에서 주기·지연 작업을 예약해 두는 타이머 객체**
        1. 파일 I/O 이벤트와 **같은 싱글 스레드 루프**에서 돌아가므로 락 없이도 일관성을 유지하면서, 필요 작업(serverCron 등)을 틱‑틱‑틱 실행할 수 있게 해 줌.
    
    | 작업 | 설명 |
    | --- | --- |
    | 만료 키 샘플링 삭제 | TTL 처리 → 일부만 만료처리 시킴 이를 active expire 라고 함 |
    | 클라이언트 timeout 확인 | 오래된 연결 정리 |
    | AOF, RDB 체크 | 백그라운드 작업 상태 점검 |
    | 복제 및 클러스터 heartbeat | ping/pong 처리 |
    | 통계 업데이트 | ops/sec, memory usage 등 |
    
    샘플링되지 않는 TTL키는 조회할 때 TTL 이 지나있으면 만료처리되는 lazy expire 을 함.
    
    진짜 gpt 있으니까 공부하기 넘 편하다… → 논문이랑 글 넣어놓고 질의응답하면 개꿀임
    
    [978-3-319-19129-4_6.pdf](attachment:014ef679-529d-4f54-a34d-4c455c2c2830:978-3-319-19129-4_6.pdf)
    
4. Redis 키 만료 처리: 전부가 아닌 일부인 이유?
    1. https://www.pankajtanwar.in/blog/how-redis-expires-keys-a-deep-dive-into-how-ttl-works-internally-in-redis ← 정리하면 접근하거나, 무작위로만 키들 만료 시도
    2. CPU 사용량 제한, 단점은 접근이 없으면 만료가 늦어질 수 있다는 점
    3. 키 만료 이벤트는 정상적으로 작동 안할 수 있나?
        1. 넵 제대로 안할지도 모릅니다
    4. **하이럼의 법칙**
        - API사용자가 충분히 많다면 API 명세에 적힌 내용은 중요하지 않다. ㅋㅋ
        - 시스템에서 눈에 보이는 모든 행위(동작)를 누군가는 이용하게 될 것이기 때문이다.
5. 왜 redis는 빠른가? 인덱스는 왜 필요없는가?
    1. 메모리에 존재하고 키-값 자료구조이므로 O(1)로 조회 가능.
    2. RDB는 왜 못하는가? 
        1. 데이터를 디스크에 저장한다. 디스크 I/O 발생.
        2. 디스크 I/O를 줄이기 위해 인덱스를 메모리에 페이징함. O(logN)
        3. 트랜잭션 동시성 처리 필요 (잠금이 필요함)
    3. Redis의 복잡성 피하기
        1. 유사 사례, InnoDB의 쿼리캐싱
    4. 인덱스 = 정렬 → 레디스에서 정렬은 필요 없을까?
    5. manual indexing
        1. redis 데이터를 조회할 때 CrudRepository를 사용하면 암시적으로 사용해줌
        2. 근데 정리는 안해줌
        3. 팬텀 객체들이 매우 많이 생기고, 인덱스에서 값 삭제를 제대로 안해줌
    6. 원영) 어디 레퍼인지 정확히 못 찾겠는데, 속도를 매우 우선시해서, 인덱싱과 같은 기능은 사용자 정의 맡기겠다는 철학을 언급한 걸 읽은 것 같음

---

## Next Week
- redis → https://sqlite.org/atomiccommit.html
- 다음주 kafka 관련 주제로