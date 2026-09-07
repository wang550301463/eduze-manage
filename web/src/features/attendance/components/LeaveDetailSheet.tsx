import { SheetFrame } from '@/components/ui/Sheet';
import { LeaveApprovalActions } from '@/features/attendance/components/LeaveApprovalActions';
import type { LeaveRecord } from '@/features/attendance/types';

type Props = {
  leave: LeaveRecord | null;
  onClose: () => void;
  onDone: () => void;
};

export function LeaveDetailSheet({ leave, onClose, onDone }: Props): JSX.Element {
  return (
    <SheetFrame
      open={!!leave}
      onOpenChange={(open) => !open && onClose()}
      title="请假详情"
      footer={leave ? <LeaveApprovalActions leave={leave} onDone={onDone} /> : undefined}
    >
      {leave ? (
        <dl className="space-y-3 text-sm">
          <div>
            <dt className="text-muted-fg">学员</dt>
            <dd className="font-medium">{leave.studentName}</dd>
          </div>
          <div>
            <dt className="text-muted-fg">区间</dt>
            <dd>
              {leave.leaveStartDate} ~ {leave.leaveEndDate}
            </dd>
          </div>
          <div>
            <dt className="text-muted-fg">原因</dt>
            <dd>{leave.reason ?? '—'}</dd>
          </div>
          <div>
            <dt className="text-muted-fg">状态</dt>
            <dd>{leave.statusLabel}</dd>
          </div>
        </dl>
      ) : null}
    </SheetFrame>
  );
}
