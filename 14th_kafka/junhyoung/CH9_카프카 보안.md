# 카프카 보안

카프카는 보안이 자동으로 설정되지 않기 때문에, 직접 보안을 적용해야함

## 9.1 카프카 보안의 세 가지 요소

### 카프카 보안의 3요소 

![1_카프카보안의3요소.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch9/1_카프카보안의3요소.png)

세 가지 보안 요소가 존재

**암호화**: 인증과 권한설정을 한 후 누군가 악의적으로 패킷을 가로채더라도 암호화하여 데이터를 읽을 수 없게 함. https 접근과 같은 원리

**인증**: 확인된 클라이언트만 접근 가능하도록 설정. 웹에서 아이디/비밀번호를 입력해 로그인 성공한 사람만 받는 것과 비슷한 원리

**권한**: 특정 토픽 등 영역을 지정해 사용자별로 접근을 제어하는 것.

세 가지 보안 요소를 적용하기 위해 필요한 기능은 뭐가 있을까? 

### 9.1.1 암호화. SSL(Secure Socket Layer)

가로침을 당해도 읽지 못하도록 적용하는 암호화

서버와 서버 사이, 서버와 클라이언트 사이에서 통신 보안을 적용하기 위한 표준 암호 규약

SSL은 인증기관, CA(Certificate Authority)으로부터 발급받은 후 인증서를 이용한 공개 키, 비공개 키 방식으로 서버와 클라이언트가 암호화 복호화를 하며 통신함

SSL은 보안을 강화하고 효율적 고성능을 얻기 위해 대칭 키와 비대칭 키 두 가지 방식 모두 혼용하는 방식을 사용함

### 9.1.2 인증. SASL(Simple Authentication and Security Layer)

인터넷 프로토콜에서 인증과 데이터 보안을 위한 프레임워크로, 카프카에서도 사용

애플리케이션 프로토콜에서 인증 메커니즘을 분리함으로써 모든 애플리케이션에서 사용할 수 있음

SASL 메커니즘으로는 `SCRAM`, `GSSAPI`, `OPLAIN`, `AUTHBEARER`, `DIGEST-MD5` 등이 있음

카프카는 네 가지 SASL 메커니즘을 지원

`SASL/GSSAPI`: 카프카 0.9 버전부터 지원됐음. 커버로스 인증방식. 

~~~
커버로스란?
회사 내부에 별도 커버로스 서버가 있는 환경이라면 커버로스 인증 방식을 사용하는 것이 좋음.
본 인증 방식을 적용할 때는 렐름이라는 설정이 필요한데, 이때는 되도록 하나의 렐름으로 모든 애플리케이션을 적용하는 방법이 좋음
크로스 렐름 설정으로 인해 클라이언트들의 재인증, 인증 실패 등이 종종 일어남
~~~

`SASL/PLAIN`: 카프카 0.10.0 버전부터 지원됐음. PLAIN은 아이디와 비밀번호를 텍스트 형태로 사용하는 방법으로 운영 환경보다는 개발 환경에서 테스트 등의 목적으로 사용됨

`SASL/SCRAM-SHA-256, SASL/SCRAM-SHA-512`: 0.10.2 버전부터 지원됨. 후추를 뿌려 해시된 내용을 추가. 솔티드 챌린지 응답 인증 매커니즘의 약어.
인증 정보를 주키퍼에 저장해 사용하며 토큰 방식도 지원해 별도의 커버로스 서버가 구성되어있지 않을 때 가장 유용

`SASL/OUTHBEARER`: 카프카 2.0 버전부터 지원됐음. 매우 한정적이라 개발 환경 정도에만 적용 가능한 수준

### 9.1.3 권한. ACL(Access Control List)

접근제어리스트로 규칙 기반의 리스트를 만들어 접근을 제어함

### 권한 분리가 필요한 사례

![2_권한분리가필요한사례.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch9/2_권한분리가필요한사례.png)

휴먼에러로 인한 사례. B 서비스는 B 토픽으로 가야하지만 A 토픽으로 갔을 때 예기치 못한 오류가 생길 수 있음

이럴 때 각 사용자별로 올바른 권한을 할당한다면 이러한 이슈들은 발생하지 않음

카프카는 이런 권한 설정을 위해 ACL 기능을 제공함. 간단히 CLI로 ACL을 추가할 수 있으며 모든 ACL 규칙은 주키퍼에 저장됨

ACL은 리소스 타입별로 구체적인 설정이 가능함

리소스 타입은 크게 `토픽`, `그룹`, `클러스터`, `트랜잭셔널 ID`, `위임` 토큰으로 나뉨

가장 많이 사용되는 리소스 타입인 토픽과 그룹에 대해 9.4.2에서 다룸

## 9.2 SSL을 이용한 카프카 암호화

일반적으로, 자바 기반 애플리케이션에서는 키스토어라 불리는 인터페이스를 통해 공개 키, 비공개 키, 인증서를 추상화해 제공함

카프카 또한 자바 기반 애플리케이션이기 때문에 Keytool이라는 명령어를 이용해 카프카에 SSL 적용 작업을 진행함

### 카프카 SSL 적용 개요. 카프카 단독 서버모드가 아닌 클러스터 기준

![3_카프카SSL적용개요.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch9/3_카프카SSL적용개요.png)

### 9.2.1 브로커 키스토어 생성

### 키스토어와 트러스트 스토어의 관계도
![4_키스토어와트러스트스터어의관계도.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch9/4_키스토어와트러스트스터어의관계도.png)

`키스토어`와 `트러스트토어`라는 용어는 클라이언트와 서버 사이에서 자바 애플리케이션을 이용해 SSL 연결을 할 때 사용함

키스토어와 트러스트토어는 모두 keytool을 이용해 관리되고, 각 스토어별로 저장되는 내용은 조금씩 다름

키스토어(서버 측)
일반적으로 서버 측에 비공개키와 인증서를 저장하여 자격 증명을 제공, 그 외에 프라이빗하고 민감한 정보를 저장함

트러스트스토어(클라이언트 측)
클라이언트 측에서 서버가 제공하는 인증서를 검증하기 위한 공개 키와 SSL 연결에서 유효성을 검사하는 서명된 인증서를 저장하고 민감한 정보는 저장하지 않음

SSL 적용에 필요한 파일들을 생성하기 전 모두 한 곳에 모아두기 위해 ssl 디렉토리 생성

키스토어와 트러스트스토어 생성시 비밀번호 입력하는 과정을 최소화하기 위해 환경변수 등록

```shell
sudo mkdir -p /us/local/kafka/ssl
cd /us/local/kafka/ssl/ 
export SSLPASS=peterpass
```

#### 키스토어 생성

```shell
sudo keytool -keystore kafka.server.keystore.jks -alias 
localhost -keyalg RSA-validity 365 -genkey -storepass SSSLPASS -keypass $SSLPASS -dname
"CN-peter-kafka01.foo.bar" -storetype pkcs12
```

#### keytool 상세 옵션

| 옵션 이름  | 설명              |
|------------|-------------------|
| keystore   | 키스토어 이름     |
| alias      | 별칭              |
| keyalg     | 키 알고리즘       |
| genkey     | 키 생성           |
| validity   | 유효 일자         |
| storepass  | 저장소 비밀번호   |
| keypass    | 키 비밀번호       |
| dname      | 식별 이름         |
| storetype  | 저장 타입         |

`-dname`의 값을 가장 신경써야함. 현재 접속한 호스트네임으로 설정해야함

#### 키스토어 내용 확인

```shell
keytool -list -v -keystore kafka.server.keystore.jks
```

비밀번호 정상 입력시 아래와 같이 출력

~~~
키저장소 유형: PKCS12
키 저장소 제공자: SUN

키 저장소에 1개의 항목이 포함되어 있습니다.

별칭 이름: Localhost
생성 날짜: 2020. 01. 01
항목 유형: PrivateKeyEntry
인증서 체인 길이: 1
인증서[1):
소유자: CN=peter-kafka01.foo.bar 발행자: CN-peter-kafka01. f00.bar
일련 번호: 27f70778
적합한 시작 날짜 : Thu Jan 01 01:01:01 KST 2020 종료 날짜: Fri Jan 01 01:01:01 KST 2921 

인증서 지문:
    MDS: EA:86:8E:76:04:BC:EE:F1:F1:F3:61:D1:F4:D1:80:29
    SHA1: 23:07:0F:BB: 15:8C:28:DE: 44:74:C1:F5:C4:E8:8E:24:36:74:08:78
    SHA256: 86:C3:0B:90:A8:F3:90:29:48: FE:37:DF: 19:85 :AC: 1D:6C: 9B:88: F0:05:7D:57:E6:DC:86:B5:73:C8:67:1F:89
서명 알고리즘 이름: SHA256withRSA
주체 공용 키 알고리즘: 2048비트 RSA 키 
...중릭...
~~~

### 9.2.2 CA 인증서 생성

```shell
sudo openssl req-new -x509 -keyout ca-key -out ca-cert-days 356 -subj "/CN=foo.bar" -nodes
```

#### openssl 상세 옵션

| 옵션 이름 | 설명                          |
|-----------|-------------------------------|
| new       | 새로 생성 요청                |
| x509      | 표준 인증서 번호              |
| keyout    | 생성할 키 파일 이름           |
| out       | 생성할 인증서 파일 이름       |
| days      | 유효 일자                     |
| subj      | 인증서 제목                   |
| nodes     | 프라이빗 키 파일을 암호화하지 않음 |


### 9.2.3 트러스트스토어 생성

```shell
sudo keytool -keystore kafka.server.truststore. jks -alias
CARoot - importcert -file ca-cert -storepass $SSLPASS -keypass $SSLPASS
```

#### 트러스트스토어 내용 확인

```shell
keytool -list -v -keystore kafka.server.truststore.jks
```

#### 실행 결과

~~~
키 저장소 유형: jks
키 저장소 제공자: SUN

키 저장소에 1개의 항목이 포함되어 있습니다.

별칭 이름: caroot
생성 날짜: 2920. 01. 01
항목 유형: trustedCertEntry

소유자: CN=foo.bar
발행자: CN=foo.bar
일련 번호: dceb6a8ef40f3815
적합한 시작 날짜: Thu Jan 01 01:01:01 KST 2928 종료 날짜: Wed Jan 01 01:01:01 KST 2021 
인증서 지문:
    MDS: BB: 1A:20:84:07:73:C7:18:40:1:CF:CD: 7D:94:4E: 66
    SHA1: AA: FE:48:8B:53:DO:9:98:31:DF:FF:45:C9:24:AD: EB: 2B: 3B:D1:43
    SHA256: 6A:8C:9E: 1F:87:F0:B8:1D:2F:8B:44:79:62:B5:69:B1:84:C®:7A:BA:29:3F:AC:41:D8:
    BF:D8:9B:66:34:78:E2
서명 알고리즘 이름: SHA256withRSA
주체 공용 키 알고리즘: 2048비트 RSA 키
버전: 3
...중략...
~~~

### 9.2.4 인증서 성명

키스토어에 저장된 모든 인증서는 자체 서명된 CA의 서명을 바아야함

키스토어에서 인증서 추출

```shell
sudo keytool -keystore kafka.server.keystore.jks -alias 
localhost -certreq -file cert-file -storepass $SSLPASS -keypass $SSLPASS
```

~~~
Stgnature ok
subject=/CN-peter-kafka@1.foo.bar
Getting CA Private Key
~~~

서명이 완료됨을 알 수 있음

#### CA 서명을 위한 openssl 상세 옵션

| 옵션 이름 | 설명                   |
|-----------|------------------------|
| x509      | 표준 인증서 번호       |
| req       | 인증서 서명 요청       |
| ca        | 인증서 파일            |
| cakey     | 프라이빗 키 파일       |
| in        | 인풋 파일              |
| out       | 아웃풋 파일            |
| days      | 유효 일자              |
| passin    | 소스의 프라이빗 키 비밀번호 |

키스토어 자체 서명된 CA 인증서인 ca-cert와 서명된 cert-signed 추가

```shell
# ca-cert
sudo keytool -keystore kafka.server.keystore.jks -alias
CARoot -importcert -file ca-cert -storepass $SSLPASS -keypass $SSLPASS

# cert-signed
sudo keytool -keystore kafka.server.keystore.jks -alias
CARoot -importcert -file ca-cert -storepass $SSLPASS -keypass $SSLPASS
```

아까와 같은 명령어로 다시 키스토어 내용 확인

#### 키스토어 내용 확인

```shell
keytool -list -v -keystore kafka.server.keystore.jks
```

> 키스토어 내용 생략.. 좀 많네요

이전과 결과가 달라졌는데, 저장소에 총 2개의 인증서가 저장되어 있으며 자체 성명된 CA 인증서 내용이 포함되어있음

### 9.2.5 나머지 브로커에 대한 SSL 구성

도메인 유의하여 다른 서버에도 똑같이 적용

### 9.2.6 브로커 설정에 ㄴ니 cnrk

브로커 설정 파일인 server.properties 설정을 변경해야하늗네, 기존의 PLAINTEXT로 통신하는 부분은 유지하며 SSL 통신을 추가하는 형태

리눅스 vi 명령어를 이용해 listeners, advertised.listeners, security.inter.broker.protocol 설정을 아래와 같이 변경

security.inter.broker.protocol 옵션은 SSL을 이용한 브로커 간 내부 통신을 원하는 경우에만 설정하면 됨

```yaml
listeners=PLAINTEXT://0.0.0.0:9092,55L://0.0.0.0:9093

# 각 호스트네임과 일치하도록 변경
advertised. listeners=PLAINTEXT://peter-kafka01.foo.bar:9092,SSL://peter-kafka01.foo.bar:9093

ssl.truststore.location=/usr/local/kafka/ssl/kafka.server.truststore.jks 
ssl.truststore.password=peterpass
ssl.keystore.location=/usr/local/kafka/ssl/kafka.server.keystore.jks 
ssl.keystore.password=peterpass
ssl.key.password=peterpass

## 내부 브로커 통신 간 SSL을 사용할 경우
security. inter.broker.protocol=SSL
```

각 브로커마가 위와같이 변경하고, 호스트네임과 일치하도록 변경

설정 변경이 완료되면 1대씩 재시작

```shell
openssl s_client -connect peter-kafka01.foo.bar:9093 -tls1
</dev/null 2>/dev/null | grep -E 'Verify return code'
```

~~~
Verify return code: 0 (ok)
~~~

SSL 통신을 위한 준비 완료!

### 9.2.7 SSL 기반 메시지 전송

peter-kafka01 브로커에 접속 후 클라이언트용 트러스트스토 생성 (위에서 사용한 방법을 그대로 사용)

생성 후, 토픽으로 직접 메시지를 전송하는 테스트 진행

전송 테스트를 위한 별도의 peter-test07 토픽을 생성

```shell
/usr/local/kafka/bin/kafka-topics.sh --bootstrap-server 
peter-kafka01.foo.bar:9092 --create --topic peter-test07 --partitions 1 --replication-
factor 3
```

토픽 생성 후 SSL 통신이 적용된 콘솔 프로듀서를 사용하기 위해서는 클라이언트에 `별도로` 설정 파일을 만들어야함

리눅스 vi 명령어로 ssl.config 파일을 만들어야함

당연하게도, 복호화와 암호화를 반복하면 브로커와 클라이언트 모두 부하를 가중시키므로 외부와 통신하는 카프카를 사용할때만 사용해야함

## 9.3 커버로스(SASL)를 이용한 카프카 인증

SASL 중 커버로스를 설명

커버로스는 티켓을 기반으로 컴퓨터 네트워크 인증 프로토콜로, 사용자의 신원을 식별하기 위한 용도

공개된지 오래됐고, SSO를 지원하기에 커버로스가 많이 사용됨

### 커버로스 아키텍처
![5_커버로스아키텍처.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch9/5_커버로스아키텍처.png)

### 9.3.1 커버로스 구성

커버로스 서버는 앤서블을 이용해 설치함

책을 기반으로, 현재 설정된 커버로스에서 사용하는 디폴트 렐름은 FOO.BAR

설치 완료 후 peter-zk01 서버로 접속해 Principal 객체를 생성하고, Principal에 키탭을 생성해야 인증 받을 수 있음

커버로스의 kadmin.local 명령어를 이용해 커버로스에서 사용할 유저를 생성

이 책에서는 peter 1, 2, Admin 을 생성

```shell
sudo kadmin.local -q "add_principal -randkey peter01@00.BAR"
sudo kadmin.local -q "add_principal -randkey peter02@F00.BAR"
sudo kadmin.local -q "add_principal -randkey admin@F00.BAR"
```

그 후 Principal 생성

```shell
sudo kadmin.local -q "add_principal -randkey kafka/peter-kafka81.foo.bar@F00.BAR"
sudo kadmin.local -q "add_principal -randkey kafka/peter-kafka82.foo.bar@F00.BAR"
sudo kadmin.local -q "add_principal -randkey kafka/peter-kafka83.foo.bar@F00.BAR"
```

Principal 생성 후 키탭 생성

매번 비밀번호 없이 원격 시스템에 인증할 수 있으므로 잘 관리해야함

```shell
mkdir -p /home/ec2-user/keytabs/
sudo kadmin.local -q "ktadd -k /home/ec2-user/keytabs/peter@1.user.keytab peter®1@F00.BAR"
sudo kadmin.local -q "ktadd -k /home/ec2-user/keytabs/peter02.user keytab peter02@F00.BAR"
sudo kadmin.local -q "ktadd -k /home/ec2-user/keytabs/admin.user.keytab admin@F00.BAR"
sudo kadmin.local -q "ktadd -k /home/ec2-user/keytabs/peter-kafka01.service.keytab kafka/peter-kafka01.foo.bar@F00.BAR"
sudo kadmin.local -q "ktadd -k /home/ec2-user/keytabs/peter-kafka02.service.keytab kafka/peter-kafka02.foo.bar@F00.BAR"
sudo kadmin.local -q "ktadd -k /home/ec2-user/keytabs/peter-kafka03.service.keytab kafka/peter-kafka03.foo.bar@F00.BAR"
```

키탭 파일을 생성했으므로 각 브로커로 파일을 복사함. chmod를 사용하여 파일의 소유자를 ec2-user로 변경

```shell
sudo chown -R ec2-user.ec2-user keytabs/
```

### 9.3.2 키탭을 이용한 인증

scp 툴을 사용하여 -i 옵션으로 keypair.pem을 지정

pem 키는 ec2에 접속하는 비밀키이며 peter-kafka01 서버에 복사한 것

peter-kafka01 서버에 접속해 scp 명령어를 이용해 peter-zk01 서버로부터 키탭 디레ㄱ토리 복사

복사가 끝나면 /home/ec2-user 경로 하위로 keytabs 폴더가 복사되고 다음과 같이 해당 폴더를 /usr/local/kafka로 이동

위 방법을 모든 브로커에 적용 모든 브로커에 적용

위 과정이 정상적으로 동작해 등록된 Principal 라면 kinit 명령어를 통해 티켓을 발급 받을 수 있음

#### 티켓 발급

```shell
kinit -kt /usr/local/kafka/keytabs/peter01.user.keytabpeter01
```

#### 티켓 조회

```shell
klist
```

~~~
# 출력
Ticket cache: FILE:/tmp/krb5cc_1000
Default principal: peter01@F00.BAR  - Principal 이름 확인

아래는 티켓 유효시간을 나타내며 만료 시간은 24시간임
Valid starting          Expires                 Service principal
2021-01-01700:00:01     2021-01-02700:00:01     krbtgt/F00.BAR@F00.BAR
~~~

Kafka 서비스로도 티켓을 같은 방법으로 발급 받음

### 9.3.3 브로커 커버로스 설정

커버로스 설정을 적용하기 위해 모든 브로커에서 server.properties 설정을 변경해야 함

SSL 적용 후 수정했던 것과 유사

내부 프로커 통신 간에도 커버로스 기반 통신을 적용하고자 한다면 security.interbroker.protole 부분을 SASL_PLAINTEXT,

일반적인 통신을 유지하고자 한다면 PLAINTEXT로 설정

```shell
sudo vi /us/local/kafka/config/server.properties
```

```yaml
listeners=PLAINTEXT://0.0.0.0:9092, SSL://0.0.0.0:9893, SASL_PLAINTEXT://0.0.0.0:9094 
advertised. listeners=PLAINTEXT://peter-kafka®1.foo.bar:9092,SSL://peter-kafka01.
foo.bar:9093, SASL_PLAINTEXT://peter-kafka@1.foo.bar:9094

security. inter.broker.protocol=`SASL_PLAINTEXT`
sasl.mechanism.inter.broker.protocol=GSSAPI
sasl.enabled.mechanism=GSSAPI
sasl.kerberos.service.name=kafka
```

sasl.kerberos.service.name을 커버로스 생성할 때 만들었던 kafka 서비스 네임과 정확하게 일치시켜야 함

커버로스 인증을 위한 jaas.conf 파일 생성

JAAS(Java Authentication and Authorization Service)는 자바 애플리케이션의 유연성을 위해 사용자 인증에 대한 부분을 분리해 독립적으로 관리할 수 있는 표준 API

`kafka_server.jaas.conf` 파일 생성 후 아래 코드 추가

```yaml
KafkaServer {
  com.sun.security.auth.module.Krb5LoginModule required
  useKeyTab=true
  storekey=true
  keyTab="/us/local/kafka/keytabs/peter-kafka®1.service.keytab"
  principal="kafka/peter-kafka01. foo.bar@F00.BAR";
}
```

`kafka_server.jaas.conf` 파일을 모든 브로커에 설정해야하며 keyTab은 각 브로커의 호스트네임과 동일하게 설정

브로커 시작시 `kafka_server.jaas.conf` 파일을 로드할 수 있도록 KAFKA_OPTS 카프카 환경 변수에 설정을 추가

vi 명령어로 jmx 파일을 열어 추가

```shell
sudo vi /us/local/kafka/config/jmx
```

```yaml
KAFKA_OPTS="-Djava.security.auth. login.config=/usr/local/kafka/config/kafka_server_jaas.conf"
```

그 후 재시작하여 커버로스 통신으로 설정한 9094 포트가 잘 실행되는지 확인

### 9.3.4 클라이언트 커버로스 설정

카프카에 커버로스를 적용하기 위한 마지막 단계

커버로스 관련 클라이언트 설정을 추가한 후 콘솔 프로듀서와 컨슈머 클라이언트를 실행 할 것

클라이언트도 서버와 동일하게 jaas 설정 파일이 필요하며 서버와 비슷하게 이름을 설정

`kafka_client.jaas.conf`

키탭 파일과 Principal 정보를 jaas.conf에 추가해도 되지만 간단하게 캐시로 적용

클라이언트도 시작시 `kafka_client.jaas.conf` 파일을 로드할 수 있도록 KAFKA_OPTS 카프카 환경 변수에 설정을 추가

```shell
export KAFKA_OPTS="-Djava.security.auth. login.config=/home/ec2-user/kafka_client_jaas.conf"
```

콘솔 프로듀서와 콘솔 컨슈머에서 사용할 kerberos.config 파일을 생성하여 커버로스 설정을 추가

```shell
vi kerberos.config
```

콘솔 프로듀서와 콘솔 컨슈머를 위한 keberos.config 파일

```yaml
sasl.mechanism=GSSAPI
security.protocol=SASL_PLAINTEXT
sasl.kerberos.service.name=kafka
```

## 9.4 ACL을 이용한 카프카 권한 설정

카프카 보안의 3단계, 권한. 각 유저별로 특정 토픽에 접근을 허용할지에 대한 여부를 설정. 

브로커의 설정 파일을 수정하고 kafka-acls.sh 명령어로 유저별 권한을 설정

### 9.4.1 브로커 권한 설정

권한 설정을 위해 server.properties 파일 수정

카프카 ACL을 위한 코드를 추가

```yaml
# 기존에 있던 내용
security. inter.broker.protocol=SASL_PLAINTEXT 
sasl.mechanism.inter.broker.protocol=GSSAPI
sasl.enabled.mechanism=GSSAPI
sasl.kerberos.service.name=kafka

# 아래 내용 추가
authorizer.class.name=kafka.security.authorizer.AclAuthorizer
super.users-User:admin;User:kafka
```

위 설정 작업은 모든 브로커에 동일하게 설정해야함

authorized.class.name은 카프카 권한을 위한 class. 기본 내장된 것을 사용하도록 지정

super.users는 모든 권한을 갖는 슈퍼 권한, 여기서는 admin이 슈퍼맨

다시 브로커 재시작하고 토픽 2개 생성하여 실습 진행

### 9.4.2 유저별 권한 설정

그동안 만든 유저를 이용해 ACL 규칙을 만들어 테스트 수행

![6_유저별권한설정구성도.png](https://raw.githubusercontent.com/mash-up-kr/S3A/master/14th_kafka/junhyoung/image/ch9/6_유저별권한설정구성도.png)

peter01 유저는 Peter-test09 토픽에 대해 읽기와 쓰기 가능

peter02 유저는 Peter-test10 토픽에 대해 읽기와 쓰기 가능

admin 유저는 Peter-test09, Peter-test10 토픽에 대해 읽기와 쓰기 가능

ACL 규칙 설정

```shell
[ec2-user@ip-172-31-5-59 ~]$ /usr/local/kafka/bin/kafka-acls.sh --authorizer-properties
zookeeper. connect=peter-zk01.foo.bar:2181 --add --allow-principal User:peter®1
--operation Read --operation Write --operation DESCRIBE --topic peter-test09
```

허용할 특정 호스트를 필요에 따라 제어할 수 있음

호스트를 추가하고 싶다면, --allow-host 172.31.5.59와 같이 설정

앞서 적용한 ACL 규칙의 대상은 리소스 타입이 토픽이었고, 그룹 리소스 타입에 대해서는 ACL 규칙을 추가하지 않았기 때문에, 그룹에 대한 ACL 규칙을 추가해줘야함

```shell
/usr/local/kafka/bin/kafka-acls.sh --authorizer-properties 
zookeeper.connect=peter-zk01.foo.bar:2181 --add --allow-principal User:peter01
--operation Read --group '**
```