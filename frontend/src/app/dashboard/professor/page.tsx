'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts';
import api from '@/lib/api';

interface ProjectOverview {
  projectId: number;
  projectName: string;
  memberCount: number;
  averageScore: number;
  alertCount: number;
  completedTasks: number;
  totalTasks: number;
}

export default function ProfessorDashboardPage() {
  const router = useRouter();
  const [overviews, setOverviews] = useState<ProjectOverview[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchOverviews();
  }, []);

  const fetchOverviews = async () => {
    try {
      const res = await api.get('/professor/projects-overview');
      setOverviews(res.data);
    } catch (e) {
      console.error(e);
      alert('오버뷰 데이터를 불러오는데 실패했습니다. 교수 권한인지 확인하세요.');
      router.push('/dashboard');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="p-8 text-slate-300">Loading Dashboard...</div>;

  return (
    <div className="min-h-screen bg-slate-950 text-white p-8">
      <div className="max-w-6xl mx-auto space-y-8">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-emerald-400 to-cyan-400">
              Professor Dashboard
            </h1>
            <p className="text-slate-400 mt-2">전체 프로젝트 팀의 현황과 기여도를 한눈에 모니터링하세요.</p>
          </div>
          <Link href="/dashboard" className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-white font-medium rounded-lg transition-colors border border-slate-700">
            내 대시보드로 복귀
          </Link>
        </div>

        {/* 요약 카드 */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-slate-900 border border-slate-800 p-5 rounded-xl shadow-lg">
            <h3 className="text-slate-400 text-sm font-medium mb-1">활성 프로젝트</h3>
            <p className="text-3xl font-bold text-white">{overviews.length} <span className="text-lg text-slate-500 font-normal">팀</span></p>
          </div>
          <div className="bg-slate-900 border border-slate-800 p-5 rounded-xl shadow-lg">
            <h3 className="text-slate-400 text-sm font-medium mb-1">총 관리 학생 수</h3>
            <p className="text-3xl font-bold text-cyan-400">{overviews.reduce((acc, curr) => acc + curr.memberCount, 0)} <span className="text-lg text-slate-500 font-normal">명</span></p>
          </div>
          <div className="bg-slate-900 border border-slate-800 p-5 rounded-xl shadow-lg">
            <h3 className="text-slate-400 text-sm font-medium mb-1">평균 기여도 (전체 팀)</h3>
            <p className="text-3xl font-bold text-emerald-400">
              {overviews.length > 0 ? Math.round(overviews.reduce((acc, curr) => acc + curr.averageScore, 0) / overviews.length) : 0} <span className="text-lg text-slate-500 font-normal">점</span>
            </p>
          </div>
          <div className="bg-red-900/20 border border-red-500/30 p-5 rounded-xl shadow-lg">
            <h3 className="text-red-400 text-sm font-medium mb-1">총 미해결 알림</h3>
            <p className="text-3xl font-bold text-red-400">
              {overviews.reduce((acc, curr) => acc + curr.alertCount, 0)} <span className="text-lg text-red-500/70 font-normal">건</span>
            </p>
          </div>
        </div>

        {/* 차트 영역 */}
        <div className="bg-slate-900 border border-slate-800 p-6 rounded-2xl shadow-xl">
          <h2 className="text-xl font-bold mb-6 text-slate-200">팀별 평균 기여도 및 태스크 달성 현황</h2>
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={overviews} margin={{ top: 20, right: 30, left: 0, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" vertical={false} />
                <XAxis dataKey="projectName" stroke="#94a3b8" tick={{ fill: '#94a3b8' }} />
                <YAxis yAxisId="left" stroke="#10b981" tick={{ fill: '#94a3b8' }} />
                <YAxis yAxisId="right" orientation="right" stroke="#38bdf8" tick={{ fill: '#94a3b8' }} />
                <Tooltip 
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '8px' }}
                  itemStyle={{ color: '#e2e8f0' }}
                />
                <Legend />
                <Bar yAxisId="left" dataKey="averageScore" name="평균 점수" fill="#10b981" radius={[4, 4, 0, 0]} />
                <Bar yAxisId="right" dataKey="completedTasks" name="완료된 태스크" fill="#38bdf8" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* 프로젝트 테이블 */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl shadow-xl overflow-hidden">
          <div className="p-6 border-b border-slate-800">
            <h2 className="text-xl font-bold text-slate-200">개별 팀 상세 현황</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-800/50 text-slate-400 text-sm">
                  <th className="p-4 font-medium border-b border-slate-800">상태</th>
                  <th className="p-4 font-medium border-b border-slate-800">프로젝트명 (ID)</th>
                  <th className="p-4 font-medium border-b border-slate-800">멤버 수</th>
                  <th className="p-4 font-medium border-b border-slate-800">태스크 달성률</th>
                  <th className="p-4 font-medium border-b border-slate-800">평균 점수</th>
                  <th className="p-4 font-medium border-b border-slate-800">미해결 알림</th>
                  <th className="p-4 font-medium border-b border-slate-800">관리 바로가기</th>
                </tr>
              </thead>
              <tbody>
                {overviews.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="p-8 text-center text-slate-500">조회된 프로젝트가 없습니다.</td>
                  </tr>
                ) : overviews.map(proj => {
                  const progress = proj.totalTasks > 0 ? Math.round((proj.completedTasks / proj.totalTasks) * 100) : 0;
                  
                  // 건강도 지표 결정 로직
                  let healthIcon = '🟢';
                  if (proj.alertCount > 0) healthIcon = '🔴';
                  else if (progress < 20 && proj.totalTasks > 0) healthIcon = '🟡';
                  else if (proj.averageScore < 60) healthIcon = '🟠';

                  return (
                    <tr key={proj.projectId} className="border-b border-slate-800/50 hover:bg-slate-800/30 transition-colors">
                      <td className="p-4 text-center text-lg leading-none" title={healthIcon === '🔴' ? '위험' : healthIcon === '🟢' ? '정상' : '주의'}>
                        {healthIcon}
                      </td>
                      <td className="p-4 font-medium text-slate-200">{proj.projectName} <span className="text-slate-500 text-xs ml-1">#{proj.projectId}</span></td>
                      <td className="p-4 text-slate-400">{proj.memberCount}명</td>
                      <td className="p-4">
                        <div className="flex items-center gap-2">
                          <div className="w-24 h-2 bg-slate-800 rounded-full overflow-hidden">
                            <div className="h-full bg-cyan-500 rounded-full" style={{ width: `${progress}%` }}></div>
                          </div>
                          <span className="text-xs text-slate-400">{progress}%</span>
                        </div>
                      </td>
                      <td className="p-4 font-bold text-emerald-400">{proj.averageScore}</td>
                      <td className="p-4">
                        {proj.alertCount > 0 ? (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-bold bg-red-500/20 text-red-400 border border-red-500/30">
                            {proj.alertCount} 건 경보
                          </span>
                        ) : (
                          <span className="text-xs text-slate-500">안정적</span>
                        )}
                      </td>
                      <td className="p-4">
                        <Link href={`/projects/${proj.projectId}/report`} className="text-sm text-indigo-400 hover:text-indigo-300 font-medium">
                          기여도 리포트 보기 →
                        </Link>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>

      </div>
    </div>
  );
}
