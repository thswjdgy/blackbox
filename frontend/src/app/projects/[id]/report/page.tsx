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

export default function ReportPage() {
  const params = useParams();
  const projectId = params.id as string;

  const [report, setReport] = useState<ProjectScoreReport | null>(null);
  const [alerts, setAlerts] = useState<AlertResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [calculating, setCalculating] = useState(false);

  useEffect(() => {
    fetchData();
  }, [projectId]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [scoreRes, alertRes] = await Promise.all([
        api.get(`/projects/${projectId}/scores`),
        api.get(`/projects/${projectId}/alerts`),
      ]);
      setReport(scoreRes.data);
      setAlerts(alertRes.data);
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
        <button
          onClick={handleCalculate}
          disabled={calculating}
          className="bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-500 hover:to-indigo-500 disabled:opacity-50 text-white font-bold py-2.5 px-6 rounded-xl shadow-lg shadow-violet-900/30 transition-all hover:-translate-y-0.5"
        >
          {calculating ? '계산 중...' : '점수 재계산'}
        </button>
      </div>

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
                    { label: '태스크', value: member.taskScore, color: 'text-violet-400' },
                    { label: '회의', value: member.meetingScore, color: 'text-blue-400' },
                    { label: '파일', value: member.fileScore, color: 'text-emerald-400' },
                    { label: '합계', value: member.totalScore, color: 'text-slate-200' },
                  ].map(item => (
                    <div key={item.label} className="bg-slate-800/60 rounded-xl p-3 text-center">
                      <div className={`text-lg font-bold ${item.color}`}>{item.value.toFixed(1)}</div>
                      <div className="text-[11px] text-slate-500 mt-0.5">{item.label}</div>
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
