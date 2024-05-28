# 1. 카프카 보안의 세 가지 요소

카프카는 별도의 보안 설정을 해줘야 안전하게 운영이 가능하다. 그 요소는 아래 세 가지이다.

- 암호화
- 인증
- 권한

## 1.2 암호화 (SSL)

브로커 - 프로듀서

브로커 - 컨슈머

간 통신 내용을 SSL암호화를 적용한다. SSL은 CA로부터 발급받은 인증서를 기반으로 `Public Key`와 `Private Key`를 통해 암/복호화하는 비대칭키 방식으로 동작한다.

또한 비대칭키의 단점 중 하나인 오버헤드를 줄이기 위해 대칭키 방식으로도 동작하기도 한다.

---

## 1.3 인증 (SASL)

SASL은 인증/데이터 보안을 위한 프레임워크이다. 카프카에서 적용되는 사항은 아래와 같다.

### 1.3.1 SASL/GSSAPI

GSSAPI는 커버로스 인증 방식으로 REALM 설정을 통해 인가를 수행한다.

[커버로스의 동작 흐름은 아래 그림과 같다.](https://developers.hyundaimotorgroup.com/blog/50)

![](https://aw-download-file.hmg-corp.io/3m5b8c7d9k/01HDCS6XYJCM9BYD4H9A41QJRZ)

![](https://aw-download-file.hmg-corp.io/3m5b8c7d9k/01HDCS6XYJCM9BYD4H9A41QJRY)

### 1.3.2 SASL/PLAIN

아이디와 비밀번호를 텍스트 형태로 사용하는 방법으로 개발 환경에서 테스트 목적으로 활용된다.

### 1.3.3 SASL/SCRAM-SHA-256, SASL/SCRAM-SHA-512

해시에 솔트를 적용하는 인증 메커니즘이다. 주키퍼에 저장해 사용하는 방식으로 토큰 방식도 지원하여 커버로스 서버가 없을 때 대안으로 적용하기 좋은 방식이다.

### 1.3.4 SASL/OAUTHBEARER

OAUTH방식을 지원하지만 카프카 생태계에선 지원하는 기능이 한정적이라 운영 환경에서는 별도의 핸들러를 구축해야 사용할 수 있다.

---

## 1.4 권한 (ACL)

카프카에는 자체적으로 ACL기능을 제공한다. CLI로 ACL을 추가/삭제할 수 있고 이 내용은 주키퍼에서 관리된다.

ACL은 `토픽`, `그룹`, `클러스터`, `트랜잭션 ID`, `위임 토큰` 과 같이 리소스 타입별로 지정할 수 있다.

[각 리소스에 적용할 수 있는 권한 작업은 이 링크에 있다.](https://docs.confluent.io/platform/current/kafka/authorization.html#operations)

![](https://github.com/mash-up-kr/S3A/blob/master/14th_kafka/dohyeon/image/9_1.png?raw=true)

![](https://github.com/mash-up-kr/S3A/blob/master/14th_kafka/dohyeon/image/9_2.png?raw=true)

---

# 2. SSL을 활용한 암호화

키스토어, 트러스트스토어 라는 요소들을 활용하여 퍼블릭 키, 프라이빗 키, 인증서를 추상화하여 암호화를 적용한다.

## 2.1 주요 용어

###  키스토어

일반적으로 서버 측면에서 프라이빗 키와 인증서를 저장, 자격 증명을 제공한다.

### 트러스트스토어

클라이언트 측면에서 서버가 제공하는 인증서를 검증하기 위한 퍼블릭 키와 서버와 SSL 연결에서 유효성을 검사하는 서명된 인증서를 저장한다.

## 2.2 적용 방법



---
