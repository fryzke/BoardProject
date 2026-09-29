# 📌 Forum Board Project

풀스택 웹 포럼/게시판 플랫폼 프로젝트입니다.  
**Spring Boot 3 / 4 (Java 21)** 백엔드와 **React 19** 프론트엔드를 기반으로 개발되었으며, JWT 인증, Redis 토큰 관리 및 캐싱, 비동기 회원 등급 산정, 계층형 대댓글(Tree Depth), ETag 기반 HTTP 304 캐싱, 안전한 파일 첨부/다운로드 관리, 전 계층 단위 및 통합 테스트를 완비하였습니다.

---

## 🛠️ 기술 스택 (Tech Stack)

### Backend (`forum_be`)
- **Language & JDK**: Java 21
- **Framework**: Spring Boot 3.4.x / 4.x (Gradle)
- **Database & Persistence**:
  - MySQL 8.x
  - Spring Data JPA & Hibernate
  - QueryDSL 7.0 (동적 쿼리 및 다중 키워드 AND 정확도 검색)
  - Soft Delete (`@SQLDelete`, `@SQLRestriction`)
- **Caching & In-Memory**: Redis (Refresh Token TTL 저장, 토큰 블랙리스트, 중복 조회 방지)
- **Security & Authentication**:
  - Spring Security (Stateless JWT 기반 인증)
  - JWT (jjwt 0.11.5) & HttpOnly 쿠키 전송
  - BCrypt Password Encoder
- **Architecture & Advanced Features**:
  - `@Async` 기반 비동기 등급 산정 (`ThreadPoolTaskExecutor`)
  - `ShallowEtagHeaderFilter`를 활용한 ETag 기반 HTTP 304 캐싱
  - Spring AOP & Spring Retry (동시성 재시도 처리)
  - Redis Token Bucket 기반 처리율 제한 (Rate Limiting)
  - Spring Scheduled 배치 스케줄러 (고아 파일 및 삭제 데이터 자동 정리)
- **Testing**:
  - JUnit 5 & Mockito (Controller 단위/슬라이스 테스트)
  - `@SpringBootTest` & `MockMvc` 기반 전 계층 엔드투엔드 통합 테스트 (Auth, User, Post, Comment, File)

### Frontend (`forum_fe`)
- **Library & Framework**: React 19, React Router v7
- **Rich Text Editor**: TipTap Rich Text Editor (`@tiptap/react`, `@tiptap/starter-kit`, `@tiptap/extension-image`)
- **Icons & Styling**: Lucide React, CSS Variables 기반 모던 반응형 테마
- **State & Context**: Context API (ToastContext, ModalContext, ProtectedRoute)
- **HTTP Client**: Axios (인터셉터를 통한 Silent Refresh 자동 재발급 및 큐 동기화)
- **Lifecycle & Security**: AbortController 기반 비동기 취소 제어, DOMPurify XSS 방어

---

## 🌟 주요 기능 (Key Features)

### 1. 회원 및 보안 인증 시스템
- **회원가입 / 로그인**:
  - 아이디(4~16자), 비밀번호(영문/숫자/특수문자 포함 8자 이상), 닉네임(2~20자) 엄격한 Bean Validation 및 정규식 검증
  - BCrypt 암호화 및 HttpOnly/Secure 쿠키 기반 JWT Access/Refresh 토큰 발급
- **토큰 무중단 재발급 & 로그아웃**:
  - Axios 응셉터에서 401 감지 시 Refresh Token으로 자동 재발급(Silent Refresh) 후 대기 큐 요청 재시도
  - 로그아웃 시 Redis Refresh Token 삭제 및 Access Token 블랙리스트 등록
- **마이페이지 & 회원정보 수정/탈퇴**:
  - **기존 비밀번호 필수 검증**: 닉네임 또는 비밀번호 변경 시 기존 비밀번호 일치 확인 절차 수행
  - 내 활동 통계(작성 게시글 수, 댓글 수) 실시간 조회
  - 비밀번호 재확인을 통한 안전한 회원 탈퇴(Soft Delete)

### 2. 등급(Grade) 산정 시스템
- 사용자의 활동량(게시글 수, 댓글 수)을 기준으로 등급이 비동기(`@Async`)로 자동 갱신됩니다.
- **등급 기준**:
  - 🥇 **GOLD**: 게시글 20개 이상 **AND** 댓글 50개 이상
  - 🥈 **SILVER**: 게시글 5개 이상 **AND** 댓글 10개 이상
  - 🥉 **BRONZE**: 기본 가입 등급
- 게시글 및 댓글 작성/삭제 시 즉시 등급 산정 이벤트가 비동기 트리거되어 회원 정보에 실시간 반영됩니다.

### 3. 게시판 & 포스트 관리
- **카테고리 분류**: 전체, 공지사항, 자유, 정보 공유, 질문, 후기
- **공지사항 상단 고정(Pin)**: 관리자(ADMIN) 권한으로 최대 5개까지 상단 고정 가능
- **다중 키워드 정확도 검색 (AND 연산)**: QueryDSL 동적 쿼리로 여러 검색어 입력 시 모든 키워드를 만족하는 정확도 중심 검색 지원
- **조회수 및 ETag 캐싱**: Redis 기반 24시간 중복 조회 방지 및 `ETag`를 통한 304 Not Modified 대역폭 최적화
- **리치 텍스트 에디터**: TipTap 에디터 기반 서식 편집, 본문 내 인라인 이미지 삽입 및 파일 첨부 지원

### 4. 계층형 대댓글(Tree Comments)
- **무제한 깊이 방지**: 최대 Depth 제한 (최대 10단계 지원)
- **Soft Delete 적용**: 자식 댓글이 남아있는 부모 댓글 삭제 시 삭제 안내 문구로 계층 구조 보존
- **댓글 길이 제한**: 최대 400자 제한

### 5. 파일 첨부 및 보안 정책
- **개별 파일 용량**: 최대 **2MB**
- **게시글 당 총 파일 용량**: 최대 **10MB**
- **파일 개수 제한**: 최대 **10개**
- **허용 확장자**: `jpg`, `jpeg`, `png`, `gif`, `webp`, `pdf`, `doc`, `docx`, `txt`, `xlsx`, `pptx`, `zip`
- **고아 파일 정리**: 매일 새벽 3시 스케줄러(`FileCleanupScheduler`)를 통해 게시글과 연결되지 않은 임시 파일 자동 정리
- **보안 에러 핸들링**: `@RestControllerAdvice` 전역 예외 처리를 통한 내부 인프라/스택 트레이스 노출 차단

---

## 📂 프로젝트 구조 (Directory Structure)

```text
BoardProject/
├── .env.example              # 루트 통합 환경변수 템플릿
├── README.md                 # 루트 종합 문서
├── forum_be/                 # 백엔드 (Spring Boot)
│   ├── build.gradle
│   ├── .env.example          # 백엔드 환경변수 템플릿
│   ├── src/main/java/com/example/forum/
│   │   ├── annotation/       # 커스텀 어노테이션 (@RateLimit 등)
│   │   ├── config/           # Security, Redis, Async, ETag, QueryDSL 설정
│   │   ├── controller/       # REST API 컨트롤러
│   │   ├── domain/           # JPA 엔티티 및 Enum (User, Post, Comment, File, Grade, Role)
│   │   ├── dto/              # 요청/응답 DTO
│   │   ├── event/            # 비동기 이벤트 리스너
│   │   ├── exception/        # 전역 예외 처리기 (GlobalExceptionHandler)
│   │   ├── interceptor/      # 처리율 제한 인터셉터 (RateLimitInterceptor)
│   │   ├── repository/       # JPA 및 QueryDSL 커스텀 레포지토리
│   │   ├── scheduler/        # 파일/유저 정리 스케줄러
│   │   ├── security/         # JWT 토큰 프로바이더 및 인증 필터
│   │   ├── service/          # 비즈니스 로직 계층
│   │   ├── utils/            # HTML/서블릿 유틸
│   │   └── validator/        # Bean Validation 커스텀 검증기
│   └── src/test/java/com/example/forum/
│       ├── controller/       # Controller 단위/슬라이스 테스트
│       └── integration/      # 전 계층 통합 테스트 (Auth, User, Post, Comment, File)
└── forum_fe/                 # 프론트엔드 (React)
    ├── package.json
    ├── .env.example          # 프론트엔드 환경변수 템플릿
    └── src/
        ├── Components/       # Modal, Toast, SearchBar 공통 컴포넌트
        ├── Pages/
        │   ├── Forum/        # 게시글 목록, 필터링, 페이지네이션
        │   ├── Post/         # 게시글 상세, 에디터, 계층형 댓글, 첨부파일 카드
        │   ├── SearchResult/ # 검색 결과 페이지
        │   ├── MyPage/       # 마이페이지, 프로필 수정, 회원 탈퇴
        │   ├── SignIn/       # 로그인 페이지
        │   └── SignUp/       # 회원가입 페이지
        ├── api.js            # Axios 인스턴스, Silent Refresh 인터셉터 및 API 호출 모음
        ├── enum.js           # 프론트엔드 상수 및 유효성 검증 규칙
        ├── ProtectedRoutes.js# 보호된 라우트 인증 가드
        └── utils.js          # 날짜 포맷, 파일 크기 포맷, 다운로드 유틸 함수
```

---

## ⚙️ 환경 변수 설정 (Environment Variables)

### 1. Backend (`forum_be/.env` 또는 `application.properties`)

| 변수명 | 기본값 | 설명 |
|---|---|---|
| `SERVER_PORT` | `8080` | 백엔드 서버 포트 |
| `DB_URL` | `jdbc:mysql://localhost:3306/forum_db?...` | MySQL 연결 JDBC URL |
| `DB_USERNAME` | `root` | DB 사용자명 |
| `DB_PASSWORD` | `root` | DB 비밀번호 |
| `REDIS_HOST` | `localhost` | Redis 호스트 |
| `REDIS_PORT` | `6379` | Redis 포트 |
| `JWT_SECRET` | 32자 이상 비밀키 | JWT 서명 비밀키 |
| `JWT_EXPIRATION` | `1800000` (30분) | Access Token 유효 시간 (ms) |
| `JWT_REFRESH_EXPIRATION` | `604800000` (7일) | Refresh Token 유효 시간 (ms) |
| `FILE_MAX_SINGLE_SIZE` | `2097152` (2MB) | 개별 파일 최대 용량 (Bytes) |
| `FILE_MAX_TOTAL_SIZE` | `10485760` (10MB) | 게시글 당 총 파일 최대 용량 (Bytes) |
| `FILE_MAX_COUNT` | `10` | 최대 첨부 가능 파일 수 |

### 2. Frontend (`forum_fe/.env`)

| 변수명 | 기본값 | 설명 |
|---|---|---|
| `REACT_APP_API_BASE_URL` | `http://localhost:8080/api` | 백엔드 REST API 엔드포인트 기본 URL |

---

## 🚀 실행 및 테스트 방법 (Getting Started & Testing)

### 1. 인프라 실행 (MySQL & Redis)
- MySQL: 3306 포트 (`forum_db` 데이터베이스)
- Redis: 6379 포트

### 2. Backend 실행 및 테스트
```bash
cd forum_be

# 단위 및 통합 테스트 전체 실행
./gradlew test

# 백엔드 서버 구동
./gradlew bootRun
```
> 백엔드 서버는 `http://localhost:8080`에서 실행됩니다.

### 3. Frontend 실행 및 빌드
```bash
cd forum_fe

# 의존성 패키지 설치
npm install

# 개발 서버 실행
npm start

# 프로덕션 빌드
npm run build
```
> 프론트엔드 개발 서버는 `http://localhost:3000`에서 실행됩니다.

---

## 📋 REST API 엔드포인트 요약

### 인증 (`/api/auth`)
- `POST /api/auth/signup` : 회원가입
- `POST /api/auth/login` : 로그인 (JWT 쿠키 발급 및 사용자 기본 정보 반환)
- `POST /api/auth/reissue` : Access/Refresh Token 재발급
- `POST /api/auth/logout` : 로그아웃 (토큰 무효화 및 쿠키 만료)

### 회원 (`/api/users`)
- `GET /api/users/me` : 현재 로그인 사용자 정보 및 활동 통계(게시글 수, 댓글 수) 조회
- `PUT /api/users/me` : 회원 정보 및 비밀번호 수정 (**기존 비밀번호 검증 필수**)
- `DELETE /api/users/withdraw` : 회원 탈퇴 (**비밀번호 검증 필수**, Soft Delete)

### 게시글 (`/api/posts`)
- `GET /api/posts` : 게시글 목록/검색 조회 (카테고리 필터링, 정렬, 페이징, ETag 지원)
- `GET /api/posts/{id}` : 게시글 상세 조회 및 조회수 증가
- `POST /api/posts` : 게시글 작성 (비동기 등급 산정 트리거)
- `PUT /api/posts/{id}` : 게시글 수정
- `DELETE /api/posts/{id}` : 게시글 삭제 (Soft Delete & 비동기 등급 산정 트리거)

### 댓글 (`/api/comments`)
- `GET /api/comments/{postId}` : 게시글별 계층형 댓글 트리 목록 조회
- `POST /api/comments/{postId}` : 댓글/대댓글 작성 (비동기 등급 산정 트리거)
- `PUT /api/comments/{postId}/{commentId}` : 댓글 수정
- `DELETE /api/comments/{postId}/{commentId}` : 댓글 삭제 (Soft Delete & 비동기 등급 산정 트리거)

### 파일 (`/api/files`)
- `POST /api/files/upload` : 단건/다중 파일 및 이미지 업로드
- `GET /api/files/{postId}` : 특정 게시글 첨부 파일 목록 조회
- `GET /api/files/download/{fileId}` : 파일 다운로드 (스트림 전송)
- `PUT /api/files/{fileId}` : 파일 교체
- `DELETE /api/files/{fileId}` : 파일 단건 삭제
- `POST /api/files/delete-batch` : 파일 다중 일괄 삭제
