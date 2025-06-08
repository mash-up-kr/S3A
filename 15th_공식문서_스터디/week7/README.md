# Elasticsearch

## Document

- https://www.elastic.co/blog/what-is-an-elasticsearch-index
- https://www.elastic.co/docs/manage-data/data-store/index-basics
- https://www.elastic.co/docs/manage-data/data-store/near-real-time-search
- https://www.elastic.co/docs/manage-data/data-store/text-analysis
- (Optional) https://www.elastic.co/docs/manage-data/data-store/data-streams
    - https://www.elastic.co/docs/manage-data/lifecycle
- https://mash-up.tistory.com/entry/%EC%8A%A4%ED%94%84%EB%A7%81%ED%8C%80-%EA%B2%80%EC%83%89-%EA%B8%B0%EC%88%A0%EC%9D%98-%EC%9B%90%EB%A6%AC-%EB%B0%9C%ED%91%9C-%EC%84%B8%EC%85%98

## Repository

- https://github.com/elastic/elasticsearch

### Code

- Text Analysis
    - 진입점 `IcuTokenizerFactoryTests#testMultipleIcuCustomizeRuleFiles`
- Client
    - 진입점 `AbstractClientHeadersTestCase#testActions`
        - https://github.com/elastic/elasticsearch/blob/d95b037d432e1a76941fada7285f23cb2d7465ec/server/src/test/java/org/elasticsearch/client/internal/AbstractClientHeadersTestCase.java#L96-L96
    - 요청을 TaskManager에서 처리
        - https://github.com/elastic/elasticsearch/blob/d95b037d432e1a76941fada7285f23cb2d7465ec/server/src/main/java/org/elasticsearch/tasks/TaskManager.java#L65-L65
            - TransportAction

## Topic

- `index` vs `data stream`
    - index는 elasticsearch의 기본 저장 개념
    - data stream은 시계열 데이터를 위한 인덱스 묶음

## Question

- ES도 append-only를 채택하고 있는데, 사용하는 시스템콜과 프로세스가 kafka랑 유사한가?
    - https://www.elastic.co/docs/reference/elasticsearch/index-settings/translog
    - https://github.com/elastic/elasticsearch/blob/main/server/src/main/java/org/elasticsearch/index/translog/TranslogWriter.java
    - 저장 방식:
        - ES: Segment(인덱스 파일) + Translog(append-only)
            - +) Translog는 Lucene에 XXX
        - Kafka: Log Segment(append-only) + Recovery point(복구 지점 표시)
    - 디스크 반영 시점:
        - ES: fsync() 수행 시 → 버퍼가 다 차면 Translog와 Segment 모두 디스크에 안전히 기록됨
            - +) flush() 시점이 버퍼가 다 찬 경우를 제외하고도 있을
        - Kafka: fsync() or flush 조건 도달 시 → Page Cache에 있던 내용 디스크로 flush
    - 장애 복구 방식:
        - ES: Segment 로드 + Translog replay로 마지막 커밋 이후 작업 복구
            - https://www.elastic.co/docs/reference/elasticsearch/index-settings/recovery-prioritization
        - Kafka: Recovery Point 이후 log segment 무시 or truncate (flush 안 된 부분은 복구 대상 아님)
- 왜 REST API를 공식 인터페이스로 선택했을까?
    - `Free and Open Source, Distributed, RESTful Search Engine`
    - 시작부터 정해놓고 시작함.
    - 체스터슨의 울타리
- 왜 전문검색할 때 역색인을 쓸까?
    - Revisiting the Inverted Indices for Billion-Scale Approximate Nearest Neighbors https://arxiv.org/pdf/1802.02422
    - 전문검색 후보
        - 역색인
        - 벡터 검색
        - 하이브리드 검색
        - Suffix Array
        - N-gram 기반 Brute-force
    
    | 방법 | 장점 | 단점 | 추천 상황 |
    | --- | --- | --- | --- |
    | 역색인 | 빠르고 정확 | 의미 이해 부족 | 키워드 중심 검색 |
    | 벡터 검색 | 의미 기반 검색 | 느릴 수 있음 | 유사 질문/문장 검색 |
    | 하이브리드 | 정확 + 의미 | 시스템 복잡 | 정밀 검색 필요 시 |
    | Suffix Array | 압축 + 검색 | 구축 비용 큼 | DNA, 로그 분석 |
    | N-gram | 오타 대응 | 인덱스 큼 | 퍼지 검색 |
- 왜 요청에 **Reference counting** 을 쓰고 있을까?
    - `org.elasticsearch.core.RefCounted`
    - https://github.com/elastic/elasticsearch/commit/347ce36654449afcda68823bb774fafdb4c9619b#diff-b6d801fcf897640644686ad4cbf9a05492503efa96415eecf90c9a10f95f3a1e
    - AmazonS3Reference, AmazoneEc2Reference
- ES는 어떻게 성능 최적화를 했을까? → 어떻게 지연 시간이 최대 1초
    - 역색인 → 위에 누가 정리해줌
    - Lucene 세그먼트 구조 → 불변 단위 구조 + 빠른 병렬 탐색 + 세그먼트 merge를 통해 최적화
        - 세그먼트 merge? → 작은 세그먼트들을 하나로 합친다
            - 왜? → 세그먼트가 너무 많아지면 검색 효율이 낮아짐
        - LSM 트리
            - 경환씨 블로그 내용
            - 데이터베이스 인터널스 (장)
    - 메모리 기반 캐시와 필터 → 자주 조회되는 쿼리 결과는 캐싱
        - mysql 5 구조에 이런 시도가 있었음? → 쿼리 캐시
        - 세그먼트 단위로 캐싱할듯
    - 분산 구조와 샤딩 → 수평 확장성 + 병렬 처리
    - NRT 처리 → (Near Real Time) → 특정 간격마다 refresh 요청을 보내 최신 데이터 반영
- 엘라스틱서치에서 말하는 커밋은 정확히 어떤 일을 뜻하는 걸까? (Lucene commit)
    - 페이지 캐시에 데이터를 넘겨주기까지만 함 → fsync를 통해 주기적으로 커널의 페이지 캐시와 디스크의 내용을 싱크를 맞춤 → 이걸 루씬 커밋이라고 함
        - `애플리케이션 → 버퍼풀 → 파일시스템 → 페이지 캐시 → 디스크`
    - 루씬 커밋까지 되어야 디스크에 안전하게 기록
        - 위에서 말했던 버퍼에 모아뒀다가 커밋하는데, 그 사이에 장애가 일어나면?
        - 이런 문제를 해결하기 위해 엘라스틱 서치 샤드는 모든 작업마다 translog라는 작업 로그를 남김
        - 작업 요청 = 루씬 인덱스에 작업 수행 + Translog 기록까지 끝나야 성공으로 승인
            - 기록에 성공했지만 커밋까지 안된내용 → translog를 보고 샤드를 복구
- 루씬 인덱스와 엘라스틱 서치 인덱스와는 어떻게 다른걸까?

- 엘라스틱 서치 레벨의 인덱스는 분산 검색을 진행
    - 루씬 인덱스는 분산 검색이 불가능한데, 엘라스틱 서치 인덱스를 나눈 샤드에 각각 존재
    - 따라서 검색 요청 → 해당하는 각 샤드를 대상으로 검색 → 그 결과를 모아 병합하여 최종 응답
- 여러 세그먼트 → 루씬 인덱스 → 분산 저장 단위인 샤드 → 엘라스틱 인덱스 → 클러스터

**percolate** (여과기)

- 퍼콜레이터 (Percolator)는 빅테이블이라는 분산 데이터베이스에서 트랜잭션 API를 제공하는 라이브러리다. 기존 시스템 위에 트랜잭션 API를 구현할 수 있는 좋은 방법이다. 퍼콜레이터는 데이터 레코드와 커밋된 데이터의 위치 (쓰기 메타데이터)를 저장하고 칼럼 단위 잠금을 제공한다. 경쟁 상태를 방지하고 단일 RPC 호출에서 안전하게 테이블에 잠금을 걸 수 있도록 단일 원격 호출로 읽기-수정-쓰기 작업을 수행할 수 있는 빅테이블 API를 사용한다.

https://www.elastic.co/docs/reference/query-languages/query-dsl/query-dsl-percolate-query

https://wedul.site/501

https://d2.naver.com/helloworld/1044388

---
## Next Week

예고: 네트워크… 실제 구현
- redis
- kafka
- elasticsearch