import { useMutation } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
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

  const approveMutation = useMutation({
    mutationFn: () => approveLeave(leave.id),
    onSuccess: () => {
      toast.success('已批准');
      onDone();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const rejectMutation = useMutation({
    mutationFn: () => rejectLeave(leave.id),
    onSuccess: () => {
      toast.success('已拒绝');
      onDone();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  if (!canApprove || leave.status !== 1) {
    return null;
  }

  const confirmAct = (label: string, fn: () => void) => {
    if (window.confirm(`确认${label}该请假？`)) fn();
  };

  return (
    <div className={compact ? 'flex gap-2' : 'flex gap-3'} onClick={(e) => e.stopPropagation()}>
      <Button
        size="sm"
        onClick={() => confirmAct('批准', () => approveMutation.mutate())}
        disabled={approveMutation.isPending}
      >
        批准
      </Button>
      <Button
        size="sm"
        variant="secondary"
        onClick={() => confirmAct('拒绝', () => rejectMutation.mutate())}
        disabled={rejectMutation.isPending}
      >
        拒绝
      </Button>
    </div>
  );
}
