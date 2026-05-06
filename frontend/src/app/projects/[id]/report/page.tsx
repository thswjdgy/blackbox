'use client';

import { useState, useEffect } from 'react';
import { useParams } from 'next/navigation';
import api from '@/lib/api';

interface MemberScore {
  userId: number;
  userName: string;
  taskScore: number;
  meetingScore: number;
  fileScore: number;
  totalScore: number;
  normalizedScore: number;
  grade: string;
  calculatedAt: string;
}

interface ProjectScoreReport {
  projectId: number;
  members: MemberScore[];
  teamAverage: number;
  calculatedAt: string;
}

interface AlertResponse {
  id: number;
  projectId: number;
  alertType: 'IMBALANCE' | 'INACTIVITY' | 'OVERLOAD';
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
  message: string;
  resolved: boolean;
  createdAt: string;
  resolvedAt: string | null;
}

interface Weights {
  wTask: number;
  wMeeting: number;
  wFile: number;
  wExtra: number;
  updatedAt?: string;
}

const GRADE_COLOR: Record<string, string> = {
  A: 'text-emerald-400',
  B: 'text-blue-400',
  C: 'text-yellow-400',
  D: 'text-orange-400',
  F: 'text-red-400',
};

const SEVERITY_STYLE: Record<string, string> = {
  CRITICAL: 'border-red-500/50 bg-red-500/10 text-red-300',
  WARNING:  'border-yellow-500/50 bg-yellow-500/10 text-yellow-300',
  INFO:     'border-blue-500/50 bg-blue-500/10 text-blue-300',
};

const SEVERITY_ICON: Record<string, string> = {
  CRITICAL: '🚨',
  WARNING:  '⚠️',
  INFO:     'ℹ️',
};

const PRESETS: Record<string, Weights> = {
  balanced:  { wTask: 0.35, wMeeting: 0.30, wFile: 0.20, wExtra: 0.15 },
  dev:       { wTask: 0.45, wMeeting: 0.15, wFile: 0.15, wExtra: 0.25 },
  design:    { wTask: 0.30, wMeeting: 0.20, wFile: 0.40, wExtra: 0.10 },
  meeting:   { wTask: 0.20, wMeeting: 0.50, wFile: 0.15, wExtra: 0.15 },
};

const PRESET_LABELS: Record<string, string> = {
  balanced: '균형',
  dev:      '개발 중심',
  design:   '디자인 중심',
  meeting:  '회의 중심',
};

const WEIGHT_LABELS = [
  { key: 'wTask',    label: '태스크',    color: 'bg-violet-500' },
  { key: 'wMeeting', label: '회의',      color: 'bg-blue-500' },
  { key: 'wFile',    label: '파일',      color: 'bg-emerald-500' },
  { key: 'wExtra',   label: '외부활동',  color: 'bg-amber-500' },
] as const;

export default function ReportPage() {
  const params = useParams();
  const projectId = params.id as string;

  const [report, setReport] = useState<ProjectScoreReport | null>(null);
  const [alerts, setAlerts] = useState<AlertResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [calculating, setCalculating] = useState(false);

  const [weights, setWeights] = useState<Weights>({ wTask: 0.35, wMeeting: 0.30, wFile: 0.20, wExtra: 0.15 });
  const [savedWeights, setSavedWeights] = useState<Weights | null>(null);
  const [savingWeights, setSavingWeights] = useState(false);
  const [weightOpen, setWeightOpen] = useState(false);

  useEffect(() => {
    fetchData();
  }, [projectId]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [scoreRes, alertRes, weightRes] = await Promise.allSettled([
        api.get(`/projects/${projectId}/scores`),
        api.get(`/projects/${projectId}/alerts`),
        api.get(`/projects/${projectId}/weights`),
      ]);
      if (scoreRes.status === 'fulfilled') setReport(scoreRes.value.data);
      if (alertRes.status === 'fulfilled') setAlerts(alertRes.value.data);
      if (weightRes.status === 'fulfilled') {
        const d = weightRes.value.data ?? {};
        // 대소문자 두 가지 모두 시도 (jackson 직렬화 불확실성 대비)
        const pick = (a: string, b: string) => safeW(d[a] ?? d[b]);
        const w: Weights = {
          wTask:    pick('wTask',    'w_task'),
          wMeeting: pick('wMeeting', 'w_meeting'),
          wFile:    pick('wFile',    'w_file'),
          wExtra:   pick('wExtra',   'w_extra'),
          updatedAt: d.updatedAt,
        };
        const allZero = w.wTask === 0 && w.wMeeting === 0 && w.wFile === 0 && w.wExtra === 0;
        const finalW = allZero
          ? { wTask: 0.35, wMeeting: 0.30, wFile: 0.20, wExtra: 0.15 }
          : w;
        setWeights(finalW);
        setSavedWeights(finalW);
      }
    } catch (e) {
      console.error('Failed to fetch report', e);
    } finally {
      setLoading(false);
    }
  };

  const handleCalculate = async () => {
    setCalculating(true);
    try {
      const res = await api.post(`/projects/${projectId}/scores/calculate`);
      setReport(res.data);
      const alertRes = await api.get(`/projects/${projectId}/alerts`);
      setAlerts(alertRes.data);
    } catch (e) {
      console.error('Failed to calculate', e);
    } finally {
      setCalculating(false);
    }
  };

  const handleResolveAlert = async (alertId: number) => {
    try {
      await api.patch(`/projects/${projectId}/alerts/${alertId}/resolve`);
      setAlerts(prev => prev.filter(a => a.id !== alertId));
    } catch (e) {
      console.error('Failed to resolve alert', e);
    }
  };

  // NaN 방지: API 응답에 projectId 등 불필요한 필드가 섞여도 안전하게 처리
  const safeW = (v: unknown) => (typeof v === 'number' && !isNaN(v) ? v : 0);
  const wTask    = safeW(weights.wTask);
  const wMeeting = safeW(weights.wMeeting);
  const wFile    = safeW(weights.wFile);
  const wExtra   = safeW(weights.wExtra);

  const weightSum = +((wTask + wMeeting + wFile + wExtra)).toFixed(4);
  const weightValid = Math.abs(weightSum - 1.0) < 0.001;

  const handleSlider = (key: 'wTask' | 'wMeeting' | 'wFile' | 'wExtra', value: number) => {
    setWeights(prev => ({ ...prev, [key]: value }));
  };

  const applyPreset = (presetKey: string) => {
    const p = PRESETS[presetKey];
    setWeights({ wTask: p.wTask, wMeeting: p.wMeeting, wFile: p.wFile, wExtra: p.wExtra });
  };

  const handleSaveWeights = async () => {
    if (!weightValid) return;
    setSavingWeights(true);
    try {
      const res = await api.put(`/projects/${projectId}/weights`, {
        wTask, wMeeting, wFile, wExtra,
      });
      // 응답 파싱 대신 보낸 값을 저장 — 응답 필드명 불일치 방지
      const saved: Weights = {
        wTask, wMeeting, wFile, wExtra,
        updatedAt: res.data?.updatedAt ?? new Date().toISOString(),
      };
      setSavedWeights(saved);
      // weights 상태는 건드리지 않음 — 슬라이더 위치 유지
    } catch (e) {
      console.error('Failed to save weights', e);
      alert('가중치 저장 실패');
    } finally {
      setSavingWeights(false);
    }
  };

  const [downloadingPdf, setDownloadingPdf] = useState(false);
  const handleDownloadPdf = async () => {
    setDownloadingPdf(true);
    try {
      const res = await api.get(`/projects/${projectId}/report/pdf`, { responseType: 'blob' });
      const url = URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.download = `contribution-report-${projectId}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (e) {
      alert('PDF 다운로드 실패');
    } finally {
      setDownloadingPdf(false);
    }
  };

  const handleRevert = () => {
    if (!savedWeights) return;
    setWeights({
      wTask:    safeW(savedWeights.wTask),
      wMeeting: safeW(savedWeights.wMeeting),
      wFile:    safeW(savedWeights.wFile),
      wExtra:   safeW(savedWeights.wExtra),
      updatedAt: savedWeights.updatedAt,
    });
  };

  const weightsChanged = savedWeights
    ? Math.abs(wTask    - safeW(savedWeights.wTask))    > 0.001 ||
      Math.abs(wMeeting - safeW(savedWeights.wMeeting)) > 0.001 ||
      Math.abs(wFile    - safeW(savedWeights.wFile))    > 0.001 ||
      Math.abs(wExtra   - safeW(savedWeights.wExtra))   > 0.001
    : true;

  if (loading) return <div className="p-8 text-white">Loading Report...</div>;

  const maxScore = report ? Math.max(...report.members.map(m => m.normalizedScore), 1) : 1;

  return (
    <div className="flex flex-col h-full overflow-y-auto p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between shrink-0">
        <div>
          <h2 className="text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-violet-400 to-indigo-400">기여도 리포트</h2>
          {report && (
            <p className="text-sm text-slate-400 mt-1">
              마지막 계산: {new Date(report.calculatedAt).toLocaleString('ko-KR')}
              &nbsp;·&nbsp;팀 평균: {report.teamAverage.toFixed(1)}점
            </p>
          )}
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={handleDownloadPdf}
            disabled={downloadingPdf}
            className="py-2.5 px-5 rounded-xl font-bold text-sm transition-all border bg-slate-800 border-slate-700 text-slate-300 hover:border-slate-500 disabled:opacity-50"
          >
            {downloadingPdf ? 'PDF 생성 중...' : '📄 PDF 다운로드'}
          </button>
          <button
            onClick={() => setWeightOpen(o => !o)}
            className={`py-2.5 px-5 rounded-xl font-bold text-sm transition-all border
              ${weightOpen
                ? 'bg-amber-500/20 text-amber-300 border-amber-500/50'
                : 'bg-slate-800 border-slate-700 text-slate-300 hover:border-slate-500'}`}
          >
            ⚖️ 가중치 조정
          </button>
          <button
            onClick={handleCalculate}
            disabled={calculating}
            className="bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-500 hover:to-indigo-500 disabled:opacity-50 text-white font-bold py-2.5 px-6 rounded-xl shadow-lg shadow-violet-900/30 transition-all hover:-translate-y-0.5"
          >
            {calculating ? '계산 중...' : '점수 재계산'}
          </button>
        </div>
      </div>

      {/* Weight Panel */}
      {weightOpen && (
        <div className="shrink-0 bg-slate-900/80 border border-amber-500/20 rounded-2xl p-6 space-y-5">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-sm font-bold text-amber-300 uppercase tracking-wider">가중치 조정</h3>
              <p className="text-xs text-slate-500 mt-0.5">항목별 가중치 합계가 정확히 100%여야 합니다.</p>
            </div>
            <div className={`text-sm font-bold px-3 py-1 rounded-lg border ${
              weightValid
                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                : 'bg-red-500/10 text-red-400 border-red-500/30'
            }`}>
              합계: {(weightSum * 100).toFixed(1)}%
            </div>
          </div>

          {/* Preset Buttons */}
          <div className="flex flex-wrap gap-2">
            <span className="text-xs text-slate-500 self-center mr-1">프리셋:</span>
            {Object.entries(PRESET_LABELS).map(([key, label]) => (
              <button
                key={key}
                onClick={() => applyPreset(key)}
                className="text-xs px-3 py-1.5 rounded-lg bg-slate-800 border border-slate-700 text-slate-300 hover:border-amber-500/50 hover:text-amber-300 transition-all"
              >
                {label}
              </button>
            ))}
          </div>

          {/* Sliders */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {WEIGHT_LABELS.map(({ key, label, color }) => {
              const val = safeW(weights[key]);
              return (
                <div key={key}>
                  <div className="flex justify-between items-center mb-2">
                    <label className="text-xs font-semibold text-slate-300">{label}</label>
                    <span className="text-xs font-bold text-white tabular-nums">
                      {(val * 100).toFixed(0)}%
                    </span>
                  </div>
                  <div className="relative">
                    <input
                      type="range"
                      min={0}
                      max={1}
                      step={0.05}
                      value={val}
                      onChange={e => handleSlider(key, parseFloat(e.target.value))}
                      className="w-full h-2 rounded-full appearance-none cursor-pointer accent-violet-500 bg-slate-700"
                    />
                    <div
                      className={`h-2 rounded-full absolute top-0 left-0 pointer-events-none ${color}`}
                      style={{ width: `${val * 100}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>

          {/* Visual breakdown bar */}
          <div>
            <p className="text-xs text-slate-500 mb-2">가중치 비율 미리보기</p>
            <div className="flex h-3 rounded-full overflow-hidden gap-0.5">
              <div className="bg-violet-500 transition-all duration-200" style={{ width: `${weights.wTask * 100}%` }} title={`태스크 ${(weights.wTask*100).toFixed(0)}%`} />
              <div className="bg-blue-500 transition-all duration-200"   style={{ width: `${weights.wMeeting * 100}%` }} title={`회의 ${(weights.wMeeting*100).toFixed(0)}%`} />
              <div className="bg-emerald-500 transition-all duration-200" style={{ width: `${weights.wFile * 100}%` }} title={`파일 ${(weights.wFile*100).toFixed(0)}%`} />
              <div className="bg-amber-500 transition-all duration-200"  style={{ width: `${weights.wExtra * 100}%` }} title={`외부 ${(weights.wExtra*100).toFixed(0)}%`} />
            </div>
            <div className="flex gap-4 mt-1.5 text-[10px] text-slate-500">
              <span><span className="inline-block w-2 h-2 rounded-sm bg-violet-500 mr-1" />태스크</span>
              <span><span className="inline-block w-2 h-2 rounded-sm bg-blue-500 mr-1" />회의</span>
              <span><span className="inline-block w-2 h-2 rounded-sm bg-emerald-500 mr-1" />파일</span>
              <span><span className="inline-block w-2 h-2 rounded-sm bg-amber-500 mr-1" />외부활동</span>
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-1 border-t border-slate-800">
            <button
              onClick={handleRevert}
              disabled={!weightsChanged}
              className="text-xs px-4 py-2 rounded-lg bg-slate-800 border border-slate-700 text-slate-400 hover:text-slate-200 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
            >
              되돌리기
            </button>
            <button
              onClick={handleSaveWeights}
              disabled={!weightValid || !weightsChanged || savingWeights}
              className="text-sm font-bold px-5 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-black disabled:opacity-40 disabled:cursor-not-allowed transition-all"
            >
              {savingWeights ? '저장 중...' : '가중치 저장'}
            </button>
          </div>
          {savedWeights?.updatedAt && (
            <p className="text-[10px] text-slate-600 text-right -mt-3">
              마지막 저장: {new Date(savedWeights.updatedAt).toLocaleString('ko-KR')}
            </p>
          )}
        </div>
      )}

      {/* Alerts */}
      {alerts.length > 0 && (
        <div className="shrink-0 space-y-2">
          <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider">활성 경보</h3>
          {alerts.map(alert => (
            <div key={alert.id} className={`flex items-start justify-between gap-4 p-4 rounded-xl border ${SEVERITY_STYLE[alert.severity]}`}>
              <div className="flex items-start gap-3">
                <span className="text-lg">{SEVERITY_ICON[alert.severity]}</span>
                <div>
                  <div className="font-semibold text-sm">{alert.alertType}</div>
                  <div className="text-sm mt-0.5 opacity-90">{alert.message}</div>
                </div>
              </div>
              <button
                onClick={() => handleResolveAlert(alert.id)}
                className="shrink-0 text-xs px-3 py-1 rounded-lg bg-slate-800 border border-slate-700 text-slate-300 hover:text-white hover:border-slate-500 transition-colors"
              >
                해결 처리
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Score Cards */}
      {report && report.members.length > 0 ? (
        <div className="grid grid-cols-1 gap-4">
          {report.members
            .sort((a, b) => b.normalizedScore - a.normalizedScore)
            .map((member, idx) => (
              <div key={member.userId} className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-full bg-gradient-to-br from-violet-500 to-indigo-600 flex items-center justify-center font-bold text-sm">
                      {idx + 1}
                    </div>
                    <div>
                      <div className="font-bold text-white">{member.userName}</div>
                      <div className="text-xs text-slate-400">정규화 점수 {member.normalizedScore.toFixed(1)}</div>
                    </div>
                  </div>
                  <div className={`text-4xl font-black ${GRADE_COLOR[member.grade] || 'text-slate-400'}`}>
                    {member.grade}
                  </div>
                </div>

                {/* Progress bar */}
                <div className="mb-4">
                  <div className="flex justify-between text-xs text-slate-500 mb-1">
                    <span>기여도</span>
                    <span>{member.normalizedScore.toFixed(1)} / 150</span>
                  </div>
                  <div className="w-full h-2 bg-slate-800 rounded-full overflow-hidden">
                    <div
                      className="h-full rounded-full bg-gradient-to-r from-violet-500 to-indigo-500 transition-all duration-500"
                      style={{ width: `${Math.min((member.normalizedScore / 150) * 100, 100)}%` }}
                    />
                  </div>
                </div>

                {/* Score breakdown */}
                <div className="grid grid-cols-4 gap-3">
                  {[
                    { label: '태스크', value: member.taskScore,    color: 'text-violet-400',  pct: safeW(savedWeights?.wTask) },
                    { label: '회의',   value: member.meetingScore, color: 'text-blue-400',    pct: safeW(savedWeights?.wMeeting) },
                    { label: '파일',   value: member.fileScore,    color: 'text-emerald-400', pct: safeW(savedWeights?.wFile) },
                    { label: '합계',   value: member.totalScore,   color: 'text-slate-200',   pct: null },
                  ].map(item => (
                    <div key={item.label} className="bg-slate-800/60 rounded-xl p-3 text-center">
                      <div className={`text-lg font-bold ${item.color}`}>{item.value.toFixed(1)}</div>
                      <div className="text-[11px] text-slate-500 mt-0.5">
                        {item.label}
                        {item.pct !== null && (
                          <span className="text-slate-600 ml-1">({(item.pct * 100).toFixed(0)}%)</span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ))}
        </div>
      ) : (
        <div className="flex-1 flex items-center justify-center text-slate-500">
          <div className="text-center">
            <div className="text-4xl mb-3">📊</div>
            <p className="font-medium">아직 점수 데이터가 없습니다.</p>
            <p className="text-sm mt-1">점수 재계산 버튼을 눌러 시작하세요.</p>
          </div>
        </div>
      )}
    </div>
  );
}
