# 학습노트 02 — `it` 문법과 DTO 마다 확장 함수

조회(R) 추가하면서 도입된 패턴들. 코틀린 처음 보면 어색한 부분만 모음.

## 1. `it` 이 뭐임?

람다(lambda — 익명 함수)에 **파라미터가 하나일 때 자동으로 붙는 이름**. 그게 전부임.

```kotlin
// 풀어쓴 형태
list.map { item -> item.toResponse() }

// 파라미터가 하나면 이름 생략 가능 → 자동으로 `it`
list.map { it.toResponse() }
```

이번 변경에서 `it` 이 등장하는 자리들:

### (1) `Page<...>.toResponse()` — `map` 안에서

```kotlin
fun Page<PostSummaryResponseDto>.toResponse() =
  PageImpl(
    content.map { it.toResponse() },   // it = PostSummaryResponseDto 한 개
    pageable,
    totalElements,
  )
```

`content` 는 `List<PostSummaryResponseDto>`. `map` 은 리스트의 각 원소마다 `{ }` 블록을 실행해서 **다른 리스트로 변환**. `it` 은 그때그때의 원소 하나.

### (2) `?.let { ... }` — null 안전 호출 + 람다

```kotlin
postSearchRequestDto.title?.let { post.title.contains(it) }
//                       ↑                            ↑
//                  null 이면 통째로 null      it = title (non-null)
```

읽는 법: "title 이 null 이 아니면 `let` 블록을 실행해. 블록 안에서 `it` 은 non-null 인 title 값."

이걸 풀어 쓰면:
```kotlin
if (postSearchRequestDto.title != null) {
  post.title.contains(postSearchRequestDto.title!!)
} else {
  null
}
```

QueryDSL `where(...)` 는 **null 이 들어오면 그 조건을 무시**해주기 때문에, 검색어가 없으면 자연스럽게 조건이 빠짐. `?.let { }` 은 "있으면 조건 추가, 없으면 null → 무시" 를 한 줄로 표현하는 코틀린의 관용구.

### 추가 팁 — `it` 을 쓸지 말지
- 람다가 짧고 의미가 명확하면 `it` 으로 충분
- 중첩 람다거나 의미를 분명히 하고 싶으면 `{ post -> ... }` 처럼 이름 지정
- 한 람다 안에서 두 번 이상 쓰면 차라리 이름을 지정하는 게 가독성 좋음

---

## 2. 왜 DTO 파일마다 확장 함수가 따로 있음?

이번 커밋의 DTO 들과 거기 같이 정의된 확장 함수:

| 파일 | 확장 함수 | 변환 방향 |
|---|---|---|
| `controller/dto/PostCreateRequest.kt` | `PostCreateRequest.toDto()` | controller DTO → service DTO |
| `controller/dto/PostUpdateRequest.kt` | `PostUpdateRequest.toDto()` | controller DTO → service DTO |
| `controller/dto/PostSearchRequest.kt` | `PostSearchRequest.toDto()` | controller DTO → service DTO |
| `controller/dto/PostDetailResponse.kt` | `PostDetailResponseDto.toResponse()` | service DTO → controller DTO |
| `controller/dto/PostSummaryResponse.kt` | `PostSummaryResponseDto.toResponse()` | service DTO → controller DTO |
| `service/dto/PostCreateRequestDto.kt` | `PostCreateRequestDto.toEntity()` | service DTO → domain(Post) |
| `service/dto/PostDetailResponseDto.kt` | `Post.toDetailResponseDto()` | domain → service DTO |
| `service/dto/PostSummaryResponseDto.kt` | `Post.toSummaryResponseDto()` | domain → service DTO |

### 먼저, 확장 함수(extension function)가 뭐임?

```kotlin
fun PostCreateRequest.toDto() = PostCreateRequestDto(...)
//  ↑                 ↑
//  대상 클래스        새로 추가하는 함수처럼 쓸 수 있음
```

`PostCreateRequest` 클래스 안을 안 건드리고, **밖에서 마치 그 클래스의 메서드인 것처럼** 함수를 추가하는 문법. 호출은 `request.toDto()` 처럼 평범한 메서드 호출.

내부적으로는 그냥 `PostCreateRequest` 를 첫 번째 파라미터로 받는 일반 함수와 똑같음. 읽기 좋게 만든 문법 사탕(syntactic sugar).

### 그럼 왜 layer 마다 DTO 를 다르게 두고, 변환 함수가 layer 사이에 있냐?

먼저 이 프로젝트가 가진 3개의 모델을 정리하자.

```
[ HTTP 요청/응답 ]   [ 비즈니스 로직 ]   [ DB 테이블 ]
       │                    │                  │
PostCreateRequest    PostCreateRequestDto      Post
PostDetailResponse   PostDetailResponseDto    (Entity)
   ...                  ...
   ↑                    ↑                      ↑
controller/dto       service/dto            domain/
```

### 왜 분리하는지 — 짧은 답

**각 레이어가 바뀌는 이유가 다르기 때문.**

- `controller/dto`: HTTP API 스펙. 클라이언트와의 약속 (필드 이름, JSON 형식). API 버전 바뀌면 여기가 바뀜
- `service/dto`: 서비스 메서드의 입출력. 컨트롤러가 늘어도 여기는 안 바뀜
- `domain (Post)`: DB 매핑된 엔티티. JPA 가 관리. 함부로 바깥에 노출하면 위험

엔티티(`Post`)를 컨트롤러까지 그대로 들고 가면:
1. JPA 의 lazy 필드가 직렬화될 때 N+1 같은 사고가 남
2. 클라이언트한테 보여주면 안 되는 필드(예: 비밀번호 해시)도 같이 새어 나감
3. API 스펙 바꾸려고 DB 컬럼을 바꾸는 본말전도가 일어남

### 왜 변환 함수를 그 DTO 가 정의된 파일 옆에 두냐?

확장 함수의 위치 관습:
- **변환 결과 타입이 정의된 파일** 에 두는 게 일반적
- 예: `Post.toSummaryResponseDto()` 는 `PostSummaryResponseDto.kt` 에 있음. 이유는 "Summary 가 어떻게 만들어지는가" 는 `PostSummaryResponseDto` 의 관심사라서
- `PostCreateRequest.toDto()` 가 `PostCreateRequest.kt` 에 있는 건 약간 예외인데, **컨트롤러 레이어가 서비스 DTO 를 알아도 되지만 그 반대는 안 된다** 는 의존 방향 때문에 컨트롤러 쪽에 두는 게 맞음

> **의존 방향 룰**: controller → service → domain (한쪽만). 역방향 import 가 생기면 레이어 분리가 깨진 신호.

### 같은 정보를 3번이나 적는 게 비효율 아님?

겹쳐 보이지만, 각자가 보호하는 경계가 다름:
- `PostCreateRequest.createdBy: String` 이 어느 날 `creator: User` 로 바뀌어도, `PostCreateRequestDto.createdBy: String` 은 그대로 둘 수 있음 → `toDto()` 안에서 흡수
- 반대로 DB 컬럼명이 바뀌어도 API 스펙은 안 흔들림

작은 프로젝트에선 과해 보이지만, 변경이 잦아지면 이 분리가 빛을 발함.

---

## 3. 정리 — 한 번에 보는 흐름

```
HTTP 요청 (JSON)
  ↓ Spring 이 deserialize
PostCreateRequest                ← controller/dto
  ↓ .toDto()
PostCreateRequestDto             ← service/dto
  ↓ .toEntity()
Post (Entity)                    ← domain
  ↓ repository.save()
DB

────── 응답은 반대 방향 ──────

DB → Post → .toDetailResponseDto() → PostDetailResponseDto
                                       ↓ .toResponse()
                                     PostDetailResponse → JSON 응답
```

지금은 `it`, `?.let`, 확장 함수, layer 별 DTO 가 한꺼번에 등장해서 어지럽지만, **각각은 작은 도구** 임. 한 번 손에 익으면 코틀린/스프링 코드 거의 모든 자리에서 같은 모양으로 반복됨.
