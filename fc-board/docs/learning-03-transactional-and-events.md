# 학습노트 03 — `@Transactional` / 이벤트 리스너 / like 리턴이 사라진 이유

Like 기능을 이벤트로 분리하면서 헷갈렸던 것들 정리.

관련 코드:

- `LikeService.createLike`
- `LikeEventHandler.handle`
- `LikeController.createLike`

---

## 1. `@Transactional` 이 뭐임?

DB 작업을 **하나의 트랜잭션(묶음)** 으로 묶어주는 Spring 어노테이션.

- 트랜잭션 안에서 여러 SQL이 성공하면 → **커밋** (확정)
- 중간에 예외가 나면 → **롤백** (전부 취소)

```kotlin
@Service
@Transactional(readOnly = true) // 클래스 기본: 읽기 전용
class LikeService(...) {

  // countLike → 조회만 하니까 readOnly 그대로 OK

  fun createLike(...) {
    // 이벤트만 publish. DB write는 여기서 안 함
    applicationEventPublisher.publishEvent(LikeEvent(...))
  }
}
```

### `readOnly = true` 가 중요한 이유

읽기 전용 커넥션에서는 `INSERT/UPDATE/DELETE` 가 막힘.

그래서 Like 저장을 `LikeService` 트랜잭션 안에서 바로 하면 이런 에러가 남:

> Connection is read-only. Queries leading to data modification are not allowed

쓰기 작업은 아래 둘 중 하나가 필요함:

1. 메서드에 `@Transactional` (readOnly 해제)
2. 또는 이벤트 핸들러처럼 **별도 쓰기 트랜잭션**에서 처리

이 프로젝트는 2번 방식.

---

## 2. `@EventListener` vs `@TransactionalEventListener`

둘 다 "이벤트가 발행되면 이 메서드를 실행해" 라는 뜻이지만, **언제** 실행되느냐가 다름.

### `@EventListener`

- `publishEvent()` 호출 직후, **같은 스레드에서 바로** 실행
- 발행자의 트랜잭션 안이라면, 그 트랜잭션을 **그대로 공유**
- Like 케이스에서 문제:
  - `LikeService` 가 `readOnly` 트랜잭션
  - 리스너가 그 안에서 `likeRepository.save()` 시도
  - → read-only 때문에 실패

### `@TransactionalEventListener`

- 트랜잭션의 **특정 단계**에 맞춰 실행
- 기본/자주 쓰는 값: `phase = AFTER_COMMIT` (커밋 **이후**)
- 발행자 트랜잭션이 성공해서 커밋된 뒤에야 리스너 실행
- 트랜잭션이 없으면 (기본 설정상) 이벤트를 **버려버림**  
  → 그래서 발행 쪽에 트랜잭션이 있어야 함 (`LikeService` 클래스의 `@Transactional`)

### 이 프로젝트 선택

```kotlin
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)
fun handle(event: LikeEvent) {
  // Redis 증가 + Like 저장
}
```

| 어노테이션 | 역할 |
|---|---|
| `@TransactionalEventListener(AFTER_COMMIT)` | `createLike` 트랜잭션이 **커밋된 뒤** 실행 |
| `@Transactional(REQUIRES_NEW)` | 핸들러 전용 **새 쓰기 트랜잭션** 시작 |

`REQUIRES_NEW` 가 필요한 이유:

- Spring 규칙상 `@TransactionalEventListener` + `@Transactional` 조합은  
  `REQUIRES_NEW` 또는 `NOT_SUPPORTED` 만 허용
- AFTER_COMMIT 이후에도 Like INSERT를 하려면 새 쓰기 TX가 필요

### 한 줄 요약

- `@EventListener` = **지금 당장**, 같은 트랜잭션 맥락
- `@TransactionalEventListener(AFTER_COMMIT)` = **커밋 성공 후**, 보통 새 트랜잭션에서 후처리

---

## 3. 흐름으로 보기 (Like)

```text
1) POST /posts/{id}/likes
2) LikeController → LikeService.createLike()
3) publishEvent(LikeEvent)
4) createLike 트랜잭션 커밋
5) LikeEventHandler.handle() 실행 (AFTER_COMMIT + REQUIRES_NEW)
6) Redis like count +1
7) likes 테이블 INSERT
```

포인트:

- API 요청 처리(발행)와 실제 저장(후처리)이 **분리**됨
- 클라이언트가 200을 받은 직후 아주 짧은 순간에는  
  핸들러가 아직 안 돌았을 수도 있음 (AFTER_COMMIT 특성)

---

## 4. 리턴을 왜 안 해도 됨?

### 예전

```kotlin
fun createLike(...): Long {
  // save 후 like id 반환
  return likeRepository.save(...).id
}
```

컨트롤러도 `Long` 을 받아서 응답 바디로 내려줬을 가능성 큼.

### 지금

```kotlin
// Controller
fun createLike(...) {
  likeService.createLike(postId, createdBy) // 반환값 없음
}

// Service
fun createLike(...) {
  applicationEventPublisher.publishEvent(LikeEvent(...))
}
```

괜찮은 이유:

1. **HTTP 관점**  
   좋아요 생성 API는 "성공했는지"만 알면 되는 경우가 많음.  
   바디 없이 `200 OK` 면 충분. (필요하면 `201` + Location 도 가능)

2. **책임 분리**  
   이제 Service는 "이벤트 발행"만 하고,  
   실제 `Like` 엔티티 저장은 Handler가 함.  
   저장 결과 id를 Service가 동기적으로 돌려줄 구조가 아님.

3. **AFTER_COMMIT 타이밍**  
   핸들러는 커밋 **이후**에 실행되므로,  
   `createLike()` 가 return 하는 시점에는 아직 Like row / id 가 없을 수 있음.  
   그래서 id를 리턴하는 설계와 잘 안 맞음.

### 언제 리턴이 다시 필요해지나?

- 생성된 like id를 응답으로 꼭 줘야 하면
- 저장 성공을 API 응답 전에 보장해야 하면

이런 요구가 생기면 이벤트 비동기/후처리 대신,  
Service 안에서 바로 저장하고 id를 반환하는 방식이 더 맞음.

---

## 5. 처음에 안 됐던 이유 (트러블슈팅 메모)

1. `LikeEvent`, `LikeEventHandler` 에 `package` 가 없음  
   → Spring 컴포넌트 스캔 대상에서 제외 → 리스너 미등록 → like 저장/카운트 증가 안 됨

2. `@EventListener` + 클래스 `readOnly` 트랜잭션  
   → INSERT 시 read-only 에러

3. Redis 설정 키가 `spring.jpa.cache.redis.*` 로 잘못 nested  
   → `spring.cache.redis.*` 로 수정해야 `RedisConfig` 가 값을 읽음

---

## 체크리스트

- [ ] `@Transactional(readOnly=true)` 클래스에서 write 하면 실패할 수 있다는 걸 안다
- [ ] `@EventListener` 와 `@TransactionalEventListener(AFTER_COMMIT)` 실행 시점 차이를 안다
- [ ] Like API가 id를 안 돌려줘도 되는 이유를 설명할 수 있다
- [ ] "저장이 응답 이후에 일어난다"는 AFTER_COMMIT 트레이드오프를 안다
