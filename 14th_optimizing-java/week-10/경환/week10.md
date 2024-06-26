# 10. JIT 컴파일의 세계로
- 핫스팟 구현체에 국한된 내용이라서, 이번 장에 나오는 내용은 다른 구현체에는 안맞을 수도 있다.

## 10.1 JITWatch란?
- 애플리케이션 실행 중에 JIT가 무슨 일을 했는지를 시각화해주는 툴
- 핫스팟 컴파일 상세 로그를 가지고 코드를 어떻게 바꿨는지 보여준다ㅣ
    - -XX:+UnlockDiagnosticVMOptions -XX:+TraceClassLoading -XX:+LogCompliation 설정을 해야한다.
- 샌드박스 기능을 이용해 (일종의 Playground) 작은 코드가 어떻게 컴파일되는지 알 수 있다.
    - VM 스위치 조작도 가능하다. (compressed oop 해제, osr 해제, 인라이닝 디폴트 해제 등)
- 샌드박스는 일부 코드 조각만 컴파일되기 때문에 실제와는 많이 다르다.
    - 실제로는 인라이닝 등을 통해 여러 최적화 경우의 수를 계산한다.
    - 광범위한 코드 레벨에서 봐야함.

## 10.2 JIT 컴파일 개요
- 핫스팟은 실행 프로그램 정보를 Method Data Object에 저장한다.
    - 언제 무슨 최적화를 할지 결정하는데 필요한 정보를 저장한다. -> 즉 프로파일링 정보가 저장된다.
    - 어떤 메서드가 호출됐고, 어떤 분기로 갈라졌고, 호출부에서는 어떤 타입이었고 등
    - MDO를 가지고 JIT 컴파일러가 어떤 최적화 기법을 사용할 지 결정한다.
- 최적화 기법은 런타임 정보와 지원 여부에 따라 달라질 수 있다.
    - C1은 추측성 최적화를 안하지만, C2는 추측성 최적화를 하고, 실행할때마다 유효한지 체크한다.

## 10.3 인라이닝
- 인라이닝은 피호출부를 호출부에 복사하는 것
    - 전달할 매개변수 세팅, 메서드 룩업, 런타임 자료구조 생성, 제어권 이송, 결과 반환 오버헤드를 줄일 수 있다.
- 인라이닝은 JIT 컴파일러가 제일 먼저 적용하는 최적화이다.
    - 탈출 분석, 죽은 코드 제거, 루프 펼치기, 락 생략 최적화의 기반이 된다.
- 인라이닝 제한 요소
    - 인라인할 메서드의 바이트코드 크기 : 너무 크면 안된다.
    - 메서드의 깊이: 너무 깊은거까진 안한다.
    - 메서드 컴파일 버전이 코드 캐시에서 차지하는 공간: 너무 크면 안된다.
- 인라이닝 스위치
    - MaxInlineSize, FreqInlineSize, InlineSmallCode, MaxInlineLevel
    - 스위치 만지작거릴때는 항상 측정 데이터를 근거로 삼아야 한다.

## 10.4 루프 펼치기
- 루프 내부의 메서드를 인라이닝 한다면 루프 순회 비용이 더 분명해진다.
- 루프 바디가 짧을 수록 백브랜치 비용이 크므로 루프 카운터 변수 유형, 루프 보폭, 탈출지점 개수를 고려해서 루프 펼치기를 한다.
- 루프 펼치기에는 카운터가 int, short, char 일때만.
- 루프 바디를 펼치고 세이프포인트폴을 제거
- 분기예측 비용감소 및 세이프포인트 체크 안해서 비용 감소
- 정말 그러한지 체크하자. -> VM 구현체 및 버전에 의존적인 내용이므로

## 10.5 탈출 분석
- 메서드 내부에서 할당된 객체를 메서드 범위 밖에서 볼 수 있는지 체크한다.
- 탈출 이라는 말보단 Scope 분석이 적당한듯.
- 탈출 시나리오
    - NoEscape : 메서드 내부에서만 존재하는 객체이다. 스칼라로 대체 가능 -> 힙 안씀 굿
    - ArgEscape: 메서드를 탈출하진 않지만 메서드 내에서 인수로 전달되거나 레퍼런스로 참조된다 -> 스칼라 대체 불가능
    - GlobalEscape: 탈출함
- NoEscape일 때만 힙 할당 제거가 가능하다.
    - 메서드 내부에서 호출되는 메서드를 인라이닝할 수 있다면 ArgEscape -> NoEscape로 바꿀 수 있다.
    - 인라이닝 후 힙 할당제거 되면 GC 예방 가능하다.
- 탈출 분석으로 락도 최적화 가능하다.
    - 비탈출 객체에 걸린 락은 제거한다. -> 굳이 다른 메서드에서 쓰진 않으니
    - 락이 걸린 연속된 영역은 병합한다.
    - 중첩락은 하나로 합친다.
- 원소가 64개를 초과하는 배열은 탈출분석 안된다. -> VM 스위치로 조정 가능
- 부분 탈출분석은 지원하지 않는다.
    - 조건문에 따라 탈출할수도 안할수도 있는 케이스
    - 탈출하는 조건에서는 객체를 따로 할당하면된다.

## 10.6 단형성 디스패치
- 단형성 디스패치란 구현체가 런타임에 분석해봤는데 하나인 경우
- 대부분 경험적으로 단형성 디스패치이다.
    - 퀵하게 타입만 검사하고 vtable lookup을 제거할 수 있다.
    - 일종의 vtable lookup 캐시하기
- 이형성 디스패치도 호출부마다 상이한 두 klass 워드를 캐시한다. -> 두개로 분기
- 다형성 디스패치는 vtable lookup을 해야하므로 코드상에서 instance of로 분기 태워서 단형성 or 이형성으로 바꿔버리면 성능 향상될 수 있음

## 10.7 인트린직
- 인트린직이란 JVM이 이미 알고 있는 고도로 튜닝된 네이티브 메서드
- 성능이 필수적인 코어 메서드에 사용된다.
- JVM은 기동 직후 HW 살펴보고 사용가능한 프로세서 기능을 목록화해서 가능한 경우 인트린직 메서드를 사용한다.

## 10.8 온스택 치환
- JIT 컴파일 단위는 메서드 인데, 메서드에 무거운 루프가 있을 경우, 루프를 컴파일해서 치환한다.

## 10.11 마치며
- 10.9, 10.10은 생략
- 먼저 좋은 코드를 작성하고 필요한 경우에만 최적화해라
- 인라이닝 및 컴파일 한계치를 알고 있으면 자신의 코드가 이 한계를 넘지 않도록 리팩터링할 수 있다.
- 단형성 디스패치를 알고 있으면 인터페이스 사용에 부담은 없을 것.
