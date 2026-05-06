'use client';

import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'next/navigation';
import api from '@/lib/api';
import { useAuthStore } from '@/store/authStore';

interface ManualLog {
  id: number;
  projectId: number;
  userId: number;
  userName: string;
  title: string;
  description: string;
  workDate: string;
  evidenceUrl: string | null;
  trustLevel: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  reviewNote: string | null;
  reviewedById: number | null;
  createdAt: string;
}

const STATUS_LABEL: Record<string, string> = {
  PENDING:  '검토 중',
  APPROVED: '승인됨',
  REJECTED: '반려됨',
};

const STATUS_STYLE: Record<string, string> = {
  PENDING:  'bg-yellow-500/10 text-yellow-400 border-yellow-500/30',
  APPROVED: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
  REJECTED: 'bg-rose-500/10 text-rose-400 border-rose-500/30',
};

export default function ManualPage() {
  const params = useParams();
  const projectId = params.id as string;
  const { user } = useAuthStore();

  const [logs, setLogs] = useState<ManualLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [showForm, setShowForm] = useState(false);

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [workDate, setWorkDate] = useState(new Date().toISOString().split('T')[0]);
  const [evidenceUrl, setEvidenceUrl] = useState('');

  // 검토 모달 상태
  const [reviewingId, setReviewingId] = useState<number | null>(null);
  const [reviewNote, setReviewNote] = useState('');
  const [reviewing, setReviewing] = useState(false);

  const fetchLogs = useCallback(async () => {
    try {
      const res = await api.get(`/projects/${projectId}/manual-logs`);
      setLogs(res.data);
    } catch (e) {
      console.error('Failed to fetch manual logs', e);
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !description.trim()) return;
    setSubmitting(true);
    try {
      await api.post(`/projects/${projectId}/manual-logs`, {
        title: title.trim(),
        description: description.trim(),
        workDate,
        evidenceUrl: evidenceUrl.trim() || null,
      });
      setTitle('');
      setDescription('');
      setWorkDate(new Date().toISOString().split('T')[0]);
      setEvidenceUrl('');
      setShowForm(false);
      await fetchLogs();
    } catch (e) {
      alert('신고 제출 실패');
    } finally {
      setSubmitting(false);
    }
  };

  const handleReview = async (logId: number, status: 'APPROVED' | 'REJECTED') => {
    setReviewing(true);
    try {
      await api.patch(`/projects/${projectId}/manual-logs/${logId}/review`, {
        status,
        reviewNote: reviewNote.trim() || null,
      });
      setReviewingId(null);
      setReviewNote('');
      await fetchLogs();
    } catch (e) {
      alert('검토 처리 실패');
    } finally {
      setReviewing(false);
    }
  };

  const canReview = user?.role === 'PROFESSOR' || user?.role === 'TA';

  if (loading) return <div className="p-8 text-slate-400">불러오는 중...</div>;

  return (
    <div className="flex flex-col h-full overflow-y-auto custom-scrollbar p-6 space-y-6">
      {/* Header */}
      <div className="flex shrink-0 items-center justify-between backdrop-blur-md bg-slate-900/40 p-5 rounded-2xl border border-slate-800 shadow-xl">
        <div>
          <h2 className="text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-teal-400 to-cyan-400">
            수동 작업 신고
          </h2>
          <p className="text-sm text-slate-400 mt-1">시스템에서 자동 감지되지 않은 작업을 직접 신고합니다. (신뢰도 0.7 적용)</p>
        </div>
        <button
          onClick={() => setShowForm(v => !v)}
          className="bg-gradient-to-r from-teal-500 to-cyan-500 hover:from-teal-400 hover:to-cyan-400 text-white font-bold py-2.5 px-6 rounded-xl shadow-lg transition-all hover:-translate-y-0.5"
        >
          {showForm ? '취소' : '+ 작업 신고'}
        </button>
      </div>

      {/* Submit Form */}
      {showForm && (
        <div className="shrink-0 bg-slate-900/80 border border-teal-500/20 rounded-2xl p-6">
          <h3 className="text-sm font-bold text-teal-300 uppercase tracking-wider mb-4">작업 내용 입력</h3>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs text-slate-400 mb-1.5">제목 *</label>
              <input
                type="text"
                value={title}
                onChange={e => setTitle(e.target.value)}
                placeholder="ex) API 문서 작성, 디자인 시안 수정"
                className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-teal-500/50 transition-colors"
                required
              />
            </div>
            <div>
              <label className="block text-xs text-slate-400 mb-1.5">상세 설명 *</label>
              <textarea
                value={description}
                onChange={e => setDescription(e.target.value)}
                placeholder="수행한 작업의 내용을 구체적으로 설명해주세요."
                rows={3}
                className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-teal-500/50 transition-colors resize-none"
                required
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs text-slate-400 mb-1.5">작업 날짜 *</label>
                <input
                  type="date"
                  value={workDate}
                  onChange={e => setWorkDate(e.target.value)}
                  className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-teal-500/50 transition-colors"
                  required
                />
              </div>
              <div>
                <label className="block text-xs text-slate-400 mb-1.5">증빙 URL (선택)</label>
                <input
                  type="url"
                  value={evidenceUrl}
                  onChange={e => setEvidenceUrl(e.target.value)}
                  placeholder="https://..."
                  className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-teal-500/50 transition-colors"
                />
              </div>
            </div>
            <div className="flex justify-end gap-3 pt-1">
              <button
                type="button"
                onClick={() => setShowForm(false)}
                className="px-5 py-2 rounded-xl text-sm font-semibold bg-slate-800 text-slate-400 hover:text-slate-200 transition-colors"
              >
                취소
              </button>
              <button
                type="submit"
                disabled={submitting || !title.trim() || !description.trim()}
                className="px-6 py-2 rounded-xl text-sm font-bold bg-gradient-to-r from-teal-500 to-cyan-500 hover:from-teal-400 hover:to-cyan-400 text-white disabled:opacity-50 transition-all"
              >
                {submitting ? '제출 중...' : '신고 제출'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Log List */}
      {logs.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-40 gap-3">
          <p className="text-slate-500">신고된 작업이 없습니다.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {logs.map(log => (
            <div key={log.id} className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5">
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-semibold text-slate-100">{log.title}</span>
                    <span className={`text-xs px-2 py-0.5 rounded-md border ${STATUS_STYLE[log.status]}`}>
                      {STATUS_LABEL[log.status]}
                    </span>
                    <span className="text-xs bg-slate-800 text-slate-400 px-2 py-0.5 rounded-md">
                      신뢰도 {(log.trustLevel * 100).toFixed(0)}%
                    </span>
                  </div>
                  <p className="text-sm text-slate-400 mt-1.5">{log.description}</p>
                  <div className="flex items-center gap-3 mt-2 text-xs text-slate-500 flex-wrap">
                    <span>👤 {log.userName}</span>
                    <span>·</span>
                    <span>📅 {log.workDate}</span>
                    {log.evidenceUrl && (
                      <>
                        <span>·</span>
                        <a
                          href={log.evidenceUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-teal-400 hover:text-teal-300"
                        >
                          🔗 증빙 보기
                        </a>
                      </>
                    )}
                  </div>
                  {log.reviewNote && (
                    <div className="mt-2 p-2.5 bg-slate-800/50 rounded-lg text-xs text-slate-400">
                      <span className="text-slate-300 font-medium">검토 의견:</span> {log.reviewNote}
                    </div>
                  )}
                </div>

                {/* Review Actions (교수/TA만) */}
                {canReview && log.status === 'PENDING' && (
                  <div className="flex gap-2 shrink-0">
                    {reviewingId === log.id ? (
                      <div className="flex flex-col gap-2 items-end">
                        <input
                          type="text"
                          value={reviewNote}
                          onChange={e => setReviewNote(e.target.value)}
                          placeholder="검토 의견 (선택)"
                          className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-teal-500/50 w-48"
                        />
                        <div className="flex gap-2">
                          <button
                            onClick={() => handleReview(log.id, 'APPROVED')}
                            disabled={reviewing}
                            className="px-3 py-1.5 bg-emerald-600/20 hover:bg-emerald-600/40 text-emerald-400 text-xs font-bold rounded-lg border border-emerald-500/30 transition-all disabled:opacity-50"
                          >
                            승인
                          </button>
                          <button
                            onClick={() => handleReview(log.id, 'REJECTED')}
                            disabled={reviewing}
                            className="px-3 py-1.5 bg-rose-600/20 hover:bg-rose-600/40 text-rose-400 text-xs font-bold rounded-lg border border-rose-500/30 transition-all disabled:opacity-50"
                          >
                            반려
                          </button>
                          <button
                            onClick={() => { setReviewingId(null); setReviewNote(''); }}
                            className="px-3 py-1.5 text-slate-400 text-xs rounded-lg hover:text-slate-200 transition-colors"
                          >
                            취소
                          </button>
                        </div>
                      </div>
                    ) : (
                      <button
                        onClick={() => setReviewingId(log.id)}
                        className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold rounded-lg transition-all"
                      >
                        검토하기
                      </button>
                    )}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      <style dangerouslySetInnerHTML={{__html: `
        .custom-scrollbar::-webkit-scrollbar { width: 6px; }
        .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
        .custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(51,65,85,0.5); border-radius: 8px; }
      `}} />
    </div>
  );
}
