'use client';

import { useEffect } from 'react';

export default function GitHubOAuthSuccessPage() {
  useEffect(() => {
    const timer = setTimeout(() => window.close(), 500);
    return () => clearTimeout(timer);
  }, []);

  return (
    <div className="flex items-center justify-center h-screen bg-slate-950 text-white">
      <div className="text-center space-y-3">
        <div className="text-4xl">✅</div>
        <p className="text-lg font-semibold">GitHub 연동 완료</p>
        <p className="text-sm text-slate-400">창이 자동으로 닫힙니다…</p>
      </div>
    </div>
  );
}
