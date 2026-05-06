# Team Blackbox — 개발 우선순위 & TODO

## 현재 Phase: 확장 2 (Phase 7 — 13~15주)

> **진행 현황 요약 (2026-04-20 기준)**
> - Phase 1~5 (MVP): ✅ 완료
> - Phase 6 (확장 1 — 외부 연동): ✅ 대부분 완료
> - Phase 7 (확장 2 — AI & 고도화): 🔄 진행 중 (가중치 조정 완료, 나머지 미구현)

---

## Phase 1: 인프라 & 기반 구축 (1~2주)

### 🔴 P0 — Docker Compose 인프라 셋업
- [x] 프로젝트 루트 `docker-compose.yml` 작성
- [x] PostgreSQL 16 컨테이너 설정 (`db` 서비스)
- [x] `pgdata` 볼륨 (DB 영속), `uploads` 볼륨 (파일 저장)
- [x] Spring Boot Dockerfile 작성 (multi-stage 빌드)
- [x] Next.js Dockerfile 작성 (standalone 빌드)
- [x] Nginx 설정 (`/` → frontend, `/api` → backend, `/uploads` → 정적)
- [x] `docker compose up -d` 로 전체 스택 기동 확인
- [x] `.env` 환경변수 파일 구성 (DB 비밀번호, JWT 시크릿 등)
- **참조:** `docs/docker.md`

### 🔴 P0 — 프로젝트 초기화
- [x] Spring Boot 프로젝트 생성 (Java 17, Gradle)
- [x] Next.js 프로젝트 생성 (App Router, TypeScript)
- [x] GitHub 리포지토리 생성 & 브랜치 전략 확정
- [x] CI/CD 기본 설정 (GitHub Actions — Docker 빌드 확인)
- [x] 프로젝트 메인 대시보드 (/projects/[id]) 구현
- [x] 초대 코드 참여 및 자동 이동 로직 연동
- [x] `V1__init_users.sql` — users 테이블
- [x] `V2__init_projects.sql` — projects, project_members 테이블
- [x] `V3__init_tasks.sql` — tasks, task_assignees 테이블
- [x] `V4__init_meetings.sql` — meetings, meeting_attendees 테이블
- [x] `V5__init_activity_logs.sql` — activity_logs 테이블 + 인덱스
- [x] `V6__init_file_vault.sql` — file_vault + immutable 트리거 + tamper_detection_log
- [x] `V7__init_scores_alerts.sql` — contribution_scores, alerts 테이블
- [x] `V8__init_refresh_tokens.sql` — refresh_tokens 테이블
- [x] `docker compose up` 시 Flyway 자동 마이그레이션 확인
- **참조:** `docs/database.md`

---

## Phase 2: 인증 & 프로젝트 관리 (3~4주)

### 🔴 P0 — 백엔드: 인증
- [x] User 엔티티 & Repository
- [x] 회원가입 API (`POST /api/auth/signup`)
- [x] 로그인 API (`POST /api/auth/login`) — JWT 발급
- [x] JWT 필터 & SecurityConfig
- [x] Refresh Token 로직
- [x] 역할 기반 접근 제어 (STUDENT / PROFESSOR / TA)
- **참조:** `backend/modules/auth.md`

### 🔴 P0 — 백엔드: 프로젝트 관리
- [x] Project CRUD API
- [x] 초대 코드 생성 & 참여 API
- [x] 멤버 관리 (역할 변경, 탈퇴)
- [x] ProjectAccessChecker (LEADER/MEMBER/OBSERVER 권한 검증)
- [x] 데이터 수집 동의 기록 API
- [x] `GET /projects/{id}/members` — 팀원 목록 전용 엔드포인트 (설정/보드 공용)
- **참조:** `backend/modules/project.md`

### 🟡 P1 — 프론트엔드: 인증 & 프로젝트
- [x] 로그인/회원가입 페이지
- [x] JWT 토큰 관리 (Zustand store)
- [x] Axios 인터셉터 (토큰 자동 첨부, 갱신)
- [x] 프로젝트 목록 페이지
- [x] 프로젝트 생성 모달
- [x] 초대 코드 참여 페이지
- [x] 동의 플로우 온보딩 UI
- **참조:** `frontend/pages/auth.md`, `frontend/pages/dashboard.md`

---

## Phase 3: 칸반 & 회의록 (5~6주)

### 🔴 P0 — 백엔드: 태스크
- [x] Task CRUD API (생성/수정/삭제/목록)
- [x] 상태 변경 API (TODO → IN_PROGRESS → DONE)
- [x] 담당자 배정 API
- [x] 태스크 이벤트 → `activity_logs` 자동 기록
- **참조:** `backend/modules/task.md`

### 🔴 P0 — 백엔드: 회의록
- [x] Meeting CRUD API
- [x] 체크인 코드 생성 & 체크인 API
- [x] 회의록 작성/수정 API
- [x] 액션 아이템 → 태스크 생성 연결
- [x] 회의 이벤트 → `activity_logs` 자동 기록
- [x] `POST /meetings/{id}/attendees/{userId}` — 코드 없이 직접 참석 처리 (수동 체크인)
- [x] `DELETE /meetings/{id}/attendees/{userId}` — 참석자 제거 (관리자용)
- **참조:** `backend/modules/meeting.md`

### 🟡 P1 — 프론트엔드: 칸반 보드
- [x] 3단 칼럼 레이아웃 (To Do / In Progress / Done)
- [x] 태스크 카드 컴포넌트
- [x] 드래그 앤 드롭 (@dnd-kit) — `DroppableColumn`, `DragOverlay` 포함
- [x] 드래그앤드롭 영속성 수정 — `useRef`로 drag origin 고정 (페이지 이탈 후 상태 복구 버그 해결)
- [x] 태스크 생성/편집 모달
- [x] 태스크 모달 담당자 다중 선택 UI (멤버 토글 버튼)
- [x] 태스크 카드 담당자 이름 표시 (아이디 숫자 → 실제 이름 아바타 + 이름 태그)
- [x] 필터 (우선순위, 태그)
- **참조:** `frontend/pages/board.md`

### 🟡 P1 — 프론트엔드: 회의록
- [x] 회의 목록 페이지
- [x] 회의 상세 (참석자, 내용, 결정사항, 출석 현황)
- [x] 체크인 UI (QR 코드 & 링크 공유)
- [x] 직접 참석 버튼 — 코드 입력 없이 클릭 즉시 참석 처리
- [x] 참석 취소 기능 제거 (참석 후 취소 불가 정책)
- [x] 액션 아이템 → 태스크 생성 버튼 & 연관 태스크 목록
- **참조:** `frontend/pages/meeting.md`

---

## Phase 4: Hash Vault & Score Engine (5~7주, Phase 3과 병행)

### 🔴 P0 — 백엔드: Hash Vault (로컬 파일 저장)
- [x] FileStorageService (로컬 디스크 저장: `/data/uploads/{projectId}/`)
- [x] SHA-256 해시 생성 (HashService)
- [x] 파일 업로드 API (`POST /projects/:id/files` — multipart)
- [x] `file_vault` INSERT + 버전 관리
- [x] 재업로드 시 해시 비교 로직
- [x] 파일 다운로드 API (`GET /files/:id/download` — 스트리밍)
- [x] 변조 감지 시 `tamper_detection_log` + `alerts` 기록
- [x] 파일 이력 조회 API
- **참조:** `backend/modules/vault.md`

### 🔴 P0 — 백엔드: Score Engine (MVP 버전)
- [x] 플랫폼 활동 로그 기반 점수 산출
- [x] 팀 평균 기준 정규화 (상한 150 클리핑)
- [x] 항목별 점수 계산 (태스크/회의/파일)
- [x] 종합 점수 = Σ(항목 × 가중치)
- [x] 점수 재계산 스케줄러 (30분 간격 or 이벤트 트리거)
- [x] 기본 가중치 설정 (태스크 35%, 회의 30%, 파일 20%, 외부활동 15%)
- **참조:** `backend/modules/score.md`

### 🔴 P0 — 백엔드: 경보 시스템 (규칙 기반)
- [x] 불균형 감지 (점수 편차 > 40%)
- [x] 이탈 감지 (2주 연속 활동 없음)
- [x] 과부하 감지 (1인이 60% 이상)
- [x] 경보 생성 → `alerts` 테이블
- **참조:** `backend/modules/alert.md`

### 🟡 P1 — 프론트엔드: 파일 & 점수
- [x] 파일 업로드 UI
- [x] 파일 이력 뷰 (Hash Vault → "파일 검사"로 명칭 변경)
- [x] Google Drive 업로드 버튼 (`POST /projects/{id}/google/push-vault/{fileId}`)
- [x] 내 기여도 요약 카드
- **참조:** `frontend/pages/vault.md`, `frontend/pages/analytics.md`

---

## Phase 5: 교수 대시보드 & MVP 마무리 (7~8주)

### 🔴 P0 — 프론트엔드: 교수 대시보드
- [x] 팀 목록 오버뷰 (카드 뷰)
- [x] 팀 상세: 기여도 바 차트 (Recharts)
- [x] 프로젝트 진행률 표시
- [x] 경보 뱃지 & 목록
- [x] 건강도 지표 (🟢🟡🟠🔴)
- **참조:** `frontend/pages/professor.md`

### 🔴 P0 — Docker 배포 안정화
- [x] Docker Compose production 프로필 구성
- [x] Nginx SSL 설정 (self-signed for demo)
- [x] `docker compose -f docker-compose.prod.yml up -d` 검증
- [x] 데모 환경에서 전체 플로우 동작 확인

### 🔴 P0 — 통합 & 중간발표 준비
- [x] 프론트-백 통합 테스트
- [x] 데모 시나리오 리허설
- [x] 버그 수정 & 안정화
- [x] **8주차 중간발표 데모 완료**

### 🟡 P1 — UI/UX 개선 & 버그픽스
- [x] 사이드바 접기/펼치기 → 햄버거(☰) 아이콘, 최상단 배치
- [x] 사이드바 "ID: {숫자}" → 로그인 유저 이름 + 이메일 표시
- [x] 사이드바 탭명 한글화
- [x] 사이드바 이중 활성화 버그 수정
- [x] 섹션 간 좌우 네비게이션 바 (Alt+← / Alt+→)
- [x] 회의 상세 페이지 레이아웃 버그 수정 (사이드바 침범)
- [x] 태스크 카드 삭제 버튼 (호버 시 ✕)
- [x] "파일 금고" / "Hash Vault" → **"파일 검사"** 전체 리네임 (레이아웃, 홈, 파일 페이지)
- [x] 프로젝트 홈 데이터 요약 — 태스크 통계 카드, 진행률 바, 최근 회의 목록, 기여도 순위
- [x] 설정 페이지 팀원 선택 드롭다운 수정 (이름 미표시 버그 → `/members` 엔드포인트 연동)

---

## Phase 5.5: AI & 외부 서비스 1차 연동 (8~9주, Phase 5와 병행)

### 🔴 P0 — Claude AI 연동 (회의록)
- [x] Spring WebFlux WebClient 기반 Claude API 비동기 연동
- [x] 회의록 AI 요약 생성 (`POST /meetings/{id}/ai-summary`)
- [x] 액션아이템 자동 추출 → 태스크 생성 연결
- [x] AI 일정 추천 (팀원 캘린더 freeBusy 조회 → 3개 옵션 제안)

### 🔴 P0 — Google Calendar 연동 (회의 일정)
- [x] OAuth2 기반 Google 인증 (access token + refresh token)
- [x] `google_calendar_tokens` 테이블 저장
- [x] freeBusy API로 팀원 공통 빈 시간 조회
- [x] Google Calendar 이벤트 자동 등록 (`events.insert`)

### 🟡 P1 — Notion 연동 (회의록 내보내기)
- [x] 회의록 → Notion 페이지 자동 생성 (`pages.create`)
- [x] 태스크 → Notion DB 자동 동기화 (`blocks.append`)
- [x] `notion_page_id` 역참조 저장 (meetings 테이블)

### 🔴 P0 — ERD 누락 필드 추가 (`V13__add_missing_fields.sql`) ✅ 완료
- [x] `meetings` 테이블 — `ai_summary TEXT`, `notion_page_id VARCHAR(255)` 추가
- [x] `projects` 테이블 — `course_name`, `semester`, `start_date`, `end_date`, `created_by(FK)` 추가
- [x] `project_members` 테이블 — `consent_github`, `consent_drive`, `consent_ai` 추가
- [x] Meeting/Project/ProjectMember 엔티티 필드 반영
- [x] DTO (MeetingDto, ProjectDto) 새 필드 포함
- [x] `PATCH /projects/{id}/members/me/consent` — 외부 서비스 동의 업데이트 API 추가

---

## Phase 6: 확장 1 — 외부 연동 (9~12주)

### 🟡 P1 — GitHub 연동
- [x] `V9__init_github.sql` — github_configs, github_user_mappings 테이블
- [x] Webhook 수신 엔드포인트 (HMAC-SHA256 검증)
- [x] Push / PR / Issue 이벤트 파싱 → `activity_logs` (source: GITHUB)
- [x] 폴백 폴링 30분 스케줄 (`GitHubPollService`)
- [x] 레포 연동 설정 UI (PAT + webhookSecret)
- [x] GitHub 계정 ↔ 플랫폼 유저 매핑 UI
- [ ] GitHub App 공식 등록 (실제 배포 시)

### 🟡 P1 — Notion 연동
- [x] `V10__init_notion.sql` — notion_configs, notion_user_mappings 테이블
- [x] Internal Integration Token 기반 연동
- [x] 워크스페이스 전체 검색 + 특정 Database 폴링
- [x] 페이지 생성/수정 → `activity_logs` (source: NOTION)
- [x] Notion 계정 ↔ 플랫폼 유저 매핑 (수동 + 이메일 자동)
- [x] 연동 설정 UI (token, databaseId, 즉시 폴링)
- [ ] Notion 유저 ID 조회 도우미 (설정 UI에서 자동 검색)

### 🟡 P1 — Google Drive / Sheets / Forms 연동
- [x] `V11__init_google.sql` — google_configs 테이블
- [x] OAuth2 기반 Google 인증 (access token + refresh token)
- [x] Google Drive 파일 변경 이력 폴링 (`GooglePollService`)
- [x] Google Sheets 편집 이력 수집
- [x] Google Forms 응답 수집
- [x] `activity_logs` (source: GOOGLE_DRIVE / GOOGLE_SHEETS / GOOGLE_FORMS)
- [x] Score Engine에 Google 활동 점수 반영
- [x] 연동 설정 UI (Google 탭 — 인증 시작/해제)
- [x] 파일 검사 페이지 → Google Drive 업로드 버튼

### 🟡 P1 — Score Engine 확장
- [x] GitHub 활동 점수 반영 (push 3pt, PR오픈 4pt, PR머지 6pt, 이슈 2/3pt)
- [x] Notion 활동 점수 반영 (생성 4pt, 수정 2pt, 댓글 1pt)
- [x] Google 활동 점수 반영 (Drive업로드 5pt, 수정 2pt, Sheets편집 2pt, Forms응답 3pt)
- [x] 신뢰도 가중치 적용 (자동 1.0, 수동 0.7 — `trust_level` 컬럼 + SUM 집계로 ScoreEngine 반영)
- [ ] Git 기여 점수 세부 수식 고도화

### 🟡 P1 — 프론트: 확장 UI
- [x] 외부 연동 설정 페이지 (GitHub / Notion / Google 탭)
- [x] 활동 타임라인 (소스별 색상 구분, 멤버 필터)
- [x] 교수 대시보드 (Recharts 바 차트, 팀 테이블, 건강도 지표)

---

## Phase 7: 확장 2 — AI & 고도화 (13~15주, 시간 허용 시)

### 🟢 P2 — 교수 가중치 조정 ✅ 완료
- [x] `V12__project_weights.sql` — project_weights 테이블 (합계=1.0 CHECK 제약)
- [x] `ProjectWeight` JPA 엔티티 + `ProjectWeightRepository`
- [x] `GET /projects/{id}/weights` — 가중치 조회 (없으면 기본값 반환)
- [x] `PUT /projects/{id}/weights` — 가중치 저장/수정
- [x] Score Engine — 계산 시 project_weights 조회 → 프로젝트별 가중치 적용
- [x] 기여도 리포트 페이지 — 가중치 조정 패널 (슬라이더 + 4가지 프리셋 + 합계 검증 + 시각 비율 바)
- [x] 가중치 변경 이력 조회 (`weight_history` 테이블 + `GET /projects/{id}/weights/history`)

### 🟢 P2 — AI Analyzer
- [ ] Claude API 연동
- [ ] 커밋 품질 분석 (배치 10건 단위)
- [ ] quality_score 산출 & `activity_logs` 업데이트
- [ ] Anti-Gaming 로직 (마감 직전 벼락치기 패턴 탐지)
- [ ] AI 분석 동의 플로우 (consent_ai_analysis)
- **참조:** `backend/modules/analyzer.md`

### 🟢 P2 — 피어리뷰 ✅ 완료 (백엔드)
- [x] 피어리뷰 제출 API (`POST /projects/{id}/peer-reviews`) — 중복 방지, 자기평가 차단
- [x] 피어리뷰 요약 조회 (`GET /projects/{id}/peer-reviews/summary`) — 피평가자별 평균
- [x] 내가 쓴 리뷰 조회 (`GET /projects/{id}/peer-reviews/me`)
- [ ] 피어리뷰 UI (프론트엔드)

### 🟢 P2 — PDF 리포트 ✅ 완료
- [x] 기여도 리포트 PDF 자동 생성 (`GET /projects/{id}/report/pdf`) — OpenPDF 1.3.30
- [ ] PDF 다운로드 버튼 (기여도 리포트 페이지 — 프론트엔드)

### 🟢 P2 — 수동 작업 신고 ✅ 완료
- [x] 오프라인 기여 신고 API (`POST /projects/{id}/manual-logs`)
- [x] 신뢰도 가중치 0.7 자동 적용 (`trust_level=0.7` + activity_log 기록)
- [x] 팀장 승인/거절 (`PATCH /projects/{id}/manual-logs/{id}/review`)
- [ ] 수동 신고 UI (프론트엔드)

### 🟢 P2 — 미구현 유스케이스 (Use Case Diagram 기준 구현 예정 6개)
- [ ] GitHub App 공식 연동 (PAT/Webhook 기반 → GitHub App 마이그레이션)
- [ ] Google Drive 연동 (Push Notification 기반 revision 수집)
- [ ] AI 기반 일정 분석 (활동 패턴 분석 → 최적 회의 시간 자동 추천)
- [ ] 라이브러리 시스템 (프로젝트 내 참고자료 공유 기능)
- [ ] 백업 플로우 UI (DB 스냅샷 + Vault 백업 트리거)
- [ ] 권한 관리 고도화 (OBSERVER 역할 세분화, 역할별 메뉴 노출 제어)

---

## 우선순위 범례

| 레벨 | 의미 | 시점 |
|------|------|------|
| 🔴 P0 | MVP 필수 | 1~8주 |
| 🟡 P1 | 핵심 차별화 — 외부 연동 | 9~12주 |
| 🟢 P2 | 고도화 — 시간 허용 시 추가 | 13~15주 |

---

## 남은 작업 요약 (2026-04-20)

### 미구현 (P2 — 선택)
| 기능 | 예상 난이도 | 데모 가치 |
|------|------------|-----------|
| AI Analyzer (Claude API 커밋 분석) | 높음 | 높음 |
| 피어리뷰 시스템 | 중간 | 높음 |
| PDF 리포트 생성 | 중간 | 중간 |
| 수동 작업 신고 | 낮음 | 중간 |
| 가중치 변경 이력 | 낮음 | 낮음 |

### 미완성 (P1 — 작은 개선)
| 기능 | 비고 |
|------|------|
| GitHub App 공식 등록 | 실제 배포 시 필요, 데모는 PAT로 대체 가능 |
| Notion 유저 ID 조회 도우미 | 설정 편의성 개선 |
| 신뢰도 가중치 (자동 1.0 / 수동 0.7) | Score Engine 내부 수식 고도화 |

---

## 의존성 그래프

```
Docker Compose ──→ DB 스키마 (Flyway V1~V12) ──→ 백엔드 인증 ──→ 프로젝트 API
                   (V1~V8: MVP 핵심 테이블, V9~V11: 외부연동, V12: project_weights)
     │                                                  │              │
     ▼                                                  ▼              ▼
  Nginx 설정                                      프론트 인증     태스크 API
                                                        │              │
                                                        ▼              ▼
                                                  프로젝트 UI    칸반 보드 UI
                                                                       │
                                                                  회의록 API/UI
                                                                       │
                                                                  Hash Vault
                                                                 (로컬 파일 저장)
                                                                       │
                                                                  Score Engine
                                                                  (가중치 조정 포함)
                                                                       │
                                                                  교수 대시보드
                                                                       │
                                                          [MVP 완성 — 8주차 ✅]
                                                                       │
                                                 [확장 1] GitHub / Notion / Google 연동 ✅
                                                                       │
                                              [확장 2] 가중치 조정 ✅ / AI·피어리뷰 미구현
```
