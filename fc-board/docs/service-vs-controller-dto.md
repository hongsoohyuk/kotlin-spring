# Service DTO vs Controller DTO

`controller dto`와 `service dto`는 비슷해 보여도 **책임과 변경 이유가 다릅니다**.

## 한 줄 차이

- `controller dto`: HTTP 요청/응답(JSON) 스펙을 표현하는 DTO
- `service dto`: 서비스 계층의 유즈케이스 입력/출력을 표현하는 DTO

## 왜 둘 다 필요한가?

레이어별로 바뀌는 이유가 다르기 때문입니다.

- API 스펙이 바뀌면 `controller dto`가 먼저 바뀜
- 비즈니스 유즈케이스가 바뀌면 `service dto`가 바뀜
- DB/JPA 구조가 바뀌면 `domain entity`가 바뀜

이 경계를 분리하면 한 레이어 변경이 다른 레이어로 전염되는 것을 줄일 수 있습니다.

## 이 프로젝트 기준 실제 흐름

### Create 요청

1. `PostController`가 `PostCreateRequest`(controller dto)를 받음
2. `postCreateRequest.toDto()`로 `PostCreateRequestDto`(service dto)로 변환
3. `PostService.createPost()`에서 service dto를 사용
4. `requestDto.toEntity()`로 `Post`(domain entity) 생성 후 저장

### 조회 응답

1. `PostService`에서 `Post` entity를 조회
2. `toDetailResponseDto()`로 `PostDetailResponseDto`(service dto) 생성
3. 컨트롤러에서 `toResponse()`로 `PostDetailResponse`(controller dto) 생성
4. HTTP JSON 응답 반환

## 의존 방향(중요)

권장 의존 방향은 아래와 같습니다.

`controller -> service -> domain`

- `controller dto`는 `service dto`를 알아도 됨 (변환 필요)
- `service`/`domain`이 `controller dto`를 알면 안 됨

이 규칙 덕분에 웹 계층(REST, JSON, 검증 어노테이션 등)을 바꿔도 서비스 로직 영향이 줄어듭니다.

## 실무 규칙으로 정리

- 외부 입출력(HTTP) 모델은 `controller/dto`에 둔다
- 서비스 메서드 시그니처는 `service/dto`로 고정한다
- entity는 controller로 직접 노출하지 않는다
- 레이어 경계에서만 변환한다 (`toDto()`, `toResponse()`, `toEntity()`)

## 자주 나오는 질문

### 필드가 거의 같은데 DTO를 합치면 안 되나?

작은 예제에서는 합쳐도 동작합니다. 다만 요구사항이 늘어날수록
"API 변경", "비즈니스 변경", "DB 변경"의 속도가 달라져서 결국 분리 비용보다 유지보수 이득이 커집니다.

### 변환 코드가 반복되지 않나?

반복됩니다. 대신 경계가 명확해지고 변경 영향 범위를 예측하기 쉬워집니다.
이 프로젝트처럼 확장 함수(`toDto`, `toResponse`, `toEntity`)로 묶으면 반복 비용을 꽤 줄일 수 있습니다.
