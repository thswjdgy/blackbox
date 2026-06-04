'use client';

import { useState, useEffect, useMemo } from 'react';
import { useParams, useRouter } from 'next/navigation';
import api from '@/lib/api';

interface Task {
  id: number;
  title: string;
  status: 'TODO' | 'IN_PROGRESS' | 'DONE';
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  dueDate: string | null;
  assigneeIds: number[];
}

interface Meeting {
  id: number;
  title: string;
  meetingAt: string;
  attendeeCount: number;
}

type CalEvent =
  | { kind: 'task';    data: Task }
  | { kind: 'meeting'; data: Meeting };

/* 긴급은 일요일 빨강과 겹치지 않도록 fuchsia 사용 */
const PRIORITY_COLOR: Record<string, string> = {
  URGENT: 'bg-fuchsia-500',
  HIGH:   'bg-orange-500',
  MEDIUM: 'bg-amber-400',
  LOW:    'bg-slate-500',
};
const PRIORITY_BADGE: Record<string, string> = {
  URGENT: 'bg-fuchsia-500',
  HIGH:   'bg-orange-500',
  MEDIUM: 'bg-amber-400',
  LOW:    'bg-slate-600',
};
const PRIORITY_LABEL: Record<string, string> = {
  URGENT: '긴급', HIGH: '높음', MEDIUM: '보통', LOW: '낮음',
};

const STATUS_STYLE: Record<string, string> = {
  TODO:        'border-violet-500/50 bg-violet-500/10 text-violet-300',
  IN_PROGRESS: 'border-amber-500/50  bg-amber-500/10  text-amber-300',
  DONE:        'border-slate-700      bg-slate-800/50   text-slate-500',
};
const STATUS_LABEL: Record<string, string> = {
  TODO: '할 일', IN_PROGRESS: '진행 중', DONE: '완료',
};

const DAYS = ['일', '월', '화', '수', '목', '금', '토'];

function toDateStr(iso: string) {
  return new Date(iso).toLocaleDateString('sv-SE');
}

/* ── 일정 추가 모달 ── */
function CreateEventModal({
  date, projectId, onClose, onCreated,
}: {
  date: string;
  projectId: string;
  onClose: () => void;
  onCreated: (ev: CalEvent) => void;
}) {
  const [tab,    setTab]    = useState<'task' | 'meeting'>('task');
  const [title,  setTitle]  = useState('');
  const [priority, setPriority] = useState('MEDIUM');
  const [time,   setTime]   = useState('09:00');
  const [saving, setSaving] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim()) return;
    setSaving(true);
    try {
      if (tab === 'task') {
        const res = await api.post(`/projects/${projectId}/tasks`, {
          title: title.trim(),
          priority,
          dueDate: new Date(`${date}T23:59:00`).toISOString(),
        });
        onCreated({ kind: 'task', data: res.data });
      } else {
        const res = await api.post(`/projects/${projectId}/meetings`, {
          title: title.trim(),
          meetingAt: new Date(`${date}T${time}:00`).toISOString(),
        });
        onCreated({ kind: 'meeting', data: { ...res.data, attendeeCount: 0 } });
      }
      onClose();
    } catch { alert('생성 실패'); }
    finally { setSaving(false); }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4"
      onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl w-full max-w-sm">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-800">
          <div>
            <h2 className="font-bold text-white text-sm">일정 추가</h2>
            <p className="text-xs text-slate-500 mt-0.5">
              {new Date(date + 'T00:00:00').toLocaleDateString('ko-KR', { month: 'long', day: 'numeric', weekday: 'short' })}
            </p>
          </div>
          <button onClick={onClose} className="text-slate-500 hover:text-white text-xl leading-none">×</button>
        </div>

        {/* 탭 */}
        <div className="flex gap-1 p-3 border-b border-slate-800">
          {(['task', 'meeting'] as const).map(t => (
            <button key={t} onClick={() => setTab(t)}
              className={`flex-1 py-1.5 rounded-lg text-xs font-semibold transition-all
                ${tab === t ? 'bg-violet-600 text-white' : 'text-slate-400 hover:text-white'}`}>
              {t === 'task' ? '📋 태스크' : '📝 회의'}
            </button>
          ))}
        </div>

        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          <div>
            <label className="text-xs text-slate-400 mb-1.5 block">제목 *</label>
            <input
              autoFocus
              value={title}
              onChange={e => setTitle(e.target.value)}
              placeholder={tab === 'task' ? '태스크 제목' : '회의 제목'}
              required
              className="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-violet-500 transition-colors"
            />
          </div>

          {tab === 'task' && (
            <div>
              <label className="text-xs text-slate-400 mb-1.5 block">우선순위</label>
              <div className="grid grid-cols-4 gap-1.5">
                {(['LOW', 'MEDIUM', 'HIGH', 'URGENT'] as const).map(p => (
                  <button key={p} type="button" onClick={() => setPriority(p)}
                    className={`py-1.5 rounded-lg text-xs font-semibold transition-all border
                      ${priority === p
                        ? `${PRIORITY_BADGE[p]} text-white border-transparent`
                        : 'bg-slate-800 border-slate-700 text-slate-400 hover:text-white'}`}>
                    {PRIORITY_LABEL[p]}
                  </button>
                ))}
              </div>
            </div>
          )}

          {tab === 'meeting' && (
            <div>
              <label className="text-xs text-slate-400 mb-1.5 block">시간</label>
              <input
                type="time"
                value={time}
                onChange={e => setTime(e.target.value)}
                className="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2.5 text-sm text-white focus:outline-none focus:border-violet-500 transition-colors"
              />
            </div>
          )}

          <div className="flex gap-2 pt-1">
            <button type="button" onClick={onClose}
              className="flex-1 py-2 rounded-xl text-sm bg-slate-800 text-slate-400 hover:text-white transition-colors">
              취소
            </button>
            <button type="submit" disabled={saving || !title.trim()}
              className="flex-1 py-2 rounded-xl text-sm font-bold bg-violet-600 hover:bg-violet-500 text-white disabled:opacity-50 transition-all">
              {saving ? '생성 중...' : '추가'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

/* ── 메인 ── */
export default function CalendarPage() {
  const { id: projectId } = useParams() as { id: string };
  const router = useRouter();

  const today = new Date();
  const [year,  setYear]  = useState(today.getFullYear());
  const [month, setMonth] = useState(today.getMonth());
  const [selected, setSelected] = useState(toDateStr(today.toISOString()));
  const [showModal, setShowModal] = useState(false);

  const [tasks,    setTasks]    = useState<Task[]>([]);
  const [meetings, setMeetings] = useState<Meeting[]>([]);
  const [loading,  setLoading]  = useState(true);

  useEffect(() => {
    Promise.allSettled([
      api.get(`/projects/${projectId}/tasks`),
      api.get(`/projects/${projectId}/meetings`),
    ]).then(([tRes, mRes]) => {
      if (tRes.status === 'fulfilled') setTasks(tRes.value.data ?? []);
      if (mRes.status === 'fulfilled') setMeetings(mRes.value.data ?? []);
    }).finally(() => setLoading(false));
  }, [projectId]);

  const eventMap = useMemo(() => {
    const map = new Map<string, CalEvent[]>();
    const add = (date: string, ev: CalEvent) => {
      if (!map.has(date)) map.set(date, []);
      map.get(date)!.push(ev);
    };
    tasks.forEach(t => { if (t.dueDate) add(toDateStr(t.dueDate), { kind: 'task', data: t }); });
    meetings.forEach(m => add(toDateStr(m.meetingAt), { kind: 'meeting', data: m }));
    return map;
  }, [tasks, meetings]);

  const firstDay    = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const cells: (number | null)[] = [
    ...Array(firstDay).fill(null),
    ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
  ];
  while (cells.length % 7 !== 0) cells.push(null);

  const todayStr = toDateStr(today.toISOString());
  const selectedEvents = eventMap.get(selected) ?? [];

  function dateStr(day: number) {
    return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
  }
  function prevMonth() {
    if (month === 0) { setYear(y => y - 1); setMonth(11); }
    else setMonth(m => m - 1);
  }
  function nextMonth() {
    if (month === 11) { setYear(y => y + 1); setMonth(0); }
    else setMonth(m => m + 1);
  }

  function handleCreated(ev: CalEvent) {
    if (ev.kind === 'task') setTasks(p => [...p, ev.data as Task]);
    else setMeetings(p => [...p, ev.data as Meeting]);
  }

  if (loading) return <div className="p-8 text-slate-400 text-sm">캘린더 로드 중...</div>;

  return (
    <div className="flex h-full overflow-hidden">
      {/* ── 캘린더 그리드 ── */}
      <div className="flex-1 flex flex-col overflow-hidden p-5">
        <div className="flex items-center justify-between mb-4 shrink-0">
          <h2 className="text-xl font-bold text-white">{year}년 {month + 1}월</h2>
          <div className="flex gap-2">
            <button onClick={prevMonth}
              className="px-3 py-1.5 rounded-lg bg-slate-800 border border-slate-700 text-slate-300 hover:text-white text-sm transition-all">←</button>
            <button onClick={() => { setYear(today.getFullYear()); setMonth(today.getMonth()); setSelected(todayStr); }}
              className="px-3 py-1.5 rounded-lg bg-slate-800 border border-slate-700 text-slate-300 hover:text-white text-xs font-semibold transition-all">오늘</button>
            <button onClick={nextMonth}
              className="px-3 py-1.5 rounded-lg bg-slate-800 border border-slate-700 text-slate-300 hover:text-white text-sm transition-all">→</button>
          </div>
        </div>

        <div className="grid grid-cols-7 mb-1 shrink-0">
          {DAYS.map((d, i) => (
            <div key={d} className={`text-center text-xs font-semibold py-2
              ${i === 0 ? 'text-red-400' : i === 6 ? 'text-blue-400' : 'text-slate-500'}`}>{d}</div>
          ))}
        </div>

        <div className="grid grid-cols-7 flex-1 gap-px bg-slate-800/40 rounded-xl overflow-hidden border border-slate-800">
          {cells.map((day, i) => {
            if (!day) return <div key={i} className="bg-slate-950/40" />;
            const ds = dateStr(day);
            const evs = eventMap.get(ds) ?? [];
            const isToday    = ds === todayStr;
            const isSelected = ds === selected;
            const isSun = i % 7 === 0;
            const isSat = i % 7 === 6;
            return (
              <button key={i} onClick={() => setSelected(ds)}
                className={`relative flex flex-col p-1.5 text-left transition-all min-h-[70px]
                  ${isSelected ? 'bg-violet-600/20' : 'bg-slate-950/60 hover:bg-slate-800/60'}`}>
                <span className={`text-xs font-bold w-6 h-6 flex items-center justify-center rounded-full mb-1 shrink-0
                  ${isToday ? 'bg-violet-600 text-white' :
                    isSun ? 'text-red-400' : isSat ? 'text-blue-400' : 'text-slate-300'}`}>
                  {day}
                </span>
                <div className="flex flex-wrap gap-0.5">
                  {evs.slice(0, 4).map((ev, j) => (
                    <span key={j} className={`block w-1.5 h-1.5 rounded-full
                      ${ev.kind === 'meeting' ? 'bg-sky-400' :
                        ev.data.status === 'DONE' ? 'bg-slate-600' :
                        PRIORITY_COLOR[ev.data.priority] ?? 'bg-violet-500'}`} />
                  ))}
                  {evs.length > 4 && <span className="text-[9px] text-slate-500 leading-none">+{evs.length - 4}</span>}
                </div>
              </button>
            );
          })}
        </div>

        {/* 범례 */}
        <div className="flex gap-4 mt-3 shrink-0 text-[11px] text-slate-500 flex-wrap">
          <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-sky-400" />회의</span>
          <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-fuchsia-500" />긴급</span>
          <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-orange-500" />높음</span>
          <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-amber-400" />보통</span>
          <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-slate-500" />낮음</span>
          <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-slate-600" />완료</span>
        </div>
      </div>

      {/* ── 선택된 날 패널 ── */}
      <div className="w-72 shrink-0 border-l border-slate-800 flex flex-col overflow-hidden">
        <div className="px-4 py-3 border-b border-slate-800 shrink-0 flex items-center justify-between">
          <div>
            <p className="text-sm font-bold text-white">
              {new Date(selected + 'T00:00:00').toLocaleDateString('ko-KR', { month: 'long', day: 'numeric', weekday: 'short' })}
            </p>
            <p className="text-xs text-slate-500 mt-0.5">{selectedEvents.length}개 일정</p>
          </div>
          <button onClick={() => setShowModal(true)}
            className="w-8 h-8 flex items-center justify-center rounded-xl bg-violet-600 hover:bg-violet-500 text-white text-lg font-bold transition-all"
            title="일정 추가">
            +
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-3 space-y-2 custom-scrollbar">
          {selectedEvents.length === 0 ? (
            <div className="text-center mt-10 space-y-2">
              <p className="text-2xl">📭</p>
              <p className="text-xs text-slate-600">이 날의 일정이 없습니다.</p>
              <button onClick={() => setShowModal(true)}
                className="text-xs text-violet-400 hover:text-violet-300 transition-colors">
                + 일정 추가
              </button>
            </div>
          ) : (
            selectedEvents.map((ev, i) => ev.kind === 'meeting' ? (
              <button key={i}
                onClick={() => router.push(`/projects/${projectId}/meetings/${ev.data.id}`)}
                className="w-full text-left p-3 rounded-xl border border-sky-500/30 bg-sky-500/10 hover:bg-sky-500/20 transition-all">
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-sm">📝</span>
                  <span className="text-xs font-bold text-sky-300">회의</span>
                  <span className="text-xs text-slate-500 ml-auto">
                    {new Date(ev.data.meetingAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}
                  </span>
                </div>
                <p className="text-sm font-semibold text-white truncate">{ev.data.title}</p>
                <p className="text-xs text-slate-400 mt-0.5">참석자 {ev.data.attendeeCount}명</p>
              </button>
            ) : (
              <button key={i}
                onClick={() => router.push(`/projects/${projectId}/board`)}
                className={`w-full text-left p-3 rounded-xl border transition-all ${STATUS_STYLE[ev.data.status]}`}>
                <div className="flex items-center justify-between mb-1">
                  <span className="text-xs font-bold">{STATUS_LABEL[ev.data.status]}</span>
                  <span className={`text-[10px] px-1.5 py-0.5 rounded-full text-white ${PRIORITY_BADGE[ev.data.priority]}`}>
                    {PRIORITY_LABEL[ev.data.priority]}
                  </span>
                </div>
                <p className={`text-sm font-semibold truncate ${ev.data.status === 'DONE' ? 'line-through opacity-50' : 'text-white'}`}>
                  {ev.data.title}
                </p>
                {ev.data.assigneeIds?.length > 0 && (
                  <p className="text-xs text-slate-500 mt-0.5">담당자 {ev.data.assigneeIds.length}명</p>
                )}
              </button>
            ))
          )}
        </div>
      </div>

      {showModal && (
        <CreateEventModal
          date={selected}
          projectId={projectId}
          onClose={() => setShowModal(false)}
          onCreated={handleCreated}
        />
      )}

      <style dangerouslySetInnerHTML={{__html: `
        .custom-scrollbar::-webkit-scrollbar { width: 4px; }
        .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
        .custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(148,163,184,0.2); border-radius: 8px; }
      `}} />
    </div>
  );
}
