'use client';

import { useState } from 'react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onCheckin: (code: string) => Promise<void>;
}

export function CheckinModal({ isOpen, onClose, onCheckin }: Props) {
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen) return null;

  const handleSubmit = async () => {
    if (code.trim().length !== 6) return;
    setLoading(true);
    setError('');
    try {
      await onCheckin(code.toUpperCase());
      setCode('');
      onClose();
    } catch {
      setError('체크인 코드가 올바르지 않습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden">
        <div className="p-6 border-b border-white/5 text-center">
          <div className="w-12 h-12 bg-emerald-500/10 rounded-full flex items-center justify-center mx-auto mb-3">
            <span className="text-2xl">✅</span>
          </div>
          <h2 className="text-xl font-bold text-white">회의 체크인</h2>
          <p className="text-slate-400 text-sm mt-1">진행자에게 받은 6자리 코드를 입력하세요.</p>
        </div>

        <div className="p-6 space-y-4">
          <input
            className="w-full bg-slate-800/50 border border-slate-700 text-slate-100 px-4 py-4 rounded-xl text-center text-2xl font-mono tracking-[0.5em] uppercase focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500 outline-none transition-all placeholder-slate-700"
            placeholder="A B C 1 2 3"
            value={code}
            maxLength={6}
            onChange={e => { setCode(e.target.value.toUpperCase()); setError(''); }}
            onKeyDown={e => e.key === 'Enter' && handleSubmit()}
            autoFocus
          />
          {error && (
            <p className="text-rose-400 text-sm text-center">{error}</p>
          )}
        </div>

        <div className="p-6 bg-slate-950/50 flex gap-3 border-t border-white/5">
          <button
            onClick={() => { setCode(''); setError(''); onClose(); }}
            className="flex-1 py-3 bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold rounded-xl transition-all"
          >
            취소
          </button>
          <button
            onClick={handleSubmit}
            disabled={code.trim().length !== 6 || loading}
            className="flex-1 py-3 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-bold rounded-xl transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? '처리 중...' : '체크인'}
          </button>
        </div>
      </div>
    </div>
  );
}
