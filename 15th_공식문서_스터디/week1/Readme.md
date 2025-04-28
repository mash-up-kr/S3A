# Document

- [Introduction to the Spring IoC Container and Beans](https://docs.spring.io/spring-framework/reference/core/beans/introduction.html)
- [Container Overview](https://docs.spring.io/spring-framework/reference/core/beans/basics.html)
- [Bean Overview](https://docs.spring.io/spring-framework/reference/core/beans/definition.html)

<br/>

## Code
- `DefaultListableBeanFactory`: 기본 빈 팩토리 구현체
- `AbstractApplicationContext`: 기본 ApplicationContext의 기능을 구현한 구현체 
- `GenericApplicationContext`: `AbstractApplicationContext` 자식 클래스이고, `DefaultListableBeanFactory` 에게 위임해주는 구현체

<br/>

## Goal

- 질문 설명
    - 배경, 왜 궁금했는지
- 자답 or 토의

<br/>

## Quesion

1. `@Configuration`은 언제/어떻게 proxy로 등록될까? proxy로 등록하면 어떤 이점이 있을까?
   - `@Configuration`과 `@Component`는 BeanDefinition 등록 이전에 등록된다. 등록 시점에 CGLIB을 사용해 기능을 확장한 프록시로 등록한다.
       - 프록시는 쉽게 객체의 동작을 확장할 수 있는 방법이다.
   - `@Configuration` 내에서 선언된 bean method 호출 vs `@Component` 내에서 선언된 bean method 호출
     - `@Configuration` 내에서 bean method를 호출하면 `BeanMethodInterceptor` 가 동작을 가로챈다. 
     - `@Component` 내에서 bean method 호출은 가로채지 않고 그대로 호출한다.
   - `@Transactional` 
     - `TransactionInterceptor`가 메서드 호출을 가로챈다. 
     - 어느 시점에 advisor 대상이 되는지 찾아보기. `getBean()` 추정
    

2. Spring에서는 기본적으로 Bean을 singleton scope 관리하지만, 왜 애초에 다양한 scope를 제공할 필요가 있었을까? 개발자가 scope를 명시적으로 지정하지 않으면 놓칠 수 있는 문제는 무엇인가?
   - singleton 의 경우 상태에서 동시성 문제가 발생할 수 있음
     - request scope (원영 회사에서 사용), session scope
   - framework, library 개발자 입장에서 client에 유연하게 제공하기 위해서?
   - logging 시에도 사용 사례가 있음 (알아봐 주십쇼 궁금하네요~)
  

3. Bean을 lazy하게 초기화하면 어떤 장점/단점이 있을까?
   - 장점: 
     - 불필요한 빈을 생성하지 않으므로 성능 향상
     - 유사 caching 효과 ⇒ val supplier = { initialization() } → supplier.get()
   - 단점:
     - 컴파일 타임에 의존성 사이클 탐지를 할 수 없다. `A → B → A`
    

4. GenericApplicationContext는 왜 BeanFactory를 직접 갖고 있고, 그 기능을 왜 위임하는 구조?
   - application context 는 일종의 facade 패턴?
      ```java
      public class GenericApplicationContext extends AbstractApplicationContext implements BeanDefinitionRegistry {

        private final DefaultListableBeanFactory beanFactory;

        ...
      ```

5. `ApplicationContext`가 `BeanFactory`를 확장하는 구조를 갖도록 한 설계적 이유는 무엇일까? 
    - 초창기 커밋보기, 과연 누가 먼저 만들어졌을 것인가?
  

6. Bean 등록 설정에서 alias 의 역할 및 목적은? → issue or PR 기록 찾기
   - 확장성
   - 하나의 클래스를 이름만 다른 여러 빈으로 재탕 가능 → 어떤 사례?
   - 멀티 모듈에서 bean 등록시 이름 의미화? → 이슈 찾아보기
  

7. Bean 인스턴스화 방식에서 정적 팩토리 메서드와 인스턴스 팩토리 메서드 방식의 이점은?
   - framework 개발자의 bean 등록 다양성 지원
  

8. Bean 재정의의 위험성은? 향후 버전에서 deprecated 되는 이유는?
   - 초기 철학이 흔들림 → 해달라고 해서 해주긴 하는데.. 프로젝트 육성에 너무나 큰 걸림돌
   - 언제 재정의 될 줄 몰라 예측하기 어려움

<br/>

### 피드백

- 구체적인 설명 vs 열띤 토론
    - 더 해보고 결정, 지금도 나쁘지 않았음
- 답변 정리 할까 말까? → 질문한 사람이 하기
    - https://github.com/mash-up-kr/S3A
    - 매 달 마지막 주, 담당자는 룰렛으로 정하기
    - 이원영: 김경환이 걸릴 것 같음 ㅋㅋ
- 오전 9시 시작??? ㅎㅎ → 유지

![image.png](attachment:2e7af58e-8aac-4605-9c42-94d123474706:image.png)

## 다음 이시간에

![image.png](attachment:9f983505-2a3c-45a9-a685-ff17019a1cde:image.png)

https://github.com/spring-projects/spring-framework/issues/4828

### 분량

**Java-based Configuration vs Annotation-based Configuration 둘의 차이를 생각하면서 읽기**

어떻게 `@Autowired` 가 작동할까?

- https://docs.spring.io/spring-framework/reference/core/beans/annotation-config.html
- https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired.html
- https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-primary.html
- https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-qualifiers.html
- https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/custom-autowire-configurer.html
- https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/resource.html

### 코드

- AbstractAutowireCapableBeanFactory#initializeBean
