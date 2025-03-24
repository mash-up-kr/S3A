## RealMySQL 정복 (그런데 공식문서를 곁들인)

[Real MySQL 1판](https://product.kyobobook.co.kr/detail/S000001766482)

15기니까 최대 15주안에 정복해버리기

---

## 진행 시간

* 시작 날짜: 2025.03.24 (킥오프)
* 종료 날짜: 2025.???
* 스터디 시간: 매 주 월요일 22:00 ~ ???

---

## 진행 시간

* 시작 날짜: 2025.03.24 (킥오프)
* 종료 날짜: 2025.???
* 스터디 시간: 매 주 월요일 22:00 ~ ???

---

## Our Ground Rules

- PR은 스터디 직전까지 올린다.
- 결석자는 재판을 통해 `룰렛형`에 처한다.
- 지각생은 스터디 내내 말 할때마다 `온갖 구박과 박해`
- 주차 별로 이슈를 생성하여 질문,생각 및 공유하고 싶은 자료를 (적극적으로) 업로드한다.
- 스터디 일정은 월요일 21시로 되어있지만, 모두의 동의 하에 언제든 변동 가능
  - 본인의 일정상 참여가 어려울 경우 일자 변동 요청을 적극적으로 요청
  - 회사 및 업무적인 이유(회식 포함): 변동 요청 必 이나, 갑작스러울 경우 팀원 과반 수 합리적 판단하에 룰렛형 예외 가능
- 매주 스터디 인증 사진을 남긴다.

---

## 커리큘럼

### 04장: 아키텍처 관련 공식 문서

#### 4.1 MySQL 엔진 아키텍처
- [MySQL Server Architecture](https://dev.mysql.com/doc/refman/8.0/en/mysqld-server.html)
- [MySQL 스레딩 구조](https://dev.mysql.com/doc/refman/8.0/en/thread-pool.html)
- [메모리 사용 구조](https://dev.mysql.com/doc/refman/8.0/en/memory-use.html)
- [플러그인 스토리지 엔진 모델](https://dev.mysql.com/doc/refman/8.0/en/storage-engines.html)
- [컴포넌트](https://dev.mysql.com/doc/refman/8.0/en/server-components.html)
- [쿼리 실행 구조](https://dev.mysql.com/doc/refman/8.0/en/query-cache.html)
- [복제](https://dev.mysql.com/doc/refman/8.0/en/replication.html)
- [스레드 풀](https://dev.mysql.com/doc/refman/8.0/en/thread-pool.html)

#### 4.2 InnoDB 스토리지 엔진 아키텍처
- [InnoDB 소개](https://dev.mysql.com/doc/refman/8.0/en/innodb-introduction.html)
- [클러스터링 인덱스](https://dev.mysql.com/doc/refman/8.0/en/innodb-index-types.html)
- [외래 키 지원](https://dev.mysql.com/doc/refman/8.0/en/innodb-foreign-key-constraints.html)
- [MVCC](https://dev.mysql.com/doc/refman/8.0/en/innodb-multi-versioning.html)
- [버퍼 풀](https://dev.mysql.com/doc/refman/8.0/en/innodb-buffer-pool.html)
- [Double Write Buffer](https://dev.mysql.com/doc/refman/8.0/en/innodb-doublewrite-buffer.html)
- [언두 로그](https://dev.mysql.com/doc/refman/8.0/en/innodb-undo-logs.html)
- [리두 로그](https://dev.mysql.com/doc/refman/8.0/en/innodb-redo-log.html)

#### 4.3 MyISAM 스토리지 엔진 아키텍처 (스킵)
- [MyISAM 스토리지 엔진](https://dev.mysql.com/doc/refman/8.0/en/myisam-storage-engine.html)
- [키 캐시](https://dev.mysql.com/doc/refman/8.0/en/myisam-key-cache.html)

#### 4.4 MySQL 로그 파일
- [로그 파일 개요](https://dev.mysql.com/doc/refman/8.0/en/server-logs.html)
- [에러 로그](https://dev.mysql.com/doc/refman/8.0/en/error-log.html)
- [제너럴 쿼리 로그](https://dev.mysql.com/doc/refman/8.0/en/query-log.html)
- [슬로우 쿼리 로그](https://dev.mysql.com/doc/refman/8.0/en/slow-query-log.html)

### 05장: 트랜잭션과 잠금 관련 공식 문서

#### 5.1 트랜잭션
- [트랜잭션 소개](https://dev.mysql.com/doc/refman/8.0/en/innodb-transaction-model.html)

#### 5.2 MySQL 엔진의 잠금 & 5.3 InnoDB 스토리지 엔진 잠금
- [InnoDB 잠금](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking.html)
- [테이블 락](https://dev.mysql.com/doc/refman/8.0/en/lock-tables.html)
- [네임드 락](https://dev.mysql.com/doc/refman/8.0/en/locking-functions.html)
- [메타데이터 락](https://dev.mysql.com/doc/refman/8.0/en/metadata-locking.html)

#### 5.4 MySQL의 격리 수준
- [격리 수준](https://dev.mysql.com/doc/refman/8.0/en/innodb-transaction-isolation-levels.html)

### 06장: 데이터 압축 관련 공식 문서
- [InnoDB 테이블 압축](https://dev.mysql.com/doc/refman/8.0/en/innodb-table-compression.html)
- [페이지 압축](https://dev.mysql.com/doc/refman/8.0/en/innodb-page-compression.html)

### 07장: 데이터 암호화 관련 공식 문서 (스킵)
- [데이터 암호화 개요](https://dev.mysql.com/doc/refman/8.0/en/encryption.html)
- [키링 플러그인](https://dev.mysql.com/doc/refman/8.0/en/keyring.html)
- [테이블 암호화](https://dev.mysql.com/doc/refman/8.0/en/innodb-data-encryption.html)
- [바이너리 로그 암호화](https://dev.mysql.com/doc/refman/8.0/en/binary-log-encryption.html)

### 08장: 인덱스 관련 공식 문서
- [MySQL 인덱스 개요](https://dev.mysql.com/doc/refman/8.0/en/optimization-indexes.html)
- [B-Tree 인덱스](https://dev.mysql.com/doc/refman/8.0/en/index-btree-hash.html)
- [공간 인덱스(R-Tree)](https://dev.mysql.com/doc/refman/8.0/en/spatial-types.html)
- [전문 검색 인덱스](https://dev.mysql.com/doc/refman/8.0/en/fulltext-search.html)
- [함수 기반 인덱스](https://dev.mysql.com/doc/refman/8.0/en/create-index.html#create-index-functional-key-parts)
- [멀티 밸류 인덱스](https://dev.mysql.com/doc/refman/8.0/en/create-index.html#create-index-multi-valued)
- [외래키](https://dev.mysql.com/doc/refman/8.0/en/create-table-foreign-keys.html)

### 09장: 옵티마이저와 힌트 & 10장: 실행 계획 관련 공식 문서
- [옵티마이저 소개](https://dev.mysql.com/doc/refman/8.0/en/optimizer-overview.html)
- [쿼리 실행 계획](https://dev.mysql.com/doc/refman/8.0/en/execution-plan-information.html)
- [옵티마이저 힌트](https://dev.mysql.com/doc/refman/8.0/en/optimizer-hints.html)
- [통계 정보](https://dev.mysql.com/doc/refman/8.0/en/optimizer-statistics.html)
- [히스토그램](https://dev.mysql.com/doc/refman/8.0/en/optimizer-statistics.html#optimizer-statistics-histogram)
- [코스트 모델](https://dev.mysql.com/doc/refman/8.0/en/cost-model.html)