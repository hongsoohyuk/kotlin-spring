# 로컬 DB + Rancher 정리

이 디렉토리는 로컬 개발 시 자주 헷갈리는 두 가지를 정리한다.

- Docker로 MySQL 로컬 DB 띄우기
- Rancher Desktop에서 보이는 이미지 목록 기준

---

## 1) 로컬 MySQL을 Docker로 실행

현재 `application-local.yml` 기준 DB 연결 정보:

- URL: `jdbc:mysql://localhost:3306/board?useSSL=false&allowPublicKeyRetrieval=true`
- USER: `root`
- PASSWORD: `1234`
- DB NAME: `board`

실행 명령:

```bash
docker run -d \
  --name fc-board-mysql \
  -e MYSQL_ROOT_PASSWORD=1234 \
  -e MYSQL_DATABASE=board \
  -p 3306:3306 \
  mysql:8.0
```

상태 확인:

```bash
docker ps
docker logs -f fc-board-mysql
```

로그에 `ready for connections`가 보이면 애플리케이션에서 접속 가능하다.

중지/재시작:

```bash
docker stop fc-board-mysql
docker start fc-board-mysql
```

삭제(데이터도 함께 사라짐):

```bash
docker rm -f fc-board-mysql
```

---

## 2) Rancher Desktop 이미지 목록은 무엇을 기준으로 보이나?

핵심은 "Rancher에 등록" 개념보다, **현재 런타임의 로컬 이미지 캐시를 보여준다**는 점이다.

- 이미지 원본 위치: `docker.io`, `ghcr.io`, `ecr` 같은 레지스트리
- Rancher에 보이는 것: 레지스트리에서 **내 머신으로 pull된 결과**
- 왜 MySQL이 보이냐: `docker run mysql:8.0` 수행 시 이미지가 pull되어 로컬에 저장됨

### 런타임에 따라 목록이 다른 이유

Rancher Desktop은 내부적으로 `containerd`를 많이 사용한다.

- `docker` CLI로 pull/run한 목록
- `nerdctl` CLI로 pull/run한 목록

이 둘이 런타임/네임스페이스 차이로 다르게 보일 수 있다.

### 빠른 점검 명령

```bash
docker images
nerdctl images
nerdctl --namespace k8s.io images
```

위 3개 결과를 비교하면 "어디에서 pull된 이미지인지"를 대부분 확인할 수 있다.

---

## 3) 자주 겪는 이슈

- 포트 충돌: `3306`을 이미 다른 MySQL이 사용 중이면 컨테이너가 바로 종료될 수 있음
- 인증 오류: 앱 비밀번호와 컨테이너 비밀번호 불일치
- 접속 지연: 컨테이너 시작 직후에는 DB 초기화 때문에 수 초 대기 필요

문제 발생 시 먼저 `docker ps`, `docker logs fc-board-mysql`부터 확인한다.

---

## 4) "이미지는 보이는데 실행 중 컨테이너가 안 보여요"

이 경우는 대부분 아래 둘 중 하나다.

1. 이미지는 pull됐지만 컨테이너를 아직 실행하지 않음  
2. 컨테이너가 실행 직후 에러로 종료됨

### 확인 순서

```bash
# 실행 중 컨테이너
docker ps

# 종료된 것까지 포함
docker ps -a

# mysql 컨테이너 로그 확인
docker logs fc-board-mysql
```

### 다시 실행

컨테이너가 없으면 새로 생성:

```bash
docker run -d \
  --name fc-board-mysql \
  -e MYSQL_ROOT_PASSWORD=1234 \
  -e MYSQL_DATABASE=board \
  -p 3306:3306 \
  mysql:8.0
```

컨테이너가 있는데 `Exited` 상태면 재시작:

```bash
docker start fc-board-mysql
```

### Rancher Desktop UI에서 확인할 위치

- `Images` 탭: pull된 이미지 목록만 표시
- `Containers` 탭: 실제 실행/종료된 컨테이너 상태 표시

즉, `Images`에 `mysql:8.0`이 보여도 `Containers`에 `fc-board-mysql`이 없으면 "다운로드만 됐고 실행은 안 된 상태"다.
