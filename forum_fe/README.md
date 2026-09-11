# 🎨 Forum Board Frontend (`forum_fe`)

React 19 기반의 포럼/게시판 웹 프론트엔드 애플리케이션입니다.

---

## 📌 주요 기술 스택 (Tech Stack)

- **Framework & Runtime**: React 19, React Router v7
- **Editor**: TipTap Rich Text Editor (`@tiptap/react`, `@tiptap/starter-kit`, `@tiptap/extension-image`)
- **HTTP Client**: Axios (인터셉터 기반 자동 토큰 재발급 및 큐 동기화)
- **UI & Icons**: Lucide React, Custom CSS Variables (다크/라이트 모던 디자인)
- **State & Context**: Context API
  - `AuthContext`: 사용자 로그인 상태 및 토큰 관리
  - `ToastContext`: 전역 토스트 알림 시스템
  - `ModalContext`: 전역 커스텀 확인/알림 모달 시스템

---

## 🏛️ 주요 기능 및 컴포넌트 구조

```text
src/
├── Components/
│   ├── Header/          # 공통 상단 네비게이션 바
│   ├── Modal/           # ConfirmModal 및 모달 전역 컨텍스트
│   └── Toast/           # ToastContainer, ToastItem 및 토스트 컨텍스트
├── Pages/
│   ├── Forum/           # 게시글 목록, 검색, 카테고리 필터링, 페이지네이션
│   ├── Post/            # 게시글 상세(PostDetailPage), 에디터(TiptapEditor), 댓글(CommentSection), 첨부파일(AttachmentCard)
│   ├── MyPage/          # 회원정보 조회, 프로필/비밀번호 수정, 활동 통계
│   ├── SignIn/          # 로그인 페이지
│   └── SignUp/          # 회원가입 페이지
├── api.js               # Axios 인스턴스, Refresh Token 재발급 인터셉터 및 API 메서드 모음
├── enum.js              # 시스템 공통 상수 (카테고리, 에러코드, 검증 규칙)
└── utils.js             # 날짜 포맷, 파일 크기 포맷, 텍스트 자르기 유틸 함수
```

### 1. TipTap 리치 텍스트 에디터
- 텍스트 서식(Bold, Italic, Heading, BulletList 등) 지원
- 에디터 내 이미지 업로드 및 일반 파일 첨부 지원

### 2. 토큰 자동 재발급 (Silent Refresh)
- API 요청 중 Access Token 만료(401) 감지 시, 대기열(Queue)을 형성하여 토큰을 재발급받고 실패한 요청을 자동으로 재전송

### 3. 반응형 UI & 커스텀 피드백
- 기본 브라우저 `alert`/`confirm` 대신 커스텀 토스트 알림 및 모달 팝업 적용

---

## ⚙️ 환경 변수 설정 (Environment Variables)

`forum_fe/.env.example` 파일을 복사하여 `.env`를 생성합니다.

```env
REACT_APP_API_BASE_URL=http://localhost:8080/api
```

---

## 🚀 실행 방법 (Getting Started)

### 1. 패키지 설치
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
