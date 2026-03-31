'use client';

import { useState, useEffect } from 'react';
import { TaskType } from './TaskCard';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSave: (task: Partial<TaskType>) => void;
  initialData?: TaskType;
}

export function TaskModal({ isOpen, onClose, onSave, initialData }: Props) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<'LOW' | 'MEDIUM' | 'HIGH'>('MEDIUM');
  
  useEffect(() => {
    if (initialData) {
      setTitle(initialData.title);
      setDescription(initialData.description || '');
      setPriority(initialData.priority);
    } else {
      setTitle('');
      setDescription('');
      setPriority('MEDIUM');
    }
  }, [initialData, isOpen]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-in fade-in duration-200">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl w-full max-w-md overflow-hidden animate-in zoom-in-95 duration-200">
        <div className="p-6 border-b border-white/5">
          <h2 className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-violet-400 to-indigo-400">
            {initialData ? '태스크 수정' : '새 태스크 생성'}
          </h2>
        </div>
        
        <div className="p-6 space-y-4">
          <div>
            <label className="block text-xs uppercase font-bold text-slate-400 mb-2 tracking-wider">태스크 제목</label>
            <input 
              className="w-full bg-slate-800/50 border border-slate-700 text-slate-100 px-4 py-3 rounded-xl focus:ring-2 focus:ring-violet-500/50 focus:border-violet-500 outline-none transition-all placeholder-slate-600"
              placeholder="UI 디자인 시안 작업"
              value={title}
              onChange={e => setTitle(e.target.value)}
              autoFocus
            />
          </div>

          <div>
            <label className="block text-xs uppercase font-bold text-slate-400 mb-2 tracking-wider">자세한 설명</label>
            <textarea 
              className="w-full bg-slate-800/50 border border-slate-700 text-slate-100 px-4 py-3 rounded-xl focus:ring-2 focus:ring-violet-500/50 focus:border-violet-500 outline-none transition-all placeholder-slate-600 h-28 resize-none"
              placeholder="피그마를 이용해 웹앱 랜딩페이지를 기획합니다."
              value={description}
              onChange={e => setDescription(e.target.value)}
            />
          </div>

          <div>
            <label className="block text-xs uppercase font-bold text-slate-400 mb-2 tracking-wider">우선순위</label>
            <div className="flex gap-2">
              {['LOW', 'MEDIUM', 'HIGH'].map(p => (
                <button
                  key={p}
                  onClick={() => setPriority(p as any)}
                  className={`flex-1 py-2 rounded-lg text-xs font-bold transition-all border
                    ${priority === p 
                      ? (p === 'HIGH' ? 'bg-rose-500/20 text-rose-300 border-rose-500' : p === 'LOW' ? 'bg-slate-600 text-white border-slate-500' : 'bg-blue-500/20 text-blue-300 border-blue-500')
                      : 'bg-slate-800 border-slate-700 text-slate-500 hover:bg-slate-700 hover:text-slate-300'
                    }`}
                >
                  {p}
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="p-6 bg-slate-950/50 flex gap-3 border-t border-white/5">
          <button 
            onClick={onClose}
            className="flex-1 py-3 bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold rounded-xl transition-all"
          >
            취소
          </button>
          <button 
            onClick={() => onSave({ title, description, priority })}
            disabled={!title.trim()}
            className="flex-1 py-3 bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-500 hover:to-indigo-500 text-white font-bold rounded-xl transition-all disabled:opacity-50 disabled:cursor-not-allowed shadow-[0_0_20px_rgba(124,58,237,0.3)]"
          >
            {initialData ? '수정 완료' : '생성하기'}
          </button>
        </div>
      </div>
    </div>
  );
}
