# 02. 카프카 설치

주키퍼: 카프카와 직접 통신하며 카프카의 메타데이터 정보를 저장, 카프카 상태 관리

(오프셋 정보는 주키퍼와 카프캬 양쪽에 모두 저장할 수 있지만, 주키퍼에 오프셋을 저장하는 기능은 곳 사라질 예정)

## 카프카 관리를 위한 주키퍼
- 주키퍼: 분산 애플리케이션(ex. 하둡, 카프카)을 위한 코디네이션 시스템

![zkservice](https://zookeeper.apache.org/doc/r3.7.1/images/zkservice.jpg)

- Design Goal ([https://zookeeper.apache.org/doc/r3.7.1/zookeeperOver.html#sc_designGoals](https://zookeeper.apache.org/doc/r3.7.1/zookeeperOver.html#sc_designGoals))
- 분산 애플리케이션이 안정적인 서비스를 할 수 있도록, 각 애플리케이션 정보를 중앙에 집중하여 구성 관리, 그룹 관리 네이밍, 동기화 등 제공 (?)

- 분산 애플리케이션은 클라이언트가 되어 주키퍼 서버와 커넥션을 맺고 상태 정보를 주고 받음
- 상태 정보는 주키퍼 znode에 key-value 형태로 저장
- znode에 저장된 상태 정보를 사용해 분산 애플리케이션 끼리 데이터를 주고 받음
- 각 znode는 데이터 변경 등에 대한 유효성 검사를 위해 버전 번호를 관리 (znode 데이터가 변경될 때 마다 znode 버전 번호가 증가)
- 주키퍼에 저장되는 데이터는 모두 메모리에 저장되어 처리량이 크고 속도가 빠름
- 앙상블(클러스터)로 구성된 주키퍼는 `과반수 방식`에 따라 살아 있는 `노드가 과반수 이상이 유지`된다면 `지속적인 서비스` 가능
- 앙상블 구성 노드 숫자가 많을수록 처리량 상승 (읽기 요청의 비율에 따라 성능이 안좋을 수도..?)

![zkperfRW-3.2](https://zookeeper.apache.org/doc/r3.7.1/images/zkperfRW-3.2.jpg)

## 주키퍼 설치

- 주키퍼의 데이터 디렉토리
    - 스냅샷 (znode 복사본)
    - 트랜잭션 로그
        - znode에 변경 사항이 발생하면 트랜잭션 로그에 추가
        - 로그가 어느 정도 커지면, znode 상태 스냅샷이 파일 시스템에 저장됨
- myid: 앙상블 내 주키퍼 노드를 구분하기 위한 ID (주키퍼 설정 파일 zoo.cfg에서 사용됨)
- 주키퍼 설정 파일 (zoo.cfg)
    - `dataDir`: 트랜잭션 로그, 스냅샷이 저장되는 저장 경로
    - `server.x`: 앙상블 구성을 위한 서버 설정 (`server.{myid} 형식`)

## 카프카 설치

- 주키퍼는 과반수 방식으로 운영되어 홀수로 서버를 구성해야 하지만, 카프카는 홀수 운영 구성을 하지 않아도 됨
- 카프카와 주키퍼는 다른 환경에 구성하는 것이 좋음
    - 카프카는 3대 중 2대 다운되어도 서비스 가능
    - 주키퍼는 3대 중 2대 다운되면 서비스 불가 (과반수 방식)
    - 카프카 클러스터에는 문제가 없더라도 주키퍼와 통신이 되지 않아 카프카도 장애 발생할 수 있음

### 카프카 환경 설정

- 서버 별 브로커 아이디 (`broker.id`)
- 카프카 저장 디렉토리 (`log.dirs`)
    - 컨슈머가 메세지를 가져가더라도 저장된 데이터를 임시로 보관하는 기능이 있음 (디스크 저장)
- 주키퍼 정보 (`zookeeper.connect`)
    - 전체 주키퍼의 리스트를 입력하는 것이 좋음
        - 하나만 입력 시 해당 주키퍼 노드에서 장애 발생 시 카프카 클러스터는 주키퍼 앙상블과 더 이상 통신할 수 없어 장애 발생
    - 주키퍼의 최상위 경로 보다는 znode를 구분해서 사용하는 것이 좋음
        - 최상위 경로를 사용하면 하나의 주키퍼 앙상블 세트와 하나의 애플리케이션만 사용할 수 있음 (서로 다른 애플리케이션에서 동일한 znode를 사용하면 데이터 충돌 발생 가능)
        - znode 를 구분해서 사용하면 주키퍼 앙상블 한 세트로 여러 애플리케이션을 사용할 수 있음
    - 예시
    
    ```
    zookeeper.connect=seongyunkim-zk001:2181,seongyunkim-zk002:2181,seongyunkim-zk003:2181/seongyunkim-kafka01
    zookeeper.connect=seongyunkim-zk001:2181,seongyunkim-zk002:2181,seongyunkim-zk003:2181/seongyunkim-kafka02
    zookeeper.connect=seongyunkim-zk001:2181,seongyunkim-zk002:2181,seongyunkim-zk003:2181/seongyunkim-kafka/01
    zookeeper.connect=seongyunkim-zk001:2181,seongyunkim-zk002:2181,seongyunkim-zk003:2181/seongyunkim-kafka/02
    ```
    

## 카프카 상태 확인

- TCP 포트 확인
    - `netstat -ntlp` 명령어를 사용해 주키퍼 2181 포트, 카프카 9092 포트가 리스닝 중인지 확인
- 주키퍼 znode 를 이용한 카프카 정보 확인
    - 주키퍼 znode 생성 확인
    - 각 znode의 브로커 정보 확인
