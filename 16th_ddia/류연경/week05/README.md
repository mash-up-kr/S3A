
## ACID의 의미

트랜잭션이 보장하는 안전성을 ACID라 부르지만, 실제 의미는 DB마다 다르다. 

### Atomicity (원자성)

**전부 성공하거나 전부 실패**하거나.

### Consistency (일관성)

데이터에 대한 **불변식(invariant)** 이 항상 유지되어야 한다.```

### Isolation (격리성)

동시에 실행되는 트랜잭션들이 서로 **간섭하지 않는 것처럼** 보이게 하는 것.

```
이상적인 격리 (직렬성):
트랜잭션들이 마치 하나씩 순서대로 실행된 것과 같은 결과

현실:
성능 때문에 완벽한 격리는 비쌈 → 약한 격리 수준을 사용
→ 이 때문에 각종 동시성 버그 발생
```

- 이 장의 핵심 주제
- 격리 수준별로 어떤 이상 현상(anomaly)이 발생하는지가 중요

### Durability (지속성)

트랜잭션이 커밋되면, 그 데이터는 **장애가 나도 유실되지 않는다.**

- 단일 노드: 디스크에 기록 (WAL 등)
- 복제 DB: 여러 노드에 복제 완료
- 완벽한 지속성은 존재하지 않음 (디스크 전부 고장나면?)
- 복제 + 백업의 조합으로 위험을 줄임

---

## 단일 객체 vs 다중 객체 연산

### 단일 객체 연산

하나의 레코드에 대한 원자적 쓰기.


### 다중 객체 트랜잭션

여러 레코드(또는 여러 테이블)를 하나의 트랜잭션으로 묶는 것.

---

## 약한 격리 수준 (Weak Isolation Levels)

완벽한 격리(직렬성)는 성능 비용이 크다. 그래서 대부분의 DB는 약한 격리를 기본으로 쓴다. 하지만 약한 격리는 **미묘한 버그**를 유발한다.

### 격리 수준 1: Read Committed (커밋된 읽기)

**가장 기본적인 격리 수준.** 두 가지를 보장한다.

#### 보장 1: 더티 읽기 방지 (No Dirty Reads)

커밋되지 않은 데이터를 다른 트랜잭션이 읽을 수 없다.

#### 보장 2: 더티 쓰기 방지 (No Dirty Writes)

커밋되지 않은 쓰기를 다른 트랜잭션이 덮어쓸 수 없다.

#### 구현 방법

- 더티 쓰기 방지: **행 수준 잠금** (쓰기 시 잠금 획득, 커밋 시 해제)
- 더티 읽기 방지: 잠금 대신 **이전 커밋 값을 기억** (읽기 시 잠금 없음 → 성능 좋음)

### 격리 수준 2: Snapshot Isolation (스냅숏 격리)

Read Committed에서도 발생하는 문제가 있다: **읽기 스큐(read skew)** = **비반복 읽기(non-repeatable read)**.

#### MVCC (Multi-Version Concurrency Control)

스냅숏 격리의 핵심 구현 방법.

```
데이터의 여러 버전을 유지:

txid=10: x = 500 (커밋됨)
txid=13: x = 400 (커밋됨)
txid=15: x = 350 (아직 커밋 안 됨)

Tx(txid=12)가 x를 읽으면:
→ txid=12 이전에 커밋된 최신 버전 = txid=10의 값 = 500
→ txid=13, 15의 변경은 보이지 않음 (트랜잭션 시작 이후의 변경)
```

#### MSSQL에서의 스냅숏 격리

```sql
-- MSSQL: 스냅숏 격리 활성화
ALTER DATABASE MyDB SET ALLOW_SNAPSHOT_ISOLATION ON;

-- 트랜잭션에서 사용
SET TRANSACTION ISOLATION LEVEL SNAPSHOT;
BEGIN TRANSACTION;
  SELECT Balance FROM Accounts WHERE AccountId = 1;
  -- 이 시점의 스냅숏 기준으로 읽음
  -- 다른 트랜잭션이 커밋해도 이 트랜잭션 안에서는 변경 안 보임
COMMIT;
```

MSSQL은 tempdb에 행 버전을 저장하는 방식으로 MVCC를 구현한다.

#### Read Committed vs Snapshot Isolation

| 관점       | Read Committed      | Snapshot Isolation |
| -------- | ------------------- | ------------------ |
| 읽기 스큐    | 발생 가능               | 방지                 |
| 읽기 시점    | 쿼리 시작 시점            | **트랜잭션 시작 시점**     |
| 같은 쿼리 반복 | 결과 바뀔 수 있음          | 항상 같은 결과           |
| 사용 예     | PostgreSQL/MSSQL 기본 | 백업, 분석 쿼리, 긴 트랜잭션  |
| MVCC     | 문장(statement) 수준    | 트랜잭션 수준            |

---

## 갱신 손실 (Lost Update) 방지

두 트랜잭션이 동시에 같은 값을 읽고, 수정하고, 쓸 때 발생.

```
[갱신 손실]

카운터 증가:
Tx1: counter 읽기 → 42
Tx2: counter 읽기 → 42
Tx1: counter = 42 + 1 = 43 쓰기
Tx2: counter = 42 + 1 = 43 쓰기  ← Tx1의 갱신이 사라짐!

기대값: 44, 실제값: 43
```

### 발생하는 상황들

- 카운터 증가 (좋아요 수, 조회수)
- 계좌 잔액 갱신
- JSON 문서의 특정 필드 수정
- 위키 페이지 동시 편집

### 해결 방법

#### 방법 1: 원자적 쓰기 (Atomic Write)

```sql
-- DB가 제공하는 원자적 연산 사용
UPDATE counters SET value = value + 1 WHERE key = 'likes';

-- DB 내부에서 잠금을 걸어 순차 실행
-- 애플리케이션에서 read-modify-write 패턴을 안 써도 됨
```

가장 좋은 해결책. 가능하면 이걸 쓰자.

#### 방법 2: 명시적 잠금 (Explicit Locking)

```sql
-- FOR UPDATE로 해당 행을 잠금
BEGIN TRANSACTION;
  SELECT * FROM documents WHERE id = 1 FOR UPDATE;  -- 잠금!
  -- 다른 트랜잭션은 이 행을 읽을 수 없음 (대기)
  UPDATE documents SET content = '수정된 내용' WHERE id = 1;
COMMIT;
```

#### 방법 3: CAS (Compare-and-Set)

```sql
-- 내가 읽은 값이 아직 변경되지 않았을 때만 갱신
UPDATE wiki_pages
SET content = '새 내용', version = 3
WHERE id = 1 AND version = 2;  -- 내가 읽은 버전이 2였으니까

-- 영향받은 행이 0이면 → 누가 먼저 수정함 → 재시도
```

#### 방법 4: 자동 감지 (Automatic Detection)

일부 DB(PostgreSQL의 Repeatable Read, MSSQL의 Snapshot Isolation 등)는 갱신 손실을 자동 감지하고 트랜잭션을 중단시킨다.

```
Tx1: counter 읽기 → 42, 수정 → 43, 쓰기
Tx2: counter 읽기 → 42, 수정 → 43, 쓰기 시도
DB: "Tx1이 이미 이 행을 수정했네" → Tx2 강제 중단 (rollback)
Tx2: 재시도 → counter 읽기 → 43, 수정 → 44, 쓰기 ✅
```

### 갱신 손실 해결 방법 비교

|방법|장점|단점|
|---|---|---|
|원자적 쓰기|가장 단순, DB가 보장|단순 연산만 가능|
|명시적 잠금|유연함|잠금 범위 실수 위험, 데드락|
|CAS|잠금 없음|재시도 로직 필요|
|자동 감지|코드 변경 최소|DB 지원 필요|

---

## 쓰기 스큐 (Write Skew)

갱신 손실보다 더 미묘한 동시성 문제.

### 예시: 병원 당직 의사

```
규칙: 최소 1명은 당직이어야 한다
현재: 앨리스와 밥 2명이 당직

앨리스: "당직 의사 몇 명?" → 2명
밥:    "당직 의사 몇 명?" → 2명

앨리스: "2명이니까 나 빠져도 되겠다" → 앨리스 당직 해제
밥:    "2명이니까 나 빠져도 되겠다" → 밥 당직 해제

결과: 당직 0명! 💥 규칙 위반!
```

### 왜 갱신 손실과 다른가?

```
갱신 손실: 두 트랜잭션이 "같은 행"을 수정
쓰기 스큐: 두 트랜잭션이 "같은 조건을 읽고" → "다른 행"을 수정

→ 같은 행을 수정하는 게 아니라서, 행 수준 잠금으로 해결 안 됨
→ FOR UPDATE로 잠글 행이 명확하지 않음
```


### 실제 발생하는 사례들

**회의실 예약**: 같은 시간에 두 사람이 동시에 "비어있음"을 확인하고 예약

```sql
-- Tx1과 Tx2가 동시에 실행
SELECT COUNT(*) FROM bookings
WHERE room = '301' AND time = '14:00';  -- 둘 다 0

-- 둘 다 "비어있구나" → 둘 다 INSERT
INSERT INTO bookings (room, time, user) VALUES ('301', '14:00', 'Alice');
INSERT INTO bookings (room, time, user) VALUES ('301', '14:00', 'Bob');
-- 이중 예약!
```

**사용자명 중복**: 두 사람이 동시에 같은 username으로 가입

**잔액 확인 후 출금**: 잔액 충분한지 확인 후 출금하는데 동시에 두 번 실행

### 팬텀 (Phantom)

쓰기 스큐의 근본 원인. **한 트랜잭션의 쓰기가 다른 트랜잭션의 검색 조건 결과를 바꾸는 현상.**

```
Tx1: SELECT ... WHERE room='301' AND time='14:00' → 결과 0행
Tx2: SELECT ... WHERE room='301' AND time='14:00' → 결과 0행
Tx1: INSERT → 이제 같은 SELECT를 하면 1행이 나옴
Tx2: INSERT → 결과 2행 (Tx2 입장에서는 모르는 행이 생김 = 팬텀)
```

- 팬텀은 아직 **존재하지 않는 행**에 대한 문제
- FOR UPDATE는 이미 존재하는 행만 잠글 수 있음
- 존재하지 않는 행은 잠글 수 없음 → 팬텀 해결이 어려움

### 팬텀 해결: 구체화 충돌 (Materializing Conflicts)

팬텀의 원인이 "잠글 행이 없는 것"이라면, **잠글 수 있는 행을 미리 만들어 놓는** 방법.


- 실용적이지만 **설계가 어렵고 오류 가능성 있음**
- 최후의 수단. 가능하면 직렬성 격리를 쓰는 게 나음

---

## 직렬성 (Serializability)

가장 강한 격리 수준. **트랜잭션들이 마치 하나씩 순서대로 실행된 것과 같은 결과**를 보장한다. 모든 동시성 문제(더티 읽기, 갱신 손실, 쓰기 스큐, 팬텀)를 해결한다.

구현 방법은 세 가지.

### 방법 1: 실제 직렬 실행 (Actual Serial Execution)

진짜로 트랜잭션을 하나씩 실행한다. 단일 스레드.

```
트랜잭션 큐:
[Tx1] → [Tx2] → [Tx3] → ...
   ↑ 한 번에 하나씩 순서대로 실행
```

- 모든 데이터가 **메모리에 있음** (디스크 I/O 대기 없음)
- 트랜잭션이 **짧고 빠름**
- 쓰기 처리량이 **단일 CPU로 충분**

사용: VoltDB, Redis, Datomic

#### 스토어드 프로시저의 부활

단일 스레드에서 네트워크 왕복을 기다릴 수 없으므로, 트랜잭션 로직을 **스토어드 프로시저로 한 번에 제출**한다.

```
[기존 대화형 트랜잭션]
앱 → DB: BEGIN
앱 → DB: SELECT ...       ← 네트워크 왕복
앱: 로직 처리              ← 앱에서 시간 소모
앱 → DB: UPDATE ...       ← 네트워크 왕복
앱 → DB: COMMIT
→ 전체 시간의 대부분이 네트워크 대기

[스토어드 프로시저]
앱 → DB: "이 프로시저 실행해줘 (파라미터: ...)"
DB: 프로시저 내에서 SELECT → 로직 → UPDATE → 커밋
→ 네트워크 왕복 1번, 실행은 DB 내에서 전부 처리
```

### 방법 2: 2PL — 2단계 잠금 (Two-Phase Locking)

#### 핵심 규칙

```
읽는 사람과 쓰는 사람이 서로를 막는다:

- 누군가 읽고 있으면 → 다른 사람은 쓸 수 없음 (대기)
- 누군가 쓰고 있으면 → 다른 사람은 읽을 수도 없음 (대기)

스냅숏 격리와의 차이:
스냅숏 격리: "읽는 사람은 쓰는 사람을 막지 않고, 쓰는 사람은 읽는 사람을 막지 않는다"
2PL: 읽기와 쓰기가 서로를 막는다
```

#### 2단계란?

```
1단계 (Growing Phase): 트랜잭션 실행 중 필요한 잠금을 계속 획득
2단계 (Shrinking Phase): 트랜잭션 종료 시 모든 잠금을 한꺼번에 해제

→ "잠금을 하나라도 해제하면 더 이상 새 잠금을 획득할 수 없다"
```

#### 잠금 종류

```
공유 잠금 (Shared Lock, S Lock):
- 읽기 시 획득
- 여러 트랜잭션이 동시에 S Lock 가능 (읽기끼리는 충돌 안 함)
- S Lock이 걸린 행에 X Lock은 불가 (대기)

배타 잠금 (Exclusive Lock, X Lock):
- 쓰기 시 획득
- X Lock이 걸린 행에 다른 S Lock도 X Lock도 불가 (대기)
```

#### 2PL의 문제: 성능

```
Tx1: A행 읽기 (S Lock 획득)
Tx2: A행 쓰기 시도 → Tx1이 S Lock 보유 → 대기...
Tx3: A행 읽기 시도 → Tx2가 X Lock 대기 중이라 → 대기...

→ 하나의 느린 트랜잭션이 다른 많은 트랜잭션을 줄줄이 대기시킴
→ 스냅숏 격리보다 성능이 상당히 나쁨
→ 데드락 가능성도 높아짐
```

#### 서술 잠금 (Predicate Lock)과 인덱스 범위 잠금

팬텀을 막기 위한 잠금.

```
[서술 잠금]
SELECT * FROM bookings WHERE room='301' AND time='14:00'

이 조건에 해당하는 모든 행 (현재 + 미래)에 잠금
→ 다른 트랜잭션이 이 조건에 맞는 행을 INSERT/UPDATE/DELETE 못 함
→ 팬텀 방지!
→ 하지만 조건 매칭 비용이 큼

[인덱스 범위 잠금 = Next-Key Locking]
서술 잠금의 근사치. 조건보다 더 넓은 범위를 잠금.

예: room='301'인 모든 시간대를 잠금 (14:00뿐 아니라 전부)
→ 서술 잠금보다 범위가 넓지만 (더 많이 잠그지만)
→ 인덱스를 활용해서 오버헤드가 훨씬 적음
→ 대부분의 2PL 구현이 이 방식 사용 (InnoDB의 Next-Key Lock)
```

### 방법 3: SSI — 직렬성 스냅숏 격리 (Serializable Snapshot Isolation)

2008년에 제안된 비교적 새로운 알고리즘. 스냅숏 격리 + 직렬성 위반 감지.

#### 핵심 아이디어

```
낙관적 접근:
1. 일단 스냅숏 격리처럼 자유롭게 읽고 쓴다 (잠금 없음)
2. 커밋할 때 "혹시 직렬성이 깨지진 않았나?" 검사
3. 깨졌으면 → 트랜잭션 중단, 재시도
4. 안 깨졌으면 → 커밋 성공
```

#### 2PL과 SSI 비교

```
2PL (비관적):
"문제가 생길 수도 있으니 미리 잠그자"
→ 잠금 대기 → 느림, 데드락 위험

SSI (낙관적):
"일단 진행하고 나중에 확인하자"
→ 잠금 없음 → 빠름, 대신 중단 시 재시도 비용
→ 충돌이 적으면 매우 효율적
→ 충돌이 많으면 재시도 비용 증가
```

#### SSI가 감지하는 것

```
감지 1: 오래된 MVCC 읽기 (Stale Read)

Tx1 시작 → x를 읽음 (x=42)
Tx2: x=43으로 수정, 커밋
Tx1: x=42를 기반으로 결정, 쓰기 시도
→ SSI: "Tx1이 읽은 x=42는 이미 변경됨" → Tx1 중단

감지 2: 읽기에 영향을 주는 쓰기

Tx1: SELECT ... WHERE on_call = true → 2명
Tx2: SELECT ... WHERE on_call = true → 2명
Tx1: UPDATE ... SET on_call = false (자신의 행)
→ SSI: "Tx2의 SELECT 결과에 영향을 줄 수 있는 쓰기" 감지
Tx2: UPDATE 시도 → 중단
```

사용: PostgreSQL 9.1+ (SERIALIZABLE), CockroachDB

---

## 세 가지 직렬성 구현 비교

|관점|실제 직렬 실행|2PL|SSI|
|---|---|---|---|
|방식|단일 스레드 순차|잠금 기반|낙관적 (감지 후 중단)|
|읽기 차단|순서 대기|쓰기가 읽기를 막음|차단 없음|
|성능|메모리 내 데이터면 빠름|잠금 오버헤드 큼|스냅숏 격리와 비슷|
|데드락|없음 (단일 스레드)|가능 (감지+해소 필요)|없음|
|재시도|없음|데드락 시|충돌 감지 시|
|제약|짧은 트랜잭션만|긴 트랜잭션 가능|중단 비율 높으면 비효율|
|확장성|파티션 수만큼|제한적|좋음|
|대표 DB|VoltDB, Redis|MySQL InnoDB, MSSQL|PostgreSQL, CockroachDB|


---
### 퀴즈

1. 다음 두 상황 중 **Snapshot Isolation으로 막을 수 있는 것**은?
```
상황 A:
Tx1: SELECT balance FROM accounts WHERE id=1 → 1000
Tx2: UPDATE accounts SET balance = 500 WHERE id=1 (커밋)
Tx1: SELECT balance FROM accounts WHERE id=1 → ???

상황 B:
좌석 잔여: 1개
Tx1: SELECT count FROM seats WHERE flight='KE123' → 1
Tx2: SELECT count FROM seats WHERE flight='KE123' → 1
Tx1: "1자리 있네" → INSERT INTO reservations, UPDATE count=0
Tx2: "1자리 있네" → INSERT INTO reservations, UPDATE count=0
```


2. 온라인 쇼핑몰에서 쿠폰 사용 로직이다. 이 코드에는 동시성 문제가 숨어있다. **어떤 문제가 발생할 수 있고, 어떤 격리 수준에서 막을 수 있는가?**
```
-- 규칙: 쿠폰은 전체 사용자 통틀어 선착순 100명만 사용 가능

BEGIN TRANSACTION;
  SELECT @used = COUNT(*) FROM coupon_usage WHERE coupon_id = 'SUMMER2026';
  
  IF @used < 100 THEN
    INSERT INTO coupon_usage (coupon_id, user_id) VALUES ('SUMMER2026', @current_user);
  END IF;
COMMIT;
```

