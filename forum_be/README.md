# 🛠️ Forum Board Backend (`forum_be`)

Spring Boot 3 및 Java 21 기반의 포럼/게시판 RESTful API 백엔드 서버입니다.

---

## 📌 주요 기술 스택 (Tech Stack)

- **Language & JDK**: Java 21
- **Framework**: Spring Boot 3.4.x
- **Build Tool**: Gradle
- **Database & Persistence**:
  - MySQL 8.x
  - Spring Data JPA & Hibernate
  - Soft Delete (`@SQLDelete`, `@SQLRestriction`)
- **Caching & Session**: Redis (`spring-boot-starter-data-redis`)
- **Security & Authentication**:
  - Spring Security
  - JWT (JSON Web Token - `jjwt 0.11.5`)
  - BCrypt Password Encoder
- **Etc**:
  - Spring Retry & AOP (동시성 재시도)
  - Spring Async (`@Async`, `ThreadPoolTaskExecutor`)
  - ShallowEtagHeaderFilter (HTTP ETag 304 캐싱)
  - Spring Scheduled (고아 파일 자동 정리 배치)

---

## 🏛️ 아키텍처 및 핵심 설계

1. **Redis 기반 Refresh Token & 세션 관리**
   - Refresh Token을 Redis 인메모리 저장소에 TTL과 함께 저장하여 토큰 탈취 방지 및 빠른 만료 처리
2. **커스텀 Bean Validation 어노테이션**
   - `@ValidPassword`, `@ValidEmail` 등 커스텀 유효성 검증 어노테이션을 통해 DTO 레벨에서 일관된 검증 수행
3. **비동기 등급(Grade) 산정 시스템**
   - 사용자의 게시글/댓글 작성 및 삭제 시 `@Async` 비동기 워커를 통해 BRONZE / SILVER / GOLD 등급 자동 갱신
4. **파일 업로드 & 고아 파일 정리 파이프라인**
   - 파일 유효성 검증(확장자, 용량) 및 메타데이터 관리
   - 매일 배치 스케줄러(`FileCleanupScheduler`)를 통해 게시글과 연결되지 않은 임시 파일 자동 정리

---

## ⚙️ 환경 변수 설정 (Environment Variables)

`forum_be/.env.example` 파일을 복사하여 `.env` 또는 `application.properties`에 환경변수를 설정합니다.

| 환경변수명 | 기본값 | 설명 |
|---|---|---|
| `SERVER_PORT` | `8080` | 서버 구동 포트 |
| `DB_URL` | `jdbc:mysql://localhost:3306/forum_db?...` | MySQL JDBC 접속 URL |
| `DB_USERNAME` | `root` | 데이터베이스 사용자 계정 |
| `DB_PASSWORD` | - | 데이터베이스 비밀번호 |
| `REDIS_HOST` | `localhost` | Redis 호스트 |
| `REDIS_PORT` | `6379` | Redis 포트 |
| `JWT_SECRET` | - | 32자 이상의 Base64/문자열 비밀키 |
| `JWT_EXPIRATION` | `1800000` (30분) | Access Token 유효 시간(ms) |
| `JWT_REFRESH_EXPIRATION` | `604800000` (7일) | Refresh Token 유효 시간(ms) |
| `FILE_UPLOAD_DIR` | `uploads/` | 물리 파일 저장 기본 경로 |

---

## 🚀 실행 방법 (Getting Started)

### 1. 사전 요구 사항
- MySQL 서버 실행 (DB명: `forum_db`)
- Redis 서버 실행 (기본 포트 `6379`)

### 2. 빌드 및 서버 구동
```bash
# Windows
./gradlew bootRun

# Linux / macOS
./gradlew bootRun
```

### 3. 테스트 실행
```bash
./gradlew test
```

---

## 📋 API 명세 요약

- **인증 (`/api/auth`)**: 회원가입(`/signup`), 로그인(`/login`), 토큰재발급(`/reissue`), 로그아웃(`/logout`)
- **회원 (`/api/users`)**: 내 프로필 조회(`GET /me`), 비밀번호 확인 및 정보 수정(`PUT /me`)
- **게시글 (`/api/posts`)**: 목록 조회/페이징/검색(`GET`), 상세 조회(`GET /{id}`), 등록/수정/삭제
- **댓글 (`/api/comments`)**: 계층형 트리 댓글 조회(`GET /{postId}`), 작성/수정/삭제
- **파일 (`/api/files`)**: 파일 업로드(`POST /upload`), 파일 삭제(`DELETE /{fileId}`), 다운로드(`GET /download/{fileId}`)
