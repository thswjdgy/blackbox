# 📌 Team Blackbox 프로젝트 컨텍스트 (Claude 전달용)

이 문서는 AI 어시스턴트(Claude 등)에게 현재까지 진행된 프로젝트의 구조와 개발 진행 상황을 설명하기 위한 요약본입니다.

---

## 🚀 1. 프로젝트 개요
- **프로젝트명**: Team Blackbox (팀 블랙박스)
- **목표**: 대학 캡스톤 디자인 등 조별 과제에서 발생하는 무임승차(Free-riding) 방지 및 공정한 기여도 평가를 위한 협업 플랫폼.
- **주요 워크플로우**:
  1. 팀 생성 및 초대 코드를 통한 합류
  2. 태스크(칸반 보드), 회의록(체크인), 파일 공유(해시 볼트)를 통한 협업
  3. 모든 활동은 백그라운드 이벤트 로그(`activity_logs`)로 기록
  4. 기록된 활동 기반으로 **객관적인 기여도 점수 산출 (Score Engine)**

---

## 🛠 2. 기술 스택 (Tech Stack)
- **Backend**: Spring Boot 3.2.x, Java 21, Spring Data JPA, Spring Security (JWT), PostgreSQL
- **Frontend**: Next.js 14+ (App Router), React, TailwindCSS, `@dnd-kit` (드래그 앤 드롭)
- **Database / Infra**: PostgreSQL, Flyway (DB 마이그레이션), Docker Compose (DB, Backend, Frontend 컨테이너라이징)

---

## ✅ 3. 현재까지 구현된 기능 (진행률: Phase 3 일부 완료)

### Phase 1: 초기 설정 및 인프라 (완료)
- `docker-compose.yml` 리포지토리 구성
- Flyway를 활용한 데이터베이스 초기화 (V1 ~ V5 스크립트 작성)
  - 테이블: `users`, `projects`, `project_members`, `tasks`, `task_assignees`, `meetings`, `meeting_attendees`, `activity_logs`
- JWT 기반 사용자 회원가입 및 로그인 (`/api/auth/*`)
- Next.js 랜딩 페이지 및 `auth/page.tsx` UI 구성

### Phase 2: 대시보드 및 데이터 수집 연동 (완료)
- 로그인 스토어 연동 완료 및 보호된 라우팅 설정.
- 동의 플로우 뷰 개발: 사용자 프로필 조회 시 데이터 수집 동의(`dataCollectionConsent`) 여부를 체크.
- 만약 미동의 사용자라면 대시보드 접근 시 **데이터 수집 동의 온보딩 모달**을 띄워 강제 동의 유도 로직 구현.
- 초대 코드를 통한 프로젝트 생성 및 가입 (`/api/projects/*`)

### Phase 3: 칸반 보드 시스템 (완료)
- **백엔드 (Task & Activity API)**
  - 태스크 생성, 수정, 삭제, 상세조회 (`/api/projects/{projectId}/tasks`)
  - 태스크 상태 변경 API (`PATCH .../tasks/{taskId}/status`) 연동.
  - **Activity Log 시스템 훅킹**: 태스크를 생성하거나 상태를 이동시킬 때마다 `ActivityLogService`를 통해 자동으로 데이터베이스에 `TASK_CREATED`, `TASK_STATUS_CHANGED`와 같은 이벤트 기록. (이후 기여도 산출에 활용됨)
- **프론트엔드 (Kanban UI)**
  - 특정 프로젝트에 해당하는 전용 사이드바 레이아웃 (`projects/[id]/layout.tsx`) 신설.
  - `@dnd-kit`을 활용해 마우스 터치 및 모션이 부드러운 3단 칼럼 (To Do, In Progress, Done) 상태 전이 칸반 보드 개발 (`projects/[id]/board/page.tsx`).
  - 카드 UI 뱃지(우선순위 표기), 멤버 프로필 등 전용 모달(`TaskModal`) 적용.

---

## 📂 4. 주요 패키지 및 구조체

### [백엔드 구조]
- **Auth**: `AuthController`, `AuthService`, `JwtUtil`, `CustomUserDetailsService`
- **User**: `User` 엔티티 (개인정보 및 약관동의 속성 포함), `UserController` (내 프로필 및 동의 갱신)
- **Project**: `Project` & `ProjectMember` 엔티티, 초대 코드 발급 로직.
- **Task**: `Task` 엔티티 (상태값, 다대다 담당자 참조), `TaskController`, `TaskService`
- **Activity**: `ActivityLog` 엔티티 (모든 사용자의 행동을 기록하는 중앙 저장소)
- **Global**: 글로벌 예외 처리 (`BusinessException`, `ErrorCode` 등)

### [프론트엔드 구조]
- `src/lib/api.ts`: Axios 인스턴스 (인터셉터를 통해 Header에 JWT 토큰 자동 주입)
- `src/app/auth`: 로그인 & 회원가입 탭
- `src/app/dashboard`: 등록된 프로젝트 카드 나열 및 생성/조인 모달 구비. (미동의자 Onboarding 모달 연동)
- `src/app/projects/[id]/layout.tsx`: 프로젝트 상세 뷰에 진입 시 보일 사이드바 메뉴 (보드, 미팅, 볼트 등)
- `src/app/projects/[id]/board`: **(최신 구현됨)** 드래그 앤 드롭을 지원하는 DndContext 칸반 보드
- `src/components/board/`: `TaskModal`, `TaskCard` 등 칸반 보드 전용 컴포넌트

---

## 🏃 5. 다음 개발 목표 (Next Steps)
1. **Phase 3 (회의록 파트)**: 화상/오프라인 회의록 작성 및 미팅 참가 체크인(출석) 시스템.
2. **Phase 4 (Hash Vault)**: 파일 및 자료 업로드 시 블록체인/해시 기반 무결성 검증 자료실 (해시값 기록을 포함하여 무임승차/표절 등을 방지).
3. **Phase 5 (Score Engine)**: 누적된 `activity_logs`를 파싱하여, 팀원별로 종합 등급(A~F) 통계 자료를 시각화하는 리포트 기능.
