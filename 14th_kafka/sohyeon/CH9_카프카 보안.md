# 9장 카프카 보안
- 카프카 애플리케이션에서 제공하는 보안 요소
- 보안 기능 직접 구성

<br/>

## 9.1 카프카 보안의 3가지 요소

<img alt="image" width="500" src="https://github.com/mash-up-kr/S3A/assets/55437339/6668d4ee-1d8a-4abf-9594-03f7692857a4"/>

🔼 카프카 보안의 3요소
- **암호화**: 중간에서 패킷을 가로채더라도 암호화를 설정해두어 데이터를 읽을 수 없게 한다.
- **인증**: 카프카 클라이언트들이 접근할 때 확인된 클라이언트들만 접근할 수 있도록 한다.
- **권한**: 필요한 기능만 사용할 수 있도록 제한된 권한을 부여한다.

<br/>

### 9.1.1 암호화(SSL)
- 암호화 통신을 설정하기 위해 일반적으로 SSL을 사용한다.
- `SSL`: (보안 소켓 레이어) 서버와 서버/클라이언트 사이에서 통신 보안을 적용하기 위한 표준 암호 규약
- 동작 방식은 다음과 같다.
  - SSL은 인증기관(CA)으로부터 인증서를 발급받은 후, 인정서를 이용한 공개키, 개인 키 방식으로 서버와 클라이언트가 암호화/복호화하면서 통신한다.
  - 암호화/복호화를 위해 미리 지정된 키를 주고받아야 하는데, 이때 사용하는 키의 방식으로 대칭 키 방식과 비대칭 키 방식이 있다.
    - **대칭 키 방식**: 하나의 키를 가지고 서버와 클라이언트가 통신한다. (오버헤드는 적지만 노출 위험성이 있음)
    - **비대칭 키 방식**: 2개의 키를 가지고 서버와 클라이언트가 통신한다. (노출 위험성은 적지만 오버헤드가 큼)
   
- SSL은 보안을 강화하면서 효율적인 고성능을 얻기 위해 대칭 키와 비대칭 키의 2가지 방식을 혼용하는 방식으로 사용한다.

<br/>

### 9.1.2 인증(SASL)
- `SASL`: 인터넷 프로토콜에서 인증과 데이터 보안을 위한 프레임워크로서 카프카에서도 사용된다.
- 애플리케이션 프로토콜에서 **인증 메커니즘**을 분리함으로써 모든 애플리케이션에서 사용할 수 있다.
  - **SASL/GSSAPI**: (카프카 0.9ver ~) 커버로스 방식으로 많이 사용되는 인증 방식 중 하나이다. (되도록 하나의 렐름으로 모든 애플리케이션을 적용하는 것이 추천됨)
  - **SASL/PLAIN**: (카프카 0.10.0ver ~) 아이디와 비밀번호를 텍스트 형태로 사용한다. (개발 환경에서 테스트 목적으로 주로 활용됨)
  - **SASL/SCRAM-SHA-256, SASL/SCRAM-SHA-512**: (카프카 0.10.2ver ~) 본래의 암호에 해시된 내용을 추가함으로써 암호가 유출되어도 본래 암호를 알 수 없어 안전하게 저장할 수 있다.
  - **SASL/OAUTHBEARER**: (카프카 2.0ver ~) 카프카에서 제공하는 기능은 매우 한정적이라 개발 환경 정도에만 적용 가능한 수준이다. (운영 환경에서 별도의 핸들러 구성 필요)
 
<br/>

### 9.1.3 권한(ACL)
- `ACL`: (접근 제어 리스트) 규칙 기반의 리스트를 만들어 접근 제어를 하는 것
- CLI로 ACL을 추가하거나 삭제할 수 있으며 모든 ACL 규칙은 주키퍼에 저장된다.
- ACL은 **리소스 타입**별로 구체적인 설정이 가능하다.
  - **토픽**, **그룹**, 클러스터, 트랜잭셔널 ID, 위임 토큰

<br/>

<img alt="image" width="500" src="https://github.com/mash-up-kr/S3A/assets/55437339/54803e9d-1c01-4c61-9f6b-918b8cf1fb00"/>

🔼 권한 분리가 필요한 사례
- B 서비스에서 개발 담당자의 실수로 인해 A 서비스가 사용하는 A 토픽으로 B 서비스의 메시지들을 전송했다.
- A 서비스 컨슈머가 A 토픽의 내용을 읽다가 B 서비스의 메시지로 인해 파싱 에러가 발생하면서 여러 가지 문제가 발생할 수 있다.

<br/>

## 9.2 SSL을 이용한 카프카 암호화

<img alt="image" width="500" src="https://github.com/mash-up-kr/S3A/assets/55437339/6a6f9de9-b14e-4ab1-b69f-693c3186e356"/>

🔼 카프카 SSL 적용 개요 (클러스터 환경 기준)

<br/>

### 9.2.1 브로커 키스토어 생성

<img alt="image" width="500" src="https://github.com/mash-up-kr/S3A/assets/55437339/5fc8d6e2-1645-407f-804b-0a152e502bbe"/>

🔼 키스토어와 트러스트스토어의 관계도
- 클라이언트와 서버 사이에 자바 애플리케이션을 이용해 SSL 연결을 할 때 사용한다.
- 키스토어와 트러스트스토어 모두 keytool을 이용해 관리되며, 각 스토어에 저장되는 내용은 차이가 있다.
  - **키스토어**: 서버 측면에서 프라이빗 키와 인증서를 저장하며, 자격 증명을 제공한다.
  - **트러스트스토어**: 클라이언트 측면에서 서버가 제공하는 인증서를 검증하기 위한 퍼블릭 키와 SSL 연결에서 유효성을 검사하는 서명된 인증서를 저장한다. (민감한 정보X)
 
<br/>

> **실습**

```bash
$ sudo mkdir -p /usr/local/kafka/ssl # ssl 디렉토리 생성 (필요한 파일들을 모아두기 위함)
$ cd /usr/local/kafka/ssl/ # 디렉토리 진입
$ export SSLPASS=peterpass # 비밀번호 통일 (for 테스트 환경)
```
🔼 디렉토리 생성

<br/>

```bash
sudo keytool -keystore kafka.server.keystore.jks -alias localhost -keyalg RSA -validity 365 -genkey -storepass $SSLPASS -keypass $SSLPASS -dname "CN=peter-kafka01.foo.bar" -storetype pkcs12
```
🔼 키스토어 생성

<br/>

|옵션 이름|설명|
|---|---|
|keystore|키스토어 이름|
|alias|별칭|
|keyalg|키 알고리즘|
|genkey|키 생성|
|validity|유효 일자|
|storepass|저장소 비밀번호|
|keypass|키 비밀번호|
|dname|식별 이름|
|storetype|저장 타입|

🔼 keytool의 상세 옵션

<br/>

```bash
$ keytool -list -v keystore kafka.server.keystore.jks
키 저장소 비밀번호 입력: peterpass
```
🔼 키스토어의 내용 확인
- 출력 내용을 통해 옵션으로 설정한 저장소 유형, 유효 기간, 알고리즘, 소유자, 발행자 등에서 설정한 내용들을 확인할 수 있다.

<br/>

### 9.2.2 CA 인증서 생성
- 공인 인증서를 사용하는 이유는 위조된 인증서를 방지해 클라이언트-서버 간 안전한 통신을 하기 위해서이며, 이러한 역할을 보장해주는 곳이 인증기관(CA)이다.
- 일반적으로는 CA에 일부 비용을 지불하고 인증서를 발급받지만, 테스트/개발 환경에서는 자체 서명된 CA 인증서나 사설 인증서를 생성하기도 한다.

<br/>

>**실습**

```bash
$ sudo openssl req -new -x509 -keyout ca-key -out ca-cert -days 356 -subj "/CN=foo.bar" -nodes
```
🔼 CA 인증서 생성

<br/>

|옵션 이름|설명|
|---|---|
|new|새로 생성 요청|
|x509|표준 인증서 번호|
|keyout|생성할 키 파일 이름|
|out|생성할 인증서 파일 이름|
|days|유효 일자|
|subj|인증서 제목|
|nodes|프라이빗 키 파일을 암호화하지 않음|

🔼 openssl의 상세 옵션

<br/>

### 9.2.3 트러스트스토어 생성
```bash
sudo keytool -keystore kafka.server.truststore.jks -alias CARoot -importcert -file ca-cert -storepass $SSLPASS -keypass $SSLPASS
```
🔼 자체 서명된 CA 인증서를 트러스트스토어에 추가한다.
- 클라이언트가 신뢰할 수 있도록 한다.

<br/>

|옵션 이름|설명|
|---|---|
|keytool|키스토어 이름|
|alias|별칭|
|importcert|인증서를 임포트|
|file|인증서 파일|
|storepass|저장소 비밀번호|
|keypass|키 비밀번호|

🔼 트러스트 생성을 위한 keytool의 상세 옵션

<br/>

```bash
$ keytool -list -v -keystore kafka.server.truststore.jks
```
🔼 트러스트스토어의 내용 확인
- 인증서 생성 시 설정한 CN 정보와 유효 기간 등을 확인할 수 있다.

<br/>

### 9.2.4 인증서 서명
```bash
$ sudo keytool -keystore kafka.server.keystore.jks -alias localhost -certreq -file cert-file -storepass $SSLPASS -keypass $SSLPASS
```
🔼 키스토어에서 인증서 추출

<br/>

```bash
$ sudo openssl x509 -req -CA ca-cert -CAkey ca-key -in cert-file -out cert-signed -days 365 -CAcreateserial -passin pass:$PASSWORD
```
🔼 자체 서명된 CA 서명 적용

<br/>

|옵션 이름|설명|
|---|---|
|x509|표준 인증서 번호|
|req|인증서 서명 요청|
|ca|인증서 파일|
|cakey|프라이빗 키 파일|
|in|인풋 파일|
|out|아웃풋 파일|
|days|유효 일자|
|passin|소스의 프라이빗 키 비밀번호|

<br/>

```bash
$ sudo keytool -keystore kafka.server.keystore.jks -alias CARoot -importcert -file ca-cert -storepass $SSLPASS -keypass $SSLPASS
```
🔼 키스토어에 CA 인증서와 서명된 cert-signed 추가

<br/>

```
$ keytool -list -v -keystore kafka.server.keystore.jks
```
🔼 키스토어의 내용 확인
- 저장소에 총 2개의 인증서가 저장되어 있으며 자체 저장된 CA 인증서 내용이 포함되어 있음을 알 수 있다.
- 동일한 작업을 클러스터 내 다른 브로커에도 수행해야 한다.

<br/>

### 9.2.5 나머지 브로커에 대한 SSL 구성
2대를 동시에 설정한다.

<br/>

```bash
# peter-kafka02 서버 접근 후 다음 명령어 입력
$ sudo mkdir -p /usr/local/kafka/ssl
$ export SSLPASS=peterpass

# peter-kafka03 서버 접근 후 다음 명령어 입력
$ sudo mkdir -p /usr/local/kafka/ssl
$ export SSLPASS=peterpass
```
🔼 디렉토리를 생성하고 암호를 환경 변수로 지정
- 해당 작업처럼 키스토어 생성, CA 인증서 생성, 트러스트스토어 생성, 인증서 서명 작업도 마찬가지로 2대에 동시에 설정한다.

<br/>

### 9.2.6 브로커 설정에 SSL 추가
```
listners=PLAINTEXT://0.0.0.0:9092,SSL://0.0.0.0:9093
advertised.listners=PLAINTEXT://peter-kafka01.foo.bar:9092,SSL://peter-kafka01.foo.bar:9093 # 각 호스트네임과 일치하도록 변경

ssl.truststore.location=/usr/local/kafka/ssl/kafka.server.truststore.jks
ssl.truststore.password=peterpass
ssl.keystore.location=/usr/local/kafka/ssl/kafka.server.keystore.jks
ssl.keystore.password=peterpass
ssl.key.password=peterpass
security.inter.broker.protocol=SSL # 내부 브로커 통신 간 SSL을 사용할 경우
```
- `/usr/local/kafka/config/server.properties` 파일을 위 내용과 같이 변경한다.
- 브로커 간의 통신은 PLAINTEXT로 설정하고 브로커-클라이언트 간의 통신은 SSL을 적용하여 암호화/복호화로 인한 오버헤드를 줄일 수 있다.

<br/>

```bash
$ sudo systemctl restart kafka-server
```
🔼 브로커 재시작

<br/>

```bash
$ openssl s_client -connect peter-kafka01.foo.bar:9093 -tls1 </dev/null 2>/dev/null | grep -E 'Verify return code'
```
🔼 최종 확인
- 출력 내용에 ok 메시지가 뜨면 SSL 통신을 위한 준비가 완료된 것이다.

<br/>

### 9.2.7 SSL 기반 메시지 전송

```bash
$ cd /usr/local/kafka/ssl/
$ export SSLPASS=peterpass
$ sudo keytool -keystore kafka.client.truststore.jks -alias CARoot -importcert -file ca-cert -storepass $SSLPASS -keypass $SSLPASS
```
🔼 브로커 접속 후 클라이언트용 트러스트스토어 생성
- 상세 과정은 앞의 트러스트스토어 생성 작업과 동일하다.

<br/>

```bash
$ /usr/local/kafka/bin/kafka-topics.sh --bootstrap-server peter-kafka01.foo.bar:9092 --create --topic peter-test07 --partitions 1 --replication-factor 3
```
🔼 토픽 생성
- 전송 테스트를 위함이다.

<br/>

```
security.protocol=SSL
ssl.truststore.location=/usr/local/kafka/ssl/kafka.client.truststore.jks
ssl.truststore.password=peterpass
```
🔼 SSL 통신을 위한 ssl.config
- ssl.config 파일을 만들고 위의 내용을 추가한다.

<br/>

- 프로듀서가 메시지를 전송하면 컨슈머가 메시지를 정확하게 읽어올 수 있다.
- 외부 네트워크에 있는 클라이언트가 내부 네트워크에 있는 카프카로 접근하는 경우에만 SSL 작업을 수행하기가 권장된다.
- 보안을 강화하면 브로커와 클라이언트의 성능 저하가 발생할 수도 있다.

<br/>

## 9.3 커버로스(SASL)를 이용한 카프카 인증

<img alt="image" width="500" src="https://github.com/mash-up-kr/S3A/assets/55437339/902a8592-c81f-4469-80d1-acf384bc99c6"/>

🔼 커버로스 아키텍처
- 커버로스는 티켓을 기반으로 하는 컴퓨터 네트워크 인증 프로토콜로서, 사용자의 신원을 식별하기 위한 용도로 사용된다.
- 하나의 커버로스 티켓을 이용해 여러 서비스에서 같이 적용할 수 있는 싱글 사인온을 지원한다.
- 카프카 클라이언트는 인증/티켓 서버로부터 인증 확인 절차를 거치고, 인증이 완료되면 티켓을 발급받는다.
- 클라이언트는 발급받은 티켓을 가지고 카프카 서버에 인증을 할 수 있다.

<br/>

### 9.3.1 커버로스 구성

```bash
$ cd ansible_playbook
$ ansible-playbook -i hosts keyberos.yml
```
🔼 커버로스 설치

<br/>

```bash
$ sudo kadmin.local -q "add_principal -randkey peter01@FOO.BAR"
$ sudo kadmin.local -q "add_principal -randkey peter02@FOO.BAR"
$ sudo kadmin.local -q "add_principal -randkey admin@FOO.BAR"
```
🔼 유저 생성
- 커버로스에서 사용할 유저를 생성한다.
- peter01, peter02, admin이라는 총 3명의 유저를 생성한다.

<br/>

```bash
$ sudo kadmin.local -q "add_principal -randkey kafka/peter-kafka01.foo.bar@FOO.BAR"
$ sudo kadmin.local -q "add_principal -randkey kafka/peter-kafka02.foo.bar@FOO.BAR"
$ sudo kadmin.local -q "add_principal -randkey kafka/peter-kafka03.foo.bar@FOO.BAR"
```
🔼 프린시펄 생성
- kafka 서비스를 위한 프린시펄을 생성한다.
- 프린시펄의 형식은 `서비스명/각 브로커 호스트네임`이다.

<br/>

```bash
$ mkdir -p /home/ec2-user/keytabs/
$ sudo kadmin.local -q "ktadd -k /home/ec2-user/keytabs/peter01.user.keytab peter@FOO.BAR"
$ sudo kadmin.local -q "ktadd -k /home/ec2-user/keytabs/peter02.user.keytab peter@FOO.BAR"
$ sudo kadmin.local -q "ktadd -k /home/ec2-user/keytabs/admin.user.keytab admin@FOO.BAR"
$ sudo kadmin.local -q "ktadd -k /home/ec2-user/keytabs/peter-kafka01.service.keytab kafka/peter-kafka01 foo.bar@FOO.BAR"
$ sudo kadmin.local -q "ktadd -k /home/ec2-user/keytabs/peter-kafka02.service.keytab kafka/peter-kafka02 foo.bar@FOO.BAR"
$ sudo kadmin.local -q "ktadd -k /home/ec2-user/keytabs/peter-kafka03.service.keytab kafka/peter-kafka03 foo.bar@FOO.BAR"
```
🔼 키탭 생성
- 키탭 파일을 이용하면 커버로스 인증 시 별도의 비밀번호를 입력하지 않고도 원격 시스템에 인증할 수 있다.

<br/>

```bash
$ sudo chown -R ec2-user.ec2-user keytabs/
```
🔼 파일 권한 변경
- 파일의 소유자를 ec-user로 변경한다.

<br/>

### 9.3.2 키탭을 이용한 인증

```bash
$ scp -i keypair.pem -r peter-zk01.foo.bar:~/keytabs /home/ec2-user
$ sudo mv keytabs /usr/local/kafka
```
🔼 키탭 복사
- peter-zk01 서버로부터 keytabs 디렉토리를 복사한다.
- /home/ec2-user 경로 하위로 keytabs 폴더가 복사되고, 폴더를 /usr/local/kafka로 이동한다.
- 동일한 방법으로 모든 브로커에서 복사를 진행한다.

<br/>

```bash
$ kinit -kt /usr/local/kafka/keytabs/peter01.user.keytab peter01
```
🔼 티켓 발급
- kafka01 서버에 접속한 후 키탭 파일들이 있는 경로로 이동하여 티켓을 발급받는다.

<br/>

```
Ticket cache: FILE:/tmp/krb5cc_1000
Default principal: peter01@FOO.BAR # 프린시펄 이름을 확인한다.

Valid starting    Expires    Service principal # Valid Starting은 티켓이 발급된 시각을 의미하며, 발급된 티켓은 만료 기한이 있다. Expires는 티켓의 만료 기한을 의미한다. 현재 커버로스에 설정된 만료 기한은 24시간이므로 시작 시각과 24시간 차이가 나는지 확인한다.
...
```
- `klist` 명령어를 이용해 티켓 발급 내역을 확인할 수 있다.

<br/>

```bash
$ kinit -kt /usr/local/kafka/keytabs/peter-kafka01.service.keytab kafka/peter-kafka01.foo.bar
```
🔼 티켓 발급
- kafka 서비스로 티켓을 발급받는다.
- 티켓을 확인해보면 프린시펄 네임만 다르고 나머지 내용은 위와 비슷하다.

<br/>

### 9.3.3 브로커 커버로스 설정

```
listners=PLAINTEXT://0.0.0.0:9092,SSL://0.0.0.0:9093,SASL_PLAINTEXT://0.0.0.0:9094
advertised.listners=PLAINTEXT://peter-kafka01.foo.bar:9092,SSL://peter-kafka01.foo.bar:9093,SASL_PLAINTEXT://peter-kafka01.foo.bar:9094

security.inter.broker.protocol=SASL_PLAINTEXT
sasl.mechanism.inter.broker.protocol=GSSAPI
sasl.enabled.mechanism=GSSAPI
sasl.kerberos.service.name=kafka
```
🔼 커버로스 설정 적용을 위한 peter-kafka01의 server.properties 파일 일부
- `sasl.kerberos.service.name` 값은 커버로스 생성 시 만들었던 kafka 서비스네임과 정확하게 일치시켜줘야 한다.

<br/>

```
KafkaServer {
  com.sun.security.auth.module.Krb5LoginModule required
  useKeyTab=true
  storeKey=true
  keyTab="/usr/local/kafka/keytabs/peter-kafka01.service.keytab"
  principal="kafka/peter-kafka01.foo.bar@FOO.BAR";
};
```
🔼 peter-kafka01의 kafka_server_jaas.conf 파일
- `JAAS`: 자바 애플리케이션의 유연성을 위해 사용자 인증에 대한 부분을 분리해 독립적으로 관리할 수 있는 표준 API
- JAAS의 설정 파일 수정하여 인증을 적용할 수 있다.

<br/>

```
KAFKA_OPTS="-Djava.security.auth.login.config=/usr/local/kafka/config/kafka_server_jaas.conf"
```
🔼 KAFKA_OPTS 추가 설정
- jmx 파일을 열어 환경변수에 설정을 추가한다.

<br/>

```bash
$ sudo systemctl restart kafka-server
```
🔼 브로커 재시작

<br/>

```bash
$ sudo netstat -ntlp | grep 9094
```
- 커버로스 통신으로 설정한 9094 포트가 잘 실행되면 커버로스로 통신할 준비를 마친 것이다.

<br/>

### 9.3.4 클라이언트 커버로스 설정

```
KafkaClient {
  com.sun.security.auth.module.Krb5LoginModule required
  useTicketCache=true;
};
```
🔼 카프카 명령어 실행을 위한 kafka_client_jaas.conf 파일
- jaas 설정 파일을 추가한다.

<br/>

```
$ export KAFKA_OPTS="-Djava.security.auth.login.config=/home/ec2-user/kafka_client_jaas.conf"
```
- KAFKA_OPTS 환경 변수를 이용해 jaas 설정 파일을 로드한다.

<br/>

```
sasl.mechanism.inter.broker.protocol=GSSAPI
security.protocol=SASL_PLAINTEXT
sasl.kerberos.service.name=kafka
```
🔼 콘솔 프로듀서와 콘솔 컨슈머를 위한 kerberos.config 파일
- 커버로스 설정을 추가한다.

<br/>

```bash
$ kinit -kt /usr/local/kafka/keytabs/peter01.user.keytab peter01
```
🔼 티켓 발급

<br/>

- 프로듀서로 메시지를 전송하면 컨슈머가 메시지를 잘 읽어옴을 확인할 수 있다.
- 티켓 삭제 후, 컨슈머가 메시지를 읽으려고 하면 에러가 발생하는 것을 확인할 수 있다. (인증 관련 오류)

<br/>

## 9.4 ACL을 이용한 카프카 권한 설정
```
security.inter.broker.protocol=SASL_PLAINTEXT
sasl.mechanism.inter.broker.protocol=GSSAPI
security.protocol=SASL_PLAINTEXT
sasl.kerberos.service.name=kafka
# 아래 내용 추가
authorizer.class.name=kafka.security.authorizer.AclAuthorizer # 권한을 위한 클래스
super.users=User:admin;User:kafka # 모든 권한을 갖는 슈퍼유저 권한 유저
```
🔼 카프카 ACL을 위한 추가 설정
- `server.properties` 파일을 수정한다.
- 파일 수정 후 브로커를 재시작한다.

<br/>

```bash
$ unset KAFKA_OPTS
$ /usr/local/kafka/bin/kafka-topics.sh --zookeeper peter-zk01.foo.bar:2181 --create --topic peter-test09 --partitions 1 --replication-factor 1
$ /usr/local/kafka/bin/kafka-topics.sh --zookeeper peter-zk01.foo.bar:2181 --create --topic peter-test10 --partitions 1 --replication-factor 1
```
- KAFKA_OPTS 환경 변수 설정 초기화 후 토픽 2개를 생성한다.

<br/>

### 9.4.2 유저별 권한 설정

<img alt="image" width="500" src="https://github.com/mash-up-kr/S3A/assets/55437339/56778f56-0076-4b8c-893b-a62fc98209e9"/>

🔼 유저별 권한 설정 구성도
- peter01 유저는 peter-test09 토픽에 대해 읽기와 쓰기 가능
- peter02 유저는 peter-test10 토픽에 대해 읽기와 쓰기 가능
- admin 유저는 peter-test09, peter-test10 토픽에 대해 읽기와 쓰기 가능

<br/>

```bash
$ /usr/local/kafka/bin/kafka-acl.sh --authorizer-properties zookeeper.connect=peter-zk01.foo.bar:2181 --add --allow-principal User:peter01 --operation Read --operation Wrtie --operation DESCRIBE --topic peter-test09
```
- peter01 유저에 대해 ACL 규칙을 생성한다.
- 같은 명령어로 peter02 유저에 대해서도 ACL 규칙을 생성한다.

<br/>

```
$ /usr/local/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=peter-zk01.foo.bar:2181 --list
```
- ACL 규칙의 리스트를 확인할 수 있다.

<br/>

```
$ kinit -kt /usr/local/kafka/keytabs/peter01.user.keytab peter01
$ export KAFKA_OPTS="-Djava.security.auth.login.config=/home/ec2-user/kafka_client_jaas.conf"
$ /usr/local/kafka/bin/kafka-console-producer.sh --bootstrap-server peter-kafka01.foo.bar:9094 --topic peter-test09 --producer.config kerberos.config
> peter-test09 message!
```
- peter01 유저의 티켓을 발급받은 후 콘솔 프로듀서를 이용해 peter-test09 토픽에게 메시지를 전송했다.
- peter-test10 토픽으로 메시지를 전송하면 권한이 없어 에러가 발생할 것이다.

<br/>

```bash
$ /usr/local/kafka/bin/kafka-console-consumer.sh --bootstrap-server peter-kafka01.foo.bar:9094 --topic peter-test09 --from-beginning --consumer.config kerberos.config
```
- peter01 유저를 이용해 콘솔 컨슈머로 peter-test09 토픽의 메시지를 읽는다.
- 컨슈머에 대한 권한을 설정하지 않았으므로 에러가 발생할 것이다.

<br/>

```
$ /usr/local/kafka/bin/kafka-acls.sh --authorizer-properties zookeeper.connect=peter-zk01.foo.bar:2181 --add --allow-principal User:peter01 --operation Read --group '*'
```
- 모든 컨슈머 그룹이 가능하도록 ACL 규칙을 추가한다.
- 다시 메시지를 읽어보면 성공할 것이다.

<br/>

- peter02 사용자도 peter01 사용자와 같이 ACL 권한을 설정해주면 프로듀서/컨슈머를 통해 메시지를 읽고 쓸 수 있다.
- admin 사용자는 슈퍼유저이므로 모든 권한을 갖고 있다.
- 보안이 적용된 카프카가 꼭 필요하다면, 보안 카프카와 비보안 카프카를 분리해 운영하는 안도 구상해보는 것이 좋다.
