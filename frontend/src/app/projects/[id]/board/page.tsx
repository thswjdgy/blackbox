'use client';

import { useState, useEffect, useRef } from 'react';
import { useParams } from 'next/navigation';
import {
  DndContext,
  closestCorners,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  DragEndEvent,
  DragOverEvent,
  DragStartEvent,
  DragOverlay,
  useDroppable,
} from '@dnd-kit/core';
import {
  SortableContext,
  verticalListSortingStrategy,
  arrayMove,
} from '@dnd-kit/sortable';

import api from '@/lib/api';
import { TaskCard, TaskType } from '@/components/board/TaskCard';
import { TaskModal } from '@/components/board/TaskModal';

const COLUMNS = [
  { id: 'TODO', title: 'To Do' },
  { id: 'IN_PROGRESS', title: 'In Progress' },
  { id: 'DONE', title: 'Done' }
] as const;

type ColId = typeof COLUMNS[number]['id'];
type Priority = 'LOW' | 'MEDIUM' | 'HIGH';

const COL_IDS = COLUMNS.map(c => c.id) as string[];

function DroppableColumn({ colId, children }: { colId: ColId; children: React.ReactNode }) {
  const { setNodeRef, isOver } = useDroppable({ id: colId });
  return (
    <div
      ref={setNodeRef}
      className={`flex-1 overflow-y-auto space-y-3 custom-scrollbar px-1 pb-4 min-h-[80px] rounded-xl transition-colors duration-150
        ${isOver ? 'bg-violet-900/10' : ''}`}
    >
      {children}
    </div>
  );
}

export default function KanbanBoardPage() {
  const params = useParams();
  const projectId = params.id as string;

  const [tasks, setTasks] = useState<TaskType[]>([]);
  const [memberMap, setMemberMap] = useState<Record<number, string>>({});
  const [members, setMembers] = useState<{ userId: number; name: string }[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTask, setActiveTask] = useState<TaskType | null>(null);
  const dragOriginStatus = useRef<string | null>(null);

  const [filterPriority, setFilterPriority] = useState<Priority | ''>('');
  const [filterTag, setFilterTag] = useState('');

  const [selectedTask, setSelectedTask] = useState<TaskType | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  useEffect(() => {
    fetchTasks();
  }, [projectId]);

  const fetchTasks = async () => {
    try {
      const [taskRes, memberRes] = await Promise.all([
        api.get(`/projects/${projectId}/tasks`),
        api.get(`/projects/${projectId}/members`),
      ]);
      setTasks(taskRes.data);
      const list = (memberRes.data ?? []).map((m: Record<string, unknown>) => ({
        userId: m.userId as number,
        name: m.name as string,
      }));
      setMembers(list);
      setMemberMap(Object.fromEntries(list.map((m: { userId: number; name: string }) => [m.userId, m.name])));
    } catch (e) {
      console.error('Failed to fetch tasks', e);
    } finally {
      setLoading(false);
    }
  };

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor)
  );

  const getContainer = (id: string | number): ColId | undefined => {
    if (COL_IDS.includes(String(id))) return id as ColId;
    return tasks.find(t => t.id === id)?.status;
  };

  const handleDragStart = (event: DragStartEvent) => {
    const task = tasks.find(t => t.id === event.active.id);
    setActiveTask(task ?? null);
    dragOriginStatus.current = task?.status ?? null;
  };

  const handleDragOver = (event: DragOverEvent) => {
    const { active, over } = event;
    if (!over) return;

    const activeContainer = getContainer(active.id);
    const overContainer = getContainer(over.id);

    if (!activeContainer || !overContainer || activeContainer === overContainer) return;

    setTasks(prev => {
      const task = prev.find(t => t.id === active.id);
      if (!task) return prev;
      return prev.map(t => t.id === active.id ? { ...t, status: overContainer } : t);
    });
  };

  const handleDragEnd = async (event: DragEndEvent) => {
    const { active, over } = event;
    setActiveTask(null);
    const originalStatus = dragOriginStatus.current;
    dragOriginStatus.current = null;
    if (!over) return;

    const overContainer = getContainer(over.id);
    if (!originalStatus || !overContainer) return;

    if (originalStatus !== overContainer) {
      // 컬럼 이동 — 백엔드에 저장
      const task = tasks.find(t => t.id === active.id);
      if (task) {
        try {
          await api.patch(`/projects/${projectId}/tasks/${task.id}/status`, { status: overContainer });
        } catch (e) {
          console.error('Failed to update status', e);
          fetchTasks();
        }
      }
    } else if (active.id !== over.id) {
      // Reorder within same column
      setTasks(prev => {
        const colTasks = prev.filter(t => t.status === overContainer);
        const others = prev.filter(t => t.status !== overContainer);
        const oldIdx = colTasks.findIndex(t => t.id === active.id);
        const newIdx = colTasks.findIndex(t => t.id === over.id);
        if (oldIdx === -1 || newIdx === -1) return prev;
        return [...others, ...arrayMove(colTasks, oldIdx, newIdx)];
      });
    }
  };

  const handleDeleteTask = async (taskId: number) => {
    if (!confirm('태스크를 삭제할까요?')) return;
    try {
      await api.delete(`/projects/${projectId}/tasks/${taskId}`);
      setTasks(prev => prev.filter(t => t.id !== taskId));
    } catch (e) {
      console.error(e);
      alert('삭제 실패');
    }
  };

  const handleSaveTask = async (taskData: Partial<TaskType>) => {
    try {
      if (selectedTask) {
        await api.put(`/projects/${projectId}/tasks/${selectedTask.id}`, taskData);
      } else {
        await api.post(`/projects/${projectId}/tasks`, taskData);
      }
      setIsModalOpen(false);
      fetchTasks();
    } catch (e) {
      console.error(e);
      alert('저장 실패');
    }
  };

  const filteredTasks = tasks.filter(t => {
    if (filterPriority && t.priority !== filterPriority) return false;
    if (filterTag && !(t as any).tag?.toLowerCase().includes(filterTag.toLowerCase())) return false;
    return true;
  });

  const activeFilters = (filterPriority ? 1 : 0) + (filterTag ? 1 : 0);

  if (loading) return <div className="p-8 text-white">Loading Kanban Board...</div>;

  return (
    <div className="flex flex-col h-full overflow-hidden p-6">
      <div className="flex shrink-0 flex-col gap-3 backdrop-blur-md bg-slate-900/40 p-5 rounded-2xl mb-6 border border-slate-800 shadow-xl">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-violet-400 to-indigo-400">태스크 관리</h2>
            <p className="text-sm text-slate-400 mt-1">드래그 앤 드롭으로 상태를 변경하세요.</p>
          </div>
          <button
            onClick={() => { setSelectedTask(null); setIsModalOpen(true); }}
            className="bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-500 hover:to-indigo-500 text-white font-bold py-2.5 px-6 rounded-xl shadow-lg shadow-violet-900/30 transition-all hover:-translate-y-0.5"
          >
            + 태스크 생성
          </button>
        </div>

        <div className="flex items-center gap-3 flex-wrap pt-1 border-t border-slate-800">
          <span className="text-xs text-slate-500 font-medium">필터</span>
          <div className="flex gap-1.5">
            {(['LOW', 'MEDIUM', 'HIGH'] as Priority[]).map(p => (
              <button
                key={p}
                onClick={() => setFilterPriority(prev => prev === p ? '' : p)}
                className={`text-[11px] font-bold px-2.5 py-1 rounded-lg border transition-all
                  ${filterPriority === p
                    ? p === 'HIGH' ? 'bg-rose-500/20 text-rose-300 border-rose-500'
                      : p === 'LOW' ? 'bg-slate-600 text-white border-slate-500'
                      : 'bg-blue-500/20 text-blue-300 border-blue-500'
                    : 'bg-slate-800 border-slate-700 text-slate-500 hover:text-slate-300 hover:border-slate-600'
                  }`}
              >
                {p}
              </button>
            ))}
          </div>
          <input
            type="text"
            placeholder="태그 검색..."
            value={filterTag}
            onChange={e => setFilterTag(e.target.value)}
            className="bg-slate-800/50 border border-slate-700 text-slate-200 text-xs px-3 py-1.5 rounded-lg outline-none focus:border-violet-500 placeholder-slate-600 w-32"
          />
          {activeFilters > 0 && (
            <button
              onClick={() => { setFilterPriority(''); setFilterTag(''); }}
              className="text-xs text-slate-500 hover:text-slate-300 transition-colors"
            >
              초기화 ✕
            </button>
          )}
          {activeFilters > 0 && (
            <span className="text-xs text-violet-400 font-medium ml-auto">
              {filteredTasks.length} / {tasks.length} 표시 중
            </span>
          )}
        </div>
      </div>

      <DndContext
        sensors={sensors}
        collisionDetection={closestCorners}
        onDragStart={handleDragStart}
        onDragOver={handleDragOver}
        onDragEnd={handleDragEnd}
      >
        <div className="flex h-[calc(100vh-220px)] gap-6 overflow-x-auto overflow-y-hidden snap-x pb-4 custom-scrollbar">
          {COLUMNS.map(col => {
            const colTasks = filteredTasks.filter(t => t.status === col.id);
            return (
              <div
                key={col.id}
                className="flex flex-col flex-1 min-w-[320px] max-w-sm bg-slate-950/50 rounded-2xl border border-slate-800/80 shadow-2xl p-4 snap-center"
              >
                <div className="flex justify-between items-center mb-4 px-2">
                  <h3 className="font-bold text-slate-200 tracking-wide">
                    {col.title}
                    <span className="ml-2 bg-slate-800 text-slate-400 text-xs px-2 py-0.5 rounded-full">{colTasks.length}</span>
                  </h3>
                </div>

                <SortableContext
                  id={col.id}
                  items={colTasks.map(t => t.id)}
                  strategy={verticalListSortingStrategy}
                >
                  <DroppableColumn colId={col.id}>
                    {colTasks.map(task => (
                      <TaskCard
                        key={task.id}
                        task={task}
                        onClick={() => { setSelectedTask(task); setIsModalOpen(true); }}
                        onDelete={handleDeleteTask}
                        memberMap={memberMap}
                      />
                    ))}
                    {colTasks.length === 0 && (
                      <div className="border-2 border-dashed border-slate-800 rounded-xl h-24 flex items-center justify-center text-slate-600 text-sm font-medium">
                        여기로 드래그하세요
                      </div>
                    )}
                  </DroppableColumn>
                </SortableContext>
              </div>
            );
          })}
        </div>

        <DragOverlay>
          {activeTask && (
            <div className="opacity-90 rotate-2 scale-105">
              <TaskCard
                task={activeTask}
                onClick={() => {}}
                onDelete={() => {}}
                memberMap={memberMap}
              />
            </div>
          )}
        </DragOverlay>
      </DndContext>

      <style dangerouslySetInnerHTML={{__html: `
        .custom-scrollbar::-webkit-scrollbar { width: 8px; height: 8px; }
        .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
        .custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(51,65,85,0.5); border-radius: 10px; }
        .custom-scrollbar::-webkit-scrollbar-thumb:hover { background: rgba(71,85,105,0.8); }
      `}} />

      {isModalOpen && (
        <TaskModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onSave={handleSaveTask}
          initialData={selectedTask || undefined}
          members={members}
        />
      )}
    </div>
  );
}
