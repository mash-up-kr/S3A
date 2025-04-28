### OT 공유 주제

- 왜 토론을 하려고 하는가?
    - 기억을 더 잘하기 위해서
    - 생각을 정리해서 얘기하다 보면 기억에 더 잘 남음
- 왜 코드를 보려 하는가?
    - 문서보다 코드가 사실 더 최신 정보임.
    - 문서와 코드가 충돌한다면 코드가 정답임.
    - 코드를 보는 게 더 이해가 잘 되는 사람도 있음 (나)
- 나는 잘 모르는데 토론을 할 수 있나?
    - 나도 잘 모름..
    - 퀴즈 식의 정답이 있는 질문은 피하려고 함.
    - 읽고 궁금해진 점이나 알고 싶은 점을 다같이 토론(토의)하고자 함
    - 질문의 예, "왜 컨테이너에게 주도권을 넘겨줘야 하는가? 어떤 장점과 단점이 있을까?" (IOC)
- 왜 시범적으로 한 달만 하려하는가?
    - 이 방식이 적합한지, 도움이 되는지 확인하기 위한 목적
    - 누군가에게는 부담이 될 수도 있음. **토론이 의미있기 위해서는 주도적으로 해야함.**
- 왜 이름이 공식 문서 토론 스터디인가?
    - 스프링만 하지는 않을 것 같음. 토론하기 재미있는 주제 찾기가 생각보다 쉽지 않음
    - Redis, Kafka, Fixture Monkey 등 우리가 자주 사용하는 플랫폼의 문서도 다룰 예정
- 실제 사례?
    - 네이버페이 클레임 요청 화면들을 전환할 때 매우 효율적으로 한 사례
        - (구) 네이버페이의 화면은 모두 freemarker라는 템플릿 엔진으로 만듬.
        - (구) 네이버페이는 필드가 300개 이상 있는 무서운 객체들이 있음.
        - 화면을 생각없이 전환하려면 무서운 객체들을 똑같이 맞춰줘야 함. → 내가 제일 싫어하는 거
        - 내가 하려 한거 → 프리마커에서 실제로 사용한 필드만 식별하고 그 필드들만 전환
        - How? 스프링에서 프리마커를 사용해서 화면을 어떻게 그리는지 동작을 확인
        - 프리마커에서는 TemplateModel라는 인터페이스로 객체를 wrapping해서 관리함.
        - 나는 스칼라 값에 해당하는 TemplateModel 구현체를 구현하고 값을 조회할 때 사용하는 `get` 메서드에 로깅을 추가함. (프록시라고 봐도 됨)
        - 스프링에서 프리마커로 render하는 지점에서 내가 만든 TemplateModel을 끼워넣음
            - 스터디에서 얻어갔으면 하는거
        - 결과 ⇒ 불필요한 작업 99%를 줄임 → 매우 빠른 전환

- 진행방식
    - 다음주까지 문서, 코드를 읽기
    - 노션에 만들어진 다음 주차 페이지에 질문을 적기 (인당 최소 하나)
    - (Optional) 질문을 보고 답변을 준비하기

### TODO

- OT중, 후에 할 일
    - https://github.com/spring-projects/spring-framework.git clone하기
    - https://github.com/spring-projects/spring-boot.git 도 clone하기
- 결정 해야 할 일
    - 다음주 스터디 시간
        - 평일에 할지?
        - 다다음주에 할지? → **4월 5일(토) 10:00**

### 다음 이 시간에

- 첫 시간 분량
    - 문서
        - IoC와 컨테이너, 빈
            - [Introduction to the Spring IoC Container and Beans](https://docs.spring.io/spring-framework/reference/core/beans/introduction.html)
            - [Container Overview](https://docs.spring.io/spring-framework/reference/core/beans/basics.html)
            - [Bean Overview](https://docs.spring.io/spring-framework/reference/core/beans/definition.html)
    - 코드
        - 우리가 Spring boot를 사용하면 기본적으로 사용하는 `ApplicationContext`인 `AnnotationConfigServletWebServerApplicationContext` 를 살펴보고 오기 [링크](https://github.com/spring-projects/spring-boot/blob/fe163d3a9cf106cb3415a0bc4a0d63433c9ab0de/spring-boot-project/spring-boot/src/main/java/org/springframework/boot/web/servlet/context/AnnotationConfigServletWebServerApplicationContext.java#L56)
        - 스프링 띄워보면서 디버깅 모드로 브레이킹 포인트 찍어서 콜스택 까보는 것을 추천
- 4월 5일 (토) 10:00 까지 [1주차](https://www.notion.so/1-1bfbee7f599680f08493cde4e96d23f6?pvs=21) 에 질문 남겨두기
