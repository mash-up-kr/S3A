# Document
- https://docs.spring.io/spring-boot/reference/actuator/index.html
- (Optional) [micrometer](https://docs.micrometer.io/micrometer/reference/observation)

<br/>

## Code
- [Jvm metric](https://github.com/spring-projects/spring-boot/blob/67bae524b27ec932b05ecf895a2ee6d983f62a38/spring-boot-project/spring-boot-actuator-autoconfigure/src/main/java/org/springframework/boot/actuate/autoconfigure/metrics/JvmMetricsAutoConfiguration.java#L45-L45)
- [Jdbc metric](https://github.com/spring-projects/spring-boot/blob/67bae524b27ec932b05ecf895a2ee6d983f62a38/spring-boot-project/spring-boot-actuator-autoconfigure/src/main/java/org/springframework/boot/actuate/autoconfigure/metrics/jdbc/DataSourcePoolMetricsAutoConfiguration.java#L61-L61)
- [Redis metric](https://github.com/spring-projects/spring-boot/blob/67bae524b27ec932b05ecf895a2ee6d983f62a38/spring-boot-project/spring-boot-actuator-autoconfigure/src/main/java/org/springframework/boot/actuate/autoconfigure/metrics/redis/LettuceMetricsAutoConfiguration.java#L45-L45)
- [Kafka metric](https://github.com/spring-projects/spring-boot/blob/67bae524b27ec932b05ecf895a2ee6d983f62a38/spring-boot-project/spring-boot-actuator-autoconfigure/src/main/java/org/springframework/boot/actuate/autoconfigure/metrics/KafkaMetricsAutoConfiguration.java#L52-L52)
- [Thread dump](https://github.com/spring-projects/spring-boot/blob/67bae524b27ec932b05ecf895a2ee6d983f62a38/spring-boot-project/spring-boot-actuator/src/main/java/org/springframework/boot/actuate/management/ThreadDumpEndpoint.java#L37-L37)
- [Heap dump](https://github.com/spring-projects/spring-boot/blob/67bae524b27ec932b05ecf895a2ee6d983f62a38/spring-boot-project/spring-boot-actuator/src/main/java/org/springframework/boot/actuate/management/HeapDumpWebEndpoint.java#L60-L60)
- [Hikari cp](https://github.com/spring-projects/spring-boot/blob/67bae524b27ec932b05ecf895a2ee6d983f62a38/spring-boot-project/spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure/jdbc/metadata/DataSourcePoolMetadataProvidersConfiguration.java#L73-L73)

<br/>

## Reference

> Observability is the ability to observe the internal state of a running system from the outside.
It consists of the three pillars: logging, metrics and traces.
> 

observability.adoc

> Micrometer provides a simple facade over the instrumentation clients for the most popular observability system, allowing you to instrument your JVM-based application code without vendor lock-in
> 

https://docs.micrometer.io/micrometer/reference/overview.html

- 사용자에게는 `@Endpoint` 개념으로 노출
- `MeterBinder` 구현체를 만들고 `MeterRegistry` 에 등록하는 방식으로 메트릭 수집


<br/>

## Question
- heapdump, threaddump는 내부 상태(스레드, 힙 객체, 보안 토큰 등)를 그대로 노출할 수 있는데, 이를 public actuator에 열어둬도 괜찮을까?
  - 일반적으로 actuator port는 ip 기반 권한 제어를 하는 듯
  - 내부에서만 접근할 수 있게 관리 Port를 따로 두기
    - 내부망으로
  - ACL 에서 권한 제어 적용 - admin 인증
 
- ThreadDump 에서는 왜 lockedMonitors , lockedSynchronizers 를 모두 true 로 얻어오는 단일 메서드만 제공하는가?
  - code
    ```
    private <T> T getFormattedThreadDump(Function<ThreadInfo[], T> formatter) {
      return formatter.apply(ManagementFactory.getThreadMXBean().dumpAllThreads(true, true));
    }

    private static native ThreadInfo[] dumpThreads0(long[] ids, boolean lockedMonitors, boolean lockedSynchronizers, int maxDepth);
    ```

- lockedMonitors , lockedSynchronizers
  - 둘다 true 로 해야 스레드 문제를 진단할 수 있음
  - 비용이 크지도 않고, 복잡성이 올라가므로 둘다 true 로 기본 제공 한다고 함.
 
  ```
   MonitorInfo[] lockedMonitors;
  int numSyncs;
  
  if (numMonitors == 0) {
    lockedMonitors = EMPTY_MONITORS;
  } else {
    lockedMonitors = new MonitorInfo[numMonitors];

    for(numSyncs = 0; numSyncs < numMonitors; ++numSyncs) {
      Object lock = monitors[numSyncs];
      String className = lock.getClass().getName();
      int identityHashCode = System.identityHashCode(lock);
      int depth = stackDepths[numSyncs];
      StackTraceElement ste = depth >= 0 ? stackTrace[depth] : null;
      lockedMonitors[numSyncs] = new MonitorInfo(className, identityHashCode, depth, ste);
    }
  }

  numSyncs = synchronizers == null ? 0 : synchronizers.length;
  LockInfo[] lockedSynchronizers;
  if (numSyncs == 0) {
    lockedSynchronizers = EMPTY_SYNCS;
  } else {
    lockedSynchronizers = new LockInfo[numSyncs];

    for(int i = 0; i < numSyncs; ++i) {
      Object lock = synchronizers[i];
      String className = lock.getClass().getName();
      int identityHashCode = System.identityHashCode(lock);
      lockedSynchronizers[i] = new LockInfo(className, identityHashCode);
    }
  }
  ```

- WebClient 사용할 때 traceId 전파 방법
  - MDC → MDC는 Mapped Diagnostic Context의 약자로, 로깅 프레임워크에서 각 스레드별로 컨텍스트 정보를 저장하고 활용하는 기능입니다
  - spring.reactor.context-propagation = auto
    - Hooks.enable..Context()
    - https://docs.micrometer.io/micrometer/reference/observation/instrumenting.html#instrumentation_of_reactive_libraries_before_reactor_3_5_3
   
  - ThreadLocalAccessor 구현체 구현
    <img width="772" alt="image" src="https://github.com/user-attachments/assets/2297e099-81ee-487d-b4c6-5337a6cf1919" />

    ```
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Hooks.class)
    @EnableConfigurationProperties(ReactorProperties.class)
    public class ReactorAutoConfiguration {
    
    	ReactorAutoConfiguration(ReactorProperties properties) {
    		if (properties.getContextPropagation() == ReactorProperties.ContextPropagationMode.AUTO) {
    			Hooks.enableAutomaticContextPropagation();
    		}
    	}
    }
    ```

    ```
    public interface ThreadLocalAccessor<V> {
        Object key();
    
        @Nullable
        V getValue();
    
        void setValue(V var1);
    
        default void setValue() {
            this.reset();
        }
    
        /** @deprecated */
        @Deprecated
        default void reset() {
            throw new IllegalStateException(this.getClass().getName() + "#reset() should not be called. Please implement #setValue() method when removing the #reset() implementation.");
        }
    
        default void restore(V previousValue) {
            this.setValue(previousValue);
        }
    
        default void restore() {
            this.setValue();
        }
    }
    ```
