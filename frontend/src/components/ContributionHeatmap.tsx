import React, { useMemo } from 'react';

export interface ActivityData {
  date: string; // YYYY-MM-DD 형식의 날짜
  count: number; // 해당 날짜의 활동(커밋, 회의, 태스크 등) 수
}

interface Props {
  data: ActivityData[];
  year?: number; // 기본값은 올해 연도
}

const DAYS_IN_WEEK = 7;

export default function ContributionHeatmap({ data, year = new Date().getFullYear() }: Props) {
  // 날짜별 활동 수를 빠르게 찾기 위한 Map 생성
  const countMap = useMemo(() => {
    const map = new Map<string, number>();
    data.forEach(d => map.set(d.date, d.count));
    return map;
  }, [data]);

  // 선택된 연도의 1월 1일부터 12월 31일까지의 모든 날짜 생성
  const days = useMemo(() => {
    const start = new Date(year, 0, 1);
    const end = new Date(year, 11, 31);
    
    // 첫 주가 일요일부터 시작하지 않을 경우 앞에 빈 칸 삽입
    const startDayOfWeek = start.getDay();
    const result = [];
    
    for (let i = 0; i < startDayOfWeek; i++) {
      result.push(null);
    }
    
    for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
      // 한국 시간에 맞게 ISO 문자열에서 날짜만 추출 (UTC 차이 보정)
      const dateStr = new Date(d.getTime() - (d.getTimezoneOffset() * 60000))
        .toISOString()
        .split('T')[0];
        
      result.push({
        date: dateStr,
        count: countMap.get(dateStr) || 0
      });
    }
    return result;
  }, [year, countMap]);

  // 날짜들을 주 단위(열 단위)로 묶기
  const weeks = useMemo(() => {
    const w = [];
    for (let i = 0; i < days.length; i += DAYS_IN_WEEK) {
      w.push(days.slice(i, i + DAYS_IN_WEEK));
    }
    return w;
  }, [days]);

  // 활동 수에 따른 색상 결정 (GitHub 테마)
  const getColor = (count: number) => {
    if (count === 0) return 'bg-slate-800';
    if (count < 3) return 'bg-emerald-900/80';
    if (count < 6) return 'bg-emerald-700/90';
    if (count < 10) return 'bg-emerald-500';
    return 'bg-emerald-400';
  };

  return (
    <div className="flex flex-col gap-2 p-5 bg-slate-900/60 border border-slate-800 rounded-2xl w-fit overflow-x-auto">
      <div className="flex gap-1">
        {weeks.map((week, wIdx) => (
          <div key={wIdx} className="flex flex-col gap-1">
            {week.map((day, dIdx) => {
              // 1월 1일 이전의 빈 칸 처리
              if (!day) return <div key={dIdx} className="w-3.5 h-3.5 rounded-[3px] bg-transparent" />;
              
              return (
                <div
                  key={day.date}
                  className={`w-3.5 h-3.5 rounded-[3px] transition-colors cursor-pointer group relative ${getColor(day.count)}`}
                >
                  {/* Hover 시 툴팁 표시 */}
                  <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden group-hover:block z-10 w-max px-2.5 py-1.5 text-xs font-semibold text-white bg-slate-800 border border-slate-700 rounded-md shadow-xl pointer-events-none">
                    {day.date} : <span className="text-emerald-400">{day.count}개</span> 활동
                  </div>
                </div>
              );
            })}
          </div>
        ))}
      </div>
      
      {/* 범례 (Legend) */}
      <div className="flex items-center justify-end gap-2 text-[10px] text-slate-500 mt-2 font-medium">
        <span>Less</span>
        <div className="w-3 h-3 rounded-[2px] bg-slate-800" />
        <div className="w-3 h-3 rounded-[2px] bg-emerald-900/80" />
        <div className="w-3 h-3 rounded-[2px] bg-emerald-700/90" />
        <div className="w-3 h-3 rounded-[2px] bg-emerald-500" />
        <div className="w-3 h-3 rounded-[2px] bg-emerald-400" />
        <span>More</span>
      </div>
    </div>
  );
}
