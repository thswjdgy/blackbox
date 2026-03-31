'use client';

import Link from 'next/link';
import { usePathname, useParams } from 'next/navigation';
import { ReactNode } from 'react';

export default function ProjectLayout({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const params = useParams();
  const projectId = params.id as string;

  const tabs = [
    { name: '대시보드로 가기', path: '/dashboard', icon: '←' },
    { name: '태스크 칸반 보드', path: `/projects/${projectId}/board`, icon: '📋' },
    { name: '회의록', path: `/projects/${projectId}/meetings`, icon: '📝' },
    { name: '파일 (Hash Vault)', path: `/projects/${projectId}/vault`, icon: '🔒' },
    { name: '기여도 리포트', path: `/projects/${projectId}/report`, icon: '📊' },
  ];

  return (
    <div className="flex h-screen bg-slate-950 text-white overflow-hidden">
      {/* Sidebar */}
      <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col h-full shrink-0">
        <div className="p-6 border-b border-slate-800">
          <div className="flex items-center gap-2 mb-1">
            <div className="w-8 h-8 rounded bg-gradient-to-br from-violet-500 to-indigo-600 flex items-center justify-center font-black">B</div>
            <h1 className="font-bold text-lg tracking-tight">Team Blackbox</h1>
          </div>
          <p className="text-xs text-slate-400">Project ID: {projectId}</p>
        </div>

        <nav className="flex-1 overflow-y-auto p-4 space-y-1">
          {tabs.map(tab => {
            const isActive = pathname === tab.path || (tab.path.includes('board') && pathname === `/projects/${projectId}`);
            return (
              <Link 
                key={tab.path} 
                href={tab.path}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${isActive ? 'bg-violet-600 text-white shadow-lg shadow-violet-900/20' : 'text-slate-400 hover:text-white hover:bg-slate-800'}`}
              >
                <span>{tab.icon}</span>
                {tab.name}
              </Link>
            )
          })}
        </nav>
      </aside>

      {/* Main Content */}
      <main className="flex-1 h-full overflow-hidden bg-gradient-to-br from-slate-950 to-slate-900 flex flex-col">
        {children}
      </main>
    </div>
  );
}
