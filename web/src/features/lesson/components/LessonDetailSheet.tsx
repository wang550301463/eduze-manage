import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { SheetFrame } from '@/components/ui/Sheet';
import { fetchLesson, fetchLessonLogs } from '@/features/lesson/api';
import { CancelLessonDialog } from '@/features/lesson/components/CancelLessonDialog';
import { LessonRosterTable } from '@/features/lesson/components/LessonRosterTable';
import { RescheduleDialog } from '@/features/lesson/components/RescheduleDialog';

type Props = {
  lessonId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onUpdated: () => void;
};

export function LessonDetailSheet({ lessonId, open, onOpenChange, onUpdated }: Props): JSX.Element {
  const [rescheduleOpen, setRescheduleOpen] = useState(false);
  const [cancelOpen, setCancelOpen] = useState(false);

  const lessonQuery = useQuery({
    queryKey: ['lesson', lessonId],
    queryFn: () => fetchLesson(lessonId!),
    enabled: open && lessonId != null,
  });

  const logsQuery = useQuery({
    queryKey: ['lesson-logs', lessonId],
    queryFn: () => fetchLessonLogs(lessonId!),
    enabled: open && lessonId != null,
  });

  const lesson = lessonQuery.data;

  return (
    <>
      <SheetFrame
        open={open}
        onOpenChange={onOpenChange}
        title={lesson?.classGroupName ?? '课次详情'}
        footer={
          lesson && lesson.status !== 'CANCELLED' ? (
            <div className="flex gap-2">
              <Button variant="secondary" className="flex-1" onClick={() => setRescheduleOpen(true)}>
                调课
              </Button>
              <Button variant="danger" className="flex-1" onClick={() => setCancelOpen(true)}>
                取消
              </Button>
            </div>
          ) : null
        }
      >
        {lesson ? (
          <div className="space-y-4 text-sm">
            <p>
              {lesson.startAt} – {lesson.endAt}
            </p>
            <p>
              教师：{lesson.teacherName ?? '—'} · 画室：{lesson.classRoomName ?? '—'}
            </p>
            <p>状态：{lesson.status}</p>
            <div>
              <h3 className="mb-2 font-medium">学员花名册</h3>
              <LessonRosterTable
                branchId={lesson.branchId}
                lessonStartAt={lesson.startAt}
                readOnly
              />
            </div>
            <div>
              <h3 className="mb-2 font-medium">变动记录</h3>
              <ul className="space-y-2 text-muted-fg">
                {(logsQuery.data ?? []).map((log) => (
                  <li key={log.id}>
                    {log.changeType} · {log.reason ?? '—'} · {log.createdAt}
                  </li>
                ))}
              </ul>
            </div>
          </div>
        ) : null}
      </SheetFrame>
      {lesson ? (
        <>
          <RescheduleDialog
            open={rescheduleOpen}
            onOpenChange={setRescheduleOpen}
            lesson={lesson}
            onDone={onUpdated}
          />
          <CancelLessonDialog
            open={cancelOpen}
            onOpenChange={setCancelOpen}
            lessonId={lesson.id}
            onDone={onUpdated}
          />
        </>
      ) : null}
    </>
  );
}
