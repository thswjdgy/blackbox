'use client';

import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';

export interface TaskType {
  id: number;
  title: string;
  description: string;
  status: 'TODO' | 'IN_PROGRESS' | 'DONE';
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  assigneeIds: number[];
}

interface Props {
  task: TaskType;
  onClick: () => void;
  onDelete: (taskId: number) => void;
  memberMap?: Record<number, string>;
}

export function TaskCard({ task, onClick, onDelete, memberMap = {} }: Props) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: task.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  const priorityColors = {
    LOW: 'bg-slate-700 text-slate-300',
    MEDIUM: 'bg-blue-900/50 text-blue-300 border-blue-500/30',
    HIGH: 'bg-rose-900/50 text-rose-300 border-rose-500/30',
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...attributes}
      {...listeners}
      onClick={onClick}
      className={`relative flex flex-col p-4 rounded-xl bg-slate-800 border border-slate-700 shadow-md cursor-grab active:cursor-grabbing hover:border-violet-500/50 transition-colors group
        ${isDragging ? 'opacity-50 scale-105 z-50 shadow-2xl shadow-violet-900/50 border-violet-500' : ''}`}
    >
      <div className="flex items-start justify-between mb-2">
        <span className={`text-[10px] uppercase font-bold px-2 py-0.5 rounded border ${priorityColors[task.priority]}`}>
          {task.priority}
        </span>
        <div className="flex items-center gap-1">
          {task.assigneeIds.length > 0 && (
            <div className="flex -space-x-2">
              {task.assigneeIds.slice(0, 3).map(id => {
                const name = memberMap[id];
                const initial = name ? name.charAt(0) : '?';
                return (
                  <div
                    key={id}
                    className="w-6 h-6 rounded-full bg-indigo-500 border border-slate-800 flex items-center justify-center text-[10px] font-bold text-white shadow-sm"
                    title={name ?? `User ${id}`}
                  >
                    {initial}
                  </div>
                );
              })}
              {task.assigneeIds.length > 3 && (
                <div className="w-6 h-6 rounded-full bg-slate-600 border border-slate-800 flex items-center justify-center text-[10px] font-bold text-white shadow-sm">
                  +{task.assigneeIds.length - 3}
                </div>
              )}
            </div>
          )}
          <button
            onClick={e => { e.stopPropagation(); onDelete(task.id); }}
            className="opacity-0 group-hover:opacity-100 w-6 h-6 rounded-md flex items-center justify-center text-slate-500 hover:text-red-400 hover:bg-red-500/10 transition-all"
            title="삭제"
          >
            ✕
          </button>
        </div>
      </div>
      <h4 className="text-sm font-semibold text-slate-100 mb-1 leading-snug">{task.title}</h4>
      {task.description && (
        <p className="text-xs text-slate-400 line-clamp-2 leading-relaxed">{task.description}</p>
      )}
      {task.assigneeIds.length > 0 && memberMap[task.assigneeIds[0]] && (
        <div className="mt-2 flex flex-wrap gap-1">
          {task.assigneeIds.slice(0, 2).map(id => memberMap[id] && (
            <span key={id} className="text-[10px] text-indigo-300 bg-indigo-900/30 px-1.5 py-0.5 rounded-md">
              {memberMap[id]}
            </span>
          ))}
          {task.assigneeIds.length > 2 && (
            <span className="text-[10px] text-slate-500 bg-slate-800 px-1.5 py-0.5 rounded-md">
              +{task.assigneeIds.length - 2}명
            </span>
          )}
        </div>
      )}
    </div>
  );
}
