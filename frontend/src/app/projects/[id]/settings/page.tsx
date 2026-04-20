'use client';

import { useState, useEffect, useRef } from 'react';
import { useParams, useSearchParams } from 'next/navigation';
import api from '@/lib/api';

/* ── 타입 ── */
interface GitHubInstallation {
  id: number;
  repoFullName: string;
  hasToken: boolean;
  lastPolledAt: string | null;
  connectedAt: string;
}

interface GitHubMapping {
  id: number;
  userId: number;
  userName: string;
  githubLogin: string;
}

interface NotionInstallation {
  id: number;
  hasToken: boolean;
  databaseId: string | null;
  workspaceName: string | null;
  lastPolledAt: string | null;
  connectedAt: string;
}

interface NotionMapping {
  id: number;
  userId: number;
  userName: string;
  notionUserId: string;
  notionUserName: string;
}

interface GoogleInstallation {
  projectId: number;
  connected: boolean;
  driveFolderId: string | null;
  sheetId: string | null;
  formId: string | null;
  connectedAt: string | null;
}

interface GoogleMapping {
  id: number;
  userId: number;
  userName: string;
  googleEmail: string;
}

interface Member {
  userId: number;
  userName: string;
}

/* ── 섹션 탭 ── */
type Tab = 'github' | 'notion' | 'google';

export default function SettingsPage() {
  const params       = useParams();
  const searchParams = useSearchParams();
  const projectId    = params.id as string;

  const initialTab = (searchParams.get('tab') as Tab | null) ?? 'github';
  const [activeTab, setActiveTab] = useState<Tab>(initialTab);
  const popupRef = useRef<Window | null>(null);
  const [members,   setMembers]   = useState<Member[]>([]);

  /* GitHub state */
  const [ghInst,       setGhInst]       = useState<GitHubInstallation | null>(null);
  const [ghMappings,   setGhMappings]   = useState<GitHubMapping[]>([]);
  const [ghRepo,       setGhRepo]       = useState('');
  const [ghToken,      setGhToken]      = useState('');
  const [ghSecret,     setGhSecret]     = useState('');
  const [ghMapUser,    setGhMapUser]    = useState('');
  const [ghMapLogin,   setGhMapLogin]   = useState('');
  const [ghSaving,     setGhSaving]     = useState(false);
  const [ghPolling,    setGhPolling]    = useState(false);
  const [ghPollMsg,    setGhPollMsg]    = useState<string | null>(null);

  /* Google state */
  const [gInst,        setGInst]        = useState<GoogleInstallation | null>(null);
  const [gMappings,    setGMappings]    = useState<GoogleMapping[]>([]);
  const [gDriveFolder, setGDriveFolder] = useState('');
  const [gSheetId,     setGSheetId]     = useState('');
  const [gFormId,      setGFormId]      = useState('');
  const [gMapUser,     setGMapUser]     = useState('');
  const [gMapEmail,    setGMapEmail]    = useState('');
  const [gSaving,      setGSaving]      = useState(false);
  const [gPolling,     setGPolling]     = useState(false);
  const [gPollMsg,     setGPollMsg]     = useState<string | null>(null);

  /* Notion state */
  const [ntInst,       setNtInst]       = useState<NotionInstallation | null>(null);
  const [ntMappings,   setNtMappings]   = useState<NotionMapping[]>([]);
  const [ntToken,      setNtToken]      = useState('');
  const [ntDbId,       setNtDbId]       = useState('');
  const [ntWsName,     setNtWsName]     = useState('');
  const [ntMapUser,    setNtMapUser]    = useState('');
  const [ntMapNtId,    setNtMapNtId]    = useState('');
  const [ntMapNtName,  setNtMapNtName]  = useState('');
  const [ntSaving,     setNtSaving]     = useState(false);
  const [ntPolling,    setNtPolling]    = useState(false);
  const [ntPollMsg,    setNtPollMsg]    = useState<string | null>(null);

  const [loading, setLoading] = useState(true);

  useEffect(() => { fetchAll(); }, [projectId]);


  async function fetchAll() {
    setLoading(true);
    try {
      const [ghRes, ghMapRes, ntRes, ntMapRes, gRes, gMapRes, memRes] = await Promise.allSettled([
        api.get(`/projects/${projectId}/github`),
        api.get(`/projects/${projectId}/github/mappings`),
        api.get(`/projects/${projectId}/notion`),
        api.get(`/projects/${projectId}/notion/mappings`),
        api.get(`/projects/${projectId}/google`),
        api.get(`/projects/${projectId}/google/mappings`),
        api.get(`/projects/${projectId}/members`),
      ]);

      if (ghRes.status === 'fulfilled' && ghRes.value.data) {
        const g = ghRes.value.data as GitHubInstallation;
        setGhInst(g); setGhRepo(g.repoFullName ?? '');
      }
      if (ghMapRes.status === 'fulfilled') setGhMappings(ghMapRes.value.data);
      if (ntRes.status    === 'fulfilled' && ntRes.value.data) {
        const n = ntRes.value.data as NotionInstallation;
        setNtInst(n);
        setNtDbId(n.databaseId ?? '');
        setNtWsName(n.workspaceName ?? '');
      }
      if (ntMapRes.status === 'fulfilled') setNtMappings(ntMapRes.value.data);
      if (gRes.status === 'fulfilled' && gRes.value.data?.connected) {
        const g = gRes.value.data as GoogleInstallation;
        setGInst(g);
        setGDriveFolder(g.driveFolderId ?? '');
        setGSheetId(g.sheetId ?? '');
        setGFormId(g.formId ?? '');
      }
      if (gMapRes.status === 'fulfilled') setGMappings(gMapRes.value.data);
      if (memRes.status === 'fulfilled') {
        setMembers((memRes.value.data ?? []).map((m: Record<string, unknown>) => ({
          userId:   m.userId,
          userName: m.name,
        })));
      }
    } finally { setLoading(false); }
  }

  /* ── GitHub 핸들러 ── */
  async function handleGhLink(e: React.FormEvent) {
    e.preventDefault(); setGhSaving(true);
    try {
      const res = await api.post(`/projects/${projectId}/github/link`, {
        repoFullName: ghRepo,
        githubToken:  ghToken  || null,
        webhookSecret: ghSecret || null,
      });
      setGhInst(res.data); setGhToken(''); setGhSecret('');
      alert('GitHub 연동 완료');
    } catch { alert('연동 실패. 레포 이름과 토큰을 확인해 주세요.'); }
    finally { setGhSaving(false); }
  }

  async function handleGhUnlink() {
    if (!confirm('GitHub 연동을 해제하시겠습니까?')) return;
    await api.delete(`/projects/${projectId}/github/unlink`);
    setGhInst(null); setGhRepo('');
  }

  async function handleGhPoll() {
    setGhPolling(true); setGhPollMsg(null);
    try {
      const res = await api.post(`/projects/${projectId}/github/poll`);
      const r = res.data;
      setGhPollMsg(`완료: ${r.repo} — ${r.commitsProcessed}개 이벤트`);
    } catch { setGhPollMsg('폴링 실패'); }
    finally { setGhPolling(false); }
  }

  async function handleGhAddMapping(e: React.FormEvent) {
    e.preventDefault();
    try {
      const res = await api.post(`/projects/${projectId}/github/mappings`, {
        userId: Number(ghMapUser), githubLogin: ghMapLogin,
      });
      setGhMappings(p => [...p, res.data]); setGhMapUser(''); setGhMapLogin('');
    } catch { alert('매핑 추가 실패'); }
  }

  async function handleGhDelMapping(id: number) {
    await api.delete(`/projects/${projectId}/github/mappings/${id}`);
    setGhMappings(p => p.filter(m => m.id !== id));
  }

  /* ── Notion 핸들러 ── */
  async function handleNtLink(e: React.FormEvent) {
    e.preventDefault(); setNtSaving(true);
    try {
      const res = await api.post(`/projects/${projectId}/notion/link`, {
        integrationToken: ntToken  || null,
        databaseId:       ntDbId   || null,
        workspaceName:    ntWsName || null,
      });
      setNtInst(res.data); setNtToken('');
      alert('Notion 연동 완료');
    } catch { alert('연동 실패. Integration Token을 확인해 주세요.'); }
    finally { setNtSaving(false); }
  }

  async function handleNtUnlink() {
    if (!confirm('Notion 연동을 해제하시겠습니까?')) return;
    await api.delete(`/projects/${projectId}/notion/unlink`);
    setNtInst(null); setNtDbId(''); setNtWsName('');
  }

  async function handleNtPoll() {
    setNtPolling(true); setNtPollMsg(null);
    try {
      const res = await api.post(`/projects/${projectId}/notion/poll`);
      const r = res.data;
      setNtPollMsg(`완료 — 새 페이지 ${r.created}개, 수정 ${r.edited}개`);
    } catch { setNtPollMsg('폴링 실패'); }
    finally { setNtPolling(false); }
  }

  async function handleNtAddMapping(e: React.FormEvent) {
    e.preventDefault();
    try {
      const res = await api.post(`/projects/${projectId}/notion/mappings`, {
        userId: Number(ntMapUser), notionUserId: ntMapNtId, notionUserName: ntMapNtName,
      });
      setNtMappings(p => [...p, res.data]);
      setNtMapUser(''); setNtMapNtId(''); setNtMapNtName('');
    } catch { alert('매핑 추가 실패'); }
  }

  async function handleNtDelMapping(id: number) {
    await api.delete(`/projects/${projectId}/notion/mappings/${id}`);
    setNtMappings(p => p.filter(m => m.id !== id));
  }

  /* ── Google 핸들러 ── */
  async function handleGoogleConnect() {
    try {
      const res = await api.get(`/projects/${projectId}/google/auth`);
      const popup = window.open(
        res.data.url,
        'google-oauth',
        'width=600,height=700,left=400,top=100',
      );
      if (!popup) { alert('팝업이 차단되었습니다. 팝업 허용 후 다시 시도해 주세요.'); return; }
      popupRef.current = popup;

      // 팝업이 닫힐 때까지 500ms 간격으로 확인 후 데이터 갱신
      const timer = setInterval(() => {
        if (popup.closed) {
          clearInterval(timer);
          // 팝업 닫히면 현재 URL에 tab=google 붙여서 새로고침 (가장 확실한 방법)
          window.location.href = window.location.pathname + '?tab=google';
        }
      }, 500);
    } catch { alert('Google OAuth URL 가져오기 실패'); }
  }

  async function handleGoogleUnlink() {
    if (!confirm('Google 연동을 해제하시겠습니까?')) return;
    await api.delete(`/projects/${projectId}/google/unlink`);
    setGInst(null); setGDriveFolder(''); setGSheetId(''); setGFormId('');
  }

  async function handleGoogleResources(e: React.FormEvent) {
    e.preventDefault(); setGSaving(true);
    try {
      const res = await api.put(`/projects/${projectId}/google/resources`, {
        driveFolderId: gDriveFolder || null,
        sheetId:       gSheetId    || null,
        formId:        gFormId     || null,
      });
      setGInst(res.data);
      alert('리소스 ID 저장 완료');
    } catch { alert('저장 실패'); }
    finally { setGSaving(false); }
  }

  async function handleGooglePoll() {
    setGPolling(true); setGPollMsg(null);
    try {
      const res = await api.post(`/projects/${projectId}/google/poll`);
      const r = res.data;
      setGPollMsg(`완료 — Drive ${r.driveFiles}개, Sheets ${r.sheetsEdits}개, Forms ${r.formResponses}개`);
    } catch { setGPollMsg('폴링 실패'); }
    finally { setGPolling(false); }
  }

  async function handleGoogleAddMapping(e: React.FormEvent) {
    e.preventDefault();
    try {
      const res = await api.post(`/projects/${projectId}/google/mappings`, {
        userId: Number(gMapUser), googleEmail: gMapEmail,
      });
      setGMappings(p => [...p, res.data]); setGMapUser(''); setGMapEmail('');
    } catch { alert('매핑 추가 실패'); }
  }

  async function handleGoogleDelMapping(id: number) {
    await api.delete(`/projects/${projectId}/google/mappings/${id}`);
    setGMappings(p => p.filter(m => m.id !== id));
  }

  if (loading) return (
    <div className="flex items-center justify-center h-full text-slate-400 text-sm">로딩 중…</div>
  );

  return (
    <div className="h-full overflow-y-auto p-6 max-w-2xl mx-auto space-y-6">
      <h1 className="text-xl font-bold text-white">외부 연동 설정</h1>

      {/* 탭 */}
      <div className="flex gap-1 bg-slate-800/60 rounded-xl p-1 w-fit">
        {([
          { id: 'github', label: ' GitHub', connected: !!ghInst },
          { id: 'notion', label: 'N  Notion', connected: !!ntInst },
          { id: 'google', label: 'G  Google', connected: !!gInst?.connected },
        ] as const).map(t => (
          <button
            key={t.id}
            onClick={() => setActiveTab(t.id)}
            className={`relative px-5 py-2 rounded-lg text-sm font-semibold transition-all ${
              activeTab === t.id
                ? 'bg-violet-600 text-white shadow'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            {t.label}
            {t.connected && (
              <span className="absolute -top-1 -right-1 w-2 h-2 rounded-full bg-emerald-400 ring-2 ring-slate-900" />
            )}
          </button>
        ))}
      </div>

      {/* ══════════════════ GitHub 탭 ══════════════════ */}
      {activeTab === 'github' && (
        <div className="space-y-6">
          {/* 레포 연동 */}
          <Section title="레포지토리 연동" badge={ghInst ? '연동됨' : undefined}>
            {ghInst && (
              <InfoGrid rows={[
                ['레포', ghInst.repoFullName],
                ['토큰', ghInst.hasToken ? '✅ 등록됨' : '❌ 미등록'],
                ['마지막 폴링', ghInst.lastPolledAt ? new Date(ghInst.lastPolledAt).toLocaleString('ko-KR') : '없음'],
              ]} />
            )}
            <form onSubmit={handleGhLink} className="space-y-3">
              <Field label="레포지토리 (owner/repo) *">
                <input value={ghRepo} onChange={e => setGhRepo(e.target.value)}
                  placeholder="octocat/Hello-World" required className={inputCls} />
              </Field>
              <Field label="Personal Access Token (갱신 시에만)">
                <input type="password" value={ghToken} onChange={e => setGhToken(e.target.value)}
                  placeholder="ghp_xxxxxxxxxxxx" className={inputCls} />
              </Field>
              <Field label="Webhook Secret (선택)">
                <input type="password" value={ghSecret} onChange={e => setGhSecret(e.target.value)}
                  placeholder="서명 시크릿" className={inputCls} />
              </Field>
              <ActionRow>
                <Btn type="submit" disabled={ghSaving}>{ghSaving ? '저장 중…' : ghInst ? '업데이트' : '연동하기'}</Btn>
                {ghInst && <>
                  <Btn variant="gray" onClick={handleGhPoll} disabled={ghPolling}>
                    {ghPolling ? '폴링 중…' : '즉시 폴링'}
                  </Btn>
                  <Btn variant="danger" onClick={handleGhUnlink}>연동 해제</Btn>
                </>}
              </ActionRow>
              {ghPollMsg && <Msg>{ghPollMsg}</Msg>}
            </form>
          </Section>

          {/* 유저 매핑 */}
          <Section title="GitHub 계정 매핑">
            <p className="text-xs text-slate-500">팀원의 GitHub 로그인과 플랫폼 계정을 연결합니다.</p>
            <MappingList
              items={ghMappings.map(m => ({ id: m.id, label: m.userName, sub: `@${m.githubLogin}` }))}
              onDelete={handleGhDelMapping}
            />
            <form onSubmit={handleGhAddMapping} className="flex gap-2">
              <MemberSelect value={ghMapUser} onChange={setGhMapUser}
                members={members.filter(m => !ghMappings.some(mp => mp.userId === m.userId))} />
              <input value={ghMapLogin} onChange={e => setGhMapLogin(e.target.value)}
                placeholder="GitHub 로그인" required className={`${inputCls} flex-1`} />
              <Btn type="submit">추가</Btn>
            </form>
          </Section>

          {/* Webhook 안내 */}
          <Section title="Webhook 설정 안내">
            <p className="text-xs text-slate-500 leading-relaxed">
              GitHub 레포 Settings → Webhooks → Add webhook
            </p>
            <CodeBlock rows={[
              ['Payload URL', 'https://your-domain/api/github/webhook'],
              ['Content type', 'application/json'],
              ['Events', 'Pushes, Pull requests'],
            ]} />
          </Section>
        </div>
      )}

      {/* ══════════════════ Notion 탭 ══════════════════ */}
      {activeTab === 'notion' && (
        <div className="space-y-6">
          {/* 토큰 연동 */}
          <Section title="Integration 연동" badge={ntInst ? '연동됨' : undefined}>
            {ntInst && (
              <InfoGrid rows={[
                ['워크스페이스', ntInst.workspaceName ?? '—'],
                ['Database ID',  ntInst.databaseId   ?? '전체 검색'],
                ['토큰',         ntInst.hasToken ? '✅ 등록됨' : '❌ 미등록'],
                ['마지막 폴링',  ntInst.lastPolledAt ? new Date(ntInst.lastPolledAt).toLocaleString('ko-KR') : '없음'],
              ]} />
            )}
            <form onSubmit={handleNtLink} className="space-y-3">
              <Field label="Integration Token *">
                <input type="password" value={ntToken} onChange={e => setNtToken(e.target.value)}
                  placeholder="secret_xxxxxxxxxxxx" className={inputCls} />
                <p className="text-[11px] text-slate-600 mt-1">
                  갱신 시에만 입력. Notion → 설정 → 연동 → 새 API 통합에서 발급.
                </p>
              </Field>
              <Field label="Database ID (선택 — 없으면 전체 워크스페이스 검색)">
                <input value={ntDbId} onChange={e => setNtDbId(e.target.value)}
                  placeholder="xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" className={inputCls} />
              </Field>
              <Field label="워크스페이스 이름 (표시용)">
                <input value={ntWsName} onChange={e => setNtWsName(e.target.value)}
                  placeholder="Team Blackbox Workspace" className={inputCls} />
              </Field>
              <ActionRow>
                <Btn type="submit" disabled={ntSaving}>{ntSaving ? '저장 중…' : ntInst ? '업데이트' : '연동하기'}</Btn>
                {ntInst && <>
                  <Btn variant="gray" onClick={handleNtPoll} disabled={ntPolling}>
                    {ntPolling ? '폴링 중…' : '즉시 폴링'}
                  </Btn>
                  <Btn variant="danger" onClick={handleNtUnlink}>연동 해제</Btn>
                </>}
              </ActionRow>
              {ntPollMsg && <Msg>{ntPollMsg}</Msg>}
            </form>
          </Section>

          {/* 유저 매핑 */}
          <Section title="Notion 계정 매핑">
            <p className="text-xs text-slate-500">
              팀원의 Notion 사용자 ID와 플랫폼 계정을 연결합니다.
              이메일이 일치하면 자동 매핑됩니다.
            </p>
            <MappingList
              items={ntMappings.map(m => ({
                id: m.id,
                label: m.userName,
                sub: `${m.notionUserName ?? ''} (${m.notionUserId.slice(0, 8)}…)`,
              }))}
              onDelete={handleNtDelMapping}
            />
            <form onSubmit={handleNtAddMapping} className="space-y-2">
              <div className="flex gap-2">
                <MemberSelect value={ntMapUser} onChange={setNtMapUser}
                  members={members.filter(m => !ntMappings.some(mp => mp.userId === m.userId))} />
                <input value={ntMapNtId} onChange={e => setNtMapNtId(e.target.value)}
                  placeholder="Notion User ID (UUID)" required className={`${inputCls} flex-1`} />
              </div>
              <div className="flex gap-2">
                <input value={ntMapNtName} onChange={e => setNtMapNtName(e.target.value)}
                  placeholder="Notion 표시 이름 (선택)" className={`${inputCls} flex-1`} />
                <Btn type="submit">추가</Btn>
              </div>
            </form>
          </Section>

          {/* Notion 연동 방법 */}
          <Section title="연동 방법">
            <ol className="text-xs text-slate-400 space-y-2 leading-relaxed list-decimal list-inside">
              <li>notion.so → 설정 → 연동 → <strong className="text-slate-200">새 API 통합 만들기</strong></li>
              <li>통합 생성 후 <strong className="text-slate-200">Internal Integration Token</strong> 복사</li>
              <li>연동할 페이지/데이터베이스 우측 상단 <strong className="text-slate-200">⋯ → 연결 추가</strong>에서 방금 만든 통합 선택</li>
              <li>위 폼에 토큰 입력 후 저장</li>
              <li>(선택) 특정 DB만 폴링하려면 DB 페이지 URL에서 ID 추출 후 입력</li>
            </ol>
            <div className="mt-3 bg-slate-800/60 rounded-lg px-3 py-2 text-xs font-mono text-slate-400">
              DB URL 예시: notion.so/<span className="text-violet-300">{'<workspace>'}</span>/<span className="text-emerald-300">{'<database-id>'}</span>?v=...
            </div>
          </Section>
        </div>
      )}

      {/* ══════════════════ Google 탭 ══════════════════ */}
      {activeTab === 'google' && (
        <div className="space-y-6">
          {/* OAuth 연동 */}
          <Section title="Google 계정 연동" badge={gInst?.connected ? '연동됨' : undefined}>
            {gInst?.connected ? (
              <InfoGrid rows={[
                ['연동일시', gInst.connectedAt ? new Date(gInst.connectedAt).toLocaleString('ko-KR') : '—'],
                ['Drive 폴더', gInst.driveFolderId ?? '전체'],
                ['Sheet ID',   gInst.sheetId      ?? '미설정'],
                ['Form ID',    gInst.formId        ?? '미설정'],
              ]} />
            ) : (
              <p className="text-xs text-slate-500">
                Google Drive, Sheets, Forms 활동을 자동으로 수집합니다.
                아래 버튼을 누르면 Google 계정 인증 페이지로 이동합니다.
              </p>
            )}
            <ActionRow>
              {!gInst?.connected && (
                <Btn onClick={handleGoogleConnect}>Google 연동하기</Btn>
              )}
              {gInst?.connected && (
                <>
                  <Btn variant="gray" onClick={handleGooglePoll} disabled={gPolling}>
                    {gPolling ? '폴링 중…' : '즉시 폴링'}
                  </Btn>
                  <Btn variant="danger" onClick={handleGoogleUnlink}>연동 해제</Btn>
                </>
              )}
            </ActionRow>
            {gPollMsg && <Msg>{gPollMsg}</Msg>}
          </Section>

          {/* 리소스 ID 설정 */}
          {gInst?.connected && (
            <Section title="리소스 ID 설정">
              <p className="text-xs text-slate-500">
                특정 Drive 폴더, Spreadsheet, Form만 모니터링하려면 ID를 입력하세요.
                비워두면 전체 Drive를 스캔합니다.
              </p>
              <form onSubmit={handleGoogleResources} className="space-y-3">
                <Field label="Drive 폴더 ID (선택)">
                  <input value={gDriveFolder} onChange={e => setGDriveFolder(e.target.value)}
                    placeholder="1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs" className={inputCls} />
                </Field>
                <Field label="Spreadsheet ID (선택)">
                  <input value={gSheetId} onChange={e => setGSheetId(e.target.value)}
                    placeholder="1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs" className={inputCls} />
                </Field>
                <Field label="Form ID (선택)">
                  <input value={gFormId} onChange={e => setGFormId(e.target.value)}
                    placeholder="1FAIpQLSe..." className={inputCls} />
                </Field>
                <ActionRow>
                  <Btn type="submit" disabled={gSaving}>{gSaving ? '저장 중…' : '저장'}</Btn>
                </ActionRow>
              </form>
              <div className="mt-2 bg-slate-800/60 rounded-lg px-3 py-2 text-xs font-mono text-slate-400">
                Drive URL 예시: drive.google.com/drive/folders/<span className="text-violet-300">{'<folder-id>'}</span>
              </div>
            </Section>
          )}

          {/* 유저 매핑 */}
          {gInst?.connected && (
            <Section title="Google 계정 매핑">
              <p className="text-xs text-slate-500">
                팀원의 Google 이메일과 플랫폼 계정을 연결합니다. 이메일이 일치하면 자동 매핑됩니다.
              </p>
              <MappingList
                items={gMappings.map(m => ({ id: m.id, label: m.userName, sub: m.googleEmail }))}
                onDelete={handleGoogleDelMapping}
              />
              <form onSubmit={handleGoogleAddMapping} className="flex gap-2">
                <MemberSelect value={gMapUser} onChange={setGMapUser}
                  members={members.filter(m => !gMappings.some(mp => mp.userId === m.userId))} />
                <input value={gMapEmail} onChange={e => setGMapEmail(e.target.value)}
                  placeholder="google@gmail.com" required type="email" className={`${inputCls} flex-1`} />
                <Btn type="submit">추가</Btn>
              </form>
            </Section>
          )}
        </div>
      )}
    </div>
  );
}

/* ── 재사용 서브 컴포넌트 ── */

const inputCls = 'w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-violet-500';

function Section({ title, badge, children }: {
  title: string; badge?: string; children: React.ReactNode;
}) {
  return (
    <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-4">
      <div className="flex items-center gap-3">
        <h2 className="font-semibold text-slate-200">{title}</h2>
        {badge && (
          <span className="text-xs px-2 py-0.5 rounded-full bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">
            {badge}
          </span>
        )}
      </div>
      {children}
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <label className="text-xs text-slate-500 mb-1 block">{label}</label>
      {children}
    </div>
  );
}

function ActionRow({ children }: { children: React.ReactNode }) {
  return <div className="flex flex-wrap gap-2 pt-1">{children}</div>;
}

function Btn({ children, variant = 'primary', type = 'button', disabled, onClick }: {
  children: React.ReactNode;
  variant?: 'primary' | 'gray' | 'danger';
  type?: 'button' | 'submit';
  disabled?: boolean;
  onClick?: () => void;
}) {
  const cls = {
    primary: 'bg-violet-600 hover:bg-violet-500 text-white',
    gray:    'bg-slate-700 hover:bg-slate-600 text-white',
    danger:  'bg-red-500/20 hover:bg-red-500/30 text-red-400 border border-red-500/30',
  }[variant];
  return (
    <button type={type} onClick={onClick} disabled={disabled}
      className={`px-4 py-2 rounded-lg text-sm font-semibold transition-colors disabled:opacity-50 ${cls}`}>
      {children}
    </button>
  );
}

function Msg({ children }: { children: React.ReactNode }) {
  return <p className="text-xs text-slate-400 bg-slate-800 rounded-lg px-3 py-2">{children}</p>;
}

function InfoGrid({ rows }: { rows: [string, string][] }) {
  return (
    <div className="text-sm text-slate-400 space-y-1">
      {rows.map(([k, v]) => (
        <p key={k}><span className="text-slate-500">{k}:</span>{' '}
          <span className="text-white font-mono text-xs">{v}</span>
        </p>
      ))}
    </div>
  );
}

function CodeBlock({ rows }: { rows: [string, string][] }) {
  return (
    <div className="space-y-2 text-xs font-mono">
      {rows.map(([k, v]) => (
        <div key={k} className="bg-slate-800 rounded-lg px-3 py-2">
          <span className="text-slate-500">{k}: </span>
          <span className="text-violet-300">{v}</span>
        </div>
      ))}
    </div>
  );
}

function MappingList({ items, onDelete }: {
  items: { id: number; label: string; sub: string }[];
  onDelete: (id: number) => void;
}) {
  if (items.length === 0) return null;
  return (
    <ul className="space-y-2">
      {items.map(m => (
        <li key={m.id} className="flex items-center justify-between bg-slate-800 rounded-lg px-3 py-2 text-sm">
          <span className="text-slate-300">
            <span className="font-semibold text-white">{m.label}</span>
            <span className="text-slate-500 mx-2">→</span>
            <span className="font-mono text-violet-300">{m.sub}</span>
          </span>
          <button onClick={() => onDelete(m.id)}
            className="text-slate-500 hover:text-red-400 transition-colors text-xs px-2 py-1 rounded hover:bg-red-500/10">
            삭제
          </button>
        </li>
      ))}
    </ul>
  );
}

function MemberSelect({ value, onChange, members }: {
  value: string; onChange: (v: string) => void; members: Member[];
}) {
  return (
    <select value={value} onChange={e => onChange(e.target.value)} required
      className="flex-1 bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-violet-500">
      <option value="">팀원 선택</option>
      {members.map(m => <option key={m.userId} value={m.userId}>{m.userName}</option>)}
    </select>
  );
}
