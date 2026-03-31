'use client';

import { useState, useEffect } from 'react';
import { useParams } from 'next/navigation';
import {
  DndContext,
  closestCorners,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  DragEndEvent,
  DragOverEvent
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

type Priority = 'LOW' | 'MEDIUM' | 'HIGH';

export default function KanbanBoardPage() {
  const params = useParams();
  const projectId = params.id as string;

  const [tasks, setTasks] = useState<TaskType[]>([]);
  const [loading, setLoading] = useState(true);

  // Filter states
  const [filterPriority, setFilterPriority] = useState<Priority | ''>('');
  const [filterTag, setFilterTag] = useState('');

  // Modal states
  const [selectedTask, setSelectedTask] = useState<TaskType | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  useEffect(() => {
    fetchTasks();
  }, [projectId]);

  const fetchTasks = async () => {
    try {
      const res = await api.get(`/projects/${projectId}/tasks`);
      setTasks(res.data);
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

  const handleDragOver = (event: DragOverEvent) => {
    try {
      const { active, over } = event;
      if (!over) return;
      
      const activeId = active.id;
      const overId = over.id;

      if (activeId === overId) return;

      const activeContainer = tasks.find(t => t.id === activeId)?.status;
      const overContainer = COLUMNS.find(c => c.id === overId)?.id || tasks.find(t => t.id === overId)?.status;

      if (!activeContainer || !overContainer || activeContainer === overContainer) return;

      setTasks((prev) => {
        const activeItems = prev.filter(t => t.status === activeContainer);
        const overItems = prev.filter(t => t.status === overContainer);

        const activeIndex = activeItems.findIndex(t => t.id === activeId);
        const overIndex = overItems.findIndex(t => t.id === overId);

        let newIndex;
        if (overId in COLUMNS.map(c => c.id)) {
          newIndex = overItems.length + 1;
        } else {
          const isBelowOverItem = over && active.rect.current.translated && active.rect.current.translated.top > over.rect.top + over.rect.height;
          const modifier = isBelowOverItem ? 1 : 0;
          newIndex = overIndex >= 0 ? overIndex + modifier : overItems.length + 1;
        }

        const taskCopy = prev.find(t => t.id === activeId)!;
        return [
          ...prev.filter(t => t.id !== activeId),
          { ...taskCopy, status: overContainer as any }
        ];
      });
    } catch (e) {
      console.error(e);
    }
  };

  const handleDragEnd = async (event: DragEndEvent) => {
    const { active, over } = event;
    if (!over) return;

    const activeTask = tasks.find(t => t.id === active.id);
    const originStatus = activeTask?.status;

    // Determine target column
    const overId = over.id;
    let targetStatus = originStatus;
    
    if (COLUMNS.find(c => c.id === overId)) {
      targetStatus = overId as any;
    } else {
      const overTask = tasks.find(t => t.id === overId);
      if (overTask) targetStatus = overTask.status;
    }

    if (activeTask && targetStatus && originStatus !== targetStatus) {
      try {
        await api.patch(`/projects/${projectId}/tasks/${activeTask.id}/status`, { status: targetStatus });
        // Status updated locally in onDragOver roughly, but we finalize here.
        setTasks(prev => prev.map(t => t.id === activeTask.id ? { ...t, status: targetStatus as any } : t));
      } catch (e) {
        console.error('Failed to update status', e);
        fetchTasks(); // rollback on error
      }
    } else if (active.id !== over.id && activeTask && targetStatus === originStatus) {
      // Reording in same column
      const colTasks = tasks.filter(t => t.status === targetStatus);
      const oldIndex = colTasks.findIndex(t => t.id === active.id);
      const newIndex = colTasks.findIndex(t => t.id === over.id);

      setTasks(prev => {
        const otherTasks = prev.filter(t => t.status !== targetStatus);
        const reorderedTasks = arrayMove(colTasks, oldIndex, newIndex);
        return [...otherTasks, ...reorderedTasks];
      });
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

  // 필터 적용
  const filteredTasks = tasks.filter(t => {
    if (filterPriority && t.priority !== filterPriority) return false;
    if (filterTag && !(t as any).tag?.toLowerCase().includes(filterTag.toLowerCase())) return false;
    return true;
  });

  const activeFilters = (filterPriority ? 1 : 0) + (filterTag ? 1 : 0);

  if (loading) return <div className="p-8 text-white">Loading Kanban Board...</div>;

  return (
    <div className="flex flex-col h-full overflow-hidden p-6 absolute inset-0">
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

        {/* 필터 바 */}
        <div className="flex items-center gap-3 flex-wrap pt-1 border-t border-slate-800">
          <span className="text-xs text-slate-500 font-medium">필터</span>

          {/* 우선순위 */}
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

          {/* 태그 */}
          <input
            type="text"
            placeholder="태그 검색..."
            value={filterTag}
            onChange={e => setFilterTag(e.target.value)}
            className="bg-slate-800/50 border border-slate-700 text-slate-200 text-xs px-3 py-1.5 rounded-lg outline-none focus:border-violet-500 placeholder-slate-600 w-32"
          />

          {/* 초기화 */}
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
        onDragOver={handleDragOver}
        onDragEnd={handleDragEnd}
      >
        <div className="flex h-[calc(100vh-220px)] gap-6 overflow-x-auto overflow-y-hidden snap-x pb-4 custom-scrollbar">
          {COLUMNS.map(col => {
            const colTasks = filteredTasks.filter(t => t.status === col.id);
            return (
              <div key={col.id} className="flex flex-col flex-1 min-w-[320px] max-w-sm bg-slate-950/50 rounded-2xl border border-slate-800/80 shadow-2xl p-4 snap-center">
                <div className="flex justify-between items-center mb-4 px-2">
                  <h3 className="font-bold text-slate-200 tracking-wide">{col.title} <span className="ml-2 bg-slate-800 text-slate-400 text-xs px-2 py-0.5 rounded-full">{colTasks.length}</span></h3>
                </div>
                
                <SortableContext 
                  id={col.id}
                  items={colTasks.map(t => t.id)}
                  strategy={verticalListSortingStrategy}
                >
                  <div className="flex-1 overflow-y-auto space-y-3 custom-scrollbar px-1 pb-4">
                    {colTasks.map(task => (
                      <TaskCard 
                        key={task.id} 
                        task={task} 
                        onClick={() => { setSelectedTask(task); setIsModalOpen(true); }} 
                      />
                    ))}
                    {colTasks.length === 0 && (
                      <div className="border-2 border-dashed border-slate-800 rounded-xl h-24 flex items-center justify-center text-slate-600 text-sm font-medium">
                        여기로 드래그하세요
                      </div>
                    )}
                  </div>
                </SortableContext>
              </div>
            );
          })}
        </div>
      </DndContext>

      <style dangerouslySetInnerHTML={{__html: `
        .custom-scrollbar::-webkit-scrollbar {
          width: 8px;
          height: 8px;
        }
        .custom-scrollbar::-webkit-scrollbar-track {
          background: rgba(15, 23, 42, 0); 
        }
        .custom-scrollbar::-webkit-scrollbar-thumb {
          background: rgba(51, 65, 85, 0.5); 
          border-radius: 10px;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb:hover {
          background: rgba(71, 85, 105, 0.8); 
        }
      `}} />

      {isModalOpen && (
        <TaskModal 
          isOpen={isModalOpen} 
          onClose={() => setIsModalOpen(false)}
          onSave={handleSaveTask}
          initialData={selectedTask || undefined}
        />
      )}
    </div>
  );
}
