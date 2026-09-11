# 📌 Forum Board Project

현대적인 풀스택 웹 포럼/게시판 플랫폼 프로젝트입니다.  
**Spring Boot 3 (Java 21)** 백엔드와 **React** 프론트엔드를 기반으로 개발되었으며, JWT 인증, Redis 캐싱, 비동기 회원 등급 산정, 계층형 대댓글(Tree Depth), ETag 기반 HTTP 캐싱, 파일 첨부 관리 등의 기능을 제공합니다.

---

## 🛠️ 기술 스택 (Tech Stack)

### Backend (`forum_be`)
- **Language & Framework**: Java 21, Spring Boot 3.4.x (Gradle)
- **Database & ORM**: MySQL 8.x, Spring Data JPA, Hibernate
- **Caching & In-Memory**: Redis (Refresh Token & Data Cache)
- **Security & Auth**: Spring Security, JWT (jjwt 0.11.5), BCrypt Password Encoder
- **Architecture & Features**:
  - `@Async` 기반 비동기 등급 산정 (`ThreadPoolTaskExecutor`)
  - `ShallowEtagHeaderFilter`를 활용한 ETag 기반 HTTP 캐싱
  - Spring AOP & Spring Retry (동시성 재시도 처리)
  - Soft Delete (`@SQLDelete`, `@SQLRestriction`)
  - Spring Scheduled 배치 스케줄러 (고아 파일 및 삭제 데이터 정리)

### Frontend (`forum_fe`)
- **Library & Framework**: React 19, React Router v7
- **Rich Text Editor**: TipTap Editor (`@tiptap/react`, `@tiptap/starter-kit`)
- **Icons & Styling**: Lucide React, CSS Variables 기반 모던 테마
- **State & Context**: Context API (Toast, Modal, Auth Context)
- **HTTP Client**: Axios (인터셉터를 통한 Access/Refresh Token 자동 재발급)

---

## 🌟 주요 기능 (Key Features)

### 1. 회원 및 인증 시스템
- **회원가입 / 로그인**: 아이디/비밀번호 정규식 검증, BCrypt 암호화, JWT Access/Refresh 토큰 발급.
- **토큰 재발급 & 로그아웃**: Redis 기반 Refresh Token 검증 및 블랙리스트 관리.
- **마이페이지 & 회원정보 수정**:
  - **기존 비밀번호 필수 검증**: 닉네임 또는 비밀번호 변경 시 기존 비밀번호 일치 확인 절차 수행.
  - 내 활동 통계(작성 게시글 수, 댓글 수) 실시간 조회.

### 2. 등급(Grade) 산정 시스템
- 사용자의 활동량(게시글 수, 댓글 수)을 기준으로 등급이 비동기(`@Async`)로 자동 갱신됩니다.
- **등급 기준**:
  - 🥇 **GOLD**: 게시글 20개 이상 **AND** 댓글 50개 이상
  - 🥈 **SILVER**: 게시글 5개 이상 **AND** 댓글 10개 이상
  - 🥉 **BRONZE**: 기본 가입 등급
- 게시글 및 댓글 작성/삭제 시 즉시 등급 산정 로직이 트리거되어 반영됩니다.

### 3. 게시판 & 포스트 관리
- **카테고리 분류**: 전체, 공지사항, 자유, 정보 공유, 질문, 후기.
- **공지사항 상단 고정(Pin)**: 관리자 권한으로 최대 5개까지 고정 가능.
- **조회수 및 ETag 캐싱**: 클라이언트 IP/사용자 기반 중복 조회 방지 및 `ETag`를 통한 304 Not Modified 대역폭 최적화.
- **리치 텍스트 에디터**: TipTap 에디터를 활용한 서식 편집 및 이미지/파일 업로드.

### 4. 계층형 대댓글(Tree Comments)
- **무제한 깊이 방지**: 최대 Depth 제한 (최대 10단계).
- **Soft Delete 적용**: 자식 댓글이 남아있는 부모 댓글 삭제 시 데이터 구조 보존.
- **댓글 길이 제한**: 최대 400자 (DB 컬럼 `VARCHAR(1000)` 여유 할당).

### 5. 파일 첨부 및 보안 정책
- **개별 파일 용량**: 최대 **2MB**
- **게시글 당 총 파일 용량**: 최대 **10MB**
- **파일 개수 제한**: 최대 **10개**
- **허용 확장자**: `jpg`, `jpeg`, `png`, `gif`, `webp`, `pdf`, `doc`, `txt`, `xlsx`, `pptx`, `zip`
- **고아 파일 정리**: 매일 새벽 3시 게시글과 연결되지 않은 임시 파일 자동 정리.

---

## 📂 프로젝트 구조 (Directory Structure)

```text
BoardProject/
├── .env.example              # 루트 통합 환경변수 템플릿
├── README.md                 # 프로젝트 문서
├── forum_be/                 # 백엔드 (Spring Boot)
│   ├── build.gradle
│   ├── .env.example          # 백엔드 환경변수 템플릿
│   └── src/main/java/com/example/forum/
│       ├── config/           # Security, Redis, Async, ETag 설정
│       ├── controller/       # REST API 컨트롤러
│       ├── domain/           # JPA 엔티티 및 Enum (User, Post, Comment, Grade, Role)
│       ├── dto/              # 요청/응답 DTO
│       ├── repository/       # Spring Data JPA 레포지토리
│       ├── service/          # 비즈니스 로직
│       └── validator/        # 도메인 정책 유효성 검증
└── forum_fe/                 # 프론트엔드 (React)
    ├── package.json
    ├── .env.example          # 프론트엔드 환경변수 템플릿
    └── src/
        ├── Components/       # Modal, Toast 공통 컴포넌트
        ├── Pages/            # Forum, PostDetail, PostEdit, MyPage, SignIn, SignUp
        ├── api.js            # Axios API 클라이언트
        └── enum.js           # 프론트엔드 상수 및 유효성 검증 규칙
```

---

## ⚙️ 환경 변수 설정 (Environment Variables)

프로젝트 루트 또는 각 하위 폴더의 `.env.example`을 복사하여 `.env` 또는 시스템 환경변수로 설정합니다.

### 1. Backend (`forum_be/src/main/resources/application.properties` 대응)

| 변수명 | 필수 여부 / 기본값 | 설명 |
|---|---|---|
| `SERVER_PORT` | `8080` (기본값) | 백엔드 서버 포트 |
| `DB_URL` | **필수** | MySQL 연결 JDBC URL |
| `DB_USERNAME` | **필수** | DB 사용자명 |
| `DB_PASSWORD` | **필수** | DB 비밀번호 |
| `REDIS_HOST` | `localhost` (기본값) | Redis 호스트 |
| `REDIS_PORT` | `6379` (기본값) | Redis 포트 |
| `JWT_SECRET` | **필수** (32자 이상 보안 키) | JWT 서명 비밀키 |
| `JWT_EXPIRATION` | `1800000` (30분 기본값) | Access Token 만료 시간 (ms) |
| `JWT_REFRESH_EXPIRATION` | `604800000` (7일 기본값) | Refresh Token 만료 시간 (ms) |
| `COOKIE_SECURE` | `false` (운영 시 `true`) | 쿠키 Secure 속성 (HTTPS 여부) |
| `COOKIE_SAME_SITE` | `Lax` (운영 정책에 맞춤) | 쿠키 SameSite 속성 (`Lax`, `Strict`, `None`) |
| `FILE_MAX_SINGLE_SIZE`| `2097152` (2MB) | 개별 파일 최대 용량 (Bytes) |
| `FILE_MAX_TOTAL_SIZE` | `10485760` (10MB) | 총 파일 최대 용량 (Bytes) |
| `FILE_MAX_COUNT` | `10` | 최대 첨부 가능 파일 수 |

### 2. Frontend (`forum_fe/.env`)

| 변수명 | 기본값 | 설명 |
|---|---|---|
| `REACT_APP_API_BASE_URL` | `http://localhost:8080/api` | 백엔드 API 엔드포인트 URL |

---

## 🚀 실행 방법 (Getting Started)

### 1. 필수 인프라 실행 (MySQL & Redis)
로컬에 MySQL 및 Redis가 구동 중이어야 합니다.
- MySQL 기본 데이터베이스: `forum_db` (포트 `3306`)
- Redis (포트 `6379`)

### 2. Backend 실행
```bash
cd forum_be

# Windows PowerShell
./gradlew bootRun

# Linux / macOS
./gradlew bootRun
```
> 백엔드 서버는 `http://localhost:8080`에서 실행됩니다.

### 3. Frontend 실행
```bash
cd forum_fe

# 의존성 설치
npm install

# 개발 서버 실행
npm start
```
> 프론트엔드 개발 서버는 `http://localhost:3000`에서 실행됩니다.

---

## 📋 주요 API 엔드포인트

### 인증 (`/api/auth`)
- `POST /api/auth/signup` : 회원가입
- `POST /api/auth/login` : 로그인 (Access Token & Refresh Token 발급)
- `POST /api/auth/reissue` : Access Token 재발급
- `POST /api/auth/logout` : 로그아웃 (토큰 무효화)

### 회원 (`/api/users`)
- `GET /api/users/me` : 현재 로그인 사용자 정보 및 활동 통계 조회
- `PUT /api/users/me` : 회원 정보 수정 (**기존 비밀번호 검증 필수**)

### 게시글 (`/api/posts`)
- `GET /api/posts` : 게시글 목록 조회 (카테고리 필터링, 정렬, 페이징, ETag 지원)
- `GET /api/posts/{id}` : 게시글 상세 조회 및 조회수 증가
- `POST /api/posts` : 게시글 작성 (등급 자동 갱신 트리거)
- `PUT /api/posts/{id}` : 게시글 수정
- `DELETE /api/posts/{id}` : 게시글 삭제 (Soft Delete & 등급 재산정)

### 댓글 (`/api/comments`)
- `GET /api/comments/{postId}` : 게시글별 댓글 목록 트리 조회
- `POST /api/comments/{postId}` : 댓글/대댓글 작성 (최대 400자, 등급 자동 갱신 트리거)
- `PUT /api/comments/{postId}/{commentId}` : 댓글 수정
- `DELETE /api/comments/{postId}/{commentId}` : 댓글 삭제 (Soft Delete & 등급 재산정)

### 파일 (`/api/files`)
- `POST /api/files/upload` : 파일/이미지 업로드 (개별 2MB, 총 10MB 한도)
- `DELETE /api/files/{fileId}` : 업로드 파일 삭제
