# 카프카 운영과 모니터링

다른 애플리케이션에 비해 비교적 안전한 카프카. 하지만 모니터링은 당연히 필요

## 7.1 안정적인 운영을 위한 주키퍼와 카프카 구성

관리자가 단일 장애 지점 등을 제거하고 클러스터를 구성한다면? BEST

### 7.1.1 주키퍼 구성

최근들어 카프카 오픈소스 진영에서 카프카의 코디네이터 역할을 하는 주키퍼의 의존성을 제거하려고 함

주피커는 파티션과 브로커의 메타데이터를 저장하고 컨트롤러 서버를 선출하는 동작을 수행

주키퍼의 역할을 카프카 내부에서 처리하여 운영 효율성을 높일 수 있고, 더 많은 파티션을 처리할 수 있음

> 주키퍼 서버 수량
> 

주키퍼는 쿼럼(과반수) 구성을 기반으로 동작하므로 반드시 홀수로 구성해야함

주키퍼를 구성할 때 최소 수량으로 구성하려면 주키퍼 서버의 수는 3

이렇게 구성된 주키퍼는 과반수인 2를 충족할 수 있는 최대 1대까지의 주피커 장애를 허용

만약 5개로 구성한다면, 과반수인 3을 충족하므로 최대 2대까지의 주키퍼 장애를 허용하므로 높은 안정성을 확보

카프카의 사용량이 높지 않으며 카프카가 매우 중요한 클러스터가 아니라면 주키퍼는 3대로 구성하는 것이 적합

하지만 회사에서 매우 중요하게 사용되거나 사용량이 높다면? 5대가 적합

> 주키퍼 하드웨어
> 

주키퍼는 높은 하드웨어 리소스를 요구하지 않으므로 주키퍼의 물리적인 메모리 크기는 4~8GB, 디스크는 240G ~ 480G SSD 추천

주키퍼에서 필요로 하는 힙 메모리는 일반적으로 1~2GB, 나머지는 OS 영역등에서 사용

주키퍼는 트랜잭션이나 스냅샷 로그들을 로컬 디스크에 저장하는데, 일반적인 SAS(Serial Attached SCSI) 디스크보다는 쓰기 성능이 좋은 SSD 추천

주피커와 카프카 간에 메타데이터 정도만을 주고받으므로 네트워크 카드는 1G 이더넷 추천

> 주키퍼 배치
> 

물리 서버를 배치하는 경우 일반적으로 데이터 센터 내에 랙 마운트를 하게 됨

~~~
'랙 마운트'란, 서버와 같은 전자 장비를 체계적이고 효율적으로 배치하기 위해 특별히 설계된 금속 프레임이나 캐비닛을 의미
이런 랙들은 보통 표준화된 크기와 형태를 가지며, 서버를 수직으로 쌓아 공간을 최대한 효율적으로 사용할 수 있도록 도와줌
~~~

하나의 랙에 모든 주키퍼 서버를 마운트해 배치하는 것은 매우 위험함

각기 다른 랙에 부산 배치하는 방안을 권자으 전원 이중화와 스위치 이중화 장치 등도 고려해야함

AWS에서는 분산 배치를 위해 가용 영역을 2개 또는 3개의 가용 영역에 분산해 구성하는 것을 추천

### 7.1.2 카프카 구성

아래 내용들을 반영한다면 좀 더 안정적인 카프카 클러스터를 구성할 수 있을 것

> 카프카 서버 수량
> 

카프카는 주키퍼와 다르게 쿼럼 방식의 구성이 필요하지 않음
  
홀수일 필요 없음

카프카에서 추천하는 리플리케이션 백터 수인 3으로 토픽을 구성하기 위해 최소 3대의 브로커가 필요

최소 3대가 가장 적합 

> 카프카 하드웨어
> 

카프카의 CPU 사용률이 높은 편, 코어 수가 많은 CPU 추천

최소 32GB 이상의 메모리를 추천

4TB 이상의 병렬적인 디스크 추천

AWS에서 EC2 인스턴스를 이용해 카프카를 설치할 때 사용되는 EBS(Elastic Block Store) 안정적

네트워크 대역폭이 크므로 10G 이더넷 카드 추천

> 카프카 배치
> 

주키퍼와 마찬가지로 분산배치해야함

## 7.2 모니터링 시스템 구성

모니터링의 대표적인 방법인 애플리케이션 로그 분석과 JMX를 이용해 브로커들의 메트릭 정보를 확인하는 방법 사용

### 7.2.1 애플리케이션으로서 카프카의 로그 관리와 분석

카프카는 카프카 애플리케이션에서 발생하는 모든 로그를 브로커의 로컬 디스크에 기록

카프카는 자바 기반의 로깅 유틸리티인 아파치 log4j를 사용

| 로그 레벨 |                      설명                       |
|:-----:|:---------------------------------------------:|
| TRACE |              DEBUG보다 상세한 로그를 기록               |
| DEBUG |  내부 애플리케이션 상황에 대한 로그를 기록(Info 보다 상세한 로그 기록)   |
| INFO  |        로그 레벨의 기본 값. 일반적인 정보 수준의 로그를 기록        |
| WARN  |       INFO 로그 레벨보다 높은 개념, 경고 수준의 로그를 기록       |
| ERROR |      경고 수준을 넘어 런타임 에러나 예상하지 못한 에러 로그를 기록      |
| FATAL | 로그 레벨 중 최종 단계, 심각한 오류로 인한 애플리케이션 중지 등의 로그를 기록 |

````shell
cat /usr/local/kafka/config/log4j.properties
````

#### 출력 결과

```yaml
# .. 생략..
log4j.logger.kafka=INFO
log4j.logger.org.apache.kafka=INFO
```

|       로그 파일 이름       |                          설명                          |
|:--------------------:|:----------------------------------------------------:|
|      server.log      | 브로커 설정 정보ㅘ 정보성 로그 등을 기록. 브로커를 재시작하는 경우 브로커의 옵션정보가 기록 |
|   state-change.log   |                  컨트롤러로부터 받은 정보를 기록                   |
|  kafka-request.log   |                  클라이언트로부터 받은 정보를 기록                  |
|   log.cleaner.log    |                    로그 컴팩션 동작들을 기록                    |
|    controller.log    |                    컨트롤러 관련 정보를 기록                    |
| kafka-authorizer.log |                    인증과 관련된 정보를 기록                    |

### 7.2.2 JMX를 이용한 카프카 메트릭 모니터링 

JMX는 자바로 만든 모니터링을 위한 도구를 제공하는 자바 API, MBean 이라는 객체로 표현됨

JMX를 이용해 카프카의 주요 메트릭들을 그래프와 같은 형태로 한눈에 확인

브로커에 JMX를 오픈한 다음 JMX에서 제공하는 메트릭 정보를 관리자가 GUI 형태로 볼 수 있도록 구성

최근에 많이 쓰이는 프로메테우스, 익스포터를 이용해 JMX 모니터링 시스템 구성으로 설명

> 카프카 JMX 설정법
> 

여러 방법 중 systemd의 환경 변수 옵션을 추가하는 방법으로 진행

````shell
cat /usr/local/kafka/config/jmx
````

#### 출력
~~~
JMX_PORT=9999
~~~

9999포트 확인해보면 리스닝 상태

> 프로메테우스 설치
>

````shell
# 도커 설치 및 실행
sudo amazon-linux-extras install -y docker
sudo docker version

sudo service docker start
sudo usermod -a -G docker ec2-user

sudo yum install -y git
sudo chkconfig docker on
sudo reboot
````

```shell
sudo systemctl status docker
sudo mkdir -p /etc/prometheus
git clone https://github.com/onlybooks/kafka2

# 프로메테우스 설치 및 실행
sudo cp kafka2/chapter7/prometheus.yml /etc/prometheus/ 
sudo docker run -d --network host -p 9090:9090 -v /etc/prometheus/prometheus.yml /etc/prometheus/prometheus.yml --name prometheus prom/prometheus

# 프로메테우스 상태확인
sudo docker ps
```

> 그라파나 설치
> 

프로메테우스는 성능이나 매트릭 등을 대시보드 형태로 보기 힘듦

````shell
# 그라파타 설치
sudo docker run -d --network host -p 3000:3000 --name grafana grafana/grafana:7.3.7
````

> 익스포터 설치
> 

프로메테우스 모니터링 방식은 푸시가 아닌 풀 방식

모니터링하고자 하는 대상 서버에 자신의 매트릭 정보를 보여줄 수 있는 익스포터를 설치해야함

다양한 애플리케이션에서 수집되는 메트릭들을 프로메테우스가 인식할 수 있는 형태로 나타내는 에이전트

독립적인 HTTP 서버로 설정하는 방식을 설명

1. JMX 익스포터를 저장할 디렉토리를 생성
2. 깃허브에서 책의 실습 과정들이 포함된 kafka2 폴더를 다운로드
3. 깃 클론 등

````shell
sudo mkdir -p /usr/local/jmx
sudo yum -y install git
git clone https://github.com/onlybooks/kafka2

# 익스포터 실행하기 위한 파일 복사
sudo cp kafka2/chapter7/jmx_prometheus_httpserver-0.13.1-SNAPSHOT-jar-with-dependencies.jar /usr/local/jmx/
sudo cp kafka2/chapter7/jmx_prometheus_httpserver.yml /usr/local/imx/
````

```yaml
# .. 생략..
hostPort: 127.0.0.1:9999 # JMX가 실행되고 있는 IP와 포트 정보를 입력
ssl: false               # SSL 사용 유무
rules:
  - pattern: ".*"        # 순서대로 적용할 규칙의 리스트를 나타냄. 일치하지 않는 속성들은 수집되지 않음
```

```yaml
# jmx-exporter.service 예제 파일
[Unit]
Description=JMX Exporter for Kafka
After=kafka-server.target
  
[Service]
Type=simple
Restart=always
ExecStart=/usr/bin/java-jar /usr/local/jmx/jmx_prometheus_httpserver-0.13.1-

# MX 익스포터를 실행하는 명령어. 이미 프로메테우스에서 수집 대상 서버의 포트로 7071로 설정되어 있음
SNAPSHOT-jar-with-dependencies.jar 7071 /usr/local/jmx/jmx_prometheus_httpserver.yml

[Install]
WantedBy=multi-user.target
```

```shell
sudo cp kafka2/chapter7/jmx-exporter.service /etc/systemd/system
sudo systemctl daemon-reload
```

실행 후 출력에 Active: active (running) 정상 실행

JMX 익스포터는 브로커 한 대에만 설치하는 것이 아닌 카프카 클러스터 내 모든 브로커에 설치해야함

익스포터가 설치되고 즉시 모니터링이 되는 것이 아닌 프로메테우스의 환경 설정 파일에서 수집 대상 서버들을 명시한 이후 프로메테우스에 메트릭들이 수집, 저장됨

프로메테우스의 환경 설정 파일에 대상 서버들을 등록하기 전에 브로커 서버의 하드웨어 리소스 모니터링을 위해 노드 익스포터 설치

노드 익스포터는 서버에서 제공하는 CPU, 메모리, 디스크, 네트워크 등의 리소스 사용량을 수집하는 역할

```shell
# wget으로 모든 브로커에 다운로드
wget https://github.com/prometheus/node_exporter/releases/download/v1.0.1/node_exporter-1.0.1.linux-386.tar.gz
```

압축되어 있으므로 압축을 푼 후 심볼릭 링크르 걸어줌

```shell
sudo tar zxf node_exporter-1.0.1.linux-386.tar.gz -c/usr/local/
sudo In -s /usr/local/node_exporter-1.0.1.linux-386/usr/local/node_exporter
```

노드 익스포터도 systemd를 사용

```shell
sudo cp kafka2/chapter7/node-exporter.service /etc/systemd/system
sudo systemctl daemon-reload
```

리눅스 시스템에서 systemd의 변경이 생긴 후에는 반드시 systemctl daemon-reload 명령어를 입력해야함

```shell
sudo systemctl start node-exporter
sudo systemctl status node-exporter
```

실행 후 출력에 Active: active (running) 정상 실행

노드 익스포터 또한 클러스터 내 모든 브로커에 설치

프로메테우스에서 대상 서버들의 모니터링을 위한 설정을 적용해야함

```yaml
# prometheus config
global: # 프로메테우스의 전반적인 설정과 관련된 부분
  scrape_interval: 55
  evaluation_interval: 5s

scrape_configs:
  - job_name: 'peter-jmx-kafka' # 프로메테우스에서 메트릭을 수집할 대상을 설정하는 부분
    static_configs:
    - targets:
      - peter-kafka01.foo.bar:7071
      - peter-kafka02.foo.bar:7071
      - peter-kafka03.foo.bar:7071
      - 
  - job_name: 'peter-kafka-nodes' # 프로메테우스에서 메트릭을 수집할 대상을 설정하는 부분
    static_configs:
    - targets:
      - peter-kafka01.foo.bar:9100  
      - peter-kafka02.foo.bar:9100
      - peter-kafka03.foo.bar:9100
```

### 그라파나 대시보드 생성

이 책에서 그라파나가 설치된 호수트 주소: peter-ansible01.foo.bar

접속 url: http://peter-ansible01.foo.bar:3000

그라파나의 초기 암호는 admin/admin

#### 그라파나 로그인 화면

![1_그라파나로그인화면.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch7/1_그라파나로그인화면.png)

해야할 일 
1. 로그인 성공 후 새로운 암호로 변경
2. 그라파나에서 가져올 데이터 소스를 추가

#### 최초 로그인 이후 화면

![2_최초로그인이후화면.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch7/2_최초로그인이후화면.png)

#### *Add your first data source* 클릭

#### 데이터 소스 추가 화면

![3_데이터소스추가화면.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch7/3_데이터소스추가화면.png)

SQL, 클라우드 등 여러 종류의 데이터 소스를 추가할 수 있음

#### *Prometheys*  선택

#### 프로메테우스 설정 정보 입력

![4_프로메테우스설정정보입력.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch7/4_프로메테우스설정정보입력.png)

URL에 http://peter-ansible01.foo.bar:9090 입력

#### 저장 및 테스트

![5_저장및테스트.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch7/5_저장및테스트.png)

프로메테우스 정보를 입력 후 *Save & Test* 클릭

성공하면 Data source is working 메시지 표시

#### 대시보드 추가 메뉴

![6_대시보드추가메뉴.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch7/6_대시보드추가메뉴.png)

`+` 를 누르면 하위 메뉴가 펼처짐

그라파나의 장점 중 하나로 누군가 자신이 잘 만든 대시보드를 공유했다면 그 대시보드를 그대로 가져올 수 있음

관리자가 직접 한 땀 한 땀 그래프를 만들지 않아도 됨. import를 사용하여 다른 사람이 만들어놓은 메트릭 대시보드를 사용

#### JSONImport

![7_JSONImport.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch7/7_JSONImport.png)

대시보드 ID를 입력하거나, JSON으로 복붙

`Import via grafana.com` 메뉴에서 `1860`로 입력한 다음 우측의 Load 버튼 클릭

#### 대시보드 추가 단계 마지막 화면

![8_대시보드추가단계마지막화면.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch7/8_대시보드추가단계마지막화면.png)

여기서 대시보드 이름을 변경할 수 있음

맨 마지막 항목의 Prometheus 드롭다운 메뉴에서 Prometheus 선택 후 Import 버튼을 클릭하여 마무리

노드 익스포터 대시보드 추가가 완료되면 그다음으로는 JMX 익스포터 대시보드 추가

JMX 익스포터 추가를 위한 JSON 파일은 /home.ec2-user/kafka2/chapter7 경로의 kafka_metrics.json

#### 메트릭 대시보드 임포트 화면

![9_메트릭대시보드임포트화면.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch7/9_메트릭대시보드임포트화면.png)

화면 상단의 Upload JSON file 버튼 클릭 후 파일을 업로드하거나, JSON으로 복붙

#### 노드 익스포터 대시보드

![10_노드익스포터대시보드.png.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch7/10_노드익스포터대시보드.png.png)

#### JMX 익스포터 대시보드

![11_JMX.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch7/11_JMX.png)

첫 번째 카테고리에는 전체 브로커 수, 토픽 수, 파티션 수, 현재 실시간 초당인입 메시지 수 및 응답 시간 등 주요 항목들이 배치되어 카프카 클러스터의 전반적인 내용을 확인할 수 있음

두 번째 카테고리에는 브로커의 세부 정보를 확인할 수 있음

카프카 클러스터로 유입되는 전체 초당 메시지 건수, 바이트 수, 요청 비율 등을 비록해 브로커별로 리더 수나 파티션 수가 고르게 분포됐는지 등의 세부사항을 그래프로 확인 가능

세, 네, 다섯 번째 카테고리는 응답 시간과 관련된 그래프를 볼 수 있는 구역

각각 프로듀서, 컨슈머, 팔로워에 관해 전체적인 지연시간이 어디서 발생하는지 확인할 수 있음

> JMX 모니터링 지표
>

![12_표1.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch7/12_표1.png)
![13_표2.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch7/13_표2.png)
![14_표3.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch7/14_표3.png)

> JMX 공식 가이드 문서
> https://docs.confluent.io/current/kafka/monitoring.html
> 

### 7.2.3 카프카 익스포터

JMX 메트릭, 브로커의 리소스 모니터링도 중요하지만 `컨슈머의 LAG`을 모니터링하는 것이 가장 중요

프로메테우스와 그라파나를 조합한 모니터링 방식

> 카프카 익스포터 설치
> 
1. 카프카 익스포터 다운로드
2. 카프카 익스포터 실행
3. 프로메테우스 환경 설정 파일에서 카프카 익스포터 추가
4. 그라파나에서 대식보드 추가

설치 과정은 JMC 익스포터나 노드 익스포터와 동일

카프카 익스포터는 9308 포트를 사용

```yaml
- job_name: 'peter-kafka-exporter' 
  static_configs:
    - targets:
      - peter-kafka01.foo.bar:9308
      - peter-kafka02.foo.bar:9308
      - peter-kafka03.foo.bar:9308
```

#### 재시작

```shell
sudo docker restart prometheus
```

#### 카프카 익스포터 대시보드 화면

![15_카프카익스포터대시보드화면.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch7/15_카프카익스포터대시보드화면.png)

Message in per second 그래프와 Message in per minute 그래프는 초와 분 단위로 메시지 유입을 나타냄

Lag by Consumer Group 그래프는 컨슈머 그룹의 LAG 상태를 나타냄

Message consume per minute 그래프는 분 단위로 얼마나 컨슘되는지 나타냄

Partitions per topic 그래프는 토픽별 파티션 수를 막대 그래프로 나타냄