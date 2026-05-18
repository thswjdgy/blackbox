'use client';

import { useState, useEffect } from 'react';
import { useParams } from 'next/navigation';
import api from '@/lib/api';

interface CommitQuality {
  sha: string;
  message: string;
  author: string;
  score: number;
  feedback: string;
}

const SCORE_COLOR = ['', 'text-red-400', 'text-orange-400', 'text-yellow-400', 'text-blue-400', 'text-emerald-400'];
const SCORE_BAR   = ['', 'bg-red-500', 'bg-orange-500', 'bg-yellow-500', 'bg-blue-500', 'bg-emerald-500'];

export default function AnalysisPage() {
  const params = useParams();
  const projectId = params.id as string;

  const [analysis, setAnalysis] = useState<string | null>(null);
  const [analyzing, setAnalyzing] = useState(false);
  const [analyzedAt, setAnalyzedAt] = useState<string | null>(null);

  const [commits, setCommits] = useState<CommitQuality[] | null>(null);
  const [analyzingCommits, setAnalyzingCommits] = useState(false);
  const [commitLimit, setCommitLimit] = useState(10);
  const [commitsAnalyzedAt, setCommitsAnalyzedAt] = useState<string | null>(null);

  const [loadingCached, setLoadingCached] = useState(true);

  // 페이지 로드 시 마지막 저장된 결과 불러오기
  useEffect(() => {
    const load = async () => {
      const [teamRes, commitRes] = await Promise.allSettled([
        api.get(`/projects/${projectId}/ai-analysis`),
        api.get(`/projects/${projectId}/ai-analysis/commits`),
      ]);
      if (teamRes.status === 'fulfilled' && teamRes.value.data?.analysis) {
        setAnalysis(teamRes.value.data.analysis);
        setAnalyzedAt(new Date(teamRes.value.data.createdAt).toLocaleString('ko-KR'));
      }
      if (commitRes.status === 'fulfilled' && commitRes.value.data?.commits) {
        setCommits(commitRes.value.data.commits);
        setCommitsAnalyzedAt(new Date(commitRes.value.data.createdAt).toLocaleString('ko-KR'));
      }
      setLoadingCached(false);
    };
    load();
  }, [projectId]);

  const handleAnalyze = async () => {
    setAnalyzing(true);
    try {
      const res = await api.post(`/projects/${projectId}/ai-analysis`);
      setAnalysis(res.data.analysis);
      setAnalyzedAt(new Date(res.data.createdAt).toLocaleString('ko-KR'));
    } catch (e: any) {
      const msg = e?.response?.data?.message ?? 'AI 분석 실패. OPENAI_API_KEY를 확인해 주세요.';
      alert(msg);
    } finally {
      setAnalyzing(false);
    }
  };

  const handleAnalyzeCommits = async () => {
    setAnalyzingCommits(true);
    setCommits(null);
    try {
      const res = await api.post(`/projects/${projectId}/ai-analysis/commits?limit=${commitLimit}`);
      setCommits(res.data.commits);
      setCommitsAnalyzedAt(new Date(res.data.createdAt).toLocaleString('ko-KR'));
    } catch (e: any) {
      alert(e?.response?.data?.message ?? '커밋 분석 실패');
    } finally {
      setAnalyzingCommits(false);
    }
  };

  // 마크다운 굵게 처리 (**text** → <strong>)
  const renderAnalysis = (text: string) => {
    return text.split('\n').map((line, i) => {
      const parts = line.split(/\*\*(.*?)\*\*/g);
      return (
        <p key={i} className={`${line.startsWith('#') ? 'text-slate-200 font-semibold mt-2' : 'text-slate-400'} text-sm leading-relaxed`}>
          {parts.map((part, j) =>
            j % 2 === 1
              ? <strong key={j} className="text-slate-100 font-bold">{part}</strong>
              : part
          )}
        </p>
      );
    });
  };

  return (
    <div className="flex flex-col h-full overflow-y-auto custom-scrollbar p-6 space-y-6">
      {/* Header */}
      <div className="flex shrink-0 items-center justify-between backdrop-blur-md bg-slate-900/40 p-5 rounded-2xl border border-slate-800 shadow-xl">
        <div>
          <h2 className="text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-sky-400 to-indigo-400">
            AI 팀 분석
          </h2>
          <p className="text-sm text-slate-400 mt-1">
            최근 활동 로그와 기여도 점수를 바탕으로 팀 현황을 AI가 분석합니다.
          </p>
        </div>
        <button
          onClick={handleAnalyze}
          disabled={analyzing}
          className="bg-gradient-to-r from-sky-500 to-indigo-500 hover:from-sky-400 hover:to-indigo-400 text-white font-bold py-2.5 px-6 rounded-xl shadow-lg transition-all hover:-translate-y-0.5 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
        >
          {analyzing ? (
            <>
              <span className="inline-block w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              분석 중...
            </>
          ) : '✨ AI 분석 시작'}
        </button>
      </div>

      {/* 분석 중 스켈레톤 */}
      {analyzing && (
        <div className="bg-slate-900/60 border border-sky-500/20 rounded-2xl p-6 space-y-3 animate-pulse">
          <div className="h-4 bg-slate-800 rounded-lg w-1/3" />
          <div className="h-3 bg-slate-800 rounded-lg w-full" />
          <div className="h-3 bg-slate-800 rounded-lg w-5/6" />
          <div className="h-3 bg-slate-800 rounded-lg w-4/6" />
          <div className="h-4 bg-slate-800 rounded-lg w-1/3 mt-4" />
          <div className="h-3 bg-slate-800 rounded-lg w-full" />
          <div className="h-3 bg-slate-800 rounded-lg w-3/4" />
        </div>
      )}

      {/* 분석 결과 */}
      {!analyzing && analysis && (
        <div className="bg-slate-900/60 border border-sky-500/20 rounded-2xl p-6">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-sky-400" />
              <span className="text-xs font-bold text-sky-400 uppercase tracking-wider">AI 분석 결과</span>
            </div>
            {analyzedAt && (
              <span className="text-xs text-slate-600">{analyzedAt} 기준</span>
            )}
          </div>
          <div className="space-y-1">
            {renderAnalysis(analysis)}
          </div>
        </div>
      )}

      {/* 커밋 품질 분석 */}
      <div className="shrink-0 bg-slate-900/60 border border-slate-800 rounded-2xl p-6 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-base font-bold text-slate-200">커밋 품질 분석</h3>
            <p className="text-xs text-slate-500 mt-0.5">
              GitHub 커밋 메시지를 AI가 1-5점으로 평가합니다.
              {commitsAnalyzedAt && <span className="ml-2 text-slate-600">· 마지막: {commitsAnalyzedAt}</span>}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <select
              value={commitLimit}
              onChange={e => setCommitLimit(Number(e.target.value))}
              className="text-xs bg-slate-800 border border-slate-700 text-slate-300 rounded-lg px-2 py-1.5 focus:outline-none"
            >
              {[5, 10, 15, 20].map(n => <option key={n} value={n}>최근 {n}건</option>)}
            </select>
            <button
              onClick={handleAnalyzeCommits}
              disabled={analyzingCommits}
              className="text-sm font-bold py-2 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white disabled:opacity-50 transition-all flex items-center gap-1.5"
            >
              {analyzingCommits ? (
                <><span className="inline-block w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />분석 중...</>
              ) : '🔍 분석'}
            </button>
          </div>
        </div>

        {analyzingCommits && (
          <div className="space-y-2 animate-pulse">
            {[1,2,3].map(i => <div key={i} className="h-14 bg-slate-800 rounded-xl" />)}
          </div>
        )}

        {!analyzingCommits && commits !== null && commits.length === 0 && (
          <p className="text-sm text-slate-500 text-center py-4">GitHub 커밋 이벤트가 없습니다. GitHub 연동 후 폴링을 실행해주세요.</p>
        )}

        {!analyzingCommits && commits !== null && commits.length > 0 && (
          <div className="space-y-2">
            {commits.map((c, i) => (
              <div key={i} className="flex items-start gap-3 bg-slate-800/60 rounded-xl p-3">
                <div className={`shrink-0 text-xl font-black w-8 text-center ${SCORE_COLOR[c.score] ?? 'text-slate-500'}`}>
                  {c.score > 0 ? c.score : '?'}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="font-mono text-[10px] text-slate-600 bg-slate-800 px-1.5 py-0.5 rounded">{c.sha}</span>
                    <span className="text-[10px] text-slate-500">{c.author}</span>
                  </div>
                  <p className="text-sm text-slate-300 font-medium truncate">{c.message}</p>
                  <p className="text-xs text-slate-500 mt-0.5">{c.feedback}</p>
                  {c.score > 0 && (
                    <div className="mt-1.5 w-full h-1 bg-slate-700 rounded-full overflow-hidden">
                      <div className={`h-full rounded-full ${SCORE_BAR[c.score]}`} style={{ width: `${c.score * 20}%` }} />
                    </div>
                  )}
                </div>
              </div>
            ))}
            <p className="text-[10px] text-slate-600 text-right">
              평균 점수: {(commits.filter(c => c.score > 0).reduce((s, c) => s + c.score, 0) / (commits.filter(c => c.score > 0).length || 1)).toFixed(1)} / 5.0
            </p>
          </div>
        )}

        {!analyzingCommits && commits === null && (
          <p className="text-xs text-slate-600 text-center py-2">분석 버튼을 눌러 최근 커밋 메시지 품질을 확인하세요.</p>
        )}
      </div>

      {/* 초기 안내 */}
      {!analyzing && !analysis && !loadingCached && (
        <div className="flex flex-col items-center justify-center h-64 gap-4 text-center">
          <div className="text-5xl">🤖</div>
          <div>
            <p className="text-slate-300 font-semibold">팀 활동을 AI로 분석해보세요</p>
            <p className="text-slate-500 text-sm mt-1">최근 활동 로그 50개와 기여도 점수를 기반으로 분석합니다.</p>
          </div>
          <div className="bg-slate-800/60 border border-slate-700 rounded-xl p-4 text-xs text-slate-400 space-y-1.5 text-left max-w-sm">
            <p className="font-semibold text-slate-300 mb-2">분석 항목</p>
            <p>🏥 팀 전체 건강도 평가</p>
            <p>⚖️ 기여 불균형 여부 감지</p>
            <p>💡 팀 성과 향상 제안</p>
          </div>
        </div>
      )}

      <style dangerouslySetInnerHTML={{__html: `
        .custom-scrollbar::-webkit-scrollbar { width: 6px; }
        .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
        .custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(51,65,85,0.5); border-radius: 8px; }
        @keyframes spin { to { transform: rotate(360deg); } }
        .animate-spin { animation: spin 1s linear infinite; }
      `}} />
    </div>
  );
}
