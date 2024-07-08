# 11. 자바 언어의 성능 향상 기법
## 11.1 컬렉션 최적화

- 대부분의 프로그래밍 언어는 라이브러리는 최소 2가지 컨테이너 제공
    - 순차 컨테이너: 수치 인덱스로 표기한 특정 위치에 객체를 저장
    - 연관 컨테이너: 객체 자체를 이용해 컬렉션 내부에 저장할 위치를 결정
- 컨테이너에서 메서드가 정확히 작동하려면 객체가 **호환성**과 **동등성** 개념이 있어야 함
- 컨테이너세 저장되는 건 객체 자신이 아니라 객체를 가리키는 레퍼런스 : 어느 정도 성능 저하
- 컬렉션 API는 타입별로 해당 켄터이너가 준수해야 할 작업을 구체적으로 명시한 인터페이스
![image](https://github.com/mash-up-kr/S3A/assets/74983448/1c77dccb-014d-4dd3-9cec-59ff55fb2515)
 

## 11.2 List 최적화

### 11.2.1 ArrayList

- 고정 크기 배열에 기반한 리스트
- 배열의 최대 크기만큼 원소를 추가 가능 → 꽉 차면 더 큰 배열을 새로 할당
- JMH 벤치마크
    
    ```java
    @Benchmark
    public List<String> properlySizedArrayList() {
        List<String> list = new ArrayList<>(1_000_000);
        for (int i = 0; i < 1_000_000; i++) {
            list.add(item);
        }
        return list;
    }
     
    @Benchmark
    public List<String> properlySizedArrayList() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 1_000_000; i++) {
            list.add(item);
        }
        return list;
    }
    ```
    
- 결과
    
    ```java
    Benchmark                             Mode  Cnt    Score   Error  Units
    ResizingList.properlySizedArrayList  thrpt   10  287.388 ± 7.135  ops/s
    ResizingList.resizingArrayList       thrpt   10  189.510 ± 4.530  ops/s
    ```
    
    - properlySizedArrayList 테스트가 원소 추가 작업을 초당 약 100회 더 처리
    - **ArrayList 크기를 정확히 결정하고 시작하는 게 성능 ↑**

### 11.2.2 LinkedList

- 동적으로 증가하는 리스트
- 이중 연결 리스트 → 삽입은 항상 O(1)
![image](https://github.com/mash-up-kr/S3A/assets/74983448/aa27c9c2-a819-4e98-9443-796c40390dba)

### 11.2.3 ArrayList vs LinkedList

- 원소 삽입/삭제
    - ArrayList: O(n)
    - LinkedList: O(1)

```java
Benchmark                             Mode  Cnt    Score    Error  Units
InsertBegin.beginArrayList           thrpt   10    3.402 ±  0.239  ops/ms
InsertBegin.beginLinkedList          thrpt   10  559.570 ± 68.629  ops/ms
```

- 원소 액세스
    - ArrayList: O(1)
    - LinkedList: O(n)

```java
Benchmark                             Mode  Cnt       Score       Error  Units
AccessingList.accessArrayList        thrpt   10  269568.627 ± 12972.927  ops/ms
AccessingList.accessLinkedList       thrpt   10        0.863 ±    0.030  ops/ms
```

# 11.3 Map 최적화

### 11.3.1 HashMap

- 해쉬 키 탐색 코드
    
    ```java
    public Object get(Object key) {
        // 편의상 널 키는 지원하지 않음
        if (key == null) return null;
     
        int hash = key.hashCode();
        int i = indexFor(hash, table.length);
        for (Entry e = table[i]; e != null; e = e.next) {
            Object k;
            if (e.hash == hash && ((k = e.key) == key || key.equals(k)))
                return e.value;
        }
     
        return null;
    }
     
    private int indexFor(int h, int length) {
        return h & (length-1);
    }
    ```
    
    - 엔트리 순회 이유 : 해시 테이블의 충돌
    - 자바 최신 버전에서는 indexFor() 메서드를 hash()라는 보조 해시 함수(key의 hashCode를 변형)로 대체하여 충돌↓
    - HashMap 생성자 인자에서 성능에 영향을 주는 대상
        - **initialCapacity:** 현재 생성된 버킷 개수(디폴트 16)
        - **loadFactor**: 버킷 용량을 자동 증가(2배)시키는 한계치(디폴트 0.75: 현재 버킷 개수의 75%가 차면 버킷을 2배로 늘림
    - **ArrayList와 마찬가지로 저장할 데이터 양을 미리 알 수 있다면 initialCapacity를 설정 굿**
    - **loadFactor 0.75(디폴트) 정도면 공간과 접근 시간의 균형이 대략 맞아서 조정 필요 X**
- LinkedHashMap
    - HashMap의 서브클래스, 이중 연결 리스트를 사용해 원소의 삽입 순서 관리
    - 액세스 순서 모드로 변경 가능
    - Map에서는 보통 삽입, 접근이 중요하지 않아서 비교적 사용↓

### 11.3.2 TreeMap

- 레드-블럭 트리를 구현한 Map
- 기본 이진 트리 구조에 메타데이터를 부가 (노드 컬러링)
- get(), put(), containsKey(), remove() 메서드 시간복잡도: O(logn)
- 데이터 분할이 주특기인 구현체
    - 스트림이나 람다로 Map 일부를 처리할 때 유용

### 11.3.3 MultiMap은 없어요

- 자바는 MultiMap 구현체를 제공 X
- Map<K, List<V>> 형태로 충분히 구현 가능

## 11.4 Set 최적화

- 성능에 관해서 고려해야 할 사항은 Map과 비슷
- HashSet은 HashMap으로 구현 (LinkedHashSet은 LinkedHashMap으로 구현)
    - 삽입/삭제: O(1)
- TreeSet 역시 TreeMap을 활용
    - 삽입 순서와 상관 없이 원소 자체 순서 보장 -> Comparator에 정의
    - 삽입/삭제: O(logn)

## 11.5 도메인 객체

- 애플리케이션에 유의미한 비지니스 컨셉을 나타낸 코드 (ex. Order, DeliverySchedule)
- 도메인 객체의 메모리 누수
    - `jmap -histo` 로 자바 힙 상태 볼 수 있음 → 객체 순위도 확인 가능
    - **상위 30위 정도 이내에 주류 객체가 아닌 객체가 있으면 누수 의심**
    - 주류 객체: String, char Array, byte Array, Collections etc
- 전체 세대 효과
    - 특정 타입의 객체가 수집되지 않을 경우, 별의별 세대 카운트 값이 테뉴어드 세대까지 생존
    - 세대 카운트별 바이트 히스토그램 찍고, **도메인 객체 대응하는 데이터셋의 크기를 살피기**
    - 그 수치가 온당한지, 그리고 작업 세트에 존재하는 도메인 객체 수가 예상 범위 내에 들어 있는지 확인
- 부유 가비지 문제
    - 단명 도메인 객체 역시 부유 가비지(아무도 참조하지 않는데 혼자 남아있는 객체)를 일으키는 원인 가능
    - 동시 수집기의 SATB(Snapshot-at-the-Beginning) 기법
        - **단명 도메인 객체와 같이 짧은 시간만 살아 있는 객체들도 수집 대상에 포함되어 메모리 누수를 방지**

## 11.6 종료화 안 하기

### 11.6.1 무용담: 정리하는 걸 깜빡하다

- 수년간 문제 없이 돌아가던 운영계 코드를 변경했다가, 응답이 매우 느려지는 현상 발견
- 원인은 TCP 접속 오픈 이후, close() 함수를 깜빡 잊고 놓친 것

### 11.6.2 왜 종료화로 문제를 해결하지 않을까?

- 더 이상 참조되지 않는 객체는 가비지 수집기가 판단하면 해당 객체의 finalize() 메서드 호출
- 메서드가 오버라이드 되었으면 특별하게 처리
- 종료화 과정
    1. 종료화가 가능한 객체는 큐로 이동
    2. 애플리케이션 스레드 재시작 후, 별도의 종료화 스레드가 큐를 비우고 각 객체 finalize() 메서드를 실행
    3. finalize()가 종료되면 객체는 다음 사이클에 진짜 수집될 준비 끝
    
    → **이는 종료화 가능한 객체는 적어도 한 번의 GC 사이클은 더 보존된다는 뜻이고, 테뉴어드 객체의 경우 상당히 긴 시간이 될 수 있음**
    
- 종료화 스레드 실행 도중 메서드에서 예외 발생한 경우: 아무런 컨텍스트 없기 때문에 그냥 무시~
- 종료화에 블로킹 작업이 있을수도 있어서, 새 스레드 생성 →  스레드 생성 비용 고려
- C++은 직접 객체 수명을 명시적으로 관리하기 때문에 종료화가 유효
- 종료화는 의도 목적과 불일치 → 이미 9에서 deprecated

### 11.6.3 try-with-resources

- 자바 7 이전: 리소스를 닫는 일은 순전히 개발자의 몫 → 따라서 깜빡하는 경우

```java
public void readFirstLineOld(File file) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String firstLine = reader.readLine();
            System.out.println(firstLine);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
```

- 자바 7 이후: try 괄호 안에 리소스를 지정해서 생성하면, try 블록이 끝나는 지점에 close() 메서드를 예외 발생 여부와 상관없이 호출

```java
public void readFirstLineNew(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String firstLine = reader.readLine();
            System.out.println(firstLine);
        }
    }
```

- 종료화, try-with-resource 비교 → **왠만하면 try-with-resources 사용하기!**
    - 종료화: 런타임 기반, 특별한 GC 사용, 별도 종료화 스레드 동원, GC에 의존
    - try-with-resources: 컴파일 기반 -> 컴파일 시 리소스 close() 관련 코드가 알아서 생성

## 11.7 메서드 핸들

- invokedynamic 호출부가 실제로 어느 메서드를 호출할지 런타임 전까지 결정 X
    - 호출부가 인터프리터에 이르면 특수한 보조메서드(부트스트랩 메서드, BSM) 호출
    - BSM은 호출하는 실제 메서드를 가리키는 객체를 반환
- 메서드 핸들: invokedynamic 호출 부에 의해 호출되는 메서드를 나타낸 객체
    - 리플렉션과 유사, but 리플렉션은 자체 한계로 invokedynamic와 사용하기 불편
    - 자바 7부터 java.lan.invoke.MethodHandle가 추가돼서 실행 가능 메서드의 레퍼런스 직접 반영 가능
- **리플렉션 예시**
    
    ```java
    Method m = ...
    Object receiver = ...
    Object o = m.invoke(receiver, new Object(), new Object());
    ```
    
    - 바이트코드 결과: 전체 호출 시그니처는 "invoke:(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;"
    - Object를 반환하므로 컴파일 타임에는 메서드가 어떻게 호출될 지 가늠 X
        
        → 런타임 직전까지 모든 가능성을 찔러보다가 매개변수 목록에 조금이라도 오류가 생기면 런타임에 실패할 가능성↑
        
- **메서드 핸들 예시**
    
    ```java
    MethodType mt = MethodType.methodType(int.class);
    MethodHandles.Lookup l = MethodHandles.lookup();
    MethodHandle mh = l.findVirtual(String.class, "hashCode", mt);
     
    String receiver = "b";
    int ret = (int) mh.invoke(receiver);
    System.out.println(ret);
    ```
    
    - **MethodHandles.lookup()**: 룩업 컨텍스트 생성, 컨텍스트 객체를 생성한 시점에 접근 가능한 메서드 및 필드를 기록한 상태 정보
    - **findVirtual()**: 컨텍스트를 생성한 시점부터 보이는 모든 메서드의 핸들을 가져올 수 있다 -> hashCode() 메서드를 룩업
    - 바이트코드 결과: 호출 시그니처가 "invoke:(Ljava/lang/String;)I"
        
        → 소스 코드 컴파일러가 호출에 적합한 타입 시그니처를 알아서 추론
        
        → 정장적으로 디스패치될 수 있게 스택을 함께 세팅
        

## 11.8 마치며

- 성능 엔지니어라면 자신의 스킬 세트를 돋보이는 항목으로 잘 알아두기 ~
