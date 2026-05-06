# 학습노트 01 — CUD 구현하면서 헷갈렸던 것들

커밋 `e54daed feat: add cud for posts` 기준으로 정리.

## 1. `@Transactional` 이 뭐임?

DB 작업을 **트랜잭션** 단위로 묶어주는 Spring 어노테이션. 트랜잭션은 "여러 SQL을 하나의 묶음으로 처리하고, 중간에 하나라도 실패하면 전부 롤백" 하는 단위라고 생각하면 됨.

```kotlin
@Service
@Transactional(readOnly = true)   // 클래스 전체 기본값: 읽기 전용
class PostService(
  private val postRepository: PostRepository,
) {
  @Transactional                  // 메서드 단위로 덮어쓰기: 쓰기 가능
  fun createPost(requestDto: PostCreateRequestDto): Long =
    postRepository.save(requestDto.toEntity()).id
}
```

### 왜 두 번 붙임?
- 클래스에 `@Transactional(readOnly = true)` → 모든 메서드는 **기본적으로 읽기 전용**. 조회만 하면 더 빠르고, 실수로 DB 바꾸는 걸 막아줌
- 쓰기가 필요한 `create / update / delete` 에는 메서드 위에 `@Transactional` 를 다시 달아서 **읽기 전용을 풀어줌**

### 더 중요한 효과: dirty checking
`updatePost` 를 보면 신기한 점이 있음:

```kotlin
@Transactional
fun updatePost(id: Long, requestDto: PostUpdateRequestDto): Long {
  val post = postRepository.findByIdOrNull(id) ?: throw PostNotFoundException()
  post.update(requestDto)         // 객체의 필드만 바꿨음
  return id                       // save() 안 부름!
}
```

`postRepository.save()` 를 안 부르는데도 DB 가 업데이트됨. 이게 JPA + `@Transactional` 의 dirty checking — 트랜잭션 안에서 영속(persistent) 상태인 엔티티의 값이 바뀌면, 트랜잭션이 끝날 때 자동으로 UPDATE SQL을 날려줌. **트랜잭션이 없으면 이 마법이 안 일어남.**

---

## 2. DI (의존성 주입) 이 뭐임?

```kotlin
@RestController
class PostController(
  private val postService: PostService, // ← 이게 DI
)
```

요점만 말하면:
- `PostController` 는 `PostService` 가 **필요함**
- 근데 컨트롤러가 직접 `PostService()` 로 만들지 않음
- Spring 이 `@Service` 붙은 객체를 미리 만들어두고, 컨트롤러를 만들 때 **생성자 파라미터로 넣어줌**

### 왜 굳이 이렇게?
1. **테스트하기 쉬움**: 테스트할 땐 가짜(Mock) PostService 를 끼워 넣을 수 있음
2. **결합도 낮춤**: 컨트롤러는 "PostService 가 필요해" 라고만 선언하지, 어떻게 만드는지 모름
3. **싱글턴**: Spring 이 인스턴스를 한 번만 만들어 재사용

Kotlin 에서는 그냥 **주생성자 파라미터에 `val` 로 받으면 끝**. Java 처럼 `@Autowired` 안 붙여도 됨 (생성자가 하나면 Spring 이 알아서 주입).

---

## 3. `?:` (엘비스 연산자)

```kotlin
val post = postRepository.findByIdOrNull(id) ?: throw PostNotFoundException()
```

읽는 법: "왼쪽이 `null` 이면 오른쪽을 실행해."

- `findByIdOrNull(id)` 가 `Post?` (null 가능) 를 돌려줌
- `null` 이면 `throw` 가 실행됨 → 함수가 거기서 끝남
- `null` 이 아니면 그 값이 `post` 에 들어감. 이 시점부터 `post` 는 **non-null `Post`** 로 타입이 좁혀짐 (smart cast)

자바였으면 `if (post == null) throw ...` 두세 줄로 썼을 걸 한 줄에 끝낸다고 보면 됨.

오른쪽엔 값을 줄 수도 있음:
```kotlin
val name: String = nullable ?: "default"
```

---

## 4. Exception 은 Service 에? Domain(Post) 에?

이 커밋에서 두 군데에 나뉘어 있어서 헷갈렸을 거임.

```kotlin
// Post.kt (도메인) — 자기 자신의 규칙 위반은 자기가 던짐
fun update(postUpdateRequestDto: PostUpdateRequestDto) {
  if (this.createdBy != postUpdateRequestDto.updatedBy) {
    throw PostNotUpdatableException()
  }
  ...
}

// PostService.kt — "이 데이터가 존재하는가?" 같은 흐름 제어는 서비스에서
fun deletePost(id: Long, deletedBy: String): Long {
  val post = postRepository.findByIdOrNull(id) ?: throw PostNotFoundException()
  if (post.createdBy != deletedBy) {
    throw PostNotDeletableException()
  }
  ...
}
```

### 기준 잡기

| 종류 | 어디서 던지나? | 이유 |
|---|---|---|
| **도메인 규칙 위반** (수정 권한 없음, 잘못된 상태 전이 등) | **도메인 클래스** | "Post 가 어떨 때 수정 가능한가" 는 Post 자신이 아는 규칙 |
| **존재하지 않음 / 흐름 제어** (id 로 못 찾음) | **서비스** | repository 에 묻고 결과를 보고 결정하는 건 유스케이스 흐름 |

지금 코드의 일관성 관점에서 한 가지 짚자면, **`PostNotDeletableException` 도 `Post` 도메인에 두는 게 더 일관됨**. 수정도 삭제도 똑같이 "createdBy 가 본인이어야 한다" 는 도메인 규칙이라서.

```kotlin
// 더 일관된 형태 예시
class Post(...) {
  fun delete(deletedBy: String) {
    if (this.createdBy != deletedBy) throw PostNotDeletableException()
  }
}
```

서비스에서는 `post.delete(deletedBy)` 호출 후 `repository.delete(post)` 만 하면 됨.

> **rule of thumb**: "이 규칙은 데이터 자체의 성질인가, 아니면 여러 컴포넌트를 조립하는 흐름인가?" — 전자면 도메인, 후자면 서비스.
