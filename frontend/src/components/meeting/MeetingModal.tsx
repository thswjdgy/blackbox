'use client';

import { useState, useEffect } from 'react';

export interface MeetingFormData {
  title: string;
  purpose: string;
  meetingAt: string;
}

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSave: (data: MeetingFormData) => void;
  initialData?: MeetingFormData;
}

export function MeetingModal({ isOpen, onClose, onSave, initialData }: Props) {
  const [title, setTitle] = useState('');
  const [purpose, setPurpose] = useState('');
  const [meetingAt, setMeetingAt] = useState('');

  useEffect(() => {
    if (initialData) {
      setTitle(initialData.title);
      setPurpose(initialData.purpose || '');
      setMeetingAt(initialData.meetingAt || '');
    } else {
      setTitle('');
      setPurpose('');
      // 기본값: 지금으로부터 1시간 뒤
      const d = new Date();
      d.setHours(d.getHours() + 1, 0, 0, 0);
      setMeetingAt(d.toISOString().slice(0, 16));
    }
  }, [initialData, isOpen]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
        <div className="p-6 border-b border-white/5">
          <h2 className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-emerald-400 to-teal-400">
            {initialData ? '회의 수정' : '새 회의 생성'}
          </h2>
        </div>

        <div className="p-6 space-y-4">
          <div>
            <label className="block text-xs uppercase font-bold text-slate-400 mb-2 tracking-wider">회의 제목 *</label>
            <input
              className="w-full bg-slate-800/50 border border-slate-700 text-slate-100 px-4 py-3 rounded-xl focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500 outline-none transition-all placeholder-slate-600"
              placeholder="3차 스프린트 기획 회의"
              value={title}
              onChange={e => setTitle(e.target.value)}
              autoFocus
            />
          </div>

          <div>
            <label className="block text-xs uppercase font-bold text-slate-400 mb-2 tracking-wider">회의 목적</label>
            <textarea
              className="w-full bg-slate-800/50 border border-slate-700 text-slate-100 px-4 py-3 rounded-xl focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500 outline-none transition-all placeholder-slate-600 h-20 resize-none"
              placeholder="이번 회의에서 논의할 주제를 간략히 적어주세요."
              value={purpose}
              onChange={e => setPurpose(e.target.value)}
            />
          </div>

          <div>
            <label className="block text-xs uppercase font-bold text-slate-400 mb-2 tracking-wider">회의 일시 *</label>
            <input
              type="datetime-local"
              className="w-full bg-slate-800/50 border border-slate-700 text-slate-100 px-4 py-3 rounded-xl focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500 outline-none transition-all [color-scheme:dark]"
              value={meetingAt}
              onChange={e => setMeetingAt(e.target.value)}
            />
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
            onClick={() => onSave({ title, purpose, meetingAt })}
            disabled={!title.trim() || !meetingAt}
            className="flex-1 py-3 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-bold rounded-xl transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {initialData ? '수정 완료' : '생성하기'}
          </button>
        </div>
      </div>
    </div>
  );
}
