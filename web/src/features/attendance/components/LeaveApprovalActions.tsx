import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { DialogFrame } from '@/components/ui/Dialog';
import { approveLeave, rejectLeave } from '@/features/attendance/api';
import type { LeaveRecord } from '@/features/attendance/types';
import { useAuthStore } from '@/features/auth/store';
import { toast } from '@/lib/toast';

type Props = {
  leave: LeaveRecord;
  onDone: () => void;
  compact?: boolean;
};

export function LeaveApprovalActions({ leave, onDone, compact }: Props): JSX.Element | null {
  const canApprove = useAuthStore((s) => s.user?.permissions.includes('leave:approve'));
  const [action, setAction] = useState<'approve' | 'reject' | null>(null);

  const approveMutation = useMutation({
    mutationFn: () => approveLeave(leave.id),
    onSuccess: () => {
      setAction(null);
      toast.success('已批准');
      onDone();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const rejectMutation = useMutation({
    mutationFn: () => rejectLeave(leave.id),
    onSuccess: () => {
      setAction(null);
      toast.success('已拒绝');
      onDone();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  if (!canApprove || leave.status !== 1) {
    return null;
  }

  const pending = approveMutation.isPending || rejectMutation.isPending;
  const label = action === 'reject' ? '拒绝' : '批准';

  return (
    <div className={compact ? 'flex gap-2' : 'flex gap-3'} onClick={(e) => e.stopPropagation()}>
      <Button
        size="sm"
        onClick={() => setAction('approve')}
        disabled={pending}
      >
        批准
      </Button>
      <Button
        size="sm"
        variant="secondary"
        onClick={() => setAction('reject')}
        disabled={pending}
      >
        拒绝
      </Button>
      <DialogFrame
        open={action !== null}
        onOpenChange={(open) => { if (!open && !pending) setAction(null); }}
        title={`确认${label}请假`}
        description={`${leave.studentName} · ${leave.leaveStartDate} ~ ${leave.leaveEndDate}`}
        footer={<>
          <Button variant="secondary" disabled={pending} onClick={() => setAction(null)}>取消</Button>
          <Button disabled={pending} onClick={() => {
            if (action === 'approve') approveMutation.mutate();
            if (action === 'reject') rejectMutation.mutate();
          }}>{pending ? '处理中…' : `确认${label}`}</Button>
        </>}
      >
        <p className="text-sm text-muted-fg">{leave.reason || '未填写原因'}</p>
      </DialogFrame>
    </div>
  );
}
