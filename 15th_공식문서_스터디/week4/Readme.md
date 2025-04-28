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

    ```
    ContextRegistry.getInstance().registerThreadLoocalAccessor(...);
    ```

    <img width="772" alt="image" src="https://github.com/user-attachments/assets/49a6c9ee-3ec2-47a9-becd-a55bf1a40f8e" />
    
    reactor.core.publisher.AutomaticContextPropagationTest

- 어떻게 서비스에서 필요한 metric을 받아올까? ex, thread, heap
  ```
  private <T> T getFormattedThreadDump(Function<ThreadInfo[], T> formatter) {
		return formatter.apply(ManagementFactory.getThreadMXBean().dumpAllThreads(true, true));
	}
  ```
  com.sun.management.HotSpotDiagnosticMXBean

  <br/>
  
  ```
  protected static class HotSpotDiagnosticMXBeanHeapDumper implements HeapDumper {
  
  		private final Object diagnosticMXBean;
  
  		private final Method dumpHeapMethod;
  
  		@SuppressWarnings("unchecked")
  		protected HotSpotDiagnosticMXBeanHeapDumper() {
  			try {
  				Class<?> diagnosticMXBeanClass = ClassUtils
  					.resolveClassName("com.sun.management.HotSpotDiagnosticMXBean", null);
  				this.diagnosticMXBean = ManagementFactory
  					.getPlatformMXBean((Class<PlatformManagedObject>) diagnosticMXBeanClass);
  				this.dumpHeapMethod = ReflectionUtils.findMethod(diagnosticMXBeanClass, "dumpHeap", String.class,
  						Boolean.TYPE);
  			}
  			catch (Throwable ex) {
  				throw new HeapDumperUnavailableException("Unable to locate HotSpotDiagnosticMXBean", ex);
  			}
  		}
  
  		@Override
  		public File dumpHeap(Boolean live) throws IOException {
  			File file = createTempFile();
  			ReflectionUtils.invokeMethod(this.dumpHeapMethod, this.diagnosticMXBean, file.getAbsolutePath(),
  					(live != null) ? live : true);
  			return file;
  		}
  
  		private File createTempFile() throws IOException {
  			String date = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm").format(LocalDateTime.now());
  			File file = File.createTempFile("heap-" + date, ".hprof");
  			file.delete();
  			return file;
  		}
  
  }
  ```

  - 질문은 아니지만 갑자기 생각난 썰
    - 톰캣 스레드 고갈났는데 actuator health에선 감지가 안됐음
      - 포트가 다르면 새로 thread pool 생성한다? → ???????
      - port 를 같게, prometheus 붙여서 해결함
     
- @ConditionalOnMissingBean 처음 봤다.
  - 스프링 빈이 등록되어 있지 않을 때만 해당 빈을 생성하도록 한다.
  - 왜 태어났나? → custom 기능을 위해서라고 합니다~
    ```
    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource defaultDataSource() {
      return new HikariDataSource();
    }
    ```
    - 사용자가 직접 `DataSource` 빈 만들었음 → 위에 빈 등록 안 함
    - 사용자가 만든 `DataSource` 빈 따로 없음 → 위에 빈 등록 함
   
- HeapDumpWebEndpoint.class에서 Lock은 초기화하는데, HeapDumper는 lazy 호출인 이유
  - HeapDumper가 lazy 호출인 것을 알 수 있는 부분 → dumpHeap 메서드
    - 최신 정보를 받아올 수 있음
    - 리소스 아끼기 위해서라고 하네요?
   

  - 그럼 왜 Lock은 미리 초기화?
    - 왜 미리 초기화를 안하지?
    - 공유 자원을 보호(ex. 동시성 이슈 예방)해야 하니까 불변성 유지해야 하긴 함.
      - 미리 생성해두는 것이 안전한 패턴
     
    - ReentrantLock은 가볍고 생성 비용이 낮아서 미리 만들어둬도 괜찮다고 하네요?
      - 생성자에서 암것도 안하는듯?
      - 필드도 거이 없음 (부모 AbstractQueuedSynchronizer는 많음, 7개 +1개)
     
  - HeapDump를 활성화하면 서버의 메모리 사용량이 급격히 증가할 수 있을까? 만약 그렇다면 어떤 방식으로 관리해야 할까?
    - https://d2.naver.com/helloworld/1326256
    - LinkedHashMap
      - 동시성 문제가 생겨도 괜찮은데?? → 내가 생각한 범위보다 더 넓다
      - 걍 동시성 자료구조 쓰지말고 LRU 구현해도 되는 거 아닌가?
     
<br/>

# Feedback
- 유익하다
- 편하지 않다 (=스트레스가 있다)
- 공부할 게 많다

<br/>

## Next Week
Redis
