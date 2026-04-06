'use client';

import { useEffect, useState, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import api from '@/lib/api';

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
}

export default function ProjectDetailPage() {
  const params = useParams();
  const router = useRouter();
  const projectId = params.id as string;

  const [project, setProject] = useState<ProjectDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [copySuccess, setCopySuccess] = useState(false);

  const fetchProject = useCallback(async () => {
    try {
      const res = await api.get(`/projects/${projectId}`);
      setProject(res.data);
    } catch (err) {
      console.error('Failed to fetch project', err);
      // If unauthorized or not found, go back
      router.replace('/dashboard');
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    fetchProject();
  }, [fetchProject]);

  const handleCopyCode = async () => {
    if (!project) return;
    try {
      await navigator.clipboard.writeText(project.inviteCode);
      setCopySuccess(true);
      setTimeout(() => setCopySuccess(false), 2000);
    } catch (err) {
      console.error(err);
    }
  };

  if (loading) return <div className="p-8 text-slate-400">데이터를 불러오는 중...</div>;
  if (!project) return null;

  return (
    <div className="flex-1 h-full overflow-y-auto custom-scrollbar p-6">
      {/* Hero Header */}
      <div className="relative overflow-hidden bg-slate-900 border border-slate-800 rounded-3xl p-8 mb-8 shadow-2xl">
        <div className="absolute top-0 right-0 p-8 opacity-10 pointer-events-none">
          <div className="text-9xl font-black text-white">{project.name.charAt(0).toUpperCase()}</div>
        </div>
        
        <div className="relative flex flex-col md:flex-row md:items-end justify-between gap-6">
          <div className="max-w-2xl">
            <div className="flex items-center gap-3 mb-4">
              <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider ${project.active ? 'bg-emerald-900/40 text-emerald-400 border border-emerald-500/20' : 'bg-slate-700 text-slate-400 border border-slate-600'}`}>
                {project.active ? 'Active' : 'Archived'}
              </span>
              <span className="text-slate-500 text-xs">Created: {new Date(project.createdAt).toLocaleDateString()}</span>
            </div>
            <h1 className="text-4xl font-black text-white mb-3 tracking-tight">{project.name}</h1>
            <p className="text-slate-400 text-lg leading-relaxed">{project.description || '이 프로젝트에 대한 설명이 없습니다.'}</p>
          </div>
          
          <div className="shrink-0 bg-slate-800/60 backdrop-blur-sm p-5 rounded-2xl border border-slate-700/50 shadow-xl group">
            <p className="text-[10px] uppercase font-bold text-slate-500 mb-2 tracking-widest">Invite Team Members</p>
            <div className="flex items-center gap-3">
              <span className="font-mono text-2xl font-black text-violet-400 tracking-tighter">{project.inviteCode}</span>
              <button 
                onClick={handleCopyCode}
                className="p-2 rounded-xl bg-slate-700 hover:bg-violet-600 text-white transition-all active:scale-95 shadow-lg group-hover:shadow-violet-900/20"
                title="초대 코드 복사"
              >
                {copySuccess ? '✓' : 'Copy'}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Main Content Area */}
        <div className="lg:col-span-2 space-y-8">
          {/* Quick Access Grid */}
          <div>
            <h2 className="text-xl font-bold text-white mb-6">주요 기능</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <NavCard 
                href={`/projects/${projectId}/board`} 
                title="태스크 보드" 
                desc="칸반 보드에서 팀의 업무 흐름을 관리하세요." 
                icon="📋"
                color="violet"
              />
              <NavCard 
                href={`/projects/${projectId}/meetings`} 
                title="회의록" 
                desc="회의록을 기록하고 체크인 현황을 추적하세요." 
                icon="📝"
                color="emerald"
              />
              <NavCard 
                href={`/projects/${projectId}/vault`} 
                title="파일 금고" 
                desc="암호화된 해시를 저장하여 파일 무결성을 보장하세요." 
                icon="🔒"
                color="indigo"
              />
              <NavCard 
                href={`/projects/${projectId}/report`} 
                title="기여도 리포트" 
                desc="활동 로그 기반의 기여 분석 데이터를 확인하세요." 
                icon="📊"
                color="amber"
              />
            </div>
          </div>
        </div>

        {/* Sidebar Area: Members */}
        <div>
          <div className="bg-slate-900/60 border border-slate-800 rounded-3xl p-6 shadow-xl sticky top-6">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-bold text-white">팀 멤버 <span className="text-slate-500 text-sm ml-2">{project.members.length}</span></h2>
            </div>
            
            <ul className="space-y-4">
              {project.members.map(member => (
                <li key={member.userId} className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-slate-800 to-slate-900 border border-slate-700 flex items-center justify-center text-slate-300 font-bold">
                    {member.name.charAt(0).toUpperCase()}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-bold text-white truncate">{member.name}</p>
                    <p className="text-[10px] text-slate-500 uppercase tracking-wider font-semibold">{member.projectRole}</p>
                  </div>
                  <div className={`px-2 py-0.5 rounded-full text-[9px] font-bold uppercase ${member.role === 'PROFESSOR' ? 'bg-amber-900/30 text-amber-500 border border-amber-500/20' : 'bg-slate-800 text-slate-500'}`}>
                    {member.role}
                  </div>
                </li>
              ))}
            </ul>
          </div>
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

function NavCard({ href, title, desc, icon, color }: { href: string; title: string; desc: string; icon: string; color: string }) {
  const colorMap: Record<string, string> = {
    violet: 'hover:border-violet-500 group-hover:bg-violet-600',
    emerald: 'hover:border-emerald-500 group-hover:bg-emerald-600',
    indigo: 'hover:border-indigo-500 group-hover:bg-indigo-600',
    amber: 'hover:border-amber-500 group-hover:bg-amber-600',
  };

  return (
    <Link href={href} className={`group block p-6 bg-slate-900 border border-slate-800 rounded-2xl transition-all hover:shadow-2xl hover:-translate-y-1 ${colorMap[color].split(' ')[0]}`}>
      <div className={`w-12 h-12 rounded-xl bg-slate-800 flex items-center justify-center text-2xl mb-4 transition-colors ${colorMap[color].split(' ')[1]}`}>
        {icon}
      </div>
      <h3 className="text-lg font-bold text-white mb-2">{title}</h3>
      <p className="text-sm text-slate-400 leading-relaxed">{desc}</p>
    </Link>
  );
}
