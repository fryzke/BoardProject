# 🛠️ Forum Board Backend (`forum_be`)

Spring Boot 3 / 4 및 Java 21 기반의 포럼/게시판 RESTful API 백엔드 서버입니다.

---

## 📌 주요 기술 스택 (Tech Stack)

- **Language & JDK**: Java 21
- **Framework**: Spring Boot 3.4.x / 4.x
- **Build Tool**: Gradle
- **Database & Persistence**:
  - MySQL 8.x
  - Spring Data JPA & Hibernate
  - QueryDSL 7.0 (동적 쿼리 및 다중 키워드 AND 정확도 검색)
  - Soft Delete (`@SQLDelete`, `@SQLRestriction`)
- **Caching & In-Memory**:
  - Redis (`spring-boot-starter-data-redis`, `lettuce`)
  - Refresh Token TTL 관리 & 토큰 블랙리스트
  - Redis Lua 스크립트 기반 Token Bucket 처리율 제한 (Rate Limiting)
- **Security & Authentication**:
  - Spring Security (Stateless 세션 정책)
  - JWT (JSON Web Token - `jjwt 0.11.5`)
  - BCrypt Password Encoder
  - HttpOnly & Secure 쿠키 기반 토큰 전송
- **Architecture & Advanced Features**:
  - Spring Retry & AOP (동시성 재시도)
  - Spring Async (`@Async`, `ThreadPoolTaskExecutor` 비동기 등급 산정)
  - ShallowEtagHeaderFilter (HTTP ETag 304 캐싱)
  - Spring Scheduled (고아 파일 자동 정리 배치 - 매일 새벽 3시)
- **Testing**:
  - JUnit 5 & Mockito (Controller 단위/슬라이스 테스트)
  - `@SpringBootTest` & `MockMvc` 기반 전 계층 엔드투엔드 통합 테스트 (`integration/`)

---

## 🏛️ 아키텍처 및 핵심 설계

1. **Redis 기반 Refresh Token & 세션 보안 관리**
   - Refresh Token을 Redis에 TTL과 함께 저장하여 탈취 방지 및 빠른 만료 처리
   - 로그아웃 시 Access Token을 Redis 블랙리스트에 등록하여 잔여 시간 동안 재사용 차단
2. **QueryDSL 기반 다중 키워드 AND 정확도 검색**
   - 사용자 입력 키워드 배열에 대해 `BooleanBuilder`를 활용한 동적 쿼리로 모든 키워드를 만족하는 정확도 높은 검색 결과 제공
3. **커스텀 Bean Validation & 보안 에러 핸들링**
   - 커스텀 어노테이션(`@ValidUserId`, `@ValidUserName`, `@ValidPostTitle`, `@ValidPostContent`, `@ValidCommentContent` 등)
   - 전역 예외 처리(`@RestControllerAdvice`)로 내부 인프라/스택 트레이스 노출 원천 차단
4. **비동기 등급(Grade) 산정 시스템**
   - 사용자의 게시글/댓글 작성 및 삭제 시 `@Async` 비동기 워커를 통해 활동량을 집계하고 BRONZE / SILVER / GOLD 등급 자동 갱신
5. **파일 업로드 & 고아 파일 정리 파이프라인**
   - 파일 유효성 검증(확장자, 단건 2MB, 총합 10MB, 최대 10개) 및 메타데이터 관리
   - 매일 배치 스케줄러(`FileCleanupScheduler`)를 통해 게시글과 연결되지 않은 임시 파일 자동 정리
6. **Token Bucket 알고리즘 기반 처리율 제한 (Rate Limiting)**
   - Redis Lua 스크립트를 활용하여 엔드포인트별 트래픽 폭주 및 DoS 공격 방어

---

## 📂 디렉토리 구조 (Directory Structure)

```text
src/
├── main/java/com/example/forum/
│   ├── annotation/       # 커스텀 어노테이션 (@RateLimit 등)
│   ├── config/           # Security, Redis, Async, ETag, QueryDSL, Web 설정
│   ├── controller/       # REST API 컨트롤러
│   ├── domain/           # JPA 엔티티 및 Enum (User, Post, Comment, File, Grade, Role)
│   ├── dto/              # 요청/응답 DTO (공통 ApiResponse, RestPage 등)
│   ├── event/            # 비동기 이벤트 리스너 (FileDeleteEvent 등)
│   ├── exception/        # 전역 예외 처리기 (GlobalExceptionHandler)
│   ├── interceptor/      # 처리율 제한 인터셉터 (RateLimitInterceptor)
│   ├── repository/       # JPA 및 QueryDSL 커스텀 레포지토리
│   ├── scheduler/        # 파일/유저 정리 스케줄러 (FileCleanupScheduler 등)
│   ├── security/         # JWT 토큰 프로바이더 및 인증 필터
│   ├── service/          # 비즈니스 로직 계층 (Auth, User, Post, Comment, File, RateLimit)
│   ├── utils/            # HTML 파서 및 서블릿 유틸
│   └── validator/        # Bean Validation 커스텀 검증기
└── test/java/com/example/forum/
    ├── annotation/       # 테스트용 커스텀 모의 인증 유저 어노테이션
    ├── controller/       # Controller 단위/슬라이스 테스트 (MockMvc)
    └── integration/      # 전 계층 통합 테스트 (AbstractIntegrationTest 기반)
        ├── AuthIntegrationTest.java
        ├── UserIntegrationTest.java
        ├── PostIntegrationTest.java
        ├── CommentIntegrationTest.java
        └── FileIntegrationTest.java
```

---

## ⚙️ 환경 변수 설정 (Environment Variables)

`forum_be/.env.example` 파일을 복사하여 `.env` 또는 `application.properties`에 환경변수를 설정합니다.

| 환경변수명 | 기본값 | 설명 |
|---|---|---|
| `SERVER_PORT` | `8080` | 서버 구동 포트 |
| `DB_URL` | `jdbc:mysql://localhost:3306/forum_db?...` | MySQL JDBC 접속 URL |
| `DB_USERNAME` | `root` | 데이터베이스 사용자 계정 |
| `DB_PASSWORD` | `root` | 데이터베이스 비밀번호 |
| `REDIS_HOST` | `localhost` | Redis 호스트 |
| `REDIS_PORT` | `6379` | Redis 포트 |
| `JWT_SECRET` | 32자 이상 비밀키 | Base64/문자열 JWT 비밀키 |
| `JWT_EXPIRATION` | `1800000` (30분) | Access Token 유효 시간(ms) |
| `JWT_REFRESH_EXPIRATION` | `604800000` (7일) | Refresh Token 유효 시간(ms) |
| `FILE_UPLOAD_DIR` | `uploads` | 물리 파일 저장 기본 디렉토리 |
| `FILE_MAX_SINGLE_SIZE` | `2097152` (2MB) | 단건 파일 최대 크기 (Bytes) |
| `FILE_MAX_TOTAL_SIZE` | `10485760` (10MB) | 게시글 당 총 파일 최대 크기 (Bytes) |
| `FILE_MAX_COUNT` | `10` | 최대 파일 첨부 가능 수 |

---

## 🚀 실행 및 테스트 방법 (Getting Started & Testing)

### 1. 사전 요구 사항
- MySQL 8.x 서버 실행 (데이터베이스명: `forum_db`)
- Redis 서버 실행 (기본 포트 `6379`)

### 2. 빌드 및 테스트 실행
```bash
# 단위 테스트 및 전 계층 통합 테스트 실행
./gradlew test

# 애플리케이션 서버 실행
./gradlew bootRun
```

---

## 📋 REST API 엔드포인트 명세

### 1. 인증 (`/api/auth`)
- `POST /api/auth/signup` : 회원가입
- `POST /api/auth/login` : 로그인 (HttpOnly 쿠키로 토큰 발급)
- `POST /api/auth/reissue` : 토큰 재발급 (Refresh Token 검증)
- `POST /api/auth/logout` : 로그아웃 (토큰 무효화 및 쿠키 만료)

### 2. 회원 (`/api/users`)
- `GET /api/users/me` : 내 프로필 및 활동 통계(게시글/댓글 수, 등급) 조회
- `PUT /api/users/me` : 비밀번호 확인 및 회원 정보(닉네임/비밀번호) 수정
- `DELETE /api/users/withdraw` : 회원 탈퇴 (비밀번호 확인 필수, Soft Delete)

### 3. 게시글 (`/api/posts`)
- `GET /api/posts` : 목록/페이징/검색 조회 (ETag 304 지원)
- `GET /api/posts/{id}` : 게시글 상세 조회 및 조회수 증가
- `POST /api/posts` : 게시글 작성 (비동기 등급 갱신)
- `PUT /api/posts/{id}` : 게시글 수정
- `DELETE /api/posts/{id}` : 게시글 삭제 (Soft Delete)

### 4. 댓글 (`/api/comments/{postId}`)
- `GET /api/comments/{postId}` : 계층형 트리 댓글 목록 조회
- `POST /api/comments/{postId}` : 댓글/대댓글 작성 (비동기 등급 갱신)
- `PUT /api/comments/{postId}/{commentId}` : 댓글 수정
- `DELETE /api/comments/{postId}/{commentId}` : 댓글 삭제 (Soft Delete)

### 5. 파일 (`/api/files`)
- `POST /api/files/upload` : 파일/이미지 단건 및 다중 업로드
- `GET /api/files/{postId}` : 게시글 첨부 파일 목록 조회
- `GET /api/files/download/{fileId}` : 파일 다운로드 (스트림)
- `PUT /api/files/{fileId}` : 파일 교체
- `DELETE /api/files/{fileId}` : 파일 단건 삭제
- `POST /api/files/delete-batch` : 파일 다중 삭제
