'use client';

import { useEffect, useState, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import api from '@/lib/api';
import { useAuthStore } from '@/store/authStore';

interface Member {
  userId: number;
  name: string;
  email: string;
  role: string;
  projectRole: string;
  joinedAt: string;
}

interface ProjectDetail {
  id: number;
  name: string;
  description: string;
  inviteCode: string;
  active: boolean;
  members: Member[];
  createdAt: string;
  courseName?: string;
  semester?: string;
  startDate?: string;
  endDate?: string;
}

interface TaskSummary {
  todo: number;
  inProgress: number;
  done: number;
  total: number;
}

interface MeetingSummary {
  id: number;
  title: string;
  meetingAt: string;
  attendeeCount: number;
}

interface MemberScore {
  userId: number;
  userName: string;
  totalScore: number;
  grade: string;
}

const GRADE_COLOR: Record<string, string> = {
  A: 'text-emerald-400',
  B: 'text-blue-400',
  C: 'text-yellow-400',
  D: 'text-orange-400',
  F: 'text-red-400',
};

const AVATAR_COLORS = [
  'bg-rose-400',
  'bg-pink-400',
  'bg-fuchsia-400',
  'bg-violet-400',
  'bg-indigo-400',
  'bg-blue-400',
  'bg-sky-400',
  'bg-cyan-400',
  'bg-teal-400',
  'bg-emerald-400',
  'bg-green-400',
  'bg-lime-400',
  'bg-yellow-400',
  'bg-orange-400',
];

const getAvatarColor = (userId: number) => AVATAR_COLORS[userId % AVATAR_COLORS.length];

export default function ProjectDetailPage() {
  const params = useParams();
  const router = useRouter();
  const projectId = params.id as string;
  const { user } = useAuthStore();

  const [project, setProject] = useState<ProjectDetail | null>(null);
  const [taskSummary, setTaskSummary] = useState<TaskSummary>({ todo: 0, inProgress: 0, done: 0, total: 0 });
  const [recentMeetings, setRecentMeetings] = useState<MeetingSummary[]>([]);
  const [scores, setScores] = useState<MemberScore[]>([]);
  const [loading, setLoading] = useState(true);
  const [copySuccess, setCopySuccess] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [menuOpenId, setMenuOpenId] = useState<number | null>(null);

  const fetchAll = useCallback(async () => {
    try {
      const [projRes, taskRes, meetRes, scoreRes] = await Promise.allSettled([
        api.get(`/projects/${projectId}`),
        api.get(`/projects/${projectId}/tasks`),
        api.get(`/projects/${projectId}/meetings`),
        api.get(`/projects/${projectId}/scores`),
      ]);

      if (projRes.status === 'fulfilled') setProject(projRes.value.data);
      else router.replace('/dashboard');

      if (taskRes.status === 'fulfilled') {
        const tasks = taskRes.value.data as any[];
        setTaskSummary({
          todo:       tasks.filter(t => t.status === 'TODO').length,
          inProgress: tasks.filter(t => t.status === 'IN_PROGRESS').length,
          done:       tasks.filter(t => t.status === 'DONE').length,
          total:      tasks.length,
        });
      }

      if (meetRes.status === 'fulfilled') {
        setRecentMeetings((meetRes.value.data as MeetingSummary[]).slice(0, 3));
      }

      if (scoreRes.status === 'fulfilled' && scoreRes.value.data?.members) {
        setScores(scoreRes.value.data.members.slice(0, 5));
      }
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => { fetchAll(); }, [fetchAll]);

  useEffect(() => {
    if (menuOpenId === null) return;
    const close = () => setMenuOpenId(null);
    document.addEventListener('click', close);
    return () => document.removeEventListener('click', close);
  }, [menuOpenId]);

  const handleCopyCode = async () => {
    if (!project) return;
    await navigator.clipboard.writeText(project.inviteCode);
    setCopySuccess(true);
    setTimeout(() => setCopySuccess(false), 2000);
  };

  const myProjectRole = project?.members.find(m => m.userId === user?.id)?.projectRole ?? '';
  const canManage = myProjectRole === 'LEADER' || user?.role === 'PROFESSOR' || user?.role === 'TA';

  const handleRoleChange = async (targetId: number, currentRole: string) => {
    const next = currentRole === 'LEADER' ? 'MEMBER' : 'LEADER';
    if (!confirm(`역할을 ${next === 'LEADER' ? '팀장' : '팀원'}으로 변경하시겠습니까?`)) return;
    try {
      await api.patch(`/projects/${projectId}/members/${targetId}/role`, { projectRole: next });
      setProject(p => p ? { ...p, members: p.members.map(m => m.userId === targetId ? { ...m, projectRole: next } : m) } : p);
    } catch (e: any) {
      alert(e.response?.data?.message ?? '역할 변경 실패');
    } finally { setMenuOpenId(null); }
  };

  const handleKick = async (targetId: number, name: string) => {
    if (!confirm(`'${name}'님을 프로젝트에서 강퇴하시겠습니까?`)) return;
    try {
      await api.delete(`/projects/${projectId}/members/${targetId}`);
      setProject(p => p ? { ...p, members: p.members.filter(m => m.userId !== targetId) } : p);
    } catch (e: any) {
      alert(e.response?.data?.message ?? '강퇴 실패');
    } finally { setMenuOpenId(null); }
  };

  if (loading) return <div className="p-8 text-slate-400">데이터를 불러오는 중...</div>;
  if (!project) return null;

  const doneRate = taskSummary.total > 0 ? Math.round((taskSummary.done / taskSummary.total) * 100) : 0;

  return (
    <div className="flex-1 h-full overflow-y-auto custom-scrollbar p-6">
      {/* Hero Header */}
      <div className="relative overflow-hidden bg-slate-900 border border-slate-800 rounded-3xl p-8 mb-6 shadow-2xl">
        <div className="absolute top-0 right-0 p-8 opacity-10 pointer-events-none">
          <div className="text-9xl font-black text-white">{project.name.charAt(0).toUpperCase()}</div>
        </div>
        <div className="relative flex flex-col md:flex-row md:items-end justify-between gap-6">
          <div className="max-w-2xl">
            <div className="flex items-center gap-3 mb-4">
              <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider ${project.active ? 'bg-emerald-900/40 text-emerald-400 border border-emerald-500/20' : 'bg-slate-700 text-slate-400 border border-slate-600'}`}>
                {project.active ? 'Active' : 'Archived'}
              </span>
              {project.semester && (
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-violet-500/15 text-violet-400 border border-violet-500/20 font-semibold">
                  {project.semester}
                </span>
              )}
              {project.courseName && (
                <span className="text-xs text-slate-400">{project.courseName}</span>
              )}
              <span className="text-slate-500 text-xs">{new Date(project.createdAt).toLocaleDateString('ko-KR')} 시작</span>
            </div>
            <h1 className="text-4xl font-black text-white mb-3 tracking-tight">{project.name}</h1>
            <p className="text-slate-400 text-base leading-relaxed">{project.description || '이 프로젝트에 대한 설명이 없습니다.'}</p>
            {(project.startDate || project.endDate) && (
              <p className="text-xs text-slate-500 mt-2">
                📅 {project.startDate ?? '?'} ~ {project.endDate ?? '?'}
              </p>
            )}
          </div>
          <div className="flex flex-col gap-3 shrink-0">
            <button
              onClick={() => setEditOpen(true)}
              className="text-xs px-3 py-1.5 rounded-lg bg-slate-800 border border-slate-700 text-slate-400 hover:text-white hover:border-slate-500 transition-all"
            >
              ✏️ 프로젝트 편집
            </button>
          <div className="bg-slate-800/60 backdrop-blur-sm p-5 rounded-2xl border border-slate-700/50 shadow-xl group">
            <p className="text-[10px] uppercase font-bold text-slate-500 mb-2 tracking-widest">초대 코드</p>
            <div className="flex items-center gap-3">
              <span className="font-mono text-2xl font-black text-violet-400 tracking-tighter">{project.inviteCode}</span>
              <button
                onClick={handleCopyCode}
                className="px-3 py-1.5 rounded-xl bg-slate-700 hover:bg-violet-600 text-white text-xs font-bold transition-all active:scale-95"
              >
                {copySuccess ? '✓ 복사됨' : '복사'}
              </button>
            </div>
          </div>
          </div>
        </div>
      </div>

      {/* Edit Modal */}
      {editOpen && project && (
        <EditProjectModal
          project={project}
          onClose={() => setEditOpen(false)}
          onSaved={(updated) => { setProject(updated); setEditOpen(false); }}
        />
      )}

      {/* Stats Row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <StatCard label="전체 태스크" value={taskSummary.total} sub="개" color="violet" />
        <StatCard label="진행 중" value={taskSummary.inProgress} sub="개" color="amber" />
        <StatCard label="완료" value={taskSummary.done} sub={`개 (${doneRate}%)`} color="emerald" />
        <StatCard label="팀원" value={project.members.length} sub="명" color="indigo" />
      </div>

      {/* 진행률 바 */}
      {taskSummary.total > 0 && (
        <div className="mb-6 bg-slate-900/60 border border-slate-800 rounded-2xl p-5">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">프로젝트 진행률</span>
            <span className="text-sm font-bold text-white">{doneRate}%</span>
          </div>
          <div className="w-full h-2.5 bg-slate-800 rounded-full overflow-hidden">
            <div
              className="h-full bg-gradient-to-r from-violet-500 to-emerald-500 rounded-full transition-all duration-700"
              style={{ width: `${doneRate}%` }}
            />
          </div>
          <div className="flex gap-4 mt-3 text-xs text-slate-500">
            <span className="text-slate-500">할 일 <span className="text-white font-bold">{taskSummary.todo}</span></span>
            <span className="text-amber-400">진행 중 <span className="text-white font-bold">{taskSummary.inProgress}</span></span>
            <span className="text-emerald-400">완료 <span className="text-white font-bold">{taskSummary.done}</span></span>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left */}
        <div className="lg:col-span-2 space-y-6">
          {/* Quick Nav */}
          <div>
            <h2 className="text-base font-bold text-slate-300 mb-4">바로가기</h2>
            <div className="grid grid-cols-2 gap-3">
              <NavCard href={`/projects/${projectId}/board`}       title="태스크 보드"    desc="칸반 보드로 업무 관리"       icon="📋" color="violet" />
              <NavCard href={`/projects/${projectId}/meetings`}    title="회의록"         desc="회의 기록 및 출석 관리"       icon="📝" color="emerald" />
              <NavCard href={`/projects/${projectId}/vault`}       title="파일 검사"      desc="파일 무결성 검증"             icon="🔒" color="indigo" />
              <NavCard href={`/projects/${projectId}/report`}      title="기여도 리포트"  desc="팀원별 기여 점수 분석"        icon="📊" color="amber" />
            </div>
          </div>

          {/* 최근 회의록 */}
          {recentMeetings.length > 0 && (
            <div>
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-base font-bold text-slate-300">최근 회의</h2>
                <Link href={`/projects/${projectId}/meetings`} className="text-xs text-violet-400 hover:text-violet-300 transition-colors">
                  전체 보기 →
                </Link>
              </div>
              <div className="space-y-3">
                {recentMeetings.map(m => (
                  <Link
                    key={m.id}
                    href={`/projects/${projectId}/meetings/${m.id}`}
                    className="flex items-center gap-4 bg-slate-900/60 border border-slate-800 rounded-xl p-4 hover:border-slate-700 transition-all"
                  >
                    <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-lg shrink-0">📝</div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-semibold text-slate-200 truncate">{m.title}</p>
                      <p className="text-xs text-slate-500 mt-0.5">
                        {new Date(m.meetingAt).toLocaleDateString('ko-KR', { month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                      </p>
                    </div>
                    <span className="text-xs text-slate-500 shrink-0">{m.attendeeCount}명 참석</span>
                  </Link>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Right Sidebar */}
        <div className="space-y-5">
          {/* 팀원 */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5">
            <h2 className="text-sm font-bold text-slate-300 mb-4">팀 멤버 <span className="text-slate-500 ml-1">{project.members.length}</span></h2>
            <ul className="space-y-3">
              {project.members.map(member => {
                const score = scores.find(s => s.userId === member.userId);
                const isSelf = member.userId === user?.id;
                const isMenuOpen = menuOpenId === member.userId;
                return (
                  <li key={member.userId} className="flex items-center gap-3 relative">
                    <div className={`w-9 h-9 rounded-xl flex items-center justify-center text-sm font-bold shrink-0
                      ${member.projectRole === 'LEADER'
                        ? 'bg-gradient-to-br from-amber-500 to-orange-600 border-2 border-amber-400/60 shadow-lg shadow-amber-900/30 text-white'
                        : `${getAvatarColor(member.userId)} text-black`}`}>
                      {member.name.charAt(0).toUpperCase()}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-semibold text-white truncate flex items-center gap-1.5">
                        {member.name}
                        {isSelf && <span className="text-[10px] text-slate-600">(나)</span>}
                      </p>
                      <p className={`text-[10px] uppercase tracking-wider font-semibold
                        ${member.projectRole === 'LEADER' ? 'text-amber-400' : 'text-slate-500'}`}>
                        {member.projectRole === 'LEADER' ? '👑 팀장' : '팀원'}
                      </p>
                    </div>
                    {score && (
                      <span className={`text-sm font-black shrink-0 ${GRADE_COLOR[score.grade] ?? 'text-slate-400'}`}>
                        {score.grade}
                      </span>
                    )}
                    {canManage && !isSelf && (
                      <div className="relative">
                        <button
                          onClick={e => { e.stopPropagation(); setMenuOpenId(isMenuOpen ? null : member.userId); }}
                          className="w-6 h-6 flex items-center justify-center text-slate-600 hover:text-slate-300 rounded transition-colors text-base leading-none"
                        >⋯</button>
                        {isMenuOpen && (
                          <div className="absolute right-0 top-8 z-20 bg-slate-800 border border-slate-700 rounded-xl shadow-2xl py-1 w-36">
                            <button
                              onClick={() => handleRoleChange(member.userId, member.projectRole)}
                              className="w-full text-left px-4 py-2 text-xs text-slate-300 hover:bg-slate-700 hover:text-white transition-colors"
                            >
                              {member.projectRole === 'LEADER' ? '👤 팀원으로 변경' : '👑 팀장으로 변경'}
                            </button>
                            <button
                              onClick={() => handleKick(member.userId, member.name)}
                              className="w-full text-left px-4 py-2 text-xs text-red-400 hover:bg-red-500/10 hover:text-red-300 transition-colors"
                            >
                              🚫 강퇴
                            </button>
                          </div>
                        )}
                      </div>
                    )}
                  </li>
                );
              })}
            </ul>
          </div>

          {/* 기여도 순위 */}
          {scores.length > 0 && (
            <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-sm font-bold text-slate-300">기여도 순위</h2>
                <Link href={`/projects/${projectId}/report`} className="text-xs text-violet-400 hover:text-violet-300 transition-colors">
                  상세 →
                </Link>
              </div>
              <ol className="space-y-2">
                {[...scores].sort((a, b) => b.totalScore - a.totalScore).map((s, i) => (
                  <li key={s.userId} className="flex items-center gap-3">
                    <span className={`text-xs font-black w-5 text-center shrink-0 ${i === 0 ? 'text-amber-400' : i === 1 ? 'text-slate-300' : i === 2 ? 'text-amber-700' : 'text-slate-600'}`}>
                      {i + 1}
                    </span>
                    <span className="text-sm text-slate-300 flex-1 truncate">{s.userName}</span>
                    <span className="text-xs font-bold text-slate-400">{s.totalScore}pt</span>
                    <span className={`text-xs font-black ${GRADE_COLOR[s.grade] ?? 'text-slate-400'}`}>{s.grade}</span>
                  </li>
                ))}
              </ol>
            </div>
          )}
        </div>
      </div>

      <style dangerouslySetInnerHTML={{__html: `
        .custom-scrollbar::-webkit-scrollbar { width: 6px; }
        .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
        .custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(51,65,85,0.5); border-radius: 8px; }
      `}} />
    </div>
  );
}

function StatCard({ label, value, sub, color }: { label: string; value: number; sub: string; color: string }) {
  const colors: Record<string, string> = {
    violet: 'from-violet-500/20 to-violet-600/10 border-violet-500/20 text-violet-400',
    amber:  'from-amber-500/20  to-amber-600/10  border-amber-500/20  text-amber-400',
    emerald:'from-emerald-500/20 to-emerald-600/10 border-emerald-500/20 text-emerald-400',
    indigo: 'from-indigo-500/20 to-indigo-600/10 border-indigo-500/20 text-indigo-400',
  };
  return (
    <div className={`bg-gradient-to-br ${colors[color]} border rounded-2xl p-5`}>
      <p className="text-xs text-slate-500 font-medium mb-2">{label}</p>
      <p className="text-3xl font-black text-white">{value}<span className="text-sm font-normal text-slate-400 ml-1">{sub}</span></p>
    </div>
  );
}

function NavCard({ href, title, desc, icon, color }: { href: string; title: string; desc: string; icon: string; color: string }) {
  const border: Record<string, string> = {
    violet: 'hover:border-violet-500/50',
    emerald:'hover:border-emerald-500/50',
    indigo: 'hover:border-indigo-500/50',
    amber:  'hover:border-amber-500/50',
  };
  return (
    <Link href={href} className={`group block p-5 bg-slate-900/60 border border-slate-800 rounded-2xl transition-all hover:shadow-xl hover:-translate-y-0.5 ${border[color]}`}>
      <div className="text-2xl mb-3">{icon}</div>
      <h3 className="text-sm font-bold text-white mb-1">{title}</h3>
      <p className="text-xs text-slate-500 leading-relaxed">{desc}</p>
    </Link>
  );
}

function EditProjectModal({
  project,
  onClose,
  onSaved,
}: {
  project: ProjectDetail;
  onClose: () => void;
  onSaved: (updated: ProjectDetail) => void;
}) {
  const [form, setForm] = useState({
    name:        project.name        ?? '',
    description: project.description ?? '',
    courseName:  project.courseName  ?? '',
    semester:    project.semester    ?? '',
    startDate:   project.startDate   ?? '',
    endDate:     project.endDate     ?? '',
  });
  const [saving, setSaving] = useState(false);
  const [error,  setError]  = useState('');

  const set = (key: string) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm(p => ({ ...p, [key]: e.target.value }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      const res = await api.patch(`/projects/${project.id}`, {
        name:        form.name        || null,
        description: form.description || null,
        courseName:  form.courseName  || null,
        semester:    form.semester    || null,
        startDate:   form.startDate   || null,
        endDate:     form.endDate     || null,
      });
      onSaved(res.data);
    } catch (err: any) {
      setError(err.response?.data?.message ?? '저장 실패');
    } finally {
      setSaving(false);
    }
  };

  const inputCls = 'w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-violet-500 transition-colors';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl w-full max-w-md">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800">
          <h2 className="font-bold text-white">프로젝트 편집</h2>
          <button onClick={onClose} className="text-slate-500 hover:text-white transition-colors text-xl leading-none">×</button>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-xs text-slate-400 mb-1.5">프로젝트명 *</label>
            <input className={inputCls} required value={form.name} onChange={set('name')} placeholder="팀 캡스톤 프로젝트" />
          </div>
          <div>
            <label className="block text-xs text-slate-400 mb-1.5">설명</label>
            <textarea className={`${inputCls} resize-none`} rows={2} value={form.description} onChange={set('description')} placeholder="프로젝트 소개" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-slate-400 mb-1.5">강의명</label>
              <input className={inputCls} value={form.courseName} onChange={set('courseName')} placeholder="소프트웨어공학" />
            </div>
            <div>
              <label className="block text-xs text-slate-400 mb-1.5">학기</label>
              <input className={inputCls} value={form.semester} onChange={set('semester')} placeholder="2025-1" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-slate-400 mb-1.5">시작일</label>
              <input type="date" className={inputCls} value={form.startDate} onChange={set('startDate')} />
            </div>
            <div>
              <label className="block text-xs text-slate-400 mb-1.5">종료일</label>
              <input type="date" className={inputCls} value={form.endDate} onChange={set('endDate')} />
            </div>
          </div>
          {error && <p className="text-red-400 text-sm">{error}</p>}
          <div className="flex gap-2 pt-1">
            <button type="button" onClick={onClose} className="flex-1 py-2 rounded-xl text-sm font-semibold bg-slate-800 text-slate-400 hover:text-white transition-colors">취소</button>
            <button type="submit" disabled={saving} className="flex-1 py-2 rounded-xl text-sm font-bold bg-violet-600 hover:bg-violet-500 text-white disabled:opacity-50 transition-colors">
              {saving ? '저장 중...' : '저장'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
