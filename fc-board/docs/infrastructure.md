# 인프라 구성

fc-board 프로젝트가 **로컬 개발 → GitHub 푸시 → AWS 배포**까지 어떻게 연결되는지와 각 구성 요소의 역할을 정리.

## 전체 그림

```
┌─────────────────────────┐         ┌────────────────────────────────────┐
│       로컬 (Mac)        │         │              AWS                   │
│                         │         │                                    │
│  Spring Boot App        │         │   ┌─────────────────────────────┐  │
│         │               │         │   │    Elastic Beanstalk env    │  │
│         ▼ JDBC          │         │   │                             │  │
│  ┌──────────────┐       │         │   │   ┌─────┐      ┌────────┐   │  │
│  │ Docker       │       │         │   │   │ EC2 │─JDBC─│  RDS   │   │  │
│  │ (MySQL 8.0)  │       │         │   │   │ JVM │      │ MySQL  │   │  │
│  │ :3306        │       │         │   │   └─────┘      └────────┘   │  │
│  └──────────────┘       │         │   │      ▲                      │  │
│                         │         │   │      │ jar 배포              │  │
└─────────────────────────┘         │   └──────┼──────────────────────┘  │
             │                      │          │                         │
             │ git push             │   ┌──────┴─────┐                   │
             ▼                      │   │ S3 (jar    │                   │
  ┌────────────────────┐            │   │  storage)  │                   │
  │  GitHub            │            │   └────────────┘                   │
  │  ┌──────────────┐  │  deploy    │          ▲                         │
  │  │ Actions      │──┼────────────┼──────────┘                         │
  │  │ (빌드+배포)  │  │            │                                    │
  │  └──────────────┘  │            └────────────────────────────────────┘
  └────────────────────┘
```

---

## 로컬 개발 환경

### Docker (Rancher Desktop)

**역할**: 로컬 머신에서 MySQL 8.0을 격리된 환경에 실행.

**왜 Docker?**
- Mac에 MySQL을 직접 설치하면 OS가 지저분해지고 버전 관리 힘듦
- 팀원/CI가 같은 MySQL 버전을 쓰도록 보장하기 쉬움
- 필요 없으면 `docker stop` 한 번으로 프로세스 정리, 설정도 그대로 보존

**실행한 명령**:
```bash
docker run --name fc-board-mysql \
  -e MYSQL_ROOT_PASSWORD=1234 \
  -p 3306:3306 \
  -d mysql:8.0.32
```

| 옵션 | 의미 |
|------|------|
| `--name` | 컨테이너 이름 지정 (재참조용) |
| `-e MYSQL_ROOT_PASSWORD=1234` | 환경변수로 root 비밀번호 초기화 |
| `-p 3306:3306` | 호스트 3306 포트 → 컨테이너 내부 3306 포워딩 |
| `-d` | 백그라운드(detached) 실행 |
| `mysql:8.0.32` | Docker Hub의 공식 MySQL 이미지 (태그 8.0.32) |

**이미지 vs 컨테이너**
- 이미지 = 읽기 전용 스냅샷(클래스에 비유)
- 컨테이너 = 이미지를 실행한 인스턴스(객체에 비유) + 쓰기 가능한 얇은 레이어

### Spring Profile: `local`

`application.yml`의 `spring.profiles.active: local`이 기본값이라, 로컬 실행 시 자동으로 `application-local.yml` 설정이 적용되어 Docker MySQL에 붙음.

---

## CI/CD: GitHub Actions

### 역할

`main` 브랜치에 push되면 `fc-board/**` 경로 변경 시 워크플로 실행:
1. 코드 체크아웃
2. JDK 17 세팅
3. `./gradlew clean build -x test`로 실행 가능한 jar 빌드 (부트 jar 1개만 — `jar` 태스크 비활성화)
4. `deploy.zip`으로 패키징
5. Elastic Beanstalk API에 업로드 → Beanstalk이 배포 수행

### 관련 파일

- `.github/workflows/blank.yml` — 워크플로 정의
- `fc-board/build.gradle.kts` — `tasks.named<Jar>("jar") { enabled = false }`로 중복 jar 방지

### 크레덴셜

GitHub Actions Secrets에 등록:
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`

> 더 안전한 방법은 OIDC + IAM Role이지만 학습 단계에선 access key도 무방.

---

## AWS 프로덕션 환경

클라우드에서 **항상 떠 있으면서 인터넷에서 접근 가능한** Spring Boot 앱을 운영하는 구성. 세 가지 서비스가 협업한다.

### ① Elastic Beanstalk — "관리형 앱 배포 플랫폼"

**핵심 한 줄**: jar 파일만 주면, EC2 프로비저닝부터 로드밸런서 설정, 무중단 배포, 헬스체크, 로그 수집까지 **자동으로 해주는 관리형 서비스**.

**Beanstalk이 내부적으로 하는 일**:
- **EC2 인스턴스 프로비저닝** (앱이 실제로 도는 컴퓨터)
- 보안 그룹, IAM role, CloudWatch 로그 연결
- (옵션) 로드밸런서 + Auto Scaling Group 구성
- 배포 시 새 버전 jar를 EC2에 올리고 점진적 교체
- 헬스체크 실패 시 알림/롤백

**이 프로젝트의 설정**:
- Application: `fc-board`
- Environment: `Fc-board-env`
- Platform: Corretto 17 (Java 17)
- Region: `ap-northeast-2` (서울)

**Environment Properties** (콘솔 → Configuration에서 설정):

| Key | Value |
|-----|-------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | `jdbc:mysql://<RDS 엔드포인트>:3306/board?useSSL=false` |
| `DB_USERNAME` | `fastcampus` |
| `DB_PASSWORD` | (실제 비번) |

Spring Boot는 환경변수를 자동으로 프로퍼티로 매핑하므로, `application-prod.yml`의 `${DB_URL}` 같은 placeholder가 이 값들로 채워짐.

### ② EC2 — "실제 서버(가상 머신)"

**핵심 한 줄**: AWS 데이터센터 위의 **리눅스 가상 머신 1대**. 여기서 JVM이 돌며 Spring Boot 앱(jar)이 실행됨.

**왜 "내가 설정했다"고 느껴지는가?**
Beanstalk 환경을 만들 때 선택한 항목들은 전부 **Beanstalk이 관리하는 EC2 인스턴스의 스펙**이다:
- **인스턴스 타입** (e.g. `t3.small`): CPU/메모리
- **Key pair**: SSH 접속용 키
- **Security Group**: 방화벽 규칙 (80/443 포트 열기 등)
- **Single Instance vs Load Balancer**: 1대만 띄울지, ALB + Auto Scaling 구성할지

즉, **EC2 자체는 Beanstalk이 알아서 띄우지만, 어떻게 띄울지는 당신이 선택**. EC2 콘솔에 들어가 보면 Beanstalk이 띄워둔 인스턴스가 보일 것.

**관계 요약**:
```
Beanstalk (관리 계층)  ─소유/제어→  EC2 (실행 계층)
```
Beanstalk은 "매니저", EC2는 "실제 일꾼".

### ③ RDS — "관리형 데이터베이스"

**핵심 한 줄**: AWS가 운영해주는 MySQL. DB 서버 구축/백업/패치/모니터링을 다 대신 해줌.

**이 프로젝트의 DB**:
- 엔진: MySQL
- 인스턴스 클래스: (예: `db.t4g.micro` — 프리 티어 가능한 가장 저렴한 등급)
- 엔드포인트: `awseb-e-xxxxxx...rds.amazonaws.com:3306`

**Beanstalk + RDS 연동**:
- Beanstalk 환경 생성 시 "Add database" 옵션으로 RDS를 붙이면, Beanstalk이 자동으로:
  - EC2와 같은 VPC에 RDS 생성
  - EC2 → RDS 통신을 허용하는 보안 그룹 규칙 설정
  - 연결 정보를 `RDS_HOSTNAME`, `RDS_PORT` 등 환경변수로 주입

### 세 서비스의 역할 분담

| 서비스 | 층위 | 역할 |
|--------|------|------|
| Elastic Beanstalk | 오케스트레이션 | 배포/프로비저닝/모니터링 자동화 |
| EC2 | 컴퓨팅 | JVM이 도는 실제 서버 |
| RDS | 데이터 | MySQL 운영 |

> Docker 비유: Beanstalk은 "Docker Compose + 모니터링 도구", EC2는 "내 로컬 머신", RDS는 "내 로컬 MySQL 컨테이너"의 클라우드 버전이라고 생각하면 얼추 비슷.

---

## 로컬 vs 프로덕션 — Spring Profile로 스위칭

같은 jar가 두 환경에서 다르게 동작하는 비결:

```
application.yml            ← 공통 설정 + profiles.active: local (기본값)
application-local.yml      ← 로컬: Docker MySQL (localhost:3306)
application-prod.yml       ← 운영: ${DB_URL} 등 placeholder만
```

- **로컬 실행**: 환경변수 없음 → `profiles.active: local` 기본값 작동 → `application-local.yml` 로드 → Docker MySQL 접속
- **Beanstalk 실행**: `SPRING_PROFILES_ACTIVE=prod` 환경변수로 덮어씀 → `application-prod.yml` 로드 → `${DB_URL}` 등이 Beanstalk Environment Properties 값으로 채워짐 → RDS 접속

**코드에는 운영 비번이 전혀 없음**. 모든 민감값은 AWS 콘솔에서 주입.

---

## 요약: 요청 하나가 처리되는 흐름

```
브라우저 요청
   │
   ▼
[Route 53 / Beanstalk URL]   ← DNS
   │
   ▼
[Load Balancer (있다면)]
   │
   ▼
[EC2 인스턴스]               ← Beanstalk이 프로비저닝
   │
   ▼
[JVM: fc-board.jar]          ← GitHub Actions가 배포
   │   application-prod.yml 로드 (env vars 주입)
   ▼
[RDS MySQL]                  ← 데이터 조회/저장
   │
   ▼
응답
```
