'use client';

import Link from 'next/link';
import { usePathname, useParams, useRouter } from 'next/navigation';
import { ReactNode, useState, useEffect } from 'react';
import api from '@/lib/api';
import { useAuthStore } from '@/store/authStore';
import { useTheme } from '@/components/ThemeProvider';

/* ── 섹션 좌우 네비게이션 바 ─────────────────────────── */
interface Tab { name: string; path: string; icon: string }

function SectionNav({ tabs, pathname }: { tabs: Tab[]; pathname: string }) {
  const router = useRouter();
  const sections = tabs.slice(2); // 대시보드·홈 제외
  const currentIdx = sections.findIndex(t =>
    pathname === t.path || pathname.startsWith(t.path + '/')
  );
  const current = sections[currentIdx];
  const prev = currentIdx > 0 ? sections[currentIdx - 1] : null;
  const next = currentIdx < sections.length - 1 ? sections[currentIdx + 1] : null;

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (!e.altKey) return;
      if (e.key === 'ArrowLeft' && prev) router.push(prev.path);
      if (e.key === 'ArrowRight' && next) router.push(next.path);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [prev, next, router]);

  if (!current) return null;

  return (
    <div className="shrink-0 flex items-center justify-between px-5 py-2 border-b border-slate-800/60 bg-slate-900/30">
      {prev ? (
        <Link href={prev.path} className="flex items-center gap-1.5 text-xs text-slate-500 hover:text-slate-200 transition-colors group" title="Alt + ←">
          <span className="text-base group-hover:-translate-x-0.5 transition-transform">←</span>
          <span>{prev.icon} {prev.name}</span>
        </Link>
      ) : <div />}

      <span className="text-xs text-slate-400 font-medium">
        {current.icon} {current.name}
        <span className="ml-2 text-slate-600">({currentIdx + 1}/{sections.length})</span>
      </span>

      {next ? (
        <Link href={next.path} className="flex items-center gap-1.5 text-xs text-slate-500 hover:text-slate-200 transition-colors group" title="Alt + →">
          <span>{next.icon} {next.name}</span>
          <span className="text-base group-hover:translate-x-0.5 transition-transform">→</span>
        </Link>
      ) : <div />}
    </div>
  );
}

/* ── 햄버거 아이콘 ────────────────────────────────────── */
function HamburgerIcon() {
  return (
    <div className="flex flex-col gap-[5px] w-5">
      <span className="block h-[2px] w-full bg-current rounded-full" />
      <span className="block h-[2px] w-full bg-current rounded-full" />
      <span className="block h-[2px] w-full bg-current rounded-full" />
    </div>
  );
}

/* ── 메인 레이아웃 ────────────────────────────────────── */
export default function ProjectLayout({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const params = useParams();
  const projectId = params.id as string;
  const { user } = useAuthStore();

  const { theme, toggle: toggleTheme } = useTheme();
  const [projectName, setProjectName] = useState<string>('');
  const [isCollapsed, setIsCollapsed] = useState(false);

  useEffect(() => {
    const saved = localStorage.getItem('sidebar-collapsed');
    if (saved === 'true') setIsCollapsed(true);
  }, []);

  const toggleSidebar = () => {
    const next = !isCollapsed;
    setIsCollapsed(next);
    localStorage.setItem('sidebar-collapsed', String(next));
  };

  useEffect(() => {
    if (!projectId) return;
    api.get(`/projects/${projectId}`)
      .then(res => setProjectName(res.data.name))
      .catch(() => setProjectName('프로젝트'));
  }, [projectId]);

  const tabs = [
    { name: '대시보드',      path: '/dashboard',                        icon: '🏠' },
    { name: '홈',            path: `/projects/${projectId}`,            icon: '📂' },
    { name: '태스크 보드',   path: `/projects/${projectId}/board`,      icon: '📋' },
    { name: '회의록',        path: `/projects/${projectId}/meetings`,   icon: '📝' },
    { name: '파일 검사',     path: `/projects/${projectId}/vault`,      icon: '🔒' },
    { name: '기여도 리포트', path: `/projects/${projectId}/report`,     icon: '📊' },
    { name: '타임라인',     path: `/projects/${projectId}/timeline`,   icon: '🕐' },
    { name: '설정',         path: `/projects/${projectId}/settings`,   icon: '⚙️' },
  ];

  // 사이드바 상단에 표시할 유저 정보
  const displayName = user?.name ?? '';
  const displaySub  = user?.email ?? '';

  return (
    <div className="flex h-screen w-screen bg-slate-950 text-white overflow-hidden font-sans">
      {/* ── 사이드바 ── */}
      <aside
        className={`bg-slate-900 border-r border-slate-800 flex flex-col h-full shrink-0 transition-all duration-300 z-[100] ${isCollapsed ? 'w-16' : 'w-64'}`}
      >
        {/* 햄버거 토글 + 유저 정보 헤더 */}
        <div className={`border-b border-slate-800 flex items-center gap-3 px-4 py-4 ${isCollapsed ? 'justify-center' : ''}`}>
          <button
            onClick={toggleSidebar}
            className="shrink-0 w-9 h-9 rounded-lg flex items-center justify-center text-slate-400 hover:text-white hover:bg-slate-800 transition-all"
            title={isCollapsed ? '펼치기' : '접기'}
          >
            <HamburgerIcon />
          </button>

          {!isCollapsed && (
            <div className="min-w-0 animate-fade-in">
              <p className="font-bold text-sm text-white truncate leading-tight">{displayName}</p>
              <p className="text-[11px] text-slate-500 truncate">{displaySub}</p>
            </div>
          )}
        </div>

        {/* 프로젝트명 */}
        {!isCollapsed && (
          <div className="px-4 py-3 border-b border-slate-800/60">
            <div className="flex items-center gap-2">
              <div className="w-7 h-7 rounded-lg bg-gradient-to-br from-violet-600 to-indigo-600 flex items-center justify-center font-black text-sm shrink-0">B</div>
              <p className="font-semibold text-sm text-slate-200 truncate">{projectName}</p>
            </div>
          </div>
        )}
        {isCollapsed && (
          <div className="flex justify-center py-3 border-b border-slate-800/60">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-violet-600 to-indigo-600 flex items-center justify-center font-black text-sm">B</div>
          </div>
        )}

        {/* 네비게이션 */}
        <nav className="flex-1 overflow-y-auto p-3 space-y-1 custom-scrollbar">
          {tabs.map(tab => {
            const isActive = pathname === tab.path ||
              (pathname.startsWith(tab.path + '/') && tab.path !== `/projects/${projectId}`);
            return (
              <Link
                key={tab.path}
                href={tab.path}
                title={isCollapsed ? tab.name : ''}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-semibold transition-all relative
                  ${isActive
                    ? 'bg-violet-600/20 text-violet-300 border border-violet-500/30'
                    : 'text-slate-400 hover:text-slate-50 hover:bg-slate-800/60'}
                  ${isCollapsed ? 'justify-center' : ''}`}
              >
                <span className="text-base shrink-0">{tab.icon}</span>
                {!isCollapsed && <span className="truncate animate-fade-in">{tab.name}</span>}
                {isActive && !isCollapsed && (
                  <div className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-5 bg-violet-500 rounded-r-full" />
                )}
              </Link>
            );
          })}
        </nav>

        {/* 테마 토글 */}
        <div className={`border-t border-slate-800 p-3 ${isCollapsed ? 'flex justify-center' : ''}`}>
          <button
            onClick={toggleTheme}
            title={theme === 'light' ? '다크 모드로 전환' : '라이트 모드로 전환'}
            className={`flex items-center gap-2.5 px-3 py-2 rounded-xl text-sm font-medium
              text-slate-400 hover:text-slate-50 hover:bg-slate-800/60 transition-all w-full
              ${isCollapsed ? 'justify-center w-auto' : ''}`}
          >
            <span className="text-base shrink-0">{theme === 'light' ? '🌙' : '☀️'}</span>
            {!isCollapsed && (
              <span className="animate-fade-in">
                {theme === 'light' ? '다크 모드' : '라이트 모드'}
              </span>
            )}
          </button>
        </div>
      </aside>

      {/* ── 메인 콘텐츠 ── */}
      <main className="flex-1 min-w-0 h-full overflow-hidden bg-gradient-to-br from-slate-950 to-slate-900 flex flex-col">
        <SectionNav tabs={tabs} pathname={pathname} />
        <div className="flex-1 min-h-0 overflow-hidden">
          {children}
        </div>
      </main>

      <style jsx global>{`
        @keyframes fadeIn {
          from { opacity: 0; transform: translateX(-6px); }
          to   { opacity: 1; transform: translateX(0); }
        }
        .animate-fade-in { animation: fadeIn 0.3s ease forwards; }
        .custom-scrollbar::-webkit-scrollbar { width: 3px; }
        .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
        .custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(148,163,184,0.2); border-radius: 10px; }
        .custom-scrollbar::-webkit-scrollbar-thumb:hover { background: rgba(148,163,184,0.4); }
      `}</style>
    </div>
  );
}
