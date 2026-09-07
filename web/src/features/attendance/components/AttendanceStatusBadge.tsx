import { cn } from '@/lib/cn';
import type { RosterStatus } from '@/features/attendance/types';

const styles: Record<RosterStatus, string> = {
  not_arrived: 'bg-muted text-muted-fg',
  checked_in: 'bg-success/15 text-success',
  checked_out: 'bg-primary/15 text-primary',
  absent: 'bg-danger/15 text-danger',
  leave: 'bg-warning/15 text-warning',
};

const labels: Record<RosterStatus, string> = {
  not_arrived: '未到',
  checked_in: '已入园',
  checked_out: '已离园',
  absent: '缺勤',
  leave: '请假',
};

type Props = {
  status: RosterStatus;
  label?: string;
  className?: string;
};

export function AttendanceStatusBadge({ status, label, className }: Props): JSX.Element {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium',
        styles[status],
        className,
      )}
    >
      {label ?? labels[status]}
    </span>
  );
}
