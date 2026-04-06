'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import api from '@/lib/api';
import { useAuthStore } from '@/store/authStore';

interface Project {
  id: number;
  name: string;
  description: string;
  inviteCode: string;
  active: boolean;
  memberCount: number;
  createdAt: string;
}

export default function DashboardPage() {
  const router = useRouter();
  const { user, isAuthenticated, logout } = useAuthStore();
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showJoinModal, setShowJoinModal] = useState(false);
  const [showConsentModal, setShowConsentModal] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) {
      router.replace('/auth');
      return;
    }
    checkConsent();
    fetchProjects();
  }, [isAuthenticated]);

  const checkConsent = async () => {
    try {
      const res = await api.get('/users/me');
      if (!res.data.dataCollectionConsent) {
        setShowConsentModal(true);
      }
    } catch (err) {
      console.error(err);
    }
  };

  const fetchProjects = async () => {
    try {
      const res = await api.get('/projects');
      setProjects(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    const { refreshToken } = useAuthStore.getState();
    if (refreshToken) {
      try { await api.post('/auth/logout', { refreshToken }); } catch {}
    }
    logout();
    router.replace('/auth');
  };

  if (!isAuthenticated) return null;

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900">
      {/* Header */}
      <header className="border-b border-slate-700/50 bg-slate-900/80 backdrop-blur sticky top-0 z-10">
        <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-violet-500 to-indigo-600 flex items-center justify-center text-white font-black text-sm">B</div>
            <span className="text-lg font-bold text-white">Blackbox</span>
          </div>
          <div className="flex items-center gap-4">
            <span className="text-slate-400 text-sm">{user?.name} <span className="px-2 py-0.5 rounded-full bg-violet-900/50 text-violet-300 text-xs ml-1">{user?.role}</span></span>
            <button onClick={handleLogout} className="text-sm text-slate-400 hover:text-white transition-colors">로그아웃</button>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-6 py-10">
        {/* Hero */}
        <div className="mb-10">
          <h1 className="text-3xl font-bold text-white mb-1">내 프로젝트</h1>
          <p className="text-slate-400">팀 프로젝트를 생성하거나 초대 코드로 참여하세요.</p>
        </div>

        {/* Actions */}
        <div className="flex gap-3 mb-8">
          <button
            id="btn-create-project"
            onClick={() => setShowCreateModal(true)}
            className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-500 hover:to-indigo-500 text-white font-semibold text-sm transition-all shadow-lg shadow-violet-900/30"
          >
            + 프로젝트 생성
          </button>
          <button
            id="btn-join-project"
            onClick={() => setShowJoinModal(true)}
            className="px-5 py-2.5 rounded-xl border border-slate-600 hover:border-violet-500 text-slate-300 hover:text-white font-semibold text-sm transition-all"
          >
            초대 코드로 참여
          </button>
          {user?.role === 'PROFESSOR' && (
            <Link
              href="/dashboard/professor"
              className="px-5 py-2.5 rounded-xl border border-emerald-600/50 bg-emerald-900/20 hover:bg-emerald-900/40 text-emerald-300 font-semibold text-sm transition-all ml-auto"
            >
              📊 교수 오버뷰 대시보드
            </Link>
          )}
        </div>

        {/* Project Grid */}
        {loading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-48 rounded-2xl bg-slate-800/60 animate-pulse" />
            ))}
          </div>
        ) : projects.length === 0 ? (
          <div className="text-center py-24 text-slate-500">
            <div className="text-5xl mb-4">📁</div>
            <p className="text-lg">아직 참여한 프로젝트가 없습니다.</p>
            <p className="text-sm mt-1">위 버튼으로 새 프로젝트를 만들어보세요.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {projects.map((p) => (
              <Link key={p.id} href={`/projects/${p.id}`}>
                <div className="group bg-slate-800/60 hover:bg-slate-800/90 border border-slate-700/50 hover:border-violet-500/50 rounded-2xl p-6 transition-all cursor-pointer shadow-lg hover:shadow-violet-900/20 hover:-translate-y-0.5">
                  <div className="flex items-start justify-between mb-3">
                    <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-violet-500/20 to-indigo-600/20 flex items-center justify-center text-violet-400 font-bold text-lg border border-violet-500/20">
                      {p.name.charAt(0).toUpperCase()}
                    </div>
                    <span className={`text-xs px-2 py-0.5 rounded-full ${p.active ? 'bg-green-900/40 text-green-400' : 'bg-slate-700 text-slate-400'}`}>
                      {p.active ? '진행중' : '종료'}
                    </span>
                  </div>
                  <h3 className="font-semibold text-white group-hover:text-violet-300 transition-colors mb-1">{p.name}</h3>
                  <p className="text-slate-400 text-sm line-clamp-2 mb-4">{p.description ?? '설명 없음'}</p>
                  <div className="flex items-center justify-between text-xs text-slate-500">
                    <span>👥 {p.memberCount}명</span>
                    <span className="font-mono bg-slate-900/60 px-2 py-0.5 rounded">{p.inviteCode}</span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </main>

      {/* Create Modal */}
      {showCreateModal && (
        <CreateProjectModal
          onClose={() => setShowCreateModal(false)}
          onCreated={() => { setShowCreateModal(false); fetchProjects(); }}
        />
      )}

      {/* Join Modal */}
      {showJoinModal && (
        <JoinProjectModal
          onClose={() => setShowJoinModal(false)}
          onJoined={(projectId) => { 
            setShowJoinModal(false); 
            router.push(`/projects/${projectId}`);
          }}
        />
      )}

      {/* Consent Onboarding Modal */}
      {showConsentModal && (
        <ConsentModal
          onClose={() => {}} // User MUST consent to continue
          onConsented={() => setShowConsentModal(false)}
        />
      )}
    </div>
  );
}

/* ── Create Project Modal ─────────────────────────────────── */
function CreateProjectModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [form, setForm] = useState({ name: '', description: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await api.post('/projects', form);
      onCreated();
    } catch (err: any) {
      setError(err.response?.data?.message ?? '오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal title="새 프로젝트 생성" onClose={onClose}>
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm text-slate-300 mb-1">프로젝트명 *</label>
          <input id="create-project-name" className="input-field" required value={form.name} onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))} placeholder="팀 캡스톤 프로젝트 2024" />
        </div>
        <div>
          <label className="block text-sm text-slate-300 mb-1">설명</label>
          <input id="create-project-desc" className="input-field" value={form.description} onChange={(e) => setForm((p) => ({ ...p, description: e.target.value }))} placeholder="프로젝트 소개" />
        </div>
        {error && <p className="text-red-400 text-sm">{error}</p>}
        <div className="flex gap-2 pt-2">
          <button type="button" onClick={onClose} className="flex-1 btn-secondary">취소</button>
          <button type="submit" disabled={loading} className="flex-1 btn-primary">{loading ? '생성 중...' : '생성'}</button>
        </div>
      </form>
    </Modal>
  );
}

/* ── Join Project Modal ──────────────────────────────────── */
function JoinProjectModal({ onClose, onJoined }: { onClose: () => void; onJoined: (projectId: number) => void }) {
  const [inviteCode, setInviteCode] = useState('');
  const [consent, setConsent] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await api.post('/projects/join', { inviteCode, dataCollectionConsent: consent });
      onJoined(res.data.id);
    } catch (err: any) {
      setError(err.response?.data?.message ?? '오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal title="초대 코드로 참여" onClose={onClose}>
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm text-slate-300 mb-1">초대 코드 *</label>
          <input id="join-invite-code" className="input-field font-mono tracking-widest" required value={inviteCode} onChange={(e) => setInviteCode(e.target.value.toUpperCase())} placeholder="ABC12345" maxLength={8} />
        </div>
        <label className="flex items-start gap-3 cursor-pointer">
          <input type="checkbox" id="join-consent" checked={consent} onChange={(e) => setConsent(e.target.checked)} className="mt-0.5 accent-violet-500" />
          <span className="text-sm text-slate-400">데이터 수집에 동의합니다. (활동 로그, 기여도 분석)</span>
        </label>
        {error && <p className="text-red-400 text-sm">{error}</p>}
        <div className="flex gap-2 pt-2">
          <button type="button" onClick={onClose} className="flex-1 btn-secondary">취소</button>
          <button type="submit" disabled={loading} className="flex-1 btn-primary">{loading ? '참여 중...' : '참여'}</button>
        </div>
      </form>
    </Modal>
  );
}

/* ── Generic Modal ───────────────────────────────────────── */
function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: React.ReactNode }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" onClick={onClose}>
      <div className="w-full max-w-md bg-slate-800 border border-slate-700/60 rounded-2xl p-6 shadow-2xl" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-lg font-bold text-white">{title}</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-white transition-colors text-xl leading-none">&times;</button>
        </div>
        {children}
      </div>
    </div>
  );
}

/* ── Consent Onboarding Modal ────────────────────────────── */
function ConsentModal({ onClose, onConsented }: { onClose: () => void; onConsented: () => void }) {
  const [consent, setConsent] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!consent) {
      setError('데이터 수집에 동의해야 서비스를 이용할 수 있습니다.');
      return;
    }
    
    setLoading(true);
    try {
      await api.put('/users/me/consent', { dataCollectionConsent: true });
      onConsented();
    } catch (err: any) {
      setError(err.response?.data?.message ?? '오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal title="시작하기 전에" onClose={onClose}>
      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="bg-slate-900/50 p-4 rounded-xl border border-slate-700">
          <h3 className="text-white font-semibold mb-2">데이터 수집 및 활용 안내</h3>
          <p className="text-sm text-slate-400 mb-2">
            Team Blackbox는 팀 프로젝트 기여도를 분석하기 위해 다음과 같은 데이터를 수집합니다:
          </p>
          <ul className="list-disc list-inside text-sm text-slate-400 space-y-1 mb-4">
            <li>칸반 보드 태스크 완료 이력</li>
            <li>회의록 작성 및 체크인 기록</li>
            <li>파일 업로드 이력 (Hash Vault)</li>
          </ul>
          <p className="text-xs text-slate-500">
            * 수집된 데이터는 교수님께 제공되는 대시보드의 기여도 산출 지표로만 활용됩니다. AI 분석은 별도의 동의 절차를 거칩니다.
          </p>
        </div>
        
        <label className="flex items-start gap-3 cursor-pointer mt-4 border border-violet-500/30 p-3 rounded-lg bg-violet-500/5 hover:bg-violet-500/10 transition-colors">
          <input 
            type="checkbox" 
            checked={consent} 
            onChange={(e) => setConsent(e.target.checked)} 
            className="mt-0.5 accent-violet-500" 
          />
          <span className="text-sm font-medium text-slate-200">위 데이터 수집 및 활용 내용에 동의합니다. (필수)</span>
        </label>
        
        {error && <p className="text-red-400 text-sm mt-2">{error}</p>}
        
        <div className="pt-4">
          <button type="submit" disabled={loading} className="w-full btn-primary py-3 text-base">
            {loading ? '처리 중...' : '동의하고 시작하기'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
