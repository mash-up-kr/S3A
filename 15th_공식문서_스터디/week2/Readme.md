# Document

- https://docs.spring.io/spring-framework/reference/core/beans/annotation-config.html
- https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired.html
- https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-primary.html
- https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-qualifiers.html
- https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/generics-as-qualifiers.html
- https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/custom-autowire-configurer.html
- https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/value-annotations.html
- (Optional) https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-autowire.html

<br/>

## Code

- `AbstractAutowireCapableBeanFactory#populateBean`
- `AutowiredAnnotationBeanPostProcessor`
- `AutowiredFieldElement#inject`
    - `AutowiredMethodElement` 요건 왜 안 다뤘는지
        - 답변 : field 가 이해하기 쉬워서 field 로 다룸
- `AutowiredAnnotationBeanPostProcessor#registerDependentBeans`
    - `DefaultListableBeanFactory#resolveDependency`

`@Autowired` 가 코드상에서 어떻게 동작하는지 보고오기

<br/>

## Question

1. `@Autowired` 는 왜 BeanPostProcessor에서 처리되고 있을까? BeanFactoryPostProcessor에서는 왜 처리를 못할까? BeanPostProcessor와 BeanFactoryPostProcessor의 차이는 뭘까?
   - 실행 시점 차이에서 비롯된 걸로 추정
   - BeanPostProcessor 는 빈 생성 후 매번 수행
   - spring boot starter 를 custom 할 때 유용하게 사용할 수 있을 듯 하다.
     - 실제 사용 사례 : spring-chassis → starter custom library (남영 회사)
   - BeanFactoryPostProcessor custom 시, static 키워드 bean 등록이 필요함.
  

2. 왜 6.2버전부터 `@Fallback`을 추가했을까? 배경이 궁금하다
   - https://github.com/spring-projects/spring-framework/issues/26241
   - `DefaultListableBeanFactory#determineAutowireCandidate`
  

3. `AutowireCandidateResolver` 는 무슨 역할을 하는 인터페이스인가?
   - `@Autowired` 후보 키를 읽어오는 역할
   - 어노테이션 제외하고도 쓸 수 있을까?
   - `@Qualifer` 어노테이션의 값을 읽을 때도 쓰지 않을까?  → `QualifierAnnotationAutowireCandidateResolver#getSuggestedValue`
   - `@Value` 어노테이션의 값 → `QualifierAnnotationAutowireCandidateResolver#getSuggestedValue`
  

4. Bean 주입 시 resolveDependency() 는 어떤 기준으로 고를까? (동일한 타입 Bean 여러 개 일시)
   - `determineAutowireCandidate`
   - primary 우선, 이름 매칭, 우선 순위 (이를 테면 Order) - DefaultListableBeanFactory
  

5. resolveFieldValue() 는 왜 syncronized bolock 을 사용하는가? spring startup 시는 single thread 로 수행하는 게 아닌가?
   - `AutowiredAnnotationBeanPostProcessor`
   - 멀티스레드 환경
     - lazy injection, object factory - 실제 의존성 주입은 사용 시점에 - 여러 스레드 동시 접근 가능하다고 함..
       - 기본 설정에서는 빈을 context 초기화 시점에 생성하므 발생할 수 없으나 lazy하게 빈을 생성하도록 설정하면 발생할 수 있음.
       - lazy하게 생성되므로 생성 전에 요청이 동시에 여러 번 들어오면 여러 번 생성 요청이 들어갈 수도 있음.
       - `ConfigurationClassPostProcessor` Bean 생성 일부를 병렬로 처리 가능 → dependsOn, 비동기 컨텍스트 로딩 등 병렬 생성 가능
       - https://github.com/spring-projects/spring-framework/issues/10329
       - bean 생성 시, 여러 스레드에서 동시 접근할 수 있다. `preInstantiateSingletons`
      

6. 다중 DB datasource 상황에서, 특정 transaction manager 를 primary 로 등록한 상태, jpa repository 주입 받을 때 트랜잭션 매니저를 지정하여 사용 불가한 이유?
   - B트랜잭션 매니저 사용하려고 하는데, @Transacitonal(transactionManager = “BTransactionManager”) 요렇게 사용해도 A 가 Primary 이면, A 로 jdbc connection 으로 잡히더라구
   - TransactionInterceptor 에서 디버깅해서 알아오겠습니다.
     ```java
     public TransactionInterceptor(TransactionManager ptm, TransactionAttributeSource tas) {
       setTransactionManager(ptm);
       setTransactionAttributeSource(tas);
     }
     ```
        
7. @Bean, @Fallback 을 같이 쓰는 경우는 빈 등록에 실패했을 때 대체 빈 등록으로 쓰는 것 같은데, 이걸로 1주차에 나왔던 질문인 빈 재정의 기능을 대신할 수 있을까?
   - No Answer...
  

8. AutowiredElement 클래스 이름 뜻? 특정 시점을 나타내는 성격이 훨씬 두드러지는데 구성요소로만 나타낸 이유가 있을까?
   - 초기 코드여서 그럴 수도..?

<br/>

## Next Week

- https://docs.spring.io/spring-data/jpa/reference/index.html
- AOT → 공부용 (연구용) ⇒ 후우선순위
- redis를 우선순위로
