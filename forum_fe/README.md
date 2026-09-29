# 🎨 Forum Board Frontend (`forum_fe`)

React 19 기반의 모던 포럼/게시판 웹 프론트엔드 애플리케이션입니다.

---

## 📌 주요 기술 스택 (Tech Stack)

- **Framework & Runtime**: React 19, React Router v7
- **Editor**: TipTap Rich Text Editor (`@tiptap/react`, `@tiptap/starter-kit`, `@tiptap/extension-image`, `@tiptap/extension-text-align`)
- **HTTP Client**: Axios
  - 인터셉터 기반 Silent Refresh Token 자동 재발급 및 요청 대기 큐 동기화
  - `AbortController`를 통한 컴포넌트 생명주기 및 페이지 전환 시 진행 중 요청 즉시 취소
- **UI & Icons**: Lucide React, CSS Variables 기반 디자인
- **Security**: DOMPurify XSS 살균 소독
- **State & Context**: React Context API
  - `ToastContext`: 전역 토스트 알림 시스템 (성공, 경고, 에러, 정보)
  - `ModalContext`: 전역 커스텀 확인/알림 모달 시스템
  - `ProtectedRoute`: 인증 기반 라우트 보호 가드

---

## 🏛️ 주요 기능 및 컴포넌트 구조

```text
src/
├── Components/
│   ├── Modal/           # ConfirmModal, Modal 및 모달 전역 컨텍스트 (ModalContext)
│   ├── Search/          # SearchBar 검색 바 공통 컴포넌트
│   └── Toast/           # ToastContainer, ToastItem 및 토스트 컨텍스트 (ToastContext)
├── Pages/
│   ├── Forum/           # 게시글 목록(ForumPage), 페이지네이션(Pagination)
│   ├── Post/            # 게시글 상세(PostDetailPage), 에디터(PostEditPage, TiptapEditor), 댓글(CommentSection, CommentList, CommentItem, CommentForm), 첨부파일(AttachmentCard, AttachedFileList)
│   ├── SearchResult/    # 다중 키워드 검색 결과 페이지(SearchResultPage)
│   ├── MyPage/          # 회원정보 조회, 닉네임/비밀번호 수정, 회원 탈퇴(MyPage)
│   ├── SignIn/          # 로그인 페이지(SignInPage)
│   └── SignUp/          # 회원가입 페이지(SignUpPage)
├── api.js               # Axios 인스턴스, Refresh Token 자동 재발급 인터셉터 및 REST API 함수
├── enum.js              # 시스템 공통 상수 (카테고리, 정렬, 검증 규칙, 페이징 설정)
├── ProtectedRoutes.js   # 미인증 접근 차단 및 로그인 페이지 리다이렉션 가드
└── utils.js             # 날짜 포맷, 파일 크기 포맷, 에디터/본문 HTML 살균, 파일 다운로드 유틸
```

---

## 🌟 핵심 구현 및 안정화 사항

### 1. TipTap 리치 텍스트 에디터
- 텍스트 서식(굵게, 기울임, 취소선, 제목 1~3, 좌/우/중앙/양쪽 정렬, 리스트) 지원
- 에디터 내 본문 인라인 이미지 삽입 및 하단 첨부파일 리스트 독립 관리
- DOMPurify 기반 XSS 방어 및 안전한 HTML 렌더링

### 2. 토큰 자동 재발급 (Silent Refresh) 및 대기 큐
- API 요청 중 Access Token 만료(401) 감지 시, `failedQueue`를 형성하여 토큰을 1회 재발급받고 대기 중이던 모든 요청을 자동으로 재전송
- 세션 완전 만료 시 로컬 스토리지 정리 및 로그인 페이지로 안전하게 이동

### 3. 계층형 대댓글 및 직관적인 폼 피드백
- 부모 댓글 및 자식 대댓글(답글) 작성/수정/삭제 지원
- 댓글/게시글 등록 및 수정 시 유효성 조건 실시간 검증 및 버튼 활성화

### 4. 회원 정보 관리 및 보안 탈퇴
- 마이페이지에서 기존 비밀번호를 필수로 확인하여 닉네임 및 비밀번호 변경
- 모달을 통한 2차 비밀번호 확인 후 안전한 회원 탈퇴 처리

---

## ⚙️ 환경 변수 설정 (Environment Variables)

`forum_fe/.env.example` 파일을 복사하여 `.env`를 생성합니다.

```env
REACT_APP_API_BASE_URL=http://localhost:8080/api
```

---

## 🚀 실행 및 빌드 방법 (Getting Started)

### 1. 의존성 패키지 설치
```bash
npm install
```

### 2. 개발 서버 구동
```bash
npm start
```
> 애플리케이션은 기본적으로 `http://localhost:3000`에서 실행됩니다.

### 3. 프로덕션 빌드
```bash
npm run build
```
> 최적화된 프로덕션 빌드 결과물이 `build/` 디렉토리에 생성됩니다.
