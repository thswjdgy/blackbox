'use client';

import { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import api from '@/lib/api';
import { MeetingModal, MeetingFormData } from '@/components/meeting/MeetingModal';

interface MeetingSummary {
  id: number;
  projectId: number;
  title: string;
  meetingAt: string;
  attendeeCount: number;
  createdAt: string;
}

function formatDate(iso: string) {
  const d = new Date(iso);
  return d.toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

export default function MeetingsPage() {
  const params = useParams();
  const router = useRouter();
  const projectId = params.id as string;

  const [meetings, setMeetings] = useState<MeetingSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);

  useEffect(() => {
    fetchMeetings();
  }, [projectId]);

  const fetchMeetings = async () => {
    try {
      const res = await api.get(`/projects/${projectId}/meetings`);
      setMeetings(res.data);
    } catch (e) {
      console.error('Failed to fetch meetings', e);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async (data: MeetingFormData) => {
    try {
      await api.post(`/projects/${projectId}/meetings`, {
        title: data.title,
        purpose: data.purpose,
        meetingAt: new Date(data.meetingAt).toISOString(),
      });
      setIsModalOpen(false);
      fetchMeetings();
    } catch (e) {
      console.error(e);
      alert('회의 생성 실패');
    }
  };

  if (loading) return <div className="p-8 text-slate-400">불러오는 중...</div>;

  return (
    <div className="flex flex-col h-full overflow-hidden p-6 absolute inset-0">
      {/* Header */}
      <div className="flex shrink-0 items-center justify-between backdrop-blur-md bg-slate-900/40 p-5 rounded-2xl mb-6 border border-slate-800 shadow-xl">
        <div>
          <h2 className="text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-emerald-400 to-teal-400">
            회의록
          </h2>
          <p className="text-sm text-slate-400 mt-1">회의를 생성하고 팀원의 출석을 체크인하세요.</p>
        </div>
        <button
          onClick={() => setIsModalOpen(true)}
          className="bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-bold py-2.5 px-6 rounded-xl shadow-lg transition-all hover:-translate-y-0.5"
        >
          + 새 회의 생성
        </button>
      </div>

      {/* Meeting List */}
      <div className="flex-1 overflow-y-auto space-y-3 custom-scrollbar pb-4">
        {meetings.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-64 gap-4">
            <div className="w-16 h-16 bg-slate-800 rounded-full flex items-center justify-center text-3xl">📝</div>
            <p className="text-slate-500 font-medium">아직 회의가 없습니다</p>
            <button
              onClick={() => setIsModalOpen(true)}
              className="text-emerald-400 hover:text-emerald-300 text-sm font-semibold transition-colors"
            >
              첫 번째 회의를 생성해보세요 →
            </button>
          </div>
        ) : (
          meetings.map(meeting => (
            <button
              key={meeting.id}
              onClick={() => router.push(`/projects/${projectId}/meetings/${meeting.id}`)}
              className="w-full text-left p-5 bg-slate-900/60 border border-slate-800 rounded-2xl hover:border-emerald-500/40 hover:bg-slate-800/60 transition-all group"
            >
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1 min-w-0">
                  <h3 className="font-semibold text-slate-100 group-hover:text-emerald-300 transition-colors truncate">
                    {meeting.title}
                  </h3>
                  <p className="text-sm text-slate-500 mt-1">{formatDate(meeting.meetingAt)}</p>
                </div>
                <div className="flex items-center gap-3 shrink-0">
                  <div className="flex items-center gap-1.5 bg-slate-800 px-3 py-1.5 rounded-lg">
                    <span className="text-xs text-slate-400">참석자</span>
                    <span className="text-sm font-bold text-emerald-400">{meeting.attendeeCount}</span>
                  </div>
                  <span className="text-slate-600 group-hover:text-slate-400 transition-colors">→</span>
                </div>
              </div>
            </button>
          ))
        )}
      </div>

      <style dangerouslySetInnerHTML={{__html: `
        .custom-scrollbar::-webkit-scrollbar { width: 6px; }
        .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
        .custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(51,65,85,0.5); border-radius: 8px; }
      `}} />

      {isModalOpen && (
        <MeetingModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onSave={handleCreate}
        />
      )}
    </div>
  );
}
