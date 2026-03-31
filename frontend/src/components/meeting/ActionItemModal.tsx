'use client';

import { useState } from 'react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSave: (title: string, description: string) => Promise<void>;
}

export function ActionItemModal({ isOpen, onClose, onSave }: Props) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(false);

  if (!isOpen) return null;

  const handleSave = async () => {
    if (!title.trim()) return;
    setLoading(true);
    try {
      await onSave(title, description);
      setTitle('');
      setDescription('');
      onClose();
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
        <div className="p-6 border-b border-white/5">
          <h2 className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-violet-400 to-indigo-400">
            액션 아이템 → 태스크 생성
          </h2>
          <p className="text-slate-400 text-sm mt-1">회의 결정사항을 칸반 태스크로 바로 생성합니다.</p>
        </div>

        <div className="p-6 space-y-4">
          <div>
            <label className="block text-xs uppercase font-bold text-slate-400 mb-2 tracking-wider">태스크 제목 *</label>
            <input
              className="w-full bg-slate-800/50 border border-slate-700 text-slate-100 px-4 py-3 rounded-xl focus:ring-2 focus:ring-violet-500/50 focus:border-violet-500 outline-none transition-all placeholder-slate-600"
              placeholder="API 명세 문서 작성"
              value={title}
              onChange={e => setTitle(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleSave()}
              autoFocus
            />
          </div>

          <div>
            <label className="block text-xs uppercase font-bold text-slate-400 mb-2 tracking-wider">설명</label>
            <textarea
              className="w-full bg-slate-800/50 border border-slate-700 text-slate-100 px-4 py-3 rounded-xl focus:ring-2 focus:ring-violet-500/50 focus:border-violet-500 outline-none transition-all placeholder-slate-600 h-24 resize-none"
              placeholder="회의에서 결정된 내용을 바탕으로 작성..."
              value={description}
              onChange={e => setDescription(e.target.value)}
            />
          </div>
        </div>

        <div className="p-6 bg-slate-950/50 flex gap-3 border-t border-white/5">
          <button
            onClick={() => { setTitle(''); setDescription(''); onClose(); }}
            className="flex-1 py-3 bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold rounded-xl transition-all"
          >
            취소
          </button>
          <button
            onClick={handleSave}
            disabled={!title.trim() || loading}
            className="flex-1 py-3 bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-500 hover:to-indigo-500 text-white font-bold rounded-xl transition-all disabled:opacity-50 disabled:cursor-not-allowed shadow-[0_0_20px_rgba(124,58,237,0.3)]"
          >
            {loading ? '생성 중...' : '태스크 생성'}
          </button>
        </div>
      </div>
    </div>
  );
}
