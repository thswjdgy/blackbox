'use client';

import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'next/navigation';
import api from '@/lib/api';

/* ── 타입 ───────────────────────────────────────── */
interface ActivityLog {
  id: number;
  userId: number;
  userName: string;
  eventType: string;
  source: 'PLATFORM' | 'GITHUB' | 'GOOGLE_DRIVE' | 'NOTION';
  payload: Record<string, unknown>;
  createdAt: string;
}

interface Member {
  userId: number;
  userName: string;
}

/* ── 소스별 스타일 ───────────────────────────────── */
const SOURCE_STYLE = {
  PLATFORM: {
    dot:    'bg-violet-500',
    badge:  'bg-violet-500/15 text-violet-300 border-violet-500/30',
    label:  '플랫폼',
    icon:   '🖥️',
    line:   'border-violet-500/20',
  },
  GITHUB: {
    dot:    'bg-slate-300',
    badge:  'bg-slate-700/60 text-slate-200 border-slate-600/40',
    label:  'GitHub',
    icon:   '',
    line:   'border-slate-600/20',
  },
  GOOGLE_DRIVE: {
    dot:    'bg-blue-400',
    badge:  'bg-blue-500/15 text-blue-300 border-blue-500/30',
    label:  'Drive',
    icon:   '📄',
    line:   'border-blue-500/20',
  },
  NOTION: {
    dot:    'bg-slate-100',
    badge:  'bg-slate-100/10 text-slate-200 border-slate-500/30',
    label:  'Notion',
    icon:   'N',
    line:   'border-slate-500/20',
  },
} as const;

/* ── 이벤트 타입 → 한글 + 아이콘 ───────────────── */
const EVENT_META: Record<string, { label: string; icon: string; color: string }> = {
  TASK_CREATED:        { label: '태스크 생성',   icon: '✅', color: 'text-emerald-400' },
  TASK_UPDATED:        { label: '태스크 수정',   icon: '✏️', color: 'text-blue-400'    },
  TASK_STATUS_CHANGED: { label: '상태 변경',     icon: '🔄', color: 'text-yellow-400'  },
  TASK_DELETED:        { label: '태스크 삭제',   icon: '🗑️', color: 'text-red-400'     },
  MEETING_CREATED:     { label: '회의 생성',     icon: '📅', color: 'text-blue-400'    },
  MEETING_CHECKIN:     { label: '회의 체크인',   icon: '👋', color: 'text-emerald-400' },
  MEMBER_JOINED:       { label: '멤버 참여',     icon: '👤', color: 'text-violet-400'  },
  FILE_UPLOADED:       { label: '파일 업로드',   icon: '📁', color: 'text-orange-400'  },
  FILE_TAMPERED:       { label: '파일 변조 감지', icon: '🚨', color: 'text-red-400'    },
  GITHUB_PUSH:         { label: '커밋 Push',     icon: '',   color: 'text-slate-300'   },
  GITHUB_PR_OPENED:    { label: 'PR 오픈',       icon: '',   color: 'text-emerald-400' },
  GITHUB_PR_MERGED:    { label: 'PR 머지',       icon: '',   color: 'text-violet-400'  },
  GITHUB_ISSUE_OPENED: { label: '이슈 오픈',     icon: '🔴', color: 'text-red-400'    },
  GITHUB_ISSUE_CLOSED: { label: '이슈 종료',     icon: '🟣', color: 'text-violet-400' },
  NOTION_PAGE_CREATED: { label: '페이지 생성',   icon: '📝', color: 'text-blue-300'   },
  NOTION_PAGE_EDITED:  { label: '페이지 수정',   icon: '✏️', color: 'text-blue-400'   },
  NOTION_COMMENT_ADDED:{ label: '댓글 추가',     icon: '💬', color: 'text-sky-400'    },
  // Google
  GDRIVE_FILE_UPLOADED:       { label: '파일 업로드',    icon: '📤', color: 'text-blue-400'  },
  GDRIVE_FILE_MODIFIED:       { label: '파일 수정',      icon: '📝', color: 'text-blue-300'  },
  GSHEET_EDITED:              { label: '시트 수정',      icon: '📊', color: 'text-green-400' },
  GFORM_RESPONSE_SUBMITTED:   { label: '폼 응답 제출',   icon: '📋', color: 'text-indigo-400'},
};

/* ── 날짜 그룹 헤더 ─────────────────────────────── */
function formatGroupDate(dateStr: string): string {
  const d = new Date(dateStr);
  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(today.getDate() - 1);

  if (d.toDateString() === today.toDateString()) return '오늘';
  if (d.toDateString() === yesterday.toDateString()) return '어제';
  return d.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric', weekday: 'short' });
}

function formatTime(dateStr: string): string {
  return new Date(dateStr).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
}

function getDateKey(dateStr: string): string {
  return new Date(dateStr).toDateString();
}

/* ── payload 요약 텍스트 ────────────────────────── */
function payloadSummary(eventType: string, payload: Record<string, unknown>): string {
  switch (eventType) {
    case 'TASK_CREATED':
    case 'TASK_UPDATED':
    case 'TASK_STATUS_CHANGED':
    case 'TASK_DELETED':
      return payload.title ? `"${payload.title}"` : '';
    case 'GITHUB_PUSH':
      return payload.message ? `${String(payload.message).slice(0, 60)}${String(payload.message).length > 60 ? '…' : ''}` : '';
    case 'GITHUB_PR_OPENED':
    case 'GITHUB_PR_MERGED':
      return payload.title ? `#${payload.pr} ${payload.title}` : '';
    case 'GITHUB_ISSUE_OPENED':
    case 'GITHUB_ISSUE_CLOSED':
      return payload.title ? `#${payload.number} ${payload.title}` : '';
    case 'FILE_UPLOADED':
      return payload.fileName ? `${payload.fileName}` : '';
    case 'MEETING_CHECKIN':
      return payload.meetingTitle ? `${payload.meetingTitle}` : '';
    case 'NOTION_PAGE_CREATED':
    case 'NOTION_PAGE_EDITED':
    case 'NOTION_COMMENT_ADDED':
      return payload.title ? `${payload.title}` : '';
    case 'GDRIVE_FILE_UPLOADED':
    case 'GDRIVE_FILE_MODIFIED':
      return payload.name ? `${payload.name}` : '';
    case 'GSHEET_EDITED':
      return payload.editor ? `${payload.editor}` : '';
    case 'GFORM_RESPONSE_SUBMITTED':
      return payload.submittedAt ? `${new Date(String(payload.submittedAt)).toLocaleString('ko-KR')}` : '';
    default:
      return '';
  }
}

/* ── GitHub 커밋 SHA 뱃지 ───────────────────────── */
function ShaBadge({ sha }: { sha: string }) {
  if (!sha || sha.startsWith('pr-')) return null;
  return (
    <span className="font-mono text-[10px] bg-slate-800 text-slate-400 px-1.5 py-0.5 rounded border border-slate-700">
      {sha.slice(0, 7)}
    </span>
  );
}

/* ── 메인 컴포넌트 ──────────────────────────────── */
export default function TimelinePage() {
  const params  = useParams();
  const projectId = params.id as string;

  const [logs,    setLogs]    = useState<ActivityLog[]>([]);
  const [members, setMembers] = useState<Member[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(true);

  const [sourceFilter, setSourceFilter] = useState<'ALL' | 'PLATFORM' | 'GITHUB' | 'NOTION' | 'GOOGLE_DRIVE'>('ALL');
  const [userFilter,   setUserFilter]   = useState<string>('');
  const [limit] = useState(50);

  const fetchLogs = useCallback(async (reset = false) => {
    if (reset) setLoading(true); else setLoadingMore(true);

    const params = new URLSearchParams({ source: sourceFilter, limit: String(limit) });
    if (userFilter) params.set('userId', userFilter);

    try {
      const res = await api.get(`/projects/${projectId}/activities?${params}`);
      const data: ActivityLog[] = res.data;
      setLogs(data);
      setHasMore(data.length === limit);
    } catch {
      // ignore
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  }, [projectId, sourceFilter, userFilter, limit]);

  useEffect(() => {
    api.get(`/projects/${projectId}/members`).then(res => {
      setMembers(
        res.data.map((m: Record<string, unknown>) => ({
          userId:   m.userId   ?? (m.user as Record<string, unknown>)?.id,
          userName: m.userName ?? (m.user as Record<string, unknown>)?.name ?? m.name,
        }))
      );
    }).catch(() => {});
  }, [projectId]);

  useEffect(() => { fetchLogs(true); }, [fetchLogs]);

  /* 날짜별 그룹핑 */
  const grouped: { key: string; label: string; items: ActivityLog[] }[] = [];
  for (const log of logs) {
    const key = getDateKey(log.createdAt);
    const last = grouped[grouped.length - 1];
    if (last && last.key === key) {
      last.items.push(log);
    } else {
      grouped.push({ key, label: formatGroupDate(log.createdAt), items: [log] });
    }
  }

  const sourceOptions: Array<{ value: 'ALL' | 'PLATFORM' | 'GITHUB' | 'NOTION' | 'GOOGLE_DRIVE'; label: string }> = [
    { value: 'ALL',          label: '전체' },
    { value: 'PLATFORM',     label: '플랫폼' },
    { value: 'GITHUB',       label: 'GitHub' },
    { value: 'NOTION',       label: 'Notion' },
    { value: 'GOOGLE_DRIVE', label: 'Google' },
  ];

  return (
    <div className="flex flex-col h-full overflow-hidden">
      {/* ── 헤더 + 필터 ── */}
      <div className="shrink-0 px-6 py-4 border-b border-slate-800/60 bg-slate-900/30 space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-white">활동 타임라인</h2>
          <button
            onClick={() => fetchLogs(true)}
            className="text-xs text-slate-400 hover:text-white transition-colors px-3 py-1.5 rounded-lg hover:bg-slate-800"
          >
            새로고침
          </button>
        </div>

        <div className="flex flex-wrap gap-2">
          {/* 소스 필터 */}
          <div className="flex gap-1 bg-slate-800/60 rounded-lg p-1">
            {sourceOptions.map(opt => (
              <button
                key={opt.value}
                onClick={() => setSourceFilter(opt.value)}
                className={`px-3 py-1 rounded-md text-xs font-semibold transition-all ${
                  sourceFilter === opt.value
                    ? 'bg-violet-600 text-white shadow'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                {opt.label}
              </button>
            ))}
          </div>

          {/* 멤버 필터 */}
          <select
            value={userFilter}
            onChange={e => setUserFilter(e.target.value)}
            className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-1 text-xs text-slate-300 focus:outline-none focus:border-violet-500"
          >
            <option value="">전체 멤버</option>
            {members.map(m => (
              <option key={m.userId} value={m.userId}>{m.userName}</option>
            ))}
          </select>

          {/* 현재 로그 수 */}
          <span className="self-center text-xs text-slate-500 ml-auto">
            {logs.length}개 항목
          </span>
        </div>
      </div>

      {/* ── 타임라인 본문 ── */}
      <div className="flex-1 overflow-y-auto px-6 py-4">
        {loading ? (
          <div className="flex items-center justify-center h-40 text-slate-500 text-sm">불러오는 중…</div>
        ) : logs.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-40 text-slate-500 text-sm gap-2">
            <span className="text-3xl">📭</span>
            <p>활동 기록이 없습니다.</p>
            {sourceFilter !== 'ALL' && (
              <p className="text-xs">필터를 변경해 보세요.</p>
            )}
          </div>
        ) : (
          <div className="space-y-6 max-w-2xl mx-auto pb-8">
            {grouped.map(group => (
              <div key={group.key}>
                {/* 날짜 구분선 */}
                <div className="flex items-center gap-3 mb-3">
                  <div className="h-px flex-1 bg-slate-800" />
                  <span className="text-xs font-semibold text-slate-500 whitespace-nowrap">{group.label}</span>
                  <div className="h-px flex-1 bg-slate-800" />
                </div>

                {/* 이벤트 목록 */}
                <div className="relative">
                  {/* 세로 라인 */}
                  <div className="absolute left-[19px] top-0 bottom-0 w-px bg-slate-800" />

                  <div className="space-y-1">
                    {group.items.map(log => {
                      const src     = SOURCE_STYLE[log.source] ?? SOURCE_STYLE.PLATFORM;
                      const meta    = EVENT_META[log.eventType] ?? { label: log.eventType, icon: '•', color: 'text-slate-400' };
                      const summary = payloadSummary(log.eventType, log.payload);
                      const sha     = log.payload?.sha as string | undefined;
                      const externalUrl =
                        log.source === 'NOTION'       ? (log.payload?.url as string | undefined) :
                        log.source === 'GOOGLE_DRIVE' ? (log.payload?.url as string | undefined) :
                        undefined;

                      const cardContent = (
                        <>
                          <div className="flex items-start justify-between gap-2">
                            <div className="flex items-center gap-2 flex-wrap">
                              <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded border ${src.badge}`}>
                                {src.icon} {src.label}
                              </span>
                              <span className={`text-xs font-semibold ${meta.color}`}>
                                {meta.icon} {meta.label}
                              </span>
                              {sha && <ShaBadge sha={sha} />}
                            </div>
                            <span className="text-[11px] text-slate-600 shrink-0 mt-0.5">
                              {formatTime(log.createdAt)}
                            </span>
                          </div>
                          <div className="mt-1 flex items-baseline gap-2">
                            <span className="text-xs font-semibold text-slate-300">{log.userName}</span>
                            {summary && (
                              <span className="text-xs text-slate-500 truncate max-w-xs">{summary}</span>
                            )}
                            {externalUrl && (
                              <span className="text-[10px] text-slate-600 ml-auto shrink-0">
                                {log.source === 'NOTION' ? 'Notion에서 열기 →' : 'Drive에서 열기 →'}
                              </span>
                            )}
                          </div>
                        </>
                      );

                      return (
                        <div key={log.id} className="flex gap-3 group">
                          <div className="relative z-10 mt-2.5 shrink-0">
                            <div className={`w-2.5 h-2.5 rounded-full ${src.dot} ring-2 ring-slate-950`} />
                          </div>

                          {externalUrl ? (
                            <a
                              href={externalUrl}
                              target="_blank"
                              rel="noreferrer"
                              className="flex-1 bg-slate-900/60 border border-slate-800 hover:border-slate-600 rounded-xl px-4 py-2.5 transition-colors mb-1 cursor-pointer"
                            >
                              {cardContent}
                            </a>
                          ) : (
                            <div className="flex-1 bg-slate-900/60 border border-slate-800 hover:border-slate-700 rounded-xl px-4 py-2.5 transition-colors mb-1">
                              {cardContent}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              </div>
            ))}

            {/* 더 불러오기 */}
            {hasMore && (
              <div className="text-center pt-2">
                <button
                  onClick={() => fetchLogs(false)}
                  disabled={loadingMore}
                  className="text-xs text-slate-400 hover:text-white transition-colors px-4 py-2 rounded-lg hover:bg-slate-800 disabled:opacity-50"
                >
                  {loadingMore ? '불러오는 중…' : '더 보기'}
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
