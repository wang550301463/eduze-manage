import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { RequirePermission } from '@/components/auth/RequirePermission';
import { Skeleton } from '@/components/ui/Skeleton';
import { SheetFrame } from '@/components/ui/Sheet';
import { fetchLesson, fetchLessonLogs } from '@/features/lesson/api';
import { CancelLessonDialog } from '@/features/lesson/components/CancelLessonDialog';
import { LessonRosterTable } from '@/features/lesson/components/LessonRosterTable';
import { RescheduleDialog } from '@/features/lesson/components/RescheduleDialog';

type Props = {
  lessonId: string | number | null;
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
            <div className="flex w-full gap-2">
              <RequirePermission perm="lesson:reschedule">
                <Button
                  variant="secondary"
                  className="flex-1"
                  onClick={() => setRescheduleOpen(true)}
                >
                  调课
                </Button>
              </RequirePermission>
              <RequirePermission perm="lesson:cancel">
                <Button variant="danger" className="flex-1" onClick={() => setCancelOpen(true)}>
                  取消课次
                </Button>
              </RequirePermission>
            </div>
          ) : null
        }
      >
        {lessonQuery.isPending ? (
          <Skeleton className="h-48 w-full" />
        ) : lessonQuery.isError ? (
          <div role="alert" className="space-y-3 text-sm">
            <p>课次详情加载失败</p>
            <Button variant="secondary" onClick={() => void lessonQuery.refetch()}>
              重新加载详情
            </Button>
          </div>
        ) : lesson ? (
          <div className="space-y-4 text-sm">
            <p>
              {lesson.startAt.replace('T', ' ').slice(0, 16)} – {lesson.endAt.slice(11, 16)}
            </p>
            <p>
              教师：{lesson.teacherName ?? '—'} · 画室：{lesson.classRoomName ?? '—'}
            </p>
            <p>
              状态：
              {(
                { SCHEDULED: '已排课', CANCELLED: '已取消', COMPLETED: '已完成' } as Record<
                  string,
                  string
                >
              )[lesson.status] ?? lesson.status}
            </p>
            {lesson.note ? <p>备注：{lesson.note}</p> : null}
            <div>
              <h3 className="mb-2 font-medium">学员花名册</h3>
              <LessonRosterTable lessonId={lesson.id} />
            </div>
            <div>
              <h3 className="mb-2 font-medium">变动记录</h3>
              <ul className="space-y-2 text-muted-fg">
                {logsQuery.isPending ? (
                  <li>正在加载记录…</li>
                ) : logsQuery.isError ? (
                  <li>变动记录加载失败</li>
                ) : !logsQuery.data?.length ? (
                  <li>暂无变动记录</li>
                ) : null}
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
