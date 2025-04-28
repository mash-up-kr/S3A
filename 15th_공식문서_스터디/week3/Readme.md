# Document
- https://docs.spring.io/spring-framework/reference/core/resources.html

<br/>

## Assignment
아래 GPT의 말을 이해하기

```java
Spring은 org.springframework.core.io.Resource 인터페이스를 통해 리소스를 추상화합니다. 
이 인터페이스는 다음과 같은 여러 종류의 실제 자원을 동일하게 다룰 수 있게 해줍니다:

- 클래스패스에 있는 파일
- 파일 시스템 상의 파일
- URL로 접근 가능한 자원 (예: HTTP, FTP 등)
- JAR 파일 내부 자원
```

<br/>

## Code
- Resource의 구현체들
- `DefaultResourceLoader#getResource`
- `ClassPathResource`

<br/>

## Quesion

- BeanDefinitionResource#getInputStream는 옳은 구현일까?
  <img width="834" alt="image" src="https://github.com/user-attachments/assets/a2738532-c350-4ff8-a46a-111a90da27a5" />


- classpath는 정확히 어디를 가르키는 걸까?
    - `classpath*:`
    - `target/classes`
        - `resources/`
    - jar 안의 `BOOT-INF/classes`
 

- `DefaultResourceLoader`의 `getResource(String location)`은 어떻게 `Resource` 구현체를 결정하는가?
  <img width="765" alt="image" src="https://github.com/user-attachments/assets/e107d083-4b25-464d-a486-8bd97f3e8ac3" />


- spring context 초기화 시 protocolresolver load


  <img width="523" alt="image" src="https://github.com/user-attachments/assets/af461cc1-cce1-42be-ac71-dbb4da3bd79d" />

- default 는 base64 protocol resolver


  <img width="674" alt="image" src="https://github.com/user-attachments/assets/963d0412-a9bc-4001-8f5b-2dfd7995a661" />


`base64:`

```kotlin
@Override
	public Resource getResource(String location) {
    Assert.notNull(location, "Location must not be null");
    
    // 사용자가 추가한 커스텀 resource를 사용하기 위해
    for (ProtocolResolver protocolResolver : getProtocolResolvers()) {
			Resource resource = protocolResolver.resolve(location, this);
			if (resource != null) {
				return resource;
			}
		}
    
    // 1. 절대 경로 (슬래시로 시작)
    if (location.startsWith("/")) {
        return getResourceByPath(location); // 기본: ClassPathResource
    }

    // 2. classpath: 접두사
    else if (location.startsWith("classpath:")) {
        return new ClassPathResource(location.substring("classpath:".length()), getClassLoader());
    }

    // 3. URL 형식 (http://, file:// 등)
    else {
        try {
            URL url = new URL(location);
            return new UrlResource(url);
        } catch (MalformedURLException ex) {
            // 4. 그 외 (상대 경로로 간주)
            return getResourceByPath(location);
        }
    }
}
```

- FileSystemResource vs PathResource ?
    - FileSystemResource 이 좀 더 상위일 것 같은데, (path 경로를 지원하니까)
    - 그럼 PathResource 기능은 FileSystemResource에 종속되어 있나?
    - 둘을 따로 분리한 이유는?
    - createRelative() 메서드 동작이 다르대 (메서드 목적: 상대 경로로 리소스 생성), 그럼 어떻게 다른가?
        - 문자열 기반 처리 vs `java.nio.file.Path` 기반 처리

<img width="855" alt="image" src="https://github.com/user-attachments/assets/8e76d380-5c8d-4fb1-ad16-2fd81744d056" />



- 예시로만 봤을 때는 `ResourceLoaderAware` 인터페이스가 `setResourceLoader` 메서드 하나 가지네요.
    - 그럼 `setResourceLoader` 메서드로 Resource Loader를 주입 받아서 어디에 저장하지? (빈등록..?)
    - “리소스 로더를 제공받는 컴포넌트를 식별” 이 말이 이해가 잘 안 간다. 특정 Resource Loader를 가지고 있는 컴포넌트를 확인할 수 있는건가?
    - 식별하려면 식별하는 메서드를 따로 구현해야 하나?
    - `ApplicationContextAware`
    - `ApplicationContextAwareProcessor`

`ApplicationContextAwareProcessor#invokeAwareInterfaces`

<img width="962" alt="image" src="https://github.com/user-attachments/assets/beb6a30b-88ee-4655-a5c7-4ec58947cddd" />


```kotlin
class MyCustomBean : ApplicationContextAware {
  lateinit var applicationContext: ApplicationContext

	override fun setApplicationContext(applicationContext: ApplicationContext){
		this.applicationContext = applicationContext
	}
}
```

<img width="775" alt="image" src="https://github.com/user-attachments/assets/89af4b26-7794-4cfb-8307-cda41755a3d3" />


- 리소스 경로에 접두어(classpath, file 등) 안 붙이면 `ClassPathResource`, `FileSystemResource`,  `ServletContextResource` 셋 중 하나로 로드된다는데 랜덤인가? 기준이 궁금하다.
    - `ClassPathResource`??? - 나
    
    ```json
    <bean id="myBean" class="example.MyBean">
        <property name="template" value="some/resource/path/myTemplate.txt"/>
    </bean>
    ```
    
    - `ClassPathXmlApplicationContext`
      <img width="921" alt="image" src="https://github.com/user-attachments/assets/2d539d02-e070-4f55-b9cd-5012c51506f4" />

        
    - `FileSystemXmlApplicationContext`
 

      <img width="490" alt="image" src="https://github.com/user-attachments/assets/2029430b-8c5e-455a-a9e1-d3c40e5c1536" />

        
    - `AnnotationConfigApplicationContext`
        - generic application context
        
        ```kotlin
        	 * @see #getResource
        	 * @see org.springframework.core.io.DefaultResourceLoader
        	 * @see org.springframework.core.io.FileSystemResourceLoader
        	 * @see org.springframework.core.io.support.ResourcePatternResolver
        	 * @see #getResources
        	 */
        	public void setResourceLoader(ResourceLoader resourceLoader) {
        		this.resourceLoader = resourceLoader;
        	}
        
        ```
        
    - `DefaultResourceLoader`를 쓸 것 같다.
        - 실제로 그렇다. → `classPathResource`
     

- 리소스를 종속성으로 받는게 좀 위험하지 않을까?
    - 리소스란 추상화된 I/O인데, 애플리케이션의 동작이 빌드 시점의 의존적이지 않을 것같은 우려.
        - `hotswap?` → ?????
        - boot 말구 spring framework war 말아서 tomcat 에 올리면 아는..
          <img width="722" alt="image" src="https://github.com/user-attachments/assets/413375fc-dea3-4abe-981d-b7dfe46c61b0" />
        

- ApplicationContext는 왜 ResourcePatternResolver를 extend할까?
    
    ```java
    public interface ApplicationContext extends EnvironmentCapable, ListableBeanFactory, HierarchicalBeanFactory,
    		MessageSource, ApplicationEventPublisher, ResourcePatternResolver {..
    ```
    
    ```java
    /**
    	 * Resolve the given location pattern into {@code Resource} objects.
    	 * <p>Overlapping resource entries that point to the same physical
    	 * resource should be avoided, as far as possible. The result should
    	 * have set semantics.
    	 * @param locationPattern the location pattern to resolve
    	 * @return the corresponding {@code Resource} objects
    	 * @throws IOException in case of I/O errors
    	 */
    	Resource[] getResources(String locationPattern) throws IOException;
    ```
    
    ```kotlin
    applicationContext.getResources("classpath*:META-INF/spring.factories");
    ```
    
    - @남영킴 멀티 모듈 같은 구조에서..?
    - `AbstractApplicationContext`
        - 다 ApplicationContext임
        - `PathMatchingResourcePatternResolver`
      <img width="644" alt="image" src="https://github.com/user-attachments/assets/adb2ccbb-65dd-4518-b007-12990d423c8b" />

    

```kotlin
@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		Assert.notNull(locationPattern, "Location pattern must not be null");
		if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX)) {
			// a class path resource (multiple resources for same name possible)
			String locationPatternWithoutPrefix = locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length());
			// Search the module path first.
			Set<Resource> resources = findAllModulePathResources(locationPatternWithoutPrefix);
			// Search the class path next.
			if (getPathMatcher().isPattern(locationPatternWithoutPrefix)) {
				// a class path resource pattern
				Collections.addAll(resources, findPathMatchingResources(locationPattern));
			}
			else {
				// all class path resources with the given name
				Collections.addAll(resources, findAllClassPathResources(locationPatternWithoutPrefix));
			}
			return resources.toArray(EMPTY_RESOURCE_ARRAY);
		}
		else {
			// Generally only look for a pattern after a prefix here,
			// and on Tomcat only after the "*/" separator for its "war:" protocol.
			int prefixEnd = (locationPattern.startsWith("war:") ? locationPattern.indexOf("*/") + 1 :
					locationPattern.indexOf(':') + 1);
			if (getPathMatcher().isPattern(locationPattern.substring(prefixEnd))) {
				// a file pattern
				return findPathMatchingResources(locationPattern);
			}
			else {
				// a single resource with the given name
				return new Resource[] {getResourceLoader().getResource(locationPattern)};
			}
		}
	}
```

<br/>

# Next Week

- `spring boot actuator`
    - https://docs.spring.io/spring-boot/reference/actuator/index.html
    - https://docs.spring.io/spring-boot/reference/actuator/jmx.html
- `redis`
