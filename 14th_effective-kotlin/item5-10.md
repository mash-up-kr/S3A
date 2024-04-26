# Item [5~10]

- 일자: 2024년 4월 24일
- MC: 한정민
- 참석자: 한정민, 박범근, 예원 오, 김보배

## Item 5. 예외를 활용해 코드에 제한을 걸어라

- 37p) 제한을 걸면 문서를 읽지 않은 개발자도 문제를 확인할 수 있습니다.
    - 범근) 다들 테스트 코드 말고 서비스 로직에 이런 제한 들어가있나요?? 테스트코드가 문서의 역할을 하는 것과 같이 코드를 읽는데 도움을 줄 수 있을까? 아니면 오히려 로직의 가독성을 해칠까? 이런 생각이 들었어요.
- 39p) `require` 함수는 조건을 만족하지 못할 때, 무조건적으로 `IllegalArguemntException` 을 발생시켜 제약을 무시할 수 없음.
    - 정민 ) 작업 중 필드는 nullable 로 되어있지만, 비즈니스 로직상 null !! 이 아닌 것이 있는데, requireNotNull 쓰면 이후 스마트 캐스트이쁘게 쓸 수 있어 `!!` 안써도되서 좋은 듯
        - 범근) 오호 그런 사용예시가 있군여. 실제 회사 코드에 많이 적용되어 있나요?
        - 보배) 편하겠당. 필드는 nullable인 경우가 왜 있는지 궁금쓰. 추후 정의되는 필드들인건가?
    - 예원 ) 무조건적으로 `IllegalArguemntException` 를 발생시킨다는게 좋은건지 모르겠음
        - ex) 어떤 에러를 상속 받는 에러들은 로그 레벨을 다르게 찍는다던지, 센트리에 전송 한다 안한다라던지.. 를 조절하려면 이렇게 표준 오류를 사용하면 불가능하지 않나?
- 40p) 정민- 각 `check()` 에 들어가는 변수들은 당연히 위에서 정의된 거겠죠?
    - 범근) 그럴 것 같아요. is~는 boolean, token는 string 로그인 토큰?!
    - 보배) 예압.
- 40p) require와 check는 단순히 의미상 구분이겠죠? (보배)
    - 둘 다 의미에 맞게 잘 사용하고 있는지?
    - `requireNotNull` 과 `checkNotNull` 의 차이는 더 구분이 안갔는데, [stackoverflow](https://stackoverflow.com/questions/68521461/kotlin-checknotnull-vs-requirenotnull) 답변이 좋았음.
  
        > 번역 : 
            예외 유형은 컴파일러에 관한 한 유일한 실질적인 차이점이지만 코드를 읽는 사람에게는 의도에 큰 차이가 있습니다:
        > - require...() 함수는 매개변수를 검사하여 함수의 입력이 계약을 이행하는지 확인하는 데 사용됩니다. 
        > 따라서 일반적으로 함수에서 가장 먼저 호출합니다. 
        > (물론 Kotlin의 null 가능 유형이 아니므로 단일 매개 변수에 대해 requireNotNull()을 호출할 필요는 없지만 매개 변수 또는 하위 개체의 조합에 대해 더 복잡한 조건을 확인해야 할 수도 있습니다). 
        > 이것이 바로 인수가 합법적인지 확인하는 IllegalArgumentException을 던지는 이유입니다.
        > 
        > - check...() 함수는 관련 프로퍼티를 확인하여 객체 등이 지금 이 함수를 호출하기에 유효한 상태인지 확인하는 데 사용됩니다.
        > (다시 말하지만, 결코 널이 아닌 프로퍼티는 그에 따라 입력되므로, 속성, 조합 및/또는 하위 속성이 널일 수 있지만 널인 경우 이 함수를 호출해서는 안 되는 경우에는 checkNotNull()이 더 적절합니다). 
        > 따라서 객체의 현재 상태가 함수를 호출할 수 있는지 확인하는 IllegalStateException을 던집니다.
        >     
        > 물론 두 경우 모두 Java에서와 마찬가지로 표준 if 검사를 작성할 수 있습니다. 또는 Elvis 연산자 ?: 를 사용하여 null일 가능성이 있는 값이 처음 사용될 때 검사를 수행할 수도 있습니다. 
        > 
        > 그러나 이러한 함수는 보다 선언적인 형식의 대안을 제공합니다. 일반적으로 함수 맨 위에 함수의 계약이 무엇인지 코드를 훑어보는 사람이 알 수 있도록 함수의 계약이 무엇인지 설명하는 곳에 배치합니다.
        > 
        > 링크된 답변에서 알 수 있듯이 assert...() 함수도 있는데, 이 역시 실용적인 의미보다는 의미론적인 차이가 더 큽니다. 이는 함수 호출의 경계에서 벗어나 프로그래밍 오류를 감지하는 데 사용됩니다.
        > 
        > 불변성 및 기타 조건을 확인하고 단위 테스트 및 기타 자동화된 테스트에서 모든 검사를 수행합니다.
        > 
        > (어설션은 명령줄에서 활성화 및 비활성화할 수 있다는 또 다른 중요한 차이점이 있습니다.
        > 하지만 제 경험상 이는 그리 좋은 방법은 아닙니다. 중요한 검사라면 항상 실행해야 하고, 그렇지 않다면 코드를 디버깅한 후 제거하거나 최소한 자동화된 테스트로 옮겨야 합니다.)
            
- 40p) 상태 체크
    - 범근) 저는 보통 상태 setter 메서드에서 내부적으로
    
    ```kotlin
    fun expire (특정 객체) {
    	if (currentStatus != xxxStatus.READY) throw custom exception("유효하지 않은 상태 변경")
    	currentStatus = xxxStatus.EXPIRED
    }
    ```
    
    처럼 validation을 if문으로 많이 처리해줬던 것 같은데, check 메서드 많이 적용되어있나요? 예외가 `IllegalStateException` 로 발생하는 것 같은데, 혹시 적용되어 있으면 예외 처리를 어떻게 하는지 궁금해서요~
    
    (특정 custom exception으로 변경할 수 있는지도??)
    
    - 예원) 우리는 custom exception을 많이 써서, 오빠가 써준 예시처럼 custom exception을 던지는 코드가 많음.
        - 그런데 이번에 특정 validate() 함수를 만들고, (동작은 check와 비슷) custom exception을 넘기는 확장함수를 만들어주신 분이 계시긴함. (근데 잘안쓰는듯..쩝쩝쿵야)
- 41p) assert는 Java에서도 존재하는데, 이거 쓰는 사람? (보배)
    - 테스트에서만 동작한다고 하는데, 사실 단위테스트로 다 커버해서 많이 안쓰지 않나 싶은 생각 (보기엔 코드에 테스트 코드가 들어가는 느낌이라…)
- 42p ) -ea JVM 옵션 활성화
    - 정민 ) 직접 옵션으로 넣어서 써보신 분?
        - 범근) 저는 없어요..
        - 보배) 나도 놉
        - 예원 ) ㄴ ㄴ
- 42p) 참고로 이를 활용해도 여전히 단위 테스트는 따로 작성해야 합니다.
    - 범근) 테스트 코드로 체크하는데, 테스트가 분산되는 느낌을 받았어요. (로직에서도 assert, 테스트에서도 assert)
    - 만약 로직에 함수의 a에 대해 assert가 적용되어 있고 / 나는 테스트코드 작성 시 b에 대해 테스트를 하고 싶은데 의도와 달리 a assert가 실패되어 기대하지 않은 결과가 나올 수도 있을 것 같다는 생각?
- 42p) 코틀린에서는 코드를 안정적으로 만들고 싶을 때 양념처럼 assert를 사용할 수 있다
    - 예원 ) 단위테스트는 검증을 해주니 의미가 있다고 보는데, 앱단(어플리케이션 코드)에서 쓰는 이유는 뭐지? 실제로 에러를 던지지 않는데…(주석대체인가)
- 42p) nullability 와 스마트 캐스팅
    - 정민 ) - `contract` 기능 처음 봄 옹 (컴파일러에게 명시적으로 알려주는 기능이람다)
    - 보배) 진짜 `contract` - `implies` 이런건 느낌도 안옴ㅋㅋㅋ
        - [https://maivve.tistory.com/359](https://maivve.tistory.com/359)
        - 이게 설명이 좋은 듯. `contract` 내부에서 return 하는 값은 컴파일러에게 정보를 알려주는 것이지 실제 반환하는 값이 아님.
        - 근데 이것도 사실 스마트 캐스팅을 위해 컴파일러에게 미리 힌트(쿼리 힌트마냥)주는 것 같은데, 이거 그냥 안쓰고 require 써도 되는거 아닌가?
            - require not null 도 내부적으론 contract를 쓴다.
- 43p) final이라면 스마트 캐스팅됩니다.
    - 범근) 불변이어야 타입 추론한 결과가 바뀌지 않으니까 (val)
    - 예원) final 이라면 이란 의미가 val 이란 뜻인거죠?
- 43p) requireNotNull을 통해 스마트 캐스트하는건 좋은 것일까?
    - 보배) requireNotNull 이라는건 그냥 Unit 반환할 것 같은데 NotNull 한 값으로 만들어 반환해주는 역할임을 헷갈리면 안될 듯 (checkNotNull도 동일)
        - `val notNullValue = requireNotNull(value)`
    - 예원) ^ 보배오빠의 코멘트를 잘 이해 못하겠음. requireNotNull 을 통해서 검증된 변수는 스마트캐스트 된다는 뜻으로 이해했는디.
    - 정민) 난 저렇게 대입으로 쓴 적은 없당 
    - 범근) 22
- 44p) return/throw 와 run 함수 조합하여 활용
    - 정민) 이런 경험이 많다. 매핑 작업인데 여러 필드 매핑 진행중에 하나라도 매핑 안되어 null 반환 시 `?: run { logloglog }` 요런식으로  해서 무의미한 변수 생성을 막음
    - 범근) 저희도 대부분 elvis 많이 쓰는 것 같아요.
    - 보배) 람다로 lazy 하게 하는게 보기 좋네요
    - 예원) elvis 많이 써요.

### Elvis
![image](https://github.com/mash-up-kr/S3A/assets/27190617/bf2d9dfb-3a26-4153-9126-97846588b626)

## Item 6. 사용자 정의 오류보다는 표준 오류를 사용하라

- 범근) 로직 상 정상적으로 exception을 뱉어야 하는 비즈니스 예외(계좌 인출 시 잔액이 부족한 것과 같이)의 경우 custom exception을 많이 사용하는 것 같아요. 
(이런 표준 오류를 모두 래핑하지는 않는듯? 말그대로 표준 라이브러리의 오류의 네이밍이 명확하기도하고 이런 표준 exception을 하나하나 핸들링하지 않기 때문에?)
    - 보배) 인정. 모든 걸 다 표준 exception으로 커버하긴 어려운 듯. 더욱이 exception에서 비즈니스 로직 상 원하는 정보를 담아 handling할 때 뽑아쓰고 싶은데, 이걸 표준에서는 못하니 커스텀하게 구성하는 경우가 많은 것 아닐까 하는 생각 ㅎ
    - 예원) 우리도 직접 throw하는 예외들은 최대한 custom exception을 쓰려고함. (예외 메시지가 또 그대로 클라이언트에게 내려가는 경우도 있고, 에러별로 다르게 핸들링을 하기도하고..)
        - 생각해보니 커스텀 익셉션은 진짜 비즈니스 에러 용도로 많이 쓰는듯
- 다들 특정 checkedException이 발생하면, 더 구체적인 custom exception으로 변경해서 관리하나요?
    - 예원) 코틀린은 모두 언체크이긴한데, custom exception을 한 번 더 wrapping해서 던져보기도했음. (이젠 귀찮아서 그렇게 안하는 것 같다.._
    - 질문) 그럼 혹시 IOException 이런거 코틀린에서 발생하면 어떻게 되나요? 언체크해도됨?
        - 보배오빠 숙제입니다.
- 47p) 예시로 기존에 존재하는 Exception들.
    - 보배) 또 많이 사용하는 Exception들이 있는지? 나는 HTTP를 많이 다루다보니 관련되어 Spring web에 있는 Exception을 많이 활용하는 듯
        - 사실 근데 회사에서 base exception 만들어둔거 그냥 많이써서 내가 써봤자 딱히 눈에 안보이는 것 같기도 함…
        - try-catch를 할 것인가? ExceptionHandler(spring)을 사용할 것인가? 에 따른 관점 같기도 함.
- 짧아서 좋았다 개꿀~

## Item 7. 결과 부족이 발생할 경우 null 과 Failure 를 사용하라

- 49p) try-catch 블록 내부에 코드를 배치하면, 컴파일러가 할 수 있는 최적화가 제한됩니다.
    - 정민) 흠 왜 제한될까
        - gpt
            try-catch 블록 내부에 코드를 배치하면 JVM  컴파일러가 할 수 있는 최적화가 제한 된다.
            이말이 이해가 안돼 무슨 말이야
            
            try-catch 블록 내부에 코드를 배치하면 JVM(Java Virtual Machine) 컴파일러가 할 수 있는 최적화가 제한된다는 것은 다음과 같은 이유로 설명됩니다:
            
            1. **예외 발생 시 성능 저하**: try-catch 블록 내부에 코드를 배치하면 해당 블록에서 예외가 발생할 가능성이 있기 때문에, 이 예외를 처리하기 위한 추가적인 로직이 필요합니다. 이로 인해 성능이 저하될 수 있습니다.
            2. **최적화의 어려움**: try-catch 블록 내부에 코드가 있으면 JVM이 해당 코드를 최적화하기가 어려워집니다. 예외가 발생할 가능성이 있기 때문에 JVM은 예외가 발생할 때의 실행 흐름을 고려하여 최적화를 수행할 수 없습니다.
            
            따라서 예외 처리가 필요한 경우에만 try-catch 블록을 사용하고, 최대한 예외가 발생할 가능성이 적은 코드 블록에만 try-catch 블록을 배치하여 성능 저하를 최소화하는 것이 좋습니다.
            
            # 컴파일러 최적화는 뭔데?
            
            컴파일러 최적화는 컴파일러가 소스 코드를 기계어로 번역할 때, 프로그램의 실행 속도를 향상시키기 위해 코드를 수정하거나 재구성하는 과정을 말합니다. 이 최적화는 다음과 같은 목표를 가지고 이루어집니다:
            
            1. **실행 속도 향상**: 코드를 더 효율적으로 실행할 수 있도록 변경하여 프로그램의 전체 실행 속도를 높입니다.
            2. **메모리 사용량 최소화**: 프로그램이 실행될 때 필요한 메모리 양을 최소화하여 메모리 사용량을 줄입니다.
            3. **코드 크기 최소화**: 컴파일된 코드의 크기를 최소화하여 디스크 공간을 절약합니다.
            
            컴파일러 최적화는 여러 가지 방법으로 수행될 수 있습니다. 예를 들어, 루프 풀기(loop unrolling), 상수 접기(constant folding), 코드 인라이닝(code inlining), 불필요한 연산 제거, 레지스터 할당 최적화 등이 있습니다.
            
            그러나 try-catch 블록 내부에 코드를 배치하면 컴파일러가 이러한 최적화를 수행하는 것을 제한할 수 있습니다. 이는 try-catch 블록 내부의 코드가 예외가 발생할 수 있는 부분으로 간주되어, 최적화 과정에서 예외 처리 흐름을 고려해야 하기 때문입니다. 따라서 try-catch 블록을 최대한 작게 유지하고, 예외가 발생할 수 있는 부분에만 해당 블록을 사용하는 것이 성능 향상에 도움이 됩니다.
            
    - 보배) 나도 좀 궁금했는데, 뭐 JIT Compiler 같은게 보통 많이 실행되는 구문을 바탕으로 최적화를 해서 바이트코드를 만들어둘텐데 try-catch 하면 이런게 예상이 안가니깐(예외에 따라 동작이 변함) 최적화를 못하지 않을까 하는 생각이 들었음
        - 근데 뭐 사실 if 여러개쓴거랑 다른가? 싶기도 함ㅋㅋ 분기 복잡하게 하면 최적화 어렵습니다 이런 거랑 다른가?ㅋㅋ
        
        [Tiered Compilation in JVM | Baeldung](https://www.baeldung.com/jvm-tiered-compilation#compilation-levels)
        
    - 보배) 근데 다들 try-catch 사용하는지? 나는 기존에 많이 사용안하고 거의 보통 Exception handler level에서 처리했었는데, 레거시 다루다보면 예외가 예측이 안되서 exception handler level까지 보내기가 두려움. 커버해야하는 동작들이 있을텐데… 
이런 경우엔 try-catch를 사용하는데 하… 코드가 너무 복잡해져서 문제임 ㅠ
        - try-catch wrapping 해놓는 메서드 같은게 없나… 간편하게 이용할 수 있게
        - 예원) 에러를 먹어야하는 케이스에선 try-catch를 증말 잘 사용하고 있음.
- 범근) 이번 아이템은 결과 부족과 같은 경우에도 무작정 exception을 던지지 말고, null을 내려주거나 Result.isFailure와 같은 객체를 정보와 함께 내려달라는 말로 이해하면 될까요?
    - 보배) 난 이렇게 이해했어. null/Failure 같은 의미가 필요한 경우엔 exception 보내지 말고, 해당 값을 반환하는게 더 맞을 수 있다.
    - 예원) 저도 getOrNull 과 같은 메서드나, 성공/실패 여부같이 정보가 필요한 상황에서는 null 이나 failure 만들어쓰고 예외는 정보전달이 아닐 때.. 써라.
        - 근데 이 정보 전달이란 단어가 좀 애매한것 같은게 우리는 에러메시지에 정보를 담는디.. (”글자수가 부족해요. 8자 이상 작성해주세요” ← 요런건 정보가 아닌가 ㅋㅋ?)
        - 범근) 보통 클라한테 그렇게 넘깁니다요
- 51p) getOrNull 같은 네이밍
    - 보배) 코틀린에서는 `OrNull` suffix가 nullable인 경우 많이 활용되는 것 같은데… Java러로써 조금 어색함. 보통 현업에서도 nullable return method 구성할 때 해당 네이밍을 사용하는지?
    - 예원) 안했었는데,, 이번에 하자고 이야기나옴.
    - 정민) Collection 확장함수 `MaxTwoByOrNull` 요러거 만든적은 있는데, `MaxByOrNull` 요거 따라해서 만든거라 그때만 suffix가 ornull 이 되었습니다
    - 범근) 우리는 null이 나와도 Elvis 로 throw 하는 케이스가 많다
- 51p) 개발자는 항상 자신이 요소를 안전하게 추출할 거라 생각합니다.
    - 예원) 최근에 팀 바꾸고, 헷갈리지 않는 코드의 중요성을 많이 느낌

## Item 8. 적절하게 null 을 처리하라

- 52p) 안전호출
    - 정민 ) `A = A?.format(DateUtil.formatterPattern).toString()` 요런 주입이 있었는데,  오른쪽 A 는 String? 타입입니다. 정답 :  Any?.toString() null.toString() → “null” 
    오른쪽의 expireTs 가 null 인 경우 `expireTs` 는 어떤 값이 담길까요
        - 보배) null? javascript도 똑같은 elvis를 제공하는데 null로 됬던 듯
        - 예원) 왜 format()?.toString() 안해..? “null” 이 들어갈 수도 ㅋ있을것같다.
            - 훗.. 샤라웃 예원..
        - 범근) 정배로 null
- 53p) 스마트 캐스팅은 코틀린의 규약 기능을 지원합니다.
    - 예원) 규악! 기능이 뭔진 모르지만 예씨에 나온거로는 “isNullOrBlank()” 같은거~?
- 53p) `val printName2 = printer?.name ?: return`
    - 보배) 이런 코드가 쓰일 때가 있나….? 그냥 null 반환 처리되는 것 같은데 ㅋㅋㅋ 신기해씀…
    - 예원) 만약 이 메서드의 return 타입이 Unit이라면 [printer.name](http://printer.name) 이 null 일 때 메서드를 종료시켜버린다는거 아닌가

- 55p) !! 타입은 nullable 이지만, null 이 나오지 않는 것이 거의 확실한 상황에서 많이 사용된다. 하지만 현재 확실하다고, 미래에 확실한 것은 아닙니다. 문제는 미래의 어느 순간에 일어난다.
    - 정민 ) 222. 회사에서도 그래서 !! 잘 안쓰려고 하는 듯.
    - 보배) 333. 만약 `!!` 을 쓴 코드가 있다? 어떻게 설득할 것인가?
        - 예원) 우리는 쫌 쓰는 분이 계신데, 이거 not-nullable을 무조건 보장할 수 있나요? 에러가 발생했을 때 따로 처리해주지 않아도 괜찮은가요? 같은 리뷰를 달았던 기억.
- 56p) 범근) detekt와 같은 정적 분석 도구는 !! 연산자를 사용하면 아예 오류를 발생시킨다네, 거의 쓰면 혼나는 수준
    - 예원) 우리는 저거 있었다면 많이 맞았다.
- 57p) 예외는 예상하지 못한 잘못된 부분을 알려 주기 위해서 발생하는 것입니다.
    - 예원) 글쓴이 분은 예외의 역할을 비즈니스 적으로는 보지 않는 것 같은..느낌 (”글자수 부족해요”,  / “게시글 삭제할 수 없어요” 요런)
    - 관점의 문제인 것 같긴 함.
- 57p) Delegates 이런거 많이 씁니까?
    - 보배) 보통 어떤 때 쓰는지? 어떤 기능들을 가지고 있는지?
        - 예원) Delegates는 첨봄. lateinit 혹은 lazy 변수는 꽤 씀.  (필요한 시기에 호출하기 위해서)
- 59p) lateinit
    - 범근) lateinit으로 초기화를 지연시키려는 프로퍼티는 var로 선언해야만 한다. val 프로퍼티는 final 필드로 컴파일되고 무조건 생성자 안에서만 초기화해야 한다.
- 60p) 범근) 기본 타입 프로퍼티는 왜 lateinit을 사용못할까?
    - 보배) 기본 타입 프로퍼티는 class 파일로 컴파일 될 때 primitive type으로 해석되기 때문
        - lateinit은 null로 해두고 getter에서 설정안되어있으면 exception 터트리는 식으로 구성됨.
        - Int > int : int는 nullable 할 수 없으므로 lateinit이 불가능하다.
    - 예원) 기본 타입 프로퍼티가 primitive type 이라고 이해하면서 읽었는데, 둘이 다른거구나..? 다른건가
- 60p) 보배) “이후에 떤 의미를 나타내기 위해서 null을 사용하고 싶을 때, nullable로 만들 수도 있습니다”라는게 무슨 뜻인지? 한번 초기화 되면 다시 null로 설정은 안되는 것 아닌가? ‘아예 초기화를 안해둘 수도 있다’라는 의미인가?
    - 예원) lateinit은 var로 설정되어있다보니까 여기에 다시 null을 넣어줄 수 있다는거 아닌가..
    
![image](https://github.com/mash-up-kr/S3A/assets/27190617/a2b1efd7-99cd-4b35-b307-34afc10d0a99)


## Item 9. use를 사용하여 리소스를 닫아라

- 정민 ) **Closeable을 상속받는 모든 class에 접근할 수 있는 use 확장 함수가 있다.**
    - 하지만 결국 catch 에서 Exception throw 하므로 try-catch 묶긴 해야할듯

```kotlin
@InlineOnly
@RequireKotlin("1.2", versionKind = RequireKotlinVersionKind.COMPILER_VERSION, message = "Requires newer compiler version to be inlined correctly.")
public inline fun <T : Closeable?, R> T.use(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    var exception: Throwable? = null
    try {
        return block(this)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        when {
            apiVersionIsAtLeast(1, 1, 0) -> this.closeFinally(exception)
            this == null -> {}
            exception == null -> close()
            else ->
                try {
                    close()
                } catch (closeException: Throwable) {
                    // cause.addSuppressed(closeException) // ignored here
                }
        }
    }
}
```

- 보배) 이거 그냥 java try-with-resources 쓰는거랑 같은거 아닌가 하는 생각
    - 정민이 위 코드처럼 예외처리는 use에서 못해서 java가 더 나은 것 같기도…ㅋㅋ ([link](https://github.com/Meet-Coder-Study/book-effective-java/blob/main/2장/9_try-finally보다는%20try-with-resources를%20사용하라_김보배.md))

## Item 10. 단위 테스트를 만들어라

- 67p) 단위 테스트를 만드는 데 시간이 걸립니다.
    - 정민) 늘 어떤 책을 읽든 단위 테스트가 중요한 것은 알지만  window 범위만큼에 대한 테스트할 때 data set 자체를 작성하는게 하루 온종일 걸림ㅠ
    - 예원) 어라 이거 어제 들었던 것 같은데,,
- 단위 테스트는 코드를 신뢰할 수 있게 만듭니다. 리팩터링이 두렵지 않게 해줍니다. 확인도 빠릅니다. 추후 디버깅과 버그 찾기 시간 줄여줍니다.
    - 근데,, 단위 테스트 잘 짜려면 구조가 아름답게 만들어져 있어야합니다. (눈물)
