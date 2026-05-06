'use client';

import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'next/navigation';
import api from '@/lib/api';
import { useAuthStore } from '@/store/authStore';

interface ProjectMember {
  userId: number;
  name: string;
  role: string;
}

interface PeerReviewSummary {
  revieweeId: number;
  revieweeName: string;
  avgScore: number;
  reviewCount: number;
}

interface MyReview {
  id: number;
  revieweeId: number;
  revieweeName: string;
  score: number;
  comment: string | null;
  anonymous: boolean;
  createdAt: string;
  updatedAt: string | null;
}

const SCORE_LABELS: Record<number, string> = {
  1: '매우 낮음',
  2: '낮음',
  3: '보통',
  4: '높음',
  5: '매우 높음',
};

function StarRow({
  score,
  onChange,
}: {
  score: number;
  onChange: (v: number) => void;
}) {
  return (
    <div className="flex gap-1">
      {[1, 2, 3, 4, 5].map(v => (
        <button
          key={v}
          type="button"
          onClick={() => onChange(v)}
          className={`text-2xl transition-all hover:scale-110 ${v <= score ? 'text-amber-400' : 'text-slate-600 hover:text-amber-300'}`}
        >
          ★
        </button>
      ))}
    </div>
  );
}

function Toggle({ value, onChange }: { value: boolean; onChange: (v: boolean) => void }) {
  return (
    <div
      onClick={() => onChange(!value)}
      className={`relative w-10 h-5 rounded-full transition-colors cursor-pointer ${value ? 'bg-amber-500' : 'bg-slate-700'}`}
    >
      <div className={`absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white transition-transform ${value ? 'translate-x-5' : 'translate-x-0'}`} />
    </div>
  );
}

export default function ReviewPage() {
  const params = useParams();
  const projectId = params.id as string;
  const { user } = useAuthStore();

  const [members, setMembers] = useState<ProjectMember[]>([]);
  const [summary, setSummary] = useState<PeerReviewSummary[]>([]);
  const [myReviews, setMyReviews] = useState<MyReview[]>([]);
  const [loading, setLoading] = useState(true);

  // 새 평가 작성
  const [showForm, setShowForm] = useState(false);
  const [selectedReviewee, setSelectedReviewee] = useState<number | null>(null);
  const [score, setScore] = useState(3);
  const [comment, setComment] = useState('');
  const [anonymous, setAnonymous] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  // 수정 중인 리뷰
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editScore, setEditScore] = useState(3);
  const [editComment, setEditComment] = useState('');
  const [editAnonymous, setEditAnonymous] = useState(true);
  const [updating, setUpdating] = useState(false);

  const fetchAll = useCallback(async () => {
    try {
      const [memberRes, summaryRes, myRes] = await Promise.allSettled([
        api.get(`/projects/${projectId}/members`),
        api.get(`/projects/${projectId}/peer-reviews/summary`),
        api.get(`/projects/${projectId}/peer-reviews/me`),
      ]);
      if (memberRes.status === 'fulfilled') {
        const raw = memberRes.value.data ?? [];
        const all: ProjectMember[] = Array.isArray(raw) ? raw : raw.members ?? [];
        setMembers(all.filter((m: ProjectMember) => m.userId !== user?.id));
      }
      if (summaryRes.status === 'fulfilled') setSummary(summaryRes.value.data ?? []);
      if (myRes.status === 'fulfilled') setMyReviews(myRes.value.data ?? []);
    } catch (e) {
      console.error('Failed to fetch review data', e);
    } finally {
      setLoading(false);
    }
  }, [projectId, user?.id]);

  useEffect(() => {
    fetchAll();
  }, [fetchAll]);

  const alreadyReviewed = (revieweeId: number) =>
    myReviews.some((r: MyReview) => r.revieweeId === revieweeId);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedReviewee) return;
    setSubmitting(true);
    try {
      await api.post(`/projects/${projectId}/peer-reviews`, {
        revieweeId: selectedReviewee,
        score,
        comment: comment.trim() || null,
        anonymous,
      });
      setSelectedReviewee(null);
      setScore(3);
      setComment('');
      setAnonymous(true);
      setShowForm(false);
      await fetchAll();
    } catch (e: any) {
      alert(e?.response?.data?.message ?? '제출 실패');
    } finally {
      setSubmitting(false);
    }
  };

  const startEdit = (r: MyReview) => {
    setEditingId(r.id);
    setEditScore(r.score);
    setEditComment(r.comment ?? '');
    setEditAnonymous(r.anonymous);
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditScore(3);
    setEditComment('');
    setEditAnonymous(true);
  };

  const handleUpdate = async (reviewId: number) => {
    setUpdating(true);
    try {
      await api.put(`/projects/${projectId}/peer-reviews/${reviewId}`, {
        score: editScore,
        comment: editComment.trim() || null,
        anonymous: editAnonymous,
      });
      cancelEdit();
      await fetchAll();
    } catch (e: any) {
      alert(e?.response?.data?.message ?? '수정 실패');
    } finally {
      setUpdating(false);
    }
  };

  const availableReviewees = members.filter(m => !alreadyReviewed(m.userId));

  if (loading) return <div className="p-8 text-slate-400">불러오는 중...</div>;

  return (
    <div className="flex flex-col h-full overflow-y-auto custom-scrollbar p-6 space-y-6">
      {/* Header */}
      <div className="flex shrink-0 items-center justify-between backdrop-blur-md bg-slate-900/40 p-5 rounded-2xl border border-slate-800 shadow-xl">
        <div>
          <h2 className="text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-amber-400 to-yellow-400">
            피어리뷰
          </h2>
          <p className="text-sm text-slate-400 mt-1">팀원의 기여도를 익명으로 평가합니다. 평가는 점수 산출에 반영됩니다.</p>
        </div>
        {availableReviewees.length > 0 && (
          <button
            onClick={() => { setShowForm(v => !v); cancelEdit(); }}
            className="bg-gradient-to-r from-amber-500 to-yellow-500 hover:from-amber-400 hover:to-yellow-400 text-white font-bold py-2.5 px-6 rounded-xl shadow-lg transition-all hover:-translate-y-0.5"
          >
            {showForm ? '취소' : '+ 평가 작성'}
          </button>
        )}
      </div>

      {/* New Review Form */}
      {showForm && availableReviewees.length > 0 && (
        <div className="shrink-0 bg-slate-900/80 border border-amber-500/20 rounded-2xl p-6">
          <h3 className="text-sm font-bold text-amber-300 uppercase tracking-wider mb-4">평가 작성</h3>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs text-slate-400 mb-2">평가할 팀원 선택 *</label>
              <div className="flex flex-wrap gap-2">
                {availableReviewees.map(m => (
                  <button
                    key={m.userId}
                    type="button"
                    onClick={() => setSelectedReviewee(m.userId)}
                    className={`px-4 py-2 rounded-xl text-sm font-semibold border transition-all ${
                      selectedReviewee === m.userId
                        ? 'bg-amber-500/20 text-amber-300 border-amber-500/50'
                        : 'bg-slate-800 text-slate-400 border-slate-700 hover:border-slate-500 hover:text-slate-200'
                    }`}
                  >
                    {m.name}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-xs text-slate-400 mb-2">
                기여도 점수: <span className="text-amber-300 font-bold">{score}점 — {SCORE_LABELS[score]}</span>
              </label>
              <StarRow score={score} onChange={setScore} />
            </div>

            <div>
              <label className="block text-xs text-slate-400 mb-1.5">의견 (선택)</label>
              <textarea
                value={comment}
                onChange={e => setComment(e.target.value)}
                placeholder="팀원의 기여에 대한 구체적인 의견을 남겨주세요."
                rows={3}
                className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-amber-500/50 transition-colors resize-none"
              />
            </div>

            <label className="flex items-center gap-3 cursor-pointer select-none">
              <Toggle value={anonymous} onChange={setAnonymous} />
              <span className="text-sm text-slate-300">익명으로 제출</span>
              <span className="text-xs text-slate-500">(본인 이름이 노출되지 않습니다)</span>
            </label>

            <div className="flex justify-end gap-3 pt-1">
              <button
                type="button"
                onClick={() => { setShowForm(false); setSelectedReviewee(null); }}
                className="px-5 py-2 rounded-xl text-sm font-semibold bg-slate-800 text-slate-400 hover:text-slate-200 transition-colors"
              >
                취소
              </button>
              <button
                type="submit"
                disabled={submitting || !selectedReviewee}
                className="px-6 py-2 rounded-xl text-sm font-bold bg-gradient-to-r from-amber-500 to-yellow-500 hover:from-amber-400 hover:to-yellow-400 text-white disabled:opacity-50 transition-all"
              >
                {submitting ? '제출 중...' : '평가 제출'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Summary */}
      <div className="shrink-0">
        <h3 className="text-sm font-bold text-slate-400 uppercase tracking-wider mb-3">팀원 평균 점수</h3>
        {summary.length === 0 ? (
          <div className="text-center py-8 text-slate-500 text-sm bg-slate-900/40 rounded-2xl border border-slate-800">
            아직 평가가 없습니다.
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {summary.map(s => (
              <div key={s.revieweeId} className="bg-slate-900/60 border border-slate-800 rounded-2xl p-4 flex items-center gap-4">
                <div className="w-10 h-10 rounded-xl bg-amber-500/10 flex items-center justify-center text-lg shrink-0">👤</div>
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-slate-100 truncate">{s.revieweeName}</p>
                  <p className="text-xs text-slate-500 mt-0.5">{s.reviewCount}명 평가</p>
                </div>
                <div className="text-right shrink-0">
                  <div className="flex items-center gap-0.5 justify-end">
                    {[1, 2, 3, 4, 5].map(v => (
                      <span key={v} className={`text-base ${v <= Math.round(s.avgScore) ? 'text-amber-400' : 'text-slate-700'}`}>★</span>
                    ))}
                  </div>
                  <p className="text-sm font-bold text-amber-400 mt-0.5">{s.avgScore.toFixed(1)}점</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* My Reviews */}
      {myReviews.length > 0 && (
        <div className="shrink-0">
          <h3 className="text-sm font-bold text-slate-400 uppercase tracking-wider mb-3">내가 작성한 평가</h3>
          <div className="space-y-3">
            {myReviews.map(r => (
              <div key={r.id} className="bg-slate-900/60 border border-slate-800 rounded-2xl p-4">
                {editingId === r.id ? (
                  /* 수정 폼 */
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-sm font-semibold text-slate-200">{r.revieweeName} 평가 수정</span>
                    </div>

                    <div>
                      <label className="block text-xs text-slate-400 mb-1.5">
                        기여도 점수: <span className="text-amber-300 font-bold">{editScore}점 — {SCORE_LABELS[editScore]}</span>
                      </label>
                      <StarRow score={editScore} onChange={setEditScore} />
                    </div>

                    <div>
                      <label className="block text-xs text-slate-400 mb-1.5">의견 (선택)</label>
                      <textarea
                        value={editComment}
                        onChange={e => setEditComment(e.target.value)}
                        rows={2}
                        placeholder="팀원의 기여에 대한 구체적인 의견을 남겨주세요."
                        className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-amber-500/50 transition-colors resize-none"
                      />
                    </div>

                    <label className="flex items-center gap-3 cursor-pointer select-none">
                      <Toggle value={editAnonymous} onChange={setEditAnonymous} />
                      <span className="text-sm text-slate-300">익명으로 제출</span>
                    </label>

                    <div className="flex justify-end gap-2 pt-1">
                      <button
                        onClick={cancelEdit}
                        className="px-4 py-1.5 rounded-lg text-sm bg-slate-800 text-slate-400 hover:text-slate-200 transition-colors"
                      >
                        취소
                      </button>
                      <button
                        onClick={() => handleUpdate(r.id)}
                        disabled={updating}
                        className="px-5 py-1.5 rounded-lg text-sm font-bold bg-gradient-to-r from-amber-500 to-yellow-500 hover:from-amber-400 hover:to-yellow-400 text-white disabled:opacity-50 transition-all"
                      >
                        {updating ? '저장 중...' : '저장'}
                      </button>
                    </div>
                  </div>
                ) : (
                  /* 기본 보기 */
                  <div className="flex items-center gap-4">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="text-sm font-semibold text-slate-200">{r.revieweeName}</span>
                        {r.anonymous && (
                          <span className="text-xs bg-slate-800 text-slate-500 px-2 py-0.5 rounded-md">익명</span>
                        )}
                        {r.updatedAt && (
                          <span className="text-xs text-slate-600">수정됨</span>
                        )}
                      </div>
                      {r.comment && (
                        <p className="text-xs text-slate-500 mt-1">{r.comment}</p>
                      )}
                    </div>
                    <div className="flex items-center gap-3 shrink-0">
                      <div className="flex gap-0.5">
                        {[1, 2, 3, 4, 5].map(v => (
                          <span key={v} className={`text-base ${v <= r.score ? 'text-amber-400' : 'text-slate-700'}`}>★</span>
                        ))}
                      </div>
                      <button
                        onClick={() => { startEdit(r); setShowForm(false); }}
                        className="px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-300 transition-all"
                      >
                        수정
                      </button>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
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
