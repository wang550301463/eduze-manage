import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { DialogFrame } from '@/components/ui/Dialog';
import { EmptyState } from '@/components/ui/EmptyState';
import { Skeleton } from '@/components/ui/Skeleton';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/Sheet';
import { ApiError, formatApiErrorMessage } from '@/lib/api';
import { cn } from '@/lib/cn';
import { toast } from '@/lib/toast';
import { studentApi } from '../api';
import type { Branch, EntityId } from '../types';
import { statusBadgeClass, statusLabel } from '../utils';
import { StudentAttendanceTab } from './StudentAttendanceTab';
import { StudentBasicTab } from './StudentBasicTab';
import { StudentFormDialog } from './StudentFormDialog';
import { StudentGuardiansTab } from './StudentGuardiansTab';
import { StudentLeaveTab } from './StudentLeaveTab';
import { StudentLessonHourLedgerTab } from './StudentLessonHourLedgerTab';
import { StudentPackagesTab } from './StudentPackagesTab';
import { TransferClassDialog } from './TransferClassDialog';

const TABS = ['基础', '家长', '课时包', '课时流水', '出勤', '请假'] as const;

type Props = {
  studentId: EntityId | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  branches: Branch[];
};

export function StudentDetailSheet({
  studentId,
  open,
  onOpenChange,
  branches,
}: Props): JSX.Element {
  const [tab, setTab] = useState<(typeof TABS)[number]>('基础');
  const [editOpen, setEditOpen] = useState(false);
  const [transferOpen, setTransferOpen] = useState(false);
  const [confirmSuspend, setConfirmSuspend] = useState(false);
  const qc = useQueryClient();

  const {
    data: student,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['student', studentId],
    queryFn: () => studentApi.get(studentId!),
    enabled: open && studentId != null,
  });

  const statusMut = useMutation({
    mutationFn: (status: number) => studentApi.updateStatus(studentId!, status),
    onSuccess: (s) => {
      qc.invalidateQueries({ queryKey: ['students'] });
      qc.invalidateQueries({ queryKey: ['student', studentId] });
      toast.undo({
        message: `已${s.status === 2 ? '停学' : '复学'}`,
        onUndo: () => {
          void studentApi.updateStatus(studentId!, s.status === 2 ? 1 : 2).then(() => {
            qc.invalidateQueries({ queryKey: ['students'] });
            toast.info('已撤销');
          });
        },
      });
    },
    onError: (e: Error) =>
      toast.error(
        e instanceof ApiError ? formatApiErrorMessage(e.payload, e.message) : e.message,
      ),
  });

  const loadError =
    error instanceof ApiError
      ? formatApiErrorMessage(error.payload, error.message)
      : error instanceof Error
        ? error.message
        : '加载失败';

  return (
    <>
      <Sheet open={open} onOpenChange={onOpenChange}>
        <SheetContent className="flex w-full flex-col sm:max-w-lg">
          <SheetHeader className="shrink-0">
            <SheetTitle>{isLoading ? '加载中…' : student?.name ?? '学员详情'}</SheetTitle>
            {student ? (
              <span
                className={cn(
                  'inline-flex w-fit rounded-full px-2 py-0.5 text-xs',
                  statusBadgeClass(student.status),
                )}
              >
                {statusLabel(student.status)}
              </span>
            ) : null}
          </SheetHeader>

          {isLoading ? (
            <div className="space-y-2 p-4">
              <Skeleton className="h-6 w-2/3" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-full" />
            </div>
          ) : null}

          {isError ? (
            <div className="flex flex-1 flex-col justify-center p-4">
              <EmptyState title="无法加载学员详情" description={loadError} />
              <Button className="mt-4" variant="secondary" onClick={() => void refetch()}>
                重试
              </Button>
            </div>
          ) : null}

          {!isLoading && !isError && student ? (
            <div className="flex min-h-0 flex-1 flex-col">
              <div className="flex shrink-0 flex-wrap gap-2 border-b border-border px-4 pb-2">
                <Button size="sm" variant="secondary" onClick={() => setEditOpen(true)}>
                  编辑
                </Button>
                {student.status === 1 ? (
                  <Button size="sm" variant="ghost" onClick={() => setConfirmSuspend(true)}>
                    停学
                  </Button>
                ) : (
                  <Button size="sm" variant="ghost" onClick={() => statusMut.mutate(1)}>
                    复学
                  </Button>
                )}
                <Button size="sm" variant="ghost" onClick={() => setTransferOpen(true)}>
                  调班
                </Button>
              </div>
              <nav className="flex shrink-0 gap-1 overflow-x-auto px-4 pt-2 text-sm">
                {TABS.map((t) => (
                  <button
                    key={t}
                    type="button"
                    className={cn(
                      'rounded-md px-2 py-1',
                      tab === t ? 'bg-primary text-primary-fg' : 'text-muted-fg hover:bg-muted',
                    )}
                    onClick={() => setTab(t)}
                  >
                    {t}
                  </button>
                ))}
              </nav>
              <div className="min-h-0 flex-1 overflow-y-auto p-4">
                {tab === '基础' ? (
                  <StudentBasicTab student={student} onEdit={() => setEditOpen(true)} />
                ) : null}
                {tab === '家长' ? <StudentGuardiansTab studentId={student.id} /> : null}
                {tab === '课时包' ? <StudentPackagesTab studentId={student.id} /> : null}
                {tab === '课时流水' ? (
                  <StudentLessonHourLedgerTab studentId={student.id} />
                ) : null}
                {tab === '出勤' ? <StudentAttendanceTab studentId={student.id} /> : null}
                {tab === '请假' ? <StudentLeaveTab /> : null}
              </div>
            </div>
          ) : null}
        </SheetContent>
      </Sheet>
      {student ? (
        <StudentFormDialog
          open={editOpen}
          onOpenChange={setEditOpen}
          branches={branches}
          student={student}
          onSaved={() => {
            void qc.invalidateQueries({ queryKey: ['student', studentId] });
            void qc.invalidateQueries({ queryKey: ['students'] });
          }}
        />
      ) : null}
      {student ? (
        <TransferClassDialog
          open={transferOpen}
          onOpenChange={setTransferOpen}
          student={student}
          onDone={() => {
            void qc.invalidateQueries({ queryKey: ['student', studentId] });
            void qc.invalidateQueries({ queryKey: ['students'] });
            void qc.invalidateQueries({ queryKey: ['class-groups'] });
          }}
        />
      ) : null}
      <DialogFrame
        open={confirmSuspend}
        onOpenChange={setConfirmSuspend}
        title="确认停学"
        description="停学后学员将不在读列表默认展示。"
        footer={
          <Button
            variant="danger"
            onClick={() => {
              statusMut.mutate(2);
              setConfirmSuspend(false);
            }}
          >
            确认停学
          </Button>
        }
      >
        <span />
      </DialogFrame>
    </>
  );
}
