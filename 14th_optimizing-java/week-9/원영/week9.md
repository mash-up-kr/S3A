## 9.1 바이트코드 해석

- JVM 인터프리터는 일종의 스택 머신처럼 작동
- CPU와는 달리 계산 결과를 바로 보관하는 레지스터는 없음
- 대신, 작업할 값은 모두 평가 스택에 놓고 스택 머신 명령어로 스택 최상단에 위치한 값을 변환하는 방식
- JVM의 주요 데이터 공간
    - **평가 스택**: 메서드별로 하나씩 생성된다.
    - **로컬 변수**: 결과를 임시 저장한다(특정 메서드별로 존재한다).
    - **객체 힙**: 메서드끼리, 스레드끼리 공유

### 9.1.1 JVM 바이트 코드 개요

- **옵코드**
    - JVM에서 각 스택 머신의 값을 변환하는 작업 코드(옵코드)는 1바이트로 나타냄
    - 옵코드는 패밀리 단위로 구성되어 있고, 각각 기본형, 참조형 등을 쓸 수 있게 한다.
    - ex) store 패밀리 -> dstore(double형 지역 변수로 스토어), astore(참조형 지역 변수로 스토어)
- **이식성**
    - 자바는 처음부터 이식성을 염두에 두고 설계된 언어
    - JVM은 빅 엔디언, 리틀 엔디언 하드웨어 모두 실행 가능해야 되며, JVM은 어느 엔디언을 따를지 결정
- **단축형**
    - load 같은 옵코드 패밀리는 단축형이 있어서 인수를 생략 가능
    - 그만큼 클래스 파일의 인수 공간을 절약 가능
- **로드/스토어 카테고리**
    - 앞으로 표시하는 바이트코드 테이블에서 c1은 2바이트짜리 상수 풀 인덱스, i1은 현재 메서드의 지역 변수, 괄호는 해당 옵코드 패밀리 중 단축형을 지닌 옵코드가 있음을 의미
    - 상수 풀에서 데이터를 로드하거나 스택 상단을 힙에 있는 객체 필드에 저장

| 패밀리 명 | 인수 | 설명 |
| --- | --- | --- |
| load | (i1) | 지역 변수 i1 값을 스택에 로드한다. |
| store | (i1) | 스택 상단을 지역 변수 i1에 저장한다. |
| ldc | c1 | CP#c1이 가리키는 값을 스택에 로드한다. |
| const |  | 단순 상숫값을 스택에 로드한다. |
| pop |  | 스택 상단에서 값을 제거한다. |
| dup |  | 스택 상단에 있는 값을 복제한다. |
| getfield | c1 | 스택 상단에 위치한 객체에서 CP#c1이 가리키는 필드명을 찾아 그 값을 스택에 로드한다. |
| putfield | c1 | 스택 상단의 값을 CP#c1이 가리키는 필드에 저장한다. |
| getstatic | c1 | CP#c1이 가리키는 정적 필드값을 스택에 로드한다. |
| putstatic | c1 | 스택 상단의 값을 CP#c1이 가리키는 정적 필드에 저장한다. |
- **산술 카테고리**
    - 기본형에만 적용되며 순수하게 스택 기반으로 연산을 수행하므로 인수 X

| 패밀리 명 | 설명 |
| --- | --- |
| add | 스택 상단의 두 값을 더한다. |
| sub | 스택 상단의 두 값을 뺀다. |
| div | 스택 상단의 두 값을 나눈다. |
| mul | 스택 상단의 두 값을 곱한다. |
| (cast) | 스낵 상단의 값을 다른 기본형으로 캐스팅(형 변환)한다. |
| neg | 스택 상단의 값을 부정한다. |
| rem | 스택 상단의 두 값을 나눈 나머지를 구한다. |
- **흐름 제어 카테고리**
    - 소스 코드의 순회, 분기문을 바이트코드 수준으로 표현하는 옵코드
    - for, if, while, switch 문을 컴파일하면 모두 이런 흐름 제어 옵코드로 변환

| 패밀리 명 | 인수 | 설명 |
| --- | --- | --- |
| if | (i1) | 조건이 참일 경우 인수가 가리키는 위치로 분기한다. |
| goto | i1 | 주어진 오프셋으로 무조건 분기한다. |
- **메서드 호출 카테고리**
    - 자바 프로그램에서 새 메서드로 제어권을 넘기는 유일한 장치

| 패밀리 명 | 인수 | 설명 |
| --- | --- | --- |
| invokevirtual | c1 | CP#c1이 가리키는 메서드를 가상 디스패치를 통해 호출한다.-> 보통 인스턴스 메서드 호출 시 사용 |
| invokespecial | c1 | CP#c1이 가리키는 메서드를 특별한 디스패치를 통해(즉, 정확하게) 호출한다.-> 컴파일 타임에 디스패치할 메서드를 특정할 수 있는 경우(프라이빗 or 슈퍼클래스 호출) |
| invokeinterface | c1, count, 0 | CP#c1이 가리키는 인터페이스 메서드를 인터페이스 오프셋 룩업을 이용해 호출한다.-> 자바 인터펭스에 선언된 메서드 호출 시 사용 |
| invokestatic | c1 | CP#c1이 가리키는 정적 메서드를 호출한다.-> 정적 메서드 호출 시 사용 |
| invokedynamic | c1, 0, 0 | 호출해서 실행할 메서드를 동적으로 찾는다.-> 람다 표현식을 동적으로 호출할 때 사용 |
- **플랫폼 카테고리**
    - 객체별로 힙 저장 공간을 새로 할당하거나, 고유 락을 다루는 명령어

| 옵코드명 | 인수 | 설명 |
| --- | --- | --- |
| new | c1 | CP#c1이 가리키는 타입의 객체에 공간을 할당한다. |
| newarray | prim | 기본형 배열에 공간을 할당한다. |
| anewarray | c1 | CP#c1이 가리키는 타입의 객체 배열에 공간을 할당한다. |
| arraylength |  | 스택 상단에 위치한 객체를 그 길이로 치환한다. |
| monitorenter |  | 스택 상단의 객체 모니터를 잠금한다. |
| monitorexit |  | 스택 상단의 객체 모니터를 잠금 해제한다. |
- **세이브포인트**
    - JVM이 어떤 관리 작업(GC 등)을 수행하고 내부 상태를 일관되게 유지하는 데 필요한 지점
    - 일관된 상태를 유지하려면 작업 수행(GC 등) 도중 공유 힙이 변경되지 않게 모든 애플리케이션 메서드를 멈추어야 함
    - '바이트코드 사이사이'가 애플리케이션 스레드를 멈추기에 이상적인 시점이자 단순한 세이브포인트

### 9.1.2 단순 인터프리터

- **switch 문이 포함된 while 루프 형태**
- 이게 가장 단순한 인터프리터 형태
- 해당 프로젝트는 교육용으로 JVM 인터프리터 일부를 구현한 프로젝트

### 9.1.3 핫스팟에 특정한 내용

- 핫스팟은 상용 JVM이자, 인터프리티드 모드에서도 빠르게 실행될 수 있도록 여러 고급 확장 기능
    - 오슬록 같은 단순 인터프리터와 달리, 핫스팟은 템플릿 인터프리터라서 동적으로 특정 바이트코드에 맞는 템플릿을 제공
    - 심지어 핫스팟 전용 프라이빗 바이트코드까지 정의해서 사용

## 9.2 AOT와 JIT 컴파일

### **9.2.1 AOT 컴파일**

- **C/C++ 개발 경험자라면 익숙한 컴파일**
- 소스 코드를 컴파일러에 넣고 바로 실행 가능한 기계어를 뽑아내는 과정
- **AOT의 목표:** 프로그램을 실행할 플랫폼과 프로세서 아키텍처에 딱 맞은 실행 코드를 얻는 것
- 이렇게 고정된 바이너리는 프로세서의 특수 기능을 십분 활용해 프로그램 속도를 높일 수 있음
- **AOT의 한계:** 대부분의 실행 코드는 자신이 어떤 플랫폼에서 실행될지 모르는 상태에서 생성되므로, 어떤 기능이 있을 거란 전제하에 컴파일한 코드가 그렇지 못한 환경에서 실행되면 전혀 작동하지 않을 수 있음
- AOT 컴파일은 CPU 기능을 최대한 활용하지 못하는 경우가 다반사고 성능 향상의 숙제

### 9.2.2 JIT 컴파일

- **핫스팟을 비롯한 대부분의 JVM에서 사용**
- 런타임에 프로그램을 고도로 최적화한 기계어로 변환하는 기법
- 프로그램의 런타임 실행 정보를 수집해서 어느 부분을 최적화해야 좋은지 프로파일을 만들어 결정을 내린다. (PGO)
- 애플리케이션 코드는 언제 트래픽이 많이 몰릴지 모르고, 매번 성능이 심한 편차를 보이는 현상이 아주 흔함
- 미리 계산된 최적화를 이용한 시스템이 PGO를 활용한 시스템보다 경쟁력이 떨어짐
- 그래서 핫스팟은 프로파일링 정보를 보관하지 않고 VM이 꺼지면 일체 폐기

### **9.2.3 AOT 컴파일 vs JIT 컴파일**

- **AOT 컴파일**
    - 장점
        - 상대적으로 이해하기 쉽다.
        - 특정 프로세서에만 사용할거라면 극단적으로 성능이 중요한 유스케이스에는 유용
    - 단점
        - 최적화 결정을 내리는 데 유용한 런타임 정보를 포기
        - 다양한 아케틱처에서 좋은 성능을 내려면 아키텍처마다 특화된 실행 코드가 필요 (확장성이 부족)
- **JIT 컴파일**
    - 새로운 프로세서 기능에 관한 최적화 코드를 추가 가능 (확장성이 좋음)
    - 심지어 핫스팟은 AOT 컴파일 옵션을 제공

# 9.3 핫스팟 JIT 기초

### **9.3.1 klass 워드, vtable, 포인터 스위즐링**

- **JIT 시스템을 구성하는 스레드**
    - 프로파일링 스레드: 컴파일 대상 메서드를 찾아내는 스레드
    - 컴파일러 스레드: 실제 기계어를 생성하는 스레드

### 9.3.2 JIT 컴파일 로깅

- TODO…

## 9.4 코드 캐시

- JIT 컴파일드 코드는 **코드 캐시**라는 메모리 영역에 저장
- 인터프리터 부속 등 VM 자체 네이티브 코드가 함께 있음
- 네이티브 코드가 캐시에서 제거되는 경우
    - (추측성 최적화를 적용한 결과 틀린 것으로 판명되어) 역최적화될 때
    - 다른 컴파일 버전으로 교체됐을 때 (단계별 컴파일)
    - 메서드를 지닌 클래스가 언로딩될 때
- 코드 캐시의 최대 크기는 다음 VM 스위치로 조정

```kotlin
-XX:ReservedCodeCascheSize=<n>
```

## 9.5 간단한 JIT 튜닝법

- 단순 JIT 튜닝의 대원칙: 컴파일을 원하는 메서드에게 아낌없이 리소스를 베풀라
- 목표 달성을 위한 점검 항목
    - 먼저, PrintCompilation 스위치를 켜고 애플리케이션을 실행
    - 어느 메서드가 컴파일됐는지 기록된 로그 수집
    - ReservedCodeCacheSize를 통해 코드 캐시 확장
    - 애플리케이션을 재실행
    - 확장된 캐시에서 컴파일드 메서드를 살펴봄
- 성능 엔지니어는 JIT 컴파일에 내재된 불확정성 고려해야 함 → 명심하면 아래 두 가지 사실 관찰 가능
    - 캐시 크기를 늘리면 컴파일드 메서드 규모가 유의미한 방향으로 커지는가?
    - 주요 트랜잭션 경로 상에 위치한 주요 메서드가 모두 컴파일되고 있는가?
- 캐시 크기를 늘려도 컴파일드 메서드 개수는 그대로고 로딩 패턴이 뚜렷? = JIT 컴파일러의 리소스 부족 X

## 9.6 마치며

- 성능 작업에 있어서 JIT 컴파일드 코드의 동작은 인터프리터보다 훨씬 중요
- 대부분의 애플리케이션은 이 장에서 학습한 단순 코드 튜닝 기법만 잘 활용해도 충분
