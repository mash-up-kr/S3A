## 6.5 스프링 AOP
### 6.5.1 자동 프록시 생성
- 부가기능의 적용이 필요한 타깃 오브젝트마다 거의 비슷한 내용의 ProxyFactoryBean 빈 설정정보를 추가해주어야 한다.
#### 중복 문제의 접근 방법
- JDBC API를 사용하는 DAO 코드 : 템플릿 콜백 패턴으로 해결
- 프록시 클래스 코드 : 다이내믹 프록시라는 런타임 코드 자동생성 기법을 이용
    - JDK의 다이내믹 프록시는 특정 인터페이스를 구현한 오브젝트에 대해서 프록시 역할을 해주는 클래스를 런타임 시 내부적으로 만들어준다. 런타임 시에 만들어져 사용되기 때문에 클래스 소스가 따로 남지 않을 뿐이지 타깃 인터페이스의 모든 메소드를 구현하는 클래스가 분명히 만들어진다.
    - 변하지 않는 타깃으로의 위임과 부가기능 적용 여부 판단이라는 부분은 코드 생성기법을 이용하는 다이내믹 프록시 기술에 맡기고, 변하는 부가기능 코드는 별도로 만들어서 다이내믹 프록시 생성 팩토리에 DI로 제공하는 방법을 사용했다.
#### 반복적인 프록시의 메소드 구현은 코드 자동생성 기법을 이용해 해결했다면 반복적인 ProxyFactoryBean 설정 문제는 설정 자동등록 기법으로 해결할 수 없을까?
- BeanPostProcessor는 인터페이스를 구현해서 만드는 빈 후처리기다. 빈 후처리기는 이름 그대로 스프링 빈 오브젝트로 만 들어지고 난 후에, 빈 오브젝트를 다시 가공할 수 있게 해준다.
```java
public interface BeanPostProcessor {
	@Nullable
	default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Nullable
	default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}
}
```
- DefaultAdvisorAutoProxyCreator
    - 스프링이 제공하는 빈 후처리기 중 하나
    - 어드바이저를 이용한 자동 프록시 생성기
- 스프링은 빈 후처리기가 빈으로 등록되어 있으면 빈 오브젝트가 생성될 때마다 빈 후처리기에 보내서 후처리 작업을 요청한다.
    - 빈 후처리기는 빈 오브젝트의 프로퍼티를 강제로 수정할 수도 있고 별도의 초기화 작업을 수행할 수도 있다.
    - 심지어는 만들어진 빈 오브젝트를 자체를 바꿔치기할 수도 있다.

![](https://velog.velcdn.com/images/haron/post/cb278a3d-cc9f-4f0f-a27d-03a7721d598e/image.png)

1. DefaultAdvisorAutoProxyCreator 빈 후처리기가 등록되어 있으면 스프링은 빈 오브젝트를 만들 때마다 후처리기에게 빈을 보낸다.
2. DefaultAdvisorAutoProxyCreator는 빈으로 등록된 모든 어드바이저 내의 포인트컷을 이용해 전달 받은 빈이 프록시 적용 대상인지 확인한다.
3. 프록시 적용 대상이면 그때는 내장된 프록시 생성기에게 현재 빈에 대한 프록시를 만들게 하고 만들어진 프록시에 어드바이저를 연결해준다.
4. 빈 후처리기는 프록시가 생성되면 원래 컨테이너가 전달해준 빈 오브젝트 대신 프록시 오브젝트를 컨테이너에게 돌려준다.
5. 컨테이너는 최종적으로 빈 후처기가 돌려준 오브젝트를 빈으로 등록하고시용한다.

- 적용할 빈을 선정하는 로직이 추가된 포인트컷이 담긴 어드바이저를 등록하고 빈 후처리기를 사용하면 일일이 ProxyFactoryBean 빈을 등록하지 않아도 타깃 오브젝트에 자동으로 프록시가 적용되게 할 수 있다.

#### 확장된 포인트컷
```java
public interface Pointcut {

	ClassFilter getClassFilter();

    MethodMatcher getMethodMatcher();

    Pointcut TRUE = TruePointcut.INSTANCE;

}
```
- 포인트컷은 클래스 필터와 메소드 매처 두 가지를 돌려주는 메소드를 갖고 있다.
    - ClassFilter : 프록시에 적용할 클래스인지 확인한다.
    - MethodMatcher : 어드바이스를 적용할 메소드인지 확인한다.
    - 클래스 필터는 모든 클래스를 다 받아주도록 만들어져 있다.
- Pointcut 선정 기능을 모두 적용한다면 먼저 프록시를 적용할 클래스인지 판단하고 나서, 적용 대상 클래스인 경우에는 어드바이스를 적용할 메소드인지 확인하는식으로 동작한다.
- 모든 빈에 대해 프록시 자동 적용 대상을 선별해야 하는 빈 후처리기인  DefaultAdvisorAutoProxyCreator 는 클래스와 메소드 선정 알고리즘을 모두 갖고 있는 포인트컷이 필요하다. 정확히는 그런 포인트컷과 어드바이스가 결합되어 있는 어드바이저가 등록되어 있어야한다.
### 6.5.2 DefaultAdvisorAutoProxyCreator의 적용
#### 클래스 필터를 적용한 포인트컷 작성
- 메소드 이름만 비교하던 포인트컷인 NameMatchMethodPointcut을 상속해서 프로퍼티로 주어진 이름 패턴을 가지고 클래스 이름을 비교하는 ClassFilter를 추가하도록 만든다.
```java
public class NameMatchClassMethodPointcut extends NameMatchMethodPointcut {

    public void setMappedClassName(String mappedClassName) {
    // 모든 클래스를 다 허용하던 디폴트 클래스 필터를 프로퍼티로 받은 클래스 이름을 이용해서 필터를 만들어 덮어씌운다.
        setClassFilter(new SimpleClassFilter(mappedClassName));
    }

    static class SimpleClassFilter implements ClassFilter {

        private final String mappedName;

        public SimpleClassFilter(String mappedName) {
            this.mappedName = mappedName;
        }

        @Override
        public boolean matches(Class<?> clazz) {
            return PatternMatchUtils.simpleMatch(mappedName, clazz.getSimpleName());
        }
    }
}
```
#### 어드바이저를 이용하는 자동 프록시 생성기 등록
- DefaultAdvisorAutoProxyCreator는 등록된 빈 중에서 Advisor 인터페이스를 구현한 것을 모두 찾는다. 그리고 생성되는 모든 빈에 대해 어드바이저의 포인트컷을 적용해보면서 프록시 적용 대상을 선정한다.
- 기존의 포인트컷 설정을 삭제하고 새로 만든 클래스 필터 지원 포인트컷을 빈으로 등록한다.
- ProxyFactoryBean으로 등록한 빈에서처럼 어드바이저를 명시적으로 DI 하는 빈은 존재하지 않는다. 대신 어드바이저를 이용하는 자동 프록시 생성기인 DefaultAdvisorAutoProxyCreator에 의해 자동 수집되고, 프록시 대상 선정 과정에 참여하며 자동 생성된 프록시에 다이내믹하게 DI 돼서 동작하는 어드바이저가 된다.

```java
// DefaultAdvisorAutoProxyCreator 등록은 다음 한 줄과 같다.
<bean class="org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator" />
```

#### 자동생성 프록시 확인
- DefaultAdvisorAutoProxyCreator에 의해 빈이 프록시로 바꿔치기됐다면 getBean() 메소드로 가져온 오브젝트는 JDK의 Proxy 타입일 것이다. JDK 다이내믹 프록시 방식으로 만들어지는 프록시는 Proxy 클래스의 서브클래스이기 때문이다.

### 6.5.3 포인트컷 표현식을 이용한 포인트컷
- 스프링은 아주 간단하고 효과적인 방법으로 포인트컷의 클래스와 메소드를 선정하는 알고리즘을 작성할 수 있는 방법을 제공한다. 이것을 포인트컷 표현식이라고 부른다.

#### 포인트컷 표현식
- 포인트컷 표현식을 지원하는 포인트컷을 적용하려면 AspectJExpressionPointcut 클래스를 사용하면 된다.
- AspectJExpressionPointcut은 클래스와 메소드의 선정 알고리즘을 포인트컷 표현식을 이용해 한 번에 지정할 수 있게 해준다.
- 스프링이 사용하는 포인트컷 표현식은 AspectJ라는 유명한 프레임워크에서 제공하는 것을 가져와 일부 문법을 확장해서 사용하는 것이다. 그래서 이를 AspectJ 포인트컷 표현식이라고도 한다.

### 6.5.4 AOP란 무엇인가?

#### AOP: 애스펙트 지향 프로그래밍
- 부가기능 모듈화 작업은 기존의 객체지향 설계 패러다임과는 구분되는 새로운 특성이 있다고 생각했다. 그래서 이런 부가기능 모듈을 객체지향 기술에서는 주로 사용하는 오브젝트와는 다르게 특별한 이름으로 부르기 시작했다. 그것이 바로 애스펙트(aspect)다. 애스펙트란 그 자체로 애플리케이션의 핵심기능을 담고 있지는 않지만, 부가될 기능을 정의한 코드인 어드바이스와, 어드바이스를 어디에 적용할지를 결정하는 포인트컷을 함께 갖고 있다.
- 애플리케이션의 핵심적인 기능에서 부가적인 기능을 분리해서 애스펙트라는 독특한 모듈로 만들어서 설계하고 개발하는 방법을 애스펙트 지향 프로그래밍(Aspect Oriented Programming) 또는 약자로 AOP라고 부른다.

![](https://velog.velcdn.com/images/haron/post/fd9dfb8f-3b7a-497d-96c5-358eb1633cf5/image.png)

- 왼쪽
  애스펙트로 부가기능을 분리하기 전의 상태다. 핵심기능은 깔끔한 설계를 통해서 모듈화되어 있고 객체지향적인 장점을 잘 살릴 수 있도록 만들었지만 부가기능이 핵심기능의 모듈에 침투해 들어가면서 설계와 코드가 모두 지저분해졌다.

- 오른쪽
  핵심기능 코드 사이에 침투한 부가기능을 독립적인 모듈인 애스펙트로 구분해낸 것이다.
  런타임 시에는 왼쪽의 그림처럼 각 부가기능 애스펙트는 자기가 필요한 위치에 다이내믹하게 참여하게 될 것이다. 하지만 설계와 개발은 오른쪽 그림처럼 다른 특성을 띤. 애스펙트들을 독립적인 관점으로 작성하게 할 수 있다.

### 6.5.5 AOP 적용 기술

#### 바이트코드 생성과 조작을 통한 AOP
- AspectJ는 스프링처럼 다이내믹 프록시 방식을 사용하지 않는다.
- AspectJ는 프록시처럼 간접적인 방법이 아니라, 타깃 오브젝트를 뜯어 고쳐서 부가 기능을 직접 넣어주는 방법을 사용한다.
- 컴파일된 타깃의 클래스 파일 자체를 수정하거나 클래스가 JVM에 로딩되는 시점을 가로채서 바이트코드를 조작하는 복잡한 방법을 사용한다.
- 장점
    - 자동 프록시 생성 방식을 사용하지 않아도 AOP를 적용할 수 있다.
    - 프록시 방식보다 훨씬 강력하고 유연한 AOP가 가능하다. 프록시를 AOP의 핵심 메커니즘으로 사용하면 부가기능을 부여할 대상은 클라이언트가 호출할 때 사용하는 메소드로 제한된다. 하지만 바이트 코드를 직접 조작해서 AOP를 적용하면 오브젝트의 생성, 필드 값의 조회와 조작, 스태틱 초기화 등의 다양한 작업에 부가 기능을 부여해 줄 수 있다.

> JDK Dynamic Proxy 와 CGLib

## 6.6 트랜잭션 속성
```java
@Override
public Object invoke(MethodInvocation invocation) throws Throwable {
    // 트랜잭션 시작
    TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
    try {
        Object result = invocation.proceed();
        this.transactionManager.commit(status);
        return result;
    } catch (Exception e) {
        this.transactionManager.rollback(status);
        throw e;
    }
}
```
- 트랜잭션의 경계는 트랜잭션 매니저에게 트랜잭션을 가져오는 것과 commit(), rollback() 중의 하나를 호출하는 것으로 설정된다.
- DefaultTransactionDefinition의 용도는 무엇일까?

### 6.1.1 트랜잭션 정의
- DefaultTransactionDefinition이 구현하고 있는 TransactionDefinition 인터페이스는 트랜잭션의 동작방식에 영향을 줄 수 있는 네 가지 속성을 정의하고 있다.
#### 트랜잭션 전파
Spring이 제공하는 선언적 트랜잭션(트랜잭션 어노테이션, @Transactional)의 장점 중 하나는 여러 트랜잭션을 묶어서 커다란 하나의 트랜잭션 경계를 만들 수 있다는 점이다. 작업을 하다보면 기존에 트랜잭션이 진행중일 때 추가적인 트랜잭션을 진행해야 하는 경우가 있다. 이미 트랜잭션이 진행중일 때 추가 트랜잭션 진행을 어떻게 할지 결정하는 것이 전파 속성(Propagation)이다.

전파 속성에 따라 기존의 트랜잭션에 참여할 수도 있고, 별도의 트랜잭션으로 진행할 수도 있고, 에러를 발생시키는 등 여러 선택을 할 수 있다. 이렇게 하나의 트랜잭션이 다른 트랜잭션을 만나는 상황을 그림으로 나타내면 다음과 같다.
![](https://velog.velcdn.com/images/haron/post/d5284f77-98bf-4e09-b0ce-a27d48ace084/image.png)

- 물리 트랜잭션과 논리 트랜잭션
  트랜잭션은 데이터베이스에서 제공하는 기술이므로 커넥션 객체를 통해 처리한다. 그래서 1개의 트랜잭션을 사용한다는 것은 하나의 커넥션 객체를 사용한다는 것이고, 실제 데이터베이스의 트랜잭션을 사용한다는 점에서 물리 트랜잭션이라고도 한다.
  트랜잭션 전파 속성에 따라서 외부 트랜잭션과 내부 트랜잭션이 동일한 트랜잭션을 사용할 수도 있다. 하지만 스프링의 입장에서는 트랜잭션 매니저를 통해 트랜잭션을 처리하는 곳이 2군데이다. 그래서 실제 데이터베이스 트랜잭션과 스프링이 처리하는 트랜잭션 영역을 구분하기 위해 스프링은 논리 트랜잭션이라는 개념을 추가하였다.


![](https://velog.velcdn.com/images/haron/post/948d0459-a25d-4c7a-ad5a-b136036c9291/image.png)

위 그림은 외부 트랜잭션과 내부 트랜잭션이 1개의 물리 트랜잭션(커넥션)을 사용하는 경우이다.이 경우에는 2개의 트랜잭션 범위가 존재하기 때문에 개별 논리 트랜잭션이 존재하지만, 실제로는 1개의 물리 트랜잭션이 사용된다. 만약 트랜잭션 전파 없이 1개의 트랜잭션만 사용되면 물리 트랜잭션만 존재하고, 트랜잭션 전파가 사용될 때 논리 트랜잭션 개념이 사용된다. 이러한 물리 트랜잭션과 논리 트랜잭션을 정리하면 다음과 같다.

- 물리 트랜잭션: 실제 데이터베이스에 적용되는 트랜잭션으로, 커넥션을 통해 커밋/롤백하는 단위
- 논리 트랜잭션: 스프링이 트랜잭션 매니저를 통해 트랜잭션을 처리하는 단위

> 모든 논리 트랜잭션이 커밋되어야 물리 트랜잭션이 커밋됨
하나의 논리 트랜잭션이라도 롤백되면 물리 트랜잭션은 롤백됨


![](https://velog.velcdn.com/images/haron/post/0b94d39c-6307-4d06-92a3-23edaa88bce9/image.png)


#### 격리수준
모든 DB 트랜잭션은 격리 수준을 갖고 있어야한다. 적절하게 격리수준을 조정해서 가능한 한 많은 트랜잭션을 동시에 진행시키면서도 문제가 발생하지 않게 하는 제어가 필요하다.격리수준은 기본적으로 DB에 설정되어 있지만 JDBC 드라이버나 DataSource 등에서 재설정할 수 있고, 필요하다면 트랜잭션 단위로 격리수준을 조정할 수 있다. DefaultTransactionDefinition에 설정된 격리수준은 ISOLATION_DEFAULT다. 이는 DataSource에 설정되어 있는 디폴트 격리수준을 그대로 따른다는 뜻이다.

```
MySQL의 격리 수준

READ UNCOMMITTED(커밋되지 않은 읽기)
READ COMMITTED(커밋된 읽기)
REPEATABLE READ(반복 가능한 읽기)
SERIALIZABLE(직렬화 가능)
```

#### 제한시간
트랜잭션을 수행하는 제한시간을 설정할 수 있다. DefaultTransactionDefinition의 기본 설정은 제한시간이 없는 것이다. 제한시간은 트랜잭션을 직접 시작할 수 있는 PROPAGATION_REQUIRED, PROPAGATION_REQUIRES_NEW와 함께 사용해야만 의미가 있다.

#### 읽기전용
읽기 전용으로 설정해두면 트랜잭션 내에서 데이터를 조작하는 시도를 막아줄 수 있다. 또한 데이터 액세스 기술에 따라서 성능이 향상될 수도 있다.

### 6.6.2 트랜잭션 인터셉터와 트랜잭션 속성
#### TransactionInterceptor
- 스프링에서 제공하는 클래스로 편리하게 트랜잭션 경계설정 어드바이스로 사용할 수 있도록 만들어졌다.
- 트랜잭션 정의를 메소드 이름 패턴을 이용해서 다르게 지정할 수 있는 방법을 추가로 제공한다.
- PlatformTransactionManager와 Properties 타입의 두 가지 프로퍼티를 갖고 있다.
    - Properties는 트랜잭션 속성을 정의한 프로퍼티다. 트랜잭션 속성은 TransactionDefinition의 네 가지 기본 항목에 rollbackOn( )이라는 메소드를 하나 더 갖고 있는 TransactionAttribute 인터페이스로 정의된다.
    - rollbackOn() 메소드는 예외가 발생하면 롤백을 할 것인가를 결정하는 메소드다.
- 스프링이 제공하는 TransactionInterceptor에는 기본적으로 두 가지 종류의 예외 처리 방식이 있다.
    - 런타임 예외 : 트랜잭션은 롤백된다.
    - 체크 예외 : 예외상황으로 해석하지 않고 일종의 비즈니스 로직에 따른, 의미가 있는 리턴 방식의 한 가지로 인식해서 트랜잭션을 커밋한다.
    - 스프링의 기본적인 예외처리 원칙에 따라 비즈니스적인 의미가 있는 예외상황만 체크 예외를 사용하고, 그 외의 모든 복구 불가능한 순수한 예외의 경우는 런타임 예외로 포장돼서 전달하는 방식을 따른다고 가정하기 때문이다.
- TransactionInterceptor의 이러한 예외처리 기본 원칙을 따르지 않는 경우가 있을 수 있다. 그래서 TransactionAttribute는 rollbackOn( )이라는 속성을 둬서 기본원칙과 다른 예외처리가 가능하게 해준다.
- TransactionInterceptor는 이런 TransactionAttribute를 Properties라는 일종의 맵 타입 오브젝트로 전달받는다. 컬렉션을 사용하는 이유는 메소드 패턴에 따라서 각기 다른 트랜잭션 속성을 부여할 수 있게 하기 위해서다.

#### 메소드 이름 패턴을 이용한 트랜잭션 속성 지정
- TransactionInterceptor의 Properties 타입 프로퍼티는 메소드 패턴과 트랜잭션 속성을 키와 값으로 갖는 컬렉션이다.
- 트랜잭션 속성은 아래와 같은 문자열로 정의할 수 있다.
    - PROPAGATION_NAME, ISOLATION_NAME, readOnly, timeout_NNNN, -Exception1,    +Exception2
    - 트랜잭션 전파 항목만 필수이고 나머지는 전부 생략 가능하다. 생략하면 디폴트 속성이 부여된다.
      순서는 바뀌어도 상관없다.
    - readOnly : 읽기 전용 항목. 생략 가능하다.
    - -Exception1 : 체크 예외 중에서 롤백 대상으로 추가할 것을 넣는다. 한 개 이상 등록 가능하다.
    - +Exception1 : 런타임 예외지만 롤백시키지 않을 예외들을 넣는다. 한 개 이상 등록 가능하다.

> readOnly 옵션이 어떤 성능 향상을
readOnly 속성을 통해 트랜잭션을 읽기 전용으로 설정할 수 있다. JPA의 경우, 해당 옵션을 true 로 설정하게 되면 트랜잭션이 커밋되어도 영속성 컨텍스트를 플러시하지 않아서, 플러시할 때 수행되는 엔티티의 스냅샷 비교 로직이 수행되지 않으므로 성능을 향상 시킬 수 있다

### 6.6.3 포인트 컷과 트랜잭션 속성의 적용 전략
#### 트랜잭션 포인트컷 표현식은 타입 패턴이나 빈 이름을 이용한다.
- 트랜잭션용 포인트컷 표현식에는 메소드나 파라미터, 예외에 대한 패턴을 정의하지 않는게 바람직하다.
- 트랜잭션의 경계로 삼을 클래스들이 선정됐다면, 그 클래스들이 모여 있는 패키지를 통째로 선택하거나 클래스 이름에서 일정한 패턴을 찾아서 표현식으로 만들면 된다.
- 가능하면 클래스보다는 인터페이스 타입을 기준으로 타입 패턴을 적용하는 것이 좋다.
- 스프링의 빈 이름을 이용하는 bean() 표현식을 사용하는 방법도 좋다.

#### 공통된 메소드 이름 규칙을 통해 최소한의 트랜잭션 어드바이스와 속성을 정의한다
- 트랜잭션 적용 대상 클래스의 메소드는 일정한 명명 규칙을 따르게 해야 한다.
- 기준이 되는 몇 가지 트랜잭션 속성을 정의하고 그에 따라 적절한 메소드 명명 규칙을 만들어두면 하나의 어드바이스만으로 애플리케이션의 모든 서비스 빈에 트랜잭션 속성을 지정할수있다.
#### 프록시 방식 AOP는 같은 타킷 오브젝트 내의 메소드를 호출할 때는 적용되지 않는다.
- 프록시 방식의 AOP에서는 프록시를 통한 부가기능의 적용은 클라이언트로부터 호출이 일어날 때만 가능하다. (읭?!)
- 자기 자신의 메소드를 호출할 때는 프록시를 통한 부가기능의 적용이 일어나지 않는다.
  ![](https://velog.velcdn.com/images/haron/post/946ee2ab-e321-4e74-be31-ac17a64fe6b6/image.png)

- 위의 그림은 트랜잭션 프록시가 타깃에 적용되어 있는 경우의 메소드 호출 과정을 보여준다.
    - 1, 3 : 트랜잭션 경계설정 부가기능이 부여된다.
    - 2 : 프록시를 거치지 않고 직접 메소드를 호출하기 때문에, 트랜잭션 경계설정 부가기능이 부여되지 않는다.
- 타깃 안에서의 호출에는 프록시가 적용되지 않는 문제를 해결할 수 있는 방법은 두 가지가있다.
    - 스프링 API를 이용해 프록시 오브젝트에 대한 레퍼런스를 가져온 뒤에 같은 오브젝트의 메소드 호출도 프록시를 이용하도록 강제하는 방법
    - AspectJ와 같은 타깃의 바이트코드를 직접 조작하는 방식의 AOP 기술을 적용
### 6.6.4 트랜잭션 속성 적용
#### 트랜잭션 경계설정의 일원화
- 일반적으로 특정 계층의 경계를 트랜잭션 경계와 일치시키는 것이 바람직하다
- 비즈니스 로직을 담고 있는 서비스 계층 오브젝트의 메소드가 트랜잭션 경계를 부여하기에 가장 적절한 대상이다.
- 가능하면 다른 모듈의 DAO에 접근할 때는 서비스 계층을 거치도록 하는 게 바람직하다.
- 서비스 계층에서 다른 모듈의 DAO를 직접 이용할 때 신중을 기해야 한다. 안전하게 사용하려면 다른 모듈의 서비스 계층을 통해 접근하는 방법이 좋다.

#### 트랜잭션 속성 테스트
읽기 전용 트랜잭션에서 데이터를 조작하는 작업을 시도하는 경우 TransientDataAccessResourceException이 발생한다.

## 6.7 애노테이션 트랜잭션 속성과 포인트컷
### 6.7.1 트랜잭션 애노테이션
```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Transactional {

    @AliasFor("transactionManager")
	String value() default "";
	@AliasFor("value")
	String transactionManager() default "";
	Propagation propagation() default Propagation.REQUIRED;
	Isolation isolation() default Isolation.DEFAULT;
	int timeout() default TransactionDefinition.TIMEOUT_DEFAULT;
	boolean readOnly() default false;
	Class<? extends Throwable>[] rollbackFor() default {};
	String[] rollbackForClassName() default {};
	Class<? extends Throwable>[] noRollbackFor() default {};
	String[] noRollbackForClassName() default {};

}
```
#### @Transactional
- 트랜잭션 속성의 모든 항목을 엘리먼트로 지정할 수 있다. 디폴트 값이 설정되어 있으므로 모두 생략이 가능하다.
- @Inherited : 상속을 통해서도 애노테이션 정보를 얻을 수 있게 한다.
- @Transactional은 기본적으로 트랜잭션 속성을 정의하는 것이지만, 동시에 포인트컷의 자동등록에도 사용된다.
    - 이 때 사용되는 포인트컷은 TransactionAttributeSourcePointcut이다.
#### 트랜잭션 속성을 이용하는 포인트컷
- Transactionlnterceptor는 메소드 이름 패턴을 통해 부여되는 일괄적인 트랜잭션 속성 정보 대신 @Transactional 애노테이션의 엘리먼트에서 트랜잭션 속성을 가져오는 AnnotationTransactionAttributeSource를 사용한다.
- 아래 그림은 Transactional 애노태이션을 사용했을 때 어드바이저의 동작 방식을 보여준다.
  ![](https://velog.velcdn.com/images/haron/post/6920ecbc-e7bd-4768-b8dd-bae1d682bc92/image.png)

- @Transactional 방식을 이용하면 포인트컷과 트랜잭션 속성을 애노테이션 하나로 지정할 수 있다. 트랜잭션 속성은 타입 레벨에 일괄적으로 부여할 수도 있지만 메소드 단위로 세분화해서 트랜잭션 속성을 다르게 지정할 수도 있기 때문에 매우 세밀한 트랜잭션 속성 제어가 가능해진다.
#### 대체 정책
메소드의 속성을 확인할 때 타깃 메소드, 타깃 클래스, 선언 메소드, 선언 타입(클래스, 인터메이스)의 순서에 따라서 @Transactional이 적용됐는지 차례로 확인하고, 가장 먼저 발견되는 속성 정보를 사용한다.

@Transactional을 사용하면 대체 정책을 잘 활용해서 애노태이션 자체는 최소한으로 사용하면서도 세밀한 제어가 가능하다.
```java
[1]
public interface Service { 
	[2]
	void method1(); 
	[3]
	void method2();
}

[4]
public class Servicelmpl implements Service {
	[5]
	public void method1() (
	[6]
	public void method2() {
}
```
- [5], [6] : 스프링은 트랜잭션 기능이 부여될 위치인 타깃 오브젝트의 메소드부터 시작해서 @Transactional 애노테이션이 존재하는지 확인한다. 따라서 [5], [6]번이 @Transactional이 위치할 수 있는 첫번째 후보이다.
- [4] : 메소드에서 @Transactional을 발견하지 못하면, 다음은 타깃 클래스를 확인한다.
- [2, 3] : 스프링은 메소드가 선언된 인터페이스로 넘어간다. 인터페이스에서도 먼저 메소드를 확인한다.
- [1] : 인터페이스 타입 [1]의 위치에 애노태이션이 있는지 확인한다.

- @Transactional도 타깃 클래스보다는 인터페이스에 두는 게 바람직하다. 하지만 인터페이스를 사용하는 프록시 방식의 AOP가 아닌 방식으로 트랜잭션을 적용하면 인터페이스에 정의한 @Transactional은 무시되기 때문에 안전하게 타깃 클래스에 @Transactional을 두는 방법을 권장한다.

테스트를 해보니 CGLIB 방식의 AOP를 사용하는 경우, 인터페이스에 @Transactonal을 붙이는 경우 트랜잭션이 적용되는 것을 확인했다.

## 6.8 트랜잭션 지원 테스트
### 6.8.1 선언적 트랜잭션과 트랜잭션 전파 속성
- add() 메소드에 REQUIRED 방식의 트랜잭션 전파 속성을 지정했을 때 트랜잭션이 시작되고 종료되는 경계를 보여준다. add() 메소드도 스스로 트랜잭션 경계를 설정할 수 있지만, 때로는 다른 메소드에서 만들어진 트랜잭션의 경계 안에 포함된다.
  ![](https://velog.velcdn.com/images/haron/post/3dcfb033-addb-441f-99eb-20103e4bd9c3/image.png)


- 트랜잭션을 부여하는 두가지 방법
    - 선언적 트랜잭션 : AOP를 이용해 코드 외부에서 트랜잭션의 기능을 부여해주고 속성을 지정할 수 있게 하는 방법
    - 프로그램에 의한 트랜잭션 : TransactionTemplate이나 개별 데이터 기술의 트랜잭션 API를 사용해 직접 코드 안에서 사용하는 방법
### 6.8.2 트랜잭션 동기화와 테스트
#### 트랜잭션 매니저와 트랜잭션 동기화
- 트랜잭션 추상화 기술의 핵심은 트랜잭션 매니저와 트랜잭션 동기화다.
    - 트랜잭션 매니저 : PlatformTransactionManager 인터페이스를 구현한 트랜잭션 매니저를 통해 구체적인 트랜잭션 기술의 종류에 상관없이 일관된 트랜잭션 제어가 가능했다.
    - 트랜잭션 동기화 : 트랜잭션 동기화 기술이 있었기에 시작된 트랜잭션 정보를 저장소에 보관해뒀다가 DAO에서 공유 할 수 있다.
- 트랜잭션 동기화 기술은 트랜잭션 전파를 위해서도 중요한 역할을 한다. 진행 중인 트랜잭션이 있는지 확인하고 트랜잭션 전파 속성에 따라서 이에 참여할 수 있도록 만들어주는 것도 트랜잭션 동기화 기술 덕분이다.
```java
@Test
public void transactionSync() {
    userService.deleteAll();

    userService.add(users.get(0));
    userService.add(users.get(1));
}
```
- transactionSync() 테스트 메소드가 실행되는 동안에 몇 개의 트랜잭션이 만들어졌을까? UserService의 모든 메소드에는 트랜잭션을 적용했으니 당연히 3개다. 각 메소드가 모두 독립적인 트랜잭션 안에서 실행된다.

#### 트랜잭션 매니저를 이용한 테스트용 트랜잭션 제어
- 그렇다면 이 테스트 메소드에서 만들어지는 세 개의 트랜잭션을 하나로 통합할 수는 없을까?
- 세 개의 메소드 모두 트랜잭션 전파 속성이 REQUIRED이니 이 메소드들이 호출되기전에 트랜잭션이 시작되게만 한다면 가능하다.
- 테스트 메소드에서 UserService의 메소드를 호출하기 전에 트랜잭션을 미리 시작해주면 된다.
    - 트랜잭션의 전파는 트랜잭션 매니저를 통해 트랜잭션 동기화 방식이 적용되기 때문에 가능하다고 했다. 그렇다면 테스트에서 트랜잭션 매니저를 이용해 트랜잭션을 시작시키고 이를 동기화해주면 된다.
```java
@Test
public void transactionSync() {
    DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
    TransactionStatus status = transactionManager.getTransaction(definition);

    userService.deleteAll();

    userService.add(users.get(0));
    userService.add(users.get(1));

    transactionManager.commit(status);
}
```
- 테스트 코드에서 트랜잭션 매니저를 이용해서 트랜잭션을 만들고 그 후에 실행되는 UserService의 메소드들이 같은 트랜잭션에 참여하게 만들 수 있다. 세 개의 메소드 모두 속성이 REQUIRED이므로 이미 시작된 트랜잭션이 있으면 참여하고 새로운 트랜잭션을 만들지 않는다.
#### 트랜잭션 동기화 검증
- 트랜잭션 속성 중에서 읽기전용과 제한시간 등은 처음 트랜잭션이 시작할 때만 적용되고 그 이후에 참여하는 메소드의 속성은 무시된다.
```java
@Test
public void transactionSync() {
    DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
    TransactionStatus status = transactionManager.getTransaction(definition);
    definition.setReadOnly(true);

    userService.deleteAll();

    userService.add(users.get(0));
    userService.add(users.get(1));

    transactionManager.commit(status);
}
```
- 위의 테스트를 실행하면 TransientDataAccessResourceException이 발생한다. 읽기 전용 트랜잭션에서 쓰기를 했기 때문이다.
- 스프링의 트랜잭션 추상화가 제공하는 트랜잭션 동기화 기술과 트랜잭션 전파 속성 덕분에 테스트도 트랙잭션으로 묶을 수 있다.
- JdbcTemplate과 같이 스프링이 제공하는 데이터 액세스 추상화를 적용한 DAO에도 동일한 영향을 미친다. JdbcTemplate은 트랜잭션이 시작된 것이 있으면 그 트랜잭션에 자동으로 참여하고, 없으면 트랜잭션 없이 자동커밋 모드로 JDBC 작업을 수행한다. 개념은 조금 다르지만 JdbcTemplate의 메소드 단위로 마치 트랜잭션 전파 속성이 REQUIRED인것 처럼 동작 한다고 볼 수 있다.
#### 롤백 테스트
- 롤백 테스트는 테스트 내의 모든 DB 작업을 하나의 트랜잭션 안에서 동작하게하고 테스트가 끝나면 무조건 롤백해버리는 테스트를 말한다.
```java
@Test
public void transactionSync() throws InterruptedException {
    DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
    TransactionStatus status = transactionManager.getTransaction(definition);

    try {
        userService.deleteAll();

        userService.add(users.get(0));
        userService.add(users.get(1));
    } finally {
        transactionManager.rollback(status);
    }
}
```
- 롤백 테스트는 DB 작업이 포함된 테스트가 수행돼도 DB에 영향을 주지 않기 때문에 장점이 많다.
    - 테스트용 데이터를 DB에 잘 준비해놓더라도 앞에서 실행된 테스트에서 DB의 데이터를 바꿔버리면 이후에 실행되는 테스트에 영향을 미칠 수 있다.
    - 이런 이유 때문에 롤백 테스트는 매우 유용하다. 롤백 테스트는 테스트를 진행하는 동안에 조작한 데이터를 모두 롤백하고 테스트를 시작하기 전 상태로 만들어주기 때문이다.
      테스트에서 트랜잭션을 제어할 수 있기 때문에 얻을 수 있는 가장 큰 유익이 있다면 바로 롤백 테스트다.

### 6.8.3 테스트를 위한 트랜잭션 애노테이션
- 스프링의 컨텍스트 테스트 프레임워크는 애노테이션을 이용해 테스트를 편리하게 만들 수 있는 여러 가지 기능을 추가하게 해준다.

#### @Transactional
- 테스트에도 @Transactional을 적용할 수 있다. 테스트 클래스 또는 메소드에 @Transactional 애노태이션을 부여해주면 마치 타깃 클래스나 인터페이스에 적용된 것처럼 테스트 메소드에 트랜잭션 경계가 자동으로 설정된다.
- 테스트에서 사용하는 @Transactional은 AOP를 위한 것은 아니다. 단지 컨텍스트 테스트 프레임워크에 의해 트랜잭션을 부여해주는 용도로 쓰일 뿐이다.
#### @Rollback
- 테스트 메소드나 클래스에 사용하는 @Transactional은 애플리케이션의 클래스에 적용할 때와 디폴트 속성은 동일하다. 하지만 중요한 차이점이 있는데, 테스트용 트랜잭션은 테스트가 끝나면 자동으로 롤백된다는 것이다. 테스트에 적용된 @Transactional은 기본적으로 트랜잭션을 강제 롤백시키도록 설정되어 있다.
- 테스트 메소드 안에서 진행되는 작업을 하나의 트랜잭션으로 묶고 싶기는 하지만 강제 롤백을 원하지 않을 수도 있다. 이때는 @Rollback이라는 애노테이션을 이용하면 된다. @Rollback은 롤백 여부를 지정하는 값을 갖고 있다. @Rollback의 기본 값은 true다. 따라서 트랜잭션은 적용되지만 롤백을 원치 않는다면 @Rollback(false)라고 해줘야 한다.
#### @TransactionConfiguration
- @Rollback 애노테이션은 메소드 레벨에만 적용할 수 있다.
- 테스트 클래스의 모든 테스트 메소드에 트랜잭션을 적용하면서 롤백이 되지 않도록 하고 싶다면, 클래스 레벨에 부여할 수 있는 @TransactionConfiguration 애노테이션을 이용하면 편리하다.
- @TransactionConfiguration을 사용하면 롤백에 대한 공통 속성을 지정할 수 있다. 디폴트 롤백 속성은 false로 해두고, 테스트 메소드 중에서 일부만 롤백을 적용하고 싶으면 메소드에 @Rollback을 부여해주면 된다
#### Propagation.NEVER
- @Transactional(propagation = Propagation.NEVER)을 테스트 메소드에 부여하면 트랜잭션을 시작하지 않은 채로 테스트를 진행한다. 물론 테스트 안에서 호출하는 메소드에서 트랜잭션을 사용하는데는 영향을 주지 않는다.
#### 효과적인 DB 테스트
- DB가 사용되는 통합 테스트를 별도의 클래스로 만들어둔다면 기본적으로 클래스 레벨에 @Transactional을 부여해준다. DB가 사용되는 통합 테스트는 가능한 한 롤백 테스트로 만드는 게 좋다.
- 테스트가 기본적으로 롤백 테스트로 되어 있다면 테스트 사이에 서로 영향을 주지 않으므로 독립적이고 자동화된 테스트로 만들기가 매우 편하다.

## 6.9 정리
- 트랜잭션 경계설정 코드를 분리해서 별도의 클래스로 만들고 비즈니스 로직 클래스와 동일한 인터페이스를 구현하면 DI의 확장 기능을 이용해 클라이언트의 변경 없이도 깔끔하게 분리된 트랜잭션 부가기능을 만들 수 있다.
- 트랜잭션처럼 환경과 외부 리소스에 영향을 받는 코드를 분리하면 비즈니스 로직에만 충실한 태스트를 만들 수 있다.
- 목 오브젝트를 활용하면 의존관계 속에 있는 오브젝트도 손쉽게 고립된 테스트로 만들 수 있다.
- DI를 이용한 트랜잭션의 분리는 데코레이터 패턴과 프록시 패턴으로 이해될 수 있다.
- 번거로운 프록시 클래스 작성은 JDK의 다이내믹 프록시를 사용하면 간단하게 만들 수 있다.
- 다이내믹 프록시는 스태틱 팩토리 메소드를 사용하기 때문에 빈으로 등록하기 번거롭다. 따라서 팩토리 빈으로 만들어야 한다. 스프링은 자동 프록시 생성 기술에 대한 추상화 서비스를 제공하는 프록시 팩토리 빈을 제공한다.
- 프록시 팩토리 빈의 설정이 반복되는 문제를 해결하기 위해 자동 프록시 생성기와 포인트컷을 활용할 수 있다. 자동 프록시 생성기는 부가기능이 담긴 어드바이스를 제공히는 프록시를 스프링 컨테이너 초기화 시점에 자동으로 만들어준다.
- 포인트컷은 AspectJ 포인트컷 표현식을 사용해서 작성하면 편리하다.
- AOP는 OOP만으로 모듈화하기 힘든 부가기능을 효과적으로 모듈화하도록 도와주는 기술이다.
- 스프링은 자주 사용되는 AOP 설정과 트랜잭션 속성을 지정하는 데 사용할 수 있는 전용 태스크를 제공한다.
- AOP를 이용해 트랜잭션 속성을 지정하는 방법에는 포인트컷 표현식과 메소드 이름 패턴을 이용하는 방법과 타깃에 직접 부여하는 @Transactional 애노테이션을 사용하는 방법이 있다.
- @Transactional을 이용한 트랜잭션 속성을 테스트에 적용하면 손쉽게 DB를 사용하는 코드의 테스트를 만들 수 있다.


#### References
- https://mangkyu.tistory.com/269
- https://bcp0109.tistory.com/322
- https://gunju-ko.github.io/toby-spring/2018/11/20/AOP.html
- https://gunju-ko.github.io/toby-spring/2018/11/25/AOP2.html
