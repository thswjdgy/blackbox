# 📊 Team Blackbox 프로젝트 진행 보고서 (최종)

## 📅 프로젝트 개요
- **목표**: 팀 프로젝트 기여 분석 및 객관적 평가 플랫폼 (MVP)
- **현재 시점**: 8주차 (중간발표 및 MVP 완성)
- **전체 진척도**: 🟢 100% (고도화 및 안정화 완료)

## 🏁 단계별 완료 현황
- [x] **Phase 1: 기반 인프라 (100%)**
- [x] **Phase 2: 프로젝트 기반 기능 (100%)**
- [x] **Phase 3: 협업 도구 - 칸반/회의록 (100%)**
  - *고도화*: QR 체크인, 상세 출석부, 액션 아이템 연동 완료
- [x] **Phase 4: 검증 및 엔진 - Vault/Score (100%)**
- [x] **Phase 5: 시각화 및 배포 - Dashboard/Docker (100%)**
  - *안정화*: 회원가입 유효성 검사 및 런타임 오류 수정 완료

---

## 구현 완료 현황

### ✅ Phase 1 — 인프라 & 기반 (완료)

| 항목 | 상태 |
|------|------|
| Docker Compose 전체 스택 (DB + Backend + Frontend + Nginx) | ✅ |
| PostgreSQL 16 + Flyway 마이그레이션 (V1~V8) | ✅ |
| Spring Boot 3 / Java 17 멀티스테이지 Docker 빌드 | ✅ |
| Next.js 16 App Router standalone 빌드 | ✅ |
| Nginx 리버스 프록시 (`/` → frontend, `/api` → backend) | ✅ |

### ✅ Phase 2 — 인증 & 프로젝트 관리 (완료)

| 항목 | 상태 |
|------|------|
| 회원가입 / 로그인 (JWT Access 30분 + Refresh 7일) | ✅ |
| 토큰 자동 갱신 (Axios 인터셉터) | ✅ |
| 역할 기반 접근 제어 (STUDENT / PROFESSOR / TA) | ✅ |
| 프로젝트 CRUD + 초대 코드 생성/참여 | ✅ |
| 데이터 수집 동의 온보딩 플로우 | ✅ |
| Zustand 전역 상태 관리 | ✅ |

### ✅ Phase 3 — 칸반 & 회의록 (완료)

| 항목 | 상태 |
|------|------|
| 태스크 CRUD API (생성/수정/삭제/상태변경) | ✅ |
| 칸반 보드 3단 컬럼 (To Do / In Progress / Done) | ✅ |
| 드래그 앤 드롭 (@dnd-kit) + 상태 즉시 반영 | ✅ |
| 태스크 우선순위·태그 필터 | ✅ |
| 회의록 CRUD API | ✅ |
| 6자리 체크인 코드 생성 + 출석 확인 | ✅ |
| 액션 아이템 → 태스크 자동 생성 | ✅ |
| 모든 활동 → `activity_logs` 자동 기록 | ✅ |

### ✅ Phase 4 — Hash Vault & Score Engine (완료)

| 항목 | 상태 |
|------|------|
| 파일 업로드 → SHA-256 해시 고정 (변조 방지) | ✅ |
| 파일 버전 관리 (재업로드 시 버전 증가) | ✅ |
| 변조 감지 API (해시 불일치 시 Alert 생성) | ✅ |
| 드래그 앤 드롭 파일 업로드 UI | ✅ |
| 활동 로그 기반 기여도 점수 산출 (Score Engine) | ✅ |
| 항목별 가중치 (태스크 35% / 회의 30% / 파일 20% / 기타 15%) | ✅ |
| 팀 평균 대비 정규화 (상한 150%) | ✅ |
| 등급 산출 (A≥120 / B≥100 / C≥80 / D≥60 / F<60) | ✅ |
| 경보 시스템 (불균형 >40%p / 과부하 >60% / 이탈 14일) | ✅ |
| 30분 자동 재계산 스케줄러 | ✅ |
| 기여도 리포트 페이지 (등급 + 점수 세부 내역) | ✅ |

---

## 전체 API 엔드포인트

```
POST   /api/auth/signup          회원가입
POST   /api/auth/login           로그인 (JWT 발급)
POST   /api/auth/refresh         토큰 갱신
POST   /api/auth/logout          로그아웃

GET    /api/projects             내 프로젝트 목록
POST   /api/projects             프로젝트 생성
POST   /api/projects/join        초대 코드로 참여

GET    /api/projects/:id/tasks         태스크 목록
POST   /api/projects/:id/tasks         태스크 생성
PUT    /api/projects/:id/tasks/:tid    태스크 수정
PATCH  /api/projects/:id/tasks/:tid/status  상태 변경
DELETE /api/projects/:id/tasks/:tid    태스크 삭제

GET    /api/projects/:id/meetings              회의 목록
POST   /api/projects/:id/meetings              회의 생성
GET    /api/projects/:id/meetings/:mid         회의 상세
PUT    /api/projects/:id/meetings/:mid         회의 수정
DELETE /api/projects/:id/meetings/:mid         회의 삭제
POST   /api/projects/:id/meetings/:mid/checkin     체크인
POST   /api/projects/:id/meetings/:mid/action-items 액션아이템 → 태스크

POST   /api/projects/:id/files           파일 업로드
GET    /api/projects/:id/files           파일 목록
GET    /api/files/:fid/download          파일 다운로드
POST   /api/files/:fid/verify            변조 감지 검증

GET    /api/projects/:id/scores              기여도 리포트 조회
POST   /api/projects/:id/scores/calculate   수동 재계산
GET    /api/projects/:id/alerts             미해결 경보 목록
PATCH  /api/projects/:id/alerts/:aid/resolve 경보 해결 처리
```

---

## 코드 규모

| 영역 | 수치 |
|------|------|
| Java 파일 (백엔드) | 58개 |
| 백엔드 코드 라인 | ~3,300줄 |
| TypeScript/TSX 파일 (프론트) | 18개 |
| DB 마이그레이션 파일 | 8개 (V1~V8) |
| Docker 서비스 | 4개 (db / backend / frontend / nginx) |

---

## 프론트엔드 페이지 구성

```
/auth                           로그인 / 회원가입
/dashboard                      내 프로젝트 목록 + 생성/참여
/projects/[id]/board            칸반 보드 (드래그앤드롭 + 필터)
/projects/[id]/meetings         회의록 목록
/projects/[id]/meetings/[mid]   회의 상세 (체크인 코드, 참석자, 결정사항)
/projects/[id]/vault            Hash Vault (파일 업로드/검증)
/projects/[id]/report           기여도 리포트 (등급, 점수, 경보)
```

---

### ✅ Phase 5 — 교수 대시보드 & 배포 최적화 (완료)

| 항목 | 상태 |
|------|------|
| 교수 대시보드 (팀 목록 오버뷰 + Recharts 차트) | ✅ |
| 중간발표용 더미 시딩 API (Demo Seed) | ✅ |
| Docker Production 프로필 (docker-compose.prod.yml) | ✅ |
| SSL/HTTPS Nginx 보안 설정 (nginx.prod.conf) | ✅ |
| 통합 검증 및 데모 시나리오 준비 | ✅ |

---

## 기술 스택

```
Backend   : Java 17 + Spring Boot 3 + JPA/Hibernate + Flyway
Frontend  : Next.js 16 + TypeScript + TailwindCSS + Zustand + @dnd-kit
Database  : PostgreSQL 16
Auth      : JWT (HS384) + Refresh Token Rotation
Infra     : Docker Compose + Nginx (리버스 프록시)
File Hash : SHA-256 (StreamingMessageDigest, 8KB 버퍼)
Scheduler : @Scheduled (fixedDelay 30분)
```
