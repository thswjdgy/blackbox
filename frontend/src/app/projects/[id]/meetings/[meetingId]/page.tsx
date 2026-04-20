'use client';

import { useState, useEffect, useCallback, useMemo, Suspense } from 'react';
import { useParams, useRouter, useSearchParams } from 'next/navigation';
import { QRCodeSVG } from 'qrcode.react';
import api from '@/lib/api';
import { useAuthStore } from '@/store/authStore';
import { ActionItemModal } from '@/components/meeting/ActionItemModal';

interface Attendee {
  userId: number;
  name: string;
  checkedInAt: string | null;
  present: boolean;
}

interface MeetingDetail {
  id: number;
  projectId: number;
  title: string;
  purpose: string | null;
  notes: string | null;
  decisions: string | null;
  checkinCode: string;
  meetingAt: string;
  createdById: number;
  attendees: Attendee[];
  createdAt: string;
  updatedAt: string;
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('ko-KR', {
    year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit',
  });
}

function MeetingDetailContent() {
  const params = useParams();
  const searchParams = useSearchParams();
  const router = useRouter();
  const projectId = params.id as string;
  const meetingId = params.meetingId as string;

  const [meeting, setMeeting] = useState<MeetingDetail | null>(null);
  const [tasks, setTasks] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [notes, setNotes] = useState('');
  const [decisions, setDecisions] = useState('');
  const [saving, setSaving] = useState(false);
  const currentUser = useAuthStore(state => state.user);
  const [isActionItemOpen, setIsActionItemOpen] = useState(false);
  const [isQRModalOpen, setIsQRModalOpen] = useState(false);
  const [copySuccess, setCopySuccess] = useState(false);
  const [linkCopySuccess, setLinkCopySuccess] = useState(false);
  const [notionExporting, setNotionExporting] = useState(false);
  const [notionUrl, setNotionUrl] = useState<string | null>(null);
  const [driveExporting, setDriveExporting] = useState(false);
  const [driveUrl, setDriveUrl] = useState<string | null>(null);

  const checkinUrl = useMemo(() => {
    if (typeof window === 'undefined') return '';
    return `${window.location.origin}/projects/${projectId}/meetings/${meetingId}?checkin=true`;
  }, [projectId, meetingId]);

  const fetchMeeting = useCallback(async () => {
    try {
      const [mRes, tRes] = await Promise.all([
        api.get(`/projects/${projectId}/meetings/${meetingId}`),
        api.get(`/projects/${projectId}/tasks`),
      ]);
      const data: MeetingDetail = mRes.data;
      setMeeting(data);
      setNotes(data.notes ?? '');
      setDecisions(data.decisions ?? '');
      
      // Filter tasks related to this meeting
      setTasks(tRes.data.filter((t: any) => t.meetingId === Number(meetingId)));
    } catch (e) {
      console.error('Failed to fetch meeting', e);
    } finally {
      setLoading(false);
    }
  }, [projectId, meetingId]);

  useEffect(() => {
    fetchMeeting();
  }, [fetchMeeting, searchParams]);

  const handleSave = async () => {
    setSaving(true);
    try {
      await api.put(`/projects/${projectId}/meetings/${meetingId}`, { notes, decisions });
      await fetchMeeting();
    } catch (e) {
      console.error(e);
      alert('저장 실패');
    } finally {
      setSaving(false);
    }
  };

  const handleSelfCheckin = async () => {
    if (!currentUser) return;
    const myAttendance = meeting?.attendees.find(a => a.userId === currentUser.id);
    if (myAttendance?.present) return;
    const res = await api.post(`/projects/${projectId}/meetings/${meetingId}/attendees/${currentUser.id}`);
    setMeeting(prev => prev ? { ...prev, attendees: res.data.attendees } : prev);
  };

  const handleCreateActionItem = async (title: string, description: string) => {
    await api.post(`/projects/${projectId}/meetings/${meetingId}/action-items`, { title, description });
    await fetchMeeting();
  };

  const handleDeleteTask = async (taskId: number) => {
    if (!confirm('액션 아이템을 삭제할까요?')) return;
    try {
      await api.delete(`/projects/${projectId}/tasks/${taskId}`);
      setTasks(prev => prev.filter(t => t.id !== taskId));
    } catch (e) {
      console.error(e);
      alert('삭제 실패');
    }
  };

  const handleCopyCode = async () => {
    if (!meeting) return;
    await navigator.clipboard.writeText(meeting.checkinCode);
    setCopySuccess(true);
    setTimeout(() => setCopySuccess(false), 2000);
  };

  const handleCopyLink = async () => {
    await navigator.clipboard.writeText(checkinUrl);
    setLinkCopySuccess(true);
    setTimeout(() => setLinkCopySuccess(false), 2000);
  };

  const handleDriveExport = async () => {
    setDriveExporting(true);
    try {
      const res = await api.post(`/projects/${projectId}/google/push-meeting/${meetingId}`);
      setDriveUrl(res.data.url);
    } catch (e: any) {
      alert(e?.response?.data?.message ?? 'Drive 내보내기 실패. 설정에서 Google 연동을 확인해 주세요.');
    } finally {
      setDriveExporting(false);
    }
  };

  const handleNotionExport = async () => {
    setNotionExporting(true);
    try {
      const res = await api.post(`/projects/${projectId}/notion/push-meeting/${meetingId}`);
      setNotionUrl(res.data.url);
    } catch (e: any) {
      alert(e?.response?.data?.message ?? 'Notion 내보내기 실패. 설정에서 Notion 연동을 확인해 주세요.');
    } finally {
      setNotionExporting(false);
    }
  };

  const handleDelete = async () => {
    if (!confirm('이 회의를 삭제하시겠습니까?')) return;
    await api.delete(`/projects/${projectId}/meetings/${meetingId}`);
    router.push(`/projects/${projectId}/meetings`);
  };

  if (loading) return <div className="p-8 text-slate-400">불러오는 중...</div>;
  if (!meeting) return <div className="p-8 text-slate-400">회의를 찾을 수 없습니다.</div>;

  const isDirty = notes !== (meeting.notes ?? '') || decisions !== (meeting.decisions ?? '');

  return (
    <div className="flex flex-col h-full overflow-y-auto custom-scrollbar p-6">
      {/* Back + Delete */}
      <div className="flex items-center justify-between mb-6">
        <button
          onClick={() => router.push(`/projects/${projectId}/meetings`)}
          className="flex items-center gap-2 text-slate-400 hover:text-white text-sm font-medium transition-colors"
        >
          ← 회의 목록으로
        </button>
        <button
          onClick={handleDelete}
          className="text-slate-600 hover:text-rose-400 text-sm transition-colors"
        >
          회의 삭제
        </button>
      </div>

      {/* Title + Meta */}
      <div className="backdrop-blur-md bg-slate-900/40 p-6 rounded-2xl border border-slate-800 shadow-xl mb-6">
        <h1 className="text-2xl font-bold text-white mb-2">{meeting.title}</h1>
        {meeting.purpose && (
          <p className="text-slate-400 text-sm mb-3">{meeting.purpose}</p>
        )}
        <p className="text-sm text-slate-500">{formatDate(meeting.meetingAt)}</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left: Notes + Decisions */}
        <div className="lg:col-span-2 space-y-5">
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5">
            <label className="block text-xs uppercase font-bold text-slate-400 mb-3 tracking-wider">
              회의 노트
            </label>
            <textarea
              className="w-full bg-slate-800/50 border border-slate-700 text-slate-100 px-4 py-3 rounded-xl focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500 outline-none transition-all placeholder-slate-600 h-40 resize-none text-sm leading-relaxed"
              placeholder="회의 중 논의된 내용을 자유롭게 기록하세요..."
              value={notes}
              onChange={e => setNotes(e.target.value)}
            />
          </div>

          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5">
            <label className="block text-xs uppercase font-bold text-slate-400 mb-3 tracking-wider">
              결정 사항
            </label>
            <textarea
              className="w-full bg-slate-800/50 border border-slate-700 text-slate-100 px-4 py-3 rounded-xl focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500 outline-none transition-all placeholder-slate-600 h-32 resize-none text-sm leading-relaxed"
              placeholder="회의를 통해 결정된 사항을 기록하세요..."
              value={decisions}
              onChange={e => setDecisions(e.target.value)}
            />
          </div>

          <div className="flex gap-3">
            <button
              onClick={handleSave}
              disabled={!isDirty || saving}
              className="flex-1 py-3 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-bold rounded-xl transition-all disabled:opacity-40 disabled:cursor-not-allowed"
            >
              {saving ? '저장 중...' : isDirty ? '변경사항 저장' : '저장됨'}
            </button>
            <button
              onClick={() => setIsActionItemOpen(true)}
              className="flex-1 py-3 bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-500 hover:to-indigo-500 text-white font-bold rounded-xl transition-all"
            >
              + 액션 아이템 추가
            </button>
          </div>

          {/* Notion 내보내기 */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-4">
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-xs font-bold text-slate-300">Notion으로 내보내기</p>
                <p className="text-xs text-slate-500 mt-0.5">이 회의록을 Notion 데이터베이스에 새 페이지로 저장합니다.</p>
              </div>
              <button
                onClick={handleNotionExport}
                disabled={notionExporting}
                className="shrink-0 px-4 py-2 bg-slate-700 hover:bg-slate-600 text-slate-200 text-xs font-bold rounded-lg transition-all disabled:opacity-50"
              >
                {notionExporting ? '내보내는 중...' : 'N Notion에 저장'}
              </button>
            </div>
            {notionUrl && (
              <a href={notionUrl} target="_blank" rel="noreferrer"
                className="mt-3 flex items-center gap-2 text-xs text-blue-400 hover:text-blue-300 transition-colors">
                <span>✓ Notion 페이지가 생성되었습니다 →</span>
              </a>
            )}
          </div>

          {/* Google Drive 내보내기 */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-4">
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-xs font-bold text-slate-300">Google Drive로 내보내기</p>
                <p className="text-xs text-slate-500 mt-0.5">이 회의록을 Google Docs 문서로 Drive에 저장합니다.</p>
              </div>
              <button
                onClick={handleDriveExport}
                disabled={driveExporting}
                className="shrink-0 px-4 py-2 bg-blue-600/20 hover:bg-blue-600/30 text-blue-300 text-xs font-bold rounded-lg transition-all disabled:opacity-50 border border-blue-500/30"
              >
                {driveExporting ? '내보내는 중...' : '📄 Drive에 저장'}
              </button>
            </div>
            {driveUrl && (
              <a href={driveUrl} target="_blank" rel="noreferrer"
                className="mt-3 flex items-center gap-2 text-xs text-blue-400 hover:text-blue-300 transition-colors">
                <span>✓ Google Docs 문서가 생성되었습니다 →</span>
              </a>
            )}
          </div>
        </div>

        {/* Right: Checkin Code + Attendees */}
        <div className="space-y-5">
          {/* Checkin Code */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5">
            <p className="text-xs uppercase font-bold text-slate-400 mb-3 tracking-wider">체크인 코드</p>
            <div className="bg-slate-800 rounded-xl p-4 text-center mb-4">
              <span className="font-mono text-3xl font-black tracking-[0.3em] text-emerald-400">
                {meeting.checkinCode}
              </span>
            </div>
            
            <div className="grid grid-cols-2 gap-2 mb-3">
              <button
                onClick={handleCopyCode}
                className="py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold rounded-lg transition-all"
              >
                {copySuccess ? '코드 복사됨 ✓' : '코드 복사'}
              </button>
              <button
                onClick={() => setIsQRModalOpen(true)}
                className="py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold rounded-lg transition-all flex items-center justify-center gap-2"
              >
                <span>📷</span> QR 코드
              </button>
              <button
                onClick={handleCopyLink}
                className="py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold rounded-lg transition-all"
              >
                {linkCopySuccess ? '링크 복사됨 ✓' : '링크 복사'}
              </button>
              <button
                onClick={handleSelfCheckin}
                className={`py-2 text-xs font-bold rounded-lg transition-all
                  ${meeting.attendees.find(a => a.userId === currentUser?.id)?.present
                    ? 'bg-emerald-600/30 text-emerald-400 border border-emerald-500/30'
                    : 'bg-emerald-600 hover:bg-emerald-500 text-white'}`}
              >
                {meeting.attendees.find(a => a.userId === currentUser?.id)?.present ? '참석 ✓' : '참석하기'}
              </button>
            </div>
          </div>

          {/* Attendees */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5">
            <p className="text-xs uppercase font-bold text-slate-400 mb-3 tracking-wider">
              출석 현황 <span className="text-emerald-400 ml-1">{meeting.attendees.filter(a => a.present).length}</span> / {meeting.attendees.length}
            </p>
            <ul className="space-y-2">
              {meeting.attendees.map(a => (
                <li key={a.userId} className="flex items-center gap-3">
                  <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold text-white shrink-0
                    ${a.present ? 'bg-gradient-to-br from-emerald-500 to-teal-600' : 'bg-slate-800'}`}>
                    {a.name.charAt(0)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className={`text-sm font-semibold truncate ${a.present ? 'text-slate-200' : 'text-slate-500'}`}>
                      {a.name}
                    </p>
                    {a.present && a.checkedInAt && (
                      <p className="text-[10px] text-slate-500">{formatDate(a.checkedInAt)}</p>
                    )}
                  </div>
                  {a.present ? (
                    <span className="text-xs font-bold shrink-0 px-2 py-1 text-emerald-400">
                      참석 ✓
                    </span>
                  ) : (
                    <button
                      onClick={async () => {
                        const res = await api.post(`/projects/${projectId}/meetings/${meetingId}/attendees/${a.userId}`);
                        setMeeting(prev => prev ? { ...prev, attendees: res.data.attendees } : prev);
                      }}
                      className="text-xs font-bold shrink-0 px-2 py-1 rounded-lg bg-slate-800 text-slate-500 hover:bg-emerald-500/10 hover:text-emerald-400 transition-all"
                    >
                      출석 처리
                    </button>
                  )}
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>

      {/* Action Items (Related Tasks) */}
      <div className="mt-8 mb-12">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold text-white">생성된 액션 아이템</h2>
          <span className="text-xs text-slate-500 bg-slate-800 px-2 py-1 rounded-md">{tasks.length} 개</span>
        </div>
        
        {tasks.length === 0 ? (
          <div className="bg-slate-900/40 border border-slate-800 border-dashed rounded-2xl p-8 text-center">
            <p className="text-sm text-slate-500">이 회의에서 생성된 태스크가 없습니다.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {tasks.map(task => (
              <div key={task.id} className="group bg-slate-800/40 border border-slate-700/50 rounded-xl p-4 flex items-start gap-4">
                <div className={`w-2 h-2 rounded-full mt-2 shrink-0 ${
                  task.status === 'DONE' ? 'bg-emerald-500' :
                  task.status === 'IN_PROGRESS' ? 'bg-amber-500' : 'bg-slate-500'
                }`} />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-bold text-slate-200 truncate">{task.title}</p>
                  <p className="text-xs text-slate-500 mt-1 line-clamp-1">{task.description || '설명 없음'}</p>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <div className="text-[10px] bg-slate-900 text-slate-400 px-2 py-1 rounded uppercase font-bold">
                    {task.status}
                  </div>
                  <button
                    onClick={() => handleDeleteTask(task.id)}
                    className="opacity-0 group-hover:opacity-100 w-6 h-6 rounded-md flex items-center justify-center text-slate-500 hover:text-red-400 hover:bg-red-500/10 transition-all"
                    title="삭제"
                  >
                    ✕
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <style dangerouslySetInnerHTML={{__html: `
        .custom-scrollbar::-webkit-scrollbar { width: 6px; }
        .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
        .custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(51,65,85,0.5); border-radius: 8px; }
      `}} />

      <ActionItemModal
        isOpen={isActionItemOpen}
        onClose={() => setIsActionItemOpen(false)}
        onSave={handleCreateActionItem}
      />

      {/* QR Code Modal */}
      {isQRModalOpen && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/80 backdrop-blur-sm p-4" onClick={() => setIsQRModalOpen(false)}>
          <div className="force-light bg-white p-8 rounded-3xl shadow-2xl flex flex-col items-center gap-6" onClick={e => e.stopPropagation()}>
            <h3 className="text-xl font-bold text-slate-900">회의 체크인 QR 코드</h3>
            <div className="p-4 bg-slate-50 rounded-2xl">
              <QRCodeSVG value={checkinUrl} size={256} />
            </div>
            <div className="text-center">
              <p className="text-slate-600 font-medium">{meeting.title}</p>
              <p className="text-sm text-slate-400 mt-1">스캔하면 바로 체크인 페이지로 연결됩니다.</p>
            </div>
            <button
              onClick={() => setIsQRModalOpen(false)}
              className="w-full py-3 bg-slate-900 hover:bg-slate-800 text-white font-bold rounded-xl transition-all"
            >
              닫기
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default function MeetingDetailPage() {
  return (
    <Suspense fallback={<div className="p-8 text-slate-400">Loading...</div>}>
      <MeetingDetailContent />
    </Suspense>
  );
}
