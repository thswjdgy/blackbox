'use client';

import { useState } from 'react';
import { useParams } from 'next/navigation';
import api from '@/lib/api';

export default function AnalysisPage() {
  const params = useParams();
  const projectId = params.id as string;

  const [analysis, setAnalysis] = useState<string | null>(null);
  const [analyzing, setAnalyzing] = useState(false);
  const [analyzedAt, setAnalyzedAt] = useState<string | null>(null);

  const handleAnalyze = async () => {
    setAnalyzing(true);
    try {
      const res = await api.post(`/projects/${projectId}/ai-analysis`);
      setAnalysis(res.data.analysis);
      setAnalyzedAt(new Date().toLocaleString('ko-KR'));
    } catch (e: any) {
      const msg = e?.response?.data?.message ?? 'AI 분석 실패. OPENAI_API_KEY를 확인해 주세요.';
      alert(msg);
    } finally {
      setAnalyzing(false);
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

      {/* 초기 안내 */}
      {!analyzing && !analysis && (
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
