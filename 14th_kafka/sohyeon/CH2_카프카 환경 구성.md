# 2장 카프카 환경 구성
: AWS 상에서의 기본 환경 구성을 진행한다.

<br/>

## 2.1 실습 환경 구성

<img width="400" alt="image" src="https://github.com/mash-up-kr/S3A/assets/55437339/60e1086a-f746-42f1-9b28-0cf330130dd1" />

🔼 실습 환경 구성도
- AWS 상에서의 실제 운영 환경과 유사한 형태로 표현된 구성도
- 주키퍼의 경우 최소 수량인 EC2 인스턴스 3대, 카프카의 경우 최소 수량인 EC2 인스턴스 3대로 구성
- 배포 서버 1대 포함, private 도메인을 위한 foo.bar 도메인 표시

<br/>

---

### 2.1.1 AWS 환경에서 실습 환경 구성
- 과금이 필요한 기준의 설명
- 총 7개의 EC2 인스턴스 필요
- 최대 실습 시간 4시간 기준으로 약 2USD 과금 예상
- 스팟 EC2 인스턴스로 비용 절감 가능 (데이터 보관 필요X, 일시적인 많은 리소스가 필요한 경우, 근데 ON/OFF 불가..)

<br/>

> 인스턴스 생성

- AMI: Amazon Linux2 AMI 선택
- t2.medium 선택
- 인스턴스 개수 7개 변경
- 스팟 인스턴스 사용하려면 스팟 인스턴스 요청 체크박스 체크
- 유형: 모든 트패픽, 모든 TCP
- 포트범위: 0-65535 설정
- 보안 그룹 이름: peter-sg-kafka (실습 기준)
- 키페어 생성

<br/>

### 2.1.2 온프레미스 환경에서 실습 환경 구성
- 7대의 서버와 DNS 서버 필요
- 구성은 위와 동일

<br/>

---

## 2.2 카프카 클러스터 구성
- 클러스터 환경을 기본으로 구성
- 카프카 클러스터 실습 환경 정보 (앤서블, 주키퍼, 카프카 설치 필요)

  <img width="400" alt="img" src="https://github.com/mash-up-kr/S3A/assets/55437339/d1f0483f-c871-4b87-ad24-a30949431a53" />

<br/>

---

## 2.3 5분 만에 카프카 맛보기

### 2.3.1 카프카의 기본 구성

<img width="500" alt="img" src="https://github.com/mash-up-kr/S3A/assets/55437339/bb95d709-acb2-450c-8b6d-fa642a5779b1" />

🔼 카프카 기본 구성도
- 카프카는 데이터를 받아서 전달하는 데이터 버스(data bus)의 역할을 수행한다.
- 프로듀서(producer) : 데이터(메시지)를 만들어서 주는 쪽
- 컨슈머(consumer) : 카프카에서 데이터(메시지)를 빼내서 소비하는 쪽
- 주키퍼 : 카프카의 정상 동작을 보장하기 위해 메타데이터(metadata)를 관리하는 코디네이터
- 카프카는 producer와 consumer 중앙에 위치하여, 전달받은 메시지들을 저장하고, 전달하는 2가지 역할을 수행한다.
- 카프카의 메타데이터 저장소로 주키퍼를 사용하며, 브로커들의 노드 관리 등을 하고 있다.

<br/>

### 2.3.2 메시지 보내고 받기
- 메시지를 전송하기 위해 가장 먼저 **토픽을 생성**해야 한다.
- producer가 메시지를 특정 토픽으로 전송하면, consumer가 메시지를 가져온다.

<br/>

> 토픽 생성 (peter-overview01 토픽 생성)

```
$ /usr/local/kafka/bin/kafka-topics.sh --bootstrap-server peter-kafka01.foo.bar:9092 --create --topic peter-overview01 --partitions 1 --replication-factor 3
```

<br/>

> 컨슈머 실행

```
$ /usr/local/kafka/bin/kafka-console-consumer.sh --bootstrap-server peter-kafka01.foo.bar:9092 --topic peter-overview01
```

<br/>

> 프로듀서 실행

```
$ /usr/local/kafka/bin/kafka-console-producer.sh --bootstrap-server peter-kafka01.foo.bar:9092 --topic peter-overview01
```
- 실행 후 명령 프롬프트(>)가 나타나면 메시지를 입력한다.
- 입력 후 엔터를 누르면 컨슈머를 실행한 cmd 창에서 입력한 메시지가 출력되는 것을 확인할 수 있다.
