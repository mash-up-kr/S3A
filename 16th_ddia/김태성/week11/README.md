## XA Transaction

### 정의

> **여러 개의 서로 다른 리소스(DB, MQ 등)를 하나의 트랜잭션처럼 처리하기 위한 분산 트랜잭션 방식**
> 
- 여러 참여자가 모두 성공하면 Commit
- 하나라도 실패하면 전체 Rollback
- 이를 위해 **2PC(Two-Phase Commit)** 알고리즘을 사용

---

## 2PC(Two-Phase Commit)

### Phase 1 : Prepare

Coordinator가 모든 참여자에게 **Prepare 요청**을 보낸다.

#### Prepare 이전

- SQL은 이미 실행된 상태
- 변경 내용은 DB 내부에 반영되어 있음
- 하지만 아직 Commit은 되지 않은 상태

```
1차 영속성 컨텍스트
        ↓
      Flush
        ↓
     SQL 실행
        ↓
     Prepare
```

즉, Prepare는 SQL을 실행하는 과정이 아니라 **"현재 상태에서 Commit 가능한가?"** 를 확인하는 과정이다.

#### DB 내부에서는

Prepare 시 DB는 현재 활성 트랜잭션을 기준으로 다음을 검사한다.

- Constraint 검사
- Lock 상태 확인
- Serializable/SSI 검사
- Dependency 검사

그리고 Prepare 이후에도 상태가 깨지지 않도록 다음과 같은 제약을 추가한다.

- Row Lock
- Predicate Lock
- Dependency 관리

그래서 **Prepare에 YES를 응답한 이후에는 새로운 충돌이 발생하지 않는다.**

```
Prepare YES
        ↓
Coordinator가 Commit만 지시하면
반드시 Commit 가능
```

> **주의**
> 
> 
> Prepare = "반드시 Commit하겠다"가 아니라,
> **"Coordinator가 Commit을 결정하면 Commit할 수 있다."** 는 의미이다.
> 

#### Q. Prepare는 커밋 전 읽기인가?

격리 수준에서 말하는 Dirty Read와는 전혀 다르다.

다만 DB 내부에서는 **Commit되지 않은 트랜잭션의 상태와 메타데이터까지 참고하여 Prepare 가능 여부를 판단한다.**

즉, 사용자에게 보여주는 데이터는 Commit 여부를 따르지만, DB 엔진은 다음 정보를 모두 참고한다.

- Active Transaction
- Lock
- Undo
- MVCC Metadata

---

## Phase 2 : Commit / Rollback

Coordinator가 Prepare 결과를 모은다.

### 모든 참여자가 YES

```
Commit 결정
        ↓
Commit 요청
        ↓
ACK 수집
        ↓
로그 기록
        ↓
Transaction 종료
```

### 하나라도 NO

```
Rollback 결정
        ↓
Rollback 요청
        ↓
ACK 수집
        ↓
로그 기록
        ↓
Transaction 종료
```

Coordinator는 Commit/Rollback 결과를 기록하여 장애 발생 시 Recovery를 수행한다.

---

## XA와 SSI를 함께 사용하기 어려운 이유

### 1. 단일 DB 관점

SSI는 Commit 직전까지 Dependency Graph를 추적한다.

```
Commit 직전
        ↓
Cycle 발견
        ↓
Abort
```

반면 XA는

```
Prepare
        ↓
이후에는 Abort되면 안 됨
```

그래서 실제 DB는 다음과 같은 추가 제약을 이용하여 Prepare 이후 새로운 충돌이 발생하지 않도록 구현한다.

- Predicate Lock
- Dependency 관리
- Serializable 제어

### 2. 여러 DB(분산) 관점

분산 SSI를 수행하려면 전체 참여자의 다음 정보가 필요하다.

- Read Set
- Write Set
- Dependency Graph

하지만 XA는 다음 프로토콜만 정의한다.

```
Prepare
Commit
Rollback
```

Dependency Graph를 교환하는 프로토콜은 정의하지 않는다.

따라서 **XA만으로는 전역 Serializable Snapshot Isolation을 구현할 수 없다.**

---

## Spring + JPA 환경에서 XA 처리 흐름

```
@Transactional 시작
        ↓
TransactionInterceptor (Spring)
    // @Transactional 감지
        ↓
JtaTransactionManager.begin() (Spring)
    // JTA 시작 요청
        ↓
UserTransaction.begin() (JTA API)
    // 표준 API 호출
        ↓
TransactionImple.begin() (Narayana)
    // 실제 XA Transaction 생성
    // Xid 생성

────────────────────────────────

Repository.save()
        ↓
EntityManager.persist() (JPA API → Hibernate)
    // Entity를 1차 영속성 컨텍스트에 저장
        ↓
DB Connection 필요
        ↓
XAConnection 생성
    // JDBC Driver
        ↓
XAResource 획득
    // JDBC Driver
        ↓
Transaction.enlistResource()
    // Narayana
    // 현재 XA Transaction 참여자 등록
        ↓
다른 DB 접근
        ↓
다른 XAResource 등록
        ↓
Transaction
├── OrderDB
└── InventoryDB

────────────────────────────────

@Transactional 종료
        ↓
TransactionInterceptor
        ↓
JtaTransactionManager.commit()
        ↓
UserTransaction.commit()
        ↓
TransactionImple.commit()
    // 전역 Transaction 종료 요청
    // 참여자 수 확인
    // 1PC 또는 2PC 결정

────────────────────────────────

EntityManager.flush()
    // Hibernate
        ↓
SQL 실행
        ↓
DB
    // INSERT
    // UPDATE
    // DELETE
    // Undo 생성
    // MVCC Version 생성
    // Lock 획득
    // 아직 Commit은 아님

────────────────────────────────

(참여자가 여러 개)

        ↓
Phase 1 : Prepare
        ↓
Coordinator (Narayana)
        ↓
XAResource.prepare()
        ↓
Driver
        ↓
PREPARE TRANSACTION
        ↓
DB
        ↓
YES / NO
        ↓
Coordinator가 결과 수집

────────────────────────────────

전부 YES
        ↓
Commit 결정
        ↓
XAResource.commit()
        ↓
COMMIT PREPARED
        ↓
ACK
        ↓
Recovery Log 기록
        ↓
Transaction 종료

────────────────────────────────

하나라도 NO
        ↓
Rollback 결정
        ↓
XAResource.rollback()
        ↓
ROLLBACK PREPARED
        ↓
ACK
        ↓
Recovery Log 기록
        ↓
Transaction 종료
```

---

## 왜 실무에서는 XA를 잘 사용하지 않는가?

### 1. 성능 비용

Prepare 이후 Coordinator의 결정이 날 때까지 다음 리소스를 유지해야 한다.

- Lock
- Undo
- WAL
- Transaction Context

### 2. 네트워크 비용

```
Prepare
    ↓
응답
    ↓
Commit
    ↓
응답
```

왕복 호출이 많다.

### 3. 장애 복구가 복잡하다

Coordinator는 Recovery Log를 유지해야 하고, Prepared 상태의 Transaction을 Commit 또는 Rollback으로 끝까지 복구해야 한다.

### 해결

최근 MSA에서는 다음과 같은 방식을 더 많이 사용한다.

- Outbox Pattern
- Saga Pattern

```
DB 하나
    ↓
Local Transaction

──────────────

DB + MQ
    ↓
Outbox

──────────────

여러 서비스
    ↓
Saga

──────────────

강한 원자성이 반드시 필요
    ↓
XA(2PC)
```